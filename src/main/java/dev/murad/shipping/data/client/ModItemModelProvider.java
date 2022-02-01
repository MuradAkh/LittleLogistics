package dev.murad.shipping.data.client;


import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.SpringItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, ShippingMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ModelFile itemGenerated = getExistingFile(mcLoc("item/generated"));
        withExistingParent("tug_dock", modLoc("block/tug_dock"));
        withExistingParent("barge_dock", modLoc("block/barge_dock"));
        withExistingParent("guide_rail_corner", modLoc("block/guide_rail_corner"));
        withExistingParent("guide_rail_tug", modLoc("block/guide_rail_tug"));



        builder(itemGenerated, "barge");
        builder(itemGenerated, "chunk_loader_barge");
        builder(itemGenerated, "fishing_barge");
        builder(itemGenerated, "fluid_barge");
        builder(itemGenerated, "tug");
        builder(itemGenerated, "book");
        builder(itemGenerated, "tug_route");
        builder(itemGenerated, "spring")
                .override()
                .model(builder(itemGenerated, "spring_dominant_selected"))
                .predicate(new ResourceLocation(ShippingMod.MOD_ID, "springstate"), 1f).end();
    }



    private ItemModelBuilder builder(ModelFile itemGenerated, String name) {
        return getBuilder(name).parent(itemGenerated).texture("layer0", "item/" + name);
    }
}
