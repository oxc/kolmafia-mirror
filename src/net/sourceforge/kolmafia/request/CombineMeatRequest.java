/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.regex.Matcher;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class CombineMeatRequest
	extends CreateItemRequest
{
	private final int meatType;
	private final int costToMake;

	public CombineMeatRequest( final int meatType )
	{
		super( "craft.php", meatType );

		this.addFormField( "action", "makepaste" );
		this.addFormField( "whichitem", String.valueOf( meatType ) );
		this.addFormField( "ajax", "1" );

		this.meatType = meatType;
		this.costToMake =
			meatType == ItemPool.MEAT_PASTE ? -10 : meatType == ItemPool.MEAT_STACK ? -100 : -1000;
	}

	public void reconstructFields()
	{
	}

	public void run()
	{
		if ( this.costToMake * this.getQuantityNeeded() > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Insufficient funds to make meat paste." );
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + ItemDatabase.getItemName( this.meatType ) + "..." );
		this.addFormField( "qty", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}

	public void processResults()
	{
		super.processResults();
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher itemMatcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return false;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );

		Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
		int quantity = quantityMatcher.find() ? StringUtilities.parseInt( quantityMatcher.group( 2 ) ) : 1;

		RequestLogger.updateSessionLog( "Create " + quantity + " " + ItemDatabase.getItemName( itemId ) );

		int cost = itemId == ItemPool.MEAT_PASTE ? 10 : itemId == ItemPool.MEAT_STACK ? 100 : 1000;
		int total = cost * quantity;
		if ( total <= KoLCharacter.getAvailableMeat() )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -1 * total ) );
		}

		return true;
	}
}
