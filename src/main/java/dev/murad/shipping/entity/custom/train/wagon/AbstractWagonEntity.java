package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractWagonEntity extends AbstractTrainCarEntity {

    public AbstractWagonEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public AbstractWagonEntity(EntityType<?> p_38087_, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(p_38087_, level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public void setDominated(AbstractTrainCarEntity entity) {
        this.dominated = Optional.of(entity);
    }


    @Override
    public void setDominant(AbstractTrainCarEntity entity) {
        this.setTrain(entity.getTrain());
        this.dominant = Optional.of(entity);
    }

    @Override
    public void removeDominated() {
        if(!this.isAlive()){
            return;
        }
        this.dominated = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {
        if(!this.isAlive()){
            return;
        }
        this.dominant = Optional.empty();
        this.setTrain(new Train<>(this));
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

    // hack to disable hoppers
    public boolean isDockable() {
        return this.dominant.map(dom -> this.distanceToSqr(dom) < 1.05).orElse(true);
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
            return AbstractWagonEntity.super.isFrozen();
        }

        @Override
        public void freeze() {
            AbstractWagonEntity.super.setFrozen(true);
        }

        @Override
        public void unfreeze() {
            AbstractWagonEntity.super.setFrozen(false);
        }

        private Optional<StallingCapability> delegate() {
            if (train.getHead() instanceof AbstractLocomotiveEntity e) {
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
