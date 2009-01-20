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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class RepeatUntilLoop
	extends Loop
{
	private final Value condition;

	public RepeatUntilLoop( final Scope scope, final Value condition )
	{
		super( scope );
		this.condition = condition;
	}

	public Value getCondition()
	{
		return this.condition;
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();
		interpreter.trace( this.toString() );

		Value conditionResult;

		do
		{
			Value result = super.execute( interpreter );

			if ( interpreter.getState() == Interpreter.STATE_BREAK )
			{
				interpreter.setState( Interpreter.STATE_NORMAL );
				interpreter.traceUnindent();
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() != Interpreter.STATE_NORMAL )
			{
				interpreter.traceUnindent();
				return result;
			}

			interpreter.trace( "Test: " + this.condition );

			conditionResult = this.condition.execute( interpreter );
			interpreter.captureValue( conditionResult );

			interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				interpreter.traceUnindent();
				return null;
			}
		}
		while ( conditionResult.intValue() != 1 );

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	public String toString()
	{
		return "repeat";
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<REPEAT>" );
		this.getScope().print( stream, indent + 1 );
		this.getCondition().print( stream, indent + 1 );
	}
}
