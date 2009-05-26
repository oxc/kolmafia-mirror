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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;

import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class ListCellRendererFactory
{
	public static final DefaultListCellRenderer getDefaultRenderer()
	{
		return new DefaultRenderer();
	}

	private static class DefaultRenderer
		extends DefaultListCellRenderer
	{
		public DefaultRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return false;
		}

		public final Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
			{
				return defaultComponent;
			}

			if ( value instanceof AdventureResult )
			{
				return this.getRenderer( defaultComponent, (AdventureResult) value );
			}

			if ( value instanceof CreateItemRequest )
			{
				return this.getRenderer( defaultComponent, (CreateItemRequest) value );
			}

			if ( value instanceof Concoction )
			{
				return this.getRenderer( defaultComponent, (Concoction) value,
					list.getWidth() );
			}

			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			int itemId = ar.getItemId();

			if ( itemId > 0 )
			{
				int value = ItemDatabase.getPriceById( itemId );

				if ( value == 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=olive>" );
				stringForm.append( "</font></html>" );
			}
			else if ( KoLConstants.junkList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final CreateItemRequest icr )
		{
			StringBuffer stringForm = new StringBuffer();
			stringForm.append( icr.getName() );

			int itemId = icr.getItemId();

			if ( itemId > 0 )
			{
				int value = ItemDatabase.getPriceById( itemId );

				if ( value == 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( icr.getQuantityPossible() ) );
			if ( icr.getQuantityPullable() > 0 )
			{
				stringForm.append( ", " );
				stringForm.append( KoLConstants.COMMA_FORMAT.format(
					icr.getQuantityPullable() ) );
				stringForm.append( " pullable" );
			}
			stringForm.append( ")" );

			boolean isHTML = false;
			if ( icr.getQuantityPossible() == 0 && icr.getQuantityPullable() > 0 )
			{
				stringForm.insert( 0, "<i>" );
				stringForm.append( "</i>" );
				isHTML = true;
			}
			if ( KoLConstants.junkList.contains( icr.createdItem ) )
			{
				stringForm.insert( 0, "<font color=gray>" );
				stringForm.append( "</font>" );
				isHTML = true;
			}
			if ( isHTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final Concoction item,
			final int listWidth )
		{
			StringBuffer stringForm = new StringBuffer();
			String name = item.getName();
			boolean meetsRequirement = ItemDatabase.meetsLevelRequirement( name );

			stringForm.append( "<html><nobr>" );

			if ( !meetsRequirement )
			{
				stringForm.append( "<font color=#c0c0c0>" );
			}

			stringForm.append( "<b>" );
			stringForm.append( name );

			stringForm.append( " (" );
			boolean pulling = this.appendAmount( stringForm, item );

			stringForm.append( ")" );
			stringForm.append( "</b></nobr><br><nobr>&nbsp;" );
			
			switch ( item.getItemId() )
			{
			case ItemPool.MUNCHIES_PILL:
				stringForm.append( "+1-3 adv from next food eaten" );
				break;
				
			case ItemPool.SCRATCHS_FORK:
				stringForm.append( "+30% adv/mus/mys/mox from next food, +50% from salad" );
				break;
			
			case ItemPool.DIVINE_FLUTE:
				stringForm.append( "+(7*adv)+(0-15) MP from next drink" );
				break;
			
			case ItemPool.FROSTYS_MUG:
				stringForm.append( "+30% adv/mus/mys/mox from next drink, +50% from beer" );
				break;
			
			default:
				int fullness = ItemDatabase.getFullness( name );
				int inebriety = ItemDatabase.getInebriety( name );
				int spleenhit = ItemDatabase.getSpleenHit( name );
	
				if ( fullness > 0 )
				{
					stringForm.append( fullness );
					stringForm.append( " full" );
				}
				else if ( inebriety > 0 )
				{
					stringForm.append( inebriety );
					stringForm.append( " drunk" );
				}
				else if ( spleenhit > 0 )
				{
					stringForm.append( spleenhit );
					stringForm.append( " spleen" );
				}
	
				this.appendRange( stringForm, ItemDatabase.getAdventureRange( name ), "adv" );
	
				if ( Preferences.getBoolean( "showGainsPerUnit" ) )
				{
					if ( fullness > 0 )
					{
						stringForm.append( " / full" );
					}
					else if ( inebriety > 0 )
					{
						stringForm.append( " / drunk" );
					}
					else if ( spleenhit > 0 )
					{
						stringForm.append( " / spleen" );
					}
				}
	
				this.appendRange( stringForm, ItemDatabase.getMuscleRange( name ), "mus" );
				this.appendRange( stringForm, ItemDatabase.getMysticalityRange( name ), "mys" );
				this.appendRange( stringForm, ItemDatabase.getMoxieRange( name ), "mox" );
				String notes = ItemDatabase.getNotes( name );
				if ( notes != null )
				{
					stringForm.append( ", " );
					stringForm.append( notes );
				}
			}

			if ( !meetsRequirement )
			{
				stringForm.append( "</font>" );
			}
			
			if ( pulling )
			{
				stringForm.insert( 6, "<i>" );
				stringForm.append( "</i>" );
			}

			stringForm.append( "</nobr></html>" );

			defaultComponent.setFont( KoLConstants.DEFAULT_FONT );
			String text = stringForm.toString();
			( (JLabel) defaultComponent ).setText( text );
			if ( defaultComponent.getPreferredSize().width > listWidth )
			{
				((JComponent) defaultComponent).setToolTipText( text );
			}
			else
			{
				((JComponent) defaultComponent).setToolTipText( null );
			}
			return defaultComponent;
		}

		public boolean appendAmount( final StringBuffer stringForm, final Concoction item )
		{
			if ( item.getItem() != null )
			{
				int modified = item.getAvailable();
				int initial = 0;

				if ( item.getItem() != null )
				{
					initial = item.getItem().getCount( KoLConstants.inventory );
				}

				stringForm.append( modified );
				stringForm.append( " possible, " );
				stringForm.append( initial );
				stringForm.append( " current" );
			}
			else
			{
				stringForm.append( CafeRequest.discountedPrice( item.getPrice() ) );
				stringForm.append( " meat" );
				return false;
			}
			return item.initial + item.creatable <= 0;
		}

		protected void appendRange( final StringBuffer stringForm, final float range, final String suffix )
		{
			String val = ( range == 0.0f ) ? "+0.00" : KoLConstants.ROUNDED_MODIFIER_FORMAT.format( range );
			this.appendRange( stringForm, val, suffix );
		}

		protected void appendRange( final StringBuffer stringForm, final String range, final String suffix )
		{
			if ( range.equals( "+0.0" ) && !suffix.equals( "adv" ) )
			{
				return;
			}

			stringForm.append( ", " );
			stringForm.append( range );
			stringForm.append( " " );
			stringForm.append( suffix );
		}
	}

	public static final DefaultListCellRenderer getEquipmentPowerRenderer()
	{
		return new EquipmentPowerRenderer();
	}

	private static class EquipmentPowerRenderer
		extends DefaultRenderer
	{
		public EquipmentPowerRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=olive>" );
				stringForm.append( "</font></html>" );
			}
			else if ( KoLConstants.junkList.contains( ar ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final CreateItemRequest icr )
		{
			StringBuffer stringForm = new StringBuffer();
			stringForm.append( icr.getName() );

			int power = EquipmentDatabase.getPower( icr.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( icr.getQuantityPossible() ) );
			if ( icr.getQuantityPullable() > 0 )
			{
				stringForm.append( ", " );
				stringForm.append( KoLConstants.COMMA_FORMAT.format(
					icr.getQuantityPullable() ) );
				stringForm.append( " pullable" );
			}
			stringForm.append( ")" );
			
			boolean isHTML = false;
			if ( icr.getQuantityPossible() == 0 && icr.getQuantityPullable() > 0 )
			{
				stringForm.insert( 0, "<i>" );
				stringForm.append( "</i>" );
				isHTML = true;
			}
			if ( KoLConstants.junkList.contains( icr.createdItem ) )
			{
				stringForm.insert( 0, "<font color=gray>" );
				stringForm.append( "</font>" );
				isHTML = true;
			}
			if ( isHTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getCreationQueueRenderer()
	{
		return new CreationQueueRenderer();
	}

	private static class CreationQueueRenderer
		extends DefaultRenderer
	{
		public CreationQueueRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return false;
		}

		public boolean appendAmount( final StringBuffer stringForm, final Concoction item )
		{
			stringForm.append( item.getQueued() );
			return item.queuedPulls != 0;
		}
	}

	public static final DefaultListCellRenderer getNameOnlyRenderer()
	{
		return new NameOnlyRenderer();
	}

	private static class NameOnlyRenderer
		extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			return super.getListCellRendererComponent(
				list, value instanceof AdventureResult ? ( (AdventureResult) value ).getName() : value, index,
				isSelected, cellHasFocus );
		}
	}

	public static final DefaultListCellRenderer getUsableEquipmentRenderer()
	{
		return new UsableEquipmentRenderer();
	}

	private static class UsableEquipmentRenderer
		extends DefaultListCellRenderer
	{
		public UsableEquipmentRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			AdventureResult ar = (AdventureResult) value;
			int equipmentType = ItemDatabase.getConsumptionType( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );
			String stringForm = null;

			if ( equipmentType == KoLConstants.EQUIP_FAMILIAR || ar.equals( EquipmentRequest.UNEQUIP ) )
			{
				if ( ar.equals( EquipmentRequest.UNEQUIP ) )
				{
					stringForm = ar.getName();
				}
				else if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().canEquip( ar ) )
				{
					stringForm = ar.getName();
				}
				else
				{
					stringForm = "<html><font color=gray>" + ar.getName() + "</font></html>";
				}
			}
			else
			{
				if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
				{
					int count = ar.getCount( KoLConstants.inventory );
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ) ) )
					{
						++count;
					}
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ) ) )
					{
						++count;
					}
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ) ) )
					{
						++count;
					}
					stringForm = ar.getName() + " (" + count + " max)";
				}
				else if ( power > 0 )
				{
					stringForm = ar.getName() + " (+" + KoLConstants.COMMA_FORMAT.format( power ) + ")";
				}
				else
				{
					stringForm = ar.getName();
				}

				// Gray out any equipment that the player cannot currently equip
				// inside of an equipment filter.

				if ( !EquipmentManager.canEquip( ar.getName() ) )
				{
					stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
				}
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFamiliarEquipmentRenderer()
	{
		return new FamiliarEquipmentRenderer();
	}

	private static class FamiliarEquipmentRenderer
		extends UsableEquipmentRenderer
	{
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			AdventureResult ar = (AdventureResult) value;
			String effect = Modifiers.getFamiliarEffect( ar.getName() );
			if ( effect == null )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			String stringForm = ar.getName() + " (" + effect + ")";
			if ( KoLCharacter.getFamiliar() == null || 
				!KoLCharacter.getFamiliar().canEquip( ar ) )
			{
				stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getStorageRenderer()
	{
		return new StorageRenderer();
	}

	private static class StorageRenderer
		extends DefaultRenderer
	{
		public StorageRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}
			else
			{
				int fullness = ItemDatabase.getFullness( ar.getName() );
				int inebriety = ItemDatabase.getInebriety( ar.getName() );
				
				if ( fullness + inebriety > 0 )
				{
					stringForm.append( " (" );
					if ( fullness > 0 )
					{
						stringForm.append( fullness );
					}
					else 	//if ( inebriety > 0 )
					{
						stringForm.append( inebriety );
					}
	
					this.appendRange( stringForm,
						ItemDatabase.getAdventureRange( ar.getName() ), "adv" );
	
					if ( Preferences.getBoolean( "showGainsPerUnit" ) )
					{
						if ( fullness > 0 )
						{
							stringForm.append( "/full" );
						}
						else 	//if ( inebriety > 0 )
						{
							stringForm.append( "/drunk" );
						}
					}
					stringForm.append( ")" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			if ( !ItemDatabase.meetsLevelRequirement( ar.getName() ) ||
				!EquipmentManager.canEquip( ar.getName() ) )
			{
				stringForm.insert( 0, "<html><font color=gray>" );
				stringForm.append( "</font></html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFreePullsRenderer()
	{
		return new FreePullsRenderer();
	}

	private static class FreePullsRenderer
		extends DefaultRenderer
	{
		public FreePullsRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getPulverizationRenderer()
	{
		return new PulverizationRenderer();
	}

	private static class PulverizationRenderer
		extends DefaultRenderer
	{
		public PulverizationRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			int pulver = EquipmentDatabase.getPulverization( ar.getItemId() );
			boolean HTML = false;

			if ( pulver > 0 )
			{
				stringForm.append( " => " );
				stringForm.append( ItemDatabase.getItemName( pulver ) );
			}
			else if ( pulver < -1 )
			{
				stringForm.append( " => " );
				if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
				{
					stringForm.append( "Tw" );
				}
				if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
				{
					stringForm.append( "<font color=red>Ho</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
				{
					stringForm.append( "<font color=blue>Co</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
				{
					stringForm.append( "<font color=green>St</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
				{
					stringForm.append( "<font color=gray>Sp</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
				{
					stringForm.append( "<font color=purple>Sl</font>" );
					HTML = true;
				}
				
				if ( (pulver & EquipmentDatabase.MALUS_UPGRADE ) != 0 )
				{
					stringForm.append( " upgrade" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3W) != 0 )
				{
					stringForm.append( " 3W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1W3N_2W) != 0 )
				{
					stringForm.append( "  1W+3N or 2W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_4N_1W) != 0 )
				{
					stringForm.append( " 4N or 1W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3N) != 0 )
				{
					stringForm.append( " 3N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1N3P_2N) != 0 )
				{
					stringForm.append( " 1N+3P or 2N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_4P_1N) != 0 )
				{
					stringForm.append( " 4P or 1N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3P) != 0 )
				{
					stringForm.append( " 3P" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_2P) != 0 )
				{
					stringForm.append( " 2P" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1P) != 0 )
				{
					stringForm.append( " 1P" );
				}

				if ( (pulver & EquipmentDatabase.YIELD_UNCERTAIN) != 0 )
				{
					stringForm.append( "?" );
				}
			}
			else
			{	// this should have been filtered out of the list
				stringForm.append( " [NOT PULVERIZABLE]" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );
			
			int index = KoLConstants.pulverizeQueue.indexOf( ar );
			if ( index != -1 )
			{
				stringForm.append( ", " );
				stringForm.append(
					((AdventureResult)KoLConstants.pulverizeQueue.get( index )).getCount() );
				stringForm.append( " queued" );
			}

			if ( HTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}
			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
