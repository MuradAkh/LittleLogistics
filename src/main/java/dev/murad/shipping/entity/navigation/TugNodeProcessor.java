package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.guide_rail.TugGuideRailBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.pathfinding.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

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
            if (pathpoint != null && !pathpoint.closed && !isOppositeGuideRail(pathpoint, direction)) {
                p_222859_1_[i++] = pathpoint;
            }
        }

        return i;
    }

    private boolean isOppositeGuideRail(PathPoint pathPoint, Direction direction){
        BlockState state = this.level.getBlockState(pathPoint.asBlockPos().below());
        if (state.is(ModBlocks.GUIDE_RAIL_TUG.get())){
            return TugGuideRailBlock.getArrowsDirection(state).getOpposite().equals(direction);
        }
        return false;
    }

    private PathPoint getNodeSimple(int p_176159_1_, int p_176159_2_, int p_176159_3_) {
        return this.nodes.computeIfAbsent(PathPoint.createHash(p_176159_1_, p_176159_2_, p_176159_3_), (p_215743_3_) -> {
            return new PathPoint(p_176159_1_, p_176159_2_, p_176159_3_);
        });
    }

    @Override
    public FlaggedPathPoint getGoal(double p_224768_1_, double p_224768_3_, double p_224768_5_) {
        return new FlaggedPathPoint(getNodeSimple(MathHelper.floor(p_224768_1_), MathHelper.floor(p_224768_3_), MathHelper.floor(p_224768_5_)));
    }

    @Override
    protected PathPoint getNode(int p_176159_1_, int p_176159_2_, int p_176159_3_) {
        PathPoint pathpoint = super.getNode(p_176159_1_, p_176159_2_, p_176159_3_);
        if (pathpoint != null) {
            BlockPos pos = pathpoint.asBlockPos();
            float penalty = 0;
            for (BlockPos surr : Arrays.asList(
                    pos.east(),
                    pos.west(),
                    pos.south(),
                    pos.north(),
                    pos.north().west(),
                    pos.north().east(),
                    pos.south().east(),
                    pos.south().west(),
                    pos.north().west().north().west(),
                    pos.north().east().north().east(),
                    pos.south().west().south().west(),
                    pos.south().east().south().east()
            )
            ){
                // if the point's neighbour has land, penalty is 5 unless there is a dock
                if(!level.getBlockState(surr).is(Blocks.WATER)){
                    penalty = 5f;
                }
                if(
                        level.getBlockState(surr).is(ModBlocks.GUIDE_RAIL_CORNER.get()) ||
                                level.getBlockState(surr).is(ModBlocks.BARGE_DOCK.get()) ||
                                level.getBlockState(surr).is(ModBlocks.TUG_DOCK.get())

                ){
                    penalty = 0;
                    break;
                }
            }
            pathpoint.costMalus += penalty;
        }


        return pathpoint;
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
