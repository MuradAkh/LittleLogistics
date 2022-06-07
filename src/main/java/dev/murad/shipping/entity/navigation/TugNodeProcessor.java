package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.guiderail.TugGuideRailBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.*;

import java.util.Arrays;

public class TugNodeProcessor extends SwimNodeEvaluator {
    public TugNodeProcessor() {
        super(false);
    }

    private boolean isOppositeGuideRail(Node Node, Direction direction){
        BlockState state = this.level.getBlockState(Node.asBlockPos().below());
        if (state.is(ModBlocks.GUIDE_RAIL_TUG.get())){
            return TugGuideRailBlock.getArrowsDirection(state).getOpposite().equals(direction);
        }
        return false;
    }

    @Override
    public int getNeighbors(Node[] p_222859_1_, Node p_222859_2_) {
        int i = 0;

        for(Direction direction : Arrays.asList(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH)) {
            Node Node = this.getWaterNode(p_222859_2_.x + direction.getStepX(), p_222859_2_.y + direction.getStepY(), p_222859_2_.z + direction.getStepZ());
            if (Node != null && !Node.closed && !isOppositeGuideRail(Node, direction)) {
                p_222859_1_[i++] = Node;
            }
        }

        return i;
    }

    private Node getNodeSimple(int p_176159_1_, int p_176159_2_, int p_176159_3_) {
        return this.nodes.computeIfAbsent(Node.createHash(p_176159_1_, p_176159_2_, p_176159_3_), (p_215743_3_) -> {
            return new Node(p_176159_1_, p_176159_2_, p_176159_3_);
        });
    }

    @Override
    public Target getGoal(double p_224768_1_, double p_224768_3_, double p_224768_5_) {
        return new Target(getNodeSimple(Mth.floor(p_224768_1_), Mth.floor(p_224768_3_), Mth.floor(p_224768_5_)));
    }

    @Override
    protected Node getNode(int p_176159_1_, int p_176159_2_, int p_176159_3_) {
        Node Node = super.getNode(p_176159_1_, p_176159_2_, p_176159_3_);
        if (Node != null) {
            BlockPos pos = Node.asBlockPos();
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
                if(level.getFluidState(surr).isEmpty()){
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
            Node.costMalus += penalty;
        }


        return Node;
    }

    private Node getWaterNode(int p_186328_1_, int p_186328_2_, int p_186328_3_) {
        BlockPathTypes types = this.getCachedBlockType(p_186328_1_, p_186328_2_, p_186328_3_);
        return  (types != BlockPathTypes.WATER && types != BlockPathTypes.LAVA) ? null : this.getNode(p_186328_1_, p_186328_2_, p_186328_3_);
    }

    private BlockPathTypes isFree(int p_186327_1_, int p_186327_2_, int p_186327_3_) {
        BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();

        for(int i = p_186327_1_; i < p_186327_1_ + this.entityWidth; ++i) {
            for(int j = p_186327_2_; j < p_186327_2_ + this.entityHeight; ++j) {
                for(int k = p_186327_3_; k < p_186327_3_ + this.entityDepth; ++k) {
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable.set(i, j, k));
                    BlockState blockstate = this.level.getBlockState(blockpos$mutable.set(i, j, k));
                    if (fluidstate.isEmpty() && blockstate.isPathfindable(this.level, blockpos$mutable.below(), PathComputationType.WATER) && blockstate.isAir()) {
                        return BlockPathTypes.BREACH;
                    }

                    if (fluidstate.isEmpty()) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
            }
        }

        BlockState blockstate1 = this.level.getBlockState(blockpos$mutable);
        if(blockstate1.isPathfindable(this.level, blockpos$mutable, PathComputationType.WATER))
            return BlockPathTypes.WATER;
        else if(blockstate1.is(Blocks.LAVA))
            return BlockPathTypes.WATER;
        else return BlockPathTypes.BLOCKED;
    }

    public BlockPathTypes getBlockPathType(BlockGetter pBlockaccess, int pX, int pY, int pZ, Mob pEntityliving, int pXSize, int pYSize, int pZSize, boolean pCanBreakDoors, boolean pCanEnterDoors) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(int i = pX; i < pX + pXSize; ++i) {
            for(int j = pY; j < pY + pYSize; ++j) {
                for(int k = pZ; k < pZ + pZSize; ++k) {
                    FluidState fluidstate = pBlockaccess.getFluidState(blockpos$mutableblockpos.set(i, j, k));
                    BlockState blockstate = pBlockaccess.getBlockState(blockpos$mutableblockpos.set(i, j, k));
                    if (fluidstate.isEmpty() && blockstate.isPathfindable(pBlockaccess, blockpos$mutableblockpos.below(), PathComputationType.WATER) && blockstate.isAir()) {
                        return BlockPathTypes.BREACH;
                    }

                    if (fluidstate.isEmpty()) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
            }
        }

        FluidState state = this.level.getFluidState(blockpos$mutableblockpos);
        if(!state.isEmpty())
            return BlockPathTypes.WATER;
        else return BlockPathTypes.BLOCKED;

    }

}
