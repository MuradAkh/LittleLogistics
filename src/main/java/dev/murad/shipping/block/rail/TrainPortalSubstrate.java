package dev.murad.shipping.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Denotes this block is a valid substrate for the train portal
 */
public interface TrainPortalSubstrate {
    void onTeleport(ServerLevel level, BlockState state, BlockPos pos);
}
