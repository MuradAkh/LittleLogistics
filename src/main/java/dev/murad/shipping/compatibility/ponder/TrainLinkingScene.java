package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

final class TrainLinkingScene {

    private TrainLinkingScene() {
    }

    static void linking(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("train_linking", "Linking Train Cars");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(10);

        ElementLink<EntityElement> firstCar = scene.world().createEntity(level -> {
            ChestCarEntity car = new ChestCarEntity(ModEntityTypes.CHEST_CAR.get(), level);
            car.setPos(2.5, 1.0625, 1.5);
            return car;
        });
        scene.idle(10);

        ElementLink<EntityElement> secondCar = scene.world().createEntity(level -> {
            ChestCarEntity car = new ChestCarEntity(ModEntityTypes.CHEST_CAR.get(), level);
            car.setPos(2.5, 1.0625, 4.5);
            return car;
        });
        scene.overlay().showControls(util.vector().centerOf(2, 1, 3), Pointing.DOWN, 50)
                .withItem(new ItemStack(ModItems.SPRING.get()))
                .rightClick();
        scene.overlay().showText(70)
                .colored(PonderPalette.INPUT)
                .text("Use a Vehicle Chain to link two nearby train cars.")
                .pointAt(util.vector().centerOf(2, 1, 3))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(60)
                .colored(PonderPalette.INPUT)
                .text("Linked cars pull together until the Vehicle Chain is taut.")
                .pointAt(util.vector().centerOf(2, 1, 3))
                .placeNearTarget();
        for (double firstCarZ : new double[] {1.75, 2.0, 2.25, 2.5}) {
            double secondCarZ = 6.0 - firstCarZ;
            scene.world().modifyEntity(firstCar, entity -> entity.setPos(2.5, 1.0625, firstCarZ));
            scene.world().modifyEntity(secondCar, entity -> entity.setPos(2.5, 1.0625, secondCarZ));
            scene.idle(7);
        }
        scene.idle(32);

        scene.world().createEntity(level -> {
            SteamLocomotiveEntity locomotive = new SteamLocomotiveEntity(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level);
            locomotive.setPos(2.5, 1.0625, 4.5);
            return locomotive;
        });
        scene.idle(20);

        scene.overlay().showText(60)
                .text("A train can have one locomotive and any number of cars.")
                .pointAt(util.vector().centerOf(2, 1, 3))
                .placeNearTarget();
        scene.idle(70);
        scene.markAsFinished();
    }
}
