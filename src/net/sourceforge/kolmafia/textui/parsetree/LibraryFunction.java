/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Interpreter.InterpreterState;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;

public class LibraryFunction
	extends Function
{
	private Method method;

	public LibraryFunction( final String name, final Type type, final Type[] params )
	{
		super( name.toLowerCase(), type );

		Class[] args = new Class[ params.length + 1 ];

		args[ 0 ] = Interpreter.class;

		// Make a list of VariableReferences, even though the library
		// function will not use them, so that tracing works
		for ( int i = 1; i <= params.length; ++i )
		{
			Variable variable = new Variable( params[ i - 1 ] );
			this.variableReferences.add( new VariableReference( variable ) );
			args[ i ] = Value.class;
		}

		try
		{
			this.method = RuntimeLibrary.findMethod( name, args );
		}
		catch ( Exception e )
		{
			// This should not happen; it denotes a coding
			// error that must be fixed before release.

			StaticEntity.printStackTrace( e, "No method found for built-in function: " + name );
		}
	}

	@Override
	public Value execute( final Interpreter interpreter, Object[] values )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( InterpreterState.EXIT );
			return null;
		}

		if ( StaticEntity.isDisabled( this.getName() ) )
		{
			this.printDisabledMessage( interpreter );
			return this.getType().initialValue();
		}

		if ( this.method == null )
		{
			throw interpreter.runtimeException( "Internal error: no method for " + this.getName() );
		}

		try
		{
			// Bind values to variable references.
			// Collapse values into VarArgs array
			values = this.bindVariableReferences( interpreter, values );

			// Invoke the method
			return (Value) this.method.invoke( this, values );
		}
		catch ( InvocationTargetException e )
		{
			// This is an error in the called method. Pass
			// it on up so that we'll print a stack trace.

			Throwable cause = e.getCause();
			if ( cause instanceof ScriptException )
			{
				// Pass up exceptions intentionally generated by library
				throw (ScriptException) cause;
			}
			throw new RuntimeException( cause );
		}
		catch ( IllegalAccessException e )
		{
			// This is not expected, but is an internal error in ASH
			throw new ScriptException( e );
		}
	}
}
