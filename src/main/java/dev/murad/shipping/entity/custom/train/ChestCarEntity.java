package dev.murad.shipping.entity.custom.train;

import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.Train;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class ChestCarEntity extends AbstractTrainCar {
    public ChestCarEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public ChestCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.CHEST_CAR.get(), level, aDouble, aDouble1, aDouble2);

    }

    @Override
    public void setDominated(AbstractTrainCar entity) {
        this.dominated = Optional.of(entity);
    }


    @Override
    public void setDominant(AbstractTrainCar entity) {
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

}
