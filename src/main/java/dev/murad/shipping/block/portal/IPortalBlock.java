package dev.murad.shipping.block.portal;

import dev.murad.shipping.util.CrossDimensionalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public interface IPortalBlock {
    DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    EnumProperty<PortalMode> PORTAL_MODE = EnumProperty.create("train_portal_mode", PortalMode.class);

    Set<ResourceKey<Level>> validDims();

    enum PortalMode implements StringRepresentable {
        UNLINKED("unlinked"),
        LINKED("linked");

        private final String name;

        PortalMode(String name){
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    default boolean checkValidLinkPair(Level destinationLevel, BlockPos savedPos, BlockPos clickedPos, ResourceKey<Level> dimension, double scale){
        Vec3 diff = CrossDimensionalUtil.distanceInDimension(
                scale, destinationLevel.dimensionType().coordinateScale(), savedPos, clickedPos);
        return Math.abs(diff.x) <= linkRadius(destinationLevel.dimensionType()) && Math.abs(diff.z) <= linkRadius(destinationLevel.dimensionType());
    }

    default boolean checkValidDimension(Level level) {
        return validDims().contains(level.dimension());
    }


    int linkRadius();

    default int linkRadius(DimensionType dimensionType){
        return (int) (linkRadius() / dimensionType.coordinateScale());
    }

}
