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
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EffectDatabase
{
	private static String [] canonicalNames = new String[0];
	private static final Map nameById = new TreeMap();
	private static final Map dataNameById = new TreeMap();
	private static final Map effectByName = new TreeMap();
	private static final HashMap defaultActions = new HashMap();

	private static final Map imageById = new HashMap();
	private static final Map descriptionById = new TreeMap();
	private static final Map effectByDescription = new HashMap();

	static
	{
		BufferedReader reader =
			FileUtilities.getVersionedReader( "statuseffects.txt", KoLConstants.STATUSEFFECTS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				Integer effectId = Integer.valueOf( data[ 0 ] );
				String name = data[ 1 ];
				String image = data[ 2 ];
				String descId = data.length > 3 ? data[ 3 ] : null;
				String defaultAction = data.length > 4 ? data[ 4 ] : null;

				EffectDatabase.addToDatabase(
					effectId,
					name,
					image, descId, defaultAction );

				if ( name.equalsIgnoreCase( "Temporary Blindness" ) )
				{
					// We need two copies of this effect in
					// activeEffects, since the intrinsic &
					// normal versions stack.
					EffectDatabase.addToDatabase(
						new Integer( -2 ),
						"Temporary Blindness (intrinsic)",
						image, descId, defaultAction );
				}
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

		EffectDatabase.canonicalNames = new String[ EffectDatabase.effectByName.size() ];
		EffectDatabase.effectByName.keySet().toArray( EffectDatabase.canonicalNames );
	}

	private static final void addToDatabase( final Integer effectId, final String name, final String image,
		final String descriptionId, final String defaultAction )
	{
		String canonicalName = StringUtilities.getCanonicalName( name );
		String displayName = StringUtilities.getDisplayName( name );
		EffectDatabase.nameById.put( effectId, displayName );
		EffectDatabase.dataNameById.put( effectId, name );
		EffectDatabase.effectByName.put( canonicalName, effectId );
		EffectDatabase.imageById.put( effectId, image );

		if ( descriptionId != null )
		{
			EffectDatabase.descriptionById.put( effectId, descriptionId );
			EffectDatabase.effectByDescription.put( descriptionId, effectId );
		}

		if ( defaultAction != null )
		{
			EffectDatabase.defaultActions.put( canonicalName, defaultAction );
		}
	}

	public static final String getDefaultAction( final String effectName )
	{
		String rv = StringUtilities.getDisplayName( (String) EffectDatabase.defaultActions.get( StringUtilities.getCanonicalName( effectName ) ) );
		if ( rv != null && rv.startsWith( "#" ) )
		{	// Callers of this API expect an actual command, not a note.
			rv = null;
		}
		return rv;
	}

	public static final String getActionNote( final String effectName )
	{
		String rv = StringUtilities.getDisplayName( (String) EffectDatabase.defaultActions.get( StringUtilities.getCanonicalName( effectName ) ) );
		if ( rv != null && rv.startsWith( "#" ) )
		{
			return rv.substring( 1 ).trim();
		}
		return null;
	}

	/**
	 * Returns the name for an effect, given its Id.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getEffectName( final int effectId )
	{
		return effectId == -1 ?
			"Unknown effect" :
			StringUtilities.getDisplayName( (String) EffectDatabase.nameById.get( new Integer( effectId ) ) );
	}

	public static final String getEffectDataName( final int effectId )
	{
		return effectId == -1 ?
			null:
			(String) EffectDatabase.dataNameById.get( new Integer( effectId ) );
	}

	public static final String getEffectName( final String descriptionId )
	{
		Object effectId = EffectDatabase.effectByDescription.get( descriptionId );
		return effectId == null ? null : EffectDatabase.getEffectName( ( (Integer) effectId ).intValue() );
	}

	public static final int getEffect( final String descriptionId )
	{
		Object effectId = EffectDatabase.effectByDescription.get( descriptionId );
		return effectId == null ? -1 : ( (Integer) effectId ).intValue();
	}

	public static final String getDescriptionId( final int effectId )
	{
		return (String) EffectDatabase.descriptionById.get( new Integer( effectId ) );
	}

	public static final Set descriptionIdKeySet()
	{
		return EffectDatabase.descriptionById.keySet();
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectName The name of the effect to lookup
	 * @return The Id number of the corresponding effect
	 */

	public static final int getEffectId( final String effectName )
	{
		Object effectId = EffectDatabase.effectByName.get( StringUtilities.getCanonicalName( effectName ) );
		if ( effectId != null )
		{
			return ( (Integer) effectId ).intValue();
		}

		List names = EffectDatabase.getMatchingNames( effectName );
		if ( names.size() == 1 )
		{
			return EffectDatabase.getEffectId( (String) names.get( 0 ) );
		}

		return -1;
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getImage( final int effectId )
	{
		Object imageName = effectId == -1 ? null : EffectDatabase.imageById.get( new Integer( effectId ) );
		return imageName == null ? "/images/debug.gif" : "http://images.kingdomofloathing.com/itemimages/" + imageName;
	}

	/**
	 * Returns the set of status effects keyed by Id
	 *
	 * @return The set of status effects keyed by Id
	 */

	public static final Set entrySet()
	{
		return EffectDatabase.nameById.entrySet();
	}

	public static final Set dataNameEntrySet()
	{
		return EffectDatabase.dataNameById.entrySet();
	}

	public static final Collection values()
	{
		return EffectDatabase.nameById.values();
	}

	/**
	 * Returns whether or not an item with a given name exists in the database; this is useful in the event that an item
	 * is encountered which is not tradeable (and hence, should not be displayed).
	 *
	 * @param effectName The name of the effect to lookup
	 * @return <code>true</code> if the item is in the database
	 */

	public static final boolean contains( final String effectName )
	{
		return Arrays.binarySearch( EffectDatabase.canonicalNames, StringUtilities.getCanonicalName( effectName ) ) >= 0;
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames( EffectDatabase.canonicalNames, substring );
	}

	public static final void addDescriptionId( final int effectId, final String descriptionId )
	{
		if ( effectId == -1 )
		{
			return;
		}

		Integer id = new Integer( effectId );

		EffectDatabase.effectByDescription.put( descriptionId, id );
		EffectDatabase.descriptionById.put( id, descriptionId );

		EffectDatabase.saveDataOverride();
	}

	private static final Pattern STATUS_EFFECT_PATTERN =
		Pattern.compile( "<input type=radio name=whicheffect value=(\\d+)></td><td><img src=\"http://images.kingdomofloathing.com/itemimages/(.*?)\" width=30 height=30></td><td>(.*?) \\(" );

	public static final void findStatusEffects()
	{
		if ( !KoLConstants.inventory.contains( UneffectRequest.REMEDY ) )
		{
			return;
		}

		GenericRequest effectChecker = new GenericRequest( "uneffect.php" );
		RequestLogger.printLine( "Checking for new status effects..." );
		RequestThread.postRequest( effectChecker );

		Matcher effectsMatcher = EffectDatabase.STATUS_EFFECT_PATTERN.matcher( effectChecker.responseText );
		boolean foundChanges = false;

		while ( effectsMatcher.find() )
		{
			Integer effectId = Integer.valueOf( effectsMatcher.group( 1 ) );
			if ( EffectDatabase.nameById.containsKey( effectId ) )
			{
				continue;
			}

			foundChanges = true;
			EffectDatabase.addToDatabase(
				effectId, effectsMatcher.group( 3 ), effectsMatcher.group( 2 ), null, null );
		}

		RequestThread.postRequest( CharPaneRequest.getInstance() );

		if ( foundChanges )
		{
			EffectDatabase.canonicalNames = new String[ EffectDatabase.effectByName.size() ];
			EffectDatabase.effectByName.keySet().toArray( EffectDatabase.canonicalNames );

			EffectDatabase.saveDataOverride();
		}
	}

	public static final void saveDataOverride()
	{
		File output = new File( UtilityConstants.DATA_LOCATION, "statuseffects.txt" );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.STATUSEFFECTS_VERSION );

		int lastInteger = 1;
		Iterator it = EffectDatabase.dataNameById.entrySet().iterator();

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();

			Integer nextInteger = (Integer) entry.getKey();
			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = nextInteger.intValue() + 1;

			String name = (String) entry.getValue();
			String image = (String) EffectDatabase.imageById.get( nextInteger );

			writer.print( nextInteger + "\t" + name + "\t" + image );

			writer.print( "\t" );
			if ( EffectDatabase.descriptionById.containsKey( nextInteger ) )
			{
				String effectId = (String) EffectDatabase.descriptionById.get( nextInteger );
				writer.print( effectId );
			}

			String canonicalName = StringUtilities.getCanonicalName( name );
			writer.print( "\t" );
			if ( EffectDatabase.defaultActions.containsKey( canonicalName ) )
			{
				String defaultAction = (String) EffectDatabase.defaultActions.get( canonicalName );
				writer.print( defaultAction );
			}


			writer.println();
		}

		writer.close();
	}

	/**
	 * Utility method which determines the first effect which matches the given parameter string. Note that the string
	 * may also specify an effect duration before the string.
	 */
	
	public static final AdventureResult getFirstMatchingEffect( final String parameters )
	{
		String effectName = null;
		int duration = 0;
	
		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.
	
		List matchingNames = getMatchingNames( parameters );
	
		if ( matchingNames.size() != 0 )
		{
			effectName = (String) matchingNames.get( 0 );
			duration = 1;
		}
		else
		{
			String durationString = "";
			int spaceIndex = parameters.indexOf( " " );
			
			if ( spaceIndex != -1 )
			{
				durationString = parameters.substring( 0, spaceIndex );
			}
			
			if ( durationString.equals( "*" ) )
			{
				duration = 0;
			}
			else
			{
				if ( StringUtilities.isNumeric( durationString ) )
				{
					duration = StringUtilities.parseInt( durationString );
				}
				else
				{
					durationString = "";
					duration = 1;
				}
			}
			
			String effectNameString = parameters.substring( durationString.length() ).trim();
	
			matchingNames = getMatchingNames( effectNameString );
	
			if ( matchingNames.size() == 0 )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"[" + effectNameString + "] does not match anything in the status effect database." );

				return null;
			}
	
			effectName = (String) matchingNames.get( 0 );
		}
	
		if ( effectName == null )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "[" + parameters + "] does not match anything in the status effect database." );
			return null;
		}
	
		return new AdventureResult( effectName, duration, true );
	}
}
