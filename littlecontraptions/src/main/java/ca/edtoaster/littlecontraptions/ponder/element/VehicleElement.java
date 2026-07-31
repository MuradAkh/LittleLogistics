package ca.edtoaster.littlecontraptions.ponder.element;

import ca.edtoaster.littlecontraptions.entity.ContraptionBargeEntity;
import ca.edtoaster.littlecontraptions.setup.LCEntityTypes;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.vessel.barge.ChestBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FishingBargeEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Lightweight handle to a vehicle spawned into a Ponder scene as an {@link EntityElement}.
 *
 * <p>The old Create 0.5.1 implementation rendered vehicles through a custom
 * {@code AnimatedSceneElement}. Standalone Ponder (Create 6.0) instead spawns entities into the
 * Ponder world via {@code scene.world().createEntity(...)}, so this class is now just a wrapper
 * around the returned {@link ElementLink} plus a set of "frozen" entity factories whose
 * {@code tick()} is a no-op (matching Little Logistics' own Ponder scenes) so the display entities
 * do not self-move.
 */
public class VehicleElement {

    private final ElementLink<EntityElement> link;

    public VehicleElement(ElementLink<EntityElement> link) {
        this.link = link;
    }

    public ElementLink<EntityElement> getLink() {
        return link;
    }

    /** Creates a vehicle entity for a Ponder scene at spawn time. */
    @FunctionalInterface
    public interface EntityFactory {
        Entity create(Level level);
    }

    public static final EntityFactory STEAM_TUG = FrozenSteamTug::new;
    public static final EntityFactory ENERGY_TUG = FrozenEnergyTug::new;
    public static final EntityFactory CHEST_BARGE = FrozenChestBarge::new;
    public static final EntityFactory FISHING_BARGE = FrozenFishingBarge::new;
    public static final EntityFactory CONTRAPTION_BARGE = FrozenContraptionBarge::new;
    public static final EntityFactory STEAM_LOCOMOTIVE = FrozenSteamLocomotive::new;
    public static final EntityFactory ENERGY_LOCOMOTIVE = FrozenEnergyLocomotive::new;
    public static final EntityFactory CHEST_CAR = FrozenChestCar::new;

    private static final class FrozenSteamTug extends SteamTugEntity {
        private FrozenSteamTug(Level level) {
            super(ModEntityTypes.STEAM_TUG.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenEnergyTug extends EnergyTugEntity {
        private FrozenEnergyTug(Level level) {
            super(ModEntityTypes.ENERGY_TUG.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenChestBarge extends ChestBargeEntity {
        private FrozenChestBarge(Level level) {
            super(ModEntityTypes.CHEST_BARGE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenFishingBarge extends FishingBargeEntity {
        private FrozenFishingBarge(Level level) {
            super(ModEntityTypes.FISHING_BARGE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenContraptionBarge extends ContraptionBargeEntity {
        private FrozenContraptionBarge(Level level) {
            super(LCEntityTypes.CONTRAPTION_BARGE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenSteamLocomotive extends SteamLocomotiveEntity {
        private FrozenSteamLocomotive(Level level) {
            super(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenEnergyLocomotive extends EnergyLocomotiveEntity {
        private FrozenEnergyLocomotive(Level level) {
            super(ModEntityTypes.ENERGY_LOCOMOTIVE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class FrozenChestCar extends ChestCarEntity {
        private FrozenChestCar(Level level) {
            super(ModEntityTypes.CHEST_CAR.get(), level);
        }

        @Override
        public void tick() {
        }
    }
}
