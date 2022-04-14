package dev.murad.shipping.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(DataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return ImmutableList.of(
                Pair.of(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        );
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker) {
        map.forEach((p_218436_2_, p_218436_3_) -> LootTables.validate(validationtracker, p_218436_2_, p_218436_3_));
    }

    public static class ModBlockLootTables extends BlockLoot {
        @Override
        protected void addTables() {
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
        protected Iterable<Block> getKnownBlocks() {
            return Registration.BLOCKS.getEntries().stream()
                    .map(RegistryObject::get)
                    .collect(Collectors.toList());
        }
    }
}
