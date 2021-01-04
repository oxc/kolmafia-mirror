/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.listener.ListenerRegistry;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class DebugRequestCommand
	extends AbstractCommand
{
	public DebugRequestCommand()
	{
		this.usage = " [on] | off | ? | note | trace [ [on] | off | ? ] | ash [ [on] | off ] | listener [ [on] | off ] - start or stop logging of debugging data.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "" ) || command.equals( "on" ) )
		{
			RequestLogger.openDebugLog();
		}
		else if ( command.equals( "off" ) )
		{
			RequestLogger.closeDebugLog();
		}
		else if ( command.equals( "?" ) )
		{
			if (RequestLogger.isDebugging()) {
				KoLmafia.updateDisplay("Debugging is on.");
			}
			else
			{
				KoLmafia.updateDisplay("Debugging is off.");
			}
		}
		else if ( command.equals( "trace" ) )
		{
			command = split.length < 2 ? "" : split[ 1 ];
			if ( command.equals( "" ) || command.equals( "on" ) )
			{
				RequestLogger.openTraceStream();
			}
			else if ( command.equals( "off" ) )
			{
				RequestLogger.closeTraceStream();
			}
			else if (command.equals( "?" ) )
			{
				if (RequestLogger.isTracing()) {
					KoLmafia.updateDisplay("Tracing is on.");
				}
				else
				{
					KoLmafia.updateDisplay("Tracing is off.");
				}
			}
		}
		else if ( command.equals( "ash" ) )
		{
			command = split.length < 2 ? "" : split[ 1 ];
			if ( command.equals( "" ) || command.equals( "on" ) )
			{
				ScriptRuntime.openTraceStream();
			}
			else if ( command.equals( "off" ) )
			{
				ScriptRuntime.closeTraceStream();
			}
		}
		else if ( command.equals( "listener" ) )
		{
			command = split.length < 2 ? "" : split[ 1 ];
			if ( command.equals( "" ) || command.equals( "on" ) )
			{
				ListenerRegistry.setLogging( true );
			}
			else if ( command.equals( "off" ) )
			{
				ListenerRegistry.setLogging( false );
			}
		}
		else if (command.equals( "note" ) )
		{
			String debugNote = parameters.substring(command.length()).trim();
			if ( debugNote.equals( "" ) )
			{
			KoLmafia.updateDisplay( MafiaState.ERROR, "debug note must include text to add to the debug log." );
			return;

			}
			java.util.Date noteTime = new java.util.Date(); 
			if ( RequestLogger.isDebugging() ) {
				RequestLogger.updateDebugLog( "-----User Note: "
				+ noteTime
				+ "-----\n"
				+ debugNote 
				+ "\n-----" );
			}
			else
			{
				RequestLogger.openDebugLog();
				RequestLogger.updateDebugLog( "-----User Note: " 
				+ noteTime 
				+ "-----\n"
				+ debugNote 
				+ "\n-----" );
				RequestLogger.closeDebugLog();
			}
		}
		else 
		{
			KoLmafia.updateDisplay( "I don't know how to debug " + command );
		}
	}
}
