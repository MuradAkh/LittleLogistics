package dev.murad.shipping.data;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(packOutput, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.VESSEL_DETECTOR.get(), 2)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONES)
                .define('$', Items.REDSTONE_TORCH)
                .pattern("_#_")
                .pattern("_$_")
                .pattern("___")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.SWITCH_RAIL.get(), 4)
                .define('#', Items.RAIL)
                .pattern("# ")
                .pattern("##")
                .pattern("# ")
                .unlockedBy("has_item", has(Items.RAIL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.TEE_JUNCTION_RAIL.get(), 4)
                .define('#', Items.RAIL)
                .pattern("###")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.RAIL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.JUNCTION_RAIL.get(), 5)
                .define('#', Items.RAIL)
                .pattern(" # ")
                .pattern("###")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.RAIL))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, ModBlocks.AUTOMATIC_SWITCH_RAIL.get(), 1)
                .requires(ModBlocks.SWITCH_RAIL.get())
                .requires(ModItems.RECEIVER_COMPONENT.get())
                .unlockedBy("has_item", has(Items.RAIL))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get(), 1)
                .requires(ModBlocks.TEE_JUNCTION_RAIL.get())
                .requires(ModItems.RECEIVER_COMPONENT.get())
                .unlockedBy("has_item", has(Items.RAIL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.DOCKING_STATION.get())
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONES)
                .define('$', Items.IRON_INGOT)
                .pattern("$_$")
                .pattern("$_$")
                .pattern("$#$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.SPRING.get(), 6)
                .define('_', Tags.Items.STRINGS)
                .define('$', Items.IRON_NUGGET)
                .pattern("_$_")
                .pattern("$_$")
                .unlockedBy("has_item", has(Items.STRING))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.TUG_ROUTE.get())
                .define('_', ModItems.TRANSMITTER_COMPONENT.get())
                .define('#', Items.REDSTONE)
                .define('$', Items.IRON_NUGGET)
                .pattern(" # ")
                .pattern("$_$")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.LOCO_ROUTE.get())
                .define('_', ModItems.TRANSMITTER_COMPONENT.get())
                .define('#', Items.IRON_NUGGET)
                .define('$', Items.REDSTONE)
                .pattern(" # ")
                .pattern("$_$")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.STEAM_TUG.get())
                .define('_', Items.PISTON)
                .define('#', Items.FURNACE)
                .define('$', Items.IRON_INGOT)
                .pattern(" $ ")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.PISTON))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.ENERGY_TUG.get())
                .define('_', Items.PISTON)
                .define('#', Items.COPPER_BLOCK)
                .define('$', Items.IRON_INGOT)
                .pattern(" $ ")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.PISTON))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.CHEST_BARGE.get())
                .define('_', Items.CHEST)
                .define('#', Items.STICK)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.CHEST))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.BARREL_BARGE.get())
                .define('_', Items.BARREL)
                .define('#', Items.STICK)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.BARREL))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.VACUUM_BARGE.get())
                .define('_', Items.HOPPER)
                .define('#', Items.ENDER_EYE)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.HOPPER))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.SEATER_BARGE.get())
                .define('_', ItemTags.WOODEN_STAIRS)
                .define('#', ItemTags.SIGNS)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.FISHING_BARGE.get())
                .define('#', Items.FISHING_ROD)
                .define('$', Items.IRON_INGOT)
                .pattern("###")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.FISHING_ROD))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.FLUID_BARGE.get())
                .define('#', Items.GLASS)
                .define('$', Items.IRON_INGOT)
                .pattern("# #")
                .pattern(" # ")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.GLASS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.SEATER_CAR.get())
                .define('#', ItemTags.PLANKS)
                .define('$', Items.IRON_INGOT)
                .pattern("   ")
                .pattern("###")
                .pattern("$ $")
                .unlockedBy("has_item", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.CHEST_CAR.get())
                .define('#', Items.CHEST)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("   ")
                .pattern(" # ")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.BARREL_CAR.get())
                .define('#', Items.BARREL)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("   ")
                .pattern(" # ")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.FLUID_CAR.get())
                .define('#', Items.GLASS)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("# #")
                .pattern(" # ")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.ENERGY_LOCOMOTIVE.get())
                .define('#', Items.IRON_INGOT)
                .define('.', Items.COPPER_BLOCK)
                .define('_', Blocks.PISTON)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern(" # ")
                .pattern("_._")
                .pattern("#$#")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModItems.STEAM_LOCOMOTIVE.get())
                .define('#', Items.IRON_INGOT)
                .define('.', Items.FURNACE)
                .define('_', Blocks.PISTON)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern(" # ")
                .pattern("_._")
                .pattern("#$#")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RECEIVER_COMPONENT.get(), 8)
                .define('o', Items.ENDER_EYE)
                .define('#', Items.REDSTONE)
                .define('_', Items.STONE_SLAB)
                .pattern("o")
                .pattern("#")
                .pattern("_")
                .unlockedBy("has_item", has(Items.ENDER_EYE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRANSMITTER_COMPONENT.get(), 4)
                .define('o', Items.ENDER_PEARL)
                .define('#', Items.GLOWSTONE_DUST)
                .define('_', Items.STONE_SLAB)
                .pattern("o")
                .pattern("#")
                .pattern("_")
                .unlockedBy("has_item", has(Items.ENDER_EYE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CONDUCTORS_WRENCH.get(), 1)
                .define('-', Items.IRON_INGOT)
                .define('^', ModItems.SPRING.get())
                .define('r', Items.RED_DYE)
                .pattern("  ^")
                .pattern(" -r")
                .pattern("-  ")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(output);
    }
}
