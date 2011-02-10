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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class Value
	implements ParseTreeNode, Comparable
{
	public Type type;

	public int contentInt = 0;
	public float contentFloat = 0.0f;
	public String contentString = null;
	public Object content = null;

	public Value()
	{
		this.type = DataTypes.VOID_TYPE;
	}

	public Value( final int value )
	{
		this.type = DataTypes.INT_TYPE;
		this.contentInt = value;
	}

	public Value( final boolean value )
	{
		this.type = DataTypes.BOOLEAN_TYPE;
		this.contentInt = value ? 1 : 0;
	}

	public Value( final String value )
	{
		this.type = DataTypes.STRING_TYPE;
		this.contentString = value;
	}

	public Value( final float value )
	{
		this.type = DataTypes.FLOAT_TYPE;
		this.contentInt = (int) value;
		this.contentFloat = value;
	}

	public Value( final Type type )
	{
		this.type = type;
	}

	public Value( final Type type, final int contentInt, final String contentString )
	{
		this.type = type;
		this.contentInt = contentInt;
		this.contentString = contentString;
	}

	public Value( final Type type, final String contentString, final Object content )
	{
		this.type = type;
		this.contentString = contentString;
		this.content = content;
	}

	public Value( final Value original )
	{
		this.type = original.type;
		this.contentInt = original.contentInt;
		this.contentString = original.contentString;
		this.content = original.content;
	}

	public Value toFloatValue()
	{
		if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return this;
		}
		return DataTypes.makeFloatValue( (float) this.contentInt );
	}

	public Value toIntValue()
	{
		if ( this.type.equals( DataTypes.TYPE_INT ) )
		{
			return this;
		}
		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return DataTypes.makeIntValue( this.contentInt != 0 );
		}
		return DataTypes.makeIntValue( (int) this.contentFloat );
	}

	public Value toBooleanValue()
	{
		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return this;
		}
		return DataTypes.makeBooleanValue( this.contentInt );
	}

	public Type getType()
	{
		return this.type.getBaseType();
	}

	public String toString()
	{
		if ( this.content instanceof StringBuffer )
		{
			return ( (StringBuffer) this.content ).toString();
		}

		if ( this.type.equals( DataTypes.TYPE_VOID ) )
		{
			return "void";
		}

		if ( this.contentString != null )
		{
			return this.contentString;
		}

		if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			return String.valueOf( this.contentInt != 0 );
		}

		if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
		{
			return String.valueOf( this.contentFloat );
		}

		return String.valueOf( this.contentInt );
	}

	public String toQuotedString()
	{
		if ( this.contentString != null )
		{
			return "\"" + this.contentString + "\"";
		}
		return this.toString();
	}

	public Value toStringValue()
	{
		return new Value( this.toString() );
	}

	public Object rawValue()
	{
		return this.content;
	}

	public int intValue()
	{
		return this.contentInt;
	}

	public float floatValue()
	{
		return this.contentFloat;
	}

	public Value execute( final Interpreter interpreter )
	{
		return this;
	}
	
	public Value asProxy()
	{
		if ( this.type == DataTypes.ITEM_TYPE )
		{
			return new ProxyRecordValue.ItemProxy( this );
		}
		if ( this.type == DataTypes.FAMILIAR_TYPE )
		{
			return new ProxyRecordValue.FamiliarProxy( this );
		}
		return this;
	}
	
	/* null-safe version of the above */
	public static Value asProxy( Value value )
	{
		if ( value == null )
		{
			return null;
		}
		return value.asProxy();
	}

	public int compareTo( final Object o )
	{
		if ( !( o instanceof Value ) )
		{
			throw new ClassCastException();
		}

		Value it = (Value) o;

		if ( this.type == DataTypes.BOOLEAN_TYPE || this.type == DataTypes.INT_TYPE )
		{
			return this.contentInt < it.contentInt ? -1 : this.contentInt == it.contentInt ? 0 : 1;
		}

		if ( this.type == DataTypes.FLOAT_TYPE )
		{
			return this.contentFloat < it.contentFloat ? -1 : this.contentFloat == it.contentFloat ? 0 : 1;
		}

		if ( this.contentString != null && it.contentString != null )
		{
			return this.contentString.compareToIgnoreCase( it.contentString );
		}

		return -1;
	}

	public int count()
	{
		return 1;
	}

	public void clear()
	{
	}

	public boolean contains( final Value index )
	{
		return false;
	}

	public boolean equals( final Object o )
	{
		return o == null || !( o instanceof Value ) ? false : this.compareTo( (Comparable) o ) == 0;
	}

	public void dumpValue( final PrintStream writer )
	{
		writer.print( this.toStringValue().toString() );
	}

	public void dump( final PrintStream writer, final String prefix, final boolean compact )
	{
		writer.println( prefix + this.toStringValue().toString() );
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<VALUE " + this.getType() + " [" + this.toString() + "]>" );
	}
}
