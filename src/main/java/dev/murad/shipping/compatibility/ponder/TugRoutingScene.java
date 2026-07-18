package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.setup.ModItems;
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

import java.util.ArrayList;
import java.util.List;

final class TugRoutingScene {

    private static final double PREVIEW_Y = 2.08;
    private static final double SAMPLE_DISTANCE = 0.2;
    private static final int CONVOY_GAP_SAMPLES = 6;
    private static final int TUG_START_SAMPLE = 12;

    private static final RoutePoint[] LOOP_PREVIEW = {
            new RoutePoint(3.0, 7.5),
            new RoutePoint(10.0, 7.5),
            new RoutePoint(10.5, 7.0),
            new RoutePoint(10.5, 2.0),
            new RoutePoint(10.0, 1.5),
            new RoutePoint(3.0, 1.5),
            new RoutePoint(2.5, 2.0),
            new RoutePoint(2.5, 7.0),
            new RoutePoint(3.0, 7.5)
    };

    private static final List<RoutePoint> MOVEMENT_PATH = samplePath(new RoutePoint[] {
            new RoutePoint(5.5, 7.5),
            new RoutePoint(10.0, 7.5),
            new RoutePoint(10.5, 7.0),
            new RoutePoint(10.5, 2.0),
            new RoutePoint(10.0, 1.5),
            new RoutePoint(3.0, 1.5),
            new RoutePoint(2.5, 2.0),
            new RoutePoint(2.5, 7.0),
            new RoutePoint(3.0, 7.5),
            new RoutePoint(10.0, 7.5)
    });

    private TugRoutingScene() {
    }

    static void routing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("tug_routing", "Routing a Tug");
        scene.configureBasePlate(0, -2, 13);
        scene.scaleSceneView(0.85F);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 0, 12, 1, 8), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showControls(util.vector().topOf(3, 1, 7), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Right-click navigable water with a Tug Route to add waypoints in travel order.")
                .pointAt(util.vector().centerOf(3, 1, 7))
                .placeNearTarget();
        showPreview(scene, util, 0, 2, 70);
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(10, 1, 7), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showControls(util.vector().topOf(10, 1, 1), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showControls(util.vector().topOf(3, 1, 1), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .text("The route calculates a navigable water path between each waypoint.")
                .pointAt(util.vector().centerOf(10, 1, 4))
                .placeNearTarget();
        showPreview(scene, util, 2, 6, 70);
        scene.idle(80);

        scene.overlay().showOutline(PonderPalette.OUTPUT, "tug-route-island",
                util.select().fromTo(4, 1, 3, 8, 1, 5), 70);
        scene.overlay().showText(70)
                .text("Place waypoints around obstacles to guide the path through open water.")
                .pointAt(util.vector().centerOf(6, 1, 4))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(3, 1, 7), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Click the first waypoint again to complete the repeating loop.")
                .pointAt(util.vector().centerOf(3, 1, 7))
                .placeNearTarget();
        showPreview(scene, util, 6, 8, 70);
        scene.idle(80);

        ElementLink<EntityElement> tug = scene.world().createEntity(level -> {
            var entity = PonderVesselSceneUtil.steamTug(level);
            initializeAtPath(entity, MOVEMENT_PATH.get(TUG_START_SAMPLE), -90.0F, DyeColor.RED);
            return entity;
        });
        ElementLink<EntityElement> barge = scene.world().createEntity(level -> {
            var entity = PonderVesselSceneUtil.chestBarge(level);
            initializeAtPath(entity, MOVEMENT_PATH.get(TUG_START_SAMPLE - CONVOY_GAP_SAMPLES),
                    -90.0F, DyeColor.RED);
            return entity;
        });
        scene.idle(20);

        scene.overlay().showControls(util.vector().of(
                        MOVEMENT_PATH.get(TUG_START_SAMPLE).x(), 2.2,
                        MOVEMENT_PATH.get(TUG_START_SAMPLE).z()), Pointing.DOWN, 40)
                .withItem(new ItemStack(ModItems.TUG_ROUTE.get()))
                .rightClick();
        scene.overlay().showText(70)
                .text("Place the completed route in the tug's route slot.")
                .pointAt(util.vector().of(MOVEMENT_PATH.get(TUG_START_SAMPLE).x(), 1.8,
                        MOVEMENT_PATH.get(TUG_START_SAMPLE).z()))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .colored(PonderPalette.OUTPUT)
                .text("Start the engine to send the entire convoy around the route.")
                .pointAt(util.vector().centerOf(6, 1, 4))
                .placeNearTarget();
        scene.idle(20);

        for (int point = TUG_START_SAMPLE + 1; point < MOVEMENT_PATH.size(); point++) {
            moveVessel(scene, tug, MOVEMENT_PATH.get(point - 1), MOVEMENT_PATH.get(point));
            moveVessel(scene, barge,
                    MOVEMENT_PATH.get(point - CONVOY_GAP_SAMPLES - 1),
                    MOVEMENT_PATH.get(point - CONVOY_GAP_SAMPLES));
            scene.idle(1);
        }
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void initializeAtPath(Entity entity, RoutePoint position, float yaw, DyeColor color) {
        PonderVesselSceneUtil.initialize(entity, position.x(), position.z(), yaw, color);
    }

    private static void moveVessel(SceneBuilder scene, ElementLink<EntityElement> link,
                                   RoutePoint previous, RoutePoint position) {
        float yaw = yaw(previous, position);
        PonderVesselSceneUtil.move(scene, link, previous.x(), previous.z(), position.x(), position.z(), yaw);
    }

    private static float yaw(RoutePoint previous, RoutePoint position) {
        double x = position.x() - previous.x();
        double z = position.z() - previous.z();
        return (float) Math.toDegrees(Math.atan2(-x, z));
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
            int steps = (int) Math.ceil(distance / SAMPLE_DISTANCE);
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
}
