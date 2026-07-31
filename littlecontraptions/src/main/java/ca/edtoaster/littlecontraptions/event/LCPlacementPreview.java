package ca.edtoaster.littlecontraptions.event;

import ca.edtoaster.littlecontraptions.LCMod;
import ca.edtoaster.littlecontraptions.item.BargeAssemblerItem;
import ca.edtoaster.littlecontraptions.setup.LCItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * While the player holds the Barge Assembler and aims at a water surface, outlines the block that
 * would be replaced with particles, so it is clear where the assembler will be placed.
 */
@EventBusSubscriber(modid = LCMod.MOD_ID, value = Dist.CLIENT)
public class LCPlacementPreview {

    private static final Vector3f HIGHLIGHT_COLOR = new Vector3f(0.25f, 0.6f, 1.0f);
    // Only emit the outline every few ticks so it reads as a steady frame, not a dense cloud.
    private static final int EMIT_INTERVAL_TICKS = 3;
    private static final int POINTS_PER_EDGE = 4;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || mc.isPaused()) {
            return;
        }

        boolean holding = player.getMainHandItem().is(LCItems.BARGE_ASSEMBLER.get())
                || player.getOffhandItem().is(LCItems.BARGE_ASSEMBLER.get());
        if (!holding || level.getGameTime() % EMIT_INTERVAL_TICKS != 0) {
            return;
        }

        BlockHitResult hit = BargeAssemblerItem.getWaterSurfaceTarget(level, player);
        if (hit != null) {
            outlineTopFace(level, hit.getBlockPos());
        }
    }

    private static void outlineTopFace(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        double y = pos.getY() + fluid.getHeight(level, pos) + 0.02;
        double x = pos.getX();
        double z = pos.getZ();
        DustParticleOptions dust = new DustParticleOptions(HIGHLIGHT_COLOR, 1.0f);

        for (int i = 0; i <= POINTS_PER_EDGE; i++) {
            double t = (double) i / POINTS_PER_EDGE;
            level.addParticle(dust, x + t, y, z, 0, 0, 0);
            level.addParticle(dust, x + t, y, z + 1, 0, 0, 0);
            level.addParticle(dust, x, y, z + t, 0, 0, 0);
            level.addParticle(dust, x + 1, y, z + t, 0, 0, 0);
        }
    }
}
