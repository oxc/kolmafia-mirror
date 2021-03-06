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

package net.sourceforge.kolmafia.textui.javascript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class ProxyRecordMethodWrapper
	extends BaseFunction
{
	private static final long serialVersionUID = 1L;

	private final Method method;

	public ProxyRecordMethodWrapper( Scriptable scope, Scriptable prototype, Method method )
	{
		super( scope, prototype );
		this.method = method;
	}

	@Override
	public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
	{
		if ( !( thisObj instanceof EnumeratedWrapper ) )
		{
			return null;
		}

		try
		{
			Object returnValue = method.invoke( ((EnumeratedWrapper) thisObj).getWrapped().asProxy() );

			if ( returnValue instanceof Value && ((Value) returnValue).asProxy() instanceof ProxyRecordValue )
			{
				returnValue = EnumeratedWrapper.wrap( scope, returnValue.getClass(), (Value) returnValue );
			}
			else if ( !(returnValue instanceof Scriptable) )
			{
				returnValue = Context.javaToJS( returnValue, scope );
			}

			if ( returnValue instanceof NativeJavaObject )
			{
				throw new ScriptException( "ASH function returned native Java object." );
			}

			return returnValue;
		}
		catch ( IllegalAccessException e )
		{
			return null;
		}
		catch ( InvocationTargetException e )
		{
			return null;
		}
	}
}
