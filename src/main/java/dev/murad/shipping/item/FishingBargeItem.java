package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.barge.FishingBargeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public class FishingBargeItem extends AbstractEntityAddItem {

    public FishingBargeItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }

    @Override
    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new FishingBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslatableComponent("item.littlelogistics.fishing_barge.description")   );
    }
}
