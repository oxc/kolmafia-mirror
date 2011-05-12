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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ProxyRecordValue
	extends RecordValue
{
	public ProxyRecordValue( final RecordType type, final Value obj )
	{
		super( type );

		this.contentInt = obj.contentInt;
		this.contentFloat = obj.contentFloat;
		this.contentString = obj.contentString;
		this.content = obj.content;
	}

	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		return this.aref( index, interpreter );
	}

	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		Object rv;
		try
		{
			rv = this.getClass().getMethod(
				"get_" + type.getFieldNames()[ index ], null ).invoke( this, null );
		}
		catch ( InvocationTargetException e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e.getCause() );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e );
		}

		if ( rv == null )
		{
			return type.getFieldTypes()[ index ].initialValue();
		}
		if ( rv instanceof Value )
		{
			return (Value) rv;
		}
		if ( rv instanceof Integer )
		{
			return DataTypes.makeIntValue( ((Integer) rv).intValue() );
		}
		if ( rv instanceof Float )
		{
			return DataTypes.makeFloatValue( ((Float) rv).floatValue() );
		}
		if ( rv instanceof String )
		{
			return new Value( rv.toString() );
		}
		if ( rv instanceof Boolean )
		{
			return DataTypes.makeBooleanValue( ((Boolean) rv).booleanValue() );
		}
		throw interpreter.runtimeException( "Unable to convert attribute value of type: " + rv.getClass() );
	}

	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public Value remove( final Value key, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	public void clear()
	{
	}

	/* Helper for building parallel arrays of field names & types */
	private static class RecordBuilder
	{
		private ArrayList names;
		private ArrayList types;

		public RecordBuilder()
		{
			names = new ArrayList();
			types = new ArrayList();
		}

		public RecordBuilder add( String name, Type type )
		{
			this.names.add( name.toLowerCase() );
			this.types.add( type );
			return this;
		}

		public RecordType finish( String name )
		{
			int len = this.names.size();
			return new RecordType( name,
				(String[]) this.names.toArray( new String[len] ),
				(Type[]) this.types.toArray( new Type[len] ) );
		}
	}

	public static class ItemProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "plural", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "levelreq", DataTypes.INT_TYPE )
			.add( "fullness", DataTypes.INT_TYPE )
			.add( "inebriety", DataTypes.INT_TYPE )
			.add( "spleen", DataTypes.INT_TYPE )
			.add( "notes", DataTypes.STRING_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "reusable", DataTypes.BOOLEAN_TYPE )
			.add( "usable", DataTypes.BOOLEAN_TYPE )
			.add( "multi", DataTypes.BOOLEAN_TYPE )
			.finish( "item proxy" );

		public ItemProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_plural()
		{
			return ItemDatabase.getPluralName( this.contentString );
		}

		public String get_descid()
		{
			return ItemDatabase.getDescriptionId( this.contentString );
		}

		public String get_image()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getImage( id );
		}

		public Integer get_levelreq()
		{
			return ItemDatabase.getLevelReqByName( this.contentString );
		}

		public int get_fullness()
		{
			return ItemDatabase.getFullness( this.contentString );
		}

		public int get_inebriety()
		{
			return ItemDatabase.getInebriety( this.contentString );
		}

		public int get_spleen()
		{
			return ItemDatabase.getSpleenHit( this.contentString );
		}

		public String get_notes()
		{
			return ItemDatabase.getNotes( this.contentString );
		}

		public boolean get_combat()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getAttribute( id, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_reusable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getConsumptionType( id) == KoLConstants.INFINITE_USES ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_REUSABLE | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_usable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getConsumptionType( id) == KoLConstants.CONSUME_USE ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_USABLE | ItemDatabase.ATTR_MULTIPLE | ItemDatabase.ATTR_REUSABLE );
		}

		public boolean get_multi()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getConsumptionType( id) == KoLConstants.CONSUME_MULTIPLE ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_MULTIPLE );
		}
	}

	public static class FamiliarProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "hatchling", DataTypes.ITEM_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.finish( "familiar proxy" );

		public FamiliarProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_combat()
		{
			return FamiliarDatabase.isCombatType( this.contentInt );
		}

		public Value get_hatchling()
		{
			return DataTypes.makeItemValue(
				FamiliarDatabase.getFamiliarLarva( this.contentInt ) );
		}

		public String get_image()
		{
			return FamiliarDatabase.getFamiliarImageLocation( this.contentInt );
		}
	}

	public static class SkillProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "level", DataTypes.INT_TYPE )
			.add( "traincost", DataTypes.INT_TYPE )
			.add( "class", DataTypes.STRING_TYPE )
			.add( "libram", DataTypes.BOOLEAN_TYPE )
			.add( "passive", DataTypes.BOOLEAN_TYPE )
			.add( "buff", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "permable", DataTypes.BOOLEAN_TYPE )
			.finish( "skill proxy" );

		public SkillProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_level()
		{
			return SkillDatabase.getSkillLevel( this.contentInt );
		}

		public int get_traincost()
		{
			return SkillDatabase.getSkillPurchaseCost( this.contentInt );
		}

		public Value get_class()
		{
			return DataTypes.parseClassValue(
				SkillDatabase.getSkillCategory( this.contentInt ), true );
		}

		public boolean get_libram()
		{
			return SkillDatabase.isLibramSkill( this.contentInt );
		}

		public boolean get_passive()
		{
			return SkillDatabase.isPassive( this.contentInt );
		}

		public boolean get_buff()
		{
			return SkillDatabase.isBuff( this.contentInt );
		}

		public boolean get_combat()
		{
			return SkillDatabase.isCombat( this.contentInt );
		}

		public boolean get_permable()
		{
			return SkillDatabase.isPermable( this.contentInt );
		}
	}		

	public static class EffectProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "default", DataTypes.STRING_TYPE )
			.add( "note", DataTypes.STRING_TYPE )
			.add( "all",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.finish( "effect proxy" );

		public EffectProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_default()
		{
			return EffectDatabase.getDefaultAction( this.contentString );
		}

		public String get_note()
		{
			return EffectDatabase.getActionNote( this.contentString );
		}

		public Value get_all()
		{
			Iterator i = EffectDatabase.getAllActions( this.contentString );
			ArrayList rv = new ArrayList();
			while ( i.hasNext() )
			{
				rv.add( new Value( (String) i.next() ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public String get_image()
		{
			return EffectDatabase.getImage( this.contentInt );
		}

		public String get_descid()
		{
			return EffectDatabase.getDescriptionId( this.contentInt );
		}
	}

	public static class LocationProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "nocombats", DataTypes.BOOLEAN_TYPE )
			.add( "zone", DataTypes.STRING_TYPE )
			.add( "parent", DataTypes.STRING_TYPE )
			.add( "parentdesc", DataTypes.STRING_TYPE )
			.finish( "location proxy" );

		public LocationProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_nocombats()
		{
			return ((KoLAdventure) this.content).isNonCombatsOnly();
		}

		public String get_zone()
		{
			return ((KoLAdventure) this.content).getZone();
		}

		public String get_parent()
		{
			return ((KoLAdventure) this.content).getParentZone();
		}

		public String get_parentdesc()
		{
			return ((KoLAdventure) this.content).getParentZoneDescription();
		}
	}
}
