package dev.murad.shipping.item;
/*
MIT License

Copyright (c) 2018 Xavier "jglrxavpok" Niochaut

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */


import dev.murad.shipping.entity.custom.SpringEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
