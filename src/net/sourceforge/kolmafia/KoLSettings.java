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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * An extension of {@link java.util.Properties} which handles all the
 * user settings of <code>KoLmafia</code>.  In order to maintain issues
 * involving compatibility (J2SE 1.4 does not support XML output directly),
 * all data is written using {@link java.util.Properties#store(OutputStream,String)}.
 */

public class KoLSettings extends Properties implements UtilityConstants, KoLConstants
{
	private static final TreeMap CLIENT_SETTINGS = new TreeMap();
	private static final TreeMap PLAYER_SETTINGS = new TreeMap();

	static
	{
		// Renaming data files to make then easier to find for most
		// people (so they aren't afraid to open them).

		initializeMaps();
		StaticEntity.renameDataFiles( "kcs", "prefs" );

		// Delete any temporary files that were created in the process
		// of a bad exit.

		if ( !SETTINGS_DIRECTORY.exists() )
			SETTINGS_DIRECTORY.mkdirs();

		if ( !SCRIPT_DIRECTORY.exists() )
			SCRIPT_DIRECTORY.mkdirs();

		File [] files = SETTINGS_DIRECTORY.listFiles();
		for ( int i = 0; i < files.length; ++i )
			if ( files[i].getPath().endsWith( ".tmp" ) )
				files[i].delete();
	}

	public static final KoLSettings GLOBAL_SETTINGS = new KoLSettings( "" );

	private File settingsFile;
	private String noExtensionName;

	/**
	 * Constructs a settings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	noExtensionName	The name of the character this settings file represents
	 */

	public KoLSettings( String characterName )
	{
		this.noExtensionName = KoLCharacter.baseUserName( characterName );
		this.settingsFile = new File( "settings/prefs_" + noExtensionName + ".txt" );

		loadSettings();
		ensureDefaults();
	}

	public static boolean isGlobalProperty( String name )
	{
		return CLIENT_SETTINGS.containsKey( name ) || name.startsWith( "saveState" ) || name.startsWith( "displayName" ) ||
			name.startsWith( "getBreakfast" ) || name.startsWith( "loginScript" ) || name.startsWith( "initialFrames" ) || name.startsWith( "initialDesktop" );
	}

	public String getProperty( String name )
	{
		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (GLOBAL_SETTINGS == null || this != GLOBAL_SETTINGS) )
		{
			String value = GLOBAL_SETTINGS.getProperty( name );
			return value == null ? "" : value;
		}
		else if ( !isGlobalProperty && this == GLOBAL_SETTINGS )
			return "";

		String value = super.getProperty( name );
		return value == null ? "" : value;
	}

	public Object setProperty( String name, String value )
	{
		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (GLOBAL_SETTINGS == null || this != GLOBAL_SETTINGS) )
			return GLOBAL_SETTINGS.setProperty( name, value );
		else if ( !isGlobalProperty && this == GLOBAL_SETTINGS )
			return "";

		// All tests passed.  Now, go ahead and execute the
		// set property and return the old value.

		String oldValue = getProperty( name );

		if ( oldValue != null && oldValue.equals( value ) )
			return oldValue;

		return super.setProperty( name, value );
	}

	public void saveSettings()
	{
		storeSettings();
		if ( GLOBAL_SETTINGS != null && this != GLOBAL_SETTINGS )
			GLOBAL_SETTINGS.saveSettings();
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	private void loadSettings()
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !settingsFile.exists() )
				return;

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( settingsFile );
			load( istream );
			istream.close();
			istream = null;
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
		}
	}

	private static void initializeMaps()
	{
		// Do not initialize the maps more than once, as this
		// would not serve any purpose.

		CLIENT_SETTINGS.put( "allowGenericUse", "false" );
		CLIENT_SETTINGS.put( "allowStasisTactics", "false" );
		CLIENT_SETTINGS.put( "allowThiefShrugOff", "true" );
		CLIENT_SETTINGS.put( "alwaysGetBreakfast", "false" );
		CLIENT_SETTINGS.put( "areaValidation", "true" );
		CLIENT_SETTINGS.put( "assumeInfiniteNPCItems", "false" );
		CLIENT_SETTINGS.put( "autoCheckpoint", "true" );
		CLIENT_SETTINGS.put( "autoExecuteTimeIn", "true" );
		CLIENT_SETTINGS.put( "autoLogin", "" );
		CLIENT_SETTINGS.put( "autoRepairBoxes", "false" );
		CLIENT_SETTINGS.put( "autoSatisfyWithMall", "false" );
		CLIENT_SETTINGS.put( "autoSatisfyWithNPCs", "true" );
		CLIENT_SETTINGS.put( "autoSatisfyWithStash", "false" );
		CLIENT_SETTINGS.put( "autoSetConditions", "false" );
		CLIENT_SETTINGS.put( "battleStop", "0.0" );
		CLIENT_SETTINGS.put( "breakfastSoftcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "breakfastHardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "browserBookmarks", "" );
		CLIENT_SETTINGS.put( "cacheRelayImages", "false" );
		CLIENT_SETTINGS.put( "chatStyle", "0" );
		CLIENT_SETTINGS.put( "clanRosterHeader", ClanSnapshotTable.getDefaultHeader() );
		CLIENT_SETTINGS.put( "cloverProtectActive", "false" );
		CLIENT_SETTINGS.put( "commandLineNamespace", "" );
		CLIENT_SETTINGS.put( "createWithoutBoxServants", "false" );
		CLIENT_SETTINGS.put( "defaultDropdown1", "0" );
		CLIENT_SETTINGS.put( "defaultDropdown2", "1" );
		CLIENT_SETTINGS.put( "defaultLimit", "5" );
		CLIENT_SETTINGS.put( "defaultLoginServer", "1" );
		CLIENT_SETTINGS.put( "defaultToRelayBrowser", "true" );
		CLIENT_SETTINGS.put( "desiredLookAndFeel", "" );
		CLIENT_SETTINGS.put( "desiredLookAndFeelTitle", String.valueOf( System.getProperty( "os.name" ).startsWith( "Mac" ) ) );
		CLIENT_SETTINGS.put( "eSoluScriptType", "0" );
		CLIENT_SETTINGS.put( "fontSize", "3" );
		CLIENT_SETTINGS.put( "guiUsesOneWindow", "false" );
		CLIENT_SETTINGS.put( "highlightList", "" );
		CLIENT_SETTINGS.put( "http.proxyHost", "" );
		CLIENT_SETTINGS.put( "http.proxyPort", "" );
		CLIENT_SETTINGS.put( "http.proxyUser", "" );
		CLIENT_SETTINGS.put( "http.proxyPassword", "" );
		CLIENT_SETTINGS.put( "initialDesktop", "AdventureFrame,MallSearchFrame,HagnkStorageFrame,SkillBuffFrame" );
		CLIENT_SETTINGS.put( "initialFrames", "EventsFrame" );
		CLIENT_SETTINGS.put( "lastUsername", "" );
		CLIENT_SETTINGS.put( "loadedWikiOnce", "false" );
		CLIENT_SETTINGS.put( "previousNotifyList", "<>" );
		CLIENT_SETTINGS.put( "previousUpdateVersion", VERSION_NAME );
		CLIENT_SETTINGS.put( "proxySet", "false" );
		CLIENT_SETTINGS.put( "relayAddsCustomCombat", "false" );
		CLIENT_SETTINGS.put( "relayAddsGraphicalCLI", "false" );
		CLIENT_SETTINGS.put( "relayAddsKoLSimulator", "false" );
		CLIENT_SETTINGS.put( "relayAddsQuickScripts", "false" );
		CLIENT_SETTINGS.put( "relayAddsRestoreLinks", "true" );
		CLIENT_SETTINGS.put( "relayAddsUpArrowLinks", "true" );
		CLIENT_SETTINGS.put( "relayAddsUseLinks", "true" );
		CLIENT_SETTINGS.put( "relayAlwaysBuysGum", "false" );
		CLIENT_SETTINGS.put( "relayBrowserOnly", "false" );
		CLIENT_SETTINGS.put( "relayRemovesExpensiveItems", "true" );
		CLIENT_SETTINGS.put( "relayRemovesMinpricedItems", "true" );
		CLIENT_SETTINGS.put( "relayRemovesUnrelatedItems", "true" );
		CLIENT_SETTINGS.put( "relayRemovesRunaway", "true" );
		CLIENT_SETTINGS.put( "saveState", "" );
		CLIENT_SETTINGS.put( "scriptButtonPosition", "0" );
		CLIENT_SETTINGS.put( "scriptList", "restore hp | restore mp" );
		CLIENT_SETTINGS.put( "showAllRequests", "false" );
		CLIENT_SETTINGS.put( "showClosetIngredients", "true" );
		CLIENT_SETTINGS.put( "showStashIngredients", "false" );
		CLIENT_SETTINGS.put( "sortAdventures", "false" );
		CLIENT_SETTINGS.put( "toolbarPosition", "1" );
		CLIENT_SETTINGS.put( "trackLocationChanges", "false" );

		CLIENT_SETTINGS.put( "useSystemTrayIcon", String.valueOf( System.getProperty( "os.name" ).startsWith( "Windows" ) ) );
		CLIENT_SETTINGS.put( "usePopupContacts", "1" );
		CLIENT_SETTINGS.put( "useTabbedChat", "1" );
		CLIENT_SETTINGS.put( "useTextHeavySidepane", "true" );
		CLIENT_SETTINGS.put( "useToolbars", "true" );
		CLIENT_SETTINGS.put( "zoneExcludeList", "Holiday,Removed" );

		PLAYER_SETTINGS.put( "battleAction", "attack" );
		PLAYER_SETTINGS.put( "betweenBattleScript", "" );
		PLAYER_SETTINGS.put( "buffBotCasting", "" );
		PLAYER_SETTINGS.put( "buffBotMessageDisposal", "0" );
		PLAYER_SETTINGS.put( "chosenTrip", "" );
		PLAYER_SETTINGS.put( "currentMood", "default" );
		PLAYER_SETTINGS.put( "defaultAutoAttack", "0" );
		PLAYER_SETTINGS.put( "hpAutoRecovery", "-0.1" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryTarget", "1.0" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryItems", "cannelloni cocoon;scroll of drastic healing;tongue of the walrus;lasagna bandages" );
		PLAYER_SETTINGS.put( "invalidBuffMessage", "You sent an amount which does not correspond to a valid buff amount." );
		PLAYER_SETTINGS.put( "lastAdventure", "" );
		PLAYER_SETTINGS.put( "lastBreakfast", "19691231" );
		PLAYER_SETTINGS.put( "lastMessageID", "" );
		PLAYER_SETTINGS.put( "luckySewerAdventure", "stolen accordion" );
		PLAYER_SETTINGS.put( "mpAutoRecovery", "0.0" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryTarget", "0.3" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryItems", "phonics down;knob goblin superseltzer;mountain stream soda;magical mystery juice;knob goblin seltzer;soda water" );
		PLAYER_SETTINGS.put( "plantingDay", "-1" );
		PLAYER_SETTINGS.put( "plantingDate", "" );
		PLAYER_SETTINGS.put( "plantingLength", "" );
		PLAYER_SETTINGS.put( "plantingScript", "" );
		PLAYER_SETTINGS.put( "retrieveContacts", "true" );
		PLAYER_SETTINGS.put( "thanksMessage", "Thank you for the donation!" );
		PLAYER_SETTINGS.put( "trapperOre", "chrome ore" );
		PLAYER_SETTINGS.put( "violetFogGoal", "0" );
		PLAYER_SETTINGS.put( "visibleBrowserInventory", "" );
		PLAYER_SETTINGS.put( "whiteList", "" );

		// These are settings related to the tavern faucet
		// used to make the interface friendlier.

		PLAYER_SETTINGS.put( "lastTavernSquare", "0" );
		PLAYER_SETTINGS.put( "lastTavernAscension", "0" );
		PLAYER_SETTINGS.put( "tavernLayout", "0000000000000000000000000" );

		// Yay for the Louvre.

		PLAYER_SETTINGS.put( "lastLouvreMap", "0" );
		PLAYER_SETTINGS.put( "louvreLayout", "" );
		PLAYER_SETTINGS.put( "louvreDesiredGoal", "0" );
		PLAYER_SETTINGS.put( "louvreGoal", "0" );
		PLAYER_SETTINGS.put( "louvreBoostsLowestStat", "false" );

		// These are settings related to choice adventures.
		// Ensure that they exist, and if they do not, load
		// them to their default settings.

		PLAYER_SETTINGS.put( "choiceAdventure2", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure3", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure4", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure5", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure7", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure8", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure9", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure10", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure11", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure12", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure14", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure15", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure16", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure17", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure18", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure19", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure20", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure21", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure22", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure23", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure24", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure25", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure26", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure27", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure28", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure29", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure40", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure41", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure42", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure45", "0" );
		PLAYER_SETTINGS.put( "choiceAdventure46", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure47", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure71", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure72", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure73", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure74", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure75", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure76", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure77", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure78", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure79", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure80", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure81", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure82", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure83", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure84", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure85", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure86", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure87", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure88", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure89", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure90", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure91", "2" );
	}

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private void ensureDefaults()
	{
		// If this is the set of global settings, be sure
		// to initialize the global settings.

		if ( noExtensionName.equals( "GLOBAL" ) )
		{
			Object [] keys = CLIENT_SETTINGS.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
				if ( !containsKey( keys[i] ) )
					super.setProperty( (String) keys[i], (String) CLIENT_SETTINGS.get( keys[i] ) );

			return;
		}

		// Otherwise, initialize the client-specific settings.
		// No global settings will be loaded.

		Object [] keys = PLAYER_SETTINGS.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !containsKey( keys[i] ) )
				super.setProperty( (String) keys[i], (String) PLAYER_SETTINGS.get( keys[i] ) );
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 */

	private void storeSettings()
	{
		try
		{
			if ( settingsFile.exists() )
				settingsFile.delete();

			// Determine the contents of the file by
			// actually printing them.

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			store( ostream, VERSION_NAME );

			String [] lines = ostream.toString().split( LINE_BREAK );
			Arrays.sort( lines );

			settingsFile.createNewFile();
			ostream.writeTo( new FileOutputStream( settingsFile ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}
}
