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
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class DimemasterRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You've.*?got ([\\d,]+) dime" );
	public static final CoinmasterData HIPPY =
		new CoinmasterData(
			"Dimemaster",
			DimemasterRequest.class,
			"bigisland.php?place=camp&whichcamp=1",
			"dime",
			"You don't have any dimes",
			false,
			DimemasterRequest.TOKEN_PATTERN,
			null,
			"availableDimes",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"getgear",
			CoinmastersDatabase.getDimeItems(),
			CoinmastersDatabase.dimeBuyPrices(),
			"turnin",
			CoinmastersDatabase.dimeSellPrices()
			);

	public static final int WAR_HIPPY_OUTFIT = 32;

	public DimemasterRequest()
	{
		super( DimemasterRequest.HIPPY );
	}

	public DimemasterRequest( final String action )
	{
		super( DimemasterRequest.HIPPY, action );
	}

	public DimemasterRequest( final String action, final int itemId, final int quantity )
	{
		super( DimemasterRequest.HIPPY, action, itemId, quantity );
	}

	public DimemasterRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public DimemasterRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) || urlString.indexOf( "whichcamp=1" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = DimemasterRequest.HIPPY;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		IslandDecorator.ensureUpdatedBigIsland();
		String message = null;

		if ( !Preferences.getString( "warProgress" ).equals( "started" ) )
		{
			message = "You're not at war.";
		}
		else if ( !EquipmentManager.hasOutfit( DimemasterRequest.WAR_HIPPY_OUTFIT ) )
		{
			message = "You don't have the War Hippy Fatigues";
		}

		return message;
	}

	public static void equip()
	{
		if ( !EquipmentManager.isWearingOutfit( DimemasterRequest.WAR_HIPPY_OUTFIT ) )
		{
			EquipmentManager.retrieveOutfit( DimemasterRequest.WAR_HIPPY_OUTFIT );
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( DimemasterRequest.WAR_HIPPY_OUTFIT );
			EquipmentRequest request = new EquipmentRequest( outfit );
			RequestThread.postRequest( request );
		}
	}
}
