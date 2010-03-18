/**
 * Copyright (c) 2005-2010, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *	  notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *	  notice, this list of conditions and the following disclaimer in
 *	  the documentation and/or other materials provided with the
 *	  distribution.
 *  [3] Neither the name "KoLmafia" nor the names of
 *	  its contributors may be used to endorse or promote products
 *	  derived from this software without specific prior written
 *	  permission.
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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public class LocalRelayServer
	implements Runnable
{
	public static final ArrayList agentThreads = new ArrayList();

	private static long lastStatusMessage = 0;
	private static Thread relayThread = null;

	private ServerSocket serverSocket = null;
	private static int port = 60080;
	private static boolean listening = false;
	private static boolean updateStatus = false;

	private static final LocalRelayServer INSTANCE = new LocalRelayServer();
	private static final StringBuffer statusMessages = new StringBuffer();

	private LocalRelayServer()
	{
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basics.css" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basement.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basics.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "chat.html" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "cli.html" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "hotkeys.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "onfocus.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "palinshelves.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "sorttable.js" );

		Preferences.setString( "lastRelayUpdate", StaticEntity.getVersion() );
	}

	public static final void updateStatus()
	{
		LocalRelayServer.updateStatus = true;
	}

	public static final void startThread()
	{
		if ( LocalRelayServer.relayThread != null )
		{
			return;
		}

		LocalRelayServer.relayThread = new Thread( LocalRelayServer.INSTANCE,
			"LocalRelayServer" );
		LocalRelayServer.relayThread.start();
	}

	public static final int getPort()
	{
		return LocalRelayServer.port;
	}

	public static final boolean isRunning()
	{
		return LocalRelayServer.listening;
	}

	public static final void stop()
	{
		LocalRelayServer.listening = false;
	}

	public void run()
	{
		LocalRelayServer.port = 60080;
		while ( !this.openServerSocket() )
		{
			if ( LocalRelayServer.port <= 60089 )
			{
				++LocalRelayServer.port;
			}
			else
			{
				System.exit( -1 );
			}
		}

		LocalRelayServer.listening = true;

		while ( LocalRelayServer.listening )
		{
			try
			{
				this.dispatchAgent( this.serverSocket.accept() );
			}
			catch ( Exception e )
			{
				// If an exception occurs here, that means
				// someone closed the thread; just reset
				// the listening state and fall through.

				LocalRelayServer.listening = false;
			}
		}

		this.closeAgents();

		try
		{
			if ( this.serverSocket != null )
			{
				this.serverSocket.close();
			}
		}
		catch ( Exception e )
		{
			// The end result of a socket closing
			// should not throw an exception, but
			// if it does, the socket closes.
		}

		this.serverSocket = null;
		LocalRelayServer.relayThread = null;
	}

	private synchronized boolean openServerSocket()
	{
		try
		{
			this.serverSocket = new ServerSocket( LocalRelayServer.port, 25, InetAddress.getByName( "127.0.0.1" ) );
			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private synchronized void closeAgents()
	{
		while ( !LocalRelayServer.agentThreads.isEmpty() )
		{
			LocalRelayAgent agent = (LocalRelayAgent) LocalRelayServer.agentThreads.remove( 0 );
			agent.setSocket( null );
		}
	}

	private synchronized void dispatchAgent( final Socket socket )
	{
		LocalRelayAgent agent = null;

		for ( int i = 0; i < LocalRelayServer.agentThreads.size(); ++i )
		{
			agent = (LocalRelayAgent) LocalRelayServer.agentThreads.get( i );

			if ( agent.isWaiting() )
			{
				agent.setSocket( socket );
				return;
			}
		}

		this.createAgent( socket );
	}

	private synchronized void createAgent( final Socket socket )
	{
		LocalRelayAgent agent = new LocalRelayAgent( LocalRelayServer.agentThreads.size() );
		agent.setSocket( socket );

		LocalRelayServer.agentThreads.add( agent );
		agent.start();
	}

	public static final void addStatusMessage( final String message )
	{
		if ( System.currentTimeMillis() - LocalRelayServer.lastStatusMessage < 4000 )
		{
			LocalRelayServer.statusMessages.append( message );
		}
	}

	public static final String getNewStatusMessages()
	{
		if ( LocalRelayServer.updateStatus )
		{
			LocalRelayServer.updateStatus = false;
			LocalRelayServer.statusMessages.append( "<!-- REFRESH -->" );
		}

		String newMessages = LocalRelayServer.statusMessages.toString();
		LocalRelayServer.statusMessages.setLength( 0 );

		LocalRelayServer.lastStatusMessage = System.currentTimeMillis();
		return newMessages;
	}
}
