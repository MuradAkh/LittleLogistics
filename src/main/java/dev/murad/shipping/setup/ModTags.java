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
//            return BlockTags.bind(new ResourceLocation("forge", path).toString());
//        }
//
//        private static Tag.Named<Block> mod(String path) {
//            return BlockTags.bind(new ResourceLocation(ShippingMod.MOD_ID, path).toString());
//        }
    }

    public static final class Items {
        public static final TagKey<Item> WRENCHES = forge("tools/wrench");

        private static TagKey<Item> forge(String path) {
            return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
        }

        private static TagKey<Item> mod(String path) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(ShippingMod.MOD_ID, path));
        }
    }
}
