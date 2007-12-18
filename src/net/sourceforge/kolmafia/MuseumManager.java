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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class MuseumManager
	extends StaticEntity
{
	private static final KoLRequest SHELF_REORDER = new KoLRequest( "managecollection.php" );

	private static final Pattern SELECTED_PATTERN = Pattern.compile( "(\\d+) selected>" );
	private static final Pattern OPTION_PATTERN =
		Pattern.compile( "<td>([^<]*?)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<select name=whichshelf(\\d+)>(.*?)</select>" );
	private static final Pattern SELECT_PATTERN = Pattern.compile( "<select.*?</select>" );
	private static final Pattern SHELF_PATTERN = Pattern.compile( "<option value=(\\d+).*?>(.*?)</option>" );

	private static final LockableListModel headers = new LockableListModel();
	private static final LockableListModel shelves = new LockableListModel();

	public static final void clearCache()
	{
		KoLConstants.collection.clear();
		MuseumManager.headers.clear();
		MuseumManager.shelves.clear();
	}

	public static final LockableListModel getHeaders()
	{
		return MuseumManager.headers;
	}

	public static final String getHeader( final int shelf )
	{
		return (String) MuseumManager.headers.get( shelf );
	}

	public static final LockableListModel getShelves()
	{
		return MuseumManager.shelves;
	}

	public static final void move( final Object[] moving, final int sourceShelf, final int destinationShelf )
	{
		// In order to take advantage of the utilities of the
		// Collections interface, place everything inside of
		// a list first.

		List movingList = new ArrayList();
		for ( int i = 0; i < moving.length; ++i )
		{
			movingList.add( moving[ i ] );
		}

		// Use the removeAll() and addAll() methods inside of
		// the Collections interface.

		( (SortedListModel) MuseumManager.shelves.get( sourceShelf ) ).removeAll( movingList );
		( (SortedListModel) MuseumManager.shelves.get( destinationShelf ) ).addAll( movingList );

		// Save the lists to the server and update the display
		// on theto reflect the change.

		RequestThread.openRequestSequence();
		MuseumManager.save( MuseumManager.shelves );

		KoLmafia.updateDisplay( "Display case updated." );
		RequestThread.closeRequestSequence();
	}

	public static final void reorder( final String[] headers )
	{
		headers[ 0 ] = "-none-";

		// Unfortunately, if there are deleted shelves, the
		// shelves cannot be re-ordered directly.  What has
		// to happen is that the number of deleted shelves
		// needs to be created with some dummy name and then
		// deleted afterwards.

		boolean containsDeletedShelf = false;
		boolean[] deleted = new boolean[ headers.length ];

		for ( int i = 0; i < headers.length; ++i )
		{
			deleted[ i ] = headers[ i ].equals( "(Deleted Shelf)" );
			containsDeletedShelf |= deleted[ i ];
		}

		RequestThread.openRequestSequence();

		for ( int i = 0; i < deleted.length; ++i )
		{
			if ( deleted[ i ] )
			{
				MuseumManager.SHELF_REORDER.addFormField( "action", "newshelf" );
				MuseumManager.SHELF_REORDER.addFormField( "pwd" );
				MuseumManager.SHELF_REORDER.addFormField( "shelfname", "Deleted Shelf " + i );
				RequestThread.postRequest( MuseumManager.SHELF_REORDER );
			}
		}

		// Determine where the headers are in the existing
		// list of headers to find out where the shelf contents
		// should be stored after the update.

		List shelforder = new ArrayList();
		for ( int i = 0; i < headers.length; ++i )
		{
			shelforder.add( MuseumManager.shelves.get( MuseumManager.headers.indexOf( headers[ i ] ) ) );
		}

		// Save the lists to the server and update the display
		// on theto reflect the change.

		MuseumManager.save( shelforder );

		// Redelete the previously deleted shelves so that the
		// user isn't stuck with shelves they aren't going to use.

		MuseumManager.SHELF_REORDER.clearDataFields();
		MuseumManager.SHELF_REORDER.addFormField( "action", "modifyshelves" );
		MuseumManager.SHELF_REORDER.addFormField( "pwd" );

		for ( int i = 1; i < headers.length; ++i )
		{
			MuseumManager.SHELF_REORDER.addFormField( "newname" + i, headers[ i ] );
			if ( deleted[ i ] )
			{
				MuseumManager.SHELF_REORDER.addFormField( "delete" + i, "on" );
			}
		}

		RequestThread.postRequest( MuseumManager.SHELF_REORDER );
		RequestThread.postRequest( new MuseumRequest() );

		KoLmafia.updateDisplay( "Display case updated." );
		RequestThread.closeRequestSequence();
	}

	private static final void save( final List shelfOrder )
	{
		int elementCounter = 0;
		SortedListModel currentShelf;

		// In order to ensure that all data is saved with no
		// glitches server side, all items submit their state.
		// Store the data in two parallel arrays.

		int size = KoLConstants.collection.size();
		int[] newShelves = new int[ size ];
		AdventureResult[] newItems = new AdventureResult[ size ];

		// Iterate through each shelf and place the item into
		// the parallel arrays.

		for ( int i = 0; i < shelfOrder.size(); ++i )
		{
			currentShelf = (SortedListModel) shelfOrder.get( i );
			for ( int j = 0; j < currentShelf.size(); ++j, ++elementCounter )
			{
				newShelves[ elementCounter ] = i;
				newItems[ elementCounter ] = (AdventureResult) currentShelf.get( j );
			}
		}

		// Once the parallel arrays are properly initialized,
		// send the update request to the server.

		RequestThread.postRequest( new MuseumRequest( newItems, newShelves ) );
	}

	public static final void update( final String data )
	{
		MuseumManager.updateShelves( data );
		Matcher selectedMatcher;

		int itemId, itemCount;
		String[] itemString;

		Matcher optionMatcher = MuseumManager.OPTION_PATTERN.matcher( data );
		while ( optionMatcher.find() )
		{
			selectedMatcher = MuseumManager.SELECTED_PATTERN.matcher( optionMatcher.group( 3 ) );

			itemId = StaticEntity.parseInt( optionMatcher.group( 2 ) );

			itemString = optionMatcher.group( 1 ).split( "[\\(\\)]" );
			if ( TradeableItemDatabase.getItemName( itemId ) == null )
			{
				TradeableItemDatabase.registerItem( itemId, itemString[ 0 ].trim() );
			}

			itemCount = itemString.length == 1 ? 1 : StaticEntity.parseInt( itemString[ 1 ] );

			MuseumManager.registerItem(
				new AdventureResult( itemId, itemCount ),
				selectedMatcher.find() ? StaticEntity.parseInt( selectedMatcher.group( 1 ) ) : 0 );
		}
	}

	private static final void registerItem( final AdventureResult item, final int shelf )
	{
		KoLConstants.collection.add( item );
		( (SortedListModel) MuseumManager.shelves.get( shelf ) ).add( item );
	}

	private static final void updateShelves( final String data )
	{
		MuseumManager.clearCache();

		Matcher selectMatcher = MuseumManager.SELECT_PATTERN.matcher( data );
		if ( selectMatcher.find() )
		{
			int currentShelf;
			Matcher shelfMatcher = MuseumManager.SHELF_PATTERN.matcher( selectMatcher.group() );
			while ( shelfMatcher.find() )
			{
				currentShelf = StaticEntity.parseInt( shelfMatcher.group( 1 ) );

				for ( int i = MuseumManager.headers.size(); i < currentShelf; ++i )
				{
					MuseumManager.headers.add( "(Deleted Shelf)" );
				}

				MuseumManager.headers.add( RequestEditorKit.getUnicode( shelfMatcher.group( 2 ) ) );
			}
		}

		if ( MuseumManager.headers.size() == 0 )
		{
			MuseumManager.headers.add( "" );
		}

		MuseumManager.headers.set( 0, "-none-" );
		for ( int i = 0; i < MuseumManager.headers.size(); ++i )
		{
			MuseumManager.shelves.add( new SortedListModel() );
		}
	}
}
