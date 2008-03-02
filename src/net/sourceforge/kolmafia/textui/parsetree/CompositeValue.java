/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import net.sourceforge.kolmafia.textui.DataTypes;

public class CompositeValue
	extends Value
{
	public CompositeValue( final CompositeType type )
	{
		super( type );
	}

	public CompositeType getCompositeType()
	{
		return (CompositeType) this.type;
	}

	public Value aref( final Value key )
	{
		return null;
	}

	public void aset( final Value key, final Value val )
	{
	}

	public Value remove( final Value key )
	{
		return null;
	}

	public void clear()
	{
	}

	public Value[] keys()
	{
		return new Value[ 0 ];
	}

	public Value initialValue( final Object key )
	{
		return ( (CompositeType) this.type ).getDataType( key ).initialValue();
	}

	public void dump( final PrintStream writer, final String prefix, final boolean compact )
	{
		Value[] keys = this.keys();
		if ( keys.length == 0 )
		{
			return;
		}

		for ( int i = 0; i < keys.length; ++i )
		{
			Value key = keys[ i ];
			Value value = this.aref( key );
			String first = prefix + key + "\t";
			value.dump( writer, first, compact );
		}
	}

	public void dumpValue( final PrintStream writer )
	{
	}

	// Returns number of fields consumed
	public int read( final String[] data, final int index, final boolean compact )
	{
		CompositeType type = (CompositeType) this.type;
		Value key = null;

		if ( index < data.length )
		{
			key = type.getKey( DataTypes.parseValue( type.getIndexType(), data[ index ], true ) );
		}
		else
		{
			key = type.getKey( DataTypes.parseValue( type.getIndexType(), "none", true ) );
		}

		// If there's only a key and a value, parse the value
		// and store it in the composite

		if ( !( type.getDataType( key ) instanceof CompositeType ) )
		{
			this.aset( key, DataTypes.parseValue( type.getDataType( key ), data[ index + 1 ], true ) );
			return 2;
		}

		// Otherwise, recurse until we get the final slice
		CompositeValue slice = (CompositeValue) this.aref( key );

		// Create missing intermediate slice
		if ( slice == null )
		{
			slice = (CompositeValue) this.initialValue( key );
			this.aset( key, slice );
		}

		return slice.read( data, index + 1, compact ) + 1;
	}

	public String toString()
	{
		return "composite " + this.type.toString();
	}
}
