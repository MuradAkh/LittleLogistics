package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.setup.ModItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

final class TugLinkingScene {

    private TugLinkingScene() {
    }

    static void linking(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("tug_linking", "Linking Barges");
        scene.configureBasePlate(-1, 0, 8);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 1, 7), Direction.DOWN);
        scene.idle(10);

        ElementLink<EntityElement> rearBarge = scene.world().createEntity(level -> {
            var barge = PonderVesselSceneUtil.chestBarge(level);
            PonderVesselSceneUtil.initialize(barge, 2.5, 1.5, 0.0F, DyeColor.RED);
            return barge;
        });
        ElementLink<EntityElement> frontBarge = scene.world().createEntity(level -> {
            var barge = PonderVesselSceneUtil.chestBarge(level);
            PonderVesselSceneUtil.initialize(barge, 2.5, 5.5, 0.0F, DyeColor.RED);
            return barge;
        });
        scene.idle(15);

        scene.overlay().showControls(util.vector().of(2.5, 2.2, 3.5), Pointing.DOWN, 50)
                .withItem(new ItemStack(ModItems.SPRING.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Use a Vehicle Chain to link two nearby barges.")
                .pointAt(util.vector().of(2.5, 1.8, 3.5))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .text("Linked vessels move together until the Vehicle Chain is taut.")
                .pointAt(util.vector().of(2.5, 1.8, 3.5))
                .placeNearTarget();
        for (int step = 1; step <= 10; step++) {
            double progress = step / 10.0;
            double rearZ = 1.5 + (2.8 - 1.5) * progress;
            double frontZ = 5.5 + (4.0 - 5.5) * progress;
            PonderVesselSceneUtil.move(scene, rearBarge, 2.5,
                    1.5 + (2.8 - 1.5) * (step - 1) / 10.0, 2.5, rearZ, 0.0F);
            PonderVesselSceneUtil.move(scene, frontBarge, 2.5,
                    5.5 + (4.0 - 5.5) * (step - 1) / 10.0, 2.5, frontZ, 0.0F);
            scene.idle(3);
        }
        scene.idle(45);

        scene.world().createEntity(level -> {
            var tug = PonderVesselSceneUtil.steamTug(level);
            PonderVesselSceneUtil.initialize(tug, 2.5, 5.2, 0.0F, DyeColor.RED);
            return tug;
        });
        scene.idle(20);

        scene.overlay().showText(70)
                .colored(PonderPalette.OUTPUT)
                .text("A convoy can have one tug followed by any number of barges.")
                .pointAt(util.vector().of(2.5, 1.8, 4.5))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }
}
