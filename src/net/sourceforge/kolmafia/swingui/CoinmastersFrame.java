/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;

import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CoinmastersFrame
	extends GenericFrame
	implements ChangeListener
{
	public static final AdventureResult LUCRE = ItemPool.get( ItemPool.LUCRE, -1 );
	public static final AdventureResult SAND_DOLLAR = ItemPool.get( ItemPool.SAND_DOLLAR, -1 );
	public static final AdventureResult CRIMBUCK = ItemPool.get( ItemPool.CRIMBUCK, -1 );
	public static final AdventureResult TICKET = ItemPool.get( ItemPool.GG_TICKET, -1 );

	public static final AdventureResult AERATED_DIVING_HELMET = ItemPool.get( ItemPool.AERATED_DIVING_HELMET, 1 );
	public static final AdventureResult SCUBA_GEAR = ItemPool.get( ItemPool.SCUBA_GEAR, 1 );
	public static final AdventureResult BATHYSPHERE = ItemPool.get( ItemPool.BATHYSPHERE, 1 );
	public static final AdventureResult DAS_BOOT = ItemPool.get( ItemPool.DAS_BOOT, 1 );
	public static final AdventureResult BUBBLIN_STONE = ItemPool.get( ItemPool.BUBBLIN_STONE, 1 );

	public static final int WAR_HIPPY_OUTFIT = 32;
	public static final int WAR_FRAT_OUTFIT = 33;

	private static CoinmastersFrame INSTANCE = null;
	private static boolean atWar = false;
	private static int dimes = 0;
	private static int quarters = 0;
	private static int lucre = 0;
	private static int sandDollars = 0;
	private static int crimbux = 0;
	private static int tickets = 0;

	private CoinmasterPanel dimePanel = null;
	private CoinmasterPanel quarterPanel = null;
	private CoinmasterPanel lucrePanel = null;
	private CoinmasterPanel sandDollarPanel = null;
	private CoinmasterPanel ticketPanel = null;
	// private CoinmasterPanel crimbuckPanel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );
		CoinmastersFrame.INSTANCE = this;

		JPanel panel = new JPanel( new BorderLayout() );
		dimePanel = new DimemasterPanel();
		panel.add( dimePanel );
		this.tabs.add( "Dimemaster", panel );

		panel = new JPanel( new BorderLayout() );
		quarterPanel = new QuartersmasterPanel();
		panel.add( quarterPanel );
		this.tabs.add( "Quartersmaster", panel );

		panel = new JPanel( new BorderLayout() );
		lucrePanel = new BountyHunterHunterPanel();
		panel.add( lucrePanel );
		this.tabs.add( "Bounty Hunter Hunter", panel );

		panel = new JPanel( new BorderLayout() );
		sandDollarPanel = new BigBrotherPanel();
		panel.add( sandDollarPanel );
		this.tabs.add( "Big Brother", panel );

		panel = new JPanel( new BorderLayout() );
		ticketPanel = new TicketCounterPanel();
		panel.add( ticketPanel );
		this.tabs.add( "Ticket Counter", panel );

		// panel = new JPanel( new BorderLayout() );
		// crimbuckPanel = new CrimboCartelPanel();
		// panel.add( crimbuckPanel );
		// this.tabs.add( "Crimbo Cartel", panel );

		this.tabs.addChangeListener( this );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
		CoinmastersFrame.externalUpdate();
	}

	/**
	 * Whenever the tab changes, this method is used to change the title to
	 * count the coins of the new tab
	 */

	private CoinmasterPanel currentPanel()
	{
		return (CoinmasterPanel)( ((Container)(this.tabs.getSelectedComponent())).getComponent( 0 ) );
	}

	public void stateChanged( final ChangeEvent e )
	{
		this.currentPanel().setTitle();
	}

	public static void externalUpdate()
	{
		if ( INSTANCE == null )
		{
			return;
		}

		IslandDecorator.ensureUpdatedBigIsland();
		atWar = Preferences.getString( "warProgress" ).equals( "started" );
		dimes = Preferences.getInteger( "availableDimes" );
		quarters = Preferences.getInteger( "availableQuarters" );
		lucre = LUCRE.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableLucre", lucre );
		sandDollars = SAND_DOLLAR.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableSandDollars", sandDollars );
		crimbux = CRIMBUCK.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableCrimbux", crimbux );
		tickets = TICKET.getCount( KoLConstants.inventory );
		Preferences.setInteger( "availableTickets", tickets );

		INSTANCE.update();
	}

	private void update()
	{
		dimePanel.update();
		quarterPanel.update();
		lucrePanel.update();
		sandDollarPanel.update();
		ticketPanel.update();
		// crimbuckPanel.update();
		this.currentPanel().setTitle();
	}

	private class DimemasterPanel
		extends WarMasterPanel
	{
		public DimemasterPanel()
		{
			super( CoinmastersDatabase.getDimeItems(),
			       CoinmastersDatabase.dimeSellPrices(),
			       CoinmastersDatabase.dimeBuyPrices(),
			       WAR_HIPPY_OUTFIT,
			       "availableDimes",
			       "dime",
			       "dimemaster",
			       "hippy");
		}
	}

	private class QuartersmasterPanel
		extends WarMasterPanel
	{
		public QuartersmasterPanel()
		{
			super( CoinmastersDatabase.getQuarterItems(),
			       CoinmastersDatabase.quarterSellPrices(),
			       CoinmastersDatabase.quarterBuyPrices(),
			       WAR_FRAT_OUTFIT,
			       "availableQuarters",
			       "quarter",
			       "quartersmaster",
			       "fratboy" );
		}
	}

	private class BountyHunterHunterPanel
		extends CoinmasterPanel
	{
		public BountyHunterHunterPanel()
		{
			super( CoinmastersDatabase.getLucreItems(),
			       null,
			       CoinmastersDatabase.lucreBuyPrices(),
			       "availableLucre",
			       "lucre",
			       "bounty hunter hunter",
				null );
			buyAction = "buy";
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class BigBrotherPanel
		extends CoinmasterPanel
	{
		private AdventureResult self = null;
		private AdventureResult familiar = null;
		private boolean rescuedBigBrother = false;

		public BigBrotherPanel()
		{
			super( CoinmastersDatabase.getSandDollarItems(),
			       null,
			       CoinmastersDatabase.sandDollarBuyPrices(),
			       "availableSandDollars",
			       "sand dollar",
			       "big brother",
				null );
			buyAction = "buyitem";
		}

		public void update()
		{
			if ( InventoryManager.hasItem( CoinmastersFrame.AERATED_DIVING_HELMET ) )
			{
				this.self = CoinmastersFrame.AERATED_DIVING_HELMET;
				this.rescuedBigBrother = true;
			}
			else if ( InventoryManager.hasItem( CoinmastersFrame.SCUBA_GEAR ) )
			{
				this.self = CoinmastersFrame.SCUBA_GEAR;
				this.rescuedBigBrother = InventoryManager.hasItem( CoinmastersFrame.BUBBLIN_STONE );
			}
			else
			{
				this.rescuedBigBrother = false;
			}

			if ( InventoryManager.hasItem( CoinmastersFrame.DAS_BOOT ) )
			{
				this.familiar = CoinmastersFrame.DAS_BOOT;
			}
			else if ( InventoryManager.hasItem( CoinmastersFrame.BATHYSPHERE ) )
			{
				this.familiar = CoinmastersFrame.BATHYSPHERE;
			}
		}

		public boolean enabled()
		{
			return this.rescuedBigBrother;
		}

		public boolean accessible()
		{
			if ( !this.rescuedBigBrother )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You haven't rescued Big Brother yet." );
				return false;
			}

			if ( this.self == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right equipment to adventure underwater." );
				return false;
			}

			if ( !this.waterBreathingFamiliar() && this.familiar == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your familiar doesn't have the right equipment to adventure underwater." );
				return false;
			}

			return true;
		}

		public void equip()
		{
			if ( !KoLCharacter.hasEquipped( self ) )
			{
				EquipmentRequest request = new EquipmentRequest( self );
				RequestThread.postRequest( request );
			}

			if ( !this.waterBreathingFamiliar() && !KoLCharacter.hasEquipped( familiar ) )
			{
				EquipmentRequest request = new EquipmentRequest( familiar );
				RequestThread.postRequest( request );
			}
		}

		public boolean waterBreathingFamiliar()
		{
			return KoLCharacter.getFamiliar().waterBreathing();
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class CrimboCartelPanel
		extends CoinmasterPanel
	{
		public CrimboCartelPanel()
		{
			super( CoinmastersDatabase.getCrimbuckItems(),
			       null,
			       CoinmastersDatabase.crimbuckBuyPrices(),
			       "availableCrimbux",
			       "Crimbuck",
			       "Crimbo Cartel",
				null );
			buyAction = "buygift";
		}

		public void update()
		{
		}

		public boolean enabled()
		{
			return true;
		}

		public boolean accessible()
		{
			return true;
		}

		public void equip()
		{
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class TicketCounterPanel
		extends CoinmasterPanel
	{
		public TicketCounterPanel()
		{
			super( CoinmastersDatabase.getTicketItems(),
			       null,
			       CoinmastersDatabase.ticketBuyPrices(),
			       "availableTickets",
			       "Game Grid ticket",
			       "Ticket Counter",
				null );
			buyAction = "redeem";
		}

		public void update()
		{
		}

		public boolean enabled()
		{
			return true;
		}

		public boolean accessible()
		{
			return true;
		}

		public void equip()
		{
		}

		public int buyDefault( final int max )
		{
			return 1;
		}
	}

	private class WarMasterPanel
		extends CoinmasterPanel
	{
		private final int outfit;

		private boolean hasOutfit = false;

		public WarMasterPanel( LockableListModel purchases, Map sellPrices, Map buyPrices, int outfit, String property, String token, String master, String side )
		{
			super( purchases, sellPrices, buyPrices, property, token, master, side);
			this.outfit = outfit;
			buyAction = "getgear";
			sellAction = "turnin";
		}

		public void update()
		{
			this.hasOutfit = EquipmentManager.hasOutfit( this.outfit );
		}

		public boolean enabled()
		{
			return CoinmastersFrame.atWar && this.hasOutfit;
		}

		public boolean accessible()
		{
			if ( !CoinmastersFrame.atWar )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're not at war." );
				return false;
			}

			if ( !this.hasOutfit )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have the right outfit" );
				return false;
			}

			return true;
		}

		public void equip()
		{
			if ( !EquipmentManager.isWearingOutfit( this.outfit ) )
			{

				EquipmentManager.retrieveOutfit( this.outfit );
				SpecialOutfit outfit = EquipmentDatabase.getOutfit( this.outfit );
				EquipmentRequest request = new EquipmentRequest( outfit );
				RequestThread.postRequest( request );
			}
		}
	}

	public class CoinmasterPanel
		extends JPanel
	{
		private final LockableListModel purchases;
		private final Map sellPrices;
		private final Map buyPrices;
		private final String property;
		private final String token;
		private final String master;
		private final String side;

		protected String buyAction;
		protected String sellAction;

		private SellPanel sellPanel = null;
		private BuyPanel buyPanel = null;

		public CoinmasterPanel( LockableListModel purchases, Map sellPrices, Map buyPrices, String property, String token, String master, String side )
		{
			super( new BorderLayout() );

			this.purchases = purchases;
			this.sellPrices = sellPrices;
			this.buyPrices = buyPrices;
			this.property = property;
			this.token = token;
			this.master = master;
			this.side = side;

			if ( sellPrices != null )
			{
				sellPanel = new SellPanel();
				this.add( sellPanel, BorderLayout.NORTH );
			}

			if ( buyPrices != null )
			{
				buyPanel = new BuyPanel();
				this.add( buyPanel, BorderLayout.CENTER );
			}
		}

		public void setTitle()
		{
			int count =  Preferences.getInteger( CoinmasterPanel.this.property );
			INSTANCE.setTitle( "Coin Masters (" + count + " " + CoinmasterPanel.this.token + "s)" );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public boolean enabled()
		{
			return true;
		}

		public boolean accessible()
		{
			return true;
		}

		public void equip()
		{
		}

		public String lighthouseSide()
		{
			return this.side;
		}

		public void update()
		{
		}

		public int buyDefault( final int max )
		{
			return max;
		}

		public void check()
		{
			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();
			this.equip();
			RequestThread.postRequest( new CoinMasterRequest( this.token ) );
			RequestThread.closeRequestSequence();
		}

		private void execute( final String action, final Object [] items )
		{
			if ( items.length == 0 )
			{
				return;
			}

			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();

			this.equip();

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult it = (AdventureResult)items[i];
				GenericRequest request = new CoinMasterRequest( token, action, it );
				RequestThread.postRequest( request );
			}

			RequestThread.closeRequestSequence();

			// Update our token count in the title
			CoinmasterPanel.this.setTitle();
		}

		private class SellPanel
			extends ItemManagePanel
		{
			public SellPanel()
			{
				super( KoLConstants.inventory );
				this.setButtons( true, new ActionListener[] {
						new SellListener(),
					} );

				this.elementList.setCellRenderer( getCoinmasterRenderer( sellPrices, token ) );
				this.setEnabled( true );
				this.filterItems();
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( CoinmasterPanel.this.enabled() );
			}

			public void addFilters()
			{
			}

			public AutoFilterTextField getWordFilter()
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
					if ( !InputFieldUtilities.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					Object[] items = SellPanel.this.getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					execute( sellAction, items );
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
					int price = CoinmastersDatabase.getPrice( ar.getName(), CoinmasterPanel.this.sellPrices );
					return ( price > 0 ) && super.isVisible( element );
				}
			}
		}

		private class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel()
			{
				super( purchases );

				this.setButtons( true, new ActionListener[] {
						new BuyListener(),
					} );

				this.eastPanel.add( new InvocationButton( "check", CoinmasterPanel.this, "check" ), BorderLayout.SOUTH );

				this.elementList.setCellRenderer( getCoinmasterRenderer( buyPrices, token, property, CoinmasterPanel.this.lighthouseSide() ) );
				this.elementList.setVisibleRowCount( 6 );
				this.setEnabled( true );
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( CoinmasterPanel.this.enabled() );
			}

			public void addFilters()
			{
			}

			public void addMovers()
			{
			}

			public Object[] getDesiredItems()
			{
				Object[] items = this.elementList.getSelectedValues();
				if ( items.length == 0 )
				{
					return null;
				}

				int neededSize = items.length;
				int originalBalance = Preferences.getInteger( CoinmasterPanel.this.property );
				int balance = originalBalance;

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult item = (AdventureResult) items[ i ];
					String itemName = item.getName();
					int price = CoinmastersDatabase.getPrice( itemName, CoinmasterPanel.this.buyPrices );

					if ( price > originalBalance )
					{
						// This was grayed out.
						items[ i ] = null;
						--neededSize;
						continue;
					}

					int max = balance / price;
					int quantity = max;

					if ( max > 1 )
					{
						int def = CoinmasterPanel.this.buyDefault( max );
						String value = InputFieldUtilities.input( "Buying " + itemName + "...", KoLConstants.COMMA_FORMAT.format( def ) );
						if ( value == null )
						{
							// He hit cancel
							return null;
						}

						StringUtilities.parseInt( value );
					}

					if ( quantity > max )
					{
						quantity = max;
					}

					if ( quantity <= 0 )
					{
						items[ i ] = null;
						--neededSize;
						continue;
					}

					items[ i ] = item.getInstance( quantity );
					balance -= quantity * price;
				}

				// Shrink the array which will be returned so
				// that it removes any nulled values.

				if ( neededSize == 0 )
				{
					return null;
				}

				Object[] desiredItems = new Object[ neededSize ];
				neededSize = 0;

				for ( int i = 0; i < items.length; ++i )
				{
					if ( items[ i ] != null )
					{
						desiredItems[ neededSize++ ] = items[ i ];
					}
				}

				return desiredItems;
			}

			public class BuyListener
				extends ThreadedListener
			{
				public void run()
				{
					Object[] items = BuyPanel.this.getDesiredItems();
					if ( items == null )
					{
						return;
					}

					execute( buyAction, items );
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

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token, String property, String side )
	{
		return new CoinmasterRenderer( prices, token, property, side );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private Map prices;
		private String token;
		private String property;
		private String side;

		public CoinmasterRenderer( final Map prices, final String token )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.property = null;
			this.side = null;
		}

		public CoinmasterRenderer( final Map prices, final String token, String property, String side )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.property = property;
			this.side = side;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
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
			String canonicalName = StringUtilities.getCanonicalName( name );

			if ( this.side != null &&
			     CoinmastersDatabase.lighthouseItems().get( canonicalName ) != null &&
			     !Preferences.getString( "sidequestLighthouseCompleted" ).equals( this.side ) )
			{
				return null;
			}

			Integer iprice = (Integer)prices.get( canonicalName );

			if ( iprice == null )
			{
				return defaultComponent;
			}

			int price = iprice.intValue();
			boolean show = CoinmastersDatabase.availableItem( canonicalName);

			if ( show && property != null )
			{
				int balance = Preferences.getInteger( property );
				if ( price > balance )
				{
					show = false;
				}
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( "<html>" );
			if ( !show )
			{
				stringForm.append( "<font color=gray>" );
			}
			stringForm.append( name );
			stringForm.append( " (" );
			stringForm.append( price );
			stringForm.append( " " );
			stringForm.append( token );
			if ( price > 1 )
				stringForm.append( "s" );
			stringForm.append( ")" );
			int count = ar.getCount();
			if ( count > 0 )
			{
				stringForm.append( " (" );
				stringForm.append( KoLConstants.COMMA_FORMAT.format( count ) );
				stringForm.append( ")" );
			}
			if ( !show )
			{
				stringForm.append( "</font>" );
			}
			stringForm.append( "</html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
