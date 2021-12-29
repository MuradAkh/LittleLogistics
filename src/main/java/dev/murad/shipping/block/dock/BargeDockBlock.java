package dev.murad.shipping.block.dock;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class BargeDockBlock extends AbstractDockBlock {
    public BargeDockBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntitiesTypes.BARGE_DOCK.get().create();
    }
}
