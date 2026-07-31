package ca.edtoaster.littlecontraptions.ponder;

import ca.edtoaster.littlecontraptions.ponder.element.VehicleElement;
import ca.edtoaster.littlecontraptions.ponder.element.VehicleInstructions;
import dev.murad.shipping.block.rail.SwitchRail;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LocomotiveScenes {
    private static ItemStack stackOf(Item item) {
        return new ItemStack(item, 1);
    }
    private static BlockPos of(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }


    public static void dockingScene(SceneBuilder scene, SceneBuildingUtil util) {
        VehicleInstructions bargeInst = new VehicleInstructions(scene);

        // Setup Scene
        scene.title("loco_dock", "Docking the locomotive");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.world().showSection(util.select().fromTo(0, 0, 3, 6, 0, 6), Direction.UP);
        scene.idle(5);

        // add rails

        List<BlockPos> railPos = List.of(
                of(0, 1, 3),
                of(1, 1, 3),
                of(2, 1, 3),
                of(3, 1, 3),
                of(4, 1, 3),
                of(5, 1, 3),
                of(6, 1, 3));

        for (BlockPos pos : railPos) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(2);
        }

        scene.overlay().showText(80)
                .pointAt(util.vector().topOf(of(2, 0, 3)))
                .placeNearTarget()
                .text("Docks can be used to stop the locomotives automatically while loading/unloading");

        scene.idle(100);

        scene.overlay().showText(80)
                .pointAt(util.vector().topOf(of(3, 0, 3)))
                .placeNearTarget()
                .text("Every docking station needs one docking station block per locomotive and serviced car.");

        scene.idle(120);

        scene.overlay().showText(100)
                .pointAt(util.vector().topOf(of(4, 0, 3)))
                .placeNearTarget()
                .text("Dye each dock to match the car it should service, using the conductor's wrench to configure it.");

        scene.idle(130);

        scene.addKeyframe();

        scene.overlay().showControls(util.vector().topOf(of(4, 0, 3)), Pointing.DOWN, 30)
                .rightClick()
                .withItem(stackOf(ModItems.CONDUCTORS_WRENCH.get()));

        // PORT-BLOCKED: ModBlocks.CAR_DOCK_RAIL / dev.murad.shipping.block.rail.AbstractDockingRail and
        // ModBlocks.RAPID_HOPPER were removed from Little Logistics; the blue/orange
        // DockingBlockStates.INVERTED toggle is now the COLOR filter on the unified
        // ModBlocks.DOCKING_STATION. The original replaceBlocks(orangeDock)/replaceBlocks(RAPID_HOPPER)
        // calls cannot be ported mechanically and are dropped here.

        // add hoppers
        scene.idle(30);
        scene.world().showSection(util.select().fromTo(0, 1, 4, 6, 1, 6), Direction.UP);

        scene.idle(30);

        scene.overlay().showText(50)
                .pointAt(util.vector().topOf(of(4, 1, 4)))
                .placeNearTarget()
                .text("To load the trains, place a hopper beside the dock");

        scene.idle(70);

        scene.overlay().showText(50)
                .pointAt(util.vector().blockSurface(of(3, 0, 3), Direction.DOWN))
                .placeNearTarget()
                .text("To unload the trains, place a hopper below the rail block.");

        scene.idle(100);

        // add the rest of the surface

        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 0, 2), Direction.UP);

        // Spawn train

        VehicleElement tug =
                bargeInst.createVehicle(util.vector().of(4.5, 1.5, 3.5), 270.0F, VehicleElement.ENERGY_LOCOMOTIVE);
        VehicleElement chest1 =
                bargeInst.createVehicle(util.vector().of(5.5, 1.5, 3.5), 270.0F, VehicleElement.CHEST_CAR);
        VehicleElement chest2 =
                bargeInst.createVehicle(util.vector().of(6.5, 1.5, 3.5), 270.0F, VehicleElement.CHEST_CAR);

        bargeInst.moveVehicle(tug, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest1, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest2, util.vector().of(-2, 0, 0), 50);

        scene.idle(80);

        scene.overlay().showText(70)
                .pointAt(util.vector().topOf(of(2, 0, 3)))
                .placeNearTarget()
                .text("The locomotive will automatically wait until the whole train is loaded/unloaded.");

        scene.idle(100);

        bargeInst.moveVehicle(tug, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest1, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest2, util.vector().of(-2, 0, 0), 50);

        scene.idle(5);


    }

    public static void routeScene(SceneBuilder scene, SceneBuildingUtil util) {
        VehicleInstructions bargeInst = new VehicleInstructions(scene);

        // Setup Scene
        scene.title("loco_route", "Locomotive routing");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 0, 6), Direction.UP);
        scene.idle(5);

        // add rails

        List<BlockPos> railPos = List.of(
                of(0, 1, 5),
                of(1, 1, 5),
                of(2, 1, 5),
                of(3, 1, 5),
                of(4, 1, 5),
                of(5, 1, 5),
                of(6, 1, 5),
                of(3, 1, 2),
                of(3, 1, 3),
                of(3, 1, 4),
                of(0, 1, 1),
                of(1, 1, 1),
                of(2, 1, 1),
                of(3, 1, 1),
                of(4, 1, 1),
                of(5, 1, 1),
                of(6, 1, 1));

        for (BlockPos pos : railPos) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(2);
        }

        scene.overlay().showText(90)
                .pointAt(util.vector().topOf(of(3, 0, 5)))
                .placeNearTarget()
                .text("Automatic switch tracks let you create train routes that share stretches of rail");


        scene.idle(100);

        scene.overlay().showControls(util.vector().topOf(of(1, 0, 1)), Pointing.DOWN, 30)
                .rightClick()
                .withItem(stackOf(ModItems.LOCO_ROUTE.get()));

        scene.overlay().showText(100)
                .pointAt(util.vector().topOf(of(1, 0, 1)))
                .placeNearTarget()
                .text("Routes are defined by waypoints by right-clicking with the locomotive route item");

        ItemStack route = stackOf(ModItems.LOCO_ROUTE.get());
        // Store a waypoint in the route item so it renders as a configured (green) route.
        LocoRoute routeSet = new LocoRoute();
        routeSet.add(new LocoRouteNode("", 0, 0, 0));
        LocoRouteItem.saveRoute(routeSet, route);

        ElementLink<EntityElement> firstWaypoint = scene.world().createItemEntity(
                util.vector().topOf(of(1, 0, 1)),
                util.vector().of(0.0D, 0.0D, 0.0D),
                route);

        scene.idle(110);

        // first train

        scene.addKeyframe();

        VehicleElement loco1 =
                bargeInst.createVehicle(util.vector().of(6.5, 1.5, 5.5), 270.0F, VehicleElement.STEAM_LOCOMOTIVE);

        bargeInst.moveVehicle(loco1, util.vector().of(-1.5, 0, 0), 25);

        scene.overlay().showText(120)
                .pointAt(util.vector().topOf(of(3, 0, 5)))
                .placeNearTarget()
                .text("The locomotive will switch the rails automatically, selecting the branch that leads to the waypoint");

        scene.idle(130);
        scene.world().modifyBlock(of(3, 1, 5), LocomotiveScenes::flipAutoRail, false);


        bargeInst.moveVehicle(loco1, util.vector().of(-1.5, 0, 0), 25);


        scene.idle(25);
        bargeInst.rotateVehicle(loco1, -90, 10);
        scene.idle(10);
        bargeInst.moveVehicle(loco1, util.vector().of(0, 0, -4), 50);

        scene.idle(25);
        scene.world().modifyBlock(of(3, 1, 1), LocomotiveScenes::flipAutoRail, false);

        scene.idle(25);
        bargeInst.rotateVehicle(loco1, 90, 10);
        scene.idle(10);
        bargeInst.moveVehicle(loco1, util.vector().of(-3, 0, 0), 50);
        scene.idle(40);
        scene.world().modifyEntity(firstWaypoint, Entity::discard);
        scene.idle(10);
        bargeInst.removeVehicle(loco1);

        scene.idle(10);

        // second train
        scene.addKeyframe();

        VehicleElement loco2 =
                bargeInst.createVehicle(util.vector().of(6.5, 1.5, 5.5), 270.0F, VehicleElement.ENERGY_LOCOMOTIVE);

        ElementLink<EntityElement> secondWaypoint = scene.world().createItemEntity(
                util.vector().topOf(of(5, 0, 1)),
                util.vector().of(0.0D, 0.0D, 0.0D),
                route);

        ElementLink<EntityElement> thirdWaypoint = scene.world().createItemEntity(
                util.vector().topOf(of(1, 0, 5)),
                util.vector().of(0.0D, 0.0D, 0.0D),
                route);


        bargeInst.moveVehicle(loco2, util.vector().of(-1.5, 0, 0), 25);


        scene.overlay().showText(120)
                .pointAt(util.vector().topOf(of(3, 0, 5)))
                .placeNearTarget()
                .text("Given two waypoints, the locomotive will select a branch with the closest waypoint that has not been visited yet");

        scene.idle(130);
        scene.world().modifyBlock(of(3, 1, 5), LocomotiveScenes::flipAutoRail, false);
        bargeInst.moveVehicle(loco2, util.vector().of(-4.5, 0, 0), 75);

        scene.idle(60);

        scene.overlay().showText(120)
                .pointAt(util.vector().topOf(of(1, 0, 5)))
                .placeNearTarget()
                .text("When locomotive reaches a waypoint, it is marked as visited, this resets once all waypoints the route are reached");

        scene.world().modifyEntity(thirdWaypoint, Entity::discard);

        scene.idle(5);
    }

    private static BlockState flipAutoRail(BlockState autorail) {
        return autorail.setValue(SwitchRail.POWERED, !autorail.getValue(SwitchRail.POWERED));
    }
}
