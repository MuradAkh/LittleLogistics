package dev.murad.shipping.data.client;


import dev.murad.shipping.ShippingMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ShippingMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ModelFile itemGenerated = getExistingFile(mcLoc("item/generated"));
        withExistingParent("docking_station", modLoc("block/docking_station_crane"));
        withExistingParent("vessel_detector", modLoc("block/vessel_detector"));

        builder(itemGenerated, "barge");
        builder(itemGenerated, "barrel_barge");
        builder(itemGenerated, "vacuum_barge");
        builder(itemGenerated, "chunk_loader_barge");
        builder(itemGenerated, "fishing_barge");
        builder(itemGenerated, "fluid_barge");
        builder(itemGenerated, "seater_barge");
        builder(itemGenerated, "tug");
        builder(itemGenerated, "energy_tug");
        builder(itemGenerated, "steam_locomotive");
        builder(itemGenerated, "energy_locomotive");
        builder(itemGenerated, "chest_car");
        builder(itemGenerated, "barrel_car");
        builder(itemGenerated, "chunk_loader_car");
        builder(itemGenerated, "fluid_car");
        builder(itemGenerated, "seater_car");
        builder(itemGenerated, "book");
        getBuilder("tug_route").parent(itemGenerated).texture("layer0", "item/tug_route_blank")
                .override()
                .model(builder(itemGenerated, "tug_route_incomplete"))
                .predicate(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "routestate"), 1f).end()
                .override()
                .model(getBuilder("tug_route_complete").parent(itemGenerated).texture("layer0", "item/tug_route"))
                .predicate(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "routestate"), 2f).end();

        builder(itemGenerated, "spring")
                .override()
                .model(builder(itemGenerated, "spring_dominant_selected"))
                .predicate(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "springstate"), 1f).end();

        builder(itemGenerated, "conductors_wrench");
        builder(itemGenerated, "creative_capacitor");
        builder(itemGenerated, "dock_rail");
        builder(itemGenerated, "switch_rail");
        builder(itemGenerated, "automatic_switch_rail");
        builder(itemGenerated, "tee_junction_rail");
        builder(itemGenerated, "automatic_tee_junction_rail");
        builder(itemGenerated, "junction_rail");

        builder(itemGenerated, "receiver_component");
        builder(itemGenerated, "transmitter_component");

        getBuilder("locomotive_route").parent(itemGenerated).texture("layer0", "item/locomotive_route_blank")
                .override()
                .model(builder(itemGenerated, "locomotive_route_incomplete"))
                .predicate(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "locoroutestate"), 1f).end()
                .override()
                .model(getBuilder("locomotive_route_complete").parent(itemGenerated).texture("layer0", "item/locomotive_route"))
                .predicate(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "locoroutestate"), 2f).end();
    }



    private ItemModelBuilder builder(ModelFile itemGenerated, String name) {
        return getBuilder(name).parent(itemGenerated).texture("layer0", "item/" + name);
    }
}
