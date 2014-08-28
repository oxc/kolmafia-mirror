/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class ChatPoller
	extends Thread
{
	private static final LinkedList<Object> chatHistoryEntries = new RollingLinkedList( 5 );

	private static long localLastSeen = 0;
	private static long serverLastSeen = 0;

	private static final int CHAT_DELAY = 500;
	private static final int CHAT_DELAY_COUNT = 16;

	private static String rightClickMenu = "";

	@Override
	public void run()
	{
		long lastSeen = 0;
		long serverLast = serverLastSeen;

		PauseObject pauser = new PauseObject();
		do
		{
			try
			{
				if ( serverLast == serverLastSeen )
				{
					List<HistoryEntry> entries = ChatPoller.getEntries( lastSeen, false );
					for ( HistoryEntry entry : entries )
					{
						lastSeen = Math.max( lastSeen, entry.getLocalLastSeen() );
					}
				}
				serverLast = serverLastSeen;
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace(e);
			}

			for ( int i = 0; i < ChatPoller.CHAT_DELAY_COUNT && ChatManager.isRunning(); ++i )
			{
				pauser.pause( ChatPoller.CHAT_DELAY );
			}
		}
		while ( ChatManager.isRunning() );
	}

	public static final void reset()
	{
		ChatPoller.chatHistoryEntries.clear();

		ChatPoller.localLastSeen = 0;
		ChatPoller.serverLastSeen = 0;
	}

	public synchronized static void addEntry( ChatMessage message )
	{
		HistoryEntry entry = new HistoryEntry( message, ++ChatPoller.localLastSeen );

		ChatPoller.chatHistoryEntries.add( entry );
		ChatManager.processMessages( entry.getChatMessages() );
	}

	public synchronized static void addSentEntry( final String responseText, final boolean isRelayRequest )
	{
		SentMessageEntry entry = new SentMessageEntry( responseText, ++localLastSeen, isRelayRequest );

		entry.executeAjaxCommand();

		ChatPoller.chatHistoryEntries.add( entry );
	}

	private static final void addValidEntry( final List<HistoryEntry> newEntries, final HistoryEntry entry, final boolean isRelayRequest )
	{
		if ( !( entry instanceof SentMessageEntry ) )
		{
			newEntries.add( entry );
			return;
		}

		if ( !isRelayRequest )
		{
			return;
		}

		SentMessageEntry sentEntry = (SentMessageEntry) entry;

		if ( sentEntry.isRelayRequest() )
		{
			return;
		}

		newEntries.add( entry );
		return;
	}

	public synchronized static List<HistoryEntry> getEntries( final long lastSeen, final boolean isRelayRequest )
	{
		if ( ChatManager.getCurrentChannel() == null )
		{
			ChatSender.sendMessage( null, "/listen", true );
		}

		List<HistoryEntry> newEntries = new ArrayList<HistoryEntry>();

		Iterator<Object> entryIterator = ChatPoller.chatHistoryEntries.iterator();

		if ( lastSeen != 0 )
		{
			while ( entryIterator.hasNext() )
			{
				HistoryEntry entry = (HistoryEntry) entryIterator.next();

				if ( entry.getLocalLastSeen() > lastSeen )
				{
					addValidEntry( newEntries, entry, isRelayRequest );

					while ( entryIterator.hasNext() )
					{
						addValidEntry( newEntries, (HistoryEntry) entryIterator.next(), isRelayRequest );
					}
				}
			}
		}

		ChatRequest request = null;

		request = new ChatRequest( ChatPoller.serverLastSeen );
		request.run();

		HistoryEntry entry = new HistoryEntry( request.responseText, ++ChatPoller.localLastSeen );
		ChatPoller.serverLastSeen = entry.getServerLastSeen();

		newEntries.add( entry );

		ChatPoller.chatHistoryEntries.add( entry );
		ChatManager.processMessages( entry.getChatMessages() );

		return newEntries;
	}

	public static final String getRightClickMenu()
	{
		if ( ChatPoller.rightClickMenu.equals( "" ) )
		{
			GenericRequest request = new GenericRequest( "lchat.php" );
			RequestThread.postRequest( request );

			int actionIndex = request.responseText.indexOf( "actions = {" );
			if ( actionIndex != -1 )
			{
				ChatPoller.rightClickMenu =
					request.responseText.substring( actionIndex, request.responseText.indexOf( ";", actionIndex ) + 1 );
			}
		}

		return ChatPoller.rightClickMenu;
	}

	public static void handleNewChat( String responseData, String sent )
	{
		try {
			JSONObject obj = new JSONObject( responseData );

			if ( obj.has( "last" ) )
			{
				serverLastSeen = obj.getLong( "last" );
			}

			List<ChatMessage> newMessages = new LinkedList<ChatMessage>();
			// note: output is where /who, /listen, + various game commands
			// (/use etc) output goes. May exist.
			String output = obj.optString( "output", null );
			if ( output != null )
			{
				// TODO: strip channelname so /cli works again.
				ChatSender.processResponse( newMessages, output, sent );
			}

			JSONArray msgs = obj.getJSONArray( "msgs" );
			for ( int i = 0; i < msgs.length(); i++ )
			{
				JSONObject msg = msgs.getJSONObject( i );
				String type = msg.getString( "type" );
				String sender = msg.has( "who" ) ? msg.getJSONObject( "who" ).getString( "name" ) : null;
				String senderId = msg.has( "who" ) ? msg.getJSONObject( "who" ).getString( "id" ) : null;
				String recipient = msg.has( "for" ) ? msg.getJSONObject( "for" ).optString( "name" ) : null;
				String content = msg.optString( "msg", null );
				if ( type.equals( "event" ) )
				{
					// TODO: handle events
					newMessages.add( new EventMessage( content, "green" ) );
					continue;
				}
				if ( sender == null )
				{
					ChatSender.processResponse( newMessages, content, sent );
					continue;
				}
				if ( recipient == null )
				{
					if ( type.equals( "system" ) )
					{
						newMessages.add( new ModeratorMessage( ChatManager.getCurrentChannel(), sender, senderId, content ) );
						continue;
					}
					recipient = type.equals( "public" ) ? "/" + msg.getString( "channel" ) : KoLCharacter.getUserName();
				}
				recipient = recipient.replaceAll( " ", "_" );
				// Apparently isAction corresponds to /em commands.
				boolean isAction = "1".equals( msg.optString( "format" ) );
				if ( isAction )
				{
					// username ends with "</b></font></a>"; remove trailing
					// </i>
					content = content.substring( content.indexOf("</a>" ) + 4, content.length() - 4 );
				}

				newMessages.add( new ChatMessage( sender, recipient, content, isAction ) );
			}

			ChatManager.processMessages( newMessages );
		}
		catch ( JSONException e )
		{
			e.printStackTrace();
		}

	}
}
