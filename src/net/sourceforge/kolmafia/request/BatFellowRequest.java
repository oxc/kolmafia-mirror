/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class BatFellowRequest
	extends GenericRequest
{
	public BatFellowRequest()
	{
		super( "place.php" );
	}

	private static final AdventureResult[] ITEMS =
	{
		// Raw materials for Bat-Fabricator
		ItemPool.get( ItemPool.HIGH_GRADE_METAL, 1 ),
		ItemPool.get( ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1 ),
		ItemPool.get( ItemPool.HIGH_GRADE_EXPLOSIVES, 1 ),

		// Items from Bat-Fabricator
		ItemPool.get( ItemPool.BAT_OOMERANG, 1 ),
		ItemPool.get( ItemPool.BAT_JUTE, 1 ),
		ItemPool.get( ItemPool.BAT_O_MITE, 1 ),

		// Currency & items from Orphanage
		ItemPool.get( ItemPool.KIDNAPPED_ORPHAN, 1 ),
		ItemPool.get( ItemPool.CONFIDENCE_BUILDING_HUG, 1 ),
		ItemPool.get( ItemPool.EXPLODING_KICKBALL, 1 ),

		// Currency & items from ChemiCorp
		ItemPool.get( ItemPool.DANGEROUS_CHEMICALS, 1 ),
		ItemPool.get( ItemPool.EXPERIMENTAL_GENE_THERAPY, 1 ),
		ItemPool.get( ItemPool.ULTRACOAGULATOR, 1 ),

		// Currency & items from GotPork P.D.
		ItemPool.get( ItemPool.INCRIMINATING_EVIDENCE, 1 ),
		ItemPool.get( ItemPool.SELF_DEFENSE_TRAINING, 1 ),
		ItemPool.get( ItemPool.FINGERPRINT_DUSTING_KIT, 1 ),

		// Bat-Suit upgrade
		ItemPool.get( ItemPool.BAT_AID_BANDAGE, 1 ),

		// Bat-Sedan upgrade
		ItemPool.get( ItemPool.BAT_BEARING, 1 ),

		// Bat-Cavern upgrade
		ItemPool.get( ItemPool.GLOB_OF_BAT_GLUE, 1 ),
	};

	public static void begin()
	{
		// Preferences.resetToDefault( "batFellowStatus" );

		// Clean up inventory
		BatFellowRequest.resetItems();

		// Add items that you begin with
		ResultProcessor.processItem( ItemPool.BAT_OOMERANG, 1 );
		ResultProcessor.processItem( ItemPool.BAT_JUTE, 1 );
		ResultProcessor.processItem( ItemPool.BAT_O_MITE, 1 );
	}

	public static void end()
	{
		BatFellowRequest.resetItems();
	}

	private static void resetItems()
	{
		for ( AdventureResult item : BatFellowRequest.ITEMS )
		{
			int count = item.getCount( KoLConstants.inventory );
			if ( count > 0 )
			{
				AdventureResult result = item.getInstance( -count );
				AdventureResult.addResultToList( KoLConstants.inventory, result );
				AdventureResult.addResultToList( KoLConstants.tally, result );
			}
		}
	}
	public static void parseCharpane( final String responseText )
	{
		if ( !responseText.contains( "You're Batfellow" ) )
		{
			return;
		}
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichplace=batman" ) )
		{
			return;
		}
	}

	public static int getTimeLeft()
	{
		// Return minutes left: 0 - 600
		return 600;
	}

	public static String getTimeLeftString()
	{
		int minutes = BatFellowRequest.getTimeLeft();
		StringBuilder buffer = new StringBuilder();
		int hours = minutes / 60;
		if ( hours > 0 )
		{
			buffer.append( String.valueOf( hours ) );
			buffer.append( " h. " );
			minutes = minutes % 60;
		}
		buffer.append( String.valueOf( minutes ) );
		buffer.append( " m." );
		return buffer.toString();
	}

	public static boolean registerRequest( final String urlString )
	{
		// place.php?whichplace=batman_xxx

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		String location = null;

		// whichplace=batman_cave

		if ( action.equals( "batman_cave_rnd" ) )
		{
			location = "Bat-Research and Bat-Development";
		}
		else if ( action.equals( "batman_cave_car" ) )
		{
			location = "The Bat-Sedan";
		}

		// whichplace=batman_downtown

		else if ( action.equals( "batman_downtown_hospital" ) )
		{
			location = "The Bat-Sedan";
		}
		else if ( action.equals( "batman_downtown_car" ) )
		{
			location = "The Bat-Sedan";
		}

		// whichplace=batman_park

		else if ( action.equals( "batman_park_car" ) )
		{
			location = "The Bat-Sedan";
		}

		// whichplace=batman_slums

		else if ( action.equals( "batman_slums_car" ) )
		{
			location = "The Bat-Sedan";
		}

		// whichplace=batman_industrial

		else if ( action.equals( "batman_industrial_car" ) )
		{
			location = "The Bat-Sedan";
		}

		else
		{
			return false;
		}

		String message = message = "{" + BatFellowRequest.getTimeLeftString() + "} " + location;

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
