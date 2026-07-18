package dev.murad.shipping.item;

import com.mojang.datafixers.util.Function4;
import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

// taken from Minecart item
public class TrainCarItem extends Item {
    private static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

        /**
         * Dispense the specified stack, play the dispense sound and spawn particles.
         */
        public ItemStack execute(BlockSource p_42949_, ItemStack p_42950_) {
            Direction direction = p_42949_.state().getValue(DispenserBlock.FACING);
            Level level = p_42949_.level();
            Vec3 vec3 = p_42949_.center();
            double d0 = vec3.x() + (double)direction.getStepX() * 1.125D;
            double d1 = Math.floor(vec3.y()) + (double)direction.getStepY();
            double d2 = vec3.z() + (double)direction.getStepZ() * 1.125D;
            BlockPos blockpos = p_42949_.pos().relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            RailShape railshape = getPlacementShape(blockstate, level, blockpos,
                    direction.getAxis().isHorizontal() ? direction : null);
            double d3;
            if (blockstate.is(BlockTags.RAILS)) {
                if (railshape.isAscending()) {
                    d3 = 0.6D;
                } else {
                    d3 = 0.1D;
                }
            } else {
                if (!blockstate.isAir() || !level.getBlockState(blockpos.below()).is(BlockTags.RAILS)) {
                    return this.defaultDispenseItemBehavior.dispense(p_42949_, p_42950_);
                }

                BlockState blockstate1 = level.getBlockState(blockpos.below());
                RailShape railshape1 = getPlacementShape(blockstate1, level, blockpos.below(),
                        direction.getAxis().isHorizontal() ? direction : null);
                if (direction != Direction.DOWN && railshape1.isAscending()) {
                    d3 = -0.4D;
                } else {
                    d3 = -0.9D;
                }
            }

            AbstractMinecart abstractminecart = ((TrainCarItem)p_42950_.getItem()).constructor.apply(level, d0, d1 + d3, d2);
            initializeDirection(abstractminecart, direction, railshape);
            if (p_42950_.has(DataComponents.CUSTOM_NAME)) {
                abstractminecart.setCustomName(p_42950_.getHoverName());
            }

            level.addFreshEntity(abstractminecart);
            p_42950_.shrink(1);
            return p_42950_;
        }

        /**
         * Play the dispense sound from the specified block.
         */
        protected void playSound(BlockSource p_42947_) {
            p_42947_.level().levelEvent(1000, p_42947_.pos(), 0);
        }
    };
    final Function4<Level, Double, Double, Double, AbstractTrainCarEntity> constructor;

    public TrainCarItem(Function4<Level, Double, Double, Double, AbstractTrainCarEntity> constructor, Item.Properties pProperties) {
        super(pProperties);
        this.constructor = constructor;
        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    /**
     * Called when this item is used when targetting a Block
     */
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (!blockstate.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemstack = pContext.getItemInHand();
            if (!level.isClientSide) {
                Direction preferredDirection = pContext.getPlayer() == null
                        ? null
                        : pContext.getPlayer().getDirection().getOpposite();
                RailShape railshape = getPlacementShape(
                        blockstate, level, blockpos, preferredDirection);
                double d0 = 0.0D;
                if (railshape.isAscending()) {
                    d0 = 0.5D;
                }

                AbstractMinecart abstractminecart = constructor.apply(level, (double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.0625D + d0, (double)blockpos.getZ() + 0.5D);
                initializeDirection(abstractminecart, preferredDirection, railshape);
                if (itemstack.has(DataComponents.CUSTOM_NAME)) {
                    abstractminecart.setCustomName(itemstack.getHoverName());
                }


                if(pContext.getPlayer() != null && abstractminecart.getDirection().equals(pContext.getPlayer().getDirection())){
                    if(abstractminecart instanceof AbstractLocomotiveEntity l)
                        l.flip();
                }

                if (!level.addFreshEntity(abstractminecart)) {
                    return InteractionResult.FAIL;
                }
                if (pContext.getPlayer() != null && abstractminecart instanceof AbstractLocomotiveEntity locomotive) {
                    locomotive.setOwner(pContext.getPlayer().getUUID());
                }
                level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
            }

            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    private static RailShape getPlacementShape(BlockState state, Level level, BlockPos pos,
                                               Direction preferredDirection) {
        if (!(state.getBlock() instanceof BaseRailBlock rail)) return RailShape.NORTH_SOUTH;
        if (preferredDirection != null && state.getBlock() instanceof MultiShapeRail multiShapeRail) {
            return multiShapeRail.getVanillaRailShapeFromDirection(
                    state, pos, level, preferredDirection);
        }
        return rail.getRailDirection(state, level, pos, null);
    }

    private static void initializeDirection(AbstractMinecart minecart, Direction preferredDirection,
                                            RailShape shape) {
        if (!(minecart instanceof AbstractTrainCarEntity trainCar)) return;

        var exits = RailHelper.EXITS_DIRECTION.get(shape);
        if (exits == null) return;
        Direction direction = preferredDirection != null
                && (exits.getFirst().horizontal == preferredDirection
                    || exits.getSecond().horizontal == preferredDirection)
                ? preferredDirection
                : exits.getFirst().horizontal;
        trainCar.initializeRailTravelDirection(direction);
        trainCar.setYRot(direction.toYRot());
    }
}
