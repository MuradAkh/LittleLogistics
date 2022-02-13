package dev.murad.shipping.block.vessel_detector;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VesselDetectorTileEntity extends TileEntity implements ITickableTileEntity {
    private int cooldown = 0;

    public VesselDetectorTileEntity() {
        super(ModTileEntitiesTypes.VESSEL_DETECTOR.get());
    }

    private static boolean isValidBlock(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.AIR);
    }

    private static int getSearchLimit(BlockPos initialPos, Direction direction, World level){
        int i = 0;
        BlockPos pos = initialPos.relative(direction);
        for (; i < 4 && isValidBlock(level.getBlockState(pos)); i++) {
            pos = pos.relative(direction);
        }
        return i;
    }

    private void checkForVessel(){
        Direction direction = this.getBlockState().getValue(VesselDetectorBlock.FACING);
        boolean found = !this.level.getEntities((Entity) null, getSearchBox(this.getBlockPos(), direction, this.level),
                (e) -> e instanceof VesselEntity).isEmpty();
        this.getBlockState().setValue(VesselDetectorBlock.POWERED, found);
        level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(VesselDetectorBlock.POWERED, found));
    }

    public static AxisAlignedBB getSearchBox(BlockPos initialPos, Direction direction, World level) {
        int searchLimit = getSearchLimit(initialPos, direction, level);
        return new AxisAlignedBB(
                initialPos.getX() - 0.5D,
                initialPos.getY() - 0.5D,
                initialPos.getZ() - 0.5D,
                initialPos.getX() + direction.getStepX() * searchLimit + 0.5D,
                initialPos.getY() + direction.getStepY() * searchLimit + 0.5D,
                initialPos.getZ() + direction.getStepZ() * searchLimit + 0.5D);
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
