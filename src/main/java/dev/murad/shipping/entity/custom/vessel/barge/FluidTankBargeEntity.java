package dev.murad.shipping.entity.custom.vessel.barge;

import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.FluidDisplayUtil;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidTankBargeEntity extends AbstractBargeEntity{
    public static int CAPACITY = FluidType.BUCKET_VOLUME * 10;
    protected FluidTank tank = new FluidTank(CAPACITY){
        @Override
        protected void onContentsChanged(){
            sendInfoToClient();
        }
    };
    private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);
    private Fluid clientCurrFluid = Fluids.EMPTY;
    private int clientCurrAmount = 0;


    private final LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);


    public FluidTankBargeEntity(EntityType<? extends AbstractBargeEntity> type, Level world) {
        super(type, world);
    }

    public FluidTankBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.FLUID_TANK_BARGE.get(), worldIn, x, y, z);
    }

    @Override
    public Item getDropItem() {
        return ModItems.FLUID_BARGE.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLUID_TYPE, "minecraft:empty");
        entityData.define(VOLUME, 0);
    }

    @Override
    protected void doInteract(Player player) {
        FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, tank);
        player.displayClientMessage(FluidDisplayUtil.getFluidDisplay(tank), false);
    }

    public FluidStack getFluidStack(){
        return tank.getFluid();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        tank.readFromNBT(tag);
        sendInfoToClient();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tank.writeToNBT(tag);
    }

    private void sendInfoToClient(){
        entityData.set(VOLUME, tank.getFluidAmount());
        entityData.set(FLUID_TYPE,ForgeRegistries.FLUIDS.getKey(tank.getFluid().getFluid()).toString());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(VOLUME.equals(key)) {
                clientCurrAmount =  entityData.get(VOLUME);
                tank.setFluid(new FluidStack(clientCurrFluid, clientCurrAmount));
            } else if (FLUID_TYPE.equals(key)) {
                ResourceLocation fluidName = new ResourceLocation(entityData.get(FLUID_TYPE));
                clientCurrFluid = ForgeRegistries.FLUIDS.getValue(fluidName);
                tank.setFluid(new FluidStack(clientCurrFluid, clientCurrAmount));
            }
        }
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
    {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return holder.cast();
        return super.getCapability(capability, facing);
    }
}
