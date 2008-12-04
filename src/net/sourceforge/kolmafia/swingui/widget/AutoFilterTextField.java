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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JList;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class AutoFilterTextField
	extends AutoHighlightTextField
	implements ActionListener, ListElementFilter
{
	protected JList list;
	protected String text;
	protected LockableListModel model;
	protected boolean strict;

	public AutoFilterTextField( final JList list )
	{
		this.list = list;
		this.model = (LockableListModel) list.getModel();

		this.model.setFilter( this );
		this.addKeyListener( new FilterListener() );
		
		// Make this look like a normal search field on OS X:
		// Unfortunately, has nasty visual glitch in AdventureSelectPanel.
		//this.putClientProperty( "JTextField.variant", "search" );
	}

	public AutoFilterTextField( final JList list, Object initial )
	{
		this( list );

		if ( initial != null )
		{
			this.setText( initial.toString() );
		}
	}

	public void actionPerformed( final ActionEvent e )
	{
		this.update();
	}

	public void setText( final String text )
	{
		super.setText( text );
		this.update();
	}

	public void update()
	{
		AutoFilterTextField.this.text = AutoFilterTextField.this.getText().toLowerCase();

		AutoFilterTextField.this.strict = true;
		AutoFilterTextField.this.model.updateFilter( false );

		if ( AutoFilterTextField.this.model.getSize() == 0 )
		{
			AutoFilterTextField.this.strict = false;
			AutoFilterTextField.this.model.updateFilter( false );
		}

		if ( this.model.getSize() == 1 )
		{
			this.list.setSelectedIndex( 0 );
		}
		else
		{
			this.list.clearSelection();
		}
	}

	public boolean isVisible( final Object element )
	{
		if ( this.text == null || this.text.length() == 0 )
		{
			return true;
		}

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		String elementName = AutoFilterTextField.getResultName( element );

		if ( this.text == null || this.text.length() == 0 )
		{
			return true;
		}

		return this.strict ? elementName.indexOf( this.text ) != -1 :
			StringUtilities.fuzzyMatches( elementName, this.text );
	}

	public static final String getResultName( final Object element )
	{
		if ( element == null )
		{
			return "";
		}

		if ( element instanceof AdventureResult )
		{
			return ( (AdventureResult) element ).getName().toLowerCase();
		}
		if ( element instanceof CreateItemRequest )
		{
			return ( (CreateItemRequest) element ).getName().toLowerCase();
		}
		if ( element instanceof Concoction )
		{
			return ( (Concoction) element ).getName().toLowerCase();
		}
		if ( element instanceof SoldItem )
		{
			return ( (SoldItem) element ).getItemName().toLowerCase();
		}
		if ( element instanceof LowerCaseEntry )
		{
			return ( (LowerCaseEntry) element ).getLowerCase();
		}
		if ( element instanceof KoLAdventure )
		{
			return ( (KoLAdventure) element ).toLowerCaseString();
		}

		return element.toString();
	}

	private class FilterListener
		extends KeyAdapter
	{
		public void keyReleased( final KeyEvent e )
		{
			AutoFilterTextField.this.update();
		}
	}
}
