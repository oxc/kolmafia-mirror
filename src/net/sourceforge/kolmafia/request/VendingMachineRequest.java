/**
 * Copyright (c) 2005-2017, KoLmafia development team
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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

public class VendingMachineRequest
	extends CoinMasterRequest
{
	public static final String master = "Vending Machine"; 

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( VendingMachineRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( VendingMachineRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( VendingMachineRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(\\d+) fat loot token" );
	public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get( ItemPool.FAT_LOOT_TOKEN, 1 );

	public static final CoinmasterData VENDING_MACHINE =
		new CoinmasterData(
			VendingMachineRequest.master,
			"vendingmachine",
			VendingMachineRequest.class,
			"fat loot token",
			"no fat loot tokens",
			false,
			VendingMachineRequest.TOKEN_PATTERN,
			VendingMachineRequest.FAT_LOOT_TOKEN,
			null,
			VendingMachineRequest.itemRows,
			"shop.php?whichshop=damachine",
			"buyitem",
			VendingMachineRequest.buyItems,
			VendingMachineRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.SEWING_KIT:
					return InventoryManager.getCount( itemId ) == 0;
				}
				return super.canBuyItem( itemId );
			}
		};

	public VendingMachineRequest()
	{
		super( VendingMachineRequest.VENDING_MACHINE );
	}

	public VendingMachineRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( VendingMachineRequest.VENDING_MACHINE, buying, attachments );
	}

	public VendingMachineRequest( final boolean buying, final AdventureResult attachment )
	{
		super( VendingMachineRequest.VENDING_MACHINE, buying, attachment );
	}

	public VendingMachineRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( VendingMachineRequest.VENDING_MACHINE, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		VendingMachineRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.contains( "whichshop=damachine" ) )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=damachine" ) )
		{
			return false;
		}

		CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
