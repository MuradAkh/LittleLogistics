package dev.murad.shipping.entity.custom.train.locomotive;

import com.mojang.datafixers.types.Func;
import dev.murad.shipping.block.rail.blockentity.LocomotiveDockTileEntity;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.setup.ModSounds;
import dev.murad.shipping.util.ItemHandlerVanillaContainerWrapper;
import dev.murad.shipping.util.LinkableEntityHead;
import dev.murad.shipping.util.Train;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractLocomotiveEntity extends AbstractTrainCarEntity implements LinkableEntityHead<AbstractTrainCarEntity>, ItemHandlerVanillaContainerWrapper {
    protected boolean engineOn = false;
    @Setter
    private boolean doflip = false;
    private boolean independentMotion = false;
    private boolean docked = false;


    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private int dockCheckCooldown = 0;


    public AbstractLocomotiveEntity(EntityType<?> type, Level p_38088_) {
        super(type, p_38088_);
    }

    public AbstractLocomotiveEntity(EntityType<?> type, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(type, level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public boolean allowDockInterface(){
        return docked;
    }


    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if(!pHand.equals(InteractionHand.MAIN_HAND)){
            return InteractionResult.PASS;
        }
        if(!this.level.isClientSide){
            if(pPlayer.isCrouching()){
                engineOn = !engineOn;
            }else{
                NetworkHooks.openGui((ServerPlayer) pPlayer, createContainerProvider(), getDataAccessor()::write);
            }
        }

        return InteractionResult.CONSUME;
    }

    protected abstract MenuProvider createContainerProvider();

    public abstract DataAccessor getDataAccessor();

    protected abstract boolean tickFuel();

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(INDEPENDENT_MOTION.equals(key)) {
                independentMotion = entityData.get(INDEPENDENT_MOTION);
            }
        }
    }


    @Override
    public void tick(){
        super.tickLoad();
        super.tickMinecart();

        if(!this.level.isClientSide){
            prevent180();

        }
        tickYRot();
        if(!this.level.isClientSide){
            tickDockCheck();
            tickMovement();
            enforceMaxVelocity(0.2);
        }


        if(doflip && dominated.isEmpty()){
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(getDirection().getOpposite().toYRot());
            doflip = false;
        }

        if(this.level.isClientSide
                && independentMotion){
            doMovementEffect();
        }

    }

    protected void doMovementEffect() {

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(INDEPENDENT_MOTION, false);
    }

    private void tickMovement() {
        if(!docked && engineOn && tickFuel()) {
            entityData.set(INDEPENDENT_MOTION, true);
            accelerate();
        }else{
            entityData.set(INDEPENDENT_MOTION, false);
            setDeltaMovement(Vec3.ZERO);
        }
    }


    protected void onDock() {
        this.playSound(ModSounds.TUG_DOCKING.get(), 0.6f, 1.0f);
    }

    protected void onUndock() {
        this.playSound(ModSounds.TUG_UNDOCKING.get(), 0.6f, 1.5f);
    }

    private void tickDockCheck() {
        getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(cap -> {
            int x = (int) Math.floor(this.getX());
            int y = (int) Math.floor(this.getY());
            int z = (int) Math.floor(this.getZ());

            boolean docked = cap.isDocked();

            if (docked && dockCheckCooldown > 0){
                dockCheckCooldown--;
                this.setDeltaMovement(Vec3.ZERO);
                this.moveTo(x + 0.5 ,getY(),z + 0.5);
                return;
            }

            Function<Double, Double> prepCord =
                    (Double d) -> Math.abs(d - d.intValue());
            Predicate<Double> aroundCentre =
                    (var i) -> prepCord.apply(i) < 0.65 && prepCord.apply(i) > 0.35;

            if(!aroundCentre.test(this.getX()) || !aroundCentre.test(this.getZ())){
                return;
            }


            // Check docks
            boolean shouldDock = Optional.ofNullable(level.getBlockEntity(getOnPos().above()))
                                    .filter(entity -> entity instanceof LocomotiveDockTileEntity)
                                    .map(entity -> (LocomotiveDockTileEntity) entity)
                                    .map(dock -> dock.hold(this, getDirection()))
                                    .orElse(false);

            boolean changedDock = !docked && shouldDock;
            boolean changedUndock = docked && !shouldDock;

            if(shouldDock) {
                dockCheckCooldown = 20; // todo: magic number
                cap.dock(x + 0.5 ,getY(),z + 0.5);
            } else {
                dockCheckCooldown = 0;
                cap.undock();
            }

            if (changedDock) onDock();
            if (changedUndock) onUndock();
        });

    }


    private double getSpeedModifier(){
        // adjust speed based on slope etc.
        var state = this.level.getBlockState(this.getOnPos().above());
        if (state.is(Blocks.POWERED_RAIL)){
            if(!state.getValue(PoweredRailBlock.POWERED)){
                return 0;
            } else {
                return 0.005;
            }
        }
        return getRailShape().map(shape -> switch (shape) {
            case NORTH_SOUTH, EAST_WEST -> 0.07;
            case SOUTH_WEST, NORTH_WEST, SOUTH_EAST, NORTH_EAST -> 0.03;
            default -> 0.07; //TODO lower if descending
        }).orElse(0d);
    }

    private void accelerate() {
        var dir = this.getDirection();
        if(Math.abs(this.getDeltaMovement().x) < 0.12 && Math.abs(this.getDeltaMovement().z) < 0.12){
            var mod = this.getSpeedModifier();
            this.push(dir.getStepX() * mod, 0, dir.getStepZ() * mod);
        }
    }

    @Override
    public void setDominated(AbstractTrainCarEntity entity) {
        dominated = Optional.of(entity);
    }

    @Override
    public void setDominant(AbstractTrainCarEntity entity) {
    }

    @Override
    public void removeDominated() {
        dominated = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {

    }


    @Override
    public void setTrain(Train train) {
        this.train = train;
    }


    private final StallingCapability stalling = new StallingCapability() {
        @Override
        public boolean isDocked() {
            return docked;
        }

        @Override
        public void dock(double x, double y, double z) {
            docked = true;
            setDeltaMovement(Vec3.ZERO);
            moveTo(x, y, z);
        }

        @Override
        public void undock() {
            docked = false;
        }

        @Override
        public boolean isStalled() {
            return engineOn;
        }

        @Override
        public void stall() {
            engineOn = true;
        }

        @Override
        public void unstall() {
            engineOn = false;
        }

        @Override
        public boolean isFrozen() {
            return AbstractLocomotiveEntity.super.isFrozen();
        }

        @Override
        public void freeze() {
            setFrozen(true);
        }

        @Override
        public void unfreeze() {
            setFrozen(false);
        }
    };

    // cache for best performance
    private final LazyOptional<StallingCapability> stallingOpt = LazyOptional.of(() -> stalling);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if (cap == StallingCapability.STALLING_CAPABILITY) {
            return stallingOpt.cast();
        }
        return super.getCapability(cap);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if(compound.contains("eo")) {
            engineOn = compound.getBoolean("eo");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("eo", engineOn);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(this.distanceToSqr(pPlayer) > 64.0D);
        }
    }
}
