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

import java.awt.Component;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;

public class SimpleScrollPane extends JScrollPane implements KoLConstants
{
	public SimpleScrollPane( LockableListModel model )
	{	this( model, 8 );
	}

	public SimpleScrollPane( LockableListModel model, int visibleRows )
	{	this( new ShowDescriptionList( model, visibleRows ), VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER );
	}

	public SimpleScrollPane( Component view )
	{	this( view, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER );
	}

	public SimpleScrollPane( Component view, int hsbPolicy )
	{	this( view, VERTICAL_SCROLLBAR_ALWAYS, hsbPolicy );
	}

	public SimpleScrollPane( Component view, int vsbPolicy, int hsbPolicy )
	{
		super( view, vsbPolicy, hsbPolicy );
		setOpaque( true );

		if ( view instanceof JList )
		{
			if ( StaticEntity.getProperty( "swingLookAndFeel" ).equals( UIManager.getCrossPlatformLookAndFeelClassName() ) )
				((JList)view).setFont( DEFAULT_FONT );
		}
		else if ( !(view instanceof JTextComponent) )
			this.getVerticalScrollBar().setUnitIncrement( 30 );
	}
}
