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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.CreateItemRequest;

public class CreateItemCommand
	extends AbstractCommand
{
	public CreateItemCommand()
	{
		this.usage = " [ <item>... ] - list creatables, or create specified items.";
	}

	public void run( final String cmd, final String parameters )
	{
		SpecialOutfit.createImplicitCheckpoint();
		CreateItemCommand.create( parameters );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public static void create( final String parameters )
	{
		ConcoctionDatabase.refreshConcoctions();

		if ( parameters.equals( "" ) )
		{
			RequestLogger.printList( ConcoctionDatabase.getCreatables() );
			return;
		}

		ItemFinder.setMatchType( ItemFinder.CREATE_MATCH );
		Object[] itemList = ItemFinder.getMatchingItemList( null, parameters );
		ItemFinder.setMatchType( ItemFinder.ANY_MATCH );

		AdventureResult currentMatch;
		CreateItemRequest irequest;

		for ( int i = 0; i < itemList.length; ++i )
		{
			currentMatch = (AdventureResult) itemList[ i ];
			if ( itemList[ i ] == null )
			{
				continue;
			}

			irequest = CreateItemRequest.getInstance( currentMatch );

			if ( irequest == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					ConcoctionDatabase.excuse != null ? ConcoctionDatabase.excuse
					: "That item cannot be created." );
				return;
			}

			irequest.setQuantityNeeded( currentMatch.getCount() );
			RequestThread.postRequest( irequest );
		}
	}
}
