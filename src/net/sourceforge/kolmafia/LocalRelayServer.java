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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRelayServer implements Runnable
{
	private static final int INITIAL_THREAD_COUNT = 10;

	private static long lastStatusMessage = 0;

	private static Thread relayThread = null;
	private static final LocalRelayServer INSTANCE = new LocalRelayServer();

	public static ArrayList agentThreads = new ArrayList();

	private ServerSocket serverSocket = null;
	private static int port = 60080;
	private static boolean listening = false;

	private static StringBuffer statusMessages = new StringBuffer();

	private LocalRelayServer()
	{
	}

	public static void startThread()
	{
		if ( relayThread != null )
			return;

		relayThread = new Thread( INSTANCE );
		relayThread.start();
	}

	public static int getPort()
	{	return port;
	}

	public static boolean isRunning()
	{	return listening;
	}

	public static void stop()
	{	listening = false;
	}

	public void run()
	{
		port = 60080;
		while ( !openServerSocket() )
		{
			if ( port <= 60089 )
				++port;
			else
				System.exit(-1);
		}

		listening = true;

		while ( listening )
		{
			try
			{
				dispatchAgent( serverSocket.accept() );
			}
			catch ( Exception e )
			{
				// If an exception occurs here, that means
				// someone closed the thread; just reset
				// the listening state and fall through.

				listening = false;
			}
		}

		closeAgents();

		try
		{
			if ( serverSocket != null )
				serverSocket.close();
		}
		catch ( Exception e )
		{
			// The end result of a socket closing
			// should not throw an exception, but
			// if it does, the socket closes.
		}

		serverSocket = null;
		relayThread = null;
	}

	private synchronized boolean openServerSocket()
	{
		try
		{
			serverSocket = new ServerSocket( port, 25, InetAddress.getByName( "127.0.0.1" ) );

			while ( agentThreads.size() < INITIAL_THREAD_COUNT )
				createAgent();

			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private synchronized void closeAgents()
	{
		while ( !agentThreads.isEmpty() )
		{
			LocalRelayAgent agent = (LocalRelayAgent) agentThreads.remove( 0 );
			agent.setSocket( null );
		}
	}

	private synchronized void dispatchAgent( Socket socket )
	{
		LocalRelayAgent agent = null;

		for ( int i = 0; i < agentThreads.size(); ++i )
		{
			agent = (LocalRelayAgent) agentThreads.get(i);

			if ( agent.isWaiting() )
			{
				agent.setSocket( socket );
				return;
			}
		}

		createAgent();
	}

	private synchronized LocalRelayAgent createAgent()
	{
		LocalRelayAgent agent = new LocalRelayAgent( agentThreads.size() );

		agentThreads.add( agent );
		agent.start();

		return agent;
	}

	public static void addStatusMessage( String message )
	{
		if ( System.currentTimeMillis() - lastStatusMessage < 4000 )
			statusMessages.append( message );
		else
			statusMessages.setLength(0);
	}

	public static String getNewStatusMessages()
	{
		String newMessages = statusMessages.toString();
		statusMessages.setLength(0);

		lastStatusMessage = System.currentTimeMillis();
		return newMessages;
	}
}
