/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class KoLSettings extends Properties implements KoLConstants
{
	private boolean initializingDefaults = false;

	private static final TreeMap CLIENT_SETTINGS = new TreeMap();
	private static final TreeMap PLAYER_SETTINGS = new TreeMap();

	public static final String [] COMMON_JUNK =
	{
		// Items which usually get autosold by people, regardless of the situation.
		// This includes the various meat combinables, sewer items, and stat boosters.

		"meat paste", "meat stack", "dense meat stack", "twinkly powder",
		"seal-clubbing club", "seal tooth", "helmet turtle", "scroll of turtle summoning", "pasta spoon", "ravioli hat", "saucepan", "disco mask", "mariachi pants",
		"moxie weed", "strongness elixir", "magicalness-in-a-can", "enchanted barbell", "concentrated magicalness pill", "giant moxie weed", "extra-strength strongness elixir", "jug-o-magicalness", "suntan lotion of moxiousness",

		// Next, some common drops in low level areas that are farmed for other
		// reasons other than those items.

		"Mad Train wine", "ice-cold fotie", "ice-cold Willer", "ice-cold Sir Schlitz", "bowl of cottage cheese", "Knob Goblin firecracker",
		"Knob Goblin pants", "Knob Goblin scimitar", "viking helmet", "bar skin", "spooky shrunken head", "dried face", "barskin hat", "spooky stick",
		"batgut", "bat guano", "ratgut", "briefcase", "taco shell", "uncooked chorizo", "Gnollish plunger", "gnoll teeth", "gnoll lips", "Gnollish toolbox",

		// Next, some common drops in medium level areas that are also farmed for
		// other reasons beyond these items.

		"hill of beans", "Knob Goblin love potion", "Knob Goblin steroids", "Imp Ale", "hot wing", "evil golden arch", "leather mask",
		"necklace chain", "hemp bracelet", "piercing post", "phat turquoise bead", "carob chunks", "Feng Shui for Big Dumb Idiots", "homoerotic frat-paddle",
		"crowbarrr", "sunken chest", "barrrnacle", "safarrri hat", "arrrgyle socks", "snakehead charrrm", "charrrm", "leotarrrd", "pirate pelvis",
		"grave robbing shovel", "ghuol ears", "ghuol egg", "ghuol guolash", "lihc eye",
		"mind flayer corpse", "royal jelly", "goat beard", "sabre teeth", "t8r tots", "pail", "Trollhouse cookies", "Spam Witch sammich",
		"white satin pants", "white chocolate chips", "catgut", "white snake skin", "mullet wig",

		// High level area item drops which tend to be autosold or auto-used.

		"cocoa eggshell fragment", "glowing red eye", "amulet of extreme plot significance", "Penultimate Fantasy chest",
		"Warm Subject gift certificate", "disturbing fanfic", "probability potion", "procrastination potion", "Mick's IcyVapoHotness Rub"
	};

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
	private File junkItemsFile;
	private String noExtensionName;

	/**
	 * Constructs a settings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	characterName	The name of the character this settings file represents
	 */

	public KoLSettings( String characterName )
	{
		this.noExtensionName = KoLCharacter.baseUserName( characterName );
		this.settingsFile = new File( "settings/prefs_" + noExtensionName + ".txt" );
		this.junkItemsFile = new File( "settings/junk_" + noExtensionName + ".txt" );

		loadSettings();
		ensureDefaults();
	}

	public static boolean isGlobalProperty( String name )
	{
		return CLIENT_SETTINGS.containsKey( name ) || name.startsWith( "saveState" ) || name.startsWith( "displayName" ) ||
			name.startsWith( "getBreakfast" ) || name.startsWith( "loginScript" ) || name.startsWith( "autoPlant" ) || name.startsWith( "visitRumpus" ) ||
			name.startsWith( "initialFrames" ) || name.startsWith( "initialDesktop" );
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
		return value == null ? "" : RequestEditorKit.getUnicode( value );
	}

	public Object setProperty( String name, String value )
	{
		if ( value == null )
			return "";

		boolean isGlobalProperty = isGlobalProperty( name );

		if ( isGlobalProperty && (GLOBAL_SETTINGS == null || this != GLOBAL_SETTINGS) )
			return GLOBAL_SETTINGS.setProperty( name, value );
		else if ( !isGlobalProperty && this == GLOBAL_SETTINGS )
			return "";

		// All tests passed.  Now, go ahead and execute the
		// set property and return the old value.

		String oldValue = getProperty( name );
		value = RequestEditorKit.getEntities( value );

		if ( oldValue != null && oldValue.equals( value ) )
			return oldValue;

		super.setProperty( name, value );
		saveSettings();
		return oldValue == null ? "" : oldValue;
	}

	public void saveJunkItemList()
	{
		if ( junkItemsFile.exists() )
			junkItemsFile.delete();

		if ( KoLCharacter.baseUserName().equals( "GLOBAL" )	)
			return;

		try
		{
			junkItemsFile.createNewFile();
			LogStream ostream = new LogStream( junkItemsFile );

			ostream.println( "[junk items]" );
			for ( int i = 0; i < junkItemList.size(); ++i )
				ostream.println( ((AdventureResult)junkItemList.get(i)).getName() );

			ostream.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public void saveSettings()
	{
		if ( initializingDefaults )
			return;

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

			ostream.reset();

			for ( int i = 0; i < lines.length; ++i )
			{
				if ( lines[i].startsWith( "#" ) )
					continue;

				ostream.write( lines[i].getBytes() );
				ostream.write( LINE_BREAK.getBytes() );
			}

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

			if ( this == GLOBAL_SETTINGS )
				return;

			junkItemList.clear();

			if ( !junkItemsFile.exists() )
			{
				for ( int i = 0; i < COMMON_JUNK.length; ++i )
					junkItemList.add( new AdventureResult( COMMON_JUNK[i], 1, false ) );

				saveJunkItemList();
				return;
			}

			istream = new FileInputStream( junkItemsFile );
			BufferedReader reader = new BufferedReader( new InputStreamReader( istream ) );

			String line;

			while ( (line = reader.readLine()) != null && !line.equals( "[junk items]" ) );
			while ( line != null && (line = reader.readLine()) != null )
			{
				if ( line.equals( "" ) )
					continue;

				junkItemList.add( new AdventureResult( line, 1, false ) );
			}

			reader.close();
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
		CLIENT_SETTINGS.put( "assumeInfiniteNPCItems", "false" );
		CLIENT_SETTINGS.put( "autoLogin", "" );
		CLIENT_SETTINGS.put( "autoPlantHardcore", "false" );
		CLIENT_SETTINGS.put( "autoPlantSoftcore", "false" );
		CLIENT_SETTINGS.put( "autoRepairBoxes", "false" );
		CLIENT_SETTINGS.put( "autoRetrieveScrewdriver", "false" );
		CLIENT_SETTINGS.put( "autoSatisfyWithMall", "false" );
		CLIENT_SETTINGS.put( "autoSatisfyWithNPCs", "true" );
		CLIENT_SETTINGS.put( "autoSatisfyWithStash", "false" );
		CLIENT_SETTINGS.put( "autoSetConditions", "true" );
		CLIENT_SETTINGS.put( "breakfastSoftcore", "Summon Snowcone,Summon Hilarious Objects,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "breakfastHardcore", "Summon Snowcone,Advanced Saucecrafting,Pastamastery,Advanced Cocktailcrafting" );
		CLIENT_SETTINGS.put( "browserBookmarks", "" );
		CLIENT_SETTINGS.put( "chatStyle", "0" );
		CLIENT_SETTINGS.put( "closeLastFrameAction", "0" );
		CLIENT_SETTINGS.put( "cloverProtectActive", "false" );
		CLIENT_SETTINGS.put( "commandLineNamespace", "" );
		CLIENT_SETTINGS.put( "createWithoutBoxServants", "false" );
		CLIENT_SETTINGS.put( "defaultBorderColor", "blue" );
		CLIENT_SETTINGS.put( "defaultDropdown1", "0" );
		CLIENT_SETTINGS.put( "defaultDropdown2", "1" );
		CLIENT_SETTINGS.put( "defaultLimit", "5" );
		CLIENT_SETTINGS.put( "defaultLoginServer", "1" );
		CLIENT_SETTINGS.put( "eSoluScriptType", "0" );
		CLIENT_SETTINGS.put( "fontSize", "3" );
		CLIENT_SETTINGS.put( "guiUsesOneWindow", "false" );
		CLIENT_SETTINGS.put( "highlightList", "" );
		CLIENT_SETTINGS.put( "http.proxyHost", "" );
		CLIENT_SETTINGS.put( "http.proxyPort", "" );
		CLIENT_SETTINGS.put( "http.proxyUser", "" );
		CLIENT_SETTINGS.put( "http.proxyPassword", "" );
		CLIENT_SETTINGS.put( "ignoreGreenEvents", "false" );
		CLIENT_SETTINGS.put( "initialDesktop", "AdventureFrame,MallSearchFrame,SkillBuffFrame,HagnkStorageFrame" );
		CLIENT_SETTINGS.put( "initialFrames", "EventsFrame" );
		CLIENT_SETTINGS.put( "lastUsername", "" );
		CLIENT_SETTINGS.put( "loginServerName", "" );
		CLIENT_SETTINGS.put( "loginWindowLogo", "lantern.jpg" );

		CLIENT_SETTINGS.put( "logAcquiredItems", "true" );
		CLIENT_SETTINGS.put( "logBattleAction", "true" );
		CLIENT_SETTINGS.put( "logGainMessages", "true" );
		CLIENT_SETTINGS.put( "logReverseOrder", "false" );
		CLIENT_SETTINGS.put( "logStatGains", "true" );
		CLIENT_SETTINGS.put( "logStatusEffects", "false" );

		CLIENT_SETTINGS.put( "proxySet", "false" );
		CLIENT_SETTINGS.put( "http.proxyHost", "" );
		CLIENT_SETTINGS.put( "http.proxyPort", "" );
		CLIENT_SETTINGS.put( "http.proxyUser", "" );
		CLIENT_SETTINGS.put( "http.proxyPassword", "" );

		CLIENT_SETTINGS.put( "previousNotifyList", "<>" );
		CLIENT_SETTINGS.put( "previousUpdateVersion", VERSION_NAME );
		CLIENT_SETTINGS.put( "relayAddsGraphicalCLI", "false" );
		CLIENT_SETTINGS.put( "relayAddsKoLSimulator", "false" );
		CLIENT_SETTINGS.put( "relayAddsQuickScripts", "false" );
		CLIENT_SETTINGS.put( "relayAddsRestoreLinks", "true" );
		CLIENT_SETTINGS.put( "relayAddsUpArrowLinks", "true" );
		CLIENT_SETTINGS.put( "relayAlwaysBuysGum", "true" );
		CLIENT_SETTINGS.put( "relayHidesJunkMallItems", "true" );
		CLIENT_SETTINGS.put( "relayMaintainsMoods", "false" );
		CLIENT_SETTINGS.put( "relayBrowserOnly", "false" );
		CLIENT_SETTINGS.put( "relayTextualizesEffects", "false" );
		CLIENT_SETTINGS.put( "relayUsesIntegratedChat", "true" );
		CLIENT_SETTINGS.put( "relayBrowserOnly", "false" );
		CLIENT_SETTINGS.put( "saveState", "" );
		CLIENT_SETTINGS.put( "saveStateActive", "" );
		CLIENT_SETTINGS.put( "scriptButtonPosition", "0" );
		CLIENT_SETTINGS.put( "scriptList", "restore hp | restore mp" );
		CLIENT_SETTINGS.put( "showAllRequests", "false" );
		CLIENT_SETTINGS.put( "showClosetIngredients", "false" );
		CLIENT_SETTINGS.put( "showStashIngredients", "false" );
		CLIENT_SETTINGS.put( "sortAdventures", "false" );
		CLIENT_SETTINGS.put( "swingLookAndFeel", "" );
		CLIENT_SETTINGS.put( "toolbarPosition", "1" );

		CLIENT_SETTINGS.put( "useSystemTrayIcon", String.valueOf( System.getProperty( "os.name" ).startsWith( "Windows" ) ) );
		CLIENT_SETTINGS.put( "usePopupContacts", "1" );
		CLIENT_SETTINGS.put( "useTabbedChat", "1" );
		CLIENT_SETTINGS.put( "useToolbars", "true" );
		CLIENT_SETTINGS.put( "visitRumpusHardcore", "false" );
		CLIENT_SETTINGS.put( "visitRumpusSoftcore", "false" );

		PLAYER_SETTINGS.put( "autoRetrieveWorthless", "true" );
		PLAYER_SETTINGS.put( "battleAction", "attack with weapon" );
		PLAYER_SETTINGS.put( "betweenBattleScript", "" );
		PLAYER_SETTINGS.put( "buffBotCasting", "" );
		PLAYER_SETTINGS.put( "buffBotMessageDisposal", "0" );
		PLAYER_SETTINGS.put( "chosenTrip", "" );
		PLAYER_SETTINGS.put( "currentMood", "default" );
		PLAYER_SETTINGS.put( "defaultAutoAttack", "0" );
		PLAYER_SETTINGS.put( "hpAutoRecovery", "-0.1" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryTarget", "1.0" );
		PLAYER_SETTINGS.put( "hpAutoRecoveryItems", "cannelloni cocoon;scroll of drastic healing;tongue of the walrus;lasagna bandages" );
		PLAYER_SETTINGS.put( "hpThreshold", "-0.1" );
		PLAYER_SETTINGS.put( "invalidBuffMessage", "You sent an amount which does not correspond to a valid buff amount." );
		PLAYER_SETTINGS.put( "lastAdventure", "" );
		PLAYER_SETTINGS.put( "lastBreakfast", "19691231" );
		PLAYER_SETTINGS.put( "lastMessageId", "" );
		PLAYER_SETTINGS.put( "luckySewerAdventure", "stolen accordion" );
		PLAYER_SETTINGS.put( "mpAutoRecovery", "0.0" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryTarget", "0.3" );
		PLAYER_SETTINGS.put( "mpAutoRecoveryItems", "phonics down;knob goblin superseltzer;mountain stream soda;magical mystery juice;knob goblin seltzer;soda water" );
		PLAYER_SETTINGS.put( "mpThreshold", "-0.1" );
		PLAYER_SETTINGS.put( "plantingDay", "-1" );
		PLAYER_SETTINGS.put( "plantingDate", "" );
		PLAYER_SETTINGS.put( "plantingLength", "" );
		PLAYER_SETTINGS.put( "plantingScript", "" );
		PLAYER_SETTINGS.put( "protectAgainstOverdrink", "true" );
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
		PLAYER_SETTINGS.put( "choiceAdventure6", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure7", "1" );
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
		PLAYER_SETTINGS.put( "choiceAdventure77", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure78", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure79", "1" );
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
		PLAYER_SETTINGS.put( "choiceAdventure105", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure106", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure107", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure108", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure109", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure110", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure111", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure112", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure113", "3" );
		PLAYER_SETTINGS.put( "choiceAdventure114", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure115", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure116", "4" );
		PLAYER_SETTINGS.put( "choiceAdventure117", "1" );
		PLAYER_SETTINGS.put( "choiceAdventure118", "2" );
		PLAYER_SETTINGS.put( "choiceAdventure120", "1" );
	}

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private void ensureDefaults()
	{
		initializingDefaults = true;

		// If this is the set of global settings, be sure
		// to initialize the global settings.

		if ( noExtensionName.equals( "GLOBAL" ) )
		{
			Object [] keys = CLIENT_SETTINGS.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
				if ( !containsKey( keys[i] ) )
					super.setProperty( (String) keys[i], (String) CLIENT_SETTINGS.get( keys[i] ) );

			initializingDefaults = false;
			return;
		}

		// Otherwise, initialize the client-specific settings.
		// No global settings will be loaded.

		Object [] keys = PLAYER_SETTINGS.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !containsKey( keys[i] ) )
				super.setProperty( (String) keys[i], (String) PLAYER_SETTINGS.get( keys[i] ) );

		initializingDefaults = false;
		saveSettings();
	}
}
