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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;

public class LocalRelayRequest extends PasswordHashRequest
{
	private static final Pattern MENU1_PATTERN = Pattern.compile( "<select name=\"loc\".*?</select>", Pattern.DOTALL );
	private static final Pattern MENU2_PATTERN = Pattern.compile( "<select name=location.*?</select>", Pattern.DOTALL );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<img src=\"([^\"]*?)\"" );
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "['\\s-]" );

	private static final Pattern SEARCHITEM_PATTERN = Pattern.compile( "searchitem=(\\d+)&searchprice=(\\d+)" );
	private static final Pattern STORE_PATTERN = Pattern.compile( "<tr><td><input name=whichitem type=radio value=(\\d+).*?</tr>", Pattern.DOTALL );

	private static String lastUsername = "";

	public List headers = new ArrayList();
	public byte [] rawByteBuffer = null;
	public String contentType = null;
	public String statusLine = "HTTP/1.1 302 Found";

	public LocalRelayRequest( String formURLString )
	{
		super( formURLString );

		if ( this.formURLString.endsWith( ".css" ) )
			this.contentType = "text/css";
		else if ( this.formURLString.endsWith( ".js" ) )
			this.contentType = "text/javascript";
		else if ( this.formURLString.endsWith( ".gif" ) )
			this.contentType = "image/gif";
		else if ( this.formURLString.endsWith( ".png" ) )
			this.contentType = "image/png";
		else if ( this.formURLString.endsWith( ".jpg" ) || this.formURLString.endsWith( ".jpeg" ) )
			this.contentType = "image/jpeg";
		else if ( this.formURLString.endsWith( ".ico" ) )
			this.contentType = "image/x-icon";
		else
			this.contentType = "text/html";
	}

	private static final boolean isJunkItem( int itemId, int price )
	{
		if ( !StaticEntity.getBooleanProperty( "relayHidesJunkMallItems" ) )
			return false;

		if ( price > KoLCharacter.getAvailableMeat() )
			return true;

		if ( NPCStoreDatabase.contains( TradeableItemDatabase.getItemName( itemId ) ) )
			if ( price == 100 || price > TradeableItemDatabase.getPriceById( itemId ) * 2 )
				return true;

		for ( int i = 0; i < junkItemList.size(); ++i )
			if ( ((AdventureResult)junkItemList.get(i)).getItemId() == itemId )
				return true;

		return false;
	}

	public void processResponse()
	{
		this.statusLine = "HTTP/1.1 200 OK";
		super.processResponse();

		if ( formURLString.startsWith( "http" ) )
			return;

		StringBuffer responseBuffer = new StringBuffer( responseText );

		// If this is a store, you can opt to remove all the min-priced items from view
		// along with all the items which are priced above affordable levels.

		if ( formURLString.indexOf( "mallstore.php" ) != -1 )
		{
			int searchItemId = -1;
			int searchPrice = -1;

			Matcher itemMatcher = SEARCHITEM_PATTERN.matcher( getURLString() );
			if ( itemMatcher.find() )
			{
				searchItemId = StaticEntity.parseInt( itemMatcher.group(1) );
				searchPrice = StaticEntity.parseInt( itemMatcher.group(2) );
			}

			itemMatcher = STORE_PATTERN.matcher( responseText );

			while ( itemMatcher.find() )
			{
				String itemData = itemMatcher.group(1);

				int itemId = StaticEntity.parseInt( itemData.substring( 0, itemData.length() - 9 ) );
				int price = StaticEntity.parseInt( itemData.substring( itemData.length() - 9 ) );

				if ( itemId != searchItemId && isJunkItem( itemId, price ) )
					StaticEntity.singleStringDelete( responseBuffer, itemMatcher.group() );
			}

			// Also make sure the item that the person selected when coming into the
			// store is pre-selected.

			if ( searchItemId != -1 )
			{
				String searchString = MallPurchaseRequest.getStoreString( searchItemId, searchPrice );
				StaticEntity.singleStringReplace( responseBuffer, "value=" + searchString, "checked value=" + searchString );
			}
		}

		if ( formURLString.indexOf( "compactmenu.php" ) != -1 )
		{
			// Mafiatize the function menu

			StringBuffer functionMenu = new StringBuffer();
			functionMenu.append( "<select name=\"loc\" onChange=\"goloc();\">" );
			functionMenu.append( "<option value=\"nothing\">- Select -</option>" );

			for ( int i = 0; i < FUNCTION_MENU.length; ++i )
			{
				functionMenu.append( "<option value=\"" );
				functionMenu.append( FUNCTION_MENU[i][1] );
				functionMenu.append( "\">" );
				functionMenu.append( FUNCTION_MENU[i][0] );
				functionMenu.append( "</option>" );
			}

			functionMenu.append( "<option value=\"donatepopup.php?pid=" );
			functionMenu.append( KoLCharacter.getUserId() );
			functionMenu.append( "\">Donate</option>" );
			functionMenu.append( "</select>" );

			Matcher menuMatcher = MENU1_PATTERN.matcher( responseText );
			if ( menuMatcher.find() )
				StaticEntity.singleStringReplace( responseBuffer, menuMatcher.group(), functionMenu.toString() );

			// Mafiatize the goto menu

			StringBuffer gotoMenu = new StringBuffer();
			gotoMenu.append( "<select name=location onChange='move();'>" );

			gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );
			for ( int i = 0; i < GOTO_MENU.length; ++i )
			{
				gotoMenu.append( "<option value=\"" );
				gotoMenu.append( GOTO_MENU[i][1] );
				gotoMenu.append( "\">" );
				gotoMenu.append( GOTO_MENU[i][0] );
				gotoMenu.append( "</option>" );
			}

			String [] bookmarkData = StaticEntity.getProperty( "browserBookmarks" ).split( "\\|" );

			if ( bookmarkData.length > 1 )
			{
				gotoMenu.append( "<option value=\"nothing\"> </option>" );
				gotoMenu.append( "<option value=\"nothing\">- Select -</option>" );

				for ( int i = 0; i < bookmarkData.length; i += 3 )
				{
					gotoMenu.append( "<option value=\"" );
					gotoMenu.append( bookmarkData[i+1] );
					gotoMenu.append( "\">" );
					gotoMenu.append( bookmarkData[i] );
					gotoMenu.append( "</option>" );
				}
			}

			gotoMenu.append( "</select>" );

			menuMatcher = MENU2_PATTERN.matcher( responseText );
			if ( menuMatcher.find() )
				StaticEntity.singleStringReplace( responseBuffer, menuMatcher.group(), gotoMenu.toString() );

			// Now kill off the weird focusing problems inherent in
			// the Javascript.

			StaticEntity.globalStringReplace( responseBuffer, "selectedIndex=0;", "selectedIndex=0; if ( parent && parent.mainpane ) parent.mainpane.focus();" );
		}

		// Fix chat javascript problems with relay system

		else if ( formURLString.indexOf( "lchat.php" ) != -1 )
		{
			StaticEntity.globalStringDelete( responseBuffer, "spacing: 0px;" );
			StaticEntity.globalStringReplace( responseBuffer, "cycles++", "cycles = 0" );
			StaticEntity.globalStringReplace( responseBuffer, "location.hostname", "location.host" );

			StaticEntity.singleStringReplace( responseBuffer, "if (postedgraf", "if (postedgraf == \"/exit\") { document.location.href = \"chatlaunch.php\"; return true; } if (postedgraf" );

			// This is a hack to fix KoL chat, as it is handled
			// in Opera.  No guarantees it works, though.

			StaticEntity.singleStringReplace( responseBuffer, "http.onreadystatechange", "executed = false; http.onreadystatechange" );
			StaticEntity.singleStringReplace( responseBuffer, "readyState==4) {", "readyState==4 && !executed) { executed = true;" );
		}

		// Fix KoLmafia getting outdated by events happening
		// in the browser by using the sidepane.

		else if ( formURLString.indexOf( "charpane.php" ) != -1 )
		{
			CharpaneRequest.processCharacterPane( responseText );
		}

		// Fix it a little more by making sure that familiar
		// changes and equipment changes are remembered.

		else
			StaticEntity.externalUpdate( getURLString(), responseText );

		// Allow a way to get from KoL back to the gCLI
		// using the chat launcher.

		if ( formURLString.indexOf( "chatlaunch" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "relayAddsGraphicalCLI" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"KoLmafia/cli.html\"><b>KoLmafia gCLI</b></a></center><p>Type KoLmafia scripting commands in your browser!</p><center>" );
			}

			if ( StaticEntity.getBooleanProperty( "relayAddsKoLSimulator" ) )
			{
				int linkIndex = responseBuffer.indexOf( "<a href" );
				if ( linkIndex != -1 )
					responseBuffer.insert( linkIndex, "<a href=\"KoLmafia/simulator/index.html\" target=\"_blank\"><b>KoL Simulator</b></a></center><p>See what might happen before it happens!</p><center>" );
			}
		}

		if ( StaticEntity.getBooleanProperty( "relayAddsQuickScripts" ) && formURLString.indexOf( "menu" ) != -1 )
		{
			try
			{
				StringBuffer selectBuffer = new StringBuffer();
				selectBuffer.append( "<td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><form name=\"gcli\">" );
				selectBuffer.append( "<select id=\"scriptbar\">" );

				String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );
				for ( int i = 0; i < scriptList.length; ++i )
				{
					selectBuffer.append( "<option value=\"" );
					selectBuffer.append( URLEncoder.encode( scriptList[i], "UTF-8" ) );
					selectBuffer.append( "\">" );
					selectBuffer.append( i + 1 );
					selectBuffer.append( ": " );
					selectBuffer.append( scriptList[i] );
					selectBuffer.append( "</option>" );
				}

				selectBuffer.append( "</select></td><td>&nbsp;</td><td>" );
				selectBuffer.append( "<input type=\"button\" class=\"button\" value=\"exec\" onClick=\"" );

				selectBuffer.append( "var script = document.getElementById( 'scriptbar' ).value; " );
				selectBuffer.append( "parent.charpane.location = '/KoLmafia/sideCommand?cmd=' + script; void(0);" );
				selectBuffer.append( "\">" );
				selectBuffer.append( "</form></td>" );

				int lastRowIndex = responseBuffer.lastIndexOf( "</tr>" );
				if ( lastRowIndex != -1 )
					responseBuffer.insert( lastRowIndex, selectBuffer.toString() );
			}
			catch ( Exception e )
			{
				// Something bad happened, let's ignore it for now, because
				// no script bar isn't the end of the world.
			}
		}

		try
		{
			RequestEditorKit.getFeatureRichHTML( formURLString.toString(), responseBuffer, true );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		// Load image files locally to reduce bandwidth
		// and improve mini-browser performance.

		if ( StaticEntity.getBooleanProperty( "relayUsesCachedImages" ) )
			StaticEntity.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com", "/images" );
		else
			StaticEntity.globalStringReplace( responseBuffer, "http://images.kingdomofloathing.com/scripts", "/images/scripts" );

		// Download and link to any Players of Loathing
		// picture pages locally.

		StaticEntity.globalStringReplace( responseBuffer, "http://pics.communityofloathing.com/albums", "/images" );

		// Remove the default frame busting script so that
		// we can detach user interface elements.

		StaticEntity.singleStringReplace( responseBuffer, "frames.length == 0", "frames.length == -1" );
		responseText = responseBuffer.toString();
		CustomItemDatabase.linkCustomItem( this );
	}

	public String getHeader( int index )
	{
		if ( headers.isEmpty() )
		{
			// This request was relayed to the server. Respond with those headers.

			for ( int i = 0; formConnection.getHeaderFieldKey( i ) != null; ++i )
				if ( !formConnection.getHeaderFieldKey( i ).equals( "Transfer-Encoding" ) )
					headers.add( formConnection.getHeaderFieldKey( i ) + ": " + formConnection.getHeaderField( i ) );
		}

		return index >= headers.size() ? null : (String) headers.get( index );
	}

	public void pseudoResponse( String status, String responseText )
	{
		this.statusLine = status;

		headers.clear();
		headers.add( "Date: " + ( new Date() ) );
		headers.add( "Server: " + VERSION_NAME );

		if ( status.indexOf( "302" ) != -1 )
		{
			headers.add( "Location: " + responseText );

			this.responseCode = 302;
			this.responseText = "";
		}
		else if ( status.indexOf( "200" ) != -1 )
		{
			this.responseCode = 200;

			if ( responseText.length() == 0 )
			{
				this.responseText = " ";
			}
			else
			{
				this.rawByteBuffer = null;
				this.responseText = responseText;
			}
		}
	}

	private StringBuffer readContents( BufferedReader reader, String filename ) throws IOException
	{
		String line = null;
		StringBuffer contentBuffer = new StringBuffer();

		if ( reader == null )
			return contentBuffer;

		while ( (line = reader.readLine()) != null )
		{
			if ( !filename.endsWith( ".js" ) && line.indexOf( "<img" ) != -1 )
			{
				String directory = (new File( "html/" + filename )).getParent();
				if ( !directory.endsWith( "/" ) )
					directory += "/";

				StringBuffer lineBuffer = new StringBuffer();

				Matcher imageMatcher = IMAGE_PATTERN.matcher( line );
				while ( imageMatcher.find() )
				{
					String location = imageMatcher.group(1);
					if ( location.indexOf( "http://" ) == -1 )
					{
						imageMatcher.appendReplacement( lineBuffer,
							"<img src=\"" + (new File( directory + location )).toURI().toURL().toString() + "\"" );
					}
					else
					{
						imageMatcher.appendReplacement( lineBuffer, "$0" );
					}
				}

				imageMatcher.appendTail( lineBuffer );
				line = lineBuffer.toString();
			}

			contentBuffer.append( line );
			contentBuffer.append( LINE_BREAK );
		}

		reader.close();
		return contentBuffer;
	}

	private void sendLocalImage( String filename ) throws IOException
	{
		// The word "KoLmafia" prefixes all of the local
		// images.  Therefore, make sure it's removed.

		BufferedInputStream in = new BufferedInputStream( RequestEditorKit.downloadImage(
			"http://images.kingdomofloathing.com" + filename.substring(6) ).openConnection().getInputStream() );

		ByteArrayOutputStream outbytes = new ByteArrayOutputStream( 4096 );
		byte [] buffer = new byte[4096];

		int offset;
		while ((offset = in.read(buffer)) > 0)
			outbytes.write(buffer, 0, offset);

		in.close();
		outbytes.flush();

		this.rawByteBuffer = outbytes.toByteArray();
		pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	private void sendSharedFile( String filename ) throws IOException
	{
		boolean isServerRequest = !filename.startsWith( "KoLmafia" );
		if ( !isServerRequest )
			filename = filename.substring( 9 );

		int index = filename.indexOf( "/" );
		boolean writePseudoResponse = !isServerRequest;

		BufferedReader reader = null;
		StringBuffer replyBuffer = new StringBuffer();
		StringBuffer scriptBuffer = new StringBuffer();

		String name = filename.substring( index + 1 );
		String directory = index == -1 ? "html" : "html/" + filename.substring( 0, index );

		reader = DataUtilities.getReader( directory, name );
		if ( reader == null && filename.startsWith( "simulator" ) )
		{
			downloadSimulatorFile( name );
			reader = DataUtilities.getReader( directory, name );
		}

		if ( reader != null )
		{
			// Now that you know the reader exists, read the
			// contents of the reader.

			replyBuffer = readContents( reader, filename );
			writePseudoResponse = true;
		}
		else
		{
			if ( isServerRequest )
			{
				// If there's no override file, go ahead and
				// request the page from the server normally.

				super.run();

				if ( responseCode == 302 )
				{
					pseudoResponse( "HTTP/1.1 302 Found", redirectLocation );
					return;
				}
				else if ( responseCode != 200 )
				{
					sendNotFound();
					return;
				}
			}
			else
			{
				sendNotFound();
				return;
			}
		}

		// Add brand new Javascript to every single page.  Check
		// to see if a reader exists for the file.

		if ( !name.endsWith( ".js" ) )
		{
			reader = DataUtilities.getReader( directory, name.substring( 0, name.lastIndexOf( "." ) ) + ".js" );
			if ( reader != null )
			{
				// Initialize the reply buffer with the contents of
				// the full response if you're a standard KoL request.

				if ( isServerRequest )
				{
					replyBuffer.append( responseText );
					writePseudoResponse = true;
				}

				scriptBuffer = readContents( reader, filename.substring( 0, filename.lastIndexOf( "." ) ) + ".js" );
				if ( filename.equals( "simulator/index.html" ) )
					handleSimulatorIndex( replyBuffer, scriptBuffer );

				int terminalIndex = replyBuffer.lastIndexOf( "</html>" );
				if ( terminalIndex == -1 )
				{
					replyBuffer.append( scriptBuffer.toString() );
					replyBuffer.append( "</html>" );
				}
				else
				{
					scriptBuffer.insert( 0, LINE_BREAK );
					scriptBuffer.insert( 0, LINE_BREAK );
					scriptBuffer.insert( 0, "<script language=\"Javascript\">" );
					scriptBuffer.append( LINE_BREAK );
					scriptBuffer.append( LINE_BREAK );
					scriptBuffer.append( "</script>" );
					replyBuffer.insert( terminalIndex, scriptBuffer.toString() );
				}
			}
		}

		if ( writePseudoResponse )
		{
			// Make sure to print the reply buffer to the
			// response buffer for the local relay server.

			if ( isChatRequest )
				StaticEntity.globalStringReplace( replyBuffer, "<br>", "</font><br>" );

			pseudoResponse( "HTTP/1.1 200 OK", replyBuffer.toString() );
		}
	}

	private static String getSimulatorName( int equipmentSlot )
	{
		AdventureResult item = KoLCharacter.getEquipment( equipmentSlot );

		if ( equipmentSlot == KoLCharacter.FAMILIAR && item.getName().equals( FamiliarsDatabase.getFamiliarItem( KoLCharacter.getFamiliar().getId() ) ) )
			return "familiar-specific +5 lbs.";

		if ( item == EquipmentRequest.UNEQUIP )
			return "(None)";

		return item.getName();
	}

	private void handleSimulatorIndex( StringBuffer replyBuffer, StringBuffer scriptBuffer ) throws IOException
	{
		// This is the simple Javascript which can be added
		// arbitrarily to the end without having to modify
		// the underlying HTML.

		int classIndex = -1;
		for ( int i = 0; i < KoLmafiaASH.CLASSES.length; ++i )
			if ( KoLmafiaASH.CLASSES[i].equalsIgnoreCase( KoLCharacter.getClassType() ) )
				classIndex = i;

		// Basic additions of player state info

		StaticEntity.globalStringReplace( scriptBuffer, "/*classIndex*/", classIndex );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMuscle*/", KoLCharacter.getBaseMuscle() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMysticality*/", KoLCharacter.getBaseMysticality() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*baseMoxie*/", KoLCharacter.getBaseMoxie() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*mindControl*/", KoLCharacter.getMindControlLevel() );

		// Change the player's familiar to the current
		// familiar.  Input the weight and change the
		// familiar equipment.

		StaticEntity.globalStringReplace( scriptBuffer, "/*familiar*/",  KoLCharacter.getFamiliar().getRace() );
		StaticEntity.globalStringReplace( scriptBuffer, "/*familiarWeight*/", KoLCharacter.getFamiliar().getWeight() );

		String familiarEquipment = getSimulatorName( KoLCharacter.FAMILIAR );
		if ( FamiliarData.itemWeightModifier( TradeableItemDatabase.getItemId( familiarEquipment ) ) == 5 )
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", "familiar-specific +5 lbs." );
		else
			StaticEntity.globalStringReplace( scriptBuffer, "/*familiarEquip*/", familiarEquipment );

		// Change the player's equipment

		StaticEntity.globalStringReplace( scriptBuffer, "/*hat*/", getSimulatorName( KoLCharacter.HAT ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*weapon*/", getSimulatorName( KoLCharacter.WEAPON ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*offhand*/", getSimulatorName( KoLCharacter.OFFHAND ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*shirt*/", getSimulatorName( KoLCharacter.SHIRT ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*pants*/", getSimulatorName( KoLCharacter.PANTS ) );

		// Change the player's accessories

		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory1*/", getSimulatorName( KoLCharacter.ACCESSORY1 ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory2*/", getSimulatorName( KoLCharacter.ACCESSORY2 ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*accessory3*/", getSimulatorName( KoLCharacter.ACCESSORY3 ) );

		// Load up the player's current skillset to figure
		// out what passive skills are available.

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		StringBuffer passiveSkills = new StringBuffer();
		for ( int i = 0; i < skills.length; ++i )
		{
			int skillId = skills[i].getSkillId();
			if ( !( ClassSkillsDatabase.getSkillType( skillId ) == ClassSkillsDatabase.PASSIVE && !(skillId < 10 || (skillId > 14 && skillId < 1000)) ) )
				continue;

			passiveSkills.append( "\"" );
			passiveSkills.append( WHITESPACE_PATTERN.matcher( skills[i].getSkillName() ).replaceAll( "" ).toLowerCase() );
			passiveSkills.append( "\"," );
		}

		StaticEntity.globalStringReplace( scriptBuffer, "/*passiveSkills*/", passiveSkills.toString() );

		// Also load up the player's current active effects
		// and fill them into the buffs area.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		StringBuffer activeEffects = new StringBuffer();
		for ( int i = 0; i < effects.length; ++i )
		{
			activeEffects.append( "\"" );
			activeEffects.append( WHITESPACE_PATTERN.matcher( effects[i].getName() ).replaceAll( "" ).replaceAll( "\u00f1", "n" ).toLowerCase() );
			activeEffects.append( "\"," );
		}

		StaticEntity.globalStringReplace( scriptBuffer, "/*activeEffects*/", activeEffects.toString() );

		if ( inventory.contains( UseSkillRequest.ROCKNROLL_LEGEND ) )
			StaticEntity.globalStringReplace( scriptBuffer, "/*rockAndRoll*/", "true" );
		else
			StaticEntity.globalStringReplace( scriptBuffer, "/*rockAndRoll*/", "false" );

		StaticEntity.globalStringReplace( scriptBuffer, "/*lastZone*/", StaticEntity.getProperty( "lastAdventure" ) );
		StaticEntity.globalStringReplace( scriptBuffer, "/*lastMonster*/", FightRequest.getLastMonster() );

		StaticEntity.globalStringReplace( scriptBuffer, "/*moonPhase*/", (int) ((MoonPhaseDatabase.getGrimacePhase()-1) * 2
			+ Math.round( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f - Math.floor( (MoonPhaseDatabase.getRonaldPhase()-1) / 2.0f ) )) );

		StaticEntity.globalStringReplace( scriptBuffer, "/*minimoonPhase*/", String.valueOf( MoonPhaseDatabase.getHamburglarPosition( new Date() ) ) );
	}

	public void submitCommand()
	{
		CommandDisplayFrame.executeCommand( getFormField( "cmd" ) );
		pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	public void executeCommand()
	{
		CommandDisplayFrame.executeCommand( getFormField( "cmd" ) );
		pseudoResponse( "HTTP/1.1 200 OK", "" );
	}

	public void sideCommand()
	{
		CommandDisplayFrame.executeCommand( getFormField( "cmd" ) );
		while ( CommandDisplayFrame.hasQueuedCommands() )
			delay( 500 );

		pseudoResponse( "HTTP/1.1 302 Found", "/charpane.php" );
	}

	public void sendNotFound()
	{
		pseudoResponse( "HTTP/1.1 404 Not Found", "" );
		this.responseCode = 404;
	}

	public void run()
	{
		// If there is an attempt to view the error page, or if
		// there is an attempt to view the robots file, neither
		// are available on KoL, so return.

		if ( formURLString.equals( "missingimage.gif" ) || formURLString.endsWith( "robots.txt" ) )
		{
			sendNotFound();
			return;
		}

		if ( formURLString.equals( "desc_item.php" ) )
		{
			String item = getFormField( "whichitem" );
			if ( item != null && item.startsWith( "custom" ) )
			{
				pseudoResponse( "HTTP/1.1 200 OK", CustomItemDatabase.retrieveCustomItem( item.substring(6) ) );
				return;
			}
		}

		// Special handling of adventuring locations before it's
		// registered internally with KoLmafia.

		if ( formURLString.indexOf( "aventure.php" ) != -1 )
		{
			String location = getFormField( "snarfblat" );
			if ( location == null )
				location = getFormField( "adv" );

			// Abort request if the person is attempting to stasis
			// mine, even if it's done manually.

			if ( location != null && location.equals( "101" ) && KoLCharacter.getFamiliar().isThiefFamiliar() && KoLCharacter.canInteract() )
			{
				pseudoResponse( "HTTP/1.1 200 OK", "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head><body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><blockquote>Please reconsider your meat farming strategy.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

				return;
			}

			// Special protection against adventuring in the pirates
			// in disguise before level 9.

			if ( location != null && location.equals( "67" ) && KoLCharacter.getLevel() < 9 )
			{
				pseudoResponse( "HTTP/1.1 200 OK", "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head><body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><blockquote>Adventuring here before level 9 is a really bad idea.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

				return;
			}
		}

		// If the person is visiting the sorceress and they forgot
		// to equip the Wand, remind them.

		if ( formURLString.indexOf( "lair6.php" ) != -1 && getFormField( "place" ) != null && getFormField( "place" ).equals( "5" ) )
		{
			if ( !KoLCharacter.hasEquipped( SorceressLair.NAGAMAR ) )
			{
				pseudoResponse( "HTTP/1.1 200 OK", "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://images.kingdomofloathing.com/styles.css\"></head><body><center><table width=95%  cellspacing=0 cellpadding=0><tr><td style=\"color: white;\" align=center bgcolor=blue><b>Results:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td><center><img src=\"http://images.kingdomofloathing.com/itemimages/wand.gif\" width=30 height=30><br></center><blockquote>Hm, it's possible there is something very important you're forgetting.  Maybe you should <a href=\"inventory.php?which=2\">double-check</a> just to make sure.</blockquote></td></tr></table></center></td></tr></table></center></body></html>" );

				return;
			}
		}

		if ( formURLString.indexOf( "ascend.php" ) != -1 && getFormField( "action" ) != null )
			RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );

		// If you are in chat, and the person submitted a command
		// via chat, check to see if it's a CLI command.

		String chatResponse = ChatRequest.executeChatCommand( getFormField( "graf" ) );
		if ( chatResponse != null )
		{
			pseudoResponse( "HTTP/1.1 200 OK",
				"<font color=\"blue\"><b><a target=\"mainpane\" href=\"showplayer.php?who=458968\" style=\"color:blue\">" +
				VERSION_NAME + "</a> (private)</b>: " + chatResponse + "</font><br>" );

			return;
		}

		// None of the above checks wound up happening.  So, do some
		// special handling, catching any exceptions that happen to
		// popup along the way.

		try
		{
			if ( formURLString.endsWith( "favicon.ico" ) )
			{
				StaticEntity.loadLibrary( "KoLmelion.ico" );
				sendLocalImage( "KoLmelion.ico" );
			}
			else if ( formURLString.endsWith( "submitCommand" ) )
				submitCommand();
			else if ( formURLString.endsWith( "executeCommand" ) )
				executeCommand();
			else if ( formURLString.endsWith( "sideCommand" ) )
				sideCommand();
			else if ( formURLString.endsWith( "messageUpdate" ) )
				pseudoResponse( "HTTP/1.1 200 OK", LocalRelayServer.getNewStatusMessages() );
			else if ( formURLString.indexOf( "images/playerpics/" ) != -1 )
			{
				RequestEditorKit.downloadImage( "http://pics.communityofloathing.com/albums/" +
					formURLString.substring( formURLString.indexOf( "playerpics" ) ) );

				sendLocalImage( formURLString );
			}
			else if ( formURLString.indexOf( "images/" ) != -1 )
			{
				sendLocalImage( formURLString );
			}
			else if ( formURLString.indexOf( "lchat.php" ) != -1 )
			{
				if ( StaticEntity.getBooleanProperty( "relayUsesIntegratedChat" ) )
				{
					sendSharedFile( "chat.html" );
				}
				else
				{
					sendSharedFile( formURLString );
					responseText = StaticEntity.globalStringReplace( responseText, "<p>", "<br><br>" );
					responseText = StaticEntity.globalStringReplace( responseText, "<P>", "<br><br>" );
					responseText = StaticEntity.singleStringDelete( responseText, "</span>" );
				}
			}
			else
			{
				sendSharedFile( formURLString );

				if ( isChatRequest )
				{
					try
					{
						if ( !KoLMessenger.isRunning() || formURLString.indexOf( "submitnewchat.php" ) != -1 )
							KoLMessenger.updateChat( responseText );
					}
					catch ( Exception e )
					{
						StaticEntity.printStackTrace( e );
					}

					responseText = KoLMessenger.getNormalizedContent( responseText, false );
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  However, if it does,
			// that means the browser is asking for a bad URL.
			// Go ahead and return a 404 in this case.

			e.printStackTrace( KoLmafia.getDebugStream() );
			sendNotFound();
		}
	}

	private void downloadSimulatorFile( String filename )
	{
		try
		{
			BufferedReader reader = DataUtilities.getReader( "http://sol.kolmafia.us/" + filename );

			String line;
			StringBuffer contents = new StringBuffer();

			while ( (line = reader.readLine()) != null )
			{
				contents.append( line );
				contents.append( LINE_BREAK );
			}

			File directory = new File( "html/simulator/" );
			directory.mkdirs();

			StaticEntity.globalStringReplace( contents, "images/", "http://sol.kolmafia.us/images/" );

			PrintStream writer = KoLmafia.openStream( "html/simulator/" + filename, NullStream.INSTANCE, true );
			writer.println( contents.toString() );
			writer.close();
		}
		catch ( Exception e )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to create simulator file <" + filename + ">" );
		}
	}
}
