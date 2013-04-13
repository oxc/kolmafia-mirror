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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class JarlsbergRequest
	extends CreateItemRequest
{
	public JarlsbergRequest( final Concoction conc )
	{
		// shop.php?pwd&whichshop=jarl&action=buyitem&whichrow=60&quantity=1
		super( "shop.php", conc );

		this.addFormField( "whichshop", "jarl" );
		this.addFormField( "action", "buyitem" );
		int row = this.idToRow( this.getItemId() );
		this.addFormField( "whichrow", String.valueOf( row ) );
	}

	private static final int ID_TO_ROW_DIFFERENCE = 6143;
	private static final int STAFF_ID_TO_ROW_DIFFERENCE = 6154;

	private static final int idToRow( int itemId )
	{
		// The cosmic six-pack is the only one not in itemId order.
		if ( itemId == ItemPool.COSMIC_SIX_PACK )
		{
			return 112;
		}

		if ( itemId >= ItemPool.STAFF_OF_BREAKFAST )
		{
			return itemId - STAFF_ID_TO_ROW_DIFFERENCE;
		}

		int row = itemId - ID_TO_ROW_DIFFERENCE;

		// Mediocre lager appears in the middle of Jarlsberg consumables,
		// but isn't available in the Cosmic Kitchen.
		// Since it doesn't use a row, the row number of higher itemIds is shifted.
		if ( itemId > ItemPool.MEDIOCRE_LAGER )
		{
			row -= 1;
		}

		return row;
	}

	private static final int rowToId( int row )
	{
		// The cosmic six-pack is the only one not in itemId order.
		if ( row == 112 )
		{
			return ItemPool.COSMIC_SIX_PACK;
		}

		if ( row >= 104 )
		{
			return row + STAFF_ID_TO_ROW_DIFFERENCE;
		}

		int itemId = row + ID_TO_ROW_DIFFERENCE;

		// Mediocre lager appears in the middle of Jarlsberg consumables,
		// but isn't available in the Cosmic Kitchen.
		// Since it doesn't use a row, the row number of higher itemIds is shifted.
		if ( itemId >= ItemPool.MEDIOCRE_LAGER )
		{
			itemId += 1;
		}

		return itemId;
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
	public void run()
	{
		if ( !this.makeIngredients() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );
		this.addFormField( "quantity", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}

	@Override
	public void processResults()
	{
		JarlsbergRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=jarl" ) )
		{
			return;
		}

		if ( urlString.indexOf( "action=buyitem" ) == -1 )
		{
			return;
		}

		if ( responseText.indexOf( "You acquire" ) == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cosmic shopping was unsuccessful." );
			return;
		}

		Matcher rowMatcher = CreateItemRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		int itemId = JarlsbergRequest.rowToId( row );

		CreateItemRequest jarlsItem = CreateItemRequest.getInstance( itemId );
		if ( jarlsItem == null )
		{
			return; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = jarlsItem.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 2 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			ResultProcessor.processResult(
				ingredients[ i ].getInstance( -1 * ingredients[ i ].getCount() * quantity ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher rowMatcher = CreateItemRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !rowMatcher.find() )
		{
			return true;
		}

		int row = StringUtilities.parseInt( rowMatcher.group( 1 ) );
		int itemId = JarlsbergRequest.rowToId( row );

		CreateItemRequest jarlsItem = CreateItemRequest.getInstance( itemId );
		if ( jarlsItem == null )
		{
			return true; // this is an unknown item
		}

		int quantity = 1;
		if ( urlString.contains( "buymax=" ) )
		{
			quantity = jarlsItem.getQuantityPossible();
		}
		else
		{
			Matcher quantityMatcher = CreateItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				String quantityString = quantityMatcher.group( 2 ).trim();
				quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString );
			}
		}

		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );
		StringBuilder text = new StringBuilder();
		text.append( "Using " );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( i > 0 )
			{
				text.append( " + " );
			}

			text.append( ingredients[ i ].getCount() * quantity );
			text.append( " " );
			text.append( ingredients[ i ].getName() );
		}
		text.append( " to make " ).append( quantity ).append( " " ).append( jarlsItem.getName() );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( text.toString() );

		return true;
	}
}
