/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class MoodSettings implements KoLConstants
{
	private static final AdventureResult [] AUTO_CLEAR =
	{
		new AdventureResult( "Beaten Up", 1, true ), new AdventureResult( "Tetanus", 1, true ),
		new AdventureResult( "Hardly Poisoned at All", 1, true ), new AdventureResult( "Majorly Poisoned", 1, true ),
		new AdventureResult( "A Little Bit Poisoned", 1, true ), new AdventureResult( "Somewhat Poisoned", 1, true ),
		new AdventureResult( "Really Quite Poisoned", 1, true )
	};

	private static final TreeMap reference = new TreeMap();
	private static final SortedListModel displayList = new SortedListModel();
	private static final SortedListModel availableMoods = new SortedListModel();

	private static int thiefTriggerLimit = 3;
	private static File settingsFile = null;

	private static boolean isExecuting = false;
	private static SortedListModel mappedList = null;

	public static final String settingsFileName()
	{	return KoLCharacter.baseUserName() + "_moods.txt";
	}

	public static final boolean isExecuting()
	{	return isExecuting;
	}

	public static final void restoreDefaults()
	{
		reference.clear();
		availableMoods.clear();
		displayList.clear();

		String currentMood = StaticEntity.getProperty( "currentMood" );
		settingsFile = new File( SETTINGS_LOCATION, settingsFileName() );
		loadSettings();

		setMood( currentMood );
		saveSettings();
	}

	public static final SortedListModel getAvailableMoods()
	{	return availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given
	 * mood.  Also ensures that all defaults are loaded
	 * for the given mood if no data exists.
	 */

	public static final void setMood( String mood )
	{
		mood = (mood == null || mood.trim().equals( "" )) ? "default" :
			StaticEntity.globalStringDelete( mood.toLowerCase().trim(), " " );

		if ( mood.equals( "clear" ) || mood.equals( "autofill" ) || mood.startsWith( "exec" ) || mood.startsWith( "repeat" ) )
			return;

		StaticEntity.setProperty( "currentMood", mood );

		ensureProperty( mood );
		availableMoods.setSelectedItem( mood );

		mappedList = (SortedListModel) reference.get( mood );

		displayList.clear();
		displayList.addAll( mappedList );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static final SortedListModel getTriggers()
	{	return displayList;
	}

	public static final void addTriggers( Object [] nodes, int duration )
	{
		removeTriggers( nodes );
		StringBuffer newAction = new StringBuffer();

		for ( int i = 0; i < nodes.length; ++i )
		{
			MoodTrigger mt = (MoodTrigger) nodes[i];
			String [] action = mt.getAction().split( " " );

			newAction.setLength(0);
			newAction.append( action[0] );

			if ( action.length > 1 )
			{
				newAction.append( ' ' );
				int startIndex = 2;

				if ( action[1].charAt(0) == '*' )
				{
					newAction.append( '*' );
				}
				else
				{
					if ( !Character.isDigit( action[1].charAt(0) ) )
						startIndex = 1;

					newAction.append( duration );
				}

				for ( int j = startIndex; j < action.length; ++j )
				{
					newAction.append( ' ' );
					newAction.append( action[j] );
				}
			}

			addTrigger( mt.getType(), mt.getName(), newAction.toString() );
		}
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static final void addTrigger( String type, String name, String action )
	{	addTrigger( MoodTrigger.constructNode( type + " " + name + " => " + action ) );
	}

	private static final void addTrigger( MoodTrigger node )
	{
		if ( node == null )
			return;

		if ( displayList.contains( node ) )
			removeTrigger( node );

		mappedList.add( node );
		displayList.add( node );
	}

	/**
	 * Removes all the current displayList.
	 */

	public static final void removeTriggers( Object [] toRemove )
	{
		for ( int i = 0; i < toRemove.length; ++i )
			removeTrigger( (MoodTrigger) toRemove[i] );
	}

	private static final void removeTrigger( MoodTrigger toRemove )
	{
		mappedList.remove( toRemove );
		displayList.remove( toRemove );
	}

	public static final void minimalSet()
	{
		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = getDefaultAction( "lose_effect", effects[i].getName() );
			if ( action != null && !action.equals( "" ) )
				addTrigger( "lose_effect", effects[i].getName(), action );
		}
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	public static final void maximalSet()
	{
		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		thiefTriggerLimit = KoLCharacter.hasEquipped( UseSkillRequest.PLEXI_PENDANT ) ? 4 : 3;
		ArrayList thiefSkills = new ArrayList();

		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[i].getSkillId() < 1000 )
				continue;

			// Combat rate increasers are not handled by mood
			// autofill, since KoLmafia has a preference for
			// non-combats in the area below.

			if ( skills[i].getSkillId() == 1019 || skills[i].getSkillId() == 6016 )
				continue;

			if ( skills[i].getSkillId() > 6000 && skills[i].getSkillId() < 7000 )
			{
				thiefSkills.add( skills[i].getSkillName() );
				continue;
			}

			String effectName = UneffectRequest.skillToEffect( skills[i].getSkillName() );
			if ( StatusEffectDatabase.contains( effectName ) )
				addTrigger( "lose_effect", effectName, getDefaultAction( "lose_effect", effectName ) );
		}


		if ( !thiefSkills.isEmpty() && thiefSkills.size() <= thiefTriggerLimit )
		{
			String [] skillNames = new String[ thiefSkills.size() ];
			thiefSkills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
			{
				String effectName = UneffectRequest.skillToEffect( skillNames[i] );
				addTrigger( "lose_effect", effectName, getDefaultAction( "lose_effect", effectName ) );
			}
		}
		else if ( !thiefSkills.isEmpty() )
		{
			// To make things more convenient for testing, automatically
			// add some of the common accordion thief buffs if they are
			// available skills.

			String [] rankedBuffs = null;

			if ( KoLCharacter.isHardcore() )
			{
				rankedBuffs = new String [] {
					"Fat Leon's Phat Loot Lyric", "The Moxious Madrigal",
					"Aloysius' Antiphon of Aptitude", "The Sonata of Sneakiness",
					"The Psalm of Pointiness", "Ur-Kel's Aria of Annoyance"
				};
			}
			else
			{
				rankedBuffs = new String [] {
					"Fat Leon's Phat Loot Lyric", "Aloysius' Antiphon of Aptitude",
					"Ur-Kel's Aria of Annoyance", "The Sonata of Sneakiness",
					"Jackasses' Symphony of Destruction", "Cletus's Canticle of Celerity"
				};
			}

			int foundSkillCount = 0;
			for ( int i = 0; i < rankedBuffs.length && foundSkillCount < thiefTriggerLimit; ++i )
			{
				if ( KoLCharacter.hasSkill( rankedBuffs[i] ) )
				{
					++foundSkillCount;
					addTrigger( "lose_effect", UneffectRequest.skillToEffect( rankedBuffs[i] ), "cast " + rankedBuffs[i] );
				}
			}
		}

		// Now add in all the buffs from the minimal buff set, as those
		// are included here.

		minimalSet();
	}

	/**
	 * Deletes the current mood and sets the current mood
	 * to apathetic.
	 */

	public static final void deleteCurrentMood()
	{
		String currentMood = StaticEntity.getProperty( "currentMood" );

		if ( currentMood.equals( "default" ) )
		{
			mappedList.clear();
			displayList.clear();

			setMood( "default" );
			return;
		}

		reference.remove( currentMood );
		availableMoods.remove( currentMood );

		availableMoods.setSelectedItem( "apathetic" );
		setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static final void copyTriggers( String newListName )
	{
		String currentMood = StaticEntity.getProperty( "currentMood" );

		if ( newListName == "" )
			return;

		// Can't copy into apathetic list
		if ( currentMood.equals( "apathetic" ) || newListName.equals( "apathetic" ) )
			return;

		// Copy displayList from current list, then
		// create and switch to new list

		SortedListModel oldList = mappedList;
		setMood( newListName );

		mappedList.addAll( oldList );
		displayList.addAll( oldList );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static final void execute()
	{	execute( -1 );
	}

	public static final void burnExtraMana( boolean isManualInvocation )
	{
		if ( !isManualInvocation && KoLCharacter.canInteract() && KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			return;

		String nextBurnCast;

		isExecuting = true;

		while ( (nextBurnCast = getNextBurnCast( true )) != null )
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( nextBurnCast );

		isExecuting = false;
	}

	public static final String getNextBurnCast( boolean shouldExecute )
	{
		// Rather than keeping a safety for the player, let the player
		// make the mistake of burning below their auto-restore threshold.

		int starting = (int) (StaticEntity.getFloatProperty( "manaBurningThreshold" ) * (float) KoLCharacter.getMaximumMP());
		if ( shouldExecute && (starting < 0 || KoLCharacter.getCurrentMP() <= starting) )
			return null;

		int minimum = Math.max( 0, (int) (StaticEntity.getFloatProperty( "mpAutoRecovery" ) * (float) KoLCharacter.getMaximumMP()) ) + 1;
		minimum = Math.max( minimum, starting );

		if ( shouldExecute && KoLCharacter.getCurrentMP() <= minimum )
			return null;

		String skillName = null;
		int desiredDuration = 0;

		// Rather than maintain mood-related buffs only, maintain
		// any active effect that the character can auto-cast.  This
		// makes the feature useful even for people who have never
		// defined a mood.

		AdventureResult currentEffect;
		AdventureResult nextEffect;

		for ( int i = 0; i < activeEffects.size() && KoLmafia.permitsContinue(); ++i )
		{
			currentEffect = (AdventureResult) activeEffects.get(i);
			nextEffect = i + 1 >= activeEffects.size() ? null : (AdventureResult) activeEffects.get( i + 1 );

			skillName = UneffectRequest.effectToSkill( currentEffect.getName() );
			if ( !ClassSkillsDatabase.contains( skillName ) || !KoLCharacter.hasSkill( skillName ) )
				continue;

			// Only cast if a matching skill was found.  Limit cast count
			// to two in order to ensure that KoLmafia doesn't make the
			// buff counts too far out of balance.

			if ( nextEffect != null )
				desiredDuration = nextEffect.getCount() - currentEffect.getCount();

			int skillId = ClassSkillsDatabase.getSkillId( skillName );

			// Never recast ode when doing MP burning, because there's no
			// need for it to have a long duration.

			if ( skillId == 6014 )
				continue;

			if ( !StaticEntity.getBooleanProperty( "allowEncounterRateBurning" ) )
				if ( skillId == 1019 || skillId == 5017 || skillId == 6015 || skillId == 6016 )
					continue;

			int castCount = (KoLCharacter.getCurrentMP() - minimum) / ClassSkillsDatabase.getMPConsumptionById( skillId );
			int duration = ClassSkillsDatabase.getEffectDuration( skillId );

			// If the player opts in to allowing breakfast casting to burn
			// off excess MP, rather than using auto-restore, do so.

			if ( currentEffect.getCount() >= 10 )
			{
				String breakfast = considerBreakfastBurning( minimum, shouldExecute );
				if ( breakfast != null )
					return breakfast;

				castCount = (KoLCharacter.getCurrentMP() - minimum) / ClassSkillsDatabase.getMPConsumptionById( skillId );
			}

			if ( currentEffect.getCount() >= KoLCharacter.getAdventuresLeft() + 200 )
				return null;

			// If the player only wishes to cast buffs related to their
			// mood, then skip the buff if it's not in the player's moods.

			if ( !StaticEntity.getBooleanProperty( "allowNonMoodBurning" ) )
			{
				boolean shouldIgnore = true;

				for ( int j = 0; j < displayList.size(); ++j )
					shouldIgnore &= !currentEffect.equals( ((MoodTrigger)displayList.get(j)).effect );

				if ( shouldIgnore )
					continue;
			}

			if ( castCount > 2 && duration > desiredDuration )
				castCount = 2;
			else if ( duration * castCount > desiredDuration )
				castCount = Math.min( 3, castCount );

			if ( castCount > 0 )
				return "cast " + castCount + " " + skillName;
			else
				return considerBreakfastBurning( minimum, shouldExecute );
		}

		return considerBreakfastBurning( minimum, shouldExecute );
	}

	private static final String considerBreakfastBurning( int minimum, boolean shouldExecute )
	{
		if ( !StaticEntity.getBooleanProperty( "allowBreakfastBurning" ) )
			return null;

		for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
		{
			if ( !KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[i] ) )
				continue;
			if ( UseSkillRequest.BREAKFAST_SKILLS[i].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
				continue;
			if ( UseSkillRequest.BREAKFAST_SKILLS[i].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
				continue;

			UseSkillRequest skill = UseSkillRequest.getInstance( UseSkillRequest.BREAKFAST_SKILLS[i] );
			if ( skill.getMaximumCast() == 0 )
				continue;

			if ( shouldExecute )
				StaticEntity.getClient().getBreakfast( UseSkillRequest.BREAKFAST_SKILLS[i], false, minimum );
			else if ( ClassSkillsDatabase.getMPConsumptionById( skill.getSkillId() ) <= KoLCharacter.getCurrentMP() - minimum )
				return "cast 1 " + UseSkillRequest.BREAKFAST_SKILLS[i];
		}

		return null;
	}

	public static final void execute( int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
			return;

		if ( !willExecute( multiplicity ) )
			return;

		isExecuting = true;

		MoodTrigger current = null;

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		thiefTriggerLimit = KoLCharacter.hasEquipped( UseSkillRequest.PLEXI_PENDANT ) ? 4 : 3;

		// If you have too many accordion thief buffs to execute
		// your displayList, then shrug off your extra buffs, but
		// only if the user allows for this.

		// First we determine which buffs are already affecting the
		// character in question.

		ArrayList thiefBuffs = new ArrayList();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[i].getName() );
			if ( ClassSkillsDatabase.contains( skillName ) )
			{
				int skillId = ClassSkillsDatabase.getSkillId( skillName );
				if ( skillId > 6000 && skillId < 7000 )
					thiefBuffs.add( effects[i] );
			}
		}

		// Then, we determine the displayList which are thief skills, and
		// thereby would be cast at this time.

		ArrayList thiefSkills = new ArrayList();
		for ( int i = 0; i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.isThiefTrigger() )
				thiefSkills.add( current.effect );
		}

		// We then remove the displayList which will be used from the pool of
		// effects which could be removed.  Then we compute how many we
		// need to remove and remove them.

		thiefBuffs.removeAll( thiefSkills );

		int buffsToRemove = thiefBuffs.size() + thiefSkills.size() - thiefTriggerLimit;
		for ( int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i )
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "uneffect " + ((AdventureResult)thiefBuffs.get(i)).getName() );

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.

		for ( int i = 0; !KoLmafia.refusesContinue() && i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.skillId != -1 )
				current.execute( multiplicity );
		}

		for ( int i = 0; i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.skillId == -1 )
				current.execute( multiplicity );
		}

		isExecuting = false;
	}

	public static final boolean willExecute( int multiplicity )
	{
		if ( displayList.isEmpty() || StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
			return false;

		boolean willExecute = false;

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			willExecute |= current.shouldExecute( multiplicity );
		}

		return willExecute;
	}

	public static final ArrayList getMissingEffects()
	{
		ArrayList missing = new ArrayList();
		if ( displayList.isEmpty() )
			return missing;

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( current.getType().equals( "lose_effect" ) && !activeEffects.contains( current.effect ) )
				missing.add( current.effect );
		}

		return missing;
	}

	public static final void fixMaximumHealth( String restoreSetting )
	{
		String action;

		for ( int i = 0; i < AUTO_CLEAR.length; ++i )
		{
			if ( activeEffects.contains( AUTO_CLEAR[i] ) )
			{
				action = MoodSettings.getDefaultAction( "gain_effect", AUTO_CLEAR[i].getName() );

				if ( action.startsWith( "cast" ) && restoreSetting.indexOf( action.substring(5) ) != -1 )
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				else if ( action.startsWith( "use" ) && restoreSetting.indexOf( action.substring(4) ) != -1 )
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
				else if ( KoLCharacter.canInteract() )
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
			}
		}
	}

	public static final int getMaintenanceCost()
	{
		if ( displayList.isEmpty() )
			return 0;

		int runningTally = 0;

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( !current.getType().equals( "lose_effect" ) || !current.shouldExecute( 1 ) )
				continue;

			String action = current.getAction();
			if ( !action.startsWith( "cast" ) && !action.startsWith( "buff" ) )
				continue;

			int spaceIndex = action.indexOf( " " );
			if ( spaceIndex == -1 )
				continue;

			action = action.substring( spaceIndex + 1 );

			int multiplier = 1;

			if ( Character.isDigit( action.charAt(0) ) )
			{
				spaceIndex = action.indexOf( " " );
				multiplier = StaticEntity.parseInt( action.substring( 0, spaceIndex ) );
				action = action.substring( spaceIndex + 1 );
			}

			String skillName = KoLmafiaCLI.getSkillName( action );
			if ( skillName != null )
				runningTally += ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) * multiplier;
		}

		// Running tally calculated, return the amount of
		// MP required to sustain this mood.

		return runningTally;
	}

	/**
	 * Stores the settings maintained in this <code>MoodSettings</code>
	 * object to disk for later retrieval.
	 */

	public static final void saveSettings()
	{
		PrintStream writer = LogStream.openStream( settingsFile, true );

		SortedListModel triggerList;
		for ( int i = 0; i < availableMoods.size(); ++i )
		{
			triggerList = (SortedListModel) reference.get( availableMoods.get(i) );
			writer.println( "[ " + availableMoods.get(i) + " ]" );

			for ( int j = 0; j < triggerList.size(); ++j )
				writer.println( ((MoodTrigger)triggerList.get(j)).toSetting() );

			writer.println();
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 */

	public static final void loadSettings()
	{
		reference.clear();
		availableMoods.clear();

		ensureProperty( "default" );
		ensureProperty( "apathetic" );

		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !settingsFile.exists() )
			{
				settingsFile.createNewFile();
				return;
			}

			BufferedReader reader = KoLDatabase.getReader( settingsFile );

			String line;
			String currentKey = "";

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey = StaticEntity.globalStringDelete( line.substring( 1, line.length() - 1 ).trim().toLowerCase(), " " );

					if ( currentKey.equals( "clear" ) || currentKey.equals( "autofill" ) || currentKey.startsWith( "exec" ) || currentKey.startsWith( "repeat" ) )
						currentKey = "default";

					displayList.clear();

					if ( reference.containsKey( currentKey ) )
					{
						mappedList = (SortedListModel) reference.get( currentKey );
					}
					else
					{
						mappedList = new SortedListModel();
						reference.put( currentKey, mappedList );
						availableMoods.add( currentKey );
					}
				}
				else if ( line.length() != 0 )
				{
					addTrigger( MoodTrigger.constructNode( line ) );
				}
			}

			displayList.clear();

			reader.close();
			reader = null;

			setMood( StaticEntity.getProperty( "currentMood" ) );
		}
		catch ( IOException e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			StaticEntity.printStackTrace( e2 );
			settingsFile.delete();
			loadSettings();
		}
	}

	public static final String getDefaultAction( String type, String name )
	{
		if ( type == null || name == null )
			return "";

		// We can look at the displayList list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		String strictAction = "";

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( current.getType().equals( type ) && current.name.equals( name ) )
				strictAction = current.action;
		}

		if ( type.equals( "unconditional" ) )
			return strictAction;

		if ( type.equals( "lose_effect" ) )
		{
			if ( !strictAction.equals( "" ) )
				return strictAction;

			String action = StatusEffectDatabase.getDefaultAction( name );
			if ( action != null )
				return action;

			return strictAction;
		}

		if ( UneffectRequest.isShruggable( name ) )
			return "uneffect " + name;

		if ( name.indexOf( "Poisoned" ) != -1 )
			return "use anti-anti-antidote";

		if ( name.equals( "Beaten Up" ) )
		{
			if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
				return "cast Tongue of the Walrus";
			if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
				return "cast Tongue of the Otter";
			if ( KoLCharacter.hasItem( UneffectRequest.FOREST_TEARS ) )
				return "use 1 forest tears";
		}

		boolean powerNapClearable = name.equals( "Confused" ) || name.equals( "Cunctatitis" ) ||
			name.equals( "Embarrassed" ) || name.equals( "Easily Embarrassed" ) ||
			name.equals( "Prestidigysfunction" ) || name.equals( "Sleepy" ) ||
			name.equals( "Socialismydia" ) || name.equals( "Sunburned" ) ||
			name.equals( "Tenuous Grip on Reality" ) || name.equals( "Tetanus" ) ||
			name.equals( "Wussiness" );

		if ( powerNapClearable && KoLCharacter.hasSkill( "Disco Power Nap" ) )
			return "cast Disco Power Nap";

		boolean discoNapClearable = name.equals( "Wussiness" ) || name.equals( "Sleepy" ) ||
			name.equals( "Embarrassed" ) || name.equals( "Confused" );

		if ( discoNapClearable && KoLCharacter.hasSkill( "Disco Nap" ) )
			return "cast Disco Nap";

		if ( KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
			return "uneffect " + name;

		boolean tinyHouseClearable = name.equals( "Beaten Up" ) || name.equals( "Confused" ) ||
			name.equals( "Sunburned" ) || name.equals( "Wussiness" );

		if ( tinyHouseClearable && (KoLCharacter.canInteract() || KoLCharacter.hasItem( UneffectRequest.TINY_HOUSE )) )
			return "use 1 tiny house";

		if ( KoLCharacter.canInteract() )
			return "uneffect " + name;

		return strictAction;
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private static final void ensureProperty( String key )
	{
		if ( !reference.containsKey( key ) )
		{
			SortedListModel defaultList = new SortedListModel();
			reference.put( key, defaultList );
			availableMoods.add( key );
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	settingsFile	The file to which the settings will be stored.
	 */

	public static class MoodTrigger implements Comparable
	{
		private int skillId = -1;
		private AdventureResult effect;
		private boolean isThiefTrigger = false;

		private StringBuffer stringForm;

		private String action;
		private String type, name;

		private int count;
		private AdventureResult item;
		private UseSkillRequest skill;

		public MoodTrigger( String type, AdventureResult effect, String action )
		{
			this.type = type;
			this.effect = effect;
			this.name = effect == null ? null : effect.getName();

			if ( action.startsWith( "use " ) || action.startsWith( "cast " ) )
			{
				// Determine the command, the count amount,
				// and the parameter's unambiguous form.

				int spaceIndex = action.indexOf( " " );
				String parameters = KoLDatabase.getDisplayName( action.substring( spaceIndex + 1 ).trim() );

				if ( action.startsWith( "use" ) )
				{
					this.item = KoLmafiaCLI.getFirstMatchingItem( parameters );
					this.count = this.item.getCount();
					this.action = "use " + this.count + " " + this.item.getName();
				}
				else
				{
					this.count = 1;

					if ( Character.isDigit( parameters.charAt(0) ) )
					{
						spaceIndex = parameters.indexOf( " " );
						this.count = StaticEntity.parseInt( parameters.substring( 0, spaceIndex ) );
						parameters = parameters.substring( spaceIndex ).trim();
					}

					if ( !ClassSkillsDatabase.contains( parameters ) )
						parameters = KoLmafiaCLI.getSkillName( parameters );

					this.skill = UseSkillRequest.getInstance( parameters );
					this.action = "cast " + this.count + " " + this.skill.getSkillName();
				}
			}
			else
			{
				this.action = action;
			}

			if ( type != null && type.equals( "lose_effect" ) && effect != null )
			{
				String skillName = UneffectRequest.effectToSkill( effect.getName() );
				if ( ClassSkillsDatabase.contains( skillName ) )
				{
					this.skillId = ClassSkillsDatabase.getSkillId( skillName );
					this.isThiefTrigger = this.skillId > 6000 && this.skillId < 7000;
				}
			}

			this.stringForm = new StringBuffer();
			this.updateStringForm();
		}

		public String getType()
		{	return this.type;
		}

		public String getName()
		{	return this.name;
		}

		public String getAction()
		{	return this.action;
		}

		public String toString()
		{	return this.stringForm.toString();
		}

		public String toSetting()
		{
			if ( this.effect == null )
				return this.type + " => " + this.action;

			if ( this.item != null )
			{
				return this.type + " " + KoLDatabase.getCanonicalName( this.name ) +
					" => use " + this.count + " " + KoLDatabase.getCanonicalName( this.item.getName() );
			}

			if ( this.skill != null )
			{
				return this.type + " " + KoLDatabase.getCanonicalName( this.name ) +
					" => cast " + this.count + " " + KoLDatabase.getCanonicalName( this.skill.getSkillName() );
			}

			return this.type + " " + KoLDatabase.getCanonicalName( this.name ) + " => " + this.action;
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof MoodTrigger) )
				return false;

			MoodTrigger mt = (MoodTrigger) o;
			if ( !this.type.equals( mt.getType() ) )
				return false;

			if ( this.name == null )
				return mt.name == null;

			if ( mt.getType() == null )
				return false;

			return this.name.equals( mt.name );
		}

		public void execute( int multiplicity )
		{
			if ( !this.shouldExecute( multiplicity ) )
				return;

			if ( item != null )
			{
				RequestThread.postRequest( new ConsumeItemRequest( item.getInstance( Math.max( this.count, this.count * multiplicity ) ) ) );
				return;
			}
			else if ( skill != null )
			{
				skill.setBuffCount( Math.max( this.count, this.count * multiplicity ) );
				RequestThread.postRequest( skill );
				return;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.action );
		}

		public boolean shouldExecute( int multiplicity )
		{
			if ( KoLmafia.refusesContinue() )
				return false;

			if ( this.type.equals( "lose_effect" ) && multiplicity > 0 )
				return true;

			boolean shouldExecute = false;

			if ( this.effect == null )
			{
				shouldExecute = true;
			}
			else if ( this.type.equals( "gain_effect" ) )
			{
				shouldExecute = activeEffects.contains( this.effect );
			}
			else if ( this.type.equals( "lose_effect" ) )
			{
				boolean unstackable = this.action.indexOf( "cupcake" ) != -1 || this.action.indexOf( "snowcone" ) != -1 ||
					this.action.indexOf( "mushroom" ) != -1 || this.action.indexOf( "oasis" ) != -1;

				shouldExecute = unstackable ? !activeEffects.contains( this.effect ) :
					this.effect.getCount( activeEffects ) <= (multiplicity == -1 ? 1 : 5);
			}

			return shouldExecute;
		}

		public boolean isThiefTrigger()
		{	return this.isThiefTrigger;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof MoodTrigger) )
				return -1;

			String othertype = ((MoodTrigger)o).getType();
			String othername = ((MoodTrigger)o).name;
			String otherTriggerAction = ((MoodTrigger)o).action;

			int compareResult = 0;

			if ( this.type.equals( "unconditional" ) )
			{
				if ( othertype.equals( "unconditional" ) )
					compareResult = this.action.compareToIgnoreCase( otherTriggerAction );
				else
					compareResult = -1;
			}
			else if ( this.type.equals( "gain_effect" ) )
			{
				if ( othertype.equals( "unconditional" ) )
					compareResult = 1;
				else if ( othertype.equals( "gain_effect" ) )
					compareResult = this.name.compareToIgnoreCase( othername );
				else
					compareResult = -1;
			}
			else if ( this.type.equals( "lose_effect" ) )
			{
				if ( othertype.equals( "lose_effect" ) )
					compareResult = this.name.compareToIgnoreCase( othername );
				else
					compareResult = 1;
			}

			return compareResult;
		}

		public void updateStringForm()
		{
			this.stringForm.setLength(0);

			if ( this.type.equals( "gain_effect" ) )
				this.stringForm.append( "When I get" );
			else if ( this.type.equals( "lose_effect" ) )
				this.stringForm.append( "When I run low on" );
			else
				this.stringForm.append( "Always" );

			if ( this.name != null )
			{
				this.stringForm.append( " " );
				this.stringForm.append( this.name );
			}

			this.stringForm.append( ", " );
			this.stringForm.append( this.action );
		}

		public static final MoodTrigger constructNode( String line )
		{
			String [] pieces = line.split( " => " );
			if ( pieces.length != 2 )
				return null;

			String type = null;

			if ( pieces[0].startsWith( "gain_effect" ) )
				type = "gain_effect";
			else if ( pieces[0].startsWith( "lose_effect" ) )
				type = "lose_effect";
			else if ( pieces[0].startsWith( "unconditional" ) )
				type = "unconditional";

			if ( type == null )
				return null;

			String name = type.equals( "unconditional" ) ? null :
				pieces[0].substring( pieces[0].indexOf( " " ) ).trim();

			AdventureResult effect = null;
			if ( !type.equals( "unconditional" ) )
			{
				effect = KoLmafiaCLI.getFirstMatchingEffect( name );
				if ( effect == null )
					return null;
			}

			return new MoodTrigger( type, effect, pieces[1].trim() );
		}
	}
}
