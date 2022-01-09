package dev.murad.shipping.data;

import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.data.*;
import net.minecraft.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(DataGenerator p_i48262_1_) {
        super(p_i48262_1_);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(ModBlocks.TUG_DOCK.get())
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.IRON_INGOT)
                .pattern("___")
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.BARGE_DOCK.get())
                .define('#', ModItems.SPRING.get())
                .define('_', Tags.Items.STONE)
                .define('$', Items.IRON_INGOT)
                .pattern("___")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.SPRING.get(), 2)
                .define('_', Tags.Items.STRING)
                .define('$', Items.IRON_NUGGET)
                .pattern("   ")
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

        ShapedRecipeBuilder.shaped(ModItems.TUG.get())
                .define('_', Items.PISTON)
                .define('#', Items.FURNACE)
                .define('$', Items.IRON_INGOT)
                .pattern(" $ ")
                .pattern("_#_")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.FURNACE))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHEST_BARGE.get())
                .define('_', Items.CHEST)
                .define('#', Items.STICK)
                .define('$', Items.IRON_INGOT)
                .pattern("   ")
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.CHEST))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.FISHING_BARGE.get())
                .define('#', Items.FISHING_ROD)
                .define('$', Items.IRON_INGOT)
                .pattern("   ")
                .pattern("###")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.FISHING_ROD))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModItems.CHUNK_LOADER_BARGE.get())
                .define('_', Items.ENDER_EYE)
                .define('#', Items.OBSIDIAN)
                .define('$', Items.IRON_INGOT)
                .pattern("   ")
                .pattern("#_#")
                .pattern("$$$")
                .unlockedBy("has_item", has(Items.ENDER_EYE))
                .save(consumer);

    }


}
