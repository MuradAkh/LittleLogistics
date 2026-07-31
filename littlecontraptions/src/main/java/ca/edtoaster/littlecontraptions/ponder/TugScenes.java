package ca.edtoaster.littlecontraptions.ponder;

import ca.edtoaster.littlecontraptions.ponder.element.VehicleElement;
import ca.edtoaster.littlecontraptions.ponder.element.VehicleInstructions;
import dev.murad.shipping.setup.ModItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TugScenes {
    private static ItemStack stackOf(Item item) {
        return new ItemStack(item, 1);
    }

    public static void basicTugScene(SceneBuilder scene, SceneBuildingUtil util) {
        VehicleInstructions bargeInst = new VehicleInstructions(scene);

        // Setup Scene
        scene.title("basic_tug", "Just the basics about tugs");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75F);
        scene.world().showSection(util.select().fromTo(0, 0, 0, 0, 0, 6)
                .add(util.select().fromTo(2, 0, 0, 6, 0, 4))
                .add(util.select().fromTo(0, 0, 6, 6, 0, 6)), Direction.UP);
        scene.idle(5);

        List<BlockPos> waterPos = List.of(
                of(1, 0, 0),
                of(1, 0, 1),
                of(1, 0, 2),
                of(1, 0, 3),
                of(1, 0, 4),
                of(1, 0, 5),
                of(2, 0, 5),
                of(3, 0, 5),
                of(4, 0, 5),
                of(5, 0, 5),
                of(6, 0, 5));

        for (BlockPos pos : waterPos) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(2);
        }

        // Spawn tug
        BlockPos steamTugInitialPosition = new BlockPos(4, 0, 5);
        Vec3 steamTugInteractPosition = util.vector().topOf(steamTugInitialPosition).subtract(0.5, 0, 0);

        scene.overlay().showText(100)
                .pointAt(util.vector().topOf(steamTugInitialPosition))
                .placeNearTarget()
                .text("Tugboats can be placed anywhere on water");
        scene.idle(50);
        scene.overlay().showControls(util.vector().topOf(steamTugInitialPosition), Pointing.DOWN, 50)
                .rightClick()
                .withItem(stackOf(ModItems.STEAM_TUG.get()));
        scene.idle(70);

        VehicleElement steamTug =
                bargeInst.createVehicle(util.vector().topOf(steamTugInitialPosition), 270.0F, VehicleElement.STEAM_TUG);

        scene.idle(20);
        scene.addKeyframe();

        // Tug route scene
        BlockPos waypoint = new BlockPos(1, 0, 0);
        scene.overlay().showText(60)
                .pointAt(util.vector().topOf(waypoint))
                .placeNearTarget()
                .text("Tugboats will naturally pathfind to Tug Route waypoints");

        scene.idle(70);

        ItemStack emptyTugRoute = stackOf(ModItems.TUG_ROUTE.get());
        ItemStack fullTugRoute = stackOf(ModItems.TUG_ROUTE.get());

        // Simulate right click :)
        scene.overlay().showControls(util.vector().topOf(waypoint), Pointing.DOWN, 30)
                .rightClick()
                .withItem(emptyTugRoute);
        scene.idle(37);
        scene.overlay().showControls(util.vector().topOf(waypoint), Pointing.DOWN, 50)
                .rightClick()
                .withItem(fullTugRoute);
        scene.idle(50);

        // Render tugroute item on the water
        ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(
                util.vector().topOf(waypoint),
                util.vector().of(0.0D, 0.0D, 0.0D),
                fullTugRoute);
        scene.world().modifyEntity(itemEntity, e -> e.setNoGravity(true));
        scene.idle(20);

        scene.addKeyframe();

        // Put tugroute in tugboat
        scene.overlay().showText(70)
                .pointAt(steamTugInteractPosition)
                .placeNearTarget()
                .text("Putting the Tug Route item in the Tug will start its journey!");
        scene.idle(35);
        scene.overlay().showControls(steamTugInteractPosition.add(0, 1, 0), Pointing.DOWN, 35)
                .withItem(fullTugRoute);
        scene.idle(45);

        // Start moving tugboat
        bargeInst.moveVehicle(steamTug, new Vec3(-2, 0, 0), 25);
        scene.idle(15);
        bargeInst.rotateVehicle(steamTug, -35, 10);
        scene.idle(25);

        scene.addKeyframe();

        // PORT-BLOCKED: the corner guide rail (dev.murad.shipping.block.guiderail.CornerGuideRailBlock /
        // ModBlocks.GUIDE_RAIL_CORNER) was removed from Little Logistics, so the original
        // "stuck on a corner -> place/flip a guide rail" demonstration no longer has a block to show.
        // The narrative is reduced to tugs pathfinding around corners on their own.
        Vec3 turnPosition = new Vec3(2, 1, 5);
        scene.overlay().showText(80)
                .pointAt(turnPosition)
                .placeNearTarget()
                .text("Tugboats will pathfind around corners on their way to the next waypoint");
        scene.idle(100);

        scene.addKeyframe();

        // Continue!
        bargeInst.moveVehicle(steamTug, new Vec3(-1, 0, -1), 10);
        bargeInst.rotateVehicle(steamTug, -55, 10);
        scene.idle(10);

        bargeInst.moveVehicle(steamTug, new Vec3(0, 0, -3), 36);
        scene.idle(36);
        scene.world().modifyEntity(itemEntity, Entity::discard);

    }

    public static void dockingScene(SceneBuilder scene, SceneBuildingUtil util) {
        VehicleInstructions bargeInst = new VehicleInstructions(scene);

        // Setup Scene
        scene.title("tug_dock", "Docking the tug");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.world().showSection(util.select().fromTo(0, 0, 4, 6, 0, 6), Direction.UP);
        scene.idle(5);

        scene.overlay().showText(80)
                .pointAt(util.vector().topOf(of(2, 0, 4)))
                .placeNearTarget()
                .text("Docks can be used to stop tugboats automatically while loading/unloading");

        scene.idle(100);

        scene.overlay().showText(80)
                .pointAt(util.vector().topOf(of(3, 0, 4)))
                .placeNearTarget()
                .text("Every docking station needs one docking station block per vehicle and serviced barge.");

        scene.idle(120);

        scene.overlay().showText(100)
                .pointAt(util.vector().topOf(of(4, 0, 4)))
                .placeNearTarget()
                .text("Dye each dock to match the vessel it should service, using the conductor's wrench to configure it.");

        scene.idle(130);

        scene.addKeyframe();

        scene.overlay().showControls(util.vector().topOf(of(4, 0, 4)), Pointing.DOWN, 30)
                .rightClick()
                .withItem(stackOf(ModItems.CONDUCTORS_WRENCH.get()));

        // PORT-BLOCKED: dev.murad.shipping.block.dock.DockingBlockStates and ModBlocks.BARGE_DOCK were
        // removed; the blue/orange "INVERTED" toggle is now the 0-15 COLOR filter on the unified
        // ModBlocks.DOCKING_STATION. The original replaceBlocks(orangeDock) recolor cannot be ported
        // mechanically (it depends on the reworked schematic/state layout) and is dropped here.

        // add hoppers
        scene.idle(30);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 1, 6), Direction.UP);
        ElementLink<WorldSectionElement> unloadHopper = scene.world().makeSectionIndependent(util.select().position(of(4, 0, 7)));
        scene.world().moveSection(unloadHopper, util.vector().of(-1, -1, -4), 10);

        scene.idle(30);

        scene.overlay().showText(50)
                .pointAt(util.vector().topOf(of(4, 1, 4)))
                .placeNearTarget()
                .text("To load the vessels, place a hopper on top of the dock");

        scene.idle(70);

        scene.overlay().showText(50)
                .pointAt(util.vector().blockSurface(of(3, -1, 4), Direction.DOWN))
                .placeNearTarget()
                .text("To unload the vessels, place a hopper below the water block.");

        scene.idle(100);

        scene.addKeyframe();

        // add the rest of the surface

        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 0, 2), Direction.UP);

        // add water

        List<BlockPos> waterPos = List.of(
                of(0, 0, 3),
                of(1, 0, 3),
                of(2, 0, 3),
                of(3, 0, 3),
                of(4, 0, 3),
                of(5, 0, 3),
                of(6, 0, 3));

        for (BlockPos pos : waterPos) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(2);
        }

        // Spawn vessels
        VehicleElement tug =
                bargeInst.createVehicle(util.vector().of(4.5, 1, 3.5), 270.0F, VehicleElement.ENERGY_TUG);
        VehicleElement chest1 =
                bargeInst.createVehicle(util.vector().of(5.5, 1, 3.5), 270.0F, VehicleElement.FISHING_BARGE);
        VehicleElement chest2 =
                bargeInst.createVehicle(util.vector().of(6.5, 1, 3.5), 270.0F, VehicleElement.CHEST_BARGE);

        bargeInst.moveVehicle(tug, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest1, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest2, util.vector().of(-2, 0, 0), 50);

        scene.idle(80);

        scene.overlay().showText(70)
                .pointAt(util.vector().topOf(of(2, 0, 3)))
                .placeNearTarget()
                .text("The tug will automatically wait until all vessels are fully loaded/unloaded.");

        scene.idle(100);

        bargeInst.moveVehicle(tug, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest1, util.vector().of(-2, 0, 0), 50);
        bargeInst.moveVehicle(chest2, util.vector().of(-2, 0, 0), 50);

        scene.idle(5);


    }

    private static BlockPos of(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
