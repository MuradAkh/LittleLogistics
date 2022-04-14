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


import dev.murad.shipping.entity.custom.tug.VehicleFrontPart;
import dev.murad.shipping.util.LinkableEntity;
import lombok.extern.log4j.Log4j2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

@Log4j2
public class SpringItem extends Item {

    private TranslatableComponent springInfo = new TranslatableComponent("item.littlelogistics.spring.description");

    public SpringItem(Properties properties) {
        super(properties);

//        addProperty(new ResourceLocation("first_selected"), (stack, a, b) -> getState(stack) == State.WAITING_NEXT ? 1f : 0f);
    }

    // because 'itemInteractionForEntity' is only for Living entities
    public void onUsedOnEntity(ItemStack stack, Player player, Level world, Entity target) {
        if(target instanceof VehicleFrontPart){
            target = ((VehicleFrontPart) target).getParent();
        }
        if(world.isClientSide)
            return;
        switch(getState(stack)) {
            case WAITING_NEXT: {
                createSpringHelper(stack, player, world, target);
            }
            break;

            default: {
                setDominant(world, stack, target);
            }
            break;
        }
    }

    private void createSpringHelper(ItemStack stack, Player player, Level world, Entity target) {
        Entity dominant = getDominant(world, stack);
        if(dominant == null)
            return;
        if(dominant == target) {
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.notToSelf"), true);
        } else if(dominant instanceof LinkableEntity d) {
            if(d.linkEntities(player, target) && !player.isCreative())
                stack.shrink(1);
        }
        resetLinked(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(springInfo);
    }

    private void setDominant(Level worldIn, ItemStack stack, Entity entity) {
        stack.getOrCreateTag().putInt("linked", entity.getId());
    }

    @Nullable
    private Entity getDominant(Level worldIn, ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("linked")) {
            int id = stack.getTag().getInt("linked");
            return worldIn.getEntity(id);
        }
        resetLinked(stack);
        return null;
    }

    private void resetLinked(ItemStack itemstack) {
        itemstack.removeTagKey("linked");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        resetLinked(playerIn.getItemInHand(handIn));
        return super.use(worldIn, playerIn, handIn);
    }

    public static State getState(ItemStack stack) {
        if(stack.getTag() != null && stack.getTag().contains("linked"))
            return State.WAITING_NEXT;
        return State.READY;
    }



    public enum State {
        WAITING_NEXT,
        READY
    }
}
