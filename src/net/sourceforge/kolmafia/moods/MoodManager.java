/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
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

	private static Mood currentMood = null;
	private static final SortedListModel availableMoods = new SortedListModel();
	private static final SortedListModel displayList = new SortedListModel();
	
	static boolean isExecuting = false;

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
		MoodManager.availableMoods.clear();

		MoodManager.currentMood = null;
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

	public static final void setMood( String newMoodName )
	{
		if ( newMoodName == null || newMoodName.trim().equals( "" ) )
		{
			newMoodName = "default";
		}

		if ( newMoodName.equals( "clear" ) || newMoodName.equals( "autofill" ) || newMoodName.startsWith( "exec" ) || newMoodName.startsWith( "repeat" ) )
		{
			return;
		}

		Preferences.setString( "currentMood", newMoodName );
		
		Mood newMood = new Mood( newMoodName );
		Iterator moodIterator = MoodManager.availableMoods.iterator();
		
		MoodManager.currentMood = null;
		
		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			
			if ( mood.equals( newMood ) )
			{
				MoodManager.currentMood = mood;
				
				if ( newMoodName.indexOf( " extends " ) != -1 )
				{
					MoodManager.currentMood.setParentNames( newMood.getParentNames() );
				}
				
				break;
			}
		}

		if ( MoodManager.currentMood == null )
		{
			MoodManager.currentMood = newMood;			
			MoodManager.availableMoods.remove( MoodManager.currentMood );
			MoodManager.availableMoods.add( MoodManager.currentMood );
		}

		MoodManager.displayList.clear();
		MoodManager.displayList.addAll( MoodManager.currentMood.getTriggers() );
		
		MoodManager.availableMoods.setSelectedItem( MoodManager.currentMood );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static final SortedListModel getTriggers()
	{
		return MoodManager.displayList;
	}
	
	public static final List getTriggers( String moodName )
	{
		if ( moodName == null || moodName.length() == 0 )
		{
			return Collections.EMPTY_LIST;
		}
		
		Mood moodToFind = new Mood( moodName );
		
		Iterator moodIterator = MoodManager.availableMoods.iterator();

		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			
			if ( mood.equals( moodToFind ) )
			{
				return mood.getTriggers();
			}
		}
		
		return Collections.EMPTY_LIST;
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
		MoodTrigger trigger = MoodTrigger.constructNode( type + " " + name + " => " + action );

		if ( MoodManager.currentMood.addTrigger( trigger ) )
		{
			MoodManager.displayList.remove( trigger );
			MoodManager.displayList.add( trigger );
		}
	}

	/**
	 * Removes all the current displayList.
	 */

	public static final void removeTriggers( final Object[] triggers )
	{
		for ( int i = 0; i < triggers.length; ++i )
		{
			MoodTrigger trigger = (MoodTrigger) triggers[ i ];

			if ( MoodManager.currentMood.removeTrigger( trigger ) )
			{
				MoodManager.displayList.remove( trigger );
			}
		}
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

		if ( !thiefSkills.isEmpty() && thiefSkills.size() <= UseSkillRequest.songLimit() )
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
			for ( int i = 0; i < rankedBuffs.length && foundSkillCount < UseSkillRequest.songLimit(); ++i )
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
		MoodManager.displayList.clear();

		String moodName = Preferences.getString( "currentMood" );

		if ( moodName.equals( "default" ) )
		{
			MoodManager.currentMood.getTriggers().clear();
			return;
		}

		MoodManager.availableMoods.remove( MoodManager.currentMood );
		MoodManager.setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static final void copyTriggers( final String newMoodName )
	{
		// Copy displayList from current list, then
		// create and switch to new list

		Mood newMood = new Mood( newMoodName );
		newMood.copyFrom( MoodManager.currentMood );
		
		MoodManager.availableMoods.add( newMood );
		MoodManager.setMood( newMoodName );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static final void execute()
	{
		MoodManager.execute( -1 );
	}

	public static final boolean effectInMood( final AdventureResult effect )
	{
		return MoodManager.currentMood.isTrigger( effect );
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

		// Then, we determine the triggers which are thief skills, and
		// thereby would be cast at this time.

		ArrayList thiefKeep = new ArrayList();
		ArrayList thiefNeed = new ArrayList();
		
		List triggers = MoodManager.currentMood.getTriggers();
		
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( current.isThiefTrigger() )
			{
				AdventureResult effect = current.getEffect();
				
				if ( thiefBuffs.remove( effect ) )
				{	// Already have this one
					thiefKeep.add( effect );
				}
				else
				{	// New or completely expired buff - we may
					// need to shrug a buff to make room for it.
					thiefNeed.add( effect );
				}
			}
		}

		int buffsToRemove = thiefNeed.isEmpty() ? 0 :
			thiefBuffs.size() + thiefKeep.size() + thiefNeed.size() - UseSkillRequest.songLimit();

		for ( int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "uneffect " + ( (AdventureResult) thiefBuffs.get( i ) ).getName() );
		}

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.
		
		triggerIterator = triggers.iterator();

		while ( !KoLmafia.refusesContinue() && triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( current.isSkill() )
			{
				current.execute( multiplicity );
			}
		}

		triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( !current.isSkill() )
			{
				current.execute( multiplicity );
			}
		}

		MoodManager.isExecuting = false;
	}

	public static final boolean willExecute( final int multiplicity )
	{
		if ( !MoodManager.currentMood.isExecutable() )
		{
			return false;
		}

		boolean willExecute = false;
		
		List triggers = MoodManager.currentMood.getTriggers();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
			willExecute |= current.shouldExecute( multiplicity );
		}

		return willExecute;
	}

	public static final List getMissingEffects()
	{
		List triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return Collections.EMPTY_LIST;
		}

		ArrayList missing = new ArrayList();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
			if ( current.getType().equals( "lose_effect" ) && !current.matches() )
			{
				missing.add( current.getEffect() );
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
		List triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return 0;
		}

		int runningTally = 0;
		Iterator triggerIterator = triggers.iterator();

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
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
		Iterator moodIterator = MoodManager.availableMoods.iterator();

		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			writer.println( mood.toSettingString() );;
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object. Note that all settings are overridden; if the
	 * given file does not exist, the current global settings will also be rewritten into the appropriate file.
	 */

	public static final void loadSettings()
	{
		MoodManager.availableMoods.clear();

		Mood mood = new Mood( "apathetic" );
		MoodManager.availableMoods.add( mood );
		
		mood = new Mood( "default" );
		MoodManager.availableMoods.add( mood );
		
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			BufferedReader reader = FileUtilities.getReader( getFile() );
			
			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				line = line.trim();
				
				if ( line.length() == 0 )
				{
					continue;
				}
				
				if ( !line.startsWith( "[" ) )
				{
					mood.addTrigger( MoodTrigger.constructNode( line ) );
					continue;
				}

				int closeBracketIndex = line.indexOf( "]" );
				
				if ( closeBracketIndex == -1 )
				{
					continue;
				}

				String moodName = line.substring( 1, closeBracketIndex );
				mood = new Mood( moodName );

				MoodManager.availableMoods.remove( mood );
				MoodManager.availableMoods.add( mood );
			}

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

		List triggers = MoodManager.currentMood.getTriggers();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();

			if ( current.getType().equals( type ) && current.getName().equals( name ) )
			{
				strictAction = current.getAction();
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
}
