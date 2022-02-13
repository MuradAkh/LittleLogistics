package dev.murad.shipping.block.vessel_detector;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class VesselDetectorTileEntity extends TileEntity implements ITickableTileEntity {
    private int cooldown = 0;

    public VesselDetectorTileEntity() {
        super(ModTileEntitiesTypes.VESSEL_DETECTOR.get());
    }

    private int getLimit(Direction direction){
        int i = 0;
        BlockPos pos = this.getBlockPos().relative(direction);
        for (; i < 4 && level.getBlockState(pos).is(Blocks.WATER); i++) {
            pos = pos.relative(direction);
        }
        return i;
    }

    private void checkForVessel(){
        Direction direction = this.getBlockState().getValue(VesselDetectorBlock.FACING);
        boolean found = !this.level.getEntities((Entity) null, getSearchBox(direction, getLimit(direction)), (e) -> e instanceof VesselEntity).isEmpty();
        this.getBlockState().setValue(VesselDetectorBlock.POWERED, found);
        level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(VesselDetectorBlock.POWERED, found));
    }

    private AxisAlignedBB getSearchBox(Direction direction, int limit) {
        return new AxisAlignedBB(
                this.getBlockPos().getX() - 0.5D,
                this.getBlockPos().getY() - 0.5D,
                this.getBlockPos().getZ() - 0.5D,
                this.getBlockPos().getX() + direction.getStepX() * limit + 0.5D,
                this.getBlockPos().getY() + direction.getStepY() * limit + 0.5D,
                this.getBlockPos().getZ() + direction.getStepZ() * limit + 0.5D);
    }

    @Override
    public void tick() {
        if(!this.level.isClientSide){
            if(cooldown < 0){
                cooldown = 10;
                checkForVessel();
            }else {
                cooldown--;
            }
        }

    }
}
