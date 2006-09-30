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

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * An extension of <code>KoLRequest</code> which handles logins.
 * A new instance is created and started for every login attempt,
 * and in the event that it is successful, theprovided
 * at construction time will be notified of the success.
 */

public class LoginRequest extends KoLRequest
{
	private static final Pattern FAILURE_PATTERN = Pattern.compile( "<p><b>(.*?)</b>" );
	private static final Pattern CHALLENGE_PATTERN = Pattern.compile( "<input type=hidden name=challenge value=\"([^\"]*?)\">" );

	private static String lastUsername;
	private static String lastPassword;

	private static int STANDARD_WAIT = 75;
	private static int TOO_MANY_WAIT = 960;
	private static int ROLLOVER_WAIT = 1800;
	private static int BAD_CHALLENGE_WAIT = 15;

	private static int waitTime = STANDARD_WAIT;
	private static boolean instanceRunning = false;

	private String username;
	private String password;
	private boolean savePassword;
	private boolean getBreakfast;
	private boolean isQuickLogin;

	/**
	 * Constructs a new <code>LoginRequest</code>.  The given
	 *will be notified in the event of success.
	 *
	 * @param	client	Theassociated with this <code>LoginRequest</code>
	 * @param	loginname	The name of the player to be logged in
	 * @param	password	The password to be used in the login attempt
	 * @param	getBreakfast	Whether or not theshould retrieve breakfast after login
	 */

	public LoginRequest( String username, String password )
	{	this( username, password, true, StaticEntity.getGlobalProperty( username, "getBreakfast" ).equals( "true" ), false );
	}

	public LoginRequest( String username, String password, boolean savePassword, boolean getBreakfast, boolean isQuickLogin )
	{
		super( "login.php" );

		this.username = username == null ? "" : StaticEntity.globalStringReplace( username, "/q", "" );
		this.password = password;
		this.savePassword = savePassword;
		this.getBreakfast = getBreakfast;
		this.isQuickLogin = isQuickLogin;
	}

	/**
	 * Handles the challenge in order to send the password securely
	 * via KoL.
	 */

	public void detectChallenge()
	{
		KoLmafia.updateDisplay( "Validating login server..." );

		// Setup the login server in order to ensure that
		// the initial try is randomized.  Or, in the case
		// of a devster, the developer server.

		KoLRequest.applySettings();

		if ( username.toLowerCase().startsWith( "devster" ) )
			setLoginServer( "dev.kingdomofloathing.com" );

		if ( !KOL_HOST.equals( "dev.kingdomofloathing.com" ) )
			setLoginServer( "www.kingdomofloathing.com" );

		clearDataFields();
		super.run();

		// If the pattern is not found, then do not submit
		// the challenge version.

		Matcher challengeMatcher = CHALLENGE_PATTERN.matcher( responseText );
		if ( !challengeMatcher.find() )
		{
			clearDataFields();
			addFormField( "password", this.password );

			return;
		}

		// We got this far, so that means we now have a
		// challenge pattern.

		try
		{
			clearDataFields();
			String challenge = challengeMatcher.group(1);

			addFormField( "secure", "on" );
			addFormField( "challenge", challenge );
			addFormField( "response", digestPassword( this.password, challenge ) );

			return;
		}
		catch ( Exception e )
		{
			clearDataFields();
			addFormField( "password", this.password );
			return;
		}
	}

	private static String digestPassword( String password, String challenge ) throws Exception
	{
		// KoL now makes use of a HMAC-MD5 in order to preprocess the
		// password so that we aren't submitting plaintext passwords
		// all the time.  Here is the implementation.  Note that the
		// password is processed two times.

		MessageDigest digester = MessageDigest.getInstance( "MD5" );
		byte [] key = getHexString( digester.digest( password.getBytes() ) ).getBytes();
		digester.reset();

		byte [] data = challenge.getBytes();

		// Step 1, compute [ K ]
		byte [] step1 = new byte[64];
		for ( int i = 0; i < key.length; ++i )
			step1[i] = key[i];

		// Step 2, compute [ K XOR ipad ]
		byte [] step2 = new byte[64];
		for ( int i = 0; i < 64; ++i )
			step2[i] = (byte) (step1[i] ^ 0x36);

		// Step 3, compute [ H( K XOR ipad, text ) ]
		digester.update( step2 );
		digester.update( data );
		byte [] step3 = digester.digest();
		digester.reset();

		// Step 4, compute [ K XOR opad ]
		byte [] step4 = new byte[64];
		for ( int i = 0; i < 64; ++i )
			step4[i] = (byte) (step1[i] ^ 0x5c);

		// Step 5, compute [ H( K XOR opad, H( K XOR ipad, text ) ) ]
		digester.update( step4 );
		digester.update( step3 );
		byte [] step5 = digester.digest();
		digester.reset();

		return getHexString( step5 );
	}

	private static String getHexString( byte [] bytes )
	{
		byte [] output = new byte[ bytes.length + 1 ];
		for ( int i = 0; i < bytes.length; ++i )
			output[i+1] = bytes[i];

		StringBuffer result = new StringBuffer( (new BigInteger( output )).toString( 16 ) );

		while ( result.length() < bytes.length * 2 )
			result.insert( 0, '0' );

		while ( result.length() > bytes.length * 2 )
			result.delete( 0, result.length() - (bytes.length * 2) );

		return result.toString();
	}

	/**
	 * Runs the <code>LoginRequest</code>.  This method determines
	 * whether or not the login was successful, and updates the
	 * display or notifies the as appropriate.
	 */

	public void run()
	{
		if ( instanceRunning )
			return;

		instanceRunning = true;
		lastUsername = username;
		lastPassword = password;

		try
		{
			KoLmafia.forceContinue();

			while ( !KoLmafia.refusesContinue() && executeLogin() )
			{
				KoLmafia.forceContinue();
				StaticEntity.executeCountdown( "Next login attempt in ", waitTime );
			}
		}
		catch ( Exception e )
		{
			// It's possible that all the login hangups are due
			// to an exception in executeLogin().  Let's try to
			// catch it.

			StaticEntity.printStackTrace( e );
		}

		instanceRunning = false;
	}

	public static void executeTimeInRequest()
	{	executeTimeInRequest( false );
	}

	public static void executeTimeInRequest( boolean isRollover )
	{
		sessionID = null;
		waitTime = isRollover ? ROLLOVER_WAIT : STANDARD_WAIT;

		LoginRequest loginAttempt = new LoginRequest( lastUsername, lastPassword, false, false, true );
		loginAttempt.run();

		if ( sessionID != null )
			KoLmafia.updateDisplay( "Session timed-in." );

	}

	public static boolean isInstanceRunning()
	{	return instanceRunning;
	}

	public boolean executeLogin()
	{
		sessionID = null;

		if ( !StaticEntity.getBooleanProperty( "useSecureLogin" ) )
		{
			clearDataFields();
			addFormField( "password", this.password );
		}
		else
		{
			clearDataFields();
			detectChallenge();
		}

		addFormField( "loggingin", "Yup." );
		addFormField( "loginname", this.username + "/q" );

		KoLmafia.updateDisplay( "Sending login request..." );
		waitTime = STANDARD_WAIT;
		super.run();

		if ( responseCode == 302 && redirectLocation.equals( "maint.php" ) )
		{
			// Nightly maintenance, so KoLmafia should retry.
			// Therefore, return true.

			waitTime = ROLLOVER_WAIT;
			return true;
		}
		else if ( responseCode == 302 && redirectLocation.startsWith( "main" ) )
		{
			if ( redirectLocation.equals( "main_c.html" ) )
				KoLRequest.isCompactMode = true;

			// If the login is successful, you notify the client
			// of success.  But first, if there was a desire to
			// save the password, do so here.

			if ( this.savePassword )
				KoLmafia.addSaveState( username, password );

			KoLmafia.forceContinue();
			sessionID = formConnection.getHeaderField( "Set-Cookie" );

			KoLmafia.updateDisplay( "Initializing session for " + username + "..." );
			StaticEntity.getClient().initialize( username, this.getBreakfast, this.isQuickLogin );

			return false;
		}
		else if ( responseCode == 302 )
		{
			// It's possible that KoL will eventually make the redirect
			// the way it used to be, but enforce the redirect.  If this
			// happens, then validate here.

			Matcher matcher = REDIRECT_PATTERN.matcher( redirectLocation );
			if ( matcher.find() )
			{
				setLoginServer( matcher.group(1) );
				return executeLogin();
			}
		}
		else if ( responseText.indexOf( "wait a minute" ) != -1 )
		{
			// Ooh, logged in too fast.  KoLmafia should recognize this and
			// try again automatically in 75 seconds.

			waitTime = STANDARD_WAIT;
			return true;
		}
		else if ( responseText.indexOf( "wait fifteen minutes" ) != -1 )
		{
			// Ooh, logged in too fast.  KoLmafia should recognize this and
			// try again automatically in 1000 seconds.

			waitTime = TOO_MANY_WAIT;
			return true;
		}

		Matcher failureMatcher = FAILURE_PATTERN.matcher( responseText );
		if ( failureMatcher.find() );
			KoLmafia.updateDisplay( failureMatcher.group(1) );

		waitTime = BAD_CHALLENGE_WAIT;
		return true;
	}

	protected boolean mayChangeCreatables()
	{	return false;
	}
}
