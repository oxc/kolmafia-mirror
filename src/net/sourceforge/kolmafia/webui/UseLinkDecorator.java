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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.PyroRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class UseLinkDecorator
{
	private static final Pattern ACQUIRE_PATTERN =
		Pattern.compile( "(You acquire|O hai, I made dis)([^<]*?)<b>(.*?)</b>(.*?)</td>", Pattern.DOTALL );
	
	private static final StringBuffer deferred = new StringBuffer();

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		boolean inCombat = location.startsWith( "fight.php" );
		if ( !inCombat && buffer.indexOf( "You acquire" ) == -1 &&
		     buffer.indexOf( "O hai, I made dis" ) == -1)
		{
			return;
		}

		// Defer use links until later if this isn't the final combat round
		boolean duringCombat = inCombat &&
			( Preferences.getBoolean( "serverAddsCustomCombat" ) 
				? buffer.indexOf( "(show old combat form)" ) != -1
				: buffer.indexOf( "fight.php" ) != -1 );

		String text = buffer.toString();
		buffer.setLength( 0 );

		Matcher useLinkMatcher = ACQUIRE_PATTERN.matcher( text );

		int specialLinkId = 0;
		String specialLinkText = null;

		while ( useLinkMatcher.find() )
		{
			// See if it's an effect
			if ( UseLinkDecorator.addEffectLink( location, useLinkMatcher, buffer ) )
			{
				continue;
			}
				
			int itemCount = 1;
			String itemName = useLinkMatcher.group( 3 );

			int spaceIndex = itemName.indexOf( " " );
			if ( spaceIndex != -1 && useLinkMatcher.group( 2 ).indexOf( ":" ) == -1 )
			{
				itemCount = StringUtilities.parseInt( itemName.substring( 0, spaceIndex ) );
				itemName = itemName.substring( spaceIndex + 1 );
			}

			int itemId = ItemDatabase.getItemId( itemName, itemCount, false );
			if ( itemId == -1 )
			{
				continue;
			}

			// Certain items get use special links to minimize the
			// amount of scrolling to find the item again.

			if ( location.startsWith( "inventory.php" ) ||
			     ( location.startsWith( "inv_use.php" ) && location.indexOf( "ajax=1" ) != -1 ) )
			{
				switch ( itemId )
				{
				case ItemPool.FOIL_BOW:
				case ItemPool.FOIL_RADAR:
				case ItemPool.FOIL_CAT_EARS:
					specialLinkId = itemId;
					specialLinkText = "fold";
					break;
				}
			}

			int pos = buffer.length();
			if ( UseLinkDecorator.addUseLink( itemId, itemCount, location, useLinkMatcher, buffer ) && duringCombat )
			{	// Find where the replacement was appended
				pos = buffer.indexOf( useLinkMatcher.group( 1 ) + useLinkMatcher.group( 2 )
					+ "<b>" + useLinkMatcher.group( 3 ), pos );
				if ( pos == -1 )
				{
					continue;
				}
				// Find start of table row containing it
				pos = buffer.lastIndexOf( "<tr", pos );
				if ( pos == -1 )
				{
					continue;
				}
				UseLinkDecorator.deferred.append( buffer.substring( pos ) );
				UseLinkDecorator.deferred.append( "</tr>" );
			}
		}

		useLinkMatcher.appendTail( buffer );

		if ( !duringCombat && specialLinkText != null )
		{
			StringUtilities.singleStringReplace(
				buffer,
				"</center></blockquote>",
				"<p><center><a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=2&whichitem=" + specialLinkId + "\">[" + specialLinkText + " it again]</a></center></blockquote>" );
		}
		
		if ( duringCombat )
		{	// discard all changes, the links aren't usable yet
			buffer.setLength( 0 );
			buffer.append( text );
		}
		
		StringUtilities.singleStringReplace( buffer,
			"A sticker falls off your weapon, faded and torn.",
			"A sticker falls off your weapon, faded and torn. <font size=1>" +
			"[<a href=\"bedazzle.php\">bedazzle</a>]</font>" );
		
		if ( inCombat && !duringCombat && UseLinkDecorator.deferred.length() > 0 )
		{
			int pos = buffer.lastIndexOf( "</table>" );
			if ( pos == -1 )
			{
				return;
			}
			text = buffer.substring( pos );
			buffer.setLength( pos );
			buffer.append( "</table><table><tr><td colspan=2>Previously seen:</td></tr>" );
			buffer.append( UseLinkDecorator.deferred );
			buffer.append( text );
			UseLinkDecorator.deferred.setLength( 0 );
		}
	}

	private static final int shouldAddCreateLink( int itemId, String location )
	{
		if ( location == null || location.indexOf( "craft.php" ) != -1 || location.indexOf( "paster" ) != -1 || location.indexOf( "smith" ) != -1 )
		{
			return KoLConstants.NOCREATE;
		}

		// Retrieve the known ingredient uses for the item.
		SortedListModel creations = ConcoctionDatabase.getKnownUses( itemId );
		if ( creations.isEmpty() )
		{
			return KoLConstants.NOCREATE;
		}

		// Skip items which are multi-use or are mp restores.
		int consumeMethod = ItemDatabase.getConsumptionType( itemId );
		if ( consumeMethod == KoLConstants.CONSUME_MULTIPLE ||
		     consumeMethod == KoLConstants.MP_RESTORE )
		{
			return KoLConstants.NOCREATE;
		}

		switch ( itemId )
		{
		// If you find the wooden stakes, you want to equip them
		case ItemPool.WOODEN_STAKES:
			return KoLConstants.NOCREATE;

		// If you find goat cheese, let the trapper link handle it.
		case ItemPool.GOAT_CHEESE:
			return KoLConstants.NOCREATE;

		// If you find ore, let the trapper link handle it.
		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:
			return KoLConstants.NOCREATE;

		// Dictionaries and bridges should link to the chasm quest.
		case ItemPool.DICTIONARY:
		case ItemPool.BRIDGE:
			return KoLConstants.NOCREATE;

		// Blackbird components link to the black market map
		case ItemPool.BROKEN_WINGS:
		case ItemPool.SUNKEN_EYES:
			return KoLConstants.NOCREATE;

		// The eyepatch can be combined, but is usually an outfit piece
		case ItemPool.EYEPATCH:
			return KoLConstants.NOCREATE;

		// Spooky Fertilizer CAN be cooked, but almost always is used
		// for with the spooky temple map.
		case ItemPool.SPOOKY_FERTILIZER:
			return KoLConstants.NOCREATE;

		// Enchanted beans are primarily used for the beanstalk quest.
		case ItemPool.ENCHANTED_BEAN:
			if ( KoLCharacter.getLevel() >= 10 && !InventoryManager.hasItem( ItemPool.SOCK ) && !InventoryManager.hasItem( ItemPool.ROWBOAT ) )
			{
				return KoLConstants.NOCREATE;
			}
			break;
		}

		for ( int i = 0; i < creations.size(); ++i )
		{
			AdventureResult creation = (AdventureResult) creations.get( i );
			int mixingMethod = ConcoctionDatabase.getMixingMethod( creation );

			// Only accept if it's a creation method that the
			// editor kit currently understands and links.

			switch ( mixingMethod )
			{
			case KoLConstants.COMBINE:
			case KoLConstants.MIX:
			case KoLConstants.MIX_SPECIAL:
			case KoLConstants.MIX_SUPER:
			case KoLConstants.MIX_SALACIOUS:
			case KoLConstants.COOK:
			case KoLConstants.COOK_REAGENT:
			case KoLConstants.SUPER_REAGENT:
			case KoLConstants.DEEP_SAUCE:
			case KoLConstants.COOK_PASTA:
			case KoLConstants.JEWELRY:
			case KoLConstants.EXPENSIVE_JEWELRY:
				break;
			default:
				continue;
			}

			CreateItemRequest irequest = CreateItemRequest.getInstance( creation );

			if ( ConcoctionDatabase.isPermittedMethod( mixingMethod ) && irequest != null && irequest.getQuantityPossible() > 0 )
			{
				return mixingMethod;
			}
		}

		return KoLConstants.NOCREATE;
	}

	private static final boolean addEffectLink( String location, Matcher useLinkMatcher, StringBuffer buffer )
	{
		String message = useLinkMatcher.group(0);
		if ( message.indexOf( "You acquire an effect" ) == -1 )
		{
			return false;
		}

		String effect = useLinkMatcher.group(3);
		UseLink link = null;

		if ( effect.equals( "Filthworm Larva Stench" ) )
		{
			link = new UseLink( 0, "feeding chamber", "adventure.php?snarfblat=128" );
		}
		else if ( effect.equals( "Filthworm Drone Stench" ) )
		{
			link = new UseLink( 0, "guards' chamber", "adventure.php?snarfblat=129" );
		}
		else if ( effect.equals( "Filthworm Guard Stench" ) )
		{
			link = new UseLink(0, "queen's chamber", "adventure.php?snarfblat=130" );
		}
		else if ( effect.equals( "Knob Goblin Perfume" ) )
		{
			link = new UseLink(0, "king's chamber", "knob.php?king=1" );
		}
		else
		{
			return false;
		}

		String useType = link.getUseType();
		String useLocation = link.getUseLocation();

		useLinkMatcher.appendReplacement(
			buffer,
			"$1$2<b>$3</b>$4 <font size=1>[<a href=\"" + useLocation + "\">" + useType + "</a>]</font></td>" );
		return true;
	}

	private static final boolean addUseLink( int itemId, int itemCount, String location, Matcher useLinkMatcher, StringBuffer buffer )
	{
		int consumeMethod = ItemDatabase.getConsumptionType( itemId );
		int mixingMethod = shouldAddCreateLink( itemId, location );
		UseLink link;

		if ( mixingMethod != KoLConstants.NOCREATE )
		{
			link = getCreateLink( itemId, itemCount, mixingMethod );
		}
		else if ( consumeMethod == KoLConstants.NO_CONSUME || consumeMethod == KoLConstants.COMBAT_ITEM )
		{
			link = getNavigationLink( itemId, location );
		}
		else
		{
			link = getUseLink( itemId, itemCount, consumeMethod );
		}

		if ( link == null )
		{
			return false;
		}

		useLinkMatcher.appendReplacement( buffer, "$1$2<b>$3</b> "+ link.getItemHTML() );

		buffer.append( "</td>" );
		return true;
	}

	private static final UseLink getCreateLink( final int itemId, final int itemCount, final int mixingMethod )
	{
		switch ( mixingMethod )
		{
		case KoLConstants.COMBINE:
			return new UseLink( itemId, itemCount, "combine", KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "craft.php?mode=combine&a=" );

		case KoLConstants.MIX:
		case KoLConstants.MIX_SPECIAL:
		case KoLConstants.MIX_SUPER:
		case KoLConstants.MIX_SALACIOUS:
			return new UseLink( itemId, itemCount, "mix", "craft.php?mode=cocktail&a=" );

		case KoLConstants.COOK:
		case KoLConstants.COOK_REAGENT:
		case KoLConstants.SUPER_REAGENT:
		case KoLConstants.DEEP_SAUCE:
		case KoLConstants.COOK_PASTA:
		case KoLConstants.COOK_TEMPURA:
			return new UseLink( itemId, itemCount, "cook", "craft.php?mode=cook&a=" );

		case KoLConstants.JEWELRY:
		case KoLConstants.EXPENSIVE_JEWELRY:
			return new UseLink( itemId, itemCount, "jewelry", "craft.php?mode=jewelry&a=" );
		}

		return null;
	}

	private static final UseLink getUseLink( int itemId, int itemCount, int consumeMethod )
	{
		switch ( consumeMethod )
		{
		case KoLConstants.GROW_FAMILIAR:

			if ( itemId  == ItemPool.MOSQUITO_LARVA )
			{
				return new UseLink( itemId, "council", "council.php" );
			}

			return new UseLink( itemId, "grow", "inv_familiar.php?whichitem=" );

		case KoLConstants.CONSUME_EAT:

			if ( itemId == ItemPool.GOAT_CHEESE )
			{
				return new UseLink( itemId, InventoryManager.getCount( itemId ), "trapper.php" );
			}

			if ( !KoLCharacter.canEat() )
			{
				return null;
			}

			if ( itemId == ItemPool.BLACK_PUDDING )
			{
				return new UseLink( itemId, itemCount, "eat", "inv_eat.php?which=1&whichitem=", false );
			}

			return new UseLink( itemId, itemCount, "eat", "inv_eat.php?which=1&whichitem=" );

		case KoLConstants.CONSUME_DRINK:

			if ( !KoLCharacter.canDrink() )
			{
				return null;
			}

			return new UseLink( itemId, itemCount, "drink", "inv_booze.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_FOOD_HELPER:
			if ( !KoLCharacter.canEat() )
			{
				return null;
			}
			return new UseLink( itemId, 1, "eat with", "inv_use.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_DRINK_HELPER:
			if ( !KoLCharacter.canDrink() )
			{
				return null;
			}
			return new UseLink( itemId, 1, "drink with", "inv_use.php?which=1&whichitem=" );
		
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:

			int useCount = Math.min( UseItemRequest.maximumUses( itemId ), InventoryManager.getCount( itemId ) );

			if ( useCount == 0 )
			{
				return null;
			}

			if ( useCount == 1 )
			{
				String page = ( consumeMethod == KoLConstants.CONSUME_MULTIPLE ) ? "3" : "1";
				return new UseLink( itemId, useCount, "use", "inv_use.php?which=" + page + "&whichitem=" );
			}

			if ( Preferences.getBoolean( "relayUsesInlineLinks" ) )
			{
				return new UseLink( itemId, useCount, "use", "#" );
			}

			return new UseLink( itemId, useCount, "use", "multiuse.php?passitem=" );

		case KoLConstants.CONSUME_USE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:

			switch ( itemId )
			{
			case ItemPool.MACGUFFIN_DIARY:

				return new UseLink( itemId, itemCount, "read", "diary.php?textversion=1" );

			case ItemPool.SPOOKY_MAP:
			case ItemPool.SPOOKY_SAPLING:
			case ItemPool.SPOOKY_FERTILIZER:

				if ( !InventoryManager.hasItem( ItemPool.SPOOKY_MAP ) ||
				     !InventoryManager.hasItem( ItemPool.SPOOKY_SAPLING ) ||
				     !InventoryManager.hasItem( ItemPool.SPOOKY_FERTILIZER ) )
				{
					return null;
				}

				return new UseLink( ItemPool.SPOOKY_MAP, 1, "map", "inv_use.php?which=3&whichitem=" );

			case ItemPool.BLACK_MARKET_MAP:

				if ( !InventoryManager.hasItem( ItemPool.BROKEN_WINGS ) ||
				     !InventoryManager.hasItem( ItemPool.SUNKEN_EYES ) )
				{
					return null;
				}

				return new UseLink( ItemPool.BLACK_MARKET_MAP, 1, "map", "inv_use.php?which=3&whichitem=" );

			case ItemPool.COBBS_KNOB_MAP:

				if ( !InventoryManager.hasItem( ItemPool.ENCRYPTION_KEY ) )
				{
					return null;
				}

				return new UseLink( ItemPool.COBBS_KNOB_MAP, 1, "map", "inv_use.php?which=3&whichitem=" );

			case ItemPool.DINGHY_PLANS:

				if ( InventoryManager.hasItem( ItemPool.DINGY_PLANKS ) )
				{
					return new UseLink( itemId, 1, "use", "inv_use.php?which=3&whichitem=" );
				}

				if ( HermitRequest.getWorthlessItemCount() == 0 )
				{
					return new UseLink( ItemPool.DINGY_PLANKS, 1, "planks", "hermit.php?autopermit=on&action=trade&quantity=1&whichitem=" );
				}

				return new UseLink( itemId, "sewer", "sewer.php" );

			case ItemPool.DRUM_MACHINE:
			case ItemPool.CARONCH_MAP:
			case ItemPool.SPOOKY_PUTTY_MONSTER:
			case ItemPool.SHAKING_CAMERA:
			case ItemPool.CURSED_PIECE_OF_THIRTEEN:
				return new UseLink( itemId, itemCount, "use", "inv_use.php?which=3&whichitem=", false );

			default:

				return new UseLink( itemId, itemCount, "use", "inv_use.php?which=3&whichitem=" );
			}

		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_FAMILIAR:

			switch ( itemId )
			{
			case ItemPool.WORM_RIDING_HOOKS:

				return new UseLink( itemId, itemCount, "wormride", "beach.php?action=woodencity" );
			}

			int outfit = EquipmentDatabase.getOutfitWithItem( itemId );

			if ( outfit != -1 && EquipmentManager.hasOutfit( outfit ) )
			{
				return new UseLink( itemId, itemCount, "outfit", "inv_equip.php?action=outfit&which=2&whichoutfit=" + outfit );
			}
			
			if ( consumeMethod == KoLConstants.EQUIP_ACCESSORY &&
				!EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ).equals( EquipmentRequest.UNEQUIP ) && 
				!EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ).equals( EquipmentRequest.UNEQUIP ) && 
				!EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ).equals( EquipmentRequest.UNEQUIP ) )
			{
				return new UsesLink( new UseLink[] {
						new UseLink( itemId, itemCount, "acc1", "inv_equip.php?which=2&action=equip&slot=1&whichitem=" ),
						new UseLink( itemId, itemCount, "acc2", "inv_equip.php?which=2&action=equip&slot=2&whichitem=" ),
						new UseLink( itemId, itemCount, "acc3", "inv_equip.php?which=2&action=equip&slot=3&whichitem=" ),
					} );
			}

			return new UseLink( itemId, itemCount, "equip", "inv_equip.php?which=2&action=equip&whichitem=" );

		case KoLConstants.CONSUME_ZAP:

			return new UseLink( itemId, itemCount, "zap", "wand.php?whichwand=" );
		}

		return null;
	}

	private static final UseLink getNavigationLink( int itemId, String location )
	{
		String useType = null;
		String useLocation = null;

		switch ( itemId )
		{
		// Soft green echo eyedrop antidote gets an uneffect link

		case ItemPool.REMEDY:

			useType = "use";
			useLocation = "uneffect.php";
			break;

		// Strange leaflet gets a quick 'read' link which sends you
		// to the leaflet completion page.

		case ItemPool.STRANGE_LEAFLET:

			useType = "read";
			useLocation = "leaflet.php?action=auto";
			break;

		// You want to give the rusty screwdriver to the Untinker, so
		// make it easy.

		case ItemPool.RUSTY_SCREWDRIVER:

			useType = "visit untinker";
			useLocation = "town_right.php?place=untinker";
			break;

		// Hedge maze puzzle and hedge maze key have a link to the maze
		// for easy access.

		case ItemPool.HEDGE_KEY:
		case ItemPool.PUZZLE_PIECE:

			useType = "maze";
			useLocation = "hedgepuzzle.php";
			break;

		// Pixels have handy links indicating how many white pixels are
		// present in the player's inventory.

		case ItemPool.WHITE_PIXEL:
		case ItemPool.RED_PIXEL:
		case ItemPool.GREEN_PIXEL:
		case ItemPool.BLUE_PIXEL:

			int whiteCount = CreateItemRequest.getInstance( ItemPool.WHITE_PIXEL ).getQuantityPossible() + InventoryManager.getCount( ItemPool.WHITE_PIXEL );
			useType = whiteCount + " white";
			useLocation = "mystic.php";
			break;

		// Special handling for star charts, lines, and stars, where
		// KoLmafia shows you how many of each you have.

		case ItemPool.STAR_CHART:
		case ItemPool.STAR:
		case ItemPool.LINE:

			useType = InventoryManager.getCount( ItemPool.STAR_CHART ) + "," + InventoryManager.getCount( ItemPool.STAR ) + "," + InventoryManager.getCount( ItemPool.LINE );
			useLocation = "starchart.php";
			break;

		// Worthless items get a link to the hermit.

		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:

			useType = "hermit";
			useLocation = "hermit.php?autopermit=on";
			break;

		// The different kinds of ores will only have a link if they're
		// the ones applicable to the trapper quest.

		case ItemPool.LINOLEUM_ORE:
		case ItemPool.ASBESTOS_ORE:
		case ItemPool.CHROME_ORE:
		case ItemPool.LUMP_OF_COAL:

			if ( location.startsWith( "dwarffactory.php" ) )
			{
				useType = String.valueOf( InventoryManager.getCount( itemId ) );
				useLocation = "dwarfcontraption.php";
				break;
			}

			if ( itemId != ItemDatabase.getItemId( Preferences.getString( "trapperOre" ) ) )
			{
				return null;
			}

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "trapper.php";
			break;

		case ItemPool.FRAUDWORT:
		case ItemPool.SHYSTERWEED:
		case ItemPool.SWINDLEBLOSSOM:

			if ( InventoryManager.getCount( ItemPool.FRAUDWORT ) < 3 ||
			     InventoryManager.getCount( ItemPool.SHYSTERWEED ) < 3 ||
			     InventoryManager.getCount( ItemPool.SWINDLEBLOSSOM ) < 3 )
			{
				return null;
			}

			useType = "galaktik";
			useLocation = "galaktik.php";
			break;

		// Disintegrating sheet music gets a link which lets you sing it
		// to yourself. We'll call it "sing" for now.

		case ItemPool.SHEET_MUSIC:

			useType = "sing";
			useLocation = "curse.php?action=use&targetplayer=" + KoLCharacter.getPlayerId() + "&whichitem=";
			break;

		// Link which uses the plans when you acquire the planks.

		case ItemPool.DINGY_PLANKS:

			if ( !InventoryManager.hasItem( ItemPool.DINGHY_PLANS ) )
			{
				return null;
			}

			useType = "plans";
			useLocation = "inv_use.php?which=3&whichitem=";
			itemId = ItemPool.DINGHY_PLANS;
			break;

		// Link which uses the Knob map when you get the encryption key.

		case ItemPool.ENCRYPTION_KEY:

			if ( !InventoryManager.hasItem( ItemPool.COBBS_KNOB_MAP ) )
			{
				return null;
			}

			useType = "use map";
			useLocation = "inv_use.php?which=3&whichitem=";
			itemId = ItemPool.COBBS_KNOB_MAP;
			break;

		// Link to the guild upon completion of the Citadel quest.

		case ItemPool.CITADEL_SATCHEL:
		case ItemPool.THICK_PADDED_ENVELOPE:

			useType = "guild";
			useLocation = "guild.php?place=paco";
			break;
		
		// Link to the guild when receiving guild quest items.
		
		case ItemPool.FERNSWARTHYS_KEY:
			// ...except that the guild gives you the key again
			if ( location.startsWith( "guild.php" ) )
			{
				useType = "ruins";
				useLocation = "fernruin.php";
				break;
			}
			/*FALLTHRU*/
		case ItemPool.DUSTY_BOOK:
			useType = "guild";
			useLocation = "guild.php?place=ocg";
			break;

		// Link to the untinkerer if you find an abridged dictionary.

		case ItemPool.ABRIDGED:

			useType = "untinker";
			useLocation = "town_right.php?action=untinker&whichitem=";
			break;

		// Link to the chasm if you just untinkered a dictionary.

		case ItemPool.BRIDGE:
		case ItemPool.DICTIONARY:

			useType = "chasm";
			useLocation = "mountains.php?orcs=1";
			break;

		// Link to the frat house if you acquired a Spanish Fly

		case ItemPool.SPANISH_FLY:

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "adventure.php?snarfblat=27";
			break;

		// Link to Big Brother if you pick up a sand dollar

		case ItemPool.SAND_DOLLAR:

			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = "monkeycastle.php?who=2";
			break;

		// Link to the Old Man if you buy the damp old boot

		case ItemPool.DAMP_OLD_BOOT:

			useType = "old man";
			useLocation = "oldman.php?action=talk";
			break;

		// Link to use the Orcish Frat House Blueprints

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			
			useType = "use";
			useLocation = "inv_use.php?which=3&whichitem=";
			break;

		// Link to use the Black Market Map if you get blackbird parts

		case ItemPool.BROKEN_WINGS:
		case ItemPool.SUNKEN_EYES:

			if ( !InventoryManager.hasItem( ItemPool.BROKEN_WINGS ) ||
			     !InventoryManager.hasItem( ItemPool.SUNKEN_EYES ) ||
			     !InventoryManager.hasItem( ItemPool.BLACK_MARKET_MAP ) )
			{
				return null;
			}

			useType = "use map";
			useLocation = "inv_use.php?which=3&whichitem=";
			itemId = ItemPool.BLACK_MARKET_MAP;
			break;

		case ItemPool.GUNPOWDER:
			useType = String.valueOf( InventoryManager.getCount( itemId ) );
			useLocation = PyroRequest.pyroURL();
			break;

		case ItemPool.TOWEL:
			useType = "fold";
			useLocation = "inv_use.php?which=3&whichitem=";
			break;

		case ItemPool.BAT_BANDANA:
		case ItemPool.BONERDAGON_SKULL:
		case ItemPool.HOLY_MACGUFFIN:

			useType = "council";
			useLocation = "council.php";
			break;

		// Link to the Pretentious Artist when you find his last tool

		case ItemPool.PRETENTIOUS_PAINTBRUSH:
		case ItemPool.PRETENTIOUS_PALETTE:
		case ItemPool.PRETENTIOUS_PAIL:

			if ( !InventoryManager.hasItem( ItemPool.PRETENTIOUS_PAINTBRUSH ) ||
			     !InventoryManager.hasItem( ItemPool.PRETENTIOUS_PALETTE ) ||
			     !InventoryManager.hasItem( ItemPool.PRETENTIOUS_PAIL ) )
			{
				return null;
			}

			useType = "artist";
			useLocation = "town_wrong.php?place=artist";
			break;

		case ItemPool.FILTHWORM_QUEEN_HEART:

			useType = "stand";
			useLocation = "bigisland.php?place=orchard&action=stand";
			break;

		case ItemPool.EMPTY_AGUA_DE_VIDA_BOTTLE:

			useType = "gaze";
			useLocation = "memories.php";
			break;

		default:

			// Bounty items get a count and a link to the Bounty
			// Hunter Hunter.

			if ( ItemDatabase.isBountyItem( itemId ) )
			{
				if ( itemId != Preferences.getInteger( "currentBountyItem" ) )
				{
					Preferences.setInteger( "currentBountyItem", itemId );
				}
				useType = String.valueOf( InventoryManager.getCount( itemId ) );
				useLocation = "bhh.php";
			}
		}

		if ( useType == null || useLocation == null )
		{
			return null;
		}

		return new UseLink( itemId, useType, useLocation );
	}

	public static class UseLink
	{
		private int itemId;
		private int itemCount;
		private String useType;
		private String useLocation;
		private boolean inline;
		
		protected UseLink()
		{
		}

		public UseLink( int itemId, String useType, String useLocation )
		{
			this( itemId, 1, useType, useLocation );
		}

		public UseLink( int itemId, int itemCount, String useLocation )
		{
			this( itemId, itemCount, String.valueOf( itemCount ), useLocation );
		}

		public UseLink( int itemId, int itemCount, String useType, String useLocation )
		{
			this( itemId, itemCount, useType, useLocation, useLocation.startsWith( "inv" ) );
		}

		public UseLink( int itemId, int itemCount, String useType, String useLocation, boolean inline )
		{
			this.itemId = itemId;
			this.itemCount = itemCount;
			this.useType = useType;
			this.useLocation = useLocation;
			this.inline = inline;

			if ( this.useLocation.endsWith( "=" ) )
			{
				this.useLocation += this.itemId;
			}

			int formIndex = this.useLocation.indexOf( "?" );
			if ( formIndex != -1 )
			{
				this.useLocation += "&pwd=" + GenericRequest.passwordHash;
			}
		}

		public int getItemId()
		{
			return this.itemId;
		}

		public int getItemCount()
		{
			return this.itemCount;
		}

		public String getUseType()
		{
			return this.useType;
		}

		public String getUseLocation()
		{
			return this.useLocation;
		}

		public boolean showInline()
		{
			return this.inline && Preferences.getBoolean( "relayUsesInlineLinks" );
		}

		public String getItemHTML()
		{
			if ( this.useLocation.equals( "#" ) )
			{
				return "<font size=1>[<a href=\"javascript:" + "multiUse('multiuse.php'," + this.itemId + "," + this.itemCount + ");void(0);\">use multiple</a>]</font>";
			}

			if ( !this.showInline() )
			{
				return "<font size=1>[<a href=\"" + this.useLocation + "\">" + this.useType + "</a>]</font>";
			}

			String[] pieces = this.useLocation.toString().split( "\\?" );

			return "<font size=1>[<a href=\"javascript:" + "singleUse('" + pieces[ 0 ].trim() + "','" + pieces[ 1 ].trim() + "&ajax=1');void(0);\">" + this.useType + "</a>]</font>";
		}
	}
	
	public static class UsesLink
	extends UseLink
	{
		private UseLink[] links;
		
		public UsesLink( UseLink[] links )
		{
			this.links = links;
		}
		
		public String getItemHTML()
		{
			StringBuffer buf = new StringBuffer();
			for ( int i = 0; i < this.links.length; ++i )
			{
				if ( i > 0 ) buf.append( "&nbsp;" );
				buf.append( this.links[ i ].getItemHTML() );
			}
			return buf.toString();
		}
	}
}
