package dev.murad.shipping.data;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModTags;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(DataGenerator gen, ExistingFileHelper existingFileHelper) {
        super(gen, ShippingMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(BlockTags.RAILS).add(ModBlocks.SWITCH_RAIL.get());
        tag(BlockTags.RAILS).add(ModBlocks.JUNCTION_RAIL.get());
        tag(BlockTags.RAILS).add(ModBlocks.LOCOMOTIVE_DOCK_RAIL.get());
        tag(BlockTags.RAILS).add(ModBlocks.CAR_DOCK_RAIL.get());
    }
}
