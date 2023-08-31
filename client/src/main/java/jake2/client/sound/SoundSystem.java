/*
 * S.java
 * Copyright (C) 2003
 * 
 * $Id: S.java,v 1.13 2005-12-13 00:00:25 salomo Exp $
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
package jake2.client.sound;

import jake2.client.sound.lwjgl.LWJGLSoundImpl;
import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.exec.cvar_t;
import org.lwjgl.Sys;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundSystem {
	private static Sound driverInUse = DummyDriver.INSTANCE;
	static cvar_t s_impl;

	static Map<String, Sound> DRIVERS = new ConcurrentHashMap<>();
	
	/** 
	 * Searches for and initializes all known sound drivers.
	 */
	static {	    
		DRIVERS.put("dummy", DummyDriver.INSTANCE);

		try {
			Sys.initialize();
			SoundSystem.register("lwjgl", LWJGLSoundImpl.INSTANCE);
		} catch (Exception e) {
			// ignore the lwjgl driver if runtime not in classpath
			Com.DPrintf("could not init lwjgl sound driver class.");
		}

		// prefered driver
		try {
			Class.forName("com.jogamp.openal.AL");
			Class.forName("jake2.client.sound.joal.JOALSoundImpl");
		} catch (Exception e) {
			// ignore the joal driver if runtime not in classpath
			Com.DPrintf("could not init joal sound driver class.");
		}
	}
	
	/**
	 * Registers a new Sound Implementor.
	 */
	public static void register(String name, Sound driver) {
		if (driver == null) {
			throw new IllegalArgumentException("Sound implementation can't be null");
		}
		if (!DRIVERS.containsValue(driver)) {
			DRIVERS.put(name, driver);
		}
	}

	public static void register(Sound driver) {
		register(driver.getName(), driver);
	}

	/**
	 * Switches to the specific sound driver.
	 */
	public static void useDriver(String driverName) {
		driverInUse = DRIVERS.get(driverName);
	}
	
	/**
	 * Initializes the sound module.
	 */
	public static void Init() {
		
		Com.Printf("\n------- sound initialization -------\n");

		cvar_t cv = Cvar.getInstance().Get("s_initsound", "1", 0);
		if (cv.value == 0.0f) {
			Com.Printf("not initializing.\n");
			useDriver("dummy");
			return;			
		}

		// set the last registered driver as default
		String defaultDriver = "dummy";
		if (DRIVERS.size() > 1){
			defaultDriver = getDriverNames()[DRIVERS.size() - 1];
		}
		
		s_impl = Cvar.getInstance().Get("s_impl", defaultDriver, Defines.CVAR_ARCHIVE);
		useDriver(s_impl.string);

		if (driverInUse.Init()) {
			// driver ok
			Cvar.getInstance().Set("s_impl", driverInUse.getName());
		} else {
			// fallback
			useDriver("dummy");
		}
		
		Com.Printf("\n------- use sound driver \"" + driverInUse.getName() + "\" -------\n");
		StopAllSounds();
	}
	
	public static void Shutdown() {
		driverInUse.Shutdown();
	}
	
	/**
	 * Called before the sounds are to be loaded and registered.
	 */
	public static void BeginRegistration() {
		driverInUse.BeginRegistration();
	}
	
	/**
	 * Registers and loads a sound.
	 */
	public static sfx_t RegisterSound(String sample) {
		return driverInUse.RegisterSound(sample);
	}
	
	/**
	 * Called after all sounds are registered and loaded.
	 */
	public static void EndRegistration() {
		driverInUse.EndRegistration();
	}
	
	/**
	 * Starts a local sound.
	 */
	public static void StartLocalSound(String sound) {
		driverInUse.StartLocalSound(sound);
	}
	
	/** 
	 * StartSound - Validates the parms and ques the sound up
	 * if pos is NULL, the sound will be dynamically sourced from the entity
	 * Entchannel 0 will never override a playing sound
	 */
	public static void StartSound(float[] origin, int entnum, int entchannel, sfx_t sfx, float fvol, float attenuation, float timeofs) {
		driverInUse.StartSound(origin, entnum, entchannel, sfx, fvol, attenuation, timeofs);
	}

	/**
	 * Updates the sound renderer according to the changes in the environment,
	 * called once each time through the main loop.
	 */
	public static void Update(float[] origin, float[] forward, float[] right, float[] up) {
		driverInUse.Update(origin, forward, right, up);
	}

	/**
	 * Cinematic streaming and voice over network.
	 */
	public static void RawSamples(int samples, int rate, int width, int channels, ByteBuffer data) {
		driverInUse.RawSamples(samples, rate, width, channels, data);
	}
    
	/**
	 * Switches off the sound streaming.
	 */ 
    public static void disableStreaming() {
        driverInUse.disableStreaming();
    }

	/**
	 * Stops all sounds. 
	 */
	public static void StopAllSounds() {
		driverInUse.StopAllSounds();
	}
	
	public static String getDriverName() {
		return driverInUse.getName();
	}
	
	/**
	 * Returns a string array containing all sound driver names.
	 */
	public static String[] getDriverNames() {
		return DRIVERS.keySet().toArray(new String[0]);
	}
	
	/**
	 * This is used, when resampling to this default sampling rate is activated 
	 * in the wavloader. It is placed here that sound implementors can override 
	 * this one day.
	 */
	public static int getDefaultSampleRate()
	{
		return 44100;
	}
}