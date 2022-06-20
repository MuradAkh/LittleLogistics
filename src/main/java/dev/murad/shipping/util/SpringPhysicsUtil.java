/*
MIT License

Copyright (c) 2018 Xavier "jglrxavpok" Niochaut

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class SpringPhysicsUtil {

    public static float computeTargetYaw(Float currentYaw, Vec3 anchorPos, Vec3 otherAnchorPos) {
        float idealYaw = (float) (Math.atan2(otherAnchorPos.x - anchorPos.x, -(otherAnchorPos.z - anchorPos.z)) * (180f/Math.PI));
        float closestDistance = Float.POSITIVE_INFINITY;
        float closest = idealYaw;
        for(int sign : Arrays.asList(-1, 0, 1)) {
            float potentialYaw = idealYaw + sign * 360f;
            float distance = Math.abs(potentialYaw - currentYaw);
            if(distance < closestDistance) {
                closestDistance = distance;
                closest = potentialYaw;
            }
        }
        return closest;
    }


    public static <T extends Entity & LinkableEntity<T>> void adjustSpringedEntities(T dominant, T dominated) {
        if (dominated.distanceTo(dominant) > 20) {
            dominated.removeDominant();
        }

        double distSq = dominant.distanceToSqr(dominated);
        double maxDstSq = dominant.getTrain().getTug().map(tug -> ((AbstractTugEntity) tug).isDocked() ? 1 : 1.2).orElse(1.2);

        Vec3 frontAnchor = dominant.position();
        Vec3 backAnchor = dominated.position();
        double dist = Math.sqrt(distSq);
        double dx = (frontAnchor.x - backAnchor.x) / dist;
        double dy = (frontAnchor.y - backAnchor.y) / dist;
        double dz = (frontAnchor.z - backAnchor.z) / dist;
        final double alpha = 0.5;


        float targetYaw = SpringPhysicsUtil.computeTargetYaw(dominated.getYRot(), frontAnchor, backAnchor);
        dominated.setYRot((float) ((alpha * dominated.getYRot() + targetYaw * (1f - alpha)) % 360));
        double k = dominant instanceof AbstractTugEntity ? 0.2 : 0.13;
        double l0 = maxDstSq;
        dominated.setDeltaMovement(k * (dist - l0) * dx, k * (dist - l0) * dy, k * (dist - l0) * dz);
    }
}
