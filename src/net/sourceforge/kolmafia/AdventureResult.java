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

package net.sourceforge.kolmafia;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureResult
	implements Comparable, KoLConstants
{
	public static final int[] SESSION_SUBSTATS = new int[ 3 ];
	public static final int[] SESSION_FULLSTATS = new int[ 3 ];
	public static final int[] CONDITION_SUBSTATS = new int[ 3 ];

	public static final String[] STAT_NAMES = { "muscle", "mysticality", "moxie" };

	private int itemId;
	private int[] count;
	protected String name;
	private int priority;

	private static final int NO_PRIORITY = 0;
	private static final int ADV_PRIORITY = 1;
	private static final int MEAT_PRIORITY = 2;
	private static final int SUBSTAT_PRIORITY = 3;
	private static final int FULLSTAT_PRIORITY = 4;
	private static final int ITEM_PRIORITY = 5;
	private static final int EFFECT_PRIORITY = 6;

	protected static final int MONSTER_PRIORITY = -1;

	public static final String HP = "HP";
	public static final String MP = "MP";
	public static final String ADV = "Adv";
	public static final String CHOICE = "Choice";
	public static final String DRUNK = "Drunk";
	public static final String MEAT = "Meat";
	public static final String SUBSTATS = "Substats";
	public static final String FULLSTATS = "Fullstats";
	public static final String PULL = "Pull";

	private static final List MUS_SUBSTAT = new ArrayList();
	private static final List MYS_SUBSTAT = new ArrayList();
	private static final List MOX_SUBSTAT = new ArrayList();

	static
	{
		AdventureResult.MUS_SUBSTAT.add( "Beefiness" );
		AdventureResult.MUS_SUBSTAT.add( "Fortitude" );
		AdventureResult.MUS_SUBSTAT.add( "Muscleboundness" );
		AdventureResult.MUS_SUBSTAT.add( "Strengthliness" );
		AdventureResult.MUS_SUBSTAT.add( "Strongness" );
		// The following only under Can Has Cyborger
		AdventureResult.MUS_SUBSTAT.add( "muskewlairtees" );

		AdventureResult.MYS_SUBSTAT.add( "Enchantedness" );
		AdventureResult.MYS_SUBSTAT.add( "Magicalness" );
		AdventureResult.MYS_SUBSTAT.add( "Mysteriousness" );
		AdventureResult.MYS_SUBSTAT.add( "Wizardliness" );
		// The following only under Can Has Cyborger
		AdventureResult.MYS_SUBSTAT.add( "mistikkaltees" );

		AdventureResult.MOX_SUBSTAT.add( "Cheek" );
		AdventureResult.MOX_SUBSTAT.add( "Chutzpah" );
		AdventureResult.MOX_SUBSTAT.add( "Roguishness" );
		AdventureResult.MOX_SUBSTAT.add( "Sarcasm" );
		AdventureResult.MOX_SUBSTAT.add( "Smarm" );
		// The following only under Can Has Cyborger
		AdventureResult.MOX_SUBSTAT.add( "mawksees" );
	}

	public static final AdventureResult SESSION_SUBSTATS_RESULT =
		new AdventureResult( AdventureResult.SUBSTATS, AdventureResult.SESSION_SUBSTATS );
	public static final AdventureResult SESSION_FULLSTATS_RESULT =
		new AdventureResult( AdventureResult.FULLSTATS, AdventureResult.SESSION_FULLSTATS );
	public static final AdventureResult CONDITION_SUBSTATS_RESULT =
		new AdventureResult( AdventureResult.SUBSTATS, AdventureResult.CONDITION_SUBSTATS );

	/**
	 * Constructs a new <code>AdventureResult</code> with the given name. The amount of gain will default to zero.
	 *
	 * @param name The name of the result
	 */

	public AdventureResult( final String name )
	{
		this( AdventureResult.choosePriority( name ), name, 0 );
	}

	public AdventureResult( final String name, final int count )
	{
		this( AdventureResult.choosePriority( name ), name, count );
	}

	public AdventureResult( final String name, final int[] count )
	{
		this( AdventureResult.choosePriority( name ), name, count );
	}

	public AdventureResult( final String name, final int count, final boolean isStatusEffect )
	{
		this( isStatusEffect ? EFFECT_PRIORITY : ITEM_PRIORITY, name, count );
	}

	protected AdventureResult( final int subType, final String name )
	{
		this( subType, name, 1 );
	}

	protected AdventureResult( final int subType, final String name, final int count )
	{
		this( subType, name, new int[] { count } );
	}

	protected AdventureResult( final int subType, final String name, final int[] count )
	{
		this.name = name;
		this.count = count;
		this.priority = subType;
		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			this.normalizeEffectName();
		}
		else if ( this.priority == AdventureResult.ITEM_PRIORITY )
		{
			this.normalizeItemName();
		}
	}

	private static int choosePriority( final String name )
	{
		if ( name.equals( AdventureResult.ADV ) ||
			name.equals( AdventureResult.CHOICE ) ||
			name.equals( AdventureResult.PULL ))
		{
			return AdventureResult.ADV_PRIORITY;
		}
		if ( name.equals( AdventureResult.MEAT ) )
		{
			return AdventureResult.MEAT_PRIORITY;
		}
		if ( name.equals( AdventureResult.HP ) || name.equals( AdventureResult.MP ) || name.equals( AdventureResult.DRUNK ) )
		{
			return AdventureResult.NO_PRIORITY;
		}
		if ( name.equals( AdventureResult.SUBSTATS ) )
		{
			return AdventureResult.SUBSTAT_PRIORITY;
		}
		if ( name.equals( AdventureResult.FULLSTATS ) )
		{
			return AdventureResult.FULLSTAT_PRIORITY;
		}
		if ( EffectDatabase.contains( name ) )
		{
			return AdventureResult.EFFECT_PRIORITY;
		}
		return AdventureResult.ITEM_PRIORITY;
	}

	public AdventureResult( final int itemId, final int count )
	{
		String name = ItemDatabase.getItemName( itemId );
		this.name = name != null ? name : "(unknown item " + String.valueOf( this.itemId ) + ")";
		this.itemId = itemId;
		this.count = new int[] { count };
		this.priority = AdventureResult.ITEM_PRIORITY;
	}

	public void normalizeEffectName()
	{
		this.priority = AdventureResult.EFFECT_PRIORITY;

		if ( this.name == null )
		{
			this.name = "(unknown effect)";
			return;
		}

		int effectId = EffectDatabase.getEffectId( this.name );
		if ( effectId > 0 )
		{
			this.name = EffectDatabase.getEffectName( effectId );
		}
	}

	public void normalizeItemName()
	{
		this.priority = AdventureResult.ITEM_PRIORITY;

		if ( this.name == null )
		{
			this.name = "(unknown item " + String.valueOf( this.itemId ) + ")";
			return;
		}

		if ( this.name.equals( "(none)" ) || this.name.equals( "-select an item-" ) )
		{
			return;
		}

		this.itemId = ItemDatabase.getItemId( this.name, this.getCount() );

		if ( this.itemId > 0 )
		{
			this.name = ItemDatabase.getItemName( this.itemId );
		}
		else if ( StaticEntity.getClient() != null )
		{
			RequestLogger.printLine( "Unknown item found: " + this.name );
		}
	}

	public static final AdventureResult pseudoItem( final String name )
	{
		AdventureResult item = ItemFinder.getFirstMatchingItem( name, false );
		if ( item != null )
		{
			return item;
		}

		// Make a pseudo-item with the required name
		return AdventureResult.tallyItem( name, false );
	}

	public static final AdventureResult tallyItem( final String name )
	{
		return AdventureResult.tallyItem( name, true );
	}

	public static final AdventureResult tallyItem( final String name, final boolean setItemId )
	{
		AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, name );
		item.priority = AdventureResult.ITEM_PRIORITY;
		item.itemId = setItemId ? ItemDatabase.getItemId( name, 1, false ) : -1;
		return item;
	}
	
	public static final AdventureResult tallyItem( final String name, final int count, final boolean setItemId )
	{
		AdventureResult item = AdventureResult.tallyItem( name, setItemId );
		item.count[ 0 ] = count;
		return item;
	}

	/**
	 * Accessor method to determine if this result is a status effect.
	 *
	 * @return <code>true</code> if this result represents a status effect
	 */

	public boolean isStatusEffect()
	{
		return this.priority == AdventureResult.EFFECT_PRIORITY;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 *
	 * @return <code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMuscleGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 0 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is a mysticality gain.
	 *
	 * @return <code>true</code> if this result represents mysticality subpoint gain
	 */

	public boolean isMysticalityGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 1 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is a muscle gain.
	 *
	 * @return <code>true</code> if this result represents muscle subpoint gain
	 */

	public boolean isMoxieGain()
	{
		return this.priority == AdventureResult.SUBSTAT_PRIORITY && this.count[ 2 ] != 0;
	}

	/**
	 * Accessor method to determine if this result is an item, as opposed to meat, drunkenness, adventure or substat
	 * gains.
	 *
	 * @return <code>true</code> if this result represents an item
	 */

	public boolean isItem()
	{
		return this.priority == AdventureResult.ITEM_PRIORITY;
	}

	public boolean isMeat()
	{
		return this.priority == AdventureResult.MEAT_PRIORITY;
	}

	public boolean isMP()
	{
		return this.name.equals( AdventureResult.MP );
	}

	/**
	 * Accessor method to retrieve the name associated with the result.
	 *
	 * @return The name of the result
	 */

	public String getName()
	{
		switch ( this.itemId )
		{
		case ItemPool.MILKY_POTION:
		case ItemPool.SWIRLY_POTION:
		case ItemPool.BUBBLY_POTION:
		case ItemPool.SMOKY_POTION:
		case ItemPool.CLOUDY_POTION:
		case ItemPool.EFFERVESCENT_POTION:
		case ItemPool.FIZZY_POTION:
		case ItemPool.DARK_POTION:
		case ItemPool.MURKY_POTION:

			return AdventureResult.bangPotionName( this.itemId );

		case ItemPool.MOSSY_STONE_SPHERE:
		case ItemPool.SMOOTH_STONE_SPHERE:
		case ItemPool.CRACKED_STONE_SPHERE:
		case ItemPool.ROUGH_STONE_SPHERE:

			return AdventureResult.stoneSphereName( this.itemId );

		case ItemPool.PUNCHCARD_ATTACK:
		case ItemPool.PUNCHCARD_REPAIR:
		case ItemPool.PUNCHCARD_BUFF:
		case ItemPool.PUNCHCARD_MODIFY:
		case ItemPool.PUNCHCARD_BUILD:
		case ItemPool.PUNCHCARD_TARGET:
		case ItemPool.PUNCHCARD_SELF:
		case ItemPool.PUNCHCARD_FLOOR:
		case ItemPool.PUNCHCARD_DRONE:
		case ItemPool.PUNCHCARD_WALL:
		case ItemPool.PUNCHCARD_SPHERE:

			return AdventureResult.punchCardName( this.itemId );

		default:
			return this.name;
		}
	}

	/**
	 * Accessor method to retrieve the item Id associated with the result, if this is an item and the item Id is known.
	 *
	 * @return The item Id associated with this item
	 */

	public int getItemId()
	{
		return this.itemId;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result. In the event of substat points, this
	 * returns the total subpoints within the <code>AdventureResult</code>; in the event of an item or meat gains,
	 * this will return the total number of meat/items in this result.
	 *
	 * @return The amount associated with this result
	 */

	public int getCount()
	{
		int totalCount = 0;
		for ( int i = 0; i < this.count.length; ++i )
		{
			totalCount += this.count[ i ];
		}
		return totalCount;
	}

	/**
	 * Accessor method to retrieve the total value associated with the result stored at the given index of the count
	 * array.
	 *
	 * @return The total value at the given index of the count array
	 */

	public int getCount( final int index )
	{
		return index < 0 || index >= this.count.length ? 0 : this.count[ index ];
	}

	/**
	 * A static final method which parses the given string for any content which might be applicable to an
	 * <code>AdventureResult</code>, and returns the resulting <code>AdventureResult</code>.
	 *
	 * @param s The string suspected of being an <code>AdventureResult</code>
	 * @return An <code>AdventureResult</code> with the appropriate data
	 * @throws NumberFormatException The string was not a recognized <code>AdventureResult</code>
	 * @throws ParseException The value enclosed within parentheses was not a number.
	 */

	public static final AdventureResult parseResult( final String s )
	{
		if ( s.startsWith( "You gain" ) || s.startsWith( "You lose" ) )
		{
			// A stat has been modified - now you figure out which
			// one it was, how much it's been modified by, and
			// return the appropriate value

			StringTokenizer parsedGain = new StringTokenizer( s, " ." );
			parsedGain.nextToken();

			int modifier =
				StringUtilities.parseInt( ( parsedGain.nextToken().startsWith( "gain" ) ? "" : "-" ) + parsedGain.nextToken() );
			String statname = parsedGain.nextToken();

			// Stats actually fall into one of four categories -
			// simply pick the correct one and return the result.

			if ( parsedGain.hasMoreTokens() )
			{
				char identifier = statname.charAt( 0 );
				return new AdventureResult(
					identifier == 'H' || identifier == 'h' ? AdventureResult.HP : AdventureResult.MP, modifier );
			}

			if ( statname.startsWith( "Adv" ) )
			{
				return new AdventureResult( AdventureResult.ADV, modifier );
			}

			if ( statname.startsWith( "Dru" ) )
			{
				return new AdventureResult( AdventureResult.DRUNK, modifier );
			}

			if ( statname.startsWith( "Me" ) )
			{
				// "Meat" or "Meets", if Can Has Cyborger
				return new AdventureResult( AdventureResult.MEAT, modifier );
			}

			// In the current implementations, all stats gains are
			// located inside of a generic adventure which
			// indicates how much of each substat is gained.

			int[] gained =
				{ AdventureResult.MUS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MYS_SUBSTAT.contains( statname ) ? modifier : 0, AdventureResult.MOX_SUBSTAT.contains( statname ) ? modifier : 0 };

			return new AdventureResult( AdventureResult.SUBSTATS, gained );
		}

		return AdventureResult.parseItem( s, false );
	}

	public static final AdventureResult parseItem( final String s, final boolean pseudoAllowed )
	{
		StringTokenizer parsedItem = new StringTokenizer( s, "()" );

		String name = parsedItem.nextToken().trim();
		int count = parsedItem.hasMoreTokens() ? StringUtilities.parseInt( parsedItem.nextToken() ) : 1;

		if ( !pseudoAllowed )
		{
			return new AdventureResult( name, count );
		}

		// Hand craft an item Adventure Result, regardless of the name
		AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, name );
		item.priority = AdventureResult.ITEM_PRIORITY;
		item.itemId = ItemDatabase.getItemId( name, 1, false );
		item.count[0] = count;

		return item;
	}

	/**
	 * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful in debug, or
	 * if the <code>AdventureResult</code> is to be displayed in a <code>ListModel</code>.
	 *
	 * @return The string version of this <code>AdventureResult</code>
	 */

	public String toString()
	{
		if ( this.name == null )
		{
			return "(Unrecognized result)";
		}

		if ( this.name.equals( AdventureResult.ADV ) )
		{
			return " Advs Used: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return " Meat Gained: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.CHOICE ) )
		{
			return " Choices Left: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.PULL ) )
		{
			return " Budgeted Pulls: " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.HP ) || this.name.equals( AdventureResult.MP ) || this.name.equals( AdventureResult.DRUNK ) )
		{
			return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] );
		}

		if ( this.name.equals( AdventureResult.SUBSTATS ) || this.name.equals( AdventureResult.FULLSTATS ) )
		{
			return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format( this.count[ 0 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.count[ 1 ] ) + " / " + KoLConstants.COMMA_FORMAT.format( this.count[ 2 ] );
		}

		if ( this.priority == AdventureResult.MONSTER_PRIORITY )
		{
			return this.name;
		}

		String name = this.getName();

		if ( this.priority == AdventureResult.EFFECT_PRIORITY )
		{
			if ( name.equals( "On the Trail" ) )
			{
				String monster = Preferences.getString( "olfactedMonster" );
				if ( !monster.equals( "" ) )
				{
					name = name + " [" + monster + "]";
				}
			}
			else 
			{
				String skillName = UneffectRequest.effectToSkill( name );
				if ( SkillDatabase.contains( skillName ) )
				{
					int skillId = SkillDatabase.getSkillId( skillName );
					if ( skillId > 6000 && skillId < 7000 )
					{
						name = "\u266B " + name;
					}
				}
			}
		}

		int count = this.count[ 0 ];

		return count == 1 ? name : 
			count > Integer.MAX_VALUE/2 ? name + " (\u221E)" :
			name + " (" + KoLConstants.COMMA_FORMAT.format( count ) + ")";
	}

	public String toConditionString()
	{
		if ( this.name == null )
		{
			return "";
		}

		if ( this.name.equals( AdventureResult.ADV ) || this.name.equals( AdventureResult.CHOICE ) )
		{
			return this.count[ 0 ] + " choiceadv";
		}

		if ( this.name.equals( AdventureResult.MEAT ) )
		{
			return this.count[ 0 ] + " meat";
		}

		if ( this.name.equals( AdventureResult.HP ) )
		{
			return this.count[ 0 ] + " health";
		}

		if ( this.name.equals( AdventureResult.MP ) )
		{
			return this.count[ 0 ] + " mana";
		}

		if ( this.name.equals( AdventureResult.SUBSTATS ) )
		{
			StringBuffer stats = new StringBuffer();

			if ( this.count[ 0 ] > 0 )
			{
				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() + this.count[ 0 ] ) + " muscle" );
			}

			if ( this.count[ 1 ] > 0 )
			{
				if ( this.count[ 0 ] > 0 )
				{
					stats.append( ", " );
				}

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() + this.count[ 1 ] ) + " mysticality" );
			}

			if ( this.count[ 2 ] > 0 )
			{
				if ( this.count[ 0 ] > 0 || this.count[ 1 ] > 0 )
				{
					stats.append( ", " );
				}

				stats.append( KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() + this.count[ 2 ] ) + " moxie" );
			}

			return stats.toString();
		}

		return "+" + this.count[ 0 ] + " " + this.name;
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality. Note that this will still
	 * return <code>true</code> if the values do not match; this merely matches on names.
	 *
	 * @param o The <code>Object</code> to be compared with this <code>AdventureResult</code>
	 * @return <code>true</code> if the <code>Object</code> is an <code>AdventureResult</code> and has the same
	 *	   name as this one
	 */

	public boolean equals( final Object o )
	{
		if ( !( o instanceof AdventureResult ) || o == null )
		{
			return false;
		}

		AdventureResult ar = (AdventureResult) o;
		if ( this.name == null || ar.name == null || this.count == null || ar.count == null )
		{
			return false;
		}
		if ( ar instanceof WildcardResult )
		{
			return ar.equals( this );
		}

		return this.count.length == ar.count.length && ( !ar.isItem() || this.itemId == ar.itemId ) && this.name.equalsIgnoreCase( ar.name );
	}

	/**
	 * Compares the <code>AdventureResult</code> with the given object for name equality and priority differences.
	 * Return values are consistent with the rules laid out in {@link java.lang.Comparable#compareTo(Object)}.
	 */

	public int compareTo( final Object o )
	{
		if ( !( o instanceof AdventureResult ) || o == null )
		{
			return -1;
		}

		AdventureResult ar = (AdventureResult) o;
		if ( this.name.equalsIgnoreCase( ar.name ) )
		{
			return 0;
		}

		int priorityDifference = this.priority - ar.priority;
		if ( priorityDifference != 0 )
		{
			return priorityDifference;
		}

		if ( this.isStatusEffect() )
		{
			return this.getCount() - ar.getCount();
		}

		int nameComparison = this.name.compareToIgnoreCase( ar.name );
		if ( nameComparison != 0 )
		{
			return nameComparison;
		}

		return this.isItem() ? this.itemId - ar.itemId : 0;
	}

	/**
	 * Utility method used for adding a given <code>AdventureResult</code> to a tally of <code>AdventureResult</code>s.
	 *
	 * @param tally The tally accumulating <code>AdventureResult</code>s
	 * @param result The result to add to the tally
	 */

	public static final void addResultToList( final List sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );

		// First, filter out things where it's a simple addition of an
		// item, or something which may not result in a change in the
		// state of the sourceList list.

		if ( index == -1 )
		{
			if ( !result.isItem() )
			{
				sourceList.add( result );
				return;
			}
			int count = result.getCount();
			if ( count == 0 ) return;
			if ( count < 0 && ( sourceList != KoLConstants.tally ||
				!Preferences.getBoolean( "allowNegativeTally" ) ) )
			{
				return;
			}
			sourceList.add( result );
			return;
		}

		// These don't involve any addition -- ignore this entirely
		// for now.

		if ( result == AdventureResult.SESSION_SUBSTATS_RESULT || result == AdventureResult.SESSION_FULLSTATS_RESULT || result == AdventureResult.CONDITION_SUBSTATS_RESULT )
		{
			return;
		}

		// Compute the sum of the existing adventure result and the
		// current adventure result, and construct the sum.

		AdventureResult current = (AdventureResult) sourceList.get( index );
		AdventureResult sumResult;

		if ( current.count.length == 1 )
		{
			sumResult = current.getInstance( current.count[ 0 ] + result.count[ 0 ] );
		}
		else
		{
			sumResult = current.getInstance( new int[ current.count.length ] );
			for ( int i = 0; i < current.count.length; ++i )
			{
				sumResult.count[ i ] = current.count[ i ] + result.count[ i ];
			}
		}

		// Check to make sure that the result didn't transform the value
		// to zero - if it did, then remove the item from the list if
		// it's an item (non-items are exempt).

		if ( sumResult.isItem() )
		{
			if ( sumResult.getCount() == 0 )
			{
				sourceList.remove( index );
				return;
			}
			else if ( sumResult.getCount() < 0 && ( sourceList != KoLConstants.tally || !Preferences.getBoolean( "allowNegativeTally" ) ) )
			{
				sourceList.remove( index );
				return;
			}
		}
		else if ( sumResult.getCount() == 0 && ( sumResult.isStatusEffect() || sumResult.getName().equals(
			AdventureResult.CHOICE ) ) )
		{
			sourceList.remove( index );
			return;
		}
		else if ( sumResult.getCount() < 0 && sumResult.isStatusEffect() )
		{
			sourceList.remove( index );
			return;
		}

		sourceList.set( index, sumResult );
	}

	public static final void removeResultFromList( final List sourceList, final AdventureResult result )
	{
		int index = sourceList.indexOf( result );
		if ( index != -1 )
		{
			sourceList.remove( index );
		}
	}

	public AdventureResult getNegation()
	{
		if ( this.isItem() && this.itemId != -1 )
		{
			return this.count[ 0 ] == 0 ? this : new AdventureResult( this.itemId, 0 - this.count[ 0 ] );
		}
		else if ( this.isStatusEffect() )
		{
			return this.count[ 0 ] == 0 ? this : new AdventureResult( this.name, 0 - this.count[ 0 ], true );
		}

		int[] newcount = new int[ this.count.length ];
		for ( int i = 0; i < this.count.length; ++i )
		{
			newcount[ i ] = 0 - this.count[ i ];
		}

		return this.getInstance( newcount );
	}

	public AdventureResult getInstance( final int quantity )
	{
		if ( this.isItem() )
		{
			if ( this.count[ 0 ] == quantity )
			{
				return this;
			}

			// Handle pseudo and tally items
			AdventureResult item = new AdventureResult( AdventureResult.NO_PRIORITY, this.name );
			item.priority = AdventureResult.ITEM_PRIORITY;
			item.itemId = this.itemId;
			item.count[ 0 ] = quantity;
			return item;
		}

		if ( this.isStatusEffect() )
		{
			return this.count[ 0 ] == quantity ? this : new AdventureResult( this.name, quantity, true );
		}

		return new AdventureResult( this.name, quantity );
	}

	public AdventureResult getInstance( final int[] quantity )
	{
		if ( this.priority != AdventureResult.SUBSTAT_PRIORITY && this.priority != AdventureResult.FULLSTAT_PRIORITY )
		{
			return this.getInstance( quantity[ 0 ] );
		}

		if ( this.priority == AdventureResult.SUBSTAT_PRIORITY )
		{
			return new AdventureResult( AdventureResult.SUBSTATS, quantity );
		}

		AdventureResult stats = new AdventureResult( AdventureResult.FULLSTATS );
		stats.count = quantity;
		return stats;
	}

	/**
	 * Special method which simplifies the constant use of indexOf and count retrieval. This makes intent more
	 * transparent.
	 */

	public int getCount( final List list )
	{
		int index = list.indexOf( this );
		return index == -1 ? 0 : ( (AdventureResult) list.get( index ) ).getCount();
	}

	public static final String bangPotionName( final int itemId )
	{
		String itemName = ItemDatabase.getItemName( itemId );

		String effect = Preferences.getString( "lastBangPotion" + itemId );
		if ( effect.equals( "" ) )
		{
			return itemName;
		}

		return itemName + " of " + effect;
	}

	public final String bangPotionAlias()
	{
		if ( this.itemId < 819 || this.itemId > 827 )
		{
			return this.name;
		}

		String effect = Preferences.getString( "lastBangPotion" + this.itemId );
		if ( effect.equals( "" ) )
		{
			return this.name;
		}

		return "potion of " + effect;
	}

	public static final String stoneSphereName( final int itemId )
	{
		String itemName = ItemDatabase.getItemName( itemId );

		String effect = Preferences.getString( "lastStoneSphere" + itemId );
		if ( effect.equals( "" ) )
		{
			return itemName;
		}

		return itemName + " of " + effect;
	}

	public static final String punchCardName( final int itemId )
	{
		for ( int i = 0; i < RequestEditorKit.PUNCHCARDS.length; ++i )
		{
			Object [] punchcard = RequestEditorKit.PUNCHCARDS[i];
			if ( ( (Integer) punchcard[0]).intValue() == itemId )
			{
				return (String) punchcard[2];
			}
		}

		return ItemDatabase.getItemName( itemId );
	}
	
	public static class WildcardResult
	extends AdventureResult
	{
		// Note that these objects must not be placed in a sorted list, since they
		// are not meaningfully comparable other than via equals().
		private String match;
		private boolean negated;
		
		public WildcardResult( String name, int count, String match, boolean negated )
		{
			super( AdventureResult.ITEM_PRIORITY, name, count );
			this.match = match.toLowerCase();
			this.negated = negated;
		}
		
		public AdventureResult getInstance( int count )
		{
			return new WildcardResult( this.getName(), count, this.match, this.negated );
		}

		public boolean equals( final Object o )
		{
			if ( !( o instanceof AdventureResult ) || o == null )
			{
				return false;
			}
	
			AdventureResult ar = (AdventureResult) o;
			return (ar.getName().toLowerCase().indexOf( this.match ) != -1) ^ this.negated;
		}
		
		public int getCount( final List list )
		{
			int count = 0;
			Iterator i = list.iterator();
			while ( i.hasNext() )
			{
				AdventureResult ar = (AdventureResult) i.next();
				if ( this.equals( ar ) )
				{
					count += ar.getCount();
				}
			}
			return count;
		}
		
		public static WildcardResult getInstance( String text )
		{
			if ( text.indexOf( "any" ) == -1 )
			{
				return null;
			}
			
			String[] pieces = text.split( " ", 2 );
			int count = StringUtilities.parseInt( pieces[ 0 ] );
			if ( pieces.length > 1 && count != 0 )
			{
				text = pieces[ 1 ];
			}
			else
			{
				count = 1;
			}
			
			if ( text.startsWith( "any " ) )
			{
				return new WildcardResult( text, count, text.substring( 4 ), false );
			}
			if ( text.startsWith( "anything but " ) )
			{
				return new WildcardResult( text, count, text.substring( 13 ), true );
			}
			return null;
		}
	}
}
