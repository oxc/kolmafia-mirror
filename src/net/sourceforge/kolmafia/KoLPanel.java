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
import java.awt.Container;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.File;

import java.lang.ref.WeakReference;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class KoLPanel extends ActionVerifyPanel implements KoLConstants
{
	private VerifiableElement [] elements;

	public JPanel southContainer;
	public JPanel actionStatusPanel;
	public StatusLabel actionStatusLabel;

	public KoLPanel( Dimension left, Dimension right )
	{
		super( left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText )
	{
		super( confirmedText );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, boolean isCenterPanel )
	{
		super( confirmedText, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText )
	{
		super( confirmedText, cancelledText );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText, left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	public void setContent( VerifiableElement [] elements, boolean bothDisabledOnClick )
	{	setContent( elements, null, null, bothDisabledOnClick );
	}

	public void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel, boolean bothDisabledOnClick )
	{
		super.setContent( elements, mainPanel, eastPanel, bothDisabledOnClick );

		// In addition to setting the content on these, also
		// add a return-key listener to each of the input fields.

		this.elements = elements;

		addListeners();
		addStatusLabel();
	}

	public void addListeners()
	{
		if ( elements == null )
			return;

		ActionConfirmListener listener = new ActionConfirmListener();

		for ( int i = 0; i < elements.length; ++i )
			if ( elements[i].getInputField() instanceof JTextField )
				((JTextField)elements[i].getInputField()).addKeyListener( listener );
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		if ( elements == null || elements.length == 0 )
			return;

		if ( elements[0].getInputField().isEnabled() == isEnabled )
			return;

		for ( int i = 0; i < elements.length; ++i )
			elements[i].getInputField().setEnabled( isEnabled );
	}

	public void setStatusMessage( String message )
	{
		if ( actionStatusLabel != null )
			actionStatusLabel.setStatusMessage( message );
	}

	public void addStatusLabel()
	{
		if ( elements == null )
			return;

		boolean shouldAddStatusLabel = elements != null && elements.length != 0;
		for ( int i = 0; shouldAddStatusLabel && i < elements.length; ++i )
			shouldAddStatusLabel &= !(elements[i].getInputField() instanceof JScrollPane);

		if ( !shouldAddStatusLabel )
			return;

		JPanel statusContainer = new JPanel();
		statusContainer.setLayout( new BoxLayout( statusContainer, BoxLayout.Y_AXIS ) );

		actionStatusPanel = new JPanel( new BorderLayout() );
		actionStatusLabel = new StatusLabel();
		actionStatusPanel.add( actionStatusLabel, BorderLayout.SOUTH );

		statusContainer.add( actionStatusPanel );
		statusContainer.add( Box.createVerticalStrut( 20 ) );

		southContainer = new JPanel( new BorderLayout() );
		southContainer.add( statusContainer, BorderLayout.NORTH );
		container.add( southContainer, BorderLayout.SOUTH );
	}

	private class StatusLabel extends JLabel
	{
		public StatusLabel()
		{	super( " ", JLabel.CENTER );
		}

		public void setStatusMessage( String message )
		{
			String label = getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( message == null || label == null || message.length() == 0 )
				return;

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			setText( message );
		}
	}


	/**
	 * This internal class is used to process the request for selecting
	 * a script using the file dialog.
	 */

	public class ScriptSelectPanel extends JPanel implements ActionListener, Runnable
	{
		private JTextField scriptField;
		private JButton scriptButton;

		public ScriptSelectPanel( JTextField scriptField )
		{
			setLayout( new BorderLayout( 0, 0 ) );

			add( scriptField, BorderLayout.CENTER );
			scriptButton = new JButton( "..." );

			JComponentUtilities.setComponentSize( scriptButton, 20, 20 );
			scriptButton.addActionListener( this );
			add( scriptButton, BorderLayout.EAST );

			this.scriptField = scriptField;
		}

		public void setEnabled( boolean isEnabled )
		{
			scriptField.setEnabled( isEnabled );
			scriptButton.setEnabled( isEnabled );
		}

		public String getText()
		{	return scriptField.getText();
		}

		public void setText( String text )
		{	scriptField.setText( text );
		}

		public void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}

		public void run()
		{
			JFileChooser chooser = new JFileChooser( SCRIPT_LOCATION.getAbsolutePath() );
			chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null )
				return;

			scriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
			actionConfirmed();
		}
	}

	public class ActionConfirmListener extends KeyAdapter
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				actionConfirmed();
		}
	}
}
