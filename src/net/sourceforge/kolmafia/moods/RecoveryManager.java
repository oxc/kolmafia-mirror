/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.io.File;

import java.lang.reflect.Method;
import java.lang.NoSuchMethodException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.HPRestoreItemList.HPRestoreItem;
import net.sourceforge.kolmafia.moods.MPRestoreItemList.MPRestoreItem;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LightsOutManager;
import net.sourceforge.kolmafia.session.VoteMonsterManager;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.parsetree.Value;

public class RecoveryManager
{
	private static boolean recoveryActive;

	public static boolean isRecoveryActive()
	{
		return RecoveryManager.recoveryActive;
	}

	public static void setRecoveryActive( final boolean recoveryActive )
	{
		RecoveryManager.recoveryActive = recoveryActive;
	}

	public static boolean isRecoveryPossible()
	{
		return	!RecoveryManager.isRecoveryActive() &&
			FightRequest.currentRound == 0 &&
			!FightRequest.inMultiFight && !FightRequest.choiceFollowsFight &&
			( !ChoiceManager.handlingChoice || ChoiceManager.canWalkAway() ) &&
			!CharPaneRequest.inValhalla() && KoLCharacter.getLimitmode() == null;
	}

	public static boolean runThresholdChecks()
	{
		float autoStopValue = Preferences.getFloat( "autoAbortThreshold" );
		if ( autoStopValue >= 0.0f )
		{
			autoStopValue *= KoLCharacter.getMaximumHP();
			if ( KoLCharacter.getCurrentHP() <= autoStopValue )
			{
				KoLmafia.updateDisplay(
					MafiaState.ABORT, "Health fell below " + (int) autoStopValue + ". Auto-abort triggered." );
				return false;
			}
		}

		return true;
	}

	public static void runBetweenBattleChecks( final boolean isFullCheck )
	{
		RecoveryManager.runBetweenBattleChecks( isFullCheck, isFullCheck, true, isFullCheck );
	}

	public static void runBetweenBattleChecks( final boolean isScriptCheck, final boolean isMoodCheck,
		final boolean isHealthCheck, final boolean isManaCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( !RecoveryManager.isRecoveryPossible() || KoLmafia.refusesContinue() )
		{
			return;
		}

		// First, run the between battle script defined by the
		// user, which may obviate the built in behavior.

		RecoveryManager.recoveryActive = true;

		if ( isScriptCheck )
		{
			KoLmafia.executeScript( Preferences.getString( "betweenBattleScript" ) );
		}

		// Now, run the built-in behavior to take care of any loose ends.

		Checkpoint checkpoint = new Checkpoint();
		try
		{
			if ( isMoodCheck )
			{
				MoodManager.execute();
			}

			if ( isHealthCheck )
			{
				RecoveryManager.recoverHP();
			}

			if ( isMoodCheck )
			{
				ManaBurnManager.burnExtraMana( false );
			}

			if ( isManaCheck )
			{
				RecoveryManager.recoverMP();
			}
		}
		finally
		{
			checkpoint.restore();
		}

		if ( KoLmafia.permitsContinue() && KoLCharacter.getCurrentHP() == 0 && !FightRequest.edFightInProgress() )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Insufficient health to continue (auto-abort triggered)." );
		}

		if ( KoLmafia.permitsContinue() && KoLmafia.currentIterationString.length() > 0 )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( KoLmafia.currentIterationString );
			KoLmafia.currentIterationString = "";
		}

		LightsOutManager.checkCounter();

		VoteMonsterManager.checkCounter();

		FightRequest.haveFought(); // reset flag

		RecoveryManager.recoveryActive = false;
	}

	private static Method getKoLCharacterMethod( String name )
	{
		try
		{
			return KoLCharacter.class.getMethod( name, new Class[ 0 ] );
		}
		catch ( NoSuchMethodException e )
		{
			System.out.println( "Cannot find method KoLCharacter." + name + "()" );
			return null;
		}
	}

	/**
	 * Utility. The method called in between battles. This method checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public static boolean recoverHP()
	{
		return RecoveryManager.recoverHP( 0 );
	}

	public static boolean checkpointedRecoverHP( final long recover )
	{
		Checkpoint checkpoint = new Checkpoint();
		try
		{
			return RecoveryManager.recoverHP( recover );
		}
		finally
		{
			checkpoint.restore();
		}
	}

	private static final Method currentHPMethod = RecoveryManager.getKoLCharacterMethod( "getCurrentHP" );
	private static final Method maximumHPMethod = RecoveryManager.getKoLCharacterMethod( "getMaximumHP" );

	public static boolean recoverHP( final long hpNeeded )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		try
		{
			if ( Preferences.getBoolean( "removeMalignantEffects" ) )
			{
				MoodManager.removeMalignantEffects();
			}

			HPRestoreItemList.updateHealthRestored();
			if ( RecoveryManager.invokeRecoveryScript( "HP", hpNeeded ) )
			{
				return true;
			}

			String allowed;
			HPRestoreItem[] items;

			if ( KoLCharacter.isPlumber() )
			{
				allowed = "super deluxe mushroom;deluxe mushroom;mushroom";
				items = HPRestoreItemList.PLUMBER_CONFIGURES;
			}
			else
			{
				allowed = Preferences.getString( "hpAutoRecoveryItems" );
				items = HPRestoreItemList.CONFIGURES;
			}

			return RecoveryManager.recover( hpNeeded, "hpAutoRecovery", allowed, currentHPMethod, maximumHPMethod, items );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method called in between commands. This method checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public static boolean recoverMP()
	{
		return RecoveryManager.recoverMP( 0 );
	}

	/**
	 * Utility. The method which restores the character's current mana points above the given value.
	 */

	public static boolean checkpointedRecoverMP( final long recover )
	{
		Checkpoint checkpoint = new Checkpoint();
		try
		{
			return RecoveryManager.recoverMP( recover );
		}
		finally
		{
			checkpoint.restore();
		}
	}

	private static final Method currentMPMethod = RecoveryManager.getKoLCharacterMethod( "getCurrentMP" );
	private static final Method maximumMPMethod = RecoveryManager.getKoLCharacterMethod( "getMaximumMP" );

	public static boolean recoverMP( final long mpNeeded )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		try
		{
			MPRestoreItemList.updateManaRestored();
			if ( RecoveryManager.invokeRecoveryScript( "MP", mpNeeded ) )
			{
				return true;
			}

			String allowed = Preferences.getString( "mpAutoRecoveryItems" );
			return RecoveryManager.recover( mpNeeded, "mpAutoRecovery", allowed, currentMPMethod, maximumMPMethod, MPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method which ensures that the amount needed exists, and if not, calls the appropriate scripts to do
	 * so.
	 */

	private static boolean recover( float desired, float setting, float target, boolean isNonCombatHealthRestore,
					final Method currentMethod, final Method maximumMethod,
					Set<String> usableTechniques, final RestoreItem[] techniques )
		throws Exception
	{
		// See if any restoration needs to take place
		if ( setting < 0.0f && desired == 0.0f )
		{
			return true;
		}

		Object[] empty = new Object[ 0 ];
		int current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

		// If you've already reached the desired value, don't restore.
		if ( desired != 0 && current >= desired )
		{
			return true;
		}

		int maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
		int needed = (int) Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		// Check against restore target to see how far you need to go.
		desired = Math.min( maximum, Math.max( desired, target * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
		{
			desired = maximum;
		}

		if ( isNonCombatHealthRestore )
		{
			needed = 1;
			desired = 1;
		}

		if ( current >= needed )
		{
			return true;
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items. This involves a few extra
		// reflection methods.

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory intensive, but it allows for a lot
		// more flexibility.

		ArrayList<RestoreItem> possibleItems = new ArrayList<>();
		ArrayList<RestoreItem> possibleSkills = new ArrayList<>();

		for ( RestoreItem technique : techniques )
		{
			String currentTechniqueName = technique.toString().toLowerCase();
			if ( !usableTechniques.contains( currentTechniqueName ) )
			{
				continue;
			}

			if ( technique.isSkill() )
			{
				possibleSkills.add( technique );
			}
			else if ( technique.usableInCurrentPath() )
			{
				possibleItems.add( technique );
			}
		}

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// First, use any available skills. 

		if ( !possibleSkills.isEmpty() )
		{
			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
			int last = -1;

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					RestoreItem skill = possibleSkills.get( indexToTry );
					skill.recover( (int) desired, false );

					last = current;
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );

					if ( last >= current )
					{
						++indexToTry;
					}
				}
				while ( indexToTry < possibleSkills.size() && current < needed );
			}

			if ( KoLmafia.refusesContinue() )
			{
				return false;
			}
		}

		// Iterate through every restore item which is already available
		// in the player's inventory.

		Collections.sort( possibleItems );

		for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
		{
			int last = -1;
			do
			{
				RestoreItem item = possibleItems.get( i );
				item.recover( (int) desired, false );

				last = current;
				current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
				maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// If we get here, we still need healing. For areas that are
		// all noncombats, then you can heal using only unguent.

		if ( isNonCombatHealthRestore && KoLCharacter.getAvailableMeat() >= 30 )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.PUNGENT_UNGUENT ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		try
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			int last = -1;
			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

			// Plumbers purchase things with coins.
			if ( KoLCharacter.isPlumber() )
			{
				while ( last != current && current < needed )
				{
					Collections.sort( possibleItems );

					RestoreItem item = possibleItems.get( 0 );
					item.recover( (int) desired, true );

					last = current;
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
					desired = Math.min( maximum, desired );
				}
			}
			else if ( !possibleItems.isEmpty() )
			{
				while ( last != current && current < needed )
				{
					int indexToTry = 0;
					Collections.sort( possibleItems );

					do
					{
						RestoreItem item = possibleItems.get( indexToTry );
						item.recover( (int) desired, true );

						last = current;
						current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
						maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
						desired = Math.min( maximum, desired );

						if ( last >= current )
						{
							++indexToTry;
						}
					}
					while ( indexToTry < possibleItems.size() && current < needed );
				}
			}
			else if ( current < needed )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You ran out of restores." );
				return false;
			}
		}
		finally
		{
			HPRestoreItemList.setPurchaseBasedSort( false );
			MPRestoreItemList.setPurchaseBasedSort( false );
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( current < needed )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Autorecovery failed." );
			return false;
		}

		return true;
	}

	private static boolean recover( float desired, final String settingName, String allowed,
					final Method currentMethod, final Method maximumMethod,
					final RestoreItem[] techniques )
	{
		try
		{
			// Look up some settings
			float setting = Preferences.getFloat( settingName );
			float target = Preferences.getFloat( settingName + "Target" );

			// Special handling for hp restoration in adventure
			// locations with no combats: as long as your health is
			// above zero, you're okay.

			boolean isNonCombatHealthRestore =
				settingName.startsWith( "hp" ) &&
				KoLmafia.isAdventuring() &&
				KoLmafia.currentAdventure.isNonCombatsOnly();

			// Make a set of allowed restoration techniques
			Set<String> usableTechniques = new HashSet<>( Arrays.asList( allowed.trim().toLowerCase().split( "\\s*;\\s*" ) ) ); 

			return RecoveryManager.recover( desired, setting, target, isNonCombatHealthRestore,
							currentMethod, maximumMethod,
							usableTechniques, techniques );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Returns the total number of mana restores currently available to the player.
	 */

	public static int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = Preferences.getString( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 )
			{
				AdventureResult item = MPRestoreItemList.CONFIGURES[ i ].getItem();
				if ( item != null )
				{
					restoreCount += item.getCount( KoLConstants.inventory );
				}
			}
		}

		return restoreCount;
	}

	private static boolean invokeRecoveryScript( final String type, final long needed )
	{
		String scriptName = Preferences.getString( "recoveryScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}

		List<File> scriptFiles = KoLmafiaCLI.findScriptFile( scriptName );
		Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFiles );
		if ( interpreter != null )
		{
			File scriptFile = scriptFiles.get( 0 );
			KoLmafiaASH.logScriptExecution( "Starting recovery script: ", scriptFile.getName(), interpreter );
			Value v = interpreter.execute( "main", new String[]
			{
				type,
				String.valueOf( needed )
			} );
			KoLmafiaASH.logScriptExecution( "Finished recovery script: ", scriptFile.getName(), interpreter );
			return v != null && v.intValue() != 0;
		}
		return false;
	}

}
