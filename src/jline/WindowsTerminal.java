/**
 *	jline - Java console input library
 *	Copyright (c) 2002-2006, Marc Prud'hommeaux <mwp1@cornell.edu>
 *	All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or
 *	without modification, are permitted provided that the following
 *	conditions are met:
 *
 *	Redistributions of source code must retain the above copyright
 *	notice, this list of conditions and the following disclaimer.
 *
 *	Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer
 *	in the documentation and/or other materials provided with
 *	the distribution.
 *
 *	Neither the name of JLine nor the names of its contributors
 *	may be used to endorse or promote products derived from this
 *	software without specific prior written permission.
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 *	BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 *	AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 *	EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *	FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *	OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *	AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *	LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *	IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *	OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jline;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.java.dev.spellcast.utilities.DataUtilities;

// TODO: handle arrow keys, which might require completely implementing the
// console input reading in the .dll. For example, see:
// http://cvs.sourceforge.net/viewcvs.py/lifelines/lifelines/
// win32/mycurses.c?rev=1.28

/**
 *	<p>
 *	Terminal implementation for Microsoft Windows. Terminal initialization
 *	in {@link #initializeTerminal} is accomplished by extracting the
 *	<em>jline_<i>version</i>.dll</em>, saving it to the system temporary
 *	directoy (determined by the setting of the <em>java.io.tmpdir</em>
 *	System property), loading the library, and then calling the Win32 APIs
 *  <a href="http://msdn.microsoft.com/library/default.asp?
 *  url=/library/en-us/dllproc/base/setconsolemode.asp">SetConsoleMode</a>
 *  and
 *  <a href="http://msdn.microsoft.com/library/default.asp?
 *  url=/library/en-us/dllproc/base/getconsolemode.asp">GetConsoleMode</a>
 *  to disable character echoing.
 *  </p>
 *
 *  <p>
 *  By default, the {@link #readCharacter} method will attempt to test
 *  to see if the specified {@link InputStream} is {@link System#in}
 *  or a wrapper around {@link FileDescriptor#in}, and if so, will
 *  bypass the character reading to directly invoke the
 *  readc() method in the JNI library. This is so the class can
 *  read special keys (like arrow keys) which are otherwise
 *  inaccessible via the {@link System#in} stream. Using JNI
 *  reading can be bypassed by setting the
 *  <code>jline.WindowsTerminal.disableDirectConsole</code> system
 *  property to <code>true</code>.
 *  </p>
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class WindowsTerminal
	extends Terminal
{
	// constants copied from wincon.h

	/**
	 *  The ReadFile or ReadConsole function returns only when
	 *  a carriage return character is read. If this mode is disable,
	 *  the functions return when one or more characters are
	 *  available.
	 */
	private static final int ENABLE_LINE_INPUT			= 2;


	/**
	 *  Characters read by the ReadFile or ReadConsole function
	 *  are written to the active screen buffer as they are read.
	 *  This mode can be used only if the ENABLE_LINE_INPUT mode
	 *  is also enabled.
	 */
	private static final int ENABLE_ECHO_INPUT			= 4;


	/**
	 *  CTRL+C is processed by the system and is not placed
	 *  in the input buffer. If the input buffer is being read
	 *  by ReadFile or ReadConsole, other control keys are processed
	 *  by the system and are not returned in the ReadFile or ReadConsole
	 *  buffer. If the ENABLE_LINE_INPUT mode is also enabled,
	 *  backspace, carriage return, and linefeed characters are
	 *  handled by the system.
	 */
	private static final int ENABLE_PROCESSED_INPUT		= 1;


	/**
	 *  User interactions that change the size of the console
	 *  screen buffer are reported in the console's input buffee.
	 *  Information about these events can be read from the input
	 *  buffer by applications using theReadConsoleInput function,
	 *  but not by those using ReadFile orReadConsole.
	 */
	private static final int ENABLE_WINDOW_INPUT		= 8;


	/**
	 *  If the mouse pointer is within the borders of the console
	 *  window and the window has the keyboard focus, mouse events
	 *  generated by mouse movement and button presses are placed
	 *  in the input buffer. These events are discarded by ReadFile
	 *  or ReadConsole, even when this mode is enabled.
	 */
	private static final int ENABLE_MOUSE_INPUT			= 16;


	/**
	 *  When enabled, text entered in a console window will
	 *  be inserted at the current cursor location and all text
	 *  following that location will not be overwritten. When disabled,
	 *  all following text will be overwritten. An OR operation
	 *  must be performed with this flag and the ENABLE_EXTENDED_FLAGS
	 *  flag to enable this functionality.
	 */
	private static final int ENABLE_PROCESSED_OUTPUT	= 1;


	/**
	 *  This flag enables the user to use the mouse to select
	 *  and edit text. To enable this option, use the OR to combine
	 *  this flag with ENABLE_EXTENDED_FLAGS.
	 */
	private static final int ENABLE_WRAP_AT_EOL_OUTPUT	= 2;


	private Boolean directConsole;


	public WindowsTerminal ()
	{
		String dir = System.getProperty ("jline.WindowsTerminal.directConsole");
		if ("true".equals (dir))
			directConsole = Boolean.TRUE;
		else if ("false".equals (dir))
			directConsole = Boolean.FALSE;
	}


	private native int getConsoleMode ();

	private native void setConsoleMode (final int mode);

	private native int readByte ();

	private native int getWindowsTerminalWidth ();

	private native int getWindowsTerminalHeight ();


	public int readCharacter (final InputStream in)
		throws IOException
	{
		// if we can detect that we are directly wrapping the system
		// input, then bypass the input stream and read directly (which
		// allows us to access otherwise unreadable strokes, such as
		// the arrow keys)
		if (directConsole == Boolean.FALSE)
			return super.readCharacter (in);
		else if (directConsole == Boolean.TRUE ||
			((in == System.in || (in instanceof FileInputStream &&
				((FileInputStream)in).getFD () == FileDescriptor.in))))
			return readByte ();
		else
			return super.readCharacter (in);
	}


	public void initializeTerminal ()
		throws Exception
	{
		loadLibrary();

		final int originalMode = getConsoleMode ();

		setConsoleMode (originalMode & ~ENABLE_ECHO_INPUT);

		// set the console to raw mode
		int newMode = originalMode
			& ~(ENABLE_LINE_INPUT
				| ENABLE_ECHO_INPUT
				| ENABLE_PROCESSED_INPUT
				| ENABLE_WINDOW_INPUT);
		setConsoleMode (newMode);

		// at exit, restore the original tty configuration (for JDK 1.3+)
		try
		{
			Runtime.getRuntime ().addShutdownHook (new Thread ()
			{
				public void start ()
				{
					// restore the old console mode
					setConsoleMode (originalMode);
				}
			});
		}
		catch (AbstractMethodError ame)
		{
			// JDK 1.3+ only method. Bummer.
			consumeException (ame);
		}
	}


	private void loadLibrary ()
		throws IOException
	{
		File library = new File( "data/jline.dll" );
		if ( !library.exists() )
		{
			InputStream input = DataUtilities.getInputStream( "", "jline.dll" );
			OutputStream output = new FileOutputStream( library );

			byte [] buffer = new byte[ 1024 ];
			int bufferLength;
			while ( (bufferLength = input.read( buffer )) != -1 )
				output.write( buffer, 0, bufferLength );

			output.close();
		}

		// now actually load the DLL
		System.load( library.getAbsolutePath() );
	}


	public int readVirtualKey (InputStream in)
		throws IOException
	{
		int c = readCharacter (in);

		// in Windows terminals, arrow keys are represented by
		// a sequence of 2 characters. E.g., the up arrow
		// key yields 224, 72
		if (c == 224)
		{
			c = readCharacter (in);
			if (c == 72)
				return CTRL_P; // translate UP -> CTRL-P
			else if (c == 80)
				return CTRL_N; // translate DOWN -> CTRL-N
			else if (c == 75)
				return CTRL_B; // translate LEFT -> CTRL-B
			else if (c == 77)
				return CTRL_F; // translate RIGHT -> CTRL-F
		}

		return c;
	}


	public boolean isSupported ()
	{
		return true;
	}


	/**
	 *  Windows doesn't support ANSI codes by default; disable them.
	 */
	public boolean isANSISupported ()
	{
		return false;
	}


	public boolean getEcho ()
	{
		return false;
	}


	/**
	 *  Unsupported; return the default.
	 *
	 *  @see Terminal#getTerminalWidth
	 */
	public int getTerminalWidth ()
	{
		return getWindowsTerminalWidth ();
	}


	/**
	 *  Unsupported; return the default.
	 *
	 *  @see Terminal#getTerminalHeight
	 */
	public int getTerminalHeight ()
	{
		return getWindowsTerminalHeight ();
	}


	/**
	 *  No-op for exceptions we want to silently consume.
	 */
	private void consumeException (final Throwable e)
	{
	}


	/**
	 *  Whether or not to allow the use of the JNI console interaction.
	 */
	public void setDirectConsole (Boolean directConsole)
	{
		this.directConsole = directConsole;
	}


	/**
	 *  Whether or not to allow the use of the JNI console interaction.
	 */
	public Boolean getDirectConsole ()
	{
		return this.directConsole;
	}


}

