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

package jake2.qcommon;

import jake2.qcommon.exec.Cmd;
import jake2.qcommon.exec.Cvar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jake2.qcommon.exec.Cmd.getArguments;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCMD {

    @Test
    public void testSampleCommand() {
        List<String> result = new ArrayList<>();
        Cmd.AddCommand("test", (List<String> args) -> {
            result.add("success");
        });
        Cmd.ExecuteString("test");
        Assertions.assertEquals("success", result.get(0));
    }

    @Test
    public void testTokenizeString() {
        List<String> result = Cmd.TokenizeString("echo test", false);
        Assertions.assertEquals(Arrays.asList("echo", "test"), result);
    }

    @Test
    public void testTokenizeWithExpansion() {
        Cvar.getInstance().Get("test1", "world", 0);
        List<String> result = Cmd.TokenizeString("echo $test1 again", true);
        Assertions.assertEquals(Arrays.asList("echo", "world", "again"), result);
    }

    @Test
    public void testTokenizeWithQuotes() {
        List<String> result = Cmd.TokenizeString("echo \"hello world\"", true);
        Assertions.assertEquals(Arrays.asList("echo", "hello world"), result);
    }

    @Test
    public void testTokenizeWithQuotesWithExpansion() {
        Cvar.getInstance().Get("test2", "world", 0);
        List<String> result = Cmd.TokenizeString("echo \"$test2\"", true);
        Assertions.assertEquals(Arrays.asList("echo", "$test2"), result);
    }

    @Test
    public void testTokenizeWithExpansionFalse() {
        Cvar.getInstance().Get("test1", "world", 0);
        List<String> result = Cmd.TokenizeString("echo $test1 again", false);
        Assertions.assertEquals(Arrays.asList("echo", "$test1", "again"), result);
    }

    @Test
    public void testExpandMacro() {
        Cvar.getInstance().Get("name", "world", 0);
        String result = Cmd.MacroExpandString("hello $name");
        Assertions.assertEquals("hello world", result);
    }

    @Test
    public void testExpandMacroMiddle() {
        Cvar.getInstance().Get("name1", "world", 0);
        String result = Cmd.MacroExpandString("hello $name1 again");
        Assertions.assertEquals("hello world again", result);
    }

    @Test
    public void testCommandWithVariable() {
        StringBuilder result = new StringBuilder();
        Cvar.getInstance().Get("test_var", "value", 0);
        Cmd.AddCommand("test_cmd", (List<String> args) -> result.append("success ").append(getArguments(args)));

        Cmd.ExecuteString("test_cmd $test_var");
        Assertions.assertEquals("success value", result.toString());
    }

    @Test
    public void testGetArguments() {
        List<String> args = Arrays.asList("echo", "hello", "world");
        assertEquals("hello world", getArguments(args));
    }

    @Test
    public void testGetArgumentsSecond() {
        List<String> args = Arrays.asList("echo", "hello", "world");
        assertEquals("world", getArguments(args, 2));
    }
}
