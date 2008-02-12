/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.Interpreter;


public abstract class Function
	extends Symbol
	implements ParseTreeNode
{
	public Type type;
	public VariableReferenceList variableReferences;

	public Function( final String name, final Type type,
		final VariableReferenceList variableReferences )
	{
		super( name );
		this.type = type;
		this.variableReferences = variableReferences;
	}

	public Function( final String name, final Type type )
	{
		this( name, type, new VariableReferenceList() );
	}

	public Type getType()
	{
		return this.type;
	}

	public VariableReferenceList getVariableReferences()
	{
		return this.variableReferences;
	}

	public void setVariableReferences( final VariableReferenceList variableReferences )
	{
		this.variableReferences = variableReferences;
	}

	public Iterator getReferences()
	{
		return this.variableReferences.iterator();
	}

	public void saveBindings( Interpreter interpreter )
	{
	}

	public void restoreBindings( Interpreter interpreter )
	{
	}

	public void printDisabledMessage( Interpreter interpreter )
	{
		try
		{
			StringBuffer message = new StringBuffer( "Called disabled function: " );
			message.append( this.getName() );

			message.append( "(" );

			Iterator it = this.variableReferences.iterator();
			for ( int i = 0; it.hasNext(); ++i )
			{
				VariableReference current = (VariableReference) it.next();

				if ( i != 0 )
				{
					message.append( ',' );
				}

				message.append( ' ' );
				message.append( current.getValue( interpreter ).toStringValue().toString() );
			}

			message.append( " )" );
			RequestLogger.printLine( message.toString() );
		}
		catch ( Exception e )
		{
			// If it fails, don't print the disabled message.
			// Which means, exiting here is okay.
		}
	}

	public abstract Value execute( final Interpreter interpreter );

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<FUNC " + this.type + " " + this.getName() + ">" );

		Iterator it = this.getReferences();
		while ( it.hasNext() )
		{
			VariableReference current = (VariableReference) it.next();
			current.print( stream, indent + 1 );
		}
	}
}