/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;

public class MonsterData
	extends AdventureResult
{
	private Object health;
	private Object attack;
	private Object defense;
	private Object initiative;
	private Object experience;
	private final Element attackElement;
	private final Element defenseElement;
	private final int physicalResistance;
	private final int meat;
	private final Phylum phylum;
	private final int poison;
	private final boolean boss;
	private final String image;
	private final int beeCount;

	private final ArrayList<AdventureResult> items;
	private final ArrayList<Double> pocketRates;

	public MonsterData( final String name, final Object health,
			    final Object attack, final Object defense,
			    final Object initiative, final Object experience,
			    final Element attackElement, final Element defenseElement,
			    final int physicalResistance,
			    final int meat,
			    final Phylum phylum, final int poison, final boolean boss,
			    final String image )
	{
		super( AdventureResult.MONSTER_PRIORITY, name );

		this.health = health;
		this.attack = attack;
		this.defense = defense;
		this.initiative = initiative;
		this.experience = experience;
		this.attackElement = attackElement;
		this.defenseElement = defenseElement;
		this.physicalResistance = physicalResistance;
		this.meat = meat;
		this.phylum = phylum;
		this.poison = poison;
		this.boss = boss;
		this.image = image;

		int beeCount = 0;
		for ( int i = 0; i < name.length(); ++i )
		{
			char c = name.charAt( i );
			if ( c == 'b' || c == 'B' )
			{
				beeCount++;
			}
		}
		this.beeCount = beeCount;

		this.items = new ArrayList<AdventureResult>();
		this.pocketRates = new ArrayList<Double>();
	}

	private static int ML()
	{
		/* For brevity, and to handle the possible future need for
		   asking for speculative monster stats */
		return KoLCharacter.getMonsterLevelAdjustment();
	}

	private MonsterExpression compile( Object expr )
	{
		return MonsterExpression.getInstance( (String) expr, this.getName() );
	}
 
	private double getBeeosity()
	{
		return 1.0 + ( KoLCharacter.inBeecore() ? ( this.beeCount / 5.0 ) : 0.0 );
	}

	public int getHP()
	{
		if ( this.health == null )
		{
			return 0;
		}
		if ( this.health instanceof Integer )
		{
			int hp = ((Integer) this.health).intValue();
			if ( hp == 0 && ((Integer) this.attack).intValue() == 0 )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				hp += 150;
			}
			return (int) Math.floor( Math.max( 1, hp + ML() ) *
			       getBeeosity() );
		}
		if ( this.health instanceof String )
		{
			this.health = compile( this.health );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.health).eval() * getBeeosity() ) );
	}

	public int getRawHP()
	{
		if ( this.health == null )
		{
			return -1;
		}
		if ( this.health instanceof Integer )
		{
			return ((Integer) this.health).intValue();
		}
		if ( this.health instanceof String )
		{
			this.health = compile( this.health );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.health).eval() ) );
	}

	public int getAttack()
	{
		if ( this.attack == null )
		{
			return 0;
		}
		if ( this.attack instanceof Integer )
		{
			int attack = ((Integer) this.attack).intValue();
			if ( attack == 0 && ((Integer) this.health).intValue() == 0 )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				// The bonus attack from BIG cannot raise a monster's attack above 300
				attack = Math.min( attack + 150, Math.max( 300, attack ) );
			}
			return (int) Math.floor( Math.max( 1, attack + ML() ) *
			       getBeeosity() );
		}
		if ( this.attack instanceof String )
		{
			this.attack = compile( this.attack );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.attack).eval() * getBeeosity() ) );
	}

	public int getRawAttack()
	{
		if ( this.attack == null )
		{
			return -1;
		}
		if ( this.attack instanceof Integer )
		{
			return ((Integer) this.attack).intValue();
		}
		if ( this.attack instanceof String )
		{
			this.attack = compile( this.attack );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.attack).eval() ) );
	}

	public int getDefense()
	{
		double reduceMonsterDefense = KoLCharacter.currentNumericModifier( Modifiers.REDUCE_ENEMY_DEFENSE ) / 100;
		if ( this.defense == null )
		{
			return 0;
		}
		if ( this.defense instanceof Integer )
		{
			int defense = ((Integer) this.defense).intValue();
			if ( defense == 0 && ((Integer) this.health).intValue() == 0 )
			{
				// The monster is unknown, so do not apply modifiers
				return 0;
			}
			if ( KoLCharacter.inBigcore() )
			{
				// The bonus defense from BIG cannot raise a monster's defense above 300
				defense = Math.min( defense + 150, Math.max( 300, defense ) );
			}
			return (int) Math.floor( Math.max( 1, defense + ML() ) *
			       getBeeosity() * ( 1 - reduceMonsterDefense ) );
		}
		if ( this.defense instanceof String )
		{
			this.defense = compile( this.defense );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.defense).eval() * 
				getBeeosity() * ( 1 - reduceMonsterDefense ) ) );
	}

	public int getRawDefense()
	{
		if ( this.defense == null )
		{
			return -1;
		}
		if ( this.defense instanceof Integer )
		{
			return ((Integer) this.defense).intValue();
		}
		if ( this.defense instanceof String )
		{
			this.defense = compile( this.defense );
		}
		return Math.max( 1, (int) (((MonsterExpression) this.defense).eval() ) );
	}

	public int getRawInitiative()
	{
		if ( this.initiative == null )
		{
			return -1;
		}
		if ( this.initiative instanceof Integer )
		{
			return ((Integer) this.initiative).intValue();
		}
		if ( this.initiative instanceof String )
		{
			this.initiative = compile( this.initiative );
		}
		return (int) ((MonsterExpression) this.initiative).eval();
	}

	public int getInitiative()
	{
		return this.getInitiative( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public int getInitiative( final int monsterLevel )
	{
		int baseInit = this.getRawInitiative();
		if ( baseInit == -1 )
		{
			return -1;
		}
		else
		{
			return baseInit + initPenalty( monsterLevel );
		}
	}

	public int getJumpChance()
	{
		if ( this.initiative == null )
		{
			return -1;
		}
		return getJumpChance( (int) KoLCharacter.getInitiativeAdjustment() );
	}

	public int getJumpChance( final int initBonus )
	{
		int jumpChance = 100 - this.getInitiative() + initBonus + Math.max( 0, KoLCharacter.getBaseMainstat() - this.getAttack() );
		return jumpChance > 100 ? 100 : jumpChance < 0 ? 0 : jumpChance;
	}

	public int getJumpChance( final int initBonus, final int monsterLevel )
	{
		int jumpChance = 100 - this.getInitiative( monsterLevel) + initBonus + Math.max( 0, KoLCharacter.getBaseMainstat() - this.getAttack() );
		return jumpChance > 100 ? 100 : jumpChance < 0 ? 0 : jumpChance;
	}

	public static final int initPenalty( final int monsterLevel )
	{
		return monsterLevel <= 20 ? 0 :
			monsterLevel <= 40 ? ( monsterLevel - 20 ) :
			monsterLevel <= 60 ? ( 20 + 2 * ( monsterLevel - 40 ) ) :
			monsterLevel <= 80 ? ( 60 + 3 * ( monsterLevel - 60 ) ) :
			monsterLevel <= 100 ? ( 120 + 4 * ( monsterLevel - 80 ) ) :
			( 200 + 5 * ( monsterLevel - 100 ) );
	}

	public Element getAttackElement()
	{
		return this.attackElement;
	}

	public Element getDefenseElement()
	{
		return this.defenseElement;
	}

	public int getPhysicalResistance()
	{
		return this.physicalResistance;
	}

	public int getMinMeat()
	{
		int variation = (int) Math.max( 1, Math.floor( this.meat * 0.2 ) );
		return this.meat > 0 ? this.meat - variation : 0;
	}

	public int getMaxMeat()
	{
		int variation = (int) Math.max( 1, Math.floor( this.meat * 0.2 ) );
		return this.meat > 0 ? this.meat + variation : 0;
	}

	public Phylum getPhylum()
	{
		return this.phylum;
	}

	public int getPoison()
	{
		return this.poison;
	}

	public boolean isBoss()
	{
		return this.boss;
	}

	public String getImage()
	{
		return this.image == null ? "" : this.image;
	}

	public List<AdventureResult> getItems()
	{
		return this.items;
	}

	public List getPocketRates()
	{
		return this.pocketRates;
	}

	public boolean shouldSteal()
	{
		// If the player has an acceptable dodge rate or
		// then steal anything.

		if ( this.willUsuallyDodge( 0 ) )
		{
			return this.shouldSteal( this.items );
		}

		// Otherwise, only steal from monsters that drop
		// something on your conditions list.

		return this.shouldSteal( GoalManager.getGoals() );
	}

	private boolean shouldSteal( final List checklist )
	{
		double dropModifier = AreaCombatData.getDropRateModifier();

		for ( int i = 0; i < checklist.size(); ++i )
		{
			if ( this.shouldStealItem( (AdventureResult) checklist.get( i ), dropModifier ) )
			{
				return true;
			}
		}

		return false;
	}

	private boolean shouldStealItem( AdventureResult item, final double dropModifier )
	{
		if ( !item.isItem() )
		{
			return false;
		}

		int itemIndex = this.items.indexOf( item );

		// If the monster drops this item, then return true
		// when the drop rate is less than 100%.

		if ( itemIndex != -1 )
		{
			item = (AdventureResult) this.items.get( itemIndex );
			switch ( (char) item.getCount() & 0xFFFF )
			{
			case 'p':
				return true;
			case 'n':
			case 'c':
			case 'f':
			case 'b':
				return false;
			default:
				return (item.getCount() >> 16) * dropModifier < 100.0;
			}
		}

		// If the item does not drop, check to see if maybe
		// the monster drops one of its ingredients.

		AdventureResult[] subitems = ConcoctionDatabase.getStandardIngredients( item.getItemId() );
		if ( subitems.length < 2 )
		{
			return false;
		}

		for ( int i = 0; i < subitems.length; ++i )
		{
			if ( this.shouldStealItem( subitems[ i ], dropModifier ) )
			{
				return true;
			}
		}

		// The monster doesn't drop the item or any of its
		// ingredients.

		return false;
	}

	public void clearItems()
	{
		this.items.clear();
	}

	public void addItem( final AdventureResult item )
	{
		this.items.add( item );
	}

	public void doneWithItems()
	{
		this.items.trimToSize();

		// Calculate the probability that an item will be yoinked
		// based on the integral provided by Buttons on the HCO forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3396

		double probability = 0.0;
		double[] coefficients = new double[ this.items.size() ];

		for ( int i = 0; i < this.items.size(); ++i )
		{
			coefficients[ 0 ] = 1.0;
			for ( int j = 1; j < coefficients.length; ++j )
			{
				coefficients[ j ] = 0.0;
			}

			for ( int j = 0; j < this.items.size(); ++j )
			{
				AdventureResult item = (AdventureResult) this.items.get( j );
				probability = (item.getCount() >> 16) / 100.0;
				switch ( (char) item.getCount() & 0xFFFF )
				{
				case 'p':
					if ( probability == 0.0 )
					{	// assume some probability of a pickpocket-only item
						probability = 0.05;
					}
					break;
				case 'n':
				case 'c':
				case 'f':
				case 'b':
					probability = 0.0;
					break;
				}

				if ( i == j )
				{
					for ( int k = 0; k < coefficients.length; ++k )
					{
						coefficients[ k ] = coefficients[ k ] * probability;
					}
				}
				else
				{
					for ( int k = coefficients.length - 1; k >= 1; --k )
					{
						coefficients[ k ] = coefficients[ k ] - probability * coefficients[ k - 1 ];
					}
				}
			}

			probability = 0.0;

			for ( int j = 0; j < coefficients.length; ++j )
			{
				probability += coefficients[ j ] / ( j + 1 );
			}

			this.pocketRates.add( new Double( probability ) );
		}
	}

	public double getExperience()
	{
		if ( this.experience == null )
		{
			return ( this.getAttack() / this.getBeeosity() ) / 8.0 + ( ML() > 0 ? ML() : 0 ) / 24;
		}
		if ( this.experience instanceof Integer )
		{
			return ((Integer) this.experience).intValue() / 2.0;
		}
		if ( this.experience instanceof String )
		{
			this.experience = compile( this.experience );
		}
		return ((MonsterExpression) this.experience).eval() / 2.0 + ( ML() > 0 ? ML() : 0 ) / 24;
	}

	public boolean willUsuallyMiss()
	{
		return this.willUsuallyMiss( 0 );
	}

	public boolean willUsuallyDodge( final int offenseModifier )
	{
		int dodgeRate = KoLCharacter.getAdjustedMoxie() - ( this.getAttack() + offenseModifier ) - 6;
		return dodgeRate > 0;
	}

	public boolean willUsuallyMiss( final int defenseModifier )
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();

		return AreaCombatData.hitPercent( hitStat - defenseModifier, this.getDefense() ) <= 50.0;
	}
}
