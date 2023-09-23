package dev.murad.shipping.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.Registration;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(), ImmutableList.of(
                new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {
        protected ModBlockLootTables() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            dropSelf(ModBlocks.TUG_DOCK.get());
            dropSelf(ModBlocks.BARGE_DOCK.get());
            dropSelf(ModBlocks.GUIDE_RAIL_CORNER.get());
            dropSelf(ModBlocks.GUIDE_RAIL_TUG.get());
            dropSelf(ModBlocks.FLUID_HOPPER.get());
            dropSelf(ModBlocks.VESSEL_CHARGER.get());
            dropSelf(ModBlocks.VESSEL_DETECTOR.get());
            dropSelf(ModBlocks.SWITCH_RAIL.get());
            dropSelf(ModBlocks.AUTOMATIC_SWITCH_RAIL.get());
            dropSelf(ModBlocks.TEE_JUNCTION_RAIL.get());
            dropSelf(ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get());
            dropSelf(ModBlocks.JUNCTION_RAIL.get());
            dropSelf(ModBlocks.RAPID_HOPPER.get());
            dropSelf(ModBlocks.CAR_DOCK_RAIL.get());
            dropSelf(ModBlocks.LOCOMOTIVE_DOCK_RAIL.get());
        }

        @Override
        public @NotNull Iterable<Block> getKnownBlocks() {
            return Registration.BLOCKS.getEntries().stream()
                    .map(RegistryObject::get)
                    .collect(Collectors.toList());
        }
    }
}
