package ca.edtoaster.littlecontraptions.data;

import ca.edtoaster.littlecontraptions.setup.LCBlocks;
import ca.edtoaster.littlecontraptions.setup.Registration;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(PackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, Set.of(), ImmutableList.of(
                new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {
        protected ModBlockLootTables(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }
        @Override
        protected void generate() {
            dropSelf(LCBlocks.BARGE_ASSEMBLER.get());
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return Registration.BLOCKS.getEntries().stream()
                    .map(DeferredHolder::get)
                    .collect(Collectors.toList());
        }
    }
}
