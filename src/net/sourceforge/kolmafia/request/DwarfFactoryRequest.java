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

package net.sourceforge.kolmafia.request;

import java.lang.CharSequence;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DwarfFactoryRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	public static final Pattern RUNE_PATTERN = Pattern.compile( "title=\"Dwarf (Digit|Word) Rune (.)\"" );
	public static final Pattern ITEMDESC_PATTERN = Pattern.compile( "descitem\\((\\d*)\\)" );
	public static final Pattern MEAT_PATTERN = Pattern.compile( "You (gain|lose) (\\d*) Meat" );
	public static final Pattern COLOR_PATTERN = Pattern.compile( "(red|orange|yellow|green|blue|indigo|violet)" );

	private static final int [] ITEMS = new int[]
	{
		ItemPool.SPRING,
		ItemPool.SPROCKET,
		ItemPool.COG,
		ItemPool.MINERS_HELMET,
		ItemPool.MINERS_PANTS,
		ItemPool.MATTOCK,
		ItemPool.LINOLEUM_ORE,
		ItemPool.ASBESTOS_ORE,
		ItemPool.CHROME_ORE,
		ItemPool.DWARF_BREAD,
		ItemPool.LUMP_OF_COAL,
	};

	private static final int [] ORES = new int[]
	{
		ItemPool.LINOLEUM_ORE,
		ItemPool.ASBESTOS_ORE,
		ItemPool.CHROME_ORE,
		ItemPool.LUMP_OF_COAL,
	};

	private static final int [] EQUIPMENT = new int[]
	{
		ItemPool.MINERS_HELMET,
		ItemPool.MINERS_PANTS,
		ItemPool.MATTOCK,
	};

	private static DwarfNumberTranslator digits = null;

	public static void reset()
	{
		DwarfFactoryRequest.digits = null;
	}

	public DwarfFactoryRequest()
	{
		super( "dwarffactory.php" );
	}

	public DwarfFactoryRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	public void processResults()
	{
		DwarfFactoryRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "dwarffactory.php" ) )
		{
			return;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "ware" ) )
		{
			Matcher runeMatcher = DwarfFactoryRequest.getRuneMatcher( responseText );
			String rune1 = DwarfFactoryRequest.getRune( runeMatcher );
			String rune2 = DwarfFactoryRequest.getRune( runeMatcher );
			String rune3 = DwarfFactoryRequest.getRune( runeMatcher );
			int itemId = DwarfFactoryRequest.getItemId( responseText );
			DwarfFactoryRequest.setItemRunes( itemId, rune1, rune2, rune3 );
			return;
		}

		if ( action.equals( "dodice" ) )
		{
			Matcher meatMatcher = MEAT_PATTERN.matcher( responseText );
			if ( !meatMatcher.find() )
			{
				return;
			}

			boolean won = meatMatcher.group(1).equals( "gain" );
			int meat = StringUtilities.parseInt( meatMatcher.group( 2 ) ) / 7;
			String meat7 = String.valueOf( meat / 7 ) + String.valueOf( meat % 7 );

			Matcher runeMatcher = DwarfFactoryRequest.getRuneMatcher( responseText );
			String first = DwarfFactoryRequest.getRune( runeMatcher ) + DwarfFactoryRequest.getRune( runeMatcher );
			String second = DwarfFactoryRequest.getRune( runeMatcher ) + DwarfFactoryRequest.getRune( runeMatcher );

			String message;
			if ( won )
			{
				message = second + "-" + first + "=" + meat7;
			}
			else
			{
				message = first + "-" +	 second + "=" + meat7;
			}

			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );

			KoLCharacter.ensureUpdatedDwarfFactory();
			String dice = Preferences.getString( "lastDwarfDiceRolls" );
			Preferences.setString( "lastDwarfDiceRolls", dice + message + ":" );

			// Crack the digit code!~
			DwarfFactoryRequest.solve();

			return;
		}
	}

	private static void setItemRunes( final int itemId, final String rune1, final String rune2, final String rune3 )
	{
		KoLCharacter.ensureUpdatedDwarfFactory();

		// If we are looking at runes for ore or equipment and we know
		// the complete list of ore runes or equipment runes because
		// we've looked at an office item, we can eliminate runes not
		// on the appropriate list.

		String typeRunes = "";
		String ores = null;
		String equipment = null;
			
		switch ( itemId )
		{
		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:
		case ItemPool.LUMP_OF_COAL:
			ores = Preferences.getString( "lastDwarfOreRunes" );
			if ( ores.length() == 4 )
			{
				typeRunes = ores;
			}
			break;
		case ItemPool.MINERS_HELMET:
		case ItemPool.MINERS_PANTS:
		case ItemPool.MATTOCK:
			equipment = Preferences.getString( "lastDwarfEquipmentRunes" );
			if ( equipment.length() == 3 )
			{
				typeRunes = equipment;
			}
			break;
		}

		String setting = "lastDwarfFactoryItem" + itemId;
		String oldRunes = Preferences.getString( setting );
		String newRunes = "";

		if ( typeRunes.equals( "" ) || typeRunes.indexOf( rune1 ) != -1 )
		{
			if ( oldRunes.equals( "" ) || oldRunes.indexOf( rune1 ) != -1 )
			{
				newRunes += rune1;
			}
		}

		if ( typeRunes.equals( "" ) || typeRunes.indexOf( rune2 ) != -1 )
		{
			if ( oldRunes.equals( "" ) || oldRunes.indexOf( rune2 ) != -1 )
			{
				newRunes += rune2;
			}
		}

		if ( typeRunes.equals( "" ) || typeRunes.indexOf( rune3 ) != -1 )
		{
			if ( oldRunes.equals( "" ) || oldRunes.indexOf( rune3) != -1 )
			{
				newRunes += rune3;
			}
		}

		// Eliminate any runes which definitively belong to another item.
		for ( int i = 0; i < ITEMS.length; ++i )
		{
			int id = ITEMS[i];
			if ( id == itemId )
			{
				continue;
			}

			String value = Preferences.getString( "lastDwarfFactoryItem" + id );

			if ( value.length() == 1 && newRunes.indexOf( value ) != -1 )
			{
				newRunes = newRunes.replace( value, "" );

			}
		}

		DwarfFactoryRequest.setItemRunes( itemId, newRunes );
	}

	private static void checkForLastRune( String runes, int [] items, String type )
	{
		if ( items == null )
		{
			return;
		}

		int candidate = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			int itemId = items[i];
			String setting = "lastDwarfFactoryItem" + itemId;
			String value = Preferences.getString( setting );

			if ( value.length() != 1 )
			{
				// Unidentified item
				if ( candidate != 0 )
				{
					// Already another
					return;
				}
				candidate = itemId;
			}

			// This is an identified rune. Remove from master list.
			runes = runes.replace( value, "" );
		}

		// If we get here, there is at most one item on the list we've
		// not identified.
		if ( candidate != 0 )
		{
			DwarfFactoryRequest.setItemRunes( candidate, runes );
			KoLmafia.updateDisplay( "All " + type + " have been identified!" );
		}
	}

	public static void setItemRunes( final int itemId, final String runes )
	{
		String setting = "lastDwarfFactoryItem" + itemId;
		Preferences.setString( setting, runes );

		if ( runes.length() > 1 )
		{
			return;
		}

		// See if we've identified the penultimate item and can thus
		// deduce the final item.
		switch ( itemId )
		{
		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:
		case ItemPool.LUMP_OF_COAL:
			String ores = Preferences.getString( "lastDwarfOreRunes" );
			if ( ores.length() == 4 )
			{
				DwarfFactoryRequest.checkForLastRune( ores, DwarfFactoryRequest.ORES, "ores" );
			}
			break;
		case ItemPool.MINERS_HELMET:
		case ItemPool.MINERS_PANTS:
		case ItemPool.MATTOCK:
			String equipment = Preferences.getString( "lastDwarfEquipmentRunes" );
			if ( equipment.length() == 3 )
			{
				DwarfFactoryRequest.checkForLastRune( equipment, DwarfFactoryRequest.EQUIPMENT, "pieces of equipment");
			}
			break;
		}

		// If the length is 1, that rune has been matched with an
		// item. Therefore, if it appears in the list of runes for
		// another item, it can't be the match for that item, too, and
		// can be removed from that list.

		DwarfFactoryRequest.pruneItemRunes( itemId, runes );
	}

	private static void pruneItemRunes( final int id, final String rune )
	{
		for ( int i = 0; i < ITEMS.length; ++i )
		{
			int itemId = ITEMS[i];
			if ( id == itemId )
			{
				continue;
			}

			DwarfFactoryRequest.eliminateItemRune( itemId, rune );
		}
	}

	private static void eliminateItemRune( final int itemId, final String rune )
	{
		String setting = "lastDwarfFactoryItem" + itemId;
		String value = Preferences.getString( setting );

		if ( value.length() == 1 )
		{
			return;
		}

		if ( value.indexOf( rune ) == -1 )
		{
			return;
		}

		value = value.replace( rune, "" );
		Preferences.setString( setting, value );

		if ( value.length() > 1 )
		{
			return;
		}

		// We've identified another item. Recurse!
		DwarfFactoryRequest.pruneItemRunes( itemId, value );
	}

	public static final void setHopperRune( final int hopper, final String responseText )
	{
		KoLCharacter.ensureUpdatedDwarfFactory();

		// Parse the rune from the response text
		String rune = DwarfFactoryRequest.getRune( responseText );

		// Associate this rune with this hopper
		Preferences.setString( "lastDwarfHopper" + hopper, rune );

		// Add to list of known ore runes
		DwarfFactoryRequest.setOreRune( rune );
	}

	public static void setOreRune( final String rune )
	{
		String runes = Preferences.getString( "lastDwarfOreRunes" );
		if ( runes.indexOf( rune) != -1 )
		{
			return;
		}

		// It's a new ore. Add it to the list of ores.
		Preferences.setString( "lastDwarfOreRunes", runes + rune );

		// Prune this rune from any non-ores
		for ( int i = 0; i < ITEMS.length; ++i )
		{
			int itemId = ITEMS[i];

			switch ( itemId )
			{
			case ItemPool.LINOLEUM_ORE:
			case ItemPool.ASBESTOS_ORE:
			case ItemPool.CHROME_ORE:
			case ItemPool.LUMP_OF_COAL:
				continue;
			}

			DwarfFactoryRequest.eliminateItemRune( itemId, rune );
		}
	}

	private static void setEquipmentRune( final String rune )
	{
		String runes = Preferences.getString( "lastDwarfEquipmentRunes" );
		if ( runes.indexOf( rune) != -1 )
		{
			return;
		}

		// It's a new piece of equipment. Add it to the list of equipment.
		Preferences.setString( "lastDwarfEquipmentRunes", runes + rune );

		// Prune this rune from any non-equipment
		for ( int i = 0; i < ITEMS.length; ++i )
		{
			int itemId = ITEMS[i];

			switch ( itemId )
			{
			case ItemPool.MINERS_HELMET:
			case ItemPool.MINERS_PANTS:
			case ItemPool.MATTOCK:
				continue;
			}

			DwarfFactoryRequest.eliminateItemRune( itemId, rune );
		}
	}

	private static Matcher getRuneMatcher( final String responseText )
	{
		return RUNE_PATTERN.matcher( responseText );
	}

	public static String getRune( final String responseText )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		return DwarfFactoryRequest.getRune( matcher );
	}

	public static String getRunes( final String responseText )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		String result = "";
		while ( matcher.find() )
		{
			result += matcher.group( 2 );
		}

		return result;
	}

	private static String getRune( final Matcher matcher )
	{
		if ( !matcher.find() )
		{
			return "";
		}

		return matcher.group( 2 );
	}

	private static String getDigits()
	{
		return Preferences.getString( "lastDwarfDigitRunes" );
	}

	private static void setDigits()
	{
		if ( DwarfFactoryRequest.digits == null )
		{
			return;
		}
		String digits = DwarfFactoryRequest.digits.digitString();
		Preferences.setString( "lastDwarfDigitRunes", digits );
	}

	public static int parseNumber( final String runes )
	{
		if ( DwarfFactoryRequest.digits == null )
		{
			DwarfFactoryRequest.digits = new DwarfNumberTranslator();
		}
		return DwarfFactoryRequest.digits.parseNumber( runes );
	}

	public static int parseNumber( final String runes, final String digits )
	{
		DwarfNumberTranslator translator = new DwarfNumberTranslator( digits );
		return translator.parseNumber( runes );
	}

	public static void useUnlaminatedItem( final int itemId, final String responseText )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		String runes = "";
		int count = 0;

		while ( matcher.find() )
		{
			String rune = matcher.group( 2 );

			if ( count++ == 0 )
			{
				DwarfFactoryRequest.setEquipmentRune( rune );
				runes += rune;
				continue;
			}

			String type = matcher.group(1);
			if ( type.equals( "Word" ) )
			{
				DwarfFactoryRequest.setOreRune( rune );
				runes += ',';
			}
			runes += rune;
		}
		Preferences.setString( "lastDwarfOfficeItem" + itemId, runes );
	}

	public static void useLaminatedItem( final int itemId, final String responseText )
	{
		Matcher matcher = DwarfFactoryRequest.getRuneMatcher( responseText );
		String runes = "";
		int count = 0;

		while ( matcher.find() )
		{
			String rune = matcher.group( 2 );

			if ( count++ == 0 )
			{
				DwarfFactoryRequest.setOreRune( rune );
				runes += rune;
				continue;
			}

			if ( count == 2 )
			{
				// Skip rune for "gauges"
				continue;
			}

			String type = matcher.group(1);
			if ( type.equals( "Word" ) )
			{
				DwarfFactoryRequest.setEquipmentRune( rune );
				runes += ',';
			}
			runes += rune;
		}
		Preferences.setString( "lastDwarfOfficeItem" + itemId, runes );
	}

	private static int getItemId( final String responseText )
	{
		Matcher matcher = ITEMDESC_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return -1;
		}

		return ItemDatabase.getItemIdFromDescription( matcher.group(1) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "dwarffactory.php" ) )
		{
			return false;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		if ( action.equals( "ware" ) )
		{
			String message = "[" + KoLAdventure.getAdventureCount() + "] Dwarven Factory Warehouse";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );

			return true;
		}

		if ( action.equals( "dorm" ) )
		{
			String message = "Visiting the Dwarven Factory Dormitory";

			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( action.equals( "dodice" ) || action.equals( "nodice" ) || action.equals( "nonodice" ) )
		{
			return true;
		}

		return false;
	}

	// Module to parse special messages from Dwarvish War Uniform items

	private static final Pattern DWARF_MATTOCK_PATTERN =
		Pattern.compile( "<p>Your mattock glows (really\\s+)*bright blue.</p>");
	private static final Pattern REALLY_PATTERN = Pattern.compile( "really" );

	public static Matcher hpMessage( final CharSequence responseText )
	{
		Matcher mattockMatcher = DwarfFactoryRequest.DWARF_MATTOCK_PATTERN.matcher( responseText );
		return mattockMatcher.find() ? mattockMatcher : null;
	}

	public static int deduceHP( final CharSequence responseText )
	{
		Matcher mattockMatcher = DwarfFactoryRequest.hpMessage( responseText );
		if ( mattockMatcher == null )
		{
			return 0;
		}
		return DwarfFactoryRequest.deduceHP( mattockMatcher );
	}

	public static int deduceHP( final Matcher mattockMatcher )
	{
		Matcher reallyMatcher = REALLY_PATTERN.matcher( mattockMatcher.group( 0 ) );
		int count = 0;
		while ( reallyMatcher.find() )
		{
			++count;
		}

		return 0;
	}

	private static final Pattern DWARF_HELMET_PATTERN =
		Pattern.compile( "<p>A small crystal lens flips down.*?</p>" );

	public static Matcher attackMessage( final CharSequence responseText )
	{
		Matcher helmetMatcher = DwarfFactoryRequest.DWARF_HELMET_PATTERN.matcher( responseText );
		return helmetMatcher.find() ? helmetMatcher : null;
	}

	public static int deduceAttack( final CharSequence responseText )
	{
		Matcher helmetMatcher = DwarfFactoryRequest.attackMessage( responseText );
		if ( helmetMatcher == null )
		{
			return 0;
		}
		return DwarfFactoryRequest.deduceAttack( helmetMatcher );
	}

	public static int deduceAttack( final Matcher helmetMatcher )
	{
		String runes = DwarfFactoryRequest.getRunes( helmetMatcher.group(0) );
		return DwarfFactoryRequest.parseNumber( runes );
	}

	public static final Pattern DWARF_KILT_PATTERN =
		Pattern.compile( "<p>.*?our sporran.*?</p>" );

	public static Matcher defenseMessage( final CharSequence responseText )
	{
		Matcher kiltMatcher = DwarfFactoryRequest.DWARF_KILT_PATTERN.matcher( responseText );
		return kiltMatcher.find() ? kiltMatcher : null;
	}

	public static int deduceDefense( final CharSequence responseText )
	{
		Matcher kiltMatcher = DwarfFactoryRequest.defenseMessage( responseText );
		if ( kiltMatcher == null )
		{
			return 0;
		}
		return DwarfFactoryRequest.deduceDefense( kiltMatcher );
	}

	public static int deduceDefense( final Matcher kiltMatcher )
	{
		Matcher matcher= DwarfFactoryRequest.COLOR_PATTERN.matcher( kiltMatcher.group(0) );
		int number = 0;

		while ( matcher.find() )
		{
			String color = matcher.group(1);
			int digit = -1;
			if ( color.equals( "red" ) )
			{
				digit = 0;
			}
			else if ( color.equals( "orange" ) )
			{
				digit = 1;
			}
			else if ( color.equals( "yellow" ) )
			{
				digit = 2;
			}
			else if ( color.equals( "green" ) )
			{
				digit = 3;
			}
			else if ( color.equals( "blue" ) )
			{
				digit = 4;
			}
			else if ( color.equals( "indigo" ) )
			{
				digit = 5;
			}
			else if ( color.equals( "violet" ) )
			{
				digit = 6;
			}
			else
			{
				return -1;
			}
			number = ( number * 7 ) + digit;
		}
		return number;
	}

	// Module to check whether you've found everything you need

	public static boolean check( final boolean use )
	{
		StringBuffer output = new StringBuffer();

		// Check the office items
		DwarfFactoryRequest.checkOrUse( ItemPool.SMALL_LAMINATED_CARD, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.LITTLE_LAMINATED_CARD, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.NOTBIG_LAMINATED_CARD, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.UNLARGE_LAMINATED_CARD, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.DWARVISH_DOCUMENT, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.DWARVISH_PAPER, use, output );
		DwarfFactoryRequest.checkOrUse( ItemPool.DWARVISH_PARCHMENT, use, output );

		// Check the hoppers
		DwarfFactoryRequest.checkHopper( 1, use, output );
		DwarfFactoryRequest.checkHopper( 2, use, output );
		DwarfFactoryRequest.checkHopper( 3, use, output );
		DwarfFactoryRequest.checkHopper( 4, use, output );

		// Check the ores and equipment
		DwarfFactoryRequest.checkItem( ItemPool.MINERS_HELMET, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.MINERS_PANTS, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.MATTOCK, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.LINOLEUM_ORE, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.ASBESTOS_ORE, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.CHROME_ORE, use, output );
		DwarfFactoryRequest.checkItem( ItemPool.LUMP_OF_COAL, use, output );

		String text = output.toString();
		if ( text.length() > 0 )
		{
			RequestLogger.printLine( text );
			return false;
		}

		return true;
	}

	public static void checkOrUse( final int itemId, final boolean use, final StringBuffer output )
	{
		if ( !InventoryManager.hasItem( itemId	) )
		{
			output.append( "You do not have the " + ItemDatabase.getItemName( itemId ) + KoLConstants.LINE_BREAK );
			return;
		}

		if ( !use || !Preferences.getString( "lastDwarfOfficeItem" + itemId ).equals( "" ) )
		{
			return;
		}

		RequestThread.postRequest( new UseItemRequest( ItemPool.get( itemId, 1 ) ) );
	}

	public static void checkHopper( final int hopper, final boolean use, final StringBuffer output )
	{
		if ( !use || !Preferences.getString( "lastDwarfHopper" + hopper ).equals( "" ) )
		{
			return;
		}

		RequestThread.postRequest( new DwarfContraptionRequest( "hopper" + ( hopper - 1) ) );
	}

	public static void checkItem( final int itemId, final boolean use, final StringBuffer output )
	{
		if ( Preferences.getString( "lastDwarfFactoryItem" + itemId ).length() != 1 )
		{
			output.append( "You not yet identified the " + ItemDatabase.getItemName( itemId ) + KoLConstants.LINE_BREAK );
			return;
		}
	}

	// Module to solve the digit code

	public static void solve()
	{
		// If we don't currently have a digit translator, create one
		// from the preference
		if ( DwarfFactoryRequest.digits == null )
		{
			DwarfFactoryRequest.digits = new DwarfNumberTranslator();
		}

		// If it's valid, we're golden
		if ( DwarfFactoryRequest.digits.valid() )
		{
			return;
		}

		// Here's where we solve it.

		// Step 0: get the unlaminated numbers
		String[] unlaminated = getUnlaminatedNumbers();
		for ( int i = 0; i < unlaminated.length; ++i )
		{
			DwarfFactoryRequest.digits.addNumber( unlaminated[i] );
		}

		// Step 1: try to deduce what we can from the laminated items
		String[] laminated = getLaminatedNumbers();
		for ( int i = 0; i < laminated.length; ++i )
		{
			DwarfFactoryRequest.digits.addNumber( laminated[i] );
		}
		DwarfFactoryRequest.digits.analyzeNumbers();

		// Step 2: iterate over saved dice rules, deducing what we can
		String[] rolls = getDiceRolls();
		for ( int i = 0; i < rolls.length; ++i )
		{
			DwarfFactoryRequest.digits.addRoll( rolls[i] );
		}
		DwarfFactoryRequest.digits.analyzeRolls();

		// Save the current digit string, complete or not
		DwarfFactoryRequest.setDigits();

		if ( DwarfFactoryRequest.digits.valid() )
		{
			KoLmafia.updateDisplay( "Dwarf digit code solved!" );
		}
		else
		{
			RequestLogger.printLine( "Unable to solve digit code. Get more data!" );
		}
	}

	private static String[] getUnlaminatedNumbers()
	{
		ArrayList numbers = new ArrayList();
		// lastDwarfOfficeItem3212=H,QEG,BFD,OJI,JED
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.DWARVISH_DOCUMENT, 4 );
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.DWARVISH_PAPER, 4 );
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.DWARVISH_PARCHMENT, 4 );
		return (String[])numbers.toArray( new String[ numbers.size() ] );
	}

	private static String[] getLaminatedNumbers()
	{
		ArrayList numbers = new ArrayList();
		// lastDwarfOfficeItem3208=B,HGIG,MGDE,PJD
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.SMALL_LAMINATED_CARD, 3 );
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.LITTLE_LAMINATED_CARD, 3 );
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.NOTBIG_LAMINATED_CARD, 3 );
		DwarfFactoryRequest.getItemNumbers( numbers, ItemPool.UNLARGE_LAMINATED_CARD, 3 );
		return (String[])numbers.toArray( new String[ numbers.size() ] );
	}

	private static void getItemNumbers( final ArrayList list, final int itemId, final int count )
	{
		String setting = DwarfFactoryRequest.getItemSetting( itemId );
		String[] splits = setting.split( "," );
		if ( splits.length != count + 1 )
		{
			return;
		}

		for ( int i = 1; i <= count; ++i )
		{
			list.add( splits[i].substring(1) );
		}
	}

	private static String getItemSetting( int itemId )
	{
		String settingName = "lastDwarfOfficeItem" + itemId;
		String setting = Preferences.getString( settingName );
		if ( !setting.equals( "" ) )
		{
			return setting;
		}

		if ( InventoryManager.hasItem( itemId ) )
		{
			RequestThread.postRequest( new UseItemRequest( ItemPool.get( itemId, 1 ) ) );
		}

		return Preferences.getString( settingName );
	}

	private static String[] getDiceRolls()
	{
		String rolls = Preferences.getString( "lastDwarfDiceRolls" );
		return rolls.split( ":" );
	}

	// Module to report on what we've gleaned about the factory quest

	public static void report()
	{
		DwarfFactoryRequest.solve();
		if ( DwarfFactoryRequest.digits == null )
		{
			return;
		}
		DwarfFactoryRequest.report( DwarfFactoryRequest.digits );
	}

	public static void report( final String digits )
	{
		if ( digits.length() != 7 )
		{
			RequestLogger.printLine( "Digit string must have 7 characters" );
			return;
		}
		DwarfNumberTranslator translator = new DwarfNumberTranslator( digits );
		DwarfFactoryRequest.report( translator );
	}

	private static void report( final DwarfNumberTranslator digits )
	{
		if ( !digits.valid() )
		{
			RequestLogger.printLine( "Invalid or incomplete digit set" );
			return;
		}

		if ( !DwarfFactoryRequest.check( true ) )
		{
			return;
		}

		FactoryData data = new FactoryData( digits );

		StringBuffer output = new StringBuffer();

		output.append( "<table border=2 cols=6>" );

		// Put in a header
		output.append( "<tr>" );
		output.append( "<td rowspan=2 colspan=2></td>" );
		output.append( "<td align=center>Hopper #1</td>" );
		output.append( "<td align=center>Hopper #2</td>" );
		output.append( "<td align=center>Hopper #3</td>" );
		output.append( "<td align=center>Hopper #4</td>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<td align=center>" + data.getHopperOre( 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getHopperOre( 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getHopperOre( 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getHopperOre( 3 ) + "</td>" );
		output.append( "</tr>" );

		// Add HAT
		output.append( "<tr>" );
		output.append( "<td align=center rowspan=2>Hat</td>" );
		output.append( "<td align=center>Gauges</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.HAT, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.HAT, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.HAT, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.HAT, 3 ) + "</td>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<td align=center>Ores</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.HAT, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.HAT, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.HAT, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.HAT, 3 ) + "</td>" );
		output.append( "</tr>" );

		// Add PANTS
		output.append( "<tr>" );
		output.append( "<td align=center rowspan=2>Pants</td>" );
		output.append( "<td align=center>Gauges</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.PANTS, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.PANTS, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.PANTS, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.PANTS, 3 ) + "</td>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<td align=center>Ores</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.PANTS, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.PANTS, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.PANTS, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.PANTS, 3 ) + "</td>" );
		output.append( "</tr>" );

		// Add WEAPON
		output.append( "<tr>" );
		output.append( "<td align=center rowspan=2>Weapon</td>" );
		output.append( "<td align=center>Gauges</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.WEAPON, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.WEAPON, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.WEAPON, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getGaugeSetting( FactoryData.WEAPON, 3 ) + "</td>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<td align=center>Ores</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.WEAPON, 0 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.WEAPON, 1 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.WEAPON, 2 ) + "</td>" );
		output.append( "<td align=center>" + data.getOreQuantity( FactoryData.WEAPON, 3 ) + "</td>" );
		output.append( "</tr>" );

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}

	public static class DwarfNumberTranslator
	{
		private final HashMap digitMap = new HashMap();
		private final HashMap charMap = new HashMap();

		public DwarfNumberTranslator()
		{
			this( DwarfFactoryRequest.getDigits() );
		}

		public DwarfNumberTranslator( final String digits )
		{
			// Make maps from dwarf digit rune to base 7 digit and
			// vice versa
			for ( int i = 0; i < digits.length() && i < 7 ; ++i )
			{
				char digit = digits.charAt( i );
				if ( !Character.isLetter( digit ) )
				{
					continue;
				}

				this.mapCharacter( digit, i );
			}
		}

		private void mapCharacter( final char c, final int i )
		{
			Character code = new Character( Character.toUpperCase( c ) );
			Integer val = new Integer( i );
			this.mapCharacter( code, val );
		}

		private void mapCharacter( final Character code, final Integer val )
		{
			this.digitMap.put( code, val );
			this.charMap.put( val, code );
		}

		public Integer getDigit( final Character code )
		{
			return (Integer) this.digitMap.get( code );
		}

		public Integer getDigit( final char c )
		{
			return this.getDigit( new Character( c ) );
		}

		public String digitString()
		{
			String val = "";
			for ( int i = 0; i < 7; ++i )
			{
				Character code = (Character)this.charMap.get( new Integer( i ) );
				val += code == null ? '-' : code.charValue();
			}
			return val;
		}

		public boolean valid()
		{
			return this.digitMap.size() == 7;
		}

		public int parseNumber( final String string )
		{
			int number = 0;
			for ( int i = 0; i < string.length(); ++i )
			{
				Integer val = (Integer) this.digitMap.get( new Character( string.charAt( i ) ) );
				if ( val == null )
				{
					return -1;
				}
				number = ( number * 7 ) + val.intValue();
			}
			return number;
		}

		// Methods for solving the digit code

		private final ArrayList numbers = new ArrayList();
		private final ArrayList digits = new ArrayList();

		public void addNumber( final String number )
		{
			// See if it's a new number
			for ( int i = 0; i < this.numbers.size(); ++i )
			{
				String old = (String)this.numbers.get(i);
				if ( old.equals( number) )
				{
					return;
				}
			}

			// Add the new number to the list
			this.numbers.add( number );

			// Add all the digits to the set of digits
			for ( int i = 0; i < number.length(); ++i )
			{
				Character digit = new Character( number.charAt( i ) );
				if ( !this.digits.contains( digit ) )
				{
					this.digits.add( digit );
				}
			}
		}

		// Step 1: Deduce initial digit of three digit numbers from
		// laminated items

		private int numberCount = 0;

		public void analyzeNumbers()
		{
			// If we've already looked at all the numbers in the
			// array, it's pointless to do it again.

			if ( this.numbers.size() == numberCount )
			{
				return;
			}

			// Save current size of array
			numberCount = this.numbers.size();

			// Gauge numbers encode base-10 numbers from 00 - 99 in
			// base-7. In base 7, numbers greater than 48 take 3
			// digits with the first digit equal to 1. Numbers
			// greater than 97 take 3 digits with the first digit
			// equal to 2. In particular, 98 is 200 and 99 is 201.

			// Therefore, when we have a three digit number, 50
			// times out of 52, it will encode "1", and 2 times out
			// of 52 it will encode "2". If it encodes "1", the
			// following two digits can be any of the 7 digits.

			// Make an array to save each initial digit and the set
			// of all digits that appear after each initial digit

			char [][] matches = new char[2][8];
			int [] counts = new int[2];

			for ( int i = 0; i < this.numbers.size(); ++i )
			{
				String val = (String)this.numbers.get(i);

				// We only deduce digits from 3-digit numbers.
				if ( val.length() < 3 )
				{
					continue;
				}

				char d1 = val.charAt(0);
				char d2 = val.charAt(1);
				char d3 = val.charAt(2);

				int off = 0;
				for ( int j = 0; j < matches.length; ++ j )
				{
					char match = matches[j][0];
					if ( match == 0 || match == d1 )
					{
						off = j;
						break;
					}
				}

				char [] digits = matches[off];
				digits[0] = d1;

				for ( int k = 1; k < digits.length; ++k )
				{
					char match = digits[k];
					if ( match == d2 )
					{
						break;
					}
					if ( match == 0 )
					{
						digits[k] = d2;
						counts[off]++;
						break;
					}
				}

				for ( int k = 1; k < digits.length; ++k )
				{
					char match = digits[k];
					if ( match == d3 )
					{
						break;
					}
					if ( match == 0 )
					{
						digits[k] = d3;
						counts[off]++;
						break;
					}
				}
			}

			// We've now saved all the digits. If any initial
			// character has 3 or more different digits that follow
			// it, it must be 1.

			int oneOffset = counts[0] >= 3 ? 0 : counts[1] >= 3 ? 1 : -1;

			// In the unlikely event that no initial digit has more
			// than 2 digits that follow it, we can deduce nothing.
			if ( oneOffset == -1 )
			{
				return;
			}

			this.mapCharacter( matches[oneOffset][0], 1 );

			// If we have identified 1, if we have another initial
			// digit, it must be 2.

			int twoOffset = 1 - oneOffset;
			if ( counts[ twoOffset ] == 0 )
			{
				return;
			}

			this.mapCharacter( matches[twoOffset][0], 2 );

			// 2 is always followed by 0
			this.mapCharacter( matches[twoOffset][1], 0 );
		}

		// Step 2: Deduce digits from the dice game

		private final ArrayList rolls = new ArrayList();

		public void addRoll( final String roll )
		{
			// See if it's a new roll
			for ( int i = 0; i < this.rolls.size(); ++i )
			{
				String old = (String)this.rolls.get(i);
				if ( old.equals( roll) )
				{
					return;
				}
			}

			// Add the new roll to the list
			this.rolls.add( roll );
		}

		private int rollCount = 0;

		public void analyzeRolls()
		{
			// If we've already looked at all the rolls in the
			// array, it's pointless to do it again.
			if ( this.rolls.size() == rollCount )
			{
				return;
			}

			// Save current size of array
			rollCount = this.rolls.size();

			while ( true )
			{
				// Find how many variables are known
				int known = this.digitMap.size();

				// If we know all the digits, nothing to do
				if ( known == 7 )
				{
					return;
				}

				// If we have learned all but one, we can
				// deduce the last one

				if ( known == 6 )
				{
					this.deduceLastDigit();
					return;
				}

				// Make a linear system to analyze the rolls
				LinearSystem system = this.makeSystem();

				// Solve it
				system.calculate();

				// If we deduced nothing new, we are done
				if ( known == this.digitMap.size() )
				{
					return;
				}

				// We learned something! Try again; if we
				// learned zero, we can now use doubles.
			}
		}

		private void deduceLastDigit()
		{
			// Only do this if we know all but one of the digits
			// and all the characters which are used as digits.
			if ( this.digitMap.size() != 6 || this.digits.size() != 7 )
			{
				return;
			}

			// Find the character we have not used for a digit
			Character code = null;
			for ( int i = 0; i < 7; ++i )
			{
				Character c = (Character)this.digits.get( i );
				if ( !this.digitMap.containsKey( c ) )
				{
					code = c;
					break;
				}
			}

			// Find the digit we have not identified
			for ( int i = 0; i < 7; ++i )
			{
				Integer val = new Integer( i );
				if ( this.charMap.get( val ) == null )
				{
					this.mapCharacter( code, val );
					return;
				}
			}
		}

		private LinearSystem makeSystem()
		{
			// Iterate until we stop learning variables by simple
			// substitution.
			while ( true )
			{
				// The digit for zero is special: a roll of
				// "00" equals 49
				Character z = (Character)this.charMap.get( new Integer( 0 ) );
				LinearSystem system = this.makeSystem( z );
				if ( system != null )
				{
					return system;
				}
			}
		}

		private LinearSystem makeSystem( final Character zero)
		{
			// Make a list of variables
			ArrayList variables = new ArrayList();
			for ( int i = 0; i < this.digits.size(); ++i )
			{
				Character c = (Character)this.digits.get( i );
				if ( !this.digitMap.containsKey( c ) )
				{
					variables.add( c );
				}
			}

			// Make a linear system containing these variables
			LinearSystem system = new LinearSystem( variables );

			// Give it equations
			for ( int i = 0; i < this.rolls.size(); ++i )
			{
				String roll = (String) this.rolls.get( i );

				// AB-CD=xx
				if ( roll.length() != 8 )
				{
					continue;
				}

				char d1 = roll.charAt( 0 );
				char d2 = roll.charAt( 1 );
				char d3 = roll.charAt( 3 );
				char d4 = roll.charAt( 4 );
				int high = roll.charAt( 6 ) - '0';
				int low = roll.charAt( 7 ) - '0';
				int val = ( high * 7) + low;

				// Double zero = 49, not 0. Unless we know what
				// zero is, we can't make that conversion, so
				// skip all doubles until we learn 0.

				if ( d1 == d2 )
				{
					if ( zero == null )
					{
						continue;
					}

					// The linear equation solver doesn't
					// know that 00 is 49. Adjust sum.
					if ( d1 == zero.charValue() )
					{
						val -= 49;
					}
				}

				int known = this.digitMap.size();
				system.addEquation( 7, d1, 1, d2, -7, d3, -1, d4, val );

				// If adding an equation found a variable by
				// substitution, punt, since that could make
				// already added equations simpler.

				if ( known != this.digitMap.size() )
				{
					return null;
				}
			}

			return system;
		}

		private class LinearSystem
		{
			private final int vcount;
			private final Character[] variables;
			private final int[] counts;
			private final ArrayList equations;

			public LinearSystem( final ArrayList variables )
			{
				this.vcount = variables.size();
				this.variables = (Character[]) variables.toArray( new Character[ this.vcount ] );
				this.counts = new int[ this.vcount ];
				this.equations = new ArrayList();
			}

			public void addEquation( final int c1, final char v1,
						 final int c2, final char v2,
						 final int c3, final char v3,
						 final int c4, final char v4,
						 final int val )
			{
				int[] equation = new int[ this.vcount + 1];

				// Store the sum first, since we will adjust it.
				equation[ this.vcount ] = val;

				// Store or substitute all the variables
				this.addVariable( equation, c1, v1 );
				this.addVariable( equation, c2, v2 );
				this.addVariable( equation, c3, v3 );
				this.addVariable( equation, c4, v4 );

				// If we only have one variable unsubstituted
				// in this equation, we've discovered a new
				// digit.

				int offset = -1;
				for ( int i = 0; i < this.vcount; ++i )
				{
					// Skip unused variables
					if ( equation[i] == 0 )
					{
						continue;
					}

					// At least two unknown variables.
					// Register equation.
					if ( offset != -1 )
					{
						this.equations.add( equation );
						return;
					}

					// This variable is unknown
					offset = i;
				}

				// If there is only one unsubstituted variable,
				// register the new digit.
				if ( offset != -1 )
				{
					char code = this.variables[ offset ].charValue();
					int i = equation[ this.vcount ] / equation [ offset ];

					DwarfNumberTranslator.this.mapCharacter( code, i );
				}

				// All the variables in this equation are
				// already known. Ignore it.
			}

			public void addVariable( final int[] equation, final int c1, final char v1 )
			{
				Integer digit = DwarfNumberTranslator.this.getDigit( v1 );
				// If this is a known variable, substitute
				if ( digit != null )
				{
					int val = c1 * digit.intValue();
					equation[ this.vcount ] -= val;
					return;
				}

				// This is an unknown variable. Find it in the
				// list.

				for ( int i = 0; i < this.vcount; ++i )
				{
					if ( v1 == this.variables[ i ].charValue() )
					{
						equation[ i ] += c1;
						return;
					}
				}
			}

			public void calculate()
			{
				if ( vcount == 0 || equations.size() == 0 )
				{
					return;
				}

				// Count rows that use each variable
				for ( int col = 0; col < this.vcount; ++col )
				{
					this.counts[col] = this.countRows( col );
				}
			}

			private int countRows( final int column )
			{
				int count = 0;
				for ( int i = 0; i < this.equations.size(); ++i )
				{
					int [] equation = (int[])this.equations.get( i );
					if ( equation[ column ] != 0 )
					{
						count++;
					}
				}
				return count;
			}

			private void swapRows( final int row1, final int row2 )
			{
				Object temp = this.equations.get( row1 );
				this.equations.set( row1, this.equations.get( row2 ) );
				this.equations.set( row2, temp );
			}

			private void swapColumns( final int col1, final int col2 )
			{
				// Swap columns in all equations
				for ( int i = 0; i < this.equations.size(); ++i )
				{
					int[] equation = (int[]) this.equations.get( i );
					int temp = equation[ col1 ];
					equation[ col1 ] = equation[ col2 ];
					equation[ col2 ] = temp;
				}

				// Swap columns in equation counts
				int count = this.counts[ col1 ];
				this.counts[ col1 ] = this.counts[ col2 ];
				this.counts[ col2 ] = count;
					
				// Swap columns in variables
				Character var =	 this.variables[ col1 ];
				this.variables[ col1 ] = this.variables[ col2 ];
				this.variables[ col2 ] = var;
			}
		}
	}

	public static class FactoryData
	{
		public static final int HAT = 0;
		public static final int PANTS = 1;
		public static final int WEAPON = 2;

		private DwarfNumberTranslator digits;
		private HashMap itemMap = new HashMap();
		private HashMap runeMap = new HashMap();

		// Indexed by [item]
		private char [] equipment = new char[3];

		// Indexed by [hopper]
		private int [] ores = new int[4];

		// Indexed by [item][hopper]
		private int [][] oreQuantities = new int[3][4];
		private int [][] gaugeSettings = new int[3][4];

		public FactoryData( final DwarfNumberTranslator digits )
		{
			// Get a Dwarf Number Translator
			this.digits = digits;

			// Make maps from dwarf word rune to itemId and vice versa
			for ( int i = 0; i < DwarfFactoryRequest.ITEMS.length; ++i )
			{
				int itemId = DwarfFactoryRequest.ITEMS[i];
				String setting = "lastDwarfFactoryItem" + itemId;
				String value = Preferences.getString( setting );
				if ( value.length() == 1 )
				{
					Character rune = new Character( value.charAt( 0 ) );
					Integer id = new Integer( itemId );
					this.itemMap.put( rune, id );
					this.runeMap.put( id, rune );
				}
			}

			// Get the 3 pieces of equipment
			this.equipment[ HAT ] = this.findRune( ItemPool.MINERS_HELMET );
			this.equipment[ PANTS ] = this.findRune( ItemPool.MINERS_PANTS );
			this.equipment[ WEAPON ] = this.findRune( ItemPool.MATTOCK );

			// Get the 4 ores in hopper order
			this.ores[0] = this.findItem( Preferences.getString( "lastDwarfHopper1" ) );
			this.ores[1] = this.findItem( Preferences.getString( "lastDwarfHopper2" ) );
			this.ores[2] = this.findItem( Preferences.getString( "lastDwarfHopper3" ) );
			this.ores[3] = this.findItem( Preferences.getString( "lastDwarfHopper4" ) );

			// Translate the unlaminated items into ore quantities
			this.getOreQuantities();

			// Translate the laminated items into gauge settings
			this.getGaugeSettings();
		}

		public String getHopperOre( final int hopper )
		{
			if ( hopper < 0 || hopper > 3 )
			{
				return null;
			}
			return ItemDatabase.getItemName( this.ores[ hopper ] );
		}

		public AdventureResult getOre( final int item, final int hopper )
		{
			if ( item < 0 || item > 2 || hopper < 0 || hopper > 3 )
			{
				return null;
			}
			int itemId = this.ores[ hopper ];
			if ( itemId == -1 )
			{
				return null;
			}
			int count = this.oreQuantities[ item ][ hopper ];
			return new AdventureResult( itemId, count );
		}

		public int getOreQuantity( final int item, final int hopper )
		{
			if ( item < 0 || item > 2 || hopper < 0 || hopper > 3 )
			{
				return 0;
			}
			return this.oreQuantities[ item ][ hopper ];
		}

		private void setOreQuantity( final int item, final int hopper, final int value )
		{
			this.oreQuantities[ item ][ hopper ] = value;
		}

		public int getGaugeSetting( final int item, final int hopper )
		{
			if ( item < 0 || item > 2 || hopper < 0 || hopper > 3 )
			{
				return -1;
			}
			return this.gaugeSettings[ item ][ hopper ];
		}

		private void setGaugeSetting( final int item, final int hopper, final int value )
		{
			this.gaugeSettings[ item ][ hopper ] = value;
		}

		private int findItem( final String rune )
		{
			if ( rune.length() != 1 )
			{
				return -1;
			}
			return this.findItem( rune.charAt(0) );
		}

		private int findItem( final char rune )
		{
			Integer val = (Integer) this.itemMap.get( new Character( rune ) );
			return val == null ? -1 : val.intValue();
		}

		private char findRune( final int itemId )
		{
			Character val = (Character) this.runeMap.get( new Integer( itemId ) );
			return val == null ? 0 : val.charValue();
		}

		private int findHopper( final char rune )
		{
			int item = this.findItem( rune );

			if ( item == -1 )
			{
				return -1;
			}

			for ( int i = 0; i < 4; ++i )
			{
				if ( item == this.ores[i] )
				{
					return i;
				}
			}

			return -1;
		}

		private int findEquipment( final char rune )
		{
			for ( int i = 0; i < 3; ++i )
			{
				if ( rune == this.equipment[i] )
				{
					return i;
				}
			}
			return -1;
		}

		private void getGaugeSettings()
		{
			this.getGaugeSetting( Preferences.getString( "lastDwarfOfficeItem3208" ) );
			this.getGaugeSetting( Preferences.getString( "lastDwarfOfficeItem3209" ) );
			this.getGaugeSetting( Preferences.getString( "lastDwarfOfficeItem3210" ) );
			this.getGaugeSetting( Preferences.getString( "lastDwarfOfficeItem3211" ) );
		}

		private void getGaugeSetting( final String setting )
		{
			// lastDwarfOfficeItem3208=B,HGIG,MGDE,PJD
			// lastDwarfOfficeItem3209=O,HGAA,MGAG,PGEA
			// lastDwarfOfficeItem3210=J,HFJ,MGED,PGAG
			// lastDwarfOfficeItem3211=Q,HGE,MGGI,PGG

			String[] splits = setting.split( "," );
			if ( splits.length != 4 )
			{
				return;
			}

			int hopper = this.findHopper( splits[0].charAt(0) );
			if ( hopper < 0 )
			{
				return;
			}

			for ( int i = 1; i <= 3; ++i )
			{
				int item = this.findEquipment( splits[i].charAt(0) );
				if ( item < 0 )
				{
					continue;
				}

				int number = this.digits.parseNumber( splits[i].substring(1) );
				this.setGaugeSetting( item, hopper, number );
			}
		}

		private void getOreQuantities(	)
		{
			this.getOreQuantity( Preferences.getString( "lastDwarfOfficeItem3212" ) );
			this.getOreQuantity( Preferences.getString( "lastDwarfOfficeItem3213" ) );
			this.getOreQuantity( Preferences.getString( "lastDwarfOfficeItem3214" ) );
		}

		private void getOreQuantity( final String setting )
		{
			// lastDwarfOfficeItem3212=H,QEG,BFD,OJI,JED
			// lastDwarfOfficeItem3213=M,OGD,BGD,QJI,JGJ
			// lastDwarfOfficeItem3214=P,JFJ,OJE,BGA,QEG

			String[] splits = setting.split( "," );
			if ( splits.length != 5 )
			{
				return;
			}

			int item = this.findEquipment( splits[0].charAt(0) );
			if ( item < 0 )
			{
				return;
			}

			for ( int i = 1; i <= 4; ++i )
			{
				int hopper = this.findHopper( splits[i].charAt(0) );
				if ( hopper < 0 )
				{
					continue;
				}

				int number = this.digits.parseNumber( splits[i].substring(1) );
				this.setOreQuantity( item, hopper, number );
			}
		}
	}
}
