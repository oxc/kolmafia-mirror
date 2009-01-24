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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class WineCellarRequest
	extends GenericRequest
{
	private static final Pattern DEMON_PATTERN = Pattern.compile( "demonname=([^&]*)" );
	private static final Pattern BOTTLE_PATTERN = Pattern.compile( "whichwine=(\\d+)" );

	public WineCellarRequest( final String demon)
	{
		super( "manor3.php" );
		this.addFormField( "action", "summon" );
		this.addFormField( "demonname", demon );
	}

	public WineCellarRequest( final int bottle)
	{
		super( "manor3.php" );
		this.addFormField( "action", "pourwine" );
		this.addFormField( "whichwine", String.valueOf( bottle ) );
	}

	public void processResults()
	{
		WineCellarRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final boolean parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "manor3.php" ) )
		{
			return false;
		}

		if ( location.indexOf( "action=summon" ) != -1 )
		{
			if ( responseText.indexOf( "You light three black candles" ) != -1 )
			{
				AdventureRequest.registerDemonName( "Summoning Chamber", responseText );
				ResultProcessor.processItem( ItemPool.BLACK_CANDLE, -3 );
				ResultProcessor.processItem( ItemPool.EVIL_SCROLL, -1 );

				if ( responseText.indexOf( "some sort of crossed signal" ) == -1 &&
					responseText.indexOf( "hum, which eventually cuts off" ) == -1 &&
					responseText.indexOf( "get right back to you" ) == -1 &&
					responseText.indexOf( "Please check the listing" ) == -1 )
				{
					Preferences.setBoolean( "demonSummoned", true );
				}
			}
		}

		return false;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "manor3.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=summon" ) != -1 )
		{
			Matcher matcher = DEMON_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return true;
			}

			String demon = matcher.group(1);

			try
			{
				demon = URLDecoder.decode( demon, "UTF-8" );
			}
			catch ( UnsupportedEncodingException e )
			{
				// This shouldn't happen. Use name as is.
			}

			if ( demon.equals( "" ) ||
			     !InventoryManager.retrieveItem( ItemPool.BLACK_CANDLE, 3 ) ||
			     !InventoryManager.retrieveItem( ItemPool.EVIL_SCROLL ) )
			{
				return true;
			}

			RequestLogger.updateSessionLog( "summon " + demon );

			return true;
		}

		if ( urlString.indexOf( "action=pourwine" ) != -1 )
		{
			Matcher matcher = BOTTLE_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return true;
			}

			int itemId = Integer.parseInt( matcher.group( 1 ) );
			if ( !InventoryManager.retrieveItem( itemId, 1 ) )
			{
				return true;
			}

			String name = ItemDatabase.getItemName( itemId );
			RequestLogger.updateSessionLog( "pour " + name + " into goblet" );
			ResultProcessor.processItem( itemId, -1 );

			return true;
		}

		if ( urlString.indexOf( "place=chamber" ) != -1 )
		{
			String message = "[" + KoLAdventure.getAdventureCount() + "] Summoning Chamber";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		return true;
	}
}
