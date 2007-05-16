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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRelayAgent extends Thread
{
	private static final Pattern INVENTORY_COOKIE_PATTERN = Pattern.compile( "inventory=(\\d+)" );

	private int id;
	private Socket socket = null;

	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private boolean isCheckingModified;
	private LocalRelayRequest request;

	public LocalRelayAgent( int id )
	{	this.id = id;
	}

	boolean isWaiting()
	{	return socket == null;
	}

	void setSocket( Socket socket )
	{
		this.socket = socket;

		synchronized ( this )
		{	notify();
		}
	}

	public void run()
	{
		while ( true )
		{
			if ( socket == null )
			{
				// Wait indefinitely for a client.  Exception
				// handling is probably not the best way to
				// handle this, but for now, it will do.

				try
				{
					synchronized ( this )
					{	wait();
					}
				}
				catch ( InterruptedException e )
				{
					// We expect this to happen only when we are
					// interrupted.  Fall through.
				}
			}

			if ( socket != null )
				performRelay();

			socket = null;
		}
	}

	public void sendHeaders( PrintStream printStream, LocalRelayRequest request ) throws IOException
	{
		String header = null;
		String lowercase = null;

		if ( request.contentType == null )
			request.contentType = "text/html";

		for ( int i = 0; (header = request.getHeader( i )) != null; ++i )
		{
			lowercase = header.toLowerCase();

			if ( lowercase.startsWith( "content-type" ) )
				continue;

			if ( lowercase.startsWith( "content-length" ) )
				continue;

			if ( lowercase.startsWith( "cache-control" ) )
				continue;

			if ( lowercase.startsWith( "pragma" ) )
				continue;

			if ( lowercase.startsWith( "connection" ) )
				continue;

			if ( lowercase.startsWith( "transfer-encoding" ) )
				continue;

			if ( lowercase.startsWith( "set-cookie" ) )
				continue;

			if ( lowercase.startsWith( "http-referer" ) )
				continue;

			if ( !lowercase.equals( "" ) )
				printStream.println( header );
		}

		if ( request.responseCode == 200 )
		{
			if ( request.contentType.equals( "text/html" ) )
				printStream.println( "Content-Type: text/html; charset=UTF-8" );
			else
				printStream.println( "Content-Type: " + request.contentType );

			printStream.println( "Content-Length: " + request.rawByteBuffer.length );

			if ( request.formURLString.indexOf( ".php" ) != -1 )
			{
				printStream.println( "Cache-Control: no-cache, must-revalidate" );
				printStream.println( "Pragma: no-cache" );
			}
		}

		printStream.println( "Connection: close" );
	}

	public void performRelay()
	{
		if ( socket == null )
			return;

		path = null;
		reader = null;
		writer = null;
		request = null;

		try
		{
			reader = KoLDatabase.getReader( socket.getInputStream() );

			readBrowserRequest();
			readServerResponse();

			writer = new PrintStream( socket.getOutputStream(), true, "UTF-8" );

			if ( request.rawByteBuffer != null )
			{
				writer.println( request.statusLine );
				sendHeaders( writer, request );

				writer.println();
				writer.write( request.rawByteBuffer );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		closeRelay( socket, reader, writer );
	}

	public void readBrowserRequest() throws Exception
	{
		String line = null;
		String method = "GET";
		int contentLength = 0;

		if ( (line = reader.readLine()) == null )
			return;

		int spaceIndex = line.indexOf( " " );
		if ( spaceIndex == -1 )
			return;

		method = line.trim().substring( 0, spaceIndex );
		int lastSpaceIndex = line.lastIndexOf( " " );

		path = line.substring( spaceIndex, lastSpaceIndex ).trim();
		request = new LocalRelayRequest( path );

		isCheckingModified = false;

		int colonIndex = 0;
		String [] tokens = new String[2];

		while ( (line = reader.readLine()) != null && line.trim().length() != 0 )
		{
			colonIndex = line.indexOf( ": " );
			if ( colonIndex == -1 )
				continue;

			tokens[0] = line.substring( 0, colonIndex );
			tokens[1] = line.substring( colonIndex + 2 );

			if ( tokens[0].equals( "Content-Length" ) )
				contentLength = StaticEntity.parseInt( tokens[1].trim() );

			if ( tokens[0].equals( "Cookie" ) )
			{
				// Let's find out what kind of cookie the browser is trying
				// to tell KoLmafia about.

				Matcher inventoryMatcher = INVENTORY_COOKIE_PATTERN.matcher( tokens[1] );
				if ( inventoryMatcher.find() )
					StaticEntity.setProperty( "visibleBrowserInventory", "inventory=" + inventoryMatcher.group(1) );
			}

			if ( tokens[0].equals( "If-Modified-Since" ) )
				isCheckingModified = true;
		}

		if ( method.equals( "POST" ) && contentLength > 0 )
		{
			char [] data = new char[ contentLength ];
			reader.read( data, 0, contentLength );

			request.addEncodedFormFields( new String( data ) );
		}
	}

	private void readServerResponse() throws Exception
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( isCheckingModified && !request.contentType.startsWith( "text" ) )
		{
			request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
			request.responseCode = 304;
		}
		else if ( path.indexOf( "fight.php" ) != -1 && (FightRequest.isTrackingFights() || path.indexOf( "action=script" ) != -1) )
		{
			if ( !FightRequest.isTrackingFights() )
			{
				StaticEntity.setProperty( "battleAction", "custom combat script" );

				FightRequest.beginTrackingFights();
				(new Thread( FightRequest.INSTANCE )).start();
			}

			String fightResponse = FightRequest.getNextTrackedRound();

			if ( fightResponse == null )
			{
				request.pseudoResponse( "HTTP/1.1 404 Not Found", "" );
			}
			else
			{
				if ( FightRequest.isTrackingFights() )
				{
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "top.charpane.location.href=\"charpane.php\";" );
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "src=\"http://images.kingdomofloathing.com/scripts/window.js\"" );
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "src=\"http://images.kingdomofloathing.com/scripts/core.js\"" );

					fightResponse = StaticEntity.singleStringReplace( fightResponse, "</html>",
						"<script language=\"Javascript\"> function continueAutomatedFight() { document.location = \"fight.php\"; return 0; } setTimeout( continueAutomatedFight, 400 ); </script></html>" );
				}

				request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
			}
		}
		else if ( path.indexOf( "charpane.php" ) != -1 )
		{
			if ( FightRequest.getActualRound() == 0 )
			{
				StaticEntity.getClient().runBetweenBattleChecks( false, StaticEntity.getBooleanProperty( "relayMaintainsMoods" ),
					StaticEntity.getBooleanProperty( "relayMaintainsHealth" ), StaticEntity.getBooleanProperty( "relayMaintainsMana" ) );
			}

			request.run();
		}
		else
		{
			request.run();

			if ( path.endsWith( "noobmessage=true" ) )
			{
				request.responseText = StaticEntity.singleStringReplace( request.responseText, "</html>",
					"<script language=\"Javascript\"> function visitTootOriole() { document.location = \"mtnoob.php?action=toot\"; return 0; } setTimeout( visitTootOriole, 4000 ); </script></html>" );
			}
		}

		if ( request.rawByteBuffer == null && request.responseText != null )
			request.rawByteBuffer = request.responseText.getBytes( "UTF-8" );
	}

	private void closeRelay( Socket socket, BufferedReader reader, PrintStream writer )
	{
		try
		{
			if ( reader != null )
				reader.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print stream is already closed.  Ignore.
		}

		try
		{
			if ( writer != null )
				writer.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print stream is already closed.  Ignore.
		}

		try
		{
			if ( socket != null )
				socket.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// socket is already closed.  Ignore.
		}

	}
}
