package dev.murad.shipping.data.client;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dock.DockBlock;
import dev.murad.shipping.block.dock.DockRail;
import dev.murad.shipping.block.guiderail.CornerGuideRailBlock;
import dev.murad.shipping.block.rail.SwitchRail;
import dev.murad.shipping.block.vesseldetector.VesselDetectorBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ShippingMod.MOD_ID, exFileHelper);
    }

    public static ResourceLocation getBlTx(String name){
        return ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, String.format("block/%s", name));
    }

    private ModelFile getCornerGuideRailModel(BlockState state){
        String inv = state.getValue(CornerGuideRailBlock.INVERTED) ? "_inv" : "";
        return  models().orientable("guide_rail_corner" + inv,
                getBlTx("guide_rail_side"),
                getBlTx("guide_rail_front" + inv),
                getBlTx("guide_rail_top" + inv));
    }

    private ModelFile getTugGuideRailModel(BlockState state){
        return  models().orientable("guide_rail_tug",
                getBlTx("guide_rail_side"),
                getBlTx("guide_rail_side"),
                getBlTx("guide_rail_front"));
    }

    private ModelFile getVesselDetectorModel(BlockState state){
        String powered = state.getValue(VesselDetectorBlock.POWERED) ? "_powered" : "";

        return models().withExistingParent("vessel_detector" + powered, modLoc("orientable_with_back"))
                .texture("side", getBlTx("vessel_detector_side"))
                .texture("front", getBlTx("vessel_detector_front"))
                .texture("back", getBlTx("vessel_detector_back" + powered));
    }

    private int xRotFromDir(Direction direction){
        switch (direction) {
            case DOWN:
                return 270;
            case UP:
                return 90;
            default:
                return 0;
        }
    }


    @Override
    protected void registerStatesAndModels() {
        getVariantBuilder(ModBlocks.GUIDE_RAIL_CORNER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getCornerGuideRailModel(state))
                .rotationY((int) state.getValue(CornerGuideRailBlock.FACING).getOpposite().toYRot())
                .build()
        );

        getVariantBuilder(ModBlocks.VESSEL_DETECTOR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getVesselDetectorModel(state))
                .rotationY((int) state.getValue(VesselDetectorBlock.FACING).getOpposite().toYRot())
                .rotationX(xRotFromDir(state.getValue(VesselDetectorBlock.FACING).getOpposite()))
                .build()
        );

        getVariantBuilder(ModBlocks.GUIDE_RAIL_TUG.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getTugGuideRailModel(state))
                .rotationY((int) state.getValue(CornerGuideRailBlock.FACING).getClockWise().toYRot())
                .build()
        );

        getVariantBuilder(ModBlocks.SWITCH_RAIL.get()).forAllStates(state ->  {
            String outDir = state.getValue(SwitchRail.OUT_DIRECTION).getSerializedName();
            String powered = state.getValue(SwitchRail.POWERED) ? "on" : "off";
            return ConfiguredModel.builder()
                    .modelFile(models()
                            .withExistingParent("switch_rail_" + outDir + "_" + powered, mcLoc("rail_flat"))
                            .texture("rail", getBlTx("switch_rail_" + outDir + "_" + powered)))
                    .rotationY((int) state.getValue(SwitchRail.FACING).getOpposite().toYRot())
                    .build();
        });

        getVariantBuilder(ModBlocks.AUTOMATIC_SWITCH_RAIL.get()).forAllStates(state ->  {
            String outDir = state.getValue(SwitchRail.OUT_DIRECTION).getSerializedName();
            String powered = state.getValue(SwitchRail.POWERED) ? "on" : "off";
            return ConfiguredModel.builder()
                    .modelFile(models()
                            .withExistingParent("automatic_switch_rail_" + outDir + "_" + powered, mcLoc("rail_flat"))
                            .texture("rail", getBlTx("automatic_switch_rail_" + outDir + "_" + powered)))
                    .rotationY((int) state.getValue(SwitchRail.FACING).getOpposite().toYRot())
                    .build();
        });

        getVariantBuilder(ModBlocks.TEE_JUNCTION_RAIL.get()).forAllStates(state ->  {
            String powered = state.getValue(SwitchRail.POWERED) ? "on" : "off";
            return ConfiguredModel.builder()
                    .modelFile(models()
                            .withExistingParent("tee_junction_rail_" + powered, mcLoc("rail_flat"))
                            .texture("rail", getBlTx("tee_junction_rail_" + powered)))
                    .rotationY((int) state.getValue(SwitchRail.FACING).getOpposite().toYRot())
                    .build();
        });

        getVariantBuilder(ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get()).forAllStates(state ->  {
            String powered = state.getValue(SwitchRail.POWERED) ? "on" : "off";
            return ConfiguredModel.builder()
                    .modelFile(models()
                            .withExistingParent("automatic_tee_junction_rail_" + powered, mcLoc("rail_flat"))
                            .texture("rail", getBlTx("automatic_tee_junction_rail_" + powered)))
                    .rotationY((int) state.getValue(SwitchRail.FACING).getOpposite().toYRot())
                    .build();
        });

        getVariantBuilder(ModBlocks.JUNCTION_RAIL.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models()
                        .withExistingParent("junction_rail", mcLoc("rail_flat"))
                        .texture("rail", getBlTx("junction_rail")))
                .build());

        getVariantBuilder(ModBlocks.DOCK_BLOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models().orientable("dock",
                        getBlTx("dock_side"),
                        getBlTx("dock_front"),
                        getBlTx("dock_top")))
                .rotationY((int) state.getValue(DockBlock.FACING).getOpposite().toYRot())
                .build()
        );

        // --- Dock Rail (multipart) ---
        ResourceLocation deepslate = ResourceLocation.withDefaultNamespace("block/deepslate");

        ModelFile dockRailModel = models()
                .withExistingParent("dock_rail", mcLoc("rail_flat"))
                .texture("rail", getBlTx("dock_rail"));

        getMultipartBuilder(ModBlocks.DOCK_RAIL.get())
                // Base: north_south
                .part().modelFile(dockRailModel).addModel()
                    .condition(DockRail.RAIL_SHAPE, RailShape.NORTH_SOUTH).end()
                // Base: east_west (rotated 90)
                .part().modelFile(dockRailModel).rotationY(90).addModel()
                    .condition(DockRail.RAIL_SHAPE, RailShape.EAST_WEST).end()
                // North panel
                .part().modelFile(models().getBuilder("dock_rail_panel_north")
                    .texture("panel", deepslate)
                    .element().from(0, 0, -1).to(16, 16, 1)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.NORTH, true).end()
                // South panel
                .part().modelFile(models().getBuilder("dock_rail_panel_south")
                    .texture("panel", deepslate)
                    .element().from(0, 0, 15).to(16, 16, 17)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.SOUTH, true).end()
                // East panel
                .part().modelFile(models().getBuilder("dock_rail_panel_east")
                    .texture("panel", deepslate)
                    .element().from(15, 0, 0).to(17, 16, 16)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.EAST, true).end()
                // West panel
                .part().modelFile(models().getBuilder("dock_rail_panel_west")
                    .texture("panel", deepslate)
                    .element().from(-1, 0, 0).to(1, 16, 16)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.WEST, true).end()
                // Top panel (tunnel ceiling)
                .part().modelFile(models().getBuilder("dock_rail_panel_top")
                    .texture("panel", deepslate)
                    .element().from(0, 15, 0).to(16, 17, 16)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.UP, true).end()
                // Bottom panel (opaque floor)
                .part().modelFile(models().getBuilder("dock_rail_panel_bottom")
                    .texture("panel", deepslate)
                    .element().from(0, 0, 0).to(16, 2, 16)
                        .allFaces((dir, f) -> f.texture("#panel")).end())
                    .addModel()
                    .condition(DockRail.DOWN, true).end();
    }

}
