package dev.murad.shipping.data;

import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.data.*;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(DataGenerator p_i48262_1_) {
        super(p_i48262_1_);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(ModBlocks.TUG_DOCK.get())
                .define('#', ModItems.SPRING.get())
                .define('$', Items.IRON_INGOT)
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SPRING.get()))
                .save(consumer);

    }
}
