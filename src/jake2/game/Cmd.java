/*
 * Cmd.java
 * Copyright (C) 2003
 * 
 * $Id: Cmd.java,v 1.8 2003-12-01 13:25:57 hoz Exp $
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
package jake2.game;

import jake2.Globals;
import jake2.qcommon.*;

/**
 * Cmd
 */
public final class Cmd {
	
	static xcommand_t List_f = new xcommand_t() {
		public void execute() {
			cmd_function_t  cmd = Cmd.cmd_functions;
			int i = 0;

			while (cmd != null) {
				Com.print(cmd.name + '\n');
				i++;
				cmd = cmd.next;
			}
			Com.print(i + " commands\n");
		}
	};
	
	static xcommand_t Exec_f = new xcommand_t() {
		public void execute() {
			if (Cmd.Argc() != 2) {
				Com.print("exec <filename> : execute a script file\n");
				return;
			}

			byte[] f = null;
			int len = FS.LoadFile(Cmd.Argv(1), f);
			if (f == null) {
				Com.print("couldn't exec " + Cmd.Argv(1) + "\n");
				return;
			}
			Com.print("execing " + Cmd.Argv(1) + "\n");

			byte[] f2 = Z.Malloc(len);
			System.arraycopy(f, 0, f2, 0, f.length);
			
			Cbuf.InsertText(new String(f2));
			
			Z.Free(f2);
			FS.FreeFile(f);
		}
	};
	static xcommand_t Echo_f = new xcommand_t() {
		public void execute() {
			for (int i  = 1;  i < Cmd.Argc(); i++) {
				Com.print(Cmd.Argv(i) + " ");
			}
			Com.print("'\n");
		}
	};
	
	static xcommand_t Alias_f = new xcommand_t() {
		public void execute() {
			cmdalias_t a = null;
			if (Cmd.Argc() == 1) {
				Com.print("Current alias commands:\n");
				for (a = Globals.cmd_alias; a != null; a = a.next) {
					Com.print(a.name + " : " + a.value);
				}
				return;
			}

			String s = Cmd.Argv(1);
			if (s.length() > Globals.MAX_ALIAS_NAME) {
				Com.print("Alias name is too long\n");
				return;
			}

			// if the alias already exists, reuse it
			for (a = Globals.cmd_alias; a != null; a = a.next) {
				if (s.equalsIgnoreCase(a.name)) {
					a.value = null;
					break;
				}
			}
			
			if (a == null) {
				a = new cmdalias_t();
				a.next = Globals.cmd_alias;
				Globals.cmd_alias = a;
			}
			a.name = s;
			
			// copy the rest of the command line
			String cmd = "";
			int c = Cmd.Argc();
			for (int i = 2; i < c; i++) {
				cmd = cmd + Cmd.Argv(i);
				if (i != (c-1)) cmd = cmd + " ";
			}
			cmd = cmd + "\n";
			
			a.value = cmd;
		}
	};
	static xcommand_t Wait_f = new xcommand_t() {
		public void execute() {
			Globals.cmd_wait = true;
		}
	};
	
	public static cmd_function_t cmd_functions = null;
	static int cmd_argc;
	static String[] cmd_argv = new String[Globals.MAX_STRING_TOKENS];

	/**
	 * register our commands
	 */
	public static void Init() {
    	Cmd.AddCommand ("cmdlist", List_f);
		Cmd.AddCommand ("exec", Exec_f);
		Cmd.AddCommand ("echo", Echo_f);
		Cmd.AddCommand ("alias",Alias_f);
		Cmd.AddCommand ("wait", Wait_f);
	}

	/**
	 * @param cmdname
	 * @param function
	 * TODO implement Cmd.AddCommand()
	 */
	public static void AddCommand(String cmdname, xcommand_t function) { 	
	}
	
	/**
	 * @return number of command arguments
	 */
	public static int Argc() {
		return cmd_argc;	
	}
	
	/**
	 * @param i index
	 * @return command argument at position i
	 */
	public static String Argv(int i) {
		if (i < 0 || i >= cmd_argc) return "";
		return cmd_argv[i];
	}

	/**
	 * 
	 */
	public static String Args() {
		// TODO Auto-generated method stub
		return "";
		
	}
}
