/*
 * DancingQueens.java
 * Copyright (C) 2003
 *
 * $Id: DancingQueens.java,v 1.13 2008-03-02 14:56:21 cawe Exp $
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
package jake2.fullgame;

import jake2.client.*;
import jake2.client.render.Renderer;
import jake2.qcommon.Defines;
import jake2.qcommon.Globals;
import jake2.qcommon.exec.Cbuf;
import jake2.qcommon.exec.Cmd;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;
import jake2.qcommon.util.Math3D;

import java.util.List;

/**
 * DancingQueens
 * 
 * @author cwei
 */
@Environment(EnvType.CLIENT)
public class DancingQueens {
    String[] args;
    
    refexport_t re;
    
    viddef_t viddef;
    
    int framecount = 0;
    
    public DancingQueens(String[] args) {
        this.args = args;
    }
    
    public static void main(String[] args) {
        
        DancingQueens test = new DancingQueens(args);
        test.init();
        test.run();
    }
    
    KBD kbd;
    
    void init() {
    	Globals.dedicated = Cvar.getInstance().Get("dedicated", "0", Defines.CVAR_NOSET );

	String DRIVER = "lwjgl";
	
        Jake2.Init(new String[] { "DancingQueens", "+set", "gl_mode", "4",
                "+set", "vid_fullscreen", "0", "+set", "vid_ref", DRIVER });
        // sehr wichtig !!!
        VID.Shutdown();

        this.re = Renderer.getDriver(DRIVER);
        ClientGlobals.re = this.re;

        re.Init(0, 0);
        kbd = re.getKeyboardHandler();
        kbd.Init();
        
        Cbuf.AddText("unbind t");
        Cbuf.Execute();        
        Cmd.AddCommand("togglemouse", (List<String> args) -> IN.toggleMouse());
        Cbuf.AddText("bind t togglemouse");
        Cbuf.Execute();
        ClientGlobals.cls.key_dest = Defines.key_game;
        ClientGlobals.cls.state = Defines.ca_active;
        
        viddef = ClientGlobals.viddef;
        fov_y = Math3D.CalcFov(fov_x, viddef.getWidth(), viddef.getHeight());
    }
    
    float fps = 0.0f;
    
    long start = 0;
    
    void updateScreen() {
        re.BeginFrame(0.0f);
        
        if (framecount % 500 == 0) {
            long time = System.currentTimeMillis();
            fps = 500000.0f / (time - start);
            start = time;
        }
        String text = ((int)(fps + 0.5f)) + " fps";
        
        testModel();
        
        drawString(10, viddef.getHeight() - 16, text);
        
        re.EndFrame();
        framecount++;
    }
    
    long startTime;
    
    void run() {
        startTime = System.currentTimeMillis();
        while (true) {
            re.updateScreen((List<String> args) -> updateScreen());
            kbd.Update();
            Cbuf.Execute();
        }
    }
    
    //	===================================================================
    
    private float yaw = 0;
    
    private entity_t[] models;
    
    private final static String[] skinNames = { "players/female/athena",
            "players/female/lotus", "players/female/venus",
            "players/female/voodoo", "players/female/cobalt",
            "players/female/lotus", "players/female/brianna" };
    
    private float fov_x = 50;
    
    private float fov_y;
    
    private void testModel() {
        
        refdef_t refdef = new refdef_t();
        
        refdef.x = 0;
        refdef.y = 0;
        refdef.width = viddef.getWidth();
        refdef.height = viddef.getHeight();
        refdef.fov_x = fov_x;
        refdef.fov_y = fov_y;
        refdef.time = 0.001f;
        
        if (models == null) {
            models = new entity_t[12]; // model count
            entity_t m;
            for (int i = 0; i < models.length; i++) {
                m = getModel(skinNames[i % skinNames.length]);
                m.origin[0] += 30 * i;
                m.origin[1] += ((i % 4)) * 30 - 20;
                models[i] = m;
            }
        }
        
        yaw = time() * 0.1f;
        if (yaw > 360)
            yaw -= 360;
        if (yaw < 0)
            yaw += 360;
        
        for (int i = 0; i < models.length; i++) {
            models[i].frame = (time() / 70) % models[i].model.numframes;
            models[i].angles[1] = yaw;
            models[i].origin[0] += KBD.my;
            models[i].origin[1] += KBD.mx;
        }
        
        refdef.areabits = null;
        refdef.num_entities = models.length;
        refdef.entities = models;
        
        refdef.lightstyles = null;
        refdef.rdflags = Defines.RDF_NOWORLDMODEL;
        
        re.RenderFrame(refdef);
    }
    
    private entity_t getModel(String name) {
        entity_t entity = new entity_t();
        String modelName = "players/female/tris.md2";
        String modelSkin = name + ".pcx";
        
        entity.model = re.RegisterModel(modelName);
        entity.skin = re.RegisterSkin(modelSkin);
        entity.flags = Defines.RF_FULLBRIGHT;
        entity.origin[0] = 80;
        entity.origin[1] = 0;
        entity.origin[2] = 0;
        Math3D.VectorCopy(entity.origin, entity.oldorigin);
        entity.frame = 0;
        entity.oldframe = 0;
        entity.backlerp = 0.0f;
        return entity;
    }
    
    private void drawString(int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            re.DrawChar(x + 8 * i, y, text.charAt(i));
        }
    }
    
    private int time() {
        return (int) (System.currentTimeMillis() - startTime);
    }

}