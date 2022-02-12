package dev.murad.shipping.setup;

import dev.murad.shipping.recipe.TugRouteRecipe;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;

public class ModRecipeSerializers {

    public static final RegistryObject<SpecialRecipeSerializer<TugRouteRecipe>> TUG_ROUTE_COPY = Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SpecialRecipeSerializer<>(TugRouteRecipe::new));

    public static void register () {

    }
}
