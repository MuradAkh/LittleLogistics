package dev.murad.shipping.block.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public interface IPortalBlock {
    EnumProperty<PortalMode> PORTAL_MODE = EnumProperty.create("train_portal_mode", PortalMode.class);

    enum PortalMode implements StringRepresentable {
        UNLINKED("unlinked"),
        SENDER("sender"),
        RECEIVER("receiver");

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

}
