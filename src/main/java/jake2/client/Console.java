/*
 * Con.java
 * Copyright (C) 2003
 *
 * $Id: Console.java,v 1.10 2011-07-07 21:10:18 salomo Exp $
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
package jake2.client;

import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.Globals;
import jake2.qcommon.ServerStates;
import jake2.qcommon.exec.Cbuf;
import jake2.qcommon.exec.Cmd;
import jake2.qcommon.exec.Command;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.filesystem.FS;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;
import jake2.qcommon.util.Lib;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

/**
 * Console
 */
// TODO move to client
@Environment(EnvType.CLIENT)
public final class Console extends Globals {

    private static Command Clear_f = (List<String> args) -> Arrays.fill(ClientGlobals.con.text, (byte) ' ');    static Command ToggleConsole_f = (List<String> args) -> {
        SCR.EndLoadingPlaque(); // get rid of loading plaque

        if (ClientGlobals.cl.attractloop) {
            Cbuf.AddText("killserver\n");
            return;
        }

        if (ClientGlobals.cls.state == Defines.ca_disconnected) {
            // start the demo loop again
            // todo: intro
            //Cbuf.AddText("d1\n");
            return;
        }

        Key.ClearTyping();
        Console.ClearNotify();

        if (ClientGlobals.cls.key_dest == Defines.key_console) {
            Menu.ForceMenuOff();
            Cvar.getInstance().Set("paused", "0");
        } else {
            Menu.ForceMenuOff();
            ClientGlobals.cls.key_dest = Defines.key_console;

            if (Cvar.getInstance().VariableValue("maxclients") == 1
                    && Globals.server_state != ServerStates.SS_DEAD)
                Cvar.getInstance().Set("paused", "1");
        }
    };
    /**
     * Dump console contents to file (file name as a 1st argument)
     */
    private static Command Dump_f = (List<String> args) -> {

        int l, x;
        int line;
        RandomAccessFile f;
        byte[] buffer = new byte[1024];
        String name;

        if (args.size() != 2) {
            Com.Printf("usage: condump <filename>\n");
            return;
        }

        // Com_sprintf (name, sizeof(name), "%s/%s.txt", FS_Gamedir(),
        // Cmd_Argv(1));
        name = FS.getWriteDir() + "/" + args.get(1) + ".txt";

        Com.Printf("Dumped console text to " + name + ".\n");
        FS.CreatePath(name);
        f = Lib.fopen(name, "rw");
        if (f == null) {
            Com.Printf("ERROR: couldn't open.\n");
            return;
        }

        // skip empty lines
        for (l = ClientGlobals.con.current - ClientGlobals.con.totallines + 1; l <= ClientGlobals.con.current; l++) {
            line = (l % ClientGlobals.con.totallines) * ClientGlobals.con.linewidth;
            for (x = 0; x < ClientGlobals.con.linewidth; x++)
                if (ClientGlobals.con.text[line + x] != ' ')
                    break;
            if (x != ClientGlobals.con.linewidth)
                break;
        }

        // write the remaining lines
        buffer[ClientGlobals.con.linewidth] = 0;
        for (; l <= ClientGlobals.con.current; l++) {
            line = (l % ClientGlobals.con.totallines) * ClientGlobals.con.linewidth;
            // strncpy (buffer, line, con.linewidth);
            System.arraycopy(ClientGlobals.con.text, line, buffer, 0, ClientGlobals.con.linewidth);
            for (x = ClientGlobals.con.linewidth - 1; x >= 0; x--) {
                if (buffer[x] == ' ')
                    buffer[x] = 0;
                else
                    break;
            }
            for (x = 0; buffer[x] != 0; x++)
                buffer[x] &= 0x7f;

            buffer[x] = '\n';
            // fprintf (f, "%s\n", buffer);
            try {
                f.write(buffer, 0, x + 1);
            } catch (IOException e) {
            }
        }

        Lib.fclose(f);

    };
    /*
     * ================ Con_MessageMode_f ================
     */
    private static Command MessageMode_f = (List<String> args) -> {
        ClientGlobals.chat_team = false;
        ClientGlobals.cls.key_dest = key_message;
    };
    /*
     * ================ Con_MessageMode2_f ================
     */
    private static Command MessageMode2_f = (List<String> args) -> {
        ClientGlobals.chat_team = true;
        ClientGlobals.cls.key_dest = key_message;
    };    /*
     * ================ Con_ToggleChat_f ================
     */
    private static Command ToggleChat_f = (List<String> args) -> {
        Key.ClearTyping();

        if (ClientGlobals.cls.key_dest == key_console) {
            if (ClientGlobals.cls.state == ca_active) {
                Menu.ForceMenuOff();
                ClientGlobals.cls.key_dest = key_game;
            }
        } else
            ClientGlobals.cls.key_dest = key_console;

        ClearNotify();
    };
    /*
     * ================ Con_Print
     *
     * Handles cursor positioning, line wrapping, etc All console printing must
     * go through this in order to be logged to disk If no console is visible,
     * the text will appear at the top of the game window ================
     */
    private static int cr;

    public static void Init() {
        ClientGlobals.con.linewidth = -1;
        ClientGlobals.con.backedit = 0;

        CheckResize();

        Com.Printf("Console initialized.\n");

        //
        // register our commands
        //
        ClientGlobals.con_notifytime = Cvar.getInstance().Get("con_notifytime", "3", 0);

        Cmd.AddCommand("toggleconsole", ToggleConsole_f);
        Cmd.AddCommand("togglechat", ToggleChat_f);
        Cmd.AddCommand("messagemode", MessageMode_f);
        Cmd.AddCommand("messagemode2", MessageMode2_f);
        Cmd.AddCommand("clear", Clear_f);
        Cmd.AddCommand("condump", Dump_f);
        Cmd.AddCommand("console_print", args -> Console.Print(args.get(0)));

        ClientGlobals.con.initialized = true;
    }

    /**
     * If the line width has changed, reformat the buffer.
     */
    static void CheckResize() {

        int width = (ClientGlobals.viddef.getWidth() >> 3) - 2;
        if (width > Defines.MAXCMDLINE)
            width = Defines.MAXCMDLINE;

        if (width == ClientGlobals.con.linewidth)
            return;

        if (width < 1) { // video hasn't been initialized yet
            width = 38;
            ClientGlobals.con.linewidth = width;
            ClientGlobals.con.backedit = 0; // sfranzyshen
            ClientGlobals.con.totallines = Defines.CON_TEXTSIZE
                    / ClientGlobals.con.linewidth;
            Arrays.fill(ClientGlobals.con.text, (byte) ' ');
        } else {
            int oldwidth = ClientGlobals.con.linewidth;
            ClientGlobals.con.linewidth = width;
            ClientGlobals.con.backedit = 0; // sfranzyshen
            int oldtotallines = ClientGlobals.con.totallines;
            ClientGlobals.con.totallines = Defines.CON_TEXTSIZE
                    / ClientGlobals.con.linewidth;
            int numlines = oldtotallines;

            if (ClientGlobals.con.totallines < numlines)
                numlines = ClientGlobals.con.totallines;

            int numchars = oldwidth;

            if (ClientGlobals.con.linewidth < numchars)
                numchars = ClientGlobals.con.linewidth;

            byte[] tbuf = new byte[Defines.CON_TEXTSIZE];
            System
                    .arraycopy(ClientGlobals.con.text, 0, tbuf, 0,
                            Defines.CON_TEXTSIZE);
            Arrays.fill(ClientGlobals.con.text, (byte) ' ');

            for (int i = 0; i < numlines; i++) {
                for (int j = 0; j < numchars; j++) {
                    ClientGlobals.con.text[(ClientGlobals.con.totallines - 1 - i)
                            * ClientGlobals.con.linewidth + j] = tbuf[((ClientGlobals.con.current
                            - i + oldtotallines) % oldtotallines)
                            * oldwidth + j];
                }
            }

            Console.ClearNotify();
        }

        ClientGlobals.con.current = ClientGlobals.con.totallines - 1;
        ClientGlobals.con.display = ClientGlobals.con.current;
    }

    static void ClearNotify() {
        int i;
        for (i = 0; i < Defines.NUM_CON_TIMES; i++)
            ClientGlobals.con.times[i] = 0;
    }

    static void DrawString(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            ClientGlobals.re.DrawChar(x, y, s.charAt(i));
            x += 8;
        }
    }

    static void DrawAltString(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            ClientGlobals.re.DrawChar(x, y, s.charAt(i) ^ 0x80);
            x += 8;
        }
    }

    /*
     * =============== Con_Linefeed ===============
     */
    private static void Linefeed() {
        ClientGlobals.con.x = 0;
        if (ClientGlobals.con.display == ClientGlobals.con.current)
            ClientGlobals.con.display++;
        ClientGlobals.con.current++;
        int i = (ClientGlobals.con.current % ClientGlobals.con.totallines)
                * ClientGlobals.con.linewidth;
        int e = i + ClientGlobals.con.linewidth;
        while (i < ClientGlobals.con.text.length - 1 && i++ < e)
            ClientGlobals.con.text[i] = (byte) ' ';
    }

    public static void Print(String txt) {
        int y;
        int c, l;
        int mask;
        int txtpos = 0;

        if (!ClientGlobals.con.initialized)
            return;

        if (txt.charAt(0) == 1 || txt.charAt(0) == 2) {
            mask = 128; // go to colored text
            txtpos++;
        } else
            mask = 0;

        while (txtpos < txt.length()) {
            c = txt.charAt(txtpos);
            // count word length
            for (l = 0; l < ClientGlobals.con.linewidth && l < (txt.length() - txtpos); l++)
                if (txt.charAt(l + txtpos) <= ' ')
                    break;

            // word wrap
            if (l != ClientGlobals.con.linewidth && (ClientGlobals.con.x + l > ClientGlobals.con.linewidth))
                ClientGlobals.con.x = 0;

            txtpos++;

            if (cr != 0) {
                ClientGlobals.con.current--;
                cr = 0;
            }

            if (ClientGlobals.con.x == 0) {
                Console.Linefeed();
                // mark time for transparent overlay
                if (ClientGlobals.con.current >= 0)
                    ClientGlobals.con.times[ClientGlobals.con.current % NUM_CON_TIMES] = ClientGlobals.cls.realtime;
            }

            switch (c) {
                case '\n':
                    ClientGlobals.con.x = 0;
                    break;

                case '\r':
                    ClientGlobals.con.x = 0;
                    cr = 1;
                    break;

                default: // display character and advance
                    y = ClientGlobals.con.current % ClientGlobals.con.totallines;
                    ClientGlobals.con.text[y * ClientGlobals.con.linewidth + ClientGlobals.con.x] = (byte) (c | mask | ClientGlobals.con.ormask);
                    ClientGlobals.con.x++;
                    if (ClientGlobals.con.x >= ClientGlobals.con.linewidth)
                        ClientGlobals.con.x = 0;
                    break;
            }
        }
    }

    /*
     * ================ Con_DrawInput
     *
     * The input line scrolls horizontally if typing goes beyond the right edge
     * ================
     */
    private static void DrawInput() {
        int i;
        byte[] text;
        int start = 0;

        if (ClientGlobals.cls.key_dest == key_menu)
            return;

        if (ClientGlobals.cls.key_dest != key_console && ClientGlobals.cls.state == ca_active)
            return; // don't draw anything (always draw if not active)

        text = ClientGlobals.key_lines[ClientGlobals.edit_line];

        // add the cursor frame
        //text[key_linepos] = (byte) (10 + ((int) (cls.realtime >> 8) & 1)); //sfranzyshen

        // fill out remainder with spaces
        for (i = ClientGlobals.key_linepos; i < ClientGlobals.con.linewidth; i++) // sfranzyshen
            text[i] = ' ';

        // prestep if horizontally scrolling
        if (ClientGlobals.key_linepos >= ClientGlobals.con.linewidth)
            start += 1 + ClientGlobals.key_linepos - ClientGlobals.con.linewidth;

        // draw it
        // y = con.vislines-16;

        // sfranzyshen --start
        for (i = 0; i < ClientGlobals.con.linewidth; i++) {
            //old:re.DrawChar((i + 1) << 3, con.vislines - 22, text[i]);
            if (ClientGlobals.con.backedit == ClientGlobals.key_linepos - i && (((int) (ClientGlobals.cls.realtime >> 8) & 1) != 0))
                ClientGlobals.re.DrawChar((i + 1) << 3, ClientGlobals.con.vislines - 22, (char) 11);
            else
                ClientGlobals.re.DrawChar((i + 1) << 3, ClientGlobals.con.vislines - 22, text[i]);
        }
        // sfranzyshen - stop


        // remove cursor
        ClientGlobals.key_lines[ClientGlobals.edit_line][ClientGlobals.key_linepos] = 0;
    }

    /*
     * ================ Con_DrawNotify
     *
     * Draws the last few lines of output transparently over the game top
     * ================
     */
    static void DrawNotify() {
        int x, v;
        int text;
        int i;
        int time;
        String s;
        int skip;

        v = 0;
        for (i = ClientGlobals.con.current - NUM_CON_TIMES + 1; i <= ClientGlobals.con.current; i++) {
            if (i < 0)
                continue;

            time = (int) ClientGlobals.con.times[i % NUM_CON_TIMES];
            if (time == 0)
                continue;

            time = (int) (ClientGlobals.cls.realtime - time);
            if (time > ClientGlobals.con_notifytime.value * 1000)
                continue;

            text = (i % ClientGlobals.con.totallines) * ClientGlobals.con.linewidth;

            for (x = 0; x < ClientGlobals.con.linewidth; x++)
                ClientGlobals.re.DrawChar((x + 1) << 3, v, ClientGlobals.con.text[text + x]);

            v += 8;
        }

        if (ClientGlobals.cls.key_dest == key_message) {
            if (ClientGlobals.chat_team) {
                DrawString(8, v, "say_team:");
                skip = 11;
            } else {
                DrawString(8, v, "say:");
                skip = 5;
            }

            s = ClientGlobals.chat_buffer;
            if (ClientGlobals.chat_bufferlen > (ClientGlobals.viddef.getWidth() >> 3) - (skip + 1))
                //s = s.substring(chat_bufferlen
                //	- ((viddef.getWidth() >> 3) - (skip + 1)));

                // sfranzyshen -start
                s = s.substring(ClientGlobals.chat_bufferlen - ((ClientGlobals.viddef.getWidth() >> 3) - (skip + 1)));

            for (x = 0; x < s.length(); x++) {
                if (ClientGlobals.chat_backedit > 0 && ClientGlobals.chat_backedit == ClientGlobals.chat_buffer.length() - x && ((int) (ClientGlobals.cls.realtime >> 8) & 1) != 0) {
                    ClientGlobals.re.DrawChar((x + skip) << 3, v, (char) 11);
                } else {
                    ClientGlobals.re.DrawChar((x + skip) << 3, v, s.charAt(x));
                }
            }

            if (ClientGlobals.chat_backedit == 0)
                ClientGlobals.re.DrawChar((x + skip) << 3, v, (int) (10 + ((ClientGlobals.cls.realtime >> 8) & 1)));
            // sfranzyshen -stop

            v += 8;
        }

        if (v != 0) {
            SCR.AddDirtyPoint(0, 0);
            SCR.AddDirtyPoint(ClientGlobals.viddef.getWidth() - 1, v);
        }
    }

    /**
     * Draws the console with the solid background
     *
     * @param frac - percentage of screen to occupy by console, (from 0 to 1)
     */
    static void DrawConsole(float frac) {

        int width = ClientGlobals.viddef.getWidth();
        int height = ClientGlobals.viddef.getHeight();
        int lines = (int) (height * frac);
        if (lines <= 0)
            return;

        if (lines > height)
            lines = height;

        // draw the background
        ClientGlobals.re.DrawStretchPic(0, -height + lines, width, height, "conback");
        SCR.AddDirtyPoint(0, 0);
        SCR.AddDirtyPoint(width - 1, lines - 1);

        String version = String.format("v%4.2f", VERSION);
        for (int x = 0; x < 5; x++)
            ClientGlobals.re
                    .DrawChar(width - 44 + x * 8, lines - 12, 128 + version
                            .charAt(x));

        // draw the text
        ClientGlobals.con.vislines = lines;

        int rows = (lines - 22) >> 3; // rows of text to draw

        int y = lines - 30;

        // draw from the bottom up
        if (ClientGlobals.con.display != ClientGlobals.con.current) {
            // draw arrows to show the buffer is backscrolled
            for (int x = 0; x < ClientGlobals.con.linewidth; x += 4)
                ClientGlobals.re.DrawChar((x + 1) << 3, y, '^');

            y -= 8;
            rows--;
        }

        int i, j, x, n;

        int row = ClientGlobals.con.display;
        for (i = 0; i < rows; i++, y -= 8, row--) {
            if (row < 0)
                break;
            if (ClientGlobals.con.current - row >= ClientGlobals.con.totallines)
                break; // past scrollback wrap point

            int first = (row % ClientGlobals.con.totallines) * ClientGlobals.con.linewidth;

            for (x = 0; x < ClientGlobals.con.linewidth; x++)
                ClientGlobals.re.DrawChar((x + 1) << 3, y, ClientGlobals.con.text[x + first]);
        }

        // ZOID
        // draw the download bar
        // figure out width
        if (ClientGlobals.cls.download != null) {
            int text;
            if ((text = ClientGlobals.cls.downloadname.lastIndexOf('/')) != 0)
                text++;
            else
                text = 0;

            x = ClientGlobals.con.linewidth - ((ClientGlobals.con.linewidth * 7) / 40);
            y = x - (ClientGlobals.cls.downloadname.length() - text) - 8;
            i = ClientGlobals.con.linewidth / 3;
            StringBuffer dlbar = new StringBuffer(512);
            if (ClientGlobals.cls.downloadname.length() - text > i) {
                y = x - i - 11;
                int end = text + i - 1;
                ;
                dlbar.append(ClientGlobals.cls.downloadname.substring(text, end));
                dlbar.append("...");
            } else {
                dlbar.append(ClientGlobals.cls.downloadname.substring(text));
            }
            dlbar.append(": ");
            dlbar.append((char) 0x80);

            // where's the dot go?
            if (ClientGlobals.cls.downloadpercent == 0)
                n = 0;
            else
                n = y * ClientGlobals.cls.downloadpercent / 100;

            for (j = 0; j < y; j++) {
                if (j == n)
                    dlbar.append((char) 0x83);
                else
                    dlbar.append((char) 0x81);
            }
            dlbar.append((char) 0x82);
            dlbar.append((ClientGlobals.cls.downloadpercent < 10) ? " 0" : " ");
            dlbar.append(ClientGlobals.cls.downloadpercent).append('%');
            // draw it
            y = ClientGlobals.con.vislines - 12;
            for (i = 0; i < dlbar.length(); i++)
                ClientGlobals.re.DrawChar((i + 1) << 3, y, dlbar.charAt(i));
        }
        // ZOID

        // draw the input prompt, user text, and cursor if desired
        DrawInput();
    }




}