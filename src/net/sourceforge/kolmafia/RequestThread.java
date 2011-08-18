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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import javax.swing.SwingUtilities;
import net.sourceforge.foxtrot.Job;
import net.sourceforge.foxtrot.Worker;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.EventMessage;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

public abstract class RequestThread
{
	private static int sequenceCount = 0;

	/**
	 * Posts a single request one time without forcing concurrency. The display will be enabled if there is no sequence.
	 */

	public static final void postRequest( final GenericRequest request )
	{
		if ( request == null )
		{
			return;
		}

		if ( RequestThread.sequenceCount == 0 && !request.hasNoResult() )
		{
			KoLmafia.forceContinue();
		}

		++RequestThread.sequenceCount;

		try
		{
			// If you're not in the event dispatch thread, you can run
			// without posting to a separate thread.

			if ( !SwingUtilities.isEventDispatchThread() )
			{
				request.run();
			}
			else
			{
				Worker.post( new RunnableWrapper( request ) );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--RequestThread.sequenceCount;
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final void postRequest( final KoLAdventure request )
	{
		if ( request == null )
		{
			return;
		}

		KoLmafia.forceContinue();
		++RequestThread.sequenceCount;

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				request.run();
			}
			else
			{
				Worker.post( new RunnableWrapper( request ) );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--RequestThread.sequenceCount;
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final void postRequest( final Runnable request )
	{
		if ( request == null )
		{
			return;
		}

		if ( RequestThread.sequenceCount == 0 )
		{
			KoLmafia.forceContinue();
		}

		++RequestThread.sequenceCount;

		// If you're not in the event dispatch thread, you can run
		// without posting to a separate thread.

		try
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				request.run();
			}
			else
			{
				Worker.post( new RunnableWrapper( request ) );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		--RequestThread.sequenceCount;
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final void openRequestSequence()
	{
		if ( RequestThread.sequenceCount == 0 )
		{
			KoLmafia.forceContinue();
		}

		++RequestThread.sequenceCount;
	}

	public static final void closeRequestSequence()
	{
		if ( RequestThread.sequenceCount <= 0 )
		{
			return;
		}

		--RequestThread.sequenceCount;
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final boolean enableDisplayIfSequenceComplete()
	{
		if ( RequestThread.sequenceCount != 0 )
		{
			return false;
		}

		if ( KoLmafia.getLastMessage().endsWith( "..." ) )
		{
			KoLmafia.updateDisplay( "Requests complete." );
			SystemTrayFrame.showBalloon( "Requests complete." );
			RequestLogger.printLine();
		}

		if ( KoLmafia.permitsContinue() || KoLmafia.refusesContinue() )
		{
			KoLmafia.enableDisplay();
		}

		return true;
	}

	/**
	 * Declare world peace. This causes all pending requests and queued commands to be cleared, along with all currently
	 * running requests to be notified that they should stop as soon as possible.
	 */

	public static final void declareWorldPeace()
	{
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "KoLmafia declares world peace." );
		EventMessage message = new EventMessage( "KoLmafia declares world peace.", "red" );
		ChatManager.broadcastEvent( message );
	}

	private static class RunnableWrapper
		extends Job
	{
		private final Runnable wrapped;

		public RunnableWrapper( final Runnable wrapped )
		{
			this.wrapped = wrapped;
		}

		public Object run()
		{
			try
			{
				this.wrapped.run();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			return null;
		}
	}
}
