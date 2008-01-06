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

public abstract class SorceressLair
	extends StaticEntity
{
	private static final KoLRequest QUEST_HANDLER = new KoLRequest( "" );

	// Patterns for repeated usage.
	private static final Pattern MAP_PATTERN = Pattern.compile( "usemap=\"#(\\w+)\"" );
	private static final Pattern LAIR6_PATTERN = Pattern.compile( "lair6.php\\?place=(\\d+)" );
	private static final Pattern GATE_PATTERN = Pattern.compile( "<p>&quot;Through the (.*?)," );

	// Items for the entryway
	public static final AdventureResult NAGAMAR = new AdventureResult( 626, 1 );

	private static final AdventureResult WUSSINESS = new AdventureResult( "Wussiness", 0 );
	private static final AdventureResult HARDLY_POISONED = new AdventureResult( "Hardly Poisoned at All", 0 );
	private static final AdventureResult TELEPORTITIS = new AdventureResult( "Teleportitis", 0 );

	private static final AdventureResult STAR_SWORD = new AdventureResult( 657, 1 );
	private static final AdventureResult STAR_CROSSBOW = new AdventureResult( 658, 1 );
	private static final AdventureResult STAR_STAFF = new AdventureResult( 659, 1 );
	private static final AdventureResult STAR_HAT = new AdventureResult( 661, 1 );

	private static final AdventureResult STONE_BANJO = new AdventureResult( 53, 1 );
	private static final AdventureResult DISCO_BANJO = new AdventureResult( 54, 1 );
	private static final AdventureResult SHAGADELIC_BANJO = new AdventureResult( 2556, 1 );
	private static final AdventureResult ACOUSTIC_GUITAR = new AdventureResult( 404, 1 );
	private static final AdventureResult HEAVY_METAL_GUITAR = new AdventureResult( 507, 1 );
	private static final AdventureResult UKELELE = new AdventureResult( 2209, 1 );
	private static final AdventureResult ZIM_MERMANS_GUITAR = new AdventureResult( 2364, 1 );
	private static final AdventureResult SITAR = new AdventureResult( 2693, 1 );
	private static final AdventureResult [] STRINGED = new AdventureResult[]
	{
		SorceressLair.ACOUSTIC_GUITAR,
		SorceressLair.HEAVY_METAL_GUITAR,
		SorceressLair.STONE_BANJO,
		SorceressLair.DISCO_BANJO,
		SorceressLair.SHAGADELIC_BANJO,
		SorceressLair.UKELELE,
		SorceressLair.SITAR,
		SorceressLair.ZIM_MERMANS_GUITAR
	};

	private static final AdventureResult BROKEN_SKULL = new AdventureResult( 741, 1 );
	private static final AdventureResult BONE_RATTLE = new AdventureResult( 168, 1 );
	private static final AdventureResult TAMBOURINE = new AdventureResult( 740, 1 );
	private static final AdventureResult [] PERCUSSION = new AdventureResult[]
	{
		SorceressLair.BONE_RATTLE,
		SorceressLair.TAMBOURINE,
		SorceressLair.BROKEN_SKULL
	};

	private static final AdventureResult ACCORDION = new AdventureResult( 11, 1 );
	private static final AdventureResult ROCKNROLL_LEGEND = new AdventureResult( 50, 1 );
	private static final AdventureResult SQUEEZEBOX = new AdventureResult( 2557, 1 );
	private static final AdventureResult [] ACCORDIONS = new AdventureResult[]
	{
		SorceressLair.ACCORDION,
		SorceressLair.ROCKNROLL_LEGEND,
		SorceressLair.SQUEEZEBOX
	};

	private static final AdventureResult CLOVER = new AdventureResult( 24, 1 );

	private static final AdventureResult DIGITAL = new AdventureResult( 691, 1 );
	private static final AdventureResult RICHARD = new AdventureResult( 665, 1 );
	private static final AdventureResult SKELETON = new AdventureResult( 642, 1 );
	private static final AdventureResult KEY_RING = new AdventureResult( 643, 1 );

	private static final AdventureResult BORIS = new AdventureResult( 282, 1 );
	private static final AdventureResult JARLSBERG = new AdventureResult( 283, 1 );
	private static final AdventureResult SNEAKY_PETE = new AdventureResult( 284, 1 );
	private static final AdventureResult BALLOON = new AdventureResult( 436, 1 );

	// Results of key puzzles

	private static final AdventureResult STRUMMING = new AdventureResult( 736, 1 );
	private static final AdventureResult SQUEEZINGS = new AdventureResult( 737, 1 );
	private static final AdventureResult RHYTHM = new AdventureResult( 738, 1 );

	private static final AdventureResult BOWL = new AdventureResult( 729, 1 );
	private static final AdventureResult TANK = new AdventureResult( 730, 1 );
	private static final AdventureResult HOSE = new AdventureResult( 731, 1 );
	private static final AdventureResult HOSE_TANK = new AdventureResult( 732, 1 );
	private static final AdventureResult HOSE_BOWL = new AdventureResult( 733, 1 );
	private static final AdventureResult SCUBA = new AdventureResult( 734, 1 );

	// Items for the hedge maze

	public static final AdventureResult PUZZLE_PIECE = new AdventureResult( 727, 1 );
	public static final AdventureResult HEDGE_KEY = new AdventureResult( 728, 1 );

	// Items for the shadow battle

	private static final AdventureResult MIRROR_SHARD = new AdventureResult( "huge mirror shard", 1, false );

	private static final AdventureResult RED_POTION = new AdventureResult( "red pixel potion", 1 );
	private static final AdventureResult HIPPY_HEAL = new AdventureResult( "filthy poultice", 1 );
	private static final AdventureResult FRATBOY_HEAL = new AdventureResult( "gauze garter", 1 );

	// Gates, what they look like through the Telescope, the effects you
	// need to pass them, and where to get it

	public static final String[][] GATE_DATA =
	{
		// The first gate: Miscellaneous effects
		{
			"gate of hilarity",
			"a banana peel",
			"Comic Violence",
			"gremlin juice"
		},
		{
			"gate of humility",
			"a cowardly-looking man",
			"Wussiness",
			"wussiness potion"
		},
		{
			"gate of morose morbidity and moping",
			"a glum teenager",
			"Rainy Soul Miasma",
			"thin black candle",
			"picture of a dead guy's girlfriend"
		},
		{
			"gate of slack",
			"a smiling man smoking a pipe",
			"Extreme Muscle Relaxation",
			"Mick's IcyVapoHotness Rub"
		},
		{
			"gate of spirit",
			"an armchair",
			"Woad Warrior",
			"pygmy pygment"
		},
		{
			"gate of the porcupine",
			"a hedgehog",
			"Spiky Hair",
			"super-spikey hair gel"
		},
		{
			"twitching gates of the suc rose",
			"a rose",
			"Sugar Rush",
			"Angry Farmer candy",
			"Tasty Fun Good rice candy",
			"marzipan skull"
		},
		{
			"gate of the viper",
			"a coiled viper",
			"Deadly Flashing Blade",
			"adder bladder"
		},
		{
			"locked gate",
			"a raven",
			"Locks Like the Raven",
			"Black No. 2"
		},

		// The second gate: South of the Border effects
		{
			"gate of bad taste",
			"",
			"Spicy Limeness",
			"lime-and-chile-flavored chewing gum"
		},
		{
			"gate of flame",
			"",
			"Spicy Mouth",
			"jaba&ntilde;ero-flavored chewing gum"
		},
		{
			"gate of intrigue",
			"",
			"Mysteriously Handsome",
			"handsomeness potion"
		},
		{
			"gate of machismo",
			"",
			"Engorged Weapon",
			"Meleegra&trade; pills"
		},
		{
			"gate of mystery",
			"",
			"Mystic Pickleness",
			"pickle-flavored chewing gum"
		},
		{
			"gate of the dead",
			"",
			"Hombre Muerto Caminando",
			"marzipan skull"
		},
		{
			"gate of torment",
			"",
			"Tamarind Torment",
			"tamarind-flavored chewing gum"
		},
		{
			"gate of zest",
			"",
			"Spicy Limeness",
			"lime-and-chile-flavored chewing gum"
		},

		// The third gate: Bang potion effects
		{
			"gate of light",
			"",
			"Izchak's Blessing",
			"potion of blessing"
		},
		{
			"gate of that which is hidden",
			"",
			"Object Detection",
			"potion of detection"
		},
		{
			"gate of the mind",
			"",
			"Strange Mental Acuity",
			"potion of mental acuity"
		},
		{
			"gate of the observant",
			"",
			"Object Detection",
			"potion of detection"
		},
		{
			"gate of the ogre",
			"",
			"Strength of Ten Ettins",
			"potion of ettin strength"
		},
		{
			"gate that is not a gate",
			"",
			"Teleportitis",
			"potion of teleportitis"
		},
	};

	public static String gateName( final String[] gateData )
	{
		return gateData[ 0 ];
	}

	public static String gateDescription( final String[] gateData )
	{
		return gateData[ 1 ];
	}

	public static String gateEffect( final String[] gateData )
	{
		return gateData[ 2 ];
	}

	public static String[] findGateByName( final String gateName )
	{
		for ( int i = 0; i < SorceressLair.GATE_DATA.length; ++i )
		{
			if ( gateName.equalsIgnoreCase( SorceressLair.GATE_DATA[ i ][ 0 ] ) )
			{
				return SorceressLair.GATE_DATA[ i ];
			}
		}
		return null;
	}

	public static String[] findGateByDescription( final String text )
	{
		for ( int i = 0; i < SorceressLair.GATE_DATA.length; ++i )
		{
			String desc = SorceressLair.GATE_DATA[ i ][ 1 ];
			if ( desc.equals( "" ) )
			{
				continue;
			}
			if ( text.indexOf( desc ) != -1 )
			{
				return SorceressLair.GATE_DATA[ i ];
			}
		}
		return null;
	}

	// Guardians, what they look like through the Telescope, and the items
	// that defeat them

	public static final String[][] GUARDIAN_DATA =
	{
		{
			"beer batter",
			"tip of a baseball bat",
			"baseball"
		},
		{
			"best-selling novelist",
			"writing desk",
			"plot hole"
		},
		{
			"big meat golem",
			"huge face made of Meat",
			"meat vortex"
		},
		{
			"bowling cricket",
			"fancy-looking tophat",
			"sonar-in-a-biscuit"
		},
		{
			"bronze chef",
			"bronze figure holding a spatula",
			"leftovers of indeterminate origin"
		},
		{
			"collapsed mineshaft golem",
			"wooden beam",
			"stick of dynamite"
		},
		{
			"concert pianist",
			"long coattails",
			"Knob Goblin firecracker"
		},
		{
			"darkness",
			"strange shadow",
			"inkwell"
		},
		{
			"el diablo",
			"neck of a huge bass guitar",
			"mariachi G-string"
		},
		{
			"electron submarine",
			"periscope",
			"photoprotoneutron torpedo"
		},
		{
			"endangered inflatable white tiger",
			"giant white ear",
			"pygmy blowgun"
		},
		{
			"enraged cow",
			"pair of horns",
			"barbed-wire fence"
		},
		{
			"fancy bath slug",
			"slimy eyestalk",
			"fancy bath salts"
		},
		{
			"fickle finger of f8",
			"giant cuticle",
			"razor-sharp can lid"
		},
		{
			"flaming samurai",
			"flaming katana",
			"frigid ninja stars"
		},
		{
			"giant bee",
			"formidable stinger",
			"tropical orchid"
		},
		{
			"giant fried egg",
			"flash of albumen",
			"black pepper"
		},
		{
			"giant desktop globe",
			"the North Pole",
			"NG"
		},
		{
			"ice cube",
			"moonlight reflecting off of what appears to be ice",
			"can of hair spray"
		},
		{
			"malevolent crop circle",
			"amber waves of grain",
			"bronzed locust"
		},
		{
			"possessed pipe-organ",
			"pipes with steam shooting out of them",
			"powdered organs"
		},
		{
			"pretty fly",
			"translucent wing",
			"spider web"
		},
		{
			"tyrannosaurus tex",
			"large cowboy hat",
			"chaos butterfly"
		},
		{
			"vicious easel",
			"tall wooden frame",
			"disease"
		},
	};

	public static String guardianName( final String[] guardianData )
	{
		return guardianData[ 0 ];
	}

	public static String guardianDescription( final String[] guardianData )
	{
		return guardianData[ 1 ];
	}

	public static String guardianItem( final String[] guardianData )
	{
		return guardianData[ 2 ];
	}

	public static String[] findGuardianByName( final String guardianName )
	{
		for ( int i = 0; i < SorceressLair.GUARDIAN_DATA.length; ++i )
		{
			if ( guardianName.equalsIgnoreCase( SorceressLair.GUARDIAN_DATA[ i ][ 0 ] ) )
			{
				return SorceressLair.GUARDIAN_DATA[ i ];
			}
		}
		return null;
	}

	public static String[] findGuardianByDescription( final String text )
	{
		for ( int i = 0; i < SorceressLair.GUARDIAN_DATA.length; ++i )
		{
			String desc = SorceressLair.GUARDIAN_DATA[ i ][ 1 ];
			if ( desc.equals( "" ) )
			{
				continue;
			}
			if ( text.indexOf( desc ) != -1 )
			{
				return SorceressLair.GUARDIAN_DATA[ i ];
			}
		}
		return null;
	}

	// Items for the Sorceress's Chamber

	private static final FamiliarData STARFISH = new FamiliarData( 17 );
	private static final AdventureResult STARFISH_ITEM = new AdventureResult( 664, 1 );

	// Familiars and the familiars that defeat them
	private static final String[][] FAMILIAR_DATA =
	{
		{ "giant sabre-toothed lime", "Levitating Potato" },
		{ "giant mosquito", "Sabre-Toothed Lime" },
		{ "giant barrrnacle", "Angry Goat" },
		{ "giant goat", "Mosquito" },
		{ "giant potato", "Barrrnacle" }
	};

	private static final boolean checkPrerequisites( final int min, final int max )
	{
		KoLmafia.updateDisplay( "Checking prerequisites..." );

		// Make sure you have a starfish.  If not,
		// acquire the item and use it; use the
		// default acquisition mechanisms.

		if ( !KoLCharacter.getFamiliarList().contains( SorceressLair.STARFISH ) )
		{
			RequestThread.postRequest( new ConsumeItemRequest( SorceressLair.STARFISH_ITEM ) );
			if ( !KoLmafia.permitsContinue() )
			{
				return false;
			}
		}

		// Make sure he's been given the quest

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "main.php" ) );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "lair.php" ) == -1 )
		{
			// Visit the council to see if the quest can be
			// unlocked, but only if you've reached level 13.

			boolean unlockedQuest = false;
			if ( KoLCharacter.getLevel() >= 13 )
			{
				// We should theoretically be able to figure out
				// whether or not the quest is unlocked from the
				// HTML in the council request, but for now, use
				// this inefficient workaround.

				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "main.php" ) );
				unlockedQuest = SorceressLair.QUEST_HANDLER.responseText.indexOf( "lair.php" ) != -1;
			}

			if ( !unlockedQuest )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "You haven't been given the quest to fight the Sorceress!" );
				return false;
			}
		}

		// Make sure he can get to the desired area

		// Deduce based on which image map is used:
		//
		// NoMap = lair1
		// Map = lair1, lair3
		// Map2 = lair1, lair3, lair4
		// Map3 = lair1, lair3, lair4, lair5
		// Map4 = lair1, lair3, lair4, lair5, lair6

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair.php" ) );
		Matcher mapMatcher = SorceressLair.MAP_PATTERN.matcher( SorceressLair.QUEST_HANDLER.responseText );

		if ( mapMatcher.find() )
		{
			String map = mapMatcher.group( 1 );
			int reached;

			if ( map.equals( "NoMap" ) )
			{
				reached = 1;
			}
			else if ( map.equals( "Map" ) )
			{
				reached = 3;
			}
			else if ( map.equals( "Map2" ) )
			{
				reached = 4;
			}
			else if ( map.equals( "Map3" ) )
			{
				reached = 5;
			}
			else if ( map.equals( "Map4" ) )
			{
				reached = 6;
			}
			else
			{
				reached = 0;
			}

			if ( reached < min )
			{
				switch ( min )
				{
				case 0:
				case 1:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The sorceress quest has not yet unlocked." );
					return false;
				case 2:
				case 3:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must complete the entryway first." );
					return false;
				case 4:
				case 5:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must complete the hedge maze first." );
					return false;
				case 6:
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must complete the tower first." );
					return false;
				}
			}

			if ( reached > max )
			{
				KoLmafia.updateDisplay( "You're already past this script." );
				return false;
			}
		}

		// Otherwise, they've passed all the standard checks
		// on prerequisites.  Return true.

		return true;
	}

	private static final AdventureResult pickOne( final AdventureResult[] itemOptions )
	{
		if ( itemOptions.length == 1 )
		{
			return itemOptions[ 0 ];
		}

		for ( int i = 0; i < itemOptions.length; ++i )
		{
			if ( KoLConstants.inventory.contains( itemOptions[ i ] ) )
			{
				return itemOptions[ i ];
			}
		}

		for ( int i = 0; i < itemOptions.length; ++i )
		{
			if ( SorceressLair.isItemAvailable( itemOptions[ i ] ) )
			{
				return itemOptions[ i ];
			}
		}

		return itemOptions[ 0 ];
	}

	private static final boolean isItemAvailable( final AdventureResult item )
	{
		return KoLCharacter.hasItem( item, true );
	}

	public static final void completeCloveredEntryway()
	{
		SpecialOutfit.createImplicitCheckpoint();
		SorceressLair.completeEntryway( true );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public static final void completeCloverlessEntryway()
	{
		SpecialOutfit.createImplicitCheckpoint();
		SorceressLair.completeEntryway( false );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void completeEntryway( final boolean useCloverForSkeleton )
	{
		if ( !SorceressLair.checkPrerequisites( 1, 2 ) )
		{
			return;
		}

		// If you couldn't complete the gateway, then return
		// from this method call.

		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();
		if ( !SorceressLair.completeGateway() )
		{
			return;
		}

		List requirements = new ArrayList();

		// Next, figure out which instruments are needed for the final
		// stage of the entryway.

		requirements.add( SorceressLair.pickOne( STRINGED ) );
		AdventureResult percussion = SorceressLair.pickOne( PERCUSSION );
		requirements.add( percussion );
		requirements.add( SorceressLair.pickOne( ACCORDIONS ) );

		// If he brought a balloon monkey, get him an easter egg

		if ( SorceressLair.isItemAvailable( SorceressLair.BALLOON ) )
		{
			AdventureDatabase.retrieveItem( SorceressLair.BALLOON );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.BALLOON.getItemId() ) );
		}

		// Now, iterate through each of the completion steps;
		// at the end, check to make sure you've completed
		// all the needed requirements.

		requirements.addAll( SorceressLair.retrieveRhythm( useCloverForSkeleton ) );
		requirements.addAll( SorceressLair.retrieveStrumming() );
		requirements.addAll( SorceressLair.retrieveSqueezings() );
		requirements.addAll( SorceressLair.retrieveScubaGear() );

		RequestThread.postRequest( new FamiliarRequest( originalFamiliar ) );

		if ( !KoLmafia.checkRequirements( requirements ) || KoLmafia.refusesContinue() )
		{
			return;
		}

		if ( KoLCharacter.hasItem( SorceressLair.HOSE_BOWL ) && KoLCharacter.hasItem( SorceressLair.TANK ) )
		{
			( new UntinkerRequest( SorceressLair.HOSE_BOWL.getItemId() ) ).run();
		}

		RequestThread.postRequest( new EquipmentRequest( SorceressLair.SCUBA, KoLCharacter.ACCESSORY1 ) );

		KoLmafia.updateDisplay( "Pressing switch beyond odor..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?action=odor" ) );

		// If you decided to use a broken skull because
		// you had no other items, untinker the key.

		if ( percussion == SorceressLair.BROKEN_SKULL )
		{
			RequestThread.postRequest( new UntinkerRequest( SorceressLair.SKELETON.getItemId() ) );
			RequestThread.postRequest( ItemCreationRequest.getInstance( SorceressLair.BONE_RATTLE ) );
		}

		// Finally, arm the stone mariachis with their
		// appropriate instruments.

		KoLmafia.updateDisplay( "Arming stone mariachis..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?action=statues" ) );

		// "As the mariachis reach a dire crescendo (Hey, have you
		// heard my new band, Dire Crescendo?) the gate behind the
		// statues slowly grinds open, revealing the way to the
		// Sorceress' courtyard."

		// Just check to see if there is a link to lair3.php

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "lair3.php" ) == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to complete entryway." );
			return;
		}

		// This consumes the tablets

		StaticEntity.getClient().processResult( SorceressLair.RHYTHM.getNegation() );
		StaticEntity.getClient().processResult( SorceressLair.STRUMMING.getNegation() );
		StaticEntity.getClient().processResult( SorceressLair.SQUEEZINGS.getNegation() );

		KoLmafia.updateDisplay( "Sorceress entryway complete." );
	}

	private static final boolean completeGateway()
	{
		// Check to see if the person has crossed through the
		// gates already.  If they haven't, then that's the
		// only time you need the special effects.

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair1.php" ) );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "gatesdone" ) == -1 )
		{
			KoLmafia.updateDisplay( "Crossing three door puzzle..." );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair1.php?action=gates" ) );
			if ( !SorceressLair.passThreeGatePuzzle() )
			{
				return false;
			}

			// We want to remove unpleasant effects created by
			// consuming items used to pass the gates.
			//
			// Wussiness - removed by tiny house
			// Hardly Poisoned at All - removed by tiny house
			//
			// Teleportitis - removed by universal remedy

			if ( KoLConstants.activeEffects.contains( SorceressLair.WUSSINESS ) || KoLConstants.activeEffects.contains( SorceressLair.HARDLY_POISONED ) )
			{
				if ( KoLCharacter.hasItem( UneffectRequest.TINY_HOUSE ) )
				{
					RequestThread.postRequest( new ConsumeItemRequest( UneffectRequest.TINY_HOUSE ) );
				}
			}

			if ( KoLConstants.activeEffects.contains( SorceressLair.TELEPORTITIS ) )
			{
				if ( KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
				{
					RequestThread.postRequest( new UneffectRequest( SorceressLair.TELEPORTITIS ) );
				}
			}
		}

		// Now, unequip all of your equipment and cross through
		// the mirror. Process the mirror shard that results.

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "lair2.php" ) == -1 )
		{
			RequestThread.postRequest( new FamiliarRequest( FamiliarData.NO_FAMILIAR ) );
			RequestThread.postRequest( new EquipmentRequest( SpecialOutfit.BIRTHDAY_SUIT ) );

			// We will need to re-equip

			KoLmafia.updateDisplay( "Crossing mirror puzzle..." );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair1.php?action=mirror" ) );
		}

		return true;
	}

	private static final boolean passThreeGatePuzzle()
	{
		// Visiting the gates with the correct effects opens them.
		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "gatesdone.gif" ) != -1 )
		{
			return true;
		}

		// Get a list of items we need to consume to get effects
		// we don't already have

		Matcher gateMatcher = SorceressLair.GATE_PATTERN.matcher( SorceressLair.QUEST_HANDLER.responseText );
		List requirements = new ArrayList();

		SorceressLair.addGateItem( 1, gateMatcher, requirements );
		SorceressLair.addGateItem( 2, gateMatcher, requirements );
		SorceressLair.addGateItem( 3, gateMatcher, requirements );

		// Punt now if we couldn't parse a gate
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		List missing = new ArrayList();

		// Get the necessary items into inventory
		for ( int i = 0; i < requirements.size(); ++i )
		{
			AdventureResult item = (AdventureResult) requirements.get( i );
			// See if it's an unknown bang potion
			if ( item.getItemId() == -1 )
			{
				missing.add( item );
				continue;
			}

			// See if the user aborted
			if ( !KoLmafia.permitsContinue() )
			{
				missing.add( item );
				continue;
			}

			// Otherwise, move the item into inventory
			if ( !AdventureDatabase.retrieveItem( item ) )
			{
				missing.add( item );
			}
		}

		// If we're missing any items, report all of them
		if ( missing.size() > 0 )
		{
			String items = "";
			for ( int i = 0; i < missing.size(); ++i )
			{
				AdventureResult item = (AdventureResult) missing.get( i );
				if ( i > 0 )
				{
					items += " and ";
				}
				items += "a " + item.getName();
			}
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need " + items );
			return false;
		}

		// See if the user aborted
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		// Use the necessary items
		for ( int i = 0; i < requirements.size(); ++i )
		{
			AdventureResult item = (AdventureResult) requirements.get( i );
			RequestThread.postRequest( new ConsumeItemRequest( item ) );
		}

		// The gates should be passable. Visit them again.
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair1.php?action=gates" ) );
		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "gatesdone.gif" ) != -1 )
		{
			return true;
		}

		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to pass gates!" );
		return false;
	}

	private static final void addGateItem( final int gate, final Matcher gateMatcher, final List requirements )
	{
		// Find the name of the gate from the responseText
		if ( !gateMatcher.find() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Gate " + gate + " is missing." );
			return;
		}

		String gateName = gateMatcher.group( 1 );
		if ( gateName == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to detect gate" + gate );
			return;
		}

		// Find the gate in our data

		String[] gateData = SorceressLair.findGateByName( gateName );

		if ( gateData == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unrecognized gate: " + gateName );
			return;
		}

		// See if we have the needed effect already
		AdventureResult effect = new AdventureResult( SorceressLair.gateEffect( gateData ), 1, true );
		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			return;
		}

		// Pick an item that grants the effect
		AdventureResult[] items = new AdventureResult[ gateData.length - 3 ];
		for ( int i = 3; i < gateData.length; ++i )
		{
			String name = gateData[ i ];
			AdventureResult item =
				name.startsWith( "potion of " ) ? AdventureResult.bangPotion( name ) : new AdventureResult(
					name, 1, false );
			items[ i - 3 ] = item;
		}

		AdventureResult item = SorceressLair.pickOne( items );

		// Add the item to our list of requirements
		requirements.add( item );
	}

	private static final List retrieveRhythm( final boolean useCloverForSkeleton )
	{
		// Skeleton key and a clover unless you already have the
		// Really Evil Rhythms

		List requirements = new ArrayList();

		if ( SorceressLair.isItemAvailable( SorceressLair.RHYTHM ) )
		{
			return requirements;
		}

		if ( !SorceressLair.isItemAvailable( SorceressLair.SKELETON ) && SorceressLair.isItemAvailable( SorceressLair.KEY_RING ) )
		{
			RequestThread.postRequest( new ConsumeItemRequest( SorceressLair.KEY_RING ) );
		}

		if ( !AdventureDatabase.retrieveItem( SorceressLair.SKELETON ) )
		{
			requirements.add( SorceressLair.SKELETON );
			return requirements;
		}

		do
		{
			// The character needs to have at least 50 HP, or 25% of
			// maximum HP (whichever is greater) in order to play
			// the skeleton dice game, UNLESS you have a clover.

			int healthNeeded = Math.max( KoLCharacter.getMaximumHP() / 4, 50 );
			StaticEntity.getClient().recoverHP( healthNeeded + 1 );

			// Verify that you have enough HP to proceed with the
			// skeleton dice game.

			if ( KoLCharacter.getCurrentHP() <= healthNeeded )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "You must have more than " + healthNeeded + " HP to proceed." );
				return requirements;
			}

			if ( useCloverForSkeleton && SorceressLair.isItemAvailable( SorceressLair.CLOVER ) )
			{
				AdventureDatabase.retrieveItem( SorceressLair.CLOVER );
			}

			// Next, handle the form for the skeleton key to
			// get the Really Evil Rhythm. This uses up the
			// clover you had, so process it.

			KoLmafia.updateDisplay( "Inserting skeleton key..." );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.SKELETON.getItemId() ) );

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
			{
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=skel" ) );
				if ( useCloverForSkeleton && SorceressLair.isItemAvailable( SorceressLair.CLOVER ) )
				{
					StaticEntity.getClient().processResult( SorceressLair.CLOVER.getNegation() );
				}
			}
		}
		while ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "looks like I owe you a beating" ) != -1 );

		if ( !SorceressLair.isItemAvailable( SorceressLair.RHYTHM ) )
		{
			requirements.add( SorceressLair.RHYTHM );
		}

		return requirements;
	}

	private static final List retrieveStrumming()
	{
		// Decide on which star weapon should be available for
		// this whole process.

		List requirements = new ArrayList();

		if ( SorceressLair.isItemAvailable( SorceressLair.STRUMMING ) )
		{
			return requirements;
		}

		AdventureResult starWeapon;

		// See which ones are available

		boolean hasSword = KoLCharacter.hasItem( SorceressLair.STAR_SWORD );
		boolean hasStaff = KoLCharacter.hasItem( SorceressLair.STAR_STAFF );
		boolean hasCrossbow = KoLCharacter.hasItem( SorceressLair.STAR_CROSSBOW );

		// See which ones he can use

		boolean canUseSword = EquipmentDatabase.canEquip( SorceressLair.STAR_SWORD.getName() );
		boolean canUseStaff = EquipmentDatabase.canEquip( SorceressLair.STAR_STAFF.getName() );
		boolean canUseCrossbow = EquipmentDatabase.canEquip( SorceressLair.STAR_CROSSBOW.getName() );

		// Pick one that he has and can use

		if ( hasSword && canUseSword )
		{
			starWeapon = SorceressLair.STAR_SWORD;
		}
		else if ( hasStaff && canUseStaff )
		{
			starWeapon = SorceressLair.STAR_STAFF;
		}
		else if ( hasCrossbow && canUseCrossbow )
		{
			starWeapon = SorceressLair.STAR_CROSSBOW;
		}
		else if ( canUseSword && SorceressLair.isItemAvailable( SorceressLair.STAR_SWORD ) )
		{
			starWeapon = SorceressLair.STAR_SWORD;
		}
		else if ( canUseStaff && SorceressLair.isItemAvailable( SorceressLair.STAR_SWORD ) )
		{
			starWeapon = SorceressLair.STAR_STAFF;
		}
		else if ( canUseCrossbow && SorceressLair.isItemAvailable( SorceressLair.STAR_SWORD ) )
		{
			starWeapon = SorceressLair.STAR_CROSSBOW;
		}
		else if ( canUseSword )
		{
			starWeapon = SorceressLair.STAR_SWORD;
		}
		else if ( canUseStaff )
		{
			starWeapon = SorceressLair.STAR_STAFF;
		}
		else if ( canUseCrossbow )
		{
			starWeapon = SorceressLair.STAR_CROSSBOW;
		}
		else if ( hasSword )
		{
			starWeapon = SorceressLair.STAR_SWORD;
		}
		else if ( hasStaff )
		{
			starWeapon = SorceressLair.STAR_STAFF;
		}
		else if ( hasCrossbow )
		{
			starWeapon = SorceressLair.STAR_CROSSBOW;
		}
		else
		{
			starWeapon = SorceressLair.STAR_SWORD;
		}

		// Star equipment check.

		if ( !AdventureDatabase.retrieveItem( starWeapon ) )
		{
			requirements.add( starWeapon );
		}

		if ( !AdventureDatabase.retrieveItem( SorceressLair.STAR_HAT ) )
		{
			requirements.add( SorceressLair.STAR_HAT );
		}

		if ( !AdventureDatabase.retrieveItem( SorceressLair.RICHARD ) )
		{
			requirements.add( SorceressLair.RICHARD );
		}

		if ( !requirements.isEmpty() )
		{
			return requirements;
		}

		// If you can't equip the appropriate weapon and buckler,
		// then tell the player they lack the required stats.

		if ( !EquipmentDatabase.canEquip( starWeapon.getName() ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Stats too low to equip a star weapon." );
			return requirements;
		}

		if ( !EquipmentDatabase.canEquip( SorceressLair.STAR_HAT.getName() ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Stats too low to equip a star hat." );
			return requirements;
		}

		FamiliarData starfish = KoLCharacter.findFamiliar( "Star Starfish" );

		if ( starfish == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't own a Star Starfish!" );
			return requirements;
		}

		// Now handle the form for the star key to get
		// the Sinister Strumming.  Note that this will
		// require you to re-equip your star weapon and
		// a star buckler and switch to a starfish first.

		RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, KoLCharacter.OFFHAND ) );
		RequestThread.postRequest( new EquipmentRequest( starWeapon, KoLCharacter.WEAPON ) );
		RequestThread.postRequest( new EquipmentRequest( SorceressLair.STAR_HAT, KoLCharacter.HAT ) );
		RequestThread.postRequest( new FamiliarRequest( starfish ) );

		KoLmafia.updateDisplay( "Inserting Richard's star key..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.RICHARD.getItemId() ) );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=starcage" ) );

			// For unknown reasons, this doesn't always work
			// Error check the possibilities

			// "You beat on the cage with your weapon, but
			// to no avail.	 It doesn't appear to be made
			// out of the right stuff."

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "right stuff" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to equip a star weapon." );
			}

			// "A fragment of a line hits you really hard
			// on the arm, and it knocks you back into the
			// main cavern."

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "knocks you back" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to equip star buckler." );
			}

			// "Trog creeps toward the pedestal, but is
			// blown backwards.  You give up, and go back
			// out to the main cavern."

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "You give up" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Failed to equip star starfish." );
			}
		}

		return requirements;
	}

	private static final List retrieveSqueezings()
	{
		// Digital key unless you already have the Squeezings of Woe

		List requirements = new ArrayList();

		if ( SorceressLair.isItemAvailable( SorceressLair.SQUEEZINGS ) )
		{
			return requirements;
		}

		if ( !AdventureDatabase.retrieveItem( SorceressLair.DIGITAL ) )
		{
			requirements.add( SorceressLair.DIGITAL );
			return requirements;
		}

		// Now handle the form for the digital key to get
		// the Squeezings of Woe.

		KoLmafia.updateDisplay( "Inserting digital key..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.DIGITAL.getItemId() ) );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
		{
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sequence&seq1=up&seq2=up&seq3=down&seq4=down&seq5=left&seq6=right&seq7=left&seq8=right&seq9=b&seq10=a" ) );
		}

		return requirements;
	}

	private static final List retrieveScubaGear()
	{
		List requirements = new ArrayList();

		// The three hero keys are needed to get the SCUBA gear

		if ( SorceressLair.isItemAvailable( SorceressLair.SCUBA ) )
		{
			return requirements;
		}

		// Next, handle the three hero keys, which involve
		// answering the riddles with the forms of fish.

		if ( !SorceressLair.isItemAvailable( SorceressLair.BOWL ) && !SorceressLair.isItemAvailable( SorceressLair.HOSE_BOWL ) )
		{
			if ( !AdventureDatabase.retrieveItem( SorceressLair.BORIS ) )
			{
				requirements.add( SorceressLair.BORIS );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Boris's key..." );
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.BORIS.getItemId() ) );

				if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle1&answer=fish" ) );
				}
			}
		}

		if ( !SorceressLair.isItemAvailable( SorceressLair.TANK ) && !SorceressLair.isItemAvailable( SorceressLair.HOSE_TANK ) )
		{
			if ( !AdventureDatabase.retrieveItem( SorceressLair.JARLSBERG ) )
			{
				requirements.add( SorceressLair.JARLSBERG );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Jarlsberg's key..." );
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.JARLSBERG.getItemId() ) );

				if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle2&answer=phish" ) );
				}
			}
		}

		if ( !SorceressLair.isItemAvailable( SorceressLair.HOSE ) && !SorceressLair.isItemAvailable( SorceressLair.HOSE_TANK ) && !SorceressLair.isItemAvailable( SorceressLair.HOSE_BOWL ) )
		{
			if ( !AdventureDatabase.retrieveItem( SorceressLair.SNEAKY_PETE ) )
			{
				requirements.add( SorceressLair.SNEAKY_PETE );
			}
			else
			{
				KoLmafia.updateDisplay( "Inserting Sneaky Pete's key..." );
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?preaction=key&whichkey=" + SorceressLair.SNEAKY_PETE.getItemId() ) );

				if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "prepreaction" ) != -1 )
				{
					RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair2.php?prepreaction=sorcriddle3&answer=fsh" ) );
				}
			}
		}

		// Equip the SCUBA gear.  Attempting to retrieve it
		// will automatically create it.

		if ( !AdventureDatabase.retrieveItem( SorceressLair.SCUBA ) )
		{
			requirements.add( SorceressLair.SCUBA );
			return requirements;
		}

		return requirements;
	}

	private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
	private static final String[] DIRECTIONS = new String[] { "north", "east", "south", "west" };

	private static final String[][] EXIT_IDS = new String[][]
	{
		{ "Upper-Left", "Middle-Left", "Lower-Left" },
		{ "Upper-Middle", "Center", "Lower-Middle" },
		{ "Upper-Right", "Middle-Right", "Lower-Right" }
	};

	public static final void completeHedgeMaze()
	{
		if ( !SorceressLair.checkPrerequisites( 3, 3 ) )
		{
			return;
		}

		// Check to see if you've run out of puzzle pieces.
		// If you have, don't bother running the puzzle.

		if ( SorceressLair.PUZZLE_PIECE.getCount( KoLConstants.inventory ) == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of puzzle pieces." );
			return;
		}

		// Otherwise, check their current state relative
		// to the hedge maze, and begin!

		int[][] interest = new int[ 3 ][ 2 ];
		boolean[][][][] exits = new boolean[ 3 ][ 3 ][ 4 ][ 4 ];

		SorceressLair.initializeMaze( exits, interest );
		SorceressLair.generateMazeConfigurations( exits, interest );

		// First mission -- retrieve the key from the hedge
		// maze puzzle.

		if ( !KoLConstants.inventory.contains( SorceressLair.HEDGE_KEY ) )
		{
			SorceressLair.retrieveHedgeKey( exits, interest[ 0 ], interest[ 1 ] );

			// Retrieving the key after rotating the puzzle pieces
			// uses an adventure. If we ran out, we canceled.

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		// Second mission -- rotate the hedge maze until
		// the hedge path leads to the hedge door.

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "hedgepuzzle.php" ) );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "Click one" ) != -1 )
		{
			SorceressLair.finalizeHedgeMaze( exits, interest[ 0 ], interest[ 2 ] );

			// Navigating up to the tower door after rotating the
			// puzzle pieces requires an adventure. If we ran out,
			// we canceled.

			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
		}

		// Check to see if you ran out of puzzle pieces
		// in the middle -- if you did, update the user
		// display to say so.

		if ( SorceressLair.PUZZLE_PIECE.getCount( KoLConstants.inventory ) == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of puzzle pieces." );
			return;
		}

		KoLmafia.updateDisplay( "Hedge maze quest complete." );
	}

	private static final boolean rotateHedgePiece( final int x, final int y, final int rotations )
	{
		String url = "hedgepuzzle.php?action=" + ( 1 + y * 3 + x );

		for ( int i = 0; i < rotations && KoLmafia.permitsContinue(); ++i )
		{
			// We're out of puzzles unless the response says:
			// "Click one of the puzzle sections to rotate that
			// section 90 degrees to the right."

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "Click one" ) == -1 )
			{
				return false;
			}

			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( url ) );

			// If the topiary golem stole one of your hedge
			// pieces, take it away.

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "Topiary Golem" ) != -1 )
			{
				StaticEntity.getClient().processResult( SorceressLair.PUZZLE_PIECE.getNegation() );
			}
		}

		return KoLmafia.permitsContinue();
	}

	private static final void initializeMaze( final boolean[][][][] exits, final int[][] interest )
	{
		KoLmafia.updateDisplay( "Retrieving maze status..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "hedgepuzzle.php" ) );

		for ( int x = 0; x < 3; ++x )
		{
			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "entrance to this hedge maze is accessible when the " + SorceressLair.EXIT_IDS[ x ][ 2 ] ) != -1 )
			{
				interest[ 0 ][ 0 ] = x;
				interest[ 0 ][ 1 ] = 2;
			}
		}

		for ( int x = 0; x < 3; ++x )
		{
			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "exit of the hedge maze is accessible when the " + SorceressLair.EXIT_IDS[ x ][ 0 ] ) != -1 )
			{
				interest[ 2 ][ 0 ] = x;
				interest[ 2 ][ 1 ] = -1;
			}
		}

		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
				Matcher squareMatcher =
					Pattern.compile( "alt=\"" + SorceressLair.EXIT_IDS[ x ][ y ] + " Tile: (.*?)\"", Pattern.DOTALL ).matcher(
						SorceressLair.QUEST_HANDLER.responseText );

				if ( !squareMatcher.find() )
				{
					return;
				}

				String squareData = squareMatcher.group( 1 );

				for ( int i = 0; i < SorceressLair.DIRECTIONS.length; ++i )
				{
					exits[ x ][ y ][ 0 ][ i ] = squareData.indexOf( SorceressLair.DIRECTIONS[ i ] ) != -1;
				}

				if ( squareData.indexOf( "key" ) != -1 )
				{
					interest[ 1 ][ 0 ] = x;
					interest[ 1 ][ 1 ] = y;
				}
			}
		}
	}

	private static final void generateMazeConfigurations( final boolean[][][][] exits, final int[][] interest )
	{
		boolean allowConfig;

		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
				for ( int config = 3; config >= 0; --config ) // For all possible maze configurations
				{
					for ( int direction = 0; direction < 4; ++direction )
					{
						exits[ x ][ y ][ config ][ ( direction + config ) % 4 ] = exits[ x ][ y ][ 0 ][ direction ];
					}

					allowConfig = true;

					for ( int direction = 0; direction < 4; ++direction )
					{
						if ( exits[ x ][ y ][ config ][ direction ] && !SorceressLair.isExitPermitted(
							direction, x, y, interest ) )
						{
							allowConfig = false;
						}
					}

					if ( !allowConfig )
					{
						for ( int direction = 0; direction < 4; ++direction )
						{
							exits[ x ][ y ][ config ][ direction ] = false;
						}
					}
				}
			}
		}
	}

	private static final boolean isExitPermitted( final int direction, final int x, final int y, final int[][] interest )
	{
		switch ( direction )
		{
		case NORTH:
			return y > 0 || x == interest[ 2 ][ 0 ];
		case EAST:
			return x < 2;
		case SOUTH:
			return y < 2 || x == interest[ 0 ][ 0 ];
		case WEST:
			return x > 0;
		default:
			return false;
		}
	}

	private static final int[][] computeSolution( final boolean[][][][] exits, final int[] start,
		final int[] destination )
	{
		KoLmafia.updateDisplay( "Computing maze solution..." );

		boolean[][] visited = new boolean[ 3 ][ 3 ];
		int[][] currentSolution = new int[ 3 ][ 3 ];
		int[][] optimalSolution = new int[ 3 ][ 3 ];

		for ( int i = 0; i < 3; ++i )
		{
			for ( int j = 0; j < 3; ++j )
			{
				optimalSolution[ i ][ j ] = -1;
			}
		}

		SorceressLair.computeSolution(
			visited, currentSolution, optimalSolution, exits, start[ 0 ], start[ 1 ], destination[ 0 ],
			destination[ 1 ], SorceressLair.SOUTH );

		for ( int i = 0; i < 3; ++i )
		{
			for ( int j = 0; j < 3; ++j )
			{
				if ( optimalSolution[ i ][ j ] == -1 )
				{
					return null;
				}
			}
		}

		return optimalSolution;
	}

	private static final void computeSolution( final boolean[][] visited, final int[][] currentSolution,
		final int[][] optimalSolution, final boolean[][][][] exits, final int currentX, final int currentY,
		final int destinationX, final int destinationY, final int incomingDirection )
	{
		// If the destination has already been reached, replace the
		// optimum value, if this involves fewer rotations.

		if ( currentX == destinationX && currentY == destinationY )
		{
			// First, determine the minimum number of spins needed
			// for the destination square.

			if ( currentY != -1 )
			{
				for ( int i = 0; i < 4; ++i )
				{
					if ( exits[ currentX ][ currentY ][ i ][ incomingDirection ] )
					{
						currentSolution[ currentX ][ currentY ] = i;
						break;
					}
				}
			}

			int currentSum = 0;
			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					if ( visited[ i ][ j ] )
					{
						currentSum += currentSolution[ i ][ j ];
					}
				}
			}

			int optimalSum = 0;
			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					optimalSum += optimalSolution[ i ][ j ];
				}
			}

			if ( optimalSum >= 0 && currentSum > optimalSum )
			{
				return;
			}

			if ( currentY != -1 )
			{
				visited[ currentX ][ currentY ] = true;
			}

			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					optimalSolution[ i ][ j ] = visited[ i ][ j ] ? currentSolution[ i ][ j ] : 0;
				}
			}

			if ( currentY != -1 )
			{
				visited[ currentX ][ currentY ] = false;
			}

			return;
		}

		if ( currentY == -1 || visited[ currentX ][ currentY ] )
		{
			return;
		}

		int nextX = -1, nextY = -1;
		visited[ currentX ][ currentY ] = true;

		for ( int config = 0; config < 4; ++config )
		{
			if ( !exits[ currentX ][ currentY ][ config ][ incomingDirection ] )
			{
				continue;
			}

			for ( int i = 0; i < 4; ++i )
			{
				if ( i == incomingDirection || !exits[ currentX ][ currentY ][ config ][ i ] )
				{
					continue;
				}

				currentSolution[ currentX ][ currentY ] = config;
				switch ( i )
				{
				case NORTH:
					nextX = currentX;
					nextY = currentY - 1;
					break;
				case EAST:
					nextX = currentX + 1;
					nextY = currentY;
					break;
				case SOUTH:
					nextX = currentX;
					nextY = currentY + 1;
					break;
				case WEST:
					nextX = currentX - 1;
					nextY = currentY;
					break;
				}

				SorceressLair.computeSolution(
					visited, currentSolution, optimalSolution, exits, nextX, nextY, destinationX, destinationY,
					i > 1 ? i - 2 : i + 2 );
			}
		}

		visited[ currentX ][ currentY ] = false;
	}

	private static final void retrieveHedgeKey( final boolean[][][][] exits, final int[] start, final int[] destination )
	{
		// Before doing anything, check to see if the hedge
		// maze has already been solved for the key.

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "There is a key here." ) == -1 )
		{
			return;
		}

		int[][] solution = SorceressLair.computeSolution( exits, start, destination );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to compute maze solution." );
			return;
		}

		KoLmafia.updateDisplay( "Retrieving hedge key..." );

		for ( int x = 0; x < 3 && KoLmafia.permitsContinue(); ++x )
		{
			for ( int y = 0; y < 3 && KoLmafia.permitsContinue(); ++y )
			{
				if ( !SorceressLair.rotateHedgePiece( x, y, solution[ x ][ y ] ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of puzzle pieces." );
				}
			}
		}

		// The hedge maze has been properly rotated!  Now go ahead
		// and retrieve the key from the maze.

		if ( KoLmafia.permitsContinue() && SorceressLair.QUEST_HANDLER.responseText.indexOf( "Click one" ) != -1 )
		{
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair3.php?action=hedge" ) );
			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of adventures." );
			}
		}
	}

	private static final void finalizeHedgeMaze( final boolean[][][][] exits, final int[] start, final int[] destination )
	{
		int[][] solution = SorceressLair.computeSolution( exits, start, destination );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to compute maze solution." );
			return;
		}

		KoLmafia.updateDisplay( "Executing final rotations..." );

		for ( int x = 0; x < 3 && KoLmafia.permitsContinue(); ++x )
		{
			for ( int y = 0; y < 3 && KoLmafia.permitsContinue(); ++y )
			{
				if ( !SorceressLair.rotateHedgePiece( x, y, solution[ x ][ y ] ) )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of puzzle pieces." );
				}
			}
		}

		// The hedge maze has been properly rotated!  Now go ahead
		// and complete the hedge maze puzzle!

		if ( KoLmafia.permitsContinue() && SorceressLair.QUEST_HANDLER.responseText.indexOf( "Click one" ) != -1 )
		{
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair3.php?action=hedge" ) );

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "You're out of adventures." ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of adventures." );
			}
		}
	}

	public static final int fightAllTowerGuardians()
	{
		return SorceressLair.fightTowerGuardians( true );
	}

	public static final int fightMostTowerGuardians()
	{
		return SorceressLair.fightTowerGuardians( false );
	}

	public static final int fightTowerGuardians( boolean fightFamiliarGuardians )
	{
		if ( !SorceressLair.checkPrerequisites( 4, 6 ) )
		{
			return -1;
		}

		// Make sure that auto-attack is deactivated for the
		// shadow fight, otherwise it will fail.

		String previousAutoAttack = KoLSettings.getUserProperty( "defaultAutoAttack" );

		if ( !previousAutoAttack.equals( "0" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=0" );
		}

		// Determine which level you actually need to start from.

		KoLmafia.updateDisplay( "Climbing the tower..." );

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair4.php" ) );
		int currentLevel = 0;

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "lair5.php" ) != -1 )
		{
			// There is a link to higher in the tower.

			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair5.php" ) );
			currentLevel = 3;
		}

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "value=\"level1\"" ) != -1 )
		{
			currentLevel += 1;
		}
		else if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "value=\"level2\"" ) != -1 )
		{
			currentLevel += 2;
		}
		else if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "value=\"level3\"" ) != -1 )
		{
			currentLevel += 3;
		}
		else
		{
			currentLevel += 4;
		}

		int requiredItemId = -1;
		for ( int towerLevel = currentLevel; KoLCharacter.getAdventuresLeft() > 0 && KoLmafia.permitsContinue() && towerLevel <= 6; ++towerLevel )
		{
			requiredItemId = SorceressLair.fightGuardian( towerLevel );

			if ( !SorceressLair.QUEST_HANDLER.containsUpdate )
			{
				RequestThread.postRequest( CharpaneRequest.getInstance() );
			}

			StaticEntity.getClient().runBetweenBattleChecks( false );

			if ( requiredItemId != -1 )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
				return requiredItemId;
			}
		}

		// You must have at least 70 in all stats before you can enter
		// the chamber.

		if ( KoLCharacter.getBaseMuscle() < 70 || KoLCharacter.getBaseMysticality() < 70 || KoLCharacter.getBaseMoxie() < 70 )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "You can't enter the chamber unless all base stats are 70 or higher." );
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
			return -1;
		}

		// Figure out how far he's gotten into the Sorceress's Chamber
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php" ) );
		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "ascend.php" ) != -1 )
		{
			KoLmafia.updateDisplay( "You've already beaten Her Naughtiness." );
			return -1;
		}

		int n = -1;
		FamiliarData originalFamiliar = KoLCharacter.getFamiliar();

		Matcher placeMatcher = SorceressLair.LAIR6_PATTERN.matcher( SorceressLair.QUEST_HANDLER.responseText );
		if ( placeMatcher.find() )
		{
			n = StaticEntity.parseInt( placeMatcher.group( 1 ) );
		}

		if ( n < 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Server-side change detected.  Script aborted." );
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
			return -1;
		}

		if ( n == 0 )
		{
			RequestThread.postRequest( CharpaneRequest.getInstance() );
		}

		for ( ; n < 5 && KoLmafia.permitsContinue(); ++n )
		{
			switch ( n )
			{
			case 0:
				SorceressLair.findDoorCode();
				break;
			case 1:
				SorceressLair.reflectEnergyBolt();
				break;
			case 2:

				if ( !fightFamiliarGuardians )
				{
					KoLmafia.updateDisplay( "Path to shadow cleared." );
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
					return -1;
				}

				SorceressLair.fightShadow();
				break;

			case 3:

				if ( !fightFamiliarGuardians )
				{
					KoLmafia.updateDisplay( "Path to shadow cleared." );
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
					return -1;
				}

				SorceressLair.familiarBattle( 3 );
				break;

			case 4:

				if ( !fightFamiliarGuardians )
				{
					KoLmafia.updateDisplay( "Path to shadow cleared." );
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
					return -1;
				}

				SorceressLair.familiarBattle( 4 );
				break;
			}

			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );
				return -1;
			}
		}

		RequestThread.postRequest( new FamiliarRequest( originalFamiliar ) );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=" + previousAutoAttack );

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Her Naughtiness awaits." );
		}

		return -1;
	}

	private static final int fightGuardian( final int towerLevel )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're out of adventures." );
			return -1;
		}

		KoLmafia.updateDisplay( "Fighting guardian on level " + towerLevel + " of the tower..." );

		// Boldly climb the stairs.

		SorceressLair.QUEST_HANDLER.constructURLString( towerLevel <= 3 ? "lair4.php" : "lair5.php" );
		SorceressLair.QUEST_HANDLER.addFormField( "action", "level" + ( ( towerLevel - 1 ) % 3 + 1 ) );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER );

		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "You don't have time to mess around in the Tower." ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're out of adventures." );
			return -1;
		}

		// Parse response to see which item we need.
		AdventureResult guardianItem = SorceressLair.getGuardianItem();

		// With the guardian item retrieved, check to see if you have
		// the item, and if so, use it and report success.  Otherwise,
		// run away and report failure.

		SorceressLair.QUEST_HANDLER.constructURLString( "fight.php" );

		if ( KoLConstants.inventory.contains( guardianItem ) )
		{
			SorceressLair.QUEST_HANDLER.addFormField( "action", "useitem" );
			SorceressLair.QUEST_HANDLER.addFormField( "whichitem", String.valueOf( guardianItem.getItemId() ) );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER );

			return -1;
		}

		// Since we don't have the item, run away

		SorceressLair.QUEST_HANDLER.addFormField( "action", "runaway" );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER );

		if ( AdventureDatabase.retrieveItem( guardianItem ) )
		{
			return SorceressLair.fightGuardian( towerLevel );
		}

		return guardianItem.getItemId();
	}

	private static final AdventureResult getGuardianItem()
	{
		String[] guardian = SorceressLair.findGuardianByName( FightRequest.getCurrentKey() );
		if ( guardian != null )
		{
			return new AdventureResult( SorceressLair.guardianItem( guardian ), 1, false );
		}

		// Shouldn't get here.

		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Server-side change detected.  Script aborted." );
		return new AdventureResult( 666, 1 );
	}

	private static final void findDoorCode()
	{
		// Enter the chamber
		KoLmafia.updateDisplay( "Cracking door code..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=0" ) );

		// Talk to the guards and crack the code
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=0&preaction=lightdoor" ) );
		String code = SorceressLair.deduceCode( SorceressLair.QUEST_HANDLER.responseText );

		if ( code == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Couldn't solve door code. Do it yourself and come back!" );
			return;
		}

		// Check for success
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=0&action=doorcode&code=" + code ) );
		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "the door slides open" ) == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "I used the wrong code. Sorry." );
		}
	}

	private static final String deduceCode( final String text )
	{
		int start = text.indexOf( "<p>The guard playing South" );
		if ( start == -1 )
		{
			return null;
		}

		int end = text.indexOf( "<p>You roll your eyes." );
		if ( end == -1 )
		{
			return null;
		}

		// Pretty up the data
		String dialog = text.substring( start + 3, end ).replaceAll( "&quot;", "\"" );

		// Make an array of lines
		String lines[] = dialog.split( " *<p>" );
		if ( lines.length != 16 )
		{
			return null;
		}

		// Initialize the three digits of the code
		String digit1 = "0", digit2 = "0", digit3 = "0";
		Matcher matcher;

		// Check for variant, per Visual WIKI
		if ( lines[ 7 ].indexOf( "You're full of it" ) != -1 )
		{
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 5 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit1 = matcher.group( 1 );
			matcher = Pattern.compile( "it's (\\d)" ).matcher( lines[ 11 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit2 = matcher.group( 1 );
			matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 12 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit3 = matcher.group( 1 );
		}
		else
		{
			if ( lines[ 13 ].indexOf( "South" ) != -1 )
			{
				matcher = Pattern.compile( "digit is (\\d)" ).matcher( lines[ 5 ] );
			}
			else
			{
				matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[ 6 ] );
			}
			if ( !matcher.find() )
			{
				return null;
			}
			digit1 = matcher.group( 1 );
			matcher = Pattern.compile( "that's (\\d)" ).matcher( lines[ 8 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit2 = matcher.group( 1 );
			matcher = Pattern.compile( "It's (\\d)" ).matcher( lines[ 13 ] );
			if ( !matcher.find() )
			{
				return null;
			}
			digit3 = matcher.group( 1 );
		}

		return digit1 + digit2 + digit3;
	}

	private static final void reflectEnergyBolt()
	{
		// Get current equipment
		SpecialOutfit.createImplicitCheckpoint();

		// Equip the huge mirror shard
		RequestThread.postRequest( new EquipmentRequest( SorceressLair.MIRROR_SHARD, KoLCharacter.WEAPON ) );

		// Reflect the energy bolt
		KoLmafia.updateDisplay( "Reflecting energy bolt..." );
		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=1" ) );

		// If we unequipped anything, equip it again
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void fightShadow()
	{
		StaticEntity.getClient().recoverHP( KoLCharacter.getMaximumHP() );
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		int itemCount = 0;
		AdventureResult[] options =
			new AdventureResult[] { SorceressLair.RED_POTION, SorceressLair.HIPPY_HEAL, SorceressLair.FRATBOY_HEAL };
		for ( int i = 0; i < options.length; ++i )
		{
			itemCount += options[ i ].getCount( KoLConstants.inventory );
		}

		if ( itemCount < 6 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Insufficient healing items to continue." );
			return;
		}

		KoLmafia.updateDisplay( "Fighting your shadow..." );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "defaultAutoAttack=0" );

		// Start the battle!

		RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=2" ) );
		if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "You don't have time to mess around up here." ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're out of adventures." );
		}

		int itemIndex = 0;
		SorceressLair.QUEST_HANDLER.constructURLString( "fight.php" );

		for ( int i = 0; i < 6; ++i )
		{
			while ( !KoLConstants.inventory.contains( options[ itemIndex ] ) )
			{
				++itemIndex;
			}

			SorceressLair.QUEST_HANDLER.addFormField( "action", "useitem" );
			SorceressLair.QUEST_HANDLER.addFormField( "whichitem", String.valueOf( options[ itemIndex ].getItemId() ) );

			if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				++i; // Increment the rounds elapsed

				boolean needsIncrement =
					!KoLConstants.inventory.contains( options[ itemIndex ] ) || options[ itemIndex ].getCount( KoLConstants.inventory ) < 2;

				if ( needsIncrement )
				{
					++itemIndex;
					while ( !KoLConstants.inventory.contains( options[ itemIndex ] ) )
					{
						++itemIndex;
					}
				}

				SorceressLair.QUEST_HANDLER.addFormField(
					"whichitem2", String.valueOf( options[ itemIndex ].getItemId() ) );
				RequestThread.postRequest( SorceressLair.QUEST_HANDLER );

				if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "fight.php" ) == -1 )
				{
					break;
				}
			}
		}

		if ( KoLCharacter.getCurrentHP() > 0 )
		{
			KoLmafia.updateDisplay( "Your shadow has been defeated." );
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to defeat your shadow." );
		}
	}

	public static final void makeGuardianItems()
	{
		if ( KoLSettings.getIntegerProperty( "lastTowerClimb" ) == KoLCharacter.getAscensions() )
		{
			return;
		}

		for ( int i = 0; i < SorceressLair.GUARDIAN_DATA.length; ++i )
		{
			String name = SorceressLair.guardianItem( SorceressLair.GUARDIAN_DATA[ i ] );
			AdventureResult item = new AdventureResult( name, 1, false );
			if ( !KoLConstants.inventory.contains( item ) )
			{
				if ( SorceressLair.isItemAvailable( item ) || NPCStoreDatabase.contains( name ) )
				{
					AdventureDatabase.retrieveItem( item );
				}
			}
		}

		KoLSettings.setUserProperty( "lastTowerClimb", String.valueOf( KoLCharacter.getAscensions() ) );
	}

	private static final void familiarBattle( final int n )
	{
		SorceressLair.familiarBattle( n, true );
	}

	private static final void familiarBattle( final int n, final boolean requiresHeal )
	{
		// Ensure that the player has more than 50 HP, since
		// you cannot enter the familiar chamber with less.

		String race = null;
		FamiliarData familiar = null;

		if ( requiresHeal )
		{
			StaticEntity.getClient().recoverHP( 51 );

			// Need more than 50 hit points.  Abort if this is
			// not the case.

			if ( KoLCharacter.getCurrentHP() <= 50 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must have more than 50 HP to proceed." );
				return;
			}

			// If you need to heal, then obviously, you don't know
			// what familiar you have.  Change to a random familiar.

			while ( familiar == null )
			{
				race =
					SorceressLair.FAMILIAR_DATA[ KoLConstants.RNG.nextInt( SorceressLair.FAMILIAR_DATA.length ) ][ 1 ];
				if ( !race.equals( KoLCharacter.getFamiliar().getRace() ) )
				{
					familiar = KoLCharacter.findFamiliar( race );
				}
			}

			RequestThread.postRequest( new FamiliarRequest( familiar ) );
		}

		// Make sure that the current familiar is at least twenty
		// pounds, if it's one of the ones which can be used against
		// the tower familiars; otherwise, it won't survive.

		if ( FamiliarTrainingFrame.buffFamiliar( 20 ) || requiresHeal )
		{
			KoLmafia.updateDisplay( "Facing giant familiar..." );
			RequestThread.postRequest( SorceressLair.QUEST_HANDLER.constructURLString( "lair6.php?place=" + n ) );

			// If you do not successfully pass the familiar, you
			// will get a "stomp off in a huff" message.

			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( "stomp off in a huff" ) == -1 )
			{
				return;
			}
		}

		// Find the necessary familiar and see if the player has one.

		race = null;
		familiar = null;

		for ( int i = 0; i < SorceressLair.FAMILIAR_DATA.length && race == null; ++i )
		{
			if ( SorceressLair.QUEST_HANDLER.responseText.indexOf( SorceressLair.FAMILIAR_DATA[ i ][ 0 ] ) != -1 )
			{
				race = SorceressLair.FAMILIAR_DATA[ i ][ 1 ];
				familiar = KoLCharacter.findFamiliar( race );
			}
		}

		// If not, tell the player to get one and come back.

		if ( familiar == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Come back with a 20 pound " + race );
			return;
		}

		// Switch to the required familiar
		RequestThread.postRequest( new FamiliarRequest( familiar ) );

		// If we can buff it to 20 pounds, try again.
		if ( !FamiliarTrainingFrame.buffFamiliar( 20 ) )
		{
			// We can't buff it high enough. Train it.
			if ( !FamiliarTrainingFrame.levelFamiliar( 20, FamiliarTrainingFrame.BUFFED, false ) )
			{
				return;
			}
		}

		// We're good to go. Fight!
		SorceressLair.familiarBattle( n, false );
	}
}
