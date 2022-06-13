package dev.murad.shipping.block.rail;

import com.mojang.math.Vector3f;
import dev.murad.shipping.util.RailShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.Random;

public class ObsidianRail extends BaseRailBlock implements TrainPortalSubstrate {
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    public static final IntegerProperty RAIL_GLOW_LEVEL = IntegerProperty.create("glow_level", 0, 4);

    private static final ParticleOptions FIRE_PARTICLE = ParticleTypes.FLAME;

    public ObsidianRail(Properties pProperties) {
        super(true, pProperties);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return RAIL_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return FLAT_AABB;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(WATERLOGGED, RAIL_SHAPE, RAIL_GLOW_LEVEL);
    }

    @Override
    public boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(RAIL_GLOW_LEVEL) > 0;
    }

    private void doParticles(ServerLevel level, BlockState state, BlockPos pos) {
        if (state.getValue(RAIL_SHAPE) == RailShape.NORTH_SOUTH) {
            level.sendParticles(FIRE_PARTICLE, pos.getX() + .25, pos.getY() + 0.1, pos.getZ() + 0.5, 10,
                    0, 0, 0.5, 0.005);
            level.sendParticles(FIRE_PARTICLE, pos.getX() + .75, pos.getY() + 0.1, pos.getZ() + 0.5, 10,
                    0, 0, 0.5, 0.005);
        } else if (state.getValue(RAIL_SHAPE) == RailShape.EAST_WEST) {
            level.sendParticles(FIRE_PARTICLE, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + .25, 10,
                    0.5, 0, 0, 0.005);
            level.sendParticles(FIRE_PARTICLE, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + .75, 10,
                    0.5, 0, 0, 0.005);
        }
    }

    @Override
    public void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, Random random) {
        doParticles(level, state, pos);
        level.setBlock(pos,
                state.setValue(RAIL_GLOW_LEVEL,state.getValue(RAIL_GLOW_LEVEL) - 1),
                2); // 2 sends update to client
    }

    // server only
    @Override
    public void onTeleport(ServerLevel level, BlockState state, BlockPos pos) {
        doParticles(level, state, pos);
        level.setBlock(pos, state.setValue(RAIL_GLOW_LEVEL, 4), 2);
    }
}
