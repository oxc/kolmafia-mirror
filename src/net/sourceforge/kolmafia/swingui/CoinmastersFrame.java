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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;

import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersFrame
	extends GenericFrame
	implements ChangeListener
{
	public static final AdventureResult GG_TOKEN = ItemPool.get( ItemPool.GG_TOKEN, 1 );

	private static final StorageRequest PULL_MR_A_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY,
				    new AdventureResult[] { MrStoreRequest.MR_A } );

	private static CoinmastersFrame INSTANCE = null;

	// Token counts
	private static int dimes = 0;
	private static int quarters = 0;
	private static int storeCredits = 0;

	private CardLayoutSelectorPanel selectorPanel = null;

	private CoinmasterPanel dimemasterPanel = null;
	private CoinmasterPanel quartersmasterPanel = null;
	private CoinmasterPanel bhhPanel = null;
	private CoinmasterPanel mrStorePanel = null;
	private CoinmasterPanel bigBrotherPanel = null;
	private CoinmasterPanel arcadePanel = null;
	private CoinmasterPanel gameShoppePanel = null;
	private CoinmasterPanel freeSnackPanel = null;
	private CoinmasterPanel isotopeSmitheryPanel = null;
	private CoinmasterPanel dollhawkerPanel = null;
	private CoinmasterPanel lunarLunchPanel = null;
	private CoinmasterPanel awolPanel = null;
	private CoinmasterPanel travelerPanel = null;

	private CoinmasterPanel altarOfBonesPanel = null;
	private CoinmasterPanel crimboCartelPanel = null;
	private CoinmasterPanel CRIMBCOGiftShopPanel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );
		CoinmastersFrame.INSTANCE = this;

		this.selectorPanel = new CardLayoutSelectorPanel( "coinMasterIndex", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
		JPanel panel;

		// Always available coinmasters
		this.selectorPanel.addCategory( "Always Available" );

		panel = new JPanel( new BorderLayout() );
		bhhPanel = new BountyHunterHunterPanel();
		panel.add( bhhPanel );
		this.selectorPanel.addPanel( bhhPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		mrStorePanel = new MrStorePanel();
		panel.add( mrStorePanel );
		this.selectorPanel.addPanel( mrStorePanel.getPanelSelector(), panel );

		// Ascension coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Ascension" );

		panel = new JPanel( new BorderLayout() );
		dimemasterPanel = new DimemasterPanel();
		panel.add( dimemasterPanel );
		this.selectorPanel.addPanel( dimemasterPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		quartersmasterPanel = new QuartersmasterPanel();
		panel.add( quartersmasterPanel );
		this.selectorPanel.addPanel( quartersmasterPanel.getPanelSelector(), panel );

		// Aftercore coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Aftercore" );

		panel = new JPanel( new BorderLayout() );
		bigBrotherPanel = new BigBrotherPanel();
		panel.add( bigBrotherPanel );
		this.selectorPanel.addPanel( bigBrotherPanel.getPanelSelector(), panel );

		// IOTM coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Item of the Month" );

		panel = new JPanel( new BorderLayout() );
		arcadePanel = new TicketCounterPanel();
		panel.add( arcadePanel );
		this.selectorPanel.addPanel( arcadePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		gameShoppePanel = new GameShoppePanel();
		panel.add( gameShoppePanel );
		this.selectorPanel.addPanel( gameShoppePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		freeSnackPanel = new SnackVoucherPanel();
		panel.add( freeSnackPanel );
		this.selectorPanel.addPanel( freeSnackPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		isotopeSmitheryPanel = new IsotopeSmitheryPanel();
		panel.add( isotopeSmitheryPanel );
		this.selectorPanel.addPanel( isotopeSmitheryPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		dollhawkerPanel = new DollHawkerPanel();
		panel.add( dollhawkerPanel );
		this.selectorPanel.addPanel( dollhawkerPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		lunarLunchPanel = new LunarLunchPanel();
		panel.add( lunarLunchPanel );
		this.selectorPanel.addPanel( lunarLunchPanel.getPanelSelector(), panel );

		// Events coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Special Events" );

		panel = new JPanel( new BorderLayout() );
		awolPanel = new CommendationPanel();
		panel.add( awolPanel );
		this.selectorPanel.addPanel( awolPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		travelerPanel = new TravelingTraderPanel();
		panel.add( travelerPanel );
		this.selectorPanel.addPanel( travelerPanel.getPanelSelector(), panel );

		// Removed coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Removed" );

		panel = new JPanel( new BorderLayout() );
		altarOfBonesPanel = new AltarOfBonesPanel();
		panel.add( altarOfBonesPanel );
		this.selectorPanel.addPanel( altarOfBonesPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		crimboCartelPanel = new CrimboCartelPanel();
		panel.add( crimboCartelPanel );
		this.selectorPanel.addPanel( crimboCartelPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		CRIMBCOGiftShopPanel = new CRIMBCOGiftShopPanel();
		panel.add( CRIMBCOGiftShopPanel );
		this.selectorPanel.addPanel( CRIMBCOGiftShopPanel.getPanelSelector(), panel );

		this.selectorPanel.addChangeListener( this );
		this.selectorPanel.setSelectedIndex( Preferences.getInteger( "coinMasterIndex" ) );

		this.framePanel.add( this.selectorPanel, BorderLayout.CENTER );

		this.add( new StatusPanel(), BorderLayout.SOUTH );

		CoinmastersFrame.externalUpdate();
	}

	public static void initializePurchaseRequests()
	{
		AltarOfBonesRequest.ALTAR_OF_BONES.registerPurchaseRequests();
		AWOLQuartermasterRequest.AWOL.registerPurchaseRequests();
		BigBrotherRequest.BIG_BROTHER.registerPurchaseRequests();
		BountyHunterHunterRequest.BHH.registerPurchaseRequests();
		CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP.registerPurchaseRequests();
		CrimboCartelRequest.CRIMBO_CARTEL.registerPurchaseRequests();
		DimemasterRequest.HIPPY.registerPurchaseRequests();
		DollHawkerRequest.DOLLHAWKER.registerPurchaseRequests();
		FreeSnackRequest.FREESNACKS.registerPurchaseRequests();
		GameShoppeRequest.GAMESHOPPE.registerPurchaseRequests();
		IsotopeSmitheryRequest.ISOTOPE_SMITHERY.registerPurchaseRequests();
		LunarLunchRequest.LUNAR_LUNCH.registerPurchaseRequests();
		MrStoreRequest.MR_STORE.registerPurchaseRequests();
		QuartersmasterRequest.FRATBOY.registerPurchaseRequests();
		TicketCounterRequest.TICKET_COUNTER.registerPurchaseRequests();
		TravelingTraderRequest.TRAVELER.registerPurchaseRequests();
	}

	/**
	 * Whenever the tab changes, this method is used to change the title to
	 * count the coins of the new tab
	 */

	public void stateChanged( final ChangeEvent e )
	{
		this.setTitle();
	}

	private void setTitle()
	{
		JPanel panel = (JPanel) this.selectorPanel.currentPanel();
		Component cm = ( panel instanceof JPanel ) ? panel.getComponent( 0 ) : null;
		if (cm instanceof CoinmasterPanel )
		{
			((CoinmasterPanel) cm).setTitle();
		}
	}

	public static void externalUpdate()
	{
		if ( INSTANCE == null )
		{
			return;
		}

		dimes = Preferences.getInteger( "availableDimes" );
		quarters = Preferences.getInteger( "availableQuarters" );
		storeCredits = Preferences.getInteger( "availableStoreCredits" );

		INSTANCE.update();
	}

	private void update()
	{
		dimemasterPanel.update();
		quartersmasterPanel.update();
		bhhPanel.update();
		mrStorePanel.update();
		bigBrotherPanel.update();
		arcadePanel.update();
		gameShoppePanel.update();
		freeSnackPanel.update();
		isotopeSmitheryPanel.update();
		dollhawkerPanel.update();
		lunarLunchPanel.update();
		awolPanel.update();
		travelerPanel.update();
		altarOfBonesPanel.update();
		crimboCartelPanel.update();
		CRIMBCOGiftShopPanel.update();
		this.setTitle();
	}

	private class DimemasterPanel
		extends WarMasterPanel
	{
		public DimemasterPanel()
		{
			super( DimemasterRequest.HIPPY, "hippy");
		}
	}

	private class QuartersmasterPanel
		extends WarMasterPanel
	{
		public QuartersmasterPanel()
		{
			super( QuartersmasterRequest.FRATBOY, "fratboy" );
		}
	}

	private class BountyHunterHunterPanel
		extends CoinmasterPanel
	{
		public BountyHunterHunterPanel()
		{
			super( BountyHunterHunterRequest.BHH );
		}
	}

	public class MrStorePanel
		extends CoinmasterPanel
	{
		private JButton pull = new InvocationButton( "pull Mr. A", this, "pull" );
		private int storageCount = 0;

		public MrStorePanel()
		{
			super( MrStoreRequest.MR_STORE );
			this.buyPanel.addButton( pull, false );
			this.storageInTitle = true;
			this.pullsInTitle = true;
			this.update();
		}

		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			this.pull.setEnabled( isEnabled && this.storageCount > 0 );
		}

		public void update()
		{
			this.storageCount = MrStoreRequest.MR_A.getCount( KoLConstants.storage );
			boolean canPull =
				KoLCharacter.isHardcore() ||
				ConcoctionDatabase.getPullsRemaining() != 0;
			this.pull.setEnabled( canPull && this.storageCount > 0 );
		}

		public void pull()
		{
			GenericRequest request = KoLCharacter.isHardcore() ?
				(GenericRequest) new MrStoreRequest( "pullmras" ) :
				(GenericRequest) CoinmastersFrame.PULL_MR_A_REQUEST;
			RequestThread.postRequest( request );
		}
	}

	private class BigBrotherPanel
		extends CoinmasterPanel
	{
		public BigBrotherPanel()
		{
			super( BigBrotherRequest.BIG_BROTHER );
		}
	}

	private class CrimboCartelPanel
		extends CoinmasterPanel
	{
		public CrimboCartelPanel()
		{
			super( CrimboCartelRequest.CRIMBO_CARTEL );
		}
	}

	public class TicketCounterPanel
		extends CoinmasterPanel
	{
		private JButton skeeball = new InvocationButton( "skeeball", this, "skeeball" );
		private int gameGridTokens = 0;

		public TicketCounterPanel()
		{
			super( TicketCounterRequest.TICKET_COUNTER );
			this.buyPanel.addButton( skeeball, false );
			this.update();
		}

		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			this.skeeball.setEnabled( isEnabled && this.gameGridTokens > 0 );
		}

		public void update()
		{
			this.gameGridTokens = GG_TOKEN.getCount( KoLConstants.inventory );
			this.skeeball.setEnabled( this.gameGridTokens > 0 );
		}

		public void skeeball()
		{
			RequestThread.postRequest( new ArcadeRequest( "skeeball" ) );
		}
	}

	private class GameShoppePanel
		extends CoinmasterPanel
	{
		public GameShoppePanel()
		{
			super( GameShoppeRequest.GAMESHOPPE );
		}
	}

	private class SnackVoucherPanel
		extends CoinmasterPanel
	{
		public SnackVoucherPanel()
		{
			super( FreeSnackRequest.FREESNACKS );
		}
	}

	private class AltarOfBonesPanel
		extends CoinmasterPanel
	{
		public AltarOfBonesPanel()
		{
			super( AltarOfBonesRequest.ALTAR_OF_BONES );
		}
	}

	private class CRIMBCOGiftShopPanel
		extends CoinmasterPanel
	{
		public CRIMBCOGiftShopPanel()
		{
			super( CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP );
		}
	}

	private class CommendationPanel
		extends CoinmasterPanel
	{
		public CommendationPanel()
		{
			super( AWOLQuartermasterRequest.AWOL );
		}
	}

	private class TravelingTraderPanel
		extends CoinmasterPanel
	{
		public TravelingTraderPanel()
		{
			super( TravelingTraderRequest.TRAVELER );
		}
	}

	private abstract class WarMasterPanel
		extends CoinmasterPanel
	{
		private final String side;

		public WarMasterPanel( CoinmasterData data, String side )
		{
			super( data );
			this.side = side;
		}

		public int buyDefault( final int max )
		{
			return max;
		}

		public String lighthouseSide()
		{
			return this.side;
		}
	}

	private class IsotopeSmitheryPanel
		extends CoinmasterPanel
	{
		public IsotopeSmitheryPanel()
		{
			super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY );
		}

		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private class DollHawkerPanel
		extends CoinmasterPanel
	{
		public DollHawkerPanel()
		{
			super( DollHawkerRequest.DOLLHAWKER );
		}

		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private class LunarLunchPanel
		extends CoinmasterPanel
	{
		public LunarLunchPanel()
		{
			super( LunarLunchRequest.LUNAR_LUNCH );
		}

		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	public abstract class CoinmasterPanel
		extends JPanel
	{
		protected final CoinmasterData data;
		protected boolean storageInTitle = false;
		protected boolean pullsInTitle = false;

		protected SellPanel sellPanel = null;
		protected BuyPanel buyPanel = null;

		public CoinmasterPanel( final CoinmasterData data )
		{
			super( new BorderLayout() );

			this.data = data;

			if ( data.getSellPrices() != null )
			{
				sellPanel = new SellPanel();
				this.add( sellPanel, BorderLayout.NORTH );
			}

			if ( data.getBuyPrices() != null )
			{
				buyPanel = new BuyPanel();
				this.add( buyPanel, BorderLayout.CENTER );
			}

			this.storageInTitle = this.data.getStorageAction() != null;
		}

		public CoinMasterRequest getRequest()
		{
			return CoinMasterRequest.getRequest( this.data );
		}

		public CoinMasterRequest getRequest( final String action, final AdventureResult it )
		{
			return CoinMasterRequest.getRequest( this.data, action, it );
		}

		public void setTitle()
		{
			StringBuffer buffer = new StringBuffer();
			AdventureResult item = this.data.getItem();
			String property = this.data.getProperty();
			int count =
				item != null ? item.getCount( KoLConstants.inventory ) :
				property != null ? Preferences.getInteger( property ) :
				0;
			String token = item != null ? item.getName() : this.data.getToken();
			String name = ( count != 1 ) ? ItemDatabase.getPluralName( token ) : token;
			buffer.append( "Coin Masters (" );
			buffer.append( String.valueOf( count ) );
			buffer.append( " " );
			buffer.append( name );

			// Makes no sense to show storage except for real items
			if ( storageInTitle && item != null )
			{
				int count1 = item.getCount( KoLConstants.storage );
				buffer.append( ", " );
				buffer.append( String.valueOf( count1 ) );
				buffer.append( " in storage" );

				// Only show pulls if we actually have the item in storage
				if ( pullsInTitle && count1 > 0 && !KoLCharacter.isHardcore() )
				{
					int pulls = ConcoctionDatabase.getPullsRemaining();
					buffer.append( ", " );
					buffer.append( KoLCharacter.inRonin() ? String.valueOf( pulls ) : "unlimited" );
					buffer.append( " pull" );
					buffer.append( pulls != 1 ? "s" : "" );
					buffer.append( " available" );
				}
			}

			buffer.append( ")" );
			INSTANCE.setTitle( buffer.toString() );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public boolean addSellMovers()
		{
			return true;
		}

		public String getMaster()
		{
			return this.data.getMaster();
		}

		public String getPanelSelector()
		{
			return "- " + this.data.getMaster();
		}

		public boolean enabled()
		{
			String message = CoinMasterRequest.accessible( this.data );
			return message == null;
		}

		public boolean accessible()
		{
			String message = CoinMasterRequest.accessible( this.data );
			if ( message != null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
			}
			return message == null;
		}

		public void equip()
		{
			CoinMasterRequest.equip( this.data );
		}

		public String lighthouseSide()
		{
			return null;
		}

		public void update()
		{
		}

		public int buyDefault( final int max )
		{
			return 1;
		}

		public void check()
		{
			this.update();

			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();
			this.equip();
			RequestThread.postRequest( this.getRequest() );
			RequestThread.closeRequestSequence();
		}

		private void execute( final String action, final Object [] items )
		{
			this.execute( action, items, null );
		}

		private void execute( final String action, final Object [] items, final String extraAction )
		{
			if ( items.length == 0 )
			{
				return;
			}

			this.update();

			if ( !this.accessible() )
			{
				return;
			}

			RequestThread.openRequestSequence();

			this.equip();

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult it = (AdventureResult)items[i];
				CoinMasterRequest request = this.getRequest( action, it );
				if ( extraAction != null )
				{
					request.addFormField( extraAction );
				}
				RequestThread.postRequest( request );
			}

			RequestThread.closeRequestSequence();

			// Update our token count in the title
			this.setTitle();
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

				Map sellPrices = CoinmasterPanel.this.data.getSellPrices();
				String token = CoinmasterPanel.this.data.getToken();
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

			public void addMovers()
			{
				if ( CoinmasterPanel.this.addSellMovers() )
				{
					super.addMovers();
				}
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

					execute( CoinmasterPanel.this.data.getSellAction(), items );
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
					int price = CoinmastersDatabase.getPrice( ar.getName(), CoinmasterPanel.this.data.getSellPrices() );
					return ( price > 0 ) && super.isVisible( element );
				}
			}
		}

		private class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel()
			{
				super( CoinmasterPanel.this.data.getBuyItems() );

				boolean storage = CoinmasterPanel.this.data.getStorageAction() != null;
				int count = storage ? 2 : 1;
				ActionListener[] listeners = new ActionListener[ count ];
				listeners[ 0 ] = new BuyListener();
				if ( storage )
				{
					listeners[ 1 ] = new BuyUsingStorageListener();
				}

				this.setButtons( true, listeners );

				this.eastPanel.add( new InvocationButton( "visit", CoinmasterPanel.this, "check" ), BorderLayout.SOUTH );

				Map buyPrices = CoinmasterPanel.this.data.getBuyPrices();

				String token = CoinmasterPanel.this.data.getToken();
				AdventureResult item = CoinmasterPanel.this.data.getItem();
				String property = CoinmasterPanel.this.data.getProperty();
				String side = CoinmasterPanel.this.lighthouseSide();
				this.elementList.setCellRenderer( getCoinmasterRenderer( buyPrices, token, item, property, side ) );
				this.elementList.setVisibleRowCount( 6 );
				this.setEnabled( true );
			}

			public void addButton( final JButton button, final boolean save )
			{
				JButton[] buttons = new JButton[1 ];
				buttons[ 0 ] = button;
				this.addButtons( buttons, save );
			}

			public void addButtons( final JButton[] buttons, final boolean save )
			{
				super.addButtons( buttons, save );
			}

			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				for ( int i = 0; i < this.buttons.length; ++i )
				{
					this.buttons[ i ].setEnabled( CoinmasterPanel.this.enabled() );
				}
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

				AdventureResult token = CoinmasterPanel.this.data.getItem();
				String property = CoinmasterPanel.this.data.getProperty();
				int originalBalance =
					token != null ? token.getCount( KoLConstants.inventory ) :
					property != null ? Preferences.getInteger( property ) :
					0;

				int neededSize = items.length;
				int balance = originalBalance;
				Map buyPrices = CoinmasterPanel.this.data.getBuyPrices();

				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult item = (AdventureResult) items[ i ];
					String itemName = item.getName();
					int price = CoinmastersDatabase.getPrice( itemName, buyPrices );

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

						quantity = StringUtilities.parseInt( value );
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

					execute( CoinmasterPanel.this.data.getBuyAction(), items );
				}

				public String toString()
				{
					return "buy";
				}
			}

			public class BuyUsingStorageListener
				extends ThreadedListener
			{
				public void run()
				{
					Object[] items = BuyPanel.this.getDesiredItems();
					if ( items == null )
					{
						return;
					}

					execute( CoinmasterPanel.this.data.getBuyAction(),
						 items,
						 CoinmasterPanel.this.data.getStorageAction() );
				}

				public String toString()
				{
					return "from storage";
				}
			}
		}
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token )
	{
		return new CoinmasterRenderer( prices, token );
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( Map prices, String token, AdventureResult item, String property, String side )
	{
		return new CoinmasterRenderer( prices, token, item, property, side );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private Map prices;
		private String token;
		private AdventureResult item;
		private String property;
		private String side;

		public CoinmasterRenderer( final Map prices, final String token )
		{
			this( prices, token, null, null, null );
		}

		public CoinmasterRenderer( final Map prices, final String token, final AdventureResult item, String property, String side )
		{
			this.setOpaque( true );
			this.prices = prices;
			this.token = token;
			this.item = item;
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

			if ( show )
			{
				int balance =
					item != null ? item.getCount( KoLConstants.inventory ) :
					property != null ? Preferences.getInteger( property ) :
					0;
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
			stringForm.append( price != 1 ?
					   ItemDatabase.getPluralName( token ) :
					   token );
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
