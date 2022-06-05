package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public class CrossDimensionalUtil {
    public static Vec3 horizontalOverworldDistance(double originDimScale, double targetDimScale, BlockPos blockPos1, BlockPos blockPos2) {
        Vec3 pos1 = Vec3.atCenterOf(blockPos1).multiply(originDimScale, 0, originDimScale);
        Vec3 pos2 = Vec3.atCenterOf(blockPos2).multiply(targetDimScale, 0, targetDimScale);

        return pos2.subtract(pos1);
    }

    public static BlockPos getPosInDimension(double originDimScale, double targetDimScale, BlockPos originPos) {
        Vec3 inUnit = Vec3.atLowerCornerOf(originPos).multiply(originDimScale, 1, originDimScale);
        Vec3 target = inUnit.multiply(1 / targetDimScale, 1, 1 / targetDimScale);

        return new BlockPos(target);
    }
}
