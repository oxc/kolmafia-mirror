/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.widget;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.request.CreateItemRequest;

public class TableCellFactory
{
	public static Object get( int columnIndex, LockableListModel model, Object result, boolean[] flags,
			boolean isSelected )
	{
		if ( result instanceof AdventureResult )
		{
			AdventureResult advresult = (AdventureResult) result;

			if ( flags[ 0 ] ) // Equipment panel
			{
				return getEquipmentCell( columnIndex, isSelected, advresult );
			}
			if ( flags[ 1 ] ) // Restores panel
			{
				return getRestoresCell( columnIndex, isSelected, advresult );
			}
			if ( model == KoLConstants.storage )
			{
				return getStorageCell( columnIndex, isSelected, advresult );
			}
			return getGeneralCell( columnIndex, isSelected, advresult );
		}
		if ( result instanceof CreateItemRequest )
		{
			return getCreationCell( columnIndex, result, isSelected );
		}
		return null;
	}

	private static Object getGeneralCell( int columnIndex, boolean isSelected, AdventureResult advresult )
	{
		switch ( columnIndex )
		{
		case 0:
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId() );
		case 2:
			return Integer.valueOf( advresult.getCount() );
		case 3:
			int price = MallPriceDatabase.getPrice( advresult.getItemId() );
			return ( price > 0 ) ? price : null;
		case 4:
			return EquipmentDatabase.getPower( advresult.getItemId() );
		default:
			return null;
		}
	}

	private static Object getStorageCell( int columnIndex, boolean isSelected, AdventureResult advresult )
	{
		switch ( columnIndex )
		{
		case 0:
			return "<html>" + addTag( ColorFactory.getStorageColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId() );
		case 2:
			return Integer.valueOf( advresult.getCount() );
		case 3:
			int price = MallPriceDatabase.getPrice( advresult.getItemId() );
			return ( price > 0 ) ? price : null;
		case 4:
			int hpRestore = HPRestoreItemList.getHealthRestored( advresult.getName() );
			if ( hpRestore <= 0 )
			{
				return null;
			}
			int maxHP = KoLCharacter.getMaximumHP();
			if ( hpRestore > maxHP )
			{
				return maxHP;
			}
			return hpRestore;
		case 5:
			int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
			if ( mpRestore <= 0 )
			{
				return null;
			}
			int maxMP = KoLCharacter.getMaximumMP();
			if ( mpRestore > maxMP )
			{
				return maxMP;
			}
			return mpRestore;
		default:
			return null;
		}
	}

	private static Object getCreationCell( int columnIndex, Object result, boolean isSelected )
	{
		CreateItemRequest CIRresult = (CreateItemRequest) result;

		switch ( columnIndex )
		{
		case 0:
			return "<html>" + addTag( ColorFactory.getCreationColor( CIRresult ), isSelected )
				+ CIRresult.getName();
		case 1:
			return getAutosellString( CIRresult.getItemId() );
		case 2:
			return Integer.valueOf( CIRresult.getQuantityPossible() );
		case 3:
			int price = MallPriceDatabase.getPrice( CIRresult.getItemId() );
			return ( price > 0 ) ? price : null;
		case 4:
			int fill = CIRresult.concoction.getFullness() + CIRresult.concoction.getInebriety();
			return fill > 0 ? fill : null;
		case 5:
			float advRange = ItemDatabase.getAdventureRange( CIRresult.getName() );
			return advRange > 0 ? KoLConstants.ROUNDED_MODIFIER_FORMAT.format( advRange ) : null;
		default:
			return null;
		}
	}

	private static Object getEquipmentCell( int columnIndex, boolean isSelected, AdventureResult advresult )
	{
		switch ( columnIndex )
		{
		case 0:
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return EquipmentDatabase.getPower( advresult.getItemId() );
		case 2:
			return Integer.valueOf( advresult.getCount() );
		case 3:
			int price = MallPriceDatabase.getPrice( advresult.getItemId() );
			return ( price > 0 ) ? price : null;
		case 4:
			return getAutosellString( advresult.getItemId() );
		case 5:
			int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
			if ( mpRestore <= 0 )
			{
				return null;
			}
			int maxMP = KoLCharacter.getMaximumMP();
			if ( mpRestore > maxMP )
			{
				return maxMP;
			}
			return mpRestore;
		default:
			return null;
		}
	}

	private static Object getRestoresCell( int columnIndex, boolean isSelected, AdventureResult advresult )
	{
		switch ( columnIndex )
		{
		case 0:
			return "<html>" + addTag( ColorFactory.getItemColor( advresult ), isSelected )
				+ advresult.getName();
		case 1:
			return getAutosellString( advresult.getItemId() );
		case 2:
			return Integer.valueOf( advresult.getCount() );
		case 3:
			int price = MallPriceDatabase.getPrice( advresult.getItemId() );
			return ( price > 0 ) ? price : null;
		case 4:
			int hpRestore = HPRestoreItemList.getHealthRestored( advresult.getName() );
			if ( hpRestore <= 0 )
			{
				return null;
			}
			int maxHP = KoLCharacter.getMaximumHP();
			if ( hpRestore > maxHP )
			{
				return maxHP;
			}
			return hpRestore;
		case 5:
			int mpRestore = MPRestoreItemList.getManaRestored( advresult.getName() );
			if ( mpRestore <= 0 )
			{
				return null;
			}
			int maxMP = KoLCharacter.getMaximumMP();
			if ( mpRestore > maxMP )
			{
				return maxMP;
			}
			return mpRestore;
		default:
			return null;
		}
	}

	private static String addTag( String itemColor, boolean isSelected )
	{
		if ( itemColor == null || isSelected )
		{
			return "";
		}
		return "<font color=" + itemColor + ">";
	}

	private static String getAutosellString( int itemId )
	{
		int price = ItemDatabase.getPriceById( itemId );

		if ( price <= 0 )
		{
			return "no-sell";
		}
		return price + " meat";
	}

	public static String[] getColumnNames( LockableListModel originalModel, boolean[] flags )
	{
		if ( flags[ 0 ] ) // Equipment panel
		{
			return new String[]
			{
				"item name", "power", "quantity", "mallprice", "autosell"
			};
		}
		else if ( flags[ 1 ] ) // Restores panel
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "HP restore", "MP restore"
			};
		}
		else if ( originalModel == KoLConstants.inventory || originalModel == KoLConstants.tally
			|| originalModel == KoLConstants.freepulls || originalModel == KoLConstants.storage
			|| originalModel == KoLConstants.closet )
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "power"
			};
		}

		else if ( originalModel == ConcoctionDatabase.getCreatables()
			|| originalModel == ConcoctionDatabase.getUsables() )
		{
			return new String[]
			{
				"item name", "autosell", "quantity", "mallprice", "fill", "adv/fill"
			};
		}
		return new String[]
		{
			"not implemented"
		};
	}

}
