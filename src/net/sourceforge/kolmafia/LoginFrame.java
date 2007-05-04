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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;

import tab.CloseTabbedPane;
import tab.CloseTabPaneEnhancedUI;

import com.informit.guides.JDnDList;
import com.sun.java.forums.SpringUtilities;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.UIManager.LookAndFeelInfo;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class LoginFrame extends KoLFrame
{
	private static LoginFrame INSTANCE;
	private LoginPanel panel;

	private String username;
	private JComboBox servers;
	private JComponent usernameField;

	private JTextField proxyHost;
	private JTextField proxyPort;
	private JTextField proxyLogin;
	private JTextField proxyPassword;

	public LoginFrame()
	{
		super( VERSION_NAME + ": Login" );

		INSTANCE = this;
		tabs.addTab( "KoL Login", constructLoginPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );

		tabs.addTab( "Main Tabs", new StartupFramesPanel() );
		tabs.addTab( "Look & Feel", new UserInterfacePanel()  );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		tabs.addTab( "Connection", connectPanel );
		tabs.addTab( "Breakfast", breakfastPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );

		setResizable( false );
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( usernameField != null )
			usernameField.requestFocus();
	}

	public static boolean instanceExists()
	{	return INSTANCE != null;
	}

	public static void hideInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.setVisible( false );
	}

	public static void disposeInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.dispose();
	}

	public void dispose()
	{
		honorProxySettings();

		if ( KoLRequest.sessionId == null )
			System.exit(0);

		super.dispose();
		INSTANCE = null;
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getImage( StaticEntity.getProperty( "loginWindowLogo" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( panel = new LoginPanel(), BorderLayout.CENTER );
		return containerPanel;
	}

	/**
	 * An internal class which represents the panel which is nested
	 * inside of the <code>LoginFrame</code>.
	 */

	private class LoginPanel extends KoLPanel
	{
		private JPasswordField passwordField;

		private JCheckBox savePasswordCheckBox;
		private JCheckBox autoLoginCheckBox;
		private JCheckBox getBreakfastCheckBox;

		/**
		 * Constructs a new <code>LoginPanel</code>, containing a place
		 * for the users to input their login name and password.  This
		 * panel, because it is intended to be the content panel for
		 * status message updates, also has a status label.
		 */

		public LoginPanel()
		{
			super( "login", "relay" );

			boolean useTextField = saveStateNames.isEmpty();
			usernameField = useTextField ? (JComponent) new JTextField() : (JComponent) new LoginNameComboBox();
			passwordField = new JPasswordField();

			savePasswordCheckBox = new JCheckBox();
			autoLoginCheckBox = new JCheckBox();
			getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", usernameField );
			elements[1] = new VerifiableElement( "Password: ", passwordField );

			setContent( elements );

			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = StaticEntity.getProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = StaticEntity.getProperty( "lastUsername" );
			else
				autoLoginCheckBox.setSelected( true );

			if ( usernameField instanceof LoginNameComboBox )
				((LoginNameComboBox)usernameField).setSelectedItem( autoLoginSetting );

			String passwordSetting = KoLmafia.getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				passwordField.setText( passwordSetting );
				savePasswordCheckBox.setSelected( true );
			}

			getBreakfastCheckBox.setSelected( StaticEntity.getBooleanProperty( "alwaysGetBreakfast" ) );
			getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			JComponentUtilities.addHotKey( usernameField, KeyEvent.VK_ENTER, CONFIRM_LISTENER );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( usernameField == null || passwordField == null )
				return;

			if ( savePasswordCheckBox == null || autoLoginCheckBox == null || getBreakfastCheckBox == null )
				return;

			super.setEnabled( isEnabled );

			usernameField.setEnabled( isEnabled );
			passwordField.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "relayBrowserOnly", "false" );
			StaticEntity.setProperty( "alwaysGetBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );

			username = null;

			if ( usernameField instanceof JTextField )
				username = ((JTextField)usernameField).getText();
			else if ( ((LoginNameComboBox)usernameField).getSelectedItem() != null )
				username = (String) ((LoginNameComboBox)usernameField).getSelectedItem();
			else
				username = (String) ((LoginNameComboBox)usernameField).currentMatch;


			String password = new String( passwordField.getPassword() );

			if ( username == null || password == null || username.equals("") || password.equals("") )
			{
				setStatusMessage( "Invalid login." );
				return;
			}

			if ( autoLoginCheckBox.isSelected() )
				StaticEntity.setProperty( "autoLogin", username );
			else
				StaticEntity.setProperty( "autoLogin", "" );

			StaticEntity.setGlobalProperty( username, "getBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );

			honorProxySettings();
			RequestThread.postRequest( new LoginRequest( username, password ) );
		}

		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
				StaticEntity.setProperty( "relayBrowserOnly", "true" );

			StaticEntity.getClient().openRelayBrowser();
			LoginFrame.this.setVisible( false );
		}

		private class AutoLoginListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( autoLoginCheckBox.isSelected() )
					actionConfirmed();
				else
					StaticEntity.setProperty( "autoLogin", "" );
			}
		}

		private class GetBreakfastListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	StaticEntity.setGlobalProperty( username, "getBreakfast", String.valueOf( getBreakfastCheckBox.isSelected() ) );
			}
		}

		private class RemovePasswordListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !savePasswordCheckBox.isSelected() && usernameField instanceof JComboBox )
				{
					String value = (String) saveStateNames.getSelectedItem();
					if ( value == null )
						return;

					saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					passwordField.setText( "" );
				}

				StaticEntity.setProperty( "saveStateActive", String.valueOf( savePasswordCheckBox.isSelected() ) );
			}
		}

		/**
		 * Special instance of a JComboBox which overrides the default
		 * key events of a JComboBox to allow you to catch key events.
		 */

		private class LoginNameComboBox extends MutableComboBox
		{
			public LoginNameComboBox()
			{	super( saveStateNames, true );
			}

			public void setSelectedItem( Object anObject )
			{
				super.setSelectedItem( anObject );
				setPassword();
			}

			public void findMatch( int keyCode )
			{
				super.findMatch( keyCode );
				setPassword();
			}

			private void setPassword()
			{
				if ( currentMatch == null )
				{
					passwordField.setText( "" );
					setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				String password = KoLmafia.getSaveState( (String) currentMatch );
				if ( password == null )
				{
					passwordField.setText( "" );
					setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				passwordField.setText( password );
				savePasswordCheckBox.setSelected( true );

				boolean breakfastSetting = StaticEntity.getGlobalProperty( (String) currentMatch, "getBreakfast" ).equals( "true" );
				getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}

	private class ScriptPanel extends OptionsPanel
	{
		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		public ScriptPanel()
		{
			super( "Miscellaneous Scripts" );

			loginScript = new ScriptSelectPanel( new JTextField() );
			logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "On Login: ", loginScript );
			elements[1] = new VerifiableElement( "On Logout: ", logoutScript );
			elements[2] = new VerifiableElement();

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", logoutScript.getText() );
		}

		public void actionCancelled()
		{
			String loginScript = StaticEntity.getProperty( "loginScript" );
			this.loginScript.setText( loginScript );

			String logoutScript = StaticEntity.getProperty( "logoutScript" );
			this.logoutScript.setText( logoutScript );
		}

	}

	private class BreakfastPanel extends JPanel implements ActionListener
	{
		private String breakfastType;
		private JCheckBox [] skillOptions;

		private JCheckBox grabClovers;
		private JCheckBox mushroomPlot;
		private JCheckBox rumpusRoom;
		private JCheckBox readManual;
		private JCheckBox loginRecovery;
		private JCheckBox pathedSummons;

		public BreakfastPanel( String title, String breakfastType )
		{
			super( new BorderLayout() );

			add( JComponentUtilities.createLabel( title, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			JPanel centerPanel = new JPanel( new GridLayout( 4, 3 ) );

			loginRecovery = new JCheckBox( "Allow auto-recovery" );
			loginRecovery.addActionListener( this );
			centerPanel.add( loginRecovery );

			pathedSummons = new JCheckBox( "Honor path restrictions" );
			pathedSummons.addActionListener( this );
			centerPanel.add( pathedSummons );

			rumpusRoom = new JCheckBox( "Visit your rumpus room" );
			rumpusRoom.addActionListener( this );
			centerPanel.add( rumpusRoom );

			this.breakfastType = breakfastType;
			skillOptions = new JCheckBox[ UseSkillRequest.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				skillOptions[i] = new JCheckBox( "Cast " + UseSkillRequest.BREAKFAST_SKILLS[i].toLowerCase() );
				skillOptions[i].addActionListener( this );
				centerPanel.add( skillOptions[i] );
			}

			mushroomPlot = new JCheckBox( "Plant mushrooms" );
			mushroomPlot.addActionListener( this );
			centerPanel.add( mushroomPlot );

			grabClovers = new JCheckBox( "Pick up clovers" );
			grabClovers.addActionListener( this );
			centerPanel.add( grabClovers );

			readManual = new JCheckBox( "Read the guild manual" );
			readManual.addActionListener( this );
			centerPanel.add( readManual );

			JPanel centerHolder = new JPanel( new BorderLayout() );
			centerHolder.add( centerPanel, BorderLayout.NORTH );

			JPanel centerContainer = new JPanel( new CardLayout( 10, 10 ) );
			centerContainer.add( centerHolder, "" );

			add( centerContainer, BorderLayout.CENTER );

			actionCancelled();
		}

		public void actionPerformed( ActionEvent e )
		{	actionConfirmed();
		}

		public void actionConfirmed()
		{
			StringBuffer skillString = new StringBuffer();

			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				if ( skillOptions[i].isSelected() )
				{
					if ( skillString.length() != 0 )
						skillString.append( "," );

					skillString.append( UseSkillRequest.BREAKFAST_SKILLS[i] );
				}
			}

			StaticEntity.setProperty( "breakfast" + breakfastType, skillString.toString() );
			StaticEntity.setProperty( "loginRecovery" + breakfastType, String.valueOf( loginRecovery.isSelected() ) );
			StaticEntity.setProperty( "pathedSummons" + breakfastType, String.valueOf( pathedSummons.isSelected() ) );
			StaticEntity.setProperty( "visitRumpus" + breakfastType, String.valueOf( rumpusRoom.isSelected() ) );
			StaticEntity.setProperty( "autoPlant" + breakfastType, String.valueOf( mushroomPlot.isSelected() ) );
			StaticEntity.setProperty( "grabClovers" + breakfastType, String.valueOf( grabClovers.isSelected() ) );
			StaticEntity.setProperty( "readManual" + breakfastType, String.valueOf( readManual.isSelected() ) );
		}

		public void actionCancelled()
		{
			String skillString = StaticEntity.getProperty( "breakfast" + breakfastType );
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
				skillOptions[i].setSelected( skillString.indexOf( UseSkillRequest.BREAKFAST_SKILLS[i] ) != -1 );

			loginRecovery.setSelected( StaticEntity.getBooleanProperty( "loginRecovery" + breakfastType ) );
			pathedSummons.setSelected( StaticEntity.getBooleanProperty( "pathedSummons" + breakfastType ) );
			rumpusRoom.setSelected( StaticEntity.getBooleanProperty( "visitRumpus" + breakfastType ) );
			mushroomPlot.setSelected( StaticEntity.getBooleanProperty( "autoPlant" + breakfastType ) );
			grabClovers.setSelected( StaticEntity.getBooleanProperty( "grabClovers" + breakfastType ) );
			readManual.setSelected( StaticEntity.getBooleanProperty( "readManual" + breakfastType ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private class StartupFramesPanel extends KoLPanel implements ListDataListener
	{
		private final String [][] FRAME_OPTIONS =
		{
			{ "Adventure", "AdventureFrame" },
			{ "Mini-Browser", "RequestFrame" },
			{ "Relay Server", "LocalRelayServer" },

			{ "Purchases", "MallSearchFrame" },
			{ "Graphical CLI", "CommandDisplayFrame" },

			{ "Player Status", "CharsheetFrame" },
			{ "Item Manager", "ItemManageFrame" },
			{ "Gear Changer", "GearChangeFrame" },

			{ "Store Manager", "StoreManageFrame" },
			{ "Museum Display", "MuseumFrame" },

			{ "Hall of Legends", "MeatManageFrame" },
			{ "Skill Casting", "SkillBuffFrame" },

			{ "Contact List", "ContactListFrame" },
			{ "Buffbot Manager", "BuffBotFrame" },
			{ "Purchase Buffs", "BuffRequestFrame" },

			{ "Flower Hunter", "FlowerHunterFrame" },
			{ "Mushroom Plot", "MushroomFrame" },
			{ "Familiar Trainer", "FamiliarTrainingFrame" },

			{ "IcePenguin Express", "MailboxFrame" },
			{ "KoLmafia Chat", "KoLMessenger" },
			{ "Recent Events", "EventsFrame" },

			{ "Clan Management", "ClanManageFrame" },
			{ "Farmer's Almanac", "CalendarFrame" },
			{ "Internal Database", "ExamineItemsFrame" },

			{ "Coin Toss Game", "MoneyMakingGameFrame" },
			{ "Preferences", "OptionsFrame" }
		};

		private boolean isRefreshing = false;
		private JComboBox usernameComboBox;

		private ScriptSelectPanel loginScript;
		private ScriptSelectPanel logoutScript;

		private LockableListModel completeList = new LockableListModel();
		private LockableListModel startupList = new LockableListModel();
		private LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super( new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			usernameComboBox = new JComboBox( saveStateNames );
			loginScript = new ScriptSelectPanel( new JTextField() );
			logoutScript = new ScriptSelectPanel( new JTextField() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Settings:  ", usernameComboBox );

			setContent( elements );

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3, 10, 10 ) );
			optionPanel.add( new LabeledScrollPanel( "Complete List", new JDnDList( completeList, false ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup as Window", new JDnDList( startupList ) ) );
			optionPanel.add( new LabeledScrollPanel( "Startup in Tabs", new JDnDList( desktopList ) ) );

			JTextArea message = new JTextArea(
				"These are the per-user settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

				"The Local Relay Server option and Contact List option cannot actually be in tabs.  When you place the former into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the latter into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( DEFAULT_FONT );

			container.add( message, BorderLayout.NORTH );
			container.add( optionPanel, BorderLayout.SOUTH );
			actionCancelled();

			completeList.addListDataListener( this );
			startupList.addListDataListener( this );
			desktopList.addListDataListener( this );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "loginScript", loginScript.getText() );
			StaticEntity.setProperty( "logoutScript", logoutScript.getText() );

			actionCancelled();
		}

		public void actionCancelled()
		{
			isRefreshing = true;

			username = (String) saveStateNames.getSelectedItem();
			if ( username == null )
				username = "";

			completeList.clear();
			startupList.clear();
			desktopList.clear();

			for ( int i = 0; i < FRAME_OPTIONS.length; ++i )
				completeList.add( FRAME_OPTIONS[i][0] );

			String frameString = StaticEntity.getGlobalProperty( username, "initialFrames" );
			String desktopString = StaticEntity.getGlobalProperty( username, "initialDesktop" );

			if ( frameString.equals( "" ) && desktopString.equals( "" ) )
			{
				frameString = StaticEntity.getGlobalProperty( "", "initialFrames" );
				desktopString = StaticEntity.getGlobalProperty( "", "initialDesktop" );
			}

			String [] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < FRAME_OPTIONS.length; ++j )
					if ( !startupList.contains( FRAME_OPTIONS[j][0] ) && FRAME_OPTIONS[j][1].equals( pieces[i] ) )
					{
						completeList.remove( FRAME_OPTIONS[j][0] );
						startupList.add( FRAME_OPTIONS[j][0] );
					}

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
				for ( int j = 0; j < FRAME_OPTIONS.length; ++j )
					if ( !desktopList.contains( FRAME_OPTIONS[j][0] ) && FRAME_OPTIONS[j][1].equals( pieces[i] ) )
					{
						completeList.remove( FRAME_OPTIONS[j][0] );
						desktopList.add( FRAME_OPTIONS[j][0] );
					}

			isRefreshing = false;
			saveLayoutSettings();

			this.loginScript.setText( StaticEntity.getProperty( "loginScript" ) );
			this.logoutScript.setText( StaticEntity.getProperty( "logoutScript" ) );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( e.getSource() == startupList )
				desktopList.removeAll( startupList );

			if ( e.getSource() == desktopList )
				startupList.removeAll( desktopList );

			saveLayoutSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	saveLayoutSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( isRefreshing )
				return;

			StringBuffer frameString = new StringBuffer();
			StringBuffer desktopString = new StringBuffer();

			for ( int i = 0; i < startupList.size(); ++i )
				for ( int j = 0; j < FRAME_OPTIONS.length; ++j )
					if ( startupList.get(i).equals( FRAME_OPTIONS[j][0] ) )
					{
						if ( frameString.length() != 0 ) frameString.append( "," );
						frameString.append( FRAME_OPTIONS[j][1] );
					}

			for ( int i = 0; i < desktopList.size(); ++i )
				for ( int j = 0; j < FRAME_OPTIONS.length; ++j )
					if ( desktopList.get(i).equals( FRAME_OPTIONS[j][0] ) )
					{
						if ( desktopString.length() != 0 ) desktopString.append( "," );
						desktopString.append( FRAME_OPTIONS[j][1] );
					}

			StaticEntity.setGlobalProperty( "", "initialFrames", frameString.toString() );
			StaticEntity.setGlobalProperty( "", "initialDesktop", desktopString.toString() );

			if ( saveStateNames.size() != 0 )
			{
				String username = (String) saveStateNames.getSelectedItem();
				if ( username == null )
					username = "";

				StaticEntity.setGlobalProperty( username, "initialFrames", frameString.toString() );
				StaticEntity.setGlobalProperty( username, "initialDesktop", desktopString.toString() );
			}
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	private class UserInterfacePanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "guiUsesOneWindow", "Restrict interface to a single window" },
			{ "useSystemTrayIcon", "Minimize to system tray (Windows only)" },
			{},
			{ "addStatusBarToFrames", "Add a status bar to independent windows" },
			{},
			{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
			{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
		};

		private JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( "", new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			UIManager.LookAndFeelInfo [] installed = UIManager.getInstalledLookAndFeels();
			Object [] installedLooks = new Object[ installed.length ];

			for ( int i = 0; i < installedLooks.length; ++i )
				installedLooks[i] = installed[i].getClassName();

			looks = new JComboBox( installedLooks );


			toolbars = new JComboBox();
			toolbars.addItem( "Show global menus only" );
			toolbars.addItem( "Put toolbar along top of panel" );
			toolbars.addItem( "Put toolbar along bottom of panel" );
			toolbars.addItem( "Put toolbar along left of panel" );

			scripts = new JComboBox();
			scripts.addItem( "Do not show script bar on main interface" );
			scripts.addItem( "Put script bar after normal toolbar" );
			scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement [] elements = new VerifiableElement[3];

			elements[0] = new VerifiableElement( "Java L&F: ", looks );
			elements[1] = new VerifiableElement( "Toolbar: ", toolbars );
			elements[2] = new VerifiableElement( "Scripts: ", scripts );

			actionCancelled();
			setContent( elements );
		}

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			container.add( new InterfaceCheckboxPanel(), BorderLayout.SOUTH );
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void actionConfirmed()
		{
			String lookAndFeel = (String) looks.getSelectedItem();
			if ( lookAndFeel != null )
				StaticEntity.setProperty( "swingLookAndFeel", lookAndFeel );

			StaticEntity.setProperty( "useToolbars", String.valueOf( toolbars.getSelectedIndex() != 0 ) );
			StaticEntity.setProperty( "scriptButtonPosition", String.valueOf( scripts.getSelectedIndex() ) );
			StaticEntity.setProperty( "toolbarPosition", String.valueOf( toolbars.getSelectedIndex() ) );
		}

		public void actionCancelled()
		{
			looks.setSelectedItem( StaticEntity.getProperty( "swingLookAndFeel" ) );
			toolbars.setSelectedIndex( StaticEntity.getIntegerProperty( "toolbarPosition" ) );
			scripts.setSelectedIndex( StaticEntity.getIntegerProperty( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel extends OptionsPanel
		{
			private JLabel innerGradient, outerGradient;

			/**
			 * Constructs a new <code>windowsPanel</code>, containing a
			 * place for the users to select their desired server and for them
			 * to modify any applicable proxy settings.
			 */

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ options.length + 3 ];

				optionBoxes = new JCheckBox[ options.length ];
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i] = new JCheckBox();

				for ( int i = 0; i < options.length; ++i )
				{
					if ( options[i].length == 0 )
						elements[i] = new VerifiableElement();
					else
						elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );
				}

				elements[ options.length ] = new VerifiableElement();

				outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ options.length + 1 ] = new VerifiableElement( "Change the outer portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, outerGradient );

				innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ options.length + 2 ] = new VerifiableElement( "Change the inner portion of the tab gradient (shiny tabs)",
					JLabel.LEFT, innerGradient );

				actionCancelled();
				setContent( elements );
			}

			public void actionConfirmed()
			{
				for ( int i = 0; i < options.length; ++i )
					if ( options[i].length > 0 )
						StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

				super.actionConfirmed();
			}

			public void actionCancelled()
			{
				for ( int i = 0; i < options.length; ++i )
					if ( options[i].length > 0 )
						optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );

				innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
				outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			public void setEnabled( boolean isEnabled )
			{
			}

			private class TabColorChanger extends LabelColorChanger
			{
				public TabColorChanger( String property )
				{	super( property );
				}

				public void applyChanges()
				{
					if ( property.equals( "innerTabColor" ) )
						CloseTabPaneEnhancedUI.selectedA = innerGradient.getBackground();
					else
						CloseTabPaneEnhancedUI.selectedB = outerGradient.getBackground();

					tabs.repaint();
				}
			}
		}
	}

	private class ConnectionOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" },
			{ "ignoreLoadBalancer", "Ignore the KoL load balancer when trying to login" },
			{ "testSocketTimeout", "Allow socket timeouts for unstable connections" }
		};

		public ConnectionOptionsPanel()
		{
			super( "Connection Options", new Dimension( 20, 20 ), new Dimension( 380, 20 ) );

			servers = new JComboBox();
			servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			for ( int i = 2; i <= KoLRequest.SERVER_COUNT; ++i )
				servers.addItem( "Attempt to use www" + i + ".kingdomofloathing.com" );

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ 2 + options.length ];

			elements[0] = new VerifiableElement( servers );
			elements[1] = new VerifiableElement();

			for ( int i = 0; i < options.length; ++i )
				elements[i+2] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			actionCancelled();
			setContent( elements );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "defaultLoginServer", String.valueOf( servers.getSelectedIndex() ) );
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );
		}

		public void actionCancelled()
		{
			servers.setSelectedIndex( StaticEntity.getIntegerProperty( "defaultLoginServer" ) );
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public void honorProxySettings()
	{
		StaticEntity.setProperty( "proxySet", String.valueOf(
				proxyHost.getText().trim().length() > 0 ) );

		StaticEntity.setProperty( "http.proxyHost", proxyHost.getText() );
		StaticEntity.setProperty( "http.proxyPort", proxyPort.getText() );
		StaticEntity.setProperty( "http.proxyUser", proxyLogin.getText() );
		StaticEntity.setProperty( "http.proxyPassword", proxyPassword.getText() );
	}

	/**
	 * This panel handles all of the things related to proxy
	 * options (if applicable).
	 */

	private class ProxyOptionsPanel extends LabeledKoLPanel
	{
		/**
		 * Constructs a new <code>ProxyOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ProxyOptionsPanel()
		{
			super( "Proxy Settings", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			proxyHost = new JTextField();
			proxyPort = new JTextField();
			proxyLogin = new JTextField();
			proxyPassword = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Host: ", proxyHost );
			elements[1] = new VerifiableElement( "Port: ", proxyPort );
			elements[2] = new VerifiableElement( "Login: ", proxyLogin );
			elements[3] = new VerifiableElement( "Password: ", proxyPassword );

			actionCancelled();
			setContent( elements );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			boolean shouldEnable = StaticEntity.getBooleanProperty( "proxySet" );

			proxyHost.setText( StaticEntity.getProperty( "http.proxyHost" ) );
			proxyPort.setText( StaticEntity.getProperty( "http.proxyPort" ) );
			proxyLogin.setText( StaticEntity.getProperty( "http.proxyUser" ) );
			proxyPassword.setText( StaticEntity.getProperty( "http.proxyPassword" ) );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}
}
