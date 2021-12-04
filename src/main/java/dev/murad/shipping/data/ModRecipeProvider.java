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
        ShapelessRecipeBuilder.shapeless(ModItems.SHIP_LINK.get(), 2)
                .requires(Items.IRON_NUGGET)
                .unlockedBy("has_item", has(Items.IRON_NUGGET))
                .save(consumer);

        ShapedRecipeBuilder.shaped(ModBlocks.SHIP_LOCK.get())
                .define('#', ModItems.SHIP_LINK.get())
                .define('$', Items.IRON_INGOT)
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .unlockedBy("has_item", has(ModItems.SHIP_LINK.get()))
                .save(consumer);

    }
}
