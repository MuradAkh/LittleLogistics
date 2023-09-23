package dev.murad.shipping.setup;

import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.recipe.AbstractRouteCopyRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

public class ModRecipeSerializers {
    public static final RegistryObject<SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> TUG_ROUTE_COPY = Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SimpleCraftingRecipeSerializer<>((loc, cat) -> new AbstractRouteCopyRecipe(loc, cat, ModItems.TUG_ROUTE.get()) {
                @Override
                public boolean stackHasNodes(ItemStack stack) {
                    return !TugRouteItem.getRoute(stack).isEmpty();
                }

                @Nonnull
                @Override
                public RecipeSerializer<?> getSerializer() {
                    return TUG_ROUTE_COPY.get();
                }
            }));

    public static final RegistryObject<SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> LOCO_ROUTE_COPY = Registration.RECIPE_SERIALIZERS.register(
            "loco_route_copy", () -> new SimpleCraftingRecipeSerializer<>((loc, cat) -> new AbstractRouteCopyRecipe(loc, cat, ModItems.LOCO_ROUTE.get()) {
                @Override
                public boolean stackHasNodes(ItemStack stack) {
                    return !LocoRouteItem.getRoute(stack).isEmpty();
                }

                @Nonnull
                @Override
                public RecipeSerializer<?> getSerializer() {
                    return LOCO_ROUTE_COPY.get();
                }
            }));

    public static void register () {

    }
}
