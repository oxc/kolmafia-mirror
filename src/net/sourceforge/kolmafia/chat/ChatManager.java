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

import java.awt.Toolkit;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ChannelColorsRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EventManager;

import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.ContactListFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.TabbedChatFrame;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.RollingLinkedList;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ChatManager
{
	private static final LinkedList<Object> clanMessages = new RollingLinkedList( 20 );
	private static final Set<String> validChatReplyRecipients = new HashSet<String>();

	private static final TreeMap<String, StyledChatBuffer> instantMessageBuffers = new TreeMap<String, StyledChatBuffer>();
	private static Entry<String,StyledChatBuffer>[] bufferEntries = new Entry[ 0 ];

	private static boolean isRunning = false;
	private static boolean checkedLiteracy = false;
	private static boolean chatLiterate = false;

	private static String currentChannel = null;

	private static List<String> activeWindows = new ArrayList<String>();
	public static List<String> activeChannels = new ArrayList<String>();

	private static TabbedChatFrame tabbedFrame = null;

	private static boolean triviaGameActive = false;
	private static int triviaGameIndex = 0;
	private static String triviaGameId = "[trivia0]";
	private static LockableListModel<String> triviaGameContacts = new LockableListModel<String>();
	private static ContactListFrame triviaGameContactListFrame = null;

	private static String faxbot = null;
	private static ChatMessage faxbotMessage = null;

	public static final void reset()
	{
		ChatManager.dispose();
		ChatPoller.reset();

		ChatManager.clanMessages.clear();
		ChatManager.instantMessageBuffers.clear();
		ChatManager.bufferEntries = new Entry[0];
		ChatManager.activeChannels.clear();
		ChatManager.currentChannel = null;

		ChatManager.triviaGameActive = false;
		ChatManager.triviaGameIndex = 0;
		ChatManager.triviaGameId = "[trivia0]";
		ChatManager.triviaGameContacts.clear();

		ChatManager.faxbot = null;
		ChatManager.faxbotMessage = null;
	}

	public static final void resetChatLiteracy()
	{
		ChatManager.checkedLiteracy = false;
	}

	public static final void resetClanMessages()
	{
		ChatManager.clanMessages.clear();
	}

	public static final boolean checkedChatLiteracy()
	{
		return ChatManager.checkedLiteracy;
	}

	public static final boolean getChatLiteracy()
	{
		return ChatManager.chatLiterate;
	}

	public static final void setChatLiteracy( final boolean on )
	{
		ChatManager.checkedLiteracy = true;
		ChatManager.chatLiterate = on;
	}

	public static final boolean chatLiterate()
	{
		// If login is incomplete because we are stuck in a fight or
		// choice, don't bother checking the Altar of Literacy

		if ( KoLmafia.isRefreshing() )
		{
			return true;
		}

		if( !ChatManager.checkedLiteracy )
		{
			ChatManager.chatLiterate = Preferences.getBoolean( "chatLiterate" );

			if ( !ChatManager.chatLiterate )
			{
				AltarOfLiteracyRequest request = new AltarOfLiteracyRequest();
				RequestThread.postRequest( request );
			}

			Preferences.setBoolean( "chatLiterate", ChatManager.chatLiterate );
		}

		return ChatManager.chatLiterate;
	}

	/**
	 * Initializes the chat buffer with the provided chat pane. Note that
	 * the chat refresher will also be initialized by calling this method;
	 * to stop the chat refresher, call the <code>dispose()</code> method.
	 */

	public static final void initialize()
	{
		if ( ChatManager.isRunning || !LoginRequest.completedLogin() )
		{
			return;
		}

		if ( !ChatManager.chatLiterate() )
		{
			KoLmafia.updateDisplay( "You cannot access chat until you complete the Altar of Literacy" );
			return;
		}

		ChatManager.isRunning = true;

		StyledChatBuffer.initializeHighlights();

		synchronized ( ChatManager.activeChannels )
		{
			for ( String channel : ChatManager.activeChannels )
			{
				ChatManager.openWindow( channel, false );
			}
		}

		ChatPoller.startInstance();

		RequestThread.postRequest( new ChannelColorsRequest() );
	}

	public static final void setFaxBot( final String faxbot )
	{
		ChatManager.faxbot = faxbot;
	}

	public static synchronized final String getLastFaxBotMessage()
	{
		if ( ChatManager.faxbotMessage != null )
		{
			String message = ChatManager.faxbotMessage.getContent();
			ChatManager.faxbotMessage = null;
			return message;
		}
		return null;
	}

	public static final boolean isValidChatReplyRecipient( String playerName )
	{
		synchronized ( ChatManager.validChatReplyRecipients )
		{
			return validChatReplyRecipients.contains( playerName );
		}
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public static final void dispose()
	{
		if ( ChatManager.isRunning )
		{
			ChatManager.isRunning = false;
			ChatSender.sendMessage( null, "/exit", false );
		}

		ChatPoller.stopInstance();

		ChatManager.activeWindows.clear();
		ChatManager.activeChannels.clear();
		ChatManager.currentChannel = null;

		ChatManager.tabbedFrame = null;
	}

	public static final boolean isRunning()
	{
		return ChatManager.isRunning;
	}

	public static final String getCurrentChannel()
	{
		return ChatManager.currentChannel;
	}

	public static final StyledChatBuffer getBuffer( final String bufferKey )
	{
		StyledChatBuffer buffer = ChatManager.instantMessageBuffers.get( bufferKey );

		if ( buffer != null )
		{
			return buffer;
		}

		buffer = new StyledChatBuffer( bufferKey, "black", !bufferKey.equals( "[high]" ) );

		if ( Preferences.getBoolean( "logChatMessages" ) )
		{
			String fileSuffix = bufferKey;

			if ( fileSuffix.startsWith( "/" ) )
			{
				fileSuffix = "[" + fileSuffix.substring( 1 ) + "]";
			}

			StringBuilder fileName = new StringBuilder();
			fileName.append( KoLConstants.DAILY_FORMAT.format( new Date() ) );
			fileName.append( "_" );
			fileName.append( KoLCharacter.baseUserName() );
			fileName.append( "_" );
			fileName.append( fileSuffix );
			fileName.append( ".html" );

			buffer.setLogFile( new File( KoLConstants.CHATLOG_LOCATION, fileName.toString() ) );
		}

		synchronized ( ChatManager.bufferEntries )
		{
			ChatManager.instantMessageBuffers.put( bufferKey, buffer );
			ChatManager.bufferEntries = new Entry[ ChatManager.instantMessageBuffers.size() ];
			ChatManager.instantMessageBuffers.entrySet().toArray( ChatManager.bufferEntries );
		}

		return buffer;
	}

	public static final void startTriviaGame()
	{
		ChatManager.triviaGameContacts.clear();
		ChatManager.triviaGameId = "[trivia" + (++ChatManager.triviaGameIndex) + "]";
		ChatManager.triviaGameActive = true;

		if ( ChatManager.triviaGameContactListFrame == null )
		{
			ChatManager.triviaGameContactListFrame = new ContactListFrame( ChatManager.triviaGameContacts );
		}

		ChatManager.triviaGameContactListFrame.setTitle( "Contestants for " + triviaGameId );
		ChatManager.triviaGameContactListFrame.setVisible( true );
	}

	public static final void stopTriviaGame()
	{
		ChatManager.triviaGameActive = false;
	}

	public static void processMessages( final List<ChatMessage> messages )
	{
		for ( ChatMessage message : messages )
		{
			ChatManager.processMessage( message );
		}
	}

	private static void displayMessage( final String bufferKey, final boolean open, final String message )
	{
		ChatManager.openWindow( bufferKey, open );
		StyledChatBuffer buffer = ChatManager.getBuffer( bufferKey );
		buffer.append( message );
	}

	public static void processMessage( final ChatMessage message )
	{
		if ( message instanceof EventMessage )
		{
			ChatManager.processEvent( (EventMessage) message );
			return;
		}

		if ( message instanceof EnableMessage )
		{
			ChatManager.processChannelEnable( (EnableMessage) message );
			return;
		}

		if ( message instanceof DisableMessage )
		{
			ChatManager.processChannelDisable( (DisableMessage) message );
			return;
		}

		String sender = message.getSender();
		String recipient = message.getRecipient();

		if ( message instanceof ModeratorMessage )
		{
			// Format the message
			String displayHTML = ChatFormatter.formatChatMessage( message );

			// Display the message in the current channel
			ChatManager.displayMessage( ChatManager.getBufferKey( recipient ), true, displayHTML );

			// If it's a System Message, also put it into Events.
			if ( sender.equals( "System Message" ) )
			{
				ChatManager.displayMessage( "[events]", false, displayHTML );
			}
			return;
		}

		if ( ChatManager.faxbot != null && sender.equalsIgnoreCase( ChatManager.faxbot ) )
		{
			ChatManager.faxbotMessage = message;
		}

		String content = message.getContent();

		if ( recipient == null )
		{
			ChatManager.processCommand( sender, content, recipient );
		}
		else if ( recipient.equals( "/clan" ) ||
			  recipient.equals( "/hobopolis" ) ||
			  recipient.equals( "/slimetube" ) ||
			  recipient.equals( "/dread" ) ||
			  recipient.equals( "/hauntedhouse" ) )
		{
			ChatManager.clanMessages.add( message );
			ChatManager.processCommand( sender, content, recipient );
		}
		else if ( recipient.equals( "/talkie" ) )
		{
			// Allow chatbot scripts to process talkie messages
			ChatManager.processCommand( sender, content, recipient );
		}
		else if ( Preferences.getBoolean( "chatBeep" ) && ( StringUtilities.globalStringReplace( KoLCharacter.getUserName(), " ", "_" ).equalsIgnoreCase( recipient ) ) )
		{
			Toolkit.getDefaultToolkit().beep();
		}

		String destination = recipient;

		if ( KoLCharacter.getUserName().equals( recipient ) )
		{
			if ( ChatManager.triviaGameActive )
			{
				if ( !ChatManager.triviaGameContacts.contains( message.getSender() ) )
				{
					ChatManager.triviaGameContacts.add( message.getSender() );
				}
			}

			ChatManager.processCommand( sender, content, "" );
			destination = sender;
		}

		// Display the message in the appropriate tab
		ChatManager.displayMessage( ChatManager.getBufferKey( destination ), true, ChatFormatter.formatChatMessage( message ) );
	}

	public static final String getBufferKey( String destination )
	{
		String bufferKey = destination.toLowerCase();

		if ( Preferences.getBoolean( "mergeHobopolisChat" ) )
		{
			if ( destination.equals( "/hobopolis" ) ||
			     destination.equals( "/slimetube" ) ||
			     destination.equals( "/dread" ) ||
			     destination.equals( "/hauntedhouse" ) )
			{
				bufferKey = "/clan";
			}
		}

		if ( !bufferKey.startsWith( "/" ) && ChatManager.triviaGameActive )
		{
			bufferKey = triviaGameId;
		}

		return bufferKey;
	}

	public static final void processEvent( final EventMessage message )
	{
		if ( Preferences.getBoolean( "greenScreenProtection" ) || BuffBotHome.isBuffBotActive() || message.isHidden() )
		{
			return;
		}

		String content = message.getContent();

		if ( content.indexOf( " has " ) != -1 )
		{
			switch ( KoLmafia.displayState )
			{
			case ABORT:
			case ERROR:
			case ENABLE:
				ApiRequest.updateStatus( true );
			}
		}

		EventManager.addChatEvent( ChatFormatter.formatChatMessage( message, false ) );
		String cleanContent = KoLConstants.ANYTAG_PATTERN.matcher( content ).replaceAll( "" );
		ChatManager.processCommand( "", cleanContent, "Events" );
		ChatManager.broadcastEvent( message );
	}

	public static final void processChannelEnable( final EnableMessage message )
	{
		String sender = message.getSender();

		if ( !ChatManager.activeChannels.contains( sender ) )
		{
			String bufferKey = ChatManager.getBufferKey( sender );
			ChatManager.activeChannels.add( sender );
			ChatManager.openWindow( bufferKey, false );
		}

		if ( message.isTalkChannel() )
		{
			ChatManager.currentChannel = sender;
		}
	}

	public static final void processChannelDisable( final DisableMessage message )
	{
		String sender = message.getSender();

		if ( ChatManager.activeChannels.contains( sender ) )
		{
			String bufferKey = ChatManager.getBufferKey( sender );
			ChatManager.activeChannels.remove( sender );
			ChatManager.closeWindow( bufferKey );
		}
	}

	public static final void processCommand( final String sender, final String content, final String channel )
	{
		if ( sender == null || content == null )
		{
			return;
		}

		// If a buffbot is running, certain commands become active, such
		// as help, restores, and logoff.

		if ( "".equals( channel ) && BuffBotHome.isBuffBotActive() )
		{
			if ( content.equalsIgnoreCase( "help" ) )
			{
				ChatSender.sendMessage( sender, "Please check my profile.", false );
				return;
			}

			if ( content.equalsIgnoreCase( "restores" ) )
			{
				ChatSender.sendMessage(
					sender, "I currently have " + RecoveryManager.getRestoreCount() + " mana restores at my disposal.", false );

				return;
			}

			if ( content.equalsIgnoreCase( "logoff" ) )
			{
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Logoff requested by " + sender );

				if ( ClanManager.isCurrentMember( sender ) )
				{
					KoLmafia.quit();
				}

				BuffBotHome.update( BuffBotHome.ERRORCOLOR, sender + " added to ignore list" );
				ChatSender.sendMessage( sender, "/baleet", false );
				return;
			}
		}

		// Otherwise, sometimes clannies want to take advantage of KoLmafia's
		// automatic chat logging.  In that case...

		if ( "".equals( channel ) && content.equalsIgnoreCase( "update" ) )
		{
			if ( !Preferences.getBoolean( "chatServesUpdates" ) )
			{
				KoLmafia.updateDisplay( sender + " tried to retrieve recent clan chat messages from you, but you have that feature disabled." );
				return;
			}
			if ( !ClanManager.isCurrentMember( sender ) )
			{
				KoLmafia.updateDisplay( sender + " tried to retrieve recent clan chat messages from you, but they are not in your clan." );
				return;
			}

			StringBuilder mailContent = new StringBuilder();

			Iterator<Object> clanMessageIterator = ChatManager.clanMessages.iterator();

			while ( clanMessageIterator.hasNext() )
			{
				ChatMessage message = (ChatMessage) clanMessageIterator.next();
				String cleanMessage = KoLConstants.ANYTAG_PATTERN.matcher( ChatFormatter.formatChatMessage( message ) ).replaceAll( "" );

				mailContent.append( cleanMessage );
				mailContent.append( "\n" );
			}

			RequestThread.postRequest( new SendMailRequest( sender, mailContent.toString() ) );
			KoLmafia.updateDisplay( "Recent clan chat messages were sent to " + sender );
			return;
		}

		ChatManager.invokeChatScript( sender, content, channel );
	}

	public static final void invokeChatScript( final String sender, final String content, final String channel )
	{
		String scriptName = Preferences.getString( "chatbotScript" );
		if ( scriptName.equals( "" ) )
		{
			return;
		}

		List<File> scriptFiles = KoLmafiaCLI.findScriptFile( scriptName );
		Interpreter interpreter = KoLmafiaASH.getInterpreter( scriptFiles );
		if ( interpreter == null )
		{
			return;
		}

		String name = scriptFiles.get( 0 ).getName();
		int parameterCount = interpreter.getParser().getMainMethod().getVariableReferences().size();
		String[] scriptParameters;

		if ( parameterCount == 3 )
		{
			scriptParameters = new String[]
			{
				sender,
				content,
				channel
			};
		}
		else if ( channel != null && !channel.equals( "" ) )
		{
			scriptParameters = new String[]
			{
				sender,
				content
			};
		}
		else
		{
			return;
		}

		synchronized ( ChatManager.validChatReplyRecipients )
		{
			ChatManager.validChatReplyRecipients.add( sender );
		}

		synchronized ( interpreter )
		{
			KoLmafiaASH.logScriptExecution( "Starting chat script: ", name, interpreter );
			interpreter.execute( "main", scriptParameters );
			KoLmafiaASH.logScriptExecution( "Finished chat script: ", name, interpreter );
		}

		synchronized ( ChatManager.validChatReplyRecipients )
		{
			ChatManager.validChatReplyRecipients.remove( sender );
		}
	}

	public static final void broadcastEvent( final EventMessage message )
	{
		if ( !ChatManager.isRunning() )
		{
			return;
		}

		String displayHTML = ChatFormatter.formatChatMessage( message );

		StyledChatBuffer buffer = ChatManager.getBuffer( "[events]" );
		buffer.append( displayHTML );

		if ( message instanceof InternalMessage )
		{
			ChatManager.openWindow( "[events]", true );
		}
		else
		{
			ChatManager.openWindow( "[events]", false );

			synchronized ( ChatManager.bufferEntries )
			{
				for ( Entry<String,StyledChatBuffer> entry : ChatManager.bufferEntries )
				{
					String key = entry.getKey();

					if ( key.equals( "[events]" ) )
					{
						continue;
					}

					buffer = entry.getValue();

					buffer.append( displayHTML );
				}
			}
		}
	}

	public static final void openWindow( final String bufferKey, boolean highlightOnOpen )
	{
		if ( StaticEntity.isHeadless() || !ChatManager.isRunning || bufferKey == null )
		{
			return;
		}

		if ( ChatManager.activeWindows.contains( bufferKey ) )
		{
			if ( ChatManager.tabbedFrame != null )
			{
				ChatManager.tabbedFrame.highlightTab( bufferKey );
			}

			return;
		}

		ChatManager.activeWindows.add( bufferKey );

		if ( Preferences.getBoolean( "useTabbedChatFrame" ) )
		{
			if ( ChatManager.tabbedFrame == null )
			{
				CreateFrameRunnable creator = new CreateFrameRunnable( TabbedChatFrame.class );
				boolean appearsInTab = GenericFrame.appearsInTab( "ChatManager" );
				ChatManager.tabbedFrame = (TabbedChatFrame) creator.createFrame( appearsInTab );
			}

			ChatManager.tabbedFrame.addTab( bufferKey );

			if ( highlightOnOpen )
			{
				ChatManager.tabbedFrame.highlightTab( bufferKey );
			}

			return;
		}

		ChatFrame frame = new ChatFrame( bufferKey );
		CreateFrameRunnable.decorate( frame );
		frame.setVisible( true );
	}

	public static final void closeWindow( String closedWindow )
	{
		if ( closedWindow == null )
		{
			ChatManager.dispose();
			return;
		}

		ChatManager.activeWindows.remove( closedWindow );

		if ( !ChatManager.isRunning() || !closedWindow.startsWith( "/" ) )
		{
			return;
		}

		if ( !ChatManager.activeChannels.contains( closedWindow ) )
		{
			return;
		}

		if ( !closedWindow.equals( ChatManager.getCurrentChannel() ) )
		{
			ChatSender.sendMessage( closedWindow, "/listen", false );
			return;
		}

		String selectedWindow = null;

		for ( String channel : ChatManager.activeChannels )
		{
			if ( channel.startsWith( "/" ) && !channel.equals( closedWindow ) )
			{
				selectedWindow = channel;
				break;
			}
		}

		if ( selectedWindow != null )
		{
			ChatSender.sendMessage( selectedWindow, "/channel", false );
			return;
		}
	}

	public static final void checkFriends()
	{
		ChatSender.sendMessage( null, "/friends", false );
	}

	public static void applyHighlights()
	{
		for ( Entry<String,StyledChatBuffer> entry : ChatManager.bufferEntries )
		{
			StyledChatBuffer buffer = entry.getValue();
			buffer.applyHighlights();
		}
	}
}
