/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.net.URLEncoder;
import java.net.URLDecoder;

import java.awt.Component;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;

import java.util.Arrays;
import java.math.BigInteger;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public abstract class KoLmafia implements KoLConstants
{
	static
	{
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		System.setProperty( "http.referer", "www.kingdomofloathing.com" );

		CombatSettings.reset();
		MoodSettings.reset();
	}

	protected static PrintStream sessionStream = NullStream.INSTANCE;
	protected static PrintStream debugStream = NullStream.INSTANCE;
	protected static PrintStream outputStream = NullStream.INSTANCE;
	protected static PrintStream mirrorStream = NullStream.INSTANCE;
	protected static PrintStream echoStream = NullStream.INSTANCE;

	private static boolean isEnabled = true;
	private static boolean hadPendingState = false;

	private static final String [] OVERRIDE_DATA =
	{
		"adventures.dat", "buffbots.dat", "classskills.dat", "combats.dat", "concoctions.dat",
		"equipment.dat", "familiars.dat", "itemdescs.dat", "monsters.dat", "npcstores.dat",
		"outfits.dat", "packages.dat", "plurals.dat", "statuseffects.dat", "tradeitems.dat", "zonelist.dat"
	};

	public static final int SNOWCONE = 0;
	public static final int HILARIOUS = 1;
	public static final int SAUCECRAFTING = 2;
	public static final int PASTAMASTERY = 3;
	public static final int COCKTAILCRAFTING = 4;

	public static String [][] BREAKFAST_SKILLS =
	{
		{ "Summon Snowcone", "1" },
		{ "Summon Hilarious Objects", "1" },
		{ "Advanced Saucecrafting", "3" },
		{ "Pastamastery", "3" },
		{ "Advanced Cocktailcrafting", "3" }
	};

	private static boolean recoveryActive = false;
	private static String currentIterationString = "";

	protected static boolean isMakingRequest = false;
	protected static KoLRequest currentRequest = null;
	protected static int continuationState = CONTINUE_STATE;

	protected static int [] initialStats = new int[3];
	protected static int [] fullStatGain = new int[3];

	protected static boolean executedLogin = false;
	protected static boolean useDisjunction = false;

	private static final Pattern FUMBLE_PATTERN = Pattern.compile( "You drop .*? ([\\d,]+) damage" );
	private static final Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static final Pattern TAVERN_PATTERN = Pattern.compile( "where=(\\d+)" );
	private static final Pattern GOURD_PATTERN = Pattern.compile( "Bring back (\\d+)" );

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafiaGUI</code>.
	 */

	public static void main( String [] args )
	{
		boolean useGUI = true;
		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equals( "--CLI" ) )
				useGUI = false;
			if ( args[i].equals( "--GUI" ) )
				useGUI = true;
		}

		hermitItems.add( "banjo strings" );
		hermitItems.add( "catsup" );
		hermitItems.add( "dingy planks" );
		hermitItems.add( "fortune cookie" );
		hermitItems.add( "golden twig" );
		hermitItems.add( "hot buttered roll" );
		hermitItems.add( "jaba\u00f1ero pepper" );
		hermitItems.add( "ketchup" );
		hermitItems.add( "sweet rims" );
		hermitItems.add( "volleyball" );
		hermitItems.add( "wooden figurine" );

		StaticEntity.reloadSettings( "" );

		if ( saveStateNames.isEmpty() )
		{
			String [] currentNames = StaticEntity.getProperty( "saveState" ).split( "//" );
			for ( int i = 0; i < currentNames.length; ++i )
				saveStateNames.add( currentNames[i] );

			// This line is added to clear out data from previous
			// releases of KoLmafia - the extra disk access does
			// affect performance, but not significantly.

			storeSaveStates();
		}

		// Also clear out any outdated data files.  Include the
		// adventure table, in case this is causing problems.

		String version = StaticEntity.getProperty( "previousUpdateVersion" );

		if ( version == null || !version.equals( VERSION_NAME ) )
		{
			StaticEntity.setProperty( "previousUpdateVersion", VERSION_NAME );
			for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
			{
				File outdated = new File( "data/" + OVERRIDE_DATA[i] );
				if ( outdated.exists() )
					outdated.delete();

				deleteSimulator( new File( "html/simulator" ) );
			}
		}

		if ( useGUI )
			KoLmafiaGUI.main( args );
		else
			KoLmafiaCLI.main( args );

		// All that completed, check to see if there is an auto-login
		// which should occur.

		String autoLogin = StaticEntity.getProperty( "autoLogin" );
		if ( !autoLogin.equals( "" ) )
		{
			// Make sure that a password was stored for this
			// character (would fail otherwise):

			String password = StaticEntity.getClient().getSaveState( autoLogin );
			if ( password != null && !password.equals( "" ) )
				(new RequestThread( new LoginRequest( StaticEntity.getClient(), autoLogin, password ) )).start();
		}
	}

	private static void deleteSimulator( File location )
	{
		if ( location.isDirectory() )
		{
			File [] files = location.listFiles();
			for ( int i = 0; i < files.length; ++i )
				deleteSimulator( files[i] );
		}

		location.delete();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafia()
	{	useDisjunction = false;
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public static final void updateDisplay( String message )
	{	updateDisplay( CONTINUE_STATE, message );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public static final void updateDisplay( int state, String message )
	{
		if ( continuationState == ABORT_STATE && message.equals( "" ) )
			return;

		continuationState = state;
		KoLmafiaCLI.printLine( state, message );

		if ( !existingFrames.isEmpty() && message.indexOf( LINE_BREAK ) == -1 )
			updateDisplayState( state, message );
	}

	private static final void updateDisplayState( int state, String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		WeakReference [] references = new WeakReference[ existingPanels.size() ];
		existingPanels.toArray( references );

		for ( int i = 0; i < references.length; ++i )
		{
			if ( references[i].get() != null )
			{
				if ( references[i].get() instanceof KoLPanel && message != null )
					((KoLPanel) references[i].get()).setStatusMessage( state, message );

				((Component)references[i].get()).setEnabled( state != CONTINUE_STATE );
			}
		}

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].updateDisplayState( state );

		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().updateDisplayState( state );

		isEnabled = (state == ERROR_STATE || state == ENABLE_STATE);
	}

	public static void enableDisplay()
	{
		if ( isEnabled )
			return;

		updateDisplayState( continuationState == ABORT_STATE || continuationState == ERROR_STATE ? ERROR_STATE : ENABLE_STATE, "" );
	}

	public static boolean executedLogin()
	{	return executedLogin;
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username, boolean getBreakfast, boolean isQuickLogin )
	{
		// Initialize the variables to their initial
		// states to avoid null pointers getting thrown
		// all over the place

		executedLogin = true;
		KoLmafia.forceContinue();

		if ( isQuickLogin )
		{
			(new AccountRequest( this )).run();
			(new CharsheetRequest( this )).run();
			CharpaneRequest.getInstance().run();
			return;
		}

		StaticEntity.setProperty( "lastUsername", username );
		StaticEntity.reloadSettings( username );

		KoLCharacter.reset( username );
		KoLmafia.openSessionStream();

		refreshSession();
		resetSession();

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
			return;

		registerPlayer( username, String.valueOf( KoLCharacter.getUserID() ) );

		if ( getBreakfast )
		{
			String today = DATED_FILENAME_FORMAT.format( new Date() );
			String lastBreakfast = StaticEntity.getProperty( "lastBreakfast" );
			StaticEntity.setProperty( "lastBreakfast", today );

			if ( lastBreakfast == null || !lastBreakfast.equals( today ) )
				getBreakfast( true );
		}

		// A breakfast script might include loading an adventure
		// area, so now go ahead and load the adventure table.

		String scriptSetting = StaticEntity.getGlobalProperty( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
			DEFAULT_SHELL.executeLine( scriptSetting );
	}

	public void resetBreakfastSummonings()
	{
		setBreakfastSummonings( SNOWCONE, 1 );
		setBreakfastSummonings( HILARIOUS, 1 );
		setBreakfastSummonings( SAUCECRAFTING, 3 );
		setBreakfastSummonings( PASTAMASTERY, 3 );
		setBreakfastSummonings( COCKTAILCRAFTING, 3 );
	}

	public void setBreakfastSummonings( int index, int count )
	{	BREAKFAST_SKILLS[index][1] = String.valueOf( count );
	}

	public void getBreakfast( boolean checkSettings )
	{
		if ( KoLCharacter.hasToaster() )
			for ( int i = 0; i < 3 && permitsContinue(); ++i )
				(new CampgroundRequest( this, "toast" )).run();

		if ( KoLCharacter.hasArches() )
			(new CampgroundRequest( this, "arches" )).run();

		boolean shouldCast = false;
		String skillSetting = StaticEntity.getProperty( "breakfast" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") );

		if ( skillSetting != null )
		{
			for ( int i = 0; i < BREAKFAST_SKILLS.length; ++i )
			{
				shouldCast = !checkSettings || skillSetting.indexOf( BREAKFAST_SKILLS[i][0] ) != -1;
				shouldCast &= KoLCharacter.hasSkill( BREAKFAST_SKILLS[i][0] );

				if ( checkSettings && shouldCast && KoLCharacter.isHardcore() )
				{
					if ( BREAKFAST_SKILLS[i][0].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
						shouldCast = false;
					if ( BREAKFAST_SKILLS[i][0].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
						shouldCast = false;
				}

				if ( shouldCast )
					getBreakfast( BREAKFAST_SKILLS[i][0], StaticEntity.parseInt( BREAKFAST_SKILLS[i][1] ) );
			}
		}

		forceContinue();
	}

	public void getBreakfast( String skillname, int standardCast )
	{	(new UseSkillRequest( this, skillname, "", standardCast )).run();
	}

	public final void refreshSession()
	{
		updateDisplay( "Refreshing session data..." );

		// Get current moon phases

		forceContinue();
		(new MoonPhaseRequest( this )).run();
		if ( refusesContinue() )
			return;

		// Retrieve the character sheet first. It's necessary to do
		// this before concoctions have a chance to get refreshed.

		forceContinue();
		(new CharsheetRequest( this )).run();
		if ( refusesContinue() )
			return;

		// Retrieve the items which are available for consumption
		// and item creation.

		forceContinue();
		(new EquipmentRequest( this, EquipmentRequest.CLOSET )).run();
		if ( refusesContinue() )
			return;

		// If the password hash is non-null, but is not available,
		// then that means you might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
			return;

		// Retrieve the list of familiars which are available to
		// the player, if they haven't opted to skip them.

		forceContinue();
		(new FamiliarRequest( this )).run();

		if ( refusesContinue() )
			return;

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		updateDisplay( "Retrieving campground data..." );

		forceContinue();
		(new CampgroundRequest( this )).run();
		if ( refusesContinue() )
			return;

		forceContinue();
		(new ItemStorageRequest( this )).run();
		if ( refusesContinue() )
			return;

		forceContinue();
		CharpaneRequest.getInstance().run();
		updateDisplay( "Session data refreshed." );
	}

	/**
	 * Utility method used to notify the client that it should attempt
	 * to retrieve breakfast.
	 */

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		KoLRequest.sessionID = null;
		KoLRequest.passwordHash = null;

		sessionStream.close();
		sessionStream = NullStream.INSTANCE;

		debugStream.close();
		debugStream = NullStream.INSTANCE;

		mirrorStream.close();
		mirrorStream = NullStream.INSTANCE;

		echoStream.close();
		echoStream = NullStream.INSTANCE;
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSession()
	{
		forceContinue();
		updateDisplay( "Resetting session tallies..." );

		missingItems.clear();
		encounterList.clear();
		adventureList.clear();

		initialStats[0] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		initialStats[1] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		initialStats[2] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		fullStatGain[0] = 0;
		fullStatGain[1] = 0;
		fullStatGain[2] = 0;

		tally.clear();
		tally.add( new AdventureResult( AdventureResult.ADV ) );
		tally.add( new AdventureResult( AdventureResult.MEAT ) );
		tally.add( new AdventureResult( AdventureResult.SUBSTATS ) );
		tally.add( new AdventureResult( AdventureResult.FULLSTATS ) );

		updateDisplay( "Session tallies reset." );
	}

	/**
	 * Utility method to parse an individual adventuring result.
	 * This method determines what the result actually was and
	 * adds it to the tally.
	 *
	 * @param	result	String to parse for the result
	 */

	public boolean parseResult( String result )
	{
		String trimResult = result.trim();

		try
		{
			debugStream.println( "Parsing result: " + trimResult );
			return processResult( AdventureResult.parseResult( trimResult ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		return false;
	}

	public void parseItem( String result )
	{
		debugStream.println( "Parsing item: " + result );

		// We do the following in order to not get confused by:
		//
		// Frobozz Real-Estate Company Instant House (TM)
		// stone tablet (Sinister Strumming)
		// stone tablet (Squeezings of Woe)
		// stone tablet (Really Evil Rhythm)
		//
		// which otherwise cause an exception and a stack trace

		// Look for a verbatim match
		int itemID = TradeableItemDatabase.getItemID( result.trim() );
		if ( itemID != -1 )
		{
			processResult( new AdventureResult( itemID, 1 ) );
			return;
		 }

		// Remove parenthesized number and match again.
		String name = result;
		int count = 1;

		int index = result.lastIndexOf( " (" );
		if ( index != -1 )
		{
			name = result.substring( 0, index );
			count = StaticEntity.parseInt( result.substring( index ) );
		}

		processResult( new AdventureResult( name, count, false ) );
	}

	public boolean parseEffect( String result )
	{
		debugStream.println( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		return processResult( new AdventureResult( parsedEffectName, StaticEntity.parseInt( parsedDuration ), true ) );
	}

	/**
	 * Utility method used to process a result.  By default, this
	 * method will also add an adventure result to the tally directly.
	 * This is used whenever the nature of the result is already known
	 * and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 */

	public boolean processResult( AdventureResult result )
	{	return processResult( result, true );
	}

	/**
	 * Utility method used to process a result, and the user wishes to
	 * specify whether or not the result should be added to the running
	 * tally.  This is used whenever the nature of the result is already
	 * known and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 * @param	shouldTally	Whether or not the result should be added to the running tally
	 */

	public boolean processResult( AdventureResult result, boolean shouldTally )
	{
		// This should not happen, but check just in case and
		// return if the result was null.

		if ( result == null )
			return false;

		debugStream.println( "Processing result: " + result );
		String resultName = result.getName();

		// This should not happen, but check just in case and
		// return if the result name was null.

		if ( resultName == null )
			return false;

		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list.  Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
		{
			AdventureResult.addResultToList( recentEffects, result );
			shouldRefresh |= !activeEffects.containsAll( recentEffects );
		}
		else if ( resultName.equals( AdventureResult.ADV ) && result.getCount() < 0 )
			AdventureResult.addResultToList( tally, result.getNegation() );

		else if ( result.isItem() || resultName.equals( AdventureResult.SUBSTATS ) || resultName.equals( AdventureResult.MEAT ) )
		{
			if ( shouldTally )
				AdventureResult.addResultToList( tally, result );
		}

		int effectCount = activeEffects.size();
		KoLCharacter.processResult( result );
		shouldRefresh |= effectCount != activeEffects.size();

		if ( !shouldTally )
			return shouldRefresh;

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && tally.size() >= 3 )
		{
			int currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - initialStats[0];
			shouldRefresh |= fullStatGain[0] != currentTest;
			fullStatGain[0] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - initialStats[1];
			shouldRefresh |= fullStatGain[1] != currentTest;
			fullStatGain[1] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - initialStats[2];
			shouldRefresh |= fullStatGain[2] != currentTest;
			fullStatGain[2] = currentTest;

			if ( tally.size() > 3 )
				tally.set( 3, new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
		}

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		int conditionsIndex = conditions.indexOf( result );

		if ( !resultName.equals( AdventureResult.ADV ) && conditionsIndex != -1 )
		{
			if ( resultName.equals( AdventureResult.SUBSTATS ) )
			{
				// If the condition is a substat condition,
				// then zero out the appropriate count, if
				// applicable, and remove the substat condition
				// if the overall count dropped to zero.

				AdventureResult condition = (AdventureResult) conditions.get( conditionsIndex );

				int [] substats = new int[3];
				for ( int i = 0; i < 3; ++i )
					substats[i] = Math.max( 0, condition.getCount(i) - result.getCount(i) );

				condition = new AdventureResult( AdventureResult.SUBSTATS, substats );

				if ( condition.getCount() == 0 )
					conditions.remove( conditionsIndex );
				else
					conditions.set( conditionsIndex, condition );
			}
			else if ( result.getCount( conditions ) <= result.getCount() )
			{
				// If this results in the satisfaction of a
				// condition, then remove it.

				conditions.remove( conditionsIndex );
			}
			else if ( result.getCount() > 0 )
			{
				// Otherwise, this was a partial satisfaction
				// of a condition.  Decrement the count by the
				// negation of this result.

				AdventureResult.addResultToList( conditions, result.getNegation() );
			}
		}

		return shouldRefresh;
	}

	/**
	 * Adds the recent effects accumulated so far to the actual effects.
	 * This should be called after the previous effects were decremented,
	 * if adventuring took place.
	 */

	public static void applyEffects()
	{
		if ( recentEffects.isEmpty() )
			return;

		for ( int j = 0; j < recentEffects.size(); ++j )
			AdventureResult.addResultToList( activeEffects, (AdventureResult) recentEffects.get(j) );

		activeEffects.sort();
		recentEffects.clear();
		KoLCharacter.recalculateAdjustments( false );
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerID	The ID of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public static String getPlayerName( String playerID )
	{	return (String) seenPlayerNames.get( playerID );
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerName	The name of the player
	 * @return	The player's ID if the player has been seen, or the player's name
	 *			with spaces replaced with underscores and other elements encoded
	 *			if the player's ID has not been seen.
	 */

	public static String getPlayerID( String playerName )
	{
		if ( playerName == null )
			return null;

		String playerID = (String) seenPlayerIDs.get( playerName.toLowerCase() );
		return playerID != null ? playerID : playerName;
	}

	/**
	 * Registers the given player name and player ID with
	 * KoLmafia's player name tracker.
	 *
	 * @param	playerName	The name of the player
	 * @param	playerID	The player ID associated with this player
	 */

	public static void registerPlayer( String playerName, String playerID )
	{
		if ( !seenPlayerIDs.containsKey( playerName.toLowerCase() ) )
		{
			seenPlayerIDs.put( playerName.toLowerCase(), playerID );
			seenPlayerNames.put( playerID, playerName );
		}
	}

	public static void registerContact( String playerName, String playerID )
	{
		registerPlayer( playerName, playerID );
		if ( !contactList.contains( playerName ) )
			contactList.add( playerName );
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return inventory.contains( SewerRequest.CLOVER );
	}

	/**
	 * Utility method which ensures that the amount needed exists,
	 * and if not, calls the appropriate scripts to do so.
	 */

	private final boolean recover( int needed, String settingName, String currentName, String maximumName, Object [] techniques ) throws Exception
	{
		if ( refusesContinue() )
			return false;

		Object [] empty = new Object[0];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[0] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[0] );

		int initial = needed;
		int maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();

		// First, check against the restore trigger to see if
		// any restoration needs to take place.

		float setting = StaticEntity.getFloatProperty( settingName );
		if ( needed == 0 && setting < 0 )
			return true;

		if ( !BuffBotHome.isBuffBotActive() )
			needed = Math.max( needed, (int) Math.max( setting * (float) maximum, (float) needed ) );

		int last = -1;
		int current = ((Number)currentMethod.invoke( null, empty )).intValue();

		// If a buffbot is currently running, only restore MP to
		// max when what you have is less than what you need.

		if ( BuffBotHome.isBuffBotActive() )
		{
			if ( current < needed )
				needed = maximum;
		}
		else if ( needed >= maximum )
		{
			needed = maximum;
		}

		if ( needed > 0 && current >= needed )
			return true;

		// Next, check against the restore target to see how
		// far you need to go.

		int threshold = initial == 0 ? needed - 1 : settingName.startsWith( "mp" ) ? current : needed - 1;
		setting = StaticEntity.getFloatProperty( settingName + "Target" );

		if ( needed == 0 && setting <= 0 )
			return true;

		needed = Math.max( (int) ( setting * (float) maximum ), needed );

		// If it gets this far, then you should attempt to recover
		// using the selected items.  This involves a few extra
		// reflection methods.

		String restoreSetting = StaticEntity.getProperty( settingName + "Items" ).trim().toLowerCase();

		// Iterate through every single restore item, checking to
		// see if the settings wish to use this item.  If so, go ahead
		// and process the item's usage.

		String currentTechniqueName;

		for ( int i = 0; i < techniques.length && current <= threshold; ++i )
		{
			currentTechniqueName = techniques[i].toString().toLowerCase();
			if ( restoreSetting.indexOf( currentTechniqueName ) != -1 )
			{
				do
				{
					last = current;
					recoverOnce( techniques[i], currentTechniqueName, needed );
					current = ((Number)currentMethod.invoke( null, empty )).intValue();

					// Do not allow seltzer to be used more than once,
					// as this indicates MP changes due to outfits.
					// Simply break the loop and move onto cola or soda
					// water as the next restore.
				}
				while ( techniques[i] != MPRestoreItemList.SELTZER && current <= threshold && last != current && !refusesContinue() );
			}
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( refusesContinue() )
			return false;

		if ( current > threshold )
			return true;

		updateDisplay( ERROR_STATE, "Autorecovery failed." );
		return false;
	}

	/**
	 * Utility method called inbetween battles.  This method
	 * checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final boolean recoverHP()
	{	return recoverHP( 0 );
	}

	public final boolean recoverHP( int recover )
	{
		try
		{
			return recover( recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility method which uses the given recovery technique (not specified
	 * in a script) in order to restore.
	 */

	private final void recoverOnce( Object technique, String techniqueName, int needed )
	{
		// If the technique is an item, and the item is not readily available,
		// then don't bother with this item -- however, if it is the only item
		// present, then rethink it.

		if ( technique instanceof HPRestoreItemList.HPRestoreItem )
			((HPRestoreItemList.HPRestoreItem)technique).recoverHP( needed );

		if ( technique instanceof MPRestoreItemList.MPRestoreItem )
			((MPRestoreItemList.MPRestoreItem)technique).recoverMP( needed );
	}

	/**
	 * Returns the total number of mana restores currently
	 * available to the player.
	 */

	public int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = StaticEntity.getProperty( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[i].toString().toLowerCase() ) != -1 )
				restoreCount += MPRestoreItemList.CONFIGURES[i].getItem().getCount( inventory );

		return restoreCount;
	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final boolean recoverMP()
	{	return recoverMP( 0 );
	}

	/**
	 * Utility method which restores the character's current
	 * mana points above the given value.
	 */

	public final boolean recoverMP( int mpNeeded )
	{
		try
		{
			return recover( mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility method used to process the results of any adventure
	 * in the Kingdom of Loathing.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 * @return	<code>true</code> if any results existed
	 */

	public final boolean processResults( String results )
	{
		debugStream.println( "Processing results..." );

		if ( results.indexOf( "gains a pound" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			sessionStream.println();
			sessionStream.println( "familiar " + KoLCharacter.getFamiliar() );
		}

		String plainTextResult = results.replaceAll( "<.*?>", LINE_BREAK );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, LINE_BREAK );
		String lastToken = null;

		Matcher damageMatcher = null;
		int lastDamageIndex = 0;

		if ( KoLCharacter.isUsingStabBat() )
		{
			damageMatcher = STABBAT_PATTERN.matcher( plainTextResult );

			while ( damageMatcher.find( lastDamageIndex ) )
			{
				lastDamageIndex = damageMatcher.end();
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				KoLmafiaCLI.printLine( message );
				sessionStream.println( message );
				parseResult( message );
			}
		}

		damageMatcher = FUMBLE_PATTERN.matcher( plainTextResult );
		lastDamageIndex = 0;

		while ( damageMatcher.find( lastDamageIndex ) )
		{
			lastDamageIndex = damageMatcher.end();
			String message = "You lose " + damageMatcher.group(1) + " hit points";

			KoLmafiaCLI.printLine( message );
			sessionStream.println( message );
			parseResult( message );
		}

		boolean requiresRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			lastToken = parsedResults.nextToken();

			// Skip effect acquisition - it's followed by a boldface
			// which makes the parser think it's found an item.

			if ( lastToken.startsWith( "You acquire" ) )
			{
				String acquisition = lastToken;

				if ( lastToken.indexOf( "effect" ) == -1 )
				{
					String item = parsedResults.nextToken();

					if ( lastToken.indexOf( "an item" ) != -1 )
					{
						KoLmafiaCLI.printLine( acquisition + " " + item );
						sessionStream.println( acquisition + " " + item );
						parseItem( item );
					}
					else
					{
						// The name of the item follows the number
						// that appears after the first index.

						String countString = item.split( " " )[0];
						int spaceIndex = item.indexOf( " " );

						String itemName = spaceIndex == -1 ? item : item.substring( spaceIndex ).trim();
						boolean isNumeric = spaceIndex != -1;

						for ( int i = 0; isNumeric && i < countString.length(); ++i )
							isNumeric &= Character.isDigit( countString.charAt(i) ) || countString.charAt(i) == ',';

						if ( !isNumeric )
							countString = "1";
						else if ( itemName.equals( "evil golden arches" ) )
							itemName = "evil golden arch";

						KoLmafiaCLI.printLine( acquisition + " " + item );
						sessionStream.println( acquisition + " " + item );
						parseItem( itemName + " (" + countString + ")" );
					}
				}
				else
				{
					String effectName = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					sessionStream.println( acquisition + " " + effectName + " " + lastToken );
					KoLmafiaCLI.printLine( acquisition + " " + effectName + " " + lastToken );

					if ( lastToken.indexOf( "duration" ) == -1 )
					{
						parseEffect( effectName );
					}
					else
					{
						String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
						requiresRefresh |= parseEffect( effectName + " (" + duration + ")" );
					}
				}
			}
			else if ( (lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " )) )
			{
				int periodIndex = lastToken.indexOf( "." );
				if ( periodIndex != -1 )
					lastToken = lastToken.substring( 0, periodIndex );

				int parenIndex = lastToken.indexOf( "(" );
				if ( parenIndex != -1 )
					lastToken = lastToken.substring( 0, parenIndex );

				lastToken = lastToken.trim();

				if ( lastToken.indexOf( "level" ) == -1 )
				{
					KoLmafiaCLI.printLine( lastToken );
					sessionStream.println( lastToken );
				}

				// Because of the simplified parsing, there's a chance that
				// the "gain" acquired wasn't a subpoint (in other words, it
				// includes the word "a" or "some"), which causes a NFE or
				// possibly a ParseException to be thrown.  catch them and
				// do nothing (eventhough it's technically bad style).

				if ( !lastToken.startsWith( "You gain a" ) && !lastToken.startsWith( "You gain some" ) )
					requiresRefresh |= parseResult( lastToken );
			}
		}

		return requiresRefresh;
	}

	public void makeRequest( Runnable request )
	{	makeRequest( request, 1 );
	}

	/**
	 * Makes the given request for the given number of iterations,
	 * or until continues are no longer possible, either through
	 * user cancellation or something occuring which prevents the
	 * requests from resuming.
	 *
	 * @param	request	The request made by the user
	 * @param	iterations	The number of times the request should be repeated
	 */

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			// Before anything happens, make sure that you are in
			// in a valid continuation state.

			if ( refusesContinue() )
				return;

			forceContinue();

			// Handle the gym, which is the only adventure type
			// which needs to be specially handled.

			if ( request instanceof KoLAdventure )
			{
				KoLAdventure adventure = (KoLAdventure) request;

				if ( adventure.getRequest() instanceof ClanGymRequest )
				{
					((ClanGymRequest)adventure.getRequest()).setTurnCount( iterations );
					((ClanGymRequest)adventure.getRequest()).run();
					return;
				}
				else if ( adventure.getRequest() instanceof SewerRequest )
				{
					if ( conditions.isEmpty() )
						AdventureDatabase.retrieveItem( SewerRequest.GUM.getInstance( iterations ) );
				}
			}

			// Execute the request as initially intended by calling
			// a subroutine.  In doing so, make sure your HP/MP restore
			// settings are scaled back down to current levels, if they've
			// been manipulated internally by KoLmafia.

			resetRestoreSettings( false );
			executeRequest( request, iterations );
			resetRestoreSettings( true );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static void resetRestoreSettings( boolean resetToZero )
	{
		String action = StaticEntity.getProperty( "battleAction" );

		float trigger = StaticEntity.getFloatProperty( "mpAutoRecovery" );
		float target = StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" );

		// Only muck with the settings if the trigger and
		// the target are the same, which logically only
		// happens if KoLmafia is changing it internally.

		if ( trigger == target )
		{
			if ( !action.startsWith( "skill" ) || resetToZero )
			{
				StaticEntity.setProperty( "mpAutoRecovery", "0.0" );
				StaticEntity.setProperty( "mpAutoRecoveryTarget", "0.0" );
			}
			else
			{
				int skillID = ClassSkillsDatabase.getSkillID( action.substring( 6 ) );
				int mpCost = ClassSkillsDatabase.getMPConsumptionByID( skillID );

				if ( trigger == 0.0f )
				{
					float required = Math.min( (float) mpCost / (float) KoLCharacter.getMaximumMP(), 1.0f );

					if ( required != 1.0 )
						required = ((float) Math.ceil( required * 10.0f )) / 10.0f;

					if ( trigger <= required )
					{
						StaticEntity.setProperty( "mpAutoRecovery", String.valueOf( required ) );
						StaticEntity.setProperty( "mpAutoRecoveryTarget", String.valueOf( required ) );
					}
				}
			}
		}
	}

	private void executeRequest( Runnable request, int iterations )
	{
		hadPendingState = false;
		boolean isCheckExempt = !(request instanceof KoLAdventure) || ((KoLAdventure)request).getRequest() instanceof CampgroundRequest ||
			KoLCharacter.getInebriety() > 25;

		if ( KoLCharacter.isFallingDown() && !isCheckExempt )
		{
			updateDisplay( ERROR_STATE, "You are too drunk to continue." );
			return;
		}

		// Check to see if there are any end conditions.  If
		// there are conditions, be sure that they are checked
		// during the iterations.

		int initialConditions = conditions.size();
		int remainingConditions = initialConditions;

		// Begin the adventuring process, or the request execution
		// process (whichever is applicable).

		int currentIteration = 0;
		boolean shouldEnableRefreshStatus = RequestFrame.isRefreshStatusEnabled();
		RequestFrame.setRefreshStatusEnabled( false );

		ItemCreationRequest [] creatables = new ItemCreationRequest[ conditions.size() ];
		for ( int i = 0; i < conditions.size(); ++i )
			creatables[i] = ItemCreationRequest.getInstance( this, (AdventureResult) conditions.get(i) );

		while ( permitsContinue() && ++currentIteration <= iterations )
		{
			// Account for the possibility that you could have run
			// out of adventures mid-request.

			if ( KoLCharacter.getAdventuresLeft() == 0 && request instanceof KoLAdventure )
			{
				iterations = currentIteration;
				break;
			}

			// See if you can create anything to satisfy your item
			// conditions, but only do so if it's an adventure.

			if ( request instanceof KoLAdventure )
			{
				for ( int i = 0; i < creatables.length; ++i )
				{
					if ( creatables[i] != null && creatables[i].getCount( ConcoctionsDatabase.getConcoctions() ) >= creatables[i].getQuantityNeeded() )
					{
						creatables[i].run();
						creatables[i] = null;
					}
				}
			}

			// If the conditions existed and have been satisfied,
			// then you should stop.

			if ( conditions.size() < remainingConditions )
			{
				if ( conditions.size() == 0 || useDisjunction )
				{
					conditions.clear();
					remainingConditions = 0;
					break;
				}
			}

			remainingConditions = conditions.size();

			// Otherwise, disable the display and update the user
			// and the current request number.  Different requests
			// have different displays.  They are handled here.

			if ( request instanceof KoLAdventure && iterations > 1 )
				currentIterationString = "Request " + currentIteration + " of " + iterations + " (" + request.toString() + ") in progress...";
			else if ( request instanceof KoLAdventure )
				currentIterationString = "Visit to " + request.toString() + " in progress...";

			if ( refusesContinue() )
				return;

			request.run();

			if ( request instanceof KoLAdventure )
				KoLmafiaCLI.printBlankLine();

			// Decrement the counter to null out the increment
			// effect on the next iteration of the loop.

			if ( request instanceof KoLAdventure && ((KoLAdventure)request).getRequest().getAdventuresUsed() == 0 )
				--currentIteration;

			// Prevent drunkenness adventures from occurring by
			// testing inebriety levels after the request is run.

			if ( KoLCharacter.isFallingDown() && !isCheckExempt )
			{
				updateDisplay( ERROR_STATE, "You are too drunk to continue." );
				return;
			}
		}

		if ( request instanceof KoLAdventure )
			currentIterationString = "";

		if ( shouldEnableRefreshStatus )
		{
			RequestFrame.setRefreshStatusEnabled( true );
			RequestFrame.refreshStatus();
		}

		if ( !permitsContinue() )
		{
			if ( continuationState != PENDING_STATE )
				return;

			hadPendingState = true;
			forceContinue();
		}

		// If you've completed the requests, make sure to update
		// the display.

		if ( !isRunningBetweenBattleChecks() && request instanceof KoLAdventure && !conditions.isEmpty() )
			updateDisplay( ERROR_STATE, "Conditions not satisfied after " + (currentIteration - 1) +
				((currentIteration == 2) ? " request." : " requests.") );

		else if ( initialConditions != 0 && conditions.isEmpty() )
			updateDisplay( "Conditions satisfied after " + (currentIteration - 1) +
				((currentIteration == 2) ? " request." : " requests.") );

		else if ( !(request instanceof UseSkillRequest || request instanceof LoginRequest || request instanceof LogoutRequest) )
			updateDisplay( iterations > 1 ? "Requests completed." : "Request completed." );
	}

	/**
	 * Makes a request which attempts to remove the given effect.
	 */

	public abstract void makeUneffectRequest();

	/**
	 * Makes a request which attempts to zap a chosen item
	 */

	public abstract void makeZapRequest();

	/**
	 * Makes a request to the hermit in order to trade worthless
	 * items for more useful items.
	 */

	public abstract void makeHermitRequest();

	/**
	 * Makes a request to the trapper to trade yeti furs for
	 * other kinds of furs.
	 */

	public abstract void makeTrapperRequest();
	/**
	 * Makes a request to the hunter to trade today's bounty
	 * items in for meat.
	 */

	public abstract void makeHunterRequest();

	/**
	 * Makes a request to the untinkerer to untinker items
	 * into their component parts.
	 */

	public abstract void makeUntinkerRequest();

	/**
	 * Makes a request to set the mind control device to the desired value
	 */

	public abstract void makeMindControlRequest();

	public static void validateFaucetQuest()
	{
		int lastAscension = StaticEntity.getIntegerProperty( "lastTavernAscension" );
		if ( lastAscension != KoLCharacter.getAscensions() )
		{
			StaticEntity.setProperty( "lastTavernSquare", "0" );
			StaticEntity.setProperty( "lastTavernAscension", String.valueOf( KoLCharacter.getAscensions() ) );
			for ( int i = 1; i <= 25; ++i )
				StaticEntity.setProperty( "tavernSquare" + i, "0" );
		}
	}

	public static void addTavernLocation( KoLRequest request )
	{
		KoLmafia.validateFaucetQuest();

		if ( request.getURLString().indexOf( "fight" ) != -1 )
		{
			String lastSquare = StaticEntity.getProperty( "lastTavernSquare" );
			if ( request.fullResponse != null )
				StaticEntity.setProperty( lastSquare, request.fullResponse.indexOf( "Baron" ) != -1 ? "4" : "1" );
		}
		else
		{
			String urlString = request.getURLString();
			if ( urlString.indexOf( "charpane" ) != -1 || urlString.indexOf( "chat" ) != -1 || urlString.equals( "rats.php" ) )
				return;

			Matcher squareMatcher = TAVERN_PATTERN.matcher( urlString );
			if ( !squareMatcher.find() )
				return;

			// Handle fighting rats.  If this was done through
			// the mini-browser, you'll have response text; else,
			// the response text will be null.

			String lastSquare = "tavernSquare" + squareMatcher.group(1);
			StaticEntity.setProperty( "lastTavernSquare", lastSquare );

			if ( request.fullResponse != null && request.fullResponse.indexOf( "faucetoff" ) != -1 )
				StaticEntity.setProperty( lastSquare, "3" );
			else if ( request.fullResponse != null && request.fullResponse.indexOf( "You acquire" ) != -1 )
				StaticEntity.setProperty( lastSquare, "2" );
			else
				StaticEntity.setProperty( lastSquare, "1" );
		}
	}

	/**
	 * Completes the infamous tavern quest.
	 */

	public int locateTavernFaucet()
	{
		validateFaucetQuest();

		// Determine which elements have already been checked
		// so you don't check through them again.

		ArrayList searchList = new ArrayList();
		Integer searchIndex = null;

		for ( int i = 1; i <= 25; ++i )
			searchList.add( new Integer(i) );

		// If the faucet has not yet been found, then go through
		// the process of trying to locate it.

		KoLAdventure adventure = new KoLAdventure( this, "", "0", "0", "rats.php", "", "Typical Tavern (Pre-Rat)" );
		boolean foundFaucet = searchList.size() < 2;

		if ( KoLCharacter.getLevel() < 3 )
		{
			updateDisplay( ERROR_STATE, "You need to level up first." );
			return -1;
		}

		KoLRequest request = new KoLRequest( this, "council.php", true );
		request.run();

		if ( request.responseText == null || request.responseText.indexOf( "rat problems" ) == -1 )
		{
			updateDisplay( ERROR_STATE, "This quest is not available." );
			return -1;
		}

		updateDisplay( "Searching for faucet..." );
		adventure.run();

		// Random guess instead of straightforward search
		// for the location of the faucet (lowers the chance
		// of bad results if the faucet is near the end).

		while ( !foundFaucet && KoLCharacter.getCurrentHP() > 0 && KoLCharacter.getAdventuresLeft() > 0 )
		{
			searchIndex = (Integer) searchList.remove( RNG.nextInt( searchList.size() ) );

			adventure.getRequest().clearDataFields();
			adventure.getRequest().addFormField( "where", searchIndex.toString() );
			adventure.run();

			foundFaucet = adventure.getRequest().responseText != null &&
				adventure.getRequest().responseText.indexOf( "faucetoff" ) != -1;
		}

		// If you have not yet found the faucet, be sure
		// to set the settings so that your next attempt
		// does not repeat located squares.

		if ( !foundFaucet )
		{
			updateDisplay( ERROR_STATE, "Unable to find faucet." );
			return -1;
		}

		// Otherwise, you've found it!  So notify the user
		// that the faucet has been found.

		int faucetRow = (int) ((searchIndex.intValue() - 1) / 5) + 1;
		int faucetColumn = (searchIndex.intValue() - 1) % 5 + 1;

		updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		return searchIndex.intValue();
	}

	/**
	 * Trades items with the guardian of the goud.
	 */

	public void tradeGourdItems()
	{
		updateDisplay( "Determining items needed..." );
		KoLRequest request = new KoLRequest( this, "town_right.php?place=gourd", true );
		request.run();

		// For every class, it's the same -- the message reads, "Bring back"
		// and then the number of the item needed.  Compare how many you need
		// with how many you have.

		Matcher neededMatcher = GOURD_PATTERN.matcher( request.responseText );
		AdventureResult item;

		switch ( KoLCharacter.getPrimeIndex() )
		{
			case 0:
				item = new AdventureResult( 747, 5 );
				break;
			case 1:
				item = new AdventureResult( 559, 5 );
				break;
			default:
				item = new AdventureResult( 27, 5 );
		}

		int neededCount = neededMatcher.find() ? StaticEntity.parseInt( neededMatcher.group(1) ) : 26;

		while ( neededCount <= 25 && neededCount <= item.getCount( inventory ) )
		{
			updateDisplay( "Giving up " + neededCount + " " + item.getName() + "s..." );
			request = new KoLRequest( this, "town_right.php?place=gourd&action=gourd", true );
			request.run();

			processResult( item.getInstance( 0 - neededCount++ ) );
		}

		int totalProvided = 0;
		for ( int i = 5; i < neededCount; ++i )
			totalProvided += i;

		updateDisplay( "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far)." );
	}

	public void unlockGuildStore()
	{	unlockGuildStore( false );
	}

	public void unlockGuildStore( boolean stopAtPaco )
	{
		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain.  Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		updateDisplay( "Entering guild challenge area..." );
		KoLRequest request = new KoLRequest( this, "guild.php?place=challenge", true );
		request.run();

		boolean success = stopAtPaco ? request.responseText.indexOf( "paco" ) != -1 :
			request.responseText.indexOf( "store.php" ) != -1;

		updateDisplay( "Completing guild tasks..." );

		for ( int i = 0; i < 6 && !success && KoLCharacter.getAdventuresLeft() > 0 && permitsContinue(); ++i )
		{
			request = new KoLRequest( this, "guild.php?action=chal", true );
			request.run();

			if ( request.responseText != null )
			{
				success |= stopAtPaco ? request.responseText.indexOf( "paco" ) != -1 :
					request.responseText.indexOf( "You've already beaten" ) != -1;
			}

			if ( !success && !request.needsRefresh )
				CharpaneRequest.getInstance().run();
		}

		if ( success && KoLCharacter.getLevel() > 3 )
		{
			request = new KoLRequest( this, "guild.php?place=paco", true );
			request.run();
		}

		if ( success && stopAtPaco )
			updateDisplay( "You have unlocked the guild meatcar quest." );
		else if ( success )
			updateDisplay( "Guild store successfully unlocked." );
		else
			updateDisplay( "Guild store was not unlocked." );
	}

	public void priceItemsAtLowestPrice()
	{
		(new StoreManageRequest( this )).run();

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		int [] itemID = new int[ sold.length ];
		int [] prices = new int[ sold.length ];
		int [] limits = new int[ sold.length ];

		for ( int i = 0; i < sold.length; ++i )
		{
			limits[i] = sold[i].getLimit();
			itemID[i] = sold[i].getItemID();

			int minimumPrice = TradeableItemDatabase.getPriceByID( sold[i].getItemID() ) * 2;
			if ( sold[i].getPrice() == 999999999 && minimumPrice > 0 )
			{
				minimumPrice = Math.max( 100, minimumPrice );
				int desiredPrice = sold[i].getLowest() - (sold[i].getLowest() % 100);
				prices[i] = desiredPrice < minimumPrice ? minimumPrice : desiredPrice;
			}
			else
				prices[i] = sold[i].getPrice();
		}

		(new StoreManageRequest( this, itemID, prices, limits )).run();
		updateDisplay( "Repricing complete." );
	}

	/**
	 * Show an HTML string to the user
	 */

	public abstract void showHTML( String text, String title );

	public static final boolean hadPendingState()
	{	return hadPendingState;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * is permitted by the client, or by current circumstances in-game.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final boolean permitsContinue()
	{	return continuationState == CONTINUE_STATE;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * will be denied by the client, regardless of continue state reset,
	 * until the display is enable (ie: in an abort state).
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final boolean refusesContinue()
	{	return continuationState == ABORT_STATE;
	}

	/**
	 * Forces a continue state.  This should only be called when
	 * there is no doubt that a continue should occur.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final void forceContinue()
	{	continuationState = CONTINUE_STATE;
	}

	/**
	 * Utility method which opens a stream to the given file
	 * and closes the original stream, if needed.
	 */

	protected static final PrintStream openStream( String filename, PrintStream originalStream, boolean hasStaticLocation )
	{
		if ( !hasStaticLocation && KoLCharacter.getUsername().equals( "" ) )
			return NullStream.INSTANCE;

		try
		{
			// Before doing anything, be sure to close the
			// original stream.

			if ( !(originalStream instanceof NullStream) )
			{
				if ( hasStaticLocation )
					return originalStream;

				originalStream.close();
			}

			// Now, create the file and wrap a LogStream around
			// it for output.

			File f = new File( filename );

			if ( !f.exists() )
			{
				if ( f.getParentFile() != null )
					f.getParentFile().mkdirs();
				f.createNewFile();
			}

			return new LogStream( f );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return NullStream.INSTANCE;
		}
	}

	/**
	 * Initializes a stream for logging debugging information.  This
	 * method creates a <code>KoLmafia.log</code> file in the default
	 * directory if one does not exist, or appends to the existing
	 * log.  This method should only be invoked if the user wishes to
	 * assist in beta testing because the output is VERY verbose.
	 */

	public static final void openSessionStream()
	{
		sessionStream = openStream( "sessions/" + KoLCharacter.getUsername() + "_" +
			DATED_FILENAME_FORMAT.format( new Date() ) + ".txt", sessionStream, false );
	}

	public static final void closeSessionStream()
	{
		sessionStream.close();
		sessionStream = NullStream.INSTANCE;
	}

	/**
	 * Retrieves the stream currently used for logging output for
	 * the URL/session logger.
	 */

	public static final PrintStream getSessionStream()
	{	return sessionStream;
	}

	/**
	 * Initializes the debug log stream.
	 */

	public static final void openDebugStream()
	{	debugStream = openStream( "KoLmafia.log", debugStream, true );
	}

	public static final PrintStream getDebugStream()
	{	return debugStream;
	}

	public static final void closeDebugStream()
	{
		debugStream.close();
		debugStream = NullStream.INSTANCE;
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public static final void addSaveState( String username, String password )
	{
		try
		{
			String utfString = URLEncoder.encode( password, "UTF-8" );

			StringBuffer encodedString = new StringBuffer();
			char currentCharacter;
			for ( int i = 0; i < utfString.length(); ++i )
			{
				currentCharacter = utfString.charAt(i);
				switch ( currentCharacter )
				{
					case '-':  encodedString.append( "2D" );  break;
					case '.':  encodedString.append( "2E" );  break;
					case '*':  encodedString.append( "2A" );  break;
					case '_':  encodedString.append( "5F" );  break;
					case '+':  encodedString.append( "20" );  break;

					case '%':
						encodedString.append( utfString.charAt( ++i ) );
						encodedString.append( utfString.charAt( ++i ) );
						break;

					default:
						encodedString.append( Integer.toHexString( (int) currentCharacter ).toUpperCase() );
						break;
				}
			}

			StaticEntity.setProperty( "saveState." + username.toLowerCase(), (new BigInteger( encodedString.toString(), 36 )).toString( 10 ) );
			saveStateNames.add( username );
			storeSaveStates();
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static void removeSaveState( String loginname )
	{
		if ( loginname == null )
			return;

		saveStateNames.remove( loginname );
		StaticEntity.removeProperty( "saveState." + loginname.toLowerCase() );
	}

	private static final void storeSaveStates()
	{
		StringBuffer saveStateBuffer = new StringBuffer();
		String [] names = new String[ saveStateNames.size() ];
		saveStateNames.toArray( names );

		for ( int i = 0; i < names.length; ++i )
		{
			if ( getSaveState( names[i] ) == null )
				removeSaveState( names[i] );

			for ( int j = 0; j < i; ++j )
				if ( names[i].equalsIgnoreCase( names[j] ) )
					saveStateNames.remove( names[i] );
		}

		if ( names.length != saveStateNames.size() )
		{
			names = new String[ saveStateNames.size() ];
			saveStateNames.toArray( names );
		}

		if ( names.length > 0 )
		{
			saveStateBuffer.append( names[0] );
			for ( int i = 1; i < names.length; ++i )
			{
				saveStateBuffer.append( "//" );
				saveStateBuffer.append( names[i] );
			}
		}

		StaticEntity.setProperty( "saveState", saveStateBuffer.toString() );
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public static final String getSaveState( String loginname )
	{
		try
		{
			String password = StaticEntity.getProperty( "saveState." + loginname.toLowerCase() );
			if ( password == null || password.length() == 0 )
				return null;

			String hexString = (new BigInteger( password, 10 )).toString( 36 );
			StringBuffer utfString = new StringBuffer();
			for ( int i = 0; i < hexString.length(); ++i )
			{
				utfString.append( '%' );
				utfString.append( hexString.charAt(i) );
				utfString.append( hexString.charAt(++i) );
			}

			return URLDecoder.decode( utfString.toString(), "UTF-8" );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	public static boolean checkRequirements( List requirements )
	{
		AdventureResult [] requirementsArray = new AdventureResult[ requirements.size() ];
		requirements.toArray( requirementsArray );

		int missingCount;
		missingItems.clear();

		// Check the items required for this quest,
		// retrieving any items which might be inside
		// of a closet somewhere.

		for ( int i = 0; i < requirementsArray.length; ++i )
		{
			if ( requirementsArray[i] == null )
				continue;

			missingCount = 0;

			if ( requirementsArray[i].isItem() )
			{
				AdventureDatabase.retrieveItem( requirementsArray[i] );
				missingCount = requirementsArray[i].getCount() - requirementsArray[i].getCount( inventory );

				if ( KoLCharacter.hasEquipped( requirementsArray[i] ) )
					--missingCount;
			}
			else if ( requirementsArray[i].isStatusEffect() )
			{
				// Status effects should be compared against
				// the status effects list.  This is used to
				// help people detect which effects they are
				// missing (like in PVP).

				missingCount = requirementsArray[i].getCount() - requirementsArray[i].getCount( activeEffects );
			}
			else if ( requirementsArray[i].getName().equals( AdventureResult.MEAT ) )
			{
				// Currency is compared against the amount
				// actually liquid -- amount in closet is
				// ignored in this case.

				missingCount = requirementsArray[i].getCount() - KoLCharacter.getAvailableMeat();
			}

			if ( missingCount > 0 )
			{
				// If there are any missing items, add
				// them to the list of needed items.

				missingItems.add( requirementsArray[i].getInstance( missingCount ) );
			}
		}

		// If there are any missing requirements
		// be sure to return false.  Otherwise,
		// you managed to get everything.

		return missingItems.isEmpty();
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected void printList( List printing )
	{
		Object [] elements = new Object[ printing.size() ];
		printing.toArray( elements );

		StringBuffer buffer = new StringBuffer();

		for ( int i = 0; i < elements.length; ++i )
		{
			buffer.append( elements[i].toString() );
			buffer.append( LINE_BREAK );
		}

		KoLmafiaCLI.printLine( buffer.toString() );
	}

	/**
	 * Utility method used to purchase the given number of items
	 * from the mall using the given purchase requests.
	 */

	public void makePurchases( List results, Object [] purchases, int maxPurchases )
	{
		if ( purchases.length == 0 )
			return;

		for ( int i = 0; i < purchases.length; ++i )
			if ( !(purchases[i] instanceof MallPurchaseRequest) )
				return;

		MallPurchaseRequest currentRequest = (MallPurchaseRequest) purchases[0];
		AdventureResult itemToBuy = new AdventureResult( currentRequest.getItemID(), 0 );

		int initialCount = itemToBuy.getCount( inventory );
		int purchaseCount = 0, previousLimit = 0;

		for ( int i = 0; i < purchases.length && purchaseCount < maxPurchases && permitsContinue(); ++i )
		{
			if ( purchases[i] instanceof MallPurchaseRequest )
			{
				currentRequest = (MallPurchaseRequest) purchases[i];

				if ( !KoLCharacter.canInteract() && currentRequest.getQuantity() != MallPurchaseRequest.MAX_QUANTITY )
				{
					updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
					return;
				}

				// Keep track of how many of the item you had before
				// you run the purchase request

				previousLimit = currentRequest.getLimit();
				currentRequest.setLimit( Math.min( previousLimit, maxPurchases - purchaseCount ) );
				currentRequest.run();

				// Calculate how many of the item you have now after
				// you run the purchase request

				purchaseCount = itemToBuy.getCount( inventory ) - initialCount;

				// Remove the purchase from the list!  Because you
				// have already made a purchase from the store

				if ( permitsContinue() )
				{
					if ( currentRequest.getQuantity() == currentRequest.getLimit() )
						results.remove( currentRequest );
					else if ( currentRequest.getQuantity() == MallPurchaseRequest.MAX_QUANTITY )
						currentRequest.setLimit( MallPurchaseRequest.MAX_QUANTITY );
					else
					{
						if ( currentRequest.getLimit() == previousLimit )
							currentRequest.setCanPurchase( false );

						currentRequest.setQuantity( currentRequest.getQuantity() - currentRequest.getLimit() );
						currentRequest.setLimit( previousLimit );
					}
				}
				else
					currentRequest.setLimit( previousLimit );
			}
		}

		// With all that information parsed out, we should
		// refresh the lists at the very end.

		if ( purchaseCount >= maxPurchases || maxPurchases == Integer.MAX_VALUE )
			updateDisplay( "Purchases complete." );
		else
			updateDisplay( "Desired purchase quantity not reached (wanted " + maxPurchases + ", got " + purchaseCount + ")." );
	}

	/**
	 * Utility method used to register a given adventure in
	 * the running adventure summary.
	 */

	public void registerAdventure( KoLAdventure adventureLocation )
	{
		String adventureName = adventureLocation.getAdventureName();
		if ( adventureName == null )
			return;

		RegisteredEncounter previousAdventure = (RegisteredEncounter) adventureList.lastElement();

		if ( previousAdventure != null && previousAdventure.name.equals( adventureName ) )
		{
			++previousAdventure.encounterCount;
			adventureList.set( adventureList.size() - 1, previousAdventure );
		}
		else
		{
			adventureList.add( new RegisteredEncounter( adventureName ) );
		}
	}

	/**
	 * Utility method used to register a given encounter in
	 * the running adventure summary.
	 */

	public void registerEncounter( String encounterName )
	{
		encounterName = encounterName.trim();

		RegisteredEncounter [] encounters = new RegisteredEncounter[ encounterList.size() ];
		encounterList.toArray( encounters );

		for ( int i = 0; i < encounters.length; ++i )
		{
			if ( encounters[i].name.equals( encounterName ) )
			{
				++encounters[i].encounterCount;

				// Manually set to force repainting in GUI
				encounterList.set( i, encounters[i] );
				return;
			}
		}

		encounterList.add( new RegisteredEncounter( encounterName ) );
	}

	private class RegisteredEncounter implements Comparable
	{
		private String name;
		private int encounterCount;

		public RegisteredEncounter( String name )
		{
			this.name = name;
			encounterCount = 1;
		}

		public String toString()
		{	return name + " (" + encounterCount + ")";
		}

		public int compareTo( Object o )
		{
			return !(o instanceof RegisteredEncounter) || o == null ? -1 :
				name.compareToIgnoreCase( ((RegisteredEncounter)o).name );
		}
	}

	public KoLRequest getCurrentRequest()
	{	return currentRequest;
	}

	public void setCurrentRequest( KoLRequest request)
	{	currentRequest = request;
	}

	public final String [] extractTargets( String targetList )
	{
		// If there are no targets in the list, then
		// return absolutely nothing.

		if ( targetList == null || targetList.trim().length() == 0 )
			return new String[0];

		// Otherwise, split the list of targets, and
		// determine who all the unique targets are.

		String [] targets = targetList.trim().split( "\\s*,\\s*" );
		for ( int i = 0; i < targets.length; ++i )
			targets[i] = getPlayerID( targets[i] ) == null ? targets[i] :
				getPlayerID( targets[i] );

		// Sort the list in order to increase the
		// speed of duplicate detection.

		Arrays.sort( targets );

		// Determine who all the duplicates are.

		int uniqueListSize = targets.length;
		for ( int i = 1; i < targets.length; ++i )
		{
			if ( targets[i].equals( targets[ i - 1 ] ) )
			{
				targets[ i - 1 ] = null;
				--uniqueListSize;
			}
		}

		// Now, create the list of unique targets;
		// if the list has the same size as the original,
		// you can skip this step.

		if ( uniqueListSize != targets.length )
		{
			int addedCount = 0;
			String [] uniqueList = new String[ uniqueListSize ];
			for ( int i = 0; i < targets.length; ++i )
				if ( targets[i] != null )
					uniqueList[ addedCount++ ] = targets[i];

			targets = uniqueList;
		}

		// Convert all the user IDs back to the
		// original player names so that the results
		// are easy to understand for the user.

		for ( int i = 0; i < targets.length; ++i )
			targets[i] = getPlayerName( targets[i] ) == null ? targets[i] :
				getPlayerName( targets[i] );

		// Sort the list one more time, this time
		// by player name.

		Arrays.sort( targets );

		// Parsing complete.  Return the list of
		// unique targets.

		return targets;
	}

	public final void downloadAdventureOverride()
	{
		try
		{
			for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
			{
				updateDisplay( "Downloading " + OVERRIDE_DATA[i] + "..." );

				BufferedReader reader = KoLDatabase.getReader(
					"http://svn.sourceforge.net/viewvc/*checkout*/kolmafia/src/data/" + OVERRIDE_DATA[i] );

				File output = new File( "data/" + OVERRIDE_DATA[i] );
				if ( output.exists() )
					output.delete();

				output.createNewFile();

				String line;
				PrintStream writer = new LogStream( output );

				while ( (line = reader.readLine()) != null )
					writer.println( line );

				writer.close();
				reader.close();
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			updateDisplay( ERROR_STATE, "Subversion service access failed.  Try again later." );
			return;
		}

		updateDisplay( "Please restart KoLmafia to complete the update." );
	}

	public static boolean isRunningBetweenBattleChecks()
	{	return recoveryActive;
	}

	public void runBetweenBattleChecks( boolean isFullCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( recoveryActive || refusesContinue() )
			return;

		recoveryActive = true;

		// First, run the between battle script defined by the
		// user, which may make it so that none of the built
		// in behavior needs to run.

		String scriptPath = StaticEntity.getProperty( "betweenBattleScript" );

		if ( !scriptPath.equals( "" ) )
			DEFAULT_SHELL.executeLine( scriptPath );

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isFullCheck )
		{
			MoodSettings.execute();
			recoverHP();
			recoverMP();
			SpecialOutfit.restoreCheckpoint( true );
		}

		recoveryActive = false;

		int haltTolerance = (int)( StaticEntity.getFloatProperty( "battleStop" ) * (float) KoLCharacter.getMaximumHP() );
		if ( haltTolerance >= 0 && KoLCharacter.getCurrentHP() <= haltTolerance )
			KoLmafia.updateDisplay( ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );

		if ( permitsContinue() && currentIterationString.length() > 0 )
		{
			updateDisplay( currentIterationString );
			currentIterationString = "";
		}
	}

	public void startRelayServer()
	{
		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 100 );

		if ( !LocalRelayServer.isRunning() )
			return;

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + (KoLRequest.isCompactMode ? "/main_c.html" : "/main.html") );
	}

	public void launchSimulator()
	{
		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 100 );

		if ( !LocalRelayServer.isRunning() )
			return;

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/KoLmafia/simulator/index.html" );
	}

	public static final void declareWorldPeace()
	{
		commandQueue.clear();
		updateDisplay( ABORT_STATE, "KoLmafia declares world peace." );
	}

	public boolean shouldMakeConflictingRequest()
	{	return currentRequest == null || !(currentRequest instanceof FightRequest) || currentRequest.getAdventuresUsed() == 1;
	}

	public void removeAllItemsFromStore()
	{
		(new StoreManageRequest( this )).run();

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && permitsContinue(); ++i )
			(new StoreManageRequest( this, sold[i].getItemID() )).run();

		updateDisplay( "Store emptying complete." );
	}

	/**
	 * Hosts a massive sale on the items currently in your store.
	 * Utilizes the "minimum meat" principle.
	 */

	public void makeEndOfRunSaleRequest()
	{
		if ( !KoLCharacter.canInteract() )
		{
			updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
			return;
		}

		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			"Are you sure you'd like to host an end-of-run sale?", "MASSIVE SALE", JOptionPane.YES_NO_OPTION ) )
				return;

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		AdventureResult [] items = new AdventureResult[ inventory.size() ];
		inventory.toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		// Only place items in the mall which are not
		// sold in NPC stores -- everything else, make
		// sure you autosell.

		for ( int i = 0; i < items.length; ++i )
		{
			switch ( items[i].getItemID() )
			{
				case ItemCreationRequest.MEAT_PASTE:
				case ItemCreationRequest.MEAT_STACK:
				case ItemCreationRequest.DENSE_STACK:
					autosell.add( items[i] );

				default:
					int standardPrice = TradeableItemDatabase.getPriceByID( items[i].getItemID() );
					if ( standardPrice > 0 )
					{
						if ( NPCStoreDatabase.contains( items[i].getName(), false ) )
							autosell.add( items[i] );
						else
							automall.add( items[i] );
					}
			}
		}

		// Now, place all the items in the mall at the
		// maximum possible price.  This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, autosell.toArray(), AutoSellRequest.AUTOSELL )).run();

		if ( automall.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, automall.toArray(), AutoSellRequest.AUTOMALL )).run();

		// Now, remove all the items that you intended
		// to remove from the store due to pricing issues.

		if ( permitsContinue() )
			priceItemsAtLowestPrice();

		updateDisplay( "Undercutting sale complete." );
	}

	protected void handleAscension()
	{
		MoodSettings.setMood( "apathetic" );

		refreshSession();
		resetSession();

		sessionStream.println();
		sessionStream.println();
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println( "           Beginning New Ascension           " );
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println();

		enableDisplay();
	}

	public void loadPreferredBrowser()
	{
		if ( StaticEntity.getBooleanProperty( "defaultToRelayBrowser" ) )
			startRelayServer();
		else
			SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class ) );
	}
}
