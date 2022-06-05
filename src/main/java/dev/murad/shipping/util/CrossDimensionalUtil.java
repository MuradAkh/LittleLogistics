package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public class CrossDimensionalUtil {
    public static Vec3 horizontalOverworldDistance(DimensionType dimension1, DimensionType dimension2, BlockPos blockPos1, BlockPos blockPos2) {
        Vec3 pos1 = Vec3.atCenterOf(blockPos1).multiply(dimension1.coordinateScale(), 0, dimension1.coordinateScale());
        Vec3 pos2 = Vec3.atCenterOf(blockPos2).multiply(dimension2.coordinateScale(), 0, dimension2.coordinateScale());

        return pos2.subtract(pos1);
    }
}
