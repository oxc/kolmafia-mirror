/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import edu.stanford.ejalbert.BrowserLauncher;

import java.awt.Frame;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.ArtistRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ChefStaffRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HiddenCityRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.PyroRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.StarChartRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.SuspiciousGuyRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;

import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.MineDecorator;

public abstract class StaticEntity
{
	private static final Pattern NEWSKILL1_PATTERN = Pattern.compile( "<td>You learn a new skill: <b>(.*?)</b>" );
	private static final Pattern NEWSKILL2_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern NEWSKILL3_PATTERN = Pattern.compile( "You acquire a skill: +<[bB]>(.*?)</[bB]>" );

	private static KoLmafia client;
	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;

	public static final ArrayList existingPanels = new ArrayList();
	private static ActionPanel[] panelArray = new GenericPanel[ 0 ];

	public static final String getVersion()
	{
		if ( KoLConstants.REVISION == null )
		{
			return KoLConstants.VERSION_NAME;
		}

		int colonIndex = KoLConstants.REVISION.indexOf( ":" );
		if ( colonIndex != -1 )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, colonIndex );
		}

		if ( KoLConstants.REVISION.endsWith( "M" ) )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, KoLConstants.REVISION.length() - 1 );
		}

		return "KoLmafia r" + KoLConstants.REVISION;
	}

	public static final void setClient( final KoLmafia client )
	{
		StaticEntity.client = client;
	}

	public static final KoLmafia getClient()
	{
		return StaticEntity.client;
	}

	public static final void registerPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.add( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.remove( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final boolean isHeadless()
	{
		return StaticEntity.client instanceof KoLmafiaCLI;
	}

	public static final ActionPanel[] getExistingPanels()
	{
		synchronized ( StaticEntity.existingPanels )
		{
			boolean needsRefresh = StaticEntity.panelArray.length != StaticEntity.existingPanels.size();

			if ( !needsRefresh )
			{
				for ( int i = 0; i < StaticEntity.panelArray.length && !needsRefresh; ++i )
				{
					needsRefresh = StaticEntity.panelArray[ i ] != StaticEntity.existingPanels.get( i );
				}
			}

			if ( needsRefresh )
			{
				StaticEntity.panelArray = new ActionPanel[ StaticEntity.existingPanels.size() ];
				StaticEntity.existingPanels.toArray( StaticEntity.panelArray );
			}

			return StaticEntity.panelArray;
		}
	}

	public static final boolean usesSystemTray()
	{
		if ( StaticEntity.usesSystemTray == 0 )
		{
			StaticEntity.usesSystemTray =
				System.getProperty( "os.name" ).startsWith( "Windows" ) && Preferences.getBoolean( "useSystemTrayIcon" ) ? 1 : 2;
		}

		return StaticEntity.usesSystemTray == 1;
	}

	public static final boolean usesRelayWindows()
	{
		if ( StaticEntity.usesRelayWindows == 0 )
		{
			StaticEntity.usesRelayWindows = Preferences.getBoolean( "useRelayWindows" ) ? 1 : 2;
		}

		return StaticEntity.usesRelayWindows == 1;
	}

	public static final void openSystemBrowser( final String location )
	{
		( new SystemBrowserThread( location ) ).start();
	}

	private static String currentBrowser = null;

	private static class SystemBrowserThread
		extends Thread
	{
		private final String location;

		public SystemBrowserThread( final String location )
		{
			super( "SystemBrowserThread@" + location );
			this.location = location;
		}

		public void run()
		{
			String preferredBrowser = Preferences.getString( "preferredWebBrowser" );

			if ( currentBrowser == null || !currentBrowser.equals( preferredBrowser ) )
			{
				System.setProperty( "os.browser", preferredBrowser );
				currentBrowser = preferredBrowser;
			}

			BrowserLauncher.openURL( this.location );
		}
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays the given location, relative to the KoL
	 * home directory for the current session. This should be called whenever <code>RequestFrame</code>s need to be
	 * created in order to keep code modular.
	 */

	public static final void openRequestFrame( final String location )
	{
		GenericRequest request = RequestEditorKit.extractRequest( location );

		if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
		{
			DescriptionFrame.showRequest( request );
			return;
		}

		Frame[] frames = Frame.getFrames();
		RequestFrame requestHolder = null;

		for ( int i = frames.length - 1; i >= 0; --i )
		{
			if ( frames[ i ].getClass() == RequestFrame.class && ( (RequestFrame) frames[ i ] ).hasSideBar() )
			{
				requestHolder = (RequestFrame) frames[ i ];
			}
		}

		if ( requestHolder == null )
		{
			RequestSynchFrame.showRequest( request );
			return;
		}

		if ( !location.equals( "main.php" ) )
		{
			requestHolder.refresh( request );
		}
	}

	public static final void externalUpdate( final String location, final String responseText )
	{
		if ( location.startsWith( "craft.php" ) )
		{
			CreateItemRequest.parseCrafting( location, responseText );
		}

		if ( location.startsWith( "account.php" ) )
		{
			if ( location.indexOf( "&ajax" ) != -1 )
			{
				AccountRequest.parseAjax( location );
			}
			else
			{
				boolean wasHardcore = KoLCharacter.isHardcore();
				boolean hadRestrictions = !KoLCharacter.canEat() || !KoLCharacter.canDrink();
	
				AccountRequest.parseAccountData( responseText );
	
				if ( wasHardcore && !KoLCharacter.isHardcore() )
				{
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "dropped hardcore" );
					RequestLogger.updateSessionLog();
				}
	
				if ( hadRestrictions && KoLCharacter.canEat() && KoLCharacter.canDrink() )
				{
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "dropped consumption restrictions" );
					RequestLogger.updateSessionLog();
				}
			}
		}

		if ( location.startsWith( "questlog.php" ) )
		{
			QuestLogRequest.registerQuests( true, location, responseText );
		}

		// Keep your current equipment and familiars updated, if you
		// visit the appropriate pages.

		if ( location.startsWith( "inventory.php" ) &&
		     ( location.indexOf( "which=2" ) != -1 || location.indexOf( "curequip=1" ) != -1 ) )
		{
			// If KoL is showing us our current equipment, parse it.
			EquipmentRequest.parseEquipment( location, responseText );
		}

		if ( location.startsWith( "inv_equip.php" ) &&
		     location.indexOf( "ajax=1" ) != -1 )
		{
			// If we are changing equipment via a chat command,
			// try to deduce what changed.
			EquipmentRequest.parseEquipmentChange( location, responseText );
		}
		
		if ( location.startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( responseText );
		}

		if ( location.startsWith( "familiar.php" ) && location.indexOf( "ajax=1" ) == -1)
		{
			FamiliarData.registerFamiliarData( responseText );
		}

		if ( location.indexOf( "charsheet.php" ) != -1 )
		{
			CharSheetRequest.parseStatus( responseText );
		}

		if ( location.startsWith( "sellstuff_ugly.php" ) )
		{
			SellStuffRequest.parseAutosell( location, responseText );
		}

		// See if the request would have used up an item.

		if ( location.indexOf( "inventory.php" ) != -1 && location.indexOf( "action=message" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		if ( ( location.indexOf( "inv_eat.php" ) != -1 || location.indexOf( "inv_booze.php" ) != -1 || location.indexOf( "inv_use.php" ) != -1 || location.indexOf( "inv_familiar.php" ) != -1 ) && location.indexOf( "whichitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		if ( ( location.indexOf( "multiuse.php" ) != -1 || location.indexOf( "skills.php" ) != -1 ) && location.indexOf( "useitem" ) != -1 )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		if ( location.startsWith( "sushi.php" ) )
		{
			SushiRequest.parseConsumption( location, responseText );
		}

		if ( location.startsWith( "hermit.php" ) )
		{
			HermitRequest.parseHermitTrade( location, responseText );
		}

		if ( location.startsWith( "hiddencity.php" ) )
		{
			HiddenCityRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "manor3" ) )
		{
			WineCellarRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "mining.php" ) )
		{
			MineDecorator.parseResponse( location, responseText );
		}

		if ( location.startsWith( "pyramid.php" ) )
		{
			PyramidRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "town_wrong.php" ) )
		{
			ArtistRequest.parseResponse( location, responseText );
			SuspiciousGuyRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "town_right.php" ) )
		{
			GourdRequest.parseResponse( location, responseText );
		}

		if ( location.indexOf( "action=pyro" ) != -1 )
		{
			PyroRequest.parseResponse( location, responseText );
		}

		if ( location.indexOf( "action=makestaff" ) != -1 )
		{
			ChefStaffRequest.parseCreation( location, responseText );
		}

		if ( location.startsWith( "starchart.php" ) )
		{
			StarChartRequest.parseCreation( location, responseText );
		}

		// See if the person learned a new skill from using a
		// mini-browser frame.

		Matcher learnedMatcher = StaticEntity.NEWSKILL1_PATTERN.matcher( responseText );
		if ( learnedMatcher.find() )
		{
			String skillName = learnedMatcher.group( 1 );

			KoLCharacter.addAvailableSkill( skillName );
			KoLCharacter.addDerivedSkills();
			KoLConstants.usableSkills.sort();
			ConcoctionDatabase.refreshConcoctions();
		}

		learnedMatcher = StaticEntity.NEWSKILL3_PATTERN.matcher( responseText );
		if ( learnedMatcher.find() )
		{
			String skillName = learnedMatcher.group( 1 );

			KoLCharacter.addAvailableSkill( skillName );
			KoLCharacter.addDerivedSkills();
			KoLConstants.usableSkills.sort();
			ConcoctionDatabase.refreshConcoctions();
		}

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.indexOf( "You leargn a new skill." ) != -1 )
		{
			learnedMatcher = StaticEntity.NEWSKILL2_PATTERN.matcher( location );
			if ( learnedMatcher.find() )
			{
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( StringUtilities.parseInt( learnedMatcher.group( 1 ) ) ) );
			}
		}

		// Player vs. player results should be recorded to the
		// KoLmafia log.

		if ( location.startsWith( "pvp.php" ) && location.indexOf( "action" ) != -1 )
		{
			PvpManager.processOffenseContests( responseText );
		}

		// If this is the hippy store, check to see if any of the
		// items offered in the hippy store are special.

		if ( location.startsWith( "store.php" ) && location.indexOf( "whichstore=h" ) != -1 && Preferences.getInteger( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
		{
			String side = "none";

			if ( responseText.indexOf( "peach" ) != -1 &&
			     responseText.indexOf( "pear" ) != -1 &&
			     responseText.indexOf( "plum" ) != -1 )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "hippy";
			}
			else if ( responseText.indexOf( "bowl of rye sprouts" ) != -1 &&
				  responseText.indexOf( "cob of corn" ) != -1 &&
				  responseText.indexOf( "juniper berries" ) != -1 )
			{
				Preferences.setInteger( "lastFilthClearance", KoLCharacter.getAscensions() );
				side = "fratboy";
			}
			Preferences.setString( "currentHippyStore", side );
			Preferences.setString( "sidequestOrchardCompleted", side );
		}
	}

	public static final boolean executeCountdown( final String message, final int seconds )
	{
		PauseObject pauser = new PauseObject();

		StringBuffer actualMessage = new StringBuffer( message );

		for ( int i = seconds; i > 0 && KoLmafia.permitsContinue(); --i )
		{
			boolean shouldDisplay = false;

			// If it's the first count, then it should definitely be shown
			// for the countdown.

			if ( i == seconds )
			{
				shouldDisplay = true;
			}
			else if ( i >= 1800 )
			{
				shouldDisplay = i % 600 == 0;
			}
			else if ( i >= 600 )
			{
				shouldDisplay = i % 300 == 0;
			}
			else if ( i >= 300 )
			{
				shouldDisplay = i % 120 == 0;
			}
			else if ( i >= 60 )
			{
				shouldDisplay = i % 60 == 0;
			}
			else if ( i >= 15 )
			{
				shouldDisplay = i % 15 == 0;
			}
			else if ( i >= 5 )
			{
				shouldDisplay = i % 5 == 0;
			}
			else
			{
				shouldDisplay = true;
			}

			// Only display the message if it should be displayed based on
			// the above checks.

			if ( shouldDisplay )
			{
				actualMessage.setLength( message.length() );

				if ( i >= 60 )
				{
					int minutes = i / 60;
					actualMessage.append( minutes );
					actualMessage.append( minutes == 1 ? " minute" : " minutes" );

					if ( i % 60 != 0 )
					{
						actualMessage.append( ", " );
					}
				}

				if ( i % 60 != 0 )
				{
					actualMessage.append( i % 60 );
					actualMessage.append( i % 60 == 1 ? " second" : " seconds" );
				}

				actualMessage.append( "..." );
				KoLmafia.updateDisplay( actualMessage.toString() );
			}

			pauser.pause( 1000 );
		}

		return KoLmafia.permitsContinue();
	}

	public static final void printStackTrace()
	{
		StaticEntity.printStackTrace( "Forced stack trace" );
	}

	public static final void printStackTrace( final String message )
	{
		try
		{
			throw new Exception( message );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static final void printStackTrace( final Throwable t )
	{
		StaticEntity.printStackTrace( t, "" );
	}

	public static final void printStackTrace( final Throwable t, final String message )
	{
		printStackTrace( t, message, false );
	}

	public static final void printStackTrace( final Throwable t, final String message, boolean printOnlyCause )
	{
		// Next, print all the information to the debug log so that
		// it can be sent.

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		KoLmafia.updateDisplay( "Unexpected error, debug log printed." );

		Throwable cause = t.getCause();

		if ( cause == null || !printOnlyCause )
		{
			StaticEntity.printStackTrace( t, message, System.err );
			StaticEntity.printStackTrace( t, message, RequestLogger.getDebugStream() );
		}

		if ( cause != null )
		{
			StaticEntity.printStackTrace( cause, message, System.err );
			StaticEntity.printStackTrace( cause, message, RequestLogger.getDebugStream() );
		}

		try
		{
			if ( shouldOpenStream )
			{
				RequestLogger.closeDebugLog();
			}
		}
		catch ( Exception e )
		{
			// Okay, since you're in the middle of handling an exception
			// and got a new one, just return from here.
		}
	}

	private static final void printStackTrace( final Throwable t, final String message, final PrintStream ostream )
	{
		ostream.println( t.getClass() + ": " + t.getMessage() );
		t.printStackTrace( ostream );
	}

	public static final void printRequestData( final GenericRequest request )
	{
		if ( request == null )
		{
			return;
		}

		boolean shouldOpenStream = RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "" + request.getClass() + ": " + request.getURLString() );
		RequestLogger.updateDebugLog( KoLConstants.LINE_BREAK_PATTERN.matcher( request.responseText ).replaceAll( "" ) );
		RequestLogger.updateDebugLog();

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final String[] getPastUserList()
	{
		ArrayList pastUserList = new ArrayList();

		String user;
		File[] files = DataUtilities.listFiles( UtilityConstants.SETTINGS_LOCATION );

		for ( int i = 0; i < files.length; ++i )
		{
			user = files[ i ].getName();
			if ( user.startsWith( "GLOBAL" ) || !user.endsWith( "_prefs.txt" ) )
			{
				continue;
			}

			user = user.substring( 0, user.length() - 10 );
			if ( !user.equals( "GLOBAL" ) && !pastUserList.contains( user ) )
			{
				pastUserList.add( user );
			}
		}

		String[] pastUsers = new String[ pastUserList.size() ];
		pastUserList.toArray( pastUsers );
		return pastUsers;
	}

	public static final void disable( final String name )
	{
		String functionName;
		StringTokenizer tokens = new StringTokenizer( name, ", " );

		while ( tokens.hasMoreTokens() )
		{
			functionName = tokens.nextToken();
			if ( !KoLConstants.disabledScripts.contains( functionName ) )
			{
				KoLConstants.disabledScripts.add( functionName );
			}
		}
	}

	public static final void enable( final String name )
	{
		if ( name.equals( "all" ) )
		{
			KoLConstants.disabledScripts.clear();
			return;
		}

		StringTokenizer tokens = new StringTokenizer( name, ", " );
		while ( tokens.hasMoreTokens() )
		{
			KoLConstants.disabledScripts.remove( tokens.nextToken() );
		}
	}

	public static final boolean isDisabled( final String name )
	{
		if ( name.equals( "enable" ) || name.equals( "disable" ) )
		{
			return false;
		}

		return KoLConstants.disabledScripts.contains( "all" ) || KoLConstants.disabledScripts.contains( name );
	}
}
