package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class TrainRoutingScene {

    private static final double VEHICLE_Y = 1.0625;
    private static final double PREVIEW_Y = 1.2;
    private static final double MOVEMENT_SAMPLE_DISTANCE = 0.25;
    private static final int VEHICLE_GAP_SAMPLES = 4;
    private static final int LOCOMOTIVE_START_SAMPLE = 8;

    private static final RoutePoint[] LOOP_PREVIEW = {
            new RoutePoint(3.0, 8.5),
            new RoutePoint(8.0, 8.5),
            new RoutePoint(8.5, 8.0),
            new RoutePoint(8.5, 3.0),
            new RoutePoint(8.0, 2.5),
            new RoutePoint(3.0, 2.5),
            new RoutePoint(2.5, 3.0),
            new RoutePoint(2.5, 8.0),
            new RoutePoint(3.0, 8.5)
    };

    private static final List<RoutePoint> MOVEMENT_PATH = samplePath(new RoutePoint[] {
            new RoutePoint(5.5, 8.5),
            new RoutePoint(8.0, 8.5),
            new RoutePoint(8.5, 8.0),
            new RoutePoint(8.5, 3.0),
            new RoutePoint(8.0, 2.5),
            new RoutePoint(3.0, 2.5),
            new RoutePoint(2.5, 3.0),
            new RoutePoint(2.5, 8.0),
            new RoutePoint(3.0, 8.5),
            new RoutePoint(8.0, 8.5),
            new RoutePoint(8.5, 8.0)
    });

    private TrainRoutingScene() {
    }

    static void routing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("train_routing", "Routing a Train");
        scene.configureBasePlate(0, 0, 15);
        scene.showBasePlate();
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(10);

        ElementLink<EntityElement> locomotive = scene.world().createEntity(level -> {
            PonderSteamLocomotiveEntity entity = new PonderSteamLocomotiveEntity(level);
            initializeVehicle(entity, MOVEMENT_PATH.get(LOCOMOTIVE_START_SAMPLE), -90.0F);
            return entity;
        });
        ElementLink<EntityElement> firstCar = scene.world().createEntity(level -> {
            PonderChestCarEntity entity = new PonderChestCarEntity(level);
            initializeVehicle(entity, MOVEMENT_PATH.get(LOCOMOTIVE_START_SAMPLE - VEHICLE_GAP_SAMPLES), -90.0F);
            return entity;
        });
        ElementLink<EntityElement> secondCar = scene.world().createEntity(level -> {
            PonderChestCarEntity entity = new PonderChestCarEntity(level);
            initializeVehicle(entity, MOVEMENT_PATH.get(LOCOMOTIVE_START_SAMPLE - 2 * VEHICLE_GAP_SAMPLES), -90.0F);
            return entity;
        });
        scene.idle(20);

        scene.overlay().showControls(util.vector().topOf(8, 1, 8), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.CONDUCTORS_WRENCH.get()))
                .rightClick();
        scene.overlay().showText(60)
                .text("Use the Conductor's Wrench to adjust junction rails.")
                .pointAt(util.vector().centerOf(8, 1, 8))
                .placeNearTarget();
        scene.idle(70);

        scene.overlay().showControls(util.vector().topOf(3, 1, 8), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.LOCO_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Right-click rails with a Locomotive Route to add waypoints in travel order.")
                .pointAt(util.vector().centerOf(3, 1, 8))
                .placeNearTarget();
        showPreview(scene, util, 0, 2, 70);
        scene.idle(80);

        scene.overlay().showOutline(PonderPalette.INPUT, "route-automatic-junction", util.select().position(8, 1, 8), 70);
        scene.overlay().showText(70)
                .text("A waypoint beyond an Automatic T-Junction Rail determines which path the train follows.")
                .pointAt(util.vector().centerOf(8, 1, 8))
                .placeNearTarget();
        showPreview(scene, util, 2, 6, 70);
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(3, 1, 8), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.LOCO_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Click the first waypoint again to compile a repeating loop.")
                .pointAt(util.vector().centerOf(3, 1, 8))
                .placeNearTarget();
        showPreview(scene, util, 6, 8, 70);
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(7, 1, 8), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.LOCO_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .text("Open the locomotive and place the completed route in its route slot.")
                .pointAt(util.vector().centerOf(7, 1, 8))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Start the engine to send the entire vehicle chain around the route.")
                .pointAt(util.vector().centerOf(8, 1, 8))
                .placeNearTarget();
        scene.idle(20);

        for (int point = LOCOMOTIVE_START_SAMPLE + 1; point < MOVEMENT_PATH.size(); point++) {
            moveTrain(scene, locomotive, firstCar, secondCar,
                    MOVEMENT_PATH.get(point), MOVEMENT_PATH.get(point - VEHICLE_GAP_SAMPLES),
                    MOVEMENT_PATH.get(point - 2 * VEHICLE_GAP_SAMPLES),
                    MOVEMENT_PATH.get(point - 1), MOVEMENT_PATH.get(point - VEHICLE_GAP_SAMPLES - 1),
                    MOVEMENT_PATH.get(point - 2 * VEHICLE_GAP_SAMPLES - 1));
            scene.idle(1);
        }
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void moveTrain(SceneBuilder scene, ElementLink<EntityElement> locomotive,
                                  ElementLink<EntityElement> firstCar, ElementLink<EntityElement> secondCar,
                                  RoutePoint locomotivePosition, RoutePoint firstCarPosition, RoutePoint secondCarPosition,
                                  RoutePoint locomotivePrevious, RoutePoint firstCarPrevious, RoutePoint secondCarPrevious) {
        scene.world().modifyEntity(locomotive,
                entity -> moveVehicle(entity, locomotivePosition, locomotivePrevious));
        scene.world().modifyEntity(firstCar,
                entity -> moveVehicle(entity, firstCarPosition, firstCarPrevious));
        scene.world().modifyEntity(secondCar,
                entity -> moveVehicle(entity, secondCarPosition, secondCarPrevious));
    }

    private static void moveVehicle(Entity entity, RoutePoint position, RoutePoint previous) {
        float yaw = yaw(previous, position);
        entity.moveTo(previous.x(), VEHICLE_Y, previous.z(), yaw, 0.0F);
        entity.setOldPosAndRot();
        entity.moveTo(position.x(), VEHICLE_Y, position.z(), yaw, 0.0F);
    }

    private static void initializeVehicle(Entity entity, RoutePoint position, float yaw) {
        entity.moveTo(position.x(), VEHICLE_Y, position.z(), yaw, 0.0F);
        entity.setOldPosAndRot();
    }

    private static float yaw(RoutePoint previous, RoutePoint position) {
        double x = position.x() - previous.x();
        double z = position.z() - previous.z();
        return (float) (Math.toDegrees(Math.atan2(-x, z)));
    }

    private static void showPreview(SceneBuilder scene, SceneBuildingUtil util, int firstPoint, int lastPoint,
                                    int duration) {
        for (int point = firstPoint; point < lastPoint; point++) {
            RoutePoint from = LOOP_PREVIEW[point];
            RoutePoint to = LOOP_PREVIEW[point + 1];
            scene.overlay().showLine(PonderPalette.INPUT,
                    util.vector().of(from.x(), PREVIEW_Y, from.z()),
                    util.vector().of(to.x(), PREVIEW_Y, to.z()), duration);
        }
    }

    private static List<RoutePoint> samplePath(RoutePoint[] controlPoints) {
        List<RoutePoint> result = new ArrayList<>();
        result.add(controlPoints[0]);
        for (int segment = 0; segment < controlPoints.length - 1; segment++) {
            RoutePoint from = controlPoints[segment];
            RoutePoint to = controlPoints[segment + 1];
            double distance = Math.hypot(to.x() - from.x(), to.z() - from.z());
            int steps = (int) Math.ceil(distance / MOVEMENT_SAMPLE_DISTANCE);
            for (int step = 1; step <= steps; step++) {
                double progress = (double) step / steps;
                result.add(new RoutePoint(
                        from.x() + (to.x() - from.x()) * progress,
                        from.z() + (to.z() - from.z()) * progress));
            }
        }
        return result;
    }

    private record RoutePoint(double x, double z) {
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
