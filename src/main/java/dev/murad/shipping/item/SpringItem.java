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


import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.custom.tug.TugDummyHitboxEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.world.item.Item.Properties;

public class SpringItem extends Item {

    private TranslatableComponent springInfo = new TranslatableComponent("item.littlelogistics.spring.description");

    public SpringItem(Properties properties) {
        super(properties);

//        addProperty(new ResourceLocation("first_selected"), (stack, a, b) -> getState(stack) == State.WAITING_NEXT ? 1f : 0f);
    }

    // because 'itemInteractionForEntity' is only for Living entities
    public void onUsedOnEntity(ItemStack stack, Player player, Level world, Entity target) {
        if(target instanceof TugDummyHitboxEntity){
            target = ((TugDummyHitboxEntity) target).getTug();
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

    private void createSpringHelper(ItemStack stack, PlayerEntity player, World world, Entity target) {
        Entity dominant = getDominant(world, stack);
        if(dominant == null)
            return;
        if(dominant == target) {
            player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.spring.notToSelf"), true);
        } else if(dominant instanceof ISpringableEntity) {
            Train firstTrain =  ((ISpringableEntity) dominant).getTrain();
            Train secondTrain = ((ISpringableEntity) target).getTrain();
            if (dominant.distanceTo(target) > 15){
                player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.spring.tooFar"), true);
            } else if (firstTrain.getTug().isPresent() && secondTrain.getTug().isPresent()) {
                player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.spring.noTwoTugs"), true);
            } else if (secondTrain.equals(firstTrain)){
                player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.spring.noLoops"), true);
            } else if (firstTrain.getTug().isPresent()) {
                SpringEntity.createSpring((VesselEntity) firstTrain.getTail(), (VesselEntity) secondTrain.getHead());
            } else {
                SpringEntity.createSpring((VesselEntity) secondTrain.getTail(), (VesselEntity) firstTrain.getHead());
            }
            // First entity clicked is the dominant
            if(!player.isCreative())
                stack.shrink(1);


        }
        resetLinked(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(springInfo);
    }

    private void setDominant(World worldIn, ItemStack stack, Entity entity) {
        nbt(stack).putInt("linked", entity.getId());
    }

    private Entity getDominant(World worldIn, ItemStack stack) {
        int id = nbt(stack).getInt("linked");
        return worldIn.getEntity(id);
    }

    private static CompoundNBT nbt(ItemStack stack)  {
        if(stack.getTag() == null) {
            stack.setTag(new CompoundNBT());
        }
        return stack.getTag();
    }

    private void resetLinked(ItemStack itemstack) {
        nbt(itemstack).remove("linked");
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        resetLinked(playerIn.getItemInHand(handIn));
        return super.use(worldIn, playerIn, handIn);
    }

    public static State getState(ItemStack stack) {
        if(nbt(stack).contains("linked"))
            return State.WAITING_NEXT;
        return State.READY;
    }



    public enum State {
        WAITING_NEXT,
        READY
    }
}
