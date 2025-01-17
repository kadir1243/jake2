/*
 * Com.java
 * Copyright (C) 2003
 * 
 * $Id: Com.java,v 1.14 2005-12-16 21:17:08 salomo Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.qcommon;

import jake2.qcommon.exec.Cmd;
import jake2.qcommon.sys.Sys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common print related functions including redirection
 *
 */
public final class Com {
	private static final Logger LOGGER = LoggerFactory.getLogger(Com.class);
	public static void Printf(int print_level, String fmt) {
		if (print_level == Defines.PRINT_ALL)
			Printf(fmt);
		else
			DPrintf(fmt);
	}

	public static void print(int print_level, String fmt, Object... vargs) {
		if (print_level == Defines.PRINT_ALL)
			print(fmt, vargs);
		else
			Dprint(fmt, vargs);
	}

	public interface RD_Flusher {
		void rd_flush(StringBuilder buffer);
	}

	private static final int rd_buffersize = Defines.SV_OUTPUTBUF_LENGTH;
	private static final StringBuilder rd_buffer = new StringBuilder();
	private static RD_Flusher rd_flusher;

	public static void BeginRedirect(RD_Flusher flush) {
		if (flush == null)
			return;

		rd_buffer.setLength(0);
		rd_flusher = flush;
	}

	public static void EndRedirect() {
		rd_flusher.rd_flush(rd_buffer);
		rd_buffer.setLength(0);
		rd_flusher = null;
	}

	// Detect recursion during shutdown
	private static boolean recursive= false;

	// helper class to replace the pointer-pointer
	@Deprecated
	public static class ParseHelp
	{
		public ParseHelp(String in)
		{
			if (in == null)
			{
				data= null;
				length = 0;
			}
			else
			{
				data= in.toCharArray();
				length = data.length;
			}
			index= 0;
		}

		public ParseHelp(String in, int offset)
		{
			data = in.toCharArray();
			index = offset;
			length = data.length;
		}

		char getchar()
		{
		    if (index < length) {
		        return data[index];
		    }
		    return 0;			
		}

		public char nextchar()
		{
			// faster than if
		    index++;
		    if (index < length) {
		        return data[index];
		    }
		    return 0;
		}
		
		char prevchar() {
			if (index > 0) 
			{
				index--;
				return data[index];
			}
			return 0;					
		}

		public boolean isEof()
		{
			return index >= length;
		}

		public int index;
		public char[] data;
		private int length;

		char skipwhites()
		{
			char c = 0;
			while ( index < length && ((c= data[index]) <= ' ') && c != 0)
				index++;
			return c;
		}

		public char skipwhitestoeol()
		{
			char c = 0;
			while ( index < length &&((c= data[index]) <= ' ') && c != '\n' && c != 0)
				index++;
			return c;
		}

		char skiptoeol()
		{
			char c = 0;
			while ( index < length &&(c= data[index]) != '\n' && c != 0)
				index++;
			return c;
		}
	}

	private static char com_token[]= new char[Defines.MAX_TOKEN_CHARS];

	// See GameSpanw.ED_ParseEdict() to see how to use it now.
	@Deprecated
	public static String Parse(ParseHelp hlp) {
		int c;
		int len = 0;

		if (hlp.data == null) {
			return "";
		}

		while (true) {
			//	   skip whitespace
			hlp.skipwhites();
			if (hlp.isEof()) {
			    hlp.data = null;
			    return "";
			}

			//	   skip // comments
			if (hlp.getchar() == '/') {
				if (hlp.nextchar() == '/') {
					hlp.skiptoeol();
					// goto skip whitespace
					continue;
				} else {
					hlp.prevchar();
					break;
				}
			} else
				break;
		}

		//	   handle quoted strings specially
		if (hlp.getchar() == '\"') {
			hlp.nextchar();
			while (true) {
				c = hlp.getchar();
				hlp.nextchar();
				if (c == '\"' || c == 0) {
					return new String(com_token, 0, len);
				}
				if (len < Defines.MAX_TOKEN_CHARS) {
					com_token[len] = (char) c;
					len++;
				}
			}
		}

		//	   parse a regular word
		c = hlp.getchar();
		do {
			if (len < Defines.MAX_TOKEN_CHARS) {
				com_token[len] = (char) c;
				len++;
			}
			c = hlp.nextchar();
		} while (c > 32);

		if (len == Defines.MAX_TOKEN_CHARS) {
			Com.Printf("Token exceeded " + Defines.MAX_TOKEN_CHARS + " chars, discarded.\n");
			len = 0;
		}

		return new String(com_token, 0, len);
	}

	private static String lastError;

	public static void Error(int code, String fmt) throws longjmpException {
		if (recursive) {
			Sys.Error("recursive error after: " + lastError);
		}
		recursive = true;

        lastError = fmt;

		if (code == Defines.ERR_DISCONNECT) {
			Cmd.ExecuteFunction("cl_drop");
			recursive= false;
			throw new longjmpException();
		} else if (code == Defines.ERR_DROP) {
			Com.Printf("********************\nERROR: " + fmt + "\n********************\n");
			Cmd.ExecuteFunction("sv_shutdown", "Server crashed: " + fmt, "false");
			Cmd.ExecuteFunction("cl_drop");
			recursive= false;
			throw new longjmpException();
		} else {
			Cmd.ExecuteFunction("sv_shutdown", "Server fatal crashed: " + fmt, "false");
			Cmd.ExecuteFunction("cl_shutdown");
		}

		Sys.Error(fmt);
	}

	public static void DPrintf(String fmt)
	{
		if (Globals.developer == null || Globals.developer.value == 0)
			return; // don't confuse non-developers with techie stuff...
		Printf(fmt);
	}
	
	public static void dprintln(String fmt)
	{
		if (Globals.developer == null || Globals.developer.value == 0)
			return; // don't confuse non-developers with techie stuff...
		Printf(fmt + "\n");
	}

	public static void Printf(String fmt) {
        if (rd_flusher != null) {
			if ((fmt.length() + rd_buffer.length()) > (rd_buffersize - 1)) {
				rd_flusher.rd_flush(rd_buffer);
				rd_buffer.setLength(0);
			}
			rd_buffer.append(fmt);
			return;
		}

		Cmd.ExecuteFunction("console_print", fmt);

		// also echo to debugging console
		// todo: use proper logging
		LOGGER.info(fmt);
	}

	public static void Dprint(String fmt, Object... vargs) {
		if (Globals.developer == null || Globals.developer.value == 0)
			return; // don't confuse non-developers with techie stuff...
		print(fmt, vargs);
	}

	public static void print(String fmt, Object... vargs) {
		String msg= String.format(fmt, vargs);
		if (rd_flusher != null) {
			if ((msg.length() + rd_buffer.length()) > (rd_buffersize - 1)) {
				rd_flusher.rd_flush(rd_buffer);
				rd_buffer.setLength(0);
			}
			rd_buffer.append(msg);
			return;
		}

		Cmd.ExecuteFunction("console_print", msg);

		// also echo to debugging console
		// todo: use proper logging
		LOGGER.info(msg);
	}

	public static void Println(String fmt)
	{
		Printf(fmt + "\n");
	}

	public static String StripExtension(String string) {
		int i = string.lastIndexOf('.');
		if (i < 0)
			return string;
		return string.substring(0, i);
	}
}
