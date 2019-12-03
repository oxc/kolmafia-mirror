 /**
 * Copyright (c) 2005-2019, KoLmafia development team
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

package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.StandardRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;


public class CheckedItem
	extends AdventureResult
{
	public CheckedItem( int itemId, int equipLevel, int maxPrice, int priceLevel )
	{
		super( itemId, 1, false );

		// special case used to get a CheckItem that .equals( EquipmentRequest.UNEQUIP ).
		if ( itemId == 0 )
		{
			this.name = "(none)";
		}

		this.inventory = InventoryManager.getCount( itemId );

		this.initial = InventoryManager.getAccessibleCount( itemId );

		String itemName = this.getName();
		this.foldable = 0;

		if ( itemId > 0 && Preferences.getBoolean( "maximizerFoldables" ) )
		{
			ArrayList group = ItemDatabase.getFoldGroup( itemName );
			if ( group != null )
			{
				for ( int i = 1; i < group.size(); ++i )
				{
					String form = (String) group.get( i );
					if ( !form.equals( itemName ) )
					{
						int foldItemId = ItemDatabase.getItemId( form );
						int count = InventoryManager.getAccessibleCount( foldItemId );
						this.foldable += count;
						if ( count > 0 )
						{
							this.foldItemId = foldItemId;
						}
					}
				}
				// Cannot have more than one item from Januuary's Garbage Tote, no matter how many you have
				// Fold groups are stored in lower case
				if ( ( (String) group.get( 1 ) ).equals( "january's garbage tote" ) )
				{
					if ( this.foldable + this.initial > 1 )
					{
						this.foldable = 1 - this.initial;
					}
				}
			}
		}

		boolean skillCreateCheck = Preferences.getBoolean( "maximizerCreateOnHand" ) && equipLevel == 1 &&
									!ItemDatabase.isEquipment( itemId );
		if ( this.initial >= 3 || ( equipLevel < 2 && !skillCreateCheck ) )
		{
			return;
		}

		Concoction c = ConcoctionPool.get( itemId );
		if ( c == null )
			return;

		this.creatable = c.creatable;

		if ( c.getAdventuresNeeded( 1 ) > 0 && Preferences.getBoolean( "maximizerNoAdventures" ) )
		{
			this.creatable = 0;
		}
		else if ( c.price > 0 )
		{
			this.npcBuyable = maxPrice / c.price;
			int limit = CheckedItem.limitBuyable( itemId );
			if ( limit < this.npcBuyable )
			{
				this.npcBuyable = limit;
			}
		}

		if ( this.getCount() >= 3 || equipLevel < 3 )
		{
			return;
		}

		if ( !StandardRequest.isAllowed( "Items", itemName ) )
		{
			// Unallowed items can't be bought or pulled, though the original code
			// just reset everything to zero

			this.initial = 0;
			this.creatable = 0;
			this.npcBuyable = 0;
		}
		else if ( InventoryManager.canUseMall( itemId ) )
		{
			// consider Mall buying, but only if none are otherwise available
			if ( this.getCount() == 0 )
			{	
				// We include things with historical price up to twice as high as limit, as current price may be lower
				long price = Math.min( maxPrice, KoLCharacter.getAvailableMeat() );
				if ( priceLevel == 0 ||
					MallPriceDatabase.getPrice( itemId ) < price * 2 )
				{
					this.mallBuyable = 1;
					this.buyableFlag = true;
				}
			}
		}
		else if ( !KoLCharacter.isHardcore() )
		{
			// consider pulling
			this.pullable = this.getCount( KoLConstants.storage );

			this.pullBuyable = 0;
			if ( InventoryManager.canUseMallToStorage( itemId ) )
			{
				// consider Mall buying, but only if none are otherwise available
				if ( this.getCount() == 0 )
				{	
					// We include things with historical price up to twice as high as limit, as current price may be lower
					long price = Math.min( maxPrice, KoLCharacter.getStorageMeat() );
					if ( priceLevel == 0 ||	MallPriceDatabase.getPrice( itemId ) < price * 2 )
					{
						this.pullBuyable = 1;
						this.buyableFlag = true;
					}
				}
			}

			this.pullfoldable = 0;
			if ( itemId > 0 && Preferences.getBoolean( "maximizerFoldables" ) )
			{
				ArrayList group = ItemDatabase.getFoldGroup( itemName );
				if ( group != null )
				{
					for ( int i = 1; i < group.size(); ++i )
					{
						String form = (String) group.get( i );
						if ( !form.equals( itemName ) )
						{
							int foldItemId = ItemDatabase.getItemId( form );
							AdventureResult foldItem = ItemPool.get( foldItemId );
							int count = foldItem.getCount( KoLConstants.storage );
							this.pullfoldable += count;
							if ( count > 0 )
							{
								this.foldItemId = foldItemId;
							}
						}
					}
					// Cannot have more than one item from Januuary's Garbage Tote, no matter how many you have
					if ( ( (String) group.get( 1 ) ).equals( "january's garbage tote" ) )
					{
						if ( this.pullfoldable > 1 )
						{
							this.pullfoldable = 1;
						}
					}
				}
			}
		}

		// We never want to suggest turning Mr. Accessories into other items
		if ( c.getIngredients().length > 0 && (
		     ( MrStoreRequest.MR_A ).equals( c.getIngredients()[0] ) ||
		     ( MrStoreRequest.UNCLE_B ).equals( c.getIngredients()[0] ) ) )
		{
			this.creatable = 0;
		}
	}

	@Override
	public final int getCount()
	{
		if ( this.getItemId() == 0 )
		{
			// We have all the no items you'd ever want!
			return Integer.MAX_VALUE;
		}
		if ( this.singleFlag )
		{
			return (int)Math.min( 1, this.initial + this.creatable + this.npcBuyable + this.mallBuyable + this.foldable + this.pullable + this.pullfoldable + this.pullBuyable );
		}

		return (int)( this.initial + this.creatable + this.npcBuyable + this.mallBuyable + this.foldable + this.pullable + this.pullfoldable + this.pullBuyable );
	}

	public void validate( int maxPrice, int priceLevel )
	{
		if ( priceLevel <= 0 )
		{
			return;
		}

		if ( !this.buyableFlag )
		{
			return;
		}

		// Check mall price
		int price = StoreManager.getMallPrice( this );

		// Check if too expensive for max price settings
		if ( price <= 0 || price > maxPrice )
		{
			this.mallBuyable = 0;
			this.pullBuyable = 0;
		}

		// Check character has meat to buy with
		if ( price > KoLCharacter.getAvailableMeat() )
		{
			this.mallBuyable = 0;
		}

		// Check character has storage meat to buy for pulling
		if ( price > KoLCharacter.getStorageMeat() )
		{
			this.pullBuyable = 0;
		}
	}

	private static final int limitBuyable( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.MIRACLE_WHIP:
			return  Preferences.getBoolean( "_mayoDeviceRented" ) ||
				Preferences.getBoolean( "itemBoughtPerAscension8266" ) ? 0 : 1;
		case ItemPool.SPHYGMAYOMANOMETER:
		case ItemPool.REFLEX_HAMMER:
		case ItemPool.MAYO_LANCE:
			return Preferences.getBoolean( "_mayoDeviceRented" ) ? 0 : 1;
		}
		return Integer.MAX_VALUE;
	}

	public static final int TOTAL_MASK = 0xFF;
	public static final int SUBTOTAL_MASK = 0x0F;
	public static final int INITIAL_SHIFT = 8;
	public static final int CREATABLE_SHIFT = 12;
	public static final int NPCBUYABLE_SHIFT = 16;
	public static final int FOLDABLE_SHIFT = 20;
	public static final int PULLABLE_SHIFT = 24;
	public static final int BUYABLE_FLAG = 1 << 28;
	public static final int AUTOMATIC_FLAG = 1 << 29;
	public static final int CONDITIONAL_FLAG = 1 << 30;

	public int inventory;
	public int initial;
	public long creatable;
	public int npcBuyable;
	public int mallBuyable;
	public int foldable;
	public int pullable;
	public int pullfoldable;
	public int pullBuyable;
	public int foldItemId;

	public boolean buyableFlag;
	public boolean automaticFlag;
	public boolean requiredFlag;
	public boolean conditionalFlag;
	public boolean singleFlag;
}
