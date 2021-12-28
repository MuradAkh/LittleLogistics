package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ModTags {
    public static final class Blocks {
        public static final ITag.INamedTag<Block> SHIP_LOCK = forge("chests/ship_lock");

        private static ITag.INamedTag<Block> forge(String path) {
            return BlockTags.bind(new ResourceLocation("forge", path).toString());
        }

        private static ITag.INamedTag<Block> mod(String path) {
            return BlockTags.bind(new ResourceLocation(ShippingMod.MOD_ID, path).toString());
        }
    }

    public static final class Items {

        public static final ITag.INamedTag<Item> SHIP_LOCK = forge("chests/ship_lock");

        private static ITag.INamedTag<Item> forge(String path) {
            return ItemTags.bind(new ResourceLocation("forge", path).toString());
        }

        private static ITag.INamedTag<Item> mod(String path) {
            return ItemTags.bind(new ResourceLocation(ShippingMod.MOD_ID, path).toString());
        }
    }
}
