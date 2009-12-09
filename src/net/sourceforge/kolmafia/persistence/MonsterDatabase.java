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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterDatabase
{
	private static final Map MONSTER_DATA = new TreeMap();
	private static String[] MONSTER_STRINGS = null;

	// Elements
	public static final int NONE = 0;
	public static final int COLD = 1;
	public static final int HEAT = 2;
	public static final int SLEAZE = 3;
	public static final int SPOOKY = 4;
	public static final int STENCH = 5;
	public static final int SLIME = 6;

	public static final String[] elementNames = { "none", "cold", "hot", "sleaze", "spooky", "stench", "slime" };

	public static final int elementNumber( final String name )
	{
		for ( int i = 0; i < MonsterDatabase.elementNames.length; ++i )
		{
			if ( name.equals( MonsterDatabase.elementNames[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final boolean elementalVulnerability( final int element1, final int element2 )
	{
		switch ( element1 )
		{
		case COLD:
			return element2 == MonsterDatabase.HEAT || element2 == MonsterDatabase.SPOOKY;
		case HEAT:
			return element2 == MonsterDatabase.SLEAZE || element2 == MonsterDatabase.STENCH;
		case SLEAZE:
			return element2 == MonsterDatabase.COLD || element2 == MonsterDatabase.SPOOKY;
		case SPOOKY:
			return element2 == MonsterDatabase.HEAT || element2 == MonsterDatabase.STENCH;
		case STENCH:
			return element2 == MonsterDatabase.SLEAZE || element2 == MonsterDatabase.COLD;
		}
		return false;
	}

	static
	{
		MonsterDatabase.refreshMonsterTable();
	}

	public static final void refreshMonsterTable()
	{
		MonsterDatabase.MONSTER_DATA.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( "monsters.txt", KoLConstants.MONSTERS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			Monster monster = null;

			if ( data.length >= 2 )
			{
				monster = MonsterDatabase.registerMonster( data[ 0 ], data[ 1 ] );
			}

			if ( monster == null )
			{
				continue;
			}

			boolean bogus = false;

			for ( int i = 2; i < data.length; ++i )
			{
				AdventureResult item = MonsterDatabase.parseItem( data[ i ] );
				if ( item == null || item.getItemId() == -1 || item.getName() == null )
				{
					RequestLogger.printLine( "Bad item for monster \"" + data[ 0 ] + "\": " + data[ i ] );
					bogus = true;
					continue;
				}

				monster.addItem( item );
			}

			if ( !bogus )
			{
				monster.doneWithItems();
				String keyName = CustomCombatManager.encounterKey( data[ 0 ], true );
				StringUtilities.registerPrepositions( keyName );
				MonsterDatabase.MONSTER_DATA.put( keyName, monster );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private static final AdventureResult parseItem( final String data )
	{
		String name = data;
		int count = 0;
		String countString;
		char prefix = '0';

		// Remove quantity and flag
		if ( name.endsWith( ")" ) )
		{
			int left = name.lastIndexOf( " (" );

			countString = name.substring( left + 2, name.length() - 1 );

			if ( !Character.isDigit( countString.charAt( 0 ) ) )
			{
				countString = countString.substring( 1 );
			}

			count =	 StringUtilities.parseInt( countString );
			prefix = name.charAt( left + 2 );
			name = name.substring( 0, left );
		}

		// Convert item numbers to names
		if ( name.startsWith( "[" ) )
		{
			int end = name.indexOf( "]" );
			int itemId = StringUtilities.parseInt( name.substring( 1, end ) );
			name = ItemDatabase.getItemName( itemId );
		}
		else if ( ItemDatabase.getItemId( name, 1 ) == -1 )
		{
			return new AdventureResult( data, (int)'0' );
		}

		return new AdventureResult( name, (count << 16) | prefix );
	}

	public static final Monster findMonster( final String name, boolean trySubstrings )
	{
		String keyName = CustomCombatManager.encounterKey( name, true );
		Monster match = (Monster) MonsterDatabase.MONSTER_DATA.get( keyName );

		// If no monster with that name exists, maybe it's
		// one of those monsters with an alternate name.

		if ( match != null )
		{
			return match;
		}

		if ( !trySubstrings )
		{
			return null;
		}

		if ( MonsterDatabase.MONSTER_STRINGS == null )
		{
			MonsterDatabase.MONSTER_STRINGS = new String[ MonsterDatabase.MONSTER_DATA.size() ];
			MonsterDatabase.MONSTER_DATA.keySet().toArray( MonsterDatabase.MONSTER_STRINGS );
		}

		List matchingNames = StringUtilities.getMatchingNames( MonsterDatabase.MONSTER_STRINGS, keyName );

		if ( matchingNames.size() != 1 )
		{
			return null;
		}

		return (Monster) MonsterDatabase.MONSTER_DATA.get( matchingNames.get( 0 ) );
	}

	public static final Set entrySet()
	{
		return MonsterDatabase.MONSTER_DATA.entrySet();
	}

	public static final Monster registerMonster( final String name, final String s )
	{
		Monster monster = MonsterDatabase.findMonster( name, false );
		if ( monster != null )
		{
			return monster;
		}

		// parse parameters and make a new monster
		int health = 0;
		int attack = 0;
		int defense = 0;
		int initiative = 0;
		int minMeat = 0;
		int maxMeat = 0;
		int attackElement = MonsterDatabase.NONE;
		int defenseElement = MonsterDatabase.NONE;
		int poison = Integer.MAX_VALUE;

		StringTokenizer tokens = new StringTokenizer( s, " " );
		while ( tokens.hasMoreTokens() )
		{
			String option = tokens.nextToken();
			String value;
			try
			{
				if ( option.equals( "HP:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						health = StringUtilities.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Atk:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						attack = StringUtilities.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Def:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						defense = StringUtilities.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Init:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						initiative = StringUtilities.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "E:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
						{
							attackElement = element;
							defenseElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "ED:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
						{
							defenseElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "EA:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
						{
							attackElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "Meat:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int dash = value.indexOf( "-" );
						if ( dash >= 0 )
						{
							minMeat = StringUtilities.parseInt( value.substring( 0, dash ) );
							maxMeat = StringUtilities.parseInt( value.substring( dash + 1 ) );
						}
						else
						{
							minMeat = StringUtilities.parseInt( value );
							maxMeat = minMeat;
						}
						continue;
					}
				}
				else if ( option.startsWith( "\"" ) )
				{
					StringBuffer temp = new StringBuffer( option );
					while ( !option.endsWith( "\"" ) && tokens.hasMoreTokens() )
					{
						option = tokens.nextToken();
						temp.append( ' ' );
						temp.append( option );
					}
					poison = AdventureSelectPanel.getPoisonLevel( temp.toString() );
					if ( poison == Integer.MAX_VALUE )
					{
						RequestLogger.printLine( "Monster: \"" + name + "\": unknown poison type: " + temp );
					}
					continue;
				}

				RequestLogger.printLine( "Monster: \"" + name + "\": unknown option: " + option );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, s );
			}

			return null;
		}

		return new Monster( name, health, attack, defense, initiative, attackElement, defenseElement, minMeat, maxMeat, poison );
	}

	private static final int parseElement( final String s )
	{
		if ( s.equals( "heat" ) )
		{
			return MonsterDatabase.HEAT;
		}
		if ( s.equals( "cold" ) )
		{
			return MonsterDatabase.COLD;
		}
		if ( s.equals( "stench" ) )
		{
			return MonsterDatabase.STENCH;
		}
		if ( s.equals( "spooky" ) )
		{
			return MonsterDatabase.SPOOKY;
		}
		if ( s.equals( "sleaze" ) )
		{
			return MonsterDatabase.SLEAZE;
		}
		if ( s.equals( "slime" ) )
		{
			return MonsterDatabase.SLIME;
		}
		return MonsterDatabase.NONE;
	}

	public static class Monster
		extends AdventureResult
	{
		private final int health;
		private final int attack;
		private final int defense;
		private final int initiative;
		private float statGain;
		private final int attackElement;
		private final int defenseElement;
		private final int minMeat;
		private final int maxMeat;
		private final int poison;

		private final ArrayList items;
		private final ArrayList pocketRates;

		public Monster( final String name, final int health, final int attack, final int defense, final int initiative,
			final int attackElement, final int defenseElement, final int minMeat, final int maxMeat, final int poison )
		{
			super( AdventureResult.MONSTER_PRIORITY, name );

			this.health = health;
			this.attack = attack;
			this.defense = defense;
			this.initiative = initiative;
			this.statGain = attack / 8.0f;
			this.attackElement = attackElement;
			this.defenseElement = defenseElement;
			this.minMeat = minMeat;
			this.maxMeat = maxMeat;
			this.poison = poison;

			this.items = new ArrayList();
			this.pocketRates = new ArrayList();
		}

		public int getHP()
		{
			return this.health;
		}

		public int getAdjustedHP( final int ml )
		{
			return this.health + ml;
		}

		public int getAttack()
		{
			return this.attack;
		}

		public int getDefense()
		{
			return this.defense;
		}

		public int getInitiative()
		{
			return this.initiative;
		}

		public int getAttackElement()
		{
			return this.attackElement;
		}

		public int getDefenseElement()
		{
			return this.defenseElement;
		}

		public int getMinMeat()
		{
			return this.minMeat;
		}

		public int getMaxMeat()
		{
			return this.maxMeat;
		}

		public int getPoison()
		{
			return this.poison;
		}

		public List getItems()
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

			return this.shouldSteal( KoLConstants.conditions );
		}

		private boolean shouldSteal( final List checklist )
		{
			float dropModifier = AreaCombatData.getDropRateModifier();

			for ( int i = 0; i < checklist.size(); ++i )
			{
				if ( this.shouldStealItem( (AdventureResult) checklist.get( i ), dropModifier ) )
				{
					return true;
				}
			}

			return false;
		}

		private boolean shouldStealItem( AdventureResult item, final float dropModifier )
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
				case 'b':
					return false;
				default:
					return (item.getCount() >> 16) * dropModifier < 100.0f;
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

			float probability = 0.0f;
			float[] coefficients = new float[ this.items.size() ];

			for ( int i = 0; i < this.items.size(); ++i )
			{
				coefficients[ 0 ] = 1.0f;
				for ( int j = 1; j < coefficients.length; ++j )
				{
					coefficients[ j ] = 0.0f;
				}

				for ( int j = 0; j < this.items.size(); ++j )
				{
					AdventureResult item = (AdventureResult) this.items.get( j );
					probability = (item.getCount() >> 16) / 100.0f;
					switch ( (char) item.getCount() & 0xFFFF )
					{
					case 'p':
						probability = 0.05f;
						break;
					case 'n':
					case 'c':
					case 'b':
						probability = 0.0f;
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

				probability = 0.0f;

				for ( int j = 0; j < coefficients.length; ++j )
				{
					probability += coefficients[ j ] / ( j + 1 );
				}

				this.pocketRates.add( new Float( probability ) );
			}
		}

		public float getExperience()
		{
			return Math.max( 1.0f, this.statGain );
		}

		public float getAdjustedExperience( final float modifier )
		{
			this.statGain = this.attack / 8.0f + modifier;

			return Math.max( 1.0f, this.statGain );
		}

		public boolean willUsuallyMiss()
		{
			return this.willUsuallyMiss( 0 );
		}

		public boolean willUsuallyDodge( final int offenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + offenseModifier;
			int dodgeRate = KoLCharacter.getAdjustedMoxie() - ( this.attack + ml ) - 6;
			return dodgeRate > 0;
		}

		public boolean willUsuallyMiss( final int defenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + defenseModifier;
			int hitStat = EquipmentManager.getAdjustedHitStat();

			return AreaCombatData.hitPercent( hitStat - ml, this.defense ) <= 50.0f;
		}
	}
}
