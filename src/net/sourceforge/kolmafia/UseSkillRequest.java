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

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UseSkillRequest extends KoLRequest implements Comparable
{
	private static final TreeMap ALL_SKILLS = new TreeMap();
	private static final Pattern SKILLID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );

	private static final Pattern COUNT1_PATTERN = Pattern.compile( "bufftimes=([\\d,]+)" );
	private static final Pattern COUNT2_PATTERN = Pattern.compile( "quantity=([\\d,]+)" );

	public static String [] BREAKFAST_SKILLS =
		{ "Advanced Cocktailcrafting", "Pastamastery", "Advanced Saucecrafting", "Summon Snowcone", "Summon Hilarious Objects", "Summon Candy Hearts" };

	private static final int OTTER_TONGUE = 1007;
	private static final int WALRUS_TONGUE = 1010;

	public static String lastUpdate = "";

	private int skillId;
	private String skillName;
	private String target;
	private int buffCount;
	private String countFieldId;

	private int lastReduction = Integer.MAX_VALUE;
	private String lastStringForm = "";

	public static final AdventureResult [] TAMER_WEAPONS = new AdventureResult []
	{
		new AdventureResult( 2558, 1 ),	 // Chelonian Morningstar
		new AdventureResult( 60, 1 ),    // Mace of the Tortoise
		new AdventureResult( 4, 1 )      // turtle totem
	};

	public static final AdventureResult [] SAUCE_WEAPONS = new AdventureResult []
	{
		new AdventureResult( 2560, 1 ),  // 17-Alarm Saucepan
		new AdventureResult( 57, 1 ),    // 5-Alarm saucepan
		new AdventureResult( 7, 1 )      // saucepan
	};

	public static final AdventureResult [] THIEF_WEAPONS = new AdventureResult []
	{
		new AdventureResult( 2557, 1 ),  // Squeezebox of the Ages
		new AdventureResult( 50, 1 ),    // Rock 'n Roll Legend
		new AdventureResult( 2234, 1 ),  // calavera concertina
		new AdventureResult( 11, 1 )     // stolen accordion
	};

	public static final AdventureResult PENDANT = new AdventureResult( 1235, 1 );
	public static final AdventureResult WIZARD_HAT = new AdventureResult( 1653, 1 );
	public static final AdventureResult POCKETWATCH = new AdventureResult( 1232, 1 );
	public static final AdventureResult SOLITAIRE = new AdventureResult( 1226, 1 );
	public static final AdventureResult BRACELET = new AdventureResult( 717, 1 );
	public static final AdventureResult EARRING = new AdventureResult( 715, 1 );

	private UseSkillRequest( String skillName )
	{
		super( "skills.php" );

		this.addFormField( "action", "Skillz." );
		this.addFormField( "pwd" );

		this.skillId = ClassSkillsDatabase.getSkillId( skillName );
		this.skillName = ClassSkillsDatabase.getSkillName( this.skillId );

		this.addFormField( "whichskill", String.valueOf( this.skillId ) );
		this.target = "yourself";
	}

	public void setTarget( String target )
	{
		if ( ClassSkillsDatabase.isBuff( this.skillId ) )
		{
			this.countFieldId = "bufftimes";

			if ( target == null || target.trim().length() == 0 || target.equals( String.valueOf( KoLCharacter.getUserId() ) ) || target.equals( KoLCharacter.getUserName() ) )
			{
				this.target = "yourself";
				this.addFormField( "specificplayer", KoLCharacter.getPlayerId() );
			}
			else
			{
				this.target = KoLmafia.getPlayerName( target );
				this.addFormField( "specificplayer", KoLmafia.getPlayerId( target ) );
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
		int mpCost = ClassSkillsDatabase.getMPConsumptionById( this.skillId );
		if ( mpCost == 0 )
		{
			this.buffCount = 0;
			return;
		}

		int maxPossible = Math.min( getMaximumCast(), KoLCharacter.getCurrentMP() / mpCost );

		// Candy hearts need to be calculated in
		// a slightly different manner.

		if ( this.skillId == 18 )
		{
			int mpRemaining = KoLCharacter.getCurrentMP();
			int count = StaticEntity.getIntegerProperty( "candyHeartSummons" );

			while ( mpCost <= mpRemaining )
			{
				++count;
				mpRemaining -= mpCost;
				mpCost = Math.max( ((count + 1) * (count + 2)) / 2 + KoLCharacter.getManaCostAdjustment(), 1 );
			}

			maxPossible = count - StaticEntity.getIntegerProperty( "candyHeartSummons" );
		}

		if ( buffCount < 1 )
			buffCount += maxPossible;
		else if ( buffCount == Integer.MAX_VALUE )
			buffCount = maxPossible;

		this.buffCount = buffCount;
	}

	public int compareTo( Object o )
	{
		if ( o == null || !(o instanceof UseSkillRequest) )
			return -1;

		int mpDifference = ClassSkillsDatabase.getMPConsumptionById( this.skillId ) -
			ClassSkillsDatabase.getMPConsumptionById( ((UseSkillRequest)o).skillId );

		return mpDifference != 0 ? mpDifference : this.skillName.compareToIgnoreCase( ((UseSkillRequest)o).skillName );
	}

	public int getSkillId()
	{	return this.skillId;
	}

	public String getSkillName()
	{	return this.skillName;
	}

	public int getMaximumCast()
	{
		int maximumCast = Integer.MAX_VALUE;

		switch ( this.skillId )
		{

		// Snowcones and grimoire items can only be summoned
		// once per day.

		case 16:

			maximumCast = Math.max( 1 - StaticEntity.getIntegerProperty( "snowconeSummons" ), 0 );
			break;

		case 17:

			maximumCast = Math.max( 1 - StaticEntity.getIntegerProperty( "grimoireSummons" ), 0 );
			break;

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery

		case 3006:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "Transcendental Noodlecraft" ) )
				maximumCast = 5;

			maximumCast = Math.max( maximumCast - StaticEntity.getIntegerProperty( "noodleSummons" ), 0 );
			break;

		// The Way of Sauce affects # of summons for
		// Advanced Saucecrafting

		case 4006:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "The Way of Sauce" ) )
				maximumCast = 5;

			maximumCast = Math.max( maximumCast - StaticEntity.getIntegerProperty( "reagentSummons" ), 0 );
			break;

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting

		case 5014:

			maximumCast = 3;
			if ( KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) )
				maximumCast = 5;

			maximumCast = Math.max( maximumCast - StaticEntity.getIntegerProperty( "cocktailSummons" ), 0 );
			break;

		}

		return maximumCast;
	}

	public String toString()
	{
		if ( this.lastReduction == KoLCharacter.getManaCostAdjustment() && this.skillId != 18 )
			return this.lastStringForm;

		this.lastReduction = KoLCharacter.getManaCostAdjustment();
		this.lastStringForm = this.skillName + " (" + ClassSkillsDatabase.getMPConsumptionById( this.skillId ) + " mp)";
		return this.lastStringForm;
	}

	private static boolean canSwitchToItem( AdventureResult item )
	{	return !KoLCharacter.hasEquipped( item ) && EquipmentDatabase.canEquip( item.getName() ) && KoLCharacter.hasItem( item );
	}

	public static void optimizeEquipment( int skillId )
	{
		if ( skillId > 2000 && skillId < 3000 )
			prepareWeapon( TAMER_WEAPONS );

		if ( skillId > 4000 && skillId < 5000 )
			prepareWeapon( SAUCE_WEAPONS );

		if ( skillId > 6000 && skillId < 7000 )
			prepareWeapon( THIEF_WEAPONS );

		if ( StaticEntity.getBooleanProperty( "switchEquipmentForBuffs" ) )
			reduceManaConsumption( skillId );
	}

	private static boolean isValidSwitch( int slotId )
	{
		AdventureResult item = KoLCharacter.getEquipment( slotId );
		return !item.equals( PENDANT ) && !item.equals( POCKETWATCH ) && !item.equals( SOLITAIRE ) && !item.equals( BRACELET ) && !item.equals( EARRING );
	}

	private static int attemptSwitch( int skillId, AdventureResult item, boolean slot1Allowed, boolean slot2Allowed, boolean slot3Allowed )
	{
		if ( ClassSkillsDatabase.getMPConsumptionById( skillId ) == 1 || KoLCharacter.getManaCostAdjustment() == -3 )
			return -1;

		if ( !canSwitchToItem( item ) )
			return -1;

		if ( slot3Allowed )
		{
			(new EquipmentRequest( item, KoLCharacter.ACCESSORY3 )).run();
			return KoLCharacter.ACCESSORY3;
		}

		if ( slot2Allowed )
		{
			(new EquipmentRequest( item, KoLCharacter.ACCESSORY2 )).run();
			return KoLCharacter.ACCESSORY2;
		}

		if ( slot1Allowed )
		{
			(new EquipmentRequest( item, KoLCharacter.ACCESSORY1 )).run();
			return KoLCharacter.ACCESSORY1;
		}

		return -1;
	}

	private static void reduceManaConsumption( int skillId )
	{
		if ( skillId > 1000 && skillId != 6014 && ClassSkillsDatabase.isBuff( skillId ) && inventory.contains( WIZARD_HAT ) )
			(new EquipmentRequest( WIZARD_HAT, KoLCharacter.HAT )).run();

		// First determine which slots are available for switching in
		// MP reduction items.

		boolean slot1Allowed = isValidSwitch( KoLCharacter.ACCESSORY1 );
		boolean slot2Allowed = isValidSwitch( KoLCharacter.ACCESSORY2 );
		boolean slot3Allowed = isValidSwitch( KoLCharacter.ACCESSORY3 );

		// Best switch is a pocketwatch, since it's a guaranteed -3 to
		// spell cost.

		switch ( attemptSwitch( skillId, POCKETWATCH, slot1Allowed, slot2Allowed, slot3Allowed ) )
		{
		case KoLCharacter.ACCESSORY1:
			slot1Allowed = false;
			break;
		case KoLCharacter.ACCESSORY2:
			slot2Allowed = false;
			break;
		case KoLCharacter.ACCESSORY3:
			slot3Allowed = false;
			break;
		}

		// Next best switch is a solitaire, since it's a guaranteed -2 to
		// spell cost.

		switch ( attemptSwitch( skillId, SOLITAIRE, slot1Allowed, slot2Allowed, slot3Allowed ) )
		{
		case KoLCharacter.ACCESSORY1:
			slot1Allowed = false;
			break;
		case KoLCharacter.ACCESSORY2:
			slot2Allowed = false;
			break;
		case KoLCharacter.ACCESSORY3:
			slot3Allowed = false;
			break;
		}

		// Earrings and bracelets are both a -1 to spell cost, so consider
		// them in the last phase.

		switch ( attemptSwitch( skillId, EARRING, slot1Allowed, slot2Allowed, slot3Allowed ) )
		{
		case KoLCharacter.ACCESSORY1:
			slot1Allowed = false;
			break;
		case KoLCharacter.ACCESSORY2:
			slot2Allowed = false;
			break;
		case KoLCharacter.ACCESSORY3:
			slot3Allowed = false;
			break;
		}

		// No need for a switch statement here, because it's the last thing
		// being switched.

		attemptSwitch( skillId, BRACELET, slot1Allowed, slot2Allowed, slot3Allowed );
	}

	public void run()
	{
		if ( !KoLCharacter.hasSkill( this.skillName ) || this.buffCount == 0 )
			return;

		lastUpdate = "";

		// Cast the skill as many times as needed

		optimizeEquipment( this.skillId );

		if ( !KoLmafia.permitsContinue() )
			return;

		this.setBuffCount( Math.min( this.buffCount, this.getMaximumCast() ) );
		this.useSkillLoop();
	}

	private void useSkillLoop()
	{
		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		int castsRemaining = this.buffCount;
		int mpPerCast = ClassSkillsDatabase.getMPConsumptionById( this.skillId );

		int currentMP = KoLCharacter.getCurrentMP();
		int maximumMP = KoLCharacter.getMaximumMP();

		if ( KoLmafia.refusesContinue() )
			return;

		int currentCast = 0;
		int maximumCast = maximumMP / mpPerCast;

		while ( !KoLmafia.refusesContinue() && castsRemaining > 0 )
		{
			if ( this.skillId == 18 )
				mpPerCast = ClassSkillsDatabase.getMPConsumptionById( this.skillId );

			if ( maximumMP < mpPerCast )
			{
				lastUpdate = "Your maximum mana is too low to cast " + this.skillName + ".";
				KoLmafia.updateDisplay( lastUpdate );
				return;
			}

			// Find out how many times we can cast with current MP

			currentCast = Math.min( castsRemaining, KoLCharacter.getCurrentMP() / mpPerCast );

			if ( this.skillId == 18 )
				currentCast = Math.min( currentCast, 1 );

			// If none, attempt to recover MP in order to cast;
			// take auto-recovery into account.

			if ( currentCast == 0 )
			{
				currentCast = Math.min( castsRemaining, maximumCast );

				currentMP = KoLCharacter.getCurrentMP();

				if ( MoodSettings.isExecuting() )
				{
					StaticEntity.getClient().recoverMP(
						Math.min( Math.max( mpPerCast * currentCast, MoodSettings.getMaintenanceCost() ), maximumMP ) );
				}
				else
				{
					StaticEntity.getClient().recoverMP( mpPerCast * currentCast );
				}

				// If no change occurred, that means the person was
				// unable to recover MP; abort the process.

				if ( currentMP == KoLCharacter.getCurrentMP() )
				{
					lastUpdate = "Could not restore enough mana to cast " + this.skillName + ".";
					KoLmafia.updateDisplay( lastUpdate );
					return;
				}

				currentCast = Math.min( castsRemaining, KoLCharacter.getCurrentMP() / mpPerCast );
			}

			if ( KoLmafia.refusesContinue() )
			{
				lastUpdate = "Error encountered during cast attempt.";
				return;
			}

			currentCast = Math.min( currentCast, maximumCast );

			if ( currentCast > 0 )
			{
				// Attempt to cast the buff.  In the event that it
				// fails, make sure to report it and return whether
				// or not at least one cast was completed.

				this.buffCount = currentCast;
				optimizeEquipment( this.skillId );

				this.addFormField( this.countFieldId, String.valueOf( currentCast ), false );

				if ( this.target == null || this.target.trim().length() == 0 )
					KoLmafia.updateDisplay( "Casting " + this.skillName + " " + currentCast + " times..." );
				else
					KoLmafia.updateDisplay( "Casting " + this.skillName + " on " + this.target + " " + currentCast + " times..." );

				super.run();

				// Otherwise, you have completed the correct number
				// of casts.  Deduct it from the number of casts
				// remaining and continue.

				castsRemaining -= currentCast;
			}
		}

		if ( KoLmafia.refusesContinue() )
			lastUpdate = "Error encountered during cast attempt.";
	}

	public static void prepareWeapon( AdventureResult [] options )
	{
		for ( int i = 0; i < options.length; ++i )
		{
			if ( !KoLCharacter.hasItem( options[i], true ) )
				continue;

			if ( KoLCharacter.hasEquipped( options[i] ) )
				return;

			AdventureDatabase.retrieveItem( options[i] );
			return;
		}
	}

	protected boolean retryOnTimeout()
	{	return false;
	}

	protected boolean processOnFailure()
	{	return true;
	}

	public void processResults()
	{
		boolean shouldStop = false;
		lastUpdate = "";

		// If a reply was obtained, check to see if it was a success message
		// Otherwise, try to figure out why it was unsuccessful.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
		{
			int initialMP = KoLCharacter.getCurrentMP();
			CharpaneRequest.getInstance().run();

			if ( initialMP == KoLCharacter.getCurrentMP() )
			{
				shouldStop = false;
				lastUpdate = "Encountered lag problems.";
			}
		}
		else if ( this.responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			shouldStop = true;
			lastUpdate = "That skill is unavailable.";
		}
		else if ( this.responseText.indexOf( "You don't have enough" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "Not enough mana to cast " + this.skillName + ".";
		}
		else if ( this.responseText.indexOf( "You can only conjure" ) != -1 || this.responseText.indexOf( "You can only scrounge up" ) != -1 || this.responseText.indexOf( "You can only summon" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "Summon limit exceeded.";
		}
		else if ( this.responseText.indexOf( "too many songs" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "Selected target has 3 AT buffs already.";
		}
		else if ( this.responseText.indexOf( "casts left of the Smile of Mr. A" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "You cannot cast that many smiles.";
		}
		else if ( this.responseText.indexOf( "Invalid target player" ) != -1 )
		{
			shouldStop = true;
			lastUpdate = "Selected target is not a valid target.";
		}
		else if ( this.responseText.indexOf( "busy fighting" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "Selected target is busy fighting.";
		}
		else if ( this.responseText.indexOf( "receive buffs" ) != -1 )
		{
			shouldStop = false;
			lastUpdate = "Selected target cannot receive buffs.";
		}
		else if ( this.responseText.indexOf( "You need" ) != -1 )
		{
			shouldStop = true;
			lastUpdate = "You need special equipment to cast that buff.";
		}

		// Now that all the checks are complete, proceed
		// to determine how to update the user display.

		if ( !lastUpdate.equals( "" ) )
		{
			KoLmafia.updateDisplay( shouldStop ? ABORT_STATE : CONTINUE_STATE, lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, lastUpdate );
		}
		else
		{
			if ( this.target == null )
				KoLmafia.updateDisplay( this.skillName + " was successfully cast." );
			else
				KoLmafia.updateDisplay( this.skillName + " was successfully cast on " + this.target + "." );

			// Tongue of the Walrus (1010) automatically
			// removes any beaten up.

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MP, 0 - (ClassSkillsDatabase.getMPConsumptionById( this.skillId ) * this.buffCount) ) );
			KoLmafia.applyEffects();

			if ( this.skillId == OTTER_TONGUE || this.skillId == WALRUS_TONGUE )
			{
				activeEffects.remove( KoLAdventure.BEATEN_UP );
				this.needsRefresh = true;
			}
		}
	}

	public boolean equals( Object o )
	{	return o != null && o instanceof UseSkillRequest && this.getSkillName().equals( ((UseSkillRequest)o).getSkillName() );
	}

	public static UseSkillRequest getInstance( int skillId )
	{	return getInstance( ClassSkillsDatabase.getSkillName( skillId ) );
	}

	public static UseSkillRequest getInstance( String skillName, int buffCount )
	{	return getInstance( skillName, KoLCharacter.getUserName(), buffCount );
	}

	public static UseSkillRequest getInstance( String skillName, String target, int buffCount )
	{
		UseSkillRequest instance = getInstance( skillName );
		if ( instance == null )
			return null;

		instance.setTarget( target == null || target.equals( "" ) ? KoLCharacter.getUserName() : target );
		instance.setBuffCount( buffCount );
		return instance;
	}

	public static UseSkillRequest getInstance( String skillName )
	{
		if ( skillName == null || !ClassSkillsDatabase.contains( skillName ) )
			return null;

		skillName = KoLDatabase.getCanonicalName( skillName );
		if ( !ALL_SKILLS.containsKey( skillName ) )
			ALL_SKILLS.put( skillName, new UseSkillRequest( skillName ) );

		UseSkillRequest request = (UseSkillRequest) ALL_SKILLS.get( skillName );
		request.setTarget( KoLCharacter.getUserName() );
		request.setBuffCount( 0 );
		return request;
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "skills.php" ) )
			return false;

		Matcher skillMatcher = SKILLID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
			return true;

		int skillId = StaticEntity.parseInt( skillMatcher.group(1) );
		String skillName = ClassSkillsDatabase.getSkillName( skillId );

		int count = 1;
		Matcher countMatcher = COUNT1_PATTERN.matcher( urlString );

		if ( countMatcher.find() )
		{
			count = StaticEntity.parseInt( countMatcher.group(1) );
		}
		else
		{
			countMatcher = COUNT2_PATTERN.matcher( urlString );
			if ( countMatcher.find() )
				count = StaticEntity.parseInt( countMatcher.group(1) );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "cast " + count + " " + skillName );

		switch ( skillId )
		{
		case 16:
			StaticEntity.setProperty( "snowconeSummons", String.valueOf( StaticEntity.getIntegerProperty( "snowconeSummons" ) + 1 ) );
			break;

		case 17:
			StaticEntity.setProperty( "grimoireSummons", String.valueOf( StaticEntity.getIntegerProperty( "grimoireSummons" ) + 1 ) );
			break;

		case 18:
			if ( ClassSkillsDatabase.getMPConsumptionById( 18 ) <= KoLCharacter.getCurrentMP() )
				StaticEntity.setProperty( "candyHeartSummons", String.valueOf( StaticEntity.getIntegerProperty( "candyHeartSummons" ) + 1 ) );

			usableSkills.sort();
			break;

		case 3006:
			StaticEntity.setProperty( "noodleSummons", String.valueOf( StaticEntity.getIntegerProperty( "noodleSummons" ) + count ) );
			break;

		case 4006:
			StaticEntity.setProperty( "reagentSummons", String.valueOf( StaticEntity.getIntegerProperty( "reagentSummons" ) + count ) );
			break;

		case 5014:
			StaticEntity.setProperty( "cocktailSummons", String.valueOf( StaticEntity.getIntegerProperty( "cocktailSummons" ) + count ) );
			break;
		}

		return true;
	}
}
