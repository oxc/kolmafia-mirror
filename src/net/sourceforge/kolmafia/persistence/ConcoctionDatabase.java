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

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingMisc;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.CrimboCafeRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.HellKitchenRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.swingui.ItemManageFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.SortedListModelArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionDatabase
{
	private static final SortedListModel EMPTY_LIST = new SortedListModel();
	private static final SortedListModel creatableList = new SortedListModel();
	private static final LockableListModel usableList = new LockableListModel();

	public static String excuse;	// reason why creation is impossible

	private static boolean refreshNeeded = true;
	private static boolean recalculateAdventureRange = false;
	public static int refreshLevel = 0;

	public static int queuedAdventuresUsed = 0;
	public static int queuedFreeCraftingTurns = 0;
	public static int queuedStillsUsed = 0;
	public static int queuedTomesUsed = 0;
	public static int queuedPullsUsed = 0;
	public static int queuedMeatSpent = 0;

	private static int queuedFullness = 0;
	private static final Stack queuedFoodChanges = new Stack();
	private static final SortedListModel queuedFoodIngredients = new SortedListModel();

	private static int queuedInebriety = 0;
	private static final Stack queuedBoozeChanges = new Stack();
	private static final SortedListModel queuedBoozeIngredients = new SortedListModel();

	private static int queuedSpleenHit = 0;
	private static final Stack queuedSpleenChanges = new Stack();
	private static final SortedListModel queuedSpleenIngredients = new SortedListModel();

	public static final Concoction stillsLimit = new Concoction( (AdventureResult) null, CraftingType.NOCREATE );
	public static final Concoction clipArtLimit = new Concoction( (AdventureResult) null, CraftingType.NOCREATE );
	public static final Concoction adventureLimit = new Concoction( (AdventureResult) null, CraftingType.NOCREATE );
	public static final Concoction turnFreeLimit = new Concoction( (AdventureResult) null, CraftingType.NOCREATE );
	public static final Concoction meatLimit = new Concoction( (AdventureResult) null, CraftingType.NOCREATE );

	public static final SortedListModelArray knownUses = new SortedListModelArray();

	public static final EnumSet<CraftingType> PERMIT_METHOD = EnumSet.noneOf(CraftingType.class);
	public static final Map<CraftingType, Integer> ADVENTURE_USAGE = new EnumMap<CraftingType, Integer>(CraftingType.class);
	public static final Map<CraftingType, Integer> CREATION_COST = new EnumMap<CraftingType, Integer>(CraftingType.class);
	public static final Map<CraftingType, String> EXCUSE = new EnumMap<CraftingType, String>(CraftingType.class);
	public static final EnumSet<CraftingRequirements> REQUIREMENT_MET = EnumSet.noneOf(CraftingRequirements.class);

	private static final AdventureResult[] NO_INGREDIENTS = new AdventureResult[ 0 ];

	public static final AdventureResult INIGO = new AdventureResult( "Inigo's Incantation of Inspiration", 0, true );

	private static final HashMap<String,Concoction> chefStaff = new HashMap<String,Concoction>();
	private static final HashMap<String,Concoction> singleUse = new HashMap<String,Concoction>();
	private static final HashMap<String,Concoction> multiUse = new HashMap<String,Concoction>();
	private static final HashMap<String,Concoction> noodles = new HashMap<String,Concoction>();
	private static final HashMap<String,Concoction> meatStack = new HashMap<String,Concoction>();

	private static CraftingType mixingMethod = null;
	private static final EnumSet<CraftingRequirements> requirements = EnumSet.noneOf(CraftingRequirements.class);
	private static final EnumSet<CraftingMisc> info = EnumSet.noneOf(CraftingMisc.class);

	public static final void resetQueue()
	{
		Stack queuedChanges = ConcoctionDatabase.queuedFoodChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( true, false, false );
		}
		queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( false, true, false );
		}
		queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
		while ( !queuedChanges.empty() )
		{
			ConcoctionDatabase.pop( false, false, true );
		}
	}

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = FileUtilities.getVersionedReader( "concoctions.txt", KoLConstants.CONCOCTIONS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			ConcoctionDatabase.addConcoction( data );
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

		// Add all concoctions to usable list

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			ConcoctionDatabase.usableList.add( current );
		}

		ConcoctionDatabase.usableList.sort();
	}

	private static final void addConcoction( final String[] data )
	{
		// Need at least concoction name and mixing method
		if ( data.length <= 2 )
		{
			return;
		}

		boolean bogus = false;

		ConcoctionDatabase.mixingMethod = null;
		ConcoctionDatabase.requirements.clear();
		ConcoctionDatabase.info.clear();
		String name = new String( data[ 0 ] );
		String[] mixes = data[ 1 ].split( "\\s*,\\s*" );
		for ( int i = 0; i < mixes.length; ++i )
		{
			String mix = mixes[ i ];
			ConcoctionDatabase.addCraftingData( mix, name );
		}

		if ( ConcoctionDatabase.mixingMethod == null )
		{
			RequestLogger.printLine( "No mixing method specified for concoction: " + name );
			bogus = true;
		}

		AdventureResult item = AdventureResult.parseItem( name, true );
		int itemId = item.getItemId();

		if ( itemId < 0 && !ConcoctionDatabase.pseudoItemMixingMethod( ConcoctionDatabase.mixingMethod ) )
		{
			RequestLogger.printLine( "Unknown concoction: " + name );
			bogus = true;
		}

		AdventureResult[] ingredients = new AdventureResult[ data.length - 2 ];
		int param = 0;
		for ( int i = 2; i < data.length; ++i )
		{
			if ( StringUtilities.isNumeric( data[ i ] ) )
			{	// Treat all-numeric element as parameter instead of item.
				// Up to 4 such parameters can be given if each fits in a byte.
				param = (param << 8) | StringUtilities.parseInt( data[ i ] );
				continue;
			}
			AdventureResult ingredient = ConcoctionDatabase.parseIngredient( data[ i ] );
			if ( ingredient == null || ingredient.getItemId() == -1 || ingredient.getName() == null )
			{
				RequestLogger.printLine( "Unknown ingredient (" + data[ i ] + ") for concoction: " + name );
				bogus = true;
				continue;
			}

			ingredients[ i - 2 ] = ingredient;
		}

		if ( !bogus )
		{
			Concoction concoction = new Concoction( item, ConcoctionDatabase.mixingMethod, ConcoctionDatabase.requirements.clone(), ConcoctionDatabase.info.clone() );
			concoction.setParam( param );

			Concoction existing = ConcoctionPool.get( item );
			if ( concoction.getMisc().contains( CraftingMisc.MANUAL ) ||
			     ( existing != null && existing.getMixingMethod() != CraftingType.NOCREATE ) )
			{	// Until multiple recipes are supported...
				return;
			}

			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( ingredient == null )
				{	// Was a parameter, not an ingredient.
					continue;
				}
				concoction.addIngredient( ingredient );
				if ( ingredient.getItemId() == ItemPool.MEAT_STACK )
				{
					ConcoctionDatabase.meatStack.put( concoction.getName(), concoction );
				}
			}

			ConcoctionPool.set( concoction );

			switch ( ConcoctionDatabase.mixingMethod )
			{
			case STAFF:
				ConcoctionDatabase.chefStaff.put( ingredients[ 0 ].getName(), concoction );
				break;
			case SINGLE_USE:
				ConcoctionDatabase.singleUse.put( ingredients[ 0 ].getName(), concoction );
				break;
			case MULTI_USE:
				ConcoctionDatabase.multiUse.put( ingredients[ 0 ].getName(), concoction );
				break;
			case WOK:
				ConcoctionDatabase.noodles.put( concoction.getName(), concoction );
				break;
			}

			if ( ConcoctionDatabase.requirements.contains( CraftingRequirements.PASTA ) )
			{
				ConcoctionDatabase.noodles.put( concoction.getName(), concoction );
			}
		}
	}

	public static Concoction chefStaffCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.chefStaff.get( name );
	}

	public static Concoction singleUseCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.singleUse.get( name );
	}

	public static Concoction multiUseCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.multiUse.get( name );
	}

	public static Concoction noodleCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.noodles.get( name );
	}

	public static Concoction meatStackCreation( final String name )
	{
		return name == null ? null : (Concoction) ConcoctionDatabase.meatStack.get( name );
	}

	private static boolean pseudoItemMixingMethod( final CraftingType mixingMethod )
	{
		return mixingMethod == CraftingType.SUSHI;
	}

	public static final boolean isKnownCombination( final AdventureResult[] ingredients )
	{
		// Known combinations which could not be added because
		// there are limitations in the item manager.

		if ( ingredients.length == 2 )
		{
			// Handle meat stacks, which are created from fairy
			// gravy and meat from yesterday.

			if ( ingredients[ 0 ].getItemId() == ItemPool.GRAVY_BOAT && ingredients[ 1 ].getItemId() == ItemPool.MEAT_FROM_YESTERDAY )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.GRAVY_BOAT && ingredients[ 0 ].getItemId() == ItemPool.MEAT_FROM_YESTERDAY )
			{
				return true;
			}

			// Handle plain pizza, which also allows flat dough
			// to be used instead of wads of dough.

			if ( ingredients[ 0 ].getItemId() == ItemPool.TOMATO && ingredients[ 1 ].getItemId() == ItemPool.DOUGH )
			{
				return true;
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.TOMATO && ingredients[ 0 ].getItemId() == ItemPool.FLAT_DOUGH )
			{
				return true;
			}

			// Handle catsup recipes, which only exist in the
			// item table as ketchup recipes.

			if ( ingredients[ 0 ].getItemId() == ItemPool.CATSUP )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.KETCHUP, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.CATSUP )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.KETCHUP, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle ice-cold beer recipes, which only uses the
			// recipe for item #41 at this time.

			if ( ingredients[ 0 ].getItemId() == ItemPool.WILLER )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.SCHLITZ, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.WILLER )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.SCHLITZ, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}

			// Handle cloaca recipes, which only exist in the
			// item table as dyspepsi cola.

			if ( ingredients[ 0 ].getItemId() == ItemPool.CLOACA_COLA )
			{
				ingredients[ 0 ] = ItemPool.get( ItemPool.DYSPEPSI_COLA, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
			if ( ingredients[ 1 ].getItemId() == ItemPool.CLOACA_COLA )
			{
				ingredients[ 1 ] = ItemPool.get( ItemPool.DYSPEPSI_COLA, 1 );
				return ConcoctionDatabase.isKnownCombination( ingredients );
			}
		}

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			if ( current.hasIngredients( ingredients ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final SortedListModel getKnownUses( final int itemId )
	{
		SortedListModel uses = ConcoctionDatabase.knownUses.get( itemId );
		return uses == null ? ConcoctionDatabase.EMPTY_LIST : uses;
	}

	public static final SortedListModel getKnownUses( final AdventureResult item )
	{
		return ConcoctionDatabase.getKnownUses( item.getItemId() );
	}

	public static final boolean isPermittedMethod( final CraftingType method, 
		  final EnumSet<CraftingRequirements> requirements )
	{
		System.out.println( "method = " + method + " requirements = " + requirements );
		System.out.println( "REQUIREMENT_MET = " + ConcoctionDatabase.REQUIREMENT_MET );
		// If we can't make anything via this method, punt
		if ( !ConcoctionDatabase.PERMIT_METHOD.contains( method ) )
		{
			return false;
		}

		// If we don't meet special creation requirements for this item, punt
		Iterator reqs = requirements.iterator();
		while ( reqs.hasNext() )
		{
			if ( !ConcoctionDatabase.REQUIREMENT_MET.contains( (CraftingRequirements) reqs.next() ) )
			{
				return false;
			}
		}

		// Otherwise, go for it!
		return true;
	}

	public static final boolean checkPermittedMethod( Concoction conc )
	{
		// Same as isPermittedMethod(), but sets excuse.
		ConcoctionDatabase.excuse = null;

		CraftingType method = conc.getMixingMethod();

		if ( !ConcoctionDatabase.PERMIT_METHOD.contains( method ) )
		{
			ConcoctionDatabase.excuse = ConcoctionDatabase.EXCUSE.get( method );
			return false;
		}

		EnumSet<CraftingRequirements> requirements = conc.getRequirements();
		Iterator req = requirements.iterator();
		while ( req.hasNext() )
		{
			CraftingRequirements next = (CraftingRequirements) req.next();
			KoLmafia.updateDisplay( next.toString() );
			if ( !ConcoctionDatabase.REQUIREMENT_MET.contains( next ) )
			{
				ConcoctionDatabase.excuse = "You lack a skill or other prerequisite for creating that item (" + req.toString() + ").";
				return false;
			}
		}

		return true;
	}

	private static final AdventureResult parseIngredient( final String data )
	{
		// If the ingredient is specified inside of brackets,
		// then a specific item Id is being designated.

		if ( data.startsWith( "[" ) )
		{
			int closeBracketIndex = data.indexOf( "]" );
			String itemIdString = data.substring( 0, closeBracketIndex ).replaceAll( "[\\[\\]]", "" ).trim();
			String quantityString = data.substring( closeBracketIndex + 1 ).trim();

			return ItemPool.get(
				StringUtilities.parseInt( itemIdString ),
				quantityString.length() == 0 ? 1 : StringUtilities.parseInt( quantityString.replaceAll( "[\\(\\)]", "" ) ) );
		}

		// Otherwise, it's a standard ingredient - use
		// the standard adventure result parsing routine.

		return AdventureResult.parseResult( data );
	}

	public static final SortedListModel getQueuedIngredients( boolean food, boolean booze, boolean spleen )
	{
		return food ? ConcoctionDatabase.queuedFoodIngredients :
			booze ? ConcoctionDatabase.queuedBoozeIngredients :
			ConcoctionDatabase.queuedSpleenIngredients;
	}

	public static final void push( final Concoction c, final int quantity )
	{
		Stack queuedChanges;
		LockableListModel queuedIngredients;
		int id = c.getItemId();
		int consumpt = ItemDatabase.getConsumptionType( id );

		if ( c.getFullness() > 0 || consumpt == KoLConstants.CONSUME_FOOD_HELPER ||
		     id == ItemPool.MUNCHIES_PILL || id == ItemPool.DISTENTION_PILL )
		{
			queuedChanges = ConcoctionDatabase.queuedFoodChanges;
			queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
			ConcoctionDatabase.queuedFullness += c.getFullness() * quantity;
		}
		else if ( c.getInebriety() > 0 || consumpt == KoLConstants.CONSUME_DRINK_HELPER )
		{
			queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
			queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
			ConcoctionDatabase.queuedInebriety += c.getInebriety() * quantity;
		}
		else
		{
			queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
			queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
			ConcoctionDatabase.queuedSpleenHit += c.getSpleenHit() * quantity;
		}

		int adventureChange = ConcoctionDatabase.queuedAdventuresUsed;
		int freeCraftChange = ConcoctionDatabase.queuedFreeCraftingTurns;
		int stillChange = ConcoctionDatabase.queuedStillsUsed;
		int tomeChange = ConcoctionDatabase.queuedTomesUsed;
		int pullChange = ConcoctionDatabase.queuedPullsUsed;
		int meatChange = ConcoctionDatabase.queuedMeatSpent;

		ArrayList ingredientChange = new ArrayList();
		c.queue( queuedIngredients, ingredientChange, quantity );

		adventureChange = ConcoctionDatabase.queuedAdventuresUsed - adventureChange;
		if ( adventureChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, adventureChange ) );
		}
		
		freeCraftChange = ConcoctionDatabase.queuedFreeCraftingTurns - freeCraftChange;
		if ( freeCraftChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.FREE_CRAFT, freeCraftChange ) );
		}

		stillChange = ConcoctionDatabase.queuedStillsUsed - stillChange;
		if ( stillChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.STILL, stillChange ) );
		}

		tomeChange = ConcoctionDatabase.queuedTomesUsed - tomeChange;
		if ( tomeChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.TOME, tomeChange ) );
		}

		pullChange = ConcoctionDatabase.queuedPullsUsed - pullChange;
		if ( pullChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, pullChange ) );
		}

		meatChange = ConcoctionDatabase.queuedMeatSpent - meatChange;
		if ( meatChange != 0 )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.MEAT_SPENT, meatChange ) );
		}

		queuedChanges.push( IntegerPool.get( meatChange ) );
		queuedChanges.push( IntegerPool.get( pullChange ) );
		queuedChanges.push( IntegerPool.get( tomeChange ) );
		queuedChanges.push( IntegerPool.get( stillChange ) );
		queuedChanges.push( IntegerPool.get( adventureChange ) );
		queuedChanges.push( IntegerPool.get( freeCraftChange ) );

		queuedChanges.push( ingredientChange );
		queuedChanges.push( IntegerPool.get( quantity ) );
		queuedChanges.push( c );
	}

	public static final Object [] pop( boolean food, boolean booze, boolean spleen )
	{
		Stack queuedChanges;
		LockableListModel queuedIngredients;

		if ( food )
		{
			queuedChanges = ConcoctionDatabase.queuedFoodChanges;
			queuedIngredients = ConcoctionDatabase.queuedFoodIngredients;
		}
		else if ( booze )
		{
			queuedChanges = ConcoctionDatabase.queuedBoozeChanges;
			queuedIngredients = ConcoctionDatabase.queuedBoozeIngredients;
		}
		else
		{
			queuedChanges = ConcoctionDatabase.queuedSpleenChanges;
			queuedIngredients = ConcoctionDatabase.queuedSpleenIngredients;
		}

		if ( queuedChanges.isEmpty() )
		{
			return null;
		}

		Concoction c = (Concoction) queuedChanges.pop();
		Integer quantity = (Integer) queuedChanges.pop();
		ArrayList ingredientChange = (ArrayList) queuedChanges.pop();

		Integer freeCraftChange = (Integer) queuedChanges.pop();
		Integer adventureChange = (Integer) queuedChanges.pop();
		Integer stillChange = (Integer) queuedChanges.pop();
		Integer tomeChange = (Integer) queuedChanges.pop();
		Integer pullChange = (Integer) queuedChanges.pop();
		Integer meatChange = (Integer) queuedChanges.pop();

		c.queued -= quantity.intValue();
		c.queuedPulls -= pullChange.intValue();
		for ( int i = 0; i < ingredientChange.size(); ++i )
		{
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, ( (AdventureResult) ingredientChange.get( i ) ).getNegation() );
		}
		
		int free = freeCraftChange.intValue();
		if ( free != 0 )
		{
			ConcoctionDatabase.queuedFreeCraftingTurns -= free;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.FREE_CRAFT, -free ) );
		}

		int advs = adventureChange.intValue();
		if ( advs != 0 )
		{
			ConcoctionDatabase.queuedAdventuresUsed -= advs;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.ADV, -advs ) );
		}

		int stills = stillChange.intValue();
		if ( stills != 0 )
		{
			ConcoctionDatabase.queuedStillsUsed -= stills;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.STILL, -stills ) );
		}

		int tome = tomeChange.intValue();
		if ( tome != 0 )
		{
			ConcoctionDatabase.queuedTomesUsed -= tome;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.TOME, -tome ) );
		}

		int pulls = pullChange.intValue();
		if ( pulls != 0 )
		{
			ConcoctionDatabase.queuedPullsUsed -= pulls;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.PULL, -pulls ) );
		}

		int meat = meatChange.intValue();
		if ( meat != 0 )
		{
			ConcoctionDatabase.queuedMeatSpent -= meat;
			AdventureResult.addOrRemoveResultToList(
				queuedIngredients, new AdventureResult( AdventureResult.MEAT_SPENT, -meat ) );
		}

		ConcoctionDatabase.queuedFullness -= c.getFullness() * quantity.intValue();
		ConcoctionDatabase.queuedInebriety -= c.getInebriety() * quantity.intValue();
		ConcoctionDatabase.queuedSpleenHit -= c.getSpleenHit() * quantity.intValue();

		return new Object [] { c, quantity };
	}

	public static final void addUsableConcoction( final Concoction c )
	{
		ConcoctionDatabase.usableList.add( c );
		ConcoctionDatabase.usableList.sort();
	}

	public static final LockableListModel getUsables()
	{
		return ConcoctionDatabase.usableList;
	}

	public static final SortedListModel getCreatables()
	{
		return ConcoctionDatabase.creatableList;
	}

	public static final void handleQueue( boolean food, boolean booze, boolean spleen, int consumptionType )
	{
		Object [] currentItem;
		Stack toProcess = new Stack();

		while ( ( currentItem = ConcoctionDatabase.pop( food, booze, spleen ) ) != null )
		{
			toProcess.push( currentItem );
		}

		// If we happen to have refreshed concoctions while there were
		// items queued, the creatable amounts will assume that queued
		// ingredients are already spoken for. Refresh again now that
		// the queue is empty.

		ConcoctionDatabase.refreshConcoctions( true );

		Concoction c;
		int quantity = 0;

		SpecialOutfit.createImplicitCheckpoint();

		while ( !toProcess.isEmpty() )
		{
			currentItem = (Object []) toProcess.pop();

			c = (Concoction) currentItem[ 0 ];
			quantity = ( (Integer) currentItem[ 1 ] ).intValue();

			if ( consumptionType != KoLConstants.CONSUME_USE && c.getItem() != null )
			{
				int consumpt = ItemDatabase.getConsumptionType( c.getItemId() );
				if ( consumpt == KoLConstants.CONSUME_FOOD_HELPER ||
				     consumpt == KoLConstants.CONSUME_DRINK_HELPER )
				{
					continue;
				}
				AdventureResult toConsume = c.getItem().getInstance( quantity );
				InventoryManager.retrieveItem( toConsume );

				if ( consumptionType == KoLConstants.CONSUME_GHOST || consumptionType == KoLConstants.CONSUME_HOBO )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( consumptionType, toConsume ) );
				}

				continue;
			}

			ConcoctionDatabase.consumeItem( c, quantity );
		}

		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void consumeItem( Concoction c, int quantity )
	{
		AdventureResult item = c.getItem();

		// First, consume any items which appear in the inventory.

		if ( item != null )
		{
			int initialConsume = Math.min( quantity, InventoryManager.getCount( item.getItemId() ) );

			UseItemRequest request = UseItemRequest.getInstance( item.getInstance( initialConsume ) );
			RequestThread.postRequest( request );

			quantity -= initialConsume;

			if ( quantity == 0 )
			{
				return;
			}
		}

		// If there's an actual item, it's not from a store

		if ( item != null )
		{
			// If concoction is a normal item, use normal item
			// acquisition methods.

			if ( item.getItemId() > 0 )
			{
				UseItemRequest request = UseItemRequest.getInstance( item.getInstance( quantity ) );
				RequestThread.postRequest( request );
				return;
			}

			// Otherwise, making item will consume it.
			CreateItemRequest request = CreateItemRequest.getInstance( item.getInstance( quantity ) );
			request.setQuantityNeeded( quantity );
			RequestThread.postRequest( request );
			return;
		}

		// Otherwise, acquire them from the restaurant.

		String name = c.getName();
		CafeRequest request;

		if ( HellKitchenRequest.onMenu( name ) )
		{
			request = new HellKitchenRequest( name );
		}
		else if ( ChezSnooteeRequest.onMenu( name ) )
		{
			request = new ChezSnooteeRequest( name );
		}
		else if ( MicroBreweryRequest.onMenu( name ) )
		{
			request = new MicroBreweryRequest( name );
		}
		else if ( CrimboCafeRequest.onMenu( name ) )
		{
			request = new CrimboCafeRequest( name );
		}
		else
		{
			return;
		}

		for ( int j = 0; j < quantity; ++j )
		{
			RequestThread.postRequest( request );
		}
	}

	public static final int getQueuedFullness()
	{
		return ConcoctionDatabase.queuedFullness;
	}

	public static final int getQueuedInebriety()
	{
		return ConcoctionDatabase.queuedInebriety;
	}

	public static final int getQueuedSpleenHit()
	{
		return ConcoctionDatabase.queuedSpleenHit;
	}

	private static final List getAvailableIngredients()
	{
		boolean includeCloset =
			!KoLConstants.closet.isEmpty() &&
			Preferences.getBoolean( "autoSatisfyWithCloset" );
		boolean includeStorage =
			KoLCharacter.canInteract() &&
			!KoLConstants.storage.isEmpty() &&
			Preferences.getBoolean( "autoSatisfyWithStorage" );
		boolean includeStash =
			KoLCharacter.canInteract() &&
			Preferences.getBoolean( "autoSatisfyWithStash" ) &&
			!ClanManager.getStash().isEmpty();

		boolean includeQueue =
			!ConcoctionDatabase.queuedFoodIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedBoozeIngredients.isEmpty() ||
			!ConcoctionDatabase.queuedSpleenIngredients.isEmpty();

		if ( !includeCloset && !includeStorage && !includeStash && !includeQueue )
		{
			return KoLConstants.inventory;
		}

		SortedListModel availableIngredients = new SortedListModel();
		availableIngredients.addAll( KoLConstants.inventory );

		if ( includeCloset )
		{
			for ( int i = 0; i < KoLConstants.closet.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) KoLConstants.closet.get( i ) );
			}
		}

		if ( includeStorage )
		{
			for ( int i = 0; i < KoLConstants.storage.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) KoLConstants.storage.get( i ) );
			}
		}

		if ( includeStash )
		{
			List stash = ClanManager.getStash();
			for ( int i = 0; i < stash.size(); ++i )
			{
				AdventureResult.addResultToList( availableIngredients, (AdventureResult) stash.get( i ) );
			}
		}

		if ( !ConcoctionDatabase.queuedFoodIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedFoodIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedFoodIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		if ( !ConcoctionDatabase.queuedBoozeIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedBoozeIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedBoozeIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		if ( !ConcoctionDatabase.queuedSpleenIngredients.isEmpty() )
		{
			for ( int i = 0; i < ConcoctionDatabase.queuedSpleenIngredients.size(); ++i )
			{
				AdventureResult ingredient = (AdventureResult) ConcoctionDatabase.queuedSpleenIngredients.get( i );
				if ( ingredient.isItem() )
				{
					AdventureResult.addResultToList(
						availableIngredients,
						ingredient.getNegation() );
				}
			}
		}

		return availableIngredients;
	}

	public static final void deferRefresh( boolean flag )
	{
		if ( flag )
		{
			++ConcoctionDatabase.refreshLevel;
		}
		else if ( ConcoctionDatabase.refreshLevel > 0 )
		{
			if ( --ConcoctionDatabase.refreshLevel == 0 )
			{
				ConcoctionDatabase.refreshConcoctions( false );
			}
		}
	}

	public static final void setRefreshNeeded( int itemId )
	{
		switch ( ItemDatabase.getConsumptionType( itemId ) )
		{
		case KoLConstants.CONSUME_EAT:
		case KoLConstants.CONSUME_DRINK:
		case KoLConstants.CONSUME_USE:
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.CONSUME_FOOD_HELPER:
		case KoLConstants.CONSUME_DRINK_HELPER:
			ConcoctionDatabase.setRefreshNeeded( false );
			return;
		}

		switch ( itemId )
		{
		// Items that affect creatability of other items, but
		// aren't explicitly listed in their recipes:
		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:

		// Interchangeable ingredients, which might have been missed
		// by the getKnownUses check because the recipes are set to
		// use the other possible ingredient:
		case ItemPool.SCHLITZ:
		case ItemPool.WILLER:
		case ItemPool.KETCHUP:
		case ItemPool.CATSUP:
		case ItemPool.DYSPEPSI_COLA:
		case ItemPool.CLOACA_COLA:
		case ItemPool.TITANIUM_UMBRELLA:
		case ItemPool.GOATSKIN_UMBRELLA:
			ConcoctionDatabase.setRefreshNeeded( false );
			return;
		}

		List uses = ConcoctionDatabase.getKnownUses( itemId );

		for ( int i = 0; i < uses.size(); ++i )
		{
			AdventureResult use = (AdventureResult) uses.get( i );
			CraftingType method = ConcoctionDatabase.getMixingMethod( use.getItemId() );
			EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements( use.getItemId() );
			
			if ( ConcoctionDatabase.isPermittedMethod( method, requirements ) )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
				return;
			}
		}

		for ( int i = 0; i < CoinmasterRegistry.COINMASTERS.length; ++i )
		{
			AdventureResult item = CoinmasterRegistry.COINMASTERS[ i ].getItem();
			if ( item != null && itemId == item.getItemId() )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
				return;
			}
		}
	}

	public static final void setRefreshNeeded( boolean recalculateAdventureRange )
	{
		ConcoctionDatabase.refreshNeeded = true;

		if ( recalculateAdventureRange )
		{
			ConcoctionDatabase.recalculateAdventureRange = true;
		}
	}

	/**
	 * Returns the concoctions which are available given the list of ingredients. The list returned contains formal
	 * requests for item creation.
	 */

	public static final synchronized void refreshConcoctions( boolean force )
	{
		if ( !force && !ConcoctionDatabase.refreshNeeded )
		{
			return;
		}

		if ( ConcoctionDatabase.refreshLevel > 0 )
		{
			return;
		}

		if ( FightRequest.initializingAfterFight() )
		{
			return;
		}

		ConcoctionDatabase.refreshNeeded = false;

		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();

		// Iterate through the concoction table, Initialize each one
		// appropriately depending on whether it is an NPC item, a Coin
		// Master item, or anything else.

		boolean useNPCStores = Preferences.getBoolean( "autoSatisfyWithNPCs" );

		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();

			// Initialize all the variables
			item.resetCalculations();

			AdventureResult concoction = item.concoction;
			if ( concoction == null )
			{
				continue;
			}

			int itemId = concoction.getItemId();

			if ( itemId == ItemPool.WORTHLESS_ITEM )
			{
				item.price = useNPCStores ? InventoryManager.currentWorthlessItemCost() : 0;
				item.initial = HermitRequest.getWorthlessItemCount( true );
				item.creatable = 0;
				item.total = item.initial;
				item.visibleTotal = item.total;
				continue;
			}


			String name = concoction.getName();

			if ( useNPCStores && NPCStoreDatabase.contains( name, true ) )
			{
				if ( itemId != ItemPool.FLAT_DOUGH )
				{
					// Don't buy flat dough from Degrassi Knoll Bakery -
					// buy wads of dough for 20 meat less, instead.

					item.price = NPCStoreDatabase.price( name );
					item.initial = concoction.getCount( availableIngredients );
					item.creatable = 0;
					item.total = item.initial;
					item.visibleTotal = item.total;
					continue;
				}
			}

			PurchaseRequest purchaseRequest = item.getPurchaseRequest();
			if (  purchaseRequest != null )
			{
				purchaseRequest.setCanPurchase();
				int acquirable = purchaseRequest.canPurchase() ?
					purchaseRequest.affordableCount() : 0;
				item.price = 0;
				item.initial = concoction.getCount( availableIngredients );
				item.creatable = acquirable;
				item.total = item.initial + acquirable;
				item.visibleTotal = item.total;
				continue;
			}

			// Set initial quantity of all remaining items.

			// Switch to the better of any interchangeable ingredients
			ConcoctionDatabase.getIngredients( item.getIngredients(), availableIngredients );

			item.initial = concoction.getCount( availableIngredients );
			item.price = 0;
			item.creatable = 0;
			item.total = item.initial;
			item.visibleTotal = item.total;
		}

		// Make assessment of availability of mixing methods.
		// This method will also calculate the availability of
		// chefs and bartenders automatically so a second call
		// is not needed.

		ConcoctionDatabase.cachePermitted( availableIngredients );

		// Finally, increment through all of the things which are
		// created any other way, making sure that it's a permitted
		// mixture before doing the calculation.

		it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();

			item.calculate2();
			item.calculate3();
		}

		// Now, to update the list of creatables without removing
		// all creatable items.	 We do this by determining the
		// number of items inside of the old list.

		boolean changeDetected = false;
		boolean considerPulls = !KoLCharacter.canInteract() &&
			!KoLCharacter.isHardcore() &&
			ConcoctionDatabase.getPullsBudgeted() > ConcoctionDatabase.queuedPullsUsed;

		it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction item = (Concoction) it.next();

			AdventureResult ar = item.getItem();
			if ( ar == null )
			{
				continue;
			}

			if ( considerPulls &&
				ar.getItemId() > 0 &&
				item.getPrice() <= 0 &&
				ItemDatabase.meetsLevelRequirement( item.getName() ) )
			{
				item.setPullable( Math.min ( ar.getCount( KoLConstants.storage ) - item.queuedPulls, ConcoctionDatabase.getPullsBudgeted() - ConcoctionDatabase.queuedPullsUsed ) );
			}
			else
			{
				item.setPullable( 0 );
			}

			CreateItemRequest instance = CreateItemRequest.getInstance( ar, false );

			if ( instance == null )
			{
				continue;
			}

			int creatable = Math.max( item.creatable, 0 );
			int pullable = Math.max( item.pullable, 0 );

			instance.setQuantityPossible( creatable );
			instance.setQuantityPullable( pullable );

			if ( creatable + pullable == 0 )
			{
				if ( item.wasPossible() )
				{
					ConcoctionDatabase.creatableList.remove( instance );
					item.setPossible( false );
					changeDetected = true;
				}
			}
			else if ( !item.wasPossible() )
			{
				ConcoctionDatabase.creatableList.add( instance );
				item.setPossible( true );
				changeDetected = true;
			}
		}

		ConcoctionDatabase.creatableList.updateFilter( changeDetected );
		ConcoctionDatabase.creatableList.sort();
		ConcoctionDatabase.usableList.updateFilter( changeDetected );
		ConcoctionDatabase.usableList.sort();

		if ( ConcoctionDatabase.recalculateAdventureRange )
		{
			ItemDatabase.calculateAdventureRanges();
			ConcoctionDatabase.recalculateAdventureRange = false;
		}
	}

	/**
	 * Reset concoction stat gains when you've logged in a new
	 * character.
	 */

	public static final void resetConcoctionStatGains()
	{
		Iterator it = ConcoctionPool.iterator();

		while ( it.hasNext() )
		{
			Concoction current = (Concoction) it.next();
			current.setStatGain();
		}

		ConcoctionDatabase.usableList.sort();
	}

	private static final void calculateBasicItems( final List availableIngredients )
	{
		// Meat paste and meat stacks can be created directly
		// and are dependent upon the amount of meat available.

		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.MEAT_PASTE, 10 );
		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.MEAT_STACK, 100 );
		ConcoctionDatabase.setBuyableItem(
			availableIngredients, ItemPool.DENSE_STACK, 1000 );
	}

	private static final void setBuyableItem( final List availableIngredients, final int itemId, final int price )
	{
		Concoction creation = ConcoctionPool.get( itemId );
		if ( creation == null )
		{
			return;
		}

		creation.initial = ItemPool.get( itemId, 1 ).getCount( availableIngredients );
		creation.price = price;
		creation.creatable = 0;
		creation.total = creation.initial;
		creation.visibleTotal = creation.total;
	}

	/**
	 * Utility method used to cache the current permissions on item creation.
	 */

	private static final void cachePermitted( final List availableIngredients )
	{
		int toolCost = KoLCharacter.inBadMoon() ? 500 : 1000;
		boolean willBuyTool =
			KoLCharacter.getAvailableMeat() >= toolCost &&
			Preferences.getBoolean( "autoSatisfyWithNPCs" );
		boolean willBuyServant = KoLCharacter.canInteract() &&
			Preferences.getBoolean( "autoRepairBoxServants" ) &&
			( Preferences.getBoolean( "autoSatisfyWithMall" ) ||
			  Preferences.getBoolean( "autoSatisfyWithStash" ) );

		// Adventures are considered Item #0 in the event that the
		// concoction will use ADVs.

		ConcoctionDatabase.adventureLimit.total = KoLCharacter.getAdventuresLeft() + ConcoctionDatabase.getFreeCraftingTurns();
		ConcoctionDatabase.adventureLimit.initial =
			ConcoctionDatabase.adventureLimit.total - ConcoctionDatabase.queuedAdventuresUsed;
		ConcoctionDatabase.adventureLimit.creatable = 0;
		ConcoctionDatabase.adventureLimit.visibleTotal = ConcoctionDatabase.adventureLimit.total;
		
		// If we want to do turn-free crafting, we can only use free turns in lieu of adventures.
		
		ConcoctionDatabase.turnFreeLimit.total = ConcoctionDatabase.getFreeCraftingTurns();
		ConcoctionDatabase.turnFreeLimit.initial = ConcoctionDatabase.turnFreeLimit.total - ConcoctionDatabase.queuedFreeCraftingTurns;
		ConcoctionDatabase.turnFreeLimit.creatable = 0;
		ConcoctionDatabase.turnFreeLimit.visibleTotal = ConcoctionDatabase.turnFreeLimit.total;

		// Stills are also considered Item #0 in the event that the
		// concoction will use stills.

		ConcoctionDatabase.stillsLimit.total = KoLCharacter.getStillsAvailable();
		ConcoctionDatabase.stillsLimit.initial =
			ConcoctionDatabase.stillsLimit.total - ConcoctionDatabase.queuedStillsUsed;
		ConcoctionDatabase.stillsLimit.creatable = 0;
		ConcoctionDatabase.stillsLimit.visibleTotal = ConcoctionDatabase.stillsLimit.total;

		// Tomes are also also also considered Item #0 in the event that the
		// concoction requires a tome summon.

		String pref = KoLCharacter.canInteract() ? "_clipartSummons" : "tomeSummons";
		ConcoctionDatabase.clipArtLimit.total = 3 - Preferences.getInteger( pref );
		ConcoctionDatabase.clipArtLimit.initial =
			ConcoctionDatabase.clipArtLimit.total - ConcoctionDatabase.queuedTomesUsed;
		ConcoctionDatabase.clipArtLimit.creatable = 0;
		ConcoctionDatabase.clipArtLimit.visibleTotal = ConcoctionDatabase.clipArtLimit.total;

		// Meat is also also considered Item #0 in the event that the
		// concoction will create paste/stacks or buy NPC items.

		ConcoctionDatabase.meatLimit.total = KoLCharacter.getAvailableMeat();
		ConcoctionDatabase.meatLimit.initial =
			ConcoctionDatabase.meatLimit.total - ConcoctionDatabase.queuedMeatSpent;
		ConcoctionDatabase.meatLimit.creatable = 0;
		ConcoctionDatabase.meatLimit.visibleTotal = ConcoctionDatabase.meatLimit.total;

		ConcoctionDatabase.calculateBasicItems( availableIngredients );

		// Clear the maps
		ConcoctionDatabase.REQUIREMENT_MET.clear();
		ConcoctionDatabase.PERMIT_METHOD.clear();
		ConcoctionDatabase.ADVENTURE_USAGE.clear();
		ConcoctionDatabase.CREATION_COST.clear();
		ConcoctionDatabase.EXCUSE.clear();
		int Inigo = ConcoctionDatabase.getFreeCraftingTurns();


		if ( KoLCharacter.getGender() == KoLCharacter.MALE )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.MALE );
		}
		else
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.FEMALE );
		}
		

		// It is never possible to create items which are flagged
		// NOCREATE

		// It is always possible to create items through meat paste
		// combination.

		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.COMBINE );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.COMBINE, 10 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COMBINE, 0 );

		// Un-untinkerable Amazing Ideas
		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.ACOMBINE );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.ACOMBINE, 10 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.ACOMBINE, 0 );

		// The gnomish tinkerer is available if the person is in a
		// gnome sign and they can access the Desert Beach.

		if ( KoLCharacter.gnomadsAvailable() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.GNOME_TINKER );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.GNOME_TINKER, 0 );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.GNOME_TINKER, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.GNOME_TINKER, "Only moxie signs can use the Supertinkerer." );

		// Smithing of items is possible whenever the person
		// has a hammer.

		if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) || willBuyTool )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SMITH );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.SMITH, 0 );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SMITH, Math.max( 0, 1 - Inigo ) );
		}

		if ( InventoryManager.hasItem( ItemPool.GRIMACITE_HAMMER ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.GRIMACITE );
		}

		// Advanced smithing is available whenever the person can
		// smith.  The appropriate skill is checked separately.

		if ( ConcoctionDatabase.PERMIT_METHOD.contains( CraftingType.SMITH ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SSMITH );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.SSMITH, 0 );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SSMITH, Math.max( 0, 1 - Inigo ) );
		}

		// Standard smithing is also possible if the person is in
		// a knoll sign.

		if ( KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SMITH );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SMITH, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.ACOMBINE, 0 );
		}

		if ( KoLCharacter.canSmithWeapons() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.SUPER_MEATSMITHING );
		}

		if ( KoLCharacter.canSmithArmor() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.ARMORCRAFTINESS );
		}

		// Jewelry making is possible as long as the person has the
		// appropriate pliers.

		if ( InventoryManager.hasItem( ItemPool.JEWELRY_PLIERS ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.JEWELRY );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.JEWELRY, 0 );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.JEWELRY, Math.max( 0, 3 - Inigo ) );
		}

		if ( KoLCharacter.canCraftExpensiveJewelry() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.EXPENSIVE );
		}

		// Star charts and pixel chart recipes are available to all
		// players at all times.

		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.STARCHART );
		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.PIXEL );
		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.MULTI_USE );
		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SINGLE_USE );
		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SUGAR_FOLDING );

		ConcoctionDatabase.CREATION_COST.put( CraftingType.STARCHART, 0 );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.PIXEL, 0 );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.MULTI_USE, 0 );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.SINGLE_USE, 0 );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.SUGAR_FOLDING, 0 );

		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.STARCHART, 0 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.PIXEL, 0 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MULTI_USE, 0 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SINGLE_USE, 0 );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SUGAR_FOLDING, 0 );


		// A rolling pin or unrolling pin can be always used in item
		// creation because we can get the same effect even without the
		// tool.

		ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.ROLLING_PIN );
		ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.ROLLING_PIN, 0 );
		ConcoctionDatabase.CREATION_COST.put( CraftingType.ROLLING_PIN, 0 );

		// Rodoric will make chefstaves for mysticality class
		// characters who can get to the guild.

		if ( KoLCharacter.isMysticalityClass() && KoLCharacter.getGuildStoreOpen() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.STAFF );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.STAFF, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.STAFF, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.STAFF, "Only mysticality classes can make chefstaves." );

		// Phineas will make things for Seal Clubbers who have defeated
		// their Nemesis, and hence have their ULEW

		if ( InventoryManager.hasItem( ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.PHINEAS );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.PHINEAS, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.PHINEAS, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.PHINEAS, "Only Seal Clubbers who have defeated Gorgolok can use Phineas." );

		// It's not possible to ask Uncle Crimbo 2005 to make toys
		// It's not possible to ask Ugh Crimbo 2006 to make toys
		// It's not possible to ask Uncle Crimbo 2007 to make toys
		// It's not possible to ask Uncle Crimbo 2012 to make toys

		// Next, increment through all the box servant creation methods.
		// This allows future appropriate calculation for cooking/drinking.

		ConcoctionPool.get( ItemPool.CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_CHEF ).calculate2();
		ConcoctionPool.get( ItemPool.BARTENDER ).calculate2();
		ConcoctionPool.get( ItemPool.CLOCKWORK_BARTENDER ).calculate2();

		// Cooking is permitted, so long as the person has an oven or a
		// range installed in their kitchen

		if ( KoLCharacter.hasOven() || KoLCharacter.hasRange() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.COOK );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COOK, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COOK, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.COOK, "You cannot cook without an oven or a range." );

		// If we have a range and a chef installed, cooking fancy foods
		// costs no adventure. If we have no chef, cooking takes
		// adventures unless we have Inigo's active.

		// If you don't have a range, you can't cook fancy food
		// We could auto buy & install a range if the character
		// has at least 1,000 Meat and autoSatisfyWithNPCs = true
		if ( !KoLCharacter.hasRange() && !willBuyTool )
		{
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.COOK_FANCY,
				"You cannot cook fancy foods without a range." );
		}
		// If you have (or will have) a chef, fancy cooking is free
		else if ( KoLCharacter.hasChef() || willBuyServant ||
			  ConcoctionDatabase.isAvailable( ItemPool.CHEF, ItemPool.CLOCKWORK_CHEF ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.COOK_FANCY );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COOK_FANCY,
				MallPriceDatabase.getPrice( ItemPool.CHEF ) / 90 );
		}
		// If we don't have a chef, Inigo's makes cooking free
/*		else if ( Inigo > 0 )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.COOK_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.COOK_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.COOK_FANCY ] = null;
		}*/
		// We might not care if cooking takes adventures
		else if ( Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.COOK_FANCY,
				"You have chosen not to cook fancy food without a chef-in-the-box." );
		}
		// Otherwise, spend those adventures!
		else
		{
			if ( KoLCharacter.getAdventuresLeft() + Inigo > 0 )
			{
				ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.COOK_FANCY );
			}
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COOK_FANCY, 1 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COOK_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.COOK_FANCY,
				"You cannot cook fancy foods without adventures." );
		}

		// Cooking may require an additional skill.

		if ( KoLCharacter.canSummonReagent() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.REAGENT );
		}

		if ( KoLCharacter.hasSkill( "The Way of Sauce" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.WAY );
		}

		if ( KoLCharacter.hasSkill( "Deep Saucery" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.DEEP_SAUCERY );
		}

		if ( KoLCharacter.canSummonNoodles() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.PASTA );
		}

		if ( KoLCharacter.hasSkill( "Tempuramancy" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.TEMPURAMANCY );
		}

		// Mixing is permitted, so long as the person has a shaker or a
		// cocktailcrafting kit installed in their kitchen

		if ( KoLCharacter.hasShaker() || KoLCharacter.hasCocktailKit() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.MIX );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MIX, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MIX, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.MIX,
				"You cannot mix without a shaker or a cocktailcrafting kit." );
		}

		// If we have a kit and a bartender installed, mixing fancy drinks
		// costs no adventure. If we have no bartender, mixing takes
		// adventures unless we have Inigo's active.

		// If you don't have a cocktailcrafting kit, you can't mix fancy drinks
		// We will auto buy & install a cocktailcrafting kit if the character
		// has at least 1,000 Meat and autoSatisfyWithNPCs = true
		if ( !KoLCharacter.hasCocktailKit() && !willBuyTool )
		{
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.MIX_FANCY,
				"You cannot mix fancy drinks without a cocktailcrafting kit." );
		}
		// If you have (or will have) a bartender, fancy mixing is free
		else if ( KoLCharacter.hasBartender() || willBuyServant ||
			  ConcoctionDatabase.isAvailable( ItemPool.BARTENDER, ItemPool.CLOCKWORK_BARTENDER ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.MIX_FANCY );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MIX_FANCY,
				MallPriceDatabase.getPrice( ItemPool.BARTENDER ) / 90 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.MIX_FANCY, null );
		}
		// If we don't have a bartender, Inigo's makes mixing free
/*		else if ( Inigo > 0 )
		{
			ConcoctionDatabase.PERMIT_METHOD[ KoLConstants.MIX_FANCY ] = true;
			ConcoctionDatabase.ADVENTURE_USAGE[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.CREATION_COST[ KoLConstants.MIX_FANCY ] = 0;
			ConcoctionDatabase.EXCUSE[ KoLConstants.MIX_FANCY ] = null;
		}*/
		// We might not care if mixing takes adventures
		else if ( Preferences.getBoolean( "requireBoxServants" ) )
		{
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.MIX_FANCY,
				"You have chosen not to mix fancy drinks without a bartender-in-the-box." );
		}
		// Otherwise, spend those adventures!
		else
		{
			if ( KoLCharacter.getAdventuresLeft() + Inigo > 0 )
			{
				ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.MIX_FANCY );
			}
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MIX_FANCY, 1 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MIX_FANCY, 0 );
			ConcoctionDatabase.EXCUSE.put( CraftingType.MIX_FANCY,
				"You cannot mix fancy drinks without adventures." );
		}

		// Mixing may require an additional skill.

		if ( KoLCharacter.canSummonShore() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.AC );
		}

		if ( KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.SHC );
		}

		if ( KoLCharacter.hasSkill( "Salacious Cocktailcrafting" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.SALACIOUS );
		}

		// Using Crosby Nash's Still is possible if the person has
		// Superhuman Cocktailcrafting and is a Moxie class character.

		if ( ConcoctionDatabase.stillsLimit.total > 0 )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.STILL_MIXER );
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.STILL_BOOZE );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.STILL_MIXER, 0 );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.STILL_BOOZE, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.STILL_MIXER, Preferences.getInteger( "valueOfStill" ) );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.STILL_BOOZE, Preferences.getInteger( "valueOfStill" ) );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.STILL_MIXER,
			KoLCharacter.isMoxieClass() ? "You have no Still uses remaining."
			: "Only moxie classes can use the Still." );

		ConcoctionDatabase.EXCUSE.put( CraftingType.STILL_BOOZE,
			KoLCharacter.isMoxieClass() ? "You have no Still uses remaining."
			: "Only moxie classes can use the Still." );

		// Summoning Clip Art is possible if the person has that tome,
		// and isn't in Bad Moon

		boolean hasClipArt = KoLCharacter.hasSkill( "Summon Clip Art" ) &&
			( !KoLCharacter.inBadMoon() || KoLCharacter.skillsRecalled() );
		boolean clipArtSummonsRemaining = hasClipArt && 
			( KoLCharacter.canInteract() ? Preferences.getInteger( "_clipartSummons" ) < 3 : 
			Preferences.getInteger( "tomeSummons" ) < 3 );
		if ( hasClipArt && clipArtSummonsRemaining )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.CLIPART );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.CLIPART, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.CLIPART, Preferences.getInteger( "valueOfTome" ) );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.CLIPART, hasClipArt ? "You have no Tome uses remaining."
				: "You don't have the Tome of Clip Art." );

		// Using the Wok of Ages is possible if the person has
		// Transcendental Noodlecraft and is a Mysticality class
		// character.

		if ( KoLCharacter.canUseWok() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.WOK );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.WOK, 1 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.WOK, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.WOK, "Only mysticality classes can use the Wok." );

		// Using the Malus of Forethought is possible if the person has
		// Pulverize and is a Muscle class character.

		if ( KoLCharacter.canUseMalus() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.MALUS );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.MALUS, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.MALUS, 0 );
		}
		ConcoctionDatabase.EXCUSE.put( CraftingType.MALUS, "You require Malus access to be able to pulverize." );

		// You can make Sushi if you have a sushi-rolling mat installed
		// in your kitchen.

		if ( KoLCharacter.hasSushiMat() )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.SUSHI );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.SUSHI, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.SUSHI, 0 );
		}
		ConcoctionDatabase.EXCUSE.put(  CraftingType.SUSHI, "You cannot make sushi without a sushi-rolling mat." );

		// You trade tokens to Coin Masters if you have opted in to do so,

		if ( Preferences.getBoolean( "autoSatisfyWithCoinmasters" ) )
		{
			ConcoctionDatabase.PERMIT_METHOD.add( CraftingType.COINMASTER );
			ConcoctionDatabase.ADVENTURE_USAGE.put( CraftingType.COINMASTER, 0 );
			ConcoctionDatabase.CREATION_COST.put( CraftingType.COINMASTER, 0 );
		}
		ConcoctionDatabase.EXCUSE.put(  CraftingType.COINMASTER, "You have not selected the option to trade with coin masters." );

		// Other creatability flags

		if ( KoLCharacter.hasSkill( "Torso Awaregness" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.TORSO );
		}

		if ( HolidayDatabase.getHoliday().equals( "St. Sneaky Pete's Day" ) ||
		     HolidayDatabase.getHoliday().equals( "Drunksgiving" ) )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.SSPD );
		}

		if ( !KoLCharacter.inBeecore() )
		{
			ConcoctionDatabase.REQUIREMENT_MET.add( CraftingRequirements.NOBEE );
		}

		// Now, go through all the cached adventure usage values and if
		// the number of adventures left is zero and the request requires
		// adventures, it is not permitted.

		int value = Preferences.getInteger( "valueOfAdventure" );
		for ( CraftingType method : CraftingType.values() )
		{
			if ( ConcoctionDatabase.PERMIT_METHOD.contains( method ) )
			{
				int adv = ConcoctionDatabase.ADVENTURE_USAGE.get( method );
				if ( adv == 0 )
				{
					continue;
				}
				if ( adv > KoLCharacter.getAdventuresLeft() + ConcoctionDatabase.getFreeCraftingTurns() )
				{
					ConcoctionDatabase.PERMIT_METHOD.remove( method );
					ConcoctionDatabase.EXCUSE.put( method, "You don't have enough adventures left to create that." );
				}
				else
				{
					int cost = ConcoctionDatabase.CREATION_COST.get( method );
					ConcoctionDatabase.CREATION_COST.put( method, cost + ( adv * value ) );
				}
			}
		}

	}

	public static int getFreeCraftingTurns()
	{
		return ConcoctionDatabase.INIGO.getCount( KoLConstants.activeEffects ) / 5;
	}

	private static final boolean isAvailable( final int servantId, final int clockworkId )
	{
		// Otherwise, return whether or not the quantity possible for
		// the given box servants is non-zero.	This works because
		// cooking tests are made after item creation tests.

		return Preferences.getBoolean( "autoRepairBoxServants" ) &&
			( ConcoctionPool.get( servantId ).total > 0 ||
			  ConcoctionPool.get( clockworkId ).total > 0 );
	}

	/**
	 * Returns the mixing method for the item with the given Id.
	 */

	public static final CraftingType getMixingMethod( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? CraftingType.NOCREATE : item.getMixingMethod();
	}

	public static final CraftingType getMixingMethod( final String name )
	{
		Concoction item = ConcoctionPool.get( name );
		return item == null ? CraftingType.NOCREATE : item.getMixingMethod();
	}

	public static final CraftingType getMixingMethod( final AdventureResult ar )
	{
		Concoction item = ConcoctionPool.get( ar );
		return item == null ? CraftingType.NOCREATE : item.getMixingMethod();
	}

	public static final EnumSet<CraftingRequirements> getRequirements( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? EnumSet.noneOf(CraftingRequirements.class) : item.getRequirements();
	}

	/**
	 * Describes a method of creation in terms of the means of creation and the
	 * restrictions, if any.
	 * @param mixingMethod the method to describe
	 * @return the description
	 */
	public static String mixingMethodDescription( final CraftingType mixingMethod, EnumSet<CraftingRequirements> mixingRequirements )
	{
		if ( mixingMethod == CraftingType.NOCREATE )
		{
			return "[cannot be created]";
		}

		StringBuilder result = new StringBuilder();

		if ( mixingMethod == CraftingType.COMBINE )
		{
			result.append( "Meatpasting" );
		}
		else if ( mixingMethod == CraftingType.COOK )
		{
			result.append( "Cooking" );
		}
		else if ( mixingMethod == CraftingType.MIX )
		{
			result.append( "Mixing" );
		}
		else if ( mixingMethod == CraftingType.SMITH )
		{
			result.append( "Meatsmithing" );
		}
		else if ( mixingMethod == CraftingType.SSMITH )
		{
			result.append( "Meatsmithing (not Innabox)" );
		}
		else if ( mixingMethod == CraftingType.STILL_BOOZE )
		{
			result.append( "Nash Crosby's Still" );
		}
		else if ( mixingMethod == CraftingType.STILL_MIXER )
		{
			result.append( "Nash Crosby's Still" );
		}
		else if ( mixingMethod == CraftingType.WOK )
		{
			result.append( "Wok of Ages" );
		}
		else if ( mixingMethod == CraftingType.MALUS )
		{
			result.append( "Malus of Forethought" );
		}
		else if ( mixingMethod == CraftingType.JEWELRY )
		{
			result.append( "Jewelry-making pliers" );
		}
		else if ( mixingMethod == CraftingType.STARCHART )
		{
			result.append( "star chart" );
		}
		else if ( mixingMethod == CraftingType.SUGAR_FOLDING )
		{
			result.append( "sugar sheet" );
		}
		else if ( mixingMethod == CraftingType.PIXEL )
		{
			result.append( "Crackpot Mystic" );
		}
		else if ( mixingMethod == CraftingType.ROLLING_PIN )
		{
			result.append( "rolling pin/unrolling pin" );
		}
		else if ( mixingMethod == CraftingType.GNOME_TINKER )
		{
			result.append( "Supertinkering" );
		}
		else if ( mixingMethod == CraftingType.STAFF )
		{
			result.append( "Rodoric, the Staffcrafter" );
		}
		else if ( mixingMethod == CraftingType.SUSHI )
		{
			result.append( "sushi-rolling mat" );
		}
		else if ( mixingMethod == CraftingType.SINGLE_USE )
		{
			result.append( "single-use" );
		}
		else if ( mixingMethod == CraftingType.MULTI_USE )
		{
			result.append( "multi-use" );
		}
		else if ( mixingMethod == CraftingType.CRIMBO05 )
		{
			result.append( "Crimbo Town Toy Factory (Crimbo 2005)" );
		}
		else if ( mixingMethod == CraftingType.CRIMBO06 )
		{
			result.append( "Uncle Crimbo's Mobile Home (Crimboween 2006)" );
		}
		else if ( mixingMethod == CraftingType.CRIMBO07 )
		{
			result.append( "Uncle Crimbo's Mobile Home (Crimbo 2007)" );
		}
		else if ( mixingMethod == CraftingType.CRIMBO12 )
		{
			result.append( "Uncle Crimbo's Futuristic Trailer (Crimboku 2012)" );
		}
		else if ( mixingMethod == CraftingType.PHINEAS )
		{
			result.append( "Phineas" );
		}
		else if ( mixingMethod == CraftingType.COOK_FANCY )
		{
			result.append( "Cooking (fancy)" );
		}
		else if ( mixingMethod == CraftingType.MIX_FANCY )
		{
			result.append( "Mixing (fancy)" );
		}
		else if ( mixingMethod == CraftingType.ACOMBINE )
		{
			result.append( "Meatpasting (not untinkerable)" );
		}
		else if ( mixingMethod == CraftingType.COINMASTER )
		{
			result.append( "Coin Master purchase" );
		}
		else if ( mixingMethod == CraftingType.CLIPART )
		{
			result.append( "Summon Clip Art" );
		}


		if ( result.length() == 0 )
		{
			result.append( "[unknown method of creation]" );
		}

		if ( mixingRequirements.contains( CraftingRequirements.MALE ) )
			result.append( " (males only)" );

		if ( mixingRequirements.contains( CraftingRequirements.FEMALE ) )
			result.append( " (females only)" );

		if ( mixingRequirements.contains( CraftingRequirements.SSPD ) )
			result.append( " (St. Sneaky Pete's Day only)" );

		if ( mixingRequirements.contains( CraftingRequirements.HAMMER ) )
			result.append( " (tenderizing hammer)" );

		if ( mixingRequirements.contains( CraftingRequirements.GRIMACITE ) )
			result.append( " (depleted Grimacite hammer)" );

		if ( mixingRequirements.contains( CraftingRequirements.TORSO ) )
			result.append( " (Torso Awaregness)" );

		if ( mixingRequirements.contains( CraftingRequirements.SUPER_MEATSMITHING ) )
			result.append( " (Super-Advanced Meatsmithing)" );

		if ( mixingRequirements.contains( CraftingRequirements.ARMORCRAFTINESS ) )
			result.append( " (Armorcraftiness)" );

		if ( mixingRequirements.contains( CraftingRequirements.EXPENSIVE ) )
			result.append( " (Really Expensive Jewelrycrafting)" );

		if ( mixingRequirements.contains( CraftingRequirements.REAGENT ) )
			result.append( " (Advanced Saucecrafting)" );

		if ( mixingRequirements.contains( CraftingRequirements.WAY ) )
			result.append( " (The Way of Sauce)" );

		if ( mixingRequirements.contains( CraftingRequirements.DEEP_SAUCERY ) )
			result.append( " (Deep Saucery)" );

		if ( mixingRequirements.contains( CraftingRequirements.PASTA ) )
			result.append( " (Pastamastery)" );

		if ( mixingRequirements.contains( CraftingRequirements.TEMPURAMANCY ) )
			result.append( " (Tempuramancy)" );

		if ( mixingRequirements.contains( CraftingRequirements.AC ) )
			result.append( " (Advanced Cocktailcrafting)" );

		if ( mixingRequirements.contains( CraftingRequirements.SHC ) )
			result.append( " (Superhuman Cocktailcrafting)" );

		if ( mixingRequirements.contains( CraftingRequirements.SALACIOUS ) )
			result.append( " (Salacious Cocktailcrafting)" );

		if ( mixingRequirements.contains( CraftingRequirements.NOBEE ) )
			result.append( " (Unavailable in Beecore)" );

		return result.toString();
	}

	/**
	 * Returns the item Ids of the ingredients for the given item. Note
	 * that if there are no ingredients, then <code>null</code> will be
	 * returned instead.
	 */

	public static final AdventureResult[] getIngredients( final int itemId )
	{
		return ConcoctionDatabase.getIngredients( ConcoctionDatabase.getStandardIngredients( itemId ) );
	}

	public static final AdventureResult[] getIngredients( final String name )
	{
		return ConcoctionDatabase.getIngredients( ConcoctionDatabase.getStandardIngredients( name ) );
	}

	public static final AdventureResult[] getIngredients( AdventureResult[] ingredients )
	{
		List availableIngredients = ConcoctionDatabase.getAvailableIngredients();
		return ConcoctionDatabase.getIngredients( ingredients, availableIngredients );
	}

	private static final AdventureResult[] getIngredients( AdventureResult[] ingredients, List availableIngredients )
	{
		// Ensure that you're retrieving the same ingredients that
		// were used in the calculations.  Usually this is the case,
		// but ice-cold beer and ketchup are tricky cases.

		if ( ingredients.length > 2 )
		{	// This is not a standard crafting recipe - and in the one case
			// where such a recipe uses one of these ingredients (Sir Schlitz
			// for the Staff of the Short Order Cook), it's not interchangeable.
			return ingredients;
		}

		for ( int i = 0; i < ingredients.length; ++i )
		{
			switch ( ingredients[ i ].getItemId() )
			{
			case ItemPool.SCHLITZ:
			case ItemPool.WILLER:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.SCHLITZ, ItemPool.WILLER, availableIngredients );
				break;

			case ItemPool.KETCHUP:
			case ItemPool.CATSUP:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.KETCHUP, ItemPool.CATSUP, availableIngredients );
				break;

			case ItemPool.DYSPEPSI_COLA:
			case ItemPool.CLOACA_COLA:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.DYSPEPSI_COLA, ItemPool.CLOACA_COLA, availableIngredients );
				break;

			case ItemPool.TITANIUM_UMBRELLA:
			case ItemPool.GOATSKIN_UMBRELLA:
				ingredients[ i ] = ConcoctionDatabase.getBetterIngredient(
					ItemPool.TITANIUM_UMBRELLA, ItemPool.GOATSKIN_UMBRELLA, availableIngredients );
				break;
			}
		}
		return ingredients;
	}

	public static final int getYield( final int itemId )
	{
		Concoction item = ConcoctionPool.get( itemId );
		return item == null ? 1 : item.getYield();
	}

	public static final AdventureResult[] getStandardIngredients( final int itemId )
	{
		return ConcoctionDatabase.getStandardIngredients( ConcoctionPool.get( itemId ) );
	}

	public static final AdventureResult[] getStandardIngredients( final String name )
	{
		return ConcoctionDatabase.getStandardIngredients( ConcoctionPool.get( name ) );
	}

	public static final AdventureResult[] getStandardIngredients( final Concoction item )
	{
		return item == null ? ConcoctionDatabase.NO_INGREDIENTS : item.getIngredients();
	}

	private static final AdventureResult getBetterIngredient( final int itemId1,
		final int itemId2, final List availableIngredients )
	{
		AdventureResult ingredient1 = ItemPool.get( itemId1, 1 );
		AdventureResult ingredient2 = ItemPool.get( itemId2, 1 );
		int diff = ingredient1.getCount( availableIngredients ) -
			ingredient2.getCount( availableIngredients );
		if ( diff == 0 )
		{
			diff = MallPriceDatabase.getPrice( itemId2 ) -
				MallPriceDatabase.getPrice( itemId1 );
		}
		return diff > 0 ? ingredient1 : ingredient2;
	}

	public static final int getPullsBudgeted()
	{
		return ConcoctionDatabase.pullsBudgeted;
	}

	public static int pullsBudgeted = 0;
	public static int pullsRemaining = 0;
	public static final int getPullsRemaining()
	{
		return pullsRemaining;
	}

	private static void addCraftingData( String mix, String name )
	{
		System.out.println( "name = " + name + " mix = " + mix );
		CraftingType currentMixingMethod = ConcoctionDatabase.mixingMethod;
		// Items anybody can create using meat paste or The Plunger
		if ( mix.equals( "COMBINE") )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COMBINE;
		}
		// Items anybody can create with an E-Z Cook Oven or Dramatic Range
		else if ( mix.equals( "COOK" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK;
		}
		// Items anybody can create with a Shaker or Cocktailcrafting Kit
		else if ( mix.equals( "MIX" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MIX;
		}
		// Items anybody can create with a tenderizing hammer or via Innabox
		else if ( mix.equals( "SMITH" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SMITH;
		}
		// Items that can only be created with a tenderizing hammer, not via Innabox
		else if ( mix.equals( "SSMITH" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
		}
		// Items requiring access to Nash Crosby's Still -- booze
		else if ( mix.equals( "BSTILL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.STILL_BOOZE;
		}
		// Items requiring Superhuman Cocktailcrafting -- mixer
		else if ( mix.equals( "MSTILL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.STILL_MIXER;
		}
		// Items requiring access to the Wok of Ages
		else if ( mix.equals( "WOK" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.WOK;
		}
		// Items requiring access to the Malus of Forethought
		else if ( mix.equals( "MALUS" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MALUS;
		}
		// Items anybody can create with jewelry-making pliers
		else if ( mix.equals( "JEWEL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.JEWELRY;
		}
		// Items anybody can create with starcharts, stars, and lines
		else if ( mix.equals( "STAR" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.STARCHART;
		}
		// Items anybody can create by folding sugar sheets
		else if ( mix.equals( "SUGAR" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SUGAR_FOLDING;
		}
		// Items anybody can create with pixels
		else if ( mix.equals( "PIXEL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.PIXEL;
		}
		// Items created with a rolling pin or and an unrolling pin
		else if ( mix.equals( "ROLL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.ROLLING_PIN;
		}
		// Items requiring access to the Gnome supertinker
		else if ( mix.equals( "TINKER" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.GNOME_TINKER;
		}
		// Items requiring access to Roderick the Staffmaker
		else if ( mix.equals( "STAFF" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.STAFF;
		}
		// Items anybody can create with a sushi-rolling mat
		else if ( mix.equals( "SUSHI" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SUSHI;
		}
		// Items created by single (or multi) using a single item.
		// Extra ingredients might also be consumed.
		// Multi-using multiple of the item creates multiple results.
		else if ( mix.equals( "SUSE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SINGLE_USE;
		}
		// Items created by multi-using specific # of a single item.
		// Extra ingredients might also be consumed.
		// You must create multiple result items one at a time.
		else if ( mix.equals( "MUSE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MULTI_USE;
		}
		// Items formerly creatable in Crimbo Town during Crimbo 2005
		else if ( mix.equals( "CRIMBO05" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO05;
		}
		// Items formerly creatable in Crimbo Town during Crimbo 2006
		else if ( mix.equals( "CRIMBO06" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO06;
		}
		// Items formerly creatable in Crimbo Town during Crimbo 2007
		else if ( mix.equals( "CRIMBO07" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO07;
		}
		// Items formerly creatable in Crimbo Town during Crimbo 2012
		else if ( mix.equals( "CRIMBO12" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.CRIMBO12;
		}
		// Items requiring access to Phineas
		else if ( mix.equals( "PHINEAS" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.PHINEAS;
		}
		// Items that require a Dramatic Range
		else if ( mix.equals( "COOK_FANCY" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
		}
		// Items that require a Cocktailcrafting Kit
		else if ( mix.equals( "MIX_FANCY" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
		}
		// Un-untinkerable Meatpasting
		else if ( mix.equals( "ACOMBINE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.ACOMBINE;
		}
		// Summon Clip Art items
		else if ( mix.equals( "CLIPART" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.CLIPART;
		}
		else if ( mix.equals( "MALE" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.MALE );
		}
		else if ( mix.equals( "FEMALE" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.FEMALE );
		}
		// Can only be made on St. Sneaky Pete's Day
		else if ( mix.equals( "SSPD" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.SSPD );
		}
		// Requires tenderizing hammer (implied for SMITH & SSMITH)
		else if ( mix.equals( "HAMMER" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.HAMMER );
		}
		// Requires depleted Grimacite hammer
		else if ( mix.equals( "GRIMACITE" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.GRIMACITE );
		}
		// Requires Torso Awaregness
		else if ( mix.equals( "TORSO" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.TORSO );
		}
		// Requires Super-Advanced Meatsmithing
		else if ( mix.equals( "WEAPON" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.SUPER_MEATSMITHING );
		}
		// Requires Armorcraftiness
		else if ( mix.equals( "ARMOR" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.ARMORCRAFTINESS );
		}
		// Requires Really Expensive Jewerlycrafting
		else if ( mix.equals( "EXPENSIVE" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.EXPENSIVE );
		}
		// Requires Advanced Saucecrafting
		else if ( mix.equals( "REAGENT" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.REAGENT );
		}
		// Requires The Way of Sauce
		else if ( mix.equals( "WAY" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.WAY );
		}
		// Requires Deep Saucery
		else if ( mix.equals( "DEEP" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.DEEP_SAUCERY );
		}
		// Requires Pastamastery
		else if ( mix.equals( "PASTAMASTERY" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.PASTA );
		}
		// Requires Tempuramancy
		else if ( mix.equals( "TEMPURAMANCY" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.TEMPURAMANCY );
		}
		// Requires Advanced Cocktailcrafting
		else if ( mix.equals( "AC" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.AC );
		}
		// Requires Superhuman Cocktailcrafting
		else if ( mix.equals( "SHC" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.SHC );
		}
		// Requires Salacious Cocktailcrafting
		else if ( mix.equals( "SALACIOUS" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.SALACIOUS );
		}
		// Items creatable only if not on Bees Hate You path
		else if ( mix.equals( "NOBEE" ) )
		{
			ConcoctionDatabase.requirements.add( CraftingRequirements.NOBEE );
		}
		// Saucerors make 3 of this item at a time
		else if ( mix.equals( "SX3" ) )
		{
			ConcoctionDatabase.info.add( CraftingMisc.TRIPLE_SAUCE );
		}
		// Recipe unexpectedly does not appear in Discoveries, even though
		// it uses a discoverable crafting type
		else if ( mix.equals( "NODISCOVERY" ) )
		{
			ConcoctionDatabase.info.add( CraftingMisc.NODISCOVERY );
		}
		// Recipe should never be used automatically
		else if ( mix.equals( "MANUAL" ) )
		{
			ConcoctionDatabase.info.add( CraftingMisc.MANUAL );
		}
		// Items requiring Pastamastery
		else if ( mix.equals( "PASTA" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.PASTA );
		}
		// Items requiring Tempuramancy
		else if ( mix.equals( "TEMPURA" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.TEMPURAMANCY );
		}
		// Items requiring Super-Advanced Meatsmithing
		else if ( mix.equals( "WSMITH" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
			ConcoctionDatabase.requirements.add( CraftingRequirements.SUPER_MEATSMITHING );
		}
		// Items requiring Armorcraftiness
		else if ( mix.equals( "ASMITH" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.SSMITH;
			ConcoctionDatabase.requirements.add( CraftingRequirements.ARMORCRAFTINESS );
		}
		// Items requiring Advanced Cocktailcrafting
		else if ( mix.equals( "ACOCK" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.AC );
		}
		// Items requiring Superhuman Cocktailcrafting
		else if ( mix.equals( "SCOCK" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.SHC );
		}
		// Items requiring Salacious Cocktailcrafting
		else if ( mix.equals( "SACOCK" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.MIX_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.SALACIOUS );
		}
		// Items requiring pliers and Really Expensive Jewelrycrafting
		else if ( mix.equals( "EJEWEL" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.JEWELRY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.EXPENSIVE );
		}
		// Items requiring Advanced Saucecrafting
		else if ( mix.equals( "SAUCE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.REAGENT );
		}
		// Items requiring The Way of Sauce
		else if ( mix.equals( "SSAUCE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.REAGENT );
		}
		// Items requiring Deep Saucery
		else if ( mix.equals( "DSAUCE" ) )
		{
			ConcoctionDatabase.mixingMethod = CraftingType.COOK_FANCY;
			ConcoctionDatabase.requirements.add( CraftingRequirements.DEEP_SAUCERY );
		}

		else
		{
			RequestLogger.printLine( "Unknown mixing method or flag (" + mix + ") for concoction: " + name );
		}

		if ( currentMixingMethod != null && currentMixingMethod != ConcoctionDatabase.mixingMethod )
		{
			RequestLogger.printLine( "Multiple mixing methods for concoction: " + name );
		}
	}

	public static final void setPullsRemaining( final int pullsRemaining )
	{
		if ( ConcoctionDatabase.pullsRemaining == pullsRemaining )
		{
			return;
		}
		
		ConcoctionDatabase.pullsRemaining = pullsRemaining;

		if ( !StaticEntity.isHeadless() )
		{
			ItemManageFrame.updatePullsRemaining( pullsRemaining );
			CoinmastersFrame.externalUpdate();
		}

		if ( pullsRemaining < pullsBudgeted )
		{
			ConcoctionDatabase.setPullsBudgeted( pullsRemaining );
		}
	}

	public static final void setPullsBudgeted( int pullsBudgeted )
	{
		if ( pullsBudgeted < queuedPullsUsed )
		{
			pullsBudgeted = queuedPullsUsed;
		}

		if ( pullsBudgeted > pullsRemaining )
		{
			pullsBudgeted = pullsRemaining;
		}

		ConcoctionDatabase.pullsBudgeted = pullsBudgeted;

		if ( !StaticEntity.isHeadless() )
		{
			ItemManageFrame.updatePullsBudgeted( pullsBudgeted );
		}
	}
}
