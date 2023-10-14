package dev.murad.shipping.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class VesselItem extends Item {

    @FunctionalInterface
    public interface AddEntityFunction {
        Entity apply(Level level, double x, double y, double z);
    }

    private final AddEntityFunction addEntity;

    public VesselItem(Properties props, AddEntityFunction addEntity) {
        super(props);
        this.addEntity = addEntity;
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockHitResult raytraceresult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.ANY);
        if (raytraceresult.getType() == BlockHitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            Vec3 vector3d = player.getViewVector(1.0F);
            List<Entity> list = world.getEntities(player, player.getBoundingBox().expandTowards(vector3d.scale(5.0D)).inflate(1.0D),
                    EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
            if (!list.isEmpty()) {
                Vec3 vector3d1 = player.getEyePosition(1.0F);

                for(Entity entity : list) {
                    AABB axisalignedbb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (axisalignedbb.contains(vector3d1)) {
                        return InteractionResultHolder.pass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == BlockHitResult.Type.BLOCK) {
                Entity entity = getEntity(world, itemstack, raytraceresult);
                entity.setYRot(player.getYRot());
                if (!world.noCollision(entity, entity.getBoundingBox().inflate(-0.1D))) {
                    return InteractionResultHolder.fail(itemstack);
                } else {
                    if (!world.isClientSide) {
                        world.addFreshEntity(entity);
                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
                }
            } else {
                return InteractionResultHolder.pass(itemstack);
            }
        }
    }

    protected Entity getEntity(Level world, ItemStack stack, BlockHitResult raytraceresult) {
        Entity e = addEntity.apply(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
        if (stack.hasCustomHoverName()) {
            e.setCustomName(stack.getHoverName());
        }
        return e;
    }
}
