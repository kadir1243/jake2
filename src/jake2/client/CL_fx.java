/*
 * CL_fx.java
 * Copyright (C) 2004
 * 
 * $Id: CL_fx.java,v 1.8 2004-02-04 11:24:15 hoz Exp $
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

import jake2.Globals;
import jake2.game.entity_state_t;
import jake2.qcommon.Com;

/**
 * CL_fx
 */
public class CL_fx extends Globals {
	
	static final float INSTANT_PARTICLE = -10000.0f;
	
	public static class cdlight_t
	{
		int		key;				// so entities can reuse same entry
		float [] color={0,0,0};
		float [] origin = {0,0,0};
		float	radius;
		float	die;				// stop lighting after this time
		float	decay;				// drop this each second
		float	minlight;			// don't add when contributing less
		void clear() {
			radius = decay = die = minlight =
			color[0] = color[1] = color[2] = origin[0] = origin[1] = origin[2] =
			key = 0;
		}
	}
	
	
////	   cl_fx.c -- entity effects parsing and management
//
//	static float []  avelocities [NUMVERTEXNORMALS];
//
//	extern	struct model_s	*cl_mod_smoke;
//	extern	struct model_s	*cl_mod_flash;
//
//	/*
//	==============================================================
//
//	LIGHT STYLE MANAGEMENT
//
//	==============================================================
//	*/
//
//	typedef struct
//	{
//		int		length;
//		float	value[3];
//		float	map[MAX_QPATH];
//	} clightstyle_t;
	static class clightstyle_t {
		int length;
		float[] value = new float[3];
		float[] map = new float[MAX_QPATH];
		void clear() {
			value[0] = value[1] = value[2] = length = 0;
			for (int i = 0; i < map.length; i++)
				map[i] = 0.0f;
		}
	}
	static clightstyle_t[] cl_lightstyle = new clightstyle_t[MAX_LIGHTSTYLES];
	static {
		for(int i=0; i<cl_lightstyle.length; i++) {
			cl_lightstyle[i] = new clightstyle_t();
		}
	}
	static int lastofs;

	/*
	================
	CL_ClearLightStyles
	================
	*/
	static void ClearLightStyles ()
	{
		//memset (cl_lightstyle, 0, sizeof(cl_lightstyle));
		for (int i = 0; i < cl_lightstyle.length; i++)
			cl_lightstyle[i].clear();
		lastofs = -1;
	}

	/*
	================
	CL_RunLightStyles
	================
	*/
	static void RunLightStyles() {
		int ofs;
		int i;
		clightstyle_t[] ls;

		ofs = cl.time / 100;
		if (ofs == lastofs)
			return;
		lastofs = ofs;
		ls = cl_lightstyle;
		for (i = 0; i < ls.length; i++) {
			if (ls[i].length == 0) {
				ls[i].value[0] = ls[i].value[1] = ls[i].value[2] = 1.0f;
				continue;
			}
			if (ls.length == 1)
				ls[i].value[0] = ls[i].value[1] = ls[i].value[2] = ls[i].map[0];
			else
				ls[i].value[0] = ls[i].value[1] = ls[i].value[2] = ls[i].map[ofs % ls[i].length];
		}
	}

	static void SetLightstyle(int i) {
		String s;
		int j, k;

		s = cl.configstrings[i + CS_LIGHTS];

		j = strlen(s);
		if (j >= MAX_QPATH)
			Com.Error(ERR_DROP, "svc_lightstyle length=" + j);

		cl_lightstyle[i].length = j;

		for (k = 0; k < j; k++)
			cl_lightstyle[i].map[k] = (float) (s.charAt(k) - 'a') / (float) ('m' - 'a');
	}

	/*
	================
	CL_AddLightStyles
	================
	*/
	static void AddLightStyles() {
		int i;
		clightstyle_t[] ls;

		ls = cl_lightstyle;
		for (i = 0; i < ls.length; i++)
			V.AddLightStyle(i, ls[i].value[0], ls[i].value[1], ls[i].value[2]);
	}

	/*
	==============================================================

	DLIGHT MANAGEMENT

	==============================================================
	*/

	static cdlight_t[] cl_dlights = new cdlight_t[MAX_DLIGHTS];
	static {
		for (int i = 0; i < cl_dlights.length; i++)
		cl_dlights[i] = new cdlight_t();
	}

	/*
	================
	CL_ClearDlights
	================
	*/
	static void ClearDlights() {
		//		memset (cl_dlights, 0, sizeof(cl_dlights));
		for (int i = 0; i < cl_dlights.length; i++) {
			cl_dlights[i].clear();
		}
	}

	/*
	===============
	CL_AllocDlight

	===============
	*/
	static cdlight_t AllocDlight(int key) {
		int i;
		cdlight_t[] dl;

		//	   first look for an exact key match
		if (key != 0) {
			dl = cl_dlights;
			for (i = 0; i < MAX_DLIGHTS; i++) {
				if (dl[i].key == key) {
					//memset (dl, 0, sizeof(*dl));
					dl[i].clear();
					dl[i].key = key;
					return dl[i];
				}
			}
		}

		//	   then look for anything else
		dl = cl_dlights;
		for (i = 0; i < MAX_DLIGHTS; i++) {
			if (dl[i].die < cl.time) {
				//memset (dl, 0, sizeof(*dl));
				dl[i].clear();
				dl[i].key = key;
				return dl[i];
			}
		}

		//dl = &cl_dlights[0];
		//memset (dl, 0, sizeof(*dl));
		dl[0].clear();
		dl[0].key = key;
		return dl[0];
	}

	/*
	===============
	CL_NewDlight
	===============
	*/
	static void NewDlight(int key, float x, float y, float z, float radius, float time) {
		cdlight_t dl;

		dl = CL.AllocDlight(key);
		dl.origin[0] = x;
		dl.origin[1] = y;
		dl.origin[2] = z;
		dl.radius = radius;
		dl.die = cl.time + time;
	}

	/*
	===============
	CL_RunDLights

	===============
	*/
	static void RunDLights() {
		int i;
		cdlight_t[] dl;

		dl = cl_dlights;
		for (i = 0; i < MAX_DLIGHTS; i++) {
			if (dl[i].radius == 0.0f)
				continue;

			if (dl[i].die < cl.time) {
				dl[i].radius = 0;
				return;
			}
			dl[i].radius -= cls.frametime * dl[i].decay;
			if (dl[i].radius < 0)
				dl[i].radius = 0;
		}
	}
//
//	/*
//	==============
//	CL_ParseMuzzleFlash
//	==============
//	*/
 	static void ParseMuzzleFlash ()
	{
//		float [] 		fv, rv;
//		cdlight_t	*dl;
//		int			i, weapon;
//		centity_t	*pl;
//		int			silenced;
//		float		volume;
//		char		soundname[64];
//
//		i = MSG_ReadShort (&net_message);
//		if (i < 1 || i >= MAX_EDICTS)
//			Com_Error (ERR_DROP, "CL_ParseMuzzleFlash: bad entity");
//
//		weapon = MSG_ReadByte (&net_message);
//		silenced = weapon & MZ_SILENCED;
//		weapon &= ~MZ_SILENCED;
//
//		pl = &cl_entities[i];
//
//		dl = CL_AllocDlight (i);
//		VectorCopy (pl->current.origin,  dl->origin);
//		AngleVectors (pl->current.angles, fv, rv, NULL);
//		VectorMA (dl->origin, 18, fv, dl->origin);
//		VectorMA (dl->origin, 16, rv, dl->origin);
//		if (silenced)
//			dl->radius = 100 + (rand()&31);
//		else
//			dl->radius = 200 + (rand()&31);
//		dl->minlight = 32;
//		dl->die = cl.time; // + 0.1;
//
//		if (silenced)
//			volume = 0.2;
//		else
//			volume = 1;
//
//		switch (weapon)
//		{
//		case MZ_BLASTER:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/blastf1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_BLUEHYPERBLASTER:
//			dl->color[0] = 0;dl->color[1] = 0;dl->color[2] = 1;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/hyprbf1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_HYPERBLASTER:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/hyprbf1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_MACHINEGUN:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0);
//			break;
//		case MZ_SHOTGUN:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/shotgf1b.wav"), volume, ATTN_NORM, 0);
//			S_StartSound (NULL, i, CHAN_AUTO,   S_RegisterSound("weapons/shotgr1b.wav"), volume, ATTN_NORM, 0.1);
//			break;
//		case MZ_SSHOTGUN:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/sshotf1b.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_CHAINGUN1:
//			dl->radius = 200 + (rand()&31);
//			dl->color[0] = 1;dl->color[1] = 0.25;dl->color[2] = 0;
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0);
//			break;
//		case MZ_CHAINGUN2:
//			dl->radius = 225 + (rand()&31);
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0;
//			dl->die = cl.time  + 0.1;	// long delay
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0);
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0.05);
//			break;
//		case MZ_CHAINGUN3:
//			dl->radius = 250 + (rand()&31);
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			dl->die = cl.time  + 0.1;	// long delay
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0);
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0.033);
//			Com_sprintf(soundname, sizeof(soundname), "weapons/machgf%ib.wav", (rand() % 5) + 1);
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound(soundname), volume, ATTN_NORM, 0.066);
//			break;
//		case MZ_RAILGUN:
//			dl->color[0] = 0.5;dl->color[1] = 0.5;dl->color[2] = 1.0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/railgf1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_ROCKET:
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0.2;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/rocklf1a.wav"), volume, ATTN_NORM, 0);
//			S_StartSound (NULL, i, CHAN_AUTO,   S_RegisterSound("weapons/rocklr1b.wav"), volume, ATTN_NORM, 0.1);
//			break;
//		case MZ_GRENADE:
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/grenlf1a.wav"), volume, ATTN_NORM, 0);
//			S_StartSound (NULL, i, CHAN_AUTO,   S_RegisterSound("weapons/grenlr1b.wav"), volume, ATTN_NORM, 0.1);
//			break;
//		case MZ_BFG:
//			dl->color[0] = 0;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/bfg__f1y.wav"), volume, ATTN_NORM, 0);
//			break;
//
//		case MZ_LOGIN:
//			dl->color[0] = 0;dl->color[1] = 1; dl->color[2] = 0;
//			dl->die = cl.time + 1.0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/grenlf1a.wav"), 1, ATTN_NORM, 0);
//			CL_LogoutEffect (pl->current.origin, weapon);
//			break;
//		case MZ_LOGOUT:
//			dl->color[0] = 1;dl->color[1] = 0; dl->color[2] = 0;
//			dl->die = cl.time + 1.0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/grenlf1a.wav"), 1, ATTN_NORM, 0);
//			CL_LogoutEffect (pl->current.origin, weapon);
//			break;
//		case MZ_RESPAWN:
//			dl->color[0] = 1;dl->color[1] = 1; dl->color[2] = 0;
//			dl->die = cl.time + 1.0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/grenlf1a.wav"), 1, ATTN_NORM, 0);
//			CL_LogoutEffect (pl->current.origin, weapon);
//			break;
//		// RAFAEL
//		case MZ_PHALANX:
//			dl->color[0] = 1;dl->color[1] = 0.5; dl->color[2] = 0.5;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/plasshot.wav"), volume, ATTN_NORM, 0);
//			break;
//		// RAFAEL
//		case MZ_IONRIPPER:	
//			dl->color[0] = 1;dl->color[1] = 0.5; dl->color[2] = 0.5;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/rippfire.wav"), volume, ATTN_NORM, 0);
//			break;
//
////	   ======================
////	   PGM
//		case MZ_ETF_RIFLE:
//			dl->color[0] = 0.9;dl->color[1] = 0.7;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/nail1.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_SHOTGUN2:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/shotg2.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_HEATBEAM:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			dl->die = cl.time + 100;
////			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/bfg__l1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_BLASTER2:
//			dl->color[0] = 0;dl->color[1] = 1;dl->color[2] = 0;
//			// FIXME - different sound for blaster2 ??
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/blastf1a.wav"), volume, ATTN_NORM, 0);
//			break;
//		case MZ_TRACKER:
//			// negative flashes handled the same in gl/soft until CL_AddDLights
//			dl->color[0] = -1;dl->color[1] = -1;dl->color[2] = -1;
//			S_StartSound (NULL, i, CHAN_WEAPON, S_RegisterSound("weapons/disint2.wav"), volume, ATTN_NORM, 0);
//			break;		
//		case MZ_NUKE1:
//			dl->color[0] = 1;dl->color[1] = 0;dl->color[2] = 0;
//			dl->die = cl.time + 100;
//			break;
//		case MZ_NUKE2:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			dl->die = cl.time + 100;
//			break;
//		case MZ_NUKE4:
//			dl->color[0] = 0;dl->color[1] = 0;dl->color[2] = 1;
//			dl->die = cl.time + 100;
//			break;
//		case MZ_NUKE8:
//			dl->color[0] = 0;dl->color[1] = 1;dl->color[2] = 1;
//			dl->die = cl.time + 100;
//			break;
////	   PGM
////	   ======================
//		}
	}
//
//
//	/*
//	==============
//	CL_ParseMuzzleFlash2
//	==============
//	*/
	static void ParseMuzzleFlash2 () 
	{
//		int			ent;
//		float [] 		origin;
//		int			flash_number;
//		cdlight_t	*dl;
//		float [] 		forward, right;
//		char		soundname[64];
//
//		ent = MSG_ReadShort (&net_message);
//		if (ent < 1 || ent >= MAX_EDICTS)
//			Com_Error (ERR_DROP, "CL_ParseMuzzleFlash2: bad entity");
//
//		flash_number = MSG_ReadByte (&net_message);
//
//		// locate the origin
//		AngleVectors (cl_entities[ent].current.angles, forward, right, NULL);
//		origin[0] = cl_entities[ent].current.origin[0] + forward[0] * monster_flash_offset[flash_number][0] + right[0] * monster_flash_offset[flash_number][1];
//		origin[1] = cl_entities[ent].current.origin[1] + forward[1] * monster_flash_offset[flash_number][0] + right[1] * monster_flash_offset[flash_number][1];
//		origin[2] = cl_entities[ent].current.origin[2] + forward[2] * monster_flash_offset[flash_number][0] + right[2] * monster_flash_offset[flash_number][1] + monster_flash_offset[flash_number][2];
//
//		dl = CL_AllocDlight (ent);
//		VectorCopy (origin,  dl->origin);
//		dl->radius = 200 + (rand()&31);
//		dl->minlight = 32;
//		dl->die = cl.time;	// + 0.1;
//
//		switch (flash_number)
//		{
//		case MZ2_INFANTRY_MACHINEGUN_1:
//		case MZ2_INFANTRY_MACHINEGUN_2:
//		case MZ2_INFANTRY_MACHINEGUN_3:
//		case MZ2_INFANTRY_MACHINEGUN_4:
//		case MZ2_INFANTRY_MACHINEGUN_5:
//		case MZ2_INFANTRY_MACHINEGUN_6:
//		case MZ2_INFANTRY_MACHINEGUN_7:
//		case MZ2_INFANTRY_MACHINEGUN_8:
//		case MZ2_INFANTRY_MACHINEGUN_9:
//		case MZ2_INFANTRY_MACHINEGUN_10:
//		case MZ2_INFANTRY_MACHINEGUN_11:
//		case MZ2_INFANTRY_MACHINEGUN_12:
//		case MZ2_INFANTRY_MACHINEGUN_13:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("infantry/infatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_SOLDIER_MACHINEGUN_1:
//		case MZ2_SOLDIER_MACHINEGUN_2:
//		case MZ2_SOLDIER_MACHINEGUN_3:
//		case MZ2_SOLDIER_MACHINEGUN_4:
//		case MZ2_SOLDIER_MACHINEGUN_5:
//		case MZ2_SOLDIER_MACHINEGUN_6:
//		case MZ2_SOLDIER_MACHINEGUN_7:
//		case MZ2_SOLDIER_MACHINEGUN_8:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("soldier/solatck3.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_GUNNER_MACHINEGUN_1:
//		case MZ2_GUNNER_MACHINEGUN_2:
//		case MZ2_GUNNER_MACHINEGUN_3:
//		case MZ2_GUNNER_MACHINEGUN_4:
//		case MZ2_GUNNER_MACHINEGUN_5:
//		case MZ2_GUNNER_MACHINEGUN_6:
//		case MZ2_GUNNER_MACHINEGUN_7:
//		case MZ2_GUNNER_MACHINEGUN_8:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("gunner/gunatck2.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_ACTOR_MACHINEGUN_1:
//		case MZ2_SUPERTANK_MACHINEGUN_1:
//		case MZ2_SUPERTANK_MACHINEGUN_2:
//		case MZ2_SUPERTANK_MACHINEGUN_3:
//		case MZ2_SUPERTANK_MACHINEGUN_4:
//		case MZ2_SUPERTANK_MACHINEGUN_5:
//		case MZ2_SUPERTANK_MACHINEGUN_6:
//		case MZ2_TURRET_MACHINEGUN:			// PGM
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("infantry/infatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_BOSS2_MACHINEGUN_L1:
//		case MZ2_BOSS2_MACHINEGUN_L2:
//		case MZ2_BOSS2_MACHINEGUN_L3:
//		case MZ2_BOSS2_MACHINEGUN_L4:
//		case MZ2_BOSS2_MACHINEGUN_L5:
//		case MZ2_CARRIER_MACHINEGUN_L1:		// PMM
//		case MZ2_CARRIER_MACHINEGUN_L2:		// PMM
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("infantry/infatck1.wav"), 1, ATTN_NONE, 0);
//			break;
//
//		case MZ2_SOLDIER_BLASTER_1:
//		case MZ2_SOLDIER_BLASTER_2:
//		case MZ2_SOLDIER_BLASTER_3:
//		case MZ2_SOLDIER_BLASTER_4:
//		case MZ2_SOLDIER_BLASTER_5:
//		case MZ2_SOLDIER_BLASTER_6:
//		case MZ2_SOLDIER_BLASTER_7:
//		case MZ2_SOLDIER_BLASTER_8:
//		case MZ2_TURRET_BLASTER:			// PGM
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("soldier/solatck2.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_FLYER_BLASTER_1:
//		case MZ2_FLYER_BLASTER_2:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("flyer/flyatck3.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_MEDIC_BLASTER_1:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("medic/medatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_HOVER_BLASTER_1:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("hover/hovatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_FLOAT_BLASTER_1:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("floater/fltatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_SOLDIER_SHOTGUN_1:
//		case MZ2_SOLDIER_SHOTGUN_2:
//		case MZ2_SOLDIER_SHOTGUN_3:
//		case MZ2_SOLDIER_SHOTGUN_4:
//		case MZ2_SOLDIER_SHOTGUN_5:
//		case MZ2_SOLDIER_SHOTGUN_6:
//		case MZ2_SOLDIER_SHOTGUN_7:
//		case MZ2_SOLDIER_SHOTGUN_8:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("soldier/solatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_TANK_BLASTER_1:
//		case MZ2_TANK_BLASTER_2:
//		case MZ2_TANK_BLASTER_3:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("tank/tnkatck3.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_TANK_MACHINEGUN_1:
//		case MZ2_TANK_MACHINEGUN_2:
//		case MZ2_TANK_MACHINEGUN_3:
//		case MZ2_TANK_MACHINEGUN_4:
//		case MZ2_TANK_MACHINEGUN_5:
//		case MZ2_TANK_MACHINEGUN_6:
//		case MZ2_TANK_MACHINEGUN_7:
//		case MZ2_TANK_MACHINEGUN_8:
//		case MZ2_TANK_MACHINEGUN_9:
//		case MZ2_TANK_MACHINEGUN_10:
//		case MZ2_TANK_MACHINEGUN_11:
//		case MZ2_TANK_MACHINEGUN_12:
//		case MZ2_TANK_MACHINEGUN_13:
//		case MZ2_TANK_MACHINEGUN_14:
//		case MZ2_TANK_MACHINEGUN_15:
//		case MZ2_TANK_MACHINEGUN_16:
//		case MZ2_TANK_MACHINEGUN_17:
//		case MZ2_TANK_MACHINEGUN_18:
//		case MZ2_TANK_MACHINEGUN_19:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			Com_sprintf(soundname, sizeof(soundname), "tank/tnkatk2%c.wav", 'a' + rand() % 5);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound(soundname), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_CHICK_ROCKET_1:
//		case MZ2_TURRET_ROCKET:			// PGM
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0.2;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("chick/chkatck2.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_TANK_ROCKET_1:
//		case MZ2_TANK_ROCKET_2:
//		case MZ2_TANK_ROCKET_3:
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0.2;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("tank/tnkatck1.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_SUPERTANK_ROCKET_1:
//		case MZ2_SUPERTANK_ROCKET_2:
//		case MZ2_SUPERTANK_ROCKET_3:
//		case MZ2_BOSS2_ROCKET_1:
//		case MZ2_BOSS2_ROCKET_2:
//		case MZ2_BOSS2_ROCKET_3:
//		case MZ2_BOSS2_ROCKET_4:
//		case MZ2_CARRIER_ROCKET_1:
////		case MZ2_CARRIER_ROCKET_2:
////		case MZ2_CARRIER_ROCKET_3:
////		case MZ2_CARRIER_ROCKET_4:
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0.2;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("tank/rocket.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_GUNNER_GRENADE_1:
//		case MZ2_GUNNER_GRENADE_2:
//		case MZ2_GUNNER_GRENADE_3:
//		case MZ2_GUNNER_GRENADE_4:
//			dl->color[0] = 1;dl->color[1] = 0.5;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("gunner/gunatck3.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_GLADIATOR_RAILGUN_1:
//		// PMM
//		case MZ2_CARRIER_RAILGUN:
//		case MZ2_WIDOW_RAIL:
//		// pmm
//			dl->color[0] = 0.5;dl->color[1] = 0.5;dl->color[2] = 1.0;
//			break;
//
////	   --- Xian's shit starts ---
//		case MZ2_MAKRON_BFG:
//			dl->color[0] = 0.5;dl->color[1] = 1 ;dl->color[2] = 0.5;
//			//S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("makron/bfg_fire.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_MAKRON_BLASTER_1:
//		case MZ2_MAKRON_BLASTER_2:
//		case MZ2_MAKRON_BLASTER_3:
//		case MZ2_MAKRON_BLASTER_4:
//		case MZ2_MAKRON_BLASTER_5:
//		case MZ2_MAKRON_BLASTER_6:
//		case MZ2_MAKRON_BLASTER_7:
//		case MZ2_MAKRON_BLASTER_8:
//		case MZ2_MAKRON_BLASTER_9:
//		case MZ2_MAKRON_BLASTER_10:
//		case MZ2_MAKRON_BLASTER_11:
//		case MZ2_MAKRON_BLASTER_12:
//		case MZ2_MAKRON_BLASTER_13:
//		case MZ2_MAKRON_BLASTER_14:
//		case MZ2_MAKRON_BLASTER_15:
//		case MZ2_MAKRON_BLASTER_16:
//		case MZ2_MAKRON_BLASTER_17:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("makron/blaster.wav"), 1, ATTN_NORM, 0);
//			break;
//	
//		case MZ2_JORG_MACHINEGUN_L1:
//		case MZ2_JORG_MACHINEGUN_L2:
//		case MZ2_JORG_MACHINEGUN_L3:
//		case MZ2_JORG_MACHINEGUN_L4:
//		case MZ2_JORG_MACHINEGUN_L5:
//		case MZ2_JORG_MACHINEGUN_L6:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("boss3/xfire.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_JORG_MACHINEGUN_R1:
//		case MZ2_JORG_MACHINEGUN_R2:
//		case MZ2_JORG_MACHINEGUN_R3:
//		case MZ2_JORG_MACHINEGUN_R4:
//		case MZ2_JORG_MACHINEGUN_R5:
//		case MZ2_JORG_MACHINEGUN_R6:
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			break;
//
//		case MZ2_JORG_BFG_1:
//			dl->color[0] = 0.5;dl->color[1] = 1 ;dl->color[2] = 0.5;
//			break;
//
//		case MZ2_BOSS2_MACHINEGUN_R1:
//		case MZ2_BOSS2_MACHINEGUN_R2:
//		case MZ2_BOSS2_MACHINEGUN_R3:
//		case MZ2_BOSS2_MACHINEGUN_R4:
//		case MZ2_BOSS2_MACHINEGUN_R5:
//		case MZ2_CARRIER_MACHINEGUN_R1:			// PMM
//		case MZ2_CARRIER_MACHINEGUN_R2:			// PMM
//
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//
//			CL_ParticleEffect (origin, vec3_origin, 0, 40);
//			CL_SmokeAndFlash(origin);
//			break;
//
////	   ======
////	   ROGUE
//		case MZ2_STALKER_BLASTER:
//		case MZ2_DAEDALUS_BLASTER:
//		case MZ2_MEDIC_BLASTER_2:
//		case MZ2_WIDOW_BLASTER:
//		case MZ2_WIDOW_BLASTER_SWEEP1:
//		case MZ2_WIDOW_BLASTER_SWEEP2:
//		case MZ2_WIDOW_BLASTER_SWEEP3:
//		case MZ2_WIDOW_BLASTER_SWEEP4:
//		case MZ2_WIDOW_BLASTER_SWEEP5:
//		case MZ2_WIDOW_BLASTER_SWEEP6:
//		case MZ2_WIDOW_BLASTER_SWEEP7:
//		case MZ2_WIDOW_BLASTER_SWEEP8:
//		case MZ2_WIDOW_BLASTER_SWEEP9:
//		case MZ2_WIDOW_BLASTER_100:
//		case MZ2_WIDOW_BLASTER_90:
//		case MZ2_WIDOW_BLASTER_80:
//		case MZ2_WIDOW_BLASTER_70:
//		case MZ2_WIDOW_BLASTER_60:
//		case MZ2_WIDOW_BLASTER_50:
//		case MZ2_WIDOW_BLASTER_40:
//		case MZ2_WIDOW_BLASTER_30:
//		case MZ2_WIDOW_BLASTER_20:
//		case MZ2_WIDOW_BLASTER_10:
//		case MZ2_WIDOW_BLASTER_0:
//		case MZ2_WIDOW_BLASTER_10L:
//		case MZ2_WIDOW_BLASTER_20L:
//		case MZ2_WIDOW_BLASTER_30L:
//		case MZ2_WIDOW_BLASTER_40L:
//		case MZ2_WIDOW_BLASTER_50L:
//		case MZ2_WIDOW_BLASTER_60L:
//		case MZ2_WIDOW_BLASTER_70L:
//		case MZ2_WIDOW_RUN_1:
//		case MZ2_WIDOW_RUN_2:
//		case MZ2_WIDOW_RUN_3:
//		case MZ2_WIDOW_RUN_4:
//		case MZ2_WIDOW_RUN_5:
//		case MZ2_WIDOW_RUN_6:
//		case MZ2_WIDOW_RUN_7:
//		case MZ2_WIDOW_RUN_8:
//			dl->color[0] = 0;dl->color[1] = 1;dl->color[2] = 0;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("tank/tnkatck3.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_WIDOW_DISRUPTOR:
//			dl->color[0] = -1;dl->color[1] = -1;dl->color[2] = -1;
//			S_StartSound (NULL, ent, CHAN_WEAPON, S_RegisterSound("weapons/disint2.wav"), 1, ATTN_NORM, 0);
//			break;
//
//		case MZ2_WIDOW_PLASMABEAM:
//		case MZ2_WIDOW2_BEAMER_1:
//		case MZ2_WIDOW2_BEAMER_2:
//		case MZ2_WIDOW2_BEAMER_3:
//		case MZ2_WIDOW2_BEAMER_4:
//		case MZ2_WIDOW2_BEAMER_5:
//		case MZ2_WIDOW2_BEAM_SWEEP_1:
//		case MZ2_WIDOW2_BEAM_SWEEP_2:
//		case MZ2_WIDOW2_BEAM_SWEEP_3:
//		case MZ2_WIDOW2_BEAM_SWEEP_4:
//		case MZ2_WIDOW2_BEAM_SWEEP_5:
//		case MZ2_WIDOW2_BEAM_SWEEP_6:
//		case MZ2_WIDOW2_BEAM_SWEEP_7:
//		case MZ2_WIDOW2_BEAM_SWEEP_8:
//		case MZ2_WIDOW2_BEAM_SWEEP_9:
//		case MZ2_WIDOW2_BEAM_SWEEP_10:
//		case MZ2_WIDOW2_BEAM_SWEEP_11:
//			dl->radius = 300 + (rand()&100);
//			dl->color[0] = 1;dl->color[1] = 1;dl->color[2] = 0;
//			dl->die = cl.time + 200;
//			break;
////	   ROGUE
////	   ======
//
////	   --- Xian's shit ends ---
//
//		}
	}

	/*
	===============
	CL_AddDLights

	===============
	*/
	static void AddDLights() {
		int i;
		cdlight_t[] dl;

		dl = cl_dlights;

		//	  =====
		//	  PGM
		if (vidref_val == VIDREF_GL) {
			for (i = 0; i < MAX_DLIGHTS; i++) {
				if (dl[i].radius == 0.0f)
					continue;
				V.AddLight(dl[i].origin, dl[i].radius, dl[i].color[0], dl[i].color[1], dl[i].color[2]);
			}
		} else {
			for (i = 0; i < MAX_DLIGHTS; i++) {
				if (dl[i].radius == 0.0f)
					continue;

				// negative light in software. only black allowed
				if ((dl[i].color[0] < 0) || (dl[i].color[1] < 0) || (dl[i].color[2] < 0)) {
					dl[i].radius = - (dl[i].radius);
					dl[i].color[0] = 1;
					dl[i].color[1] = 1;
					dl[i].color[2] = 1;
				}
				V.AddLight(dl[i].origin, dl[i].radius, dl[i].color[0], dl[i].color[1], dl[i].color[2]);
			}
		}
		//	  PGM
		//	  =====
	}

//
//
//	/*
//	==============================================================
//
//	PARTICLE MANAGEMENT
//
//	==============================================================
//	*/
//
//	/*
////	   THIS HAS BEEN RELOCATED TO CLIENT.H
//	typedef struct particle_s
//	{
//		struct particle_s	*next;
//
//		float		time;
//
//		float [] 		org;
//		float [] 		vel;
//		float [] 		accel;
//		float		color;
//		float		colorvel;
//		float		alpha;
//		float		alphavel;
//	} cparticle_t;
//
//
	static final int PARTICLE_GRAVITY = 40;
//	*/
//
//	cparticle_t	*active_particles, *free_particles;
	static cparticle_t active_particles, free_particles;
//
	static cparticle_t[] particles = new cparticle_t[MAX_PARTICLES];
	static {
		for (int i = 0; i < particles.length; i++)
			particles[i] = new cparticle_t();
	}
	static int cl_numparticles = MAX_PARTICLES;

	/*
	===============
	CL_ClearParticles
	===============
	*/
	static void ClearParticles()
	{
		int		i;
	
		free_particles = particles[0];
		active_particles = null;

		for (i=0 ; i<particles.length - 1; i++)
			particles[i].next = particles[i+1];
		particles[particles.length - 1].next = null;
	}
//
//
//	/*
//	===============
//	CL_ParticleEffect
//
//	Wall impact puffs
//	===============
//	*/
	static void ParticleEffect (float []  org, float []  dir, int color, int count)
	{
//		int			i, j;
//		cparticle_t	*p;
//		float		d;
//
//		for (i=0 ; i<count ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = color + (rand()&7);
//
//			d = rand()&31;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()&7)-4) + d*dir[j];
//				p->vel[j] = crand()*20;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (0.5 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_ParticleEffect2
//	===============
//	*/
	static void ParticleEffect2 (float []  org, float []  dir, int color, int count)
	{
//		int			i, j;
//		cparticle_t	*p;
//		float		d;
//
//		for (i=0 ; i<count ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = color;
//
//			d = rand()&7;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()&7)-4) + d*dir[j];
//				p->vel[j] = crand()*20;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (0.5 + frand()*0.3);
//		}
	}
//
//
////	   RAFAEL
//	/*
//	===============
//	CL_ParticleEffect3
//	===============
//	*/
	static void ParticleEffect3 (float []  org, float []  dir, int color, int count)
	{
//		int			i, j;
//		cparticle_t	*p;
//		float		d;
//
//		for (i=0 ; i<count ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = color;
//
//			d = rand()&7;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()&7)-4) + d*dir[j];
//				p->vel[j] = crand()*20;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (0.5 + frand()*0.3);
//		}
	}
//
//	/*
//	===============
//	CL_TeleporterParticles
//	===============
//	*/
	static void TeleporterParticles (entity_state_t ent)
	{
//		int			i, j;
//		cparticle_t	*p;
//
//		for (i=0 ; i<8 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = 0xdb;
//
//			for (j=0 ; j<2 ; j++)
//			{
//				p->org[j] = ent->origin[j] - 16 + (rand()&31);
//				p->vel[j] = crand()*14;
//			}
//
//			p->org[2] = ent->origin[2] - 8 + (rand()&7);
//			p->vel[2] = 80 + (rand()&7);
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -0.5;
//		}
	}
//
//
//	/*
//	===============
//	CL_LogoutEffect
//
//	===============
//	*/
	static void LogoutEffect (float []  org, int type)
	{
//		int			i, j;
//		cparticle_t	*p;
//
//		for (i=0 ; i<500 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//
//			if (type == MZ_LOGIN)
//				p->color = 0xd0 + (rand()&7);	// green
//			else if (type == MZ_LOGOUT)
//				p->color = 0x40 + (rand()&7);	// red
//			else
//				p->color = 0xe0 + (rand()&7);	// yellow
//
//			p->org[0] = org[0] - 16 + frand()*32;
//			p->org[1] = org[1] - 16 + frand()*32;
//			p->org[2] = org[2] - 24 + frand()*56;
//
//			for (j=0 ; j<3 ; j++)
//				p->vel[j] = crand()*20;
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (1.0 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_ItemRespawnParticles
//
//	===============
//	*/
	static void ItemRespawnParticles (float []  org)
	{
//		int			i, j;
//		cparticle_t	*p;
//
//		for (i=0 ; i<64 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//
//			p->color = 0xd4 + (rand()&3);	// green
//
//			p->org[0] = org[0] + crand()*8;
//			p->org[1] = org[1] + crand()*8;
//			p->org[2] = org[2] + crand()*8;
//
//			for (j=0 ; j<3 ; j++)
//				p->vel[j] = crand()*8;
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY*0.2;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (1.0 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_ExplosionParticles
//	===============
//	*/
	static void ExplosionParticles (float []  org)
	{
//		int			i, j;
//		cparticle_t	*p;
//
//		for (i=0 ; i<256 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = 0xe0 + (rand()&7);
//
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()%32)-16);
//				p->vel[j] = (rand()%384)-192;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -0.8 / (0.5 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_BigTeleportParticles
//	===============
//	*/
	static void BigTeleportParticles (float []  org)
	{
//		int			i;
//		cparticle_t	*p;
//		float		angle, dist;
//		static int colortable[4] = {2*8,13*8,21*8,18*8};
//
//		for (i=0 ; i<4096 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//
//			p->color = colortable[rand()&3];
//
//			angle = M_PI*2*(rand()&1023)/1023.0;
//			dist = rand()&31;
//			p->org[0] = org[0] + cos(angle)*dist;
//			p->vel[0] = cos(angle)*(70+(rand()&63));
//			p->accel[0] = -cos(angle)*100;
//
//			p->org[1] = org[1] + sin(angle)*dist;
//			p->vel[1] = sin(angle)*(70+(rand()&63));
//			p->accel[1] = -sin(angle)*100;
//
//			p->org[2] = org[2] + 8 + (rand()%90);
//			p->vel[2] = -100 + (rand()&31);
//			p->accel[2] = PARTICLE_GRAVITY*4;
//			p->alpha = 1.0;
//
//			p->alphavel = -0.3 / (0.5 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_BlasterParticles
//
//	Wall impact puffs
//	===============
//	*/
	static void BlasterParticles (float []  org, float []  dir)
	{
//		int			i, j;
//		cparticle_t	*p;
//		float		d;
//		int			count;
//
//		count = 40;
//		for (i=0 ; i<count ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = 0xe0 + (rand()&7);
//
//			d = rand()&15;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()&7)-4) + d*dir[j];
//				p->vel[j] = dir[j] * 30 + crand()*40;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -1.0 / (0.5 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_BlasterTrail
//
//	===============
//	*/
	static void BlasterTrail (float []  start, float []  end)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		int			dec;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 5;
//		VectorScale (vec, 5, vec);
//
//		// FIXME: this is a really silly way to have a loop
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//			VectorClear (p->accel);
//		
//			p->time = cl.time;
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (0.3+frand()*0.2);
//			p->color = 0xe0;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand();
//				p->vel[j] = crand()*5;
//				p->accel[j] = 0;
//			}
//
//			VectorAdd (move, vec, move);
//		}
	}
//
//	/*
//	===============
//	CL_QuadTrail
//
//	===============
//	*/
	static void QuadTrail (float []  start, float []  end)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		int			dec;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 5;
//		VectorScale (vec, 5, vec);
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//			VectorClear (p->accel);
//		
//			p->time = cl.time;
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (0.8+frand()*0.2);
//			p->color = 115;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand()*16;
//				p->vel[j] = crand()*5;
//				p->accel[j] = 0;
//			}
//
//			VectorAdd (move, vec, move);
//		}
	}
//
//	/*
//	===============
//	CL_FlagTrail
//
//	===============
//	*/
 	static void FlagTrail (float []  start, float []  end, float color)
	{
		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		int			dec;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 5;
//		VectorScale (vec, 5, vec);
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//			VectorClear (p->accel);
//		
//			p->time = cl.time;
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (0.8+frand()*0.2);
//			p->color = color;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand()*16;
//				p->vel[j] = crand()*5;
//				p->accel[j] = 0;
//			}
//
//			VectorAdd (move, vec, move);
//		}
	}
//
//	/*
//	===============
//	CL_DiminishingTrail
//
//	===============
//	*/
	static void DiminishingTrail (float []  start, float []  end, centity_t  old, int flags)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		float		dec;
//		float		orgscale;
//		float		velscale;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 0.5;
//		VectorScale (vec, dec, vec);
//
//		if (old->trailcount > 900)
//		{
//			orgscale = 4;
//			velscale = 15;
//		}
//		else if (old->trailcount > 800)
//		{
//			orgscale = 2;
//			velscale = 10;
//		}
//		else
//		{
//			orgscale = 1;
//			velscale = 5;
//		}
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//
//			// drop less particles as it flies
//			if ((rand()&1023) < old->trailcount)
//			{
//				p = free_particles;
//				free_particles = p->next;
//				p->next = active_particles;
//				active_particles = p;
//				VectorClear (p->accel);
//		
//				p->time = cl.time;
//
//				if (flags & EF_GIB)
//				{
//					p->alpha = 1.0;
//					p->alphavel = -1.0 / (1+frand()*0.4);
//					p->color = 0xe8 + (rand()&7);
//					for (j=0 ; j<3 ; j++)
//					{
//						p->org[j] = move[j] + crand()*orgscale;
//						p->vel[j] = crand()*velscale;
//						p->accel[j] = 0;
//					}
//					p->vel[2] -= PARTICLE_GRAVITY;
//				}
//				else if (flags & EF_GREENGIB)
//				{
//					p->alpha = 1.0;
//					p->alphavel = -1.0 / (1+frand()*0.4);
//					p->color = 0xdb + (rand()&7);
//					for (j=0; j< 3; j++)
//					{
//						p->org[j] = move[j] + crand()*orgscale;
//						p->vel[j] = crand()*velscale;
//						p->accel[j] = 0;
//					}
//					p->vel[2] -= PARTICLE_GRAVITY;
//				}
//				else
//				{
//					p->alpha = 1.0;
//					p->alphavel = -1.0 / (1+frand()*0.2);
//					p->color = 4 + (rand()&7);
//					for (j=0 ; j<3 ; j++)
//					{
//						p->org[j] = move[j] + crand()*orgscale;
//						p->vel[j] = crand()*velscale;
//					}
//					p->accel[2] = 20;
//				}
//			}
//
//			old->trailcount -= 5;
//			if (old->trailcount < 100)
//				old->trailcount = 100;
//			VectorAdd (move, vec, move);
//		}
	}
//
 
//	/*
//	===============
//	CL_RocketTrail
//
//	===============
//	*/
	static void RocketTrail (float []  start, float []  end, centity_t  old)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		float		dec;
//
//		// smoke
//		CL_DiminishingTrail (start, end, old, EF_ROCKET);
//
//		// fire
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 1;
//		VectorScale (vec, dec, vec);
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//
//			if ( (rand()&7) == 0)
//			{
//				p = free_particles;
//				free_particles = p->next;
//				p->next = active_particles;
//				active_particles = p;
//			
//				VectorClear (p->accel);
//				p->time = cl.time;
//
//				p->alpha = 1.0;
//				p->alphavel = -1.0 / (1+frand()*0.2);
//				p->color = 0xdc + (rand()&3);
//				for (j=0 ; j<3 ; j++)
//				{
//					p->org[j] = move[j] + crand()*5;
//					p->vel[j] = crand()*20;
//				}
//				p->accel[2] = -PARTICLE_GRAVITY;
//			}
//			VectorAdd (move, vec, move);
//		}
	}
//
//	/*
//	===============
//	CL_RailTrail
//
//	===============
//	*/
	static void RailTrail (float []  start, float []  end)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		float		dec;
//		float [] 		right, up;
//		int			i;
//		float		d, c, s;
//		float [] 		dir;
//		byte		clr = 0x74;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		MakeNormalVectors (vec, right, up);
//
//		for (i=0 ; i<len ; i++)
//		{
//			if (!free_particles)
//				return;
//
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//		
//			p->time = cl.time;
//			VectorClear (p->accel);
//
//			d = i * 0.1;
//			c = cos(d);
//			s = sin(d);
//
//			VectorScale (right, c, dir);
//			VectorMA (dir, s, up, dir);
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (1+frand()*0.2);
//			p->color = clr + (rand()&7);
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + dir[j]*3;
//				p->vel[j] = dir[j]*6;
//			}
//
//			VectorAdd (move, vec, move);
//		}
//
//		dec = 0.75;
//		VectorScale (vec, dec, vec);
//		VectorCopy (start, move);
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			VectorClear (p->accel);
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (0.6+frand()*0.2);
//			p->color = 0x0 + rand()&15;
//
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand()*3;
//				p->vel[j] = crand()*3;
//				p->accel[j] = 0;
//			}
//
//			VectorAdd (move, vec, move);
//		}
	}
//
////	   RAFAEL
//	/*
//	===============
//	CL_IonripperTrail
//	===============
//	*/
	static void IonripperTrail (float []  start, float []  ent)
	{
//		float [] 	move;
//		float [] 	vec;
//		float	len;
//		int		j;
//		cparticle_t *p;
//		int		dec;
//		int     left = 0;
//
//		VectorCopy (start, move);
//		VectorSubtract (ent, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 5;
//		VectorScale (vec, 5, vec);
//
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//			VectorClear (p->accel);
//
//			p->time = cl.time;
//			p->alpha = 0.5;
//			p->alphavel = -1.0 / (0.3 + frand() * 0.2);
//			p->color = 0xe4 + (rand()&3);
//
//			for (j=0; j<3; j++)
//			{
//				p->org[j] = move[j];
//				p->accel[j] = 0;
//			}
//			if (left)
//			{
//				left = 0;
//				p->vel[0] = 10;
//			}
//			else 
//			{
//				left = 1;
//				p->vel[0] = -10;
//			}
//
//			p->vel[1] = 0;
//			p->vel[2] = 0;
//
//			VectorAdd (move, vec, move);
//		}
	}
//
//
//	/*
//	===============
//	CL_BubbleTrail
//
//	===============
//	*/
	static void BubbleTrail (float []  start, float []  end)
	{
//		float [] 		move;
//		float [] 		vec;
//		float		len;
//		int			i, j;
//		cparticle_t	*p;
//		float		dec;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 32;
//		VectorScale (vec, dec, vec);
//
//		for (i=0 ; i<len ; i+=dec)
//		{
//			if (!free_particles)
//				return;
//
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			VectorClear (p->accel);
//			p->time = cl.time;
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (1+frand()*0.2);
//			p->color = 4 + (rand()&7);
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand()*2;
//				p->vel[j] = crand()*5;
//			}
//			p->vel[2] += 6;
//
//			VectorAdd (move, vec, move);
//		}
	}
//
//
//	/*
//	===============
//	CL_FlyParticles
//	===============
//	*/
//
//	#define	BEAMLENGTH			16
	static  void FlyParticles (float []  origin, int count)
	{
//		int			i;
//		cparticle_t	*p;
//		float		angle;
//		float		sr, sp, sy, cr, cp, cy;
//		float [] 		forward;
//		float		dist = 64;
//		float		ltime;
//
//
//		if (count > NUMVERTEXNORMALS)
//			count = NUMVERTEXNORMALS;
//
//		if (!avelocities[0][0])
//		{
//			for (i=0 ; i<NUMVERTEXNORMALS*3 ; i++)
//				avelocities[0][i] = (rand()&255) * 0.01;
//		}
//
//
//		ltime = (float)cl.time / 1000.0;
//		for (i=0 ; i<count ; i+=2)
//		{
//			angle = ltime * avelocities[i][0];
//			sy = sin(angle);
//			cy = cos(angle);
//			angle = ltime * avelocities[i][1];
//			sp = sin(angle);
//			cp = cos(angle);
//			angle = ltime * avelocities[i][2];
//			sr = sin(angle);
//			cr = cos(angle);
//	
//			forward[0] = cp*cy;
//			forward[1] = cp*sy;
//			forward[2] = -sp;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//
//			dist = sin(ltime + i)*64;
//			p->org[0] = origin[0] + bytedirs[i][0]*dist + forward[0]*BEAMLENGTH;
//			p->org[1] = origin[1] + bytedirs[i][1]*dist + forward[1]*BEAMLENGTH;
//			p->org[2] = origin[2] + bytedirs[i][2]*dist + forward[2]*BEAMLENGTH;
//
//			VectorClear (p->vel);
//			VectorClear (p->accel);
//
//			p->color = 0;
//			p->colorvel = 0;
//
//			p->alpha = 1;
//			p->alphavel = -100;
//		}
	}
//
	static void FlyEffect (centity_t  ent, float []  origin)
 	{
//		int		n;
//		int		count;
//		int		starttime;
//
//		if (ent->fly_stoptime < cl.time)
//		{
//			starttime = cl.time;
//			ent->fly_stoptime = cl.time + 60000;
//		}
//		else
//		{
//			starttime = ent->fly_stoptime - 60000;
//		}
//
//		n = cl.time - starttime;
//		if (n < 20000)
//			count = n * 162 / 20000.0;
//		else
//		{
//			n = ent->fly_stoptime - cl.time;
//			if (n < 20000)
//				count = n * 162 / 20000.0;
//			else
//				count = 162;
//		}
//
//		CL_FlyParticles (origin, count);
	}
//
//
//	/*
//	===============
//	CL_BfgParticles
//	===============
//	*/
//
//	#define	BEAMLENGTH			16
	static  void BfgParticles (entity_t  ent)
	{
//		int			i;
//		cparticle_t	*p;
//		float		angle;
//		float		sr, sp, sy, cr, cp, cy;
//		float [] 		forward;
//		float		dist = 64;
//		float [] 		v;
//		float		ltime;
//	
//		if (!avelocities[0][0])
//		{
//			for (i=0 ; i<NUMVERTEXNORMALS*3 ; i++)
//				avelocities[0][i] = (rand()&255) * 0.01;
//		}
//
//
//		ltime = (float)cl.time / 1000.0;
//		for (i=0 ; i<NUMVERTEXNORMALS ; i++)
//		{
//			angle = ltime * avelocities[i][0];
//			sy = sin(angle);
//			cy = cos(angle);
//			angle = ltime * avelocities[i][1];
//			sp = sin(angle);
//			cp = cos(angle);
//			angle = ltime * avelocities[i][2];
//			sr = sin(angle);
//			cr = cos(angle);
//	
//			forward[0] = cp*cy;
//			forward[1] = cp*sy;
//			forward[2] = -sp;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//
//			dist = sin(ltime + i)*64;
//			p->org[0] = ent->origin[0] + bytedirs[i][0]*dist + forward[0]*BEAMLENGTH;
//			p->org[1] = ent->origin[1] + bytedirs[i][1]*dist + forward[1]*BEAMLENGTH;
//			p->org[2] = ent->origin[2] + bytedirs[i][2]*dist + forward[2]*BEAMLENGTH;
//
//			VectorClear (p->vel);
//			VectorClear (p->accel);
//
//			VectorSubtract (p->org, ent->origin, v);
//			dist = VectorLength(v) / 90.0;
//			p->color = floor (0xd0 + dist * 7);
//			p->colorvel = 0;
//
//			p->alpha = 1.0 - dist;
//			p->alphavel = -100;
//		}
	}
//
//
//	/*
//	===============
//	CL_TrapParticles
//	===============
//	*/
////	   RAFAEL
	static void TrapParticles (entity_t ent)
	{
//		float [] 		move;
//		float [] 		vec;
//		float [] 		start, end;
//		float		len;
//		int			j;
//		cparticle_t	*p;
//		int			dec;
//
//		ent->origin[2]-=14;
//		VectorCopy (ent->origin, start);
//		VectorCopy (ent->origin, end);
//		end[2]+=64;
//
//		VectorCopy (start, move);
//		VectorSubtract (end, start, vec);
//		len = VectorNormalize (vec);
//
//		dec = 5;
//		VectorScale (vec, 5, vec);
//
//		// FIXME: this is a really silly way to have a loop
//		while (len > 0)
//		{
//			len -= dec;
//
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//			VectorClear (p->accel);
//		
//			p->time = cl.time;
//
//			p->alpha = 1.0;
//			p->alphavel = -1.0 / (0.3+frand()*0.2);
//			p->color = 0xe0;
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = move[j] + crand();
//				p->vel[j] = crand()*15;
//				p->accel[j] = 0;
//			}
//			p->accel[2] = PARTICLE_GRAVITY;
//
//			VectorAdd (move, vec, move);
//		}
//
//		{
//
//	
//		int			i, j, k;
//		cparticle_t	*p;
//		float		vel;
//		float [] 		dir;
//		float [] 		org;
//
//	
//		ent->origin[2]+=14;
//		VectorCopy (ent->origin, org);
//
//
//		for (i=-2 ; i<=2 ; i+=4)
//			for (j=-2 ; j<=2 ; j+=4)
//				for (k=-2 ; k<=4 ; k+=4)
//				{
//					if (!free_particles)
//						return;
//					p = free_particles;
//					free_particles = p->next;
//					p->next = active_particles;
//					active_particles = p;
//
//					p->time = cl.time;
//					p->color = 0xe0 + (rand()&3);
//
//					p->alpha = 1.0;
//					p->alphavel = -1.0 / (0.3 + (rand()&7) * 0.02);
//				
//					p->org[0] = org[0] + i + ((rand()&23) * crand());
//					p->org[1] = org[1] + j + ((rand()&23) * crand());
//					p->org[2] = org[2] + k + ((rand()&23) * crand());
//	
//					dir[0] = j * 8;
//					dir[1] = i * 8;
//					dir[2] = k * 8;
//	
//					VectorNormalize (dir);						
//					vel = 50 + rand()&63;
//					VectorScale (dir, vel, p->vel);
//
//					p->accel[0] = p->accel[1] = 0;
//					p->accel[2] = -PARTICLE_GRAVITY;
//				}
//		}
	}
//
//
//	/*
//	===============
//	CL_BFGExplosionParticles
//	===============
//	*/
////	  FIXME combined with CL_ExplosionParticles
	static void BFGExplosionParticles (float []  org)
	{
//		int			i, j;
//		cparticle_t	*p;
//
//		for (i=0 ; i<256 ; i++)
//		{
//			if (!free_particles)
//				return;
//			p = free_particles;
//			free_particles = p->next;
//			p->next = active_particles;
//			active_particles = p;
//
//			p->time = cl.time;
//			p->color = 0xd0 + (rand()&7);
//
//			for (j=0 ; j<3 ; j++)
//			{
//				p->org[j] = org[j] + ((rand()%32)-16);
//				p->vel[j] = (rand()%384)-192;
//			}
//
//			p->accel[0] = p->accel[1] = 0;
//			p->accel[2] = -PARTICLE_GRAVITY;
//			p->alpha = 1.0;
//
//			p->alphavel = -0.8 / (0.5 + frand()*0.3);
//		}
	}
//
//
//	/*
//	===============
//	CL_TeleportParticles
//
//	===============
//	*/
	static void TeleportParticles (float [] org)
	{
//		int			i, j, k;
//		cparticle_t	*p;
//		float		vel;
//		float [] 		dir;
//
//		for (i=-16 ; i<=16 ; i+=4)
//			for (j=-16 ; j<=16 ; j+=4)
//				for (k=-16 ; k<=32 ; k+=4)
//				{
//					if (!free_particles)
//						return;
//					p = free_particles;
//					free_particles = p->next;
//					p->next = active_particles;
//					active_particles = p;
//
//					p->time = cl.time;
//					p->color = 7 + (rand()&7);
//
//					p->alpha = 1.0;
//					p->alphavel = -1.0 / (0.3 + (rand()&7) * 0.02);
//				
//					p->org[0] = org[0] + i + (rand()&3);
//					p->org[1] = org[1] + j + (rand()&3);
//					p->org[2] = org[2] + k + (rand()&3);
//	
//					dir[0] = j*8;
//					dir[1] = i*8;
//					dir[2] = k*8;
//	
//					VectorNormalize (dir);						
//					vel = 50 + (rand()&63);
//					VectorScale (dir, vel, p->vel);
//
//					p->accel[0] = p->accel[1] = 0;
//					p->accel[2] = -PARTICLE_GRAVITY;
//				}
	}
//
//
//	/*
//	===============
//	CL_AddParticles
//	===============
//	*/
	static void AddParticles ()
	{
//		cparticle_t		*p, *next;
//		float			alpha;
//		float			time, time2;
//		float [] 			org;
//		int				color;
//		cparticle_t		*active, *tail;
//
//		active = NULL;
//		tail = NULL;
//
//		for (p=active_particles ; p ; p=next)
//		{
//			next = p->next;
//
//			// PMM - added INSTANT_PARTICLE handling for heat beam
//			if (p->alphavel != INSTANT_PARTICLE)
//			{
//				time = (cl.time - p->time)*0.001;
//				alpha = p->alpha + time*p->alphavel;
//				if (alpha <= 0)
//				{	// faded out
//					p->next = free_particles;
//					free_particles = p;
//					continue;
//				}
//			}
//			else
//			{
//				alpha = p->alpha;
//			}
//
//			p->next = NULL;
//			if (!tail)
//				active = tail = p;
//			else
//			{
//				tail->next = p;
//				tail = p;
//			}
//
//			if (alpha > 1.0)
//				alpha = 1;
//			color = p->color;
//
//			time2 = time*time;
//
//			org[0] = p->org[0] + p->vel[0]*time + p->accel[0]*time2;
//			org[1] = p->org[1] + p->vel[1]*time + p->accel[1]*time2;
//			org[2] = p->org[2] + p->vel[2]*time + p->accel[2]*time2;
//
//			V_AddParticle (org, color, alpha);
//			// PMM
//			if (p->alphavel == INSTANT_PARTICLE)
//			{
//				p->alphavel = 0.0;
//				p->alpha = 0.0;
//			}
//		}
//
//		active_particles = active;
	}
//
//
//	/*
//	==============
//	CL_EntityEvent
//
//	An entity has just been parsed that has an event value
//
//	the female events are there for backwards compatability
//	==============
//	*/
//	extern struct sfx_s	*cl_sfx_footsteps[4];
//
	static void EntityEvent (entity_state_t ent)
	{
//		switch (ent->event)
//		{
//		case EV_ITEM_RESPAWN:
//			S_StartSound (NULL, ent->number, CHAN_WEAPON, S_RegisterSound("items/respawn1.wav"), 1, ATTN_IDLE, 0);
//			CL_ItemRespawnParticles (ent->origin);
//			break;
//		case EV_PLAYER_TELEPORT:
//			S_StartSound (NULL, ent->number, CHAN_WEAPON, S_RegisterSound("misc/tele1.wav"), 1, ATTN_IDLE, 0);
//			CL_TeleportParticles (ent->origin);
//			break;
//		case EV_FOOTSTEP:
//			if (cl_footsteps->value)
//				S_StartSound (NULL, ent->number, CHAN_BODY, cl_sfx_footsteps[rand()&3], 1, ATTN_NORM, 0);
//			break;
//		case EV_FALLSHORT:
//			S_StartSound (NULL, ent->number, CHAN_AUTO, S_RegisterSound ("player/land1.wav"), 1, ATTN_NORM, 0);
//			break;
//		case EV_FALL:
//			S_StartSound (NULL, ent->number, CHAN_AUTO, S_RegisterSound ("*fall2.wav"), 1, ATTN_NORM, 0);
//			break;
//		case EV_FALLFAR:
//			S_StartSound (NULL, ent->number, CHAN_AUTO, S_RegisterSound ("*fall1.wav"), 1, ATTN_NORM, 0);
//			break;
//		}
	}


	/*
	==============
	CL_ClearEffects

	==============
	*/
	static void ClearEffects() {
		CL.ClearParticles();
		CL.ClearDlights();
		CL.ClearLightStyles();
	}

}
