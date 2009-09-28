
package net.sourceforge.kolmafia.session;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.HPRestoreItemList;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MPRestoreItemList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.HPRestoreItemList.HPRestoreItem;
import net.sourceforge.kolmafia.MPRestoreItemList.MPRestoreItem;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class RecoveryManager
{
	private static boolean recoveryActive;

	public static boolean isRecoveryActive()
	{
		return RecoveryManager.recoveryActive || MoodManager.isExecuting();
	}

	public static void setRecoveryActive( final boolean recoveryActive )
	{
		RecoveryManager.recoveryActive = recoveryActive;
	}

	public static boolean isRecoveryPossible()
	{
		return RecoveryManager.isRecoveryActive() && MoodManager.isExecuting() && FightRequest.getCurrentRound() == 0;
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
					KoLConstants.ABORT_STATE, "Health fell below " + (int) autoStopValue + ". Auto-abort triggered." );
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
		// user, which may make it so that none of the built
		// in behavior needs to run.

		RequestThread.openRequestSequence();
		RecoveryManager.recoveryActive = true;

		if ( isScriptCheck )
		{
			String scriptPath = Preferences.getString( "betweenBattleScript" );
			if ( !scriptPath.equals( "" ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptPath );
			}
		}

		SpecialOutfit.createImplicitCheckpoint();

		// Now, run the built-in behavior to take care of
		// any loose ends.

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
			MoodManager.burnExtraMana( false );
		}

		if ( isManaCheck )
		{
			RecoveryManager.recoverMP();
		}

		RecoveryManager.recoveryActive = false;
		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		if ( KoLCharacter.getCurrentHP() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );
		}

		if ( KoLmafia.permitsContinue() && KoLmafia.currentIterationString.length() > 0 )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( KoLmafia.currentIterationString );
			KoLmafia.currentIterationString = "";
		}

		FightRequest.haveFought(); // reset flag
	}

	/**
	 * Utility. The method called in between battles. This method checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public static boolean recoverHP()
	{
		return RecoveryManager.recoverHP( 0 );
	}

	public static boolean recoverHP( final int recover )
	{
		try
		{
			if ( Preferences.getBoolean( "removeMalignantEffects" ) )
			{
				MoodManager.removeMalignantEffects();
			}

			HPRestoreItemList.updateHealthRestored();
			if ( RecoveryManager.invokeRecoveryScript( "HP", recover ) )
			{
				return true;
			}
			return RecoveryManager.recover(
				recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
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

	public static boolean recoverMP( final int mpNeeded )
	{
		try
		{
			MPRestoreItemList.updateManaRestored();
			if ( RecoveryManager.invokeRecoveryScript( "MP", mpNeeded ) )
			{
				return true;
			}
			return RecoveryManager.recover(
				mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
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

	private static boolean recover( float desired, final String settingName, final String currentName,
		final String maximumName, final Object[] techniques )
		throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option. This takes precedence over all other checks.

		String restoreSetting = Preferences.getString( settingName + "Items" ).trim().toLowerCase();

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object[] empty = new Object[ 0 ];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[ 0 ] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[ 0 ] );

		float setting = Preferences.getFloat( settingName );

		if ( setting < 0.0f && desired == 0 )
		{
			return true;
		}

		int current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
		{
			return true;
		}

		int maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
		int needed = (int) Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		if ( current >= needed )
		{
			return true;
		}

		// Next, check against the restore target to see how
		// far you need to go.

		setting = Preferences.getFloat( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, setting * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
		{
			desired = maximum;
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items. This involves a few extra
		// reflection methods.

		String currentTechniqueName;

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory intensive, but it allows for a lot
		// more flexibility.

		ArrayList possibleItems = new ArrayList();
		ArrayList possibleSkills = new ArrayList();

		for ( int i = 0; i < techniques.length; ++i )
		{
			currentTechniqueName = techniques[ i ].toString().toLowerCase();
			if ( restoreSetting.indexOf( currentTechniqueName ) == -1 )
			{
				continue;
			}

			if ( techniques[ i ] instanceof HPRestoreItem )
			{
				if ( ( (HPRestoreItem) techniques[ i ] ).isSkill() )
				{
					possibleSkills.add( techniques[ i ] );
				}
				else
				{
					possibleItems.add( techniques[ i ] );
				}
			}

			if ( techniques[ i ] instanceof MPRestoreItem )
			{
				if ( ( (MPRestoreItem) techniques[ i ] ).isSkill() )
				{
					possibleSkills.add( techniques[ i ] );
				}
				else
				{
					possibleItems.add( techniques[ i ] );
				}
			}
		}

		int last = -1;

		// Special handling of the Hidden Temple. Here, as
		// long as your health is above zero, you're okay.

		boolean isNonCombatHealthRestore =
			settingName.startsWith( "hp" ) && KoLmafia.isAdventuring && KoLmafia.currentAdventure.isNonCombatsOnly();

		if ( isNonCombatHealthRestore )
		{
			if ( KoLCharacter.getCurrentHP() > 0 )
			{
				return true;
			}

			needed = 1;
			desired = 1;
		}

		// Consider clearing beaten up if your restoration settings
		// include the appropriate items.

		if ( current >= needed )
		{
			return true;
		}

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// Next, use any available skills. This only applies to health
		// restoration, since no MP-using skill restores MP.

		if ( !possibleSkills.isEmpty() )
		{
			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					last = current;
					currentTechniqueName = possibleSkills.get( indexToTry ).toString().toLowerCase();

					RecoveryManager.recoverOnce(
						possibleSkills.get( indexToTry ), currentTechniqueName, (int) desired, false );
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
			do
			{
				last = current;
				currentTechniqueName = possibleItems.get( i ).toString().toLowerCase();

				RecoveryManager.recoverOnce( possibleItems.get( i ), currentTechniqueName, (int) desired, false );
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

		// For areas that are all noncombats, then you can go ahead
		// and heal using only unguent.

		if ( isNonCombatHealthRestore && KoLCharacter.getAvailableMeat() >= 30 )
		{
			RequestThread.postRequest( new UseItemRequest( new AdventureResult( 231, 1 ) ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		if ( !possibleItems.isEmpty() )
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
			last = -1;

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleItems );

				do
				{
					last = current;
					currentTechniqueName = possibleItems.get( indexToTry ).toString().toLowerCase();

					RecoveryManager.recoverOnce(
						possibleItems.get( indexToTry ), currentTechniqueName, (int) desired, true );
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

			HPRestoreItemList.setPurchaseBasedSort( false );
			MPRestoreItemList.setPurchaseBasedSort( false );
		}
		else if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You ran out of restores." );
			return false;
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Autorecovery failed." );
			return false;
		}

		return true;
	}

	/**
	 * Utility. The method which uses the given recovery technique (not specified in a script) in order to restore.
	 */

	private static void recoverOnce( final Object technique, final String techniqueName, final int needed,
		final boolean purchase )
	{
		// If the technique is an item, and the item is not readily
		// available, then don't bother with this item -- however, if
		// it is the only item present, then rethink it.

		if ( technique instanceof HPRestoreItem )
		{
			( (HPRestoreItem) technique ).recoverHP( needed, purchase );
		}

		if ( technique instanceof MPRestoreItem )
		{
			( (MPRestoreItem) technique ).recoverMP( needed, purchase );
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

	private static boolean invokeRecoveryScript( final String type, final int needed )
	{
		String scriptName = Preferences.getString( "recoveryScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}
		Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			Value v = interpreter.execute( "main", new String[]
			{
				type,
				String.valueOf( needed )
			} );
			return v != null && v.intValue() != 0;
		}
		return false;
	}

}
