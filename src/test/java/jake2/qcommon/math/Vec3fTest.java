package jake2.qcommon.math;

import jake2.qcommon.util.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Vec3fTest {
    @Test
    public void slerpTest() {
        var a = new Vector3f(1f, 0f, 0f);
        var b = new Vector3f(0f, 1f, 0f);

        // Test t = 0
        var result1 = a.slerp(b, 0f);
        assertEquals(a, result1);

        // Test t = 1
        var result2 = a.slerp(b, 1f);
        assertEquals(b, result2);

        // Test t = 0.5
        var expected = new Vector3f(0.5f, 0.5f, 0f).normalize();
        var result3 = a.slerp(b, 0.5f);
        assertEquals(expected, result3);
    }

}
