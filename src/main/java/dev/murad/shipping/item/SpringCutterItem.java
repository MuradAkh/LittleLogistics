package dev.murad.shipping.item;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.SpringEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.world.World;

public class SpringCutterItem extends Item {

    public SpringCutterItem(Properties properties) {
        super(properties);
    }

    // because 'itemInteractionForEntity' is only for Living entities
    public void onUsedOnEntity(ItemStack stack, PlayerEntity player, World world, Entity target) {
        if(world.isClientSide) {
            long count = SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINANT, target).count()
                    + SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINATED, target).count();
            if(count > 0) {
                world.playLocalSound(target.getX(), target.getY(), target.getZ(), SoundEvents.SHEEP_SHEAR, SoundCategory.PLAYERS, 10f, 1f, false);
            }
            return;
        }
        world.getServer().executeBlocking(new TickDelayedTask(0, new Runnable() {
            @Override
            public void run() {
                long count = SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINANT, target).count()
                        + SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINATED, target).count();
                if(count > 0) {
                    if(!player.isCreative())
                        stack.hurtAndBreak(1, player, item -> {});

                    SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINANT, target).forEach(SpringEntity::kill);
                    SpringEntity.streamSpringsAttachedTo(SpringEntity.SpringSide.DOMINATED, target).forEach(SpringEntity::kill);
                }
            }
        }));

    }
}
