package dev.murad.shipping.recipe;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModRecipeSerializers;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Optional;

public class TugRouteRecipe extends CustomRecipe {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteRecipe.class);
    public TugRouteRecipe(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    private boolean isTugRouteWithTag(ItemStack stack, boolean hasTag) {
        if (stack.getItem() == ModItems.TUG_ROUTE.get()) {
            return (stack.getTag() == null) ^ hasTag;
        }
        return false;
    }

    // returns a pair of <Filled Tug Route, Unfilled Tug Route>
    private Optional<Pair<ItemStack, Integer>> checkTugRoutes(CraftingInventory inventory) {
        int i = 0;
        ItemStack filledRoute = ItemStack.EMPTY;

        for(int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack stack = inventory.getItem(j);
            if (!stack.isEmpty()) {
                if (isTugRouteWithTag(stack, true)) {
                    if (!filledRoute.isEmpty()) {
                        // can't have 2 filled routes
                        return Optional.empty();
                    }

                    filledRoute = stack;
                } else {
                    if (!isTugRouteWithTag(stack, false)) {
                        return Optional.empty();
                    }

                    ++i;
                }
            }
        }

        // if we have a filled route, and 1 unfilled route
        if (!filledRoute.isEmpty() && i >= 1 && i <= filledRoute.getMaxStackSize() - 1) {
            return Optional.of(new Pair<>(filledRoute, i));
        }

        return Optional.empty();
    }

    @Override
    public boolean matches(@Nonnull CraftingInventory inventory, @Nonnull World level) {
        return checkTugRoutes(inventory).isPresent();
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInventory inventory) {
        Optional<Pair<ItemStack, Integer>> matchOpt = checkTugRoutes(inventory);
        if (!matchOpt.isPresent()) return ItemStack.EMPTY;

        Pair<ItemStack, Integer> match = matchOpt.get();
        ItemStack filled = match.getFirst();
        int num = match.getSecond();

        ItemStack output = filled.copy();
        output.setCount(num + 1);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 2;
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.TUG_ROUTE_COPY.get();
    }
}
