package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.entity.container.SteamTugContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SteamTugEntity extends AbstractTugEntity {
    public SteamTugEntity(EntityType<? extends WaterMobEntity> type, World world) {
        super(type, world);
    }



    public SteamTugEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.STEAM_TUG.get(), worldIn, x, y, z);
    }

    public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.6D);

    }

    @Override
    protected int getNonRouteItemSlots() {
        return 1; // 1 extra slot for fuel
    }

    @Override
    protected INamedContainerProvider createContainerProvider() {
        return new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("screen.shipping.tug");
            }

            @Nullable
            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new SteamTugContainer(i, level, getId(), playerInventory, playerEntity);
            }
        };
    }

    @Override
    protected boolean isTugSlotItemValid(int slot, @Nonnull ItemStack stack){
        return slot == 1 && FurnaceTileEntity.isFuel(stack);
    }

    @Override
    protected int getTugSlotLimit(int slot){
        return slot == 1 ? 64 : 0;
    }

    @Override
    protected boolean tickFuel() {
        if (burnTime > 0) {
            burnTime--;
            return true;
        } else {
            ItemStack stack = itemHandler.getStackInSlot(1);
            if (!stack.isEmpty()) {
                burnCapacity = ForgeHooks.getBurnTime(stack, null) - 1;
                burnTime = burnCapacity - 1;
                stack.shrink(1);
                return true;
            } else {
                burnCapacity = 0;
                burnTime = 0;
                return false;
            }
        }
    }

    // Have to implement IInventory to work with hoppers



    @Override
    public boolean isEmpty() {
        return itemHandler.getStackInSlot(1).isEmpty();
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        if (!this.itemHandler.isItemValid(1, p_70299_2_)){
            return;
        }
        this.itemHandler.insertItem(1, p_70299_2_, false);
        if (!p_70299_2_.isEmpty() && p_70299_2_.getCount() > this.getMaxStackSize()) {
            p_70299_2_.setCount(this.getMaxStackSize());
        }
    }

}
