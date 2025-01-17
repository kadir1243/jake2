/*
 * Renderer.java
 * Copyright (C) 2003
 *
 * $Id: Renderer.java,v 1.14 2011-07-07 21:19:14 salomo Exp $
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

import jake2.client.refexport_t;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

import java.util.Vector;

/**
 * Renderer
 * 
 * @author cwei
 */
@Environment(EnvType.CLIENT)
public class Renderer {

    static RenderAPI fastRenderer = new jake2.client.render.fast.Misc();
    // rst: lets use the fast renderer from now on
    //static RenderAPI basicRenderer = new jake2.client.render.basic.Misc();

    static Vector<Ref> drivers = new Vector<>(3);

    static {
        try {
            Class.forName("jake2.client.render.LwjglRenderer");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void register(Ref impl) {
        if (impl == null) {
            throw new IllegalArgumentException(
                    "Ref implementation can't be null");
        }
        if (!drivers.contains(impl)) {
            drivers.add(impl);
        }
    }

    /**
     * Factory method to get the Renderer implementation.
     * 
     * @return refexport_t (Renderer singleton)
     */
    public static refexport_t getDriver(String driverName) {
        return getDriver(driverName, true);
    }

    /**
     * Factory method to get the Renderer implementation.
     * 
     * @return refexport_t (Renderer singleton)
     */
    public static refexport_t getDriver(String driverName, boolean fast) {
        // find a driver
        for (Ref o : drivers) {
            if (o.getName().equals(driverName)) {
                // lets use the fast renderer only
                return o.GetRefAPI((fast) ? fastRenderer : fastRenderer);
            }
        }
        // null if driver not found
        return null;
    }

    public static String getDefaultName() {
        return drivers.stream().findFirst().map(Ref::getName).orElse(null);
    }

    public static String getPreferedName() {
        return (drivers.isEmpty()) ? null : drivers.lastElement()
                .getName();
    }

    public static String[] getDriverNames() {
        return drivers.stream().map(Ref::getName).toArray(String[]::new);
    }

}