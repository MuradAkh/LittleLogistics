package dev.murad.shipping.entity.custom;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TrainCar extends AbstractMinecart implements IForgeAbstractMinecart, LinkableEntity {
    protected Optional<TrainCar> dominant = Optional.empty();
    protected Optional<TrainCar> dominated = Optional.empty();
    protected Train train;
    public TrainCar(EntityType<?> p_38087_, Level p_38088_) {
        super(ModEntityTypes.TRAIN_CAR.get(), p_38088_);
        train = new Train(this);
    }

    public TrainCar(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.TRAIN_CAR.get(), level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public void tick(){
        super.tick();
        if(!this.level.isClientSide()){
            doChainMath();
        }
        this.setYRot(this.getDirection().toYRot());
    }

    @Override
    public void remove(RemovalReason r){
        handleLinkableKill();
        super.remove(r);
    }

    private void doChainMath(){
        dominant.ifPresent(trainCar -> {
            var distance = trainCar.distanceTo(this);
            if(distance <= 7) {
                Vec3 direction = trainCar.position().subtract(position()).normalize();

                if(distance > 1.1) {
                    Vec3 parentVelocity = trainCar.getDeltaMovement();

                    if(parentVelocity.length() == 0) {
                        setDeltaMovement(direction.scale(0.05));
                    }
                    else {
                        setDeltaMovement(direction.scale(parentVelocity.length()));
                        setDeltaMovement(getDeltaMovement().scale(distance));
                    }
                }
                else if(distance < 0.8)
                    setDeltaMovement(direction.scale(-0.05));
                else
                    setDeltaMovement(Vec3.ZERO);
            }
        });
    }

    @Override
    public Type getMinecartType() {
        // Why does this even exist
        return Type.CHEST;
    }

    @Override
    public Optional<LinkableEntity> getDominated() {
        return dominated.map(t -> t);
    }

    @Override
    public Optional<LinkableEntity> getDominant() {
        return dominant.map(t -> t);
    }

    @Override
    public void setDominated(LinkableEntity entity) {
        dominated = Optional.of((TrainCar) entity);
    }

    @Override
    public void setDominant(LinkableEntity entity) {
        dominant = Optional.of((TrainCar) entity);
    }

    @Override
    public void removeDominated() {
        dominated = Optional.empty();
    }

    @Override
    public void removeDominant() {
        dominant = Optional.empty();
    }

    @Override
    public void handleShearsCut() {

    }

    @Override
    public Train getTrain() {
        return train;
    }

    @Override
    public boolean linkEntities(Player player, Entity target) {
        if(target instanceof TrainCar t){
            t.setDominant(this);
            this.setDominated(t);
            return true;
        } else {
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.badTypes"), true);
            return false;
        }
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
    public boolean hasWaterOnSides() {
        return false;
    }
}
