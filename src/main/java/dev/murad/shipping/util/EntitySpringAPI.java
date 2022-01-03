package dev.murad.shipping.util;
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


import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.entity.custom.tug.TugDummyHitboxEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class EntitySpringAPI {
    private EntitySpringAPI() {
    }

    private static final BiFunction<Entity, SpringEntity.SpringSide, Vector3d> DEFAULT_ANCHOR_LOCATION = (e, sideArg) -> e.position();
    private static final List<Predicate<Entity>> predicates = new ArrayList<>();
    private static final Map<Class<? extends Entity>, BiFunction<Entity, SpringEntity.SpringSide, Vector3d>> mapping = new HashMap<>();
    public static final BiFunction<Entity, SpringEntity.SpringSide, Vector3d> DEFAULT_BOAT_ANCHOR = (entity, side) -> {
        float distanceFromCenter = 0.0625f * 17f * (side == SpringEntity.SpringSide.DOMINANT ? 1f : -1f);
        double anchorX = entity.getX() + MathHelper.cos((float) ((entity.yRot + 90f) * Math.PI / 180f)) * distanceFromCenter;
        double anchorY = entity.getY();
        double anchorZ = entity.getZ() + MathHelper.sin((float)((entity.yRot + 90f) * Math.PI / 180f)) * distanceFromCenter;
        return new Vector3d(anchorX, anchorY, anchorZ);
    };

    static {
        addGenericAnchorMapping(BoatEntity.class, DEFAULT_BOAT_ANCHOR);
    }

    public static boolean isValidTarget(Entity target) {
        return target instanceof ISpringableEntity || target instanceof TugDummyHitboxEntity;
    }

    public static void addGenericAnchorMapping(Class<? extends Entity> entity, BiFunction<Entity, SpringEntity.SpringSide, Vector3d> function) {
        mapping.put(entity, function);
    }

    public static <T extends Entity> void addAnchorMapping(Class<? extends T> entity, BiFunction<T, SpringEntity.SpringSide, Vector3d> function) {
        mapping.put(entity, (e, side) -> function.apply((T) e, side));
    }

    public static Vector3d calculateAnchorPosition(Entity entity, SpringEntity.SpringSide side) {
        BiFunction<Entity, SpringEntity.SpringSide, Vector3d> function = mapping.getOrDefault(entity.getClass(), DEFAULT_ANCHOR_LOCATION);
        return function.apply(entity, side);
    }
}
