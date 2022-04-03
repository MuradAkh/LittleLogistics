package dev.murad.shipping.setup;

import dev.murad.shipping.recipe.RouteCopyRecipe;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializers {

    public static final RegistryObject<SimpleRecipeSerializer<RouteCopyRecipe>> TUG_ROUTE_COPY = Registration.RECIPE_SERIALIZERS.register(
            "tug_route_copy", () -> new SimpleRecipeSerializer<>(RouteCopyRecipe::new));

    public static void register () {

    }
}
