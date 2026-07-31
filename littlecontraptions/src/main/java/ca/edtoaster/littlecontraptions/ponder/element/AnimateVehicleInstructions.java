package ca.edtoaster.littlecontraptions.ponder.element;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.world.phys.Vec3;

/**
 * Non-blocking Ponder instruction that nudges a scene vehicle entity a little each tick, so several
 * vehicles can animate concurrently while the storyboard idles (mirroring the old
 * {@code AnimateElementInstruction} behaviour, which is not available for entity elements in
 * standalone Ponder).
 */
public class AnimateVehicleInstructions extends PonderInstruction {

    private final ElementLink<EntityElement> link;
    private final int totalTicks;
    private final boolean rotate;
    private final Vec3 positionStep;
    private final float yawStep;
    private int remainingTicks;

    private AnimateVehicleInstructions(ElementLink<EntityElement> link, int ticks, boolean rotate,
                                       Vec3 positionStep, float yawStep) {
        this.link = link;
        this.totalTicks = Math.max(ticks, 0);
        this.remainingTicks = this.totalTicks;
        this.rotate = rotate;
        this.positionStep = positionStep;
        this.yawStep = yawStep;
    }

    public static AnimateVehicleInstructions move(ElementLink<EntityElement> link, Vec3 offset, int ticks) {
        Vec3 step = ticks <= 0 ? offset : offset.scale(1.0D / ticks);
        return new AnimateVehicleInstructions(link, ticks, false, step, 0.0F);
    }

    public static AnimateVehicleInstructions rotate(ElementLink<EntityElement> link, float yRotation, int ticks) {
        float step = ticks <= 0 ? yRotation : yRotation / ticks;
        return new AnimateVehicleInstructions(link, ticks, true, Vec3.ZERO, step);
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return remainingTicks <= 0;
    }

    @Override
    public void onScheduled(PonderScene scene) {
        if (totalTicks == 0) {
            applyStep(scene);
        }
    }

    @Override
    public void tick(PonderScene scene) {
        if (remainingTicks <= 0) {
            return;
        }
        applyStep(scene);
        remainingTicks--;
    }

    private void applyStep(PonderScene scene) {
        scene.resolveOptional(link).ifPresent(element -> element.ifPresent(entity -> {
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            float yaw = entity.getYRot();
            // Seed the previous position/rotation so Ponder interpolates smoothly over this tick.
            entity.moveTo(x, y, z, yaw, 0.0F);
            entity.setOldPosAndRot();
            if (rotate) {
                entity.moveTo(x, y, z, yaw + yawStep, 0.0F);
            } else {
                entity.moveTo(x + positionStep.x, y + positionStep.y, z + positionStep.z, yaw, 0.0F);
            }
        }));
    }
}
