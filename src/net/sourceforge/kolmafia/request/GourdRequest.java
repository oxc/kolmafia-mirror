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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GourdRequest
	extends GenericRequest
{
	private static final Pattern GOURD_PATTERN = Pattern.compile( "Bring back (\\d*)", Pattern.DOTALL );

	public GourdRequest()
	{
		this(false);
	}

	public GourdRequest( boolean trade )
	{
		super( "town_right.php");
		this.addFormField( trade ? "action" : "place", "gourd" );
	}

	public void processResults()
	{
		GourdRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// Either place=gourd or action=gourd

		if ( !location.startsWith( "town_right.php" ) || location.indexOf( "gourd" ) == -1 )
		{
			return;
		}

		// Bring back 5 of their... erp... lids, and you'll
		// be... be... gurk... rewarded

		Matcher matcher = GourdRequest.GOURD_PATTERN.matcher( responseText );
		int count =  matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 26;

		Preferences.setInteger( "gourdItemCount", count );
	}

	public static final AdventureResult gourdItem( final int count )
	{
		switch ( KoLCharacter.getPrimeIndex() )
		{
		case 0:
			return ItemPool.get( ItemPool.KNOB_FIRECRACKER, count );
		case 1:
			return ItemPool.get( ItemPool.CAN_LID, count );
		default:
			return ItemPool.get( ItemPool.SPIDER_WEB, count );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_right.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=gourd" ) != -1 )
		{
			int count = Preferences.getInteger( "gourdItemCount" );
			AdventureResult item = GourdRequest.gourdItem( -count );
			if ( item.getCount( KoLConstants.inventory ) < count )
			{
				return true;
			}
			message = "Giving " + count + " " + item.getName() + "s to the Captain of the Gourd";
			ResultProcessor.processResult( item );
		}
		else if ( urlString.indexOf( "place=gourd" ) != -1 )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the Captain of the Gourd";
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
