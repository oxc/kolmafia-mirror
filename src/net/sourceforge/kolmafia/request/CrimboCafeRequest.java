/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrimboCafeRequest
	extends CafeRequest
{
	public CrimboCafeRequest( final String name )
	{
		super( "Crimbo Cafe", "6" );

		int itemId = 0;
		int price = 0;

		switch ( KoLConstants.cafeItems.indexOf( name ) )
		{
		case 0:
			itemId = -67;
			price = 50;
			break;

		case 1:
			itemId = -68;
			price = 75;
			break;

		case 2:
			itemId = -69;
			price = 100;
			break;

		case 3:
			itemId = -70;
			price = 50;
			break;

		case 4:
			itemId = -71;
			price = 75;
			break;

		case 5:
			itemId = -72;
			price = 100;
			break;
		}

		this.setItem( name, itemId, price );
	}

	public static final boolean onMenu( final String name )
	{
		return KoLConstants.cafeItems.contains( name );
	}

	public static final void getMenu()
	{
		KoLmafia.updateDisplay( "Visiting Crimbo Cafe..." );
		KoLConstants.cafeItems.clear();
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Caviar Carbonara", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Halibut Alfredo", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Red Herring Velvet Cake", 100 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Gin and Herring", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Black and Tan and Red All Over", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "Antarctic Ice Tea", 100 );
		ConcoctionDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static final void reset()
	{
		CafeRequest.reset( KoLConstants.cafeItems );
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() || !matcher.group( 1 ).equals( "6" ) )
		{
			return false;
		}

		matcher = CafeRequest.ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
		String itemName;
		int price;

		switch ( itemId )
		{
		case -67:
			itemName = "Caviar Carbonara";
			price = 50;
			break;
		case -68:
			itemName = "Halibut Alfredo";
			price = 75;
			break;
		case -69:
			itemName = "Red Herring Velvet Cake";
			price = 100;
			break;
		case -70:
			itemName = "Gin and Herring";
			price = 50;
			break;
		case -71:
			itemName = "Black and Tan and Red All Over";
			price = 75;
			break;
		case -72:
			itemName = "Antarctic Ice Tea";
			price = 100;
			break;
		default:
			return false;
		}

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
