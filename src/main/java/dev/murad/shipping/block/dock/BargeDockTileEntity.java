package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.ModBargeEntity;
import dev.murad.shipping.entity.custom.tug.TugEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

public class BargeDockTileEntity extends AbstractDockTileEntity {
    public BargeDockTileEntity(TileEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public BargeDockTileEntity() {
        super(ModTileEntitiesTypes.BARGE_DOCK.get());
    }


    @Override
    public boolean holdVessel(IInventory vessel, Direction direction) {
        if (!(vessel instanceof ModBargeEntity) || !getBlockState().getValue(BargeDockBlock.FACING).getOpposite().equals(direction)){
            return false;
        }

        return getInsertHopper().map(hopper -> mayMoveIntoInventory(vessel, hopper)).orElse(false);
    }
}
