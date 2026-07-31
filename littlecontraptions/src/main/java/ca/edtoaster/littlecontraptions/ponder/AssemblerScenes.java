package ca.edtoaster.littlecontraptions.ponder;

import ca.edtoaster.littlecontraptions.ponder.element.VehicleElement;
import ca.edtoaster.littlecontraptions.ponder.element.VehicleInstructions;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class AssemblerScenes {
    public static void basicAssemblerScene(SceneBuilder scene, SceneBuildingUtil util) {
        VehicleInstructions bargeInst = new VehicleInstructions(scene);
        // Create-specific instructions (super glue + kinetic speed) live on the Create wrapper.
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);

        scene.title("basic_assembler", "Moving Structures using Barge Assemblers");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9F);
        scene.world().showSection(util.select().fromTo(0, 0, 0, 1, 0, 4)
                .add(util.select().fromTo(3, 0, 0, 4, 0, 4)), Direction.UP);
        scene.idle(5);

        for(int z = 0; z < 5; ++z) {
            scene.world().showSection(util.select().position(2, 0, z), Direction.DOWN);
            scene.idle(2);
        }

        BlockPos assemblerPos = util.grid().at(2, 1, 2);
        BlockPos leverPos = util.grid().at(0, 1, 2);
        Selection assemblySection = util.select().fromTo(assemblerPos, leverPos);
        scene.idle(17);
        scene.world().showSection(assemblySection, Direction.EAST);
        scene.idle(20);
        scene.world().toggleRedstonePower(assemblySection);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(10);
        scene.overlay().showText(70).text("Powered Barge Assemblers mount attached structures to passing Contraption Barges")
                .attachKeyFrame().pointAt(util.vector().topOf(assemblerPos)).placeNearTarget();
        scene.idle(80);

        // Spawn barge
        VehicleElement barge =
                bargeInst.createVehicle(util.vector().topOf(2, 0, 4), 90.0F, VehicleElement.CONTRAPTION_BARGE);
        scene.world().showSection(util.select().position(assemblerPos.above()), Direction.DOWN);
        scene.idle(10);

        // Move barge to assembler
        bargeInst.moveVehicle(barge, util.vector().of(0.0D, 0.0D, -2.0D), 20);
        scene.idle(20);
        ElementLink<WorldSectionElement> plank = scene.world().makeSectionIndependent(util.select().position(assemblerPos.above()));
        scene.effects().indicateSuccess(assemblerPos);
        scene.idle(1);

        // Move barge and block to end
        scene.world().moveSection(plank, util.vector().of(0.0D, 0.0D, -2.0D), 20);
        bargeInst.moveVehicle(barge, util.vector().of(0.0D, 0.0D, -2.0D), 20);
        scene.idle(20);
        scene.world().toggleRedstonePower(assemblySection);
        scene.idle(10);
        scene.overlay().showText(70).text("Without a redstone signal, it disassembles passing barge contraptions back into blocks").colored(PonderPalette.RED).attachKeyFrame().pointAt(util.vector().topOf(assemblerPos)).placeNearTarget();
        scene.idle(80);

        // Rotate barge and block
        scene.world().rotateSection(plank, 0.0D, 180.0D, 0.0D, 6);
        bargeInst.rotateVehicle(barge, 180.0f, 6);
        scene.idle(3);

        // Move barge and block back to assembler
        scene.world().moveSection(plank, util.vector().of(0.0D, 0.0D, 2.0D), 20);
        bargeInst.moveVehicle(barge, util.vector().of(0.0D, 0.0D, 2.0D), 20);
        scene.idle(20);

        // Move barge to beginning
        bargeInst.moveVehicle(barge, util.vector().of(0.0D, 0.0D, 2.0D), 20);
        scene.idle(30);
        scene.world().destroyBlock(assemblerPos.above());
        bargeInst.rotateVehicle(barge, 180.0f, 6);
        scene.idle(10);

        scene.addKeyframe();

        // Show contraption, toggle redstone
        ElementLink<WorldSectionElement> contraption =
                scene.world().showIndependentSection(util.select().fromTo(2, 3, 2, 4, 3, 2), Direction.DOWN);
        scene.world().moveSection(contraption, new Vec3(0, -1, 0), 0);
        scene.world().toggleRedstonePower(assemblySection);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(10);

        // Super glue contraption
        createScene.effects().superGlue(util.grid().at(4, 2, 1), Direction.SOUTH, true);
        scene.overlay().showText(70).text("Similar to Cart Contraptions, you can attach larger contraptions using Chassis and Super Glue")
                .pointAt(util.vector().topOf(assemblerPos)).placeNearTarget();
        scene.idle(80);
        // Show drill
        Selection drillPos = util.select().position(4, 3, 1);
        scene.world().showSectionAndMerge(drillPos, Direction.SOUTH, contraption);
        scene.world().configureCenterOfRotation(contraption, new Vec3(2.5, 0, 2.5));
        scene.idle(10);

        // Move barge to assembler
        bargeInst.moveVehicle(barge, new Vec3(0, 0, -2), 20);
        scene.idle(20);

        // Move everything to end
        scene.effects().indicateSuccess(assemblerPos);
        bargeInst.moveVehicle(barge, new Vec3(0, 0, -2), 20);
        scene.world().moveSection(contraption, new Vec3(0, 0, -2), 20);
        createScene.world().setKineticSpeed(drillPos, 20);
        scene.idle(40);

        // Rotate
        bargeInst.rotateVehicle(barge, 180.0f, 16);
        scene.world().rotateSection(contraption, 0.0D, 180.0D, 0.0D, 16);
        scene.idle(16);

        // Move everything to start
        bargeInst.moveVehicle(barge, new Vec3(0, 0, 4), 40);
        scene.world().moveSection(contraption, new Vec3(0, 0, 4), 40);
        scene.idle(40);

        // Disassemble
        createScene.world().setKineticSpeed(drillPos, 0);
        scene.world().hideIndependentSection(contraption, Direction.UP);
        scene.idle(10);
    }
}
