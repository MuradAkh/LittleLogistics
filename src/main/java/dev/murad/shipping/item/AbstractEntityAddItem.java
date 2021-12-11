package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.ModBargeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;

public abstract class AbstractEntityAddItem extends Item {

    public AbstractEntityAddItem(Item.Properties p_i48526_2_) {
        super( p_i48526_2_);
    }

    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        RayTraceResult raytraceresult = getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.ANY);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.pass(itemstack);
        } else {
            Vector3d vector3d = player.getViewVector(1.0F);
            double d0 = 5.0D;
            List<Entity> list = world.getEntities(player, player.getBoundingBox().expandTowards(vector3d.scale(5.0D)).inflate(1.0D),
                    EntityPredicates.NO_SPECTATORS.and(Entity::isPickable));
            if (!list.isEmpty()) {
                Vector3d vector3d1 = player.getEyePosition(1.0F);

                for(Entity entity : list) {
                    AxisAlignedBB axisalignedbb = entity.getBoundingBox().inflate((double)entity.getPickRadius());
                    if (axisalignedbb.contains(vector3d1)) {
                        return ActionResult.pass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
                Entity entity = getEntity(world, raytraceresult);
                entity.yRot = player.yRot;
                if (!world.noCollision(entity, entity.getBoundingBox().inflate(-0.1D))) {
                    return ActionResult.fail(itemstack);
                } else {
                    if (!world.isClientSide) {
                        world.addFreshEntity(entity);
                        if (!player.abilities.instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    return ActionResult.sidedSuccess(itemstack, world.isClientSide());
                }
            } else {
                return ActionResult.pass(itemstack);
            }
        }
    }

    protected abstract Entity getEntity(World world, RayTraceResult raytraceresult);
}
