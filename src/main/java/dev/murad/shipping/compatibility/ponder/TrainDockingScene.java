package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModEntityTypes;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class TrainDockingScene {

    private static final double VEHICLE_Y = 1.0625;
    private static final double TRACK_Z = 3.5;
    private static final float EAST_YAW = -90.0F;

    private TrainDockingScene() {
    }

    static void docking(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("train_docking", "Docking a Train");
        scene.configureBasePlate(0, 0, 12);
        scene.scaleSceneView(0.8F);
        scene.showBasePlate();

        scene.world().showSection(util.select().fromTo(0, 1, 3, 11, 1, 3), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(7, 1, 2, 7, 2, 3), Direction.DOWN);
        scene.overlay().showControls(util.vector().topOf(7, 1, 2), Pointing.DOWN, 35)
                .withItem(new ItemStack(ModBlocks.DOCKING_STATION.get()));
        scene.overlay().showText(60)
                .colored(PonderPalette.INPUT)
                .text("Place a Docking Station beside a straight section of track.")
                .pointAt(util.vector().centerOf(7, 1, 2))
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(util.select().fromTo(6, 1, 2, 6, 2, 3), Direction.DOWN);
        scene.overlay().showOutline(PonderPalette.INPUT, "docking-station-line",
                util.select().fromTo(6, 1, 2, 7, 2, 3), 70);
        scene.overlay().showText(70)
                .text("Adjacent, equally-facing docks form one station. Include a position for the locomotive and each car you want to service.")
                .pointAt(util.vector().centerOf(6, 1, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showOutline(PonderPalette.OUTPUT, "locomotive-dock",
                util.select().position(7, 1, 3), 60);
        scene.overlay().showOutline(PonderPalette.INPUT, "car-dock",
                util.select().position(6, 1, 3), 60);
        scene.overlay().showText(70)
                .text("The locomotive passes the earlier position and stops at the front, aligning its linked car behind it.")
                .pointAt(util.vector().centerOf(7, 1, 3))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(6, 2, 2), Pointing.DOWN, 45)
                .withItem(new ItemStack(Items.BLUE_DYE))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("This blue dock matches the blue car, while the red dock matches the locomotive.")
                .pointAt(util.vector().centerOf(6, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(6, 1, 1, 6, 2, 1), Direction.SOUTH);
        scene.overlay().showOutline(PonderPalette.INPUT, "docking-station-port",
                util.select().position(6, 1, 2), 70);
        scene.overlay().showText(70)
                .text("Connect automation to the controller face pointing away from the track.")
                .pointAt(util.vector().centerOf(6, 1, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().of(5.8, 3.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.IRON_INGOT));
        scene.overlay().showControls(util.vector().of(6.5, 3.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showControls(util.vector().of(7.2, 3.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.REDSTONE));
        scene.overlay().showText(60)
                .text("An occupied dock exposes the vehicle's item, fluid, or energy storage.")
                .pointAt(util.vector().centerOf(6, 1, 2))
                .placeNearTarget();
        scene.idle(70);

        ElementLink<EntityElement> locomotive = scene.world().createEntity(level -> {
            PonderSteamLocomotiveEntity entity = new PonderSteamLocomotiveEntity(level);
            initializeVehicle(entity, 2.5, DyeColor.RED);
            return entity;
        });
        ElementLink<EntityElement> chestCar = scene.world().createEntity(level -> {
            PonderChestCarEntity entity = new PonderChestCarEntity(level);
            initializeVehicle(entity, 1.5, DyeColor.BLUE);
            return entity;
        });
        scene.idle(15);

        scene.overlay().showText(60)
                .colored(PonderPalette.INPUT)
                .text("A matching train travelling through the station docks automatically.")
                .pointAt(util.vector().centerOf(5, 1, 3))
                .placeNearTarget();
        moveTrain(scene, locomotive, chestCar, 2.5, 7.5);
        scene.idle(30);

        scene.overlay().showOutline(PonderPalette.OUTPUT, "occupied-docks",
                util.select().fromTo(6, 1, 3, 7, 1, 3), 70);
        scene.overlay().showText(70)
                .text("The locomotive claims the front dock while linked cars claim the matching docks behind it.")
                .pointAt(util.vector().centerOf(6, 1, 3))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("The attached hopper, pipe, pump, or cable controls whether cargo is loaded or unloaded.")
                .pointAt(util.vector().centerOf(6, 1, 2))
                .placeNearTarget();
        animateItemTransfer(scene);
        scene.idle(30);

        scene.overlay().showControls(util.vector().topOf(7, 1, 2), Pointing.DOWN, 45)
                .withItem(new ItemStack(Items.CLOCK));
        scene.overlay().showText(80)
                .text("Transfers reset the idle timer. The whole train waits while any occupied dock is still active.")
                .pointAt(util.vector().centerOf(7, 1, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(7, 1, 2), Pointing.DOWN, 45)
                .rightClick();
        scene.overlay().showText(80)
                .text("Right-click a controller to configure its timeout, name, statistics, and redstone behavior.")
                .pointAt(util.vector().centerOf(7, 1, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(60)
                .colored(PonderPalette.OUTPUT)
                .text("When every dock becomes idle, the train continues its journey.")
                .pointAt(util.vector().centerOf(7, 1, 3))
                .placeNearTarget();
        moveTrain(scene, locomotive, chestCar, 7.5, 11.5);
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void moveTrain(SceneBuilder scene, ElementLink<EntityElement> locomotive,
                                  ElementLink<EntityElement> chestCar, double fromX, double toX) {
        int steps = (int) Math.round((toX - fromX) / 0.25);
        for (int step = 1; step <= steps; step++) {
            double locomotiveX = fromX + step * 0.25;
            moveVehicle(scene, locomotive, locomotiveX - 0.25, locomotiveX);
            moveVehicle(scene, chestCar, locomotiveX - 1.25, locomotiveX - 1.0);
            scene.idle(1);
        }
    }

    private static void moveVehicle(SceneBuilder scene, ElementLink<EntityElement> link,
                                    double previousX, double x) {
        scene.world().modifyEntity(link, entity -> {
            entity.moveTo(previousX, VEHICLE_Y, TRACK_Z, EAST_YAW, 0.0F);
            entity.setOldPosAndRot();
            entity.moveTo(x, VEHICLE_Y, TRACK_Z, EAST_YAW, 0.0F);
        });
    }

    private static void initializeVehicle(Entity entity, double x, DyeColor color) {
        entity.moveTo(x, VEHICLE_Y, TRACK_Z, EAST_YAW, 0.0F);
        entity.setOldPosAndRot();
        if (entity instanceof dev.murad.shipping.entity.Colorable colorable) {
            colorable.setColor(color.getId());
        }
    }

    private static void animateItemTransfer(SceneBuilder scene) {
        Vec3[] path = {
                new Vec3(6.5, 2.8, 1.5),
                new Vec3(6.5, 2.1, 1.5),
                new Vec3(6.5, 1.7, 2.0),
                new Vec3(6.5, 1.45, 2.5),
                new Vec3(6.5, 1.3, 3.0),
                new Vec3(6.5, 1.3, 3.5)
        };

        for (int item = 0; item < 3; item++) {
            ElementLink<EntityElement> cargo = scene.world().createItemEntity(
                    path[0], Vec3.ZERO, new ItemStack(Items.IRON_INGOT));
            scene.world().modifyEntity(cargo, entity -> entity.setNoGravity(true));
            for (int point = 1; point < path.length; point++) {
                Vec3 previous = path[point - 1];
                Vec3 next = path[point];
                scene.world().modifyEntity(cargo, entity -> {
                    entity.moveTo(previous.x, previous.y, previous.z);
                    entity.setOldPosAndRot();
                    entity.moveTo(next.x, next.y, next.z);
                    entity.setDeltaMovement(Vec3.ZERO);
                });
                scene.idle(4);
            }
            scene.world().modifyEntity(cargo, Entity::discard);
            scene.idle(3);
        }
    }

    private static final class PonderSteamLocomotiveEntity extends SteamLocomotiveEntity {
        private PonderSteamLocomotiveEntity(Level level) {
            super(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level);
        }

        @Override
        public void tick() {
        }
    }

    private static final class PonderChestCarEntity extends ChestCarEntity {
        private PonderChestCarEntity(Level level) {
            super(ModEntityTypes.CHEST_CAR.get(), level);
        }

        @Override
        public void tick() {
        }
    }
}
