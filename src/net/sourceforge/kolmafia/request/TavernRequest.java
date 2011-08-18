/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TavernRequest
	extends GenericRequest
{
	private static final Pattern GOOFBALL_PATTERN = Pattern.compile( "Buy some goofballs \\((\\d+),000 Meat\\)" );

	// tavern.php?place=barkeep
	//	store.php?whichstore=v&buying=Yep.&phash&whichitem=xxx&howmany=y
	// tavern.php?place=susguy
	//	action=buygoofballs
	// tavern.php?place=pooltable
	//	action=pool&opponent=1&wager=50
	//	action=pool&opponent=2&wager=200
	//	action=pool&opponent=3&wager=500
	// cellar.php
	//	action=explore&whichspot=4

	public TavernRequest( final int itemId )
	{
		super( "tavern.php" );

		switch (itemId )
		{
		case ItemPool.GOOFBALLS:
			this.addFormField( "action", "buygoofballs" );
			break;
		case ItemPool.OILY_GOLDEN_MUSHROOM:
			this.addFormField( "sleazy", "1" );
			break;
		default:
			this.addFormField( "place", "susguy" );

			break;
		}
	}

	public void processResults()
	{
		TavernRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "tavern.php" ) )
		{
			return;
		}

		if ( location.indexOf( "action=buygoofballs" ) != -1 )
		{
			// Here you go, man. If you get caught, you didn't get
			// these from me, man.

			if ( responseText.indexOf( "If you get caught" ) == -1 )
			{
				return;
			}

			Matcher matcher = GOOFBALL_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return;
			}

			int cost = 1000 * Integer.parseInt( matcher.group( 1 ) ) - 1000;
			if ( cost > 0 )
			{
				ResultProcessor.processMeat( -cost );
			}

			return;
		}

		if ( location.indexOf( "sleazy=1" ) != -1 )
		{
			// The suspicious-looking guy takes your gloomy black
			// mushroom and smiles that unsettling little smile
			// that makes you nervous. "Sweet, man. Here ya go."

			if ( responseText.indexOf ("takes your gloomy black mushroom" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.GLOOMY_BLACK_MUSHROOM, -1 );
			}

			return;
		}
	}

	private static final Pattern MAP_PATTERN = Pattern.compile( "alt=\"([^\"]*) \\(([\\d]*),([\\d]*)\\)\"" );

	private static final void parseCellarMap( final String text )
	{
		TavernRequest.validateFaucetQuest();

		String oldLayout = Preferences.getString( "tavernLayout" );
		StringBuffer layout = new StringBuffer( oldLayout );

		Matcher matcher = TavernRequest.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int col = StringUtilities.parseInt( matcher.group(2) );
			int row = StringUtilities.parseInt( matcher.group(3) );
			int square = ( row - 1 ) * 5 + ( col - 1 );

			if ( square < 0 || square >= 25 || layout.charAt( square ) != '0' )
			{
				continue;
			}

			String type = matcher.group(1);
			char code =
				type.startsWith( "Darkness" ) ? '0' :
				type.startsWith( "Explored" ) ? '1' :
				type.startsWith( "A Rat Faucet" ) ? '3' :
				type.startsWith( "A Tiny Mansion" ) ? '4' :
				type.startsWith( "Stairs Up" ) ? '1' :
				'0';

			layout.setCharAt( square, code );
		}

		String newLayout = layout.toString();

		if ( !oldLayout.equals( newLayout ) )
		{
			Preferences.setString( "tavernLayout", newLayout );
		}
	}

	private static final Pattern SPOT_PATTERN = Pattern.compile( "whichspot=([\\d,]+)" );
	private static final int getSquare( final String urlString )
	{
		// cellar.php?action=explore&whichspot=4
		if ( !urlString.startsWith( "cellar.php" ) || urlString.indexOf( "action=explore") == -1 )
		{
			return 0;
		}

		Matcher matcher = TavernRequest.SPOT_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	public static final String cellarLocationString( final String urlString )
	{
		int square = TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return null;
		}

		int row = ( ( square - 1 ) / 5 ) + 1;
		int col = ( ( square - 1 ) % 5 ) + 1;
		return "Tavern Cellar (row " + row + ", col " + col + ")";
	}

	public static final void validateFaucetQuest()
	{
		int lastAscension = Preferences.getInteger( "lastTavernAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastTavernSquare", 0 );
			Preferences.setInteger( "lastTavernAscension", KoLCharacter.getAscensions() );
			Preferences.setString( "tavernLayout", "0000000000000000000000000" );
		}
	}

	public static final void preTavernVisit( final GenericRequest request )
	{
		TavernRequest.validateFaucetQuest();

		String urlString = request.getURLString();
		int square = TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		Preferences.setInteger( "lastTavernSquare", square );
	}

	public static final void postTavernVisit( final GenericRequest request )
	{
		String urlString = request.getURLString();

		if ( urlString.equals( "cellar.php" ) )
		{
			TavernRequest.parseCellarMap( request.responseText );
			return;
		}

		if ( KoLCharacter.getAdventuresLeft() == 0 ||
		     KoLCharacter.getCurrentHP() == 0 ||
		     KoLCharacter.getInebriety() > KoLCharacter.getInebrietyLimit() )
		{
			return;
		}

		if ( urlString.startsWith( "fight.php" ) )
		{
			int square = Preferences.getInteger( "lastTavernSquare" );
			char replacement = request.responseText.indexOf( "Baron" ) != -1 ? '4' : '1';
			TavernRequest.addTavernLocation( square, replacement );
			return;
		}

		int square = urlString.startsWith( "choice.php" ) ?
			Preferences.getInteger( "lastTavernSquare" ) :
			TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		char replacement = '1';
		if ( request.responseText.indexOf( "Those Who Came Before You" ) != -1 )
		{
			// Dead adventurer
			replacement = '2';
		}
		else if ( request.responseText.indexOf( "Of Course!" ) != -1 ||
			  request.responseText.indexOf( "Hot and Cold Running Rats" ) != -1 )
		{
			// Rat faucet, before and after turning off
			replacement = '3';
		}
		else if ( request.responseText.indexOf( "is it Still a Mansion" ) != -1 )
		{
			// Baron von Ratsworth
			replacement = '4';
		}
		else if ( request.responseText.indexOf( "whichchoice" ) != -1 )
		{
			// Various Barrels
			replacement = '5';
		}

		TavernRequest.addTavernLocation( square, replacement );
		Preferences.setInteger( "lastTavernSquare", square );
	}

	private static final void addTavernLocation( final int square, final char value )
	{
		TavernRequest.validateFaucetQuest();
		StringBuffer layout = new StringBuffer( Preferences.getString( "tavernLayout" ) );
		layout.setCharAt( square - 1, value );
		Preferences.setString( "tavernLayout", layout.toString() );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "tavern.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=buygoofballs" ) != -1 )
		{
			message = "Buying goofballs from the suspicious looking guy";
		}
		else if ( urlString.indexOf( "sleazy=1" ) != -1 )
		{
			message = "Trading a gloomy black mushroom for an oily golden mushroom";
		}
		else if ( urlString.indexOf( "sleazy=2" ) != -1 )
		{
			// Keeping your gloomy black mushroom
			return true;
		}
		else if ( urlString.indexOf( "place=susguy" ) != -1 )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the suspicious looking guy";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
