package dev.murad.shipping.item;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class SpringItem extends Item {

    private TranslationTextComponent springInfo = new TranslationTextComponent("item.spring.description");

    public SpringItem(Properties properties) {
        super(properties);

//        addProperty(new ResourceLocation("first_selected"), (stack, a, b) -> getState(stack) == State.WAITING_NEXT ? 1f : 0f);
    }

    // because 'itemInteractionForEntity' is only for Living entities
    public void onUsedOnEntity(ItemStack stack, PlayerEntity player, World world, Entity target) {
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
            player.displayClientMessage(new TranslationTextComponent("item.spring.notToSelf"), true);
        } else if(dominant instanceof ISpringableEntity) {
            Train firstTrain =  ((ISpringableEntity) dominant).getTrain();
            Train secondTrain = ((ISpringableEntity) target).getTrain();
            if (firstTrain.getTug().isPresent() && secondTrain.getTug().isPresent()) {
                player.displayClientMessage(new TranslationTextComponent("item.spring.noTwoTugs"), true);
            } else if (secondTrain.equals(firstTrain)){
                player.displayClientMessage(new TranslationTextComponent("item.spring.noLoops"), true);
            } else if (firstTrain.getTug().isPresent()) {
                SpringEntity.createSpring((Entity) firstTrain.getTail(), (Entity) secondTrain.getHead());
            } else {
                SpringEntity.createSpring((Entity) secondTrain.getTail(), (Entity) firstTrain.getHead());
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
