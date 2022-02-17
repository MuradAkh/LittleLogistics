package dev.murad.shipping.block.vessel_detector;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;

public class VesselDetectorTileEntity extends BlockEntity implements TickableBlockEntity {
    private static final int MAX_RANGE = 3;
    private int cooldown = 0;

    public VesselDetectorTileEntity() {
        super(ModTileEntitiesTypes.VESSEL_DETECTOR.get());
    }

    private static boolean isValidBlock(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.AIR);
    }

    private static int getSearchLimit(BlockPos pos, Direction direction, World level){
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

    public static AxisAlignedBB getSearchBox(BlockPos pos, Direction direction, World level) {
        int searchLimit = getSearchLimit(pos.relative(direction), direction, level);

        Direction.AxisDirection posNeg = direction.getAxisDirection();
        BlockPos start = posNeg == Direction.AxisDirection.POSITIVE ? pos.relative(direction) : pos;

        int offX = direction.getStepX() == 0 ? 1 : direction.getStepX() * searchLimit;
        int offY = direction.getStepY() == 0 ? 1 : direction.getStepY() * searchLimit;
        int offZ = direction.getStepZ() == 0 ? 1 : direction.getStepZ() * searchLimit;

        BlockPos end = start.offset(offX, offY, offZ);

        return new AxisAlignedBB(start, end);
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
