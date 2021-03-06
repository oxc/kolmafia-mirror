/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JComboBox;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
/**
 * Maintains a most recently used list of scripts
 * @author Fronobulax
 */
public class ScriptMRUList
{
	protected int maxMRU = 16;
	protected final LinkedList<String> mruList = new LinkedList<>();
	protected boolean isInit;
	private final String prefList;
	private final String prefLen;
	private static final String SEMICOLON = ";";

	public ScriptMRUList( String pList, String pLen )
	{
		isInit = false;
		prefList = pList;
		prefLen = pLen;
	}

	protected void init()
	{
		maxMRU = Preferences.getInteger( prefLen );
		if ( maxMRU > 0 )
		{
			// Load list from preference - use whatever is there
			String oldValues = Preferences.getString( prefList );
			if ( ( oldValues != null ) && ( !oldValues.equals( "" ) ) )
			{
				// First to last, delimited by semi-colon.  Split and insert.
				String[] items = oldValues.split( SEMICOLON );
				for ( int i = ( items.length - 1 ); i >= 0; i-- )
				{
					mruList.addFirst( items[i] );
				}
			}
			while ( mruList.size() > maxMRU )
			{
				mruList.removeLast();
			}
			isInit = true;
		}
	}

	public void addItem(String script)
	{
		// Initialize list, if needed
		if ( !isInit )
		{
			init();
		}
		if ( !isInit )
		{
			return;
		}
		// don't add empty or null names
		if ( ( script != null ) && ( !script.equals( "" ) ) )
		{
			// delete item if it is currently in list
			// note - as implemented this is a case sensitive compare
			while ( mruList.contains( script ) )
			{
				mruList.remove( script );
			}
			// add this as the first
			mruList.addFirst( script );
			// delete excess
			while ( mruList.size() > maxMRU )
			{
				mruList.removeLast();
			}
			// save the new list as a preference
			Iterator<String> i8r = mruList.iterator();
			StringBuilder pref = new StringBuilder();
			while (i8r.hasNext())
			{
				String val = i8r.next();
				pref.append(val);
				if (i8r.hasNext())
				{
					pref.append(SEMICOLON);
				}
			}
			// now save it
			Preferences.setString( KoLCharacter.getUserName(), prefList, pref.toString() );
		}
	}
	
	public File[] listAsFiles()
	{
		if ( !isInit )
		{
			init();
		}
		int count = mruList.size();
		if ( count < 1 )
		{
			return new File[ 0 ];
		}
		File [] result = new File [count];
		Iterator<String> i8r = mruList.iterator();
		int i = 0;
		while (i8r.hasNext())
		{
			String val = i8r.next();
			result[i] = new File( val );
			i++;
		}
		return result;
	}
	
	public void updateJComboData( JComboBox<Object> jcb)
	{
		if ( !isInit )
		{
			init();
		}
		int count = mruList.size();
		if ( count >= 1 )
		{
			jcb.removeAllItems();
			Iterator<String> i8r = mruList.iterator();
			int i = 0;
			while (i8r.hasNext())
			{
				String val = i8r.next();
				jcb.insertItemAt(val, i);
				i++;
			}
			jcb.setSelectedIndex( 0 );
		}
	}

	public String getFirst()
	{
		String NONE = "Unknown";
		if ( !isInit ) return NONE;
		if ( maxMRU <= 0 ) return NONE;
		if ( mruList.size() < 1 ) return NONE;
		return mruList.getFirst();
	}
}
