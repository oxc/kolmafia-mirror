/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CouncilFrame
	extends RequestFrame
{
	public static final GenericRequest COUNCIL_VISIT = new GenericRequest( "council.php" );

	public static final AdventureResult YETI_FUR = new AdventureResult( 388, 1 );

	private static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );
	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );

	public CouncilFrame()
	{
		super( "Council of Loathing" );
	}

	public void setVisible( boolean isVisible )
	{
		super.setVisible( isVisible );

		if ( isVisible )
		{
			CouncilFrame.COUNCIL_VISIT.responseText = null;
			this.displayRequest( CouncilFrame.COUNCIL_VISIT );
		}
	}

	public boolean hasSideBar()
	{
		return false;
	}

	public String getDisplayHTML( final String responseText )
	{
		return super.getDisplayHTML( responseText ).replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" ).replaceFirst(
			"table width=95%", "table width=100%" );
	}

	public static final void handleQuestChange( final String location, final String responseText )
	{
		if ( location.startsWith( "council" ) )
		{
			CouncilFrame.handleCouncilChange( responseText );
		}
		else if ( location.startsWith( "bigisland" ) )
		{
			IslandDecorator.parseBigIsland( location, responseText );
		}
		else if ( location.startsWith( "postwarisland" ) )
		{
			IslandDecorator.parsePostwarIsland( location, responseText );
		}
		else if ( location.startsWith( "friars" ) )
		{
			CouncilFrame.handleFriarsChange( responseText );
		}
		else if ( location.startsWith( "mountains" ) )
		{
			CouncilFrame.handleMountainsChange( responseText );
		}
		else if ( location.startsWith( "trapper" ) )
		{
			CouncilFrame.handleTrapperChange( location, responseText );
		}
		else if ( location.startsWith( "bhh" ) )
		{
			CoinMasterRequest.parseBountyVisit( location, responseText );
		}
		else if ( location.startsWith( "monkeycastle" ) )
		{
			CoinMasterRequest.parseBigBrotherVisit( location, responseText );
		}
		else if ( location.startsWith( "adventure" ) )
		{
			if ( location.indexOf( "216" ) != -1 )
			{
				CouncilFrame.handleTrickOrTreatingChange( responseText );
			}
			else if ( KoLCharacter.getInebriety() > 25 )
			{
				CouncilFrame.handleSneakyPeteChange( responseText );
			}
		}
		else if ( location.startsWith( "trickortreat" ) )
		{
			CouncilFrame.handleTrickOrTreatingChange( responseText );
		}
		else if ( location.startsWith( "woods" ) )
		{
			// If we see the Hidden Temple, mark it as unlocked
			if ( responseText.indexOf( "otherimages/woods/temple.gif" ) != -1 )
			{
				Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
			}
		}
	}

	private static final void handleSneakyPeteChange( final String responseText )
	{
		if ( responseText.indexOf( "You hand him your button and take his glowstick" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.NOVELTY_BUTTON );
			return;
		}

		if ( responseText.indexOf( "Ah, man, you dropped your crown back there!" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.TATTERED_PAPER_CROWN );
			return;
		}
	}

	private static final void handleTrickOrTreatingChange( final String responseText )
	{
		if ( responseText.indexOf( "pull the pumpkin off of your head" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.PUMPKINHEAD_MASK );
			return;
		}
		if ( responseText.indexOf( "gick all over your mummy costume" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.MUMMY_COSTUME );
			return;
		}
		if ( responseText.indexOf( "unzipping the mask and throwing it behind you" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.WOLFMAN_MASK );
			return;
		}
	}

	private static final void handleFriarsChange( final String responseText )
	{
		// "Thank you, Adventurer."

		if ( responseText.indexOf( "Thank you" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.DODECAGRAM, -1 );
			ResultProcessor.processItem( ItemPool.CANDLES, -1 );
			ResultProcessor.processItem( ItemPool.BUTTERKNIFE, -1 );
			int knownAscensions = Preferences.getInteger( "knownAscensions" );
			Preferences.setInteger( "lastFriarCeremonyAscension",  knownAscensions );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Taint cleansed." );
		}
	}

	private static final void handleMountainsChange( final String responseText )
	{
		// You approach the Orc Chasm, and whip out your trusty bridge.
		// You place the bridge across the chasm, and the path to the
		// Valley is clear.

		if ( responseText.indexOf( "trusty bridge" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.BRIDGE, -1 );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You have bridged the Orc Chasm." );
		}
	}

	private static final void handleTrapperChange( final String location, final String responseText )
	{
		if ( location.indexOf( "max=on" ) != -1 )
		{
			int furCount = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );
			ResultProcessor.processResult( CouncilFrame.YETI_FUR.getInstance( 0 - furCount ) );
			return;
		}

		if ( location.indexOf( "qty" ) != -1 )
		{
			Matcher qtyMatcher = CouncilFrame.QTY_PATTERN.matcher( location );
			if ( qtyMatcher.find() )
			{
				int furCount =
					Math.min(
						CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory ),
						StringUtilities.parseInt( qtyMatcher.group( 1 ) ) );
				ResultProcessor.processResult( CouncilFrame.YETI_FUR.getInstance( 0 - furCount ) );
			}

			return;
		}

		Matcher oreMatcher = CouncilFrame.ORE_PATTERN.matcher( responseText );
		if ( oreMatcher.find() )
		{
			Preferences.setString( "trapperOre", oreMatcher.group( 1 ) + " ore" );
		}

		// If you receive items from the trapper, then you
		// lose some items already in your inventory.

		if ( responseText.indexOf( "You acquire" ) == -1 )
		{
			return;
		}

		if ( responseText.indexOf( "asbestos" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "asbestos ore", -3, false ) );
		}
		else if ( responseText.indexOf( "linoleum" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "linoleum ore", -3, false ) );
		}
		else if ( responseText.indexOf( "chrome" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "chrome ore", -3, false ) );
		}
		else if ( responseText.indexOf( "goat cheese pizza" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "goat cheese", -6, false ) );
		}
	}

	public static final void unlockGoatlet()
	{
		if ( !EquipmentManager.hasOutfit( 8 ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You need a mining outfit to continue." );
			return;
		}

		if ( EquipmentManager.isWearingOutfit( 8 ) )
		{
			( new AdventureRequest( "Goatlet", "adventure.php", "60" ) ).run();
			return;
		}

		SpecialOutfit.createImplicitCheckpoint();
		( new EquipmentRequest( EquipmentDatabase.getOutfit( 8 ) ) ).run();
		( new AdventureRequest( "Goatlet", "adventure.php", "60" ) ).run();
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void handleCouncilChange( final String responseText )
	{
		Preferences.setInteger( "lastCouncilVisit", KoLCharacter.getLevel() );

		if ( responseText.indexOf( "500" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "mosquito larva", -1, false ) );
		}
		if ( responseText.indexOf( "batskin belt" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		}
		if ( responseText.indexOf( "dragonbone belt buckle" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "skull of the bonerdagon", -1, false ) );
		}
	}
}
