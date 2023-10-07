package dev.murad.shipping.entity.custom.vessel.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.accessor.SteamHeadVehicleDataAccessor;
import dev.murad.shipping.entity.container.SteamHeadVehicleContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import dev.murad.shipping.util.FuelItemStackHandler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SteamTugEntity extends AbstractTugEntity {
    private final ForgeConfigSpec.ConfigValue<Double> FURNACE_FUEL_MULTIPLIER = ShippingConfig.Server.STEAM_TUG_FUEL_MULTIPLIER;
    private final FuelItemStackHandler fuelItemHandler = new FuelItemStackHandler();
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> fuelItemHandler);
    protected int burnTime = 0;
    protected int burnCapacity = 0;

    public SteamTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
    }

    public SteamTugEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.STEAM_TUG.get(), worldIn, x, y, z);
    }

    @Override
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.littlelogistics.tug");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player player) {
                return new SteamHeadVehicleContainer<SteamTugEntity>(i, level(), getDataAccessor(), playerInventory, player);
            }
        };
    }

    public int getBurnProgress() {
        int i = burnCapacity;
        if (i == 0) {
            i = 200;
        }

        return burnTime * 13 / i;
    }

    // CONTAINER STUFF
    public boolean isLit() {
        return burnTime > 0;
    }

    @Override
    public SteamHeadVehicleDataAccessor getDataAccessor() {
        return (SteamHeadVehicleDataAccessor) new SteamHeadVehicleDataAccessor.Builder()
                .withBurnProgress(this::getBurnProgress)
                .withId(this.getId())
                .withLit(this::isLit)
                .withVisitedSize(() -> nextStop)
                .withOn(() -> engineOn)
                .withRouteSize(() -> path != null ? path.size() : 0)
                .withCanMove(enrollmentHandler::mayMove)
                .build();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    protected boolean tickFuel() {
        if (burnTime > 0) {
            burnTime--;
            return true;
        } else {
            int burnTime = fuelItemHandler.tryConsumeFuel();
            int adjustedBurnTime = (int) Math.ceil(burnTime * FURNACE_FUEL_MULTIPLIER.get());
            this.burnCapacity = adjustedBurnTime;
            this.burnTime = adjustedBurnTime;
            return adjustedBurnTime > 0;
        }
    }

    public Item getDropItem() {
        return ModItems.STEAM_TUG.get();
    }


    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        burnTime = compound.contains("burn") ? compound.getInt("burn") : 0;
        burnCapacity = compound.contains("burn_capacity") ? compound.getInt("burn_capacity") : 0;
        fuelItemHandler.deserializeNBT(compound.getCompound("fuelItems"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putInt("burn", burnTime);
        compound.putInt("burn_capacity", burnCapacity);
        compound.put("fuelItems", fuelItemHandler.serializeNBT());
        super.addAdditionalSaveData(compound);
    }

    @Override
    protected void onUndock() {
        super.onUndock();
        this.playSound(ModSounds.STEAM_TUG_WHISTLE.get(), 1, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    // Have to implement IInventory to work with hoppers
    @Override
    public boolean isEmpty() {
        return fuelItemHandler.getStackInSlot(0).isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int p_70301_1_) {
        return fuelItemHandler.getStackInSlot(p_70301_1_);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (!this.fuelItemHandler.isItemValid(slot, stack)){
            return;
        }
        this.fuelItemHandler.insertItem(slot, stack, false);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

}
