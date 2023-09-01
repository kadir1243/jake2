package jake2.qcommon;

public class MathLib {
    public static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }
}
