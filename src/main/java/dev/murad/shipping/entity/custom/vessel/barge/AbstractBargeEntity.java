package dev.murad.shipping.entity.custom.vessel.barge;


import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.vessel.SpringEntity;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.network.protocol.Packet;
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
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractBargeEntity extends VesselEntity {
    public AbstractBargeEntity(EntityType<? extends AbstractBargeEntity> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
        this.train = new Train(this);
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
        if (!this.level.isClientSide) {
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
        this.dominated = Optional.of(entity);
    }

    @Override
    public void setDominatedSpring(SpringEntity spring) {
        this.dominatedS = Optional.of(spring);
    }

    @Override
    public void setDominant(VesselEntity entity) {
        this.setTrain(entity.getTrain());
        this.dominant = Optional.of(entity);
    }

    @Override
    public void setDominantSpring(SpringEntity spring) {
        this.dominantS = Optional.of(spring);
    }

    @Override
    public void removeDominated() {
        if(!this.isAlive()){
            return;
        }
        this.dominated = Optional.empty();
        this.dominatedS = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {
        if(!this.isAlive()){
            return;
        }
        this.dominant = Optional.empty();
        this.dominantS = Optional.empty();
        this.setTrain(new Train(this));
    }

    @Override
    public void setTrain(Train train) {
        this.train = train;
        train.setTail(this);
        dominated.ifPresent(dominated -> {
            // avoid recursion loops
            if(!dominated.getTrain().equals(train)){
                dominated.setTrain(train);
            }
        });
    }

    @Override
    public void remove(RemovalReason r){
        if (!this.level.isClientSide) {
            this.spawnAtLocation(this.getDropItem());
        }
        handleLinkableKill();
        super.remove(r);
    }

    // hack to disable hoppers
    public boolean isDockable() {
        return this.dominant.map(dom -> this.distanceToSqr((Entity) dom) < 1.1).orElse(true);
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
            delegate().ifPresent(s -> s.dock(x, y, z));
        }

        @Override
        public void undock() {
            delegate().ifPresent(StallingCapability::undock);
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
            if (train.getHead() instanceof AbstractTugEntity e) {
                return e.getCapability(StallingCapability.STALLING_CAPABILITY).resolve();
            }
            return Optional.empty();
        }
    };

    private final LazyOptional<StallingCapability> capabilityOpt = LazyOptional.of(() -> capability);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if (cap == StallingCapability.STALLING_CAPABILITY) {
            return capabilityOpt.cast();
        }
        return super.getCapability(cap);
    }
}
