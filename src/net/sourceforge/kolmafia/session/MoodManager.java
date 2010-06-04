/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MoodManager
{
	private static final AdventureResult[] AUTO_CLEAR =
	{
		new AdventureResult( "Beaten Up", 1, true ),
		new AdventureResult( "Tetanus", 1, true ),
		new AdventureResult( "Amnesia", 1, true ),
		new AdventureResult( "Cunctatitis", 1, true ),
		new AdventureResult( "Hardly Poisoned at All", 1, true ),
		new AdventureResult( "Majorly Poisoned", 1, true ),
		new AdventureResult( "A Little Bit Poisoned", 1, true ),
		new AdventureResult( "Somewhat Poisoned", 1, true ),
		new AdventureResult( "Really Quite Poisoned", 1, true ),
	};

	public static final AdventureResult TURTLING_ROD = ItemPool.get( ItemPool.TURTLING_ROD, 1 );
	public static final AdventureResult EAU_DE_TORTUE = EffectPool.get( EffectPool.EAU_DE_TORTUE );

	private static final TreeMap reference = new TreeMap();
	private static final SortedListModel displayList = new SortedListModel();
	private static final SortedListModel availableMoods = new SortedListModel();

	private static int thiefTriggerLimit = 3;
	private static boolean isExecuting = false;
	private static SortedListModel mappedList = null;

	public static final File getFile()
	{
		return new File( UtilityConstants.SETTINGS_LOCATION, KoLCharacter.baseUserName() + "_moods.txt" );
	}

	public static final boolean isExecuting()
	{
		return MoodManager.isExecuting;
	}

	public static final void updateFromPreferences()
	{
		MoodManager.reference.clear();
		MoodManager.availableMoods.clear();
		MoodManager.displayList.clear();

		String currentMood = Preferences.getString( "currentMood" );
		MoodManager.loadSettings();

		MoodManager.setMood( currentMood );
		MoodManager.saveSettings();
	}

	public static final SortedListModel getAvailableMoods()
	{
		return MoodManager.availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given mood. Also ensures that all defaults are loaded for the given
	 * mood if no data exists.
	 */

	public static final void setMood( String mood )
	{
		mood =
			mood == null || mood.trim().equals( "" ) ? "default" : StringUtilities.globalStringDelete(
				mood.toLowerCase().trim(), " " );

		if ( mood.equals( "clear" ) || mood.equals( "autofill" ) || mood.startsWith( "exec" ) || mood.startsWith( "repeat" ) )
		{
			return;
		}

		Preferences.setString( "currentMood", mood );

		MoodManager.ensureProperty( mood );
		MoodManager.availableMoods.setSelectedItem( mood );

		MoodManager.mappedList = (SortedListModel) MoodManager.reference.get( mood );

		MoodManager.displayList.clear();
		MoodManager.displayList.addAll( MoodManager.mappedList );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static final SortedListModel getTriggers()
	{
		return MoodManager.displayList;
	}

	public static final void addTriggers( final Object[] nodes, final int duration )
	{
		MoodManager.removeTriggers( nodes );
		StringBuffer newAction = new StringBuffer();

		for ( int i = 0; i < nodes.length; ++i )
		{
			MoodTrigger mt = (MoodTrigger) nodes[ i ];
			String[] action = mt.getAction().split( " " );

			newAction.setLength( 0 );
			newAction.append( action[ 0 ] );

			if ( action.length > 1 )
			{
				newAction.append( ' ' );
				int startIndex = 2;

				if ( action[ 1 ].charAt( 0 ) == '*' )
				{
					newAction.append( '*' );
				}
				else
				{
					if ( !Character.isDigit( action[ 1 ].charAt( 0 ) ) )
					{
						startIndex = 1;
					}

					newAction.append( duration );
				}

				for ( int j = startIndex; j < action.length; ++j )
				{
					newAction.append( ' ' );
					newAction.append( action[ j ] );
				}
			}

			MoodManager.addTrigger( mt.getType(), mt.getName(), newAction.toString() );
		}
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static final void addTrigger( final String type, final String name, final String action )
	{
		MoodManager.addTrigger( MoodTrigger.constructNode( type + " " + name + " => " + action ) );
	}

	private static final void addTrigger( final MoodTrigger node )
	{
		if ( node == null )
		{
			return;
		}

		if ( MoodManager.displayList.contains( node ) )
		{
			MoodManager.removeTrigger( node );
		}

		MoodManager.mappedList.add( node );
		MoodManager.displayList.add( node );
	}

	/**
	 * Removes all the current displayList.
	 */

	public static final void removeTriggers( final Object[] toRemove )
	{
		for ( int i = 0; i < toRemove.length; ++i )
		{
			MoodManager.removeTrigger( (MoodTrigger) toRemove[ i ] );
		}
	}

	private static final void removeTrigger( final MoodTrigger toRemove )
	{
		MoodManager.mappedList.remove( toRemove );
		MoodManager.displayList.remove( toRemove );
	}

	public static final void minimalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = MoodManager.getDefaultAction( "lose_effect", effects[ i ].getName() );
			if ( action != null && !action.equals( "" ) )
			{
				MoodManager.addTrigger( "lose_effect", effects[ i ].getName(), action );
			}
		}
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	public static final void maximalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		UseSkillRequest[] skills = new UseSkillRequest[ KoLConstants.availableSkills.size() ];
		KoLConstants.availableSkills.toArray( skills );

		MoodManager.thiefTriggerLimit = UseSkillRequest.songLimit();
		ArrayList thiefSkills = new ArrayList();

		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[ i ].getSkillId() < 1000 )
			{
				continue;
			}

			// Combat rate increasers are not handled by mood
			// autofill, since KoLmafia has a preference for
			// non-combats in the area below.

			if ( skills[ i ].getSkillId() == 1019 || skills[ i ].getSkillId() == 6016 )
			{
				continue;
			}

			if ( skills[ i ].getSkillId() > 6000 && skills[ i ].getSkillId() < 7000 )
			{
				thiefSkills.add( skills[ i ].getSkillName() );
				continue;
			}

			String effectName = UneffectRequest.skillToEffect( skills[ i ].getSkillName() );
			if ( EffectDatabase.contains( effectName ) )
			{
				String action = MoodManager.getDefaultAction( "lose_effect", effectName );
				MoodManager.addTrigger( "lose_effect", effectName, action );
			}
		}

		if ( !thiefSkills.isEmpty() && thiefSkills.size() <= MoodManager.thiefTriggerLimit )
		{
			String[] skillNames = new String[ thiefSkills.size() ];
			thiefSkills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
			{
				String effectName = UneffectRequest.skillToEffect( skillNames[ i ] );
				MoodManager.addTrigger( "lose_effect", effectName, MoodManager.getDefaultAction(
					"lose_effect", effectName ) );
			}
		}
		else if ( !thiefSkills.isEmpty() )
		{
			// To make things more convenient for testing, automatically
			// add some of the common accordion thief buffs if they are
			// available skills.

			String[] rankedBuffs = null;

			if ( KoLCharacter.isHardcore() )
			{
				rankedBuffs = new String[]
				{
					"Fat Leon's Phat Loot Lyric",
					"The Moxious Madrigal",
					"Aloysius' Antiphon of Aptitude",
					"The Sonata of Sneakiness",
					"The Psalm of Pointiness",
					"Ur-Kel's Aria of Annoyance"
				};
			}
			else
			{
				rankedBuffs = new String[]
				{
					"Fat Leon's Phat Loot Lyric",
					"Aloysius' Antiphon of Aptitude",
					"Ur-Kel's Aria of Annoyance",
					"The Sonata of Sneakiness",
					"Jackasses' Symphony of Destruction",
					"Cletus's Canticle of Celerity"
				};
			}

			int foundSkillCount = 0;
			for ( int i = 0; i < rankedBuffs.length && foundSkillCount < MoodManager.thiefTriggerLimit; ++i )
			{
				if ( KoLCharacter.hasSkill( rankedBuffs[ i ] ) )
				{
					++foundSkillCount;
					MoodManager.addTrigger(
						"lose_effect", UneffectRequest.skillToEffect( rankedBuffs[ i ] ), "cast " + rankedBuffs[ i ] );
				}
			}
		}

		// Now add in all the buffs from the minimal buff set, as those
		// are included here.

		MoodManager.minimalSet();
	}

	/**
	 * Deletes the current mood and sets the current mood to apathetic.
	 */

	public static final void deleteCurrentMood()
	{
		String currentMood = Preferences.getString( "currentMood" );

		if ( currentMood.equals( "default" ) )
		{
			MoodManager.mappedList.clear();
			MoodManager.displayList.clear();

			MoodManager.setMood( "default" );
			return;
		}

		MoodManager.reference.remove( currentMood );
		MoodManager.availableMoods.remove( currentMood );

		MoodManager.availableMoods.setSelectedItem( "apathetic" );
		MoodManager.setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static final void copyTriggers( final String newListName )
	{
		String currentMood = Preferences.getString( "currentMood" );

		if ( newListName == "" )
		{
			return;
		}

		// Can't copy into apathetic list
		if ( currentMood.equals( "apathetic" ) || newListName.equals( "apathetic" ) )
		{
			return;
		}

		// Copy displayList from current list, then
		// create and switch to new list

		SortedListModel oldList = MoodManager.mappedList;
		MoodManager.setMood( newListName );

		MoodManager.mappedList.addAll( oldList );
		MoodManager.displayList.addAll( oldList );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static final void execute()
	{
		MoodManager.execute( -1 );
	}

	public static final void burnExtraMana( final boolean isManualInvocation )
	{
		String nextBurnCast;
		
		float manaBurnTrigger = Preferences.getFloat( "manaBurningTrigger" );
		if ( !isManualInvocation && KoLCharacter.getCurrentMP() <
			(int) (manaBurnTrigger * KoLCharacter.getMaximumMP()) )
		{
			return;
		}

		boolean was = MoodManager.isExecuting;
		MoodManager.isExecuting = true;

		int currentMP = -1;

		while ( currentMP != KoLCharacter.getCurrentMP() && ( nextBurnCast = MoodManager.getNextBurnCast() ) != null )
		{
			currentMP = KoLCharacter.getCurrentMP();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( nextBurnCast );
		}

		MoodManager.isExecuting = was;
	}

	public static final void burnMana( int minimum )
	{
		String nextBurnCast;

		boolean was = MoodManager.isExecuting;
		MoodManager.isExecuting = true;

		minimum = Math.max( 0, minimum );
		int currentMP = -1;

		while ( currentMP != KoLCharacter.getCurrentMP() && ( nextBurnCast = MoodManager.getNextBurnCast( minimum ) ) != null )
		{
			currentMP = KoLCharacter.getCurrentMP();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( nextBurnCast );
		}

		MoodManager.isExecuting = was;
	}

	public static final String getNextBurnCast()
	{
		// Punt immediately if mana burning is disabled

		float manaBurnPreference = Preferences.getFloat( "manaBurningThreshold" );
		if ( manaBurnPreference < 0.0f )
		{
			return null;
		}

		float manaRecoverPreference = Preferences.getFloat( "mpAutoRecovery" );
		int minimum = (int) ( Math.max( manaBurnPreference, manaRecoverPreference ) * (float) KoLCharacter.getMaximumMP() ) + 1;
		return MoodManager.getNextBurnCast( minimum );
	}

	public static final String getNextBurnCast( final int minimum )
	{
		// Punt immediately if already burned enough or must recover MP

		int allowedMP = KoLCharacter.getCurrentMP() - minimum;
		if ( allowedMP <= 0 )
		{
			return null;
		}

		// Pre-calculate possible breakfast/libram skill

		boolean onlyMood = !Preferences.getBoolean( "allowNonMoodBurning" );
		String breakfast = Preferences.getBoolean( "allowSummonBurning" ) ?
			MoodManager.considerBreakfastSkill( minimum ) : null;
		int summonThreshold = Preferences.getInteger( "manaBurnSummonThreshold" );
		int durationLimit = KoLCharacter.getAdventuresLeft() + 1000;
		Burn chosen = null;
		ArrayList burns = new ArrayList();

		// Rather than maintain mood-related buffs only, maintain any
		// active effect that the character can auto-cast. Examine all
		// active effects in order from lowest duration to highest.

		for ( int i = 0; i < KoLConstants.activeEffects.size() && KoLmafia.permitsContinue(); ++i )
		{
			AdventureResult currentEffect = (AdventureResult) KoLConstants.activeEffects.get( i );
			String skillName = UneffectRequest.effectToSkill( currentEffect.getName() );

			// Only cast if the player knows the skill

			if ( !SkillDatabase.contains( skillName ) || !KoLCharacter.hasSkill( skillName ) )
			{
				continue;
			}

			int skillId = SkillDatabase.getSkillId( skillName );

			// Never recast ode when doing MP burning, because
			// there's no need for it to have a long duration.

			if ( skillId == 6014 )
			{
				continue;
			}

			// Don't recast Hobopolis buffs, since they have daily
			// limits and the player may have other plans for the
			// remaining casts.

			if ( skillId >= 6020 && skillId <= 6024 )
			{
				continue;
			}

			// Encounter rate modifying buffs probably shouldn't be
			// cast during conditional recast.

			if ( skillId == 1019 || skillId == 5017 || skillId == 6015 || skillId == 6016 )
			{
				continue;
			}

			int currentDuration = currentEffect.getCount();

			// If we already have 1000 turns more than the number
			// of turns the player has available, that's enough.
			// Also, if we have more than twice the turns of some
			// more expensive buff, save up for that one instead
			// of extending this one.

			if ( currentDuration >= durationLimit )
			{
				continue;
			}

			// If the player wants to burn mana on summoning
			// skills, only do so if all potential effects have at
			// least 10 turns remaining.

			if ( breakfast != null && currentDuration >= summonThreshold )
			{
				return breakfast;
			}

			// If the player only wants to cast buffs related to
			// their mood, then skip the buff if it's not in the
			// any of the player's moods.

			if ( onlyMood && !MoodManager.effectInMood( currentEffect ) )
			{
				continue;
			}

			// If we don't have enough MP for this skill, consider
			// extending some cheaper effect - but only up to twice
			// the turns of this effect, so that a slow but steady
			// MP gain won't be used exclusively on the cheaper effect.

			if ( SkillDatabase.getMPConsumptionById( skillId ) > allowedMP )
			{
				durationLimit = Math.max( 10, Math.min( currentDuration * 2, durationLimit ) );
				continue;
			}

			Burn b = new Burn( skillId, skillName, currentDuration );
			if ( chosen == null )
			{
				chosen = b;
			}

			burns.add( b );
			breakfast = null; // we're definitely extending an effect
		}

		if ( chosen == null )
		{
			// No buff found. Return possible breakfast/libram skill
			return breakfast;
		}

		// Simulate casting all of the extendable skills in a balanced
		// manner, to determine the final count of the chosen skill -
		// rather than making multiple server requests.
		Iterator i = burns.iterator();
		while ( i.hasNext() )
		{
			Burn b = (Burn) i.next();
			if ( b.duration >= durationLimit )
			{
				i.remove();
				continue;
			}
			// The max(1,...) guarantees that this loop will terminate.
			int cost = Math.max( 1, SkillDatabase.getMPConsumptionById( b.skillId ) );
			if ( cost > allowedMP )
			{
				i.remove();
				continue;
			}
			++b.count;
			b.duration += SkillDatabase.getEffectDuration( b.skillId );
			allowedMP -= cost;
			Collections.sort( burns );
			i = burns.iterator();
		}

		return "cast " + chosen.count + " " + chosen.skillName;
	}

	private static class Burn
		implements Comparable
	{
		public int skillId;
		public String skillName;
		public int duration;
		public int count;

		public Burn( final int skillId, final String skillName, final int duration )
		{
			this.skillId = skillId;
			this.skillName = skillName;
			this.duration = duration;
			this.count = 0;
		}

		public int compareTo( final Object o )
		{
			return this.duration - ( (Burn) o ).duration;
		}
	}

	private static final boolean effectInMood( final AdventureResult effect )
	{
		for ( int j = 0; j < MoodManager.displayList.size(); ++j )
		{
			if ( effect.equals( ( (MoodTrigger) MoodManager.displayList.get( j ) ).effect ) )
			{
				return true;
			}
		}

		return false;
	}

	private static final String considerBreakfastSkill( final int minimum )
	{
		for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
		{
			if ( !KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[ i ] ) )
			{
				continue;
			}
			if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
			{
				continue;
			}
			if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
			{
				continue;
			}

			UseSkillRequest skill = UseSkillRequest.getInstance( UseSkillRequest.BREAKFAST_SKILLS[ i ] );

			int maximumCast = skill.getMaximumCast();

			if ( maximumCast == 0 )
			{
				continue;
			}

			int availableMP = KoLCharacter.getCurrentMP() - minimum;
			int mpPerUse = SkillDatabase.getMPConsumptionById( skill.getSkillId() );
			int castCount = Math.min( maximumCast, availableMP / mpPerUse );

			if ( castCount > 0 )
			{
				return "cast " + castCount + " " + UseSkillRequest.BREAKFAST_SKILLS[ i ];
			}
		}

		return MoodManager.considerLibramSummon( minimum );
	}

	private static final String considerLibramSummon( final int minimum )
	{
		int castCount = SkillDatabase.libramSkillCasts( KoLCharacter.getCurrentMP() - minimum );
		if ( castCount <= 0 )
		{
			return null;
		}

		List castable =	 BreakfastManager.getBreakfastLibramSkills();
		int skillCount = castable.size();

		if ( skillCount == 0 )
		{
			return null;
		}
		
		int nextCast = Preferences.getInteger( "libramSummons" );
		StringBuffer buf = new StringBuffer();
		for ( int i = 0; i < skillCount; ++i )
		{
			int thisCast = (castCount + skillCount - 1 - i) / skillCount;
			if ( thisCast <= 0 ) continue;
			buf.append( "cast " );
			buf.append( thisCast );
			buf.append( " " );
			buf.append( (String) castable.get( (i + nextCast) % skillCount ) );
			buf.append( ";" );
		}

		return buf.toString();
	}

	public static final void execute( final int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		if ( !MoodManager.willExecute( multiplicity ) )
		{
			return;
		}

		MoodManager.isExecuting = true;

		MoodTrigger current = null;

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		MoodManager.thiefTriggerLimit = UseSkillRequest.songLimit();

		// If you have too many accordion thief buffs to execute
		// your displayList, then shrug off your extra buffs, but
		// only if the user allows for this.

		// First we determine which buffs are already affecting the
		// character in question.

		ArrayList thiefBuffs = new ArrayList();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[ i ].getName() );
			if ( SkillDatabase.contains( skillName ) )
			{
				int skillId = SkillDatabase.getSkillId( skillName );
				if ( skillId > 6000 && skillId < 7000 )
				{
					thiefBuffs.add( effects[ i ] );
				}
			}
		}

		// Then, we determine the displayList which are thief skills, and
		// thereby would be cast at this time.

		ArrayList thiefSkills = new ArrayList();
		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( current.isThiefTrigger() )
			{
				thiefSkills.add( current.effect );
			}
		}

		// We then remove the displayList which will be used from the pool of
		// effects which could be removed.  Then we compute how many we
		// need to remove and remove them.

		thiefBuffs.removeAll( thiefSkills );

		int buffsToRemove = thiefBuffs.size() + thiefSkills.size() - MoodManager.thiefTriggerLimit;
		for ( int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "uneffect " + ( (AdventureResult) thiefBuffs.get( i ) ).getName() );
		}

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.

		for ( int i = 0; !KoLmafia.refusesContinue() && i < MoodManager.displayList.size(); ++i )
		{
			current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( current.skillId != -1 )
			{
				current.execute( multiplicity );
			}
		}

		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( current.skillId == -1 )
			{
				current.execute( multiplicity );
			}
		}

		MoodManager.isExecuting = false;
	}

	public static final boolean willExecute( final int multiplicity )
	{
		if ( MoodManager.displayList.isEmpty() || Preferences.getString( "currentMood" ).equals( "apathetic" ) )
		{
			return false;
		}

		boolean willExecute = false;

		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) MoodManager.displayList.get( i );
			willExecute |= current.shouldExecute( multiplicity );
		}

		return willExecute;
	}

	public static final ArrayList getMissingEffects()
	{
		ArrayList missing = new ArrayList();
		if ( MoodManager.displayList.isEmpty() )
		{
			return missing;
		}

		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( current.getType().equals( "lose_effect" ) && !KoLConstants.activeEffects.contains( current.effect ) )
			{
				missing.add( current.effect );
			}
		}

		// Special case: if the character has a turtling rod equipped,
		// assume the Eau de Tortue is a possibility

		if ( KoLCharacter.hasEquipped( MoodManager.TURTLING_ROD, EquipmentManager.OFFHAND ) && !KoLConstants.activeEffects.contains( MoodManager.EAU_DE_TORTUE ) )
		{
			missing.add( MoodManager.EAU_DE_TORTUE );
		}

		return missing;
	}

	public static final void removeMalignantEffects()
	{
		String action;

		for ( int i = 0; i < MoodManager.AUTO_CLEAR.length; ++i )
		{
			if ( KoLConstants.activeEffects.contains( MoodManager.AUTO_CLEAR[ i ] ) )
			{
				action = MoodManager.getDefaultAction( "gain_effect", MoodManager.AUTO_CLEAR[ i ].getName() );

				if ( action.startsWith( "cast" ) )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				}
				else if ( action.startsWith( "use" ) )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				}
				else if ( KoLCharacter.canInteract() )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				}
			}
		}
	}

	public static final int getMaintenanceCost()
	{
		if ( MoodManager.displayList.isEmpty() )
		{
			return 0;
		}

		int runningTally = 0;

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( !current.getType().equals( "lose_effect" ) || !current.shouldExecute( -1 ) )
			{
				continue;
			}

			String action = current.getAction();
			if ( !action.startsWith( "cast" ) && !action.startsWith( "buff" ) )
			{
				continue;
			}

			int spaceIndex = action.indexOf( " " );
			if ( spaceIndex == -1 )
			{
				continue;
			}

			action = action.substring( spaceIndex + 1 );

			int multiplier = 1;

			if ( Character.isDigit( action.charAt( 0 ) ) )
			{
				spaceIndex = action.indexOf( " " );
				multiplier = StringUtilities.parseInt( action.substring( 0, spaceIndex ) );
				action = action.substring( spaceIndex + 1 );
			}

			String skillName = SkillDatabase.getSkillName( action );
			if ( skillName != null )
			{
				runningTally +=
					SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( skillName ) ) * multiplier;
			}
		}

		// Running tally calculated, return the amount of
		// MP required to sustain this mood.

		return runningTally;
	}

	/**
	 * Stores the settings maintained in this <code>MoodManager</code> object to disk for later retrieval.
	 */

	public static final void saveSettings()
	{
		PrintStream writer = LogStream.openStream( getFile(), true );

		SortedListModel triggerList;
		for ( int i = 0; i < MoodManager.availableMoods.size(); ++i )
		{
			triggerList = (SortedListModel) MoodManager.reference.get( MoodManager.availableMoods.get( i ) );
			writer.println( "[ " + MoodManager.availableMoods.get( i ) + " ]" );

			for ( int j = 0; j < triggerList.size(); ++j )
			{
				writer.println( ( (MoodTrigger) triggerList.get( j ) ).toSetting() );
			}

			writer.println();
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object. Note that all settings are overridden; if the
	 * given file does not exist, the current global settings will also be rewritten into the appropriate file.
	 */

	public static final void loadSettings()
	{
		MoodManager.reference.clear();
		MoodManager.availableMoods.clear();

		MoodManager.ensureProperty( "default" );
		MoodManager.ensureProperty( "apathetic" );

		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			BufferedReader reader = FileUtilities.getReader( getFile() );

			String line;
			String currentKey = "";

			while ( ( line = reader.readLine() ) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey =
						StringUtilities.globalStringDelete(
							line.substring( 1, line.length() - 1 ).trim().toLowerCase(), " " );

					if ( currentKey.equals( "clear" ) || currentKey.equals( "autofill" ) || currentKey.startsWith( "exec" ) || currentKey.startsWith( "repeat" ) )
					{
						currentKey = "default";
					}

					MoodManager.displayList.clear();

					if ( MoodManager.reference.containsKey( currentKey ) )
					{
						MoodManager.mappedList = (SortedListModel) MoodManager.reference.get( currentKey );
					}
					else
					{
						MoodManager.mappedList = new SortedListModel();
						MoodManager.reference.put( currentKey, MoodManager.mappedList );
						MoodManager.availableMoods.add( currentKey );
					}
				}
				else if ( line.length() != 0 )
				{
					MoodManager.addTrigger( MoodTrigger.constructNode( line ) );
				}
			}

			MoodManager.displayList.clear();

			reader.close();
			reader = null;

			MoodManager.setMood( Preferences.getString( "currentMood" ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final String getDefaultAction( final String type, final String name )
	{
		if ( type == null || name == null )
		{
			return "";
		}

		// We can look at the displayList list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		String strictAction = "";

		for ( int i = 0; i < MoodManager.displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) MoodManager.displayList.get( i );
			if ( current.getType().equals( type ) && current.name.equals( name ) )
			{
				strictAction = current.action;
			}
		}

		if ( type.equals( "unconditional" ) )
		{
			return strictAction;
		}

		if ( type.equals( "lose_effect" ) )
		{
			if ( !strictAction.equals( "" ) )
			{
				return strictAction;
			}

			String action = EffectDatabase.getDefaultAction( name );
			if ( action != null )
			{
				return action;
			}

			return strictAction;
		}

		// gain_effect from here on

		if ( UneffectRequest.isShruggable( name ) )
		{
			return "uneffect " + name;
		}

		if ( name.indexOf( "Poisoned" ) != -1 )
		{
			return "use anti-anti-antidote";
		}

		boolean otterTongueClearable =
			name.equals( "Beaten Up" );

		if ( otterTongueClearable && KoLCharacter.hasSkill( "Tongue of the Otter" ) )
		{
			return "cast Tongue of the Otter";
		}

		boolean walrusTongueClearable =
			name.equals( "Axe Wound" ) ||
			name.equals( "Beaten Up" ) ||
			name.equals( "Grilled" ) ||
			name.equals( "Half Eaten Brain" ) ||
			name.equals( "Missing Fingers" ) ||
			name.equals( "Sunburned" );

		if ( walrusTongueClearable && KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
		{
			return "cast Tongue of the Walrus";
		}

		boolean powerNapClearable =
			name.equals( "Apathy" ) ||
			name.equals( "Confused" ) ||
			name.equals( "Cunctatitis" ) ||
			name.equals( "Embarrassed" ) ||
			name.equals( "Easily Embarrassed" ) ||
			name.equals( "Prestidigysfunction" ) ||
			name.equals( "Sleepy" ) ||
			name.equals( "Socialismydia" ) ||
			name.equals( "Sunburned" ) ||
			name.equals( "Tenuous Grip on Reality" ) ||
			name.equals( "Tetanus" ) ||
			name.equals( "Wussiness" );

		if ( powerNapClearable && KoLCharacter.hasSkill( "Disco Power Nap" ) )
		{
			return "cast Disco Power Nap";
		}

		boolean discoNapClearable =
			name.equals( "Confused" ) ||
			name.equals( "Embarrassed" ) ||
			name.equals( "Sleepy" ) ||
			name.equals( "Sunburned" ) ||
			name.equals( "Wussiness" );

		if ( discoNapClearable && KoLCharacter.hasSkill( "Disco Nap" ) )
		{
			return "cast Disco Nap";
		}

		boolean forestTearsClearable =
			name.equals( "Beaten Up" );

		if ( forestTearsClearable && InventoryManager.hasItem( UneffectRequest.FOREST_TEARS ) )
		{
			return "use 1 forest tears";
		}

		boolean isRemovable = UneffectRequest.isRemovable( name );
		if ( isRemovable && InventoryManager.hasItem( UneffectRequest.REMEDY ) )
		{
			return "uneffect " + name;
		}

		boolean tinyHouseClearable =
			name.equals( "Beaten Up" ) ||
			name.equals( "Confused" ) ||
			name.equals( "Sunburned" ) ||
			name.equals( "Wussiness" );

		if ( tinyHouseClearable && ( KoLCharacter.canInteract() || InventoryManager.hasItem( UneffectRequest.TINY_HOUSE ) ) )
		{
			return "use 1 tiny house";
		}

		if ( isRemovable && KoLCharacter.canInteract() )
		{
			return "uneffect " + name;
		}

		return strictAction;
	}

	public static final boolean currentlyExecutable( final AdventureResult effect, final String action )
	{
		// It's always OK to boost a stackable effect.
		// Otherwise, it's only OK if it's not active.
		return !MoodManager.unstackableAction( action ) || !KoLConstants.activeEffects.contains( effect );
	}

	public static final boolean unstackableAction( final String action )
	{
		return
			action.indexOf( "absinthe" ) != -1 ||
			action.indexOf( "astral mushroom" ) != -1 ||
			action.indexOf( "oasis" ) != -1 ||
			action.indexOf( "turtle pheromones" ) != -1 ||
			action.indexOf( "gong" ) != -1;
	}

	/**
	 * Ensures that the given property exists, and if it does not exist, initializes it to the given value.
	 */

	private static final void ensureProperty( final String key )
	{
		if ( !MoodManager.reference.containsKey( key ) )
		{
			SortedListModel defaultList = new SortedListModel();
			MoodManager.reference.put( key, defaultList );
			MoodManager.availableMoods.add( key );
		}
	}

	public static class MoodTrigger
		implements Comparable
	{
		private int skillId = -1;
		private final AdventureResult effect;
		private boolean isThiefTrigger = false;

		private final StringBuffer stringForm;

		private String action;
		private final String type, name;

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
					this.item = ItemFinder.getFirstMatchingItem( parameters, false );

					if ( this.item != null )
					{
						this.count = this.item.getCount();
						this.action = "use " + this.count + " " + this.item.bangPotionAlias();
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

			if ( type != null && type.equals( "lose_effect" ) && effect != null )
			{
				String skillName = UneffectRequest.effectToSkill( effect.getName() );
				if ( SkillDatabase.contains( skillName ) )
				{
					this.skillId = SkillDatabase.getSkillId( skillName );
					this.isThiefTrigger = this.skillId > 6000 && this.skillId < 7000;
				}
			}

			this.stringForm = new StringBuffer();
			this.updateStringForm();
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

			if ( this.item != null )
			{
				return this.type + " " + StringUtilities.getCanonicalName( this.name ) + " => use " + this.count + " " + StringUtilities.getCanonicalName( this.item.bangPotionAlias() );
			}

			if ( this.skill != null )
			{
				return this.type + " " + StringUtilities.getCanonicalName( this.name ) + " => cast " + this.count + " " + StringUtilities.getCanonicalName( this.skill.getSkillName() );
			}

			return this.type + " " + StringUtilities.getCanonicalName( this.name ) + " => " + this.action;
		}

		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof MoodTrigger ) )
			{
				return false;
			}

			MoodTrigger mt = (MoodTrigger) o;
			if ( !this.type.equals( mt.getType() ) )
			{
				return false;
			}

			if ( this.name == null )
			{
				return mt.name == null;
			}

			if ( mt.getType() == null )
			{
				return false;
			}

			return this.name.equals( mt.name );
		}

		public void execute( final int multiplicity )
		{
			if ( !this.shouldExecute( multiplicity ) )
			{
				return;
			}

			if ( this.item != null )
			{
				RequestThread.postRequest( new UseItemRequest( this.item.getInstance( Math.max(
					this.count, this.count * multiplicity ) ) ) );

				return;
			}
			else if ( this.skill != null )
			{
				this.skill.setBuffCount( Math.max( this.count, this.count * multiplicity ) );
				this.skill.setTarget( KoLCharacter.getUserName() );
				RequestThread.postRequest( this.skill );

				if ( !UseSkillRequest.lastUpdate.equals( "" ) )
				{
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

		public int compareTo( final Object o )
		{
			if ( o == null || !( o instanceof MoodTrigger ) )
			{
				return -1;
			}

			String othertype = ( (MoodTrigger) o ).getType();
			String othername = ( (MoodTrigger) o ).name;
			String otherTriggerAction = ( (MoodTrigger) o ).action;

			int compareResult = 0;

			if ( this.type.equals( "unconditional" ) )
			{
				if ( othertype.equals( "unconditional" ) )
				{
					compareResult = this.action.compareToIgnoreCase( otherTriggerAction );
				}
				else
				{
					compareResult = -1;
				}
			}
			else if ( this.type.equals( "gain_effect" ) )
			{
				if ( othertype.equals( "unconditional" ) )
				{
					compareResult = 1;
				}
				else if ( othertype.equals( "gain_effect" ) )
				{
					compareResult = this.name.compareToIgnoreCase( othername );
				}
				else
				{
					compareResult = -1;
				}
			}
			else if ( this.type.equals( "lose_effect" ) )
			{
				if ( othertype.equals( "lose_effect" ) )
				{
					compareResult = this.name.compareToIgnoreCase( othername );
				}
				else
				{
					compareResult = 1;
				}
			}

			return compareResult;
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

			String name =
				type.equals( "unconditional" ) ? null : pieces[ 0 ].substring( pieces[ 0 ].indexOf( " " ) ).trim();

			AdventureResult effect = null;

			if ( !type.equals( "unconditional" ) )
			{
				effect = EffectDatabase.getFirstMatchingEffect( name );

				if ( effect == null )
				{
					return null;
				}
			}

			return new MoodTrigger( type, effect, pieces[ 1 ].trim() );
		}
	}
}
