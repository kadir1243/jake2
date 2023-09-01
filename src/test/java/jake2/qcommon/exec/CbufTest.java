package jake2.qcommon.exec;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static jake2.qcommon.exec.Cbuf.splitCommandLine;
import static org.junit.jupiter.api.Assertions.*;

public class CbufTest {
    @Test
    public void testSplit() {
        assertEquals(Arrays.asList("hello", "world"), splitCommandLine("hello;world"));
    }

    @Test
    public void testSplitInQuotes() {
        assertEquals(Arrays.asList("test", "hello;world"), splitCommandLine("test;\"hello;world\""));
    }

    @Test
    public void testNewLines() {
        assertEquals(Arrays.asList("1", "2", "3", "4"), splitCommandLine("1\n2\r3;4"));
    }
}
