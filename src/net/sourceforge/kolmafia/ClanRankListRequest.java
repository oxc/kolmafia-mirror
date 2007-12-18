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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanRankListRequest
	extends KoLRequest
{
	private static final Pattern RANK_PATTERN = Pattern.compile( "<select name=level.*?</select>" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option.*?>(.*?)</option>" );

	private final LockableListModel rankList;

	public ClanRankListRequest( final LockableListModel rankList )
	{
		super( "clan_members.php" );
		this.rankList = rankList;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving list of ranks..." );
		super.run();

		this.rankList.clear();
		Matcher ranklistMatcher = ClanRankListRequest.RANK_PATTERN.matcher( this.responseText );

		if ( ranklistMatcher.find() )
		{
			Matcher rankMatcher = ClanRankListRequest.OPTION_PATTERN.matcher( ranklistMatcher.group() );

			while ( rankMatcher.find() )
			{
				this.rankList.add( rankMatcher.group( 1 ).toLowerCase() );
			}
		}

		KoLmafia.updateDisplay( "List of ranks retrieved." );
	}
}