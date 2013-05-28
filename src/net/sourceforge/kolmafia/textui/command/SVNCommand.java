/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.svn.SVNManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SVNCommand
	extends AbstractCommand
{
	public SVNCommand()
	{
		this.usage = " - add new scripts to mafia from an svn repository.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.trim().equals( "update" ) )
		{
			// user wants to update everything
			SVNManager.doUpdate();
			return;
		}

		if ( parameters.startsWith( "checkout" ) )
		{
			String params = parameters.substring( 8 ).trim();
			if ( !params.startsWith( "svn" ) && !params.startsWith( "http" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You must specify a valid SVN url to update from. " + params );
				return;
			}

			SVNURL repo;
			try
			{
				repo = SVNURL.parseURIEncoded( params );
			}
			catch ( SVNException e1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid SVN URL" );
				return;
			}
			SVNManager.doCheckout( repo );
		}
		else if ( parameters.startsWith( "update" ) )
		{
			String params = parameters.substring( 6 ).trim();
			if ( !params.startsWith( "svn" ) && !params.startsWith( "http" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You must specify a valid SVN url to update from. " + params );
				return;
			}

			SVNURL repo;
			try
			{
				repo = SVNURL.parseURIEncoded( params );
			}
			catch ( SVNException e1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid SVN URL" );
				return;
			}
			SVNManager.doUpdate( repo );
		}
		else if ( parameters.startsWith( "delete" ) )
		{
			String params = parameters.substring( 6 ).trim();
			// user may specify a url
			if ( params.startsWith( "svn" ) || params.startsWith( "http" ) )
			{
				return;
			}
			
			// or user may specify a directory name
			String[] projects = KoLConstants.SVN_LOCATION.list();
			
			List<String> matches = StringUtilities.getMatchingNames( projects, params );
			
			if ( matches.size() > 1 )
			{
				RequestLogger.printList( matches );
				RequestLogger.printLine();

				KoLmafia.updateDisplay( MafiaState.ERROR, "[" + params + "] has too many matches." );
			}
			else if ( matches.size() == 1 )
			{
				SVNManager.deleteInstalledProject( matches.get( 0 ) );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "No script matching " + params + " is currently installed." );
			}
		}
		else if ( parameters.startsWith( "list" ) )
		{
			String[] projects = KoLConstants.SVN_LOCATION.list();
			RequestLogger.printList( Arrays.asList( projects ) );
		}
	}
}
