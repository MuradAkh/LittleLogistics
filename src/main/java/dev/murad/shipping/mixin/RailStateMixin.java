package dev.murad.shipping.mixin;

import dev.murad.shipping.block.rail.MultiShapeRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows vanilla rails to detect connections to MultiShapeRail blocks (switch rails,
 * junction rails, etc.) on sides that aren't represented by the rail's current RailShape.
 * Vanilla RailShape only supports 2 connections, but our custom rails connect 3+ sides.
 */
@Mixin(RailState.class)
public class RailStateMixin {

    @Shadow @Final private BlockPos pos;
    @Shadow @Final private BaseRailBlock block;
    @Shadow private BlockState state;

    @Inject(method = "connectsTo", at = @At("HEAD"), cancellable = true)
    private void littlelogistics_connectsTo(RailState other, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.block instanceof MultiShapeRail multiShapeRail)) return;

        BlockPos otherPos = ((RailStateAccessor) other).getPos();
        int dx = otherPos.getX() - this.pos.getX();
        int dz = otherPos.getZ() - this.pos.getZ();
        if (Math.abs(dx) + Math.abs(dz) != 1) return;

        Direction dir = dx == 1 ? Direction.EAST
                      : dx == -1 ? Direction.WEST
                      : dz == 1 ? Direction.SOUTH
                      : Direction.NORTH;

        if (multiShapeRail.getAllConnectedDirections(this.state).contains(dir)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Prevent vanilla RailState from recomputing and overwriting a MultiShapeRail's blockstate.
     * Custom rail shapes are managed by the mod, not vanilla's 2-connection RailShape logic.
     */
    @Inject(method = "connectTo", at = @At("HEAD"), cancellable = true)
    private void littlelogistics_connectTo(RailState other, CallbackInfo ci) {
        if (this.block instanceof MultiShapeRail) {
            ci.cancel();
        }
    }
}
