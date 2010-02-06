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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShrineRequest
	extends GenericRequest
{
	public static final int BORIS = 1;
	public static final int JARLSBERG = 2;
	public static final int PETE = 3;

	private static final AdventureResult[] STATUE_KEYS =
	{
		ItemPool.get( ItemPool.BORIS_KEY, 1 ),
		ItemPool.get( ItemPool.JARLSBERG_KEY, 1 ),
		ItemPool.get( ItemPool.SNEAKY_PETE_KEY, 1 ),
	};

	private final int amount;
	public String statue;
	private boolean hasStatueKey;

	/**
	 * Constructs a new <code>ShrineRequest</code>.
	 *
	 * @param heroId The identifier for the hero to whom you are donating
	 * @param amount The amount you're donating to the given hero
	 */

	public ShrineRequest( final int heroId, final int amount )
	{
		super( "shrines.php" );

		this.addFormField(
			"action",
			heroId == ShrineRequest.BORIS ? "boris" : heroId == ShrineRequest.JARLSBERG ? "jarlsberg" : "sneakypete" );
		this.addFormField( "howmuch", String.valueOf( amount ) );

		this.amount = amount;
		this.statue =
			heroId == ShrineRequest.BORIS ? "boris" : heroId == ShrineRequest.JARLSBERG ? "jarlsberg" : "pete";
		this.hasStatueKey = KoLConstants.inventory.contains( ShrineRequest.STATUE_KEYS[ heroId - 1 ] );
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public void run()
	{
		if ( !this.hasStatueKey )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the appropriate key." );
			return;
		}

		KoLmafia.updateDisplay( "Donating " + this.amount + " to the shrine..." );
		super.run();

	}

	public void processResults()
	{
                String error = ShrineRequest.parseResponse( this.getURLString(), this.responseText ); 
                if ( error != null )
                {
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, error );
			return;
                }
		KoLmafia.updateDisplay( "Donation complete." );
	}

	public static final String parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shrines.php" ) )
		{
			return null;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return null;
		}

		String preference =
			action.equals( "boris" ) ? "heroDonationBoris" :
			action.equals( "jarlsberg" ) ? "heroDonationJarlsberg" :
			action.equals( "sneakypete" ) ? "heroDonationSneakyPete" :
			null;

		if ( preference == null )
		{
			return null;
		}

		Matcher matcher = GenericRequest.HOWMUCH_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}

		// If we get here, we tried donating

		if ( responseText.indexOf( "You gain" ) == -1 )
		{
			return responseText.indexOf( "That's not enough" ) == -1 ?
				"Donation limit exceeded." :
				"Donation must be larger.";
		}

		int qty = StringUtilities.parseInt( matcher.group(1) );
		ResultProcessor.processMeat( 0 - qty );
		Preferences.increment( preference, qty );

		return null;
	}
}
