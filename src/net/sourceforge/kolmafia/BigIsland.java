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

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigIsland
	implements KoLConstants
{
	private static AreaCombatData fratboyBattlefield =
		AdventureDatabase.getAreaCombatData( "Battlefield (Frat Uniform)" );
	private static AreaCombatData hippyBattlefield =
		AdventureDatabase.getAreaCombatData( "Battlefield (Hippy Uniform)" );

	private static final String progressLineStyle = "<td style=\"color: red;font-size: 80%\" align=center>";

	private static String missingGremlinTool = null;

	private static int fratboysDefeated = 0;
	private static int fratboyImage = 0;
	private static int fratboyMin = 0;
	private static int fratboyMax = 0;

	private static int hippiesDefeated = 0;
	private static int hippyImage = 0;
	private static int hippyMin = 0;
	private static int hippyMax = 0;

	// Data about current fight
	private static boolean fratboy = false;
	private static int lastFratboysDefeated = 0;
	private static int lastHippiesDefeated = 0;

	// Data about sidequests
	private static String currentJunkyardTool = "";
	private static String currentJunkyardLocation = "";
	private static int lastNunneryMeat = 0;
	private static int currentNunneryMeat = 0;

	private static final Pattern MAP_PATTERN = Pattern.compile( "bfleft(\\d*).*bfright(\\d*)", Pattern.DOTALL );
	private static final Pattern JUNKYARD_PATTERN =
		Pattern.compile( "The last time I saw my (.*?), (it was|they were) (.*?)\\.", Pattern.DOTALL );

	private static final AdventureResult JAM_FLYERS = new AdventureResult( 2404, -1 );
	private static final AdventureResult ROCK_FLYERS = new AdventureResult( 2405, -1 );

	private static final AdventureResult MAGNET = new AdventureResult( 2497, -1 );
	private static final AdventureResult HAMMER = new AdventureResult( 2498, -1 );
	private static final AdventureResult SCREWDRIVER = new AdventureResult( 2499, -1 );
	private static final AdventureResult PLIERS = new AdventureResult( 2500, -1 );
	private static final AdventureResult WRENCH = new AdventureResult( 2501, -1 );

	public static final int NONE = 0;
	public static final int JUNKYARD = 1;
	public static final int ORCHARD = 2;
	public static final int ARENA = 3;
	public static final int FARM = 4;
	public static final int LIGHTHOUSE = 5;
	public static final int NUNS = 6;

	private static int quest = BigIsland.NONE;

	// KoLmafia images showing each quest area on bigisland.php

	private static final String IMAGE_ROOT = "http://images.kingdomofloathing.com/otherimages/bigisland/";
	private static final String LOCAL_ROOT = "images/otherimages/bigisland/";

	private static final String[] SIDEQUEST_IMAGES =
	{
		null, // NONE
		BigIsland.IMAGE_ROOT + "2.gif", // JUNKYARD
		BigIsland.IMAGE_ROOT + "3.gif", // ORCHARD
		BigIsland.IMAGE_ROOT + "6.gif", // ARENA
		BigIsland.IMAGE_ROOT + "15.gif", // FARM
		BigIsland.IMAGE_ROOT + "17.gif", // LIGHTHOUSE
		BigIsland.IMAGE_ROOT + "19.gif", // NUNS
	};

	// Here are JHunz's replacement images for Big Island sidequest areas
	// from his BattlefieldCounter Greasemonkey script:
	//
	//	http://userscripts.org/scripts/show/11720

	private static final String[] FRAT_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		BigIsland.LOCAL_ROOT + "2F.gif",

		// ORCHARD = 2
		BigIsland.LOCAL_ROOT + "3F.gif",

		// ARENA = 3
		BigIsland.LOCAL_ROOT + "6F.gif",

		// FARM = 4
		BigIsland.LOCAL_ROOT + "15F.gif",

		// LIGHTHOUSE = 5
		BigIsland.LOCAL_ROOT + "17F.gif",

		// NUNS = 6
		BigIsland.LOCAL_ROOT + "19F.gif",
	};

	private static final String[] HIPPY_IMAGES =
	{
		// NONE = 0
		null,

		// JUNKYARD = 1
		BigIsland.LOCAL_ROOT + "2H.gif",

		// ORCHARD = 2
		BigIsland.LOCAL_ROOT + "3H.gif",

		// ARENA = 3
		BigIsland.LOCAL_ROOT + "6H.gif",

		// FARM = 4
		BigIsland.LOCAL_ROOT + "15H.gif",

		// LIGHTHOUSE = 5
		BigIsland.LOCAL_ROOT + "17H.gif",

		// NUNS = 6
		BigIsland.LOCAL_ROOT + "19H.gif",
	};

	/*
	 * Methods to decorate the Fight page
	 */

	public static final void addNunneryMeat( final AdventureResult result )
	{
		int delta = result.getCount();
		BigIsland.lastNunneryMeat = BigIsland.currentNunneryMeat;
		BigIsland.currentNunneryMeat =
			KoLSettings.incrementIntegerProperty( "currentNunneryMeat", delta, 100000, false );
	}

	public static final void decorateThemtharFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		int current = BigIsland.currentNunneryMeat;
		if ( current < 100000 )
		{
			int left = 100000 - current;
			int delta = current - BigIsland.lastNunneryMeat;
			int turns = (int) Math.ceil( (double) left / (double) delta );
			String message =
				"<p><center>" + KoLConstants.COMMA_FORMAT.format( current ) + " Meat recovered, " + KoLConstants.COMMA_FORMAT.format( left ) + " left (" + turns + " turns).<br>";

			buffer.insert( index, message );
		}

		// "Well," you say, "it would really help the war effort if
		// your convent could serve as a hospital for our wounded
		// troops."
		if ( buffer.indexOf( "could serve as a hospital" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "hippy" );
		}
		else if ( buffer.indexOf( "could serve as a massage parlor" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "fratboy" );
		}
	}

	private static final String[] GREMLIN_TOOLS =
	{
		"It whips out a hammer",
		"He whips out a crescent wrench",
		"It whips out a pair of pliers",
		"It whips out a screwdriver",
	};

	public static final void decorateGremlinFight( final StringBuffer buffer )
	{
		// Color the tool in the monster spoiler text
		if ( BigIsland.missingGremlinTool == null && !BigIsland.currentJunkyardTool.equals( "" ) )
		{
			StaticEntity.singleStringReplace(
				buffer, BigIsland.currentJunkyardTool,
				"<font color=#DD00FF>" + BigIsland.currentJunkyardTool + "</font>" );
		}

		for ( int i = 0; i < BigIsland.GREMLIN_TOOLS.length; ++i )
		{
			String tool = BigIsland.GREMLIN_TOOLS[ i ];
			StaticEntity.singleStringReplace( buffer, tool, "<font color=#DD00FF>" + tool + "</font>" );
		}
	}

	public static final void appendMissingGremlinTool( final StringBuffer buffer )
	{
		if ( BigIsland.missingGremlinTool != null )
		{
			buffer.append( "<br />This gremlin does <b>NOT</b> have a " + BigIsland.missingGremlinTool );
		}
	}

	private static final int[] AREA_UNLOCK =
	{
		64,
		192,
		458
	};

	private static final String[] HIPPY_AREA_UNLOCK =
	{
		"Lighthouse",
		"Junkyard",
		"Arena"
	};

	private static final String[] FRATBOY_AREA_UNLOCK =
	{
		"Orchard",
		"Nunnery",
		"Farm"
	};

	private static final String openArea( final int last, final int current, final String[] areas )
	{
		for ( int i = 0; i < BigIsland.AREA_UNLOCK.length; ++i )
		{
			int threshold = BigIsland.AREA_UNLOCK[ i ];
			if ( last < threshold && current >= threshold )
			{
				return areas[ i ];
			}
		}
		return null;
	}

	public static final void decorateBattlefieldFight( final StringBuffer buffer )
	{
		int index = buffer.indexOf( "<!--WINWINWIN-->" );
		if ( index == -1 )
		{
			return;
		}

		// Don't bother showing progress of the war if you've just won
		String monster = FightRequest.getLastMonsterName();
		if ( monster.equalsIgnoreCase( "Big Wisniewski" ) || monster.equalsIgnoreCase( "Man" ) )
		{
			return;
		}

		String side;
		int delta;
		int last;
		int current;
		String area;

		if ( BigIsland.fratboy )
		{
			last = BigIsland.lastFratboysDefeated;
			current = BigIsland.fratboysDefeated;
			delta = current - last;
			side = delta == 1 ? "frat boy" : "frat boys";
			area = BigIsland.openArea( last, current, BigIsland.HIPPY_AREA_UNLOCK );
		}
		else
		{
			last = BigIsland.lastHippiesDefeated;
			current = BigIsland.hippiesDefeated;
			delta = current - last;
			side = delta == 1 ? "hippy" : "hippies";
			area = BigIsland.openArea( last, current, BigIsland.FRATBOY_AREA_UNLOCK );
		}

		area = area == null ? "" : " The " + area + " is now accessible in this uniform!<br>";
		String message =
			"<p><center>" + delta + " " + side + " defeated; " + current + " down, " + ( 1000 - current ) + " left.<br>" + area;

		buffer.insert( index, message );
	}

	/*
	 * Method to decorate the Big Island map
	 */

	// Decorate the HTML with custom goodies
	public static final void decorateBigIsland( final String url, final StringBuffer buffer )
	{
		// Quest-specific page decorations
		BigIsland.decorateJunkyard( buffer );

		// Find the table that contains the map.
		String fratboyMessage =
			BigIsland.sideSummary(
				"frat boys", BigIsland.fratboysDefeated, BigIsland.fratboyImage, BigIsland.fratboyMin,
				BigIsland.fratboyMax );
		String hippyMessage =
			BigIsland.sideSummary(
				"hippies", BigIsland.hippiesDefeated, BigIsland.hippyImage, BigIsland.hippyMin, BigIsland.hippyMax );
		String row =
			"<tr><td><center><table width=100%><tr>" + BigIsland.progressLineStyle + fratboyMessage + "</td>" + BigIsland.progressLineStyle + hippyMessage + "</td>" + "</tr></table></td></tr>";

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Mysterious Island of Mystery</b></td>" );
		if ( tableIndex != -1 )
		{
			buffer.insert( tableIndex, row );
		}

		// Now replace sidequest location images for completed quests
		BigIsland.sidequestImage( buffer, "sidequestArenaCompleted", BigIsland.ARENA );
		BigIsland.sidequestImage( buffer, "sidequestFarmCompleted", BigIsland.FARM );
		BigIsland.sidequestImage( buffer, "sidequestJunkyardCompleted", BigIsland.JUNKYARD );
		BigIsland.sidequestImage( buffer, "sidequestLighthouseCompleted", BigIsland.LIGHTHOUSE );
		BigIsland.sidequestImage( buffer, "sidequestNunsCompleted", BigIsland.NUNS );
		BigIsland.sidequestImage( buffer, "sidequestOrchardCompleted", BigIsland.ORCHARD );
	}

	private static final String sideSummary( final String side, final int kills, final int image, int min, final int max )
	{
		if ( kills > min )
		{
			min = kills;
		}
		int minLeft = 1000 - max;
		int maxLeft = 1000 - min;
		String range =
			minLeft == maxLeft ? String.valueOf( minLeft ) : String.valueOf( minLeft ) + "-" + String.valueOf( maxLeft );
		return kills + " " + side + " defeated; " + range + " left (image " + image + ").";
	}

	private static final void sidequestImage( final StringBuffer buffer, final String setting, final int quest )
	{
		String status = KoLSettings.getUserProperty( setting );
		String image;
		if ( status.equals( "fratboy" ) )
		{
			image = BigIsland.FRAT_IMAGES[ quest ];
		}
		else if ( status.equals( "hippy" ) )
		{
			image = BigIsland.HIPPY_IMAGES[ quest ];
		}
		else
		{
			return;
		}

		String old = BigIsland.SIDEQUEST_IMAGES[ quest ];
		StaticEntity.singleStringReplace( buffer, old, image );
	}

	public static final void decorateJunkyard( final StringBuffer buffer )
	{
		if ( BigIsland.currentJunkyardTool.equals( "" ) )
		{
			return;
		}

		int tableIndex =
			buffer.indexOf( "<tr><td style=\"color: white;\" align=center bgcolor=blue><b>The Junkyard</b></td>" );
		if ( tableIndex == -1 )
		{
			return;
		}

		String row =
			"<tr><td><center><table width=100%><tr>" + BigIsland.progressLineStyle + "Look for the " + BigIsland.currentJunkyardTool + " " + BigIsland.currentJunkyardLocation + ".</td>" + "</tr></table></td></tr>";

		buffer.insert( tableIndex, row );
	}

	public static final void startFight()
	{
		BigIsland.missingGremlinTool = null;
	}

	/*
	 * Methods to mine data from request responses
	 */

	public static void handleGremlin( final String responseText )
	{
		// Batwinged Gremlin has molybdenum hammer OR
		// "It does a bombing run over your head..."

		// Erudite Gremlin has molybdenum crescent wrench OR
		// "He uses the random junk around him to make an automatic
		// eyeball-peeler..."

		// Spider Gremlin has molybdenum pliers OR
		// "It bites you in the fibula with its mandibles..."

		// Vegetable Gremlin has molybdenum screwdriver OR
		// "It picks a <x> off of itself and beats you with it..."

		String text = responseText;
		if ( text.indexOf( "bombing run" ) != -1 )
		{
			BigIsland.missingGremlinTool = "molybdenum hammer";
		}
		else if ( text.indexOf( "eyeball-peeler" ) != -1 )
		{
			BigIsland.missingGremlinTool = "molybdenum crescent wrench";
		}
		else if ( text.indexOf( "fibula" ) != -1 )
		{
			BigIsland.missingGremlinTool = "molybdenum pliers";
		}
		else if ( text.indexOf( "off of itself" ) != -1 )
		{
			BigIsland.missingGremlinTool = "molybdenum screwdriver";
		}
	}

	private static final String[][] HIPPY_MESSAGES =
	{
		// 2 total
		{
			// You see one of your frat brothers take out an
			// M.C. Escher drawing and show it to a War Hippy
			// (space) Cadet. The hippy looks at it and runs away
			// screaming about how he doesn't know which way is
			// down.
			"M.C. Escher",

			// You see a hippy loading his didgeridooka, but before
			// he can fire it, he's dragged off the battlefield by
			// another hippy protesting the war.
			"protesting the war",

			// You see a "Baker Company" hippy take one bite too
			// many from a big plate of brownies, then curl up to
			// take a nap. Looks like he's out of commission for a
			// while.
			"Baker Company",

			// You see a hippy a few paces away suddenly realize
			// that he's violating his deeply held pacifist
			// beliefs, scream in horror, and run off the
			// battlefield.
			"pacifist beliefs",

			// You look over and see a fellow frat brother
			// garotting a hippy shaman with the hippy's own
			// dreadlocks. "Right on, bra!" you shout.
			"garotting",

			// You glance over and see one of your frat brothers
			// hosing down a hippy with soapy water. You laugh and
			// run over for a high-five.
			"soapy water",

			// You glance out over the battlefield and see a hippy
			// from the F.R.O.G. division get the hiccups and knock
			// himself out on his own nasty breath.
			"nasty breath",

			// You see one of the War Hippy's "Jerry's Riggers"
			// sneeze midway through making a bomb, inadvertently
			// turning himself into smoke and dust. In the wind.
			"smoke and dust",

			// You see a frat boy hose down a hippy Airborne
			// Commander with sugar water. You applaud as the
			// Commander gets attacked by her own ferrets.
			"sugar water",

			// You see one of your frat brothers paddling a hippy
			// who seems to be enjoying it. You say "uh, keep up
			// the good work... bra... yeah."
			"enjoying it",

			// As the hippy falls, you see a hippy a few yards away
			// clutch his chest and fall over, too. Apparently the
			// hippy you were fighting was just the astral
			// projection of another hippy several yards
			// away. Freaky.
			"astral projection", },
		// 4 total
		{
			// You see a War Frat Grill Sergeant hose down three
			// hippies with white-hot chicken wing sauce. You love
			// the smell of jaba�ero in the morning. It smells like
			// victory.
			"three hippies",

			// As you finish your fight, you see a nearby Wartender
			// mixing up a cocktail of vodka and pain for a trio of
			// charging hippies. "Right on, bra!" you shout.
			"vodka and pain",

			// You see one of your frat brothers douse a trio of
			// nearby hippies in cheap aftershave. They scream and
			// run off the battlefield to find some incense to
			// burn.
			"cheap aftershave",

			// You see one of your frat brothers line up three
			// hippies for simultaneous paddling. Don't bathe --
			// that's a paddlin'. Light incense -- that's a
			// paddlin'. Paddlin' a homemade canoe -- oh, you
			// better believe that's a paddlin'.
			// "three hippies",

			// You see one of the "Fortunate 500" make a quick call
			// on his cell phone. Some mercenaries drive up, shove
			// three hippies into their bitchin' meat car, and
			// drive away.
			// "three hippies",

			// As you deliver the finishing blow, you see a frat
			// boy lob a sake bomb into a trio of nearby
			// hippies. "Nice work, bra!" you shout.
			"lob a sake bomb", },
		// 8 total
		{
			// You see one of your Beer Bongadier frat brothers use
			// a complicated beer bong to spray cheap, skunky beer
			// on a whole squad hippies at once. "Way to go, bra!"
			// you shout.
			"skunky beer",

			// You glance over and see one of the Roaring Drunks
			// from the 151st Division overturning a mobile sweat
			// lodge in a berserker rage. Several sweaty, naked
			// hippies run out and off the battlefield, brushing
			// burning coals out of their dreadlocks.
			"burning coals",

			// You see one of your frat brothers punch an
			// F.R.O.G. in the solar plexus, then aim the
			// subsequent exhale at a squad of hippies standing
			// nearby. You watch all of them fall to the ground,
			// gasping for air.
			"solar plexus",

			// You see a Grillmaster flinging hot kabobs as fast as
			// he can make them. He skewers one, two, three, four,
			// five, six... seven! Seven hippies! Ha ha ha!
			"hot kabobs", },
		// 16 total
		{
			// A streaking frat boy runs past a nearby funk of
			// hippies. One look at him makes the hippies have to
			// go ponder their previous belief that the naked human
			// body is a beautiful, wholesome thing.
			"naked human body",

			// You see one of the Fortunate 500 call in an air
			// strike. His daddy's personal airship flies over and
			// dumps cheap beer all over a nearby funk of hippies.
			"personal airship",

			// You look over and see a platoon of frat boys round
			// up a funk of hippies and take them prisoner. Since
			// being a POW of the frat boys involves a lot of beer
			// drinking, you're slightly envious. Since it also
			// involves a lot of paddling, you're somewhat less so.
			"slightly envious",

			// You see a kegtank and a mobile sweat lodge facing
			// off in the distance. Since the kegtank's made of
			// steel and the sweat lodge is made of wood, you can
			// guess the outcome.
			"guess the outcome", },
		// 32 total
		{
			// You see an entire regiment of hippies throw down
			// their arms (and their weapons) in disgust and walk
			// off the battlefield. War! What is it good for?
			// Absolutely nothing!
			"Absolutely nothing",

			// You see a squadron of police cars drive up, and a
			// squad of policemen arrest a funk of hippies who were
			// sitting around inhaling smoke from some sort of
			// glass sculpture.
			"glass sculpture",

			// You see a kegtank rumble through the battlefield,
			// firing beer cans out of its top turret. It mows
			// down, like, 30 hippies in a row, but then runs out
			// of ammo. They really should have stocked one more
			// six-pack.
			"one more six-pack", },
		// 64 total
		{
			// You see the a couple of frat boys attaching big,
			// long planks of wood to either side of a
			// kegtank. Then they drive through the rank hippy
			// ranks, mass-paddling as they go. Dozens of hippies
			// flee the battlefield, tears in their filthy, filthy
			// eyes.
			"planks of wood",

			// You see one of the "Fortunate 500" hang up his PADL
			// phone, looking smug. Several SWAT vans of police in
			// full riot gear pull up, and one of them informs the
			// hippies through a megaphone that this is not a
			// "designated free speech zone." The hippies throw
			// rocks and bottles at the police, but most of them
			// end up shoved into paddy wagons in chains. Er, the
			// hippies are the ones in the chains. Not the wagons.
			"SWAT vans",

			// You see a couple of frat boys stick a fuse into a
			// huge wooden barrel, light the fuse, and roll it down
			// the hill to where the hippy forces are
			// fighting. Judging by the big bada boom that follows,
			// that barrel was either full of scotch or gunpowder,
			// and possibly both.
			"wooden barrel", },
	};

	private static final String[][] FRAT_MESSAGES =
	{
		// 2 total
		{
			// You look over and see a fellow hippy warrior using
			// his dreadlocks to garotte a frat warrior. "Way to
			// enforce karmic retribution!" you shout.
			"garotte",

			// You see a Green Gourmet give a frat boy a plate of
			// herbal brownies. The frat boy scarfs them all, then
			// wanders off staring at his hands.
			"herbal brownies",

			// Elsewhere on the battlefield, you see a fellow hippy
			// grab a frat warrior's paddle and give the frat boy a
			// taste of his own medicine. I guess that could count
			// as homeopathic healing...
			"homeopathic healing",

			// You see a Wartender pour too much lighter fluid on
			// his grill and go up in a great ball of
			// fire. Goodness gracious!
			"lighter fluid",

			// You see a Fire Spinner blow a gout of flame onto a
			// Wartender's grill, charring all the Wartender's
			// meaty goodness. The Wartender wanders off crying.
			"meaty goodness",

			// Nearby, you see one of your sister hippies
			// explaining the rules of Ultimate Frisbee to a member
			// of the frat boys' "armchair infantry." His eyes
			// glaze and he passes out.
			"Ultimate Frisbee",

			// You see a member of the frat boy's 151st division
			// pour himself a stiff drink, knock it back, and
			// finally pass out from alcohol poisoning.
			"alcohol poisoning",

			// You glance over your shoulder and see a squadron of
			// winged ferrets descend on a frat warrior, entranced
			// by the sun glinting off his keg shield.
			"entranced by the sun",

			// You see a hippy shaman casting a Marxist spell over
			// a member of the "Fortunate 500" division of the frat
			// boy army. The frat boy gets on his cell phone and
			// starts redistributing his wealth.
			"Marxist spell",

			// You see a frat boy warrior pound a beer, smash the
			// can against his forehead, and pass out. You chuckle
			// to yourself.
			"smash the can",

			// You see an F.R.O.G. crunch a bulb of garlic in his
			// teeth and breathe all over a nearby frat boy, who
			// turns green and falls over.
			"bulb of garlic", },
		// 4 total
		{
			// You hear chanting behind you, and turn to see thick,
			// ropy (almost anime-esque) vines sprout from a War
			// Hippy Shaman's dreads and entangle three attacking
			// frat boy warriors.
			"three attacking",

			// Nearby, you see an Elite Fire Spinner take down
			// three frat boys in a whirl of flame and pain.
			"three frat boys",

			// You look over and see three ridiculously drunk
			// members of the 151st Division run together for a
			// three-way congratulatory headbutt, which turns into
			// a three-way concussion.
			"three-way",

			// You see a member of the Fortunate 500 take a phone
			// call, hear him holler something about a stock market
			// crash, then watch him and two of his fortunate
			// buddies run off the battlefield in a panic.
			"stock market crash",

			// Over the next hill, you see three frat boys abruptly
			// vanish into a cloud of green smoke. Apparently the
			// Green Ops Soldiers are on the prowl.
			// "three frat boys",

			// You hear excited chittering overhead, and look up to
			// see a squadron of winged ferrets making a
			// urine-based bombing run over three frat boys. The
			// frat boys quickly run off the field to find some
			// cheap aftershave to cover up the smell.
			// "three frat boys",
		},
		// 8 total
		{
			// Nearby, a War Hippy Elder Shaman nods almost
			// imperceptibly. A Kegtank hits a gopher hole and tips
			// over. A squad of confused frat boys stumble out and
			// off the battlefield.
			"gopher hole",

			// You leap out of the way of a runaway Mobile Sweat
			// Lodge, then watch it run over one, two, three, four,
			// five, six, seven! Seven frat boys! Ha ha ha!
			"runaway",

			// A few yards away, one of the Jerry's Riggers hippies
			// detonates a bomb underneath a Wartender's grill. An
			// entire squad of frat boys run from the battlefield
			// under the onslaught of red-hot coals.
			"red-hot coals",

			// You look over and see one of Jerry's Riggers placing
			// land mines he made out of paperclips, rubber bands,
			// and psychedelic mushrooms. A charging squad of frat
			// boys trips them, and is subsequently dragged off the
			// field ranting about the giant purple squirrels.
			"purple squirrels", },
		// 16 total
		{
			// You turn to see a nearby War Hippy Elder Shaman
			// making a series of complex hand gestures. A flock of
			// pigeons swoops down out of the sky and pecks the
			// living daylights out of a whole platoon of frat
			// boys.
			"platoon of",

			// You see a platoon of charging frat boys get mowed
			// down by a hippy. Remember, kids, a short-range
			// weapon (like a paddle) usually does poorly against a
			// long-range weapon (like a didgeridooka).
			// "platoon of",

			// You look over and see a funk of hippies round up a
			// bunch of frat boys to take as prisoners of
			// war. Since being a hippy prisoner involves lounging
			// around inhaling clouds of smoke and eating brownies,
			// you're somewhat jealous. Since it also involves
			// non-stop olfactory assault, you're somewhat less so.
			"funk of hippies",

			// Nearby, a platoon of frat boys is rocking a mobile
			// sweat lodge back and forth, trying to tip it
			// over. When they succeed, they seem surprised by the
			// hot coals and naked hippies that pour forth, and the
			// frat boys run away screaming.
			// "platoon of",
		},
		// 32 total
		{
			// A mobile sweat lodge rumbles into a regiment of frat
			// boys and the hippies inside open all of its vents
			// simultaneously. Steam that smells like a dozen
			// baking (and baked) hippies pours out, enveloping the
			// platoon and sending the frat boys into fits of
			// nauseated coughing.
			"regiment",

			// You see a squadron of police cars drive up, and a
			// squad of policemen arrest an entire regiment of frat
			// boys. You hear cries of "She told me she was 18,
			// bra!" and "I told you, I didn't hit her with a
			// roofing shingle!" as they're dragged off the
			// battlefield.
			// "regiment",

			// You see a regiment of frat boys decide they're tired
			// of drinking non-alcoholic beer and tired of not
			// hitting on chicks, so they throw down their arms,
			// and then their weapons, and head back to the frat
			// house.
			// "regiment",
		},
		// 64 total
		{
			// You see an airborne commander trying out a new
			// strategy: she mixes a tiny bottle of rum she found
			// on one of the frat boy casualties with a little of
			// the frat boy's blood, then adds that to the ferret
			// bait. A fleet of ferrets swoops down, eats the bait,
			// and goes berserk with alcohol/bloodlust. The frat
			// boys scream like schoolgirls as the ferrets decimate
			// their ranks.
			"scream like schoolgirls",

			// You see a couple of hippies rigging a mobile sweat
			// lodge with a public address system. They drive it
			// through the battlefield, blaring some concept album
			// about the dark side of Ronald. Frat boys fall asleep
			// en masse, helpless before music that's horribly
			// boring if you're not under the influence of
			// mind-altering drugs.
			"en masse",

			// You see an elder hippy shaman close her eyes, clench
			// her fists, and start to chant. She glows with an
			// eerie green light as storm clouds bubble and roil
			// overhead. A funnel cloud descends from the
			// thunderheads and dances through the frat boy ranks,
			// whisking them up and away like so many miniature
			// mobile homes.
			"mobile homes", },
	};

	private static final boolean findBattlefieldMessage( final String responseText, final String[] table )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			if ( responseText.indexOf( table[ i ] ) != -1 )
			{
				return true;
			}
		}
		return false;
	}

	public static final void handleBattlefield( final String responseText )
	{
		// Nothing to do until battle is done
		if ( responseText.indexOf( "WINWINWIN" ) == -1 )
		{
			return;
		}

		// Just in case
		PrintStream sessionStream = RequestLogger.getSessionStream();

		// We only count known monsters
		MonsterDatabase.Monster monster = FightRequest.getLastMonster();
		if ( monster == null )
		{
			// The monster is not in the monster database.
			sessionStream.println( "Unknown monster found on battlefield: " + FightRequest.getLastMonsterName() );
			return;
		}

		// Decide whether we defeated a hippy or a fratboy warrior
		if ( BigIsland.fratboyBattlefield.hasMonster( monster ) )
		{
			BigIsland.fratboy = false;
		}
		else if ( BigIsland.hippyBattlefield.hasMonster( monster ) )
		{
			BigIsland.fratboy = true;
		}
		else
		{
			// Known but unexpected monster on battlefield.
			sessionStream.println( "Unexpected monster found on battlefield: " + FightRequest.getLastMonsterName() );
			return;
		}

		// Initialize settings if necessary
		BigIsland.ensureUpdatedBigIsland();

		// Figure out how many enemies were defeated
		String[][] table = BigIsland.fratboy ? BigIsland.FRAT_MESSAGES : BigIsland.HIPPY_MESSAGES;

		int quests = 0;
		int delta = 1;
		int test = 2;

		for ( int i = 0; i < table.length; ++i )
		{
			if ( BigIsland.findBattlefieldMessage( responseText, table[ i ] ) )
			{
				quests = i + 1;
				delta = test;
				break;
			}
			test *= 2;
		}

		BigIsland.lastFratboysDefeated = BigIsland.fratboysDefeated;
		BigIsland.lastHippiesDefeated = BigIsland.hippiesDefeated;

		if ( BigIsland.fratboy )
		{
			BigIsland.fratboysDefeated = KoLSettings.incrementIntegerProperty( "fratboysDefeated", delta, 1000, false );
		}
		else
		{
			BigIsland.hippiesDefeated = KoLSettings.incrementIntegerProperty( "hippiesDefeated", delta, 1000, false );
		}
	}

	// Crowther spaded how many kills it takes to display an image in:
	// http://jick-nerfed.us/forums/viewtopic.php?p=58270#58270

	private static final int[] IMAGES =
	{
		0,	// Image 0
		3,	// Image 1
		9,	// Image 2
		17,	// Image 3
		28,	// Image 4
		40,	// Image 5
		52,	// Image 6
		64,	// Image 7
		80,	// Image 8
		96,	// Image 9
		114,	// Image 10
		132,	// Image 11
		152,	// Image 12
		172,	// Image 13
		192,	// Image 14
		224,	// Image 15
		258,	// Image 16
		294,	// Image 17
		332,	// Image 18
		372,	// Image 19
		414,	// Image 20
		458,	// Image 21
		506,	// Image 22
		556,	// Image 23
		606,	// Image 24
		658,	// Image 25
		711,	// Image 26
		766,	// Image 27
		822,	// Image 28
		880,	// Image 29
		939,	// Image 30
		999,	// Image 31
		1000	// Image 32
	};

	public static final void parseBigIsland( final String location, final String responseText )
	{
		if ( !location.startsWith( "bigisland.php" ) )
		{
			return;
		}

		// Set variables from user settings
		BigIsland.ensureUpdatedBigIsland();

		// Parse the map and deduce how many soldiers remain
		BigIsland.parseBattlefield( responseText );

		// Deduce things about quests
		BigIsland.quest = BigIsland.parseQuest( location );

		switch ( BigIsland.quest )
		{
		case ARENA:
			BigIsland.parseArena( responseText );
			break;
		case JUNKYARD:
			BigIsland.parseJunkyard( responseText );
			break;
		case ORCHARD:
			BigIsland.parseOrchard( responseText );
			break;
		case FARM:
			BigIsland.parseFarm( responseText );
			break;
		case NUNS:
			BigIsland.parseNunnery( responseText );
			break;
		case LIGHTHOUSE:
			BigIsland.parseLighthouse( responseText );
			break;
		}
	}

	private static final int parseQuest( final String location )
	{
		if ( location.indexOf( "place=concert" ) != -1 )
		{
			return BigIsland.ARENA;
		}

		if ( location.indexOf( "action=junkman" ) != -1 )
		{
			return BigIsland.JUNKYARD;
		}

		if ( location.indexOf( "action=stand" ) != -1 )
		{
			return BigIsland.ORCHARD;
		}

		if ( location.indexOf( "action=farmer" ) != -1 )
		{
			return BigIsland.FARM;
		}

		if ( location.indexOf( "place=nunnery" ) != -1 )
		{
			return BigIsland.NUNS;
		}

		if ( location.indexOf( "action=pyro" ) != -1 )
		{
			return BigIsland.LIGHTHOUSE;
		}

		return BigIsland.NONE;
	}

	private static final void parseBattlefield( final String responseText )
	{
		Matcher matcher = BigIsland.MAP_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		BigIsland.fratboyImage = StaticEntity.parseInt( matcher.group( 1 ) );
		BigIsland.hippyImage = StaticEntity.parseInt( matcher.group( 2 ) );

		if ( BigIsland.fratboyImage >= 0 && BigIsland.fratboyImage <= 32 )
		{
			BigIsland.fratboyMin = BigIsland.IMAGES[ BigIsland.fratboyImage ];
			if ( BigIsland.fratboyMin == 1000 )
			{
				BigIsland.fratboyMax = 1000;
			}
			else
			{
				BigIsland.fratboyMax = BigIsland.IMAGES[ BigIsland.fratboyImage + 1 ] - 1;
			}
		}

		if ( BigIsland.hippyImage >= 0 && BigIsland.hippyImage <= 32 )
		{
			BigIsland.hippyMin = BigIsland.IMAGES[ BigIsland.hippyImage ];
			if ( BigIsland.hippyMin == 1000 )
			{
				BigIsland.hippyMax = 1000;
			}
			else
			{
				BigIsland.hippyMax = BigIsland.IMAGES[ BigIsland.hippyImage + 1 ] - 1;
			}
		}

		// Consistency check settings against map
		if ( BigIsland.fratboysDefeated < BigIsland.fratboyMin )
		{
			BigIsland.fratboysDefeated = BigIsland.fratboyMin;
			KoLSettings.setUserProperty( "fratboysDefeated", String.valueOf( BigIsland.fratboysDefeated ) );
		}
		else if ( BigIsland.fratboysDefeated > BigIsland.fratboyMax )
		{
			BigIsland.fratboysDefeated = BigIsland.fratboyMax;
			KoLSettings.setUserProperty( "fratboysDefeated", String.valueOf( BigIsland.fratboysDefeated ) );
		}

		if ( BigIsland.hippiesDefeated < BigIsland.hippyMin )
		{
			BigIsland.hippiesDefeated = BigIsland.hippyMin;
			KoLSettings.setUserProperty( "hippiesDefeated", String.valueOf( BigIsland.hippiesDefeated ) );
		}
		else if ( BigIsland.hippiesDefeated > BigIsland.hippyMax )
		{
			BigIsland.hippiesDefeated = BigIsland.hippyMax;
			KoLSettings.setUserProperty( "hippiesDefeated", String.valueOf( BigIsland.hippiesDefeated ) );
		}
	}

	private static final void parseArena( final String responseText )
	{
		// You roll up to the amphitheater and see that the Goat Cheese
		// Occurence is well into the first song of their four-hour,
		// one-song set.
		if ( responseText.indexOf( "well into the first song" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "hippy" );
			return;
		}

		// "Hey, man," he says laconically. "You did a, like, totally
		// awesome job promoting the concert, man. If you have any
		// flyers left, I'll take 'em; we can use them at the next
		// show. Speaking of which, they're hitting the stage in just a
		// couple of minutes -- you should come back in a few and check
		// 'em out. It's a totally awesome show, man."
		if ( responseText.indexOf( "I'll take 'em" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "hippy" );
			if ( KoLCharacter.hasItem( BigIsland.JAM_FLYERS ) )
			{
				StaticEntity.getClient().processResult( BigIsland.JAM_FLYERS );
			}
			return;
		}

		// You roll up to the amphitheater and see that Radioactive
		// Child has already taken the stage.
		if ( responseText.indexOf( "has already taken the stage" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "fratboy" );
			return;
		}

		// "Hey, bra," he says, "you did excellent work promoting the
		// show. If you have any flyers left, I'll take them; we can
		// use them at the next show."
		if ( responseText.indexOf( "I'll take them" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "fratboy" );
			if ( KoLCharacter.hasItem( BigIsland.JAM_FLYERS ) )
			{
				StaticEntity.getClient().processResult( BigIsland.ROCK_FLYERS );
			}
			return;
		}

		// The stage at the Mysterious Island Arena is empty.

		if ( responseText.indexOf( "The stage at the Mysterious Island Arena is empty" ) != -1 )
		{
			// Didn't complete quest or defeated the side you
			// advertised for.
			KoLSettings.resetUserProperty( "sidequestArenaCompleted", "none" );
		}
	}

	private static final void parseJunkyard( final String responseText )
	{
		String tool = BigIsland.currentJunkyardTool;
		String location = BigIsland.currentJunkyardLocation;
		boolean done = false;

		// The last time I saw my <tool> it was <location>.
		//
		//	next to that barrel with something burning in it
		//	near an abandoned refrigerator
		//	over where the old tires are
		//	out by that rusted-out car

		Matcher matcher = BigIsland.JUNKYARD_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			tool = matcher.group( 1 );
			tool = "molybdenum " + ( tool.equals( "wrench" ) ? "crescent " : "" ) + tool;
			location = matcher.group( 3 );
		}

		// As you turn to walk away, he taps you on the shoulder. "I
		// almost forgot. I made this while you were off getting my
		// tools. It was boring, but I figure the more time I spend
		// bored, the longer my life will seem. Anyway, I don't really
		// want it, so you might as well take it."

		else if ( responseText.indexOf( "I made this while you were off getting my tools" ) != -1 )
		{
			tool = "";
			done = true;
		}

		if ( tool != BigIsland.currentJunkyardTool )
		{
			BigIsland.currentJunkyardTool = tool;
			KoLSettings.setUserProperty( "currentJunkyardTool", tool );
			BigIsland.currentJunkyardLocation = location;
			KoLSettings.setUserProperty( "currentJunkyardLocation", location );
		}

		if ( !done )
		{
			return;
		}

		// Give the magnet and the tools to Yossarian
		StaticEntity.getClient().processResult( BigIsland.MAGNET );
		StaticEntity.getClient().processResult( BigIsland.HAMMER );
		StaticEntity.getClient().processResult( BigIsland.PLIERS );
		StaticEntity.getClient().processResult( BigIsland.WRENCH );
		StaticEntity.getClient().processResult( BigIsland.SCREWDRIVER );

		if ( responseText.indexOf( "spark plug earring" ) != -1 || responseText.indexOf( "woven baling wire bracelets" ) != -1 || responseText.indexOf( "gearbox necklace" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestJunkyardCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "rusty chain necklace" ) != -1 || responseText.indexOf( "sawblade shield" ) != -1 || responseText.indexOf( "wrench bracelet" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestJunkyardCompleted", "fratboy" );
		}
	}

	private static final void parseOrchard( final String responseText )
	{
		// "Is that... it is! The heart of the filthworm queen! You've
		// done it! You've freed our orchard from the tyranny of
		// nature!"
		if ( responseText.indexOf( "tyranny of nature" ) == -1 )
		{
			return;
		}

		String side = EquipmentDatabase.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		KoLSettings.resetUserProperty( "sidequestOrchardCompleted", side );
	}

	private static final void parseFarm( final String responseText )
	{
		// "Well... How about dedicating a portion of your farm to
		// growing soybeans, to help feed the hippy army?"
		if ( responseText.indexOf( "growing soybeans" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestFarmCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "growing hops" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestFarmCompleted", "fratboy" );
		}
	}

	private static final void parseNunnery( final String responseText )
	{
		// "Hello, weary Adventurer! Please, allow us to tend to your
		// wounds."
		if ( responseText.indexOf( "tend to your wounds" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "hippy" );
		}
		else if ( responseText.indexOf( "refreshing massage" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "fratboy" );
		}
		else if ( responseText.indexOf( "world-weary traveler" ) != -1 )
		{
			KoLSettings.resetUserProperty( "sidequestNunsCompleted", "none" );
		}
	}

	private static final void parseLighthouse( final String responseText )
	{
		// He gazes at you thoughtfully for a few seconds, then a smile
		// lights up his face and he says "My life... er... my bombs
		// for you. My bombs for you, bumpty-bumpty-bump!"
		if ( responseText.indexOf( "My bombs for you" ) == -1 )
		{
			return;
		}

		String side = EquipmentDatabase.isWearingOutfit( 32 ) ? "hippy" : "fratboy";
		KoLSettings.resetUserProperty( "sidequestLighthouseCompleted", side );
	}

	public static final void ensureUpdatedBigIsland()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastBattlefieldReset", String.valueOf( KoLCharacter.getAscensions() ) );

			KoLSettings.setUserProperty( "fratboysDefeated", "0" );
			KoLSettings.setUserProperty( "hippiesDefeated", "0" );
			KoLSettings.setUserProperty( "sidequestArenaCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestFarmCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestJunkyardCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestLighthouseCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestNunsCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", "none" );
			KoLSettings.setUserProperty( "currentJunkyardTool", "" );
			KoLSettings.setUserProperty( "currentJunkyardLocation", "" );
			KoLSettings.setUserProperty( "currentNunneryMeat", "0" );
		}

		// Set variables from user settings

		BigIsland.fratboysDefeated = KoLSettings.getIntegerProperty( "fratboysDefeated" );
		BigIsland.hippiesDefeated = KoLSettings.getIntegerProperty( "hippiesDefeated" );
		BigIsland.currentJunkyardTool = KoLSettings.getUserProperty( "currentJunkyardTool" );
		BigIsland.currentJunkyardLocation = KoLSettings.getUserProperty( "currentJunkyardLocation" );
		BigIsland.currentNunneryMeat = KoLSettings.getIntegerProperty( "currentNunneryMeat" );
		BigIsland.lastNunneryMeat = BigIsland.currentNunneryMeat;
	}

	public static final void parsePostwarIsland( final String location, final String responseText )
	{
		if ( !location.startsWith( "postwarisland.php" ) )
		{
			return;
		}

		// Set variables from user settings
		BigIsland.ensureUpdatedPostwarIsland();

		// Deduce things about quests
		BigIsland.quest = BigIsland.parseQuest( location );

		switch ( BigIsland.quest )
		{
		case ARENA:
			BigIsland.parseArena( responseText );
			break;
		case NUNS:
			BigIsland.parseNunnery( responseText );
			break;
		}
	}

	public static final void ensureUpdatedPostwarIsland()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastBattlefieldReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastBattlefieldReset", String.valueOf( KoLCharacter.getAscensions() ) );

			KoLSettings.setUserProperty( "sidequestArenaCompleted", "none" );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", KoLSettings.getUserProperty( "currentHippyStore" ) );
			KoLSettings.setUserProperty( "sidequestNunsCompleted", "none" );
		}
	}

	public static final void decoratePostwarIsland( final String url, final StringBuffer buffer )
	{
		// Now replace sidequest location images for completed quests
		BigIsland.sidequestImage( buffer, "sidequestArenaCompleted", BigIsland.ARENA );
		BigIsland.sidequestImage( buffer, "sidequestNunsCompleted", BigIsland.NUNS );
	}
}
