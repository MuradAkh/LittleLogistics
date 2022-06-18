package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public class CrossDimensionalUtil {
    public static Vec3 distanceInDimension(double originDimScale, double targetDimScale, BlockPos blockPos1, BlockPos blockPos2) {
        return Vec3.atLowerCornerOf(CrossDimensionalUtil.getPosInDimension(originDimScale,
                targetDimScale, blockPos1)).subtract(Vec3.atLowerCornerOf(blockPos2));
    }

    public static BlockPos getPosInDimension(double originDimScale, double targetDimScale, BlockPos originPos) {
        Vec3 inUnit = Vec3.atLowerCornerOf(originPos).multiply(originDimScale, 1, originDimScale);
        Vec3 target = inUnit.multiply(1 / targetDimScale, 1, 1 / targetDimScale);

        return new BlockPos(target);
    }
}
