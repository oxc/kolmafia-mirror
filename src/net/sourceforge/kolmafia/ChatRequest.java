/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Responsible for handling all requests related to the Kingdom of Loathing
 * chat.  Note that once this thread is started, it will only stop if the
 * chat buffer on theis set to null.
 */

public class ChatRequest extends KoLRequest
{
	private static final Pattern LASTSEEN_PATTERN = Pattern.compile( "<!--lastseen:(\\d+)-->" );
	private static final int CHAT_DELAY = 8000;

	private static String lastSeen = "";
	private static ChatContinuationThread thread = null;

	/**
	 * Constructs a new <code>ChatRequest</code>.
	 * @param	client	Theto be updated
	 */

	public ChatRequest()
	{	this( "1" );
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given
	 * string to the server.
	 *
	 * @param	client	Theto be updated
	 * @param	message	The message to be sent
	 */

	public ChatRequest( String contact, String message )
	{
		super( "submitnewchat.php" );
		addFormField( "playerid", String.valueOf( KoLCharacter.getUserID() ) );
		addFormField( "pwd" );

		String contactID = KoLmafia.getPlayerID( contact );
		String actualMessage = null;

		if ( contact == null || (message != null && message.equals( "/exit" )) )
			actualMessage = message;
		else if ( message.equals( "/friend" ) || message.equals( "/ignore" ) || message.equals( "/baleet" ) )
			actualMessage = message + " " + contactID;
		else if ( message.startsWith( "/w " ) || message.startsWith( "/whisper" ) || message.startsWith( "/r" ) || message.startsWith( "/v" ) || message.startsWith( "/conv" ) )
			actualMessage = message;
		else
		{
			boolean foundMacro = false;
			for ( int i = 1; i <= 20; ++i )
				if ( message.startsWith( "/" + i ) )
					foundMacro = true;

			if ( foundMacro || contact.startsWith( "[" ) )
				actualMessage = message;
			else if ( contact.startsWith( "/" ) && (!message.startsWith( "/" ) || message.startsWith( "/me" ) || message.startsWith( "/em" ) || message.startsWith( "/warn" ) || message.startsWith( "/ann" )) )
				actualMessage = contact + " " + message;
			else if ( (message.equals( "/who" ) || message.equals( "/w" )) && contact.startsWith( "/" ) )
				actualMessage = "/who " + contact.substring(1);
			else if ( contact.startsWith( "/" ) && message.startsWith( "/" ) )
				actualMessage = message;
			else
				actualMessage = "/msg " + contactID.replaceAll( " ", "_" ) + " " + message;
		}

		addFormField( "graf", actualMessage );

		if ( (actualMessage.equals( "/c" ) || actualMessage.equals( "/channel" )) && actualMessage.indexOf( " " ) != -1 )
			KoLMessenger.stopConversation();

		if ( actualMessage.equals( "/exit" ) )
			KoLMessenger.dispose();
	}

	/**
	 * Constructs a new <code>ChatRequest</code> where the given parameter
	 * will be passed to the PHP file to indicate where you left off.  Note
	 * that this constructor is only available to the <code>ChatRequest</code>;
	 * this is done because only the <code>ChatRequest</code> knows what the
	 * appropriate value should be.
	 */

	private ChatRequest( String lastSeen )
	{
		super( "newchatmessages.php" );
		addFormField( "lasttime", lastSeen );
		ChatRequest.lastSeen = lastSeen;
	}

	public void run()
	{
		String graf = getFormField( "graf" );
		if ( graf != null && graf.startsWith( "/run" ) )
		{
			DEFAULT_SHELL.executeLine( graf.substring( 5 ) );
			KoLmafia.enableDisplay();
			return;
		}

		responseText = null;

		super.run();
	}

	protected void processResults()
	{
		if ( KoLMessenger.isRunning() && thread == null )
		{
			thread = new ChatContinuationThread();
			thread.start();
		}

		Matcher lastSeenMatcher = LASTSEEN_PATTERN.matcher( fullResponse );
		if ( lastSeenMatcher.find() )
			lastSeen = lastSeenMatcher.group(1);

		try
		{
			if ( KoLMessenger.isRunning() )
				KoLMessenger.updateChat( fullResponse );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Chat error" );
		}
	}

	/**
	 * An internal class used so that the previous request thread
	 * can die and a new one can begin.
	 */

	private class ChatContinuationThread extends Thread
	{
		public void run()
		{
			lastSeen = "";
			ChatRequest request = new ChatRequest( lastSeen );

			while ( delay( CHAT_DELAY ) && KoLMessenger.isRunning() )
			{
				// Before running the next request, you should wait for the
				// refresh rate indicated - this is likely the default rate
				// used for the KoLChat.

				try
				{
					request.run();
					request.clearDataFields();
					request.addFormField( "lasttime", String.valueOf( lastSeen ) );
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}
			}

			thread = null;
		}
	}
}
