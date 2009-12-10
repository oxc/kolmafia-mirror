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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpecialOutfit
	implements Comparable
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option value=[\'\"]?(.*?)[\'\"]?>(.*?)</option>" );

	private static final Stack implicitPoints = new Stack();
	private static final Stack explicitPoints = new Stack();

	private final int outfitId;
	private final String outfitName;
	private final ArrayList pieces;
	private int hash;

	public static final String NO_CHANGE = " - No Change - ";
	public static final SpecialOutfit BIRTHDAY_SUIT = new SpecialOutfit();

	private static SpecialOutfit implicitOutfit = null;
	private static int markedCheckpoint = -1;

	private SpecialOutfit()
	{
		this.outfitId = Integer.MAX_VALUE;
		this.outfitName = "Birthday Suit";
		this.pieces = new ArrayList();
		this.hash = 0;
	}

	public SpecialOutfit( final int outfitId, final String outfitName )
	{
		this.outfitId = outfitId;
		// The name is normally a substring of the equipment page,
		// and would keep that entire page in memory if not copied.
		this.outfitName = new String( outfitName );
		this.pieces = new ArrayList();
		this.hash = 0;
	}

	public boolean hasAllPieces()
	{
		for ( int i = 0; i < this.pieces.size(); ++i )
		{
			if ( !InventoryManager.hasItem( (AdventureResult) this.pieces.get( i ) ) )
			{
				return false;
			}

			if ( !EquipmentManager.canEquip( ( (AdventureResult) this.pieces.get( i ) ).getName() ) )
			{
				return false;
			}
		}

		return true;
	}

	public boolean isWearing()
	{
		return this.isWearing( -1 );
	}

	public boolean isWearing( int hash )
	{
		if ( (hash & this.hash) != this.hash ) return false;

		for ( int i = 0; i < this.pieces.size(); ++i )
		{
			if ( !KoLCharacter.hasEquipped( (AdventureResult) this.pieces.get( i ) ) )
			{
				return false;
			}
		}

		return true;
	}

	public boolean isWearing( AdventureResult[] equipment )
	{
		return this.isWearing( equipment, -1 );
	}

	public boolean isWearing( AdventureResult[] equipment, int hash )
	{
		if ( (hash & this.hash) != this.hash ) return false;

		for ( int i = 0; i < this.pieces.size(); ++i )
		{
			if ( !KoLCharacter.hasEquipped( equipment,
				(AdventureResult) this.pieces.get( i ) ) )
			{
				return false;
			}
		}

		return true;
	}

	public AdventureResult[] getPieces()
	{
		AdventureResult[] piecesArray = new AdventureResult[ this.pieces.size() ];
		this.pieces.toArray( piecesArray );
		return piecesArray;
	}

	public static int pieceHash( final AdventureResult piece )
	{
		if ( piece == null || piece == EquipmentRequest.UNEQUIP )
		{
			return 0;
		}
		return 1 << ( piece.getItemId() & 0x1F );
	}

	public static int equipmentHash( AdventureResult[] equipment )
	{
		int hash = 0;
		// Must consider every slot that can contain an outfit piece
		for ( int i = 0; i < EquipmentManager.FAMILIAR; ++i )
		{
			hash |= SpecialOutfit.pieceHash( equipment[ i ] );
		}
		return hash;
	}

	public void addPiece( final AdventureResult piece )
	{
		this.pieces.add( piece );
		this.hash |= this.pieceHash( piece );
	}

	public String toString()
	{
		return this.outfitName;
	}

	public int getOutfitId()
	{
		return this.outfitId;
	}

	public String getName()
	{
		return this.outfitName;
	}

	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof SpecialOutfit ) )
		{
			return false;
		}

		if ( this.outfitId != ( (SpecialOutfit) o ).outfitId )
		{
			return false;
		}

		return this.outfitName.equalsIgnoreCase( ( (SpecialOutfit) o ).outfitName );
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof SpecialOutfit ) )
		{
			return -1;
		}

		return this.outfitName.compareToIgnoreCase( ( (SpecialOutfit) o ).outfitName );
	}

	/**
	 * Restores a checkpoint. This should be called whenever the player needs to revert to their checkpointed outfit.
	 */

	private static final void restoreCheckpoint( final AdventureResult[] checkpoint )
	{
		RequestThread.openRequestSequence();

		AdventureResult equippedItem;
		for ( int i = 0; i < checkpoint.length && !KoLmafia.refusesContinue(); ++i )
		{
			if ( checkpoint[ i ] == null )
			{
				continue;
			}

			equippedItem = EquipmentManager.getEquipment( i );
			if ( checkpoint[ i ].equals( EquipmentRequest.UNEQUIP ) || equippedItem.equals( checkpoint[ i ] ) )
			{
				continue;
			}

			RequestThread.postRequest( new EquipmentRequest( checkpoint[ i ], i ) );
		}

		RequestThread.closeRequestSequence();
	}

	/**
	 * Creates a checkpoint. This should be called whenever the player needs an outfit marked to revert to.
	 */

	public static final void createExplicitCheckpoint()
	{
		AdventureResult[] explicit = new AdventureResult[ EquipmentManager.SLOTS ];

		for ( int i = 0; i < explicit.length; ++i )
		{
			explicit[ i ] = EquipmentManager.getEquipment( i );
		}

		SpecialOutfit.explicitPoints.push( explicit );
	}

	/**
	 * Restores a checkpoint. This should be called whenever the player needs to revert to their checkpointed outfit.
	 */

	public static final void restoreExplicitCheckpoint()
	{
		if ( SpecialOutfit.explicitPoints.isEmpty() )
		{
			return;
		}

		SpecialOutfit.restoreCheckpoint( (AdventureResult[]) SpecialOutfit.explicitPoints.pop() );
	}

	/**
	 * Creates a checkpoint. This should be called whenever the player needs an outfit marked to revert to.
	 */

	public static final void createImplicitCheckpoint()
	{
		synchronized ( SpecialOutfit.class )
		{
			AdventureResult[] implicit = new AdventureResult[ EquipmentManager.SLOTS ];

			for ( int i = 0; i < implicit.length; ++i )
			{
				implicit[ i ] = EquipmentManager.getEquipment( i );
			}

			SpecialOutfit.implicitPoints.push( implicit );
			EquipmentRequest.savePreviousOutfit();
		}
	}

	/**
	 * Restores a checkpoint. This should be called whenever the player needs to revert to their checkpointed outfit.
	 */

	public static final void restoreImplicitCheckpoint()
	{
		if ( SpecialOutfit.implicitPoints.isEmpty() )
		{
			return;
		}

		AdventureResult[] implicit = (AdventureResult[]) SpecialOutfit.implicitPoints.pop();

		if ( SpecialOutfit.implicitPoints.size() < SpecialOutfit.markedCheckpoint )
		{
			RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.implicitOutfit ) );
			SpecialOutfit.markedCheckpoint = -1;
		}
		else if ( SpecialOutfit.markedCheckpoint == -1 )
		{
			SpecialOutfit.restoreCheckpoint( implicit );
		}
	}

	public static final boolean markImplicitCheckpoint()
	{
		if ( SpecialOutfit.markedCheckpoint != -1 || SpecialOutfit.implicitPoints.isEmpty() )
		{
			return false;
		}

		SpecialOutfit.markedCheckpoint = SpecialOutfit.implicitPoints.size();
		return true;
	}

	/**
	 * static final method used to determine all of the custom outfits, based on the given HTML enclosed in
	 * <code><select></code> tags.
	 *
	 * @return A list of available outfits
	 */

	public static final LockableListModel parseOutfits( final String selectHTML )
	{
		Matcher singleOutfitMatcher = SpecialOutfit.OPTION_PATTERN.matcher( selectHTML );

		int outfitId;
		String outfitName;
		SpecialOutfit outfit;

		SpecialOutfit.implicitOutfit = null;
		SortedListModel outfits = new SortedListModel();

		while ( singleOutfitMatcher.find() )
		{
			outfitId = StringUtilities.parseInt( singleOutfitMatcher.group( 1 ) );
			if ( outfitId < 0 )
			{
				outfitName = singleOutfitMatcher.group( 2 );
				outfit = new SpecialOutfit( outfitId, singleOutfitMatcher.group( 2 ) );

				if ( outfitName.equals( "Backup" ) )
				{
					SpecialOutfit.implicitOutfit = outfit;
				}

				outfits.add( outfit );
			}
		}

		return outfits;
	}

	/**
	 * Method to replace a particular piece of equipment in all active checkpoints,
	 * after it has been transformed or consumed.
	 */

	public static final void forgetEquipment( AdventureResult item )
	{
		AdventureResult[] checkpoint;
		for ( int i = SpecialOutfit.implicitPoints.size() - 1; i >= 0; --i )
		{
			checkpoint = (AdventureResult[]) SpecialOutfit.implicitPoints.get( i );
			for ( int j = 0; j < checkpoint.length; ++j )
			{
				if ( item.equals( checkpoint[ j ] ) )
				{
					checkpoint[ j ] = EquipmentRequest.UNEQUIP;
				}
			}
		}
		for ( int i = SpecialOutfit.explicitPoints.size() - 1; i >= 0; --i )
		{
			checkpoint = (AdventureResult[]) SpecialOutfit.explicitPoints.get( i );
			for ( int j = 0; j < checkpoint.length; ++j )
			{
				if ( item.equals( checkpoint[ j ] ) )
				{
					checkpoint[ j ] = EquipmentRequest.UNEQUIP;
				}
			}
		}
	}

	/**
	 * Method to remove all active checkpoints, in cases where restoring the original
	 * outfit is undesirable (currently, Fernswarthy's Basement).
	 */

	public static final void forgetCheckpoints()
	{
		SpecialOutfit.implicitPoints.clear();
		SpecialOutfit.explicitPoints.clear();
	}
}
