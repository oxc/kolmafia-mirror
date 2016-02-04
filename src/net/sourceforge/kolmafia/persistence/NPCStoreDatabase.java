/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.StandardRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HashMultimap;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCStoreDatabase
	extends KoLDatabase
{
	private static final HashMultimap<NPCPurchaseRequest> NPC_ITEMS = new HashMultimap<NPCPurchaseRequest>();
	private static final HashMultimap<NPCPurchaseRequest> ROW_ITEMS = new HashMultimap<NPCPurchaseRequest>();
	private static final AdventureResult RABBIT_HOLE = new AdventureResult( "Down the Rabbit Hole", 1, true );
	private static final Map<String, String> storeNameById = new TreeMap<String, String>();

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "npcstores.txt", KoLConstants.NPCSTORES_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 4 )
			{
				continue;
			}

			String storeName = new String( data[0] );
			String storeId = new String( data[1] );
			NPCStoreDatabase.storeNameById.put( storeId, storeName );

			String itemName = data[ 2 ];
			int itemId = ItemDatabase.getItemId( itemName );
			if ( itemId == -1 )
			{
				RequestLogger.printLine( "Unknown item in store \"" + data[ 0 ] + "\": " + itemName );
				continue;
			}

			int price = StringUtilities.parseInt( data[ 3 ] );
			int row =
				( data.length > 4 && data[ 4 ].startsWith( "ROW" ) ) ?
				IntegerPool.get( StringUtilities.parseInt( data[ 4 ].substring( 3 ) ) ) :
				0;

			// Make the purchase request for this item
			NPCPurchaseRequest purchaseRequest = new NPCPurchaseRequest( storeName, storeId, itemId, row, price );

			// Map from item id -> purchase request
			NPCStoreDatabase.NPC_ITEMS.put( itemId, purchaseRequest );

			// Map from row -> purchase request
			if ( row != 0 )
			{
				NPCStoreDatabase.ROW_ITEMS.put( row, purchaseRequest );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final String getStoreName( final String storeId )
	{
		return (String) NPCStoreDatabase.storeNameById.get( storeId );
	}

	public static final PurchaseRequest getPurchaseRequest( final int itemId )
	{
		NPCPurchaseRequest foundItem = null;

		List<NPCPurchaseRequest> items = NPCStoreDatabase.NPC_ITEMS.get( itemId );
		if ( items == null )
		{
			return null;
		}

		for ( NPCPurchaseRequest item : items )
		{
			foundItem = item;

			if ( !NPCStoreDatabase.canPurchase( item.getStoreId(), item.getShopName(), itemId ) )
			{
				continue;
			}

			item.setCanPurchase( true );
			return item;
		}

		if ( foundItem == null )
		{
			return null;
		}

		foundItem.setCanPurchase( false );
		return foundItem;
	}

	private static final boolean canPurchase( final String storeId, final String shopName,
		final int itemId )
	{
		if ( storeId == null )
		{
			return false;
		}

		// Check for whether or not the purchase can be made from a
		// guild store.	 Store #1 is moxie classes, store #2 is for
		// mysticality classes, and store #3 is for muscle classes.

		String classType = KoLCharacter.getClassType();

		if ( storeId.equals( "gnoll" ) )
		{
			// Degrassi Knoll Bakery and Hardware Store
			return KoLCharacter.knollAvailable();
		}
		else if ( storeId.equals( "tweedle" ) )
		{
			// The Tweedleporium
			return KoLConstants.activeEffects.contains( NPCStoreDatabase.RABBIT_HOLE );
		}
		else if ( storeId.equals( "bugbear" ) )
		{
			// Bugbear Bakery
			return EquipmentManager.hasOutfit( OutfitPool.BUGBEAR_COSTUME );
		}
		else if ( storeId.equals( "madeline" ) )
		{
			// Bugbear Bakery
			return QuestDatabase.isQuestFinished( Quest.ARMORER );
		}
		else if ( storeId.equals( "bartender" ) )
		{
			// The Typical Tavern
			return !KoLCharacter.inZombiecore() && QuestLogRequest.isTavernAvailable();
		}
		else if ( storeId.equals( "blackmarket" ) )
		{
			// Black Market
			return QuestLogRequest.isBlackMarketAvailable();
		}
		else if ( storeId.equals( "chateau" ) )
		{
			// Chateau Mantenga
			return Preferences.getBoolean( "chateauAvailable" ) && StandardRequest.isAllowed( "Items", "Chateau Mantegna room key" );
		}
		else if ( storeId.equals( "chinatown" ) )
		{
			// Chinatown Shops
			return KoLConstants.inventory.contains( ItemPool.get( ItemPool.STRANGE_GOGGLES, 1 ) ) &&
			       KoLConstants.campground.contains( ItemPool.get( ItemPool.SUSPICIOUS_JAR, 1 ) );
		}
		else if ( storeId.equals( "guildstore1" ) )
		{
			// Shadowy Store
			return	KoLCharacter.isMoxieClass() &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "guildstore2" ) )
		{
			// Gouda's Grimoire and Grocery
			return ( KoLCharacter.isMysticalityClass() ||
				 ( classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9) ) &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "guildstore3" ) )
		{
			// Smacketeria
			return ( ( KoLCharacter.isMuscleClass() && !KoLCharacter.isAvatarOfBoris() ) ||
				 ( classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9 ) ) &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "hippy" ) )
		{
			int level = KoLCharacter.getLevel();

			if ( shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				if ( !KoLCharacter.mysteriousIslandAccessible() ||
					!EquipmentManager.hasOutfit( OutfitPool.HIPPY_OUTFIT ) )
				{
					return false;
				}

				if ( Preferences.getInteger( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
				{
					return false;
				}

				if ( level < 12 )
				{
					return true;
				}

				return QuestLogRequest.isHippyStoreAvailable();
			}

			// Here, you insert any logic which is able to detect
			// the completion of the filthworm infestation and
			// which outfit was used to complete it.

			if ( Preferences.getInteger( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
			{
				return false;
			}

			int outfit = OutfitPool.NONE;
			if ( shopName.equals( "Hippy Store (Hippy)" ) )
			{
				if ( !Preferences.getString( "currentHippyStore" ).equals( "hippy" ) )
				{
					return false;
				}

				outfit = OutfitPool.WAR_HIPPY_OUTFIT;
			}

			else if ( shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				if ( !Preferences.getString( "currentHippyStore" ).equals( "fratboy" ) )
				{
					return false;
				}

				outfit = OutfitPool.WAR_FRAT_OUTFIT;
			}

			else
			{
				// What is this?
				return false;
			}

			return QuestLogRequest.isHippyStoreAvailable() || EquipmentManager.hasOutfit( outfit );
		}
		else if ( storeId.equals( "knobdisp" ) )
		{
			// The Knob Dispensary
			return KoLCharacter.getDispensaryOpen();
		}
		else if ( storeId.equals( "jewelers" ) )
		{
			// Little Canadia Jewelers
			return !KoLCharacter.inZombiecore() && KoLCharacter.canadiaAvailable();
		}
		else if ( storeId.equals( "generalstore" ) )
		{
			// General Store
			// Some items restricted, often because of holidays
			String holiday = HolidayDatabase.getHoliday();

			if ( itemId == ItemPool.MARSHMALLOW )
			{
				return holiday.contains( "Yuletide" );
			}
			else if ( itemId == ItemPool.OYSTER_BASKET )
			{
				return holiday.contains( "Oyster Egg Day" );
			}
			else if ( itemId == ItemPool.PARTY_HAT )
			{
				return holiday.contains( "Festival of Jarlsberg" );
			}
			else if ( itemId == ItemPool.M282 || itemId == ItemPool.SNAKE || itemId == ItemPool.SPARKLER )
			{
				return holiday.contains( "Dependence Day" );
			}
			else if ( itemId == ItemPool.FOAM_NOODLE || itemId == ItemPool.INFLATABLE_DUCK || itemId == ItemPool.WATER_WINGS )
			{
				return holiday.contains( "Generic Summer Holiday" );
			}
			else if ( itemId == ItemPool.DESERT_BUS_PASS )
			{
				return !KoLCharacter.desertBeachAccessible();
			}
			else if ( itemId == ItemPool.FOLDER_01 || itemId == ItemPool.FOLDER_02 || itemId == ItemPool.FOLDER_03 )
			{
				AdventureResult folderHolder = new AdventureResult( ItemPool.FOLDER_HOLDER, 1, false );
				return folderHolder.getCount( KoLConstants.inventory ) + folderHolder.getCount( KoLConstants.closet ) +
					folderHolder.getCount( KoLConstants.collection ) > 0 || KoLCharacter.hasEquipped( folderHolder );
			}
			else if ( itemId == ItemPool.WATER_WINGS_FOR_BABIES || itemId == ItemPool.MINI_LIFE_PRESERVER ||
				itemId == ItemPool.HEAVY_DUTY_UMBRELLA  || itemId == ItemPool.POOL_SKIMMER )
			{
				return KoLCharacter.inRaincore();
			}
			return true;
		}
		else if ( storeId.equals( "gnomart" ) )
		{
			// Gno-Mart
			return !KoLCharacter.inZombiecore() && KoLCharacter.gnomadsAvailable();
		}
		else if ( storeId.equals( "mayoclinic" ) )
		{
			// The Mayo Clinic
			boolean available = false;
			AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
			if ( workshedItem != null )
			{
				available = workshedItem.getItemId() == ItemPool.MAYO_CLINIC && StandardRequest.isAllowed( "Items", "portable Mayo Clinic" );
				if ( itemId == ItemPool.MIRACLE_WHIP )
				{
					return available && !Preferences.getBoolean( "_mayoDeviceRented" ) && !Preferences.getBoolean( "mayoWhipRented" );
				}
				if ( itemId == ItemPool.SPHYGMAYOMANOMETER || itemId == ItemPool.REFLEX_HAMMER || itemId == ItemPool.MAYO_LANCE )
				{
					return available && !Preferences.getBoolean( "_mayoDeviceRented" );
				}
			}
			return available;
		}
		else if ( storeId.equals( "unclep" ) )
		{
			// Uncle P's Antiques
			return !KoLCharacter.inZombiecore() && KoLCharacter.desertBeachAccessible();
		}
		else if ( storeId.equals( "bartlebys" ) )
		{
			boolean available;
			if ( shopName.equals( "Barrrtleby's Barrrgain Books" ) )
			{
				available = !KoLCharacter.inBeecore();
			}
			else if ( shopName.equals( "Barrrtleby's Barrrgain Books (Bees Hate You)" ) )
			{
				available = KoLCharacter.inBeecore();
			}
			else
			{
				// What is this?
				return false;
			}

			if ( !available )
			{
				return false;
			}

			String itemName = ItemDatabase.getItemName( itemId );
			if ( Preferences.getInteger( "lastPirateEphemeraReset" ) == KoLCharacter.getAscensions()
				&& !Preferences.getString( "lastPirateEphemera" ).equalsIgnoreCase( itemName ) )
			{
				if ( NPCPurchaseRequest.PIRATE_EPHEMERA_PATTERN.matcher( itemName ).matches() )
				{
					return false;
				}
			}
			return EquipmentManager.hasOutfit( OutfitPool.SWASHBUCKLING_GETUP ) ||
				InventoryManager.hasItem( ItemPool.PIRATE_FLEDGES );
		}
		else if ( storeId.equals( "meatsmith" ) )
		{
			// Meatsmith's Shop
			return !KoLCharacter.inZombiecore();
		}
		else if ( storeId.equals( "whitecitadel" ) )
		{
			return QuestLogRequest.isWhiteCitadelAvailable();
		}
		else if ( storeId.equals( "nerve" ) )
		{
			// Nervewrecker's Store
			return KoLCharacter.inBadMoon();
		}
		else if ( storeId.equals( "armory" ) )
		{
			// Armory and Leggery
			return !KoLCharacter.inZombiecore();
		}
		else if ( storeId.equals( "fdkol" ) )
		{
			return false;
		}
		else if ( shopName.equals( "Gift Shop" ) )
		{
			return !KoLCharacter.inBadMoon();
		}
		else if ( storeId.equals( "hiddentavern" ) )
		{
			return Preferences.getInteger( "hiddenTavernUnlock" ) == KoLCharacter.getAscensions();
		}
		else if ( storeId.equals( "doc" ) )
		{
			if ( itemId == ItemPool.DOC_VITALITY_SERUM )
			{
				return QuestDatabase.isQuestFinished( Quest.DOC );
			}
		}
		else if ( storeId.equals( "mystic" ) )
		{
			if ( itemId == ItemPool.YELLOW_SUBMARINE )
			{
				return !KoLCharacter.desertBeachAccessible();
			}
			else if ( itemId == ItemPool.DIGITAL_KEY )
			{
				return !InventoryManager.hasItem( ItemPool.DIGITAL_KEY );
			}
		}

		// If it gets this far, then the item is definitely available
		// for purchase from the NPC store.

		return true;
	}

	public static final int itemIdByRow( final String shopId, final int row )
	{
		List<NPCPurchaseRequest> items = NPCStoreDatabase.ROW_ITEMS.get( row );
		if ( items == null )
		{
			return -1;
		}

		for ( NPCPurchaseRequest item : items )
		{
			if ( shopId.equals( item.getStoreId() ) )
			{
				return item.getItemId();
			}
		}

		return -1;
	}

	public static final boolean contains( final int itemId )
	{
		return NPCStoreDatabase.contains( itemId, true );
	}

	public static final int price( final int itemId )
	{
		PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest( itemId );
		return request == null ? 0 : request.getPrice();
	}

	public static final int availablePrice( final int itemId )
	{
		PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest( itemId );
		return request == null || !request.canPurchase() ? 0 : request.getPrice();
	}

	public static final boolean contains( final int itemId, boolean validate )
	{
		PurchaseRequest item = NPCStoreDatabase.getPurchaseRequest( itemId );
		return item != null && ( !validate || item.canPurchaseIgnoringMeat() );
	}
}
