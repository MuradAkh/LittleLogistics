package dev.murad.shipping.block.dockingstation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * The two positions that make up a docking station's crane structure.
 * All positions are defined relative to the CONTROLLER block, using:
 *   inwardSteps = steps in the direction FACING.getOpposite()
 *   upSteps     = steps upward
 *
 * Layout (viewed from the side, inward direction going right):
 *
 *   Y+1:  LEFT_TOP
 *   Y+0:  CONTROLLER
 *                   (vehicle lane)
 *
 * The crane arm rendered by the CONTROLLER model still visually overhangs the
 * inward vehicle lane; it is drawn by the model, not backed by a block.
 */
public enum DockingStationPart implements StringRepresentable {

    CONTROLLER ("controller",  0, 0),
    LEFT_TOP   ("left_top",    0, 1);

    private final String serializedName;
    /** Steps in the FACING.getOpposite() (inward) direction from the controller. */
    public final int inwardSteps;
    /** Steps upward from the controller. */
    public final int upSteps;

    DockingStationPart(String serializedName, int inwardSteps, int upSteps) {
        this.serializedName = serializedName;
        this.inwardSteps = inwardSteps;
        this.upSteps = upSteps;
    }

    /**
     * Given this part's world position and the FACING direction stored on the block state,
     * returns the world position of the CONTROLLER block.
     *
     * Controller is reached by reversing the inward offset (stepping in FACING direction)
     * and stepping down by upSteps.
     */
    public BlockPos controllerPos(BlockPos pos, Direction facing) {
        return pos.relative(facing, inwardSteps).below(upSteps);
    }

    /**
     * Given the CONTROLLER's world position and the FACING direction,
     * returns the world position where this part should be placed.
     */
    public BlockPos partPos(BlockPos controllerPos, Direction facing) {
        Direction inward = facing.getOpposite();
        return controllerPos.relative(inward, inwardSteps).above(upSteps);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
