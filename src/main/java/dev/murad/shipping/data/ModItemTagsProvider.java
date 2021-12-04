package dev.murad.shipping.data;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModTags;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(DataGenerator dataGenerator, BlockTagsProvider blockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(dataGenerator, blockTagsProvider, ShippingMod.MOD_ID, existingFileHelper);
    }

    protected void addTags() {
        copy(ModTags.Blocks.SHIP_LOCK, ModTags.Items.SHIP_LOCK);
        copy(Tags.Blocks.CHESTS, Tags.Items.CHESTS);

        tag(ModTags.Items.SHIP_LINK).add(ModItems.SHIP_LINK.get());
        tag(Tags.Items.STRING).addTag(ModTags.Items.SHIP_LINK);
    }
}
