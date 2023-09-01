/*
 * refexport_t.java
 * Copyright (C) 2003
 *
 * $Id: refexport_t.java,v 1.3 2004-12-14 00:11:10 hzi Exp $
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

import jake2.client.render.image_t;
import jake2.client.render.model_t;
import jake2.qcommon.exec.Command;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.util.Collections;

/**
 * refexport_t
 * 
 * @author cwei
 */
@Environment(EnvType.CLIENT)
public interface refexport_t {
	refexport_t DUMMY = new refexport_t() {};

	// ============================================================================
	// public interface for Renderer implementations
	//
	// ref.h, refexport_t
	// ============================================================================
	//
	// these are the functions exported by the refresh module
	//
	// called when the library is loaded
	default boolean Init(int vid_xpos, int vid_ypos) {
		return false;
	}

	// called before the library is unloaded
	default void Shutdown() {
	}

	// All data that will be used in a level should be
	// registered before rendering any frames to prevent disk hits,
	// but they can still be registered at a later time
	// if necessary.
	//
	// EndRegistration will free any remaining data that wasn't registered.
	// Any model_s or skin_s pointers from before the BeginRegistration
	// are no longer valid after EndRegistration.
	//
	// Skins and images need to be differentiated, because skins
	// are flood filled to eliminate mip map edge errors, and pics have
	// an implicit "pics/" prepended to the name. (a pic name that starts with a
	// slash will not use the "pics/" prefix or the ".pcx" postfix)
	default void BeginRegistration(String map) {
	}

	default model_t RegisterModel(String name) {
		return null;
	}

	default image_t RegisterSkin(String name) {
		return null;
	}

	default image_t RegisterPic(String name) {
		return null;
	}

	default void SetSky(String name, float rotate, /* vec3_t */
						float[] axis) {
	}

	default void EndRegistration() {
	}

	default void RenderFrame(refdef_t fd) {
	}

	default void DrawGetPicSize(Dimension dim /* int *w, *h */, String name) {
	}

	// will return 0 0 if not found
	default void DrawPic(int x, int y, String name) {
	}

	default void DrawStretchPic(int x, int y, int w, int h, String name) {
	}

	default void DrawChar(int x, int y, int num) { // num is 8 bit ASCII
	}

	default void DrawTileClear(int x, int y, int w, int h, String name) {
	}

	default void DrawFill(int x, int y, int w, int h, int c) {
	}

	default void DrawFadeScreen() {
	}

	// Draw images for cinematic rendering (which can have a different palette). Note that calls
	default void DrawStretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
	}

	/*
	 ** video mode and refresh state management entry points
	 */
	/* 256 r,g,b values;	null = game palette, size = 768 bytes */
	default void CinematicSetPalette(final byte[] palette) {
	}

	default void BeginFrame(float camera_separation) {
	}

	default void EndFrame() {
	}

	default void AppActivate(boolean activate) {
	}

	default void updateScreen(Command callback) {
		callback.execute(Collections.emptyList());
	}

	default int apiVersion() {
		return 0;
	}

	default DisplayMode[] getModeList() {
		return new DisplayMode[0];
	}

	default KBD getKeyboardHandler() {
		return null;
	}
}
