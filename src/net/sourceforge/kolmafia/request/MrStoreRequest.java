/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MrStoreRequest
	extends CoinMasterRequest
{
	public static final String master = "Mr. Store"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getNewList();
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getNewMap();

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have (\\w+) Mr. Accessor(?:y|ies) to trade." );
	public static final AdventureResult MR_A = ItemPool.get( ItemPool.MR_ACCESSORY, 1 );
	public static final AdventureResult UNCLE_B = ItemPool.get( ItemPool.UNCLE_BUCK, 1 );
	public static final CoinmasterData MR_STORE =
		new MrStoreCoinmasterData(
			MrStoreRequest.master,
			"mrstore",
			MrStoreRequest.class,
			"Mr. A",
			"You have no Mr. Accessories to trade",
			false,
			MrStoreRequest.TOKEN_PATTERN,
			MrStoreRequest.MR_A,
			null,
			null,
			"mrstore.php",
			"buy",
			MrStoreRequest.buyItems,
			MrStoreRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			null,
			null,
			null,
			null,
			true
			);

	// Since there are two different currencies, we need to have a map from
	// item name to item/count of currency; an AdventureResult.
	private static final Map<String, AdventureResult> buyCosts = new TreeMap<String, AdventureResult>();

	public MrStoreRequest()
	{
		super( MrStoreRequest.MR_STORE );
	}

	public MrStoreRequest( final String action )
	{
		super( MrStoreRequest.MR_STORE, action );
	}

	public MrStoreRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( MrStoreRequest.MR_STORE, buying, attachments );
	}

	public MrStoreRequest( final boolean buying, final AdventureResult attachment )
	{
		super( MrStoreRequest.MR_STORE, buying, attachment );
	}

	public MrStoreRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( MrStoreRequest.MR_STORE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		String responseText = this.responseText;
		if ( action != null && this.action.equals( "pullmras" ) )
		{
			// You can't pull any more items out of storage today.
			if ( responseText.indexOf( "You can't pull any more items out of storage today" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't pull any more items out of storage today." );
			}
		}

		MrStoreRequest.parseResponse( this.getURLString(), responseText );
	}

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "onClick='javascript:descitem\\((\\d+)\\)' class=nounder>(.*?)</a></b>.*?title=\"(.*?)\".*?<font size=\\+1>(\\d)</font></b></td><form name=buy(\\d+)" );
	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "mrstore.php" ) )
		{
			return;
		}

		// Learn new Mr. Items by simply visiting Mr. Store
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = MrStoreRequest.MR_STORE;
		LockableListModel<AdventureResult> items = MrStoreRequest.buyItems;
		Map prices = MrStoreRequest.buyPrices;
		Map costs = MrStoreRequest.buyCosts;
		items.clear();
		prices.clear();
		costs.clear();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			String currency = matcher.group(3);
			int price = StringUtilities.parseInt( matcher.group(4) );
			int itemId = StringUtilities.parseInt( matcher.group(5) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the Mr. Store inventory
			AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
			String name = StringUtilities.getCanonicalName( itemName );
			items.add( item );
			Integer iprice = IntegerPool.get( price );
			prices.put( name, iprice );
			AdventureResult cost = new AdventureResult( currency, price, false );
			costs.put( name, cost );
		}

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		// If we performed a Currency Exchange, account for it
		String action = GenericRequest.getAction( urlString );

		if ( action != null && action.equals( "a_to_b" ) )
		{
			if ( responseText.contains( "You acquire" ) )
			{
				ResultProcessor.processItem( ItemPool.MR_ACCESSORY, -1 );
				CoinMasterRequest.parseBalance( data, responseText );
			}
			return;
		}

		if ( action != null && action.equals( "b_to_a" ) )
		{
			if ( responseText.contains( "You acquire" ) )
			{
				ResultProcessor.processItem( ItemPool.UNCLE_BUCK, -10 );
				CoinMasterRequest.parseBalance( data, responseText );
			}
			return;
		}

		if ( action != null && action.equals( "pullmras" ) )
		{
			if ( responseText.contains( "You acquire" ) )
			{
				// We pulled a Mr. A from storage.
				AdventureResult remove = MrStoreRequest.MR_A.getInstance( -1 );
				AdventureResult.addResultToList( KoLConstants.storage, remove );
				CoinMasterRequest.parseBalance( data, responseText );
			}
			return;
		}

		CoinMasterRequest.parseResponse( data, urlString, responseText );

		// If we bought a Golden Mr. Accessory, it is now in inventory
		InventoryManager.countGoldenMrAccesories();
	}

	public static String accessible()
	{
		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "mrstore.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action != null && action.equals( "pullmras" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Pulling a Mr. Accessory from storage" );
			return true;
		}

		CoinmasterData data = MrStoreRequest.MR_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	private static class MrStoreCoinmasterData
		extends CoinmasterData
	{
		public MrStoreCoinmasterData( 
			final String master,
			final String nickname,
			final Class requestClass,
			final String token,
			final String tokenTest,
			final boolean positiveTest,
			final Pattern tokenPattern,
			final AdventureResult item,
			final String property,
			final Map<String, Integer> itemRows,
			final String buyURL,
			final String buyAction,
			final LockableListModel<AdventureResult> buyItems,
			final Map<String, Integer> buyPrices,
			final String sellURL,
			final String sellAction,
			final LockableListModel<AdventureResult> sellItems,
			final Map<String, Integer> sellPrices,
			final String itemField,
			final Pattern itemPattern,
			final String countField,
			final Pattern countPattern,
			final String storageAction,
			final String tradeAllAction,
			final boolean canPurchase )
		{
			super( master, nickname, requestClass,
			       token, tokenTest, positiveTest, tokenPattern,
			       item, property, itemRows,
			       buyURL, buyAction, buyItems, buyPrices,
			       sellURL, sellAction, sellItems, sellPrices,
			       itemField, itemPattern,
			       countField, countPattern,
			       storageAction, tradeAllAction,
			       canPurchase );
		}

		@Override
		public AdventureResult itemBuyPrice( final String itemName )
		{
			String name = StringUtilities.getCanonicalName( itemName );
			return MrStoreRequest.buyCosts.get( name );
		}
	}
}
