/*
 * CL_view.java Copyright (C) 2004
 * 
 * $Id: CL_view.java,v 1.6 2011-07-08 09:29:42 salomo Exp $
 */
/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
package jake2.client;

import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;
import jake2.qcommon.util.Lib;

import java.util.StringTokenizer;

@Environment(EnvType.CLIENT)
public class CL_view {

    static int num_cl_weaponmodels;

    static String[] cl_weaponmodels = new String[Defines.MAX_CLIENTWEAPONMODELS];

    /*
     * =================
     * 
     * CL_PrepRefresh
     * 
     * Call before entering a new level, or after changing dlls
     * =================
     */
    static void PrepRefresh() {
        String mapname;
        int i;
        String name;
        float rotate;
        float[] axis = new float[3];

        if ((i = ClientGlobals.cl.configstrings[Defines.CS_MODELS + 1].length()) == 0)
            return; // no map loaded

        SCR.AddDirtyPoint(0, 0);
        SCR.AddDirtyPoint(ClientGlobals.viddef.getWidth() - 1, ClientGlobals.viddef.getHeight() - 1);

        // let the render dll load the map
        mapname = ClientGlobals.cl.configstrings[Defines.CS_MODELS + 1].substring(5,
                i - 4); // skip "maps/"
        // cut off ".bsp"

        // register models, pics, and skins
        Com.Printf("Map: " + mapname + "\r");
        SCR.UpdateScreen();
        ClientGlobals.re.BeginRegistration(mapname);
        Com.Printf("                                     \r");

        // precache status bar pics
        Com.Printf("pics\r");
        SCR.UpdateScreen();
        SCR.TouchPics();
        Com.Printf("                                     \r");

        CL_tent.RegisterTEntModels();

        num_cl_weaponmodels = 1;
        cl_weaponmodels[0] = "weapon.md2";

        for (i = 1; i < Defines.MAX_MODELS
                && !ClientGlobals.cl.configstrings[Defines.CS_MODELS + i].isEmpty(); i++) {
            name = ClientGlobals.cl.configstrings[Defines.CS_MODELS + i];
            if (name.length() > 37)
                name = name.substring(0, 36);

            if (name.charAt(0) != '*')
                Com.Printf(name + "\r");

            SCR.UpdateScreen();
            CL.SendKeyEvents(); // pump message loop
            if (name.charAt(0) == '#') {
                // special player weapon model
                if (num_cl_weaponmodels < Defines.MAX_CLIENTWEAPONMODELS) {
                    cl_weaponmodels[num_cl_weaponmodels] = ClientGlobals.cl.configstrings[Defines.CS_MODELS
                            + i].substring(1);
                    num_cl_weaponmodels++;
                }
            } else {
                ClientGlobals.cl.model_draw[i] = ClientGlobals.re
                        .RegisterModel(ClientGlobals.cl.configstrings[Defines.CS_MODELS
                                + i]);
                if (name.charAt(0) == '*')
                    ClientGlobals.cl.model_clip[i] = ClientGlobals.cm.InlineModel(ClientGlobals.cl.configstrings[Defines.CS_MODELS
                                    + i]);
                else
                    ClientGlobals.cl.model_clip[i] = null;
            }
            if (name.charAt(0) != '*')
                Com.Printf("                                     \r");
        }

        Com.Printf("images\r");
        SCR.UpdateScreen();
        for (i = 1; i < Defines.MAX_IMAGES
                && !ClientGlobals.cl.configstrings[Defines.CS_IMAGES + i].isEmpty(); i++) {
            ClientGlobals.cl.image_precache[i] = ClientGlobals.re
                    .RegisterPic(ClientGlobals.cl.configstrings[Defines.CS_IMAGES + i]);
            CL.SendKeyEvents(); // pump message loop
        }

        Com.Printf("                                     \r");
        for (i = 0; i < Defines.MAX_CLIENTS; i++) {
            if (ClientGlobals.cl.configstrings[Defines.CS_PLAYERSKINS + i].isEmpty())
                continue;
            Com.Printf("client " + i + '\r');
            SCR.UpdateScreen();
            CL.SendKeyEvents(); // pump message loop
            CL_parse.ParseClientinfo(i);
            Com.Printf("                                     \r");
        }

        CL_parse.LoadClientinfo(ClientGlobals.cl.baseclientinfo,
                "unnamed\\male/grunt");

        // set sky textures and speed
        Com.Printf("sky\r");
        SCR.UpdateScreen();
        rotate = Float
                .parseFloat(ClientGlobals.cl.configstrings[Defines.CS_SKYROTATE]);
        StringTokenizer st = new StringTokenizer(
                ClientGlobals.cl.configstrings[Defines.CS_SKYAXIS]);
        axis[0] = Float.parseFloat(st.nextToken());
        axis[1] = Float.parseFloat(st.nextToken());
        axis[2] = Float.parseFloat(st.nextToken());
        ClientGlobals.re.SetSky(ClientGlobals.cl.configstrings[Defines.CS_SKY], rotate,
                axis);
        Com.Printf("                                     \r");

        // the renderer can now free unneeded stuff
        ClientGlobals.re.EndRegistration();

        // clear any lines of console text
        Console.ClearNotify();

        SCR.UpdateScreen();
        ClientGlobals.cl.refresh_prepped = true;
        ClientGlobals.cl.force_refdef = true; // make sure we have a valid refdef
        
        // start the cd track
        CDAudio.Play(Lib.atoi(ClientGlobals.cl.configstrings[Defines.CS_CDTRACK]), true);

    }

    public static void AddNetgraph() {
        int i;
        int in;
        int ping;

        // if using the debuggraph for something else, don't
        // add the net lines
        if (SCR.scr_debuggraph.value == 0.0f || SCR.scr_timegraph.value == 0.0f)
            return;

        for (i = 0; i < ClientGlobals.cls.netchan.dropped; i++)
            SCR.DebugGraph(30, 0x40);

        for (i = 0; i < ClientGlobals.cl.surpressCount; i++)
            SCR.DebugGraph(30, 0xdf);

        // see what the latency was on this packet
        in = ClientGlobals.cls.netchan.incoming_acknowledged
                & (Defines.CMD_BACKUP - 1);
        ping = ClientGlobals.cls.realtime - ClientGlobals.cl.cmd_time[in];
        ping /= 30;
        if (ping > 30)
            ping = 30;
        SCR.DebugGraph(ping, 0xd0);
    }
}