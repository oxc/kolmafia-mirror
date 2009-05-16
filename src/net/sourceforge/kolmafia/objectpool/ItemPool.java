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

package net.sourceforge.kolmafia.objectpool;

import net.sourceforge.kolmafia.AdventureResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class ItemPool
{
	public static final int SEAL_CLUB = 1;
	public static final int SEAL_TOOTH = 2;
	public static final int HELMET_TURTLE = 3;
	public static final int TURTLE_TOTEM = 4;
	public static final int PASTA_SPOON = 5;
	public static final int RAVIOLI_HAT = 6;
	public static final int SAUCEPAN = 7;
	public static final int SPICES = 8;
	public static final int DISCO_BALL = 9;
	public static final int DISCO_MASK = 10;
	public static final int STOLEN_ACCORDION = 11;
	public static final int MARIACHI_PANTS = 12;
	public static final int ASPARAGUS_KNIFE = 19;
	public static final int CHEWING_GUM = 23;
	public static final int TEN_LEAF_CLOVER = 24;
	public static final int MEAT_PASTE = 25;
	public static final int DOLPHIN_KING_MAP = 26;
	public static final int SPIDER_WEB = 27;
	public static final int BIG_ROCK = 30;
	public static final int CASINO_PASS = 40;
	public static final int SCHLITZ = 41;
	public static final int HERMIT_PERMIT = 42;
	public static final int WORTHLESS_TRINKET = 43;
	public static final int WORTHLESS_GEWGAW = 44;
	public static final int WORTHLESS_KNICK_KNACK = 45;
	public static final int ROCK_N_ROLL_LEGEND = 50;
	public static final int STONE_BANJO = 53;
	public static final int DISCO_BANJO = 54;
	public static final int FIVE_ALARM_SAUCEPAN = 57;
	public static final int MACE_OF_THE_TORTOISE = 60;
	public static final int FORTUNE_COOKIE = 61;
	public static final int NEWBIESPORT_TENT = 69;
	public static final int WOODEN_STAKES = 71;
	public static final int BARSKIN_TENT = 73;
	public static final int SPOOKY_MAP = 74;
	public static final int SPOOKY_SAPLING = 75;
	public static final int SPOOKY_FERTILIZER = 76;
	public static final int PRETTY_BOUQUET = 78;
	public static final int GRAVY_BOAT = 80;
	public static final int WILLER = 81;
	public static final int LOCKED_LOCKER = 84;
	public static final int TBONE_KEY = 86;
	public static final int MEAT_FROM_YESTERDAY = 87;
	public static final int MEAT_STACK = 88;
	public static final int MEAT_GOLEM = 101;
	public static final int SCARECROW = 104;
	public static final int KETCHUP = 106;
	public static final int CATSUP = 107;
	public static final int SPRING = 118;
	public static final int SPROCKET = 119;
	public static final int COG = 120;
	public static final int FRILLY_SKIRT = 131;
	public static final int BITCHIN_MEATCAR = 134;
	public static final int DINGY_PLANKS = 140;
	public static final int DINGHY_DINGY = 141;
	public static final int COTTAGE = 143;
	public static final int BARBED_FENCE = 145;
	public static final int DINGHY_PLANS = 146;
	public static final int BAKE_OVEN = 157;
	public static final int DOUGH = 159;
	public static final int SKELETON_BONE = 163;
	public static final int BONE_RATTLE = 168;
	public static final int BRIEFCASE = 184;
	public static final int FAT_STACKS_OF_CASH = 185;
	public static final int ENCHANTED_BEAN = 186;
	public static final int BAT_GUANO = 188;
	public static final int BATSKIN_BELT = 192;
	public static final int RAT_WHISKER = 197;
	public static final int DISASSEMBLED_CLOVER = 196;
	public static final int FENG_SHUI = 210;
	public static final int FOUNTAIN = 211;
	public static final int WINDCHIMES = 212;
	public static final int EYEPATCH = 224;
	public static final int COCKTAIL_KIT = 236;
	public static final int TOMATO = 246;
	public static final int DENSE_STACK = 258;
	public static final int MULLET_WIG = 267;
	public static final int PICKET_FENCE = 270;
	public static final int MOSQUITO_LARVA = 275;
	public static final int BORIS_KEY = 282;
	public static final int JARLSBERG_KEY = 283;
	public static final int SNEAKY_PETE_KEY = 284;
	public static final int FLAT_DOUGH = 301;
	public static final int DRY_NOODLES = 304;
	public static final int KNOB_GOBLIN_PERFUME = 307;
	public static final int GOAT_CHEESE = 322;
	public static final int TENDER_HAMMER = 338;
	public static final int SELTZER = 344;
	public static final int REAGENT = 346;
	public static final int DYSPEPSI_COLA = 347;
	public static final int MINERS_HELMET = 360;
	public static final int MINERS_PANTS = 361;
	public static final int MATTOCK = 362;
	public static final int LINOLEUM_ORE = 363;
	public static final int ASBESTOS_ORE = 364;
	public static final int CHROME_ORE = 365;
	public static final int ACOUSTIC_GUITAR = 404;
	public static final int PIRATE_CHEST = 405;
	public static final int PIRATE_PELVIS = 406;
	public static final int PIRATE_SKULL = 407;
	public static final int JOLLY_CHARRRM = 411;
	public static final int JOLLY_BRACELET = 413;
	public static final int BEANBAG_CHAIR = 429;
	public static final int LONG_SKINNY_BALLOON = 433;
	public static final int BALLOON_MONKEY = 436;
	public static final int CHEF = 438;
	public static final int BARTENDER = 440;
	public static final int PRETENTIOUS_PAINTBRUSH = 450;
	public static final int PRETENTIOUS_PALETTE = 451;
	public static final int RUSTY_SCREWDRIVER = 454;
	public static final int TRANSFUNCTIONER = 458;
	public static final int WHITE_PIXEL = 459;
	public static final int BLACK_PIXEL = 460;
	public static final int RED_PIXEL = 461;
	public static final int GREEN_PIXEL = 462;
	public static final int BLUE_PIXEL = 463;
	public static final int RED_PIXEL_POTION = 464;
	public static final int HOT_WING = 471;
	public static final int DODECAGRAM = 479;
	public static final int CANDLES = 480;
	public static final int BUTTERKNIFE = 481;
	public static final int TALISMAN = 486;
	public static final int KETCHUP_HOUND = 493;
	public static final int RAFFLE_TICKET = 500;
	public static final int PAGODA_PLANS = 502;
	public static final int HEAVY_METAL_GUITAR = 507;
	public static final int HEY_DEZE_NUTS = 509;
	public static final int HEY_DEZE_MAP = 516;
	public static final int STRANGE_LEAFLET = 520;
	public static final int HOUSE = 526;
	public static final int ABRIDGED = 534;
	public static final int BRIDGE = 535;
	public static final int DICTIONARY = 536;
	public static final int SCROLL_334 = 547;
	public static final int SCROLL_668 = 548;
	public static final int SCROLL_30669 = 549;
	public static final int SCROLL_33398 = 550;
	public static final int SCROLL_64067 = 551;
	public static final int GATES_SCROLL = 552;
	public static final int ELITE_SCROLL = 553;
	public static final int CAN_LID = 559;
	public static final int SONAR = 563;
	public static final int LUCIFER = 571;
	public static final int REMEDY = 588;
	public static final int TINY_HOUSE = 592;
	public static final int DRASTIC_HEALING = 595;
	public static final int SLUG_LORD_MAP = 598;
	public static final int DR_HOBO_MAP = 601;
	public static final int SHOPPING_LIST = 602;
	public static final int TISSUE_PAPER_IMMATERIA = 605;
	public static final int TIN_FOIL_IMMATERIA = 606;
	public static final int GAUZE_IMMATERIA = 607;
	public static final int PLASTIC_WRAP_IMMATERIA = 608;
	public static final int SOCK = 609;
	public static final int FURRY_FUR = 616;
	public static final int GIANT_NEEDLE = 619;
	public static final int BLACK_CANDLE = 620;
	public static final int WARM_SUBJECT = 621;
	public static final int AWFUL_POETRY_JOURNAL = 622;
	public static final int WAND_OF_NAGAMAR = 626;
	public static final int TOASTER = 637;
	public static final int SKELETON_KEY = 642;
	public static final int SKELETON_KEY_RING = 643;
	public static final int QUANTUM_EGG = 652;
	public static final int ROWBOAT = 653;
	public static final int STAR = 654;
	public static final int LINE = 655;
	public static final int STAR_CHART = 656;
	public static final int STAR_SWORD = 657;
	public static final int STAR_CROSSBOW = 658;
	public static final int STAR_STAFF = 659;
	public static final int STAR_HAT = 661;
	public static final int STAR_STARFISH = 664;
	public static final int STAR_KEY = 665;
	public static final int STEAMING_EVIL = 666;
	public static final int GIANT_CASTLE_MAP = 667;
	public static final int DRAGONBONE_BELT_BUCKLE = 676;
	public static final int BADASS_BELT = 677;
	public static final int DIGITAL_KEY = 691;
	public static final int JEWELRY_PLIERS = 709;
	public static final int BACONSTONE_EARRING = 715;
	public static final int BACONSTONE_BRACELET = 717;
	public static final int MIRROR_SHARD = 726;
	public static final int PUZZLE_PIECE = 727;
	public static final int HEDGE_KEY = 728;
	public static final int FISHBOWL = 729;
	public static final int FISHTANK = 730;
	public static final int FISH_HOSE = 731;
	public static final int HOSED_TANK = 732;
	public static final int HOSED_FISHBOWL = 733;
	public static final int SCUBA_GEAR = 734;
	public static final int SINISTER_STRUMMING = 736;
	public static final int SQUEEZINGS_OF_WOE = 737;
	public static final int REALLY_EVIL_RHYTHM = 738;
	public static final int TAMBOURINE = 740;
	public static final int BROKEN_SKULL = 741;
	public static final int KNOB_FIRECRACKER = 746;
	public static final int FLAMING_MUSHROOM = 755;
	public static final int FROZEN_MUSHROOM = 756;
	public static final int STINKY_MUSHROOM = 757;
	public static final int CUMMERBUND = 778;
	public static final int MAFIA_ARIA = 781;
	public static final int PLUS_SIGN = 818;
	public static final int MILKY_POTION = 819;
	public static final int SWIRLY_POTION = 820;
	public static final int BUBBLY_POTION = 821;
	public static final int SMOKY_POTION = 822;
	public static final int CLOUDY_POTION = 823;
	public static final int EFFERVESCENT_POTION = 824;
	public static final int FIZZY_POTION = 825;
	public static final int DARK_POTION = 826;
	public static final int MURKY_POTION = 827;
	public static final int ANTIDOTE = 829;
	public static final int SHOCK_COLLAR = 856;
	public static final int MOONGLASSES = 857;
	public static final int LEAD_NECKLACE = 865;
	public static final int TEARS = 869;
	public static final int ROLLING_PIN = 873;
	public static final int UNROLLING_PIN = 874;
	public static final int GOOFBALLS = 879;
	public static final int DWARF_BREAD = 910;
	public static final int PLASTIC_SWORD = 938;
	public static final int DIRTY_MARTINI = 948;
	public static final int GROGTINI = 949;
	public static final int CHERRY_BOMB = 950;
	public static final int MAID = 1000;
	public static final int TEQUILA = 1004;
	public static final int VESPER = 1023;
	public static final int BODYSLAM = 1024;
	public static final int SANGRIA_DEL_DIABLO = 1025;
	public static final int TAM_O_SHANTER = 1040;
	public static final int GREEN_BEER = 1041;
	public static final int TARGETING_CHIP = 1102;
	public static final int CLOCKWORK_BARTENDER = 1111;
	public static final int CLOCKWORK_CHEF = 1112;
	public static final int CLOCKWORK_MAID = 1113;
	public static final int ANNOYING_PITCHFORK = 1116;
	public static final int PREGNANT_FLAMING_MUSHROOM = 1118;
	public static final int PREGNANT_FROZEN_MUSHROOM = 1119;
	public static final int PREGNANT_STINKY_MUSHROOM = 1120;
	public static final int INEXPLICABLY_GLOWING_ROCK = 1121;
	public static final int SPOOKY_GLOVE = 1125;
	public static final int GRAVY_MAYPOLE = 1152;
	public static final int GIFT1 = 1167;
	public static final int GIFT2 = 1168;
	public static final int GIFT3 = 1169;
	public static final int GIFT4 = 1170;
	public static final int GIFT5 = 1171;
	public static final int GIFT6 = 1172;
	public static final int GIFT7 = 1173;
	public static final int GIFT8 = 1174;
	public static final int GIFT9 = 1175;
	public static final int GIFT10 = 1176;
	public static final int GIFT11 = 1177;
	public static final int RAT_BALLOON = 1218;
	public static final int STAINLESS_STEEL_SOLITAIRE = 1226;
	public static final int PLEXIGLASS_POCKETWATCH = 1232;
	public static final int PLEXIGLASS_PENDANT = 1235;
	public static final int TOY_HOVERCRAFT = 1243;
	public static final int PRETENTIOUS_PAIL = 1258;
	public static final int WAX_LIPS = 1260;
	public static final int NOSE_BONE_FETISH = 1264;
	public static final int GLOOMY_BLACK_MUSHROOM = 1266;
	public static final int DEAD_MIMIC = 1267;
	public static final int PINE_WAND = 1268;
	public static final int EBONY_WAND = 1269;
	public static final int HEXAGONAL_WAND = 1270;
	public static final int ALUMINUM_WAND = 1271;
	public static final int MARBLE_WAND = 1272;
	public static final int MAKEUP_KIT = 1305;
	public static final int COMFY_BLANKET = 1305;
	public static final int FACSIMILE_DICTIONARY = 1316;
	public static final int CLOACA_COLA = 1334;
	public static final int TOY_SOLDIER = 1397;
	public static final int SNOWCONE_BOOK = 1411;
	public static final int PURPLE_SNOWCONE = 1412;
	public static final int GREEN_SNOWCONE = 1413;
	public static final int ORANGE_SNOWCONE = 1414;
	public static final int RED_SNOWCONE = 1415;
	public static final int BLUE_SNOWCONE = 1416;
	public static final int BLACK_SNOWCONE = 1417;
	public static final int TEDDY_SEWING_KIT = 1419;
	public static final int ICEBERGLET = 1423;
	public static final int ICE_SICKLE = 1424;
	public static final int ICE_BABY = 1425;
	public static final int ICE_PICK = 1426;
	public static final int ICE_SKATES = 1427;
	public static final int OILY_GOLDEN_MUSHROOM = 1432;
	public static final int USELESS_POWDER = 1437;
	public static final int TWINKLY_WAD = 1450;
	public static final int HOT_WAD = 1451;
	public static final int COLD_WAD = 1452;
	public static final int SPOOKY_WAD = 1453;
	public static final int STENCH_WAD = 1454;
	public static final int SLEAZE_WAD = 1455;
	public static final int GIFTV = 1460;
	public static final int BAG_OF_CATNIP = 1486;
	public static final int HANG_GLIDER = 1487;
	public static final int MINIATURE_DORMOUSE = 1489;
	public static final int HILARIOUS_BOOK = 1498;
	public static final int RUBBER_EMO_ROE = 1503;
	public static final int BAKULA = 1519;
	public static final int SNOOTY_DISGUISE = 1526;
	public static final int GIFTR = 1534;
	public static final int WEEGEE_SQOUIJA = 1537;
	public static final int TAM_O_SHATNER = 1539;
	public static final int MSG = 1549;
	public static final int GRIMACITE_GOGGLES = 1540;
	public static final int GRIMACITE_GLAIVE = 1541;
	public static final int GRIMACITE_GREAVES = 1542;
	public static final int GRIMACITE_GARTER = 1543;
	public static final int GRIMACITE_GALOSHES = 1544;
	public static final int GRIMACITE_GORGET = 1545;
	public static final int GRIMACITE_GUAYABERA = 1546;
	public static final int CATALYST = 1605;
	public static final int ULTIMATE_WAD = 1606;
	public static final int MUNCHIES_PILL = 1619;
	public static final int ASTRAL_MUSHROOM = 1622;
	public static final int BADGER_BADGE = 1623;
	public static final int BLUE_CUPCAKE = 1624;
	public static final int GREEN_CUPCAKE = 1625;
	public static final int ORANGE_CUPCAKE = 1626;
	public static final int PURPLE_CUPCAKE = 1627;
	public static final int PINK_CUPCAKE = 1628;
	public static final int SPANISH_FLY = 1633;
	public static final int PHIAL_OF_HOTNESS = 1637;
	public static final int PHIAL_OF_COLDNESS = 1638;
	public static final int PHIAL_OF_SPOOKINESS = 1639;
	public static final int PHIAL_OF_STENCH = 1640;
	public static final int PHIAL_OF_SLEAZINESS = 1641;
	public static final int MILK_OF_MAGNESIUM = 1650;
	public static final int JEWEL_EYED_WIZARD_HAT = 1653;
	public static final int CITADEL_SATCHEL = 1656;
	public static final int FRAUDWORT = 1670;
	public static final int SHYSTERWEED = 1671;
	public static final int SWINDLEBLOSSOM = 1672;
	public static final int GROUCHO_DISGUISE = 1678;
	public static final int EXPRESS_CARD = 1687;
	public static final int SWORD_PREPOSITIONS = 1734;
	public static final int LIBRARY_KEY = 1764;
	public static final int GALLERY_KEY = 1765;
	public static final int BALLROOM_KEY = 1766;
	public static final int PACK_OF_CHEWING_GUM = 1767;
	public static final int DUSTY_ANIMAL_SKULL = 1799;
	public static final int SPOOKYRAVEN_SPECTACLES = 1916;
	public static final int TOILET_PAPER = 1923;
	public static final int TUNING_FORK = 1928;
	public static final int ANTIQUE_GREAVES = 1929;
	public static final int ANTIQUE_HELMET = 1930;
	public static final int ANTIQUE_SPEAR = 1931;
	public static final int ANTIQUE_SHIELD = 1932;
	public static final int QUILL_PEN = 1957;
	public static final int INKWELL = 1958;
	public static final int SCRAP_OF_PAPER = 1959;
	public static final int EVIL_SCROLL = 1960;
	public static final int DANCE_CARD = 1963;
	public static final int PUMPKIN_BUCKET = 1971;
	public static final int STUFFED_COCOABO = 1974;
	public static final int MACGUFFIN_DIARY = 2044;
	public static final int BROKEN_WINGS = 2050;
	public static final int SUNKEN_EYES = 2051;
	public static final int REASSEMBLED_BLACKBIRD = 2052;
	public static final int BLACK_MARKET_MAP = 2054;
	public static final int FORGED_ID_DOCUMENTS = 2064;
	public static final int PADL_PHONE = 2065;
	public static final int NOVELTY_BUTTON = 2072;
	public static final int MAKESHIFT_TURBAN = 2079;
	public static final int MAKESHIFT_CAPE = 2080;
	public static final int MAKESHIFT_SKIRT = 2081;
	public static final int MAKESHIFT_CRANE = 2083;
	public static final int CAN_OF_STARCH = 2084;
	public static final int TOWEL = 2085;
	public static final int LUCRE = 2098;
	public static final int ASCII_SHIRT = 2121;
	public static final int TOY_MERCENARY = 2139;
	public static final int EVIL_TEDDY_SEWING_KIT = 2147;
	public static final int TRIANGULAR_STONE = 2173;
	public static final int MOSSY_STONE_SPHERE = 2174;
	public static final int SMOOTH_STONE_SPHERE = 2175;
	public static final int CRACKED_STONE_SPHERE = 2176;
	public static final int ROUGH_STONE_SPHERE = 2177;
	public static final int HAROLDS_HAMMER = 2184;
	public static final int ANCIENT_CAROLS = 2191;
	public static final int SHEET_MUSIC = 2192;
	public static final int CRIMBO_UKELELE = 2209;
	public static final int LIARS_PANTS = 2222;
	public static final int JUGGLERS_BALLS = 2223;
	public static final int PINK_SHIRT = 2224;
	public static final int FAMILIAR_DOPPELGANGER = 2225;
	public static final int EYEBALL_PENDANT = 2226;
	public static final int CALAVERA_CONCERTINA = 2234;
	public static final int PALINDROME_BOOK = 2258;
	public static final int PHOTOGRAPH_OF_GOD = 2259;
	public static final int HARD_ROCK_CANDY = 2260;
	public static final int OSTRICH_EGG = 2261;
	public static final int WET_STUNT_NUT_STEW = 2266;
	public static final int MEGA_GEM = 2267;
	public static final int FERNSWARTHYS_KEY = 2277;
	public static final int DUSTY_BOOK = 2279;
	public static final int MUS_MANUAL = 2280;
	public static final int MYS_MANUAL = 2281;
	public static final int MOX_MANUAL = 2282;
	public static final int SEAL_HELMET = 2283;
	public static final int RED_PAPER_CLIP = 2289;
	public static final int REALLY_BIG_TINY_HOUSE = 2290;
	public static final int NONESSENTIAL_AMULET = 2291;
	public static final int WHITE_WINE_VINAIGRETTE = 2292;
	public static final int CUP_OF_STRONG_TEA = 2293;
	public static final int CURIOUSLY_SHINY_AX = 2294;
	public static final int MARINATED_STAKES = 2295;
	public static final int KNOB_BUTTER = 2296;
	public static final int VIAL_OF_ECTOPLASM = 2297;
	public static final int BOOCK_OF_MAGIKS = 2298;
	public static final int EZ_PLAY_HARMONICA_BOOK = 2299;
	public static final int FINGERLESS_HOBO_GLOVES = 2300;
	public static final int CHOMSKYS_COMICS = 2301;
	public static final int WORM_RIDING_HOOKS = 2302;
	public static final int CANDY_BOOK = 2303;
	public static final int ANCIENT_BRONZE_TOKEN = 2317;
	public static final int ANCIENT_BOMB = 2318;
	public static final int CARVED_WOODEN_WHEEL = 2319;
	public static final int WORM_RIDING_MANUAL_1 = 2320;
	public static final int WORM_RIDING_MANUAL_2 = 2321;
	public static final int WORM_RIDING_MANUAL_3_15 = 2322;
	public static final int STONE_ROSE = 2326;
	public static final int BLACK_PAINT = 2327;
	public static final int DRUM_MACHINE = 2328;
	public static final int CONFETTI = 2329;
	public static final int HOLY_MACGUFFIN = 2334;
	public static final int BLACK_PUDDING = 2338;
	public static final int FILTHWORM_QUEEN_HEART = 2347;
	public static final int COMMUNICATIONS_WINDCHIMES = 2354;
	public static final int ZIM_MERMANS_GUITAR = 2364;
	public static final int FILTHY_POULTICE = 2369;
	public static final int GAUZE_GARTER = 2402;
	public static final int GUNPOWDER = 2403;
	public static final int JAM_BAND_FLYERS = 2404;
	public static final int ROCK_BAND_FLYERS = 2405;
	public static final int RHINO_HORMONES = 2419;
	public static final int MAGIC_SCROLL = 2420;
	public static final int PIRATE_JUICE = 2421;
	public static final int PET_SNACKS = 2422;
	public static final int INHALER = 2423;
	public static final int CYCLOPS_EYEDROPS = 2424;
	public static final int SPINACH = 2425;
	public static final int FIRE_FLOWER = 2426;
	public static final int ICE_CUBE = 2427;
	public static final int FAKE_BLOOD = 2428;
	public static final int GUANEAU = 2429;
	public static final int LARD = 2430;
	public static final int MYSTIC_SHELL = 2431;
	public static final int LIP_BALM = 2432;
	public static final int ANTIFREEZE = 2433;
	public static final int BLACK_EYEDROPS = 2434;
	public static final int DOGSGOTNONOZ = 2435;
	public static final int FLIPBOOK = 2436;
	public static final int NEW_CLOACA_COLA = 2437;
	public static final int MASSAGE_OIL = 2438;
	public static final int POLTERGEIST = 2439;
	public static final int ENCRYPTION_KEY = 2441;
	public static final int COBBS_KNOB_MAP = 2442;
	public static final int ODOR_EXTRACTOR = 2462;
	public static final int OLFACTION_BOOK = 2463;
	public static final int SHIRT_KIT = 2491;
	public static final int MOLYBDENUM_MAGNET = 2497;
	public static final int MOLYBDENUM_HAMMER = 2498;
	public static final int MOLYBDENUM_SCREWDRIVER = 2499;
	public static final int MOLYBDENUM_PLIERS = 2500;
	public static final int MOLYBDENUM_WRENCH = 2501;
	public static final int JEWELRY_BOOK = 2502;
	public static final int WOVEN_BALING_WIRE_BRACELETS = 2514;
	public static final int TOMB_RATCHET = 2540;
	public static final int MAYFLOWER_BOUQUET = 2541;
	public static final int OUTRAGEOUS_SOMBRERO = 2548;
	public static final int SHAGADELIC_DISCO_BANJO = 2556;
	public static final int SQUEEZEBOX_OF_THE_AGES = 2557;
	public static final int CHELONIAN_MORNINGSTAR = 2558;
	public static final int SEVENTEEN_ALARM_SAUCEPAN = 2560;
	public static final int AZAZELS_UNICORN = 2566;
	public static final int AZAZELS_LOLLYPOP = 2567;
	public static final int AZAZELS_TUTU = 2568;
	public static final int ANT_HOE = 2570;
	public static final int ANT_RAKE = 2571;
	public static final int ANT_PITCHFORK = 2572;
	public static final int ANT_SICKLE = 2573;
	public static final int ANT_PICK = 2574;
	public static final int HANDFUL_OF_SAND = 2581;
	public static final int SAND_BRICK = 2582;
	public static final int TASTY_TART = 2591;
	public static final int LUNCHBOX = 2592;
	public static final int KNOB_PASTY = 2593;
	public static final int KNOB_COFFEE = 2594;
	public static final int TELESCOPE = 2599;
	public static final int PALM_FROND = 2605;
	public static final int MOJO_FILTER = 2614;
	public static final int MUMMY_WRAP = 2634;
	public static final int GAUZE_HAMMOCK = 2638;
	public static final int ABSINTHE = 2655;
	public static final int LIBRARY_CARD = 2672;
	public static final int SPECTRE_SCEPTER = 2678;
	public static final int SPARKLER = 2679;
	public static final int SNAKE = 2680;
	public static final int M282 = 2681;
	public static final int GIFTW = 2683;
	public static final int MASSIVE_SITAR = 2693;
	public static final int DUCT_TAPE = 2697;
	public static final int PARROT_CRACKER = 2710;
	public static final int STEEL_STOMACH = 2742;
	public static final int STEEL_LIVER = 2743;
	public static final int STEEL_SPLEEN = 2744;
	public static final int HAROLDS_BELL = 2765;
	public static final int SOLID_BACONSTONE_EARRING = 2780;
	public static final int BRIMSTONE_BERET = 2813;
	public static final int BRIMSTONE_BRACELET = 2818;
	public static final int GRIMACITE_GASMASK = 2819;
	public static final int GRIMACITE_GAT = 2820;
	public static final int GRIMACITE_GAITERS = 2821;
	public static final int GRIMACITE_GAUNTLETS = 2822;
	public static final int GRIMACITE_GO_GO_BOOTS = 2823;
	public static final int GRIMACITE_GIRDLE = 2824;
	public static final int GRIMACITE_GOWN = 2825;
	public static final int BOTTLE_ROCKET = 2834;
	public static final int NAVEL_RING = 2844;
	public static final int PLASTIC_BIB = 2846;
	public static final int GNOME_DEMODULIZER = 2848;
	public static final int PIRATE_INSULT_BOOK = 2947;
	public static final int CARONCH_MAP = 2950;
	public static final int FRATHOUSE_BLUEPRINTS = 2951;
	public static final int CHARRRM_BRACELET = 2953;
	public static final int RUM_CHARRRM = 2957;
	public static final int RUM_BRACELET = 2959;
	public static final int RIGGING_SHAMPOO = 2963;
	public static final int BALL_POLISH = 2964;
	public static final int MIZZENMAST_MOP = 2965;
	public static final int GRUMPY_CHARRRM = 2972;
	public static final int GRUMPY_BRACELET = 2973;
	public static final int TARRRNISH_CHARRRM = 2974;
	public static final int TARRRNISH_BRACELET = 2975;
	public static final int BOOTY_CHARRRM = 2980;
	public static final int BOOTY_BRACELET = 2981;
	public static final int CANNONBALL_CHARRRM = 2982;
	public static final int CANNONBALL_BRACELET = 2983;
	public static final int COPPER_CHARRRM = 2984;
	public static final int COPPER_BRACELET = 2985;
	public static final int TONGUE_CHARRRM = 2986;
	public static final int TONGUE_BRACELET = 2987;
	public static final int CLINGFILM = 2988;
	public static final int CARONCH_NASTY_BOOTY = 2999;
	public static final int CARONCH_DENTURES = 3000;
	public static final int IDOL_AKGYXOTH = 3009;
	public static final int EMBLEM_AKGYXOTH = 3010;
	public static final int SIMPLE_CURSED_KEY = 3013;
	public static final int ORNATE_CURSED_KEY = 3014;
	public static final int GILDED_CURSED_KEY = 3015;
	public static final int ANCIENT_CURSED_FOOTLOCKER = 3016;
	public static final int ORNATE_CURSED_CHEST = 3017;
	public static final int GILDED_CURSED_CHEST = 3018;
	public static final int PIRATE_FLEDGES = 3033;
	public static final int CURSED_PIECE_OF_THIRTEEN = 3034;
	public static final int FOIL_BOW = 3043;
	public static final int FOIL_RADAR = 3044;
	public static final int POWER_SPHERE = 3049;
	public static final int FOIL_CAT_EARS = 3056;
	public static final int LASER_CANON = 3069;
	public static final int CHIN_STRAP = 3070;
	public static final int GLUTEAL_SHIELD = 3071;
	public static final int CARBONITE_VISOR = 3072;
	public static final int UNOBTAINIUM_STRAPS = 3073;
	public static final int FASTENING_APPARATUS = 3074;
	public static final int GENERAL_ASSEMBLY_MODULE = 3075;
	public static final int TARGETING_CHOP = 3076;
	public static final int LEG_ARMOR = 3077;
	public static final int KEVLATEFLOCITE_HELMET = 3078;
	public static final int TEDDY_BORG_SEWING_KIT = 3087;
	public static final int HOBBY_HORSE = 3092;
	public static final int BALL_IN_A_CUP = 3093;
	public static final int SET_OF_JACKS = 3094;
	public static final int FISH_SCALER = 3097;
	public static final int MINIBORG_STOMPER = 3109;
	public static final int MINIBORG_STRANGLER = 3110;
	public static final int MINIBORG_LASER = 3111;
	public static final int MINIBORG_BEEPER = 3112;
	public static final int MINIBORG_HIVEMINDER = 3113;
	public static final int MINIBORG_DESTROYOBOT = 3114;
	public static final int DIVINE_BOOK = 3117;
	public static final int DIVINE_CHAMPAGNE_POPPER = 3121;
	public static final int DIVINE_FLUTE = 3123;
	public static final int HOBO_NICKEL = 3126;
	public static final int SANDCASTLE = 3127;
	public static final int MARSHMALLOW = 3128;
	public static final int ROASTED_MARSHMALLOW = 3129;
	public static final int PUNCHCARD_ATTACK = 3146;
	public static final int PUNCHCARD_REPAIR = 3147;
	public static final int PUNCHCARD_BUFF = 3148;
	public static final int PUNCHCARD_MODIFY = 3149;
	public static final int PUNCHCARD_BUILD = 3150;
	public static final int PUNCHCARD_TARGET = 3151;
	public static final int PUNCHCARD_SELF = 3152;
	public static final int PUNCHCARD_FLOOR = 3153;
	public static final int PUNCHCARD_DRONE = 3154;
	public static final int PUNCHCARD_WALL = 3155;
	public static final int PUNCHCARD_SPHERE = 3156;
	public static final int DRONE = 3157;
	public static final int EL_VIBRATO_HELMET = 3162;
	public static final int BROKEN_DRONE = 3165;
	public static final int REPAIRED_DRONE = 3166;
	public static final int AUGMENTED_DRONE = 3167;
	public static final int FORTUNE_TELLER = 3193;
	public static final int ORIGAMI_MAGAZINE = 3194;
	public static final int PAPER_SHURIKEN = 3195;
	public static final int ORIGAMI_PASTIES = 3196;
	public static final int RIDING_CROP = 3197;
	public static final int TRAPEZOID = 3198;
	public static final int LUMP_OF_COAL = 3199;
	public static final int THICK_PADDED_ENVELOPE = 3201;
	public static final int DWARVISH_PUNCHCARD = 3207;
	public static final int SMALL_LAMINATED_CARD = 3208;
	public static final int LITTLE_LAMINATED_CARD = 3209;
	public static final int NOTBIG_LAMINATED_CARD = 3210;
	public static final int UNLARGE_LAMINATED_CARD = 3211;
	public static final int DWARVISH_DOCUMENT = 3212;
	public static final int DWARVISH_PAPER = 3213;
	public static final int DWARVISH_PARCHMENT = 3214;
	public static final int OVERCHARGED_POWER_SPHERE = 3215;
	public static final int HOBO_CODE_BINDER = 3220;
	public static final int GATORSKIN_UMBRELLA = 3222;
	public static final int SEWER_WAD = 3224;
	public static final int OOZE_O = 3226;
	public static final int DUMPLINGS = 3228;
	public static final int OIL_OF_OILINESS = 3230;
	public static final int TATTERED_PAPER_CROWN = 3231;
	public static final int KISSIN_COUSINS = 3236;
	public static final int TALES_FROM_THE_FIRESIDE = 3237;
	public static final int BLIZZARDS_I_HAVE_DIED_IN = 3238;
	public static final int MAXING_RELAXING = 3239;
	public static final int BIDDY_CRACKERS_COOKBOOK = 3240;
	public static final int TRAVELS_WITH_JERRY = 3241;
	public static final int LET_ME_BE = 3242;
	public static final int ASLEEP_IN_THE_CEMETERY = 3243;
	public static final int SUMMER_NIGHTS = 3244;
	public static final int SENSUAL_MASSAGE_FOR_CREEPS = 3245;
	public static final int BAG_OF_CANDY = 3261;
	public static final int TASTEFUL_BOOK = 3263;
	public static final int BLACK_BLUE_LIGHT = 3276;
	public static final int LOUDMOUTH_LARRY = 3277;
	public static final int CHEAP_STUDDED_BELT = 3278;
	public static final int MACARONI_FRAGMENTS = 3287;
	public static final int SHIMMERING_TENDRILS = 3288;
	public static final int SCINTILLATING_POWDER = 3289;
	public static final int PERSONAL_MASSAGER = 3279;
	public static final int PLASMA_BALL = 3281;
	public static final int EPIC_WAD = 3316;
	public static final int SCRATCHS_FORK = 3323;
	public static final int FROSTYS_MUG = 3324;
	public static final int FERMENTED_PICKLE_JUICE = 3325;
	public static final int EXTRA_GREASY_SLIDER = 3327;
	public static final int DOUBLE_SIDED_TAPE = 3336;
	public static final int HOT_BEDDING = 3344;	// bed of coals
	public static final int COLD_BEDDING = 3345;	// frigid air mattress
	public static final int STENCH_BEDDING = 3346;	// filth-encrusted futon
	public static final int SPOOKY_BEDDING = 3347;	// comfy coffin
	public static final int SLEAZE_BEDDING = 3348;	// stained mattress
	public static final int ZEN_MOTORCYCLE = 3352;
	public static final int GONG = 3353;
	public static final int GRUB = 3356;
	public static final int MOTH = 3357;
	public static final int FIRE_ANT = 3358;
	public static final int ICE_ANT = 3359;
	public static final int STINKBUG = 3360;
	public static final int DEATH_WATCH_BEETLE = 3361;
	public static final int LOUSE = 3362;
	public static final int INTERESTING_TWIG = 3367;
	public static final int TWIG_HOUSE = 3374;
	public static final int RICHIE_THINGFINDER = 3375;
	public static final int MEDLEY_OF_DIVERSITY = 3376;
	public static final int EXPLOSIVE_ETUDE = 3377;
	public static final int CHORALE_OF_COMPANIONSHIP = 3378;
	public static final int PRELUDE_OF_PRECISION = 3379;
	public static final int EMPTY_EYE = 3388;
	public static final int ICEBALL = 3391;
	public static final int NEVERENDING_SODA = 3393;
	public static final int SQUEEZE = 3399;
	public static final int FISHYSOISSE = 3400;
	public static final int LAMP_SHADE = 3401;
	public static final int GARBAGE_JUICE = 3402;
	public static final int LEWD_CARD = 3403;
	public static final int HODGMAN_JOURNAL_1 = 3412;
	public static final int HODGMAN_JOURNAL_2 = 3413;
	public static final int HODGMAN_JOURNAL_3 = 3414;
	public static final int HODGMAN_JOURNAL_4 = 3415;
	public static final int HOBO_FORTRESS = 3416;
	public static final int FIREWORKS = 3421;
	public static final int GIFTH = 3430;
	public static final int SPICE_MELANGE = 3433;
	public static final int RAINBOWS_GRAVITY = 3439;
	public static final int HAIKU_KATANA = 3466;
	public static final int BATHYSPHERE = 3470;
	public static final int DAMP_OLD_BOOT = 3471;
	public static final int BOXTOP = 3473;
	public static final int SEA_SALT_CRYSTAL = 3495;
	public static final int LARP_MEMBERSHIP_CARD = 3506;
	public static final int STICKER_BOOK = 3507;
	public static final int STICKER_SWORD = 3508;
	public static final int STICKER_CROSSBOW = 3526;
	public static final int GRIMACITE_HAMMER = 3542;
	public static final int GRIMACITE_GRAVY_BOAT = 3543;
	public static final int GRIMACITE_WEIGHTLIFTING_BELT = 3544;
	public static final int GRIMACITE_GRAPPLING_HOOK = 3545;
	public static final int GRIMACITE_NINJA_MASK = 3546;
	public static final int GRIMACITE_SHINGUARDS = 3547;
	public static final int GRIMACITE_ASTROLABE = 3548;
	public static final int SEED_PACKET = 3553;
	public static final int GREEN_SLIME = 3554;
	public static final int SEA_CARROT = 3555;
	public static final int SEA_CUCUMBER = 3556;
	public static final int SEA_AVOCADO = 3557;
	public static final int POTION_OF_PUISSANCE = 3561;
	public static final int POTION_OF_PERSPICACITY = 3562;
	public static final int POTION_OF_PULCHRITUDE = 3563;
	public static final int SAND_DOLLAR = 3575;
	public static final int BEZOAR_RING = 3577;
	public static final int SUSHI_ROLLING_MAT = 3581;
	public static final int WHITE_RICE = 3582;
	public static final int RUSTY_BROKEN_DIVING_HELMET = 3602;
	public static final int BUBBLIN_STONE = 3605;
	public static final int AERATED_DIVING_HELMET = 3607;
	public static final int DAS_BOOT = 3609;
	public static final int IMITATION_WHETSTONE = 3610;
	public static final int BURROWGRUB_HIVE = 3629;
	public static final int JAMFISH_JAM = 3641;
	public static final int DRAGONFISH_CAVIAR = 3642;
	public static final int GRIMACITE_KNEECAPPING_STICK = 3644;
	public static final int MINIATURE_ANTLERS = 3651;
	public static final int SPOOKY_PUTTY_MITRE = 3662;
	public static final int SPOOKY_PUTTY_LEOTARD = 3663;
	public static final int SPOOKY_PUTTY_BALL = 3664;
	public static final int SPOOKY_PUTTY_SHEET = 3665;
	public static final int SPOOKY_PUTTY_SNAKE = 3666;
	public static final int SPOOKY_PUTTY_MONSTER = 3667;
	public static final int RAGE_GLAND = 3674;
	public static final int MERKIN_PRESSUREGLOBE = 3675;
	public static final int POTION_OF_PERCEPTION = 3593;
	public static final int POTION_OF_PROFICIENCY = 3594;
	public static final int GNOLL_EYE = 3731;
	public static final int BOOZEHOUND_TOKEN = 3739;
	public static final int UNSTABLE_QUARK = 3743;
	public static final int LOVE_BOOK = 3753;
	public static final int VAGUE_AMBIGUITY = 3754;
	public static final int SMOLDERING_PASSION = 3755;
	public static final int ICY_REVENGE = 3756;
	public static final int SUGARY_CUTENESS = 3757;
	public static final int DISTURBING_OBSESSION = 3758;
	public static final int NAUGHTY_INNUENDO = 3759;
	public static final int MERKIN_PINKSLIP = 3775;
	public static final int PARANORMAL_RICOTTA = 3784;
	public static final int SMOKING_TALON = 3785;
	public static final int VAMPIRE_GLITTER = 3786;
	public static final int WINE_SOAKED_BONE_CHIPS = 3787;
	public static final int CRUMBLING_RAT_SKULL = 3788;
	public static final int TWITCHING_TRIGGER_FINGER = 3789;
	public static final int AQUAVIOLET_JUBJUB_BIRD = 3800;
	public static final int CRIMSILION_JUBJUB_BIRD = 3801;
	public static final int CHARPUCE_JUBJUB_BIRD = 3802;
	public static final int SEA_RADISH = 3817;
	public static final int EEL_SAUCE = 3819;
	public static final int FISHY_WAND = 3822;
	public static final int GRANDMAS_NOTE = 3824;
	public static final int FUSCHIA_YARN = 3825;
	public static final int CHARTREUSE_YARN = 3826;
	public static final int GRANDMAS_MAP = 3828;
	public static final int TINY_COSTUME_WARDROBE = 3835;
	public static final int OOT_BIWA = 3842;
	public static final int JUNGLE_DRUM = 3846;
	public static final int HIPPY_BONGO = 3847;
	public static final int GUITAR_4D = 3849;
	public static final int HALF_GUITAR = 3852;
	public static final int BASS_DRUM = 3853;
	public static final int SMALLEST_VIOLIN = 3855;
	public static final int PLASTIC_GUITAR = 3863;
	public static final int FINGER_CYMBALS = 3864;
	public static final int KETTLE_DRUM = 3865;
	public static final int VIP_LOUNGE_KEY = 3947;
	public static final int STUFFED_CHEST = 3949;
	public static final int STUFFED_KEY = 3950;
	public static final int MOONTAN_LOTION = 4003;

	public static final AdventureResult get( String itemName, int count )
	{
		int itemId = ItemDatabase.getItemId( itemName, 1, false );

		if ( itemId != -1 )
		{
			return ItemPool.get( itemId, count );
		}

		return AdventureResult.tallyItem( itemName, count, false );
	}

	public static final AdventureResult get( int itemId, int count )
	{
		return new AdventureResult( itemId, count );
	}

	// Support for various classes of items:

	// BANG POTIONS and STONE SPHERES

	public static final String[][] bangPotionStrings =
	{
		// name, combat use mssage, inventory use message
		{ "inebriety", "wino", "liquid fire" },
		{ "healing", "better", "You gain" },
		{ "confusion", "confused", "Confused" },
		{ "blessing", "stylish", "Izchak's Blessing" },
		{ "detection", "blink", "Object Detection" },
		{ "sleepiness", "yawn", "Sleepy" },
		{ "mental acuity", "smarter", "Strange Mental Acuity" },
		{ "ettin strength", "stronger", "Strength of Ten Ettins" },
		{ "teleportitis", "disappearing", "Teleportitis" },
	};
	
	public static final String[][] stoneSphereStrings =
	{
		// name, combat use mssage
		{ "fire", "bright red light" },
		{ "lightning", "bright yellow light" },
		{ "water", "bright blue light" },
		{ "nature", "bright green light" },
	};
	
	public static final boolean eliminationProcessor( final String[][] strings, final int index,
		final int id, final int minId, final int maxId, final String baseName )
	{
		String effect = strings[index][0];
		Preferences.setString( baseName + id, effect );
		String name = ItemDatabase.getItemName( id );
		String testName = name + " of " + effect;
		String testPlural = name + "s of " + effect;
		ItemDatabase.registerItemAlias( id, testName, testPlural );
		
		HashSet possibilities = new HashSet();
		for ( int i = 0; i < strings.length; ++i )
		{
			possibilities.add(strings[i][0]);
		}

		int missing = 0;
		for ( int i = minId; i <= maxId; ++i ) 
		{
			effect = Preferences.getString( baseName + i );
			if ( effect.equals( "" ) )
			{
				if ( missing != 0 )
				{
					// more than one missing item in set
					return false;
				}
				missing = i;		
			}
			else
			{
				possibilities.remove( effect );
			}
		}
	
		if ( missing == 0 )
		{
			// no missing items
			return false;
		}

		if ( possibilities.size() != 1 )
		{
			// something's screwed up if this happens
			return false;
		}

		effect = (String) possibilities.iterator().next();
		Preferences.setString( baseName + missing, effect );
		name = ItemDatabase.getItemName( missing );
		testName = name + " of " + effect;
		testPlural = name + "s of " + effect;
		ItemDatabase.registerItemAlias( missing, testName, testPlural );
		return true;	
	}
	
	// Suggest one or two items from a permutation group that need to be identified.
	// Strategy: identify the items the player has the most of first,
	// to maximize the usefulness of having identified them.
	// If two items remain unidentified, only identify one, since
	// eliminationProcessor will handle the other.
	
	public static final void suggestIdentify( final List items,
		final int minId, final int maxId, final String baseName )
	{
		ArrayList possible = new ArrayList();
		int count;
		int unknown = 0;
		for ( int i = minId; i <= maxId; ++i ) 
		{
			if ( !Preferences.getString( baseName + i ).equals( "" ) )
			{
				continue;	// already identified;
			}
			++unknown;
			AdventureResult item = new AdventureResult( i, 1 );
			count = item.getCount( KoLConstants.inventory );
			if ( count <= 0 )
			{
				continue;	// can't identify yet
			}
			possible.add( new Integer( i | Math.min( count, 127 ) << 24 ) );
		}
		count = possible.size();
		if ( count == 0 )
		{
			return;
		}
		Collections.sort( possible, Collections.reverseOrder() );
		count = possible.size();
		if ( unknown == 2 && count == 2 )
		{
			possible.remove( --count );
		}
		items.add( String.valueOf( ((Integer) possible.get( 0 )).intValue()
			& 0x00FFFFFF ) );
		if ( count > 1 )
		{
			items.add( String.valueOf( ((Integer) possible.get( 1 )).intValue()
				& 0x00FFFFFF ) );
		}
	}
}
