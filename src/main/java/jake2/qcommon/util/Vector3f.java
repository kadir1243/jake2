package jake2.qcommon.util;

import java.util.Objects;

import static java.lang.Math.*;

public class Vector3f {
    public static final Vector3f zero = new Vector3f(0f, 0f, 0f);
    public static final Vector3f one = new Vector3f(1f, 1f, 1f);
    public static final Vector3f unitX = new Vector3f(1f, 0f, 0f);
    public static final Vector3f unitY = new Vector3f(0f, 1f, 0f);
    public static final Vector3f unitZ = new Vector3f(0f, 0f, 1f);

    public final float x;
    public final float y;
    public final float z;

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(float[] vector) {
        this(vector[0], vector[1], vector[2]);
    }

    public Vector3f plus(Vector3f other) {
        return new Vector3f(x + other.x, y + other.y, z + other.z);
    }

    public Vector3f minus(Vector3f other) {
        return new Vector3f(x - other.x, y - other.y, z - other.z);
    }

    public Vector3f times(float scalar) {
        return new Vector3f(x * scalar, y * scalar, z * scalar);
    }

    public Vector3f div(float scalar) {
        return new Vector3f(x / scalar, y / scalar, z / scalar);
    }

    public Vector3f unaryMinus() {
        return new Vector3f(-x, -y, -z);
    }

    public float dot(Vector3f other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3f cross(Vector3f other) {
        return new Vector3f(y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public float length() {
        return (float) sqrt(x * x + y * y + z * z);
    }

    public Vector3f normalize() {
        return div(length());
    }

    public float distance(Vector3f other) {
        return this.minus(other).length();
    }

    public float angle(Vector3f other) {
        return (float) acos((this.dot(other)) / (this.length() * other.length()));
    }

    public Vector3f abs() {
        return new Vector3f(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public Vector3f lerp(Vector3f other, float t) {
        return this.plus(other.minus(this).times(t));
    }

    /**
     * Performs spherical linear interpolation (slerp) between this vector and the
     * specified vector.
     *
     * @param other the other vector
     * @param t the interpolation factor, in the range [0, 1]
     * @return the interpolated vector
     */
    public Vector3f slerp(Vector3f other, float t) {
        // Normalize both vectors
        var a = this.normalize();
        var b = other.normalize();

        // Compute the cosine of the angle between the vectors
        var dot = a.dot(b);

        // If the dot product is close to 1, then the vectors are close together,
        // and we can use simple linear interpolation
        if (dot > 0.9995) {
            return a.lerp(b, t);
        }

        // Clamp the dot product to the range [-1, 1] to avoid errors due to
        // floating-point imprecision
        var theta = acos(max(-1.0f, min(1.0f, dot)));

        // Compute the interpolated vector using spherical linear interpolation
        float s = (float) (1F / sin(theta));
        return a.times((float) sin((1 - t) * theta)).plus(b.times((float) sin(t * theta)).div(s));
    }

    public float[] toArray() {
        return new float[] {x, y, z};
    }

    public float[] toAngles() {
        float yaw;
        float pitch;
        if (y == 0f && x == 0f) {
            yaw = 0f;
            pitch = z > 0 ? 90f : 270f;
        } else {
            if (x != 0f) yaw = ((float)(int)(atan2(y, x) * 180 / Math.PI)); // todo: implement proper rounding
            else if (y > 0) yaw = 90f;
            else yaw = -90f;

            if (yaw < 0)
                yaw += 360f;
            var forward = sqrt((x * x + y * y));
            pitch = ((float)(int)(atan2(z, forward) * 180 / Math.PI)); // todo: implement proper rounding
            if (pitch < 0)
                pitch += 360f;
        }
        return new float[]{-pitch, yaw, 0f};
    }

    public Vector3f copy() {
        return new Vector3f(x, y, z);
    }

    @Override
    public String toString() {
        return "Vector3f{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof float[] array && array.length == 3) {
            return x == array[0] && y == array[1] && z == array[2];
        }
        if (!(o instanceof Vector3f vector3f)) return false;
        return x == vector3f.x && y == vector3f.y && z == vector3f.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
