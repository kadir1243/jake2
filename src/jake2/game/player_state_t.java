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

public class player_state_t {
	//	player_state_t is the information needed in addition to pmove_state_t
	//	to rendered a view.  There will only be 10 player_state_t sent each second,
	//	but the number of pmove_state_t changes will be reletive to client
	//	frame rates

	pmove_state_t pmove; // for prediction

	// these fields do not need to be communicated bit-precise

	float[] viewangles= { 0, 0, 0 }; // for fixed views

	float[] viewoffset= { 0, 0, 0 }; // add to pmovestate->origin
	float[] kick_angles= { 0, 0, 0 }; // add to view direction to get render angles
	// set by weapon kicks, pain effects, etc

	float[] gunangles= { 0, 0, 0 };
	float[] gunoffset= { 0, 0, 0 };
	int gunindex;
	int gunframe;

	float blend[]= new float[4]; // rgba full screen effect

	float fov; // horizontal field of view

	int rdflags; // refdef flags

	short stats[]= new short[gamedefs.MAX_STATS]; // fast status bar updates
}
