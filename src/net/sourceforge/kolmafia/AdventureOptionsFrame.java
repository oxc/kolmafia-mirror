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

import java.io.File;
import java.io.BufferedReader;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JTree;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.swing.tree.DefaultTreeModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileFilter;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.MoodSettings.MoodTrigger;


public abstract class AdventureOptionsFrame extends KoLFrame
{
	private ExecuteButton begin;
	private boolean isHandlingConditions = false;

	private JComboBox actionSelect;
	private JComboBox activeMood;

	private TreeMap zoneMap;
	private JSpinner countField;

	protected JTree combatTree;
	protected JTextArea combatEditor;
	protected DefaultTreeModel combatModel;
	protected CardLayout combatCards;
	protected JPanel combatPanel;

	protected JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect, hpHaltCombatSelect;
	protected JCheckBox [] hpRestoreCheckbox;
	protected JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect, mpBalanceSelect;
	protected JCheckBox [] mpRestoreCheckbox;

	protected JList moodList;
	protected JList locationSelect;
	protected JComboBox zoneSelect;

	protected KoLAdventure lastAdventure = null;
	protected JCheckBox autoSetCheckBox = new JCheckBox();
	protected JTextField conditionField = new JTextField();

	public AdventureOptionsFrame( String title )
	{	super( title );
	}

	public JPanel constructLabelPair( String label, JComponent element1 )
	{	return this.constructLabelPair( label, element1, null );
	}

	public JPanel constructLabelPair( String label, JComponent element1, JComponent element2 )
	{
		JPanel container = new JPanel();
		container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

		if ( element1 != null && element1 instanceof JComboBox )
			JComponentUtilities.setComponentSize( element1, 240, 20 );

		if ( element2 != null && element2 instanceof JComboBox )
			JComponentUtilities.setComponentSize( element2, 240, 20 );

		JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
		labelPanel.add( new JLabel( "<html><b>" + label + "</b></html>", JLabel.LEFT ) );

		container.add( labelPanel );

		if ( element1 != null )
		{
			container.add( Box.createVerticalStrut( 5 ) );
			container.add( element1 );
		}

		if ( element2 != null )
		{
			container.add( Box.createVerticalStrut( 5 ) );
			container.add( element2 );
		}

		return container;
	}

	public JTabbedPane getSouthernTabs()
	{
		// Components of auto-restoration

		JPanel restorePanel = new JPanel( new GridLayout( 1, 2, 10, 10 ) );

		JPanel healthPanel = new JPanel();
		healthPanel.add( new HealthOptionsPanel() );

		JPanel manaPanel = new JPanel();
		manaPanel.add( new ManaOptionsPanel() );

		restorePanel.add( healthPanel );
		restorePanel.add( manaPanel );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < this.hpRestoreCheckbox.length; ++i )
			this.hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < this.mpRestoreCheckbox.length; ++i )
			this.mpRestoreCheckbox[i].addActionListener( listener );

		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		this.combatTree = new JTree();
		this.combatModel = (DefaultTreeModel) this.combatTree.getModel();

		this.combatCards = new CardLayout();
		this.combatPanel = new JPanel( this.combatCards );
		this.combatPanel.add( "tree", new CustomCombatTreePanel() );
		this.combatPanel.add( "editor", new CustomCombatPanel() );

		SimpleScrollPane restoreScroller = new SimpleScrollPane( restorePanel );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );

		this.tabs.addTab( "HP/MP Usage", restoreScroller );

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		AddTriggerPanel triggers = new AddTriggerPanel();
		this.moodList.addListSelectionListener( triggers );
		moodPanel.add( triggers, BorderLayout.NORTH );

		this.tabs.addTab( "Mood Setup", moodPanel );
		this.tabs.addTab( "Custom Combat", this.combatPanel );

		return this.tabs;
	}

	public class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea() );
			AdventureOptionsFrame.this.combatEditor = (JTextArea) this.scrollComponent;
			AdventureOptionsFrame.this.combatEditor.setFont( DEFAULT_FONT );
			AdventureOptionsFrame.this.refreshCombatSettings();
		}

		public void actionConfirmed()
		{
			String saveText = AdventureOptionsFrame.this.combatEditor.getText();

			File location = new File( SETTINGS_LOCATION, CombatSettings.settingsFileName() );
			LogStream writer = LogStream.openStream( location, true );

			writer.print( saveText );
			writer.close();
			writer = null;

			KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
			StaticEntity.setProperty( "battleAction", "custom combat script" );

			// After storing all the data on disk, go ahead
			// and reload the data inside of the tree.

			AdventureOptionsFrame.this.refreshCombatTree();
			AdventureOptionsFrame.this.combatCards.show( AdventureOptionsFrame.this.combatPanel, "tree" );
		}

		public void actionCancelled()
		{	StaticEntity.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public class CustomCombatTreePanel extends LabeledScrollPanel
	{
		public CustomCombatTreePanel()
		{
			super( "Custom Combat", "edit", "load", AdventureOptionsFrame.this.combatTree );
			AdventureOptionsFrame.this.combatTree.setVisibleRowCount( 8 );
		}

		public void actionConfirmed()
		{
			AdventureOptionsFrame.this.refreshCombatSettings();
			AdventureOptionsFrame.this.combatCards.show( AdventureOptionsFrame.this.combatPanel, "editor" );
		}

		public void actionCancelled()
		{
			JFileChooser chooser = new JFileChooser( SETTINGS_LOCATION.getAbsolutePath() );
			chooser.setFileFilter( CCS_FILTER );

			int returnVal = chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null || returnVal != JFileChooser.APPROVE_OPTION )
				return;

			CombatSettings.loadSettings( chooser.getSelectedFile() );
			AdventureOptionsFrame.this.refreshCombatSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	public static final FileFilter CCS_FILTER = new FileFilter()
	{
		public boolean accept( File file )
		{
			String name = file.getName();
			return !name.startsWith( "." ) && name.startsWith( "combat_" );
		}

		public String getDescription()
		{	return "Custom Combat Settings";
		}
	};

	public void refreshCombatSettings()
	{
		try
		{
			CombatSettings.restoreDefaults();
			BufferedReader reader = KoLDatabase.getReader( SETTINGS_DIRECTORY + CombatSettings.settingsFileName() );

			if ( reader == null )
				return;

			StringBuffer buffer = new StringBuffer();
			String line;

			while ( (line = reader.readLine()) != null )
			{
				buffer.append( line );
				buffer.append( '\n' );
			}

			reader.close();
			reader = null;

			// If the buffer is empty, add in the default settings.

			if ( buffer.length() == 0 )
				buffer.append( "[ default ]\n1: attack with weapon" );

			this.combatEditor.setText( buffer.toString() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		this.refreshCombatTree();
	}

	/**
	 * Internal class used to handle everything related to
	 * displaying custom combat.
	 */

	public void refreshCombatTree()
	{
		CombatSettings.restoreDefaults();
		this.combatModel.setRoot( CombatSettings.getRoot() );
		this.combatTree.setRootVisible( false );

		for ( int i = 0; i < this.combatTree.getRowCount(); ++i )
			this.combatTree.expandRow( i );
	}

	public void saveRestoreSettings()
	{
		StaticEntity.setProperty( "autoAbortThreshold", getPercentage( this.hpHaltCombatSelect ) );
		StaticEntity.setProperty( "hpAutoRecovery", getPercentage( this.hpAutoRecoverSelect ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", getPercentage( this.hpAutoRecoverTargetSelect ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", this.getSettingString( this.hpRestoreCheckbox ) );

		StaticEntity.setProperty( "manaBurningThreshold", getPercentage( this.mpBalanceSelect ) );
		StaticEntity.setProperty( "mpAutoRecovery", getPercentage( this.mpAutoRecoverSelect ) );
		StaticEntity.setProperty( "mpAutoRecoveryTarget", getPercentage( this.mpAutoRecoverTargetSelect ) );
		StaticEntity.setProperty( "mpAutoRecoveryItems", this.getSettingString( this.mpRestoreCheckbox ) );
	}

	private static String getPercentage( JComboBox option )
	{	return String.valueOf( (option.getSelectedIndex() - 1) / 20.0f );
	}

	private static void setSelectedIndex( JComboBox option, String property )
	{
		int desiredIndex = (int) ((StaticEntity.getFloatProperty( property ) * 20.0f) + 1);
		option.setSelectedIndex( Math.min( Math.max( desiredIndex, 0 ), option.getItemCount() ) );
	}

	public class CheckboxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	AdventureOptionsFrame.this.saveRestoreSettings();
		}
	}

	public class HealthOptionsPanel extends JPanel implements ActionListener
	{
		public boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			AdventureOptionsFrame.this.hpHaltCombatSelect = new JComboBox();
			AdventureOptionsFrame.this.hpHaltCombatSelect.addItem( "Stop if auto-recovery fails" );
			for ( int i = 0; i <= 19; ++i )
				AdventureOptionsFrame.this.hpHaltCombatSelect.addItem( "Stop if health at " + (i*5) + "%" );

			AdventureOptionsFrame.this.hpAutoRecoverSelect = new JComboBox();
			AdventureOptionsFrame.this.hpAutoRecoverSelect.addItem( "Do not auto-recover health" );
			for ( int i = 0; i <= 19; ++i )
				AdventureOptionsFrame.this.hpAutoRecoverSelect.addItem( "Auto-recover health at " + (i*5) + "%" );

			AdventureOptionsFrame.this.hpAutoRecoverTargetSelect = new JComboBox();
			AdventureOptionsFrame.this.hpAutoRecoverTargetSelect.addItem( "Do not recover health" );
			for ( int i = 0; i <= 20; ++i )
				AdventureOptionsFrame.this.hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*5) + "% health" );

			// Add the elements to the panel

			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			this.add( AdventureOptionsFrame.this.constructLabelPair( "Stop automation: ", AdventureOptionsFrame.this.hpHaltCombatSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( AdventureOptionsFrame.this.constructLabelPair( "Restore your health: ", AdventureOptionsFrame.this.hpAutoRecoverSelect, AdventureOptionsFrame.this.hpAutoRecoverTargetSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( AdventureOptionsFrame.this.constructLabelPair( "Use these restores: ", AdventureOptionsFrame.this.constructScroller( AdventureOptionsFrame.this.hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			setSelectedIndex( AdventureOptionsFrame.this.hpHaltCombatSelect, "autoAbortThreshold" );
			setSelectedIndex( AdventureOptionsFrame.this.hpAutoRecoverSelect, "hpAutoRecovery" );
			setSelectedIndex( AdventureOptionsFrame.this.hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget" );

			AdventureOptionsFrame.this.hpHaltCombatSelect.addActionListener( this );
			AdventureOptionsFrame.this.hpAutoRecoverSelect.addActionListener( this );
			AdventureOptionsFrame.this.hpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < AdventureOptionsFrame.this.hpRestoreCheckbox.length; ++i )
				AdventureOptionsFrame.this.hpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	AdventureOptionsFrame.this.saveRestoreSettings();
		}
	}

	public class ManaOptionsPanel extends JPanel implements ActionListener
	{
		public ManaOptionsPanel()
		{
			AdventureOptionsFrame.this.mpBalanceSelect = new JComboBox();
			AdventureOptionsFrame.this.mpBalanceSelect.addItem( "Do not rebalance buffs" );
			for ( int i = 0; i <= 19; ++i )
				AdventureOptionsFrame.this.mpBalanceSelect.addItem( "Recast buffs until " + (i*5) + "%" );

			AdventureOptionsFrame.this.mpAutoRecoverSelect = new JComboBox();
			AdventureOptionsFrame.this.mpAutoRecoverSelect.addItem( "Do not auto-recover mana" );
			for ( int i = 0; i <= 19; ++i )
				AdventureOptionsFrame.this.mpAutoRecoverSelect.addItem( "Auto-recover mana at " + (i*5) + "%" );

			AdventureOptionsFrame.this.mpAutoRecoverTargetSelect = new JComboBox();
			AdventureOptionsFrame.this.mpAutoRecoverTargetSelect.addItem( "Do not auto-recover mana" );
			for ( int i = 0; i <= 20; ++i )
				AdventureOptionsFrame.this.mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*5) + "% mana" );

			// Add the elements to the panel

			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

			this.add( AdventureOptionsFrame.this.constructLabelPair( "Mana burning: ", AdventureOptionsFrame.this.mpBalanceSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( AdventureOptionsFrame.this.constructLabelPair( "Restore your mana: ", AdventureOptionsFrame.this.mpAutoRecoverSelect, AdventureOptionsFrame.this.mpAutoRecoverTargetSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( AdventureOptionsFrame.this.constructLabelPair( "Use these restores: ", AdventureOptionsFrame.this.constructScroller( AdventureOptionsFrame.this.mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			setSelectedIndex( AdventureOptionsFrame.this.mpBalanceSelect, "manaBurningThreshold" );
			setSelectedIndex( AdventureOptionsFrame.this.mpAutoRecoverSelect, "mpAutoRecovery" );
			setSelectedIndex( AdventureOptionsFrame.this.mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget" );

			AdventureOptionsFrame.this.mpBalanceSelect.addActionListener( this );
			AdventureOptionsFrame.this.mpAutoRecoverSelect.addActionListener( this );
			AdventureOptionsFrame.this.mpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < AdventureOptionsFrame.this.mpRestoreCheckbox.length; ++i )
				AdventureOptionsFrame.this.mpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	AdventureOptionsFrame.this.saveRestoreSettings();
		}
	}

	public class AddTriggerPanel extends KoLPanel implements ListSelectionListener
	{
		public LockableListModel EMPTY_MODEL = new LockableListModel();
		public LockableListModel EFFECT_MODEL = new LockableListModel();

		public TypeComboBox typeSelect;
		public ValueComboBox valueSelect;
		public JTextField commandField;

		public AddTriggerPanel()
		{
			super( "add entry", "auto-fill" );

			this.typeSelect = new TypeComboBox();

			Object [] names = StatusEffectDatabase.values().toArray();

			for ( int i = 0; i < names.length; ++i )
				this.EFFECT_MODEL.add( names[i].toString() );

			this.EFFECT_MODEL.sort();

			this.valueSelect = new ValueComboBox();
			this.commandField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Trigger On: ", this.typeSelect );
			elements[1] = new VerifiableElement( "Check For: ", this.valueSelect );
			elements[2] = new VerifiableElement( "Command: ", this.commandField );

			this.setContent( elements );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			Object selected = AdventureOptionsFrame.this.moodList.getSelectedValue();
			if ( selected == null )
				return;

			MoodTrigger node = (MoodTrigger) selected;
			String type = node.getType();

			// Update the selected type

			if ( type.equals( "lose_effect" ) )
				this.typeSelect.setSelectedIndex(0);
			else if ( type.equals( "gain_effect" ) )
				this.typeSelect.setSelectedIndex(1);
			else if ( type.equals( "unconditional" ) )
				this.typeSelect.setSelectedIndex(2);

			// Update the selected effect

			this.valueSelect.setSelectedItem( node.getName() );
			this.commandField.setText( node.getAction() );
		}

		public void actionConfirmed()
		{
			String currentMood = StaticEntity.getProperty( "currentMood" );
			if ( currentMood.equals( "apathetic" ) )
			{
				alert( "You cannot add triggers to an apathetic mood." );
				return;
			}

			MoodSettings.addTrigger( (String) this.typeSelect.getSelectedType(), (String) this.valueSelect.getSelectedItem(), this.commandField.getText() );
			MoodSettings.saveSettings();
		}

		public void actionCancelled()
		{
			String [] autoFillTypes = new String [] { "minimal set (current active buffs)", "maximal set (all castable buffs)" };
			String desiredType = (String) KoLFrame.input( "Which kind of buff set would you like to use?", autoFillTypes );

			if ( desiredType == autoFillTypes[0] )
				MoodSettings.minimalSet();
			else
				MoodSettings.maximalSet();

			MoodSettings.saveSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public void addStatusLabel()
		{
		}

		public class ValueComboBox extends MutableComboBox
		{
			public ValueComboBox()
			{	super( AddTriggerPanel.this.EFFECT_MODEL, false );
			}

			public void setSelectedItem( Object anObject )
			{
				AddTriggerPanel.this.commandField.setText( MoodSettings.getDefaultAction( AddTriggerPanel.this.typeSelect.getSelectedType(), (String) anObject ) );
				super.setSelectedItem( anObject );
			}
		}

		public class TypeComboBox extends JComboBox
		{
			public TypeComboBox()
			{
				this.addItem( "When an effect is lost" );
				this.addItem( "When an effect is gained" );
				this.addItem( "Unconditional trigger" );

				this.addActionListener( new TypeComboBoxListener() );
			}

			public String getSelectedType()
			{
				switch ( this.getSelectedIndex() )
				{
				case 0:
					return "lose_effect";
				case 1:
					return "gain_effect";
				case 2:
					return "unconditional";
				default:
					return null;
				}
			}

			public class TypeComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	AddTriggerPanel.this.valueSelect.setModel( TypeComboBox.this.getSelectedIndex() == 2 ? AddTriggerPanel.this.EMPTY_MODEL : AddTriggerPanel.this.EFFECT_MODEL );
				}
			}
		}
	}

	public class MoodTriggerListPanel extends LabeledScrollPanel
	{
		public JComboBox availableMoods;

		public MoodTriggerListPanel()
		{
			super( "", new ShowDescriptionList( MoodSettings.getTriggers() ) );

			this.availableMoods = new MoodComboBox();

			this.centerPanel.add( this.availableMoods, BorderLayout.NORTH );
			AdventureOptionsFrame.this.moodList = (JList) this.scrollComponent;

			JPanel extraButtons = new JPanel( new GridLayout( 3, 1, 5, 5 ) );

			extraButtons.add( new NewMoodButton() );
			extraButtons.add( new DeleteMoodButton() );
			extraButtons.add( new CopyMoodButton() );

			JPanel buttonHolder = new JPanel( new BorderLayout() );
			buttonHolder.add( extraButtons, BorderLayout.NORTH );

			this.actualPanel.add( buttonHolder, BorderLayout.EAST );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		public class MoodComboBox extends JComboBox
		{
			public MoodComboBox()
			{
				super( MoodSettings.getAvailableMoods() );
				this.setSelectedItem( StaticEntity.getProperty( "currentMood" ) );
				this.addActionListener( new MoodComboBoxListener() );
			}

			public class MoodComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	MoodSettings.setMood( (String) MoodComboBox.this.getSelectedItem() );
				}
			}
		}

		public class NewMoodButton extends ThreadedButton
		{
			public NewMoodButton()
			{	super( "new list" );
			}

			public void run()
			{
				String name = input( "Give your list a name!" );
				if ( name == null )
					return;

				MoodSettings.setMood( name );
				MoodSettings.saveSettings();
			}
		}

		public class DeleteMoodButton extends ThreadedButton
		{
			public DeleteMoodButton()
			{	super( "delete list" );
			}

			public void run()
			{
				MoodSettings.deleteCurrentMood();
				MoodSettings.saveSettings();
			}
		}

		public class CopyMoodButton extends ThreadedButton
		{
			public CopyMoodButton()
			{	super( "copy list" );
			}

			public void run()
			{
				String moodName = input( "Make a copy of current mood list called:" );
				if ( moodName == null )
					return;

				if ( moodName.equals( "default" ) )
					return;

				MoodSettings.copyTriggers( moodName );
				MoodSettings.setMood( moodName );
				MoodSettings.saveSettings();
			}
		}
	}


	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	public class AdventureSelectPanel extends JPanel implements ChangeListener
	{
		public AdventureSelectPanel( boolean enableAdventures )
		{
			super( new BorderLayout( 10, 10 ) );

			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			// West pane is a scroll pane which lists all of the available
			// locations -- to be included is a map on a separate tab.

			Object currentZone;
			AdventureOptionsFrame.this.zoneMap = new TreeMap();

			AdventureOptionsFrame.this.zoneSelect = new JComboBox();
			AdventureOptionsFrame.this.zoneSelect.addItem( "All Locations" );

			Object [] zones = AdventureDatabase.PARENT_LIST.toArray();

			for ( int i = 0; i < zones.length; ++i )
			{
				currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get( zones[i] );
				AdventureOptionsFrame.this.zoneMap.put( currentZone, zones[i] );
				AdventureOptionsFrame.this.zoneSelect.addItem( currentZone );
			}

			if ( enableAdventures )
			{
				AdventureOptionsFrame.this.countField = new JSpinner();
				AdventureOptionsFrame.this.countField.addChangeListener( this );
				JComponentUtilities.setComponentSize( AdventureOptionsFrame.this.countField, 50, 24 );
			}

			JComponentUtilities.setComponentSize( AdventureOptionsFrame.this.zoneSelect, 200, 24 );
			JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );

			if ( enableAdventures )
				zonePanel.add( AdventureOptionsFrame.this.countField, BorderLayout.EAST );

			zonePanel.add( AdventureOptionsFrame.this.zoneSelect, BorderLayout.CENTER );

			AdventureOptionsFrame.this.zoneSelect.addActionListener( new ZoneChangeListener() );
			AdventureOptionsFrame.this.locationSelect = new JList( adventureList );
			AdventureOptionsFrame.this.locationSelect.setVisibleRowCount( 4 );

			JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
			locationPanel.add( zonePanel, BorderLayout.NORTH );
			locationPanel.add( new SimpleScrollPane( AdventureOptionsFrame.this.locationSelect ), BorderLayout.CENTER );

			if ( enableAdventures )
			{
				JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
				locationHolder.add( locationPanel, "" );
				this.add( locationHolder, BorderLayout.WEST );

				this.add( new ObjectivesPanel(), BorderLayout.CENTER );
				((JSpinner.DefaultEditor)AdventureOptionsFrame.this.countField.getEditor()).getTextField().addKeyListener( AdventureOptionsFrame.this.begin );
			}
			else
			{
				this.add( locationPanel, BorderLayout.WEST );
			}
		}

		public void stateChanged( ChangeEvent e )
		{
			int desired = getValue( AdventureOptionsFrame.this.countField, KoLCharacter.getAdventuresLeft() );
			if ( desired == KoLCharacter.getAdventuresLeft() + 1 )
				AdventureOptionsFrame.this.countField.setValue( new Integer( 1 ) );
			else if ( desired <= 0 || desired > KoLCharacter.getAdventuresLeft() )
				AdventureOptionsFrame.this.countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

		}

		public void requestFocus()
		{	AdventureOptionsFrame.this.locationSelect.requestFocus();
		}
	}

	private class ObjectivesPanel extends KoLPanel
	{
		public ObjectivesPanel()
		{
			super( new Dimension( 70, 20 ), new Dimension( 200, 20 ) );

			AdventureOptionsFrame.this.actionSelect = new MutableComboBox( KoLCharacter.getBattleSkillNames(), false );
			AdventureOptionsFrame.this.activeMood = new JComboBox( MoodSettings.getAvailableMoods() );

			AdventureOptionsFrame.this.locationSelect.addListSelectionListener( new ConditionChangeListener() );

			JPanel conditionPanel = new JPanel( new BorderLayout( 5, 5 ) );
			conditionPanel.add( AdventureOptionsFrame.this.conditionField, BorderLayout.CENTER );
			conditionPanel.add( AdventureOptionsFrame.this.autoSetCheckBox, BorderLayout.EAST );

			AdventureOptionsFrame.this.autoSetCheckBox.setSelected( StaticEntity.getBooleanProperty( "autoSetConditions" ) );
			AdventureOptionsFrame.this.conditionField.setEnabled( StaticEntity.getBooleanProperty( "autoSetConditions" ) );

			addActionListener( AdventureOptionsFrame.this.autoSetCheckBox, new EnableObjectivesListener() );

			JPanel buttonWrapper = new JPanel();
			buttonWrapper.add( AdventureOptionsFrame.this.begin = new ExecuteButton() );
			buttonWrapper.add( new InvocationButton( "stop", RequestThread.class, "declareWorldPeace" ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Action:  ", AdventureOptionsFrame.this.actionSelect );
			elements[1] = new VerifiableElement( "Goals:  ", conditionPanel );

			this.setContent( elements );
			this.container.add( buttonWrapper, BorderLayout.SOUTH );

			JComponentUtilities.addHotKey( this, KeyEvent.VK_ENTER, AdventureOptionsFrame.this.begin );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "battleAction", (String) AdventureOptionsFrame.this.actionSelect.getSelectedItem() );
			MoodSettings.setMood( (String) AdventureOptionsFrame.this.activeMood.getSelectedItem() );
		}

		public void actionCancelled()
		{
		}

		public void addStatusLabel()
		{
		}

		public void setEnabled( boolean isEnabled )
		{	AdventureOptionsFrame.this.begin.setEnabled( isEnabled );
		}

		private class EnableObjectivesListener extends ThreadedListener
		{
			public void run()
			{
				StaticEntity.setProperty( "autoSetConditions", String.valueOf(
					AdventureOptionsFrame.this.autoSetCheckBox.isSelected() ) );

				AdventureOptionsFrame.this.conditionField.setEnabled(
					AdventureOptionsFrame.this.autoSetCheckBox.isSelected() && !KoLmafia.isAdventuring() );
			}
		}
	}

	private class ZoneChangeListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			if ( AdventureOptionsFrame.this.zoneSelect.getSelectedIndex() == 0 )
			{
				AdventureDatabase.refreshAdventureList();
				return;
			}

			String zone = (String) AdventureOptionsFrame.this.zoneSelect.getSelectedItem();

			if ( zone == null )
				return;

			zone = (String) AdventureOptionsFrame.this.zoneMap.get( zone );
			if ( zone == null )
				return;

			AdventureDatabase.refreshAdventureList( zone );
		}
	}

	private class ConditionChangeListener implements ListSelectionListener, ListDataListener
	{
		public ConditionChangeListener()
		{	conditions.addListDataListener( this );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			if ( KoLmafia.isAdventuring() )
				return;

			conditions.clear();
			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
				return;

			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void intervalRemoved( ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
				return;

			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void contentsChanged( ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
				return;

			AdventureOptionsFrame.this.fillCurrentConditions();
		}
	}

	private class ExecuteButton extends ThreadedButton
	{
		private boolean isProcessing = false;

		public ExecuteButton()
		{
			super( "begin" );
			this.setToolTipText( "Start Adventuring" );
		}

		public void run()
		{
			if ( this.isProcessing )
				return;

			KoLmafia.updateDisplay( "Validating adventure sequence..." );

			KoLAdventure request = (KoLAdventure) AdventureOptionsFrame.this.locationSelect.getSelectedValue();
			if ( request == null )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "No location selected." );
				return;
			}

			this.isProcessing = true;

			// If there are conditions in the condition field, be
			// sure to process them.

			if ( conditions.isEmpty() || (AdventureOptionsFrame.this.lastAdventure != null && AdventureOptionsFrame.this.lastAdventure != request) )
			{
				Object stats = null;
				int substatIndex = conditions.indexOf( tally.get(2) );

				if ( substatIndex != 0 )
					stats = conditions.get( substatIndex );

				conditions.clear();

				if ( stats != null )
					conditions.add( stats );

				AdventureOptionsFrame.this.lastAdventure = request;

				RequestThread.openRequestSequence();
				AdventureOptionsFrame.this.isHandlingConditions = true;
				boolean shouldAdventure = this.handleConditions( request );
				AdventureOptionsFrame.this.isHandlingConditions = false;
				RequestThread.closeRequestSequence();

				if ( !shouldAdventure )
				{
					this.isProcessing = false;
					return;
				}
			}

			int requestCount = Math.min( getValue( AdventureOptionsFrame.this.countField, 1 ), KoLCharacter.getAdventuresLeft() );
			AdventureOptionsFrame.this.countField.setValue( new Integer( requestCount ) );

			boolean resetCount = requestCount == KoLCharacter.getAdventuresLeft();

			StaticEntity.getClient().makeRequest( request, requestCount );

			if ( resetCount )
				AdventureOptionsFrame.this.countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

			this.isProcessing = false;
		}

		private boolean handleConditions( KoLAdventure request )
		{
			if ( KoLmafia.isAdventuring() )
				return false;

			String conditionList = AdventureOptionsFrame.this.conditionField.getText().trim().toLowerCase();
			conditions.clear();

			if ( !AdventureOptionsFrame.this.autoSetCheckBox.isSelected() || conditionList.length() == 0 || conditionList.equalsIgnoreCase( "none" ) )
				return true;

			String [] splitConditions = conditionList.split( "\\s*,\\s*" );

			// First, figure out whether or not you need to do a disjunction
			// on the conditions, which changes how KoLmafia handles them.

			for ( int i = 0; i < splitConditions.length; ++i )
			{
				if ( splitConditions[i] == null )
					continue;

				if ( splitConditions[i].equals( "check" ) )
				{
					// Postpone verification of conditions
					// until all other conditions added.
				}
				else if ( splitConditions[i].equals( "outfit" ) )
				{
					// Determine where you're adventuring and use
					// that to determine which components make up
					// the outfit pulled from that area.

					if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						return true;
				}
				else
				{
					if ( splitConditions[i].startsWith( "+" ) )
					{
						if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + splitConditions[i].substring(1) ) )
							return false;
					}
					else if ( !DEFAULT_SHELL.executeConditionsCommand( "set " + splitConditions[i] ) )
					{
						return false;
					}
				}
			}

			if ( conditions.isEmpty() )
			{
				KoLmafia.updateDisplay( "All conditions already satisfied." );
				return false;
			}

			if ( ((Integer)AdventureOptionsFrame.this.countField.getValue()).intValue() == 0 )
				AdventureOptionsFrame.this.countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

			return true;
		}
	}

	public void fillDefaultConditions()
	{
		AdventureOptionsFrame.this.conditionField.setText(
			AdventureDatabase.getDefaultConditions( (KoLAdventure) this.locationSelect.getSelectedValue() ) );
	}

	public void fillCurrentConditions()
	{
		StringBuffer conditionString = new StringBuffer();

		for ( int i = 0; i < conditions.size(); ++i )
		{
			if ( i > 0 )
				conditionString.append( ", " );

			conditionString.append( ((AdventureResult)conditions.get(i)).toConditionString() );
		}

		if ( conditionString.length() == 0 )
			this.fillDefaultConditions();
		else
			this.conditionField.setText( conditionString.toString() );
	}

	public void requestFocus()
	{
		super.requestFocus();

		if ( this.locationSelect != null )
			this.locationSelect.requestFocus();
	}

	protected JPanel getAdventureSummary( String property, JList locationSelect )
	{
		int selectedIndex = StaticEntity.getIntegerProperty( property );

		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		int cardCount = 0;

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new SimpleScrollPane( tally, 4 ), String.valueOf( cardCount++ ) );

		if ( property.startsWith( "default" ) )
		{
			resultSelect.addItem( "Location Details" );
			resultPanel.add( new SafetyField( locationSelect ), String.valueOf( cardCount++ ) );

			resultSelect.addItem( "Conditions Left" );
			resultPanel.add( new SimpleScrollPane( conditions, 4 ), String.valueOf( cardCount++ ) );
		}

		resultSelect.addItem( "Available Skills" );
		resultPanel.add( new SimpleScrollPane( availableSkills, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new SimpleScrollPane( activeEffects, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new SimpleScrollPane( encounterList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new SimpleScrollPane( adventureList, 4 ), String.valueOf( cardCount++ ) );


		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect, property ) );

		if ( selectedIndex >= cardCount )
			selectedIndex = cardCount - 1;

		resultSelect.setSelectedIndex( selectedIndex );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		return containerPanel;
	}

	private class ResultSelectListener implements ActionListener
	{
		private String property;
		private CardLayout resultCards;
		private JPanel resultPanel;
		private JComboBox resultSelect;

		public ResultSelectListener( CardLayout resultCards, JPanel resultPanel, JComboBox resultSelect, String property )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
			this.property = property;
		}

		public void actionPerformed( ActionEvent e )
		{
			String index = String.valueOf( this.resultSelect.getSelectedIndex() );
			this.resultCards.show( this.resultPanel, index );
			StaticEntity.setProperty( this.property, index );

		}
	}

	protected class SafetyField extends JPanel implements Runnable, ListSelectionListener
	{
		private JTextPane safetyText;
		private String savedText = " ";
		private JList locationSelect;

		public SafetyField( JList locationSelect )
		{
			super( new BorderLayout() );

			this.safetyText = new JTextPane();
			this.locationSelect = locationSelect;

			SimpleScrollPane textScroller = new SimpleScrollPane( this.safetyText );
			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			this.add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addListSelectionListener( this );

			this.safetyText.setContentType( "text/html" );
			this.safetyText.setEditable( false );
			this.setSafetyString();
		}

		public void run()
		{	this.setSafetyString();
		}

		public void valueChanged( ListSelectionEvent e )
		{	this.setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = (KoLAdventure) this.locationSelect.getSelectedValue();

			if ( request == null )
				return;

			AreaCombatData combat = request.getAreaSummary();
			String text = ( combat == null ) ? " " : combat.toString();

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( text.equals( this.savedText ) )
				return;

			this.savedText = text;
			this.safetyText.setText( text );

			// Change the font for the JEditorPane to the
			// same ones used in a JLabel.

			MutableAttributeSet fonts = this.safetyText.getInputAttributes();

			StyleConstants.setFontSize( fonts, DEFAULT_FONT.getSize() );
			StyleConstants.setFontFamily( fonts, DEFAULT_FONT.getFamily() );

			StyledDocument html = this.safetyText.getStyledDocument();
			html.setCharacterAttributes( 0, html.getLength() + 1, fonts, false );

			this.safetyText.setCaretPosition( 0 );
		}
	}
}
