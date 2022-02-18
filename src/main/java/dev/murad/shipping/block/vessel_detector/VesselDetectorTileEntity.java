package dev.murad.shipping.block.vessel_detector;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class VesselDetectorTileEntity extends BlockEntity  {
    private static final int MAX_RANGE = 3;
    private int cooldown = 0;

    public VesselDetectorTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.VESSEL_DETECTOR.get(), pos, state);
    }

    private static boolean isValidBlock(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.AIR);
    }

    private static int getSearchLimit(BlockPos pos, Direction direction, Level level){
        int i = 0;
        for (; i < MAX_RANGE && isValidBlock(level.getBlockState(pos)); i++) {
            pos = pos.relative(direction);
        }
        return i;
    }

    private void checkForVessel(){
        Direction direction = this.getBlockState().getValue(VesselDetectorBlock.FACING);
        boolean found = !this.level.getEntities((Entity) null, getSearchBox(this.getBlockPos(), direction, this.level),
                (e) -> e instanceof VesselEntity).isEmpty();
        boolean previousPowered = this.getBlockState().getValue(VesselDetectorBlock.POWERED);

        this.getBlockState().setValue(VesselDetectorBlock.POWERED, found);
        level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(VesselDetectorBlock.POWERED, found));

        if (found != previousPowered) {
            // update back neighbour
            BlockPos neighbour = getBlockPos().relative(direction.getOpposite());
            Block block = getBlockState().getBlock();
            this.level.neighborChanged(neighbour, block, getBlockPos());
            this.level.updateNeighborsAtExceptFromFacing(neighbour, block, direction);
        }
    }

    public static AABB getSearchBox(BlockPos pos, Direction direction, Level level) {
        int searchLimit = getSearchLimit(pos.relative(direction), direction, level);

        Direction.AxisDirection posNeg = direction.getAxisDirection();
        BlockPos start = posNeg == Direction.AxisDirection.POSITIVE ? pos.relative(direction) : pos;

        int offX = direction.getStepX() == 0 ? 1 : direction.getStepX() * searchLimit;
        int offY = direction.getStepY() == 0 ? 1 : direction.getStepY() * searchLimit;
        int offZ = direction.getStepZ() == 0 ? 1 : direction.getStepZ() * searchLimit;

        BlockPos end = start.offset(offX, offY, offZ);

        return new AABB(start, end);
    }

    public void serverTickInternal(){
        if(cooldown < 0){
            cooldown = 10;
            checkForVessel();
        }else {
            cooldown--;
        }
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, VesselDetectorTileEntity e) {
        e.serverTickInternal();
    }
}
