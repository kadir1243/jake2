package jake2.qcommon;
/*
 * SZ.java
 * Copyright (C) 2003
 * 
 * $Id: SZ.java,v 1.5 2003-12-01 13:25:57 hoz Exp $
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
/**
 * SZ
 */
public final class SZ {

	/**
	 * @param buf
	 * @param data
	 * @param length
	 */
	public static void Init(sizebuf_t buf, byte[] data, int length) {
		buf = new sizebuf_t();
		buf.data = data;
		buf.maxsize = length;
	}
	
	public static void Print(sizebuf_t buf, byte[] data) {
	}
}
