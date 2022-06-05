package dev.murad.shipping.block.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IPortalTileEntity {
    void linkPortals(ResourceKey<Level> targetLevel, BlockPos savedPos);
}
