package jake2.game;

import jake2.qcommon.util.Math3D;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnapToEightsTest {
    private static final Map<Float, Float> TEST_DATA = Map.of(
            0f, 0f,
            0.02f, 0f,
            0.05f, 0f,
            0.07f, 0.125f,
            0.18f, 0.125f,
            0.19f, 0.25f,
            -0.18f, -0.125f,
            -0.19f, -0.25f
    );

    @Test
    public void testSnapToEights() {
        TEST_DATA.forEach((key, value) -> assertEquals(value, Math3D.snapToEights(key), key + ".snapToEights() -> " + value));
    }
}
