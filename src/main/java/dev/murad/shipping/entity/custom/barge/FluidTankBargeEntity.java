package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidTankBargeEntity extends AbstractBargeEntity{
    public static int CAPACITY = FluidAttributes.BUCKET_VOLUME * 10;
    protected FluidTank tank = new FluidTank(CAPACITY);
    private static final DataParameter<Integer> VOLUME = EntityDataManager.defineId(AbstractTugEntity.class, DataSerializers.INT);
    private static final DataParameter<String> FLUID_TYPE = EntityDataManager.defineId(AbstractTugEntity.class, DataSerializers.STRING);
    private Fluid clientCurrFluid = Fluids.EMPTY;
    private int clientCurrAmount = 0;


    private final LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);


    public FluidTankBargeEntity(EntityType<? extends AbstractBargeEntity> type, World world) {
        super(type, world);
    }

    public FluidTankBargeEntity(World worldIn, double x, double y, double z) {
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
    protected void doInteract(PlayerEntity player) {
        FluidUtil.interactWithFluidHandler(player, Hand.MAIN_HAND, tank);
        player.displayClientMessage(new StringTextComponent(tank.getFluidAmount() + "/" + tank.getCapacity() + " ml"), false);
        sendInfoToClient();
    }

    public FluidStack getFluidStack(){
        return tank.getFluid();
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT tag)
    {
        super.readAdditionalSaveData(tag);
        tank.readFromNBT(tag);
        sendInfoToClient();
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT tag)
    {
        super.addAdditionalSaveData(tag);
        tank.writeToNBT(tag);
    }

    private void sendInfoToClient(){
        entityData.set(VOLUME, tank.getFluidAmount());
        entityData.set(FLUID_TYPE, tank.getFluid().getFluid().getRegistryName().toString());
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
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
