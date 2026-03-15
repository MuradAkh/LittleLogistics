package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public abstract class AbstractWagonEntity extends AbstractTrainCarEntity implements StallingCapability {

    public AbstractWagonEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public AbstractWagonEntity(EntityType<?> p_38087_, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(p_38087_, level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public void setDominated(AbstractTrainCarEntity entity) {
        linkingHandler.follower = Optional.of(entity);
    }


    @Override
    public void setDominant(AbstractTrainCarEntity entity) {
        this.setTrain(entity.getTrain());
        linkingHandler.leader = Optional.of(entity);
    }

    @Override
    public void tick() {
        if(this.isFrozen() || linkingHandler.train.getTug().map(s -> (AbstractLocomotiveEntity) s).map(AbstractLocomotiveEntity::shouldFreezeTrain).orElse(false)){
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            super.tick();
        }
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
        this.setTrain(new Train<>(this));
    }

    @Override
    public void setTrain(Train<AbstractTrainCarEntity> train) {
        linkingHandler.train = train;
        train.setTail(this);
        linkingHandler.follower.ifPresent(dominated -> {
            // avoid recursion loops
            if(!dominated.getTrain().equals(train)){
                dominated.setTrain(train);
            }
        });
    }

    // hack to disable hoppers
    public boolean isDockable() {
        return linkingHandler.leader.map(dom -> this.distanceToSqr(dom) < 1.05).orElse(true);
    }


    public boolean allowDockInterface(){
        return isDockable();
    }

    private Optional<StallingCapability> delegateStalling() {
        var head = linkingHandler.train.getHead();
        if (head != this && head instanceof StallingCapability s) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    @Override
    public boolean isDocked() {
        return delegateStalling().map(StallingCapability::isDocked).orElse(false);
    }

    @Override
    public void dock(double x, double y, double z) {
        delegateStalling().ifPresent(s -> s.dock(x, y, z));
    }

    @Override
    public void undock() {
        delegateStalling().ifPresent(StallingCapability::undock);
    }

    @Override
    public boolean isStalled() {
        return delegateStalling().map(StallingCapability::isStalled).orElse(false);
    }

    @Override
    public void stall() {
        delegateStalling().ifPresent(StallingCapability::stall);
    }

    @Override
    public void unstall() {
        delegateStalling().ifPresent(StallingCapability::unstall);
    }

    @Override
    public boolean isFrozen() {
        return super.isFrozen();
    }

    @Override
    public void freeze() {
        setFrozen(true);
    }

    @Override
    public void unfreeze() {
        setFrozen(false);
    }
}
