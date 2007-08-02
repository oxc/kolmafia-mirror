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

public class GreenMessageRequest extends SendMessageRequest
{
	private boolean isInternal;
	private String recipient, message;

	public GreenMessageRequest( String recipient, String message )
	{
		super( "sendmessage.php" );

		this.recipient = recipient;
		this.message = message;

		this.addFormField( "action", "send" );
		this.addFormField( "towho", KoLmafia.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public GreenMessageRequest( String recipient, KoLmafiaASH script )
	{
		super( "sendmessage.php" );

		this.recipient = recipient;
		this.message = "I have opted to let you know that I have chosen to run <" + script.getFileName() + ">.  Thanks for writing this script!";

		this.addFormField( "action", "send" );
		this.addFormField( "towho", KoLmafia.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public GreenMessageRequest( String recipient, String message, AdventureResult attachment )
	{
		super( "sendmessage.php", attachment );

		this.recipient = recipient;
		this.message = RequestEditorKit.getUnicode( message );

		this.addFormField( "action", "send" );
		this.addFormField( "towho", KoLmafia.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public GreenMessageRequest( String recipient, String message, Object [] attachments, boolean isInternal )
	{
		super( "sendmessage.php", attachments );

		this.recipient = recipient;
		this.message = RequestEditorKit.getUnicode( message );

		this.addFormField( "action", "send" );
		this.addFormField( "towho", KoLmafia.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		if ( !isInternal )
			this.addFormField( "savecopy", "on" );

		this.isInternal = isInternal;
	}

	public String getRecipient()
	{	return this.recipient;
	}

	public int getCapacity()
	{	return 11;
	}

	public SendMessageRequest getSubInstance( Object [] attachments )
	{	return new GreenMessageRequest( this.recipient, this.message, attachments, this.isInternal );
	}

	public String getSuccessMessage()
	{	return "<center>Message ";
	}

	public String getStatusMessage()
	{	return "Sending kmail to " + KoLmafia.getPlayerName( this.recipient );
	}

	public String getItemField()
	{	return "whichitem";
	}

	public String getQuantityField()
	{	return "howmany";
	}

	public String getMeatField()
	{	return "sendmeat";
	}

	public boolean allowMementoTransfer()
	{	return true;
	}

	public boolean allowUntradeableTransfer()
	{	return true;
	}

	public static final boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "sendmessage.php" ) )
			return false;

		return registerRequest( "send a kmail", urlString, inventory, null, "sendmeat", 0 );
	}
}

