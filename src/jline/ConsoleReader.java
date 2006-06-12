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

import java.io.*;
import java.util.*;

/**
 *  A reader for console applications. It supports custom tab-completion,
 *  saveable command history, and command line editing. On some
 *  platforms, platform-specific commands will need to be
 *  issued before the reader will function properly. See
 *  {@link Terminal#initializeTerminal} for convenience methods for
 *  issuing platform-specific setup commands.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleReader
	implements ConsoleOperations
{
	String prompt;

	public static final String CR = System.getProperty ("line.separator");

	/**
	 *  The map for logical operations.
	 */
	private final short[] keybindings;


	/**
	 *  If true, issue an audible keyboard bell when appropriate.
	 */
	private boolean bellEnabled = true;


	/**
	 *  The current character mask.
	 */
	private Character mask = null;


	/**
	 *  The null mask.
	 */
	private static final Character NULL_MASK = new Character ((char)0);


	/**
	 *  The number of tab-completion candidates above which a warning
	 *  will be prompted before showing all the candidates.
	 */
	private int autoprintThreshhold = Integer.getInteger (
		"jline.completion.threshold", 100).intValue (); // same default as bash


	/**
	 *  The Terminal to use.
	 */
	private final Terminal terminal;


	private CompletionHandler completionHandler
		= new CandidateListCompletionHandler ();


	InputStream in;
	final Writer out;
	final CursorBuffer buf = new CursorBuffer ();
	static PrintWriter debugger;
	History history = new History ();
	final List completors = new LinkedList ();

	private Character echoCharacter = null;


	/**
	 *  Create a new reader using {@link FileDescriptor#in} for input
	 *  and {@link System#out} for output. {@link FileDescriptor#in} is
	 *  used because it has a better chance of being unbuffered.
	 */
	public ConsoleReader ()
		throws IOException
	{
		this (new FileInputStream (FileDescriptor.in),
			new PrintWriter (System.out));
	}


	/**
	 *  Create a new reader using the specified {@link InputStream}
	 *  for input and the specific writer for output, using the
	 *  default keybindings resource.
	 */
	public ConsoleReader (final InputStream in, final Writer out)
		throws IOException
	{
		this (in, out, Terminal.getTerminal());
	}

	/**
	 *  Create a new reader.
	 *
	 *  @param  in			the input
	 *  @param  out			the output
	 *  @param  term		the terminal to use
	 */

	public ConsoleReader (InputStream in, Writer out, Terminal term)
		throws IOException
	{
		this.terminal = term;
		setInput (in);
		this.out = out;

		this.keybindings = new short[Byte.MAX_VALUE * 2];
		Arrays.fill (this.keybindings, UNKNOWN);


		// hardwired arrow key bindings
		keybindings[1] = MOVE_TO_BEG;
		keybindings[2] = PREV_CHAR;
		keybindings[4] = EXIT;
		keybindings[5] = MOVE_TO_END;
		keybindings[6] = NEXT_CHAR;
		keybindings[7] = PREV_WORD;
		keybindings[8] = DELETE_PREV_CHAR;
		keybindings[9] = COMPLETE;
		keybindings[10] = NEWLINE;
		keybindings[11] = KILL_LINE;
		keybindings[12] = CLEAR_SCREEN;
		keybindings[13] = NEWLINE;
		keybindings[14] = NEXT_HISTORY;
		keybindings[16] = PREV_HISTORY;
		keybindings[18] = REDISPLAY;
		keybindings[21] = KILL_LINE_PREV;
		keybindings[22] = PASTE;
		keybindings[23] = DELETE_PREV_WORD;
		keybindings[127] = DELETE_PREV_CHAR;
	}

	public Terminal getTerminal ()
	{
		return this.terminal;
	}



	/**
	 *  Set the stream for debugging. Development use only.
	 */
	public void setDebug (final PrintWriter debugger)
	{
		ConsoleReader.debugger = debugger;
	}


	/**
	 *  Set the stream to be used for console input.
	 */
	public void setInput (final InputStream in)
	{
		this.in = in;
	}


	/**
	 *  Returns the stream used for console input.
	 */
	public InputStream getInput ()
	{
		return this.in;
	}


	/**
	 *  Read the next line and return the contents of the buffer.
	 */
	public String readLine ()
		throws IOException
	{
		return readLine ((String)null);
	}


	/**
	 *  Read the next line with the specified character mask. If null, then
	 *  characters will be echoed. If 0, then no characters will be echoed.
	 */
	public String readLine (final Character mask)
		throws IOException
	{
		return readLine (null, mask);
	}


	/**
	 *  @param  bellEnabled  if true, enable audible keyboard bells if
	 *  					an alert is required.
	 */
	public void setBellEnabled (final boolean bellEnabled)
	{
		this.bellEnabled = bellEnabled;
	}


	/**
	 *  @return  true is audible keyboard bell is enabled.
	 */
	public boolean getBellEnabled ()
	{
		return this.bellEnabled;
	}


	/**
	 *	Query the terminal to find the current width;
	 *
	 *	@see	 Terminal#getTerminalWidth
	 *  @return  the width of the current terminal.
	 */
	public int getTermwidth ()
	{
		return Terminal.setupTerminal ().getTerminalWidth ();
	}


	/**
	 *	Query the terminal to find the current width;
	 *
	 *	@see	 Terminal#getTerminalHeight
	 *
	 *  @return  the height of the current terminal.
	 */
	public int getTermheight ()
	{
		return Terminal.setupTerminal ().getTerminalHeight ();
	}


	/**
	 *  @param  autoprintThreshhold  the number of candidates to print
	 *  							without issuing a warning.
	 */
	public void setAutoprintThreshhold (final int autoprintThreshhold)
	{
		this.autoprintThreshhold = autoprintThreshhold;
	}


	/**
	 *  @return  the number of candidates to print without issing a warning.
	 */
	public int getAutoprintThreshhold ()
	{
		return this.autoprintThreshhold;
	}


	int getKeyForAction (short logicalAction)
	{
		for (int i = 0; i < keybindings.length; i++)
		{
			if (keybindings[i] == logicalAction)
			{
				return i;
			}
		}

		return -1;
	}


	/**
	 *  Clear the echoed characters for the specified character code.
	 */
	int clearEcho (int c)
		throws IOException
	{
		// if the terminal is not echoing, then just return...
		if (!terminal.getEcho ())
			return 0;

		// otherwise, clear
		int num = countEchoCharacters ((char)c);
		back (num);
		drawBuffer (num);

		return num;
	}


	int countEchoCharacters (char c)
	{
		// tabs as special: we need to determine the number of spaces
		// to cancel based on what out current cursor position is
		if (c == 9)
		{
			int tabstop = 8; // will this ever be different?
			int position = getCursorPosition ();
			return tabstop - (position % tabstop);
		}

		return getPrintableCharacters (c).length ();
	}


	/**
	 *  Return the number of characters that will be printed when the
	 *  specified character is echoed to the screen. Adapted from
	 *	cat by Torbjorn Granlund, as repeated in stty by
	 *	David MacKenzie.
	 */
	StringBuffer getPrintableCharacters (char ch)
	{
		StringBuffer sbuff = new StringBuffer ();
		if (ch >= 32)
		{
			if (ch < 127)
			{
				sbuff.append (ch);
			}
			else if (ch == 127)
			{
				sbuff.append ('^');
				sbuff.append ('?');
			}
			else
			{
				sbuff.append ('M');
				sbuff.append ('-');
				if (ch >= 128 + 32)
				{
					if (ch < 128 + 127)
					{
						sbuff.append ((char)(ch - 128));
					}
					else
					{
						sbuff.append ('^');
						sbuff.append ('?');
					}
				}
				else
				{
					sbuff.append ('^');
					sbuff.append ((char)(ch - 128 + 64));
				}
			}
		}
		else
		{
			sbuff.append ('^');
			sbuff.append ((char)(ch + 64));
		}

		return sbuff;
	}


	int getCursorPosition ()
	{
		// FIXME: does not handle anything but a line with a prompt
		return (prompt == null ? 0 : prompt.length ())
			+ buf.cursor; // absolute position
	}


	public String readLine (final String prompt)
		throws IOException
	{
		return readLine (prompt, null);
	}


	/**
	 *  Read a line from the <i>in</i> {@link InputStream}, and
	 *  return the line (without any trailing newlines).
	 *
	 *  @param  prompt	the prompt to issue to the console, may be null.
	 *  @return	a line that is read from the terminal, or null if there
	 *  		was null input (e.g., <i>CTRL-D</i> was pressed).
	 */
	public String readLine (final String prompt, final Character mask)
		throws IOException
	{
		this.mask = mask;
		this.prompt = prompt;

		if (prompt != null && prompt.length () > 0)
		{
			out.write (prompt);
			out.flush ();
		}

		// if the terminal is unsupported, just use plain-java reading
		if (!terminal.isSupported ())
			return readLine (in);

		while (true)
		{
			int[] next = readBinding ();
			if (next == null)
				return null;

			int c = next[0];
			int code = next[1];

			if (c == -1)
				return null;

			boolean success = true;

			switch (code)
			{
				case EXIT: // ctrl-d
					if (buf.buffer.length () == 0)
						return null;
				case COMPLETE: // tab
					success = complete ();
					break;
				case MOVE_TO_BEG:
					success = setCursorPosition (0);
					break;
				case KILL_LINE: // CTRL-K
					success = killLine ();
					break;
				case CLEAR_SCREEN: // CTRL-L
					success = clearScreen ();
					break;
				case KILL_LINE_PREV: // CTRL-U
					success = resetLine ();
					break;
				case NEWLINE: // enter
					printNewline (); // output newline
					return finishBuffer ();
				case DELETE_PREV_CHAR: // backspace
					success = backspace ();
					break;
				case MOVE_TO_END:
					success = moveToEnd ();
					break;
				case PREV_CHAR:
					success = moveCursor (-1) != 0;
					break;
				case NEXT_CHAR:
					success = moveCursor (1) != 0;
					break;
				case NEXT_HISTORY:
					success = moveHistory (true);
					break;
				case PREV_HISTORY:
					success = moveHistory (false);
					break;
				case REDISPLAY:
					break;
				case PASTE:
					success = paste ();
					break;
				case DELETE_PREV_WORD:
					success = deletePreviousWord ();
					break;
				case PREV_WORD:
					success = previousWord ();
					break;
				case NEXT_WORD:
					success = nextWord ();
					break;

				case UNKNOWN:
				default:
					putChar (c, true);
			}

			if (!(success))
				beep ();

			flushConsole ();
		}
	}


	private String readLine (InputStream in)
		throws IOException
	{
		StringBuffer buf = new StringBuffer ();
		while (true)
		{
			int i = in.read ();
			if (i == -1 || i == '\n' || i == '\r')
				return buf.toString ();

			buf.append ((char)i);
		}

		// return new BufferedReader (new InputStreamReader (in)).readLine ();
	}


	/**
	 *  Reads the console input and returns an array of the form
	 *  [raw, key binding].
	 */
	private int[] readBinding ()
		throws IOException
	{
		int c = readVirtualKey ();
		if (c == -1)
			return null;

		// extract the appropriate key binding
		short code = keybindings[c];

		if (debugger != null)
			debug ("    translated: " + (int)c + ": " + code);

		return new int[] { c, code };
	}


	/**
	 *  Move up or down the history tree.
	 *
	 *  @param  direction  less than 0 to move up the tree, down otherwise
	 */
	private final boolean moveHistory (final boolean next)
		throws IOException
	{
		if (next && !history.next ())
		   return false;
		else if (!next && !history.previous ())
			return false;

		setBuffer (history.current ());
		return true;
	}


	/**
	 *  Paste the contents of the clipboard into the console buffer
	 *
	 *  @return  true if clipboard contents pasted
	 */
	public boolean paste ()
		throws IOException
	{
		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
			return false;

		java.awt.datatransfer.Clipboard clipboard
			= java.awt.Toolkit.getDefaultToolkit ().getSystemClipboard ();
		if (clipboard == null)
			return false;

		java.awt.datatransfer.Transferable transferable
			= clipboard.getContents (null);

		if (transferable == null)
			return false;

		try
		{
			Object content = new java.awt.datatransfer.DataFlavor().
				getReaderForText (transferable);

			if (content == null)
				return false;

			String value;

			if (content instanceof Reader)
			{
				// TODO: we might want instead connect to the input stream
				// so we can interpret individual lines
				value = "";
				String line = null;
				for (BufferedReader read = new BufferedReader ((Reader)content);
					(line = read.readLine ()) != null; )
				{
					if (value.length () > 0)
						value += "\n";

					value += line;
				}
			}
			else
			{
				value = content.toString ();
			}


			if (value == null)
				return true;

			putString (value);

			return true;
		}
		catch (java.awt.datatransfer.UnsupportedFlavorException ufe)
		{
			ufe.printStackTrace ();
			return false;
		}
	}


	/**
	 *  Kill the buffer ahead of the current cursor position.
	 *
	 *  @return  true if successful
	 */
	public boolean killLine ()
		throws IOException
	{
		int cp = buf.cursor;
		int len = buf.buffer.length ();
		if (cp >= len)
			return false;

		int num = buf.buffer.length () - cp;
		clearAhead (num);
		for (int i = 0; i < num; i++)
			buf.buffer.deleteCharAt (len - i - 1);
		return true;
	}


	/**
	 *  Clear the screen by issuing the ANSI "clear screen" code.
	 */
	public boolean clearScreen ()
		throws IOException
	{
		if (!terminal.isANSISupported ())
			return false;

		// send the ANSI code to clear the screen
		printString (((char)27) + "[2J");
		flushConsole ();

		// then send the ANSI code to go to position 1,1
		printString (((char)27) + "[1;1H");
		flushConsole ();

		redrawLine ();

		return true;
	}


	/**
	 *  Use the completors to modify the buffer with the
	 *  appropriate completions.
	 *
	 *  @return  true if successful
	 */
	private final boolean complete ()
		throws IOException
	{
		// debug ("tab for (" + buf + ")");

		if (completors.size () == 0)
			return false;

		List candidates = new LinkedList ();
		String bufstr = buf.buffer.toString ();
		int cursor = buf.cursor;

		int position = -1;

		for (Iterator i = completors.iterator (); i.hasNext (); )
		{
			Completor comp = (Completor)i.next ();
			if ((position = comp.complete (bufstr, cursor, candidates)) != -1)
				break;
		}

		// no candidates? Fail.
		if (candidates.size () == 0)
			return false;

		return completionHandler.complete (this, candidates, position);
	}


	public CursorBuffer getCursorBuffer ()
	{
		return buf;
	}


	/**
	 *  Output the specified {@link Collection} in proper columns.
	 *
	 *  @param  stuff  the stuff to print
	 */
	public void printColumns (final Collection stuff)
		throws IOException
	{
		if (stuff == null || stuff.size () == 0)
			return;

		int width = getTermwidth ();
		int maxwidth = 0;
		for (Iterator i = stuff.iterator (); i.hasNext ();
			maxwidth = Math.max (maxwidth, i.next ().toString ().length ()));

		StringBuffer line = new StringBuffer ();

		for (Iterator i = stuff.iterator (); i.hasNext (); )
		{
			String cur = (String)i.next ();

			if (line.length () + maxwidth > width)
			{
				printString (line.toString ().trim ());
				printNewline ();
				line.setLength (0);
			}

			pad (cur, maxwidth + 3, line);
		}

		if (line.length () > 0)
		{
			printString (line.toString ().trim ());
			printNewline ();
			line.setLength (0);
		}
	}


	/**
	 *  Append <i>toPad</i> to the specified <i>appendTo</i>, as
	 *  well as (<i>toPad.length () - len</i>) spaces.
	 *
	 *  @param  toPad		the {@link String} to pad
	 *  @param  len			the target length
	 *  @param  appendTo	the {@link StringBuffer} to which to append the
	 *  					padded {@link String}.
	 */
	private final void pad (final String toPad,
		final int len, final StringBuffer appendTo)
	{
		appendTo.append (toPad);
		for (int i = 0; i < (len - toPad.length ());
			i++, appendTo.append (' '));
	}


	/**
	 *  Add the specified {@link Completor} to the list of handlers
	 *  for tab-completion.
	 *
	 *  @param  completor  the {@link Completor} to add
	 *  @return	true if it was successfully added
	 */
	public boolean addCompletor (final Completor completor)
	{
		return completors.add (completor);
	}


	/**
	 *  Remove the specified {@link Completor} from the list of handlers
	 *  for tab-completion.
	 *
	 *  @param  completor  the {@link Completor} to remove
	 *  @return	true if it was successfully removed
	 */
	public boolean removeCompletor (final Completor completor)
	{
		return completors.remove (completor);
	}


	/**
	 *  Returns an unmodifiable list of all the completors.
	 */
	public Collection getCompletors ()
	{
		return Collections.unmodifiableList (completors);
	}


	/**
	 *  Erase the current line.
	 *
	 *  @return  false if we failed (e.g., the buffer was empty)
	 */
	final boolean resetLine ()
		throws IOException
	{
		if (buf.cursor == 0)
			return false;

		backspaceAll ();

		return true;
	}


	/**
	 *  Move the cursor position to the specified absolute index.
	 */
	public final boolean setCursorPosition (final int position)
		throws IOException
	{
		return moveCursor (position - buf.cursor) != 0;
	}


	/**
	 *  Set the current buffer's content to the specified
	 *  {@link String}. The visual console will be modified
	 *  to show the current buffer.
	 *
	 *  @param  buffer  the new contents of the buffer.
	 */
	private final void setBuffer (final String buffer)
		throws IOException
	{
		// don't bother modifying it if it is unchanged
		if (buffer.equals (buf.buffer.toString ()))
			return;

		// obtain the difference between the current buffer and the new one
		int sameIndex = 0;
		for (int i = 0, l1 = buffer.length (), l2 = buf.buffer.length ();
			i < l1 && i < l2; i++)
		{
			if (buffer.charAt (i) == buf.buffer.charAt (i))
				sameIndex++;
			else
				break;
		}

		int diff = buf.buffer.length () - sameIndex;

		backspace (diff); // go back for the differences
		killLine (); // clear to the end of the line
		buf.buffer.setLength (sameIndex); // the new length
		putString (buffer.substring (sameIndex)); // append the differences
	}


	/**
	 *  Clear the line and redraw it.
	 */
	public final void redrawLine ()
		throws IOException
	{
		printCharacter (RESET_LINE);
		flushConsole ();
		drawLine ();
	}


	/**
	 *  Output put the prompt + the current buffer
	 */
	public final void drawLine ()
		throws IOException
	{
		if (prompt != null)
			printString (prompt);
		printString (buf.buffer.toString ());
	}


	/**
	 *  Output a platform-dependant newline.
	 */
	public final void printNewline ()
		throws IOException
	{
		printString (CR);
		flushConsole ();
	}


	/**
	 *  Clear the buffer and add its contents to the history.
	 *
	 *  @return  the former contents of the buffer.
	 */
	final String finishBuffer ()
	{
		String str = buf.buffer.toString ();

		// we only add it to the history if the buffer is not empty
		// and if mask is null, since having a mask typically means
		// the string was a password. We clear the mask after this call
		if (str.length () > 0)
		{
			if (mask == null)
			{
				history.addToHistory (str);
			}
			else
			{
				mask = null;
			}
		}

		history.moveToEnd ();

		buf.buffer.setLength (0);
		buf.cursor = 0;
		return str;
	}


	/**
	 *  Write out the specified string to the buffer and the
	 *  output stream.
	 */
	public final void putString (final String str)
		throws IOException
	{
		buf.insert (str);
		printString (str);
		drawBuffer ();
	}


	/**
	 *  Output the specified string to the output stream (but not the
	 *  buffer).
	 */
	public final void printString (final String str)
		throws IOException
	{
		printCharacters (str.toCharArray ());
	}


	/**
	 *  Output the specified character, both to the buffer
	 *  and the output stream.
	 */
	private final void putChar (final int c, final boolean print)
		throws IOException
	{
		buf.insert ((char)c);

		if (print)
		{
			// no masking...
			if (mask == null)
			{
				printCharacter (c);
			}
			// null mask: don't print anything...
			else if (mask.charValue () == 0);
			// otherwise print the mask...
			else
			{
				printCharacter (mask.charValue ());
			}
			drawBuffer ();
		}
	}


	/**
	 *  Redraw the rest of the buffer from the cursor onwards. This
	 *  is necessary for inserting text into the buffer.
	 *
	 *  @param clear	the number of characters to clear after the
	 *  				end of the buffer
	 */
	private final void drawBuffer (final int clear)
		throws IOException
	{
		// debug ("drawBuffer: " + clear);

		char[] chars = buf.buffer.substring (buf.cursor).toCharArray ();
		printCharacters (chars);

		clearAhead (clear);
		back (chars.length);
		flushConsole ();
	}


	/**
	 *  Redraw the rest of the buffer from the cursor onwards. This
	 *  is necessary for inserting text into the buffer.
	 */
	private final void drawBuffer ()
		throws IOException
	{
		drawBuffer (0);
	}


	/**
	 *  Clear ahead the specified number of characters
	 *  without moving the cursor.
	 */
	private final void clearAhead (final int num)
		throws IOException
	{
		if (num == 0)
			return;

		// debug ("clearAhead: " + num);

		// print blank extra characters
		printCharacters (' ', num);

		// we need to flush here so a "clever" console
		// doesn't just ignore the redundancy of a space followed by
		// a backspace.
		flushConsole ();

		// reset the visual cursor
		back (num);

		flushConsole ();
	}


	/**
	 *  Move the visual cursor backwards without modifying the
	 *  buffer cursor.
	 */
	private final void back (final int num)
		throws IOException
	{
		printCharacters (BACKSPACE, num);
		flushConsole ();
	}


	/**
	 *  Issue an audible keyboard bell, if
	 *  {@link #getBellEnabled} return true.
	 */
	public final void beep ()
		throws IOException
	{
		if (!(getBellEnabled ()))
			return;

		printCharacter (KEYBOARD_BELL);
		// need to flush so the console actually beeps
		flushConsole ();
	}


	/**
	 *  Output the specified character to the output stream
	 *  without manipulating the current buffer.
	 */
	private final void printCharacter (final int c)
		throws IOException
	{
		out.write (c);
	}


	/**
	 *  Output the specified characters to the output stream
	 *  without manipulating the current buffer.
	 */
	private final void printCharacters (final char[] c)
		throws IOException
	{
		out.write (c);
	}


	private final void printCharacters (final char c, final int num)
		throws IOException
	{
		if (num == 1)
		{
			printCharacter (c);
		}
		else
		{
			char[] chars = new char[num];
			Arrays.fill (chars, c);
			printCharacters (chars);
		}
	}


	/**
	 *  Flush the console output stream. This is important for
	 *  printout out single characters (like a backspace or keyboard)
	 *  that we want the console to handle immedately.
	 */
	public final void flushConsole ()
		throws IOException
	{
		out.flush ();
	}


	private final int backspaceAll ()
		throws IOException
	{
		return backspace (Integer.MAX_VALUE);
	}


	/**
	 *  Issue <em>num</em> backspaces.
	 *
	 *  @return  the number of characters backed up
	 */
	private final int backspace (final int num)
		throws IOException
	{
		if (buf.cursor == 0)
			return 0;

		int count = 0;

		count = moveCursor (-1 * num) * -1;
		// debug ("Deleting from " + buf.cursor + " for " + count);

		buf.buffer.delete (buf.cursor, buf.cursor + count);
		drawBuffer (count);

		return count;
	}


	/**
	 *  Issue a backspace.
	 *
	 *  @return  true if successful
	 */
	public final boolean backspace ()
		throws IOException
	{
		return backspace (1) == 1;
	}


	private final boolean moveToEnd ()
		throws IOException
	{
		if (moveCursor (1) == 0)
			return false;

		while (moveCursor (1) != 0);

		return true;
	}


	/**
	 *  Delete the character at the current position and
	 *  redraw the remainder of the buffer.
	 */
	private final boolean deleteCurrentCharacter ()
		throws IOException
	{
		buf.buffer.deleteCharAt (buf.cursor);
		drawBuffer (1);
		return true;
	}


	private final boolean previousWord ()
		throws IOException
	{
		while (isDelimiter (buf.current ()) && moveCursor (-1) != 0);
		while (!isDelimiter (buf.current ()) && moveCursor (-1) != 0);

		return true;
	}


	private final boolean nextWord ()
		throws IOException
	{
		while (isDelimiter (buf.current ()) && moveCursor (1) != 0);
		while (!isDelimiter (buf.current ()) && moveCursor (1) != 0);

		return true;
	}


	private final boolean deletePreviousWord ()
		throws IOException
	{
		while (isDelimiter (buf.current ()) && backspace ());
		while (!isDelimiter (buf.current ()) && backspace ());

		return true;
	}


	/**
	 *  Move the cursor <i>where</i> characters.
	 *
	 *  @param  where  if less than 0, move abs(<i>where</i>) to the left,
	 *  				otherwise move <i>where</i> to the right.
	 *
	 *  @return  the number of spaces we moved
	 */
	private final int moveCursor (final int num)
		throws IOException
	{
		int where = num;
		if (buf.cursor == 0 && where < 0)
			return 0;

		if (buf.cursor == buf.buffer.length () && where > 0)
			return 0;

		if (buf.cursor + where < 0)
			where = -buf.cursor;
		else if (buf.cursor + where > buf.buffer.length ())
			where = buf.buffer.length () - buf.cursor;

		moveInternal (where);
		return where;
	}


	/**
	 *  debug.
	 *
	 *  @param  str  the message to issue.
	 */
	public static void debug (final String str)
	{
		if (debugger != null)
		{
			debugger.println (str);
			debugger.flush ();
		}
	}


	/**
	 *  Move the cursor <i>where</i> characters, withough checking
	 *  the current buffer.
	 *
	 *  @see	#where
	 *
	 *  @param  where  the number of characters to move to the right or left.
	 */
	private final void moveInternal (final int where)
		throws IOException
	{
		// debug ("move cursor " + where + " ("
			// + buf.cursor + " => " + (buf.cursor + where) + ")");

		buf.cursor += where;

		char c;

		if (where < 0)
		{
			c = BACKSPACE;
		}
		else if (buf.cursor == 0)
		{
			return;
		}
		else
		{
			c = buf.buffer.charAt (buf.cursor - 1); // draw replacement
		}

		// null character mask: don't output anything
		if (NULL_MASK.equals (mask))
			return;

		printCharacters (c, Math.abs (where));
	}


	/**
	 *  Read a character from the console.
	 *
	 *  @return  the character, or -1 if an EOF is received.
	 */
	public final int readVirtualKey ()
		throws IOException
	{
		int c = terminal.readVirtualKey (in);

		if (debugger != null)
			debug ("keystroke: " + c + "");

		// clear any echo characters
		clearEcho (c);

		return c;
	}


	public final int readCharacter (final char[] allowed)
		throws IOException
	{

		// if we restrict to a limited set and the current character
		// is not in the set, then try again.
		char c;

		Arrays.sort (allowed); // always need to sort before binarySearch
		while (Arrays.binarySearch (allowed,
			c = (char)readVirtualKey ()) == -1);

		return c;
	}


	public void setHistory (final History history)
	{
		this.history = history;
	}


	public History getHistory ()
	{
		return this.history;
	}


	public void setCompletionHandler (final CompletionHandler completionHandler)
	{
		this.completionHandler = completionHandler;
	}


	public CompletionHandler getCompletionHandler ()
	{
		return this.completionHandler;
	}



	/**
	 *	<p>
	 *  Set the echo character. For example, to have "*" entered
	 *  when a password is typed:
	 *  </p>
	 *
	 *	<pre>
	 *    myConsoleReader.setEchoCharacter (new Character ('*'));
	 *	</pre>
	 *
	 *	<p>
	 *	Setting the character to <pre>null</pre> will restore normal
	 *	character echoing. Setting the character to
	 *	<pre>new Character (0)</pre> will cause nothing to be echoed.
	 *	</p>
	 *
	 *  @param  echoCharacter	the character to echo to the console in
	 *  						place of the typed character.
	 */
	public void setEchoCharacter (final Character echoCharacter)
	{
		this.echoCharacter = echoCharacter;
	}


	/**
	 *  Returns the echo character.
	 */
	public Character getEchoCharacter ()
	{
		return this.echoCharacter;
	}


	/**
	 *  No-op for exceptions we want to silently consume.
	 */
	private void consumeException (final Throwable e)
	{
	}


	/**
	 *  Checks to see if the specified character is a delimiter. We
	 *  consider a character a delimiter if it is anything but a letter or
	 *  digit.
	 *
	 *  @param  c	the character to test
	 *  @return		true if it is a delimiter
	 */
	private boolean isDelimiter (char c)
	{
		return !Character.isLetterOrDigit (c);
	}
}

