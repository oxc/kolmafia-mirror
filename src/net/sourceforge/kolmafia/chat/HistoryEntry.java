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

package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HistoryEntry
{
	private static final Pattern LASTSEEN_PATTERN = Pattern.compile( "<!--lastseen:(\\d+)-->" );
	private final String content;

	private final long localLastSeen;
	private final long serverLastSeen;

	private final boolean isInternal;
	private final List chatMessages;

	public HistoryEntry( final String responseText, final boolean isInternal, final long localLastSeen )
	{
		Matcher lastSeenMatcher = HistoryEntry.LASTSEEN_PATTERN.matcher( responseText );
		lastSeenMatcher.find();

		this.localLastSeen = localLastSeen;
		this.serverLastSeen = StringUtilities.parseLong( lastSeenMatcher.group( 1 ) );

		this.content = lastSeenMatcher.replaceFirst( "" );

		this.isInternal = isInternal;
		this.chatMessages = new ArrayList();

		ChatParser.parseLines( this.chatMessages, this.content );
	}

	public String getContent()
	{
		return this.content;
	}
	
	public long getLocalLastSeen()
	{
		return this.localLastSeen;
	}

	public long getServerLastSeen()
	{
		return this.serverLastSeen;
	}

	public boolean isInternal()
	{
		return this.isInternal;
	}

	public List getChatMessages()
	{
		return this.chatMessages;
	}
}
