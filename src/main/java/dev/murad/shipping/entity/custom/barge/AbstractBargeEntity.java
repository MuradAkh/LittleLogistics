package dev.murad.shipping.entity.custom.barge;


import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractBargeEntity extends BoatEntity implements ISpringableEntity {
    private Optional<Pair<ISpringableEntity, SpringEntity>> dominated = Optional.empty();
    private Optional<Pair<ISpringableEntity, SpringEntity>> dominant = Optional.empty();
    private Train train;

    public AbstractBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
        this.train = new Train(this);
    }

    public AbstractBargeEntity(EntityType<? extends BoatEntity> type, World worldIn, double x, double y, double z) {
        this(type, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void addPassenger(Entity passenger){

    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }


    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (!this.level.isClientSide) {
            doInteract(player);
            return ActionResultType.PASS;
        }
        return ActionResultType.SUCCESS;
    }

    abstract protected void doInteract(PlayerEntity player);

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        if (this.isInvulnerableTo(p_70097_1_)) {
            return false;
        } else if (!this.level.isClientSide && !this.removed) {
            this.spawnAtLocation(this.getDropItem());
            this.remove();
            return true;
        } else {
            return true;
        }
    }

    public boolean hasWaterOnSides(){
        return this.level.getBlockState(this.getOnPos().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.WATER) &&
                this.level.getBlockState(this.getOnPos().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.WATER) &&
                this.level.getBlockState(this.getOnPos().above().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.AIR) &&
                this.level.getBlockState(this.getOnPos().above().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.AIR);
    }

    @Override
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated() {
        return this.dominated;
    }

    @Override
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominant() {
        return this.dominant;
    }

    @Override
    public void setDominated(ISpringableEntity entity, SpringEntity spring) {
        this.dominated = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void setDominant(ISpringableEntity entity, SpringEntity spring) {
        this.setTrain(entity.getTrain());
        this.dominant = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void removeDominated() {
        this.dominated = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {
        this.dominant = Optional.empty();
        this.setTrain(new Train(this));
    }

    @Override
    public Train getTrain() {
        return train;
    }

    @Override
    public void setTrain(Train train) {
        this.train = train;
        train.setTail(this);
        dominated.ifPresent(dominated -> {
            // avoid recursion loops
            if(!dominated.getFirst().getTrain().equals(train)){
                dominated.getFirst().setTrain(train);
            }
        });
    }

    @Override
    public void remove(){
        handleSpringableKill();
        super.remove();
    }

    // hack to disable hoppers
    public boolean isDockable() {
        return this.dominant.map(dom -> this.distanceToSqr((Entity) dom.getFirst()) < 1.1).orElse(true);
    }
}
