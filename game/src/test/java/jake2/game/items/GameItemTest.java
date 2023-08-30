package jake2.game.items;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static jake2.qcommon.Defines.EF_COLOR_SHELL;
import static jake2.qcommon.Defines.EF_ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameItemTest {
    @Test
    public void testTwoFlags() {
        final int flags = GameItem.parseFlags("EF_ROTATE|EF_COLOR_SHELL", Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL));
        assertEquals(EF_ROTATE | EF_COLOR_SHELL, flags);
    }

    @Test
    public void testSingeFlags() {
        final int flags = GameItem.parseFlags("EF_ROTATE", Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL));
        assertEquals(EF_ROTATE, flags);
    }

    @Test
    public void testEmptyFlags() {
        final int flags = GameItem.parseFlags("", Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL));
        assertEquals(0, flags);
    }

    @Test
    public void testNullFlags() {
        final int flags = GameItem.parseFlags(null, Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL));
        assertEquals(0, flags);
    }

    @Test
    public void testEmptyDictFlags() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> GameItem.parseFlags("EF_ROTATE|EF_COLOR_SHELL", null));
    }

    @Test
    public void testUnknownFlags() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> GameItem.parseFlags("EF_ROTATE|EF_BLAH_BLAH", Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL)));
    }

    @Test
    public void testUnknownFlag() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> GameItem.parseFlags("EF_BLAH_BLAH", Map.of(
                "EF_ROTATE", EF_ROTATE,
                "EF_COLOR_SHELL", EF_COLOR_SHELL)));
    }


}
