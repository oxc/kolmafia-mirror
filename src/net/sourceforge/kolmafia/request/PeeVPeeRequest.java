/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PeeVPeeRequest
	extends GenericRequest
{
	public static final String[] WIN_MESSAGES =
		new String[] { "50 CHARACTER LIMIT BREAK!", "HERE'S YOUR CHEETO, MOTHER!*$#ER.", "If you want it back, I'll be in my tent.", "PWNED LIKE CRAPSTORM." };

	public static final String[] LOSE_MESSAGES =
		new String[] { "OMG HAX H4X H5X!!", "Please return my pants.", "How do you like my Crotch-To-Your-Foot style?", "PWNED LIKE CRAPSTORM." };
	
	private static final Pattern ATTACKS_PATTERN =
		Pattern.compile( "You have (\\d+) fight" );
	
	private static final Pattern HIPPY_STONE_PATTERN = 
		Pattern.compile( "You must break your <a href=\"campground.php?action=stone\">Magical Mystical Hippy Stone</a> to participate in PvP combat." );

	public PeeVPeeRequest()
	{
		super( "peevpee.php" );
	}
	
	public PeeVPeeRequest( final String place )
	{
		super( "peevpee.php" );
		this.addFormField( "place", place );
	}
	
	public PeeVPeeRequest( final String opponent, final int stance, final String mission )
	{
		super( "peevpee.php" );
		
		this.addFormField( "action", "fight" );
		this.addFormField( "place", "fight" );
		this.addFormField( "attacktype", mission );
		// ranked=1 for normal, 2 for harder
		this.addFormField( "ranked", "1" );
		this.addFormField( "stance", String.valueOf( stance ) );
		this.addFormField( "who", opponent );
		
		String win = Preferences.getString( "defaultFlowerWinMessage" );
		String lose = Preferences.getString( "defaultFlowerLossMessage" );

		if ( win.equals( "" ) )
		{
			win = PeeVPeeRequest.WIN_MESSAGES[ KoLConstants.RNG.nextInt( PvpRequest.WIN_MESSAGES.length ) ];
		}
		if ( lose.equals( "" ) )
		{
			lose =
				PeeVPeeRequest.LOSE_MESSAGES[ KoLConstants.RNG.nextInt( PvpRequest.LOSE_MESSAGES.length ) ];
		}
		
		this.addFormField( "winmessage", win );
		this.addFormField( "losemessage", lose );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( location.indexOf( "place=shop" ) != -1 || location.indexOf( "action=buy" ) != -1 )
		{
			SwaggerShopRequest.parseResponse( location, responseText );
			return;
		}
		
		if ( location.indexOf( "place=fight" ) != -1 )
		{
			Matcher attacksMatcher = PeeVPeeRequest.ATTACKS_PATTERN.matcher( responseText );
			if ( attacksMatcher.find() )
			{
				KoLCharacter.setAttacksLeft( StringUtilities.parseInt( attacksMatcher.group( 1 ) ) );
			}
			else
			{
				KoLCharacter.setAttacksLeft( 0 );
			}
			
			Matcher hippyStoneMatcher = PeeVPeeRequest.HIPPY_STONE_PATTERN.matcher( responseText );
			if ( hippyStoneMatcher.find() )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "This feature is not available to hippies." );
				return;
			}
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "peevpee.php" ) )
		{
			return false;
		}

		Matcher matcher = GenericRequest.PLACE_PATTERN.matcher( urlString );
		String place = matcher.find() ? matcher.group(1) : null;

		matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// Don't log visits to the container document
		if ( place == null && action == null )
		{
			return true;
		}
		
		// place = rules
		// place = fight
		// place = boards
		// place = logs
		// place = shop

		if ( ( place != null && place.equals( "shop" ) ) ||
		     ( action != null && action.equals( "buy" ) ) )
		{
			return SwaggerShopRequest.registerRequest( urlString );
		}

		// Log everything, for now
		return false;
	}
}
