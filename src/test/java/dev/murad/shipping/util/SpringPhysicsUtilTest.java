package dev.murad.shipping.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpringPhysicsUtilTest {

    private static final float EPSILON = 0.001f;

    @Test
    void computeTargetYaw_straightNorth() {
        // anchor at origin, other anchor to the north (negative Z)
        float yaw = SpringPhysicsUtil.computeTargetYaw(0f, new Vec3(0, 0, 0), new Vec3(0, 0, -1));
        // atan2(dx=0, -dz=1) = 0 degrees
        assertEquals(0f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightEast() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(90f, new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        // atan2(dx=1, -dz=0) = 90 degrees
        assertEquals(90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightSouth() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(180f, new Vec3(0, 0, 0), new Vec3(0, 0, 1));
        // atan2(dx=0, -dz=-1) = 180 degrees
        assertEquals(180f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightWest() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(-90f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0));
        // atan2(dx=-1, -dz=0) = -90 degrees
        assertEquals(-90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_choosesClosestWrap() {
        // Current yaw is 170, ideal is -170 (equivalent to 190).
        // Without wrapping, distance is 340. With +360 wrap (190), distance is 20.
        float yaw = SpringPhysicsUtil.computeTargetYaw(170f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0.176f));
        // Should pick the value closest to 170, which is the +360 variant
        assertTrue(Math.abs(yaw - 170f) < 180f, "Should choose wrap closest to current yaw 170, got " + yaw);
    }

    @Test
    void computeTargetYaw_negativeCurrentYaw() {
        // Current yaw is -170, ideal is 170.
        // Should wrap to -190 (170 - 360) which is 20 away from -170.
        float yaw = SpringPhysicsUtil.computeTargetYaw(-170f, new Vec3(0, 0, 0), new Vec3(1, 0, 0.176f));
        assertTrue(Math.abs(yaw - (-170f)) < 180f, "Should choose wrap closest to current yaw -170, got " + yaw);
    }
}
