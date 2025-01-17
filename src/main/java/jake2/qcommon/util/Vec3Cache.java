/*
 * Vec3Cache.java
 * Copyright (C) 2003
 *
 * $Id: Vec3Cache.java,v 1.2 2006-08-20 21:46:40 salomo Exp $
 */
package jake2.qcommon.util;

/**
 * Vec3Cache contains float[3] for temporary usage.
 * The usage can reduce the garbage at runtime.
 *
 * @author cwei
 */
public final class Vec3Cache {
    private static final float[][] cache = new float[64][3];
    private static int index = 0;

    public static float[] get() {
        //max = Math.max(index, max);
        return cache[index++];
    }
    
    public static void release() {
        index--;
    }

    public static void release(int count) {
        index-=count;
    }
}