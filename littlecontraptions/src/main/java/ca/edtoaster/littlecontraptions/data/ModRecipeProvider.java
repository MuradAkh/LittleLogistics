package ca.edtoaster.littlecontraptions.data;

import ca.edtoaster.littlecontraptions.setup.LCBlocks;
import ca.edtoaster.littlecontraptions.setup.LCItems;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, registries);
    }

    // Look up a Create item by id without depending on Create's Registrate-typed AllItems.
    private static Item createItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, LCBlocks.BARGE_ASSEMBLER.get())
                .define('L', ItemTags.LOGS)
                .define('A', createItem("andesite_alloy"))
                .define('R', Items.REDSTONE)
                .define('C', ModItems.SPRING.get())
                .pattern(" L ")
                .pattern("ARA")
                .pattern("LCL")
                .unlockedBy("has_item", has(createItem("andesite_alloy")))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, LCItems.CONTRAPTION_BARGE_ITEM.get())
                .define('B', createItem("andesite_alloy"))
                .define('A', createItem("brass_ingot"))
                .define('$', Items.IRON_INGOT)
                .pattern("ABA")
                .pattern("$$$")
                .unlockedBy("has_item", has(createItem("andesite_alloy")))
                .save(output);
    }
}
