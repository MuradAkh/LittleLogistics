package dev.murad.shipping.block.dock;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.InventoryUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.util.Direction;
import net.minecraft.core.BlockPos;

public class BargeDockTileEntity extends AbstractDockTileEntity {
    public BargeDockTileEntity(BlockEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public BargeDockTileEntity() {
        super(ModTileEntitiesTypes.BARGE_DOCK.get());
    }

    protected BlockPos getTargetBlockPos(){
        if (isExtract()) {
            return this.getBlockPos()
                    .below()
                    .relative(this.getBlockState().getValue(BargeDockBlock.FACING));
        } else return this.getBlockPos().above();
    }


    private boolean handleItemHopper(VesselEntity bargeEntity, HopperTileEntity hopper){
        if(!(bargeEntity instanceof IInventory)){
            return false;
        }
        if (isExtract()) {
            return InventoryUtils.mayMoveIntoInventory(hopper, (IInventory) bargeEntity);
        } else {
            return InventoryUtils.mayMoveIntoInventory((IInventory) bargeEntity, hopper);
        }
    }

    private Boolean isExtract() {
        return getBlockState().getValue(BargeDockBlock.EXTRACT_MODE);
    }


    @Override
    public boolean holdVessel(VesselEntity vessel, Direction direction) {
        if (!(vessel instanceof AbstractBargeEntity)
                || !getBlockState().getValue(BargeDockBlock.FACING).getOpposite().equals(direction))
        {
            return false;
        }

        return getHopper().map(h -> handleItemHopper(vessel, h))
                .orElse(getVesselLoader().map(l -> l.holdVessel(vessel, isExtract() ? IVesselLoader.Mode.IMPORT : IVesselLoader.Mode.EXPORT))
                        .orElse(false));
    }
}
