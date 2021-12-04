package dev.murad.shipping.setup;


import dev.murad.shipping.block.shiplock.ShipLockBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final RegistryObject<Block> SHIP_LOCK = register(
            "ship_lock",
            () -> new Block(AbstractBlock.Properties.of(Material.METAL)
                    .harvestLevel(1)
            ),
            ItemGroup.TAB_TRANSPORTATION);

    public static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> block){
        return Registration.BLOCKS.register(name, block);
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, ItemGroup group){
        RegistryObject<T> ret = registerNoItem(name, block);
        Registration.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties().tab(ItemGroup.TAB_TRANSPORTATION)));
        return ret;
    }

    public static void register () {}
}
