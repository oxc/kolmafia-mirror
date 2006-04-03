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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * A special class used to determine the current moon phase.
 * Theoretically, the calculations are simple enough to be
 * an internal class elsewhere, but because it makes everything
 * cleaner to do things this way, so it goes.
 */

public class MoonPhaseDatabase extends StaticEntity
{
	// Special date marked as the new year.  This is
	// done as a string, since sdf.parse() throws an
	// exception, most of the time.

	private static long NEWYEAR = 0;
	private static long BOUNDARY = 0;

	static
	{
		try
		{
			NEWYEAR = sdf.parse( "20050917" ).getTime();
			BOUNDARY = sdf.parse( "20051027" ).getTime();
		}
		catch ( Exception e )
		{
			// Because the date string was manually
			// constructed, this error will not happen.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	private static int RONALD_PHASE = -1;
	private static int GRIMACE_PHASE = -1;

	static
	{
		try
		{
			int calendarDay = getCalendarDay( sdf.parse( sdf.format( new Date() ) ) );
			int phaseStep = ((calendarDay % 16) + 16) % 16;

			RONALD_PHASE = phaseStep % 8;
			GRIMACE_PHASE = phaseStep / 2;
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	// Static array of status effect day predictions
	// within the KoL lunar calendar.

	private static final String [] STAT_EFFECT =
	{
		"Moxie day today and yesterday.", "3 days until Mysticism.", "2 days until Mysticism.", "Mysticism tomorrow (not today).",
		"Mysticism day today (not tomorrow).", "3 days until Muscle.", "2 days until Muscle.", "Muscle tomorrow (not today).",
		"Muscle day today and tomorrow.", "Muscle day today and yesterday.", "2 days until Mysticism.", "Mysticism tomorrow (not today).",
		"Mysticism day today (not tomorrow).", "2 days until Moxie.", "Moxie tomorrow (not today).", "Moxie day today and tomorrow."
	};

	// Static array of month names, as they exist within
	// the KoL calendar.

	private static final String [] MONTH_NAMES =
	{
		"", "Jarlsuary", "Frankruary", "Starch", "April", "Martinus", "Bill",
		"Bor", "Petember", "Carlvember", "Porktober", "Boozember", "Dougtember"
	};

	// Static array of holidays.  This holiday is filled with the
	// name of the holiday which occurs on the given KoL month and
	// given KoL day.

	private static String [][] HOLIDAYS = new String[13][9];

	static
	{
		for ( int i = 0; i < 13; ++i )
			for ( int j = 0; j < 9; ++j )
				HOLIDAYS[i][j] = "No known holiday today.";

		// Initialize all the known holidays here so that
		// they can be used in later initializers.

		HOLIDAYS[2][4] = "Valentine's Day";
		HOLIDAYS[3][3] = "St. Sneaky Pete's Day";
		HOLIDAYS[4][2] = "Oyster Egg Day";
		HOLIDAYS[10][8] = "Halloween";
		HOLIDAYS[11][7] = "Feast of Boris";
	}

	// Static array of when the special events in KoL occur, including
	// stat days, holidays and all that jazz.  Values are false where
	// there is no special occasion, and true where there is.

	private static int [] SPECIAL = new int[96];

	public static final int SP_NOTHING = 0;
	public static final int SP_HOLIDAY = 1;
	public static int SP_STATDAY = 2;

	static
	{
		// Assume there are no special days at all, and then
		// fill them in once they're encountered.

		for ( int i = 0; i < 96; ++i )
			SPECIAL[i] = SP_NOTHING;

		// Muscle days occur every phase 8 and phase 9 on the
		// KoL calendar.

		for ( int i = 8; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 9; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Mysticism days occur every phase 4 and phase 12 on the
		// KoL calendar.

		for ( int i = 4; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 12; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Moxie days occur every phase 0 and phase 15 on the
		// KoL calendar.

		for ( int i = 0; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;
		for ( int i = 15; i < 96; i += 16 )
			SPECIAL[i] = SP_STATDAY;

		// Next, fill in the holidays.  These are manually
		// computed based on the recurring day in the year
		// at which these occur.

		for ( int i = 0; i < 13; ++i )
			for ( int j = 0; j < 9; ++j )
				if ( !HOLIDAYS[i][j].equals( "No known holiday today." ) )
					SPECIAL[ 8 * i + j - 9 ] = SP_HOLIDAY;
	}

	public static final void setMoonPhases( int ronaldPhase, int grimacePhase )
	{
		// Reset the new year based on the internal
		// phase error.

		int phaseError = getPhaseStep();

		RONALD_PHASE = ronaldPhase;
		GRIMACE_PHASE = grimacePhase;

		phaseError -= getPhaseStep();

		// Adjust the new year by the appropriate
		// number of days.

		NEWYEAR += ((long)phaseError) * 86400000L;
	}

	public static final int getRonaldPhase()
	{	return RONALD_PHASE + 1;
	}

	public static final int getGrimacePhase()
	{	return GRIMACE_PHASE + 1;
	}

	/**
	 * Method to return which phase of the moon is currently
	 * appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return	The current phase of Ronald
	 */

	public static final String getRonaldPhaseAsString()
	{	return getPhaseName( RONALD_PHASE );
	}

	/**
	 * Method to return which phase of the moon is currently
	 * appearing over the Kingdom of Loathing, as a string.
	 *
	 * @return	The current phase of Ronald
	 */

	public static final String getGrimacePhaseAsString()
	{	return getPhaseName( GRIMACE_PHASE );
	}

	public static final String getPhaseName( int phase )
	{
		switch ( phase )
		{
			case 0:  return "new moon";
			case 1:  return "waxing crescent";
			case 2:  return "first quarter";
			case 3:  return "waxing gibbous";
			case 4:  return "full moon";
			case 5:  return "waning gibbous";
			case 6:  return "third quarter";
			case 7:  return "waning crescent";
			default:  return "unknown";
		}
	}

	/**
	 * Returns the moon effect applicable today, or the amount
	 * of time until the next moon effect becomes applicable
	 * if today is not a moon effect day.
	 */

	public static final String getMoonEffect()
	{	return getMoonEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the moon effect applicable at the given phase
	 * step, or the amount of time until the next moon effect,
	 * given the phase value.
	 */

	public static final String getMoonEffect( int ronaldPhase, int grimacePhase )
	{
		int phaseStep = getPhaseStep( ronaldPhase, grimacePhase );
		return phaseStep == -1 || phaseStep >= STAT_EFFECT.length ? "Could not determine moon phase." : STAT_EFFECT[ phaseStep ];
	}

	/**
	 * Returns the "phase step" currently recognized by the
	 * KoL calendar.  This corresponds to the day within the
	 * KoL lunar calendar, which has a cycle of 16 days.
	 */

	public static final int getPhaseStep()
	{	return getPhaseStep( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the "phase step" currently recognized by the
	 * KoL calendar, corresponding to the given phases.  This
	 * corresponds to the day within the KoL lunar calendar,
	 * which has a cycle of 16 days.
	 */

	public static final int getPhaseStep( int ronaldPhase, int grimacePhase )
	{	return grimacePhase >= 4 ? 8 + ronaldPhase : ronaldPhase;
	}

	/**
	 * Returns whether or not the grue will fight during the
	 * current moon phase.
	 */

	public static final boolean getGrueEffect()
	{	return getGrueEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns whether or not the grue will fight during the
	 * given moon phases.
	 */

	public static final boolean getGrueEffect( int ronaldPhase, int grimacePhase )
	{	return getMoonlight( ronaldPhase, grimacePhase ) < 5;
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of Blood of the Wereseal for today.
	 */

	public static final int getBloodEffect()
	{	return getBloodEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of Blood of the Wereseal for the given moon phase.
	 */

	public static final int getBloodEffect( int ronaldPhase, int grimacePhase )
	{	return (getMoonlight( ronaldPhase, grimacePhase ) - 4) * 25;
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of the Talisman of Baio for today.
	 */

	public static final int getBaioEffect()
	{	return getBaioEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the effect percentage (as a whole number integer)
	 * of the Talisman of Baio for the given moon phases.
	 */

	public static final int getBaioEffect( int ronaldPhase, int grimacePhase )
	{	return getMoonlight( ronaldPhase, grimacePhase ) * 10;
	}

	/**
	 * Returns the effect of the Jekyllin, based on the current
	 * moon phase information.
	 */

	public static final String getJekyllinEffect()
	{	return getJekyllinEffect( RONALD_PHASE, GRIMACE_PHASE );
	}

	/**
	 * Returns the effect of the Jekyllin for the given moon phases
	 */

	public static final String getJekyllinEffect( int ronaldPhase, int grimacePhase )
	{
		int moonlight = getMoonlight( ronaldPhase, grimacePhase );
		return "+" + (9 - moonlight) + " stats, " + (15 + moonlight * 5) + "% items";
	}

	/**
	 * Utility method which determines the moonlight available,
	 * given the moon phases as stated.
	 */

	private static final int getMoonlight( int ronaldPhase, int grimacePhase )
	{
		int ronaldLight = ronaldPhase > 4 ? 8 - ronaldPhase : ronaldPhase;
		int grimaceLight = grimacePhase > 4 ? 8 - grimacePhase : grimacePhase;

		return ronaldLight + grimaceLight;
	}

	/**
	 * Computes the difference in days based on the given
	 * millisecond counts since January 1, 1970.
	 */

	public static int getCalendarDay( Date time )
	{
		long timeDifference = time.getTime();
		if ( timeDifference > BOUNDARY )
			timeDifference -= 86400000L;

		timeDifference -= NEWYEAR;
		int dayDifference = (int) Math.floor( timeDifference / 86400000L );
		return ((dayDifference % 96) + 96) % 96;
	}

	/**
	 * Utility method which calculates which day of the
	 * KoL calendar you're currently on, based on the number
	 * of milliseconds since January 1, 1970.
	 */

	public static final String getCalendarDayAsString( Date time )
	{
		int [] calendarDayAsArray = convertCalendarDayToArray( getCalendarDay( time ) );
		return MONTH_NAMES[ calendarDayAsArray[0] ] + " " + calendarDayAsArray[1];
	}

	/**
	 * Utility method which decomposes a given calendar day
	 * into its actual calendar components.
	 */

	private static final int [] convertCalendarDayToArray( int calendarDay )
	{	return new int [] { (calendarDay / 8) + 1, (calendarDay % 8) + 1 };
	}

	/**
	 * Utility method which returns the given day count as
	 * an easily-understood string (today, tomorrow) instead
	 * of just "x days".
	 */

	public static final String getDayCountAsString( int dayCount )
	{	return dayCount == 0 ? "today" : dayCount == 1 ? "tomorrow" : dayCount + " days";
	}

	/**
	 * Returns the KoL calendar month associated with the
	 * given date in the real world.
	 */

	public static final int getCalendarMonth( Date time )
	{	return convertCalendarDayToArray( getCalendarDay( time ) )[0];
	}

	/**
	 * Returns whether or not the given day's most important
	 * attribute is being a holiday.
	 */

	public static boolean isHoliday( Date time )
	{	return SPECIAL[ getCalendarDay( time ) ] == SP_HOLIDAY;
	}

	/**
	 * Returns whether or not the given day's most important
	 * attribute is being a stat day.  Note that this ranks
	 * behind being a holiday, so holidays which are also stat
	 * days (Halloween and Oyster Egg Day, for example), will
	 * not be recognized as "stat days" in this method.
	 */

	public static boolean isStatDay( Date time )
	{	return SPECIAL[ getCalendarDay( time ) ] == SP_STATDAY;
	}

	/**
	 * Returns a complete list of all holiday predictions for
	 * the given day, as an array.
	 */

	public static final String [] getHolidayPredictions( Date time )
	{
		List predictionsList = new ArrayList();
		int currentCalendarDay = getCalendarDay( time );

		int [] calendarDayAsArray;

		for ( int i = 0; i < 96; ++i )
		{
			if ( SPECIAL[i] == SP_HOLIDAY )
			{
				calendarDayAsArray = convertCalendarDayToArray( i );
				predictionsList.add( HOLIDAYS[ calendarDayAsArray[0] ][ calendarDayAsArray[1] ] + ": " +
					getDayCountAsString( (i - currentCalendarDay + 96) % 96 ) );
			}
		}

		String [] predictionsArray = new String[ predictionsList.size() ];
		predictionsList.toArray( predictionsArray );
		return predictionsArray;
	}

	/**
	 * Returns the KoL holiday associated with the given
	 * date in the real world.
	 */

	public static final String getHoliday( Date time )
	{
		int [] calendarDayAsArray = convertCalendarDayToArray( getCalendarDay( time ) );
		return HOLIDAYS[ calendarDayAsArray[0] ][ calendarDayAsArray[1] ];
	}
}
