package dev.murad.shipping.recipe;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractRouteCopyRecipe extends CustomRecipe {
    private final Item item;
    public AbstractRouteCopyRecipe(CraftingBookCategory cat, Item matchingItem) {
        super(cat);
        this.item = matchingItem;
    }

    public abstract boolean stackHasNodes(ItemStack stack);

    @Nonnull
    @Override
    public abstract RecipeSerializer<?> getSerializer();

    /**
     * If hasNodes is set, return if stack has nodes,
     * otherwise return if stack is empty.
     */
    private boolean isRouteWithNodes(ItemStack stack, boolean hasNodes) {
        if (stack.getItem() == item) {
            return !stackHasNodes(stack) ^ hasNodes;
        }
        return false;
    }

    // returns a pair of <Filled Tug Route, Unfilled Tug Route>
    private Optional<Pair<ItemStack, Integer>> checkTugRoutes(CraftingInput inventory) {
        int i = 0;
        ItemStack filledRoute = ItemStack.EMPTY;

        for(int j = 0; j < inventory.size(); ++j) {
            ItemStack stack = inventory.getItem(j);
            if (!stack.isEmpty()) {
                if (isRouteWithNodes(stack, true)) {
                    if (!filledRoute.isEmpty()) {
                        // can't have 2 filled routes
                        return Optional.empty();
                    }

                    filledRoute = stack;
                } else {
                    if (!isRouteWithNodes(stack, false)) {
                        return Optional.empty();
                    }

                    ++i;
                }
            }
        }

        // if we have a filled route
        if (!filledRoute.isEmpty() && i <= filledRoute.getMaxStackSize() - 1) {
            return Optional.of(new Pair<>(filledRoute, i));
        }

        return Optional.empty();
    }

    @Override
    public boolean matches(@Nonnull CraftingInput inventory, @Nonnull Level level) {
        return checkTugRoutes(inventory).isPresent();
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput inventory, HolderLookup.@NotNull Provider registries) {
        Optional<Pair<ItemStack, Integer>> matchOpt = checkTugRoutes(inventory);
        if (matchOpt.isEmpty()) return ItemStack.EMPTY;

        Pair<ItemStack, Integer> match = matchOpt.get();
        ItemStack filled = match.getFirst();
        int num = match.getSecond();

        if (num == 0) {
            // clear! (no blanks present: consume the route and hand back a fresh blank)
            return new ItemStack(item, 1);
        } else {
            // copy: produce a single copy per craft. The source route itself is preserved
            // in the grid via getRemainingItems, and one blank is consumed per craft, so
            // one filled route + a stack of n blanks yields n copies (shift-click supported).
            ItemStack output = filled.copy();
            output.setCount(1);
            return output;
        }
    }

    @Nonnull
    @Override
    public NonNullList<ItemStack> getRemainingItems(@Nonnull CraftingInput inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
        // Only preserve the source route in copy mode; in clear mode (no blanks) it is consumed.
        boolean copyMode = checkTugRoutes(inventory).map(Pair::getSecond).orElse(0) > 0;
        if (copyMode) {
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (isRouteWithNodes(stack, true)) {
                    ItemStack keep = stack.copy();
                    keep.setCount(1);
                    remaining.set(i, keep);
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 2;
    }
}
