package dev.murad.shipping.data.client;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dock.BargeDockBlock;
import dev.murad.shipping.block.dock.TugDockBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.data.DataGenerator;
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
        return  models().orientable("tug_dock" + inv,
                getBlTx("tug_dock"),
                getBlTx("tug_dock_front"),
                getBlTx("tug_dock_top" + inv));
    }

    @Override
    protected void registerStatesAndModels() {
        getVariantBuilder(ModBlocks.TUG_DOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(getTugDockModel(state))
                .rotationY((int) state.getValue(TugDockBlock.FACING).getOpposite().toYRot())
                .build()
        );

        ModelFile bargeDockModel = models().orientable("barge_dock",
                getBlTx("barge_dock"),
                getBlTx("barge_dock_front"),
                getBlTx("barge_dock_top"));

        getVariantBuilder(ModBlocks.BARGE_DOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(bargeDockModel)
                .rotationY((int) state.getValue(BargeDockBlock.FACING).getOpposite().toYRot())
                .build()
        );
    }
}
