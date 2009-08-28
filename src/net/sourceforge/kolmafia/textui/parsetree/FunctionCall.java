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

import java.util.Iterator;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class FunctionCall
	extends Value
{
	protected Function target;
	protected final ValueList params;
	protected final int paramCount;
	protected final String fileName;
	protected final int lineNumber;

	public FunctionCall( final Function target, final ValueList params, final Parser parser )
	{
		this.target = target;
		this.params = params;
		this.paramCount = params.size();
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	public Function getTarget()
	{
		return this.target;
	}

	public Iterator getValues()
	{
		return this.params.iterator();
	}

	public Type getType()
	{
		return this.target.getType();
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		// Save current variable bindings
		this.target.saveBindings( interpreter );
		interpreter.traceIndent();

		Iterator valIterator = this.params.iterator();
		Value [] params = new Value[ this.paramCount ];
		int paramCount = 0;

		while ( valIterator.hasNext() )
		{
			Value paramValue = (Value) valIterator.next();
			++paramCount;

			interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

			Value value = paramValue.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				this.target.restoreBindings( interpreter );
				interpreter.traceUnindent();
				return null;
			}

			params[ paramCount -1 ] = value;
		}

		Iterator refIterator = this.target.getReferences();
		paramCount = 0;
		while ( refIterator.hasNext() )
		{
			VariableReference paramVarRef = (VariableReference) refIterator.next();
			Value value = params[ paramCount ];
			++paramCount;

			// Bind parameter to new value
			paramVarRef.setValue( interpreter, value );
		}

		interpreter.trace( "Entering function " + this.target.getName() );
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		Value result = this.target.execute( interpreter );
		interpreter.trace( "Function " + this.target.getName() + " returned: " + result );

		if ( interpreter.getState() != Interpreter.STATE_EXIT )
		{
			interpreter.setState( Interpreter.STATE_NORMAL );
		}

		// Restore initial variable bindings
		this.target.restoreBindings( interpreter );
		interpreter.traceUnindent();

		return result;
	}

	public String toString()
	{
		return this.target.getName() + "()";
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<CALL " + this.getTarget().getName() + ">" );

		Iterator it = this.getValues();
		while ( it.hasNext() )
		{
			Value current = (Value) it.next();
			current.print( stream, indent + 1 );
		}
	}
}
