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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoSellRequest extends SendMessageRequest
{
	public static final Pattern AUTOSELL_PATTERN = Pattern.compile( "for ([\\d,]+) [Mm]eat" );
	private static final Pattern EMBEDDED_ID_PATTERN = Pattern.compile( "item(\\d+)" );

	private int sellType;

	private int [] prices;
	private int [] limits;

	public static final int AUTOSELL = 1;
	public static final int AUTOMALL = 2;

	public AutoSellRequest( AdventureResult item )
	{	this( new AdventureResult [] { item }, AUTOSELL );
	}

	public AutoSellRequest( AdventureResult item, int price, int limit )
	{	this( new AdventureResult [] { item }, new int [] { price }, new int [] { limit }, AUTOMALL );
	}

	public AutoSellRequest( Object [] items, int sellType )
	{	this( items, new int[0], new int[0], sellType );
	}

	public AutoSellRequest( Object [] items, int [] prices, int [] limits, int sellType )
	{
		super( getSellPage( sellType ), items );

		this.sellType = sellType;
		this.prices = new int[ prices.length ];
		this.limits = new int[ limits.length ];

		if ( sellType == AUTOMALL )
		{
			this.addFormField( "action", "additem" );

			for ( int i = 0; i < prices.length; ++i )
				this.prices[i] = prices[i];

			for ( int i = 0; i < limits.length; ++i )
				this.limits[i] = limits[i];
		}
	}

	public String getItemField()
	{	return "whichitem";
	}

	public String getQuantityField()
	{	return "qty";
	}

	public String getMeatField()
	{	return "sendmeat";
	}

	private static final String getSellPage( int sellType )
	{
		if ( sellType == AUTOMALL )
			return "managestore.php";

		// Get the autosell mode the first time we need it
		if ( KoLCharacter.getAutosellMode().equals( "" ) )
			(new AccountRequest()).run();

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
			return "sellstuff_ugly.php";

		return "sellstuff.php";
	}

	public void attachItem( AdventureResult item, int index )
	{
		if ( this.sellType == AUTOMALL )
		{
			this.addFormField( "item" + index, String.valueOf( item.getItemId() ) );
			this.addFormField( this.getQuantityField() + index, String.valueOf( item.getCount() ) );

			this.addFormField( "price" + index, index - 1 >= this.prices.length || this.prices[ index - 1 ] == 0 ? "" : String.valueOf( this.prices[ index - 1 ] ) );
			this.addFormField( "limit" + index, index - 1 >= this.limits.length || this.limits[ index - 1 ] == 0 ? "" : String.valueOf( this.limits[ index - 1 ] ) );

			return;
		}

		// Autosell: "compact" or "detailed" mode

		this.addFormField( "action", "sell" );

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			if ( this.getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				this.addFormField( "quantity", String.valueOf( item.getCount() ) );
			}

			String itemId = String.valueOf( item.getItemId() );
			this.addFormField( "item" + itemId, itemId );
		}
		else
		{
			if ( this.getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				this.addFormField( "type", "quant" );
				this.addFormField( "howmany", String.valueOf( item.getCount() ) );
			}
			else
			{
				// Otherwise, we are selling all.  As of
				// 2/1/2006, must specify a quantity field even
				// for this - but the value is ignored

				this.addFormField( "type", "all" );
				this.addFormField( "howmany", "1" );
			}

			// This is a multiple selection input field.
			// Therefore, you can give it multiple items.

			this.addFormField( "whichitem[]", String.valueOf( item.getItemId() ), true );
		}
	}

	public int getCapacity()
	{
		// If you are attempting to send things to the mall,
		// the capacity is one.

		if ( this.sellType == AUTOMALL )
			return 11;

		// Otherwise, if you are autoselling multiple items,
		// then it depends on which mode you are using.

		int mode = KoLCharacter.getAutosellMode().equals( "detailed" ) ? 1 : 0;

		AdventureResult currentAttachment;
		int inventoryCount, attachmentCount;

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			currentAttachment = (AdventureResult) this.attachments[i];

			inventoryCount = currentAttachment.getCount( inventory );
			if ( inventoryCount == 0 )
				continue;

			attachmentCount = currentAttachment.getCount();

			if ( mode == 0 )
			{
				// We are in compact mode. If we are not
				// selling everything, we must do it one item
				// at a time
				if ( attachmentCount < inventoryCount )
					return 1;

				// Otherwise, look at remaining items
				continue;
			}

			if ( mode == 1 )
			{
				// We are in detailed "sell all" mode.
				if ( attachmentCount >= inventoryCount )
					continue;

				// ...but no longer
				if ( i == 0 && attachmentCount == inventoryCount - 1 )

				{
					// First item and we're selling one
					// less than max. Switch to detailed
					// "all but one" mode
					mode = 2;
					continue;
				}

				// Switch to "quantity" mode
				this.addFormField( "mode", "3" );
				return 1;
			}

			// We are in detailed "all but one" mode. This item had
			// better also be "all but one"

			if ( attachmentCount != inventoryCount - 1 )
			{
				// Nope. Switch to "quantity" mode
				this.addFormField( "mode", "3" );
				return 1;
			}

			// We continue in "all but one" mode
		}

		// We can sell all the items with the same mode.
		if ( mode > 0 )
		{
			// Add detailed "mode" field
			this.addFormField( "mode", String.valueOf( mode ) );
		}

		return Integer.MAX_VALUE;
	}

	public SendMessageRequest getSubInstance( Object [] attachments )
	{
		int [] prices = new int[ this.prices.length == 0 ? 0 : attachments.length ];
		int [] limits = new int[ this.prices.length == 0 ? 0 : attachments.length ];

		for ( int i = 0; i < prices.length; ++i )
		{
			for ( int j = 0; j < this.attachments.length; ++j )
				if ( attachments[i].equals( this.attachments[j] ) )
				{
					prices[i] = this.prices[i];
					limits[i] = this.limits[i];
				}
		}

		return new AutoSellRequest( attachments, prices, limits, this.sellType );
	}

	public void processResults()
	{
		super.processResults();

		if ( this.sellType == AUTOMALL )
		{
			// We placed stuff in the mall.
			StoreManager.update( this.responseText, false );

			if ( this.responseText.indexOf( "You don't have a store." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You don't have a store." );
				return;
			}

			KoLmafia.updateDisplay( "Items sold." );
			return;
		}

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
			StaticEntity.externalUpdate( "sellstuff_ugly.php", this.responseText );

		// Move out of inventory. Process meat gains, if old autosell
		// interface.

		KoLmafia.updateDisplay( "Items sold." );
		KoLCharacter.updateStatus();
	}

	public static final boolean registerRequest( String urlString )
	{
		Pattern itemPattern = null;
		Pattern quantityPattern = null;

		int quantity = 1;

		String sellType = null;

		if ( urlString.startsWith( "sellstuff.php" ) )
		{
			Matcher quantityMatcher = HOWMANY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
				quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			if ( urlString.indexOf( "type=allbutone" ) != -1 )
				quantity = -1;
			else if ( urlString.indexOf( "type=all" ) != -1 )
				quantity = 0;

			itemPattern = ITEMID_PATTERN;
			sellType = "autosell";
		}
		else if ( urlString.startsWith( "sellstuff_ugly.php" ) )
		{
			Matcher quantityMatcher = HOWMANY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
				quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			if ( urlString.indexOf( "mode=1" ) != -1 )
				quantity = 0;
			else if ( urlString.indexOf( "mode=2" ) != -1 )
				quantity = -1;

			itemPattern = EMBEDDED_ID_PATTERN;
			sellType = "autosell";
		}
		else if ( urlString.startsWith( "managestore.php" ) && urlString.indexOf( "action=additem" ) != -1 )
		{
			itemPattern = ITEMID_PATTERN;
			quantityPattern = QTY_PATTERN;
			sellType = "mallsell";
		}

		if ( itemPattern == null )
			return false;

		return registerRequest( sellType, urlString, itemPattern, quantityPattern, inventory, null, null, quantity );
	}

	public String getSuccessMessage()
	{	return "";
	}

	public boolean allowMementoTransfer()
	{	return false;
	}

	public boolean allowSingletonTransfer()
	{	return false;
	}

	public boolean allowUntradeableTransfer()
	{	return this.sellType == AUTOSELL;
	}

	public boolean allowUngiftableTransfer()
	{	return this.sellType == AUTOSELL;
	}

	public String getStatusMessage()
	{	return this.sellType == AUTOMALL ? "Transferring items to store" : "Autoselling items to NPCs";
	}
}
