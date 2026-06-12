package dev.murad.shipping.setup;


import com.google.common.collect.ImmutableList;
import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.guiderail.CornerGuideRailBlock;
import dev.murad.shipping.block.guiderail.TugGuideRailBlock;
import dev.murad.shipping.block.rail.*;
import dev.murad.shipping.block.vesseldetector.VesselDetectorBlock;
import dev.murad.shipping.util.MultiMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.*;
import java.util.function.Supplier;

public class ModBlocks {

    private static final MultiMap<ResourceKey<CreativeModeTab>, DeferredHolder<?, ? extends ItemLike>> PRIVATE_TAB_REGISTRY = new MultiMap<>();

    // Taken from IRON_BLOCK
    private static BlockBehaviour.Properties METAL_BLOCK_BEHAVIOUR =
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.METAL);
    private static BlockBehaviour.Properties RAIL_BLOCK_BEHAVIOUR = BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL);

    public static final DeferredHolder<Block, Block> GUIDE_RAIL_CORNER = register(
            "guide_rail_corner",
            () -> new CornerGuideRailBlock(METAL_BLOCK_BEHAVIOUR),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> VESSEL_DETECTOR = register(
            "vessel_detector",
            () -> new VesselDetectorBlock(METAL_BLOCK_BEHAVIOUR),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> GUIDE_RAIL_TUG = register(
            "guide_rail_tug",
            () -> new TugGuideRailBlock(METAL_BLOCK_BEHAVIOUR),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> SWITCH_RAIL = register(
            "switch_rail",
            () -> new SwitchRail(RAIL_BLOCK_BEHAVIOUR, false),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> AUTOMATIC_SWITCH_RAIL = register(
            "automatic_switch_rail",
            () -> new SwitchRail(RAIL_BLOCK_BEHAVIOUR, true),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> TEE_JUNCTION_RAIL = register(
            "tee_junction_rail",
            () -> new TeeJunctionRail(RAIL_BLOCK_BEHAVIOUR, false),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> AUTOMATIC_TEE_JUNCTION_RAIL = register(
            "automatic_tee_junction_rail",
            () -> new TeeJunctionRail(RAIL_BLOCK_BEHAVIOUR, true),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> JUNCTION_RAIL = register(
            "junction_rail",
            () -> new JunctionRail(RAIL_BLOCK_BEHAVIOUR),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static final DeferredHolder<Block, Block> PORTAL_RAIL = registerNoItem(
            "portal_rail",
            () -> new PortalRail(RAIL_BLOCK_BEHAVIOUR));

    public static final DeferredHolder<Block, Block> DOCKING_STATION = register(
            "docking_station",
            () -> new DockingStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()),
            ImmutableList.of(
                    CreativeModeTabs.TOOLS_AND_UTILITIES,
                    CreativeModeTabs.REDSTONE_BLOCKS));

    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        PRIVATE_TAB_REGISTRY.getOrDefault(event.getTabKey(), new ArrayList<>())
                .forEach(holder -> event.accept(holder.get()));
    }

    private static <T extends Block> DeferredHolder<Block, T> registerNoItem(String name, Supplier<T> block){
        return Registration.BLOCKS.register(name, block);
    }

    private static <T extends Block> DeferredHolder<Block, T> register(String name, Supplier<T> block, List<ResourceKey<CreativeModeTab>> tabs){
        DeferredHolder<Block, T> ret = registerNoItem(name, block);
        DeferredHolder<Item, BlockItem> item = Registration.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties()));

        for (var tab : tabs) {
            PRIVATE_TAB_REGISTRY.putInsert(tab, item);
        }

        return ret;
    }

    public static void register () {}
}
