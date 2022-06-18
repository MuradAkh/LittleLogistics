package dev.murad.shipping.setup;


import dev.murad.shipping.block.dock.BargeDockBlock;
import dev.murad.shipping.block.dock.TugDockBlock;
import dev.murad.shipping.block.energy.VesselChargerBlock;
import dev.murad.shipping.block.fluid.FluidHopperBlock;
import dev.murad.shipping.block.guiderail.CornerGuideRailBlock;
import dev.murad.shipping.block.guiderail.TugGuideRailBlock;
import dev.murad.shipping.block.portal.AdvancedTrainPortalBlock;
import dev.murad.shipping.block.portal.NetherTrainPortalBlock;
import dev.murad.shipping.block.rail.*;
import dev.murad.shipping.block.rapidhopper.RapidHopperBlock;
import dev.murad.shipping.block.vesseldetector.VesselDetectorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final RegistryObject<Block> TUG_DOCK = register(
            "tug_dock",
            () -> new TugDockBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> BARGE_DOCK = register(
            "barge_dock",
            () -> new BargeDockBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> GUIDE_RAIL_CORNER = register(
            "guide_rail_corner",
            () -> new CornerGuideRailBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> VESSEL_DETECTOR = register(
            "vessel_detector",
            () -> new VesselDetectorBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> GUIDE_RAIL_TUG = register(
            "guide_rail_tug",
            () -> new TugGuideRailBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> FLUID_HOPPER = register(
            "fluid_hopper",
            () -> new FluidHopperBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> VESSEL_CHARGER = register(
            "vessel_charger",
            () -> new VesselChargerBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> RAPID_HOPPER = register(
            "rapid_hopper",
            () -> new RapidHopperBlock(Block.Properties.of(Material.METAL)
                    .destroyTime(0.5f)
            ),
            CreativeModeTab.TAB_REDSTONE);

    public static final RegistryObject<Block> SWITCH_RAIL = register(
            "switch_rail",
            () -> new SwitchRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL), false),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> AUTOMATIC_SWITCH_RAIL = register(
            "automatic_switch_rail",
            () -> new SwitchRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL), true),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> TEE_JUNCTION_RAIL = register(
            "tee_junction_rail",
            () -> new TeeJunctionRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL), false),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> AUTOMATIC_TEE_JUNCTION_RAIL = register(
            "automatic_tee_junction_rail",
            () -> new TeeJunctionRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL), true),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> JUNCTION_RAIL = register(
            "junction_rail",
            () -> new JunctionRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> CAR_DOCK_RAIL = register(
            "car_dock_rail",
            () -> new TrainCarDockingRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> LOCOMOTIVE_DOCK_RAIL = register(
            "locomotive_dock_rail",
            () -> new LocomotiveDockingRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> NETHER_TRAIN_PORTAL = register(
            "nether_train_portal",
            () -> new NetherTrainPortalBlock(BlockBehaviour.Properties.of(Material.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> ADVANCED_TRAIN_PORTAL = register(
            "advanced_train_portal",
            () -> new AdvancedTrainPortalBlock(BlockBehaviour.Properties.of(Material.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);

    public static final RegistryObject<Block> OBSIDIAN_RAIL = register(
            "obsidian_rail",
            () -> new ObsidianRail(BlockBehaviour.Properties.of(Material.DECORATION).noCollission().strength(0.7F).sound(SoundType.METAL)),
            CreativeModeTab.TAB_TRANSPORTATION);




    public static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> block){
        return Registration.BLOCKS.register(name, block);
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, CreativeModeTab group){
        RegistryObject<T> ret = registerNoItem(name, block);
        Registration.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties().tab(group)));
        return ret;
    }

    public static void register () {}
}
