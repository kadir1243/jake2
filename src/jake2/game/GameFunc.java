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

// Created on 18.11.2003 by RST.
// $Id: GameFunc.java,v 1.3 2003-12-04 20:35:26 rst Exp $

package jake2.game;

public class GameFunc extends Game {

	/*
	=========================================================
	
	  PLATS
	
	  movement options:
	
	  linear
	  smooth start, hard stop
	  smooth start, smooth stop
	
	  start
	  end
	  acceleration
	  speed
	  deceleration
	  begin sound
	  end sound
	  target fired when reaching end
	  wait at end
	
	  object characteristics that use move segments
	  ---------------------------------------------
	  movetype_push, or movetype_stop
	  action when touched
	  action when blocked
	  action when used
		disabled?
	  auto trigger spawning
	
	
	=========================================================
	*/

	public final static int PLAT_LOW_TRIGGER = 1;

	public final static int STATE_TOP = 0;
	public final static int STATE_BOTTOM = 1;
	public final static int STATE_UP = 2;
	public final static int STATE_DOWN = 3;

	public final static int DOOR_START_OPEN = 1;
	public final static int DOOR_REVERSE = 2;
	public final static int DOOR_CRUSHER = 4;
	public final static int DOOR_NOMONSTER = 8;
	public final static int DOOR_TOGGLE = 32;
	public final static int DOOR_X_AXIS = 64;
	public final static int DOOR_Y_AXIS = 128;

	//
	//	   Support routines for movement (changes in origin using velocity)
	//

	static EntThinkAdapter Move_Done = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			VectorClear(ent.velocity);
			ent.moveinfo.endfunc.think(ent);
			return true;
		}
	};

	static EntThinkAdapter Move_Final = new EntThinkAdapter() {
		public boolean think(edict_t ent) {

			if (ent.moveinfo.remaining_distance == 0) {
				Move_Done.think(ent);
				return true;
			}

			VectorScale(ent.moveinfo.dir, ent.moveinfo.remaining_distance / FRAMETIME, ent.velocity);

			ent.think = Move_Done;
			ent.nextthink = level.time + FRAMETIME;
			return true;
		}
	};

	static EntThinkAdapter Move_Begin = new EntThinkAdapter() {
		public boolean think(edict_t ent) {

			float frames;

			if ((ent.moveinfo.speed * FRAMETIME) >= ent.moveinfo.remaining_distance) {
				Move_Final.think(ent);
				return true;
			}
			VectorScale(ent.moveinfo.dir, ent.moveinfo.speed, ent.velocity);
			frames = (float) Math.floor((ent.moveinfo.remaining_distance / ent.moveinfo.speed) / FRAMETIME);
			ent.moveinfo.remaining_distance -= frames * ent.moveinfo.speed * FRAMETIME;
			ent.nextthink = level.time + (frames * FRAMETIME);
			ent.think = Move_Final;
			return true;
		}
	};


	static void Move_Calc(edict_t ent, float[] dest, EntThinkAdapter func) {
		VectorClear(ent.velocity);
		VectorSubtract(dest, ent.s.origin, ent.moveinfo.dir);
		ent.moveinfo.remaining_distance = VectorNormalize(ent.moveinfo.dir);
		ent.moveinfo.endfunc = func;

		if (ent.moveinfo.speed == ent.moveinfo.accel && ent.moveinfo.speed == ent.moveinfo.decel) {
			if (level.current_entity == ((ent.flags & FL_TEAMSLAVE) != 0 ? ent.teammaster : ent)) {
				Move_Begin.think(ent);
			} else {
				ent.nextthink = level.time + FRAMETIME;
				ent.think = Move_Begin;
			}
		} else {
			// accelerative
			ent.moveinfo.current_speed = 0;
			ent.think = Think_AccelMove;
			ent.nextthink = level.time + FRAMETIME;
		}
	}

	//
	//	   Support routines for angular movement (changes in angle using avelocity)
	//

	static EntThinkAdapter AngleMove_Done = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			VectorClear(ent.avelocity);
			ent.moveinfo.endfunc.think(ent);
			return true;
		}
	};

	static EntThinkAdapter AngleMove_Final = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			float[] move={0,0,0};

			if (ent.moveinfo.state == STATE_UP)
				VectorSubtract(ent.moveinfo.end_angles, ent.s.angles, move);
			else
				VectorSubtract(ent.moveinfo.start_angles, ent.s.angles, move);

			if (VectorCompare(move, vec3_origin) != 0) {
				AngleMove_Done.think(ent);
				return true;
			}

			VectorScale(move, 1.0f / FRAMETIME, ent.avelocity);

			ent.think = AngleMove_Done;
			ent.nextthink = level.time + FRAMETIME;
			return true;
		}
	};

	static EntThinkAdapter AngleMove_Begin = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			float[] destdelta={0,0,0};
			float len;
			float traveltime;
			float frames;

			// set destdelta to the vector needed to move
			if (ent.moveinfo.state == STATE_UP)
				VectorSubtract(ent.moveinfo.end_angles, ent.s.angles, destdelta);
			else
				VectorSubtract(ent.moveinfo.start_angles, ent.s.angles, destdelta);

			// calculate length of vector
			len = VectorLength(destdelta);

			// divide by speed to get time to reach dest
			traveltime = len / ent.moveinfo.speed;

			if (traveltime < FRAMETIME) {
				AngleMove_Final.think(ent);
				return true;
			}

			frames = (float) (Math.floor(traveltime / FRAMETIME));

			// scale the destdelta vector by the time spent traveling to get velocity
			VectorScale(destdelta, 1.0f / traveltime, ent.avelocity);

			// set nextthink to trigger a think when dest is reached
			ent.nextthink = level.time + frames * FRAMETIME;
			ent.think = AngleMove_Final;
			return true;
		}
	};

	static void AngleMove_Calc(edict_t ent, EntThinkAdapter func) {
		VectorClear(ent.avelocity);
		ent.moveinfo.endfunc = func;
		if (level.current_entity == ((ent.flags & FL_TEAMSLAVE) != 0 ? ent.teammaster : ent)) {
			AngleMove_Begin.think(ent);
		} else {
			ent.nextthink = level.time + FRAMETIME;
			ent.think = AngleMove_Begin;
		}
	}

	/*
	==============
	Think_AccelMove
	
	The team has completed a frame of movement, so
	change the speed for the next frame
	==============
	*/
	static float AccelerationDistance(float target, float rate) {
		return target * ((target / rate) + 1) / 2;
	};

	static void plat_CalcAcceleratedMove(moveinfo_t moveinfo) {
		float accel_dist;
		float decel_dist;

		moveinfo.move_speed = moveinfo.speed;

		if (moveinfo.remaining_distance < moveinfo.accel) {
			moveinfo.current_speed = moveinfo.remaining_distance;
			return;
		}

		accel_dist = AccelerationDistance(moveinfo.speed, moveinfo.accel);
		decel_dist = AccelerationDistance(moveinfo.speed, moveinfo.decel);

		if ((moveinfo.remaining_distance - accel_dist - decel_dist) < 0) {
			float f;

			f = (moveinfo.accel + moveinfo.decel) / (moveinfo.accel * moveinfo.decel);
			moveinfo.move_speed = (float) ((-2 + Math.sqrt(4 - 4 * f * (-2 * moveinfo.remaining_distance))) / (2 * f));
			decel_dist = AccelerationDistance(moveinfo.move_speed, moveinfo.decel);
		}

		moveinfo.decel_distance = decel_dist;
	};

	static void plat_Accelerate(moveinfo_t moveinfo) {
		// are we decelerating?
		if (moveinfo.remaining_distance <= moveinfo.decel_distance) {
			if (moveinfo.remaining_distance < moveinfo.decel_distance) {
				if (moveinfo.next_speed != 0) {
					moveinfo.current_speed = moveinfo.next_speed;
					moveinfo.next_speed = 0;
					return;
				}
				if (moveinfo.current_speed > moveinfo.decel)
					moveinfo.current_speed -= moveinfo.decel;
			}
			return;
		}

		// are we at full speed and need to start decelerating during this move?
		if (moveinfo.current_speed == moveinfo.move_speed)
			if ((moveinfo.remaining_distance - moveinfo.current_speed) < moveinfo.decel_distance) {
				float p1_distance;
				float p2_distance;
				float distance;

				p1_distance = moveinfo.remaining_distance - moveinfo.decel_distance;
				p2_distance = moveinfo.move_speed * (1.0f - (p1_distance / moveinfo.move_speed));
				distance = p1_distance + p2_distance;
				moveinfo.current_speed = moveinfo.move_speed;
				moveinfo.next_speed = moveinfo.move_speed - moveinfo.decel * (p2_distance / distance);
				return;
			}

		// are we accelerating?
		if (moveinfo.current_speed < moveinfo.speed) {
			float old_speed;
			float p1_distance;
			float p1_speed;
			float p2_distance;
			float distance;

			old_speed = moveinfo.current_speed;

			// figure simple acceleration up to move_speed
			moveinfo.current_speed += moveinfo.accel;
			if (moveinfo.current_speed > moveinfo.speed)
				moveinfo.current_speed = moveinfo.speed;

			// are we accelerating throughout this entire move?
			if ((moveinfo.remaining_distance - moveinfo.current_speed) >= moveinfo.decel_distance)
				return;

			// during this move we will accelrate from current_speed to move_speed
			// and cross over the decel_distance; figure the average speed for the
			// entire move
			p1_distance = moveinfo.remaining_distance - moveinfo.decel_distance;
			p1_speed = (old_speed + moveinfo.move_speed) / 2.0f;
			p2_distance = moveinfo.move_speed * (1.0f - (p1_distance / p1_speed));
			distance = p1_distance + p2_distance;
			moveinfo.current_speed = (p1_speed * (p1_distance / distance)) + (moveinfo.move_speed * (p2_distance / distance));
			moveinfo.next_speed = moveinfo.move_speed - moveinfo.decel * (p2_distance / distance);
			return;
		}

		// we are at constant velocity (move_speed)
		return;
	};

	static EntThinkAdapter Think_AccelMove = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			ent.moveinfo.remaining_distance -= ent.moveinfo.current_speed;

			if (ent.moveinfo.current_speed == 0) // starting or blocked
				plat_CalcAcceleratedMove(ent.moveinfo);

			plat_Accelerate(ent.moveinfo);

			// will the entire move complete on next frame?
			if (ent.moveinfo.remaining_distance <= ent.moveinfo.current_speed) {
				Move_Final.think(ent);
				return true;
			}

			VectorScale(ent.moveinfo.dir, ent.moveinfo.current_speed * 10, ent.velocity);
			ent.nextthink = level.time + FRAMETIME;
			ent.think = Think_AccelMove;
			return true;
		}
	};

	static EntThinkAdapter plat_hit_top = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			if (0 == (ent.flags & FL_TEAMSLAVE)) {
				if (ent.moveinfo.sound_end != 0)
					gi.sound(ent, CHAN_NO_PHS_ADD + CHAN_VOICE, ent.moveinfo.sound_end, 1, ATTN_STATIC, 0);
				ent.s.sound = 0;
			}
			ent.moveinfo.state = STATE_TOP;

			ent.think = plat_go_down;
			ent.nextthink = level.time + 3;
			return true;
		}
	};

	static EntThinkAdapter plat_hit_bottom = new EntThinkAdapter() {
		public boolean think(edict_t ent) {

			if (0 == (ent.flags & FL_TEAMSLAVE)) {
				if (ent.moveinfo.sound_end != 0)
					gi.sound(ent, CHAN_NO_PHS_ADD + CHAN_VOICE, ent.moveinfo.sound_end, 1, ATTN_STATIC, 0);
				ent.s.sound = 0;
			}
			ent.moveinfo.state = STATE_BOTTOM;
			return true;
		}
	};

	static EntThinkAdapter plat_go_down = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			if (0 == (ent.flags & FL_TEAMSLAVE)) {
				if (ent.moveinfo.sound_start != 0)
					gi.sound(ent, CHAN_NO_PHS_ADD + CHAN_VOICE, ent.moveinfo.sound_start, 1, ATTN_STATIC, 0);
				ent.s.sound = ent.moveinfo.sound_middle;
			}
			ent.moveinfo.state = STATE_DOWN;
			Move_Calc(ent, ent.moveinfo.end_origin, plat_hit_bottom);
			return true;
		}
	};

	static void plat_go_up(edict_t ent) {
		if (0 == (ent.flags & FL_TEAMSLAVE)) {
			if (ent.moveinfo.sound_start != 0)
				gi.sound(ent, CHAN_NO_PHS_ADD + CHAN_VOICE, ent.moveinfo.sound_start, 1, ATTN_STATIC, 0);
			ent.s.sound = ent.moveinfo.sound_middle;
		}
		ent.moveinfo.state = STATE_UP;
		Move_Calc(ent, ent.moveinfo.start_origin, plat_hit_top);
	}

	static EntBlockedAdapter plat_blocked = new EntBlockedAdapter() {
		public void blocked(edict_t self, edict_t other) {
			if (0 == (other.svflags & SVF_MONSTER) && (null == other.client)) {
				// give it a chance to go away on it's own terms (like gibs)
				T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, 100000, 1, 0, MOD_CRUSH);
				// if it's still there, nuke it
				if (other != null)
					BecomeExplosion1(other);
				return;
			}

			T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);

			if (self.moveinfo.state == STATE_UP)
				plat_go_down.think(self);
			else if (self.moveinfo.state == STATE_DOWN)
				plat_go_up(self);

		}
	};

	static EntUseAdapter Use_Plat = new EntUseAdapter() {
		public void use(edict_t ent, edict_t other, edict_t activator) {
			if (ent.think != null)
				return; // already down
			plat_go_down.think(ent);
		}
	};

	static EntTouchAdapter Touch_Plat_Center = new EntTouchAdapter() {
		public void touch(edict_t ent, edict_t other, cplane_t plane, csurface_t surf) {
			if (other.client == null)
				return;

			if (other.health <= 0)
				return;

			ent = ent.enemy; // now point at the plat, not the trigger
			if (ent.moveinfo.state == STATE_BOTTOM)
				plat_go_up(ent);
			else if (ent.moveinfo.state == STATE_TOP)
				ent.nextthink = level.time + 1; // the player is still on the plat, so delay going down
		}
	};

	void plat_spawn_inside_trigger(edict_t ent) {
		edict_t trigger;
		float[] tmin={0,0,0}, tmax={0,0,0};

		//
		//	   middle trigger
		//	
		trigger = G_Spawn();
		trigger.touch = Touch_Plat_Center;
		trigger.movetype = MOVETYPE_NONE;
		trigger.solid = SOLID_TRIGGER;
		trigger.enemy = ent;

		tmin[0] = ent.mins[0] + 25;
		tmin[1] = ent.mins[1] + 25;
		tmin[2] = ent.mins[2];

		tmax[0] = ent.maxs[0] - 25;
		tmax[1] = ent.maxs[1] - 25;
		tmax[2] = ent.maxs[2] + 8;

		tmin[2] = tmax[2] - (ent.pos1[2] - ent.pos2[2] + st.lip);

		if ((ent.spawnflags & PLAT_LOW_TRIGGER) != 0)
			tmax[2] = tmin[2] + 8;

		if (tmax[0] - tmin[0] <= 0) {
			tmin[0] = (ent.mins[0] + ent.maxs[0]) * 0.5f;
			tmax[0] = tmin[0] + 1;
		}
		if (tmax[1] - tmin[1] <= 0) {
			tmin[1] = (ent.mins[1] + ent.maxs[1]) * 0.5f;
			tmax[1] = tmin[1] + 1;
		}

		VectorCopy(tmin, trigger.mins);
		VectorCopy(tmax, trigger.maxs);

		gi.linkentity(trigger);
	}

	/*QUAKED func_plat (0 .5 .8) ? PLAT_LOW_TRIGGER
	speed	default 150
	
	Plats are always drawn in the extended position, so they will light correctly.
	
	If the plat is the target of another trigger or button, it will start out disabled in the extended position until it is trigger, when it will lower and become a normal plat.
	
	"speed"	overrides default 200.
	"accel" overrides default 500
	"lip"	overrides default 8 pixel lip
	
	If the "height" key is set, that will determine the amount the plat moves, instead of being implicitly determoveinfoned by the model's height.
	
	Set "sounds" to one of the following:
	1) base fast
	2) chain slow
	*/
	void SP_func_plat(edict_t ent) {
		VectorClear(ent.s.angles);
		ent.solid = SOLID_BSP;
		ent.movetype = MOVETYPE_PUSH;

		gi.setmodel(ent, ent.model);

		ent.blocked = plat_blocked;

		if (0 == ent.speed)
			ent.speed = 20;
		else
			ent.speed *= 0.1;

		if (ent.accel == 0)
			ent.accel = 5;
		else
			ent.accel *= 0.1;

		if (ent.decel == 0)
			ent.decel = 5;
		else
			ent.decel *= 0.1;

		if (ent.dmg == 0)
			ent.dmg = 2;

		if (st.lip == 0)
			st.lip = 8;

		// pos1 is the top position, pos2 is the bottom
		VectorCopy(ent.s.origin, ent.pos1);
		VectorCopy(ent.s.origin, ent.pos2);
		if (st.height != 0)
			ent.pos2[2] -= st.height;
		else
			ent.pos2[2] -= (ent.maxs[2] - ent.mins[2]) - st.lip;

		ent.use = Use_Plat;

		plat_spawn_inside_trigger(ent); // the "start moving" trigger	

		if (ent.targetname == null) {
			ent.moveinfo.state = STATE_UP;
		} else {
			VectorCopy(ent.pos2, ent.s.origin);
			gi.linkentity(ent);
			ent.moveinfo.state = STATE_BOTTOM;
		}

		ent.moveinfo.speed = ent.speed;
		ent.moveinfo.accel = ent.accel;
		ent.moveinfo.decel = ent.decel;
		ent.moveinfo.wait = ent.wait;
		VectorCopy(ent.pos1, ent.moveinfo.start_origin);
		VectorCopy(ent.s.angles, ent.moveinfo.start_angles);
		VectorCopy(ent.pos2, ent.moveinfo.end_origin);
		VectorCopy(ent.s.angles, ent.moveinfo.end_angles);

		ent.moveinfo.sound_start = gi.soundindex("plats/pt1_strt.wav");
		ent.moveinfo.sound_middle = gi.soundindex("plats/pt1_mid.wav");
		ent.moveinfo.sound_end = gi.soundindex("plats/pt1_end.wav");
	}

	//	  ====================================================================

	/*QUAKED func_rotating (0 .5 .8) ? START_ON REVERSE X_AXIS Y_AXIS TOUCH_PAIN STOP ANIMATED ANIMATED_FAST
	You need to have an origin brush as part of this entity.  The center of that brush will be
	the point around which it is rotated. It will rotate around the Z axis by default.  You can
	check either the X_AXIS or Y_AXIS box to change that.
	
	"speed" determines how fast it moves; default value is 100.
	"dmg"	damage to inflict when blocked (2 default)
	
	REVERSE will cause the it to rotate in the opposite direction.
	STOP mean it will stop moving instead of pushing entities
	*/

	static EntBlockedAdapter rotating_blocked = new EntBlockedAdapter() {
		void blocked(edict_t self, edict_t other) {
			T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);
		}
	};

	static EntTouchAdapter rotating_touch = new EntTouchAdapter() {
		public void touch(edict_t self, edict_t other, cplane_t plane, csurface_t surf) {
			if (self.avelocity[0] != 0 || self.avelocity[1] != 0 || self.avelocity[2] != 0)
				T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);
		}
	};

	static EntUseAdapter rotating_use = new EntUseAdapter() {
		public void use(edict_t self, edict_t other, edict_t activator) {
			if (0 == VectorCompare(self.avelocity, vec3_origin)) {
				self.s.sound = 0;
				VectorClear(self.avelocity);
				self.touch = null;
			} else {
				self.s.sound = self.moveinfo.sound_middle;
				VectorScale(self.movedir, self.speed, self.avelocity);
				if ((self.spawnflags & 16) != 0)
					self.touch = rotating_touch;
			}
		}
	};
	static EntThinkAdapter SP_func_rotating = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			ent.solid = SOLID_BSP;
			if ((ent.spawnflags & 32) != 0)
				ent.movetype = MOVETYPE_STOP;
			else
				ent.movetype = MOVETYPE_PUSH;

			// set the axis of rotation
			VectorClear(ent.movedir);
			if ((ent.spawnflags & 4) != 0)
				ent.movedir[2] = 1.0f;
			else if ((ent.spawnflags & 8) != 0)
				ent.movedir[0] = 1.0f;
			else // Z_AXIS
				ent.movedir[1] = 1.0f;

			// check for reverse rotation
			if ((ent.spawnflags & 2) != 0)
				VectorNegate(ent.movedir, ent.movedir);

			if (0 == ent.speed)
				ent.speed = 100;
			if (0 == ent.dmg)
				ent.dmg = 2;

			//		ent.moveinfo.sound_middle = "doors/hydro1.wav";

			ent.use = rotating_use;
			if (ent.dmg != 0)
				ent.blocked = rotating_blocked;

			if ((ent.spawnflags & 1) != 0)
				ent.use.use(ent, null, null);

			if ((ent.spawnflags & 64) != 0)
				ent.s.effects |= EF_ANIM_ALL;
			if ((ent.spawnflags & 128) != 0)
				ent.s.effects |= EF_ANIM_ALLFAST;

			gi.setmodel(ent, ent.model);
			gi.linkentity(ent);
			return true;
		}
	};

	/*
	======================================================================
	
	BUTTONS
	
	======================================================================
	*/

	/*QUAKED func_button (0 .5 .8) ?
	When a button is touched, it moves some distance in the direction of it's angle, triggers all of it's targets, waits some time, then returns to it's original position where it can be triggered again.
	
	"angle"		determines the opening direction
	"target"	all entities with a matching targetname will be used
	"speed"		override the default 40 speed
	"wait"		override the default 1 second wait (-1 = never return)
	"lip"		override the default 4 pixel lip remaining at end of move
	"health"	if set, the button must be killed instead of touched
	"sounds"
	1) silent
	2) steam metal
	3) wooden clunk
	4) metallic click
	5) in-out
	*/

	static EntThinkAdapter button_done = new EntThinkAdapter() {
		public boolean think(edict_t self) {

			self.moveinfo.state = STATE_BOTTOM;
			self.s.effects &= ~EF_ANIM23;
			self.s.effects |= EF_ANIM01;
			return true;
		}
	};

	static EntThinkAdapter button_return = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			self.moveinfo.state = STATE_DOWN;

			Move_Calc(self, self.moveinfo.start_origin, button_done);

			self.s.frame = 0;

			if (self.health != 0)
				self.takedamage = DAMAGE_YES;
			return true;
		}
	};

	static EntThinkAdapter button_wait = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			self.moveinfo.state = STATE_TOP;
			self.s.effects &= ~EF_ANIM01;
			self.s.effects |= EF_ANIM23;

			G_UseTargets(self, self.activator);
			self.s.frame = 1;
			if (self.moveinfo.wait >= 0) {
				self.nextthink = level.time + self.moveinfo.wait;
				self.think = button_return;
			}
			return true;
		}
	};

	static EntThinkAdapter button_fire = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (self.moveinfo.state == STATE_UP || self.moveinfo.state == STATE_TOP)
				return true;

			self.moveinfo.state = STATE_UP;
			if (self.moveinfo.sound_start != 0 && 0 == (self.flags & FL_TEAMSLAVE))
				gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_start, 1, ATTN_STATIC, 0);
			Move_Calc(self, self.moveinfo.end_origin, button_wait);
			return true;
		}
	};

	static EntUseAdapter button_use = new EntUseAdapter() {
		public boolean think(edict_t self, edict_t activator) {
			self.activator = activator;
			button_fire.think(self);
			return true;
		}
	};

	static EntTouchAdapter button_touch = new EntTouchAdapter() {
		public void touch(edict_t self, edict_t other, cplane_t plane, csurface_t surf) {
			if (null == other.client)
				return;

			if (other.health <= 0)
				return;

			self.activator = other;
			button_fire.think(self);

		}
	};

	static EntDieAdapter button_killed = new EntDieAdapter() {
		public void die(edict_t self, edict_t inflictor, edict_t attacker, int damage, float[] point) {
			self.activator = attacker;
			self.health = self.max_health;
			self.takedamage = DAMAGE_NO;
			button_fire.think(self);

		}
	};

	static EntThinkAdapter SP_func_button = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			float[] abs_movedir={0,0,0};
			float dist;

			G_SetMovedir(ent.s.angles, ent.movedir);
			ent.movetype = MOVETYPE_STOP;
			ent.solid = SOLID_BSP;
			gi.setmodel(ent, ent.model);

			if (ent.sounds != 1)
				ent.moveinfo.sound_start = gi.soundindex("switches/butn2.wav");

			if (0 == ent.speed)
				ent.speed = 40;
			if (0 == ent.accel)
				ent.accel = ent.speed;
			if (0 == ent.decel)
				ent.decel = ent.speed;

			if (0 == ent.wait)
				ent.wait = 3;
			if (0 == st.lip)
				st.lip = 4;

			VectorCopy(ent.s.origin, ent.pos1);
			abs_movedir[0] = (float) Math.abs(ent.movedir[0]);
			abs_movedir[1] = (float) Math.abs(ent.movedir[1]);
			abs_movedir[2] = (float) Math.abs(ent.movedir[2]);
			dist = abs_movedir[0] * ent.size[0] + abs_movedir[1] * ent.size[1] + abs_movedir[2] * ent.size[2] - st.lip;
			VectorMA(ent.pos1, dist, ent.movedir, ent.pos2);

			ent.use = button_use;
			ent.s.effects |= EF_ANIM01;

			if (ent.health != 0) {
				ent.max_health = ent.health;
				ent.die = button_killed;
				ent.takedamage = DAMAGE_YES;
			} else if (null == ent.targetname)
				ent.touch = button_touch;

			ent.moveinfo.state = STATE_BOTTOM;

			ent.moveinfo.speed = ent.speed;
			ent.moveinfo.accel = ent.accel;
			ent.moveinfo.decel = ent.decel;
			ent.moveinfo.wait = ent.wait;
			VectorCopy(ent.pos1, ent.moveinfo.start_origin);
			VectorCopy(ent.s.angles, ent.moveinfo.start_angles);
			VectorCopy(ent.pos2, ent.moveinfo.end_origin);
			VectorCopy(ent.s.angles, ent.moveinfo.end_angles);

			gi.linkentity(ent);
			return true;
		}
	};

	/*
	======================================================================
	
	DOORS
	
	  spawn a trigger surrounding the entire team unless it is
	  already targeted by another
	
	======================================================================
	*/

	/*QUAKED func_door (0 .5 .8) ? START_OPEN x CRUSHER NOMONSTER ANIMATED TOGGLE ANIMATED_FAST
	TOGGLE		wait in both the start and end states for a trigger event.
	START_OPEN	the door to moves to its destination when spawned, and operate in reverse.  It is used to temporarily or permanently close off an area when triggered (not useful for touch or takedamage doors).
	NOMONSTER	monsters will not trigger this door
	
	"message"	is printed when the door is touched if it is a trigger door and it hasn't been fired yet
	"angle"		determines the opening direction
	"targetname" if set, no touch field will be spawned and a remote button or trigger field activates the door.
	"health"	if set, door must be shot open
	"speed"		movement speed (100 default)
	"wait"		wait before returning (3 default, -1 = never return)
	"lip"		lip remaining at end of move (8 default)
	"dmg"		damage to inflict when blocked (2 default)
	"sounds"
	1)	silent
	2)	light
	3)	medium
	4)	heavy
	*/

	static void door_use_areaportals(edict_t self, boolean open) {
		edict_t t = null;

		if (self.target == null)
			return;

		EdictIterator edit = null;

		while ((edit = G_Find(edit, findByTarget, self.target)) != null) {
			t = edit.o;
			if (Q_stricmp(t.classname, "func_areaportal") == 0) {
				gi.SetAreaPortalState(t.style, open);
			}
		}
	}

	static EntThinkAdapter door_hit_top = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (0 == (self.flags & FL_TEAMSLAVE)) {
				if (self.moveinfo.sound_end != 0)
					gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_end, 1, ATTN_STATIC, 0);
				self.s.sound = 0;
			}
			self.moveinfo.state = STATE_TOP;
			if ((self.spawnflags & DOOR_TOGGLE) != 0)
				return true;
			if (self.moveinfo.wait >= 0) {
				self.think = door_go_down;
				self.nextthink = level.time + self.moveinfo.wait;
			}
			return true;
		}
	};

	static EntThinkAdapter door_hit_bottom = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (0 == (self.flags & FL_TEAMSLAVE)) {
				if (self.moveinfo.sound_end != 0)
					gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_end, 1, ATTN_STATIC, 0);
				self.s.sound = 0;
			}
			self.moveinfo.state = STATE_BOTTOM;
			door_use_areaportals(self, false);
			return true;
		}
	};

	static EntThinkAdapter door_go_down = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (0 == (self.flags & FL_TEAMSLAVE)) {
				if (self.moveinfo.sound_start != 0)
					gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_start, 1, ATTN_STATIC, 0);
				self.s.sound = self.moveinfo.sound_middle;
			}
			if (self.max_health != 0) {
				self.takedamage = DAMAGE_YES;
				self.health = self.max_health;
			}

			self.moveinfo.state = STATE_DOWN;
			if (strcmp(self.classname, "func_door") == 0)
				Move_Calc(self, self.moveinfo.start_origin, door_hit_bottom);
			else if (strcmp(self.classname, "func_door_rotating") == 0)
				AngleMove_Calc(self, door_hit_bottom);
			return true;
		}
	};

	static void door_go_up(edict_t self, edict_t activator) {
		if (self.moveinfo.state == STATE_UP)
			return; // already going up

		if (self.moveinfo.state == STATE_TOP) { // reset top wait time
			if (self.moveinfo.wait >= 0)
				self.nextthink = level.time + self.moveinfo.wait;
			return;
		}

		if (0 == (self.flags & FL_TEAMSLAVE)) {
			if (self.moveinfo.sound_start != 0)
				gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_start, 1, ATTN_STATIC, 0);
			self.s.sound = self.moveinfo.sound_middle;
		}
		self.moveinfo.state = STATE_UP;
		if (strcmp(self.classname, "func_door") == 0)
			Move_Calc(self, self.moveinfo.end_origin, door_hit_top);
		else if (strcmp(self.classname, "func_door_rotating") == 0)
			AngleMove_Calc(self, door_hit_top);

		G_UseTargets(self, activator);
		door_use_areaportals(self, true);
	}

	static EntUseAdapter door_use = new EntUseAdapter() {
		public void use(edict_t self, edict_t other, edict_t activator) {
			edict_t ent;

			if ((self.flags & FL_TEAMSLAVE) != 0)
				return;

			if ((self.spawnflags & DOOR_TOGGLE) != 0) {
				if (self.moveinfo.state == STATE_UP || self.moveinfo.state == STATE_TOP) {
					// trigger all paired doors
					for (ent = self; ent != null; ent = ent.teamchain) {
						ent.message = null;
						ent.touch = null;
						door_go_down.think(ent);
					}
					return;
				}
			}

			// trigger all paired doors
			for (ent = self; ent != null; ent = ent.teamchain) {
				ent.message = null;
				ent.touch = null;
				door_go_up(ent, activator);
			}
		}
	};

	static EntTouchAdapter Touch_DoorTrigger = new EntTouchAdapter() {
		public void touch(edict_t self, edict_t other, cplane_t plane, csurface_t surf) {
			if (other.health <= 0)
				return;

			if (0 == (other.svflags & SVF_MONSTER) && (null == other.client))
				return;

			if (0 != (self.owner.spawnflags & DOOR_NOMONSTER) && 0 != (other.svflags & SVF_MONSTER))
				return;

			if (level.time < self.touch_debounce_time)
				return;
			self.touch_debounce_time = level.time + 1.0f;

			door_use.use(self.owner, other, other);
		}
	};

	static EntThinkAdapter Think_CalcMoveSpeed = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			edict_t ent;
			float min;
			float time;
			float newspeed;
			float ratio;
			float dist;

			if ((self.flags & FL_TEAMSLAVE) != 0)
				return true; // only the team master does this

			// find the smallest distance any member of the team will be moving
			min = Math.abs(self.moveinfo.distance);
			for (ent = self.teamchain; ent != null; ent = ent.teamchain) {
				dist = Math.abs(ent.moveinfo.distance);
				if (dist < min)
					min = dist;
			}

			time = min / self.moveinfo.speed;

			// adjust speeds so they will all complete at the same time
			for (ent = self; ent != null; ent = ent.teamchain) {
				newspeed = Math.abs(ent.moveinfo.distance) / time;
				ratio = newspeed / ent.moveinfo.speed;
				if (ent.moveinfo.accel == ent.moveinfo.speed)
					ent.moveinfo.accel = newspeed;
				else
					ent.moveinfo.accel *= ratio;
				if (ent.moveinfo.decel == ent.moveinfo.speed)
					ent.moveinfo.decel = newspeed;
				else
					ent.moveinfo.decel *= ratio;
				ent.moveinfo.speed = newspeed;
			}
			return true;
		}
	};

	static EntThinkAdapter Think_SpawnDoorTrigger = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			edict_t other;
			float[] mins={0,0,0}, maxs={0,0,0};

			if ((ent.flags & FL_TEAMSLAVE) != 0)
				return true; // only the team leader spawns a trigger

			VectorCopy(ent.absmin, mins);
			VectorCopy(ent.absmax, maxs);

			for (other = ent.teamchain; other != null; other = other.teamchain) {
				AddPointToBounds(other.absmin, mins, maxs);
				AddPointToBounds(other.absmax, mins, maxs);
			}

			// expand 
			mins[0] -= 60;
			mins[1] -= 60;
			maxs[0] += 60;
			maxs[1] += 60;

			other = G_Spawn();
			VectorCopy(mins, other.mins);
			VectorCopy(maxs, other.maxs);
			other.owner = ent;
			other.solid = SOLID_TRIGGER;
			other.movetype = MOVETYPE_NONE;
			other.touch = Touch_DoorTrigger;
			gi.linkentity(other);

			if ((ent.spawnflags & DOOR_START_OPEN) != 0)
				door_use_areaportals(ent, true);

			Think_CalcMoveSpeed.think(ent);
			return true;
		}
	};

	static EntBlockedAdapter door_blocked = new EntBlockedAdapter() {
		void blocked(edict_t self, edict_t other) {
			edict_t ent;

			if (0 == (other.svflags & SVF_MONSTER) && (null == other.client)) {
				// give it a chance to go away on it's own terms (like gibs)
				T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, 100000, 1, 0, MOD_CRUSH);
				// if it's still there, nuke it
				if (other != null)
					BecomeExplosion1(other);
				return;
			}

			T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);

			if ((self.spawnflags & DOOR_CRUSHER) != 0)
				return;

			//	   if a door has a negative wait, it would never come back if blocked,
			//	   so let it just squash the object to death real fast
			if (self.moveinfo.wait >= 0) {
				if (self.moveinfo.state == STATE_DOWN) {
					for (ent = self.teammaster; ent != null; ent = ent.teamchain)
						door_go_up(ent, ent.activator);
				} else {
					for (ent = self.teammaster; ent != null; ent = ent.teamchain)
						door_go_down.think(ent);
				}
			}
		}
	};

	static EntDieAdapter door_killed = new EntDieAdapter() {
		public void die(edict_t self, edict_t inflictor, edict_t attacker, int damage, float[] point) {
			edict_t ent;

			for (ent = self.teammaster; ent != null; ent = ent.teamchain) {
				ent.health = ent.max_health;
				ent.takedamage = DAMAGE_NO;
			}
			door_use.use(self.teammaster, attacker, attacker);
		}
	};

	static EntTouchAdapter door_touch = new EntTouchAdapter() {
		public void touch(edict_t self, edict_t other, cplane_t plane, csurface_t surf) {
			if (null == other.client)
				return;

			if (level.time < self.touch_debounce_time)
				return;
			self.touch_debounce_time = level.time + 5.0f;

			gi.centerprintf(other, self.message);
			gi.sound(other, CHAN_AUTO, gi.soundindex("misc/talk1.wav"), 1, ATTN_NORM, 0);
		}
	};

	static EntThinkAdapter SP_func_door = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			float[] abs_movedir={0,0,0};

			if (ent.sounds != 1) {
				ent.moveinfo.sound_start = gi.soundindex("doors/dr1_strt.wav");
				ent.moveinfo.sound_middle = gi.soundindex("doors/dr1_mid.wav");
				ent.moveinfo.sound_end = gi.soundindex("doors/dr1_end.wav");
			}

			G_SetMovedir(ent.s.angles, ent.movedir);
			ent.movetype = MOVETYPE_PUSH;
			ent.solid = SOLID_BSP;
			gi.setmodel(ent, ent.model);

			ent.blocked = door_blocked;
			ent.use = door_use;

			if (0 == ent.speed)
				ent.speed = 100;
			if (deathmatch.value != 0)
				ent.speed *= 2;

			if (0 == ent.accel)
				ent.accel = ent.speed;
			if (0 == ent.decel)
				ent.decel = ent.speed;

			if (0 == ent.wait)
				ent.wait = 3;
			if (0 == st.lip)
				st.lip = 8;
			if (0 == ent.dmg)
				ent.dmg = 2;

			// calculate second position
			VectorCopy(ent.s.origin, ent.pos1);
			abs_movedir[0] = Math.abs(ent.movedir[0]);
			abs_movedir[1] = Math.abs(ent.movedir[1]);
			abs_movedir[2] = Math.abs(ent.movedir[2]);
			ent.moveinfo.distance =
				abs_movedir[0] * ent.size[0] + abs_movedir[1] * ent.size[1] + abs_movedir[2] * ent.size[2] - st.lip;
			VectorMA(ent.pos1, ent.moveinfo.distance, ent.movedir, ent.pos2);

			// if it starts open, switch the positions
			if ((ent.spawnflags & DOOR_START_OPEN) != 0) {
				VectorCopy(ent.pos2, ent.s.origin);
				VectorCopy(ent.pos1, ent.pos2);
				VectorCopy(ent.s.origin, ent.pos1);
			}

			ent.moveinfo.state = STATE_BOTTOM;

			if (ent.health != 0) {
				ent.takedamage = DAMAGE_YES;
				ent.die = door_killed;
				ent.max_health = ent.health;
			} else if (ent.targetname != null && ent.message != null) {
				gi.soundindex("misc/talk.wav");
				ent.touch = door_touch;
			}

			ent.moveinfo.speed = ent.speed;
			ent.moveinfo.accel = ent.accel;
			ent.moveinfo.decel = ent.decel;
			ent.moveinfo.wait = ent.wait;
			VectorCopy(ent.pos1, ent.moveinfo.start_origin);
			VectorCopy(ent.s.angles, ent.moveinfo.start_angles);
			VectorCopy(ent.pos2, ent.moveinfo.end_origin);
			VectorCopy(ent.s.angles, ent.moveinfo.end_angles);

			if ((ent.spawnflags & 16) != 0)
				ent.s.effects |= EF_ANIM_ALL;
			if ((ent.spawnflags & 64) != 0)
				ent.s.effects |= EF_ANIM_ALLFAST;

			// to simplify logic elsewhere, make non-teamed doors into a team of one
			if (null == ent.team)
				ent.teammaster = ent;

			gi.linkentity(ent);

			ent.nextthink = level.time + FRAMETIME;
			if (ent.health != 0 || ent.targetname != null)
				ent.think = Think_CalcMoveSpeed;
			else
				ent.think = Think_SpawnDoorTrigger;
			return true;
		}
	};

	/*QUAKED func_door_rotating (0 .5 .8) ? START_OPEN REVERSE CRUSHER NOMONSTER ANIMATED TOGGLE X_AXIS Y_AXIS
	TOGGLE causes the door to wait in both the start and end states for a trigger event.
	
	START_OPEN	the door to moves to its destination when spawned, and operate in reverse.  It is used to temporarily or permanently close off an area when triggered (not useful for touch or takedamage doors).
	NOMONSTER	monsters will not trigger this door
	
	You need to have an origin brush as part of this entity.  The center of that brush will be
	the point around which it is rotated. It will rotate around the Z axis by default.  You can
	check either the X_AXIS or Y_AXIS box to change that.
	
	"distance" is how many degrees the door will be rotated.
	"speed" determines how fast the door moves; default value is 100.
	
	REVERSE will cause the door to rotate in the opposite direction.
	
	"message"	is printed when the door is touched if it is a trigger door and it hasn't been fired yet
	"angle"		determines the opening direction
	"targetname" if set, no touch field will be spawned and a remote button or trigger field activates the door.
	"health"	if set, door must be shot open
	"speed"		movement speed (100 default)
	"wait"		wait before returning (3 default, -1 = never return)
	"dmg"		damage to inflict when blocked (2 default)
	"sounds"
	1)	silent
	2)	light
	3)	medium
	4)	heavy
	*/

	static EntThinkAdapter SP_func_door_rotating = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			VectorClear(ent.s.angles);

			// set the axis of rotation
			VectorClear(ent.movedir);
			if ((ent.spawnflags & DOOR_X_AXIS) != 0)
				ent.movedir[2] = 1.0f;
			else if ((ent.spawnflags & DOOR_Y_AXIS) != 0)
				ent.movedir[0] = 1.0f;
			else // Z_AXIS
				ent.movedir[1] = 1.0f;

			// check for reverse rotation
			if ((ent.spawnflags & DOOR_REVERSE) != 0)
				VectorNegate(ent.movedir, ent.movedir);

			if (0 == st.distance) {
				gi.dprintf(ent.classname + " at " + vtos(ent.s.origin) + " with no distance set\n");
				st.distance = 90;
			}

			VectorCopy(ent.s.angles, ent.pos1);
			VectorMA(ent.s.angles, st.distance, ent.movedir, ent.pos2);
			ent.moveinfo.distance = st.distance;

			ent.movetype = MOVETYPE_PUSH;
			ent.solid = SOLID_BSP;
			gi.setmodel(ent, ent.model);

			ent.blocked = door_blocked;
			ent.use = door_use;

			if (0 == ent.speed)
				ent.speed = 100;
			if (0 == ent.accel)
				ent.accel = ent.speed;
			if (0 == ent.decel)
				ent.decel = ent.speed;

			if (0 == ent.wait)
				ent.wait = 3;
			if (0 == ent.dmg)
				ent.dmg = 2;

			if (ent.sounds != 1) {
				ent.moveinfo.sound_start = gi.soundindex("doors/dr1_strt.wav");
				ent.moveinfo.sound_middle = gi.soundindex("doors/dr1_mid.wav");
				ent.moveinfo.sound_end = gi.soundindex("doors/dr1_end.wav");
			}

			// if it starts open, switch the positions
			if ((ent.spawnflags & DOOR_START_OPEN) != 0) {
				VectorCopy(ent.pos2, ent.s.angles);
				VectorCopy(ent.pos1, ent.pos2);
				VectorCopy(ent.s.angles, ent.pos1);
				VectorNegate(ent.movedir, ent.movedir);
			}

			if (ent.health != 0) {
				ent.takedamage = DAMAGE_YES;
				ent.die = door_killed;
				ent.max_health = ent.health;
			}

			if (ent.targetname != null && ent.message != null) {
				gi.soundindex("misc/talk.wav");
				ent.touch = door_touch;
			}

			ent.moveinfo.state = STATE_BOTTOM;
			ent.moveinfo.speed = ent.speed;
			ent.moveinfo.accel = ent.accel;
			ent.moveinfo.decel = ent.decel;
			ent.moveinfo.wait = ent.wait;
			VectorCopy(ent.s.origin, ent.moveinfo.start_origin);
			VectorCopy(ent.pos1, ent.moveinfo.start_angles);
			VectorCopy(ent.s.origin, ent.moveinfo.end_origin);
			VectorCopy(ent.pos2, ent.moveinfo.end_angles);

			if ((ent.spawnflags & 16) != 0)
				ent.s.effects |= EF_ANIM_ALL;

			// to simplify logic elsewhere, make non-teamed doors into a team of one
			if (ent.team == null)
				ent.teammaster = ent;

			gi.linkentity(ent);

			ent.nextthink = level.time + FRAMETIME;
			if (ent.health != 0 || ent.targetname != null)
				ent.think = Think_CalcMoveSpeed;
			else
				ent.think = Think_SpawnDoorTrigger;
			return true;
		}
	};

	/*QUAKED func_water (0 .5 .8) ? START_OPEN
	func_water is a moveable water brush.  It must be targeted to operate.  Use a non-water texture at your own risk.
	
	START_OPEN causes the water to move to its destination when spawned and operate in reverse.
	
	"angle"		determines the opening direction (up or down only)
	"speed"		movement speed (25 default)
	"wait"		wait before returning (-1 default, -1 = TOGGLE)
	"lip"		lip remaining at end of move (0 default)
	"sounds"	(yes, these need to be changed)
	0)	no sound
	1)	water
	2)	lava
	*/

	void SP_func_water(edict_t self) {
		float[] abs_movedir={0,0,0};

		G_SetMovedir(self.s.angles, self.movedir);
		self.movetype = MOVETYPE_PUSH;
		self.solid = SOLID_BSP;
		gi.setmodel(self, self.model);

		switch (self.sounds) {
			default :
				break;

			case 1 : // water
				self.moveinfo.sound_start = gi.soundindex("world/mov_watr.wav");
				self.moveinfo.sound_end = gi.soundindex("world/stp_watr.wav");
				break;

			case 2 : // lava
				self.moveinfo.sound_start = gi.soundindex("world/mov_watr.wav");
				self.moveinfo.sound_end = gi.soundindex("world/stp_watr.wav");
				break;
		}

		// calculate second position
		VectorCopy(self.s.origin, self.pos1);
		abs_movedir[0] = Math.abs(self.movedir[0]);
		abs_movedir[1] = Math.abs(self.movedir[1]);
		abs_movedir[2] = Math.abs(self.movedir[2]);
		self.moveinfo.distance =
			abs_movedir[0] * self.size[0] + abs_movedir[1] * self.size[1] + abs_movedir[2] * self.size[2] - st.lip;
		VectorMA(self.pos1, self.moveinfo.distance, self.movedir, self.pos2);

		// if it starts open, switch the positions
		if ((self.spawnflags & DOOR_START_OPEN) != 0) {
			VectorCopy(self.pos2, self.s.origin);
			VectorCopy(self.pos1, self.pos2);
			VectorCopy(self.s.origin, self.pos1);
		}

		VectorCopy(self.pos1, self.moveinfo.start_origin);
		VectorCopy(self.s.angles, self.moveinfo.start_angles);
		VectorCopy(self.pos2, self.moveinfo.end_origin);
		VectorCopy(self.s.angles, self.moveinfo.end_angles);

		self.moveinfo.state = STATE_BOTTOM;

		if (0 == self.speed)
			self.speed = 25;
		self.moveinfo.accel = self.moveinfo.decel = self.moveinfo.speed = self.speed;

		if (0 == self.wait)
			self.wait = -1;
		self.moveinfo.wait = self.wait;

		self.use = door_use;

		if (self.wait == -1)
			self.spawnflags |= DOOR_TOGGLE;

		self.classname = "func_door";

		gi.linkentity(self);
	}

	public final static int TRAIN_START_ON = 1;
	public final static int TRAIN_TOGGLE = 2;
	public final static int TRAIN_BLOCK_STOPS = 4;

	/*QUAKED func_train (0 .5 .8) ? START_ON TOGGLE BLOCK_STOPS
	Trains are moving platforms that players can ride.
	The targets origin specifies the min point of the train at each corner.
	The train spawns at the first target it is pointing at.
	If the train is the target of a button or trigger, it will not begin moving until activated.
	speed	default 100
	dmg		default	2
	noise	looping sound to play when the train is in motion
	
	*/

	static EntBlockedAdapter train_blocked = new EntBlockedAdapter() {

		public void blocked(edict_t self, edict_t other) {
			if (0 == (other.svflags & SVF_MONSTER) && (null == other.client)) {
				// give it a chance to go away on it's own terms (like gibs)
				T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, 100000, 1, 0, MOD_CRUSH);
				// if it's still there, nuke it
				if (other != null)
					BecomeExplosion1(other);
				return;
			}

			if (level.time < self.touch_debounce_time)
				return;

			if (self.dmg == 0)
				return;
			self.touch_debounce_time = level.time + 0.5f;
			T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);
		}
	};

	static EntThinkAdapter train_wait = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (self.target_ent.pathtarget != null) {
				String savetarget;
				edict_t ent;

				ent = self.target_ent;
				savetarget = ent.target;
				ent.target = ent.pathtarget;
				G_UseTargets(ent, self.activator);
				ent.target = savetarget;

				// make sure we didn't get killed by a killtarget
				if (!self.inuse)
					return true;
			}

			if (self.moveinfo.wait != 0) {
				if (self.moveinfo.wait > 0) {
					self.nextthink = level.time + self.moveinfo.wait;
					self.think = train_next;
				} else if (0 != (self.spawnflags & TRAIN_TOGGLE)) // && wait < 0
					{
					train_next.think(self);
					self.spawnflags &= ~TRAIN_START_ON;
					VectorClear(self.velocity);
					self.nextthink = 0;
				}

				if (0 == (self.flags & FL_TEAMSLAVE)) {
					if (self.moveinfo.sound_end != 0)
						gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_end, 1, ATTN_STATIC, 0);
					self.s.sound = 0;
				}
			} else {
				train_next.think(self);
			}
			return true;
		}
	};

	static EntThinkAdapter train_next = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			edict_t ent=null;
			float[] dest={0,0,0};
			boolean first;

			first = true;

			boolean dogoto = true;
			while (dogoto) {
				if (null == self.target) {
					//			gi.dprintf ("train_next: no next target\n");
					return true;
				}

				ent = G_PickTarget(self.target);
				if (null == ent) {
					gi.dprintf("train_next: bad target " + self.target + "\n");
					return true;
				}

				self.target = ent.target;
				dogoto = false;
				// check for a teleport path_corner
				if ((ent.spawnflags & 1) != 0) {
					if (!first) {
						gi.dprintf("connected teleport path_corners, see " + ent.classname + " at " + vtos(ent.s.origin) + "\n");
						return true;
					}
					first = false;
					VectorSubtract(ent.s.origin, self.mins, self.s.origin);
					VectorCopy(self.s.origin, self.s.old_origin);
					self.s.event = EV_OTHER_TELEPORT;
					gi.linkentity(self);
					dogoto = true;
				}
			}
			self.moveinfo.wait = ent.wait;
			self.target_ent = ent;

			if (0 == (self.flags & FL_TEAMSLAVE)) {
				if (self.moveinfo.sound_start != 0)
					gi.sound(self, CHAN_NO_PHS_ADD + CHAN_VOICE, self.moveinfo.sound_start, 1, ATTN_STATIC, 0);
				self.s.sound = self.moveinfo.sound_middle;
			}

			VectorSubtract(ent.s.origin, self.mins, dest);
			self.moveinfo.state = STATE_TOP;
			VectorCopy(self.s.origin, self.moveinfo.start_origin);
			VectorCopy(dest, self.moveinfo.end_origin);
			Move_Calc(self, dest, train_wait);
			self.spawnflags |= TRAIN_START_ON;
			return true;
		}
	};

	static void train_resume(edict_t self) {
		edict_t ent;
		float[] dest={0,0,0};

		ent = self.target_ent;

		VectorSubtract(ent.s.origin, self.mins, dest);
		self.moveinfo.state = STATE_TOP;
		VectorCopy(self.s.origin, self.moveinfo.start_origin);
		VectorCopy(dest, self.moveinfo.end_origin);
		Move_Calc(self, dest, train_wait);
		self.spawnflags |= TRAIN_START_ON;

	}

	static EntThinkAdapter func_train_find = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			edict_t ent;

			if (null == self.target) {
				gi.dprintf("train_find: no target\n");
				return true;
			}
			ent = G_PickTarget(self.target);
			if (null == ent) {
				gi.dprintf("train_find: target " + self.target + " not found\n");
				return true;
			}
			self.target = ent.target;

			VectorSubtract(ent.s.origin, self.mins, self.s.origin);
			gi.linkentity(self);

			// if not triggered, start immediately
			if (null == self.targetname)
				self.spawnflags |= TRAIN_START_ON;

			if ((self.spawnflags & TRAIN_START_ON) != 0) {
				self.nextthink = level.time + FRAMETIME;
				self.think = train_next;
				self.activator = self;
			}
			return true;
		}
	};

	static EntUseAdapter train_use = new EntUseAdapter() {
		public void use(edict_t self, edict_t other, edict_t activator) {
			self.activator = activator;

			if ((self.spawnflags & TRAIN_START_ON) != 0) {
				if (0 == (self.spawnflags & TRAIN_TOGGLE))
					return;
				self.spawnflags &= ~TRAIN_START_ON;
				VectorClear(self.velocity);
				self.nextthink = 0;
			} else {
				if (self.target_ent != null)
					train_resume(self);
				else
					train_next.think(self);
			}
		}
	};

	void SP_func_train(edict_t self) {
		self.movetype = MOVETYPE_PUSH;

		VectorClear(self.s.angles);
		self.blocked = train_blocked;
		if ((self.spawnflags & TRAIN_BLOCK_STOPS) != 0)
			self.dmg = 0;
		else {
			if (0 == self.dmg)
				self.dmg = 100;
		}
		self.solid = SOLID_BSP;
		gi.setmodel(self, self.model);

		if (st.noise != null)
			self.moveinfo.sound_middle = gi.soundindex(st.noise);

		if (0 == self.speed)
			self.speed = 100;

		self.moveinfo.speed = self.speed;
		self.moveinfo.accel = self.moveinfo.decel = self.moveinfo.speed;

		self.use = train_use;

		gi.linkentity(self);

		if (self.target != null) {
			// start trains on the second frame, to make sure their targets have had
			// a chance to spawn
			self.nextthink = level.time + FRAMETIME;
			self.think = func_train_find;
		} else {
			gi.dprintf("func_train without a target at " + vtos(self.absmin) + "\n");
		}
	}

	/*QUAKED trigger_elevator (0.3 0.1 0.6) (-8 -8 -8) (8 8 8)
	*/
	static EntUseAdapter trigger_elevator_use = new EntUseAdapter() {

		public void use(edict_t self, edict_t other, edict_t activator) {
			edict_t target;

			if (0 != self.movetarget.nextthink) {
				//			gi.dprintf("elevator busy\n");
				return;
			}

			if (null == other.pathtarget) {
				gi.dprintf("elevator used with no pathtarget\n");
				return;
			}

			target = G_PickTarget(other.pathtarget);
			if (null == target) {
				gi.dprintf("elevator used with bad pathtarget: " + other.pathtarget + "\n");
				return;
			}

			self.movetarget.target_ent = target;
			train_resume(self.movetarget);
		}
	};

	static EntThinkAdapter trigger_elevator_init = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (null == self.target) {
				gi.dprintf("trigger_elevator has no target\n");
				return true;
			}
			self.movetarget = G_PickTarget(self.target);
			if (null == self.movetarget) {
				gi.dprintf("trigger_elevator unable to find target " + self.target + "\n");
				return true;
			}
			if (strcmp(self.movetarget.classname, "func_train") != 0) {
				gi.dprintf("trigger_elevator target " + self.target + " is not a train\n");
				return true;
			}

			self.use = trigger_elevator_use;
			self.svflags = SVF_NOCLIENT;
			return true;
		}
	};

	static EntThinkAdapter SP_trigger_elevator = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			self.think = trigger_elevator_init;
			self.nextthink = level.time + FRAMETIME;
			return true;
		}
	};

	/*QUAKED func_timer (0.3 0.1 0.6) (-8 -8 -8) (8 8 8) START_ON
	"wait"			base time between triggering all targets, default is 1
	"random"		wait variance, default is 0
	
	so, the basic time between firing is a random time between
	(wait - random) and (wait + random)
	
	"delay"			delay before first firing when turned on, default is 0
	
	"pausetime"		additional delay used only the very first time
					and only if spawned with START_ON
	
	These can used but not touched.
	*/

	static EntThinkAdapter func_timer_think = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			G_UseTargets(self, self.activator);
			self.nextthink = level.time + self.wait + crandom() * self.random;
			return true;
		}
	};

	static EntUseAdapter func_timer_use = new EntUseAdapter() {
		public void use(edict_t self, edict_t other, edict_t activator) {
			self.activator = activator;

			// if on, turn it off
			if (self.nextthink != 0) {
				self.nextthink = 0;
				return;
			}

			// turn it on
			if (self.delay != 0)
				self.nextthink = level.time + self.delay;
			else
				func_timer_think.think(self);
		}
	};

	void SP_func_timer(edict_t self) {
		if (0 == self.wait)
			self.wait = 1.0f;

		self.use = func_timer_use;
		self.think = func_timer_think;

		if (self.random >= self.wait) {
			self.random = self.wait - FRAMETIME;
			gi.dprintf("func_timer at " + vtos(self.s.origin) + " has random >= wait\n");
		}

		if ((self.spawnflags & 1) != 0) {
			self.nextthink = level.time + 1.0f + st.pausetime + self.delay + self.wait + crandom() * self.random;
			self.activator = self;
		}

		self.svflags = SVF_NOCLIENT;
	}

	/*QUAKED func_conveyor (0 .5 .8) ? START_ON TOGGLE
	Conveyors are stationary brushes that move what's on them.
	The brush should be have a surface with at least one current content enabled.
	speed	default 100
	*/

	static EntUseAdapter func_conveyor_use = new EntUseAdapter() {
		public void use(edict_t self, edict_t other, edict_t activator) {
			if ((self.spawnflags & 1) != 0) {
				self.speed = 0;
				self.spawnflags &= ~1;
			} else {
				self.speed = self.count;
				self.spawnflags |= 1;
			}

			if (0 == (self.spawnflags & 2))
				self.count = 0;
		}
	};

	static EntThinkAdapter SP_func_conveyor = new EntThinkAdapter() {
		public boolean think(edict_t self) {

			if (0 == self.speed)
				self.speed = 100;

			if (0 == (self.spawnflags & 1)) {
				self.count = (int) self.speed;
				self.speed = 0;
			}

			self.use = func_conveyor_use;

			gi.setmodel(self, self.model);
			self.solid = SOLID_BSP;
			gi.linkentity(self);
			return true;
		}
	};

	/*QUAKED func_door_secret (0 .5 .8) ? always_shoot 1st_left 1st_down
	A secret door.  Slide back and then to the side.
	
	open_once		doors never closes
	1st_left		1st move is left of arrow
	1st_down		1st move is down from arrow
	always_shoot	door is shootebale even if targeted
	
	"angle"		determines the direction
	"dmg"		damage to inflic when blocked (default 2)
	"wait"		how long to hold in the open position (default 5, -1 means hold)
	*/

	public final static int SECRET_ALWAYS_SHOOT = 1;
	public final static int SECRET_1ST_LEFT = 2;
	public final static int SECRET_1ST_DOWN = 4;

	static EntUseAdapter door_secret_use = new EntUseAdapter() {

		public void use(edict_t self, edict_t other, edict_t activator) {
				// make sure we're not already moving
	if (0 == VectorCompare(self.s.origin, vec3_origin))
				return;

			Move_Calc(self, self.pos1, door_secret_move1);
			door_use_areaportals(self, true);
		}
	};

	static EntThinkAdapter door_secret_move1 = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			self.nextthink = level.time + 1.0f;
			self.think = door_secret_move2;
			return true;
		}
	};

	static EntThinkAdapter door_secret_move2 = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			Move_Calc(self, self.pos2, door_secret_move3);
			return true;
		}
	};

	static EntThinkAdapter door_secret_move3 = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (self.wait == -1)
				return true;
			self.nextthink = level.time + self.wait;
			self.think = door_secret_move4;
			return true;
		}
	};

	static EntThinkAdapter door_secret_move4 = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			Move_Calc(self, self.pos1, door_secret_move5);
			return true;
		}
	};

	static EntThinkAdapter door_secret_move5 = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			self.nextthink = level.time + 1.0f;
			self.think = door_secret_move6;
			return true;
		}
	};

	static EntThinkAdapter door_secret_move6 = new EntThinkAdapter() {
		public boolean think(edict_t self) {

			Move_Calc(self, vec3_origin, door_secret_done);
			return true;
		}
	};

	static EntThinkAdapter door_secret_done = new EntThinkAdapter() {
		public boolean think(edict_t self) {
			if (null == (self.targetname) || 0 != (self.spawnflags & SECRET_ALWAYS_SHOOT)) {
				self.health = 0;
				self.takedamage = DAMAGE_YES;
			}
			door_use_areaportals(self, false);
			return true;
		}
	};

	static EntBlockedAdapter door_secret_blocked = new EntBlockedAdapter() {

		public void blocked(edict_t self, edict_t other) {
			if (0 == (other.svflags & SVF_MONSTER) && (null == other.client)) {
				// give it a chance to go away on it's own terms (like gibs)
				T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, 100000, 1, 0, MOD_CRUSH);
				// if it's still there, nuke it
				if (other != null)
					BecomeExplosion1(other);
				return;
			}

			if (level.time < self.touch_debounce_time)
				return;
			self.touch_debounce_time = level.time + 0.5f;

			T_Damage(other, self, self, vec3_origin, other.s.origin, vec3_origin, self.dmg, 1, 0, MOD_CRUSH);
		}
	};

	static EntDieAdapter door_secret_die = new EntDieAdapter() {
		public void die(edict_t self, edict_t inflictor, edict_t attacker, int damage, float[] point) {
			self.takedamage = DAMAGE_NO;
			door_secret_use.use(self, attacker, attacker);
		}
	};

	static EntThinkAdapter SP_func_door_secret = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			float[] forward={0,0,0}, right={0,0,0}, up={0,0,0};
			float side;
			float width;
			float length;

			ent.moveinfo.sound_start = gi.soundindex("doors/dr1_strt.wav");
			ent.moveinfo.sound_middle = gi.soundindex("doors/dr1_mid.wav");
			ent.moveinfo.sound_end = gi.soundindex("doors/dr1_end.wav");

			ent.movetype = MOVETYPE_PUSH;
			ent.solid = SOLID_BSP;
			gi.setmodel(ent, ent.model);

			ent.blocked = door_secret_blocked;
			ent.use = door_secret_use;

			if (null == (ent.targetname) || 0 != (ent.spawnflags & SECRET_ALWAYS_SHOOT)) {
				ent.health = 0;
				ent.takedamage = DAMAGE_YES;
				ent.die = door_secret_die;
			}

			if (0 == ent.dmg)
				ent.dmg = 2;

			if (0 == ent.wait)
				ent.wait = 5;

			ent.moveinfo.accel = ent.moveinfo.decel = ent.moveinfo.speed = 50;

			// calculate positions
			AngleVectors(ent.s.angles, forward, right, up);
			VectorClear(ent.s.angles);
			side = 1.0f - (ent.spawnflags & SECRET_1ST_LEFT);
			if ((ent.spawnflags & SECRET_1ST_DOWN) != 0)
				width = Math.abs(DotProduct(up, ent.size));
			else
				width = Math.abs(DotProduct(right, ent.size));
			length = Math.abs(DotProduct(forward, ent.size));
			if ((ent.spawnflags & SECRET_1ST_DOWN) != 0)
				VectorMA(ent.s.origin, -1 * width, up, ent.pos1);
			else
				VectorMA(ent.s.origin, side * width, right, ent.pos1);
			VectorMA(ent.pos1, length, forward, ent.pos2);

			if (ent.health != 0) {
				ent.takedamage = DAMAGE_YES;
				ent.die = door_killed;
				ent.max_health = ent.health;
			} else if (ent.targetname != null && ent.message != null) {
				gi.soundindex("misc/talk.wav");
				ent.touch = door_touch;
			}

			ent.classname = "func_door";

			gi.linkentity(ent);
			return true;
		}
	};

	/*QUAKED func_killbox (1 0 0) ?
	Kills everything inside when fired, irrespective of protection.
	*/
	static EntUseAdapter use_killbox = new EntUseAdapter() {
		void use_killbox(edict_t self, edict_t other, edict_t activator) {
			KillBox(self);
		}
	};

	static EntThinkAdapter SP_func_killbox = new EntThinkAdapter() {
		public boolean think(edict_t ent) {
			gi.setmodel(ent, ent.model);
			ent.use = use_killbox;
			ent.svflags = SVF_NOCLIENT;
			return true;
		}
	};

}
