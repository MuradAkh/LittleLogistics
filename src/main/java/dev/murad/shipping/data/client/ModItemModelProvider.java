package dev.murad.shipping.data.client;


import dev.murad.shipping.ShippingMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
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
        withExistingParent("fluid_hopper", modLoc("block/fluid_hopper"));
        withExistingParent("vessel_detector", modLoc("block/vessel_detector"));
        withExistingParent("vessel_charger", modLoc("block/vessel_charger"));

        builder(itemGenerated, "barge");
        builder(itemGenerated, "chunk_loader_barge");
        builder(itemGenerated, "fishing_barge");
        builder(itemGenerated, "fluid_barge");
        builder(itemGenerated, "seater_barge");
        builder(itemGenerated, "tug");
        builder(itemGenerated, "energy_tug");
        builder(itemGenerated, "book");
        builder(itemGenerated, "tug_route")
                .override()
                .model(builder(itemGenerated, "tug_route_empty"))
                .predicate(new ResourceLocation(ShippingMod.MOD_ID, "routestate"), 1f).end();

        builder(itemGenerated, "spring")
                .override()
                .model(builder(itemGenerated, "spring_dominant_selected"))
                .predicate(new ResourceLocation(ShippingMod.MOD_ID, "springstate"), 1f).end();

        builder(itemGenerated, "creative_capacitor");
        builder(itemGenerated, "rapid_hopper");
    }



    private ItemModelBuilder builder(ModelFile itemGenerated, String name) {
        return getBuilder(name).parent(itemGenerated).texture("layer0", "item/" + name);
    }
}
