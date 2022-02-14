package dev.murad.shipping.data.client;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dock.BargeDockBlock;
import dev.murad.shipping.block.dock.TugDockBlock;
import dev.murad.shipping.block.vessel_detector.VesselDetectorBlock;
import dev.murad.shipping.block.energy.VesselChargerBlock;
import dev.murad.shipping.block.fluid.FluidHopperBlock;
import dev.murad.shipping.block.guide_rail.CornerGuideRailBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, ShippingMod.MOD_ID, exFileHelper);
    }

    public static ResourceLocation getBlTx(String name){
        return new ResourceLocation(ShippingMod.MOD_ID, String.format("block/%s", name));
    }

    private ModelFile getTugDockModel(BlockState state){
        String inv = state.getValue(TugDockBlock.INVERTED) ? "_inv" : "";
        String powered = state.getValue(TugDockBlock.POWERED) ? "_powered" : "";
        return  models().orientable("tug_dock" + inv + powered,
                getBlTx("tug_dock"),
                getBlTx("tug_dock_front" + powered),
                getBlTx("tug_dock_top" + inv));
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

    private ModelFile getBargeDockModel(BlockState state){
        String inv = state.getValue(BargeDockBlock.EXTRACT_MODE) ? "_extract" : "";
        return  models().orientable("barge_dock" + inv,
                getBlTx("barge_dock"),
                getBlTx("barge_dock_front" + inv),
                getBlTx("barge_dock_top"));
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
        getVariantBuilder(ModBlocks.TUG_DOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getTugDockModel(state))
                .rotationY((int) state.getValue(TugDockBlock.FACING).getOpposite().toYRot())
                .build()
        );

        getVariantBuilder(ModBlocks.BARGE_DOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getBargeDockModel(state))
                .rotationY((int) state.getValue(BargeDockBlock.FACING).getOpposite().toYRot())
                .build()
        );

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

        getVariantBuilder(ModBlocks.FLUID_HOPPER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models()
                        .withExistingParent("fluid_hopper", modLoc("fluid_hopper_parent_model"))
                )
                .rotationY((int) state.getValue(FluidHopperBlock.FACING).getClockWise().toYRot())
                .build()
        );

        getVariantBuilder(ModBlocks.VESSEL_CHARGER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models()
                        .withExistingParent("vessel_charger", modLoc("vessel_charger_parent_model"))
                )
                .rotationY((int) state.getValue(VesselChargerBlock.FACING).getOpposite().toYRot())
                .build()
        );
    }
}
