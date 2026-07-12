package dev.murad.shipping.data.client;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.dockingstation.DockingStationPart;
import dev.murad.shipping.block.guiderail.CornerGuideRailBlock;
import dev.murad.shipping.block.rail.PortalRail;
import dev.murad.shipping.block.rail.SwitchRail;
import dev.murad.shipping.block.vesseldetector.VesselDetectorBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
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

        ModelFile portalRailModel = models().getExistingFile(modLoc("block/portal_rail"));
        getVariantBuilder(ModBlocks.PORTAL_RAIL.get()).forAllStates(state -> {
            Direction facing = state.getValue(PortalRail.PORTAL_FACING);
            int yRot = switch (facing) {
                case NORTH -> 0;
                case EAST  -> 90;
                case SOUTH -> 180;
                default    -> 270; // WEST
            };
            return ConfiguredModel.builder()
                    .modelFile(portalRailModel)
                    .rotationY(yRot)
                    .build();
        });

        // --- Docking Station ---
        // Models are hand-crafted in src/main/resources; just reference them.
        ModelFile craneModel = models().getExistingFile(modLoc("block/docking_station_crane"));
        ModelFile emptyModel = models().getExistingFile(modLoc("block/docking_station_empty"));

        getVariantBuilder(ModBlocks.DOCKING_STATION.get()).forAllStates(state -> {
            Direction facing = state.getValue(DockingStationBlock.FACING);
            DockingStationPart part = state.getValue(DockingStationBlock.PART);

            // Crane model is exported facing opposite the block's outward direction.
            int portRot = ((int) facing.getOpposite().toYRot() + 180) % 360;

            return switch (part) {
                case CONTROLLER -> ConfiguredModel.builder()
                        .modelFile(craneModel)
                        .rotationY(portRot)
                        .build();
                case BRIDGE -> ConfiguredModel.builder()
                        .modelFile(emptyModel)
                        .rotationY(portRot)
                        .build();
                case LEFT_TOP -> ConfiguredModel.builder()
                        .modelFile(emptyModel)
                        .build();
            };
        });
    }

}
