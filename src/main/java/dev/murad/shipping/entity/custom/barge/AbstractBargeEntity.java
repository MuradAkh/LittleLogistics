package dev.murad.shipping.entity.custom.barge;


import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.util.Train;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractBargeEntity extends VesselEntity implements LinkableEntity {
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


    @Nonnull
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        if (this.isInvulnerableTo(p_70097_1_)) {
            return false;
        } else if (!this.level.isClientSide && !this.dead) {
            this.spawnAtLocation(this.getDropItem());
            this.remove(RemovalReason.KILLED);
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
    public void setDominated(LinkableEntity entity, SpringEntity spring) {
        this.dominated = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void setDominant(LinkableEntity entity, SpringEntity spring) {
        this.setTrain(entity.getTrain());
        this.dominant = Optional.of(new Pair<>(entity, spring));
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
        this.setTrain(new Train(this));
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
    public void remove(RemovalReason r){
        handleSpringableKill();
        super.remove(r);
    }

    // hack to disable hoppers
    public boolean isDockable() {
        return this.dominant.map(dom -> this.distanceToSqr((Entity) dom.getFirst()) < 1.1).orElse(true);
    }

    public boolean allowDockInterface(){
        return isDockable();
    }
}
