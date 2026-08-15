package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.accessor.SteamHeadVehicleDataAccessor;
import dev.murad.shipping.entity.container.SteamHeadVehicleContainer;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import dev.murad.shipping.util.FuelItemStackHandler;
import dev.murad.shipping.util.ItemHandlerVanillaContainerWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;


import javax.annotation.Nullable;

public class SteamLocomotiveEntity extends AbstractLocomotiveEntity implements ItemHandlerVanillaContainerWrapper, WorldlyContainer {
    private final FuelItemStackHandler fuelItemHandler = new FuelItemStackHandler();

    // This has to remain as ConfigValue as the class isn't reloaded when changing worlds
    private static final ModConfigSpec.ConfigValue<Double> FURNACE_FUEL_MULTIPLIER = ShippingConfig.Server.STEAM_LOCO_FUEL_MULTIPLIER;

    // How many ticks left on this fuel
    protected int burnTime = 0;

    // Max number of ticks for this fuel
    protected int burnCapacity = 0;

    public boolean isLit() {
        return burnTime > 0;
    }

    public int getBurnProgress() {
        int i = burnCapacity;
        if (i == 0) {
            i = 200;
        }

        return burnTime * 13 / i;
    }


    @Override
    public SteamHeadVehicleDataAccessor getDataAccessor() {
        return (SteamHeadVehicleDataAccessor) new SteamHeadVehicleDataAccessor.Builder()
                .withBurnProgress(this::getBurnProgress)
                .withBurnTime(() -> burnTime / 20)
                .withId(this.getId())
                .withOn(() -> engineOn)
                .withRouteSize(() -> navigator.getRouteSize())
                .withVisitedSize(() -> navigator.getVisitedSize())
                .withLit(this::isLit)
                .withCanMove(ownership::mayMove)
                .build();
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

    @Override
    protected void onUndock() {
        super.onUndock();
        this.playSound(ModSounds.STEAM_TUG_WHISTLE.get(), 1, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("entity.littlelogistics.steam_locomotive");
            }

            @Override
            public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player player) {
                return new SteamHeadVehicleContainer<>(i, level(), getDataAccessor(), playerInventory, player);
            }
        };
    }

    @Override
    public void remove(RemovalReason r) {
        if(!this.level().isClientSide && r != RemovalReason.UNLOADED_TO_CHUNK){
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(r);
    }

    public FuelItemStackHandler getFuelItemHandler() {
        return fuelItemHandler;
    }

    public SteamLocomotiveEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public SteamLocomotiveEntity(Level level, Double x, Double y, Double z) {
        super(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level, x, y, z);
    }


    @Override
    public @NotNull ItemStack getPickResult() {
        return new ItemStack(ModItems.STEAM_LOCOMOTIVE.get());
    }


    protected void doMovementEffect() {
        Level level = this.level();
        BlockPos blockpos = this.getOnPos().above().above();
        RandomSource random = level.random;
        if (random.nextFloat() < ShippingConfig.Client.LOCO_SMOKE_MODIFIER.get()) {
            for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                AbstractTugEntity.makeParticles(level, blockpos, this);
            }
        }
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @NotNull ItemStack itemStack, @NotNull Direction dir) {
        return false;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction dir) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack itemStack, @Nullable Direction dir) {
        return isDocked();
    }

    @Override
    public ItemStackHandler getRawHandler() {
        return fuelItemHandler;
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        fuelItemHandler.deserializeNBT(this.registryAccess(), compound.getCompound("fuelItems"));
        burnTime = compound.contains("burn") ? compound.getInt("burn") : 0;
        burnCapacity = compound.contains("burn_capacity") ? compound.getInt("burn_capacity") : 0;
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.put("fuelItems", fuelItemHandler.serializeNBT(this.registryAccess()));
        compound.putInt("burn", burnTime);
        compound.putInt("burn_capacity", burnCapacity);
        super.addAdditionalSaveData(compound);
    }
}
