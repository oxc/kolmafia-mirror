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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import net.sourceforge.kolmafia.Modifiers;
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

	public static boolean newEffects = false;

	static
	{
		EffectDatabase.reset();
	}

	public static void reset()
	{
		EffectDatabase.newEffects = false;

		BufferedReader reader =
			FileUtilities.getVersionedReader( "statuseffects.txt", KoLConstants.STATUSEFFECTS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				Integer effectId = Integer.valueOf( data[ 0 ] );
				if ( effectId.intValue() < 0 )
				{
					continue;
				}

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
						image, null, defaultAction );
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
		if ( rv == null )
		{
			return null;
		}
		if ( rv.startsWith( "#" ) )
		{	// Callers of this API expect an actual command, not a note.
			return null;
		}
		return rv.split( "\\|" )[0];
	}

	public static final Iterator getAllActions( final String effectName )
	{
		String actions = StringUtilities.getDisplayName( (String) EffectDatabase.defaultActions.get( StringUtilities.getCanonicalName( effectName ) ) );
		if ( actions == null )
		{
			return Collections.EMPTY_LIST.iterator();
		}
		ArrayList rv = new ArrayList();
		String[] pieces = actions.split( "\\|" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			String action = pieces[ i ];
			String[] either = action.split( " ", 3 );
			if ( either.length == 3 && either[ 1 ].equals( "either" ) )
			{	// Split commands like "use either X, Y" into "use X", "use Y"
				String cmd = either[ 0 ];
				either = either[ 2 ].split( "\\s*,\\s*" );
				for ( int j = 0; j < either.length; ++j )
				{
					rv.add( cmd + " " + either[ j ] );
				}
			}
			else
			{
				rv.add( action );
			}
		}
			
		return rv.iterator();
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
	 * Returns whether or not an effect with a given name exists in the database
	 *
	 * @param effectName The name of the effect to lookup
	 * @return <code>true</code> if the effect is in the database
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

	public static final int learnEffectId( String name, String descId )
	{
		// Load the description text for this effect
		String text = DebugDatabase.readEffectDescriptionText( descId );
		if ( text == null )
		{
			return -1;
		}

		int effectId = DebugDatabase.parseEffectId( text );
		if ( effectId == -1 )
		{
			return -1;
		}

		String image = DebugDatabase.parseImage( text );

		// Detach name, descid, and image from being substrings
		name = new String( name );
		descId = new String( descId );
		image = new String( image );

		String canonicalName = StringUtilities.getCanonicalName( name );
		String displayName = StringUtilities.getDisplayName( name );
		Integer id = new Integer( effectId );

		EffectDatabase.nameById.put( id, displayName );
		EffectDatabase.dataNameById.put( id, name );
		EffectDatabase.effectByName.put( canonicalName, id );
		EffectDatabase.imageById.put( id, image );
		EffectDatabase.descriptionById.put( id, descId );
		EffectDatabase.effectByDescription.put( descId, id );

		RequestLogger.printLine( "--------------------" );
		RequestLogger.printLine( EffectDatabase.effectString( effectId, name, image, descId, null ) );

		// Let modifiers database do what it wishes with this effect
		Modifiers.registerEffect( name, text );

		// Done generating data
		RequestLogger.printLine( "--------------------" );

		EffectDatabase.newEffects = true;

		return effectId;
	}

	public static final void writeEffects( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.STATUSEFFECTS_VERSION );

		Iterator it = EffectDatabase.dataNameById.entrySet().iterator();
		int lastInteger = 1;

		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer nextInteger = (Integer) entry.getKey();
			int effectId = nextInteger.intValue();

                        // Skip pseudo effects
                        if ( effectId < 1 )
                        {
                                continue;
                        }

			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = effectId + 1;

			String name = (String) entry.getValue();
			String image = (String) EffectDatabase.imageById.get( nextInteger );
			String descId = (String) EffectDatabase.descriptionById.get( nextInteger );
			String canonicalName = StringUtilities.getCanonicalName( name );
			String defaultAction = (String) EffectDatabase.defaultActions.get( canonicalName );
			EffectDatabase.writeEffect( writer, effectId, name, image, descId, defaultAction );
		}

		writer.close();
	}

	public static void writeEffect( final PrintStream writer, final int effectId, final String name,
					final String image, final String descId, final String defaultAction )
	{
		writer.println( EffectDatabase.effectString( effectId, name, image, descId, defaultAction ) );
	}

	public static String effectString( final int effectId, final String name,
					   String image, String descId, String defaultAction )
	{
		// The effect file can have 3, 4, or 5 fields. "image" must be
		// present, even if we don't have the actual file name.
		if ( image == null )
		{
			image = "";
		}

		if ( defaultAction != null )
		{
			// It's unlikely we'll know the default action without
			// knowing the effect ID, but handle it just in case
			if ( descId == null )
			{
				descId = "";
			}
			return effectId + "\t" + name + "\t" + image + "\t" + descId + "\t" + defaultAction;
		}

		if ( descId != null )
		{
			return effectId + "\t" + name + "\t" + image + "\t" + descId;
		}

		return effectId + "\t" + name + "\t" + image;
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
