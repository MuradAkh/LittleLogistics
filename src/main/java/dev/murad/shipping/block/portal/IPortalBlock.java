package dev.murad.shipping.block.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.dimension.DimensionType;

public interface IPortalBlock {
    DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    EnumProperty<PortalMode> PORTAL_MODE = EnumProperty.create("train_portal_mode", PortalMode.class);

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

    boolean checkValidLinkPair(Level level, ItemStack stack, BlockPos pos, ResourceKey<Level> dimension);

    boolean checkValidDimension(Level level);

    int linkRadius();

    default int linkRadius(DimensionType dimensionType){
        return (int) (linkRadius() / dimensionType.coordinateScale());
    }

}
