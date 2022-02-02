package dev.murad.shipping.setup;


import dev.murad.shipping.block.dock.BargeDockBlock;
import dev.murad.shipping.block.dock.TugDockBlock;
import dev.murad.shipping.block.fluid.FluidHopperBlock;
import dev.murad.shipping.block.guide_rail.CornerGuideRailBlock;
import dev.murad.shipping.block.guide_rail.TugGuideRailBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final RegistryObject<Block> TUG_DOCK = register(
            "tug_dock",
            () -> new TugDockBlock(AbstractBlock.Properties.of(Material.METAL)
                    .harvestLevel(1)
            ),
            ItemGroup.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> BARGE_DOCK = register(
            "barge_dock",
            () -> new BargeDockBlock(AbstractBlock.Properties.of(Material.METAL)
                    .harvestLevel(1)
            ),
            ItemGroup.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> GUIDE_RAIL_CORNER = register(
            "guide_rail_corner",
            () -> new CornerGuideRailBlock(AbstractBlock.Properties.of(Material.METAL)
                    .harvestLevel(1)
            ),
            ItemGroup.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> GUIDE_RAIL_TUG = register(
            "guide_rail_tug",
            () -> new TugGuideRailBlock(AbstractBlock.Properties.of(Material.METAL)
                    .harvestLevel(1)
            ),
            ItemGroup.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> FLUID_HOPPER = register(
            "fluid_hopper",
            () -> new FluidHopperBlock(AbstractBlock.Properties.of(Material.METAL)
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
