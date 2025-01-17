/*
 * LwjglRenderer.java
 * Copyright (C) 2004
 *
 * $Id: LwjglRenderer.java,v 1.5 2007-01-11 23:20:40 cawe Exp $
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
package jake2.client.render;

import jake2.client.KBD;
import jake2.client.LWJGLKBD;
import jake2.client.refdef_t;
import jake2.client.refexport_t;
import jake2.client.render.opengl.LwjglDriver;
import jake2.qcommon.Defines;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

import java.awt.Dimension;

/**
 * LwjglRenderer
 * 
 * @author dsanders/cwei
 */
@Environment(EnvType.CLIENT)
final class LwjglRenderer extends LwjglDriver implements refexport_t, Ref {
	
    	public static final String DRIVER_NAME = "lwjgl";
    
    	private KBD kbd = new LWJGLKBD();
    
    	// is set from Renderer factory
    	private RenderAPI impl;

	static {
		Renderer.register(new LwjglRenderer());
	}

    private LwjglRenderer() {
	}

	// ============================================================================
	// public interface for Renderer implementations
	//
	// refexport_t (ref.h)
	// ============================================================================

	/** 
	 * @see refexport_t#Init
	 */
	public boolean Init(int vid_xpos, int vid_ypos) {
        // init the OpenGL drivers
        impl.setGLDriver(this);
		
		// pre init
		if (!impl.R_Init(vid_xpos, vid_ypos)) return false;
		// post init		
		return impl.R_Init2();
	}

	/** 
	 * @see refexport_t#Shutdown()
	 */
	public void Shutdown() {
		impl.R_Shutdown();
	}

	/** 
	 * @see refexport_t#BeginRegistration(String)
	 */
	public void BeginRegistration(String map) {
		impl.R_BeginRegistration(map);
	}

	/** 
	 * @see refexport_t#RegisterModel(String)
	 */
	public model_t RegisterModel(String name) {
		return impl.R_RegisterModel(name);
	}

	/** 
	 * @see refexport_t#RegisterSkin(String)
	 */
	public image_t RegisterSkin(String name) {
		return impl.R_RegisterSkin(name);
	}
	
	/** 
	 * @see refexport_t#RegisterPic(String)
	 */
	public image_t RegisterPic(String name) {
		return impl.Draw_FindPic(name);
	}
	/** 
	 * @see refexport_t#SetSky(String, float, float[])
	 */
	public void SetSky(String name, float rotate, float[] axis) {
		impl.R_SetSky(name, rotate, axis);
	}

	/** 
	 * @see refexport_t#EndRegistration()
	 */
	public void EndRegistration() {
		impl.R_EndRegistration();
	}

	/** 
	 * @see refexport_t#RenderFrame(refdef_t)
	 */
	public void RenderFrame(refdef_t fd) {
		impl.R_RenderFrame(fd);
	}

	/** 
	 * @see refexport_t#DrawGetPicSize(Dimension, String)
	 */
	public void DrawGetPicSize(Dimension dim, String name) {
		impl.Draw_GetPicSize(dim, name);
	}

	/** 
	 * @see refexport_t#DrawPic(int, int, String)
	 */
	public void DrawPic(int x, int y, String name) {
		impl.Draw_Pic(x, y, name);
	}

	/** 
	 * @see refexport_t#DrawStretchPic(int, int, int, int, String)
	 */
	public void DrawStretchPic(int x, int y, int w, int h, String name) {
		impl.Draw_StretchPic(x, y, w, h, name);
	}

	/** 
	 * @see refexport_t#DrawChar(int, int, int)
	 */
	public void DrawChar(int x, int y, int num) {
		impl.Draw_Char(x, y, num);
	}

	/** 
	 * @see refexport_t#DrawTileClear(int, int, int, int, String)
	 */
	public void DrawTileClear(int x, int y, int w, int h, String name) {
		impl.Draw_TileClear(x, y, w, h, name);
	}

	/** 
	 * @see refexport_t#DrawFill(int, int, int, int, int)
	 */
	public void DrawFill(int x, int y, int w, int h, int c) {
		impl.Draw_Fill(x, y, w, h, c);
	}

	/** 
	 * @see refexport_t#DrawFadeScreen()
	 */
	public void DrawFadeScreen() {
		impl.Draw_FadeScreen();
	}

	/** 
	 * @see refexport_t#DrawStretchRaw(int, int, int, int, int, int, byte[])
	 */
	public void DrawStretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
		impl.Draw_StretchRaw(x, y, w, h, cols, rows, data);
	}

	/** 
	 * @see refexport_t#CinematicSetPalette(byte[])
	 */
	public void CinematicSetPalette(byte[] palette) {
		impl.R_SetPalette(palette);
	}

	/** 
	 * @see refexport_t#BeginFrame(float)
	 */
	public void BeginFrame(float camera_separation) {
		impl.R_BeginFrame(camera_separation);
	}

	/** 
	 * @see refexport_t#EndFrame()
	 */
	public void EndFrame() {
		endFrame();
	}

	/** 
	 * @see refexport_t#AppActivate(boolean)
	 */
	public void AppActivate(boolean activate) {
	    appActivate(activate);
	}
	
    	public void screenshot() {
    	    impl.GL_ScreenShot_f();
	}

	public int apiVersion() {
		return Defines.API_VERSION;
	}
    
	public KBD getKeyboardHandler() {
		return kbd;
	}
	
	// ============================================================================
	// Ref interface
	// ============================================================================

	public String getName() {
		return DRIVER_NAME;
	}

	public String toString() {
		return DRIVER_NAME;
	}

	public refexport_t GetRefAPI(RenderAPI renderer) {
        	this.impl = renderer;
		return this;
	}
}