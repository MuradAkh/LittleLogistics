package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final class Blocks {
//        private static Tag.Named<Block> forge(String path) {
//            return BlockTags.bind(ResourceLocation.fromNamespaceAndPath("forge", path).toString());
//        }
//
//        private static Tag.Named<Block> mod(String path) {
//            return BlockTags.bind(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, path).toString());
//        }
    }

    public static final class Items {
        public static final TagKey<Item> WRENCHES = conventional("tools/wrench");

        private static TagKey<Item> conventional(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private static TagKey<Item> mod(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, path));
        }
    }
}
