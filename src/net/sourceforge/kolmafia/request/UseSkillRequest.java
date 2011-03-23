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

package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.HPRestoreItemList;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.RecoveryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class UseSkillRequest
	extends GenericRequest
	implements Comparable
{
	private static final HashMap ALL_SKILLS = new HashMap();
	private static final Pattern SKILLID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern BOOKID_PATTERN = Pattern.compile( "preaction=summon([^&]*)" );

	private static final Pattern COUNT1_PATTERN = Pattern.compile( "bufftimes=([\\*\\d,]+)" );
	private static final Pattern COUNT2_PATTERN = Pattern.compile( "quantity=([\\*\\d,]+)" );

	public static final String[] BREAKFAST_SKILLS =
	{
		"Advanced Cocktailcrafting",
		"Advanced Saucecrafting",
		"Pastamastery",
		"Summon Crimbo Candy",
		"Lunch Break",
	};

	public static final String[] TOME_SKILLS =
	{
		"Summon Snowcones",
		"Summon Stickers",
		"Summon Sugar Sheets",
	};

	public static final String[] LIBRAM_SKILLS =
	{
		"Summon Candy Hearts",
		"Summon Party Favor",
		"Summon Love Song",
		"Summon BRICKOs",
	};

	public static final String[] GRIMOIRE_SKILLS =
	{
		"Summon Hilarious Objects",
		"Summon Tasteful Items",
		"Summon Alice's Army Cards",
	};

	private static final int OTTER_TONGUE = 1007;
	private static final int WALRUS_TONGUE = 1010;
	private static final int BANDAGES = 3009;
	private static final int COCOON = 3012;
	private static final int DISCO_NAP = 5007;
	private static final int POWER_NAP = 5011;
	private static final int ODE_TO_BOOZE = 6014;

	public static String lastUpdate = "";
	private static int lastSkillUsed = -1;
	private static int lastSkillCount = 0;

	private final int skillId;
	private final String skillName;
	private String target;
	private int buffCount;
	private String countFieldId;

	private int lastReduction = Integer.MAX_VALUE;
	private String lastStringForm = "";

	public static final AdventureResult[] TAMER_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 1 ),
		ItemPool.get( ItemPool.CHELONIAN_MORNINGSTAR, 1 ),
		ItemPool.get( ItemPool.MACE_OF_THE_TORTOISE, 1 ),
		ItemPool.get( ItemPool.TURTLE_TOTEM, 1 )
	};
	public static final int[] TAMER_WEAPONS_BONUS = new int[] { 15, 10, 5, 0 };

	public static final AdventureResult[] SAUCE_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 1 ),
		ItemPool.get( ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 1 ),
		ItemPool.get( ItemPool.FIVE_ALARM_SAUCEPAN, 1 ),
		ItemPool.get( ItemPool.SAUCEPAN, 1 )
	};
	public static final int[] SAUCE_WEAPONS_BONUS = new int[] { 15, 10, 5, 0 };

	public static final AdventureResult[] THIEF_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.TRICKSTER_TRIKITIXA, 1 ),
		ItemPool.get( ItemPool.SQUEEZEBOX_OF_THE_AGES, 1 ),
		ItemPool.get( ItemPool.ROCK_N_ROLL_LEGEND, 1 ),
		ItemPool.get( ItemPool.CALAVERA_CONCERTINA, 1 ),
		ItemPool.get( ItemPool.STOLEN_ACCORDION, 1 )
	};
	public static final int[] THIEF_WEAPONS_BONUS = new int[] { 15, 10, 5, 2, 0 };

	public static final AdventureResult PLEXI_PENDANT = ItemPool.get( ItemPool.PLEXIGLASS_PENDANT, 1 );
	public static final AdventureResult BRIM_BERET = ItemPool.get( ItemPool.BRIMSTONE_BERET, 1 );
	public static final AdventureResult WIZARD_HAT = ItemPool.get( ItemPool.JEWEL_EYED_WIZARD_HAT, 1 );

	public static final AdventureResult PLEXI_WATCH = ItemPool.get( ItemPool.PLEXIGLASS_POCKETWATCH, 1 );
	public static final AdventureResult BRIM_BRACELET = ItemPool.get( ItemPool.BRIMSTONE_BRACELET, 1 );
	public static final AdventureResult SOLITAIRE = ItemPool.get( ItemPool.STAINLESS_STEEL_SOLITAIRE, 1 );

	public static final AdventureResult NAVEL_RING = ItemPool.get( ItemPool.NAVEL_RING, 1 );
	public static final AdventureResult WIRE_BRACELET = ItemPool.get( ItemPool.WOVEN_BALING_WIRE_BRACELETS, 1 );
	public static final AdventureResult BACON_BRACELET = ItemPool.get( ItemPool.BACONSTONE_BRACELET, 1 );
	public static final AdventureResult BACON_EARRING = ItemPool.get( ItemPool.BACONSTONE_EARRING, 1 );
	public static final AdventureResult SOLID_EARRING = ItemPool.get( ItemPool.SOLID_BACONSTONE_EARRING, 1 );
	public static final AdventureResult EMBLEM_AKGYXOTH = ItemPool.get( ItemPool.EMBLEM_AKGYXOTH, 1 );

	public static final AdventureResult SAUCEBLOB_BELT = ItemPool.get( ItemPool.SAUCEBLOB_BELT, 1 );
	public static final AdventureResult JUJU_MOJO_MASK = ItemPool.get( ItemPool.JUJU_MOJO_MASK, 1 );

	// The following list must contain only accessories!
	private static final AdventureResult[] AVOID_REMOVAL = new AdventureResult[]
	{
		UseSkillRequest.PLEXI_WATCH,	// -3
		UseSkillRequest.BRIM_BRACELET,	// -3
		UseSkillRequest.SOLITAIRE,		// -2
		UseSkillRequest.NAVEL_RING,		// -1
		UseSkillRequest.WIRE_BRACELET,	// -1
		UseSkillRequest.BACON_BRACELET,	// -1, discontinued item
		UseSkillRequest.BACON_EARRING,	// -1
		UseSkillRequest.SOLID_EARRING,	// -1
		UseSkillRequest.EMBLEM_AKGYXOTH,	// -1
		// Removing the following might drop an AT song
		UseSkillRequest.PLEXI_PENDANT,
		// Removing the following may lose a buff
		UseSkillRequest.JUJU_MOJO_MASK,
	};

	// The number of items at the end of AVOID_REMOVAL that are simply
	// there to avoid removal - there's no point in equipping them
	// temporarily during casting:

	private static final int AVOID_REMOVAL_ONLY = 2;
	
	// Other known MP cost/song count items:
	//
	// wizard hat (-1) - has to be handled specially since it's not an accessory.
	// Vile Vagrant Vestments (-5) - unlikely to be equippable during Ronin.
	// Idol of Ak'gyxoth (-1) - off-hand, would require special handling.
	// Scandalously Skimpy Bikini (4 songs) - custom accessory.
	// Sombrero de Vida (4 songs) - custom hat.

	private UseSkillRequest( final String skillName )
	{
		super( UseSkillRequest.chooseURL( skillName ) );

		this.skillId = SkillDatabase.getSkillId( skillName );
		if ( this.skillId == -1 )
		{
			RequestLogger.printLine( "Unrecognized skill: " + skillName );
			this.skillName = skillName;
		}
		else
		{
			this.skillName = SkillDatabase.getSkillName( this.skillId );
		}
		this.target = "yourself";

		this.addFormFields();
	}

	private static String chooseURL( final String skillName )
	{
		switch ( SkillDatabase.getSkillId( skillName ) )
		{
		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:
		case SkillDatabase.HILARIOUS:
		case SkillDatabase.TASTEFUL:
		case SkillDatabase.CARDS:
		case SkillDatabase.CANDY_HEART:
		case SkillDatabase.PARTY_FAVOR:
		case SkillDatabase.LOVE_SONG:
		case SkillDatabase.BRICKOS:
			return "campground.php";
		}

		return "skills.php";
	}

	private void addFormFields()
	{
		switch ( this.skillId )
		{
		case SkillDatabase.SNOWCONE:
			this.addFormField( "preaction", "summonsnowcone" );
			break;

		case SkillDatabase.STICKER:
			this.addFormField( "preaction", "summonstickers" );
			break;

		case SkillDatabase.SUGAR:
			this.addFormField( "preaction", "summonsugarsheets" );
			break;

		case SkillDatabase.HILARIOUS:
			this.addFormField( "preaction", "summonhilariousitems" );
			break;

		case SkillDatabase.TASTEFUL:
			this.addFormField( "preaction", "summonspencersitems" );
			break;

		case SkillDatabase.CARDS:
			this.addFormField( "preaction", "summonaa" );
			break;

		case SkillDatabase.CANDY_HEART:
			this.addFormField( "preaction", "summoncandyheart" );
			break;

		case SkillDatabase.PARTY_FAVOR:
			this.addFormField( "preaction", "summonpartyfavor" );
			break;

		case SkillDatabase.LOVE_SONG:
			this.addFormField( "preaction", "summonlovesongs" );
			break;

		case SkillDatabase.BRICKOS:
			this.addFormField( "preaction", "summonbrickos" );
			break;

		default:
			this.addFormField( "action", "Skillz." );
			this.addFormField( "whichskill", String.valueOf( this.skillId ) );
			break;
		}
	}

	public void setTarget( final String target )
	{
		if ( SkillDatabase.isBuff( this.skillId ) )
		{
			this.countFieldId = "bufftimes";

			if ( target == null || target.trim().length() == 0 || target.equals( KoLCharacter.getPlayerId() ) || target.equals( KoLCharacter.getUserName() ) )
			{
				this.target = null;
				this.addFormField( "specificplayer", KoLCharacter.getPlayerId() );
			}
			else
			{
				this.target = ContactManager.getPlayerName( target );
				this.addFormField( "specificplayer", ContactManager.getPlayerId( target ) );
			}
		}
		else
		{
			this.countFieldId = "quantity";
			this.target = null;
		}
	}

	public void setBuffCount( int buffCount )
	{
		int mpCost = SkillDatabase.getMPConsumptionById( this.skillId );
		if ( mpCost == 0 )
		{
			this.buffCount = 0;
			return;
		}

		int maxPossible = 0;
		int availableMP = KoLCharacter.getCurrentMP();

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			maxPossible = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			maxPossible = Math.min( this.getMaximumCast(), availableMP / mpCost );
		}

		if ( buffCount < 1 )
		{
			buffCount += maxPossible;
		}
		else if ( buffCount == Integer.MAX_VALUE )
		{
			buffCount = maxPossible;
		}

		this.buffCount = buffCount;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof UseSkillRequest ) )
		{
			return -1;
		}

		int mpDifference =
			SkillDatabase.getMPConsumptionById( this.skillId ) - SkillDatabase.getMPConsumptionById( ( (UseSkillRequest) o ).skillId );

		return mpDifference != 0 ? mpDifference : this.skillName.compareToIgnoreCase( ( (UseSkillRequest) o ).skillName );
	}

	public int getSkillId()
	{
		return this.skillId;
	}

	public String getSkillName()
	{
		return this.skillName;
	}

	public int getMaximumCast()
	{
		int maximumCast = Integer.MAX_VALUE;

		switch ( this.skillId )
		{
		
		// Vent Rage Gland can be used once per day
		case SkillDatabase.RAGE_GLAND:
			maximumCast = Preferences.getBoolean( "rageGlandVented" ) ? 0 : 1;
			break;

		// Tomes can be used three times per day

		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:

			maximumCast = Math.max( 3 - Preferences.getInteger( "tomeSummons" ), 0 );
			break;

		// Grimoire items can only be summoned once per day.
		case SkillDatabase.HILARIOUS:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire1Summons" ), 0 );
			break;

		case SkillDatabase.TASTEFUL:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire2Summons" ), 0 );
			break;

		case SkillDatabase.CARDS:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire3Summons" ), 0 );
			break;

		// You can summon Crimbo candy once a day
		case SkillDatabase.CRIMBO_CANDY:

			maximumCast = Math.max( 1 - Preferences.getInteger( "_candySummons" ), 0 );
			break;

		// Rainbow Gravitation can be cast 3 times per day.  Each
		// casting consumes five elemental wads and a twinkly wad

		case SkillDatabase.RAINBOW:
			maximumCast = Math.max( 3 - Preferences.getInteger( "prismaticSummons" ), 0 );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.COLD_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.HOT_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SLEAZE_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SPOOKY_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.STENCH_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.TWINKLY_WAD ), maximumCast );
			break;

		// You can take a Lunch Break once a day
		case SkillDatabase.LUNCH_BREAK:

			maximumCast = Preferences.getBoolean( "_lunchBreak" ) ? 0 : 1;
			break;

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery

		case 3006:

			maximumCast = KoLCharacter.hasSkill( "Transcendental Noodlecraft" ) ? 5 : 3;
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "noodleSummons" ), 0 );
			break;

		// The Way of Sauce affects # of summons for Advanced
		// Saucecrafting. So does the Gravyskin Belt of the Sauceblob

		case 4006:

			maximumCast = KoLCharacter.hasSkill( "The Way of Sauce" ) ? 5 : 3;
			if ( KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) &&
			     ( KoLCharacter.hasEquipped( UseSkillRequest.SAUCEBLOB_BELT ) ||
			       UseSkillRequest.SAUCEBLOB_BELT.getCount( KoLConstants.inventory ) > 0 ) )
			{
				maximumCast += 3;
			}
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "reagentSummons" ), 0 );
			break;

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting

		case 5014:

			maximumCast = KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) ? 5 : 3;
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "cocktailSummons" ), 0 );
			break;

		}

		return maximumCast;
	}

	public String toString()
	{
		if ( this.lastReduction == KoLCharacter.getManaCostAdjustment() && !SkillDatabase.isLibramSkill( this.skillId ) )
		{
			return this.lastStringForm;
		}

		this.lastReduction = KoLCharacter.getManaCostAdjustment();
		this.lastStringForm = this.skillName + " (" + SkillDatabase.getMPConsumptionById( this.skillId ) + " mp)";
		return this.lastStringForm;
	}

	private static final boolean canSwitchToItem( final AdventureResult item )
	{
		return !KoLCharacter.hasEquipped( item ) &&
			EquipmentManager.canEquip( item.getName() ) &&
			InventoryManager.hasItem( item, false );
	}

	public static final void optimizeEquipment( final int skillId )
	{
		boolean isBuff = SkillDatabase.isBuff( skillId );

		if ( isBuff )
		{
			if ( skillId > 2000 && skillId < 3000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.TAMER_WEAPONS, skillId );
			}

			if ( skillId > 4000 && skillId < 5000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.SAUCE_WEAPONS, skillId );
			}

			if ( skillId > 6000 && skillId < 7000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.THIEF_WEAPONS, skillId );
			}
		}

		if ( Preferences.getBoolean( "switchEquipmentForBuffs" ) )
		{
			UseSkillRequest.reduceManaConsumption( skillId, isBuff );
		}
	}

	private static final boolean isValidSwitch( final int slotId )
	{
		AdventureResult item = EquipmentManager.getEquipment( slotId );
		if ( item.equals( EquipmentRequest.UNEQUIP ) ) return true;
		
		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i )
		{
			if ( item.equals( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				return false;
			}
		}
		
		Speculation spec = new Speculation();
		spec.equip( slotId, EquipmentRequest.UNEQUIP );
		int[] predictions = spec.calculate().predict();
		if ( KoLCharacter.getCurrentMP() > predictions[ Modifiers.BUFFED_MP ] )
		{
			return false;
		}
		if ( KoLCharacter.getCurrentHP() > predictions[ Modifiers.BUFFED_HP ] )
		{
			return false;
		}

		return true;
	}

	private static final int attemptSwitch( final int skillId, final AdventureResult item, final boolean slot1Allowed,
		final boolean slot2Allowed, final boolean slot3Allowed )
	{
		if ( slot3Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY3 ) ).run();
			return EquipmentManager.ACCESSORY3;
		}

		if ( slot2Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY2 ) ).run();
			return EquipmentManager.ACCESSORY2;
		}

		if ( slot1Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY1 ) ).run();
			return EquipmentManager.ACCESSORY1;
		}

		return -1;
	}

	private static final void reduceManaConsumption( final int skillId, final boolean isBuff )
	{
		// Never bother trying to reduce mana consumption when casting
		// ode to booze or a libram skill

		if ( skillId == UseSkillRequest.ODE_TO_BOOZE || SkillDatabase.isLibramSkill( skillId ) )
		{
			return;
		}

		if ( KoLCharacter.canInteract() )
		{
			return;
		}

		// Best switch is a PLEXI_WATCH, since it's a guaranteed -3 to
		// spell cost.

		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length - AVOID_REMOVAL_ONLY; ++i )
		{
			if ( SkillDatabase.getMPConsumptionById( skillId ) == 1 || 
				KoLCharacter.currentNumericModifier( Modifiers.MANA_COST ) <= -3 )
			{
				return;
			}

			if ( !UseSkillRequest.canSwitchToItem( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				continue;
			}

			// First determine which slots are available for switching in
			// MP reduction items.  This has do be done inside the loop now
			// that max HP/MP prediction is done, since two changes that are
			// individually harmless might add up to a loss of points.
	
			boolean slot1Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY1 );
			boolean slot2Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY2 );
			boolean slot3Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY3 );
	
			UseSkillRequest.attemptSwitch(
				skillId, UseSkillRequest.AVOID_REMOVAL[ i ], slot1Allowed, slot2Allowed, slot3Allowed );
		}
		
		if ( UseSkillRequest.canSwitchToItem( UseSkillRequest.WIZARD_HAT ) &&
			!KoLCharacter.hasEquipped( UseSkillRequest.BRIM_BERET ) &&
			UseSkillRequest.isValidSwitch( EquipmentManager.HAT ) )
		{
			( new EquipmentRequest( UseSkillRequest.WIZARD_HAT, EquipmentManager.HAT ) ).run();
		}
	}

	public static final int songLimit()
	{
		int rv = 3;
		if ( KoLCharacter.currentBooleanModifier( Modifiers.FOUR_SONGS ) )
		{
			++rv;
		}
		if ( KoLCharacter.currentBooleanModifier( Modifiers.ADDITIONAL_SONG ) )
		{
			++rv;
		}
		return rv;
	}

	public Object run()
	{
		if ( !KoLCharacter.hasSkill( this.skillName ) || this.buffCount == 0 )
		{
			return null;
		}

		UseSkillRequest.lastUpdate = "";
		UseSkillRequest.optimizeEquipment( this.skillId );

		if ( !KoLmafia.permitsContinue() )
		{
			return null;
		}

		this.setBuffCount( Math.min( this.buffCount, this.getMaximumCast() ) );
		this.useSkillLoop();
		return null;
	}

	private void useSkillLoop()
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		int castsRemaining = this.buffCount;

		int maximumMP = KoLCharacter.getMaximumMP();
		int mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
		int maximumCast = maximumMP / mpPerCast;
		
		// Save name so we can guarantee correct target later
		
		String originalTarget = this.target;

		while ( !KoLmafia.refusesContinue() && castsRemaining > 0 )
		{
			if ( SkillDatabase.isLibramSkill( this.skillId ) )
			{
				mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
			}

			if ( maximumMP < mpPerCast )
			{
				UseSkillRequest.lastUpdate = "Your maximum mana is too low to cast " + this.skillName + ".";
				KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
				return;
			}

			// Find out how many times we can cast with current MP

			int currentCast = this.availableCasts( castsRemaining, mpPerCast );

			// If none, attempt to recover MP in order to cast;
			// take auto-recovery into account.
			// Also recover MP if an opera mask is worn, to maximize its benefit.
			// (That applies only to AT buffs, but it's unlikely that an opera mask
			// will be worn at any other time than casting one.)
			boolean needExtra = currentCast < maximumCast && currentCast < castsRemaining &&
				EquipmentManager.getEquipment( EquipmentManager.HAT ).getItemId() == ItemPool.OPERA_MASK;

			if ( currentCast == 0 || needExtra )
			{
				currentCast = Math.min( castsRemaining, maximumCast );
				int currentMP = KoLCharacter.getCurrentMP();

				int recoverMP = mpPerCast * currentCast;

				SpecialOutfit.createImplicitCheckpoint();
				if ( MoodManager.isExecuting() )
				{
					recoverMP = Math.min( Math.max( recoverMP, MoodManager.getMaintenanceCost() ), maximumMP );
				}
				RecoveryManager.recoverMP( recoverMP  );
				SpecialOutfit.restoreImplicitCheckpoint();

				// If no change occurred, that means the person
				// was unable to recover MP; abort the process.

				if ( currentMP == KoLCharacter.getCurrentMP() )
				{
					UseSkillRequest.lastUpdate = "Could not restore enough mana to cast " + this.skillName + ".";
					KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
					return;
				}

				currentCast = this.availableCasts( castsRemaining, mpPerCast );
			}

			if ( KoLmafia.refusesContinue() )
			{
				UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
				return;
			}

			// If this happens to be a health-restorative skill,
			// then there is an effective cap based on how much
			// the skill is able to restore.

			switch ( this.skillId )
			{
			case OTTER_TONGUE:
			case WALRUS_TONGUE:
			case DISCO_NAP:
			case POWER_NAP:
			case BANDAGES:
			case COCOON:

				int healthRestored = HPRestoreItemList.getHealthRestored( this.skillName );
				int maxPossible = Math.max( 1, ( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() ) / healthRestored );
				castsRemaining = Math.min( castsRemaining, maxPossible );
				currentCast = Math.min( currentCast, castsRemaining );
				break;
			}

			currentCast = Math.min( currentCast, maximumCast );

			if ( currentCast > 0 )
			{
				// Attempt to cast the buff.

				this.buffCount = currentCast;
				UseSkillRequest.optimizeEquipment( this.skillId );

				if ( KoLmafia.refusesContinue() )
				{
					UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
					return;
				}

				this.setTarget( originalTarget );
				
				this.addFormField( this.countFieldId, String.valueOf( currentCast ), false );

				if ( this.target == null || this.target.trim().length() == 0 )
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " " + currentCast + " times..." );
				}
				else
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " on " + this.target + " " + currentCast + " times..." );
				}

				super.run();

				// Otherwise, you have completed the correct
				// number of casts.  Deduct it from the number
				// of casts remaining and continue.

				castsRemaining -= currentCast;
			}
		}

		if ( KoLmafia.refusesContinue() )
		{
			UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
		}
	}

	public final int availableCasts( int maxCasts, int mpPerCast )
	{
		int availableMP = KoLCharacter.getCurrentMP();
		int currentCast = 0;

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			currentCast = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			currentCast = availableMP / mpPerCast;
			currentCast = Math.min( this.getMaximumCast(), currentCast );
		}

		currentCast = Math.min( maxCasts, currentCast );

		return currentCast;
	}

	public static final boolean hasAccordion()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.THIEF_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.THIEF_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final boolean hasTotem()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.TAMER_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.TAMER_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final boolean hasSaucepan()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.SAUCE_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.SAUCE_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final void prepareWeapon( final AdventureResult[] options, int skillId )
	{
		if ( KoLCharacter.canInteract() )
		{
			// The first weapon is a quest item: the reward for
			// finally defeating your Nemesis
			if ( InventoryManager.hasItem( options[ 0 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 0 ] ) )
				{
					InventoryManager.retrieveItem( options[ 0 ] );
				}

				return;
			}

			// The second weapon is a quest item: the Legendary
			// Epic Weapon of the class
			if ( InventoryManager.hasItem( options[ 1 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 1 ] ) )
				{				
					InventoryManager.retrieveItem( options[ 1 ] );
				}

				return;
			}

			// The third weapon is tradeable: the Epic Weapon of
			// the class
			if ( InventoryManager.hasItem( options[ 2 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 2 ] ) )
				{
					InventoryManager.retrieveItem( options[ 2 ] );
				}

				return;
			}
			
			// Allow ode to continue through to use a weaker weapon
			
			if ( skillId != UseSkillRequest.ODE_TO_BOOZE )
			{
				// Otherwise, obtain the Epic Weapon
				InventoryManager.retrieveItem( options[ 2 ] );
				return;
			}
		}

		// Check for the weakest equipped item
		
		AdventureResult equippedItem = null;

		for ( int i = options.length - 1; i >= 0; --i )
		{
			if ( KoLCharacter.hasEquipped( options[ i ] ) )
			{
				equippedItem = options[ i ];
				break;
			}
		}
		
		// Check for the strongest available item
		
		for ( int i = 0; i < options.length; ++i )
		{
			if ( !InventoryManager.hasItem( options[ i ], false ) )
			{
				continue;
			}

			if ( equippedItem != null && options[ i ] != equippedItem )
			{
				( new EquipmentRequest( EquipmentRequest.UNEQUIP,
					EquipmentManager.WEAPON ) ).run();
			}

			if ( !KoLCharacter.hasEquipped( options[ i ] ) )
			{
				InventoryManager.retrieveItem( options[ i ] );
			}

			return;
		}
		
		// Nothing available, try to retrieve the weakest item
		
		InventoryManager.retrieveItem( options[ options.length - 1 ] );
	}

	protected boolean retryOnTimeout()
	{
		return false;
	}

	protected boolean processOnFailure()
	{
		return true;
	}

	public void processResults()
	{
		UseSkillRequest.lastUpdate = "";

		boolean shouldStop = UseSkillRequest.parseResponse( this.getURLString(), this.responseText );

		if ( !UseSkillRequest.lastUpdate.equals( "" ) )
		{
			int state = shouldStop ? KoLConstants.ABORT_STATE : KoLConstants.CONTINUE_STATE;
			KoLmafia.updateDisplay( state, UseSkillRequest.lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
			{
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, UseSkillRequest.lastUpdate );
			}

			return;
		}

		if ( this.target == null )
		{
			KoLmafia.updateDisplay( this.skillName + " was successfully cast." );
		}
		else
		{
			KoLmafia.updateDisplay( this.skillName + " was successfully cast on " + this.target + "." );
		}
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof UseSkillRequest && this.getSkillName().equals(
			( (UseSkillRequest) o ).getSkillName() );
	}

	public static final UseSkillRequest getInstance( final int skillId )
	{
		return UseSkillRequest.getInstance( SkillDatabase.getSkillName( skillId ) );
	}

	public static final UseSkillRequest getInstance( final String skillName, final int buffCount )
	{
		return UseSkillRequest.getInstance( skillName, KoLCharacter.getUserName(), buffCount );
	}

	public static final UseSkillRequest getInstance( final String skillName, final String target, final int buffCount )
	{
		UseSkillRequest instance = UseSkillRequest.getInstance( skillName );
		if ( instance == null )
		{
			return null;
		}

		instance.setTarget( target == null || target.equals( "" ) ? KoLCharacter.getUserName() : target );
		instance.setBuffCount( buffCount );
		return instance;
	}

	public static final UseSkillRequest getUnmodifiedInstance( String skillName )
	{
		if ( skillName == null || !SkillDatabase.contains( skillName ) )
		{
			return null;
		}

		skillName = StringUtilities.getCanonicalName( skillName );
		UseSkillRequest request = (UseSkillRequest) UseSkillRequest.ALL_SKILLS.get( skillName );
		if ( request == null )
		{
			request = new UseSkillRequest( skillName );
			UseSkillRequest.ALL_SKILLS.put( skillName, request );
		}

		return request;
	}

	public static final UseSkillRequest getInstance( String skillName )
	{
		if ( skillName == null || !SkillDatabase.contains( skillName ) )
		{
			return null;
		}

		skillName = StringUtilities.getCanonicalName( skillName );
		UseSkillRequest request = (UseSkillRequest) UseSkillRequest.ALL_SKILLS.get( skillName );
		if ( request == null )
		{
			request = new UseSkillRequest( skillName );
			UseSkillRequest.ALL_SKILLS.put( skillName, request );
		}

		request.setTarget( KoLCharacter.getUserName() );
		request.setBuffCount( 0 );
		return request;
	}

	public static final boolean parseResponse( final String urlString, final String responseText )
	{
		int skillId = UseSkillRequest.lastSkillUsed;
		int count = UseSkillRequest.lastSkillCount;

		if ( skillId == -1 )
		{
			return false;
		}

		UseSkillRequest.lastSkillUsed = -1;
		UseSkillRequest.lastSkillCount = 0;

		if ( responseText == null || responseText.trim().length() == 0 )
		{
			int initialMP = KoLCharacter.getCurrentMP();
			CharPaneRequest.getInstance().run();

			if ( initialMP == KoLCharacter.getCurrentMP() )
			{
				UseSkillRequest.lastUpdate = "Encountered lag problems.";
				return false;
			}

			return true;
		}

		if ( responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "That skill is unavailable.";
			return true;
		}

		boolean exceeded = false;
		if ( responseText.indexOf( "You can only conjure" ) != -1 ||
		     responseText.indexOf( "You can only scrounge up" ) != -1 ||
		     responseText.indexOf( "You can only summon" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Summon limit exceeded.";
			exceeded = true;
			// Must continue with parsing in this case, so that the
			// cast counter can be incremented, in the hopes of
			// eventually getting back in sync with the actual
			// number of casts.
		}

		if ( responseText.indexOf( "too many songs" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Selected target has the maximum number of AT buffs already.";	
			return false;
		}

		if ( responseText.indexOf( "casts left of the Smile of Mr. A" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "You cannot cast that many smiles.";
			return false;
		}

		if ( responseText.indexOf( "Invalid target player" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Selected target is not a valid target.";
			return true;
		}

		// You can't cast that spell on persons who are lower than
		// level 15, like <name>, who is level 13.
		if ( responseText.indexOf( "lower than level" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Selected target is too low level.";
			return false;
		}

		if ( responseText.indexOf( "busy fighting" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Selected target is busy fighting.";
			return false;
		}

		if ( responseText.indexOf( "receive buffs" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Selected target cannot receive buffs.";
			return false;
		}

		if ( responseText.indexOf( "You need" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "You need special equipment to cast that buff.";
			return true;
		}

		if ( responseText.indexOf( "You can't remember how to use that skill" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "That skill is currently unavailable.";
			return true;
		}

		if ( responseText.indexOf( "You can't cast this spell because you are not an Accordion Thief" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Only Accordion Thieves can use that skill.";
			return true;
		}

		// You think your stomach has had enough for one day.
		if ( responseText.indexOf( "enough for one day" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "You can only do that once a day.";	
			return false;
		}

		String skillName = SkillDatabase.getSkillName( skillId );

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			UseSkillRequest.lastUpdate = "Not enough mana to cast " + skillName + ".";
			CharPaneRequest.getInstance().run();
			return true;
		}
			
		// The skill was successfully cast. Deal with its effects.
		if ( responseText.indexOf( "tear the opera mask" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.OPERA_MASK,
				"Your opera mask shattered." );
		}

		int mpCost = SkillDatabase.getMPConsumptionById( skillId ) * count;
		if ( exceeded )
		{
			// We're out of sync with the actual number of times
			// this skill has been cast.  Adjust the counter by 1
			// at a time.
			count = 1;
			mpCost = 0;
		}

		switch ( skillId )
		{
		case UseSkillRequest.ODE_TO_BOOZE:
			ConcoctionDatabase.getUsables().sort();
			break;
			
		case UseSkillRequest.OTTER_TONGUE:
		case UseSkillRequest.WALRUS_TONGUE:
			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
			break;

		case UseSkillRequest.DISCO_NAP:
		case UseSkillRequest.POWER_NAP:
			KoLConstants.activeEffects.clear();
			break;

		case SkillDatabase.RAGE_GLAND:
			Preferences.setBoolean( "rageGlandVented", true );
			break;
			
		case SkillDatabase.RAINBOW:

			// Each cast of Rainbow Gravitation consumes five
			// elemental wads and a twinkly wad

			ResultProcessor.processResult( ItemPool.get( ItemPool.COLD_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.HOT_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SLEAZE_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SPOOKY_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.STENCH_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.TWINKLY_WAD, -count ) );

			Preferences.increment( "prismaticSummons", count );
			break;

		case SkillDatabase.LUNCH_BREAK:
			Preferences.setBoolean( "_lunchBreak", true );
			break;

		case 3006:
			Preferences.increment( "noodleSummons", count );
			break;

		case 3024:
			Preferences.increment( "carboLoading", 1 );
			break;

		case 4006:
			Preferences.increment( "reagentSummons", count );
			break;

		case 5014:
			Preferences.increment( "cocktailSummons", count );
			break;

		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:
			Preferences.increment( "tomeSummons", count );
			break;

		case SkillDatabase.HILARIOUS:
			Preferences.increment( "grimoire1Summons", 1 );
			break;

		case SkillDatabase.TASTEFUL:
			Preferences.increment( "grimoire2Summons", 1 );
			break;

		case SkillDatabase.CARDS:
			Preferences.increment( "grimoire3Summons", 1 );
			break;

		case SkillDatabase.CRIMBO_CANDY:
			Preferences.increment( "_candySummons", 1 );
			break;

		case SkillDatabase.CANDY_HEART:
		case SkillDatabase.PARTY_FAVOR:
		case SkillDatabase.LOVE_SONG:
		case SkillDatabase.BRICKOS:
			int cast = Preferences.getInteger( "libramSummons" );
			mpCost = SkillDatabase.libramSkillMPConsumption( cast + 1, count );
			Preferences.increment( "libramSummons", count );
			KoLConstants.summoningSkills.sort();
			KoLConstants.usableSkills.sort();
			break;
		}

		ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );

		return false;
	}

	private static int getSkillId( final String urlString )
	{
		Matcher skillMatcher = UseSkillRequest.SKILLID_PATTERN.matcher( urlString );
		if ( skillMatcher.find() )
		{
			return StringUtilities.parseInt( skillMatcher.group( 1 ) );
		}

		skillMatcher = UseSkillRequest.BOOKID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
		{
			return -1;
		}

		String action = skillMatcher.group( 1 );

		if ( action.equals( "snowcone" ) )
		{
			return SkillDatabase.SNOWCONE;
		}

		if ( action.equals( "stickers" ) )
		{
			return SkillDatabase.STICKER;
		}

		if ( action.equals( "sugarsheets" ) )
		{
			return SkillDatabase.SUGAR;
		}

		if ( action.equals( "hilariousitems" ) )
		{
			return SkillDatabase.HILARIOUS;
		}

		if ( action.equals( "spencersitems" ) )
		{
			return	SkillDatabase.TASTEFUL;
		}

		if ( action.equals( "aa" ) )
		{
			return	SkillDatabase.CARDS;
		}

		if ( action.equals( "candyheart" ) )
		{
			return SkillDatabase.CANDY_HEART;
		}

		if ( action.equals( "partyfavor" ) )
		{
			return SkillDatabase.PARTY_FAVOR;
		}

		if ( action.equals( "lovesongs" ) )
		{
			return SkillDatabase.LOVE_SONG;
		}

		if ( action.equals( "brickos" ) )
		{
			return SkillDatabase.BRICKOS;
		}

		return -1;
	}

	private static final int getCount( final String urlString, int skillId )
	{
		Matcher countMatcher = UseSkillRequest.COUNT1_PATTERN.matcher( urlString );

		if ( !countMatcher.find() )
		{
			countMatcher = UseSkillRequest.COUNT2_PATTERN.matcher( urlString );
			if ( !countMatcher.find() )
			{
				return 1;
			}
		}

		int availableMP = KoLCharacter.getCurrentMP();
		int maxcasts;
		if ( SkillDatabase.isLibramSkill( skillId ) )
		{
			maxcasts = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			int MP = SkillDatabase.getMPConsumptionById( skillId );
			maxcasts = MP == 0 ? 1 : availableMP / MP;
		}
		
		if ( countMatcher.group( 1 ).startsWith( "*" ) )
		{
			return maxcasts;
		}

		return Math.min( maxcasts, StringUtilities.parseInt( countMatcher.group( 1 ) ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) && !urlString.startsWith( "skills.php" ) )
		{
			return false;
		}

		int skillId = UseSkillRequest.getSkillId( urlString );
                // Quick skills has (select a skill) with ID = 999
		if ( skillId == -1 || skillId == 999 )
		{
			return false;
		}

		int count = UseSkillRequest.getCount( urlString, skillId );
		String skillName = SkillDatabase.getSkillName( skillId );

		UseSkillRequest.lastSkillUsed = skillId;
		UseSkillRequest.lastSkillCount = count;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "cast " + count + " " + skillName );

		return true;
	}
}
