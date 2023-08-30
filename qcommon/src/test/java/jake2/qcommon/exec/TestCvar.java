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

// Created on 29.12.2003 by RST.
// $Id: TestCvar.java,v 1.1 2004-07-07 19:59:56 hzi Exp $

package jake2.qcommon.exec;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCvar {

	@Test
	public void testInit() {
		Cvar.getInstance().Init();
	}

	@Test
	public void testGet() {
		Cvar.getInstance().Set("rene", "is cool.");

		Assertions.assertEquals("is cool.", Cvar.getInstance().Get("rene", "default", 0).string);
	}

	@Test
	public void testGetDefault() {
		Cvar.getInstance().Set("rene1", "is cool.");

		Assertions.assertEquals("default", Cvar.getInstance().Get("hello", "default", 0).string);
	}

	@Test
	public void testGetDefaultNull() {
		Cvar.getInstance().Set("rene2", "is cool.");

		Assertions.assertNull(Cvar.getInstance().Get("hello2", null, 0));
	}

	@Test
	public void testFind() {
		Cvar.getInstance().Set("rene3", "is cool.");

		Assertions.assertEquals("is cool.", Cvar.getInstance().FindVar("rene3").string);
	}

	@Test
	public void testVariableString() {
		Cvar.getInstance().Set("rene4", "is cool.");
		Assertions.assertEquals("is cool.", Cvar.getInstance().VariableString("rene4"));
	}

	@Test
	public void testFullSetCreateNew() {
		Cvar.getInstance().FullSet("rene5", "0.56", 0);

		cvar_t rene5 = Cvar.getInstance().FindVar("rene5");
		Assertions.assertNotNull(rene5);
		Assertions.assertEquals("0.56", rene5.string);
		Assertions.assertTrue(rene5.value > 0.5f);
	}

	@Test
	public void testFullSetOverwrite() {
		Cvar.getInstance().FullSet("rene6", "0.56", 0);
		Cvar.getInstance().FullSet("rene6", "10.6", 0);

		cvar_t rene6 = Cvar.getInstance().FindVar("rene6");
		Assertions.assertNotNull(rene6);
		Assertions.assertTrue(rene6.modified);
		Assertions.assertEquals("10.6", rene6.string);
		Assertions.assertTrue(rene6.value > 0.5f);

	}
}
