/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class SpecialOutfit implements Comparable, KoLConstants
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option value=(.*?)>(.*?)</option>" );

	private static String lastCheckpoint = "";

	private static final AdventureResult [] IMPLICIT = new AdventureResult[ KoLCharacter.FAMILIAR ];
	private static final AdventureResult [] EXPLICIT = new AdventureResult[ KoLCharacter.FAMILIAR ];

	private int outfitId;
	private String outfitName;
	private ArrayList pieces;

	public static final String NO_CHANGE = " - No Change - ";
	public static final SpecialOutfit BIRTHDAY_SUIT = new SpecialOutfit();

	private SpecialOutfit()
	{
		this.outfitId = Integer.MAX_VALUE;
		this.outfitName = "Birthday Suit";
		this.pieces = new ArrayList();
	}

	public SpecialOutfit( int outfitId, String outfitName )
	{
		this.outfitId = outfitId;
		this.outfitName = outfitName;
		this.pieces = new ArrayList();
	}

	public boolean hasAllPieces()
	{
		for ( int i = 0; i < pieces.size(); ++i )
		{
			boolean itemAvailable = KoLCharacter.hasItem( (AdventureResult) pieces.get(i) ) &&
				EquipmentDatabase.canEquip( ((AdventureResult) pieces.get(i)).getName() );

			if ( !itemAvailable )
				return false;
		}

		return true;
	}

	public boolean isWearing()
	{
		for ( int i = 0; i < pieces.size(); ++i )
			if ( !KoLCharacter.hasEquipped( (AdventureResult) pieces.get(i) ) )
				return false;

		return true;
	}

	public String [] getPieces()
	{
		ArrayList piecesList = new ArrayList();
		for ( int i = 0; i < pieces.size(); ++i )
			piecesList.add( ((AdventureResult) pieces.get(i)).getName() );

		String [] piecesArray = new String[ piecesList.size() ];
		piecesList.toArray( piecesArray );
		return piecesArray;
	}


	public void addPiece( AdventureResult piece )
	{	this.pieces.add( piece );
	}

	public String toString()
	{	return outfitName;
	}

	public int getOutfitId()
	{	return outfitId;
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof SpecialOutfit && outfitId == ((SpecialOutfit)o).outfitId;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof SpecialOutfit) )
			return -1;

		return outfitName.compareToIgnoreCase( ((SpecialOutfit)o).outfitName );
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	private static void restoreCheckpoint( AdventureResult [] checkpoint )
	{
		if ( checkpoint[0] == null )
			return;

		if ( !checkpoint[KoLCharacter.OFFHAND].equals( KoLCharacter.getEquipment( KoLCharacter.OFFHAND ) ) )
			(new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.OFFHAND )).run();

		AdventureResult equippedItem;
		for ( int i = 0; i < checkpoint.length && lastCheckpoint.equals( KoLCharacter.getUserName() ); ++i )
		{
			equippedItem = KoLCharacter.getEquipment( i );
			if ( checkpoint[i] != null && !checkpoint[i].equals( EquipmentRequest.UNEQUIP ) && !equippedItem.equals( checkpoint[i] ) )
			{
				if ( equippedItem.equals( UseSkillRequest.ROCKNROLL_LEGEND ) && !checkpoint[i].equals( UseSkillRequest.ROCKNROLL_LEGEND ) )
				{
					UseSkillRequest.untinkerCloverWeapon( equippedItem );
					ItemCreationRequest.getInstance( checkpoint[i].getInstance(1) ).run();
				}

				(new EquipmentRequest( checkpoint[i], i )).run();
			}
		}
	}

	/**
	 * Creates a checkpoint.  This should be called whenever
	 * the player needs an outfit marked to revert to.
	 */

	public static void createExplicitCheckpoint()
	{
		for ( int i = 0; i < IMPLICIT.length; ++i )
			EXPLICIT[i] = KoLCharacter.getEquipment(i);

		SpecialOutfit.lastCheckpoint = KoLCharacter.getUserName();
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	public static void restoreExplicitCheckpoint()
	{	restoreCheckpoint( EXPLICIT );
	}

	/**
	 * Creates a checkpoint.  This should be called whenever
	 * the player needs an outfit marked to revert to.
	 */

	public static void createImplicitCheckpoint()
	{
		if ( IMPLICIT[0] != null )
			return;

		for ( int i = 0; i < IMPLICIT.length; ++i )
			IMPLICIT[i] = KoLCharacter.getEquipment(i);

		SpecialOutfit.lastCheckpoint = KoLCharacter.getUserName();
	}

	/**
	 * Clears a checkpoint.  This should be called whenever a
	 * checkpoint is no longer applicable.
	 */

	public static void clearImplicitCheckpoint()
	{
		for ( int i = 0; i < IMPLICIT.length; ++i )
			IMPLICIT[i] = null;
	}

	/**
	 * Restores a checkpoint.  This should be called whenever
	 * the player needs to revert to their checkpointed outfit.
	 */

	public static void restoreImplicitCheckpoint()
	{
		if ( KoLmafia.isRunningBetweenBattleChecks() )
			return;

		restoreCheckpoint( IMPLICIT );
		clearImplicitCheckpoint();
		return;
	}

	/**
	 * Static method used to determine all of the custom outfits,
	 * based on the given HTML enclosed in <code><select></code> tags.
	 *
	 * @return	A list of available outfits
	 */

	public static LockableListModel parseOutfits( String selectHTML )
	{
		Matcher singleOutfitMatcher = OPTION_PATTERN.matcher( selectHTML );

		int outfitId;
		SortedListModel outfits = new SortedListModel();

		while ( singleOutfitMatcher.find() )
		{
			outfitId = StaticEntity.parseInt( singleOutfitMatcher.group(1) );
			if ( outfitId < 0 )
				outfits.add( new SpecialOutfit( outfitId, singleOutfitMatcher.group(2) ) );
		}

		return outfits;
	}
}
