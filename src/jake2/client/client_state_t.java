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

// Created on 27.11.2003 by RST.
//$Id: client_state_t.java,v 1.18 2004-02-08 13:26:13 hoz Exp $

package jake2.client;

import jake2.Defines;
import jake2.game.cmodel_t;
import jake2.game.usercmd_t;
import jake2.render.image_t;
import jake2.render.model_t;

import java.io.RandomAccessFile;

public class client_state_t {

	public client_state_t() {
		for (int n = 0; n < Defines.CMD_BACKUP; n++)
			cmds[n] = new usercmd_t();
		for (int i = 0; i < frames.length; i++) {
			frames[i] = new frame_t();
		}

		for (int n = 0; n < Defines.MAX_CONFIGSTRINGS; n++)
			configstrings[n] = new String();
			
		for (int n=0; n < Defines.MAX_CLIENTS; n++)
			clientinfo[n] = new clientinfo_t();
	}
	//
	//	   the client_state_t structure is wiped completely at every
	//	   server map change
	//
	int timeoutcount;

	int timedemo_frames;
	int timedemo_start;

	public boolean refresh_prepped; // false if on new level or new ref dll
	boolean sound_prepped; // ambient sounds can start
	boolean force_refdef; // vid has changed, so we can't use a paused refdef

	int parse_entities; // index (not anded off) into cl_parse_entities[]

	usercmd_t cmd = new usercmd_t();
	usercmd_t cmds[] = new usercmd_t[Defines.CMD_BACKUP]; // each mesage will send several old cmds

	int cmd_time[] = new int[Defines.CMD_BACKUP]; // time sent, for calculating pings
	short predicted_origins[][] = new short[Defines.CMD_BACKUP][3]; // for debug comparing against server

	float predicted_step; // for stair up smoothing
	int predicted_step_time;

	float[] predicted_origin ={0,0,0}; // generated by CL_PredictMovement
	float[] predicted_angles={0,0,0};
	float[] prediction_error={0,0,0};

	public frame_t frame = new frame_t(); // received from server
	int surpressCount; // number of messages rate supressed
	frame_t frames[] = new frame_t[Defines.UPDATE_BACKUP];

	// the client maintains its own idea of view angles, which are
	// sent to the server each frame.  It is cleared to 0 upon entering each level.
	// the server sends a delta each frame which is added to the locally
	// tracked view angles to account for standing on rotating objects,
	// and teleport direction changes
	public float[] viewangles = { 0, 0, 0 };

	int time; // this is the time value that the client
	// is rendering at.  always <= cls.realtime
	float lerpfrac; // between oldframe and frame

	refdef_t refdef = new refdef_t();

	float[] v_forward = { 0, 0, 0 };
	float[] v_right = { 0, 0, 0 };
	float[] v_up = { 0, 0, 0 }; // set when refdef.angles is set

	//
	// transient data from server
	//

	String layout = ""; // general 2D overlay
	int inventory[] = new int[Defines.MAX_ITEMS];

	//
	// non-gameserver infornamtion
	// FIXME: move this cinematic stuff into the cin_t structure
	RandomAccessFile cinematic_file;
	
	int cinematictime; // cls.realtime for first cinematic frame
	int cinematicframe;
	byte cinematicpalette[] = new byte[768];
	boolean cinematicpalette_active;

	//
	// server state information
	//
	boolean attractloop; // running the attract loop, any key will menu
	int servercount; // server identification for prespawns
	String gamedir ="";
	int playernum;

	String configstrings[] = new String[Defines.MAX_CONFIGSTRINGS];

	//
	// locally derived information from server state
	//
	model_t model_draw[] = new model_t[Defines.MAX_MODELS];
	cmodel_t model_clip[] = new cmodel_t[Defines.MAX_MODELS];

	sfx_t sound_precache[] = new sfx_t[Defines.MAX_SOUNDS];
	image_t image_precache[] = new image_t[Defines.MAX_IMAGES];

	clientinfo_t clientinfo[] = new clientinfo_t[Defines.MAX_CLIENTS];
	clientinfo_t baseclientinfo = new clientinfo_t();

}
