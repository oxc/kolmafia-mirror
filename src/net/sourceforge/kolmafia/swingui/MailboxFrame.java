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

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.MailboxRequest;
import net.sourceforge.kolmafia.session.MailManager;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MailboxFrame
	extends GenericFrame
	implements ChangeListener
{
	private KoLMailMessage displayed;
	private final RequestPane messageContent;

	private MailSelectList messageListInbox;
	private MailSelectList messageListPvp;
	private MailSelectList messageListPenPal;
	private MailSelectList messageListOutbox;
	private MailSelectList messageListSaved;

	public MailboxFrame()
	{
		super( "IcePenguin Express" );

		this.addTab( "Inbox", this.messageListInbox = new MailSelectList( "Inbox" ) );
		this.addTab( "PvP", this.messageListPvp = new MailSelectList( "PvP" ) );
		this.addTab( "Pen Pal", this.messageListPenPal = new MailSelectList( "Pen Pal" ) );
		this.addTab( "Outbox", this.messageListOutbox = new MailSelectList( "Outbox" ) );
		this.addTab( "Saved", this.messageListSaved = new MailSelectList( "Saved" ) );

		this.tabs.addChangeListener( this );
		this.tabs.setMinimumSize( new Dimension( 0, 150 ) );

		this.messageContent = new RequestPane();
		this.messageContent.addHyperlinkListener( new MailLinkClickedListener() );

		JScrollPane messageContentDisplay =
			new JScrollPane(
				this.messageContent, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		messageContentDisplay.setMinimumSize( new Dimension( 0, 150 ) );

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, true, this.tabs, messageContentDisplay );

		splitPane.setOneTouchExpandable( true );
		JComponentUtilities.setComponentSize( splitPane, 500, 300 );
		this.getContentPane().add( splitPane );

		this.getToolbar();
	}

	public JToolBar getToolbar()
	{
		JToolBar toolbarPanel = super.getToolbar( true );

		toolbarPanel.add( new SaveAllButton() );
		toolbarPanel.add( new DeleteButton() );
		toolbarPanel.add( new RefreshButton() );

		return toolbarPanel;
	}

	public void setEnabled( final boolean isEnabled )
	{
		if ( this.tabs == null || this.messageListInbox == null || this.messageListPvp == null || this.messageListPenPal == null || this.messageListOutbox == null || this.messageListSaved == null )
		{
			return;
		}

		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			this.tabs.setEnabledAt( i, isEnabled );
		}

		this.messageListInbox.setEnabled( isEnabled );
		this.messageListPvp.setEnabled( isEnabled );
		this.messageListPenPal.setEnabled( isEnabled );
		this.messageListOutbox.setEnabled( isEnabled );
		this.messageListSaved.setEnabled( isEnabled );
	}

	/**
	 * Whenever the tab changes, this method is used to retrieve the messages from the appropriate if the mailbox is
	 * currently empty.
	 */

	public void stateChanged( final ChangeEvent e )
	{
		this.refreshMailManager();

		boolean requestMailbox;
		String currentTabName = this.tabs.getTitleAt( this.tabs.getSelectedIndex() );

		if ( currentTabName.equals( "Inbox" ) )
		{
			if ( this.messageListInbox.isInitialized() )
			{
				this.messageListInbox.valueChanged( null );
			}
			requestMailbox = !this.messageListInbox.isInitialized();
		}
		else if ( currentTabName.equals( "PvP" ) )
		{
			if ( this.messageListPvp.isInitialized() )
			{
				this.messageListPvp.valueChanged( null );
			}

			requestMailbox = !this.messageListPvp.isInitialized();
		}
		else if ( currentTabName.equals( "Pen Pal" ) )
		{
			if ( this.messageListPenPal.isInitialized() )
			{
				this.messageListPenPal.valueChanged( null );
			}

			requestMailbox = !this.messageListPenPal.isInitialized();
		}
		else if ( currentTabName.equals( "Outbox" ) )
		{
			if ( this.messageListOutbox.isInitialized() )
			{
				this.messageListOutbox.valueChanged( null );
			}
			requestMailbox = !this.messageListOutbox.isInitialized();
		}
		else
		{
			if ( this.messageListSaved.isInitialized() )
			{
				this.messageListSaved.valueChanged( null );
			}
			requestMailbox = !this.messageListSaved.isInitialized();
		}

		if ( requestMailbox )
		{
			new MailRefresher( currentTabName ).run();
		}
	}

	private void refreshMailManager()
	{
		this.messageListInbox.setModel( MailManager.getMessages( "Inbox" ) );
		this.messageListPvp.setModel( MailManager.getMessages( "PvP" ) );
		this.messageListPenPal.setModel( MailManager.getMessages( "Pen Pal" ) );
		this.messageListOutbox.setModel( MailManager.getMessages( "Outbox" ) );
		this.messageListSaved.setModel( MailManager.getMessages( "Saved" ) );
	}

	private class MailRefresher
		implements Runnable
	{
		private final String mailboxName;
		private final MailboxRequest refresher;

		public MailRefresher( final String mailboxName )
		{
			this.mailboxName = mailboxName;
			this.refresher = new MailboxRequest( mailboxName );
		}

		public void run()
		{
			MailboxFrame.this.refreshMailManager();
			MailboxFrame.this.messageContent.setText( "Retrieving messages from server..." );

			RequestThread.postRequest( this.refresher );

			if ( this.mailboxName.equals( "Inbox" ) )
			{
				MailboxFrame.this.messageListInbox.setInitialized( true );
			}
			else if ( this.mailboxName.equals( "Outbox" ) )
			{
				MailboxFrame.this.messageListOutbox.setInitialized( true );
			}
			else if ( this.mailboxName.equals( "PvP" ) )
			{
				MailboxFrame.this.messageListPvp.setInitialized( true );
			}
			else if ( this.mailboxName.equals( "Pen Pal" ) )
			{
				MailboxFrame.this.messageListPenPal.setInitialized( true );
			}
			else if ( this.mailboxName.equals( "Saved" ) )
			{
				MailboxFrame.this.messageListSaved.setInitialized( true );
			}
		}
	}

	/**
	 * An internal class used to handle selection of a specific message from the mailbox list.
	 */

	private class MailSelectList
		extends JList
		implements ListSelectionListener
	{
		private final String mailboxName;
		private boolean initialized;

		public MailSelectList( final String mailboxName )
		{
			super( MailManager.getMessages( mailboxName ) );
			this.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			this.mailboxName = mailboxName;
			this.addListSelectionListener( this );
			this.addKeyListener( new MailboxKeyListener() );
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			if ( MailboxFrame.this.messageContent == null )
			{
				return;
			}

			int newIndex = this.getSelectedIndex();

			if ( newIndex >= 0 && this.getModel().getSize() > 0 )
			{
				MailboxFrame.this.displayed =
					(KoLMailMessage) MailManager.getMessages( this.mailboxName ).get( newIndex );

				MailboxFrame.this.messageContent.setText( MailboxFrame.this.displayed.getDisplayHTML() );
			}
		}

		private boolean isInitialized()
		{
			return this.initialized;
		}

		public void setInitialized( final boolean initialized )
		{
			this.initialized = initialized;
		}

		private class MailboxKeyListener
			extends KeyAdapter
		{
			public void keyReleased( final KeyEvent e )
			{
				if ( e.isConsumed() )
				{
					return;
				}

				if ( e.getKeyCode() != KeyEvent.VK_BACK_SPACE && e.getKeyCode() != KeyEvent.VK_DELETE )
				{
					return;
				}

				Object[] messages = MailSelectList.this.getSelectedValues();
				if ( messages.length == 0 )
				{
					return;
				}

				if ( !InputFieldUtilities.confirm( "Would you like to delete the selected messages?" ) )
				{
					return;
				}

				MailManager.deleteMessages( MailSelectList.this.mailboxName, messages );
				e.consume();
			}
		}
	}

	private class SaveAllButton
		extends ThreadedButton
	{
		private Object[] messages = null;

		public SaveAllButton()
		{
			super( JComponentUtilities.getImage( "saveall.gif" ) );
			this.setToolTipText( "Save Selected" );
		}

		public void run()
		{
			this.messages = null;
			String currentTabName = MailboxFrame.this.tabs.getTitleAt( MailboxFrame.this.tabs.getSelectedIndex() );

			if ( currentTabName.equals( "Inbox" ) )
			{
				this.messages = MailboxFrame.this.messageListInbox.getSelectedValues();
			}
			else if ( currentTabName.equals( "PvP" ) )
			{
				this.messages = MailboxFrame.this.messageListPvp.getSelectedValues();
			}
			else if ( currentTabName.equals( "Pen Pal" ) )
			{
				this.messages = MailboxFrame.this.messageListPenPal.getSelectedValues();
			}
			else if ( currentTabName.equals( "Outbox" ) )
			{
				this.messages = MailboxFrame.this.messageListOutbox.getSelectedValues();
			}

			if ( this.messages == null || this.messages.length == 0 )
			{
				return;
			}

			if ( !InputFieldUtilities.confirm( "Would you like to save the selected messages?" ) )
			{
				return;
			}

			MailManager.saveMessages( currentTabName, this.messages );
		}
	}

	private class DeleteButton
		extends ThreadedButton
	{
		private String currentTabName = null;
		private Object[] messages = null;

		public DeleteButton()
		{
			super( JComponentUtilities.getImage( "delete.gif" ) );
			this.setToolTipText( "Delete Selected" );
		}

		public void run()
		{
			this.messages = null;
			this.currentTabName = MailboxFrame.this.tabs.getTitleAt( MailboxFrame.this.tabs.getSelectedIndex() );
			if ( this.currentTabName.equals( "Inbox" ) )
			{
				this.messages = MailboxFrame.this.messageListInbox.getSelectedValues();
			}
			else if ( this.currentTabName.equals( "PvP" ) )
			{
				this.messages = MailboxFrame.this.messageListPvp.getSelectedValues();
			}
			else if ( this.currentTabName.equals( "Pen Pal" ) )
			{
				this.messages = MailboxFrame.this.messageListPenPal.getSelectedValues();
			}
			else if ( this.currentTabName.equals( "Outbox" ) )
			{
				this.messages = MailboxFrame.this.messageListOutbox.getSelectedValues();
			}
			else if ( this.currentTabName.equals( "Saved" ) )
			{
				this.messages = MailboxFrame.this.messageListSaved.getSelectedValues();
			}

			if ( this.messages == null || this.messages.length == 0 )
			{
				return;
			}

			if ( !InputFieldUtilities.confirm( "Would you like to delete the selected messages?" ) )
			{
				return;
			}

			MailManager.deleteMessages( this.currentTabName, this.messages );
		}
	}

	private class RefreshButton
		extends ThreadedButton
	{
		public RefreshButton()
		{
			super( JComponentUtilities.getImage( "refresh.gif" ) );
			this.setToolTipText( "Refresh" );
		}

		public void run()
		{
			String currentTabName = MailboxFrame.this.tabs.getTitleAt( MailboxFrame.this.tabs.getSelectedIndex() );
			new MailRefresher( currentTabName ).run();
		}
	}

	/**
	 * Action listener responsible for opening a sendmessage frame when reply or quote is clicked or opening a frame in
	 * the browser when something else is clicked.
	 */

	private class MailLinkClickedListener
		extends HyperlinkAdapter
	{
		public void handleInternalLink( final String location )
		{
			// If you click on the player name:
			//     showplayer.php?who=<playerid>

			if ( !location.startsWith( "sendmessage.php" ) )
			{
				StaticEntity.openRequestFrame( location );
				return;
			}

			// If you click on [reply]:
			//     sendmessage.php?toid=<playerid>
			// If you click on [quote]:
			//     sendmessage.php?toid=<playerid>&quoteid=xxx&box=xxx

			StringTokenizer tokens = new StringTokenizer( location, "?=&" );
			tokens.nextToken();
			tokens.nextToken();

			String recipient = tokens.nextToken();

			Object[] parameters = new Object[ tokens.hasMoreTokens() ? 2 : 1 ];
			parameters[ 0 ] = recipient;

			if ( parameters.length == 2 )
			{
				String text = MailboxFrame.this.displayed.getMessageHTML();

				// Replace <br> tags with a line break and
				// quote the following line

				text = text.replaceAll( "<br>", KoLConstants.LINE_BREAK + "> " );

				// Remove all other HTML tags

				text = text.replaceAll( "><", "" ).replaceAll( "<.*?>", "" );

				// Quote first line and end with a line break

				text = "> " + text + KoLConstants.LINE_BREAK;

				parameters[ 1 ] = text;
			}

			GenericFrame.createDisplay( SendMessageFrame.class, parameters );
		}
	}
}
