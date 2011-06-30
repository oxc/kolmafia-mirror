/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

public class IsotopeSmitheryRequest
	extends CoinMasterRequest
{
	public static final AdventureResult ISOTOPE = ItemPool.get( ItemPool.LUNAR_ISOTOPE, 1 );

	public static final CoinmasterData ISOTOPE_SMITHERY =
		new CoinmasterData(
			"Isotope Smithery",
			IsotopeSmitheryRequest.class,
			"spaaace.php?place=shop1",
			"isotope",
			"You have 0 lunar isotopes",
			false,
			SpaaaceRequest.TOKEN_PATTERN,
			SpaaaceRequest.ISOTOPE,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buy",
			CoinmastersDatabase.getIsotope1Items(),
			CoinmastersDatabase.isotope1BuyPrices(),
			null,
			null
			);

	public IsotopeSmitheryRequest()
	{
		super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY );
	}

	public IsotopeSmitheryRequest( final String action )
	{
		super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY, action );
	}

	public IsotopeSmitheryRequest( final String action, final int itemId, final int quantity )
	{
		super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY, action, itemId, quantity );
	}

	public IsotopeSmitheryRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public IsotopeSmitheryRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "spaaace.php" ) || urlString.indexOf( "place=shop1" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = IsotopeSmitheryRequest.ISOTOPE_SMITHERY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return SpaaaceRequest.accessible();
	}

	public static void equip()
	{
		SpaaaceRequest.equip();
	}
}
