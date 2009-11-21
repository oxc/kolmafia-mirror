/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.awt.event.MouseEvent;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import tab.CloseListener;
import tab.CloseTabPaneUI;
import tab.CloseTabbedPane;

import com.sun.java.forums.CloseableTabbedPane;
import com.sun.java.forums.CloseableTabbedPaneListener;


import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.persistence.Preferences;

public class TabbedChatFrame
	extends ChatFrame
	implements CloseListener, CloseableTabbedPaneListener
{
	public TabbedChatFrame()
	{
		this.setTitle( "Loathing Chat" );
	}

	public JTabbedPane getTabbedPane()
	{
		return Preferences.getBoolean( "useShinyTabbedChat" ) ? (JTabbedPane) new CloseTabbedPane() : (JTabbedPane) new CloseableTabbedPane();
	}

	/**
	 * Utility method called to initialize the frame. This method should be overridden, should a different means of
	 * initializing the content of the frame be needed.
	 */

	public void initialize( final String associatedContact )
	{
		if ( this.tabs instanceof CloseTabbedPane )
		{
			( (CloseTabbedPane) this.tabs ).setCloseIconStyle( CloseTabPaneUI.GRAY_CLOSE_ICON );
			( (CloseTabbedPane) this.tabs ).addCloseListener( this );
		}
		else
		{
			( (CloseableTabbedPane) this.tabs ).addCloseableTabbedPaneListener( this );
		}

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
	}

	public boolean closeTab( final int tabIndexToClose )
	{
		if ( tabIndexToClose == -1 )
		{
			return false;
		}

		String contact = this.tabs.getTitleAt( tabIndexToClose );

		if ( contact == null )
		{
			ChatManager.dispose();
		}
		else if ( contact.startsWith( "/" ) )
		{
			ChatManager.closeWindow( contact );
		}
		
		return true;
	}

	public void closeOperation( final MouseEvent e, final int overTabIndex )
	{
		if ( this.closeTab( overTabIndex ) )
		{
			this.tabs.removeTabAt( overTabIndex );
		}
	}

	/**
	 * Adds a new tab to represent the given name. Note that this will not shift tab focus; however, if it is the first
	 * tab added, the name of the contact will be reset.
	 */

	public void addTab( final String tabName )
	{
		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			if ( this.tabs.getTitleAt( i ).trim().equals( tabName ) )
			{
				return;
			}
		}

		try
		{
			TabAdder add = new TabAdder( tabName );

			if ( SwingUtilities.isEventDispatchThread() )
			{
				add.run();
			}
			else
			{
				SwingUtilities.invokeAndWait( add );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  However, skip it
			// since nothing bad really happened.
		}
	}

	public void highlightTab( final String tabName )
	{
		if ( tabName == null )
		{
			return;
		}

		for ( int i = 0; i < this.tabs.getTabCount(); ++i )
		{
			if ( tabName.equals( this.tabs.getTitleAt( i ).trim() ) )
			{
				SwingUtilities.invokeLater( new TabHighlighter( i ) );
				return;
			}
		}
	}

	public void dispose()
	{
		ChatManager.dispose();

		for ( int i = this.tabs.getTabCount() - 1; i >= 0; --i )
		{
			this.closeOperation( null, i );
		}

		super.dispose();
	}

	private class TabAdder
		implements Runnable
	{
		private final String tabName;
		private ChatPanel createdPanel;

		private TabAdder( final String tabName )
		{
			this.tabName = tabName;
		}

		public void run()
		{
			this.createdPanel = new ChatPanel( this.tabName );

			// Add a little bit of whitespace to make the
			// chat tab larger and easier to click.

			TabbedChatFrame.this.tabs.addTab( this.tabName, this.createdPanel );
			this.createdPanel.requestFocusInWindow();
		}
	}

	private class TabHighlighter
		implements Runnable
	{
		private final int tabIndex;

		public TabHighlighter( final int tabIndex )
		{
			this.tabIndex = tabIndex;
		}

		public void run()
		{
			if ( TabbedChatFrame.this.tabs.getSelectedIndex() == this.tabIndex )
			{
				return;
			}

			if ( TabbedChatFrame.this.tabs instanceof CloseTabbedPane )
			{
				( (CloseTabbedPane) TabbedChatFrame.this.tabs ).highlightTab( this.tabIndex );
			}
			else
			{
				( (CloseableTabbedPane) TabbedChatFrame.this.tabs ).highlightTab( this.tabIndex );
			}
		}
	}
}
