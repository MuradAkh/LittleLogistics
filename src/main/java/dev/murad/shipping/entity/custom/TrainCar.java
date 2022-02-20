package dev.murad.shipping.entity.custom;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;

import java.util.Optional;

public class TrainCar extends AbstractMinecart implements IForgeAbstractMinecart, LinkableEntity {

    public TrainCar(EntityType<?> p_38087_, Level p_38088_) {
        super(ModEntityTypes.TRAIN_CAR.get(), p_38088_);
    }

    public TrainCar(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.TRAIN_CAR.get(), level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public Type getMinecartType() {
        // Why does this even exist
        return Type.CHEST;
    }

    @Override
    public Optional<Pair<LinkableEntity, SpringEntity>> getDominated() {
        return Optional.empty();
    }

    @Override
    public Optional<Pair<LinkableEntity, SpringEntity>> getDominant() {
        return Optional.empty();
    }

    @Override
    public void setDominated(LinkableEntity entity, SpringEntity spring) {

    }

    @Override
    public void setDominant(LinkableEntity entity, SpringEntity spring) {

    }

    @Override
    public void removeDominated() {

    }

    @Override
    public void removeDominant() {

    }

    @Override
    public Train getTrain() {
        return null;
    }

    @Override
    public boolean linkEntities(Player player, Entity target) {
        return false;
    }

    @Override
    public void setTrain(Train train) {

    }

    @Override
    public boolean hasWaterOnSides() {
        return false;
    }
}
