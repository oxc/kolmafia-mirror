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
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class LoginFrame extends KoLFrame
{
	private static LoginFrame INSTANCE = null;

	private String username;
	private JComboBox servers;
	private JComponent usernameField;

	private AutoHighlightField proxyHost;
	private AutoHighlightField proxyPort;
	private AutoHighlightField proxyLogin;
	private AutoHighlightField proxyPassword;

	public LoginFrame()
	{
		super( StaticEntity.getVersion() + ": Login" );

		INSTANCE = this;
		this.tabs.addTab( "KoL Login", this.constructLoginPanel() );

		JPanel breakfastPanel = new JPanel();
		breakfastPanel.setLayout( new BoxLayout( breakfastPanel, BoxLayout.Y_AXIS ) );

		breakfastPanel.add( new ScriptPanel() );
		breakfastPanel.add( new BreakfastPanel( "Softcore Characters", "Softcore" ) );
		breakfastPanel.add( new BreakfastPanel( "Hardcore Characters", "Hardcore" ) );

		this.tabs.addTab( "Main Tabs", new StartupFramesPanel() );
		this.tabs.addTab( "Look & Feel", new UserInterfacePanel()  );

		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.Y_AXIS ) );
		connectPanel.add( new ConnectionOptionsPanel() );
		connectPanel.add( new ProxyOptionsPanel() );

		this.tabs.addTab( "Connection", connectPanel );
		this.tabs.addTab( "Breakfast", breakfastPanel );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	public boolean shouldAddStatusBar()
	{	return false;
	}

	public void requestFocus()
	{
		super.requestFocus();
		if ( this.usernameField != null )
			this.usernameField.requestFocus();
	}

	public static final boolean instanceExists()
	{	return INSTANCE != null;
	}

	public static final void hideInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.setVisible( false );
	}

	public static final void disposeInstance()
	{
		if ( INSTANCE != null )
			INSTANCE.dispose();
	}

	public void dispose()
	{
		this.honorProxySettings();

		if ( !KoLDesktop.instanceExists() )
			System.exit(0);

		super.dispose();
		INSTANCE = null;
	}

	public JPanel constructLoginPanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 0, 0 ) );
		imagePanel.add( new JLabel( " " ), BorderLayout.NORTH );
		imagePanel.add( new JLabel( JComponentUtilities.getImage( KoLSettings.getUserProperty( "loginWindowLogo" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( imagePanel, BorderLayout.NORTH );
		containerPanel.add( new LoginPanel(), BorderLayout.CENTER );
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
			LoginFrame.this.usernameField = useTextField ? (JComponent) new AutoHighlightField() : (JComponent) new LoginNameComboBox();
			this.passwordField = new JPasswordField();

			this.savePasswordCheckBox = new JCheckBox();
			this.autoLoginCheckBox = new JCheckBox();
			this.getBreakfastCheckBox = new JCheckBox();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Save Password: " ), "" );
			checkBoxPanels.add( this.savePasswordCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Auto-Login: " ), "" );
			checkBoxPanels.add( this.autoLoginCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );
			checkBoxPanels.add( new JLabel( "Breakfast: " ), "" );
			checkBoxPanels.add( this.getBreakfastCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 16 ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Login: ", LoginFrame.this.usernameField );
			elements[1] = new VerifiableElement( "Password: ", this.passwordField );

			this.setContent( elements );

			this.actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ), BorderLayout.CENTER );
			this.actionStatusPanel.add( checkBoxPanels, BorderLayout.NORTH );

			String autoLoginSetting = KoLSettings.getUserProperty( "autoLogin" );
			if ( autoLoginSetting.equals( "" ) )
				autoLoginSetting = KoLSettings.getUserProperty( "lastUsername" );
			else
				this.autoLoginCheckBox.setSelected( true );

			if ( LoginFrame.this.usernameField instanceof LoginNameComboBox )
				((LoginNameComboBox)LoginFrame.this.usernameField).setSelectedItem( autoLoginSetting );

			String passwordSetting = KoLmafia.getSaveState( autoLoginSetting );

			if ( passwordSetting != null )
			{
				this.passwordField.setText( passwordSetting );
				this.savePasswordCheckBox.setSelected( true );
			}

			this.getBreakfastCheckBox.setSelected( KoLSettings.getBooleanProperty( "alwaysGetBreakfast" ) );
			this.getBreakfastCheckBox.addActionListener( new GetBreakfastListener() );
			this.autoLoginCheckBox.addActionListener( new AutoLoginListener() );
			this.savePasswordCheckBox.addActionListener( new RemovePasswordListener() );

			try
			{
				String holiday = MoonPhaseDatabase.getHoliday( DATED_FILENAME_FORMAT.parse( DATED_FILENAME_FORMAT.format( new Date() ) ), true );
				this.setStatusMessage( holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
			catch ( Exception e )
			{
				// Should not happen, you're parsing something that
				// was formatted the same way.

				StaticEntity.printStackTrace( e );
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( LoginFrame.this.usernameField == null || this.passwordField == null )
				return;

			if ( this.savePasswordCheckBox == null || this.autoLoginCheckBox == null || this.getBreakfastCheckBox == null )
				return;

			super.setEnabled( isEnabled );

			LoginFrame.this.usernameField.setEnabled( isEnabled );
			this.passwordField.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			KoLSettings.setUserProperty( "relayBrowserOnly", "false" );
			KoLSettings.setUserProperty( "alwaysGetBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.username = null;

			if ( LoginFrame.this.usernameField instanceof AutoHighlightField )
				LoginFrame.this.username = ((AutoHighlightField)LoginFrame.this.usernameField).getText();
			else if ( ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem() != null )
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).getSelectedItem();
			else
				LoginFrame.this.username = (String) ((LoginNameComboBox)LoginFrame.this.usernameField).currentMatch;


			String password = new String( this.passwordField.getPassword() );

			if ( LoginFrame.this.username == null || password == null || LoginFrame.this.username.equals("") || password.equals("") )
			{
				this.setStatusMessage( "Invalid login." );
				return;
			}

			if ( this.autoLoginCheckBox.isSelected() )
				KoLSettings.setUserProperty( "autoLogin", LoginFrame.this.username );
			else
				KoLSettings.setUserProperty( "autoLogin", "" );

			KoLSettings.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( this.getBreakfastCheckBox.isSelected() ) );

			LoginFrame.this.honorProxySettings();
			RequestThread.postRequest( new LoginRequest( LoginFrame.this.username, password ) );
		}

		public void actionCancelled()
		{
			if ( !LoginRequest.isInstanceRunning() )
			{
				KoLSettings.setUserProperty( "relayBrowserOnly", "true" );
				actionConfirmed();
			}
		}

		private class AutoLoginListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( LoginPanel.this.autoLoginCheckBox.isSelected() )
					LoginPanel.this.actionConfirmed();
				else
					KoLSettings.setUserProperty( "autoLogin", "" );
			}
		}

		private class GetBreakfastListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	KoLSettings.setGlobalProperty( LoginFrame.this.username, "getBreakfast", String.valueOf( LoginPanel.this.getBreakfastCheckBox.isSelected() ) );
			}
		}

		private class RemovePasswordListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !LoginPanel.this.savePasswordCheckBox.isSelected() && LoginFrame.this.usernameField instanceof JComboBox )
				{
					String value = (String) saveStateNames.getSelectedItem();
					if ( value == null )
						return;

					saveStateNames.remove( value );
					KoLmafia.removeSaveState( value );
					LoginPanel.this.passwordField.setText( "" );
				}

				KoLSettings.setUserProperty( "saveStateActive", String.valueOf( LoginPanel.this.savePasswordCheckBox.isSelected() ) );
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
				this.setPassword();
			}

			public void findMatch( int keyCode )
			{
				super.findMatch( keyCode );
				this.setPassword();
			}

			private void setPassword()
			{
				if ( this.currentMatch == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				String password = KoLmafia.getSaveState( (String) this.currentMatch );
				if ( password == null )
				{
					LoginPanel.this.passwordField.setText( "" );
					LoginPanel.this.setStatusMessage( " " );

					LoginPanel.this.setEnabled( true );
					return;
				}

				LoginPanel.this.passwordField.setText( password );
				LoginPanel.this.savePasswordCheckBox.setSelected( true );

				boolean breakfastSetting = KoLSettings.getGlobalProperty( (String) this.currentMatch, "getBreakfast" ).equals( "true" );
				LoginPanel.this.getBreakfastCheckBox.setSelected( breakfastSetting );
				LoginPanel.this.setEnabled( true );
			}
		}
	}


	private class ConnectionOptionsPanel extends OptionsPanel
	{
		private JCheckBox loadBalancer, loadDistributer;
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "proxySet", "Use a proxy to connect to the Kingdom of Loathing" },
			{ "testSocketTimeout", "Allow socket timeouts for unstable connections" },
			{ "printSocketTimeouts", "Show error message when a socket timeout occurs" }
		};

		public ConnectionOptionsPanel()
		{
			super( "Connection Options", new Dimension( 20, 20 ), new Dimension( 380, 20 ) );

			LoginFrame.this.servers = new JComboBox();
			LoginFrame.this.servers.addItem( "Attempt to use dev.kingdomofloathing.com" );
			LoginFrame.this.servers.addItem( "Attempt to use www.kingdomofloathing.com" );

			for ( int i = 2; i <= KoLRequest.SERVER_COUNT; ++i )
				LoginFrame.this.servers.addItem( "Attempt to use www" + i + ".kingdomofloathing.com" );

			this.optionBoxes = new JCheckBox[ this.options.length ];
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ 5 + this.options.length ];

			elements[0] = new VerifiableElement( LoginFrame.this.servers );
			elements[1] = new VerifiableElement();
			elements[2] = new VerifiableElement( "Attempt to ignore login page load balancer", JLabel.LEFT, loadBalancer = new JCheckBox() );
			elements[3] = new VerifiableElement( "Enable server-friendlier auto-adventuring", JLabel.LEFT, loadDistributer = new JCheckBox() );
			elements[4] = new VerifiableElement();

			for ( int i = 0; i < this.options.length; ++i )
				elements[i+5] = new VerifiableElement( this.options[i][1], JLabel.LEFT, this.optionBoxes[i] );

			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				String proxySet = System.getProperty( "proxySet" );
				optionBoxes[0].setSelected( proxySet != null && proxySet.equals( "true" ) );
				optionBoxes[0].setEnabled( false );
			}

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			KoLSettings.setUserProperty( "defaultLoginServer", String.valueOf( LoginFrame.this.servers.getSelectedIndex() ) );
			for ( int i = 0; i < this.options.length; ++i )
				KoLSettings.setUserProperty( this.options[i][0], String.valueOf( this.optionBoxes[i].isSelected() ) );

			LoginRequest.setIgnoreLoadBalancer( loadBalancer.isSelected() );
			KoLRequest.setDelayActive( loadDistributer.isSelected() );
		}

		public void actionCancelled()
		{
			LoginFrame.this.servers.setSelectedIndex( KoLSettings.getIntegerProperty( "defaultLoginServer" ) );
			for ( int i = 0; i < this.options.length; ++i )
				this.optionBoxes[i].setSelected( KoLSettings.getBooleanProperty( this.options[i][0] ) );

			loadBalancer.setSelected( false );
			loadDistributer.setSelected( true );

			LoginRequest.setIgnoreLoadBalancer( false );
			KoLRequest.setDelayActive( true );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public void honorProxySettings()
	{
		KoLSettings.setUserProperty( "http.proxyHost", this.proxyHost.getText() );
		KoLSettings.setUserProperty( "http.proxyPort", this.proxyPort.getText() );
		KoLSettings.setUserProperty( "http.proxyUser", this.proxyLogin.getText() );
		KoLSettings.setUserProperty( "http.proxyPassword", this.proxyPassword.getText() );
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

			LoginFrame.this.proxyHost = new AutoHighlightField();
			LoginFrame.this.proxyPort = new AutoHighlightField();
			LoginFrame.this.proxyLogin = new AutoHighlightField();
			LoginFrame.this.proxyPassword = new AutoHighlightField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Host: ", LoginFrame.this.proxyHost );
			elements[1] = new VerifiableElement( "Port: ", LoginFrame.this.proxyPort );
			elements[2] = new VerifiableElement( "Login: ", LoginFrame.this.proxyLogin );
			elements[3] = new VerifiableElement( "Password: ", LoginFrame.this.proxyPassword );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			{
				LoginFrame.this.proxyHost.setText( System.getProperty( "http.proxyHost" ) );
				LoginFrame.this.proxyPort.setText( System.getProperty( "http.proxyPort" ) );
				LoginFrame.this.proxyLogin.setText( System.getProperty( "http.proxyUser" ) );
				LoginFrame.this.proxyPassword.setText( System.getProperty( "http.proxyPassword" ) );

				LoginFrame.this.proxyHost.setEnabled( false );
				LoginFrame.this.proxyPort.setEnabled( false );
				LoginFrame.this.proxyLogin.setEnabled( false );
				LoginFrame.this.proxyPassword.setEnabled( false );
			}
			else
			{
				LoginFrame.this.proxyHost.setText( KoLSettings.getUserProperty( "http.proxyHost" ) );
				LoginFrame.this.proxyPort.setText( KoLSettings.getUserProperty( "http.proxyPort" ) );
				LoginFrame.this.proxyLogin.setText( KoLSettings.getUserProperty( "http.proxyUser" ) );
				LoginFrame.this.proxyPassword.setText( KoLSettings.getUserProperty( "http.proxyPassword" ) );
			}
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}
}
