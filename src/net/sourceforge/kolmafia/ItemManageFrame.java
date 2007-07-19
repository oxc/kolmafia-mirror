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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.StoreManager.SoldItem;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class ItemManageFrame extends KoLFrame
{
	private static int pullsRemaining = 0;

	private static JTabbedPane fullnessTabs;
	private static JTabbedPane inebrietyTabs;
	private static JLabel pullsRemainingLabel1 = new JLabel( " " );
	private static JLabel pullsRemainingLabel2 = new JLabel( " " );

	private static final Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

	private LockableListModel itemPanelNames = new LockableListModel();
	private JList itemPanelList = new JList( this.itemPanelNames );
	private CardLayout itemPanelCards = new CardLayout();
	private JPanel managePanel = new JPanel( this.itemPanelCards );

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public ItemManageFrame()
	{	this( true );
	}

	public ItemManageFrame( boolean useTabs )
	{
		super( "Item Manager" );

		this.addPanel( "Usable", new UsableItemPanel() );

		JPanel foodPanel = new JPanel( new BorderLayout() );

		if ( StaticEntity.getBooleanProperty( "addCreationQueue" ) )
			foodPanel.add( new ConsumePanel( true, false ), BorderLayout.NORTH );
		foodPanel.add( new QueuePanel( true, false ), BorderLayout.CENTER );

		this.addPanel( " - Food", foodPanel );

		JPanel boozePanel = new JPanel( new BorderLayout() );

		if ( StaticEntity.getBooleanProperty( "addCreationQueue" ) )
			boozePanel.add( new ConsumePanel( false, true ), BorderLayout.NORTH );
		boozePanel.add( new QueuePanel( false, true ), BorderLayout.CENTER );

		this.addPanel( " - Booze", boozePanel );
		this.addPanel( " - Restores", new RestorativeItemPanel() );

		this.addSeparator();

		this.addPanel( "General", new InventoryManagePanel( inventory, false ) );
		this.addPanel( " - Recent", new InventoryManagePanel( tally, false ) );
		this.addPanel( " - Closet", new InventoryManagePanel( closet, false ) );
		this.addPanel( " - Storage", new HagnkStoragePanel( false ) );

		this.addSeparator();

		this.addPanel( "Creatable", new CreateItemPanel( true, true, true, true ) );

		this.addPanel( " - Cookable", new CreateItemPanel( true, false, false, false ) );
		this.addPanel( " - Mixable", new CreateItemPanel( false, true, false, false ) );

		this.addSeparator();

		this.addPanel( "Equipment", new InventoryManagePanel( inventory, true ) );
		this.addPanel( " - Create", new CreateItemPanel( false, false, true, false ) );
		this.addPanel( " - Storage", new HagnkStoragePanel( true ) );

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		this.itemPanelList.addListSelectionListener( new CardSwitchListener() );
		this.itemPanelList.setPrototypeCellValue( "ABCDEFGHIJKLM" );
		this.itemPanelList.setCellRenderer( new OptionRenderer() );

		JPanel listHolder = new JPanel( new CardLayout( 10, 10 ) );
		listHolder.add( new SimpleScrollPane( this.itemPanelList ), "" );

		JPanel mainPanel = new JPanel( new BorderLayout() );

		mainPanel.add( listHolder, BorderLayout.WEST );
		mainPanel.add( this.managePanel, BorderLayout.CENTER );

		this.tabs.addTab( "Handle Items", mainPanel );
		this.addTab( "Update Filters", new FlaggedItemsPanel() );
		this.addTab( "Scripted Actions", new CommonActionsPanel() );

		JPanel tabHolder = new JPanel( new CardLayout( 10, 10 ) );
		tabHolder.add( this.tabs, "" );

		this.itemPanelList.setSelectedIndex( StaticEntity.getIntegerProperty( "itemManagerIndex" ) );
		this.framePanel.add( tabHolder, BorderLayout.CENTER );
	}

	public static int getPullsRemaining()
	{	return pullsRemaining;
	}

	public static void setPullsRemaining( int pullsRemaining )
	{
		ItemManageFrame.pullsRemaining = pullsRemaining;

		if ( KoLCharacter.isHardcore() )
		{
			pullsRemainingLabel1.setText( "In Hardcore" );
			pullsRemainingLabel2.setText( "In Hardcore" );
			return;
		}

		switch ( pullsRemaining )
		{
		case 0:
				pullsRemainingLabel1.setText( "No Pulls Left" );
				pullsRemainingLabel2.setText( "No Pulls Left" );
			break;
		case 1:
				pullsRemainingLabel1.setText( "1 Pull Left" );
				pullsRemainingLabel2.setText( "1 Pull Left" );
			break;
		default:
				pullsRemainingLabel1.setText( pullsRemaining + " Pulls Left" );
				pullsRemainingLabel2.setText( pullsRemaining + " Pulls Left" );
		}
	}

	private void addPanel( String name, JComponent panel )
	{
		this.itemPanelNames.add( name );
		this.managePanel.add( panel, String.valueOf( this.itemPanelNames.size() ) );
	}

	private void addSeparator()
	{
		JPanel separator = new JPanel();
		separator.setOpaque( false );
		separator.setLayout( new BoxLayout( separator, BoxLayout.Y_AXIS ) );

		separator.add( Box.createVerticalGlue() );
		separator.add( new JSeparator() );
		this.itemPanelNames.add( separator );
	}

	private class CardSwitchListener implements ListSelectionListener
	{
		public void valueChanged( ListSelectionEvent e )
		{
			int cardIndex = ItemManageFrame.this.itemPanelList.getSelectedIndex();

			if ( ItemManageFrame.this.itemPanelNames.get( cardIndex ) instanceof JComponent )
				return;

			StaticEntity.setProperty( "itemManagerIndex", String.valueOf( cardIndex ) );
			ItemManageFrame.this.itemPanelCards.show( ItemManageFrame.this.managePanel, String.valueOf( cardIndex + 1 ) );
		}
	}

	private class FlaggedItemsPanel extends JPanel
	{
		private JPanel container;

		public FlaggedItemsPanel()
		{
			this.container = new JPanel();
			this.container.setLayout( new BoxLayout( this.container, BoxLayout.Y_AXIS ) );

			// Memento list.

			JLabel description = new JLabel( "<html>The following items are flagged as \"mementos\".  IF YOU SET A PREFERENCE, KoLmafia will never autosell these items, place them in the mall, or pulverize them, even if they are flagged as junk.  Furthermore, any item which cannot be autosold in game will be avoided by the end of run sale script and need not be added here to take effect.  The only way to bypass this restriction is to use the relay browser, which does not use this list.</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			this.container.add( description );
			this.container.add( Box.createVerticalStrut( 10 ) );

			ItemManagePanel scroller = new ItemManagePanel( mementoList );
			scroller.setMaximumSize( MAX_WIDTH );
			scroller.setAlignmentX( LEFT_ALIGNMENT );
			this.container.add( scroller );

			this.container.add( Box.createVerticalStrut( 30 ) );

			// Junk item list.

			description = new JLabel( "<html>The following items are the items in your inventory which are flagged as \"junk\".  On many areas of KoLmafia's interface, these items will be flagged with a gray color.  In addition, there is a junk item script available in the scripts tab of this item manager which sells all of these items at once.</html>" );

			description.setMaximumSize( MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			this.container.add( description );
			this.container.add( Box.createVerticalStrut( 10 ) );

			scroller = new ItemManagePanel( junkItemList );
			scroller.setMaximumSize( MAX_WIDTH );
			scroller.setAlignmentX( LEFT_ALIGNMENT );
			this.container.add( scroller );

			this.setLayout( new CardLayout( 10, 10 ) );
			this.add( this.container, "" );
		}
	}

	private class JunkDetailsLabel extends JLabel implements ListDataListener
	{
		public void intervalRemoved( ListDataEvent e )
		{	this.updateText();
		}

		public void intervalAdded( ListDataEvent e )
		{	this.updateText();
		}

		public void contentsChanged( ListDataEvent e )
		{	this.updateText();
		}

		public void updateText()
		{
			int totalValue = 0;

			AdventureResult currentItem;
			Object [] items = junkItemList.toArray();

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];
				totalValue += currentItem.getCount( inventory ) * TradeableItemDatabase.getPriceById( currentItem.getItemId() );
			}

			this.setText( "<html>Gnollish toolboxes, briefcases, small and large boxes, 31337 scrolls, Warm Subject gift certificates, Penultimate Fantasy chests, and black pension checks, if flagged as junk, will be used. " +
				"If you have the Pulverize and a tenderizing hammer, then items will be pulverized if you have malus access or they are weapons, armor, or pants with power greater than or equal to 100. " +
				"All other items flagged as junk will be autosold.  The current autosell value of items to be handled in this script is " + COMMA_FORMAT.format( totalValue ) + " meat.</html>" );
		}
	}

	private class JunkOnlyFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			if ( element instanceof AdventureResult )
			{
				if ( junkItemList.contains( element ) )
					return true;
			}
			else if ( element instanceof ItemCreationRequest )
			{
				if ( junkItemList.contains( ((ItemCreationRequest) element).createdItem ) )
					return true;
			}

			return false;

		}
	}

	private class ExcludeMementoFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			AdventureResult data = null;

			if ( element instanceof AdventureResult )
				data = (AdventureResult) element;
			else if ( element instanceof ItemCreationRequest )
				data = ((ItemCreationRequest) element).createdItem;

			if ( data == null )
				return false;

			return !mementoList.contains( data ) && TradeableItemDatabase.getPriceById( data.getItemId() ) > 0;
		}
	}

	private class CommonActionsPanel extends JPanel
	{
		private JPanel container;
		private JunkDetailsLabel label;
		private Dimension MAX_WIDTH = new Dimension( 500, Integer.MAX_VALUE );

		public CommonActionsPanel()
		{
			this.container = new JPanel();
			this.container.setLayout( new BoxLayout( this.container, BoxLayout.Y_AXIS ) );

			this.addButtonLabelList( new JunkItemsButton(), "", new ShowDescriptionList( inventory, junkItemList, new JunkOnlyFilter() ) );
			this.label.updateText();

			inventory.addListDataListener( this.label );
			junkItemList.addListDataListener( this.label );

			this.container.add( new JSeparator() );
			this.container.add( Box.createVerticalStrut( 15 ) );

			this.addButtonLabelList( new EndOfRunSaleButton(),
				"All items flagged as junk will be \"junked\" (see above script for more information).  KoLmafia will then place all items which are not already in your store at 999,999,999 meat, except for items flagged as \"mementos\" (see Filters tab for more details). " + StoreManageFrame.UNDERCUT_MESSAGE,
				new ShowDescriptionList( inventory, mementoList, new ExcludeMementoFilter() ) );

			this.container.add( new JSeparator() );
			this.container.add( Box.createVerticalStrut( 15 ) );

			this.addButtonAndLabel( new MallRestockButton(),
				"This feature looks at all the items currently in your store, and if you have any matching items in your inventory that are also auto-sellable, drops those items into your store at your current price.  Note that if any items are already sold out, these items will not be re-added, even if you've run this script previously on this character, as KoLmafia does not currently remember past decisions related to store management." );

			this.container.add( new JSeparator() );
			this.container.add( Box.createVerticalStrut( 15 ) );

			this.addButtonAndLabel( new DisplayCaseButton(),
				"This feature scans your inventory and, if it finds any items which match what's in your display case, and if you have more than one of that item in your display case, puts those items on display.  If there are items which you would rather not have extras of on display, then before running this script, auto-sell these items, pulverize these items, place these items in your closet, or place these items in your clan's stash, and KoLmafia will not add those items to your display case.  Alternatively, you may run one of the other scripts listed above, which may remove the item from your inventory." );

			this.setLayout( new CardLayout( 10, 10 ) );
			this.add( this.container, "" );
		}

		private void addButtonAndLabel( ThreadedButton button, String label )
		{	this.addButtonLabelList( button, label, null );
		}

		private void addButtonLabelList( ThreadedButton button, String label, ShowDescriptionList list )
		{
			JPanel buttonPanel = new JPanel();
			buttonPanel.add( button );
			buttonPanel.setAlignmentX( LEFT_ALIGNMENT );
			buttonPanel.setMaximumSize( this.MAX_WIDTH );

			this.container.add( buttonPanel );
			this.container.add( Box.createVerticalStrut( 5 ) );

			if ( list != null )
			{
				SimpleScrollPane scroller = new SimpleScrollPane( list );
				scroller.setMaximumSize( this.MAX_WIDTH );
				scroller.setAlignmentX( LEFT_ALIGNMENT );

				this.container.add( scroller );
				this.container.add( Box.createVerticalStrut( 15 ) );
			}

			JLabel description = button instanceof JunkItemsButton ? new JunkDetailsLabel() : new JLabel( "<html>" + label + "</html>" );

			description.setMaximumSize( this.MAX_WIDTH );
			description.setVerticalAlignment( JLabel.TOP );
			description.setAlignmentX( LEFT_ALIGNMENT );
			this.container.add( description );
			this.container.add( Box.createVerticalStrut( 10 ) );

			if ( button instanceof JunkItemsButton )
				this.label = (JunkDetailsLabel) description;

			this.container.add( Box.createVerticalStrut( 25 ) );
		}

		private class JunkItemsButton extends ThreadedButton
		{
			public JunkItemsButton()
			{	super( "junk item script" );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				StaticEntity.getClient().makeJunkRemovalRequest();
			}
		}

		private class EndOfRunSaleButton extends ThreadedButton
		{
			public EndOfRunSaleButton()
			{
				super( "end of run sale" );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				StaticEntity.getClient().makeEndOfRunSaleRequest();
			}
		}

		private class MallRestockButton extends ThreadedButton
		{
			public MallRestockButton()
			{
				super( "mall store restocker" );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				RequestThread.postRequest( new StoreManageRequest() );

				SoldItem [] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
				StoreManager.getSoldItemList().toArray( sold );

				int itemCount;
				AdventureResult item;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < sold.length; ++i )
				{
					item = new AdventureResult( sold[i].getItemId(), 1 );
					itemCount = item.getCount( inventory );

					if ( itemCount > 0 && TradeableItemDatabase.getPriceById( item.getItemId() ) > 0 )
						items.add( item.getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
				{
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}

				RequestThread.postRequest( new AutoSellRequest( items.toArray(), AutoSellRequest.AUTOMALL ) );
			}
		}

		private class DisplayCaseButton extends ThreadedButton
		{
			public DisplayCaseButton()
			{
				super( "display case matcher" );
			}

			public void run()
			{
				KoLmafia.updateDisplay( "Gathering data..." );
				RequestThread.postRequest( new MuseumRequest() );

				AdventureResult [] display = new AdventureResult[ collection.size() ];
				collection.toArray( display );

				int itemCount;
				ArrayList items = new ArrayList();

				for ( int i = 0; i < display.length; ++i )
				{
					itemCount = display[i].getCount( inventory );
					if ( itemCount > 1 )
						items.add( display[i].getInstance( itemCount ) );
				}

				if ( items.isEmpty() )
				{
					RequestThread.enableDisplayIfSequenceComplete();
					return;
				}

				RequestThread.postRequest( new MuseumRequest( items.toArray(), true ) );
			}
		}
	}

	private static final AdventureResult MAGNESIUM = new AdventureResult( "milk of magnesium", 1, false );

	private class ConsumePanel extends ItemManagePanel
	{
		private boolean food, booze;

		public ConsumePanel( boolean food, boolean booze )
		{
			super( "", "consume", "create", ConcoctionsDatabase.getUsables(), false, false );

			this.food = food;
			this.booze = booze;

			JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			this.elementList.setCellRenderer( AdventureResult.getCreationQueueRenderer() );
			this.elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

			this.elementList.setVisibleRowCount( 3 );
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			JTabbedPane queueTabs = getTabbedPane();

			if ( this.food )
			{
				fullnessTabs = queueTabs;
				queueTabs.addTab( "0 Full Queued", centerPanel );
			}
			else
			{
				inebrietyTabs = queueTabs;
				queueTabs.addTab( "0 Drunk Queued", centerPanel );
			}

			queueTabs.addTab( "Ingredients Used", new SimpleScrollPane( ConcoctionsDatabase.getQueue(), 7 ) );
			actualPanel.add( queueTabs, BorderLayout.CENTER );

			this.eastPanel.add( new UndoQueueButton(), BorderLayout.SOUTH );

			this.setEnabled( true );
			this.filterItems();
		}

		public FilterItemField getWordFilter()
		{	return new ConsumableFilterField();
		}

		public void actionConfirmed()
		{	ConcoctionsDatabase.handleQueue( true );
		}

		public void actionCancelled()
		{	ConcoctionsDatabase.handleQueue( false );
		}

		private class UndoQueueButton extends ThreadedButton
		{
			public UndoQueueButton()
			{	super( "undo" );
			}

			public void run()
			{
				ConcoctionsDatabase.pop();
				ConcoctionsDatabase.refreshConcoctions();

				if ( fullnessTabs != null )
					fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
				if ( inebrietyTabs != null )
					inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
		}

		private class ConsumableFilterField extends FilterItemField
		{
			public ConsumableFilterField()
			{	this.filter = new ConsumableFilter();
			}

			public void filterItems()
			{	ConsumePanel.this.elementList.applyFilter( this.filter );
			}

			private class ConsumableFilter extends SimpleListFilter
			{
				public ConsumableFilter()
				{	super( ConsumableFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					Concoction creation = (Concoction) element;

					if ( creation.getQueued() == 0 )
						return false;

					int fullness = TradeableItemDatabase.getFullness( creation.getName() );
					int inebriety = TradeableItemDatabase.getInebriety( creation.getName() );

					if ( fullness > 0 )
						return ConsumePanel.this.food && super.isVisible( element );
					else if ( inebriety > 0 )
						return ConsumePanel.this.booze && super.isVisible( element );
					else
						return false;
				}
			}
		}
	}

	private class QueuePanel extends ItemManagePanel
	{
		private boolean food, booze;
		private JCheckBox [] filters;

		public QueuePanel( boolean food, boolean booze )
		{
			super( ConcoctionsDatabase.getUsables(), true, true );

			this.food = food;
			this.booze = booze;

			if ( StaticEntity.getBooleanProperty( "addCreationQueue" ) )
				this.setButtons( false, new ActionListener [] { new EnqueueListener(), new ExecuteListener(), new BuffUpListener() } );
			else
				this.setButtons( false, new ActionListener [] { new ExecuteListener(), new BuffUpListener() } );

			JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

			this.elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

			this.elementList.setVisibleRowCount( 6 );
			this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.filters = new JCheckBox[ food || booze ? 5 : 4 ];

			this.filters[0] = new JCheckBox( "no create" );
			this.filters[1] = new JCheckBox( "+mus only" );
			this.filters[2] = new JCheckBox( "+mys only" );
			this.filters[3] = new JCheckBox( "+mox only" );

			for ( int i = 0; i < 4; ++i )
				this.listenToCheckBox( this.filters[i] );

			if ( food || booze )
				this.filters[4] = new ExperimentalCheckbox( food, booze );

			JPanel filterPanel = new JPanel();
			for ( int i = 0; i < this.filters.length; ++i )
				filterPanel.add( this.filters[i] );

			this.setEnabled( true );
			this.northPanel.add( filterPanel, BorderLayout.NORTH );

			this.filterItems();
		}

		public FilterItemField getWordFilter()
		{	return new ConsumableFilterField();
		}

		protected void listenToCheckBox( JCheckBox box )
		{
			super.listenToCheckBox( box );
			box.addActionListener( new ReSortListener() );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		private class ReSortListener extends ThreadedListener
		{
			public void run()
			{	ConcoctionsDatabase.getUsables().sort();
			}
		}

		private class EnqueueListener extends ThreadedListener
		{
			public void run()
			{
				getDesiredItems( "Queue" );
				ConcoctionsDatabase.refreshConcoctions();

				if ( fullnessTabs != null )
					fullnessTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedFullness() + " Full Queued" );
				if ( inebrietyTabs != null )
					inebrietyTabs.setTitleAt( 0, ConcoctionsDatabase.getQueuedInebriety() + " Drunk Queued" );
			}

			public String toString()
			{	return "enqueue";
			}
		}

		private class ExecuteListener extends ThreadedListener
		{
			public void run()
			{
				getDesiredItems( "Consume" );
				ConcoctionsDatabase.refreshConcoctions();
				ConcoctionsDatabase.handleQueue( true );
			}

			public String toString()
			{	return "consume";
			}
		}

		private class BuffUpListener extends ThreadedListener
		{
			public void run()
			{
				if ( QueuePanel.this.food )
				{
					RequestThread.postRequest( new ConsumeItemRequest( MAGNESIUM ) );
				}
				else
				{
					if ( !activeEffects.contains( new AdventureResult( "Ode to Booze", 1, true ) ) )
						RequestThread.postRequest( UseSkillRequest.getInstance( "The Ode to Booze", 1 ) );
				}
			}

			public String toString()
			{	return QueuePanel.this.food ? "use milk" : "cast ode";
			}
		}

		private class ConsumableFilterField extends FilterItemField
		{
			public ConsumableFilterField()
			{	this.filter = new ConsumableFilter();
			}

			public void filterItems()
			{	QueuePanel.this.elementList.applyFilter( this.filter );
			}

			private class ConsumableFilter extends SimpleListFilter
			{
				public ConsumableFilter()
				{	super( ConsumableFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					Concoction creation = (Concoction) element;

					int fullness = TradeableItemDatabase.getFullness( creation.getName() );
					int inebriety = TradeableItemDatabase.getInebriety( creation.getName() );

					if ( fullness > 0 )
					{
						if ( !QueuePanel.this.food )
							return false;
					}
					else if ( inebriety > 0 )
					{
						if ( !QueuePanel.this.booze )
							return false;
					}
					else
						return false;

					if ( creation.getTotal() == 0 )
						return false;

					if ( QueuePanel.this.filters[0].isSelected() )
					{
						AdventureResult item = creation.getItem();
						if ( item != null && item.getCount( inventory ) == 0 )
							return false;
					}

					if ( QueuePanel.this.filters[1].isSelected() )
					{
						String range = TradeableItemDatabase.getMuscleRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					if ( QueuePanel.this.filters[2].isSelected() )
					{
						String range = TradeableItemDatabase.getMysticalityRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					if ( QueuePanel.this.filters[3].isSelected() )
					{
						String range = TradeableItemDatabase.getMoxieRange( creation.getName() );
						if ( range.equals( "+0.0" ) || range.startsWith( "-" ) )
							return false;
					}

					return super.isVisible( element );
				}
			}
		}
	}

	private class ExperimentalCheckbox extends JCheckBox implements ActionListener
	{
		public ExperimentalCheckbox( boolean food, boolean booze )
		{
			super( food && booze ? "per full/drunk" : booze ? "per drunk" : "per full" );

			this.setToolTipText( "Sort gains per adventure" );
			this.setSelected( StaticEntity.getBooleanProperty( "showGainsPerUnit" ) );

			this.addActionListener( this );
			KoLSettings.registerCheckbox( "showGainsPerUnit", this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( StaticEntity.getBooleanProperty( "showGainsPerUnit" ) == this.isSelected() )
				return;

			StaticEntity.setProperty( "showGainsPerUnit", String.valueOf( this.isSelected() ) );
			ConcoctionsDatabase.getUsables().sort();
		}
	}


	private class CreationSettingCheckBox extends JCheckBox implements ActionListener
	{
		private String property;

		public CreationSettingCheckBox( String label, String property, String tooltip )
		{
			super( label );

			this.setToolTipText( tooltip );
			this.setSelected( StaticEntity.getBooleanProperty( property ) );

			this.addActionListener( this );

			this.property = property;
			KoLSettings.registerCheckbox( property, this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( StaticEntity.getBooleanProperty( this.property ) == this.isSelected() )
				return;

			StaticEntity.setProperty( this.property, String.valueOf( this.isSelected() ) );
			ConcoctionsDatabase.refreshConcoctions();
		}
	}

	protected class UsableItemPanel extends InventoryManagePanel
	{
		public UsableItemPanel()
		{
			super( inventory, false );
			this.filterItems();
		}

		public FilterItemField getWordFilter()
		{	return new UsableItemFilterField();
		}

		public void actionConfirmed()
		{
			Object [] items = this.getDesiredItems( "Consume" );
			if ( items.length == 0 )
				return;

			for ( int i = 0; i < items.length; ++i )
				RequestThread.postRequest( new ConsumeItemRequest( (AdventureResult) items[i] ) );
		}

		public void actionCancelled()
		{
			String name;
			Object [] values = this.elementList.getSelectedValues();

			for ( int i = 0; i < values.length; ++i )
			{
				name = ((AdventureResult)values[i]).getName();
				if ( name != null )
					StaticEntity.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name );
			}
		}

		private class UsableItemFilterField extends FilterItemField
		{
			public SimpleListFilter getFilter()
			{	return new UsableItemFilter();
			}

			private class UsableItemFilter extends SimpleListFilter
			{
				public UsableItemFilter()
				{	super( UsableItemFilterField.this );
				}

				public boolean isVisible( Object element )
				{
					AdventureResult item = (AdventureResult)element;
					int itemId = item.getItemId();

					if ( !UsableItemFilterField.this.notrade && !TradeableItemDatabase.isTradeable( itemId ) )
					     return false;

					boolean filter = false;

					switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
					{
					case CONSUME_EAT:
						filter = UsableItemFilterField.this.food;
						break;

					case CONSUME_DRINK:
						filter = UsableItemFilterField.this.booze;
						break;

					case CONSUME_USE:
					case CONSUME_MULTIPLE:
					case GROW_FAMILIAR:
					case CONSUME_ZAP:
					case MP_RESTORE:
					case HP_RESTORE:
						filter = UsableItemFilterField.this.other;
						break;

					case EQUIP_FAMILIAR:
					case EQUIP_ACCESSORY:
					case EQUIP_HAT:
					case EQUIP_PANTS:
					case EQUIP_SHIRT:
					case EQUIP_WEAPON:
					case EQUIP_OFFHAND:
						filter = UsableItemFilterField.this.equip;
						break;

					default:
						return false;
					}

					return filter && super.isVisible( element );
				}
			}
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * creating items; this allows creating of items,
	 * which usually get resold in malls.
	 */

	private class CreateItemPanel extends InventoryManagePanel
	{
		public CreateItemPanel( boolean food, boolean booze, boolean equip, boolean other )
		{
			super( "create item", "create & use", ConcoctionsDatabase.getCreatables(), equip && !other );

			if ( this.isEquipmentOnly )
			{
				super.addFilters( false );
			}
			else
			{
				this.setFixedFilter( food, booze, equip, other, true );
				JPanel filterPanel = new JPanel();

				JCheckBox allowNoBox = new CreationSettingCheckBox( "Require in-a-boxes for creation", "requireBoxServants", "Require in-a-boxes, auto-repair on explosion" );
				filterPanel.add( allowNoBox );

				this.northPanel.add( filterPanel, BorderLayout.SOUTH );
			}

			ConcoctionsDatabase.getCreatables().applyListFilters();
			this.filterItems();
		}

		public void addFilters( boolean isCompact )
		{
		}

		public void actionConfirmed()
		{
			Object selected = this.elementList.getSelectedValue();

			if ( selected == null )
				return;

			ItemCreationRequest selection = (ItemCreationRequest) selected;
			int quantityDesired = getQuantity( "Creating multiple " + selection.getName() + "...", selection.getQuantityPossible() );
			if ( quantityDesired < 1 )
				return;

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			selection.setQuantityNeeded( quantityDesired );

			RequestThread.openRequestSequence();

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			Object selected = this.elementList.getSelectedValue();

			if ( selected == null )
				return;

			ItemCreationRequest selection = (ItemCreationRequest) selected;

			int maximum = ConsumeItemRequest.maximumUses( selection.getItemId() );
			int quantityDesired = maximum < 2 ? maximum : getQuantity( "Creating multiple " + selection.getName() + "...",
				Math.min( maximum, selection.getQuantityPossible() ) );

			if ( quantityDesired < 1 )
				return;

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			selection.setQuantityNeeded( quantityDesired );

			RequestThread.openRequestSequence();

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( selection.getItemId(), selection.getQuantityNeeded() ) ) );
			RequestThread.closeRequestSequence();
		}
	}

	private static class OptionRenderer extends DefaultListCellRenderer
	{
		public OptionRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			return value instanceof JComponent ? (Component) value :
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
		}
	}

	public class InventoryManagePanel extends ItemManagePanel
	{
		protected boolean isEquipmentOnly;
		private FilterRadioButton [] equipmentFilters;

		public InventoryManagePanel( LockableListModel elementModel, boolean isEquipmentOnly )
		{
			super( elementModel );
			this.isEquipmentOnly = isEquipmentOnly;

			boolean isCloset = (elementModel == closet);

			this.setButtons( true, new ActionListener [] {

				new ConsumeListener(),
				new PutInClosetListener( isCloset ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOSELL ),
				new AutoSellListener( isCloset, AutoSellRequest.AUTOMALL ),
				new PulverizeListener( isCloset ),
				new PutOnDisplayListener( isCloset ),
				new GiveToClanListener( isCloset )

			} );

			if ( this.isEquipmentOnly )
			{
				elementList.setCellRenderer( AdventureResult.getEquipmentRenderer() );
			}
			else
			{
				elementList.setCellRenderer( AdventureResult.getDefaultRenderer() );
				this.movers[ KoLCharacter.canInteract() ? 0 : 2 ].setSelected( true );
			}

			this.filterItems();
		}

		public InventoryManagePanel( String confirmText, String cancelText, LockableListModel model, boolean isEquipmentOnly )
		{
			super( "", confirmText, cancelText, model );
			this.isEquipmentOnly = isEquipmentOnly;

			this.addFilters( false );
			this.filterItems();

			if ( this.isEquipmentOnly )
				elementList.setCellRenderer( AdventureResult.getEquipmentRenderer() );
			else
				elementList.setCellRenderer( AdventureResult.getDefaultRenderer() );
		}

		public void addFilters( boolean isCompact )
		{
			if ( !this.isEquipmentOnly )
			{
				super.addFilters();
				return;
			}

			this.equipmentFilters = new FilterRadioButton[7];
			this.equipmentFilters[0] = new FilterRadioButton( "weapons", true );
			this.equipmentFilters[1] = new FilterRadioButton( "offhand" );
			this.equipmentFilters[2] = new FilterRadioButton( "hats" );
			this.equipmentFilters[3] = new FilterRadioButton( "shirts" );
			this.equipmentFilters[4] = new FilterRadioButton( "pants" );
			this.equipmentFilters[5] = new FilterRadioButton( "accessories" );
			this.equipmentFilters[6] = new FilterRadioButton( "familiar" );

			ButtonGroup filterGroup = new ButtonGroup();
			JPanel filterPanel = new JPanel();

			for ( int i = 0; i < 7; ++i )
			{
				filterGroup.add( this.equipmentFilters[i] );
				filterPanel.add( this.equipmentFilters[i] );
			}

			this.northPanel.add( filterPanel, BorderLayout.NORTH );
		}

		public void addMovers()
		{
			if ( !this.isEquipmentOnly )
				super.addMovers();
		}

		public FilterItemField getWordFilter()
		{	return new EquipmentFilterField();
		}

		private class FilterRadioButton extends JRadioButton
		{
			public FilterRadioButton( String label )
			{	this( label, false );
			}

			public FilterRadioButton( String label, boolean isSelected )
			{
				super( label, isSelected );
				InventoryManagePanel.this.listenToRadioButton( this );
			}
		}

		private class EquipmentFilterField extends FilterItemField
		{
			public EquipmentFilterField()
			{	this.filter = new EquipmentFilter();
			}

			private class EquipmentFilter extends ConsumptionBasedFilter
			{
				public boolean isVisible( Object element )
				{
					if ( InventoryManagePanel.this.equipmentFilters == null )
						return super.isVisible( element );

					boolean isVisibleWithFilter = true;

					if ( element == null )
						return false;

					int itemId = element instanceof AdventureResult ? ((AdventureResult)element).getItemId() :
						element instanceof ItemCreationRequest ? ((ItemCreationRequest)element).getItemId() : -1;

					if ( itemId == -1 )
						return false;

					switch ( TradeableItemDatabase.getConsumptionType( itemId ) )
					{
					case EQUIP_WEAPON:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[0].isSelected();
						break;

					case EQUIP_OFFHAND:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[1].isSelected();
						break;

					case EQUIP_HAT:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[2].isSelected();
						break;

					case EQUIP_SHIRT:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[3].isSelected();
						break;

					case EQUIP_PANTS:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[4].isSelected();
						break;

					case EQUIP_ACCESSORY:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[5].isSelected();
						break;

					case EQUIP_FAMILIAR:
						isVisibleWithFilter = InventoryManagePanel.this.equipmentFilters[6].isSelected();
						break;

					default:
						return false;
					}

					return isVisibleWithFilter && super.isVisible( element );
				}
			}
		}
	}

	private class HagnkStoragePanel extends InventoryManagePanel
	{
		public HagnkStoragePanel( boolean isEquipmentOnly )
		{
			super( "pull item", isEquipmentOnly ? "pull & use" : "closet item", storage, isEquipmentOnly );

			this.addFilters( false );
			this.addMovers();

			if ( isEquipmentOnly )
				this.eastPanel.add( pullsRemainingLabel1, BorderLayout.SOUTH );
			else
				this.eastPanel.add( pullsRemainingLabel2, BorderLayout.SOUTH );
		}

		private Object [] pullItems()
		{
			Object [] items = this.getDesiredItems( "Pulling" );
			if ( items == null )
				return null;

			RequestThread.openRequestSequence();

			if ( items.length == storage.size() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.EMPTY_STORAGE ) );
			else
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );

			RequestThread.closeRequestSequence();
			return items;
		}

		public void actionConfirmed()
		{	pullItems();
		}

		public void actionCancelled()
		{
			Object [] items = pullItems();
			if ( items == null )
				return;

			RequestThread.closeRequestSequence();

			if ( isEquipmentOnly )
			{
				for ( int i = 0; i < items.length; ++i )
					RequestThread.postRequest( new EquipmentRequest( (AdventureResult) items[i] ) );
			}
			else
			{
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );
			}

			RequestThread.closeRequestSequence();
		}
	}
}
