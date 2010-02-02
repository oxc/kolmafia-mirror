/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLoungeRequest
	extends GenericRequest
{
	private static final int SEARCH = 0;

	public static final int KLAW = 1;
	public static final int HOTTUB = 2;
	public static final int POOL_TABLE = 3;
	public static final int CRIMBO_TREE = 4;

	// Pool options
	public static final int AGGRESSIVE_STANCE = 1;
	public static final int STRATEGIC_STANCE = 2;
	public static final int STYLISH_STANCE = 3;

	private int action;
	private int option;

	/**
	 * Constructs a new <code>ClanLoungeRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private ClanLoungeRequest()
	{
		this( SEARCH );
	}

	public ClanLoungeRequest( final int action )
	{
		super( "clan_viplounge.php" );
		this.action = action;
	}

	public ClanLoungeRequest( final int action, final int option )
	{
		super( "clan_viplounge.php" );
		this.action = action;
		this.option = option;
	}

	public static final AdventureResult VIP_KEY = ItemPool.get( ItemPool.VIP_LOUNGE_KEY, 1 );
	private static final GenericRequest VIP_KEY_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { ClanLoungeRequest.VIP_KEY } );
	private static final GenericRequest VISIT_REQUEST = new ClanLoungeRequest();

	private static void pullVIPKey()
	{
		if ( VIP_KEY.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		// If you have a VIP Lounge Key in storage, pull it.
		if ( VIP_KEY.getCount( KoLConstants.storage ) > 0 )
		{
			RequestThread.postRequest( VIP_KEY_REQUEST );
		}
	}

	public static void visitLounge()
	{
		// Pull a key from storage, if necessary
		ClanLoungeRequest.pullVIPKey();

		// If we have no Clan VIP Lounge key, nothing to do
		if ( VIP_KEY.getCount( KoLConstants.inventory ) == 0 )
		{
			return;
		}

		RequestThread.postRequest( VISIT_REQUEST );
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public static String equipmentName( final String urlString )
	{
		if ( urlString.indexOf( "klaw" ) != -1 )
		{
			return "Deluxe Mr. Klaw \"Skill\" Crane Game";
		}
		if ( urlString.indexOf( "hottub" ) != -1 )
		{
			return "Relaxing Hot Tub";
		}
		if ( urlString.indexOf( "pooltable" ) != -1 )
		{
			return "Pool Table";
		}
		if ( urlString.indexOf( "crimbotree" ) != -1 )
		{
			return "Crimbo Tree";
		}
		return null;
	}

	private static String equipmentVisit( final String urlString )
	{
		String name = ClanLoungeRequest.equipmentName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in clan VIP lounge";
		}
		if ( urlString.indexOf( "poolgame" ) != -1 )
		{
			return "Playing a pool game in clan VIP lounge";
		}
		return null;
	}

	public void run()
	{
		switch ( this.action )
		{
		case ClanLoungeRequest.SEARCH:
			break;

		case ClanLoungeRequest.KLAW:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "klaw" );
			break;

		case ClanLoungeRequest.HOTTUB:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "hottub" );
			break;

		case ClanLoungeRequest.POOL_TABLE:
			this.constructURLString( "clan_viplounge.php" );
			if ( option != 0 )
			{
				this.addFormField( "action", "poolgame" );
				this.addFormField( "stance", String.valueOf( option ) );
			}
			else
			{
				this.addFormField( "action", "pooltable" );
			}
			break;

		case ClanLoungeRequest.CRIMBO_TREE:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "crimbotree" );
			break;

		default:
			break;
		}

		super.run();

		ClanLoungeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) )
		{
			return;
		}

		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// For a simple visit, look at the Crimbo tree and report on
		// whether there is a present waiting.
		if ( action == null )
		{
			if ( responseText.indexOf( "tree5.gif" ) != -1 )
			{
				RequestLogger.printLine( "You have a present under the Crimbo tree in your clan's VIP lounge!" );
			}
			return;
		}

		if ( action.equals( "hottub" ) )
		{
			// You relax in the hot tub, feeling all of your
			// troubles drift away as the bubbles massage your
			// weary muscles.

			if ( responseText.indexOf( "bubbles massage your weary muscles" ) != -1 )
			{
				Preferences.increment( "_hotTubSoaks", 1 );
			}
		}

		if ( action.equals( "klaw" ) )
		{
			// You carefully guide the claw over a prize and press
			// the button (which is mahogany inlaid with
			// mother-of-pearl -- very nice!) -- the claw slowly
			// descends...
			if ( responseText.indexOf( "claw slowly descends" ) != -1 )
			{
				Preferences.increment( "_deluxeKlawSummons", 1 );
			}
			// You probably shouldn't play with this machine any
			// more today -- you wouldn't want to look greedy in
			// front of the other VIPs, would you?
			else if ( responseText.indexOf( "you wouldn't want to look greedy" ) != -1 )
			{
				Preferences.setInteger( "_deluxeKlawSummons", 3 );
			}
		}

		if ( action.equals( "crimbotree" ) )
		{
			// You look under the Crimbo Tree and find a present
			// with your name on it! You excitedly tear it open.
		}
	}

	public static void getBreakfast()
	{
		// No Clan Lounge in Bad Moon
		boolean kl = Preferences.getBoolean( "kingLiberated" );
		if ( KoLCharacter.inBadMoon() && !kl)
		{
			return;
		}

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.
		// 
		// Unlike the regular Klaw, there is no message to tell you
		// that you are done for the day except when you try one too
		// many times: "You probably shouldn't play with this machine
		// any more today -- you wouldn't want to look greedy in front
		// of the other VIPs, would you?"

		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.KLAW );
		while ( Preferences.getInteger( "_deluxeKlawSummons" ) < 3 )
		{
			request.run();
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) )
		{
			return false;
		}

		String message = ClanLoungeRequest.equipmentVisit( urlString );

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
