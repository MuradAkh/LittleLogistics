package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.FluidDisplayUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;


public class FluidTankCarEntity extends AbstractWagonEntity {
    public static int CAPACITY = FluidType.BUCKET_VOLUME * 10;
    protected FluidTank tank = new FluidTank(CAPACITY){
        @Override
        protected void onContentsChanged(){
            sendInfoToClient();
        }
    };
    private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(FluidTankCarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(FluidTankCarEntity.class, EntityDataSerializers.STRING);
    private Fluid clientCurrFluid = Fluids.EMPTY;
    private int clientCurrAmount = 0;
    public FluidTankCarEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public FluidTankCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.FLUID_CAR.get(), level, aDouble, aDouble1, aDouble2);

    }

    @Override
    public @NotNull ItemStack getPickResult() {
        return new ItemStack(ModItems.FLUID_CAR.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID_TYPE, "minecraft:empty");
        builder.define(VOLUME, 0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand){
        InteractionResult ret = super.interact(player, hand);
        if (ret.consumesAction()) return ret;

        if(!this.level().isClientSide){
            FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, tank);
            player.displayClientMessage(FluidDisplayUtil.getFluidDisplay(tank), false);
        }
        return InteractionResult.CONSUME;
    }

    public FluidStack getFluidStack(){
        return tank.getFluid();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        tank.readFromNBT(tag);
        sendInfoToClient();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tank.writeToNBT(tag);
    }

    private void sendInfoToClient(){
        entityData.set(VOLUME, tank.getFluidAmount());
        entityData.set(FLUID_TYPE, BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).toString());
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level().isClientSide) {
            if(VOLUME.equals(key)) {
                clientCurrAmount =  entityData.get(VOLUME);
                tank.setFluid(new FluidStack(clientCurrFluid, clientCurrAmount));
            } else if (FLUID_TYPE.equals(key)) {
                ResourceLocation fluidName = ResourceLocation.parse(entityData.get(FLUID_TYPE));
                clientCurrFluid = BuiltInRegistries.FLUID.getValue(fluidName);
                tank.setFluid(new FluidStack(clientCurrFluid, clientCurrAmount));
            }
        }
    }

    public FluidTank getTank() {
        return tank;
    }
}
