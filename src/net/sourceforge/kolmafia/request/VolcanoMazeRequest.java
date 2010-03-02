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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.VolcanoMazeManager;

public class VolcanoMazeRequest
	extends GenericRequest
{
	public VolcanoMazeRequest()
	{
		super( "volcanomaze.php" );
	}

	public VolcanoMazeRequest( final int pos )
	{
		super( VolcanoMazeRequest.getMoveURL( pos ), false );
	}

	public VolcanoMazeRequest( final int col, final int row )
	{
		super( VolcanoMazeRequest.getMoveURL( col, row ), false );
	}

	public static String getMoveURL( final int pos )
	{
		int row = pos / 13;
		int col = pos % 13;
		return VolcanoMazeRequest.getMoveURL( col, row );
	}

	public static String getMoveURL( final int col, final int row )
	{
		return "volcanomaze.php?move=" + String.valueOf( col ) + "," + String.valueOf( row ) + "&ajax=1";
	}

	public void run()
	{
		super.run();
		VolcanoMazeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "volcanomaze.php" ) )
		{
			return;
		}

		// Parse and save the map
		VolcanoMazeManager.parseResult( responseText );
	}

	private static final Pattern MOVE_PATTERN = Pattern.compile("move=((\\d+),(\\d+))");

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "volcanomaze.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "jump=1" ) != -1 )
		{
			RequestLogger.updateSessionLog( "Swimming back to shore" );
			return true;
		}

		Matcher matcher = VolcanoMazeRequest.MOVE_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			String message = "Hopping from " + VolcanoMazeManager.currentCoordinates() + " to " + matcher.group(1);
			RequestLogger.updateSessionLog( message );
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Visiting the lava maze" );

		return true;
	}
}
