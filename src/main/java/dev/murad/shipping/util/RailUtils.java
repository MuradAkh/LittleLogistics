package dev.murad.shipping.util;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.Map;

public class RailUtils {
    public static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), (p_38135_) -> {
        Vec3i vec3i = Direction.WEST.getNormal();
        Vec3i vec3i1 = Direction.EAST.getNormal();
        Vec3i vec3i2 = Direction.NORTH.getNormal();
        Vec3i vec3i3 = Direction.SOUTH.getNormal();
        Vec3i vec3i4 = vec3i.below();
        Vec3i vec3i5 = vec3i1.below();
        Vec3i vec3i6 = vec3i2.below();
        Vec3i vec3i7 = vec3i3.below();
        p_38135_.put(RailShape.NORTH_SOUTH, Pair.of(vec3i2, vec3i3));
        p_38135_.put(RailShape.EAST_WEST, Pair.of(vec3i, vec3i1));
        p_38135_.put(RailShape.ASCENDING_EAST, Pair.of(vec3i4, vec3i1));
        p_38135_.put(RailShape.ASCENDING_WEST, Pair.of(vec3i, vec3i5));
        p_38135_.put(RailShape.ASCENDING_NORTH, Pair.of(vec3i2, vec3i7));
        p_38135_.put(RailShape.ASCENDING_SOUTH, Pair.of(vec3i6, vec3i3));
        p_38135_.put(RailShape.SOUTH_EAST, Pair.of(vec3i3, vec3i1));
        p_38135_.put(RailShape.SOUTH_WEST, Pair.of(vec3i3, vec3i));
        p_38135_.put(RailShape.NORTH_WEST, Pair.of(vec3i2, vec3i));
        p_38135_.put(RailShape.NORTH_EAST, Pair.of(vec3i2, vec3i1));
    });
}
