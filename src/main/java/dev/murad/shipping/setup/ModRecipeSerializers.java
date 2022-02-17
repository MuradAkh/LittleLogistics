package dev.murad.shipping.setup;

import dev.murad.shipping.recipe.TugRouteRecipe;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;

public class ModRecipeSerializers {

    public static final RegistryObject<SimpleRecipeSerializer<TugRouteRecipe>> TUG_ROUTE_COPY = Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SimpleRecipeSerializer<>(TugRouteRecipe::new));

    public static void register () {

    }
}
