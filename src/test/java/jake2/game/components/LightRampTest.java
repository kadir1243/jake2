package jake2.game.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LightRampTest {
    @Test
    public void testUpdateInterpolation() {
        var lightRamp = new LightRamp(0, 10, 1f /* default parameters: */, 0f, false, 0f, -1);
        var interpolatedValue1 = lightRamp.update(0.1f);
        assertEquals(1, interpolatedValue1);
        var interpolatedValue2 = lightRamp.update(0.4f);
        assertEquals(5, interpolatedValue2);
    }

    @Test
    public void testUpdateFractionClamping() {
        var lightRamp = new LightRamp(0, 10, 1f /* default parameters: */, 0f, false, 0f, -1);
        var maxValue = lightRamp.update(1.5f);
        assertEquals(1f, lightRamp.getFraction());
        assertEquals(10, maxValue);
    }

    @Test
    public void testToggle() {
        var lightRamp = new LightRamp(0, 25, 5f /* default parameters: */, 0f, false, 0f, -1);
        lightRamp.toggle(10f);
        assertEquals(25, lightRamp.getStart());
        assertEquals(0, lightRamp.getEnd());
        assertEquals(0f, lightRamp.getFraction());
        assertEquals(15f, lightRamp.getTargetTime());
        var newValue = lightRamp.update(1f);
        assertEquals(20, newValue);
    }
}
