/**
 * Copyright (c) 2005-2013, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.moods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.maximizer.Evaluator;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoodTrigger
	implements Comparable<MoodTrigger>
{
	private static Map<String, Set<String>> knownSources = new HashMap<String, Set<String>>();

	private int skillId = -1;
	private final AdventureResult effect;
	private boolean isThiefTrigger = false;

	private final StringBuffer stringForm;

	private String action;
	private final String type;
	private final String name;

	private int count;
	private AdventureResult item;
	private UseSkillRequest skill;

	private MoodTrigger( final String type, final AdventureResult effect, final String action )
	{
		this.type = type;
		this.effect = effect;
		this.name = effect == null ? null : effect.getName();

		if ( ( action.startsWith( "use " ) || action.startsWith( "cast " ) ) && action.indexOf( ";" ) == -1 )
		{
			// Determine the command, the count amount,
			// and the parameter's unambiguous form.

			int spaceIndex = action.indexOf( " " );
			String parameters = StringUtilities.getDisplayName( action.substring( spaceIndex + 1 ).trim() );

			if ( action.startsWith( "use" ) )
			{
				boolean star = parameters.charAt( 0 ) == '*';
				if ( star )
				{
					spaceIndex = parameters.indexOf( " " );
					parameters = parameters.substring( spaceIndex ).trim();
				}

				AdventureResult item = MoodTrigger.getUsableItem( parameters );

				if ( item != null )
				{
					if ( star )
					{
						this.action = "use * " + item.getDataName();
					}
					else
					{
						this.item = item;
						this.count = item.getCount();
						this.action = "use " + this.count + " " + item.getDataName();
					}
				}
			}
			else
			{
				this.count = 1;

				if ( Character.isDigit( parameters.charAt( 0 ) ) )
				{
					spaceIndex = parameters.indexOf( " " );
					this.count = StringUtilities.parseInt( parameters.substring( 0, spaceIndex ) );
					parameters = parameters.substring( spaceIndex ).trim();
				}

				if ( !SkillDatabase.contains( parameters ) )
				{
					parameters = SkillDatabase.getUsableSkillName( parameters );
				}

				this.skill = UseSkillRequest.getInstance( parameters );

				if ( this.skill != null )
				{
					this.action = "cast " + this.count + " " + this.skill.getSkillName();
				}
			}
		}

		if ( this.action == null )
		{
			this.count = 1;
			this.action = action;
		}

		if ( type.equals( "lose_effect" ) && effect != null )
		{
			Set<String> existingActions = (Set<String>) MoodTrigger.knownSources.get( effect.getName() );

			if ( existingActions == null )
			{
				existingActions = new LinkedHashSet<String>();
				MoodTrigger.knownSources.put( effect.getName(), existingActions );
			}

			existingActions.add( this.action );

			String skillName = UneffectRequest.effectToSkill( effect.getName() );
			if ( SkillDatabase.contains( skillName ) )
			{
				this.skillId = SkillDatabase.getSkillId( skillName );
				this.isThiefTrigger = SkillDatabase.isAccordionThiefSong( skillId );
			}
		}

		this.stringForm = new StringBuffer();
		this.updateStringForm();
	}

	private static AdventureResult getUsableItem( final String parameters )
	{
		AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, false );
		if ( item != null )
		{
			String name = item.bangPotionAlias();
			if ( !name.equals( item.getDataName() ) )
			{
				item = AdventureResult.tallyItem( name, item.getCount(), false );
			}
		}
		return item;
	}

	public static String getKnownSources( String name )
	{
		Set existingActions = (Set) MoodTrigger.knownSources.get( name );

		if ( existingActions == null )
		{
			return "";
		}

		StringBuilder buffer = new StringBuilder();

		Iterator actionIterator = existingActions.iterator();

		while ( actionIterator.hasNext() )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( "|" );
			}

			String action = (String) actionIterator.next();
			buffer.append( action );
		}

		return buffer.toString();
	}

	public static void clearKnownSources()
	{
		MoodTrigger.knownSources.clear();
	}
	
	public boolean matches()
	{
		return KoLConstants.activeEffects.contains( this.effect );
	}
	
	public boolean matches( AdventureResult effect )
	{
		if ( this.effect == null )
		{
			return effect == null;
		}
		
		if ( effect == null )
		{
			return false;
		}
		
		return this.effect.equals( effect );
	}

	public boolean isSkill()
	{
		return this.skillId != -1;
	}
	
	public AdventureResult getEffect()
	{
		return this.effect;
	}
	
	public String getType()
	{
		return this.type;
	}

	public String getName()
	{
		return this.name;
	}

	public String getAction()
	{
		return this.action;
	}

	@Override
	public String toString()
	{
		return this.stringForm.toString();
	}

	public String toSetting()
	{
		if ( this.effect == null )
		{
			return this.type + " => " + this.action;
		}

		String canonical = StringUtilities.getCanonicalName( this.name );

		if ( this.item != null )
		{
			return this.type + " " + canonical + " => use " + this.count + " " + StringUtilities.getCanonicalName( this.item.getDataName() );
		}

		if ( this.skill != null )
		{
			return this.type + " " + canonical + " => cast " + this.count + " " + StringUtilities.getCanonicalName( this.skill.getSkillName() );
		}

		return this.type + " " + canonical + " => " + this.action;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof MoodTrigger ) )
		{
			return false;
		}

		MoodTrigger mt = (MoodTrigger) o;
		if ( !this.type.equals( mt.type ) )
		{
			return false;
		}

		if ( this.name == null )
		{
			// This is an unconditional trigger. Compare actions.
			return this.action.equalsIgnoreCase( mt.action );
		}

		return this.name.equals( mt.name );
	}

	@Override
	public int hashCode()
	{
		int hash = 0;
		hash += this.type != null ? this.type.hashCode() : 0;
		hash += 31 * (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}

	public void execute( final int multiplicity )
	{
		if ( !this.shouldExecute( multiplicity ) )
		{
			return;
		}

		if ( this.item != null )
		{
			AdventureResult item = this.item;
			int itemId = item.getItemId();

			if ( itemId == -1 )
			{
				item = item.resolveBangPotion();
				itemId = item.getItemId();
			}

			if ( itemId == -1 )
			{
				RequestLogger.printLine( "You have not yet identified the " + item.toString() );
				return;
			}

			item = item.getInstance( Math.max( this.count, this.count * multiplicity ) );
			RequestThread.postRequest( UseItemRequest.getInstance( item ) );
			return;
		}

		if ( this.skill != null )
		{
			int casts = Math.max( this.count, this.count * multiplicity );
			this.skill.setBuffCount( casts );
			this.skill.setTarget( KoLCharacter.getUserName() );
			RequestThread.postRequest( this.skill );

			if ( !UseSkillRequest.lastUpdate.equals( "" ) )
			{
				String name = this.skill.getSkillName();
				KoLmafia.updateDisplay( MafiaState.ERROR, "Mood failed to cast " + casts + " " + name + ": " + UseSkillRequest.lastUpdate );
				RequestThread.declareWorldPeace();
			}

			return;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.action );
	}

	public boolean shouldExecute( final int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// Don't cast if you are restricted by your current class/skills
		if ( this.effect != null && Evaluator.checkEffectConstraints( this.effect.getName() ) )
		{
			return false;
		}

		// Don't cast it if you can't
		if ( this.skill != null && !KoLCharacter.hasSkill( this.skill.getSkillName() ) )
		{
			return false;
		}

		if ( this.type.equals( "unconditional" ) || this.effect == null )
		{
			return true;
		}

		if ( this.type.equals( "lose_effect" ) && multiplicity > 0 )
		{
			return true;
		}

		if ( this.type.equals( "gain_effect" ) )
		{
			return KoLConstants.activeEffects.contains( this.effect );
		}

		if ( MoodManager.unstackableAction( this.action ) )
		{
			return !KoLConstants.activeEffects.contains( this.effect );
		}

		int activeCount = this.effect.getCount( KoLConstants.activeEffects );

		if ( multiplicity == -1 )
		{
			return activeCount <= 1;
		}

		return activeCount <= 5;
	}

	public boolean isThiefTrigger()
	{
		return this.isThiefTrigger;
	}

	public int compareTo( final MoodTrigger o )
	{
		if ( o == null || !( o instanceof MoodTrigger ) )
		{
			return -1;
		}

		MoodTrigger mt = (MoodTrigger) o;
		String othertype = mt.type;
		String othername = mt.name;
		String otherTriggerAction = mt.action;

		// Order: unconditional, gain_effect, lose_effect

		if ( this.type.equals( "unconditional" ) )
		{
			if ( othertype.equals( "unconditional" ) )
			{
				return this.action.compareToIgnoreCase( otherTriggerAction );
			}
			return -1;
		}

		if ( this.type.equals( "gain_effect" ) )
		{
			if ( othertype.equals( "unconditional" ) )
			{
				return 1;
			}
			if ( othertype.equals( "gain_effect" ) )
			{
				return this.name.compareTo( othername );
			}
			return -1;
		}

		if ( this.type.equals( "lose_effect" ) )
		{
			if ( othertype.equals( "lose_effect" ) )
			{
				return this.name.compareTo( othername );
			}
			return  1;
		}

		return 0;
	}

	public void updateStringForm()
	{
		this.stringForm.setLength( 0 );

		if ( this.type.equals( "gain_effect" ) )
		{
			this.stringForm.append( "When I get" );
		}
		else if ( this.type.equals( "lose_effect" ) )
		{
			this.stringForm.append( "When I run low on" );
		}
		else
		{
			this.stringForm.append( "Always" );
		}

		if ( this.name != null )
		{
			this.stringForm.append( " " );
			this.stringForm.append( this.name );
		}

		this.stringForm.append( ", " );
		this.stringForm.append( this.action );
	}

	public static final MoodTrigger constructNode( final String line )
	{
		String[] pieces = CharacterEntities.unescape( line ).split( " => " );
		if ( pieces.length != 2 )
		{
			return null;
		}

		String type = null;

		if ( pieces[ 0 ].startsWith( "gain_effect" ) )
		{
			type = "gain_effect";
		}
		else if ( pieces[ 0 ].startsWith( "lose_effect" ) )
		{
			type = "lose_effect";
		}
		else if ( pieces[ 0 ].startsWith( "unconditional" ) )
		{
			type = "unconditional";
		}

		if ( type == null )
		{
			return null;
		}

		AdventureResult effect = null;

		if ( !type.equals( "unconditional" ) )
		{
			int spaceIndex = pieces[ 0 ].indexOf( " " );
			String name = pieces[ 0 ].substring( spaceIndex ).trim();

			effect = EffectDatabase.getFirstMatchingEffect( name );

			if ( effect == null )
			{
				return null;
			}
		}

		return new MoodTrigger( type, effect, pieces[ 1 ].trim() );
	}
}
