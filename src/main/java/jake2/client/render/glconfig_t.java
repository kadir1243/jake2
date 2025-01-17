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

// Created on 20.11.2003 by RST.
// $Id: glconfig_t.java,v 1.3 2006-11-21 00:51:22 cawe Exp $

package jake2.client.render;

import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

@Environment(EnvType.CLIENT)
public class glconfig_t {
    
	public int renderer;
	public String renderer_string;
	public String vendor_string;
	public String version_string;
	public String extensions_string;

	public boolean allow_cds;
	
	private float version = 1.1f;

	public void parseOpenGLVersion() {
	    try {
		version = Float.parseFloat(version_string.substring(0, 3));
	    } catch (Exception e) {
		version = 1.1f;
	    }
	}
	
	public float getOpenGLVersion() {
	    return version;
	}
}
