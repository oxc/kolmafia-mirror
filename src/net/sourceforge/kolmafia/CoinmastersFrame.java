/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionListener;

import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class CoinmastersFrame
	extends KoLFrame
{
	public CoinmastersFrame()
	{
		super( "Coin Masters" );

		JPanel dimePanel = new JPanel( new BorderLayout() );
		dimePanel.add( new DimemasterPanel() );
		JPanel quarterPanel = new JPanel( new BorderLayout() );
		quarterPanel.add( new QuartersmasterPanel() );

		this.tabs.add( "Dimemaster", dimePanel );
		this.tabs.add( "Quartersmaster", quarterPanel );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
		this.setTitle();
	}

	private void setTitle()
	{
		int dimes = KoLSettings.getIntegerProperty( "availableDimes" );
		int quarters = KoLSettings.getIntegerProperty( "availableQuarters" );
		super.setTitle( "Coin Masters (" + dimes + " dimes/" + quarters + " quarters)" );
	}

	private class DimemasterPanel
		extends CoinmasterPanel
	{
		public DimemasterPanel()
		{
			super( CoinmastersDatabase.getDimeItems(), CoinmastersDatabase.dimeSellPrices(), CoinmastersDatabase.dimeBuyPrices(), "availableDimes", "dime" );
		}
	}

	private class QuartersmasterPanel
		extends CoinmasterPanel
	{
		public QuartersmasterPanel()
		{
			super( CoinmastersDatabase.getQuarterItems(), CoinmastersDatabase.quarterSellPrices(), CoinmastersDatabase.quarterBuyPrices(), "availableQuarters", "quarter" );
		}
	}

	private class CoinmasterPanel
		extends JPanel
	{
		private LockableListModel purchases;
		private Map sellPrices;
		private Map buyPrices;
		private String property;
		private String token;

		public CoinmasterPanel( LockableListModel purchases, Map sellPrices, Map buyPrices, String property, String token )
		{
			super( new BorderLayout() );

			this.sellPrices = sellPrices;
			this.buyPrices = buyPrices;
			this.property = property;
			this.token = token;

			SellPanel sellPanel = new SellPanel( sellPrices, token );
			BuyPanel buyPanel = new BuyPanel( purchases, buyPrices, token );

			this.add( sellPanel, BorderLayout.NORTH );
			this.add( buyPanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		private class SellPanel
			extends ItemManagePanel
		{
			public SellPanel( Map prices, String token )
			{
				super( KoLConstants.inventory );
				this.setButtons( true, new ActionListener[] {
						new SellListener(),
					} );

				this.elementList.setCellRenderer( getCoinmasterRenderer( prices, token ) );
				this.setEnabled( true );
				this.filterItems();
			}

			public void addFilters()
			{
			}

			public FilterTextField getWordFilter()
			{
				return new SellableFilterField();
			}

			public void actionConfirmed()
			{
			}

			public void actionCancelled()
			{
			}

			public class SellListener
				extends ThreadedListener
			{
				public void run()
				{
					if ( !KoLFrame.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					Object[] items = SellPanel.this.getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					// RequestThread.postRequest( new CoinmasterRequest( items ) );
				}

				public String toString()
				{
					return "sell";
				}
			}

			private class SellableFilterField
				extends FilterItemField
			{
				public boolean isVisible( final Object element )
				{
					if ( !( element instanceof AdventureResult ) )
					{
						return false;
					}
					AdventureResult ar = (AdventureResult)element;
					Object price = CoinmasterPanel.this.sellPrices.get( ar.getName() );
					return ( price != null ) && super.isVisible( element );
				}
			}
		}

		private class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel( LockableListModel purchases, Map prices, String token )
			{
				super( purchases );

				this.setButtons( true, new ActionListener[] {
						new BuyListener(),
					} );

				this.elementList.setCellRenderer( getCoinmasterRenderer( prices, token ) );
				this.elementList.setVisibleRowCount( 6 );
				this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				this.setEnabled( true );
			}

			public void addFilters()
			{
			}

			public void addMovers()
			{
			}

			public class BuyListener
				extends ThreadedListener
			{
				public void run()
				{
					Object[] items = BuyPanel.this.getDesiredItems( "Buying" );
					if ( items == null )
					{
						return;
					}

					// RequestThread.postRequest( new CoinmasterRequest( items ) );
				}

				public String toString()
				{
					return "buy";
				}
			}
		}
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token )
	{
		return new CoinmasterRenderer( prices, token );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private Map prices;
		private String token;

		public CoinmasterRenderer( final Map prices, final String token )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
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

			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			String name = ar.getName();
			Integer price = (Integer)prices.get( name );

			if ( price == null )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );
			stringForm.append( price );
			stringForm.append( " " );
			stringForm.append( token );
			if ( price.intValue() > 1 )
				stringForm.append( "s" );
			stringForm.append( ")" );

			int count = ar.getCount();
			if ( count > 0 )
			{
				stringForm.append( " (" );
				stringForm.append( KoLConstants.COMMA_FORMAT.format( count ) );
				stringForm.append( ")" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
