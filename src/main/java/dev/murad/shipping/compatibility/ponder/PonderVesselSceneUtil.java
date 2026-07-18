package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.entity.Colorable;
import dev.murad.shipping.entity.custom.vessel.barge.ChestBargeEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

final class PonderVesselSceneUtil {

    static final double VESSEL_Y = 1.7;

    private PonderVesselSceneUtil() {
    }

    static SteamTugEntity steamTug(Level level) {
        return new FrozenSteamTugEntity(level);
    }

    static ChestBargeEntity chestBarge(Level level) {
        return new FrozenChestBargeEntity(level);
    }

    static void initialize(Entity entity, double x, double z, float yaw, DyeColor color) {
        entity.moveTo(x, VESSEL_Y, z, yaw, 0.0F);
        entity.setOldPosAndRot();
        if (entity instanceof Colorable colorable) {
            colorable.setColor(color.getId());
        }
    }

    static void move(SceneBuilder scene, ElementLink<EntityElement> link,
                     double previousX, double previousZ, double x, double z, float yaw) {
        scene.world().modifyEntity(link, entity -> {
            entity.moveTo(previousX, VESSEL_Y, previousZ, yaw, 0.0F);
            entity.setOldPosAndRot();
            entity.moveTo(x, VESSEL_Y, z, yaw, 0.0F);
        });
    }

    private static final class FrozenSteamTugEntity extends SteamTugEntity {
        private FrozenSteamTugEntity(Level level) {
            super(ModEntityTypes.STEAM_TUG.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenChestBargeEntity extends ChestBargeEntity {
        private FrozenChestBargeEntity(Level level) {
            super(ModEntityTypes.CHEST_BARGE.get(), level);
        }

        @Override
        public void tick() {
        }
    }
}
