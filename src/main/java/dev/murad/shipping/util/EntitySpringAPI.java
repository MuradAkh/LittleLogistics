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


import dev.murad.shipping.entity.custom.vessel.SpringEntity;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.tug.VehicleFrontPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class EntitySpringAPI {
    private EntitySpringAPI() {
    }

    private static final BiFunction<Entity, SpringEntity.SpringSide, Vec3> DEFAULT_ANCHOR_LOCATION = (e, sideArg) -> e.position();
    private static final List<Predicate<Entity>> predicates = new ArrayList<>();
    private static final Map<Class<? extends Entity>, BiFunction<Entity, SpringEntity.SpringSide, Vec3>> mapping = new HashMap<>();



    public static boolean isValidTarget(Entity target) {
        return target instanceof LinkableEntity || target instanceof VehicleFrontPart;
    }

    public static void addGenericAnchorMapping(Class<? extends Entity> entity, BiFunction<Entity, SpringEntity.SpringSide, Vec3> function) {
        mapping.put(entity, function);
    }

    public static <T extends Entity> void addAnchorMapping(Class<? extends T> entity, BiFunction<T, SpringEntity.SpringSide, Vec3> function) {
        mapping.put(entity, (e, side) -> function.apply((T) e, side));
    }

    public static Vec3 calculateAnchorPosition(Entity entity, SpringEntity.SpringSide side) {
        BiFunction<Entity, SpringEntity.SpringSide, Vec3> function = mapping.getOrDefault(entity.getClass(), DEFAULT_ANCHOR_LOCATION);
        return function.apply(entity, side);
    }
}
