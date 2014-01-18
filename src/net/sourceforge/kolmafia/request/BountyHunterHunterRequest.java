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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BountyHunterHunterRequest
	extends CoinMasterRequest
{
	public static final String master = "Bounty Hunter Hunter"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BountyHunterHunterRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BountyHunterHunterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have.*?<b>([\\d,]+)</b> filthy lucre" );
	public static final AdventureResult LUCRE = ItemPool.get( ItemPool.LUCRE, 1 );
	public static final CoinmasterData BHH =
		new CoinmasterData(
			BountyHunterHunterRequest.master,
			"hunter",
			BountyHunterHunterRequest.class,
			"bounty.php",
			"lucre",
			"You don't have any filthy lucre",
			false,
			BountyHunterHunterRequest.TOKEN_PATTERN,
			BountyHunterHunterRequest.LUCRE,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"howmany",
			GenericRequest.HOWMANY_PATTERN,
			"buy",
			BountyHunterHunterRequest.buyItems,
			BountyHunterHunterRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			null
			);

	public BountyHunterHunterRequest()
	{
		super( BountyHunterHunterRequest.BHH );
	}

	public BountyHunterHunterRequest( final String action )
	{
		super( BountyHunterHunterRequest.BHH, action );
	}

	public BountyHunterHunterRequest( final String action, final AdventureResult [] attachments )
	{
		super( BountyHunterHunterRequest.BHH, action, attachments );
	}

	public BountyHunterHunterRequest( final String action, final AdventureResult attachment )
	{
		super( BountyHunterHunterRequest.BHH, action, attachment );
	}

	public BountyHunterHunterRequest( final String action, final int itemId, final int quantity )
	{
		super( BountyHunterHunterRequest.BHH, action, itemId, quantity );
	}

	public BountyHunterHunterRequest( final String action, final int itemId )
	{
		super( BountyHunterHunterRequest.BHH, action, itemId );
	}

	@Override
	public void processResults()
	{
		BountyHunterHunterRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern EASY_PATTERN = Pattern.compile( "Easy Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern HARD_PATTERN = Pattern.compile( "Hard Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern SPECIAL_PATTERN = Pattern.compile( "Specialty Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern UNTAKEN_EASY_PATTERN = Pattern.compile( "Easy Bounty:.*?>(\\d+) (.*?) from.*?takelow" );
	private static final Pattern UNTAKEN_HARD_PATTERN = Pattern.compile( "Hard Bounty:.*?>(\\d+) (.*?) from.*?takehigh" );
	private static final Pattern UNTAKEN_SPECIAL_PATTERN = Pattern.compile( "Specialty Bounty:.*?center>(\\d+) (.*?) from.*?takespecial" );
	private static final Pattern EASY_QTY_PATTERN = Pattern.compile( "Easy Bounty.*?You have collected (\\d+) .*?giveup_low" );
	private static final Pattern HARD_QTY_PATTERN = Pattern.compile( "Hard Bounty.*?You have collected (\\d+) .*?giveup_high" );
	private static final Pattern SPECIAL_QTY_PATTERN = Pattern.compile( "Specialty Bounty.*?You have collected (\\d+) .*?giveup_spe" );

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BountyHunterHunterRequest.BHH;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			BountyHunterHunterRequest.parseEasy( responseText );
			BountyHunterHunterRequest.parseHard( responseText );
			BountyHunterHunterRequest.parseSpecial( responseText );
			return;
		}
		
		if ( action.equals( "takelow" ) )
		{
			Preferences.setString( "currentEasyBountyItem", Preferences.getString( "_untakenEasyBountyItem" ) + ":0" );
			Preferences.setString( "_untakenEasyBountyItem", "" );
			
			//KoLAdventure adventure = AdventureDatabase.getBountyLocation( bountyItem );
			//AdventureFrame.updateSelectedAdventure( adventure );
			return;
		}

		if ( action.equals( "takehigh" ) )
		{
			Preferences.setString( "currentHardBountyItem", Preferences.getString( "_untakenHardBountyItem" ) + ":0" );
			Preferences.setString( "_untakenHardBountyItem", "" );

			//KoLAdventure adventure = AdventureDatabase.getBountyLocation( bountyItem );
			//AdventureFrame.updateSelectedAdventure( adventure );
			return;
		}

		if ( action.equals( "takespecial" ) )
		{
			Preferences.setString( "currentSpecialBountyItem", Preferences.getString( "_untakenSpecialBountyItem" ) + ":0" );
			Preferences.setString( "_untakenSpecialBountyItem", "" );

			//KoLAdventure adventure = AdventureDatabase.getBountyLocation( bountyItem );
			//AdventureFrame.updateSelectedAdventure( adventure );
			return;
		}

		if ( action.equals( "giveup_low" ) )
		{
			Preferences.setString( "currentEasyBountyItem", "" );
			return;
		}
		
		if ( action.equals( "giveup_high" ) )
		{
			Preferences.setString( "currentHardBountyItem", "" );
			return;
		}
		
		if ( action.equals( "giveup_spe" ) )
		{
			Preferences.setString( "currentSpecialBountyItem", "" );
			return;
		}
		
		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	private static final void parseEasy( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.EASY_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentEasyBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_EASY_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 2 );
				String bountyItem = BountyDatabase.getName( plural );
				Preferences.setString( "_untakenEasyBountyItem", bountyItem );
			}
			else
			{
				Preferences.setString( "_untakenEasyBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );

		Matcher bountyQtyMatcher = BountyHunterHunterRequest.EASY_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}
		
		Preferences.setString( "currentEasyBountyItem", bountyItem + ":" + bountyQty );
	}

	private static final void parseHard( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.HARD_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentHardBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_HARD_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 2 );
				String bountyItem = BountyDatabase.getName( plural );
				Preferences.setString( "_untakenHardBountyItem", bountyItem );
			}
			else
			{
				Preferences.setString( "_untakenHardBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );
		
		Matcher bountyQtyMatcher = BountyHunterHunterRequest.HARD_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}

		Preferences.setString( "currentHardBountyItem", bountyItem + ":" + bountyQty );
	}

	private static final void parseSpecial( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.SPECIAL_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentSpecialBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_SPECIAL_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 2 );
				String bountyItem = BountyDatabase.getName( plural );
				Preferences.setString( "_untakenSpecialBountyItem", bountyItem );
			}
			else
			{
				Preferences.setString( "_untakenSpecialBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );
		
		Matcher bountyQtyMatcher = BountyHunterHunterRequest.SPECIAL_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}

		Preferences.setString( "currentSpecialBountyItem", bountyItem + ":" + bountyQty );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bounty.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting the Bounty Hunter Hunter" );
			return true;
		}

		if ( action.equals( "takelow" ) )
		{
			String bountyName = Preferences.getString( "_untakenEasyBountyItem" );
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept easy bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "takehigh" ) )
		{
			String bountyName = Preferences.getString( "_untakenHardBountyItem" );
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept hard bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "takespecial" ) )
		{
			String bountyName = Preferences.getString( "_untakenSpecialBountyItem" );
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept specialty bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "giveup_low" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon easy bounty assignment" );
			return true;
		}

		if ( action.equals( "giveup_high" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon hard bounty assignment" );
			return true;
		}

		if ( action.equals( "giveup_spe" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon special bounty assignment" );
			return true;
		}

		CoinmasterData data = BountyHunterHunterRequest.BHH;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
