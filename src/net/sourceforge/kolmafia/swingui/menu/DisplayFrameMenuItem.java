/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.menu;

import java.lang.ref.WeakReference;

import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LicenseDisplay;

import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.panel.VersionDataPanel;

import net.sourceforge.kolmafia.persistence.Preferences;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this internal class is used
 * to process the request for viewing frames.
 */

public class DisplayFrameMenuItem
	extends ThreadedMenuItem
{
	private String frameClass;
	private WeakReference frameReference;

	public DisplayFrameMenuItem( final String title, final String frameClass )
	{
		super( title );
		this.frameClass = frameClass;
	}

	public DisplayFrameMenuItem( final GenericFrame frame )
	{
		super( frame == null ? "Show All Displays" : frame.toString() );

		this.frameClass = null;
		this.frameReference = frame == null ? null : new WeakReference( frame );
	}

	public void run()
	{
		if ( this.frameClass == null )
		{
			this.openExistingFrame();
		}
		else
		{
			this.openNewFrame();
		}
	}

	private void openNewFrame()
	{
		KoLmafiaGUI.constructFrame( this.frameClass );
	}

	private void openExistingFrame()
	{
		if ( this.frameReference == null )
		{
			GenericFrame[] frames = StaticEntity.getExistingFrames();
			String interfaceSetting = Preferences.getString( "initialDesktop" );

			for ( int i = 0; i < frames.length; ++i )
			{
				if ( interfaceSetting.indexOf( frames[ i ].getFrameName() ) == -1 )
				{
					frames[ i ].setVisible( true );
				}
			}

			if ( KoLDesktop.instanceExists() )
			{
				KoLDesktop.getInstance().setVisible( true );
			}

			return;
		}

		GenericFrame frame = (GenericFrame) this.frameReference.get();
		if ( frame != null )
		{
			boolean appearsInTab =
				GenericFrame.appearsInTab( frame instanceof ChatFrame ? "ChatManager" : frame.getFrameName() );

			if ( !appearsInTab )
			{
				frame.setVisible( true );
			}

			frame.requestFocus();
		}
	}
}