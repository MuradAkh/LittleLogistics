package dev.murad.shipping.mixin;

import dev.murad.shipping.block.rail.PortalRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* Cancels comeOffTrack() when a minecart is inside a nether portal adjacent to a PortalRail,
   so the cart keeps its velocity instead of derailing. */
@Mixin(AbstractMinecart.class)
public class PortalRailTraversalMixin {

    @Inject(method = "comeOffTrack", at = @At("HEAD"), cancellable = true)
    private void ll_portalRailPassthrough(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        BlockPos pos = self.blockPosition();
        Level level = self.level();
        if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof PortalRail) {
                self.move(MoverType.SELF, self.getDeltaMovement());
                ci.cancel();
                return;
            }
        }
    }
}