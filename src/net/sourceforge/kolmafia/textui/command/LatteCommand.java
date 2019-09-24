/**
 * Copyright (c) 2005-2019, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.LatteRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LatteCommand
	extends AbstractCommand
{
	public LatteCommand()
	{
		this.usage = " unlocks | unlocked | refill ingredient1 ingredient2 ingredient2 - Shows unlocks, unlocked items, or refills latte";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] params = parameters.trim().split( "\\s+" );
		String command = params[0];

		if ( !InventoryManager.hasItem( ItemPool.LATTE_MUG ) && !KoLCharacter.hasEquipped( ItemPool.LATTE_MUG, EquipmentManager.OFFHAND ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a latte lovers member's mug first." );
			return;
		}
		if ( command.equalsIgnoreCase( "unlocks" ) )
		{
			LatteRequest.listUnlocks( true );
		}
		else if ( command.equalsIgnoreCase( "unlocked" ) )
		{
			LatteRequest.listUnlocks( false );
		}
		else if ( command.equalsIgnoreCase( "refill" ) )
		{
			if ( params.length < 4 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Use command 'latte refill ingredient1 ingredient2 ingredient3'. Use 'latte unlocked' to show available ingredients." );
				return;
			}			
			LatteRequest.refill( params[1].toLowerCase().trim(), params[2].toLowerCase().trim(), params[3].toLowerCase().trim() );
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, " Use command latte " + this.usage );
		}
	}
}
