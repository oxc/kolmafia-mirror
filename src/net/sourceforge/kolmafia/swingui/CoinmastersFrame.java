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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.event.ActionListener;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.AppleStoreRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.ArmoryRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.BoutiqueRequest;
import net.sourceforge.kolmafia.request.BrogurtRequest;
import net.sourceforge.kolmafia.request.BuffJimmyRequest;
import net.sourceforge.kolmafia.request.CanteenRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.Crimbo14Request;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.FDKOLRequest;
import net.sourceforge.kolmafia.request.FishboneryRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.NeandermallRequest;
import net.sourceforge.kolmafia.request.NuggletCraftingRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.SHAWARMARequest;
import net.sourceforge.kolmafia.request.ShoeRepairRequest;
import net.sourceforge.kolmafia.request.ShoreGiftShopRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.TacoDanRequest;
import net.sourceforge.kolmafia.request.TerrifiedEagleInnRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.TrapperRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;
import net.sourceforge.kolmafia.request.WarbearBoxRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemListManagePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersFrame
	extends GenericFrame
	implements ChangeListener
{
	private static final StorageRequest PULL_MR_A_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY,
				    new AdventureResult[] { MrStoreRequest.MR_A } );

	private static final List<AdventureResult> conditionalItems = CoinmastersDatabase.getItems( "Conditional" );

	private CardLayoutSelectorPanel selectorPanel = null;

	private CoinmasterPanel dimemasterPanel = null;
	private CoinmasterPanel quartersmasterPanel = null;
	private CoinmasterPanel bhhPanel = null;
	private CoinmasterPanel mrStorePanel = null;
	private CoinmasterPanel hermitPanel = null;
	private CoinmasterPanel bigBrotherPanel = null;
	private CoinmasterPanel arcadePanel = null;
	private CoinmasterPanel gameShoppePanel = null;
	private CoinmasterPanel freeSnackPanel = null;
	private CoinmasterPanel isotopeSmitheryPanel = null;
	private CoinmasterPanel dollhawkerPanel = null;
	private CoinmasterPanel lunarLunchPanel = null;
	private CoinmasterPanel awolPanel = null;
	private CoinmasterPanel fudgeWandPanel = null;
	private CoinmasterPanel travelerPanel = null;
	private CoinmasterPanel neandermallPanel = null;
	private CoinmasterPanel shoeRepairPanel = null;
	private CoinmasterPanel applePanel = null;
	private CoinmasterPanel shoreGiftShopPanel = null;
	private CoinmasterPanel trapperPanel = null;
	private CoinmasterPanel vendingMachinePanel = null;
	private CoinmasterPanel swaggerShopPanel = null;
	private CoinmasterPanel nuggletcraftingPanel = null;
	private CoinmasterPanel terrifiedEagleInnPanel = null;
	private CoinmasterPanel BURTPanel = null;
	private CoinmasterPanel fdkolPanel = null;
	private CoinmasterPanel fishboneryPanel = null;
	private CoinmasterPanel warbearBoxPanel = null;
	private CoinmasterPanel boutiquePanel = null;
	private CoinmasterPanel brogurtPanel = null;
	private CoinmasterPanel buffJimmyPanel = null;
	private CoinmasterPanel tacoDanPanel = null;
	private CoinmasterPanel SHAWARMAPanel = null;
	private CoinmasterPanel canteenPanel = null;
	private CoinmasterPanel armoryPanel = null;

	private CoinmasterPanel altarOfBonesPanel = null;
	private CoinmasterPanel crimboCartelPanel = null;
	private CoinmasterPanel CRIMBCOGiftShopPanel = null;
	private CoinmasterPanel crimbo11Panel = null;
	private CoinmasterPanel crimbo14Panel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );

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

		panel = new JPanel( new BorderLayout() );
		hermitPanel = new HermitPanel();
		panel.add( hermitPanel );
		this.selectorPanel.addPanel( hermitPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		shoreGiftShopPanel = new ShoreGiftShopPanel();
		panel.add( shoreGiftShopPanel );
		this.selectorPanel.addPanel( shoreGiftShopPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		trapperPanel = new TrapperPanel();
		panel.add( trapperPanel );
		this.selectorPanel.addPanel( trapperPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		vendingMachinePanel = new VendingMachinePanel();
		panel.add( vendingMachinePanel );
		this.selectorPanel.addPanel( vendingMachinePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		swaggerShopPanel = new SwaggerShopPanel();
		panel.add( swaggerShopPanel );
		this.selectorPanel.addPanel( swaggerShopPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		nuggletcraftingPanel = new NuggletCraftingPanel();
		panel.add( nuggletcraftingPanel );
		this.selectorPanel.addPanel( nuggletcraftingPanel.getPanelSelector(), panel );

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

		panel = new JPanel( new BorderLayout() );
		BURTPanel = new BURTPanel();
		panel.add( BURTPanel );
		this.selectorPanel.addPanel( BURTPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		fishboneryPanel = new FishboneryPanel();
		panel.add( fishboneryPanel );
		this.selectorPanel.addPanel( fishboneryPanel.getPanelSelector(), panel );

		// Aftercore coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Aftercore" );

		panel = new JPanel( new BorderLayout() );
		bigBrotherPanel = new BigBrotherPanel();
		panel.add( bigBrotherPanel );
		this.selectorPanel.addPanel( bigBrotherPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		terrifiedEagleInnPanel = new TerrifiedEagleInnPanel();
		panel.add( terrifiedEagleInnPanel );
		this.selectorPanel.addPanel( terrifiedEagleInnPanel.getPanelSelector(), panel );

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

		panel = new JPanel( new BorderLayout() );
		boutiquePanel = new BoutiquePanel();
		panel.add( boutiquePanel );
		this.selectorPanel.addPanel( boutiquePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		brogurtPanel = new BrogurtPanel();
		panel.add( brogurtPanel );
		this.selectorPanel.addPanel( brogurtPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		buffJimmyPanel = new BuffJimmyPanel();
		panel.add( buffJimmyPanel );
		this.selectorPanel.addPanel( buffJimmyPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		tacoDanPanel = new TacoDanPanel();
		panel.add( tacoDanPanel );
		this.selectorPanel.addPanel( tacoDanPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		SHAWARMAPanel = new SHAWARMAPanel();
		panel.add( SHAWARMAPanel );
		this.selectorPanel.addPanel( SHAWARMAPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		canteenPanel = new CanteenPanel();
		panel.add( canteenPanel );
		this.selectorPanel.addPanel( canteenPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		armoryPanel = new ArmoryPanel();
		panel.add( armoryPanel );
		this.selectorPanel.addPanel( armoryPanel.getPanelSelector(), panel );

		// Events coinmasters
		this.selectorPanel.addSeparator();
		this.selectorPanel.addCategory( "Special Events" );

		panel = new JPanel( new BorderLayout() );
		awolPanel = new CommendationPanel();
		panel.add( awolPanel );
		this.selectorPanel.addPanel( awolPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		fudgeWandPanel = new FudgeWandPanel();
		panel.add( fudgeWandPanel );
		this.selectorPanel.addPanel( fudgeWandPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		neandermallPanel = new NeandermallPanel();
		panel.add( neandermallPanel );
		this.selectorPanel.addPanel( neandermallPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		shoeRepairPanel = new ShoeRepairPanel();
		panel.add( shoeRepairPanel );
		this.selectorPanel.addPanel( shoeRepairPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		applePanel = new ApplePanel();
		panel.add( applePanel );
		this.selectorPanel.addPanel( applePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		travelerPanel = new TravelingTraderPanel();
		panel.add( travelerPanel );
		this.selectorPanel.addPanel( travelerPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		fdkolPanel = new fdkolPanel();
		panel.add( fdkolPanel );
		this.selectorPanel.addPanel( fdkolPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		warbearBoxPanel = new WarbearBoxPanel();
		panel.add( warbearBoxPanel );
		this.selectorPanel.addPanel( warbearBoxPanel.getPanelSelector(), panel );

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

		panel = new JPanel( new BorderLayout() );
		crimbo11Panel = new Crimbo11Panel();
		panel.add( crimbo11Panel );
		this.selectorPanel.addPanel( crimbo11Panel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		crimbo14Panel = new Crimbo14Panel();
		panel.add( crimbo14Panel );
		this.selectorPanel.addPanel( crimbo14Panel.getPanelSelector(), panel );

		this.selectorPanel.addChangeListener( this );
		this.selectorPanel.setSelectedIndex( Preferences.getInteger( "coinMasterIndex" ) );

		JPanel wrapperPanel = new JPanel( new BorderLayout() );
		wrapperPanel.add( this.selectorPanel, BorderLayout.CENTER );
		wrapperPanel.add( new StatusPanel(), BorderLayout.SOUTH );

		this.setCenterComponent( wrapperPanel );
	}

	private CoinmasterPanel currentPanel()
	{
		JComponent panel = this.selectorPanel.currentPanel();
		Component cm = ( panel instanceof JPanel ) ? ( (JPanel) panel ).getComponent( 0 ) : null;
		return ( cm instanceof CoinmasterPanel ) ? ( (CoinmasterPanel) cm )  : null;
	}

	/**
	 * Whenever the tab changes, this method is used to change the title to count the coins of the new tab
	 */

	public void stateChanged( final ChangeEvent e )
	{
		CoinmasterPanel current = this.currentPanel();
		if ( current != null )
		{
			current.setTitle();
		}
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
		private JButton AToB = new InvocationButton( "1 A -> 10 B", this, "AToB" );
		private JButton BToA = new InvocationButton( "10 B -> 1 A", this, "BToA" );
		private int storageCount = 0;
		private int ACount = 0;
		private int BCount = 0;

		public MrStorePanel()
		{
			super( MrStoreRequest.MR_STORE );
			this.buyPanel.addButton( pull, false );
			this.buyPanel.addButton( AToB, false );
			this.buyPanel.addButton( BToA, false );
			this.storageInTitle = true;
			this.setPullsInTitle();
			this.update();
		}

		@Override
		public final void update()
		{
			this.storageCount = MrStoreRequest.MR_A.getCount( KoLConstants.storage );
			this.ACount = MrStoreRequest.MR_A.getCount( KoLConstants.inventory );
			this.BCount = MrStoreRequest.UNCLE_B.getCount( KoLConstants.inventory );
			boolean canPull =
				KoLCharacter.isHardcore() ||
				ConcoctionDatabase.getPullsRemaining() != 0;
			this.pull.setEnabled( canPull && this.storageCount > 0 );
			this.AToB.setEnabled( this.ACount > 0 );
			this.BToA.setEnabled( this.BCount >= 10 );
			super.update();
		}

		@Override
		public void setTitle( final StringBuffer buffer )
		{
			this.standardTitle( buffer );
			buffer.append( " (" );
			buffer.append( String.valueOf( this.BCount ) );
			buffer.append( " " );
			buffer.append( "Uncle B" );
			if ( this.BCount != 1 )
			{
				buffer.append( "s" );
			}
			buffer.append( ")" );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			this.pull.setEnabled( isEnabled && this.storageCount > 0 );
			this.AToB.setEnabled( isEnabled && this.ACount > 0 );
			this.BToA.setEnabled( isEnabled && this.BCount >= 10 );
		}

		public void pull()
		{
			GenericRequest request = KoLCharacter.isHardcore() ?
				(GenericRequest) new MrStoreRequest( "pullmras" ) :
				(GenericRequest) CoinmastersFrame.PULL_MR_A_REQUEST;
			RequestThread.postRequest( request );
		}

		public void AToB()
		{
			RequestThread.postRequest( new MrStoreRequest( "a_to_b" ) );
		}

		public void BToA()
		{
			RequestThread.postRequest( new MrStoreRequest( "b_to_a" ) );
		}
	}

	public class HermitPanel
		extends CoinmasterPanel
	{
		private JButton fish = new InvocationButton( "go fish", this, "fish" );

		public HermitPanel()
		{
			super( HermitRequest.HERMIT );
			this.buyPanel.addButton( fish, true );
		}

		public void fish()
		{
			int available = HermitRequest.getWorthlessItemCount();
			AdventureResult item = HermitRequest.WORTHLESS_ITEM.getInstance( available + 1 );
			InventoryManager.retrieveItem( item, false );
		}
	}

	public class TrapperPanel
		extends CoinmasterPanel
	{
		public TrapperPanel()
		{
			super( TrapperRequest.TRAPPER );
		}
	}

	public class SwaggerShopPanel
		extends CoinmasterPanel
	{
		public SwaggerShopPanel()
		{
			super( SwaggerShopRequest.SWAGGER_SHOP );
			PreferenceListenerRegistry.registerPreferenceListener( "blackBartsBootyAvailable", this );
			PreferenceListenerRegistry.registerPreferenceListener( "holidayHalsBookAvailable", this );
			PreferenceListenerRegistry.registerPreferenceListener( "antagonisticSnowmanKitAvailable", this );
		}

		@Override
		public final void update()
		{
			// Update title if tokens changed
			super.update();
			// Remove item if no longer available
			this.buyPanel.filterItems();
		}

		@Override
		public boolean canBuy( AdventureResult item )
		{
			int itemId = item.getItemId();
			switch ( itemId )
			{
			case ItemPool.BLACK_BARTS_BOOTY:
				return Preferences.getBoolean( "blackBartsBootyAvailable" );
			case ItemPool.HOLIDAY_FUN_BOOK:
				return Preferences.getBoolean( "holidayHalsBookAvailable" );
			case ItemPool.ANTAGONISTIC_SNOWMAN_KIT:
				return Preferences.getBoolean( "antagonisticSnowmanKitAvailable" );
			}
			return true;
		}
	}

	public class BURTPanel
		extends CoinmasterPanel
	{
		public BURTPanel()
		{
			super( BURTRequest.BURT );
		}
	}

	public class FishboneryPanel
		extends CoinmasterPanel
	{
		public FishboneryPanel()
		{
			super( FishboneryRequest.FISHBONERY );
		}
	}

	public class NuggletCraftingPanel
		extends CoinmasterPanel
	{
		public NuggletCraftingPanel()
		{
			super( NuggletCraftingRequest.NUGGLETCRAFTING );
		}
	}

	public class NeandermallPanel
		extends CoinmasterPanel
	{
		public NeandermallPanel()
		{
			super( NeandermallRequest.NEANDERMALL );
		}
	}

	public class ShoeRepairPanel
		extends CoinmasterPanel
	{
		public ShoeRepairPanel()
		{
			super( ShoeRepairRequest.SHOE_REPAIR );
		}
	}

	public class ApplePanel
		extends CoinmasterPanel
	{
		public ApplePanel()
		{
			super( AppleStoreRequest.APPLE_STORE );
		}
	}

	public class ShoreGiftShopPanel
		extends CoinmasterPanel
	{
		public ShoreGiftShopPanel()
		{
			super( ShoreGiftShopRequest.SHORE_GIFT_SHOP );
		}
	}

	public class VendingMachinePanel
		extends CoinmasterPanel
	{
		public VendingMachinePanel()
		{
			super( VendingMachineRequest.VENDING_MACHINE );
		}
	}

	private class BigBrotherPanel
		extends CoinmasterPanel
	{
		public BigBrotherPanel()
		{
			super( BigBrotherRequest.BIG_BROTHER );
		}

		@Override
		public boolean canBuy( AdventureResult item )
		{
			int itemId = item.getItemId();
			switch ( itemId )
			{
			case ItemPool.MADNESS_REEF_MAP:
			case ItemPool.MARINARA_TRENCH_MAP:
			case ItemPool.ANEMONE_MINE_MAP:
			case ItemPool.DIVE_BAR_MAP:
			case ItemPool.SKATE_PARK_MAP:
				return !ItemDatabase.haveVirtualItem( itemId );
			case ItemPool.DAMP_OLD_BOOT:
				return !Preferences.getBoolean( "dampOldBootPurchased" );
			case ItemPool.BLACK_GLASS:
				return BigBrotherRequest.BLACK_GLASS.getCount( KoLConstants.inventory ) == 0;
			case ItemPool.FOLDER_19:
				return KoLCharacter.hasEquipped( GearChangeFrame.FOLDER_HOLDER );
			}
			return true;
		}
	}

	private class Crimbo11Panel
		extends CoinmasterPanel
	{
		public Crimbo11Panel()
		{
			super();

			this.setData( Crimbo11Request.CRIMBO11 );

			this.sellPanel = new SellPanel();
			this.add( this.sellPanel, BorderLayout.NORTH );

			ActionListener[] listeners = new ActionListener[ 2 ];
			listeners[ 0 ] = new GiftListener();
			listeners[ 1 ] = new DonateListener();

			this.buyPanel = new BuyPanel( listeners );
			this.add( this.buyPanel, BorderLayout.CENTER );
		}

		public AdventureResult[] getDesiredItems()
		{
			Object[] items = this.buyPanel.getSelectedValues();
			return this.getDesiredBuyItems( items, false );
		}

		public class GiftListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				CoinmasterData data = Crimbo11Panel.this.data;
				String reason = data.canBuy();
				if ( reason != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, reason );
					return;
				}

				AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
				if ( items == null )
				{
					return;
				}

				String victim = InputFieldUtilities.input( "Send a gift to whom?" );
				if ( victim == null )
				{
					return;
				}

				Crimbo11Panel.this.execute( true, items, "towho=" + victim );
			}

			@Override
			public String toString()
			{
				return "gift";
			}
		}

		public class DonateListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				CoinmasterData data = Crimbo11Panel.this.data;
				String reason = data.canBuy();
				if ( reason != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, reason );
					return;
				}

				AdventureResult[] items = Crimbo11Panel.this.getDesiredItems();
				if ( items == null )
				{
					return;
				}

				Crimbo11Panel.this.execute( true, items, "towho=0" );
			}

			@Override
			public String toString()
			{
				return "donate";
			}
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

	private class Crimbo14Panel
		extends CoinmasterPanel
	{
		public Crimbo14Panel()
		{
			super();

			this.setData( Crimbo14Request.CRIMBO14 );

			this.sellPanel = new SellPanel();
			this.add( this.sellPanel, BorderLayout.NORTH );
			this.buyPanel = new BuyPanel();
			this.add( this.buyPanel, BorderLayout.CENTER );
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

		@Override
		public final void update()
		{
			this.gameGridTokens = ArcadeRequest.TOKEN.getCount( KoLConstants.inventory );
			this.skeeball.setEnabled( this.gameGridTokens > 0 );
			this.buyPanel.filterItems();
			// Update title if tokens changed
			super.update();
		}

		@Override
		public boolean canBuy( AdventureResult item )
		{
			if ( !CoinmastersFrame.conditionalItems.contains( item ) )
			{
				return true;
			}

			int itemId = item.getItemId();
			if ( itemId == ItemPool.FOLDER_14 )
			{
				// Jackass Plumber folder
				return KoLCharacter.hasEquipped( GearChangeFrame.FOLDER_HOLDER );
			}
			return !Preferences.getBoolean( "lockedItem" + itemId );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			this.skeeball.setEnabled( isEnabled && this.gameGridTokens > 0 );
		}

		@Override
		public void setTitle( final StringBuffer buffer )
		{
			this.standardTitle( buffer );
			buffer.append( " (" );
			buffer.append( String.valueOf( this.gameGridTokens ) );
			buffer.append( " Game Grid tokens)" );
		}

		public void skeeball()
		{
			RequestThread.postRequest( new ArcadeRequest( "arcade_skeeball" ) );
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

	private class FudgeWandPanel
		extends CoinmasterPanel
	{
		public FudgeWandPanel()
		{
			super( FudgeWandRequest.FUDGEWAND );
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

	private class fdkolPanel
		extends CoinmasterPanel
	{
		public fdkolPanel()
		{
			super( FDKOLRequest.FDKOL );
		}
	}

	private class IsotopeSmitheryPanel
		extends CoinmasterPanel
	{
		public IsotopeSmitheryPanel()
		{
			super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY );
		}

		@Override
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

		@Override
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

		@Override
		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private class BrogurtPanel
		extends CoinmasterPanel
	{
		public BrogurtPanel()
		{
			super( BrogurtRequest.BROGURT );
		}
	}

	private class BuffJimmyPanel
		extends CoinmasterPanel
	{
		public BuffJimmyPanel()
		{
			super( BuffJimmyRequest.BUFF_JIMMY );
		}
	}

	private class TacoDanPanel
		extends CoinmasterPanel
	{
		public TacoDanPanel()
		{
			super( TacoDanRequest.TACO_DAN );
		}
	}

	private class SHAWARMAPanel
		extends CoinmasterPanel
	{
		public SHAWARMAPanel()
		{
			super( SHAWARMARequest.SHAWARMA );
		}
	}

	private class CanteenPanel
		extends CoinmasterPanel
	{
		public CanteenPanel()
		{
			super( CanteenRequest.CANTEEN );
		}
	}

	private class ArmoryPanel
		extends CoinmasterPanel
	{
		public ArmoryPanel()
		{
			super( ArmoryRequest.ARMORY );
		}
	}

	private class TerrifiedEagleInnPanel
		extends CoinmasterPanel
	{
		public TerrifiedEagleInnPanel()
		{
			super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN );
		}
	}

	private class WarbearBoxPanel
		extends CoinmasterPanel
	{
		public WarbearBoxPanel()
		{
			super( WarbearBoxRequest.WARBEARBOX );
		}
	}

	private class BoutiquePanel
		extends CoinmasterPanel
	{
		public BoutiquePanel()
		{
			super( BoutiqueRequest.BOUTIQUE );
		}
	}

	private abstract class WarMasterPanel
		extends CoinmasterPanel
	{
		String side;

		public WarMasterPanel( CoinmasterData data, String side )
		{
			super( data );
			this.side = side;
			this.buyPanel.filterItems();
			NamedListenerRegistry.registerNamedListener( "(outfit)", this );
			PreferenceListenerRegistry.registerPreferenceListener( "warProgress", this );
			PreferenceListenerRegistry.registerPreferenceListener( "sidequestLighthouseCompleted", this );
		}

		@Override
		public void update()
		{
			// Update title if tokens changed
			super.update();
			// Add items if lighthouse now completed
			this.buyPanel.filterItems();
		}

		@Override
		public boolean canBuy( AdventureResult item )
		{
			return  !CoinmastersFrame.conditionalItems.contains( item ) ||
				Preferences.getString( "sidequestLighthouseCompleted" ).equals( this.side );
		}

		@Override
		public int buyDefault( final int max )
		{
			return max;
		}
	}

	public abstract class CoinmasterPanel
		extends JPanel
		implements Listener
	{
		protected CoinmasterData data;
		protected boolean storageInTitle = false;
		protected boolean pullsInTitle = false;

		protected SellPanel sellPanel = null;
		protected BuyPanel buyPanel = null;

		public CoinmasterPanel()
		{
			super( new BorderLayout() );
			NamedListenerRegistry.registerNamedListener( "(coinmaster)", this );
		}

		protected void setData( final CoinmasterData data )
		{
			this.data = data;

			String property = data.getProperty();
			if ( property != null )
			{
				PreferenceListenerRegistry.registerPreferenceListener( property, this );
			}
		}

		protected void setPullsInTitle()
		{
			this.pullsInTitle = true;
			NamedListenerRegistry.registerNamedListener( "(pullsremaining)", this );
		}
		
		public CoinmasterPanel( final CoinmasterData data )
		{
			this();

			this.setData( data );

			if ( data.getSellPrices() != null )
			{
				this.sellPanel = new SellPanel();
				this.add( sellPanel, BorderLayout.NORTH );
			}

			if ( data.getBuyPrices() != null )
			{
				this.buyPanel = new BuyPanel();
				this.add( buyPanel, BorderLayout.CENTER );
			}

			this.storageInTitle = this.data.getStorageAction() != null;
		}

		public void update()
		{
			// (coinmaster) is fired when tokens change
			this.setTitle();
		}

		public CoinMasterRequest getRequest()
		{
			return this.data.getRequest();
		}

		public CoinMasterRequest getRequest( final boolean buying, final AdventureResult [] items )
		{
			return this.data.getRequest( buying, items );
		}

		public final void setTitle()
		{
			if ( this == CoinmastersFrame.this.currentPanel() )
			{
				StringBuffer buffer = new StringBuffer();
				this.setTitle( buffer );
				CoinmastersFrame.this.setTitle( buffer.toString() );
			}
		}

		public void setTitle( final StringBuffer buffer )
		{
			this.standardTitle( buffer );
		}

		public final void standardTitle( final StringBuffer buffer )
		{
			AdventureResult item = this.data.getItem();
			int count = this.data.availableTokens();
			String name = ( count != 1 ) ? this.data.getPluralToken() : this.data.getToken();
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

		public String getPanelSelector()
		{
			return "- " + this.data.getMaster();
		}

		public boolean enabled()
		{
			return this.data.isAccessible();
		}

		public int buyDefault( final int max )
		{
			return 1;
		}

		public void check()
		{
			RequestThread.postRequest( this.getRequest() );
			if ( this.buyPanel != null )
			{
				this.buyPanel.filterItems();
			}
		}

		protected void execute( final boolean buying, final AdventureResult [] items )
		{
			this.execute( buying, items, null );
		}

		protected void execute( final boolean buying, final AdventureResult [] items, final String extraAction )
		{
			if ( items.length == 0 )
			{
				return;
			}

			CoinMasterRequest request = this.getRequest( buying, items );
			if ( extraAction != null )
			{
				request.addFormField( extraAction );
			}

			RequestThread.postRequest( request );

			if ( this.buyPanel != null )
			{
				this.buyPanel.filterItems();
			}
		}

		public AdventureResult[] getDesiredBuyItems( Object[] items, final boolean fromStorage )
		{
			if ( items.length == 0 )
			{
				return null;
			}

			CoinmasterData data = this.data;
			Map<String,Integer> originalBalances = new TreeMap<String,Integer>();
			Map<String,Integer> balances = new TreeMap<String,Integer>();
			int neededSize = items.length;

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				String itemName = item.getName();
				String canonicalName = StringUtilities.getCanonicalName( itemName );

				if ( !CoinmastersDatabase.availableItem( canonicalName ) )
				{
					// This was shown but was grayed out.
					items[ i ] = null;
					--neededSize;
					continue;
				}

				AdventureResult cost = data.itemBuyPrice( itemName );
				String currency = cost.getName();
				int price = cost.getCount();

				Integer value = originalBalances.get( currency );
				if ( value == null )
				{
					int newValue = 
						fromStorage ?
						data.availableStorageTokens( cost ) :
						data.availableTokens( cost );
					value = new Integer( newValue );
					originalBalances.put( currency, value );
					balances.put( currency, value );
				}

				int originalBalance = value.intValue();
				int balance = balances.get( currency ).intValue();

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
					String val = InputFieldUtilities.input( "Buying " + itemName + "...", KoLConstants.COMMA_FORMAT.format( def ) );
					if ( val == null )
					{
						// He hit cancel
						return null;
					}

					quantity = StringUtilities.parseInt( val );
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
				balances.put( currency, new Integer( balance ) );
			}

			// Shrink the array which will be returned so
			// that it removes any nulled values.

			if ( neededSize == 0 )
			{
				return null;
			}

			AdventureResult[] desiredItems = new AdventureResult[ neededSize ];
			neededSize = 0;

			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[ i ] != null )
				{
					desiredItems[ neededSize++ ] = (AdventureResult)items[ i ];
				}
			}

			return desiredItems;
		}

		public boolean canBuy( AdventureResult item )
		{
			return true;
		}

		public class SellPanel
			extends ItemListManagePanel
		{
			public SellPanel()
			{
				super( KoLConstants.inventory );
				this.setButtons( true, new ActionListener[] {
						new SellListener(),
					} );

				this.getElementList().setCellRenderer( getCoinmasterRenderer( CoinmasterPanel.this.data, false ) );
				this.setEnabled( true );
				this.filterItems();
			}

			@Override
			public final void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				this.buttons[ 0 ].setEnabled( CoinmasterPanel.this.enabled() );
			}

			@Override
			public void addFilters()
			{
			}

			@Override
			public void addMovers()
			{
				if ( CoinmasterPanel.this.addSellMovers() )
				{
					super.addMovers();
				}
			}

			@Override
			public AutoFilterTextField getWordFilter()
			{
				return new SellableFilterField();
			}

			@Override
			public void actionConfirmed()
			{
			}

			@Override
			public void actionCancelled()
			{
			}

			public class SellListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					CoinmasterData data = CoinmasterPanel.this.data;
					String reason = data.canSell();
					if ( reason != null )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, reason );
						return;
					}

					if ( !InputFieldUtilities.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					AdventureResult[] items = SellPanel.this.getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( false, items );
				}

				@Override
				public String toString()
				{
					return "sell";
				}
			}

			private class SellableFilterField
				extends FilterItemField
			{
				@Override
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

		public class BuyPanel
			extends ItemListManagePanel
		{
			public BuyPanel( ActionListener[] listeners )
			{
				super( CoinmasterPanel.this.data.getBuyItems() );

				if ( listeners != null )
				{
					this.setButtons( true, listeners );
				}

				this.eastPanel.add( new InvocationButton( "visit", CoinmasterPanel.this, "check" ), BorderLayout.SOUTH );

				this.getElementList().setCellRenderer( getCoinmasterRenderer( CoinmasterPanel.this.data, true ) );
				this.getElementList().setVisibleRowCount( 6 );
				this.setEnabled( true );
				this.filterItems();
			}

			public BuyPanel()
			{
				this( null );

				boolean storage = CoinmasterPanel.this.data.getStorageAction() != null;
				int count = storage ? 2 : 1;
				ActionListener[] listeners = new ActionListener[ count ];
				listeners[ 0 ] = new BuyListener();
				if ( count > 1 )
				{
					listeners[ 1 ] = new BuyUsingStorageListener();
				}

				this.setButtons( true, listeners );
				this.setEnabled( true );
				this.filterItems();
			}

			public void addButton( final JButton button, final boolean save )
			{
				JButton[] buttons = new JButton[1 ];
				buttons[ 0 ] = button;
				this.addButtons( buttons, save );
			}

			@Override
			public void addButtons( final JButton[] buttons, final boolean save )
			{
				super.addButtons( buttons, save );
			}

			@Override
			public final void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				for ( int i = 0; this.buttons != null && i < this.buttons.length; ++i )
				{
					this.buttons[ i ].setEnabled( CoinmasterPanel.this.enabled() );
				}
			}

			@Override
			public void addFilters()
			{
			}

			@Override
			public void addMovers()
			{
			}

			@Override
			public AutoFilterTextField getWordFilter()
			{
				return new BuyableFilterField();
			}

			public AdventureResult[] getDesiredItems( final boolean fromStorage )
			{
				Object[] items = this.getSelectedValues();
				return CoinmasterPanel.this.getDesiredBuyItems( items, fromStorage );
			}

			public class BuyListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					CoinmasterData data = CoinmasterPanel.this.data;
					String reason = data.canBuy();
					if ( reason != null )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, reason );
						return;
					}

					AdventureResult[] items = BuyPanel.this.getDesiredItems( false );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( true, items );
				}

				@Override
				public String toString()
				{
					return "buy";
				}
			}

			public class BuyUsingStorageListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					AdventureResult[] items = BuyPanel.this.getDesiredItems( true );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( true,
						 items,
						 CoinmasterPanel.this.data.getStorageAction() );
				}

				@Override
				public String toString()
				{
					return "from storage";
				}
			}

			private class BuyableFilterField
				extends FilterItemField
			{
				@Override
				public boolean isVisible( final Object element )
				{
					if ( !( element instanceof AdventureResult ) )
					{
						return false;
					}
					AdventureResult ar = (AdventureResult)element;
					return  CoinmasterPanel.this.canBuy( ar ) &&
						super.isVisible( element );
				}
			}
		}
	}

	public static final DefaultListCellRenderer getCoinmasterRenderer( CoinmasterData data, final boolean buying )
	{
		return new CoinmasterRenderer( data, buying );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private final CoinmasterData data;
		private final boolean buying;

		public CoinmasterRenderer( CoinmasterData data, final boolean buying )
		{
			this.setOpaque( true );
			this.data = data;
			this.buying = buying;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		@Override
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
			AdventureResult cost = this.buying ? this.data.itemBuyPrice( name ) : this.data.itemSellPrice( name );

			if ( cost == null )
			{
				return defaultComponent;
			}

			boolean show = CoinmastersDatabase.availableItem( canonicalName );

			int price = cost.getCount();

			if ( show && this.buying)
			{
				int balance1 = this.data.availableTokens( cost );
				int balance2 = this.data.availableStorageTokens( cost );
				if ( price > balance1 && price > balance2 )
				{
					show = false;
				}
			}

			StringBuilder stringForm = new StringBuilder();
			stringForm.append( "<html>" );
			if ( !show )
			{
				stringForm.append( "<font color=gray>" );
			}
			stringForm.append( name );
			stringForm.append( " (" );
			stringForm.append( price );
			stringForm.append( " " );
			stringForm.append( cost.getPluralName( price ) );
			stringForm.append( ")" );
			int count = ar.getCount();
			if ( count == -1 )
			{
				stringForm.append( " (unknown)" );
			}
			else if ( count != PurchaseRequest.MAX_QUANTITY )
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
