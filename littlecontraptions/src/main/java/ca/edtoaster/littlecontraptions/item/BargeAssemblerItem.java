package ca.edtoaster.littlecontraptions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link BlockItem} that can be placed on top of a body of water. Vanilla block placement
 * ray-traces with {@link ClipContext.Fluid#NONE}, so the ray passes straight through water and
 * the block ends up on the floor below. This item instead ray-traces stopping at water sources,
 * so aiming at a pond places the assembler on the water surface rather than under it.
 */
public class BargeAssemblerItem extends BlockItem {

    public BargeAssemblerItem(Block block, Properties props) {
        super(block, props);
    }

    /**
     * Ray-trace from the player's point of view that stops at the surface of a water source,
     * i.e. does not pass through the water. Returns the hit on the water's top face, or
     * {@code null} if the player is not looking at a water surface within reach.
     */
    @Nullable
    public static BlockHitResult getWaterSurfaceTarget(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return null;
        }
        FluidState fluid = level.getFluidState(hit.getBlockPos());
        if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
            return null;
        }
        return hit;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = tryPlaceOnWater(level, player, hand);
        if (result != null) {
            return new InteractionResultHolder<>(result, player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            InteractionResult result = tryPlaceOnWater(context.getLevel(), player, context.getHand());
            if (result != null) {
                return result;
            }
        }
        return super.useOn(context);
    }

    /**
     * @return the placement result if the player is aiming at a water surface, or {@code null}
     * to fall back to normal block placement.
     */
    @Nullable
    private InteractionResult tryPlaceOnWater(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getWaterSurfaceTarget(level, player);
        if (hit == null) {
            return null;
        }
        // Place in the block above the water surface (sitting on top of the water) rather than
        // replacing the surface water block. Aiming the hit at the air block above makes
        // BlockPlaceContext resolve the placement position there.
        BlockPos placePos = hit.getBlockPos().above();
        BlockHitResult raised = new BlockHitResult(hit.getLocation(), hit.getDirection(), placePos, hit.isInside());
        return this.place(new BlockPlaceContext(player, hand, player.getItemInHand(hand), raised));
    }
}
