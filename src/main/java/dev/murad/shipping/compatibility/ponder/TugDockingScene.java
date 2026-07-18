package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.setup.ModBlocks;
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
import net.minecraft.world.phys.Vec3;

final class TugDockingScene {

    private static final double CHANNEL_Z = 3.5;
    private static final float EAST_YAW = -90.0F;

    private TugDockingScene() {
    }

    static void docking(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("tug_docking", "Docking a Tug Convoy");
        scene.configureBasePlate(0, -3, 12);
        scene.scaleSceneView(0.8F);
        scene.showBasePlate();
        showCanal(scene, util);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(7, 2, 2, 7, 3, 3), Direction.DOWN);
        scene.overlay().showControls(util.vector().topOf(7, 2, 2), Pointing.DOWN, 35)
                .withItem(new ItemStack(ModBlocks.DOCKING_STATION.get()));
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Place a Docking Station on the bank beside a straight canal.")
                .pointAt(util.vector().centerOf(7, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(6, 2, 2, 6, 3, 3), Direction.DOWN);
        scene.overlay().showOutline(PonderPalette.INPUT, "tug-dock-line",
                util.select().fromTo(6, 2, 2, 7, 3, 3), 70);
        scene.overlay().showText(70)
                .text("Adjacent docks form one station, with a position for the tug and each serviced barge.")
                .pointAt(util.vector().centerOf(6, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(6, 3, 2), Pointing.DOWN, 45)
                .withItem(new ItemStack(Items.BLUE_DYE))
                .rightClick();
        scene.overlay().showText(70)
                .text("The blue dock matches the blue barge, while the red dock matches the tug.")
                .pointAt(util.vector().centerOf(6, 3, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(6, 2, 1, 6, 3, 1), Direction.SOUTH);
        scene.overlay().showText(70)
                .text("Connect automation to the controller face pointing away from the canal.")
                .pointAt(util.vector().centerOf(6, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        ElementLink<EntityElement> tug = scene.world().createEntity(level -> {
            var entity = PonderVesselSceneUtil.steamTug(level);
            PonderVesselSceneUtil.initialize(entity, 2.5, CHANNEL_Z, EAST_YAW, DyeColor.RED);
            return entity;
        });
        ElementLink<EntityElement> barge = scene.world().createEntity(level -> {
            var entity = PonderVesselSceneUtil.chestBarge(level);
            PonderVesselSceneUtil.initialize(entity, 1.3, CHANNEL_Z, EAST_YAW, DyeColor.BLUE);
            return entity;
        });
        scene.idle(15);

        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("A matching convoy travelling through the canal docks automatically.")
                .pointAt(util.vector().centerOf(5, 1, 3))
                .placeNearTarget();
        moveConvoy(scene, tug, barge, 2.5, 7.5, 1.2, 1.2);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("The tug claims the front dock while its linked barge settles into the dock behind it.")
                .pointAt(util.vector().centerOf(6, 1, 3))
                .placeNearTarget();
        for (int step = 1; step <= 8; step++) {
            double previous = 6.3 + 0.2 * (step - 1) / 8.0;
            double current = 6.3 + 0.2 * step / 8.0;
            PonderVesselSceneUtil.move(scene, barge, previous, CHANNEL_Z, current, CHANNEL_Z, EAST_YAW);
            scene.idle(3);
        }
        scene.idle(45);

        scene.overlay().showControls(util.vector().of(5.8, 4.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.IRON_INGOT));
        scene.overlay().showControls(util.vector().of(6.5, 4.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showControls(util.vector().of(7.2, 4.2, 1.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.REDSTONE));
        scene.overlay().showText(70)
                .text("An occupied dock exposes the vessel's item, fluid, or energy storage.")
                .pointAt(util.vector().centerOf(6, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("The attached automation controls whether cargo is loaded or unloaded.")
                .pointAt(util.vector().centerOf(6, 2, 2))
                .placeNearTarget();
        animateItemTransfer(scene);
        scene.idle(30);

        scene.overlay().showControls(util.vector().topOf(7, 2, 2), Pointing.DOWN, 45)
                .withItem(new ItemStack(Items.CLOCK));
        scene.overlay().showText(80)
                .text("Transfers reset the idle timer, and any active dock holds the whole convoy.")
                .pointAt(util.vector().centerOf(7, 2, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(7, 2, 2), Pointing.DOWN, 45)
                .rightClick();
        scene.overlay().showText(80)
                .text("Right-click a controller to configure its timeout, name, statistics, and redstone behavior.")
                .pointAt(util.vector().centerOf(7, 2, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(60)
                .colored(PonderPalette.OUTPUT)
                .text("When every dock becomes idle, the convoy continues its route.")
                .pointAt(util.vector().centerOf(7, 1, 3))
                .placeNearTarget();
        moveConvoy(scene, tug, barge, 7.5, 11.5, 1.0, 1.2);
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void showCanal(SceneBuilder scene, SceneBuildingUtil util) {
        scene.world().showSection(util.select().fromTo(0, 1, 3, 11, 1, 5), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 11, 1, 2), Direction.DOWN);
    }

    private static void moveConvoy(SceneBuilder scene, ElementLink<EntityElement> tug,
                                   ElementLink<EntityElement> barge, double fromX, double toX,
                                   double startingGap, double endingGap) {
        int steps = (int) Math.round((toX - fromX) / 0.25);
        for (int step = 1; step <= steps; step++) {
            double progress = (double) step / steps;
            double previousProgress = (double) (step - 1) / steps;
            double tugX = fromX + step * 0.25;
            double previousTugX = tugX - 0.25;
            double gap = startingGap + (endingGap - startingGap) * progress;
            double previousGap = startingGap + (endingGap - startingGap) * previousProgress;
            PonderVesselSceneUtil.move(scene, tug,
                    previousTugX, CHANNEL_Z, tugX, CHANNEL_Z, EAST_YAW);
            PonderVesselSceneUtil.move(scene, barge,
                    previousTugX - previousGap, CHANNEL_Z, tugX - gap, CHANNEL_Z, EAST_YAW);
            scene.idle(1);
        }
    }

    private static void animateItemTransfer(SceneBuilder scene) {
        Vec3[] path = {
                new Vec3(6.5, 3.8, 1.5),
                new Vec3(6.5, 3.1, 1.5),
                new Vec3(6.5, 2.7, 2.0),
                new Vec3(6.5, 2.4, 2.5),
                new Vec3(6.5, 2.0, 3.0),
                new Vec3(6.5, 1.9, 3.5)
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
}
