/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.pages.PageRegistry;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.BurningNewspaperRequest;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanFortuneRequest;
import net.sourceforge.kolmafia.request.ClanHallRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.Crimbo10Request;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.CurseRequest;
import net.sourceforge.kolmafia.request.CustomOutfitRequest;
import net.sourceforge.kolmafia.request.DigRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.DreadsylvaniaRequest;
import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FamTeamRequest;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GnomeTinkerRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.HeyDezeRequest;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.request.LeafletRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MomRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.MonsterManuelRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.MushroomRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.NemesisRequest;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.PhineasRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.SeaMerkinRequest;
import net.sourceforge.kolmafia.request.SendGiftRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.ShowClanRequest;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.SummoningChamberRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.TrophyHutRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.request.WaxGlobRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.MineDecorator;

public class ResponseTextParser
{
	private static final Pattern NEWSKILL1_PATTERN =
			Pattern.compile( "<td>You (have learned|learn) a new skill: <b>(.*?)</b>" );
	private static final Pattern NEWSKILL2_PATTERN = Pattern.compile( "whichskill=(\\d+)" );

	// You acquire a skill:&nbsp;&nbsp;</td><td><img src="http://images.kingdomofloathing.com/itemimages/wosp_stink.gif" onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350, 300)' width=30 height=30></td><td><b><a onClick='javascript:poop("desc_skill.php?whichskill=67&self=true","skill", 350, 300)'>Stinkpalm</a></b>
	private static final Pattern NEWSKILL3_PATTERN =
		Pattern.compile( "You (?:gain|acquire) a skill:.*?<[bB]>(?:<a[^>]*>)?(.*?)(?:</a>)?</[bB]>" );
	private static final Pattern DESCITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern DESCEFFECT_PATTERN = Pattern.compile( "whicheffect=([0-9a-zA-Z]+)" );

	public static boolean hasResult( final String location )
	{
		if ( location == null )
		{
			return false;
		}

		int queryStringBegin = location.indexOf( '?' );
		String path = location;
		String queryString = "";

		if ( queryStringBegin != -1 )
		{
			path = location.substring( 0, queryStringBegin );
			queryString = location.substring( queryStringBegin + 1 );
		}

		if ( !PageRegistry.isGameAction( path, queryString ) )
		{
			return false;
		}

		if ( location.startsWith( "lchat.php" ) ||
		     location.startsWith( "mchat.php" ) ||
		     location.startsWith( "newchatmessages.php" ) ||
		     location.startsWith( "submitnewchat.php" ) )
		{
			return false;
		}

		if ( location.startsWith( "showplayer.php" ) )
		{
			// We want to register new items from Equipment and Familiar
			return true;
		}

		if ( location.startsWith( "displaycollection.php" ) )
		{
			// We want to register new items in display cases
			return true;
		}

		if ( location.endsWith( "menu.php" ) ||
		     location.startsWith( "actionbar" ) )
		{
			return false;
		}

		if ( location.startsWith( "api.php" ) ||
		     location.startsWith( "game.php" ) ||
		     location.startsWith( "desc" ) ||
		     location.startsWith( "quest" ) ||
		     location.startsWith( "static" ) )
		{
			return false;
		}

		if ( location.startsWith( "makeoffer" ) ||
		     location.startsWith( "message" ) ||
		     location.startsWith( "display" ) ||
		     location.startsWith( "managecollectionshelves" ) ||
		     location.startsWith( "search" ) ||
		     location.startsWith( "show" ) )
		{
			return false;
		}

		if ( location.startsWith( "valhalla" ) )
		{
			return false;
		}

		if ( location.startsWith( "clan" ) )
		{
			return location.startsWith( "clan_stash" ) ||
				location.startsWith( "clan_hall" ) ||
				location.startsWith( "clan_rumpus" ) ||
				location.startsWith( "clan_viplounge" ) ||
				location.startsWith( "clan_hobopolis" ) ||
				location.startsWith( "clan_slimetube" ) ||
				location.startsWith( "clan_dreadsylvania" );
		}

		if ( location.startsWith( "login.php" ) ||
		     location.startsWith( "logout.php" ) )
		{
			return false;
		}

		if ( location.startsWith( "dev" ) || 
		     location.startsWith( "ilovebugs.php" ) )
		{
			return false;
		}

		return true;
	}

	public static final void externalUpdate( final GenericRequest request )
	{
		ResponseTextParser.externalUpdate( request.getURLString(), request.responseText );
	}

	public static final void externalUpdate( final String location, final String responseText )
	{
		if ( responseText == null || responseText.length() == 0 )
		{
			return;
		}

		if ( location.startsWith( "account.php" ) )
		{
			AccountRequest.parseAccountData( location, responseText );
		}

		else if ( location.startsWith( "account_contactlist.php" ) )
		{
			ContactListRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "account_manageoutfits.php" ) )
		{
			CustomOutfitRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "adventure.php" ) )
		{
			SeaMerkinRequest.parseColosseumResponse( location, responseText );
		}

		else if ( location.startsWith( "api.php" ) )
		{
			ApiRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "ascend.php" ) && location.contains( "alttext=communityservice" ) 
			&& !Preferences.getBoolean( "kingLiberated" ) )
		{
			// Redirect from donating body to science in Community Service
			ChoiceManager.canWalkAway();
			KoLCharacter.liberateKing();
		}

		else if ( location.startsWith( "ascensionhistory.php" ) )
		{
			AscensionHistoryRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "arena.php" ) )
		{
			CakeArenaRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "backoffice.php" ) )
		{
			ManageStoreRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "basement.php" ) )
		{
			BasementRequest.checkBasement( responseText );
		}

		else if ( location.startsWith( "bedazzle.php" ) )
		{
			EquipmentRequest.parseBedazzlements( responseText );
		}

		else if ( location.startsWith( "beerpong.php" ) )
		{
			BeerPongRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bet.php" ) )
		{
			MoneyMakingGameRequest.parseResponse( location, responseText, false );
		}

		else if ( location.startsWith( "bigisland.php" ) || location.startsWith( "postwarisland.php" ) )
		{
			IslandRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bone_altar.php" ) )
		{
			AltarOfBonesRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "bounty.php" ) )
		{
			BountyHunterHunterRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "campground.php" ) )
		{
			CampgroundRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "cafe.php" ) )
		{
			ChezSnooteeRequest.parseResponse( location, responseText );
			MicroBreweryRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "cave.php" ) )
		{
			NemesisRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "charsheet.php" ) && !location.contains( "ajax=1" ) )
		{
			CharSheetRequest.parseStatus( responseText );
		}

		else if ( location.startsWith( "choice.php" ) )
		{
			if ( location.contains( "whichchoice=562" ) )
			{
				FudgeWandRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "whichchoice=585" ) )
			{
				ClanLoungeSwimmingPoolRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "whichchoice=922" ) )
			{
				SummoningChamberRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "whichchoice=1278" ) )
			{
				ClanFortuneRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "clan_hall.php" ) )
		{
			ClanHallRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "clan_rumpus.php" ) )
		{
			ClanRumpusRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "clan_stash.php" ) )
		{
			ClanStashRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "clan_dreadsylvania.php" ) )
		{
			DreadsylvaniaRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "clan_viplounge.php" ) )
		{
			if ( location.contains( "preaction=lovetester" ) )
			{
				ClanFortuneRequest.parseResponse( location, responseText );
			}
			else
			{
				ClanLoungeRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "closet.php" ) || location.startsWith( "fillcloset.php" ) )
		{
			ClosetRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "craft.php" ) )
		{
			CreateItemRequest.parseCrafting( location, responseText );
		}

		else if ( location.startsWith( "crimbo09.php" ) )
		{
			Crimbo09Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "crimbo10.php" ) )
		{
			Crimbo10Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "crimbo11.php" ) )
		{
			Crimbo11Request.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "curse.php" ) )
		{
			CurseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "da.php" ) )
		{
			ShrineRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "desc_skill.php" ) && location.contains( "self=true" ) )
		{
			Matcher m = ResponseTextParser.NEWSKILL2_PATTERN.matcher( location );
			if ( m.find() )
			{
				int skill = StringUtilities.parseInt( m.group( 1 ) );
				ConsequenceManager.parseSkillDesc( skill, responseText );
			}
		}

		else if ( location.startsWith( "desc_item.php" ) && !location.contains( "otherplayer=" ) )
		{
			Matcher m = ResponseTextParser.DESCITEM_PATTERN.matcher( location );
			if ( m.find() )
			{
				String descid = m.group( 1 );
				ConsequenceManager.parseItemDesc( descid, responseText );
				int itemId = ItemDatabase.getItemIdFromDescription( descid );
				if ( itemId == ItemPool.YEARBOOK_CAMERA )
				{
					ItemDatabase.parseYearbookCamera( responseText );
				}
			}
		}

		else if ( location.startsWith( "desc_effect.php" ) )
		{
			Matcher m = ResponseTextParser.DESCEFFECT_PATTERN.matcher( location );
			if ( m.find() )
			{
				ConsequenceManager.parseEffectDesc( m.group( 1 ), responseText );
			}
		}

		else if ( location.startsWith( "diary.php" ) )
		{
			UseItemRequest.handleDiary( responseText );
		}

		else if ( location.startsWith( "dig.php" ) )
		{
			DigRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "dwarfcontraption.php" ) )
		{
			DwarfContraptionRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "dwarffactory.php" ) )
		{
			DwarfFactoryRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "familiar.php" ) )
		{
			FamiliarRequest.parseResponse( location, responseText );
			if ( !location.contains( "ajax=1" ) )
			{
				FamiliarData.registerFamiliarData( responseText );
			}
		}

		else if ( location.startsWith( "famteam.php" ) )
		{
			FamTeamRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "familiarbinger.php" ) )
		{
			UseItemRequest.parseBinge( location, responseText );
		}

		else if ( location.startsWith( "gamestore.php" ) )
		{
			GameShoppeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "guild.php" ) )
		{
			GuildRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "hermit.php" ) )
		{
			HermitRequest.parseHermitTrade( location, responseText );
		}

		else if ( location.startsWith( "heydeze.php" ) )
		{
			HeyDezeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "friars.php" ) )
		{
			FriarRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "gamestore" ) )
		{
			GameShoppeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "gnomes.php" ) )
		{
			GnomeTinkerRequest.parseCreation( location, responseText );
		}

		// Keep your current equipment and familiars updated, if you
		// visit the appropriate pages.

		else if ( location.startsWith( "inventory.php" ) )
		{
			// If KoL is showing us our current equipment, parse it.
			if ( location.contains( "which=2" ) || location.contains( "curequip=1" ) )
			{
				EquipmentRequest.parseEquipment( location, responseText );

				// Slimeling binge requests come here, too
				if ( location.contains( "action=slime" ) )
				{
					UseItemRequest.parseBinge( location, responseText );
				}
				// Certain requests, like inserting cards into
				// an El Vibrato helmet, have a usage message,
				// not an equipment page. Check for that, too.
				else
				{
					UseItemRequest.parseConsumption( responseText, false );
				}
			}

			// If there is a consumption message, parse it
			else if ( location.contains( "action=message" ) )
			{
				UseItemRequest.parseConsumption( responseText, false );
				AWOLQuartermasterRequest.parseResponse( location, responseText );
				BURTRequest.parseResponse( location, responseText );
			}

			// If there is a bricko message, parse it
			else if ( location.contains( "action=breakbricko" ) )
			{
				UseItemRequest.parseBricko( responseText );
			}

			// If there is a binge message, parse it
			else if ( location.contains( "action=ghost" ) || location.contains( "action=hobo" ) || 
				  location.contains( "action=slime" ) || location.contains( "action=candy" ) )
			{
				UseItemRequest.parseBinge( location, responseText );
			}

			// Robortender consumption
			else if ( location.contains( "action=robooze" ) )
			{
				UseItemRequest.parseRobortenderBinge( location, responseText );
			}

			// If there is an absorb message, parse it
			else if ( location.contains( "absorb=" ) )
			{
				UseItemRequest.parseAbsorb( location, responseText );
			}

				// Closet transfers can come via inventory.php
			else if ( location.contains( "action=closetpush" ) || location.contains( "action=closetpull" ) )
			{
				ClosetRequest.parseTransfer( location, responseText );
			}

			// Emptying storage can come via inventory.php
			else if ( location.contains( "action=pullall" ) )
			{
				StorageRequest.parseTransfer( location, responseText );
			}
		}

		else if ( location.startsWith( "inv_equip.php" ) && location.contains( "ajax=1" ) )
		{
			// If we are changing equipment via a chat command,
			// try to deduce what changed.
			EquipmentRequest.parseEquipmentChange( location, responseText );
		}

		else if ( ( location.startsWith( "inv_eat.php" ) ||
			    location.startsWith( "inv_booze.php" ) ||
		            location.startsWith( "inv_spleen.php" ) || 
		            location.startsWith( "inv_use.php" ) ||
			    location.startsWith( "inv_familiar.php" ) )
		          && location.contains( "whichitem" ) )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "knoll_mushrooms.php" ) )
		{
			MushroomRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "leaflet.php" ) )
		{
			LeafletRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "mallstore.php" ) )
		{
			MallPurchaseRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "managecollection.php" ) )
		{
			DisplayCaseRequest.parseDisplayTransfer( location, responseText );
		}
		else if ( location.startsWith( "managecollectionshelves.php" ) )
		{
			DisplayCaseRequest.parseDisplayArrangement( location, responseText );
		}

		else if ( location.startsWith( "managestore.php" ) )
		{
			AutoMallRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "mining.php" ) )
		{
			MineDecorator.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "monkeycastle.php" ) )
		{
			if ( location.contains( "who=2" ) || location.contains( "action=buyitem" ) )
			{
				BigBrotherRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "who=4" ) )
			{
				MomRequest.parseResponse( location, responseText );
			}
		}

		else if ( location.startsWith( "mrstore.php" ) )
		{
			MrStoreRequest.parseResponse( location, responseText );
		}

		else if ( ( location.startsWith( "multiuse.php" ) || location.startsWith( "skills.php" ) ) && location.contains( "useitem" ) )
		{
			UseItemRequest.parseConsumption( responseText, false );
		}

		else if ( location.startsWith( "pandamonium.php" ) )
		{
			PandamoniumRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "peevpee.php" ) )
		{
			PeeVPeeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "place.php" ) )
		{
			PlaceRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "questlog.php" ) )
		{
			MonsterManuelRequest.parseResponse( location, responseText );
			QuestLogRequest.registerQuests( true, location, responseText );
		}

		else if ( location.startsWith( "raffle.php" ) )
		{
			RaffleRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "runskillz.php" ) || location.startsWith( "skillz.php" ) )
		{
			UseSkillRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sea_merkin.php" ) )
		{
			SeaMerkinRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sea_skatepark.php" ) )
		{
			SkateParkRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sellstuff.php" ) )
		{
			AutoSellRequest.parseCompactAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sellstuff_ugly.php" ) )
		{
			AutoSellRequest.parseDetailedAutoSell( location, responseText );
		}

		else if ( location.startsWith( "sendmessage.php" ) )
		{
			SendMailRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "shop.php" ) )
		{
			NPCPurchaseRequest.parseShopResponse( location, responseText );
		}

		else if ( location.startsWith( "showclan.php" ) )
		{
			ShowClanRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "skills.php" ) )
		{
			if ( location.contains( "action=useditem" ) )
			{
				UseItemRequest.parseConsumption( responseText, false );
			}
		}

		else if ( location.startsWith( "spaaace.php" ) )
		{
			SpaaaceRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "storage.php" ) )
		{
			StorageRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "suburbandis.php" ) )
		{
			SuburbanDisRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "sushi.php" ) )
		{
			SushiRequest.parseConsumption( location, responseText, true );
		}

		else if ( location.startsWith( "tavern.php" ) )
		{
			TavernRequest.parseResponse( location, responseText );
		}

		if ( location.startsWith( "tiles.php" ) )
		{
			DvorakManager.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "topmenu.php" ) )
		{
			if ( KoLCharacter.getLimitmode() == Limitmode.BATMAN )
			{
				BatManager.parseTopMenu( responseText );
			}
		}

		else if ( location.startsWith( "town_altar.php" ) )
		{
			AltarOfLiteracyRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_right.php" ) )
		{
			GourdRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "town_sendgift.php" ) )
		{
			SendGiftRequest.parseTransfer( location, responseText );
		}

		else if ( location.startsWith( "traveler.php" ) )
		{
			TravelingTraderRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "trophy.php" ) )
		{
			TrophyHutRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "typeii.php" ) )
		{
			TrendyRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "volcanoisland.php" ) )
		{
			PhineasRequest.parseResponse( location, responseText );
			VolcanoIslandRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "volcanomaze.php" ) )
		{
			VolcanoMazeRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "wand.php" ) )
		{
			ZapRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "witchess.php" ) )
		{
			WitchessRequest.parseResponse( location, responseText );
		}

		else if ( location.startsWith( "crypt.php" ) )
		{
			// Check if crypt areas have unexpectedly vanished and correct if so
			if ( !responseText.contains( "The Defiled Alcove" ) && Preferences.getInteger( "cyrptAlcoveEvilness" ) > 0 ||
			     !responseText.contains( "The Defiled Cranny" ) && Preferences.getInteger( "cyrptCrannyEvilness" ) > 0 ||
			     !responseText.contains( "The Defiled Niche" ) && Preferences.getInteger( "cyrptNicheEvilness" ) > 0 ||
			     !responseText.contains( "The Defiled Nook" ) && Preferences.getInteger( "cyrptNookEvilness" ) > 0 )
			{
				if ( InventoryManager.hasItem( ItemPool.EVILOMETER ) )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.EVILOMETER ) );
				}
				else
				{
					// Must have completed quest and already used and lost Evilometer
					Preferences.setInteger( "cyrptAlcoveEvilness", 0 );
					Preferences.setInteger( "cyrptCrannyEvilness", 0 );
					Preferences.setInteger( "cyrptNicheEvilness", 0 );
					Preferences.setInteger( "cyrptNookEvilness", 0 );
					Preferences.setInteger( "cyrptTotalEvilness", 0 );
				}
			}
		}

		// You can learn a skill on many pages.
		ResponseTextParser.learnSkill( location, responseText );

		// Currently, required recipes can only be learned via using an
		// item, but that's probably not guaranteed to be true forever.
		// Update: you can now learn them from the April Shower
		ResponseTextParser.learnRecipe( location, responseText );
	}

	private static final Pattern DIV_LINK_PATTERN = Pattern.compile( "<div id=([^ ]+)[^>]*><a .*?</a></div>", Pattern.DOTALL );
	public static String parseDivLabel( final String label, final String responseText )
	{
		Matcher matcher = ResponseTextParser.DIV_LINK_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			if ( matcher.group( 1 ).equals( label ) )
			{
				return parseDivLabel( matcher.group( 0 ) );
			}
		}
		return "";
	}

	// <img src="http://images.kingdomofloathing.com/otherimages/zonefont/percent.gif" height="10" border="0" style="margin-right: 4px"/>
	private static final Pattern DIV_CHAR_PATTERN = Pattern.compile( "otherimages/zonefont/(.*?)\\.gif" );
	public static String parseDivLabel( final String divText )
	{
		StringBuilder string = new StringBuilder();
		Matcher matcher = ResponseTextParser.DIV_CHAR_PATTERN.matcher( divText );
		while ( matcher.find() )
		{
			String c = matcher.group( 1 );
			if ( c.length() == 1 )
			{
				string.append( c.charAt( 0 ) );
			}
			else if ( c.equals( "lparen" ) )
			{
				string.append( "(" );
			}
			else if ( c.equals( "percent" ) )
			{
				string.append( "%" );
			}
			else if ( c.equals( "rparen" ) )
			{
				string.append( ")" );
			}
		}
		return string.toString();
	}

	private static final Pattern [] RECIPE_PATTERNS =
	{
		Pattern.compile( "You learn to .*?craft.*? a new item:.*?<b>(.*?)</b>" ),
		Pattern.compile( "You (?:have|just) .*?discovered.*? a new recipe.*?<b>(.*?)</b>" )
	};

	public static void learnRecipe( String location, String responseText )
	{
		if ( !ResponseTextParser.hasResult( location ) )
		{
			return;
		}

		String itemName = null;

		for ( int i = 0; i < RECIPE_PATTERNS.length; ++i )
		{
			Matcher matcher = RECIPE_PATTERNS[i].matcher( responseText );
			if ( matcher.find() )
			{
				itemName = matcher.group( 1 );
				break;
			}
		}

		if ( itemName == null )
		{
			return;
		}

		int id = ItemDatabase.getItemId( itemName, 1, false );

		if ( id <= 0 )
		{
			return;
		}

		String message = "Learned recipe: " + itemName + " (" + id + ")";
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		Preferences.setBoolean( "unknownRecipe" + id, false );
		ConcoctionDatabase.setRefreshNeeded( false );
	}

	public static void learnSkill( final String location, final String responseText )
	{
		if ( !ResponseTextParser.hasResult( location ) )
		{
			return;
		}

		// Don't parse skill acquisition via item use here, since
		// UseItemRequest will detect it.
		// Don't parse steel margarita/lasagna here, either.

		if ( location.startsWith( "inv_use.php" ) ||
		     location.startsWith( "inv_eat.php" ) ||
		     location.startsWith( "inv_booze.php" ) ||
		     location.startsWith( "inv_spleen.php" ) ||
		     location.startsWith( "showplayer.php" ) )
		{
			return;
		}

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.contains( "You leargn a new skill." ) )
		{
			Matcher matcher = ResponseTextParser.NEWSKILL2_PATTERN.matcher( location );
			if ( matcher.find() )
			{
				int skillId = StringUtilities.parseInt( matcher.group( 1 ) );
				String skillName = SkillDatabase.getSkillName( skillId );
				ResponseTextParser.learnSkill( skillName );
				return;
			}
		}

		ResponseTextParser.learnSkillFromResponse( responseText );
	}

	public static void learnSkillFromResponse( final String responseText )
	{
		boolean skillFound = false;
		Matcher matcher = ResponseTextParser.NEWSKILL1_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			ResponseTextParser.learnSkill( matcher.group( 2 ) );
			skillFound = true;
		}
		if ( skillFound )
		{
			return;
		}

		matcher = ResponseTextParser.NEWSKILL3_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			ResponseTextParser.learnSkill( matcher.group( 1 ) );
			skillFound = true;
		}
		if ( skillFound )
		{
			return;
		}
	}

	public static final void learnSkill( final String skillName )
	{
		// The following skills are found in battle and result in
		// losing an item from inventory.

		if ( skillName.equals( "Snarl of the Timberwolf" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_WOLF_STANDARD ) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_WOLF_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Spectral Snapper" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.TATTERED_SNAKE_STANDARD ) )
			{
				ResultProcessor.processItem( ItemPool.TATTERED_SNAKE_STANDARD, -1 );
			}
		}
		else if ( skillName.equals( "Scarysauce" ) || skillName.equals( "Fearful Fettucini" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY ) )
			{
				ResultProcessor.processItem( ItemPool.ENGLISH_TO_A_F_U_E_DICTIONARY, -1 );
			}
		}
		else if ( skillName.equals( "Tango of Terror" ) || skillName.equals( "Dirge of Dreadfulness" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC ) )
			{
				ResultProcessor.processItem( ItemPool.BIZARRE_ILLEGIBLE_SHEET_MUSIC, -1 );
			}
		}
		else if ( skillName.equals( "Belch The Rainbow" ) )
		{
			Preferences.increment( "skillLevel117", 1, 11, false );
		}
		else if ( skillName.equals( "Toggle Optimality" ) )
		{
			Preferences.increment( "skillLevel7254", 1 );
		}
		else if ( skillName.equals( "Bear Essence" ) )
		{
			Preferences.increment( "skillLevel134", 1 );
		}
		else if ( skillName.equals( "Calculate the Universe" ) )
		{
			Preferences.increment( "skillLevel144" );
		}
		else if ( KoLCharacter.inNuclearAutumn() )
		{
			if ( skillName.equals( "Boiling Tear Ducts" ) ||
			     skillName.equals( "Projectile Salivary Glands" ) ||
			     skillName.equals( "Translucent Skin" ) ||
			     skillName.equals( "Skunk Glands" ) ||
			     skillName.equals( "Throat Refrigerant" ) ||
			     skillName.equals( "Internal Soda Machine" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.RAD, -30 ) );
			}
			else if ( skillName.equals( "Steroid Bladder" ) ||
			          skillName.equals( "Magic Sweat" ) ||
			          skillName.equals( "Flappy Ears" ) ||
			          skillName.equals( "Self-Combing Hair" ) ||
			          skillName.equals( "Intracranial Eye" ) ||
			          skillName.equals( "Mind Bullets" ) ||
			          skillName.equals( "Extra Kidney" ) ||
			          skillName.equals( "Extra Gall Bladder" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.RAD, -60 ) );
			}
			else if ( skillName.equals( "Extra Muscles" ) ||
				    skillName.equals( "Adipose Polymers" ) ||
				    skillName.equals( "Metallic Skin" ) ||
				    skillName.equals( "Hypno-Eyes" ) ||
				    skillName.equals( "Extra Brain" ) ||
				    skillName.equals( "Magnetic Ears" ) ||
				    skillName.equals( "Extremely Punchable Face" ) ||
				    skillName.equals( "Firefly Abdomen" ) ||
				    skillName.equals( "Bone Springs" ) ||
				    skillName.equals( "Squid Glands" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.RAD, -90 ) );
			}
			else if ( skillName.equals( "Sucker Fingers" ) ||
			          skillName.equals( "Backwards Knees" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.RAD, -120 ) );
			}
		}

		String message = "You learned a new skill: " + skillName;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
		KoLCharacter.addAvailableSkill( skillName );
		KoLCharacter.updateStatus();
		LockableListFactory.sort( KoLConstants.usableSkills );
		DiscoCombatHelper.learnSkill( skillName );
		ConcoctionDatabase.setRefreshNeeded( true );
		if ( SkillDatabase.isBookshelfSkill( skillName ) )
		{
			KoLCharacter.setBookshelf( true );
		}
		PreferenceListenerRegistry.firePreferenceChanged( "(skill)" );
	}

	public static final String[][] COMBAT_MOVE_DATA =
	{
		{
			"gladiatorBallMovesKnown",
			"Ball Bust",
			"Ball Sweat",
			"Ball Sack",
		},
		{
			"gladiatorBladeMovesKnown",
			"Blade Sling",
			"Blade Roller",
			"Blade Runner",
		},
		{
			"gladiatorNetMovesKnown",
			"Net Gain",
			"Net Loss",
			"Net Neutrality",
		},
	};

	public static final void learnCombatMove( final String skillName )
	{
		for ( int type = 0; type < COMBAT_MOVE_DATA.length; ++type )
		{
			String [] moves = COMBAT_MOVE_DATA[ type ];
			for ( int index= 1; index < moves.length; ++index )
			{
				if ( skillName.equals( moves[ index ] ) )
				{
					String setting = moves[ 0 ];
					Preferences.setInteger( setting, index );
					String message = "You learned a new special combat move: " + skillName;
					RequestLogger.printLine( message );
					RequestLogger.updateSessionLog( message );
					// KoLCharacter.addCombatSkill( skillName );
					// PreferenceListenerRegistry.firePreferenceChanged( "(skill)" );
					return;
				}
			}
		}
	}
}
