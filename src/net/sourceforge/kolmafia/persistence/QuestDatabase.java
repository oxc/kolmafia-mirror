/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * Provides utility functions for dealing with quests.
 * 
 */
public class QuestDatabase
	extends KoLDatabase
{
	public static final String UNSTARTED = "unstarted";
	public static final String STARTED = "started";
	public static final String FINISHED = "finished";

	public static final String LARVA = "questL02Larva";
	public static final String RAT = "questL03Rat";
	public static final String BAT = "questL04Bat";
	public static final String GOBLIN = "questL05Goblin";
	public static final String FRIAR = "questL06Friar";
	public static final String CYRPT = "questL07Cyrptic";
	public static final String TRAPPER = "questL08Trapper";
	public static final String LOL = "questL09Lol";
	public static final String GARBAGE = "questL10Garbage";
	public static final String MACGUFFIN = "questL11MacGuffin";
	public static final String ISLAND_WAR = "questL12War";
	public static final String FINAL = "questL13Final";
	public static final String CITADEL = "questG02Whitecastle";
	public static final String GALAKTIK = "questM04Galaktic";

	public static final Pattern HTML_WHITESPACE = Pattern.compile( "<[^<]+?>|[\\s\\n]" );

	private static final String[][] questLogData =
	{
		{
			"questL02Larva",
			"Looking for a Larva in All the Wrong Places",
			"The Council of Loathing wants you to bring them a mosquito larva, for some reason. They told you to look for one in the Spooky Forest, in the Distant Woods.<p>How can a woods contain a forest? Suspension of disbelief, that's how.",
			"You delivered a mosquito larva to the Council of Loathing. Nice work!"
		},
		{
			"questL03Rat",
			"Ooh, I Think I Smell a Rat.",
			"The owner of the Typical Tavern is having a bit of a rat infestation problem.<p>The Tavern is in the Distant Woods.",
			"You've solved the rat problem at the Typical Tavern. Way to go!",
		},
		{
			"questL04Bat",
			"Ooh, I Think I Smell a Bat.",
			"The Council wants you to make your way to the chamber of the Boss Bat, and slay him.<p>His chamber can be found deep within the Bat Hole, in the Nearby Plains.",
			"You're getting closer to the Boss Bat's chamber. Keep searching the Bat Hole until you find it.",
			"You're getting very close to the Boss Bat's chamber. Keep exploring.",
			"You've discovered the Boss Bat's chamber -- now go in there and clean his clock. Then beat him up. Unless he doesn't have a clock, in which case just go straight to the beating.",
			"Now that you've defeated the Boss Bat, you should go back to the Council for your reward.",
			"You have slain the Boss Bat. Huzzah!"
		},
		{
			"questL05Goblin",
			"The Goblin Who Wouldn't Be King",
			"The Council of Loathing wants you to infiltrate Cobb's Knob and take out the Goblin King.<p>Kill him, that is. Not, like, take him out on a date.",
			"You have slain the Goblin King. Good job!"
		},
		{
			"questL06Friar",
			"Trial By Friar",
			"The Council of Loathing wants you to assist the Deep Fat Friars. They can be found in their copse in the Distant Woods.",
			"You have cleansed the taint of the Deep Fat Friars. Congratulations!"
		},
		{
			"questL07Cyrptic",
			"Cyrptic Emanations",
			"The Council of Loathing wants you to find the source of the extreme Spookiness emanating from the Cyrpt. You can find it in the Nearby Plains.",
			"You've defeated the Bonerdagon -- now take his skull back to the Council, and claim your reward!",
			"You've undefiled the Cyrpt, and defeated the Bonerdagon. Hip, Hip, Hooray!"
		},
		{
			"questL08Trapper",
			"Am I my Trapper's Keeper?",
			"The Council of Loathing wants you to visit the L337 Tr4pz0r, who lives at the base of Mt. McLargeHuge, the tallest of the Big Mountains.",
			"The Tr4pz0r wants you to infiltrate Itznotyerzitz Mine, and bring him back 3 chunks of ore.",
			"The Tr4pz0r wants you to bring him 6 chunks of goat cheese from the Goatlet.",
			"The Tr4pz0r wants you to find some way to protect yourself from the cold. If you can't find a way to do it magically, you can probably find some warm clothes on the eXtreme Slope of Mt. McLargeHuge.",
			"You have learned how to hunt Yetis from the L337 Tr4pz0r. Shazam!"
		},
		{
			"questL09Lol",
			"A Quest, LOL",
			"The Council of Loathing wants you to assist the Baron Rof L'm Fao. You must find your way past the Orc Chasm in the Big Mountains to reach the Baron's valley.",
			"Now that you've found your way to the Valley beyond the Orc Chasm, you must make your way to the gates of the Baron's fortress.",
			"You have helped the Baron Rof L'm Fao with his monster problem. w00t!"
		},
		{
			"questL10Garbage",
			"The Rain on the Plains is Mainly Garbage",
			"The Council of Loathing wants you to investigate the source of the giant garbage raining down on the Nearby Plains.",
			"You have stopped the rain of giant garbage in the Nearby Plains. Slick!"
		},
		{
			"questL11MacGuffin",
			"<Player Name> and the Quest for the Holy MacGuffin",
			"The Council has instructed you to collect your father's archaeology notes from Distant Lands, and use them to hunt down the Holy MacGuffin. Your first step is to find the Black Market, to get some forged ID.",
			"You've found the Black Market... now to hit the Travel Agency and get yourself on a slow boat to China. I mean, Distant Lands.",
			"You've picked up your father's diary, and things just got a whole lot more complicated. Oh dear.",
			"You've handed the Holy MacGuffin over to the Council, and enjoyed a ticker-tape parade in your honor. That quest was so ridiculous, it wasn't even funny, and now it's over! Hooray!"
		},
		{
			"questL11Worship",
			"Gotta Worship Them All",
			"You father seemed to think the hidden temple in the Distant Woods might be guarding part of the Staff of Ed. I hope you've got your lucky fedora with you.",
			"You've cunningly evaded one of the Hidden Temple's traps. But what else lies in store? cue ominous music",
			"Having proved that you ain't no hollaback girl, there's just one more trap to go. Pity dad never got around to translating that last passage...",
			"Awesome, you've evaded all of the temple's traps! Of course, it turned out that getting the piece of the Staff of Ed isn't going to be nearly that easy, but you were probably expecting that anyway. If you weren't, well, sorry.",
			"You've defeated the ancient ghost of an ancient mummy of an ancient high priest and claimed his ancient amulet! Go you!"
		},
		{
			"questL11Manor",
			"In a Manor of Spooking",
			"Your father's notes indicate that the gem from the Staff of Ed is probably hidden in a Seaside Town mansion. At a guess, you figure Spookyraven Manor is probably your best bet.",
			"You've unlocked the wine cellar in Spookyraven Manor. What are the chances there's a secret door hidden somewhere? Yeah, probably about one in one.",
			"You've found Lord Spookyraven's secret black magic laboratory. When you're done with him, he'll be doing black and blue magic.",
			"You've defeated Lord Spookyraven and claimed the Eye of Ed! Huzzah!"
		},
		{
			"questL11Palindome",
			"Never Odd Or Even",
			"If you're going to get the Staff of Fats, it looks like the first step is to get into the Palindome. Maybe it has something to do with that amulet your father mentioned in his diary? That password looks important, too.",
			"Congratulations, you've discovered the fabulous Palindome, rumored to be the final resting place of the legendary Staff of Fats! Now all you have to do is find it...",
			"Well, you found the Staff of Fats, but then you lost it again. Good going. Looks like you're going to have to track down this Mr. Alarm guy for help...",
			"Mr. Alan Alarm has agreed to help you nullify Dr. Awkward's ineptitude field (patent pending), but wants some wet stew in return. Those ingredients again: lion oil, a bird rib, and some stunt nuts. Sounds delicious!",
			"Oh yeah, you've got the Mega Gem, and are ready to deliver some pain to Dr. Awkward. They call you the bus driver, because you're gonna beat the hell out of that guy.",
			"Congratulations, you've recovered the long-lost Staff of Fats!<p>Nice Work!"
		},
		{
			"questL11Pyramid",
			"A Pyramid Scheme",
			"Your father's diary indicates that the key to finding the Holy MacGuffin is hidden somewhere in the desert. I hope you've got your walking shoes on.",
			"You've managed to stumble upon a hidden oasis out in the desert. That should help make your desert explorations a little less... dry.",
			"The fremegn leader Gnasir has tasked you with finding a stone rose, at his abandoned encampment near the oasis. Apparently it's an ancient symbol of his tribe or something, I dunno, whatever. He's not gonna help you unless you get it for him, though.",
			"Gnasir has asked you to prove your honor and dedication to the tribe by painting his front door black. A menial task to be sure, but at least it's not dangerous.<p>Well, unless you're really allergic to paint fumes or something.",
			"Gnasir seemed satisfied with the tasks you performed for his tribe, and has asked you to come back later.<p>...<p>Okay, that's probably long enough.",
			"For your worm-riding training, you need to find a 'thumper', something that produces a rhythmic vibration to summon sandworms.<p>It's unlikely that we're talking about bunny rabbits here.",
			"You need to find fifteen missing pages from Gnasir's worm-riding manual. Have fun!",
			"One worm-riding manual page down, fourteen to go.",
			"Two worm-riding manual pages down, thirteen to go. Sigh.",
			"You've found all of Gnasir's missing manual pages. Time to take them back to the sietch.",
			"You've earned your hooks and are ready to ride the worm. Literally, not in the South-of-the-Border sense.",
			"One excitingly-described worm-ride later, you've found the little pyramid with the map of Seaside Town inside. Looks like you're going to need the Staff of Ed to get the location of the Holy MacGuffin's hiding place.",
			"You've found the hidden buried pyramid that guards the Holy MacGuffin. You're so close you can almost taste it! (In a figurative sense, I mean -- I don't recommend you go around licking things you find in ancient tombs.)",
			"The mighty Ed the Undying has fallen! You recovered the Holy MacGuffin! Jolly good show, mate! "
		},
		{
			"questL12War",
			"Make War, Not... Oh, Wait",
			"The Council has gotten word of tensions building between the hippies and the frat boys on the Mysterious Island of Mystery.<p>They suspect that the two factions are about to go to war, and they want to make sure it's a big war. They want you to head down there and see if you can't stir up some trouble.",
			"You've managed to get the war between the hippies and frat boys started, and now the Council wants you to finish it.<p>You can aid the war effort by fighting on the Battlefield, or you can help out some of the other residents of the island in the hopes that they'll aid the side you're fighting for.",
			"You led the filthy hippies to victory in the Great War. For Gaia!",
			"You led the Orcish frat boys to victory in the Great War. For The Horde!",
			"You started a chain of events that led the pirates to annihilate both the hippies and the frat boys in the Great War. Toasty!"
		},
		{
			"questL13Final",
			"The Final Ultimate Epic Final Conflict",
			"The Council of Loathing has instructed you to make your way to the top of the Naughty Sorceress' Tower and defeat her.<p>Inside the entrance to the Lair, you've encountered three strange gates. An inscription provides a clue on how you might pass through them...",
			"Having made it through the three gates, you've encountered a giant mirror that blocks your way deeper into the Lair.",
			"You've come to an odd junction in the cave leading to the Sorceress' Lair. It seems that in order to proceed, you'll need to solve a really convoluted and contrived puzzle involving a cloud of gas, a locked door, and three statues of mariachis.",
			"The Council of Loathing has instructed you to make your way to the top of the Naughty Sorceress' Tower and defeat her. Currently, you're stuck in a hedge maze, Don't beat around the bush, get through it!",
			"You've passed through the gates, solved a fiendish puzzle, beat the hedge maze like a psychotic landscaper, and now you're facing a fiendish monster on the {level number} level of the sorceress's tower.",
			"You've solved many puzzles, and now are confronted with the most frustrating puzzle yet. Can you figure out the code to get through the heavy door?",
			"You're almost to the final epic showdown battle countdown of fate and destiny and whatnot! Get in there and kick some tail!",
			"You thought you were finally going to fight the Sorceress, but you're still stuck battling her minions. This one seems to be a shadowy, evil version of you, except it doesn't have a goatee.",
			"You find yourself fighting one of the Sorceress's freakishly overgrown familiars. What is she feeding them, anyway?",
			"This is it, sparky -- the big showdown with the Naughty Sorceress. I just wanted to say, \"good luck - we're all counting on you.\"",
			"You have defeated the Naughty Sorceress! Whoohoo! You left the king in his prism, though.",
			"You have defeated the Naughty Sorceress and freed the King! What are you hanging around here for?"
		},

		{
			"questG01Meatcar",
			"My Other Car Is Made of Meat",
			"Since your Guild's meat car has been lost due to somewhat sketchy circumstances, you need to build a new one. You might be able to find some parts at Degrassi Knoll.",
			"You've built a new meat car from parts. Impressive!"
		},
		{
			"questG02Whitecastle",
			"<Player Name> and <Familiar Name> Go To White Citadel",
			"You've been charged by your Guild (sort of) with the task of bringing back a delicious meal from the legendary White Citadel. You've been told it's somewhere near Whitey's Grove, in the Distant Woods.",
			"You've discovered the road from Whitey's Grove to the legendary White Citadel. You should explore it and see if you can find your way.",
			"You're progressing down the road towards the White Citadel, but you'll need to find something that can help you get past that stupid cheetah if you're going to make it any further. Keep looking around.",
			"You've made your way further down the Road to the White Citadel, but you still haven't found it. Keep looking!",
			"You've found the White Citadel, but it's at the bottom of a huge cliff. You should keep messing around on the Road until you find a way to get down the cliff.",
			"You have discovered the legendary White Citadel. You should probably go in there and get the carryout order you were trying to get in the first place. Funny how things spiral out of control, isn't it?",
			"You've got the Satisfaction Satchel. Take it to your contact in your Guild for a reward.",
			"You've delivered a satchel of incredibly greasy food to someone you barely know. Plus, you can now shop at White Citadel whenever you want. Awesome!"
		},
		{
			"questG03Ego",
			"The Wizard of Ego",
			"You've been tasked with digging up the grave of an ancient and powerful wizard and bringing back a key that was buried with him. What could possibly go wrong?",
			"You've turned in the old book, and they said they didn't want it and for you to go away. A bit anticlimactic, but I suppose it still counts as a success. Congratulations!"
		},
		{
			"questG04Nemesis",
			"Me and My Nemesis",
			"One of your guild leaders has tasked you to recover a mysterious and unnamed artifact stolen by your Nemesis. Your first step is to smith an Epic Weapon.<p>Two parts of the Epic Weapon can be had from the two oldest and wisest men in the kingdom, one of whom runs the casino. You weren't told where the third part is.",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, the Infernal Seal Gorgolok has fallen beneath your mighty assault. Never again will the people of the Frigid Northlands be terrorized by this foul beast! Your mother must be very proud of you. Well done!",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, Stella the Turtle Poacher has fallen beneath your mighty assault. Never again will the helpless Testudines of the Kingdom be terrorized by her horrible poachery! Your mother must be very proud of you. Well done!",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, the evil Spaghetti Elemental has fallen beneath your mighty assault. Never again will the people of the Kingdom of Loathing be terrorized by whatever it was that the Pasta Cult was actually doing (probably human sacrifices and stuff)! Your mother must be very proud of you. Well done!",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, Lumpy the Sinister Sauceblob has fallen beneath your mighty assault. Now the people of the Kingdom of Loathing are safe from whatever horrible (and probably really gross) scheme it was that Lumpy had in store! Your mother must be very proud of you. Well done!",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, the Spirit of New Wave has fallen beneath your mighty assault. Now the disco-loving people of the Kingdom of Loathing are free to groove the night away, safe from his insidious machinations! Your mother must be very proud of you. Well done!",
			"Despite being aided by <demon name>, the Demonic Lord of Revenge, the dread mariachi Somerset Lopez has fallen beneath your mighty assault. Now the eons-long war between the Accordion Thieves and the mariachis is finally at an end, and the streets of the Kingdom of Loathing are safe for cat-burglars and sneak-thieves like yourself and your cronies! Your mother must be very proud of you. Well done!"
		},
		{
			"questG05Dark",
			"A Dark and Dank and Sinister Quest",
			"Finally it's time to meet this Nemesis you've been hearing so much about! The guy at your guild has marked your map with the location of a cave in the Big Mountains, where your Nemesis is supposedly hiding.",
			"Your Nemesis has scuttled away in defeat, leaving you with a sweet Epic Hat and a feeling of smug superiority. Well done you!"
		},
		{
			"questG06Delivery",
			"<Player Name>'s Delivery Service",
			"A guy in your guild has offered you some meat if you'll grab his package for him.<p>Oh stop laughing, you know perfectly well what I mean! Honestly...<p>Anyway, you should be able to find it in the 7-Foot Dwarves' factory complex, which can supposedly be reached through their mine.",
			"You've successfully delivered a package, and been rewarded with an amount of meat that was more-or-less proportional to the difficulty of the task. Hooray! Of course, there's obviously a bit more going on in that factory, but whether or not you want to mess around with all that is up to you."
		},

		{
			"questM01Untinker",
			"Driven Crazy",
			"The Untinker in Seaside Town wants you to find his screwdriver. He thinks he left it at Degrassi Knoll, on the Nearby Plains.",
			"You fetched the Untinker's screwdriver. Nice going!"
		},
		{
			"questM02Artist",
			"Suffering For His Art",
			"The Pretentious Artist, who lives on the Wrong Side of the Tracks in Seaside Town, has lost his palette, his pail of paint, and his paintbrush.<p>He told you that he thinks the palette is in the Haunted Pantry, the pail of paint is somewhere near the Sleazy Back Alley, and the paintbrush was taken by a Knob Goblin.",
			"You helped retrieve the Pretentious Artist's stuff. Excellent!"
		},
		{
			"questM03Bugbear",
			"A Bugbear of a Problem",
			"Mayor Zapruder of Degrassi Knoll wants you to investigate the Gnolls' bugbear pens, located in the Distant Woods.",
			"Mayor Zapruder wants you to find your way to the spooky gravy fairies' barrow, but first he needs you to bring him a flaming/frozen/stinky mushroom from the mushroom fields deep within Degrassi Knoll.",
			"Now that you've got a powerful Gravy Fairy, Mayor Zapruder wants you to investigate the Spooky Gravy Barrow in the Distant Woods.",
			"Now that you've slain Queen Felonia, you should go back to Mayor Zapruder for your reward.",
			"You've helped Mayor Zapruder of Degrassi Knoll with his spooky gravy fairy problem. Nice going!"
		},
		{
			"questM04Galaktic",
			"What's Up, Doc?",
			"Doc Galaktik wants you to collect some herbs for him. This is what he told you:<p>\"First, I'll need three swindleblossoms. I'm not sure where they grow, but I know that the harem girls of Cobb's Knob like to wear them in their hair.<p>After that, I'll need three sprigs of fraudwort. It's used by ninja assassins from Hey Deze to make poisons.<p>Finally, I'll need three bundles of shysterweed -- it only grows near the graves of liars. Or so I've been told. The guy might've been lying, I guess.\"",
			"You found some herbs for Doc Galaktik, and he rewarded you with a permanent discount on Curative Nostrums and Fizzy Invigorating Tonics. Nifty!"
		},
		{
			"questM05Toot",
			"Toot!",
			"The Council of Loathing has suggested that you visit the Toot Oriole, on Mt. Noob, in the Big Mountains. You should probably listen to them.",
			"You have completed your training with the Toot Oriole. Groovy!"
		},
		{
			"questM06Gourd",
			"Out of Your Gourd",
			"The Captain of the Gourd, on the Right Side of the Tracks in Seaside Town, needs you to help him defend the gourd. He's asking you to bring back 5 [Knob Goblin firecrackers|razor-sharp can lids|spider webs] from the [Outskirts of Cobb's Knob|Haunted Pantry|Sleazy Back Alley].",
			"You've helped out the Captain of the Gourd. Urp!"
		},
		{
			"questM07Hammer",
			"Hammer Time",
			"You were approached in the Sleazy Back Alley by a guy named Harold, who wants you to repair his favorite hammer for him.",
			"You handily helped Harold with his hammer. Hallelujah!"
		},
		{
			"questM08Baker",
			"Baker, Baker",
			"A guy near the Haunted Pantry gave you a cake, and asked if you could figure out some way to light the candles on it. He says that normal fire won't work, because they're \"hilarious\" novelty candles.",
			"You helped the anonymous baker prepare his cake for Claude. What a Samaritan!"
		},
		{
			"questM09Rocks",
			"When Rocks Attack",
			"A wounded guard near Cobb's Knob wants you to go to Doc Galaktik's Medicine Show, in the Market Square of Seaside Town, and get him a container of Doc Galaktik's Pungent Unguent.",
			"You helped out a wounded Knob Goblin guard by bringing him some unguent. You're a regular Florence Nightingale Jr.!"
		},
		{
			"questM10Azazel",
			"Angry <Player Name>, this is Azazel in Hell.",
			"Azazel, one of the ArchDukes of Hey Deze, has \"lost\" several of the talismans of his evil power. If you find them, he'll probably reward you. Probably. He's kind of a jerk.<p>He lives in the City of Pandemonium, on the other side of the Deep Fat Friars' Gate in the Distant Woods.",
			"You've found Azazel's unicorn, his lollipop, and his tutu. This peek into the nature of evil is disturbing, but the reward was gratifying. Go you!"
		},
		{
			"questM11Postal",
			"Going Postal",
			"Gnorbert, elder of the gnomish gnomads, wants you to collect some comic books from a sk8 gnome named Gnathan, who usually hangs out at The eXtreme Slope.<p>Sounds pretty simple; this shouldn't take long.",
			"You did it! You successfully returned the comic books and were rewarded with some sort of gnomitronic gizmo. All I can say is, it'd better be a damn good gizmo."
		},
		{
			"questM12Pirate",
			"I Rate, You Rate",
			"A salty old pirate named Cap'm Caronch has offered to let you join his crew if you find some treasure for him. He gave you a map, which causes you to wonder why he didn't just go dig it up himself, but oh well...",
			"Now that you've found Cap'm Caronch's booty (and shaken it a few times), you should probably take it back to him.",
			"Cap'm Caronch has given you a set of blueprints to the Orcish Frat House, and asked you to steal his dentures back from the Frat Orcs.<p>If you are caught or killed, the secretary will disavow any knowledge of your actions.",
			"You have successfully swiped the Cap'm's teeth from the Frat Orcs -- time to take the nasty things back to him. And then wash your hands.",
			"You've completed two of Cap'm Caronch's tasks, but (surprise surprise) he's got a third one for you before you can join his crew. Strange how these things always come in threes...<p>Anyway, the Cap'm wants you to defeat Old Don Rickets, the current champion of Insult Beer Pong, at his own game.",
			"You have successfully joined Cap'm Caronch's crew! Unfortunately, you've been given crappy scutwork to do before you're a full-fledged pirate.<p>Your tasks: scrub the mizzenmast, polish the cannonballs, and shampoo the rigging.",
			"Congratulations, you're a mighty pirate! Time to man the poop deck and sail the eleven seas!<p>Oh, and also you've managed to scam your way belowdecks, which is cool."
		},
		{
			"questM13Escape",
			"The Pretty Good Escape",
			"Subject 37, in the Cobb's Knob Menagerie wants you to find out what the scientists in the Cobb's Knob Laboratory are planning to do to him.",
			"You've done a good turn, and helped Subject 37 make his escape from the Cobb's Knob Menagerie."
		},
		{
			"questM14Bounty", "A Bounty Hunter Is You!",
			"The bounty hunter hunter wants you to collect [amount] [item] from [monster] on [location].",
			""
		},

		{
			"questS01OldGuy",
			"An Old Guy and The Ocean",
			"The Old Man, by The Sea, wants you to retrieve his boot. He says he dropped it off the side of his boat while he was fishing.",
			"You've bought the Old Man's boot back from Big Brother. You should take it back to him.",
			"You helped the Old Man retrieve his boot from The Sea. Marvelous!"
		},
		{
			"questS02Monkees",
			"Hey, Hey, They're Sea Monkees",
			"You rescued a strange, monkey-like creature from a Neptune Flytrap. He marked the location of his sea-floor home on your map -- maybe you should go talk to him.",
			""
		},

		{
			"questF01Primordial", "Primordial Fear",
			"You remember floating aimlessly in the Primordial Soup. You wanted to do it some more.",
			"You remember creating an unstoppable supervirus. Congratulations!"
		},
		{
			"questF02Hyboria",
			"Hyboria? I don't even...",
			"Your ancient ancestor, the mighty warrior Krakrox (and also, at the moment, yourself -- and I know that's confusing, but that's time-travel for you) is exploring the jungles of Loathing and an ancient city that has lain abandoned since even more ancient times. Sounds like good fun, eh?",
			"You discovered and dug up the Pork Elves' reward to Krakrox in an abandoned lot at the Wrong Side of the Tracks. Congratulations! Ki'Rhuss's ruby eye was there as well, but your adventurer's intuition told you it was better left where it was."
		},
		{
			"questF03Future",
			"Future",
			"You've journeyed through time to a future megalopolis, and found out you aren't the savior of mankind. Oh, well. Who needs that kind of responsibility?",
			"You've used the power of all six elements to save the world, and came *this* close to makin' bacon with the Supreme Being. Congratulations! Check your inventory for a secret from the future!"
		},
		{
			"questF04Elves", "Repair the Elves' Shield Generator",
			"Explore Ronaldus and Grimacia to find out what happened to the elves.",
			"Use the Maps you've found to search for missing scientists.", "Escort Axel around the moons.",
			"Congratulations! You've saved a few of the elves!"
		},

		{
			"questI01Scapegoat",
			"Scapegoat",
			"You must find and put a stop to whoever is controlling the army of lawn gnomes in The Landscaper's Lair.<p>You've got a hunch that it's probably somebody called The Landscaper, and that he probably lives in the hut marked \"The Landscaper's Hut\" on your map.<p>It's just a hunch, though.",
			"You've defeated The Landscaper and claimed his obnoxious leafblower as your own."
		},
		{
			"questI02Beat",
			"The Quest for the Legendary Beat",
			"You must defeat Professor Jacking in order to gain access to his laboratory and search for the Legendary Beat.",
			"You've defeated Professor Jacking and gained unfettered access to his laboratory. Now... where's that Beat?",
			"You've managed to find the Legendary Beat, which Professor Jacking had cleverly miniaturized and embedded in your own skin. Fresh!"
		},
	};

	private static final String[][] councilData =
	{
		{
			QuestDatabase.LARVA,
			QuestDatabase.STARTED,
			"We require your aid, Adventurer. We need a mosquito larva. Don't ask why, because we won't tell you. In any case, the best place to find a mosquito larva is in the Spooky Forest, which is found in the Distant Woods. We'll mark it on your map for you.",
			"We still need a mosquito larva, Adventurer. Please find us one, in the Spooky Forest."
		},
		{
			QuestDatabase.LARVA,
			QuestDatabase.FINISHED,
			"Thanks for the larva, Adventurer. Er, actually, y'know what? You look pretty lonely. Maybe you should hatch this larva and keep the mosquito as a pet.<p>To do so, you'll need a Familiar-Gro� Terrarium at your campsite. You can use this Meat to buy one, if you don't already have one.",
			"Thanks for the larva, Adventurer. We'll put this to good use. Have some Meat for your troubles."
		},
		{
			QuestDatabase.RAT,
			QuestDatabase.STARTED,
			"We've received word that the owner of The Typical Tavern, in The Distant Woods, is having a bit of a rat problem. I'm sure he'd reward you if you took care of it for him.",
			"The owner of the Typical Tavern is still bugging us about his rat problems. Perhaps you could help him?"
		},
		{
			QuestDatabase.BAT,
			QuestDatabase.STARTED,
			"The Council requires another task of you, Adventurer. You must slay the Boss Bat. He can be found in the deepest part of the Bat Hole, in the Nearby Plains. Slay him, and return to us with proof of your conquest.",
			"You have not yet slain the Boss Bat. He can be found in the Bat Hole, in the Nearby Plains."
		},
		{
			QuestDatabase.BAT, QuestDatabase.FINISHED,
			"Well done! You have slain the Boss Bat. As a reward, we present you with this belt made from his skin."
		},
		{
			QuestDatabase.GOBLIN,
			QuestDatabase.STARTED,
			"We've gotten word, Adventurer, that the Knob Goblins, who normally keep to themselves over at Cobb's Knob, are planning a major military action against Seaside Town.<p>We need for you to go deep into the Knob, and nip this problem in the bud, so to speak, by neutralizing the Goblin King.<p>Our spies have determined that there is a secret entrance that will allow you to access the inside of the Knob. They recovered this map, but nobody knows how to read it.<p>You'll need to figure out how to decrypt the symbols on it if you're going to find that entrance. And be careful with it, Adventurer. Many Bothans died to... oh, wait, never mind. That was something else.",
			"You need to find your way into Cobb's Knob, Adventurer. Try looking around the Outskirts for a clue that might help you figure out that map we gave you.",
			"We still need you to neutralize the Goblin King, Adventurer."
		},
		{
			QuestDatabase.GOBLIN, QuestDatabase.FINISHED,
			"Thank you for slaying the Goblin King, Adventurer."
		},
		{
			QuestDatabase.FRIAR,
			QuestDatabase.STARTED,
			"Please, Adventurer, help us! We were performing a ritual at our Infernal Gate, and Brother Starfish dropped the butterknife. All of the infernal creatures escaped our grasp, and have tainted our grove. Please clean the taint! Collect the three items necessary to perform the ritual which will banish these fiends back to their own realm.<p>The first item can probably be found in The Dark Neck of the Woods.<p>The second item was last seen in The Dark Heart of the Woods.<p>The third item was stolen near The Dark Elbow of the Woods.",
			"You don't appear to have all of the elements necessary to perform the ritual.",
			"You've got all three of the ritual items, Adventurer! Hurry to the center of the circle, and perform the ritual!"
		},
		{
			QuestDatabase.CYRPT,
			QuestDatabase.STARTED,
			"Recently, an aura of extreme Spookiness has begun to emanate from within the Cyrpt, near the Misspelled Cemetary. We fear that some horrible monster has taken up residence there, and begun to rile up the local undead.<p>Would you be so good as to investigate? This device should help:",
			"The Spookiness still emanates from the Cyrpt, Adventurer. See if you can find and destroy the source, and bring us back proof of your conquest."
		},
		{
			QuestDatabase.CYRPT,
			QuestDatabase.FINISHED,
			"Aha! So the Spookiness was coming from this abominable creature, was it? Well, you have our thanks, Adventurer, for your courageous act of undefilement.<p>Please, allow us to fashion that skull into something a little flashier."
		},
		{
			QuestDatabase.TRAPPER,
			QuestDatabase.STARTED,
			"Adventurer! We've received an urgent letter from the L337 Tr4pz0r, requesting our assistance. We're, like, really busy right now, so we were hoping you could go out to his place and see what he wants.<p>He lives at the base of Mt. McLargeHuge, the tallest of the Big Mountains. We'll mark it on your map for you.",
			"You still have unfinished business with the L337 Tr4pz0r, Adventurer."
		},
		{
			QuestDatabase.LOL,
			QuestDatabase.STARTED,
			"Adventurer! We've just received an urgent message from the Baron Rof L'm Fao. His Valley, beyond the Orc Chasm in the Big Mountains, has been invaded! You must help him!",
			"The Baron Rof L'm Fao still needs your help, Adventurer! You can find his valley beyond the Orc Chasm, in the Big Mountains.<p>If you're having trouble getting past the Chasm, the pirates on the Mysterious Island of Mystery might have something that will help you."
		},
		{
			QuestDatabase.LOL,
			"step1",
			"Now that you've found your way into the Valley beyond the Orc Chasm, you'll have to find the gates of the Baron's keep. They're cleverly hidden, though. You'll need your wits and your arithmetic skills about you if you're going to find them."
		},
		{
			QuestDatabase.GARBAGE,
			QuestDatabase.STARTED,
			"Something is amiss, Adventurer. The Nearby Plains are filling up with giant piles of garbage, and despite our best efforts, it keeps falling from the sky faster than we can clean it up. We need you to figure out where it's coming from, and put a stop to it.",
			"Please try to figure out where this garbage is coming from, Adventurer! Perhaps you can find a clue by poking around the Nearby Plains."
		},
		{
			QuestDatabase.GARBAGE,
			QuestDatabase.FINISHED,
			"We're not sure what you did, Adventurer, but the garbage finally stopped falling. Thanks a lot!<p>Oh, by the way -- we found this in the garbage when we were cleaning up, and thought you might have some use for it."
		},
		{
			QuestDatabase.MACGUFFIN,
			QuestDatabase.STARTED,
			"You can travel there from the Travel Agency at The Shore, but there's a slight hitch -- the area you're going to requires a passport for entry, and our passport offices are temporarily closed due to a tiny photograph shortage. You'll need to acquire some forged identification documents from the Black Market instead, but we're not entirely sure where the Black Market actually is. It's probably near the Black Forest, though, and we'll mark that on your map for you.",
			"Any luck getting your father's diary and recovering the Holy MacGuffin? It's a pretty important whatchamacallit, so we'd apprecate it if you'd get on that right away."
		},
		{
			QuestDatabase.MACGUFFIN,
			QuestDatabase.FINISHED,
			"And one quick (though enjoyable) tickertape parade later, you're standing back in front of the Council Hall, picking bits of confetti out of your hair and wondering what you should do next."
		},
		{
			QuestDatabase.FINAL,
			QuestDatabase.STARTED,
			"Now that you have proven yourself, the Council has deemed that it is time for you to embark upon your final quest. Seek out and destroy the Naughty Sorceress, who has plagued these lands for so long, and rescue King Ralph XI, whom she has imprismed.<p>Go forth to her Lair, east of the Nearby Plains! Beat her down!",
			"Be strong, Adventurer! You must defeat the Naughty Sorceress! You'll find her Lair just east of the Nearby Plains."
		},
		{
			QuestDatabase.FINAL,
			QuestDatabase.FINISHED,
			"Congratulations, Adventurer! It's the end of your quest as we know it. Don't worry, we feel fine. You've freed the king and made us obsolete. Ah, well. Hail to the king, baby."
		}
	};
	static
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			// replace <player name> with player name.
			questLogData[ i ][ 1 ] = questLogData[ i ][ 1 ].replaceAll( "<Player\\sName>",
				KoLCharacter.getUserName() );
		}
	}

	public static String titleToPref( final String title )
	{
		if ( title.indexOf( "White Citadel" ) != -1 )
		{
			// Hard code this quest, for now. The familiar name in the middle of the string is annoying to
			// deal with.
			return "questG02Whitecastle";
		}
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 1 ].toLowerCase().indexOf( title.toLowerCase() ) != -1 )
			{
				return questLogData[ i ][ 0 ];
			}
		}

		// couldn't find a match
		return "";
	}

	public static String prefToTitle( final String pref )
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 0 ].toLowerCase().indexOf( pref.toLowerCase() ) != -1 )
			{
				return questLogData[ i ][ 1 ];
			}
		}

		// couldn't find a match
		return "";
	}

	public static int prefToIndex( final String pref )
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			if ( questLogData[ i ][ 0 ].toLowerCase().indexOf( pref.toLowerCase() ) != -1 )
			{
				return i;
			}
		}

		// couldn't find a match
		return -1;
	}

	public static String findQuestProgress( String pref, String details )
	{
		// Special handling due to multiple endings
		if ( pref.equals( "questL12War" ) )
		{
			return handleWarStatus( details );
		}

		// First thing to do is find which quest we're talking about.
		int index = prefToIndex( pref );

		if ( index == -1 )
		{
			return "";
		}

		// Next, find the number of quest steps
		final int steps = questLogData[ index ].length - 2;

		if ( steps < 1 )
		{
			return "";
		}

		// Now, try to see if we can find an exact match for response->step. This is often messed up by
		// whitespace, html, and the like. We'll handle that below.
		int foundAtStep = -1;

		for ( int i = 2; i < questLogData[ index ].length; ++i )
		{
			if ( questLogData[ index ][ i ].indexOf( details ) != -1 )
			{
				foundAtStep = i - 2;
				break;
			}
		}

		if ( foundAtStep == -1 )
		{
			// Didn't manage to find an exact match. Now try stripping out all whitespace, newlines, and
			// anything that looks like html from questData and response. And make everything lower case,
			// because player names can be arbitrarily capitalized.
			String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( details ).replaceAll( "" )
				.toLowerCase();
			String cleanedQuest = "";

			// RequestLogger.printLine( cleanedResponse );

			for ( int i = 2; i < questLogData[ index ].length; ++i )
			{
				cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( questLogData[ index ][ i ] )
					.replaceAll( "" ).toLowerCase();
				// RequestLogger.printLine( cleanedQuest );
				if ( cleanedQuest.indexOf( cleanedResponse ) != -1 )
				{
					foundAtStep = i - 2;
					break;
				}
			}
		}

		if ( foundAtStep != -1 )
		{
			if ( foundAtStep == 0 )
			{
				return QuestDatabase.STARTED;
			}
			else if ( foundAtStep == steps - 1 )
			{
				return QuestDatabase.FINISHED;
			}
			else
			{
				return "step" + foundAtStep;
			}
		}

		if ( pref.equals( "questG04Nemesis" ) && details.indexOf( "Demonic Lord of Revenge" ) != -1 )
		{
			// Hard code the end of the nemesis quest, for now. We could eventually programmatically handle
			// the <demon name> in the response.
			return QuestDatabase.FINISHED;
		}

		// Well, none of the above worked. Punt.
		return "";
	}

	private static String handleWarStatus( String details )
	{
		if ( details.indexOf( "You led the filthy hippies to victory" ) != -1
			|| details.indexOf( "You led the Orcish frat boys to victory" ) != -1
			|| details.indexOf( "You started a chain of events" ) != -1 )
		{
			return QuestDatabase.FINISHED;
		}
		return "";
	}

	public static void setQuestProgress( String pref, String status )
	{
		if ( prefToIndex( pref ) == -1 )
		{
			return;
		}

		if ( !status.equals( QuestDatabase.STARTED ) && !status.equals( QuestDatabase.FINISHED )
			&& status.indexOf( "step" ) == -1 && !status.equals( QuestDatabase.UNSTARTED ) )
		{
			return;
		}
		Preferences.setString( pref, status );
	}

	public static void resetQuests()
	{
		for ( int i = 0; i < questLogData.length; ++i )
		{
			QuestDatabase.setQuestProgress( questLogData[ i ][ 0 ], QuestDatabase.UNSTARTED );
		}
	}

	public static void handleCouncilText( String responseText )
	{
		String cleanedResponse = QuestDatabase.HTML_WHITESPACE.matcher( responseText ).replaceAll( "" )
			.toLowerCase();
		String cleanedQuest = "";

		String pref = "";
		String status = "";

		boolean found = false;
		for ( int i = 0; i < councilData.length && !found; ++i )
		{
			for ( int j = 2; j < councilData[ i ].length && !found; ++j )
			{
				cleanedQuest = QuestDatabase.HTML_WHITESPACE.matcher( councilData[ i ][ j ] )
					.replaceAll( "" ).toLowerCase();
				if ( cleanedResponse.indexOf( cleanedQuest ) != -1 )
				{
					pref = councilData[ i ][ 0 ];
					status = councilData[ i ][ 1 ];
					found = true;
				}
			}
		}
		if ( found )
		{
			setQuestIfBetter( pref, status );
		}
	}

	private static void setQuestIfBetter( String pref, String status )
	{
		String currentStatus = Preferences.getString( pref );
		boolean shouldSet = false;

		if ( currentStatus.equals( QuestDatabase.UNSTARTED ) )
		{
			shouldSet = true;
		}
		else if ( currentStatus.equals( QuestDatabase.STARTED ) )
		{
			if ( status.startsWith( "step" ) || status.equals( QuestDatabase.FINISHED ) )
			{
				shouldSet = true;
			}
		}
		else if ( currentStatus.startsWith( "step" ) )
		{
			if ( status.equals( QuestDatabase.FINISHED ) )
			{
				shouldSet = true;
			}
			else if ( status.startsWith( "step" ) )
			{
				try
				{
					int currentStep = StringUtilities.parseInt( currentStatus.substring( 4 ) );
					int nextStep = StringUtilities.parseInt( status.substring( 4 ) );

					if ( nextStep > currentStep )
					{
						shouldSet = true;
					}
				}
				catch ( NumberFormatException e )
				{
					shouldSet = true;
				}
			}
		}
		else if ( currentStatus.equals( QuestDatabase.FINISHED ) )
		{
			shouldSet = false;
		}
		else
		{
			// there was something garbled in the preference. overwrite it.
			shouldSet = true;
		}
		
		if ( shouldSet )
		{
			QuestDatabase.setQuestProgress( pref, status );
		}
	}
}
