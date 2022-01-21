package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.dock.AbstractDockBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.PathType;
import net.minecraft.pathfinding.SwimNodeProcessor;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class TugNodeProcessor extends SwimNodeProcessor {
    public TugNodeProcessor() {
        super(false);
    }

    @Override
    public int getNeighbors(PathPoint[] p_222859_1_, PathPoint p_222859_2_) {
        int i = 0;

        for(Direction direction : Arrays.asList(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH)) {
            PathPoint pathpoint = this.getWaterNode(p_222859_2_.x + direction.getStepX(), p_222859_2_.y + direction.getStepY(), p_222859_2_.z + direction.getStepZ());
            if (pathpoint != null && !pathpoint.closed) {
                BlockPos pos = pathpoint.asBlockPos();
                int penalty = 0;
                for (BlockPos surr : Arrays.asList(pos.east(), pos.west(), pos.south(), pos.north(), pos.north().west(), pos.north().east(), pos.south().east(), pos.south().west())){
                    Block block = level.getBlockState(surr).getBlock();
                    if(!block.is(Blocks.WATER)){
                        penalty += 1;
                        if(block instanceof AbstractDockBlock || block.is(ModBlocks.GUIDE_RAIL_CORNER.get())){
                            penalty = 0;
                            break;
                        }
                    }
                }
                pathpoint.costMalus += Math.min(penalty, 3);
                p_222859_1_[i++] = pathpoint;
            }
        }

        return i;
    }

    private PathPoint getWaterNode(int p_186328_1_, int p_186328_2_, int p_186328_3_) {
        PathNodeType pathnodetype = this.isFree(p_186328_1_, p_186328_2_, p_186328_3_);
        return  pathnodetype != PathNodeType.WATER ? null : this.getNode(p_186328_1_, p_186328_2_, p_186328_3_);
    }

    private PathNodeType isFree(int p_186327_1_, int p_186327_2_, int p_186327_3_) {
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(int i = p_186327_1_; i < p_186327_1_ + this.entityWidth; ++i) {
            for(int j = p_186327_2_; j < p_186327_2_ + this.entityHeight; ++j) {
                for(int k = p_186327_3_; k < p_186327_3_ + this.entityDepth; ++k) {
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable.set(i, j, k));
                    BlockState blockstate = this.level.getBlockState(blockpos$mutable.set(i, j, k));
                    if (fluidstate.isEmpty() && blockstate.isPathfindable(this.level, blockpos$mutable.below(), PathType.WATER) && blockstate.isAir()) {
                        return PathNodeType.BREACH;
                    }

                    if (!fluidstate.is(FluidTags.WATER)) {
                        return PathNodeType.BLOCKED;
                    }
                }
            }
        }

        BlockState blockstate1 = this.level.getBlockState(blockpos$mutable);
        return blockstate1.isPathfindable(this.level, blockpos$mutable, PathType.WATER) ? PathNodeType.WATER : PathNodeType.BLOCKED;
    }

}
