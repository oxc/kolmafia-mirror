/**
 * Copyright (c) 2005-2010, KoLmafia development team
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
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ItemDatabase
	extends KoLDatabase
{
	public static final AdventureResult ODE = EffectPool.get( EffectPool.ODE );
	public static final AdventureResult MILK = EffectPool.get( EffectPool.MILK );

	private static int maxItemId = 0;

	private static String [] canonicalNames = new String[0];
	private static final IntegerArray useTypeById = new IntegerArray();
	private static final IntegerArray attributesById = new IntegerArray();
	private static final IntegerArray priceById = new IntegerArray();
	private static final StringArray pluralById = new StringArray();

	private static final Map nameById = new TreeMap();
	private static final Map dataNameById = new HashMap();
	private static final Map descriptionById = new TreeMap();
	private static final Map itemIdByName = new HashMap();
	private static final Map itemIdByPlural = new HashMap();

	private static final Map itemIdByDescription = new HashMap();

	private static final Map levelReqByName = new HashMap();
	private static final Map fullnessByName = new HashMap();
	private static final Map inebrietyByName = new HashMap();
	private static final Map spleenHitByName = new HashMap();
	private static final Map notesByName = new HashMap();
	private static final Map foldGroupsByName = new HashMap();

	private static final Map[][][] advsByName = new HashMap[ 2 ][ 2 ][ 2 ];
	private static final Map unitCostByName = new HashMap();
	private static final Map advStartByName = new HashMap();
	private static final Map advEndByName = new HashMap();

	private static Set advNames = null;

	static
	{
		ItemDatabase.advsByName[ 0 ][ 0 ][ 0 ] = new HashMap();
		ItemDatabase.advsByName[ 0 ][ 0 ][ 1 ] = new HashMap();
		ItemDatabase.advsByName[ 0 ][ 1 ][ 0 ] = new HashMap();
		ItemDatabase.advsByName[ 0 ][ 1 ][ 1 ] = new HashMap();

		ItemDatabase.advsByName[ 1 ][ 0 ][ 0 ] = new HashMap();
		ItemDatabase.advsByName[ 1 ][ 0 ][ 1 ] = new HashMap();
		ItemDatabase.advsByName[ 1 ][ 1 ][ 0 ] = new HashMap();
		ItemDatabase.advsByName[ 1 ][ 1 ][ 1 ] = new HashMap();
	}
	
	private static Object[][] ALIASES = {
		{ new Integer( 4577 ), "bugged bonnet" },
		{ new Integer( 4578 ), "bugged meat stabbing club" },
		{ new Integer( 4579 ), "bugged Knob Goblin love potion" },
		{ new Integer( 4580 ), "bugged old school Mafia knickerbockers" },
		{ new Integer( 4581 ), "bugged Talisman of Baio" },
	};

	private static final String[] ACCESS =
	{
		"none",
		"display",
		"gift",
		"all",
	};

	private final static String parseAccess( final String data )
	{
		for ( int i = 0; i < ItemDatabase.ACCESS.length; ++i )
		{
			String access = ItemDatabase.ACCESS[ i ];
			if ( access.equals( data ) )
			{
				return access;
			}
		}
		return "bogus";
	};

	private static final Map muscleByName = new HashMap();
	private static final Map mysticalityByName = new HashMap();
	private static final Map moxieByName = new HashMap();

	private static final Map accessById = new HashMap();

	private static float muscleFactor = 1.0f;
	private static float mysticalityFactor = 1.0f;
	private static float moxieFactor = 1.0f;
	
	public static final int ATTR_TRADEABLE = 0x00000001;
	public static final int ATTR_GIFTABLE = 0x00000002;
	public static final int ATTR_DISPLAYABLE = 0x00000004;
	public static final int ATTR_COMBAT = 0x00000008;
	public static final int ATTR_COMBAT_REUSABLE = 0x00000010;
	public static final int ATTR_USABLE = 0x00000020;
	public static final int ATTR_MULTIPLE = 0x00000040;
	public static final int ATTR_REUSABLE = 0x00000080;
	public static final int ATTR_SINGLE = 0x00000100;
	public static final int ATTR_SOLO = 0x00000200;
	public static final int ATTR_CURSE = 0x00000400;
	public static final int ATTR_BOUNTY = 0x00000800;
	public static final int ATTR_CANDY = 0x00001000;
	public static final int ATTR_MATCHABLE = 0x00002000;
	
	private static final HashMap PRIMARY_USE = new HashMap();
	private static final HashMap INVERSE_PRIMARY_USE = new HashMap();
	private static final HashMap SECONDARY_USE = new HashMap();
	private static final TreeMap INVERSE_SECONDARY_USE = new TreeMap();
	private static final Set secondaryUsageEntrySet;

	private static void definePrimaryUse( final String key, final int usage )
	{
		Integer val = new Integer( usage );
		PRIMARY_USE.put( key, val );
		INVERSE_PRIMARY_USE.put( val, key );
	}

	private static void defineSecondaryUse( final String key, final int usage )
	{
		Integer val = new Integer( usage );
		SECONDARY_USE.put( key, val );
		INVERSE_SECONDARY_USE.put( val, key );
	}

	static
	{
		ItemDatabase.definePrimaryUse( "none", KoLConstants.NO_CONSUME );
		ItemDatabase.definePrimaryUse( "food", KoLConstants.CONSUME_EAT );
		ItemDatabase.definePrimaryUse( "drink", KoLConstants.CONSUME_DRINK );
		ItemDatabase.definePrimaryUse( "usable", KoLConstants.CONSUME_USE );
		ItemDatabase.definePrimaryUse( "multiple", KoLConstants.CONSUME_MULTIPLE );
		ItemDatabase.definePrimaryUse( "grow", KoLConstants.GROW_FAMILIAR );
		ItemDatabase.definePrimaryUse( "zap", KoLConstants.CONSUME_ZAP );
		ItemDatabase.definePrimaryUse( "familiar", KoLConstants.EQUIP_FAMILIAR );
		ItemDatabase.definePrimaryUse( "accessory", KoLConstants.EQUIP_ACCESSORY );
		ItemDatabase.definePrimaryUse( "hat", KoLConstants.EQUIP_HAT );
		ItemDatabase.definePrimaryUse( "pants", KoLConstants.EQUIP_PANTS );
		ItemDatabase.definePrimaryUse( "shirt", KoLConstants.EQUIP_SHIRT );
		ItemDatabase.definePrimaryUse( "weapon", KoLConstants.EQUIP_WEAPON );
		ItemDatabase.definePrimaryUse( "offhand", KoLConstants.EQUIP_OFFHAND );
		ItemDatabase.definePrimaryUse( "mp", KoLConstants.MP_RESTORE );
		ItemDatabase.definePrimaryUse( "message", KoLConstants.MESSAGE_DISPLAY );
		ItemDatabase.definePrimaryUse( "hp", KoLConstants.HP_RESTORE );
		ItemDatabase.definePrimaryUse( "hpmp", KoLConstants.HPMP_RESTORE );
		ItemDatabase.definePrimaryUse( "reusable", KoLConstants.INFINITE_USES );
		ItemDatabase.definePrimaryUse( "container", KoLConstants.EQUIP_CONTAINER );
		ItemDatabase.definePrimaryUse( "sphere", KoLConstants.CONSUME_SPHERE );
		ItemDatabase.definePrimaryUse( "food helper", KoLConstants.CONSUME_FOOD_HELPER );
		ItemDatabase.definePrimaryUse( "drink helper", KoLConstants.CONSUME_DRINK_HELPER );
		ItemDatabase.definePrimaryUse( "sticker", KoLConstants.CONSUME_STICKER );
		
		ItemDatabase.defineSecondaryUse( "usable", ItemDatabase.ATTR_USABLE );
		ItemDatabase.defineSecondaryUse( "multiple", ItemDatabase.ATTR_MULTIPLE );
		ItemDatabase.defineSecondaryUse( "reusable", ItemDatabase.ATTR_REUSABLE );
		ItemDatabase.defineSecondaryUse( "combat", ItemDatabase.ATTR_COMBAT );
		ItemDatabase.defineSecondaryUse( "combat reusable", ItemDatabase.ATTR_COMBAT_REUSABLE );
		ItemDatabase.defineSecondaryUse( "single", ItemDatabase.ATTR_SINGLE );
		ItemDatabase.defineSecondaryUse( "solo", ItemDatabase.ATTR_SOLO );
		ItemDatabase.defineSecondaryUse( "curse", ItemDatabase.ATTR_CURSE );
		ItemDatabase.defineSecondaryUse( "bounty", ItemDatabase.ATTR_BOUNTY );
		ItemDatabase.defineSecondaryUse( "candy", ItemDatabase.ATTR_CANDY );
		ItemDatabase.defineSecondaryUse( "matchable", ItemDatabase.ATTR_MATCHABLE );

		secondaryUsageEntrySet = INVERSE_SECONDARY_USE.entrySet();
	}

	public static boolean newItems = false;

	static
	{
		ItemDatabase.reset();
	}

	public static void reset()
	{
		ItemDatabase.newItems = false;

		if ( !ItemDatabase.itemIdByName.isEmpty() )
		{
			ItemDatabase.miniReset();
			return;
		}

		ItemDatabase.itemIdByName.clear();

		// For efficiency, figure out just once if today is a stat day
		int statDay = HolidayDatabase.statDay( new Date() );
		ItemDatabase.muscleFactor = statDay == KoLConstants.MUSCLE ? 1.25f : 1.0f;
		ItemDatabase.mysticalityFactor = statDay == KoLConstants.MYSTICALITY ? 1.25f : 1.0f;
		ItemDatabase.moxieFactor = statDay == KoLConstants.MOXIE ? 1.25f : 1.0f;

		ItemDatabase.readTradeItems();
		ItemDatabase.readItemDescriptions();

		ItemDatabase.readConsumptionData( "fullness.txt", KoLConstants.FULLNESS_VERSION, ItemDatabase.fullnessByName );
		ItemDatabase.readConsumptionData( "inebriety.txt", KoLConstants.INEBRIETY_VERSION, ItemDatabase.inebrietyByName );
		ItemDatabase.readConsumptionData( "spleenhit.txt", KoLConstants.SPLEENHIT_VERSION , ItemDatabase.spleenHitByName );
		ItemDatabase.readNonfillingData();

		ItemDatabase.readFoldGroups();

		ItemDatabase.addPseudoItems();
		ItemDatabase.saveCanonicalNames();
	}

	private static void miniReset()
	{
		ItemDatabase.itemIdByName.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( "tradeitems.txt", KoLConstants.TRADEITEMS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 5 )
			{
				continue;
			}

			int itemId = StringUtilities.parseInt( data[ 0 ] );
			String canonicalName = StringUtilities.getCanonicalName( data[1] );

			Integer id = new Integer( itemId );
			ItemDatabase.itemIdByName.put( canonicalName, id );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		ItemDatabase.addPseudoItems();
		ItemDatabase.saveCanonicalNames();
	}

	private static void readTradeItems()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "tradeitems.txt", KoLConstants.TRADEITEMS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 5 )
			{
				continue;
			}

			int itemId = StringUtilities.parseInt( data[ 0 ] );
			String name = new String( data[ 1 ] );
			String[] usages = data[ 2 ].split( "\\s*,\\s*" );
			String access = ItemDatabase.parseAccess( data[ 3 ] );
			int price = StringUtilities.parseInt( data[ 4 ] );

			Integer id = new Integer( itemId );
			String displayName = StringUtilities.getDisplayName( name );
			String canonicalName = StringUtilities.getCanonicalName( name );
			
			int attrs = 0;
			String usage = usages[ 0 ];
			Integer useType = (Integer) ItemDatabase.PRIMARY_USE.get( usage );
			if ( useType == null )
			{
				RequestLogger.printLine( "Unknown primary usage for " + name + ": " + usage );
			}
			else
			{
				ItemDatabase.useTypeById.set( itemId, useType.intValue() );
			}
			for ( int i = 1; i < usages.length; ++i )
			{
				usage = usages[ i ];
				useType = (Integer) ItemDatabase.SECONDARY_USE.get( usage );
				if ( useType == null )
				{
					RequestLogger.printLine( "Unknown secondary usage for " + name + ": " + usage );
				}
				else
				{
					attrs |= useType.intValue();
				}
			}
			
			ItemDatabase.priceById.set( itemId, price );

			ItemDatabase.dataNameById.put( id, name );
			ItemDatabase.nameById.put( id, displayName );

			ItemDatabase.accessById.put( id, access );
			attrs |= access.equals( "all" ) ? ItemDatabase.ATTR_TRADEABLE : 0;
			attrs |= access.equals( "all" ) || access.equals( "gift" ) ? ItemDatabase.ATTR_GIFTABLE : 0;
			attrs |= access.equals( "all" ) || access.equals( "gift" ) || access.equals( "display" ) ? ItemDatabase.ATTR_DISPLAYABLE : 0;
			ItemDatabase.attributesById.set( itemId, attrs );

			if ( itemId > ItemDatabase.maxItemId )
			{
				ItemDatabase.maxItemId = itemId;
			}

			ItemDatabase.itemIdByName.put( canonicalName, id );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static void writeTradeitems( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.TRADEITEMS_VERSION );

		Iterator it = ItemDatabase.nameByIdKeySet().iterator();
		int lastInteger = 1;

		while ( it.hasNext() )
		{
			Integer nextInteger = (Integer) it.next();
			int itemId = nextInteger.intValue();

			// Skip pseudo items
			if ( itemId == 13 )
			{
				continue;
			}

			for ( int i = lastInteger; i < itemId; ++i )
			{
				writer.println( i );
			}

			lastInteger = itemId + 1;

			String name = ItemDatabase.getItemDataName( nextInteger );
			int type = ItemDatabase.getConsumptionType( itemId );
			int attrs = ItemDatabase.getAttributes( itemId );
			String access = ItemDatabase.getAccessById( nextInteger );
			int price = ItemDatabase.getPriceById( itemId );
			ItemDatabase.writeTradeitem( writer, itemId, name, type, attrs, access, price );
		}

		writer.close();
	}

	public static void writeTradeitem( final PrintStream writer, final int itemId, final String name,
					   final int type, final int attrs, final String access, final int price )
	{
		writer.println( ItemDatabase.tradeitemString( itemId, name, type, attrs, access, price ) );
	}

	public static String tradeitemString( final int itemId, final String name, final int type,
					      final int attrs, final String access, final int price )
	{
		return itemId + "\t" +
		       name + "\t" +
		       typeToPrimaryUsage( type ) + attrsToSecondaryUsage( attrs ) + "\t" +
		       access + "\t" +
		       price;
	}

	public static void writeItemdescs( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.ITEMDESCS_VERSION );

		Iterator it = ItemDatabase.descriptionIdEntrySet().iterator();
		int lastInteger = 1;

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer nextInteger = (Integer) entry.getKey();
			int itemId = nextInteger.intValue();

			// Skip pseudo items
			if ( itemId == 13 )
			{
				continue;
			}

			for ( int i = lastInteger; i < itemId; ++i )
			{
				writer.println( i );
			}

			lastInteger = itemId + 1;

			String descId = (String) entry.getValue();
			String name = ItemDatabase.getItemDataName( nextInteger );
			String plural = ItemDatabase.getPluralById( itemId );
			writer.println( ItemDatabase.itemdescString( itemId, descId, name, plural ) );
		}

		writer.close();
	}

	public static String itemdescString( final int itemId, final String descId, final String name, final String plural)
	{
		return itemId + "\t" + descId + "\t" + name + ( plural.equals( "" ) ? "" : "\t" + plural );
	}

	private static void readItemDescriptions()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "itemdescs.txt", KoLConstants.ITEMDESCS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 2 )
			{
				continue;
			}

			int itemId = StringUtilities.parseInt( data[ 0 ].trim() );
			Integer id = new Integer( itemId );
			String descId = data[1];

			if ( StringUtilities.isNumeric( descId ) )
			{
				descId = new String( descId );
				ItemDatabase.descriptionById.put( id, descId );
				ItemDatabase.itemIdByDescription.put( descId, id );
			}

			if ( data.length > 3 )
			{
				String plural = new String( data[ 3 ] );
				ItemDatabase.pluralById.set( itemId, plural );
				ItemDatabase.itemIdByPlural.put( StringUtilities.getCanonicalName( plural ), id );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void readConsumptionData( String filename, int version, Map map )
	{
		BufferedReader reader = FileUtilities.getVersionedReader( filename, version );

		String[] data;
		Integer id;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			ItemDatabase.saveItemValues( data, map );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void readNonfillingData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "nonfilling.txt", KoLConstants.NONFILLING_VERSION );

		String[] data;
		Integer id;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 2 )
				continue;
	
			String name = StringUtilities.getCanonicalName( data[ 0 ] );
			ItemDatabase.levelReqByName.put( name, Integer.valueOf( data[ 1 ] ) );
	
			if ( data.length < 3 )
				continue;
	
			String notes = data[ 2 ];
			if ( notes.length() > 0 )
			{
				ItemDatabase.notesByName.put( name, notes );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void readFoldGroups()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "foldgroups.txt", KoLConstants.FOLDGROUPS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length <= 2 )
			{
				continue;
			}

			ArrayList group = new ArrayList();
			group.add( new Integer( StringUtilities.parseInt( data[ 0 ] ) ) );
			for ( int i = 1; i < data.length; ++i )
			{
				String name = StringUtilities.getCanonicalName( data[ i ] );
				if ( ItemDatabase.itemIdByName.get( name ) == null )
				{
					RequestLogger.printLine( "Unknown foldable item: " + name );
					continue;
				}
				ItemDatabase.foldGroupsByName.put( name, group );
				group.add( name );
			}
			group.trimToSize();
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static final void addPseudoItems()
	{
		Integer id = new Integer( 13 );

		ItemDatabase.dataNameById.put( id, "worthless item" );
		ItemDatabase.nameById.put( id, "worthless item" );
		ItemDatabase.itemIdByName.put( "worthless item", id );

		// Set aliases for the El Vibrato punch cards
		for ( int i = 0; i < RequestEditorKit.PUNCHCARDS.length; ++i )
		{
			Object [] punchcard = RequestEditorKit.PUNCHCARDS[i];
			id = (Integer) punchcard[0];

			String alias = StringUtilities.getCanonicalName( (String) punchcard[2] );
			itemIdByName.put( alias, id );
			String plural = StringUtilities.singleStringReplace( alias, "punchcard", "punchcards" );
			itemIdByPlural.put( plural, id );
		}

		// Add names of all the sushi
		id = new Integer( -1 );
		for ( int i = 0; i < SushiRequest.SUSHI.length; ++i )
		{
			String name = StringUtilities.getCanonicalName( SushiRequest.SUSHI[i] );
			itemIdByName.put( name, id );
		}
		
		// Miscellaneous aliases for untypeable item names
		for ( int i = 0; i < ItemDatabase.ALIASES.length; ++i )
		{
			ItemDatabase.itemIdByName.put( StringUtilities.getCanonicalName(
				(String) ItemDatabase.ALIASES[ i ][ 1 ] ),
				ItemDatabase.ALIASES[ i ][ 0 ] );
		}
	}

	private static final void saveCanonicalNames()
	{
		ItemDatabase.canonicalNames = new String[ ItemDatabase.itemIdByName.size() ];
		ItemDatabase.itemIdByName.keySet().toArray( ItemDatabase.canonicalNames );
		Arrays.sort( ItemDatabase.canonicalNames );
	}

	private static final void saveItemValues( String[] data, Map map )
	{
		if ( data.length < 2 )
			return;

		String name = StringUtilities.getCanonicalName( data[ 0 ] );
		map.put( name, Integer.valueOf( data[ 1 ] ) );

		if ( data.length < 7 )
			return;

		ItemDatabase.levelReqByName.put( name, Integer.valueOf( data[ 2 ] ) );
		ItemDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), data[ 3 ] );
		ItemDatabase.muscleByName.put( name, ItemDatabase.extractStatRange( data[ 4 ], ItemDatabase.muscleFactor ) );
		ItemDatabase.mysticalityByName.put( name, ItemDatabase.extractStatRange( data[ 5 ], ItemDatabase.mysticalityFactor ) );
		ItemDatabase.moxieByName.put( name, ItemDatabase.extractStatRange( data[ 6 ], ItemDatabase.moxieFactor ) );

		if ( data.length < 8 )
			return;

		String notes = data[ 7 ];
		if ( notes.length() > 0 )
		{
			ItemDatabase.notesByName.put( name, notes );
		}
	}

	private static final int getIncreasingGains( final int value )
	{
		// Adventure gains from Ode/Milk based on information
		// derived by Istari Asuka on the Hardcore Oxygenation forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=2321

		switch ( value )
		{
		case 0:
			return 0;

		case 1:
		case 2:
		case 3:
		case 4:
			return 1;

		case 5:
		case 6:
		case 7:
			return 2;

		case 8:
		case 9:
		case 10:
			return 3;

		default:
			return 4;
		}
	}

	private static final int getDecreasingGains( final int value )
	{
		// Adventure gains from Ode/Milk based on information
		// derived by Istari Asuka on the Hardcore Oxygenation forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=2321

		switch ( value )
		{
		case 0:
			return 0;

		case 1:
		case 2:
		case 3:
			return 3;

		case 4:
		case 5:
		case 6:
			return 2;

		default:
			return 1;
		}
	}

	private static final void saveAdventureRange( final String name, final int unitCost, String range )
	{
		range = range.trim();

		int dashIndex = range.indexOf( "-" );
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );
		int end = dashIndex == -1 ? start : StringUtilities.parseInt( range.substring( dashIndex + 1 ) );

		ItemDatabase.unitCostByName.put( name, new Integer( unitCost ) );
		ItemDatabase.advStartByName.put( name, new Integer( start ) );
		ItemDatabase.advEndByName.put( name, new Integer( end ) );
		ItemDatabase.advNames = null;
	}

	public static final void calculateAdventureRanges()
	{
		if ( ItemDatabase.advNames == null )
		{
			ItemDatabase.advNames = ItemDatabase.unitCostByName.keySet();
		}

		Iterator it = ItemDatabase.advNames.iterator();

		while ( it.hasNext() )
		{
			String name = (String) it.next();
			ItemDatabase.calculateAdventureRange( name );
		}
	}

	private static final void calculateAdventureRange( final String name )
	{
		Concoction c = ConcoctionPool.get( name );
		int advs = ( c == null ) ? 0 : c.getAdventuresNeeded( 1 );

		int unitCost = ( (Integer) ItemDatabase.unitCostByName.get( name ) ).intValue();
		int start = ( (Integer) ItemDatabase.advStartByName.get( name ) ).intValue();
		int end = ( (Integer) ItemDatabase.advEndByName.get( name ) ).intValue();

		int gainSum1 = 0;
		int gainSum2 = 0;
		int gainSum3 = 0;

		for ( int i = start; i <= end; ++i )
		{
			gainSum1 += i + ItemDatabase.getIncreasingGains( i );
			gainSum2 += i + ItemDatabase.getDecreasingGains( i );
			gainSum3 +=
				i + ItemDatabase.getIncreasingGains( i + ItemDatabase.getDecreasingGains( i ) );
		}

		float count = end - start + 1;

		ItemDatabase.addAdventureRange( name, unitCost, false, false, ( start + end ) / 2.0f - advs );
		ItemDatabase.addAdventureRange( name, unitCost, true, false, gainSum1 / count - advs );
		ItemDatabase.addAdventureRange( name, unitCost, false, true, gainSum2 / count - advs );
		ItemDatabase.addAdventureRange( name, unitCost, true, true, gainSum3 / count - advs );
	}

	private static final void addAdventureRange( final String name, final int unitCost, final boolean gainEffect1, final boolean gainEffect2, final float result )
	{
                // Remove adventure gains from zodiac signs
		ItemDatabase.getAdventureMap( false, gainEffect1, gainEffect2 ).put( name, new Float( result ) );
		ItemDatabase.getAdventureMap( true, gainEffect1, gainEffect2 ).put( name, new Float( result / unitCost ) );
	}

	private static final Map getAdventureMap( final boolean perUnit,
		final boolean gainEffect1, final boolean gainEffect2 )
	{
		return ItemDatabase.advsByName[ perUnit ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ];
	}

	private static final String extractStatRange( String range, float statFactor )
	{
		range = range.trim();

		boolean isNegative = range.startsWith( "-" );
		if ( isNegative )
		{
			range = range.substring( 1 );
		}

		int dashIndex = range.indexOf( "-" );
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );

		if ( dashIndex == -1 )
		{
			float num = isNegative ? 0 - start : start;
			return KoLConstants.SINGLE_PRECISION_FORMAT.format( statFactor * num );
		}

		int end = StringUtilities.parseInt( range.substring( dashIndex + 1 ) );
		float num = ( start + end ) / ( isNegative ? -2.0f : 2.0f );
		return KoLConstants.SINGLE_PRECISION_FORMAT.format( statFactor * num );
	}

	/**
	 * Temporarily adds an item to the item database. This is used whenever KoLmafia encounters an unknown item in the
	 * mall or in the player's inventory.
	 */

	private static Pattern RELSTRING_PATTERN = Pattern.compile( "([\\w]+)=([^&]*)&?");

	// "id=588&s=118&q=0&d=1&g=0&t=1&n=50&m=0&u=.&ou=use"
	//   id = item Id
	//   s = sell value
	//   q = quest item
	//   d = discardable
	//   g = gift item
	//   t = transferable
	//   n = number
	//   m = multiusable
	//   u = how can this be used?
	//	 e = Eatable (food/drink) (inv_eat)
	//	 b = Booze (inv_booze)
	//	 q = eQuipable (inv_equip)
	//	 u = potion/Useable (inv_use/multiuse)
	//	 . = can't (or doesn't fit those types)
	//  ou = "other use text" which is used to make drinks show
	//	 "drink" instead of "eat" and for items which over-ride
	//	 the word "use" in links, like the PYEC or scratch 'n'
	//	 sniff stickers.

	public static final AdventureResult itemFromRelString( final String relString )
	{
		int itemId = -1;
		int count = 1;

		Matcher matcher = RELSTRING_PATTERN.matcher( relString );
		while ( matcher.find() )
		{
			String tag = matcher.group(1);
			String value = matcher.group(2);
			if ( tag.equals( "id" ) )
			{
				itemId = StringUtilities.parseInt( value );
			}
			else if ( tag.equals( "n" ) )
			{
				count = StringUtilities.parseInt( value );
			}
		}

		return ItemPool.get( itemId, count );
	}

	public static final String relStringValue( final String relString, final String search )
	{
		Matcher matcher = RELSTRING_PATTERN.matcher( relString );
		while ( matcher.find() )
		{
			String tag = matcher.group(1);
			String value = matcher.group(2);
			if ( tag.equals( search ) )
			{
				return value;
			}
		}

		return null;
	}

	public static final int relStringNumericValue( final String relString, final String search )
	{
		String value = ItemDatabase.relStringValue( relString, search );
		return value != null ? StringUtilities.parseInt( value ) : -1;
	}

	public static final int relStringItemId( final String relString )
	{
		return ItemDatabase.relStringNumericValue( relString, "id" );
	}

	public static final int relStringCount( final String relString )
	{
		return ItemDatabase.relStringNumericValue( relString, "n" );
	}

	public static final boolean relStringMultiusable( final String relString )
	{
		String value = ItemDatabase.relStringValue( relString, "m" );
		return value != null && value.equals( "1" );
	}

	public static final void registerItem( final String itemName, final String descId, final String relString, final String items )
	{
		int itemId = -1;
		int count = 1;

		Matcher matcher = RELSTRING_PATTERN.matcher( relString );
		while ( matcher.find() )
		{
			String tag = matcher.group(1);
			String value = matcher.group(2);
			if ( tag.equals( "id" ) )
			{
				itemId = StringUtilities.parseInt( value );
			}
			else if ( tag.equals( "n" ) )
			{
				count = StringUtilities.parseInt( value );
			}
		}

		// If we could not find the item id, nothing to do
		if ( itemId < 0 )
		{
			return;
		}

		// If we found more than one item and the "items" string is not
		// null, we probably have the plural.
		String plural = null;
		if ( count > 1 && items != null )
		{
			int space = items.indexOf( " " );
			if ( space != -1 )
			{
				String num = items.substring( 0, space );
				if ( StringUtilities.isNumeric( num ) &&
				     StringUtilities.parseInt( num ) == count )
				{
					plural = items.substring( space + 1 );
				}
			}
		}

		ItemDatabase.registerItem( itemId, itemName, descId, plural );
	}

	public static final void registerItem( final String itemName, final String descId, final String relString )
	{
		int itemId = ItemDatabase.relStringItemId( relString );
		if ( itemId > 0 )
		{
			ItemDatabase.registerItem( itemId, itemName, descId, null );
		}
	}

	public static final void registerItem( final int itemId, String itemName, String descId )
	{
		ItemDatabase.registerItem( itemId, itemName, descId, null );
	}

	public static final void registerItem( final int itemId, String itemName, String descId, final String plural )
	{
		if ( itemName == null )
		{
			return;
		}

		// Detach item name and descid from being substrings
		itemName = new String( itemName );
		descId = new String( descId );

		// Remember that a new item has been discovered
		ItemDatabase.newItems = true;

		RequestLogger.printLine( "Unknown item found: " + itemName + " (" + itemId + ", " + descId + ")" );

		Integer id = new Integer( itemId );

		ItemDatabase.descriptionById.put( id, descId );
		ItemDatabase.itemIdByDescription.put( descId, id );
		ItemDatabase.dataNameById.put( id, itemName );
		ItemDatabase.nameById.put( id, StringUtilities.getDisplayName( itemName ) );
		ItemDatabase.registerItemAlias( itemId, itemName, null );
		if ( plural != null )
		{
			ItemDatabase.pluralById.set( itemId, plural );
			ItemDatabase.itemIdByPlural.put( StringUtilities.getCanonicalName( plural ), id );
		}
		ItemDatabase.parseItemDescription( id, itemName );
	}

	private static void parseItemDescription( final Integer id, final String itemName )
	{
		String descId = ItemDatabase.getDescriptionId( id );
		int itemId = id.intValue();

		String text = DebugDatabase.itemDescriptionText( itemId );
		if ( text == null )
		{
			// Assume defaults
			ItemDatabase.useTypeById.set( itemId, KoLConstants.NO_CONSUME );
			ItemDatabase.attributesById.set( itemId, 0 );
			ItemDatabase.accessById.put( id, "all" );
			ItemDatabase.priceById.set( itemId, 0 );
			return;
		}

		// Parse use type, access, and price from description
		String type = DebugDatabase.parseType( text );
		int usage = DebugDatabase.typeToPrimary( type );
		ItemDatabase.useTypeById.set( itemId, usage );

		String access = DebugDatabase.parseAccess( text );
		ItemDatabase.accessById.put( id, access );

		int attrs = DebugDatabase.typeToSecondary( type );
		attrs |= access.equals( "all" ) ? ItemDatabase.ATTR_TRADEABLE : 0;
		attrs |= access.equals( "all" ) || access.equals( "gift" ) ? ItemDatabase.ATTR_GIFTABLE : 0;
		attrs |= access.equals( "all" ) || access.equals( "gift" ) || access.equals( "display" ) ? ItemDatabase.ATTR_DISPLAYABLE : 0;
		ItemDatabase.attributesById.set( itemId, attrs );

		int price = DebugDatabase.parsePrice( text );
		ItemDatabase.priceById.set( itemId, price );

		// Print what goes in tradeitems.txt and itemdescs.txt
		RequestLogger.printLine( "--------------------" );
		RequestLogger.printLine( ItemDatabase.tradeitemString( itemId, itemName, usage, attrs, access, price ) );

		String plural = ItemDatabase.getPluralById( itemId );
		RequestLogger.printLine( ItemDatabase.itemdescString( itemId, descId, itemName, plural ) );

		if ( EquipmentDatabase.isEquipment( usage ) )
		{
			EquipmentDatabase.newEquipment = true;

			// Let equipment database do what it wishes with this item
			EquipmentDatabase.registerItem( itemId, itemName, text );
		}

		// Let modifiers database do what it wishes with this item
		Modifiers.registerItem( itemName, text );

		// Done generating data
		RequestLogger.printLine( "--------------------" );
	}

	public static void registerItemAlias( final int itemId, final String itemName, final String plural )
	{
		Integer id = new Integer( itemId );
		ItemDatabase.itemIdByName.put( StringUtilities.getCanonicalName( itemName ), id );

		ItemDatabase.canonicalNames = new String[ ItemDatabase.itemIdByName.size() ];
		ItemDatabase.itemIdByName.keySet().toArray( ItemDatabase.canonicalNames );
		Arrays.sort( ItemDatabase.canonicalNames );
		if ( plural != null )
		{
			ItemDatabase.itemIdByPlural.put( StringUtilities.getCanonicalName( plural ), id );
		}
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName )
	{
		return ItemDatabase.getItemId( itemName, 1 );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @param count How many there are
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName, final int count )
	{
		return getItemId( itemName, count, true );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 *
	 * @param itemName The name of the item to lookup
	 * @param count How many there are
	 * @param substringMatch Whether or not we match against substrings
	 * @return The Id number of the corresponding item
	 */

	public static final int getItemId( final String itemName, final int count, final boolean substringMatch )
	{
		String name = ItemDatabase.getCanonicalName( itemName, count, substringMatch );
		if ( name != null )
		{
			Object itemId = ItemDatabase.itemIdByName.get( name );
			return ( (Integer) itemId ).intValue();
		}

		return -1;
	}

	public static String getCanonicalName( final int itemId )
	{
		return ItemDatabase.getCanonicalName( new Integer( itemId ) );
	}

	public static String getCanonicalName( final Integer itemId )
	{
		return StringUtilities.getCanonicalName( (String) ItemDatabase.nameById.get( itemId ) );
	}

	public static final String getCanonicalName( final String itemName )
	{
		return ItemDatabase.getCanonicalName( itemName, 1 );
	}

	public static final String getCanonicalName( final String itemName, final int count )
	{
		return ItemDatabase.getCanonicalName( itemName, count, true );
	}

	public static final String getCanonicalName( final String itemName, final int count, final boolean substringMatch )
	{
		if ( itemName == null || itemName.length() == 0 )
		{
			return null;
		}

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = StringUtilities.getCanonicalName( itemName );
		Object itemId;

		// See if it's a weird pluralization with a pattern we can't
		// guess before checking for singles.

		if ( count > 1 )
		{
			itemId = ItemDatabase.itemIdByPlural.get( canonicalName );
			if ( itemId != null )
			{
				return ItemDatabase.getCanonicalName( (Integer) itemId );
			}
		}

		itemId = ItemDatabase.itemIdByName.get( canonicalName );

		// If the name, as-is, exists in the item database, return it

		if ( itemId != null )
		{
			return canonicalName;
		}

		// Work around specific KoL bugs:

		// "less-than-three- shaped box" -> "less-than-three-shaped box"
		if ( canonicalName.equals( "less-than-three- shaped box" ) )
		{
			return "less-than-three-shaped box";
		}

		if ( !substringMatch )
		{
			return null;
		}

		// It's possible that you're looking for a substring.  In
		// that case, prefer complete versions containing the substring
		// over truncated versions which are plurals.

		List possibilities = StringUtilities.getMatchingNames( ItemDatabase.canonicalNames, canonicalName );
		if ( possibilities.size() == 1 )
		{
			return (String) possibilities.get( 0 );
		}

		// Abort if it's clearly not going to be a plural,
		// since this might kill off multi-item detection.

		if ( count == 1 )
		{
			return null;
		}

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
		{
			return ItemDatabase.getCanonicalName( canonicalName.split( " " )[ 1 ] + " snowcone", count );
		}

		// Lo mein has this odd pluralization where there's a dash
		// introduced into the name when no such dash exists in the
		// singular form.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "-", " " ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "aves", "aff" ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a pluralized form of something that
		// ends with "y", then return the appropriate
		// item Id for the "y" version.

		if ( canonicalName.endsWith( "ies" ) )
		{
			itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemId != null )
			{
				return ItemDatabase.getCanonicalName( (Integer) itemId );
			}
		}

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item Id for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemId != null )
			{
				return ItemDatabase.getCanonicalName( (Integer) itemId );
			}
		}

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item Id for the "en" version.

		itemId = ItemDatabase.itemIdByName.get( StringUtilities.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's a standard pluralized forms, then
		// return the appropriate item Id.

		itemId = ItemDatabase.itemIdByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemId != null )
		{
			return ItemDatabase.getCanonicalName( (Integer) itemId );
		}

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemId = ItemDatabase.itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemId != null )
			{
				return ItemDatabase.getCanonicalName( (Integer) itemId );
			}
		}

		// Unknown item

		return null;
	}

	/**
	 * Returns the plural for an item, given its Id number
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The plural name of the corresponding item
	 */

	public static final String getPluralName( final int itemId )
	{
		String plural = pluralById.get( itemId );
		return plural == null ? ItemDatabase.getItemName( itemId ) : plural;
	}

	public static final String getPluralById( final int itemId )
	{
		return pluralById.get( itemId );
	}

	public static final Integer getLevelReqByName( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return (Integer) ItemDatabase.levelReqByName.get( StringUtilities.getCanonicalName( name ) );
	}

	public static final boolean meetsLevelRequirement( final String name )
	{
		if ( name == null )
		{
			return false;
		}

		Integer requirement = (Integer) ItemDatabase.levelReqByName.get( StringUtilities.getCanonicalName( name ) );
		return requirement == null ? true : KoLCharacter.getLevel() >= requirement.intValue();
	}

	public static final int getFullness( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer fullness = (Integer) ItemDatabase.fullnessByName.get( StringUtilities.getCanonicalName( name ) );
		return fullness == null ? 0 : fullness.intValue();
	}

	public static final int getInebriety( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer inebriety = (Integer) ItemDatabase.inebrietyByName.get( StringUtilities.getCanonicalName( name ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	public static final int getSpleenHit( final String name )
	{
		if ( name == null )
		{
			return 0;
		}

		Integer spleenhit = (Integer) ItemDatabase.spleenHitByName.get( StringUtilities.getCanonicalName( name ) );
		return spleenhit == null ? 0 : spleenhit.intValue();
	}

	public static final String getNotes( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return (String) ItemDatabase.notesByName.get( StringUtilities.getCanonicalName( name ) );
	}

	public static final ArrayList getFoldGroup( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return (ArrayList) ItemDatabase.foldGroupsByName.get( StringUtilities.getCanonicalName( name ) );
	}

	public static final float getAdventureRange( final String name )
	{
		if ( name == null )
		{
			return 0.0f;
		}

		String cname = StringUtilities.getCanonicalName( name );
		boolean perUnit = Preferences.getBoolean( "showGainsPerUnit" );
		Float range = null;

		if ( ItemDatabase.getFullness( name ) > 0 )
		{
			boolean sushi = (ConcoctionDatabase.getMixingMethod( cname ) & KoLConstants.CT_MASK) == KoLConstants.SUSHI;
			boolean milkEffect = !sushi && KoLConstants.activeEffects.contains( ItemDatabase.MILK );
			boolean munchiesEffect = !sushi && Preferences.getInteger( "munchiesPillsUsed" ) > 0;
			range = (Float) ItemDatabase.getAdventureMap(
				perUnit, milkEffect, munchiesEffect ).get( cname );
		}
		else if ( ItemDatabase.getInebriety( name ) > 0 )
		{
			boolean odeEffect = KoLConstants.activeEffects.contains( ItemDatabase.ODE );
			range = (Float) ItemDatabase.getAdventureMap(
				perUnit, odeEffect, false ).get( cname );
		}
		else if ( ItemDatabase.getSpleenHit( name ) > 0 )
		{
			range = (Float) ItemDatabase.getAdventureMap(
				perUnit, false, false ).get( cname );
		}

		if ( range == null )
		{
			return 0.0f;
		}

		return range.floatValue();
	}

	public static final String getMuscleRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) ItemDatabase.muscleByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMysticalityRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) ItemDatabase.mysticalityByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	public static final String getMoxieRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String range = (String) ItemDatabase.moxieByName.get( StringUtilities.getCanonicalName( name ) );
		return range == null ? "+0.0" : range;
	}

	/**
	 * Returns the price for the item with the given Id.
	 *
	 * @return The price associated with the item
	 */

	public static final int getPriceById( final int itemId )
	{
		return ItemDatabase.priceById.get( itemId );
	}

	/**
	 * Returns the access for the item with the given Id.
	 *
	 * @return The access associated with the item
	 */

	public static final String getAccessById( final Integer itemId )
	{
		return (String) ItemDatabase.accessById.get( itemId );
	}
	
	public static final int getAttributes( int itemId )
	{
		return ItemDatabase.attributesById.get( itemId );
	}

	public static final String attrsToSecondaryUsage( int attrs )
	{
		// Mask out attributes which are part of access
		attrs &= ~( ATTR_TRADEABLE|ATTR_GIFTABLE|ATTR_DISPLAYABLE );

		// If there are no other attributes, return empty string
		if ( attrs == 0 )
		{
			return "";
		}

		// Otherwise, iterate over bits
		StringBuffer result = new StringBuffer();
		Iterator it = ItemDatabase.secondaryUsageEntrySet.iterator();

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer bit = (Integer) entry.getKey();

			if ( ( attrs & bit.intValue() ) != 0 )
			{
				result.append( ", " );
				result.append( (String) entry.getValue() );
			}
		}

		return result.toString();
	}

	public static final boolean getAttribute( int itemId, int mask )
	{
		return (ItemDatabase.attributesById.get( itemId ) & mask) != 0;
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isTradeable( final int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_TRADEABLE );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isGiftable( final int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_GIFTABLE );
	}

	/**
	 * Returns true if the item is giftable, otherwise false
	 *
	 * @return true if item is tradeable
	 */

	public static final boolean isDisplayable( final int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_DISPLAYABLE );
	}

	/**
	 * Returns true if the item is a bounty, otherwise false
	 *
	 * @return true if item is a bounty
	 */

	public static final boolean isBountyItem( final int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_BOUNTY );
	}

	/**
	 * Returns the name for an item, given its Id number.
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The name of the corresponding item
	 */

	public static final String getItemName( final int itemId )
	{
		return (String) ItemDatabase.nameById.get( new Integer( itemId ) );
	}

	public static final String getItemDataName( final int itemId )
	{
		return (String) ItemDatabase.dataNameById.get( new Integer( itemId ) );
	}

	public static final String getItemDataName( final Integer itemId )
	{
		return (String) ItemDatabase.dataNameById.get( itemId );
	}

	public static final Set dataNameEntrySet()
	{
		return ItemDatabase.dataNameById.entrySet();
	}

	/**
	 * Returns the name for an item, given its Id number.
	 *
	 * @param itemId The Id number of the item to lookup
	 * @return The name of the corresponding item
	 */

	public static final String getItemName( final String descriptionId )
	{
		Integer itemId = (Integer) ItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? null : ItemDatabase.getItemName( itemId.intValue() );
	}

	/**
	 * Returns the id for an item, given its description id number.
	 *
	 * @param itemId The description id number of the item to lookup
	 * @return The item id of the corresponding item
	 */

	public static final int getItemIdFromDescription( final String descriptionId )
	{
		Integer itemId = (Integer) ItemDatabase.itemIdByDescription.get( descriptionId );
		return itemId == null ? -1 : itemId.intValue();
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( ItemDatabase.canonicalNames, substring );
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String itemName )
	{
		return ItemDatabase.getItemId( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name is usable (this includes edibility).
	 *
	 * @return <code>true</code> if the item is usable
	 */

	public static final boolean isUsable( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName );
		if ( itemId <= 0 )
		{
			return false;
		}

		switch ( ItemDatabase.useTypeById.get( itemId ) )
		{
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
		case KoLConstants.CONSUME_USE:
		case KoLConstants.MESSAGE_DISPLAY:
		case KoLConstants.INFINITE_USES:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.GROW_FAMILIAR:
		case KoLConstants.CONSUME_ZAP:
		case KoLConstants.MP_RESTORE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns whether or not the item with the given name is made of
	 * grimacite and is thus affected by the moon phases.
	 *
	 * @return <code>true</code> if the item is grimacite
	 */

	public static final boolean isGrimacite( int itemId )
	{
		switch ( itemId )
		{
			// Grimacite Generation 1
		case ItemPool.GRIMACITE_GALOSHES:
		case ItemPool.GRIMACITE_GARTER:
		case ItemPool.GRIMACITE_GORGET:
		case ItemPool.GRIMACITE_GREAVES:
		case ItemPool.GRIMACITE_GUAYABERA:
		case ItemPool.GRIMACITE_GOGGLES:
		case ItemPool.GRIMACITE_GLAIVE:
			// Grimacite Generation 2
		case ItemPool.GRIMACITE_GASMASK:
		case ItemPool.GRIMACITE_GAT:
		case ItemPool.GRIMACITE_GIRDLE:
		case ItemPool.GRIMACITE_GO_GO_BOOTS:
		case ItemPool.GRIMACITE_GAUNTLETS:
		case ItemPool.GRIMACITE_GAITERS:
		case ItemPool.GRIMACITE_GOWN:
			// Depleted Grimacite
		case ItemPool.GRIMACITE_HAMMER:
		case ItemPool.GRIMACITE_GRAVY_BOAT:
		case ItemPool.GRIMACITE_WEIGHTLIFTING_BELT:
		case ItemPool.GRIMACITE_GRAPPLING_HOOK:
		case ItemPool.GRIMACITE_NINJA_MASK:
		case ItemPool.GRIMACITE_SHINGUARDS:
		case ItemPool.GRIMACITE_ASTROLABE:
		case ItemPool.GRIMACITE_KNEECAPPING_STICK:
			return true;
		}

		return false;
	}

	public static final boolean isSealFigurine( final int itemId )
	{
		switch (itemId )
		{
		case ItemPool.WRETCHED_SEAL:
		case ItemPool.CUTE_BABY_SEAL:
		case ItemPool.ARMORED_SEAL:
		case ItemPool.ANCIENT_SEAL:
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
		case ItemPool.DEPLETED_URANIUM_SEAL:
			return true;
		}
		return false;
	}

	public static final boolean isBRICKOMonster( final int itemId )
	{
		switch (itemId )
		{
		case ItemPool.BRICKO_OOZE:
		case ItemPool.BRICKO_BAT:
		case ItemPool.BRICKO_OYSTER:
		case ItemPool.BRICKO_TURTLE:
		case ItemPool.BRICKO_ELEPHANT:
		case ItemPool.BRICKO_OCTOPUS:
		case ItemPool.BRICKO_PYTHON:
		case ItemPool.BRICKO_VACUUM_CLEANER:
			return true;
		}
		return false;
	}

	public static final boolean isStinkyCheeseItem( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.STINKY_CHEESE_SWORD:
		case ItemPool.STINKY_CHEESE_DIAPER:
		case ItemPool.STINKY_CHEESE_WHEEL:
		case ItemPool.STINKY_CHEESE_EYE:
		case ItemPool.STINKY_CHEESE_STAFF:
			return true;
		}
		return false;
	}

	/**
	 * Returns the kind of consumption associated with an item
	 *
	 * @return The consumption associated with the item
	 */

	public static final int getConsumptionType( final int itemId )
	{
		return itemId <= 0 ? KoLConstants.NO_CONSUME : ItemDatabase.useTypeById.get( itemId );
	}

	public static final String typeToPrimaryUsage( final int type )
	{
		return (String) ItemDatabase.INVERSE_PRIMARY_USE.get( new Integer( type ) );
	}

	public static final int getConsumptionType( final String itemName )
	{
		return ItemDatabase.getConsumptionType( ItemDatabase.getItemId( itemName ) );
	}

	/**
	 * Returns the item description Id used by the given item, given its item Id.
	 *
	 * @return The description Id associated with the item
	 */

	public static final String getDescriptionId( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName, 1, false );
		return ItemDatabase.getDescriptionId( itemId );
	}

	public static final String getDescriptionId( final int itemId )
	{
		return ItemDatabase.getDescriptionId( new Integer( itemId ) );
	}

	public static final String getDescriptionId( final Integer itemId )
	{
		return (String) ItemDatabase.descriptionById.get( itemId );
	}

	public static final Set nameByIdKeySet()
	{
		return ItemDatabase.nameById.keySet();
	}

	public static final Set descriptionIdKeySet()
	{
		return ItemDatabase.descriptionById.keySet();
	}

	public static final Set descriptionIdEntrySet()
	{
		return ItemDatabase.descriptionById.entrySet();
	}

	/**
	 * Returns the set of item names keyed by id
	 *
	 * @return The set of item names keyed by id
	 */

	public static final Set entrySet()
	{
		return ItemDatabase.nameById.entrySet();
	}

	/**
	 * Returns the largest item ID
	 *
	 * @return The largest item ID
	 */

	public static final int maxItemId()
	{
		return ItemDatabase.maxItemId;
	}

	// Support for dusty bottles of wine

	public static final void identifyDustyBottles()
	{
		int lastAscension = Preferences.getInteger( "lastDustyBottleReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Identifying dusty bottles..." );

		// Identify the six dusty bottles

		for ( int i = 2271; i <= 2276; ++i )
		{
			if ( !ItemDatabase.identifyDustyBottle( i ) )
			{
				return;	// don't mark data valid if identification failed!
			}
		}

		Preferences.setInteger( "lastDustyBottleReset", KoLCharacter.getAscensions() );

		// Set the consumption data

		ItemDatabase.setDustyBottles();
	}

	private static final Pattern GLYPH_PATTERN = Pattern.compile( "Arcane Glyph #(\\d)" );

	private static final boolean identifyDustyBottle( final int itemId )
	{
		String glyph = "";

		String description = DebugDatabase.rawItemDescriptionText( itemId, true );
		if ( description == null )
		{
			return false;
		}
		Matcher matcher = ItemDatabase.GLYPH_PATTERN.matcher( description );
		if ( !matcher.find() )
		{
			return false;
		}
		glyph = matcher.group( 1 );

		Preferences.setString( "lastDustyBottle" + itemId, glyph );
		return true;
	}

	public static final void getDustyBottles()
	{
		int lastAscension = Preferences.getInteger( "lastDustyBottleReset" );
		int current = KoLCharacter.getAscensions();
		if ( lastAscension < current )
		{
			if ( (current > 0 && InventoryManager.hasItem( ItemPool.SPOOKYRAVEN_SPECTACLES )) ||
				(current == 0 && KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPOOKYRAVEN_SPECTACLES, 1 ) )) )
			{
				ItemDatabase.identifyDustyBottles();
				return;
			}

			for ( int i = 2271; i <= 2276; ++i )
			{
				Preferences.setString( "lastDustyBottle" + i, "" );
			}
		}

		ItemDatabase.setDustyBottles();
	}

	private static final void setDustyBottles()
	{
		ItemDatabase.setDustyBottle( 2271 );
		ItemDatabase.setDustyBottle( 2272 );
		ItemDatabase.setDustyBottle( 2273 );
		ItemDatabase.setDustyBottle( 2274 );
		ItemDatabase.setDustyBottle( 2275 );
		ItemDatabase.setDustyBottle( 2276 );
	}

	private static final void setDustyBottle( final int itemId )
	{
		int glyph = Preferences.getInteger( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 0:
			// Unidentified
			ItemDatabase.setDustyBottle( itemId, 2, "0", "0", "0", "0", null );
			break;
		case 1: // "Prince"
			// "You drink the wine. You've had better, but you've
			// had worse."
			ItemDatabase.setDustyBottle( itemId, 2, "4-5", "6-8", "5-7", "5-9", null );
			break;
		case 2:
			// "You guzzle the entire bottle of wine before you
			// realize that it has turned into vinegar. Bleeah."
			ItemDatabase.setDustyBottle( itemId, 0, "0", "0", "0", "0",
				"10 Full of Vinegar (+weapon damage)" );
			break;
		case 3: // "Widget"
			// "You drink the bottle of wine, then belch up a cloud
			// of foul-smelling green smoke. Looks like this wine
			// was infused with wormwood. Spoooooooky."
			ItemDatabase.setDustyBottle( itemId, 2, "3-4", "3-6", "15-18", "3-6",
				"10 Kiss of the Black Fairy (+spooky damage)" );
			break;
		case 4: // "Snake"
			// "This wine is fantastic! You gulp down the entire
			// bottle, and feel great!"
			ItemDatabase.setDustyBottle( itemId, 2, "5-7", "10-15", "10-15", "10-15", null );
			break;
		case 5: // "Pitchfork"
			// "You drink the wine. It tastes pretty good, but when
			// you get to the bottom, it's full of sediment, which
			// turns out to be powdered glass. Ow."
			ItemDatabase.setDustyBottle( itemId, 2, "4-5", "7-10", "5-7", "8-10",
				"lose 60-70% HP" );
			break;
		case 6:
			// "You drink the wine, but it seems to have gone
			// bad. Not in the "turned to vinegar" sense, but the
			// "turned to crime" sense. It perpetrates some
			// violence against you on the inside."
			ItemDatabase.setDustyBottle( itemId, 2, "0-1", "0", "0", "0",
				"lose 80-90% HP" );
			break;
		}
	}

	private static final void setDustyBottle( final int itemId, final int inebriety, final String adventures,
		final String muscle, final String mysticality, final String moxie, final String note )
	{
		String name =
			StringUtilities.getCanonicalName( (String) ItemDatabase.dataNameById.get( new Integer( itemId ) ) );

		ItemDatabase.inebrietyByName.put( name, new Integer( inebriety ) );
		ItemDatabase.saveAdventureRange( name, inebriety, adventures );
		ItemDatabase.calculateAdventureRange( name );
		ItemDatabase.muscleByName.put( name, ItemDatabase.extractStatRange( muscle, ItemDatabase.muscleFactor ) );
		ItemDatabase.mysticalityByName.put( name, ItemDatabase.extractStatRange( mysticality, ItemDatabase.mysticalityFactor ) );
		ItemDatabase.moxieByName.put( name, ItemDatabase.extractStatRange( moxie, ItemDatabase.moxieFactor ) );
		ItemDatabase.notesByName.put( name, note );
	}

	public static final String dustyBottleType( final int itemId )
	{
		int glyph = Preferences.getInteger( "lastDustyBottle" + itemId );
		switch ( glyph )
		{
		case 1:
			return "average wine";
		case 2:
			return "vinegar (Full of Vinegar)";
		case 3:
			return "spooky wine (Kiss of the Black Fairy)";
		case 4:
			return "great wine";
		case 5:
			return "glassy wine";
		case 6:
			return "bad wine";
		}
		return "";
	}
}
