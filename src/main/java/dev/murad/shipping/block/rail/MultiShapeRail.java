package dev.murad.shipping.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.Set;

public interface MultiShapeRail {
    /**
     * Set the automatic rail state of this rail
     * @param state current blockstate of the rail
     * @return if state was set automatically (or in the case of manual rail,
     *         if the state conforms to the inputs already)
     */
    boolean setRailState(BlockState state, Level world, BlockPos pos, Direction in, Direction out);

    /**
     * Returns the directions a train can exit toward when entering from {@code inputSide}.
     * For automatic rails, this may return multiple options. For manual/redstone rails,
     * this respects the current powered state and may return empty if the input side
     * is on the inactive branch.
     *
     * @param state     current blockstate of the rail
     * @param inputSide the direction the train is arriving from (e.g. NORTH means
     *                  the train is entering from the north side, moving southward)
     * @return set of directions the train can exit toward, or empty if entry from
     *         this side is not currently allowed
     */
    Set<Direction> getExitDirections(BlockState state, Direction inputSide);

    /**
     * Returns alternative directions the locomotive navigator should check first when
     * approaching from {@code entrance}. This is used to prefer straight-through paths
     * over branching — e.g. a train entering from the powered branch of a switch rail
     * should check the unpowered (straight) direction first before committing to a turn.
     *
     * @param state    current blockstate of the rail
     * @param entrance the direction the train is arriving from
     * @return set of directions to prioritize checking, or empty if no priority applies
     */
    Set<Direction> getPreferredExits(BlockState state, Direction entrance);

    /**
     * @param direction Direction of travel for the train
     */
    RailShape getVanillaRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction);

    boolean isAutomaticSwitching();

    /**
     * Returns all directions this rail structurally connects to, regardless of powered state.
     * Used by the RailState mixin to determine vanilla rail auto-connection.
     */
    Set<Direction> getConnectedSides(BlockState state);

    /**
     * Trigger a reshape on adjacent vanilla rails so they connect to this multi-shape rail.
     * Call from onPlace after the custom rail has been placed in the world.
     */
    static void reshapeNeighborRails(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (BaseRailBlock.isRail(neighborState) && !(neighborState.getBlock() instanceof MultiShapeRail)) {
                BaseRailBlock railBlock = (BaseRailBlock) neighborState.getBlock();
                RailShape shape = railBlock.getRailDirection(neighborState, level, neighborPos, null);
                new RailState(level, neighborPos, neighborState)
                        .place(level.hasNeighborSignal(neighborPos), true, shape);
            }
        }
    }
}
