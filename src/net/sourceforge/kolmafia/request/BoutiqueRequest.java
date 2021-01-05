/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class BoutiqueRequest
	extends CoinMasterRequest
{
	public static final String master = "Paul's Boutique";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BoutiqueRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BoutiqueRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BoutiqueRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) odd silver coin" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.ODD_SILVER_COIN, 1 );
	public static final CoinmasterData BOUTIQUE =
		new CoinmasterData(
			BoutiqueRequest.master,
			"boutique",
			BoutiqueRequest.class,
			"odd silver coin",
			null,
			false,
			BoutiqueRequest.TOKEN_PATTERN,
			BoutiqueRequest.COIN,
			null,
			BoutiqueRequest.itemRows,
			"shop.php?whichshop=cindy",
			"buyitem",
			BoutiqueRequest.buyItems,
			BoutiqueRequest.buyPrices,
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
			);

	public BoutiqueRequest()
	{
		super( BoutiqueRequest.BOUTIQUE );
	}

	public BoutiqueRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BoutiqueRequest.BOUTIQUE, buying, attachments );
	}

	public BoutiqueRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BoutiqueRequest.BOUTIQUE, buying, attachment );
	}

	public BoutiqueRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BoutiqueRequest.BOUTIQUE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BoutiqueRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=cindy" ) )
		{
			return;
		}

		CoinmasterData data = BoutiqueRequest.BOUTIQUE;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=cindy" ) )
		{
			return false;
		}

		CoinmasterData data = BoutiqueRequest.BOUTIQUE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		int coin = BoutiqueRequest.COIN.getCount( KoLConstants.inventory );
		if ( coin == 0 )
		{
			return "You don't have an odd silver coin.";
		}
		return null;
	}
}
