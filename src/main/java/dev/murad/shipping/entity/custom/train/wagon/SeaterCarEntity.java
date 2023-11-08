package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.compatibility.create.CapabilityInjector;
import dev.murad.shipping.compatibility.create.CreateCompatibility;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SeaterCarEntity extends AbstractWagonEntity {
    @Nullable
    private LazyOptional<?> createCompatMinecartControllerCapability = null;

    public SeaterCarEntity(EntityType<SeaterCarEntity> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
        initCompat();
    }

    public SeaterCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.SEATER_CAR.get(), level, aDouble, aDouble1, aDouble2);
        initCompat();
    }

    private void initCompat() {
        if (CreateCompatibility.enabled()) {
            createCompatMinecartControllerCapability = CapabilityInjector.constructMinecartControllerCapability(this);
        }
    }

    @Override
    public @NotNull ItemStack getPickResult() {
        return new ItemStack(ModItems.SEATER_CAR.get());
    }

    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        InteractionResult ret = super.interact(pPlayer, pHand);
        if (ret.consumesAction()) return ret;

        if (pPlayer.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        } else if (this.isVehicle()) {
            return InteractionResult.PASS;
        } else if (!this.level().isClientSide) {
            return pPlayer.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Called every tick the minecart is on an activator rail.
     */
    public void activateMinecart(int pX, int pY, int pZ, boolean pReceivingPower) {
        if (pReceivingPower) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }

            if (this.getHurtTime() == 0) {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.setDamage(50.0F);
                this.markHurt();
            }
        }

    }

    @Override
    public void remove(RemovalReason r) {
        if(createCompatMinecartControllerCapability != null && CreateCompatibility.enabled()){
            createCompatMinecartControllerCapability.invalidate();
        }
        super.remove(r);
    }

    @Override
    protected void positionRider(@Nonnull Entity passenger, Entity.@NotNull MoveFunction pCallback) {
        if (this.hasPassenger(passenger)) {
            if (passenger instanceof Player) {
                // Position player differently than all other entities
                // TODO: Maybe we could override Entity#getPassengersRidingOffset instead
                float f = -0.22F;
                Vec3 vector3d = new Vec3(f, 0.0D, 0.0D).yRot(-this.getYRot() * ((float) Math.PI / 180F) - ((float) Math.PI / 2F));
                pCallback.accept(passenger, this.getX() + vector3d.x, this.getY(), this.getZ() + vector3d.z);
            } else {
                super.positionRider(passenger, pCallback);
            }
        }
    }

    private void clampRotation(Entity p_184454_1_) {
        p_184454_1_.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(p_184454_1_.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        p_184454_1_.yRotO += f1 - f;
        p_184454_1_.setYRot(p_184454_1_.getYRot() + f1 - f);
        p_184454_1_.setYHeadRot(p_184454_1_.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity p_184190_1_) {
        this.clampRotation(p_184190_1_);
    }

    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (CreateCompatibility.enabled() &&
                createCompatMinecartControllerCapability != null
                && CapabilityInjector.isMinecartControllerCapability(cap)
        ) {
            return createCompatMinecartControllerCapability.cast();
        }
        return super.getCapability(cap, side);
    }
}
