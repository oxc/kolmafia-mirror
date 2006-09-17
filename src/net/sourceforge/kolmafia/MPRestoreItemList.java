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
import javax.swing.JCheckBox;

/**
 * A special class used as a holder class to hold all of the
 * items which are available for use as MP buffers.
 */

public abstract class MPRestoreItemList extends StaticEntity
{
	private static final MPRestoreItem GALAKTIK = new MPRestoreItem( "Galaktik's Fizzy Invigorating Tonic", Integer.MAX_VALUE );
	private static final MPRestoreItem CAMPING = new MPRestoreItem( "rest at campground", Integer.MAX_VALUE );
	private static final MPRestoreItem BEANBAG = new MPRestoreItem( "relax in beanbag", Integer.MAX_VALUE );
	private static final MPRestoreItem MYSTERY_JUICE = new MPRestoreItem( "magical mystery juice", Integer.MAX_VALUE );
	public static final MPRestoreItem SELTZER = new MPRestoreItem( "Knob Goblin seltzer", 10 );

	public static final MPRestoreItem [] CONFIGURES = new MPRestoreItem []
	{
		GALAKTIK, BEANBAG, CAMPING, new MPRestoreItem( "phonics down", 48 ), new MPRestoreItem( "tonic water", 40 ),
		new MPRestoreItem( "Knob Goblin superseltzer", 27 ), new MPRestoreItem( "Blatantly Canadian", 22 ),
		new MPRestoreItem( "Dyspepsi-Cola", 12 ), new MPRestoreItem( "Cloaca-Cola", 12 ),
		new MPRestoreItem( "Mountain Stream soda", 8 ), MYSTERY_JUICE, SELTZER,
		new MPRestoreItem( "Cherry Cloaca Cola", 8 ), new MPRestoreItem( "soda water", 4 )
	};

	public static JCheckBox [] getCheckboxes()
	{
		String mpRestoreSetting = getProperty( "mpAutoRecoveryItems" );
		JCheckBox [] restoreCheckbox = new JCheckBox[ CONFIGURES.length ];

		for ( int i = 0; i < CONFIGURES.length; ++i )
		{
			restoreCheckbox[i] = new JCheckBox( CONFIGURES[i].toString() );
			restoreCheckbox[i].setSelected( mpRestoreSetting.indexOf( CONFIGURES[i].toString().toLowerCase() ) != -1 );
		}

		return restoreCheckbox;
	}

	public static class MPRestoreItem
	{
		private String itemName;
		private int mpPerUse;
		private AdventureResult itemUsed;

		public MPRestoreItem( String itemName, int mpPerUse )
		{
			this.itemName = itemName;
			this.mpPerUse = mpPerUse;
			this.itemUsed = new AdventureResult( itemName, 1, false );
		}

		public AdventureResult getItem()
		{	return itemUsed;
		}

		public void recoverMP( int needed )
		{
			if ( this == GALAKTIK )
			{
				DEFAULT_SHELL.executeLine( "galaktik mp" );
				return;
			}

			if ( this == CAMPING )
			{
				DEFAULT_SHELL.executeLine( "rest" );
				return;
			}

			if ( this == BEANBAG )
			{
				DEFAULT_SHELL.executeLine( "relax" );
				return;
			}

			if ( this == MYSTERY_JUICE )
			{
				// The restore rate on magical mystery juice changes
				// based on your current level.

				this.mpPerUse = (int) (KoLCharacter.getLevel() * 1.5 + 4.0);
			}

			int mpShort = needed - KoLCharacter.getCurrentMP();


			int numberToUse = (int) Math.ceil( (float) mpShort / (float) mpPerUse );
			int numberAvailable = itemUsed.getCount( inventory );

			if ( !NPCStoreDatabase.contains( this.toString() ) )
				numberToUse = Math.min( numberAvailable, numberToUse );

			if ( numberToUse <= 0 )
				return;

			getClient().makeRequest( new ConsumeItemRequest( getClient(), itemUsed.getInstance( numberToUse ) ) );
		}

		public String toString()
		{	return itemName;
		}
	}
}
