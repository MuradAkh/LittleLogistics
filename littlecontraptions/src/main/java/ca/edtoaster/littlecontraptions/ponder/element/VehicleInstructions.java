package ca.edtoaster.littlecontraptions.ponder.element;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Helper mirroring the old {@code VehicleInstructions} API, but backed by standalone Ponder's
 * {@code scene.world().createEntity(...)} / {@code modifyEntity(...)} instead of a custom scene
 * element.
 */
public class VehicleInstructions {

    private final SceneBuilder builder;

    public VehicleInstructions(SceneBuilder builder) {
        this.builder = builder;
    }

    public VehicleElement createVehicle(Vec3 location, float angle, VehicleElement.EntityFactory factory) {
        ElementLink<EntityElement> link = builder.world().createEntity(level -> {
            Entity entity = factory.create(level);
            entity.moveTo(location.x, location.y, location.z, angle, 0.0F);
            entity.setOldPosAndRot();
            return entity;
        });
        return new VehicleElement(link);
    }

    public void rotateVehicle(VehicleElement vehicle, float yRotation, int duration) {
        builder.addInstruction(AnimateVehicleInstructions.rotate(vehicle.getLink(), yRotation, duration));
    }

    public void moveVehicle(VehicleElement vehicle, Vec3 offset, int duration) {
        builder.addInstruction(AnimateVehicleInstructions.move(vehicle.getLink(), offset, duration));
    }

    public void removeVehicle(VehicleElement vehicle) {
        builder.world().modifyEntity(vehicle.getLink(), Entity::discard);
    }
}
