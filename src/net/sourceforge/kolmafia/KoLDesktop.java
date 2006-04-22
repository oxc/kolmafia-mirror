/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.CardLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;

import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LicenseDisplay;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;

public class KoLDesktop extends KoLFrame implements ChangeListener
{
	private static KoLDesktop INSTANCE = null;
	private boolean addedCompactPane = false;
	private static boolean isInitializing = false;

	private JTabbedPane tabs = new JTabbedPane();
	private ArrayList tabListing = new ArrayList();

	protected JPanel compactPane;
	protected JLabel levelLabel, roninLabel, mcdLabel;
	protected JLabel musLabel, mysLabel, moxLabel, drunkLabel;
	protected JLabel hpLabel, mpLabel, meatLabel, advLabel;
	protected JLabel familiarLabel;

	protected KoLCharacterAdapter refreshListener;

	protected KoLDesktop()
	{
	}

	protected KoLDesktop( String title )
	{
		super( "Main Interface" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		getContentPane().setLayout( new BorderLayout() );

		getContentPane().add( tabs, BorderLayout.CENTER );
		addCompactPane();

		JToolBar toolbarPanel = null;

		switch ( Integer.parseInt( GLOBAL_SETTINGS.getProperty( "toolbarPosition" ) ) )
		{
			case 1:
				toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				getContentPane().add( toolbarPanel, BorderLayout.NORTH );
				break;

			case 2:
				toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
				getContentPane().add( toolbarPanel, BorderLayout.SOUTH );
				break;

			case 3:
				toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
				getContentPane().add( toolbarPanel, BorderLayout.WEST );
				break;

			case 4:
				toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
				getContentPane().add( toolbarPanel, BorderLayout.EAST );
				break;
		}

		setJMenuBar( new KoLMenuBar() );
		addMainToolbar( toolbarPanel );
		tabs.addChangeListener( this );
	}

	public void stateChanged( ChangeEvent e )
	{
		int selectedIndex = tabs.getSelectedIndex();
		if ( selectedIndex != -1 && selectedIndex < tabListing.size() )
			((KoLFrame) tabListing.get( selectedIndex )).requestFocus();
	}

	public void initializeTabs()
	{
		isInitializing = true;

		String interfaceSetting = GLOBAL_SETTINGS.getProperty( "mainInterfaceTabs" );
		String [] interfaceArray = interfaceSetting.split( "," );
		for ( int i = 0; i < interfaceArray.length; ++i )
			KoLmafiaGUI.constructFrame( interfaceArray[i] );

		if ( tabs.getTabCount() != 0 )
			tabs.setSelectedIndex(0);

		isInitializing = false;
	}

	public static boolean isInitializing()
	{	return isInitializing;
	}

	public void dispose()
	{
		KoLFrame [] frames = new KoLFrame[ tabListing.size() ];
		tabListing.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].dispose();

		INSTANCE = null;
		super.dispose();
	}

	public static boolean instanceExists()
	{	return INSTANCE != null;
	}
	
	public static KoLDesktop getInstance()
	{
		if ( INSTANCE == null )
			INSTANCE = new KoLDesktop( VERSION_NAME );

		return INSTANCE;
	}

	public static void addTab( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex == -1 )
		{
			if ( content.tabs != null )
				content.tabs.setTabPlacement( JTabbedPane.BOTTOM );

			INSTANCE.tabListing.add( content );
			INSTANCE.tabs.addTab( content.lastTitle, content.getContentPane() );

			tabIndex = INSTANCE.tabListing.size() - 1;
		}

		INSTANCE.tabs.setSelectedIndex( tabIndex );
	}

	public static void removeTab( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
		{
			INSTANCE.tabListing.remove( tabIndex );
			INSTANCE.tabs.removeTabAt( tabIndex );
		}
	}

	public static void requestFocus( KoLFrame content )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
			INSTANCE.tabs.setSelectedIndex( tabIndex );
	}

	public static void setTitle( KoLFrame content, String newTitle )
	{
		if ( INSTANCE == null )
			return;

		int tabIndex = INSTANCE.tabListing.indexOf( content );
		if ( tabIndex != -1 )
			INSTANCE.tabs.setTitleAt( tabIndex, newTitle );
	}

	public static void updateTitle()
	{
		if ( INSTANCE != null )
			INSTANCE.setTitle( INSTANCE.lastTitle );
		
		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].setTitle( frames[i].lastTitle );
	}

	public static void addMainToolbar( JToolBar toolbarPanel )
	{
		if ( toolbarPanel != null )
		{
			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", CouncilFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Mini-Browser", "browser.gif", RequestFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", CommandDisplayFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "IcePenguin Express", "mail.gif", MailboxFrame.class ) );
			toolbarPanel.add( new InvocationButton( "KoLmafia Chat", "chat.gif", KoLMessenger.class, "initialize" ) );
			toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", ClanManageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Player Status", "hp.gif", CharsheetFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", ItemManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment Manager", "equipment.gif", GearChangeFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", StoreManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", HagnkStorageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Run a Buffbot", "buff.gif", BuffBotFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", FamiliarTrainingFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Player vs. Player", "flower.gif", FlowerHunterFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", MushroomFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
		}
	}
}