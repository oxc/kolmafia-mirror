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

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.Preferences;

public class LogoutRequest
	extends GenericRequest
{
	private static boolean isRunning = false;
	private static String lastResponse = "";

	public LogoutRequest()
	{
		super( "logout.php" );
	}

	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	protected boolean retryOnTimeout()
	{
		return false;
	}

	public void run()
	{
		if ( LogoutRequest.isRunning )
		{
			return;
		}

		LogoutRequest.isRunning = true;
		KoLmafia.updateDisplay( "Preparing for logout..." );

		KoLAdventure.resetAutoAttack();

		ChatManager.dispose();
		BuffBotHome.setBuffBotActive( false );

		String scriptSetting = Preferences.getString( "logoutScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}
		
		if ( Preferences.getBoolean( "sharePriceData" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "spade prices http://kolmafia.us/scripts/updateprices.php" );
		}

		super.run();

		RequestLogger.closeSessionLog();
		RequestLogger.closeDebugLog();
		RequestLogger.closeMirror();

		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Logout request submitted." );
		GenericRequest.reset();
		KoLCharacter.reset( "" );

		LogoutRequest.isRunning = false;
	}

	public void processResults()
	{
		LogoutRequest.lastResponse = this.responseText;
	}

	public static final String getLastResponse()
	{
		return LogoutRequest.lastResponse;
	}
}
