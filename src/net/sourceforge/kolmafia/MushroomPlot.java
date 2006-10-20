/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MushroomPlot extends StaticEntity
{
	public static final File PLOT_DIRECTORY = new File( "planting" );
	private static final Pattern PLOT_PATTERN = Pattern.compile( "<b>Your Mushroom Plot:</b><p><table>(<tr>.*?</tr><tr>.*></tr><tr>.*?</tr><tr>.*</tr>)</table>" );
	private static final Pattern SQUARE_PATTERN = Pattern.compile( "<td>(.*?)</td>" );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( ".*/((.*)\\.gif)" );

	// The player's mushroom plot
	//
	//  1  2  3  4
	//  5  6  7  8
	//  9 10 11 12
	// 13 14 15 16

	private static String [][] actualPlot = new String[4][4];
	private static boolean ownsPlot = false;

	// Empty spot
	public static final int EMPTY = 0;

	// Sprout
	public static final int SPROUT = 1;

	// First generation mushrooms
	public static final int SPOOKY = 724;
	public static final int KNOB = 303;
	public static final int KNOLL = 723;

	// Second generation mushrooms
	public static final int WARM = 749;
	public static final int COOL = 751;
	public static final int POINTY = 753;

	// Third generation mushrooms
	public static final int FLAMING = 755;
	public static final int FROZEN = 756;
	public static final int STINKY = 757;

	// Special mushrooms
	public static final int GLOOMY = 1266;

	// Assocations between the mushroom IDs
	// and the mushroom image.

	public static final Object [][] MUSHROOMS =
	{
		// Sprout and emptiness
		{ new Integer( EMPTY ), "dirt1.gif", "__", "__", new Integer( 0 ), "empty" },
		{ new Integer( SPROUT ), "mushsprout.gif", "..", "..", new Integer( 0 ), "unknown" },

		// First generation mushrooms
		{ new Integer( KNOB ), "mushroom.gif", "kb", "KB", new Integer( 1 ), "knob" },
		{ new Integer( KNOLL ), "bmushroom.gif", "kn", "KN", new Integer( 2 ), "knoll" },
		{ new Integer( SPOOKY ), "spooshroom.gif", "sp", "SP", new Integer( 3 ), "spooky" },

		// Second generation mushrooms
		{ new Integer( WARM ), "flatshroom.gif", "wa", "WA", new Integer( 4 ), "warm" },
		{ new Integer( COOL ), "plaidroom.gif", "co", "CO", new Integer( 5 ), "cool" },
		{ new Integer( POINTY ), "tallshroom.gif", "po", "PO", new Integer( 6 ), "pointy" },

		// Third generation mushrooms
		{ new Integer( FLAMING ), "fireshroom.gif", "fl", "FL", new Integer( 7 ), "flaming" },
		{ new Integer( FROZEN ), "iceshroom.gif", "fr", "FR", new Integer( 8 ), "frozen" },
		{ new Integer( STINKY ), "stinkshroo.gif", "st", "ST", new Integer( 9 ), "stinky" },

		// Special mushrooms
		{ new Integer( GLOOMY ), "blackshroo.gif", "gl", "GL", new Integer( 10 ), "gloomy" },
	};

	public static final int [][] BREEDING =
	{
		// EMPTY,   KNOB,    KNOLL,   SPOOKY,  WARM,    COOL,    POINTY   FLAMING  FROZEN   STINKY   GLOOMY

		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // EMPTY
		{  EMPTY,   KNOB,    COOL,    WARM,    EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // KNOB
		{  EMPTY,   COOL,    KNOLL,   POINTY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // KNOLL
		{  EMPTY,   WARM,    POINTY,  SPOOKY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   GLOOMY,  EMPTY,   EMPTY  },  // SPOOKY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   WARM,    STINKY,  FLAMING, EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // WARM
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   STINKY,  COOL,    FROZEN,  EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // COOL
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   FLAMING, FROZEN,  POINTY,  EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // POINTY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   FLAMING, EMPTY,   EMPTY,   EMPTY  },  // FLAMING
		{  EMPTY,   EMPTY,   EMPTY,   GLOOMY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   FROZEN,  EMPTY,   EMPTY  },  // FROZEN
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   STINKY,  EMPTY  },  // STINKY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  }   // GLOOMY
	};

	// Spore data - includes price of the spore
	// and the item ID associated with the spore.

	private static final int [][] SPORE_DATA = { { SPOOKY, 30 }, { KNOB, 40 }, { KNOLL, 50 } };

	/**
	 * Static method which resets the state of the
	 * mushroom plot.  This should be used whenever
	 * the login process is restarted.
	 */

	public static void reset()
	{	ownsPlot = false;
	}

	/**
	 * Utility method which returns a two-dimensional
	 * array showing the arrangement of the plot.
	 */

	public static String getMushroomPlot( boolean isDataOnly )
	{
		initialize();
		return getMushroomPlot( isDataOnly, actualPlot );
	}

	/**
	 * Utility method which returns a two-dimensional
	 * array showing the arrangement of the forecasted
	 * plot (ie: what the plot will look like tomorrow).
	 */

	public static String getForecastedPlot( boolean isDataOnly )
	{	return getForecastedPlot( isDataOnly, actualPlot );
	}

	public static String getForecastedPlot( boolean isDataOnly, String [][] plot )
	{
		// Construct the forecasted plot now.

		boolean [][] changeList = new boolean[4][4];
		String [][] forecastPlot = new String[4][4];

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4; ++col )
			{
				if ( plot[ row ][ col ].equals( "__" ) )
				{
					forecastPlot[ row ][ col ] = getForecastSquare( row, col, plot );
					changeList[ row ][ col ] = !forecastPlot[ row ][ col ].equals( "__" );
				}
				else if ( plot[ row ][ col ].equals( plot[ row ][ col ].toLowerCase() ) )
				{
					forecastPlot[ row ][ col ] = plot[ row ][ col ].toUpperCase();
					changeList[ row ][ col ] = false;
				}
				else
				{
					forecastPlot[ row ][ col ] = plot[ row ][ col ];
					changeList[ row ][ col ] = false;
				}
			}
		}

		// Whenever the forecasted plot doesn't match the original plot, the
		// surrounding mushrooms are assumed to disappear.  Also forecast the
		// growth of the mushrooms.

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4; ++col )
			{
				if ( changeList[ row ][ col ] )
				{
					if ( row != 0 && forecastPlot[ row - 1 ][ col ].equals( forecastPlot[ row - 1 ][ col ].toUpperCase() ) )
						forecastPlot[ row - 1 ][ col ] = "__";

					if ( row != 3 && forecastPlot[ row + 1 ][ col ].equals( forecastPlot[ row + 1 ][ col ].toUpperCase() ) )
						forecastPlot[ row + 1 ][ col ] = "__";

					if ( col != 0 && forecastPlot[ row ][ col - 1 ].equals( forecastPlot[ row ][ col - 1 ].toUpperCase() ) )
						forecastPlot[ row ][ col - 1 ] = "__";

					if ( col != 3 && forecastPlot[ row ][ col + 1 ].equals( forecastPlot[ row ][ col + 1 ].toUpperCase() ) )
						forecastPlot[ row ][ col + 1 ] = "__";
				}
			}
		}

		return getMushroomPlot( isDataOnly, forecastPlot );
	}

	private static String getForecastSquare( int row, int col, String [][] plot )
	{
		String [] touched = new String[4];

		// First, determine what kinds of mushrooms
		// touch the square.

		touched[0] = row == 0 ? "__" : plot[ row - 1 ][ col ];
		touched[1] = row == 3 ? "__" : plot[ row + 1 ][ col ];
		touched[2] = col == 0 ? "__" : plot[ row ][ col - 1 ];
		touched[3] = col == 3 ? "__" : plot[ row ][ col + 1 ];

		// Determine how many adult mushrooms total touch
		// the square.

		int [] touchIndex = new int[4];
		int touchCount = 0;

		for ( int i = 0; i < 4; ++i )
		{
			if ( !touched[i].equals( "__" ) && !touched[i].equals( ".." ) )
			{
				for ( int j = 0; j < MUSHROOMS.length; ++j )
					if ( touched[i].equals( MUSHROOMS[j][3] ) )
						touchIndex[ touchCount ] = ((Integer)MUSHROOMS[j][4]).intValue();

				++touchCount;
			}
		}

		// If exactly two adult mushrooms are touching the
		// square, then return the result of the breed.

		if ( touchCount == 2 && BREEDING[ touchIndex[0] ][ touchIndex[1] ] != EMPTY )
			return getShorthand( BREEDING[ touchIndex[0] ][ touchIndex[1] ], false );

		// Otherwise, it'll be the same as whatever is
		// there right now.

		return plot[ row ][ col ];
	}

	private static String getShorthand( int mushroomType, boolean isAdult )
	{
		for ( int i = 0; i < MUSHROOMS.length; ++i )
			if ( mushroomType == ((Integer)MUSHROOMS[i][0]).intValue() )
				return isAdult ? (String) MUSHROOMS[i][3] : (String) MUSHROOMS[i][2];

		return "__";
	}

	private static String getMushroomPlot( boolean isDataOnly, String [][] plot )
	{
		// Otherwise, you need to construct the string form
		// of the mushroom plot.  Shorthand and hypertext are
		// the only two versions at the moment.

		StringBuffer plotBuffer = new StringBuffer();

		if ( !isDataOnly )
			plotBuffer.append( LINE_BREAK );

		for ( int row = 0; row < 4; ++row )
		{
			// In a hypertext document, you initialize the
			// row in the table before you start appending
			// the squares.

			for ( int col = 0; col < 4; ++col )
			{
				// Hypertext documents need to have their cells opened before
				// the cell can be printed.

				if ( !isDataOnly )
					plotBuffer.append( "  " );

				String square = plot[ row ][ col ];

				// Mushroom images are used in hypertext documents, while
				// shorthand notation is used in non-hypertext documents.

				plotBuffer.append( square == null ? "__" : square );

				// Hypertext documents need to have their cells closed before
				// another cell can be printed.

				if ( isDataOnly )
					plotBuffer.append( ";" );
			}

			if ( !isDataOnly )
				plotBuffer.append( LINE_BREAK );
		}

		// Now that the appropriate string has been constructed,
		// return it to the calling method.

		return plotBuffer.toString();
	}

	/**
	 * Utility method which retrieves the image associated
	 * with the given mushroom type.
	 */

	public static String getMushroomImage( String mushroomType )
	{
		for ( int i = 1; i < MUSHROOMS.length; ++i )
		{
			if ( mushroomType.equals( MUSHROOMS[i][2] ) )
				return "itemimages/mushsprout.gif";
			if ( mushroomType.equals( MUSHROOMS[i][3] ) )
				return "itemimages/" + MUSHROOMS[i][1];
		}

		return "itemimages/dirt1.gif";
	}

	/**
	 * Utility method which retrieves the mushroom which is
	 * associated with the given image.
	 */

	public static int getMushroomType( String mushroomImage )
	{
		for ( int i = 0; i < MUSHROOMS.length; ++i )
			if ( mushroomImage.endsWith( "/" + MUSHROOMS[i][1] ) )
				return ((Integer) MUSHROOMS[i][0]).intValue();

		return EMPTY;
	}

	/**
	 * One of the major functions of the mushroom plot handler,
	 * this method plants the given spore into the given position
	 * (or square) of the mushroom plot.
	 */

	public static boolean plantMushroom( int square, int spore )
	{
		// Validate square parameter.  It's possible that
		// the user input the wrong spore number.

		if ( square < 1 || square > 16 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Squares are numbered from 1 to 16." );
			return false;
		}

		// Determine the spore that the user wishes to
		// plant and the price for that spore.  Place
		// those into holder variables.

		int sporeIndex = -1, sporePrice = -1;
		for ( int i = 0; i < SPORE_DATA.length; ++i )
			if ( SPORE_DATA[i][0] == spore )
			{
				sporeIndex = i + 1;
				sporePrice = SPORE_DATA[i][1];
			}

		// If nothing was reset, then return from this
		// method after notifying the user that the spore
		// they provided is not plantable.

		if ( sporeIndex == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't plant that." );
			return false;
		}

		// Make sure we have enough meat to pay for the spore.
		// Rather than using requirements validation, check the
		// character data.

		if ( KoLCharacter.getAvailableMeat() < sporePrice )
			return false;

		// Make sure we know current state of mushroom plot
		// before we plant the mushroom.  Bail if it fails.

		if ( !initialize() )
			return false;

		// If the square isn't empty, pick what's there

		int row = (square - 1) / 4;
		int col = (square - 1) % 4;

		if ( !actualPlot[ row ][ col ].equals( "__" ) && !pickMushroom( square, true ) )
			return false;

		// Plant the requested spore.

		MushroomPlotRequest request = new MushroomPlotRequest( square, sporeIndex );
		KoLmafia.updateDisplay( "Planting " + TradeableItemDatabase.getItemName( spore ) + " spore in square " + square + "..." );
		request.run();

		// If it failed, bail.

		if ( !KoLmafia.permitsContinue() )
			return false;

		// Pay for the spore.  At this point, it's guaranteed
		// that theallows you to continue.

		getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - sporePrice ) );
		KoLmafia.updateDisplay( "Spore successfully planted." );
		return true;
	}

	public static void clearField()
	{
		for ( int i = 1; i <= 16; ++i )
			pickMushroom( i, true );
	}

	/**
	 * Picks all the mushrooms in all squares.  This is equivalent
	 * to harvesting your mushroom crop, hence the name.
	 */

	public static void harvestMushrooms()
	{
		for ( int i = 1; i <= 16; ++i )
			pickMushroom( i, false );
	}

	/**
	 * One of the major functions of the mushroom plot handler,
	 * this method picks the mushroom located in the given square.
	 */

	public static boolean pickMushroom( int square, boolean pickSpores )
	{
		// Validate square parameter.  It's possible that
		// the user input the wrong spore number.

		if ( square < 1 || square > 16 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Squares are numbered from 1 to 16." );
			return false;
		}

		// Make sure we know current state of mushroom plot
		// before we plant the mushroom.  Bail if it fails.

		if ( !initialize() )
			return false;

		// If the square is not empty, run a request to pick
		// the mushroom in the square.

		int row = (square - 1) / 4;
		int col = (square - 1) % 4;

		boolean shouldPick = !actualPlot[ row ][ col ].equals( "__" );
		shouldPick &= !actualPlot[ row ][ col ].equals( actualPlot[ row ][ col ].toLowerCase() ) || pickSpores;

		if ( shouldPick )
		{
			MushroomPlotRequest request = new MushroomPlotRequest( square );
			KoLmafia.updateDisplay( "Picking square " + square + "..." );
			request.run();
			KoLmafia.updateDisplay( "Square picked." );
		}

		return KoLmafia.permitsContinue();
	}

	/**
	 * Utility method used to initialize the state of
	 * the plot into the one-dimensional array.
	 */

	private static boolean initialize()
	{
		if ( !ownsPlot() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You haven't bought a mushroom plot yet." );
			return false;
		}

		return true;
	}

	public static boolean ownsPlot()
	{
		// If you're not in a Muscle sign, no go.

		if ( !KoLCharacter.inMuscleSign() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find the mushroom fields." );
			return false;
		}

		if ( ownsPlot )
			return true;

		(new MushroomPlotRequest()).run();
		return ownsPlot;
	}

	private static class MushroomPlotRequest extends KoLRequest
	{
		public MushroomPlotRequest()
		{	super( "knoll_mushrooms.php", true );
		}

		public MushroomPlotRequest( int square )
		{
			this();
			addFormField( "action", "click" );
			addFormField( "pos", String.valueOf( square - 1 ) );
		}

		public MushroomPlotRequest( int square, int spore )
		{
			this();
			addFormField( "action", "plant" );
			addFormField( "pos", String.valueOf( square - 1 ) );
			addFormField( "whichspore", String.valueOf( spore ) );
		}

		public void run()
		{
			super.run();
			parsePlot( responseText );
		}
	}

	public static void parsePlot( String text )
	{
		// Pretend all of the sections on the plot are empty
		// before you begin parsing the plot.

		for ( int row = 0; row < 4; ++row )
			for ( int col = 0; col < 4; ++col )
				actualPlot[ row ][ col ] = "__";

		Matcher plotMatcher = PLOT_PATTERN.matcher( text );
		ownsPlot = plotMatcher.find();

		// If there is no plot data, then we can assume that
		// the person does not own a plot.  Return from the
		// method if this is the case.  Otherwise, try to find
		// all of the squares.

		if ( !ownsPlot )
			return;

		Matcher squareMatcher = SQUARE_PATTERN.matcher( plotMatcher.group(1) );

		for ( int row = 0; row < 4; ++row )
			for ( int col = 0; col < 4 && squareMatcher.find(); ++col )
			{
				int result = parseSquare( squareMatcher.group(1) );
				actualPlot[ row ][ col ] = getShorthand( result, true );
			}
	}

	private static int parseSquare( String text )
	{
		// We figure out what's there based on the image.  This
		// is done by checking the text in the square against
		// the table of square values.

		Matcher gifMatcher = IMAGE_PATTERN.matcher( text );
		if ( gifMatcher.find() )
		{
			String gif = gifMatcher.group(1);
			for ( int i = 0; i < MUSHROOMS.length; ++i )
				if ( gif.equals( MUSHROOMS[i][1] ) )
					return ((Integer) MUSHROOMS[i][0]).intValue();
		}

		return EMPTY;
	}

	public static void loadLayout( String filename, String [][] originalData, String [][] planningData )
	{
		// The easiest file to parse that is already provided is
		// the text file which was generated automatically.

		BufferedReader reader = KoLDatabase.getReader( new File( "planting/" + filename + ".txt" ) );

		try
		{
			String line = "";
			int dayIndex = 0;
			String [][] arrayData = new String[4][4];

			while ( line != null )
			{
				if ( dayIndex == 0 )
				{
					for ( int i = 0; i < 16; ++i )
						originalData[ dayIndex ][i] = "__";
				}
				else if ( dayIndex < originalData.length )
				{
					for ( int i = 0; i < 4; ++i )
						for ( int j = 0; j < 4; ++j )
							arrayData[i][j] = planningData[ dayIndex - 1 ][ i * 4 + j ];

					originalData[ dayIndex ] = getForecastedPlot( true, arrayData ).split( ";" );
				}

				// Skip four lines from the mushroom plot,
				// which only contain header information.

				for ( int i = 0; i < 4 && line != null; ++i )
					line = reader.readLine();

				// Now, split the line into individual units
				// based on whitespace.

				if ( line != null )
				{
					// Get the plot that will result from the
					// previous day's plantings.

					for ( int i = 0; i < 4; ++i )
					{
						line = reader.readLine().trim();
						String [] pieces = line.split( "\\*?\\s+" );

						if ( line != null )
						{
							for ( int j = 4; j < 8; ++j )
								planningData[ dayIndex ][ i * 4 + j - 4 ] = pieces[j].substring( 0, 2 );
						}
					}

					// Now that you've wrapped up a day, eat
					// an empty line and continue on with the
					// next iteration.

					++dayIndex;
					line = reader.readLine();
				}
			}
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return;
		}

		// Make sure to close the reader after you're done reading
		// all the data in the file.

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return;
		}
	}

	private static void copyMushroomImage( String location )
	{
		File source = new File( "images/" + location );
		File destination = new File( PLOT_DIRECTORY + "/" + location );

		if ( !destination.getParentFile().exists() )
			destination.getParentFile().mkdirs();

		try
		{
			FileChannel sourceChannel = (new FileInputStream( source )).getChannel();
			FileChannel destinationChannel = (new FileOutputStream( destination )).getChannel();

			sourceChannel.transferTo( 0, sourceChannel.size(), destinationChannel );
			sourceChannel.close();
			destinationChannel.close();
		}
		catch ( Exception e )
		{
			// Copy failed.
		}
	}

	public static void saveLayout( String filename, String [][] originalData, String [][] planningData )
	{
		LogStream textLayout = null;
		LogStream htmlLayout = null;
		LogStream plotScript = null;

		if ( !PLOT_DIRECTORY.exists() )
			PLOT_DIRECTORY.mkdirs();

		try
		{
			textLayout = new LogStream( PLOT_DIRECTORY + "/" + filename + ".txt" );
			htmlLayout = new LogStream( PLOT_DIRECTORY + "/" + filename + ".htm" );
			plotScript = new LogStream( PLOT_DIRECTORY + "/" + filename + ".ash" );
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return;
		}

		// The HTML file needs a little bit of header information
		// to make it proper HTML.

		htmlLayout.println( "<html><body>" );

		// Now that we know that data files can be written okay,
		// begin writing layout data.

		ArrayList days = new ArrayList();
		String image = null;
		boolean isTodayEmpty = false;

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST && !isTodayEmpty; ++i )
		{
			textLayout.println();
			textLayout.println( "Day " + (i+1) + ":" );
			textLayout.println();
			textLayout.println( "Pick                Plant" );

			htmlLayout.println( "<table border=0 cellpadding=0 cellspacing=0>" );
			htmlLayout.println( "<tr><td colspan=9><b>Day " + (i+1) + ":" + "</b></td></tr>" );
			htmlLayout.println( "<tr><td colspan=4>Pick</td><td width=50>&nbsp;</td><td colspan=4>Plant</td></tr>" );

			// Compile all the commands which are needed for the
			// planting script.

			isTodayEmpty = true;
			ArrayList commands = new ArrayList();

			StringBuffer pickText = new StringBuffer();
			StringBuffer pickHtml = new StringBuffer();
			StringBuffer plantText = new StringBuffer();
			StringBuffer plantHtml = new StringBuffer();

			for ( int j = 0; j < 16; ++j )
			{
				if ( i == 0 )
					commands.add( "field pick " + (j+1) );

				// If you've reached the end of a row, then you
				// will need to add line breaks.

				if ( j > 0 && j % 4 == 0 )
				{
					textLayout.print( pickText.toString() );
					textLayout.print( "     " );
					textLayout.println( plantText.toString() );

					pickText.setLength( 0 );
					plantText.setLength( 0 );

					htmlLayout.println( "<tr>" );
					htmlLayout.print( "\t" );  htmlLayout.println( pickHtml.toString() );
					htmlLayout.print( "<td>&nbsp;</td>" );
					htmlLayout.print( "\t" );  htmlLayout.println( plantHtml.toString() );
					htmlLayout.print( "<td>&nbsp;</td>" );
					htmlLayout.println( "</tr>" );

					pickHtml.setLength( 0 );
					plantHtml.setLength( 0 );
				}

				// If the data in the original is different from
				// the planned, then script commands are needed.
				// Also, the HTML and textual layouts will be a
				// bit different pending on what happened.

				boolean pickRequired = !originalData[i][j].equals( "__" ) &&
					!originalData[i][j].equalsIgnoreCase( planningData[i][j] );

				if ( pickRequired )
				{
					pickText.append( " ***" );
					pickHtml.append( "<td style=\"border: 1px dashed red\"><img src=\"" );
					commands.add( "field pick " + (j+1) );
				}
				else
				{
					pickText.append( " " + originalData[i][j] + " " );
					pickHtml.append( "<td><img src=\"" );
				}

				image = getMushroomImage( originalData[i][j] );
				copyMushroomImage( image );

				pickHtml.append( image );
				pickHtml.append( "\"></td>" );

				// Spore additions are a little trickier than looking
				// just at the difference.  Only certain spores can be
				// planted, and only certain

				boolean addedSpore = !originalData[i][j].equals( planningData[i][j] );

				if ( addedSpore )
				{
					addedSpore = false;

					if ( planningData[i][j].startsWith( "kb" ) )
					{
						commands.add( "field plant " + (j+1) + " knob" );
						addedSpore = true;
					}
					else if ( planningData[i][j].startsWith( "kn" ) )
					{
						commands.add( "field plant " + (j+1) + " knoll" );
						addedSpore = true;
					}
					else if ( planningData[i][j].startsWith( "sp" ) )
					{
						commands.add( "field plant " + (j+1) + " spooky" );
						addedSpore = true;
					}
				}

				// Now that you know for sure whether or not a spore
				// was added or a breeding result, update the text.

				plantText.append( " " + planningData[i][j] );

				if ( addedSpore )
				{
					plantText.append( "*" );
					plantHtml.append( "<td style=\"border: 1px dashed blue\"><img src=\"" );

					image = getMushroomImage( planningData[i][j].toUpperCase() );
					copyMushroomImage( image );
					plantHtml.append( image );

					plantHtml.append( "\"></td>" );
				}
				else
				{
					plantText.append( " " );
					plantHtml.append( "<td><img src=\"" );

					image = getMushroomImage( planningData[i][j] );
					copyMushroomImage( image );
					plantHtml.append( image );

					plantHtml.append( "\"></td>" );
				}

				isTodayEmpty &= planningData[i][j].equals( "__" );
			}

			// Print the data for the last row.

			textLayout.print( pickText.toString() );
			textLayout.print( "     " );
			textLayout.println( plantText.toString() );

			pickText.setLength( 0 );
			plantText.setLength( 0 );

			htmlLayout.println( "<tr>" );
			htmlLayout.print( "\t" );  htmlLayout.println( pickHtml.toString() );
			htmlLayout.print( "<td>&nbsp;</td>" );
			htmlLayout.print( "\t" );  htmlLayout.println( plantHtml.toString() );
			htmlLayout.print( "<td>&nbsp;</td>" );
			htmlLayout.println( "</tr>" );

			pickHtml.setLength( 0 );
			plantHtml.setLength( 0 );

			// Print any needed trailing whitespace into the layouts
			// and add the list of commands to be processed later.

			textLayout.println();
			htmlLayout.println( "</table><br /><br />" );

			if ( !isTodayEmpty )
				days.add( commands );
		}

		// All data has been printed.  Add the closing tags to the
		// HTML version and then close the streams.

		try
		{
			textLayout.close();
			htmlLayout.println( "</body></html>" );
			htmlLayout.close();
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return;
		}

		// Now that all of the commands have been compiled, generate
		// the ASH script which will do the layout.

		try
		{
			plotScript.println( "boolean main()" );
			plotScript.println( "{" );
			plotScript.println();
			plotScript.println( "    if ( !have_mushroom_plot() )" );
			plotScript.println( "    {" );
			plotScript.println( "        print( \"You do not have a mushroom plot.\" );" );
			plotScript.println( "        return false;" );
			plotScript.println( "    }" );
			plotScript.println();
			plotScript.println( "    if ( get_property( \"plantingScript\" ) != \"" + filename + "\" )" );
			plotScript.println( "    {" );
			plotScript.println( "        set_property( \"plantingDay\", \"-1\" );" );
			plotScript.println( "        set_property( \"plantingDate\", \"\" );" );
			plotScript.println( "        set_property( \"plantingLength\", \"" + days.size() + "\" );" );
			plotScript.println( "        set_property( \"plantingScript\", \"" + filename + "\" );" );
			plotScript.println( "    }" );
			plotScript.println();
			plotScript.println( "    if ( get_property( \"plantingDate\" ) == today_to_string() )" );
			plotScript.println( "        return true;" );
			plotScript.println();
			plotScript.println( "    set_property( \"plantingDate\", today_to_string() );" );
			plotScript.println( "    int index = (string_to_int( get_property( \"plantingDay\" ) ) + 1) % " + days.size() + ";" );
			plotScript.println( "    set_property( \"plantingDay\", index );" );

			for ( int i = 0; i < days.size(); ++i )
			{
				ArrayList commands = (ArrayList) days.get(i);

				if ( !commands.isEmpty() )
				{
					plotScript.println();
					plotScript.println( "    if ( index == " + i + " )" );
					plotScript.print( "        cli_execute( \"" );

					for ( int j = 0; j < commands.size(); ++j )
					{
						if ( j != 0 )  plotScript.print( ";" );
						plotScript.print( commands.get(j) );
					}

					plotScript.println( "\" );" );
				}
			}

			plotScript.println();
			plotScript.println( "    return true;" );
			plotScript.println( "}" );
			plotScript.close();
		}
		catch ( Exception e )
		{
			printStackTrace( e );
			return;
		}

		// Now that everything has been generated, open the HTML
		// inside of a browser.

		StaticEntity.openSystemBrowser( PLOT_DIRECTORY + "/" + filename + ".htm" );

	}
}
