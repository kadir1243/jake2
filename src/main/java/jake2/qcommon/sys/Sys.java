/*
 * Sys.java
 * Copyright (C) 2003
 * 
 * $Id: Sys.java,v 1.13 2011-07-07 21:09:05 salomo Exp $
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
package jake2.qcommon.sys;

import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.exec.Cmd;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Sys
 */
public final class Sys extends Defines {

    public static void Error(String error) {
        Cmd.ExecuteFunction("cl_shutdown");

        throw new AssertionError(error);
    }

    //ok!
    public static File[] FindAll(String path, int musthave, int canthave) {

        int index = path.lastIndexOf('/');

        if (index != -1) {
            findbase = path.substring(0, index);
            findpattern = path.substring(index + 1);
        } else {
            findbase = path;
            findpattern = "*";
        }

        if (findpattern.equals("*.*")) {
            findpattern = "*";
        }

        File fdir = new File(findbase);

        if (!fdir.exists())
            return null;

        FilenameFilter filter = new FileFilter(findpattern, musthave, canthave);

        return fdir.listFiles(filter);
    }

    /**
     * Match the pattern findpattern against the filename.
     * 
     * In the pattern string, `*' matches any sequence of characters, `?'
     * matches any character, [SET] matches any character in the specified set,
     * [!SET] matches any character not in the specified set. A set is composed
     * of characters or ranges; a range looks like character hyphen character
     * (as in 0-9 or A-Z). [0-9a-zA-Z_] is the set of characters allowed in C
     * identifiers. Any other character in the pattern must be matched exactly.
     * To suppress the special syntactic significance of any of `[]*?!-\', and
     * match the character exactly, precede it with a `\'.
     */
    static class FileFilter implements FilenameFilter {

        String regexpr;

        int musthave, canthave;

        FileFilter(String findpattern, int musthave, int canthave) {
            this.regexpr = convert2regexpr(findpattern);
            this.musthave = musthave;
            this.canthave = canthave;

        }

        /*
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            if (name.matches(regexpr)) {
                return CompareAttributes(dir, musthave, canthave);
            }
            return false;
        }

        String convert2regexpr(String pattern) {
            StringBuilder sb = new StringBuilder();

            char c;
            boolean escape = false;

            String subst;

            // convert pattern
            for (int i = 0; i < pattern.length(); i++) {
                c = pattern.charAt(i);
                subst = null;
                switch (c) {
                    case '*' -> subst = (!escape) ? ".*" : "*";
                    case '.' -> subst = (!escape) ? "\\." : ".";
                    case '!' -> subst = (!escape) ? "^" : "!";
                    case '?' -> subst = (!escape) ? "." : "?";
                    case '\\' -> escape = !escape;
                    default -> escape = false;
                }
                if (subst != null) {
                    sb.append(subst);
                    escape = false;
                } else
                    sb.append(c);
            }

            // the converted pattern
            String regexpr = sb.toString();

            //Com.DPrintf("pattern: " + pattern + " regexpr: " + regexpr +
            // '\n');
            try {
                Pattern.compile(regexpr);
            } catch (PatternSyntaxException e) {
                Com.Printf("invalid file pattern ( *.* is used instead )\n");
                return ".*"; // the default
            }
            return regexpr;
        }

        boolean CompareAttributes(File dir, int musthave, int canthave) {
            // . and .. never match
            String name = dir.getName();

            return !name.equals(".") && !name.equals("..");
        }

    }


    //============================================

    static File[] fdir;

    static int fileindex;

    static String findbase;

    static String findpattern;

    // ok.
    public static File FindFirst(String path, int musthave, int canthave) {

        if (fdir != null)
            Sys.Error("Sys_BeginFind without close");

        //	COM_FilePath (path, findbase);

        fdir = FindAll(path, canthave, musthave);
        fileindex = 0;

        if (fdir == null)
            return null;

        return FindNext();
    }

    public static File FindNext() {

        if (fileindex >= fdir.length)
            return null;

        return fdir[fileindex++];
    }

    public static void FindClose() {
        fdir = null;
    }

    // sfranzyshen -start
	// public static String GetClipboardData() {
	public static String GetClipboardData() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard()
		        .getContents(null);

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) t
				        .getTransferData(DataFlavor.stringFlavor);
			}
		} catch (UnsupportedFlavorException | IOException e) {
		}
        return null;
	}


}
