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

package net.sourceforge.kolmafia.request;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ProfileRequest
	extends GenericRequest
	implements Comparable
{
	private static final Pattern DATA_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>" );
	private static final Pattern NUMERIC_PATTERN = Pattern.compile( "\\d+" );
	private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat( "MMMM d, yyyy", Locale.US );
	public static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat( "MM/dd/yy", Locale.US );

	private String playerName;
	private String playerId;
	private Integer playerLevel;
	private boolean isHardcore;
	private String restriction;
	private Integer currentMeat;
	private Integer turnsPlayed, currentRun;
	private String classType;

	private Date created, lastLogin;
	private String food, drink;
	private Integer ascensionCount, pvpRank, karma;

	private Integer muscle, mysticism, moxie;
	private String title, rank;

	private String clanName;
	private int equipmentPower;

	public ProfileRequest( final String playerName )
	{
		super( "showplayer.php" );

		if ( playerName.startsWith( "#" ) )
		{
			this.playerId = playerName.substring( 1 );
			this.playerName = ContactManager.getPlayerName( this.playerId );
		}
		else
		{
			this.playerName = playerName;
			this.playerId = ContactManager.getPlayerId( playerName );
		}

		this.addFormField( "who", this.playerId );

		this.muscle = new Integer( 0 );
		this.mysticism = new Integer( 0 );
		this.moxie = new Integer( 0 );
		this.karma = new Integer( 0 );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	/**
	 * Internal method used to refresh the fields of the profile request based on the response text. This should be
	 * called after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		// Nothing to refresh if no text
		if ( this.responseText == null || this.responseText.length() == 0 )
		{
			return;
		}

		this.isHardcore = this.responseText.indexOf( "<b>(Hardcore)</b></td>" ) != -1;

		// This is a massive replace which makes the profile easier to
		// parse and re-represent inside of editor panes.

		String cleanHTML = this.responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" );
		StringTokenizer st = new StringTokenizer( cleanHTML, "\n" );

		String token = st.nextToken();

		this.playerLevel = new Integer( 0 );
		this.classType = "Recent Ascension";
		this.currentMeat = new Integer( 0 );
		this.ascensionCount = new Integer( 0 );
		this.turnsPlayed = new Integer( 0 );
		this.created = new Date();
		this.lastLogin = new Date();
		this.food = "none";
		this.drink = "none";
		this.pvpRank = new Integer( 0 );

		if ( cleanHTML.indexOf( "\nClass:" ) != -1 )
		{	// has custom title
			while ( !st.nextToken().startsWith( " (#" ) )
			{
			}
			String title = st.nextToken();	// custom title, may include level
			// Next token will be one of:
			//	(Level n), if the custom title doesn't include the level
			//	(In Ronin) or possibly similar messages
			//	Class:,	if neither of the above applies
			token = st.nextToken();
			if ( token.startsWith( "(Level" ) )
			{
				this.playerLevel = new Integer(
					StringUtilities.parseInt( token.substring( 6 ).trim() ) );
			}
			else
			{	// Must attempt to parse the level out of the custom title.
				// This is inherently inaccurate, since the title can contain other digits,
				// before, after, or adjacent to the level.
				Matcher m = ProfileRequest.NUMERIC_PATTERN.matcher( title );
				if ( m.find() && m.group().length() < 5 )
				{
					this.playerLevel = new Integer(
						StringUtilities.parseInt( m.group() ) );
				}
			}
		
			while ( !token.startsWith( "Class" ) )
			{
				token = st.nextToken();
			}
			this.classType = KoLCharacter.getClassType( st.nextToken().trim() );
		}
		else
		{	// no custom title
			if ( cleanHTML.indexOf( "Level" ) == -1 )
			{
				return;
			}
	
			while ( token.indexOf( "Level" ) == -1 )
			{
				token = st.nextToken();
			}
	
			this.playerLevel = new Integer( 
				StringUtilities.parseInt( token.substring( 5 ).trim() ) );
			this.classType = KoLCharacter.getClassType( st.nextToken().trim() );
		}
		
		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 && cleanHTML.indexOf( "\nPath" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Path" ) )
			{
				;
			}
			this.restriction = st.nextToken().trim();
		}
		else
		{
			this.restriction = "No-Path";
		}

		if ( cleanHTML.indexOf( "\nMeat:" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Meat" ) )
			{
				;
			}
			this.currentMeat = new Integer( StringUtilities.parseInt( st.nextToken().trim() ) );
		}

		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Ascensions" ) )
			{
				;
			}
			st.nextToken();
			this.ascensionCount = new Integer( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
			this.ascensionCount = new Integer( 0 );
		}

		while ( !st.nextToken().startsWith( "Turns" ) )
		{
			;
		}
		this.turnsPlayed = new Integer( StringUtilities.parseInt( st.nextToken().trim() ) );

		if ( cleanHTML.indexOf( "\nAscensions" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Turns" ) )
			{
				;
			}
			this.currentRun = new Integer( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
			this.currentRun = this.turnsPlayed;
		}

		String dateString = null;
		while ( !st.nextToken().startsWith( "Account" ) )
		{
			;
		}
		try
		{
			dateString = st.nextToken().trim();
			this.created = ProfileRequest.INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
			this.created = new Date();
		}

		while ( !st.nextToken().startsWith( "Last" ) )
		{
			;
		}

		try
		{
			dateString = st.nextToken().trim();
			this.lastLogin = ProfileRequest.INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
			this.lastLogin = this.created;
		}

		if ( cleanHTML.indexOf( "\nFavorite Food" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Favorite" ) )
			{
				;
			}
			this.food = st.nextToken().trim();
		}
		else
		{
			this.food = "none";
		}

		if ( cleanHTML.indexOf( "\nFavorite Booze" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Favorite" ) )
			{
				;
			}
			this.drink = st.nextToken().trim();
		}
		else
		{
			this.drink = "none";
		}

		if ( cleanHTML.indexOf( "\nRanking" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Ranking" ) )
			{
				;
			}
			this.pvpRank = new Integer( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
			this.pvpRank = new Integer( 0 );
		}

		this.equipmentPower = 0;
		if ( cleanHTML.indexOf( "\nEquipment" ) != -1 )
		{
			while ( !st.nextToken().startsWith( "Equipment" ) )
			{
				;
			}

			while ( EquipmentDatabase.contains( token = st.nextToken() ) )
			{
				switch ( ItemDatabase.getConsumptionType( token ) )
				{
				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_SHIRT:

					this.equipmentPower += EquipmentDatabase.getPower( token );
					break;
				}
			}
		}

		if ( cleanHTML.indexOf( "\nClan" ) != -1 )
		{
			while ( !token.startsWith( "Clan" ) )
			{
				token = st.nextToken();
			}

			this.clanName = st.nextToken();
		}

		if ( cleanHTML.indexOf( "\nTitle" ) != -1 )
		{
			while ( !token.startsWith( "Title" ) )
			{
				token = st.nextToken();
			}

			this.title = st.nextToken();
		}
	}

	/**
	 * static final method used by the clan manager in order to get an instance of a profile request based on the data
	 * already known.
	 */

	public static final ProfileRequest getInstance( final String playerName, final String playerId,
		final String playerLevel, final String responseText, final String rosterRow )
	{
		ProfileRequest instance = new ProfileRequest( playerName );

		instance.playerId = playerId;

		// First, initialize the level field for the
		// current player.

		if ( playerLevel == null )
		{
			instance.playerLevel = new Integer( 0 ); 
		}
		else
		{
			instance.playerLevel = Integer.valueOf( playerLevel );
		}

		// Next, refresh the fields for this player.
		// The response text should be copied over
		// before this happens.

		instance.responseText = responseText;
		instance.refreshFields();

		// Next, parse out all the data in the
		// row of the detail roster table.

		if ( rosterRow == null )
		{
			instance.muscle = new Integer( 0 );
			instance.mysticism = new Integer( 0 );
			instance.moxie = new Integer( 0 );
			
			instance.rank = "";
			instance.karma = new Integer( 0 );
		}
		else
		{
			Matcher dataMatcher = ProfileRequest.DATA_PATTERN.matcher( rosterRow );
	
			// The name of the player occurs in the first
			// field of the table.  Because you already
			// know the name of the player, this can be
			// arbitrarily skipped.
	
			dataMatcher.find();
			
			// At some point the player class was added to the table.  Skip over it.
	
			dataMatcher.find();
	
			// The player's three primary stats appear in
			// the next three fields of the table.
	
			dataMatcher.find();
			instance.muscle = new Integer( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			dataMatcher.find();
			instance.mysticism = new Integer( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			dataMatcher.find();
			instance.moxie = new Integer( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			// The next field contains the total power,
			// and since this is calculated, it can be
			// skipped in data retrieval.
	
			dataMatcher.find();
	
			// The next three fields contain the ascension
			// count, number of hardcore runs, and their
			// pvp ranking.
	
			dataMatcher.find();
			dataMatcher.find();
			dataMatcher.find();
	
			// Next is the player's rank inside of this clan.
			// Title was removed, so ... not visible here.
	
			dataMatcher.find();
			instance.rank = dataMatcher.group( 1 );
	
			// The last field contains the total karma
			// accumulated by this player.
	
			dataMatcher.find();
			instance.karma = new Integer( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
		}

		return instance;
	}

	/**
	 * static final method used by the flower hunter in order to get an instance of a profile request based on the data
	 * already known.
	 */

	public static final ProfileRequest getInstance( final String playerName, final String playerId,
		final String clanName, final Integer playerLevel, final String classType, final Integer pvpRank )
	{
		ProfileRequest instance = new ProfileRequest( playerName );
		instance.playerId = playerId;
		instance.playerLevel = playerLevel;
		instance.clanName = clanName == null ? "" : clanName;
		instance.classType = classType;
		instance.pvpRank = pvpRank;

		return instance;
	}

	public String getPlayerName()
	{
		return this.playerName;
	}

	public String getPlayerId()
	{
		return this.playerId;
	}

	public String getClanName()
	{
		return this.clanName;
	}

	public void initialize()
	{
		if ( this.responseText == null )
		{
			RequestThread.postRequest( this );
		}
	}

	public boolean isHardcore()
	{
		this.initialize();
		return this.isHardcore;
	}

	public String getRestriction()
	{
		this.initialize();
		return this.restriction;
	}

	public String getClassType()
	{
		if ( this.classType == null )
		{
			this.initialize();
		}

		return this.classType;
	}

	public Integer getPlayerLevel()
	{
		if ( this.playerLevel == null || this.playerLevel.intValue() == 0 )
		{
			this.initialize();
		}

		return this.playerLevel;
	}

	public Integer getCurrentMeat()
	{
		this.initialize();
		return this.currentMeat;
	}

	public Integer getTurnsPlayed()
	{
		this.initialize();
		return this.turnsPlayed;
	}

	public Integer getCurrentRun()
	{
		this.initialize();
		return this.currentRun;
	}

	public Date getLastLogin()
	{
		this.initialize();
		return this.lastLogin;
	}

	public Date getCreation()
	{
		this.initialize();
		return this.created;
	}

	public String getCreationAsString()
	{
		this.initialize();
		return ProfileRequest.OUTPUT_FORMAT.format( this.created );
	}

	public String getLastLoginAsString()
	{
		this.initialize();
		return ProfileRequest.OUTPUT_FORMAT.format( this.lastLogin );
	}

	public String getFood()
	{
		this.initialize();
		return this.food;
	}

	public String getDrink()
	{
		this.initialize();
		return this.drink;
	}

	public Integer getPvpRank()
	{
		if ( this.pvpRank == null || this.pvpRank.intValue() == 0 )
		{
			this.initialize();
		}

		return this.pvpRank;
	}

	public Integer getMuscle()
	{
		return this.muscle;
	}

	public Integer getMysticism()
	{
		return this.mysticism;
	}

	public Integer getMoxie()
	{
		return this.moxie;
	}

	public Integer getPower()
	{
		return new Integer( this.muscle.intValue() + this.mysticism.intValue() + this.moxie.intValue() );
	}

	public Integer getEquipmentPower()
	{
		return new Integer( this.equipmentPower );
	}

	public String getTitle()
	{
		return this.title != null ? this.title : ClanManager.getTitle( this.playerName );
	}

	public String getRank()
	{
		return this.rank;
	}

	public Integer getKarma()
	{
		return this.karma;
	}

	public Integer getAscensionCount()
	{
		this.initialize();
		return this.ascensionCount;
	}

	private static final Pattern GOBACK_PATTERN =
		Pattern.compile( "http://www[2345678]?\\.kingdomofloathing\\.com/ascensionhistory\\.php?back=self&who=([\\d]+)" );

	public void processResults()
	{
		Matcher dataMatcher = ProfileRequest.GOBACK_PATTERN.matcher( this.responseText );
		if ( dataMatcher.find() )
		{
			this.responseText =
				dataMatcher.replaceFirst( "../ascensions/" + ClanManager.getURLName( ContactManager.getPlayerName( dataMatcher.group( 1 ) ) ) );
		}

		this.refreshFields();
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof ProfileRequest ) )
		{
			return -1;
		}

		ProfileRequest pr = (ProfileRequest) o;

		if ( this.getPvpRank().intValue() != pr.getPvpRank().intValue() )
		{
			return this.getPvpRank().intValue() - pr.getPvpRank().intValue();
		}

		return this.getPlayerLevel().intValue() - pr.getPlayerLevel().intValue();
	}
}
