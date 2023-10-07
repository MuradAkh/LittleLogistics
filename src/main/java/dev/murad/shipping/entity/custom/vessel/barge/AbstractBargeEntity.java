package dev.murad.shipping.entity.custom.vessel.barge;


import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractBargeEntity extends VesselEntity {
    public AbstractBargeEntity(EntityType<? extends AbstractBargeEntity> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
        linkingHandler.train = new Train<>(this);
    }

    public AbstractBargeEntity(EntityType<? extends AbstractBargeEntity> type, Level worldIn, double x, double y, double z) {
        this(type, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }


    public abstract Item getDropItem();


    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            doInteract(player);
        }
        // don't interact *and* use current item
        return InteractionResult.CONSUME;
    }

    abstract protected void doInteract(Player player);

    public boolean hasWaterOnSides(){
        return super.hasWaterOnSides();
    }

    @Override
    public void setDominated(VesselEntity entity) {
        linkingHandler.follower = Optional.of(entity);
    }

    @Override
    public void setDominant(VesselEntity entity) {
        this.setTrain(entity.getTrain());
        linkingHandler.leader = Optional.of(entity);
    }

    @Override
    public void removeDominated() {
        if(!this.isAlive()){
            return;
        }
        linkingHandler.follower = Optional.empty();
        linkingHandler.train.setTail(this);
    }

    @Override
    public void removeDominant() {
        if(!this.isAlive()){
            return;
        }
        linkingHandler.leader = Optional.empty();
        this.setTrain(new Train(this));
    }

    @Override
    public void setTrain(Train<VesselEntity> train) {
        linkingHandler.train = train;
        train.setTail(this);
        linkingHandler.follower.ifPresent(dominated -> {
            // avoid recursion loops
            if(!dominated.getTrain().equals(train)){
                dominated.setTrain(train);
            }
        });
    }

    @Override
    public void remove(RemovalReason r){
        if (!this.level().isClientSide) {
            this.spawnAtLocation(this.getDropItem());
        }
        super.remove(r);
    }

    // hack to disable hoppers
    public boolean isDockable() {
        return this.linkingHandler.leader.map(dom -> this.distanceToSqr(dom) < 1.1).orElse(true);
    }

    public boolean allowDockInterface(){
        return isDockable();
    }

    private final StallingCapability capability = new StallingCapability() {
        @Override
        public boolean isDocked() {
            return delegate().map(StallingCapability::isDocked).orElse(false);
        }

        @Override
        public void dock(double x, double y, double z) {
        }

        @Override
        public void undock() {
        }

        @Override
        public boolean isStalled() {
            return delegate().map(StallingCapability::isStalled).orElse(false);
        }

        @Override
        public void stall() {
            delegate().ifPresent(StallingCapability::stall);
        }

        @Override
        public void unstall() {
            delegate().ifPresent(StallingCapability::unstall);
        }

        @Override
        public boolean isFrozen() {
            return AbstractBargeEntity.super.isFrozen();
        }

        @Override
        public void freeze() {
            AbstractBargeEntity.super.setFrozen(true);
        }

        @Override
        public void unfreeze() {
            AbstractBargeEntity.super.setFrozen(false);
        }

        private Optional<StallingCapability> delegate() {
            if (linkingHandler.train.getHead() instanceof AbstractTugEntity e) {
                return e.getCapability(StallingCapability.STALLING_CAPABILITY).resolve();
            }
            return Optional.empty();
        }
    };

    private final LazyOptional<StallingCapability> capabilityOpt = LazyOptional.of(() -> capability);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == StallingCapability.STALLING_CAPABILITY) {
            return capabilityOpt.cast();
        }
        return super.getCapability(cap, side);
    }
}
