package dev.murad.shipping.data;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(DataGenerator p_i48262_1_) {
        super(p_i48262_1_);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(ModBlocks.TUG_DOCK.get(), 2)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.IRON_INGOT)
                .pattern("___")
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.BARGE_DOCK.get(), 2)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.IRON_INGOT)
                .pattern("___")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.VESSEL_DETECTOR.get(), 2)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.REDSTONE_TORCH)
                .pattern("_#_")
                .pattern("_$_")
                .pattern("___")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);


        ShapedRecipeBuilder.shaped(ModBlocks.GUIDE_RAIL_CORNER.get(), 3)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.POWERED_RAIL)
                .pattern("#__")
                .pattern("$__")
                .pattern("#__")
                .unlockedBy("has_item", has(Items.POWERED_RAIL))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.SWITCH_RAIL.get(), 4)
                .define('#', Items.RAIL)
                .pattern(" # ")
                .pattern(" ##")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.RAIL))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.JUNCTION_RAIL.get(), 5)
                .define('#', Items.RAIL)
                .pattern(" # ")
                .pattern("###")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.RAIL))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.GUIDE_RAIL_TUG.get(), 8)
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.POWERED_RAIL)
                .pattern("#$#")
                .pattern("___")
                .pattern("___")
                .unlockedBy("has_item", has(Items.POWERED_RAIL))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.FLUID_HOPPER.get(), 1)
                .define('_', Items.GLASS)
                .define('$', Items.HOPPER)
                .pattern("_$_")
                .pattern(" _ ")
                .unlockedBy("has_item", has(Items.HOPPER))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.VESSEL_CHARGER.get(), 1)
                .define('_', Items.REDSTONE_BLOCK)
                .define('$', Items.IRON_INGOT)
                .define('.', Items.GOLD_INGOT)
                .pattern(" . ")
                .pattern(" $ ")
                .pattern("_$_")
                .unlockedBy("has_item", has(Items.REDSTONE))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.SPRING.get(), 6)
                .define('_', Tags.Items.STRING)
                .define('$', Items.IRON_NUGGET)
                .pattern("_$_")
                .pattern("$_$")
                .unlockedBy("has_item", has(Items.STRING))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.TUG_ROUTE.get())
                .define('_', Items.COMPASS)
                .define('#', Items.REDSTONE)
                .define('$', Items.IRON_NUGGET)
                .pattern(" # ")
                .pattern("$_$")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.REDSTONE))
                .save(consumer);

        ShapelessRecipeBuilder.shapeless(ModItems.TUG_ROUTE.get())
                .unlockedBy("has_item", has(Items.REDSTONE))
                .requires(ModItems.TUG_ROUTE.get())
                .save(consumer,new ResourceLocation(ShippingMod.MOD_ID, "route_reset"));

        ShapedRecipeBuilder.shaped(ModItems.STEAM_TUG.get())
                .define('_', Items.PISTON)
                .define('#', Items.FURNACE)
                .define('$', Items.IRON_INGOT)
                .pattern(" $ ")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.PISTON))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.ENERGY_TUG.get())
                .define('_', Items.PISTON)
                .define('#', ModBlocks.VESSEL_CHARGER.get())
                .define('$', Items.IRON_INGOT)
                .pattern(" $ ")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.PISTON))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHEST_BARGE.get())
                .define('_', Items.CHEST)
                .define('#', Items.STICK)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.CHEST))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.SEATER_BARGE.get())
                .define('_', ItemTags.WOODEN_STAIRS)
                .define('#', ItemTags.SIGNS)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.IRON_INGOT))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.FISHING_BARGE.get())
                .define('#', Items.FISHING_ROD)
                .define('$', Items.IRON_INGOT)
                .pattern("###")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.FISHING_ROD))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.FLUID_BARGE.get())
                .define('#', Items.GLASS)
                .define('$', Items.IRON_INGOT)
                .pattern("# #")
                .pattern(" # ")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.GLASS))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHUNK_LOADER_BARGE.get())
                .define('_', Items.ENDER_EYE)
                .define('#', Items.OBSIDIAN)
                .define('$', Items.IRON_INGOT)
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.ENDER_EYE))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.RAPID_HOPPER.get())
                .define('_', Items.HOPPER)
                .define('#', Items.REDSTONE_BLOCK)
                .define('$', Items.GOLD_INGOT)
                .pattern("$_$")
                .pattern(" # ")
                .unlockedBy("has_item", has(Items.HOPPER))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.SEATER_CAR.get())
                .define('#', ItemTags.PLANKS)
                .define('$', Items.IRON_INGOT)
                .pattern("   ")
                .pattern("###")
                .pattern("$ $")
                .unlockedBy("has_item", has(Items.IRON_INGOT))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHEST_CAR.get())
                .define('#', Items.CHEST)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("   ")
                .pattern(" # ")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHUNK_LOADER_CAR.get())
                .define('#', Items.ENDER_EYE)
                .define('_', Items.OBSIDIAN)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("   ")
                .pattern("_#_")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.FLUID_CAR.get())
                .define('#', Items.GLASS)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern("# #")
                .pattern(" # ")
                .pattern(" $ ")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.ENERGY_LOCOMOTIVE.get())
                .define('#', Items.IRON_INGOT)
                .define('.', ModBlocks.VESSEL_CHARGER.get())
                .define('_', Blocks.PISTON)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern(" # ")
                .pattern("_._")
                .pattern("#$#")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.STEAM_LOCOMOTIVE.get())
                .define('#', Items.IRON_INGOT)
                .define('.', Items.FURNACE)
                .define('_', Blocks.PISTON)
                .define('$', ModItems.SEATER_CAR.get())
                .pattern(" # ")
                .pattern("_._")
                .pattern("#$#")
                .unlockedBy("has_item", has(ModItems.SEATER_CAR.get()))
                .save(consumer);


    }


}
