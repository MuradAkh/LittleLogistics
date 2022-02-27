package dev.murad.shipping.entity.custom.train;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.RailUtils;
import dev.murad.shipping.util.Train;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class AbstractTrainCar extends AbstractMinecart implements IForgeAbstractMinecart, LinkableEntity<AbstractTrainCar> {
    protected Optional<AbstractTrainCar> dominant = Optional.empty();
    protected Optional<AbstractTrainCar> dominated = Optional.empty();
    private @Nullable CompoundTag dominantNBT;
    public static final EntityDataAccessor<Integer> DOMINANT_ID = SynchedEntityData.defineId(AbstractTrainCar.class, EntityDataSerializers.INT);
    protected Train<AbstractTrainCar> train;
    private boolean flipped = false;
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lxr;

    public AbstractTrainCar(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
        train = new Train<>(this);
    }

    protected Optional<RailShape> getRailShape(){
        for(var pos: Arrays.asList(getOnPos().above(), getOnPos())) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseRailBlock railBlock) {
                return Optional.of(RailUtils.getShape(pos, this.level, Optional.of(this)));
            }
        }
        return Optional.empty();
    }


    public boolean canBeCollidedWith() {
        return false;
    }


    public AbstractTrainCar(EntityType<?> p_38087_, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(p_38087_, level, aDouble, aDouble1, aDouble2);
        var pos = new BlockPos(aDouble, aDouble1, aDouble2);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            RailShape railshape = (railBlock).getRailDirection(state, this.level, pos, this);
            var exit = RailUtils.EXITS.get(railshape).getFirst();
            this.setYRot(RailUtils.directionFromVelocity(new Vec3(exit.getX(), exit.getY(), exit.getZ())).toYRot());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        dominantNBT = compound.getCompound("dominant");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if(dominant.isPresent()) {
            writeNBT(dominant.get(), compound);
        } else if(dominantNBT != null) {
                compound.put(SpringEntity.SpringSide.DOMINANT.name(), dominantNBT);
        }
    }

    private Optional<AbstractTrainCar> tryToLoadFromNBT(CompoundTag compound) {
        try {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            pos.set(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
            String uuid = compound.getString("UUID");
            AABB searchBox = new AABB(
                    pos.getX() - 2,
                    pos.getY() - 2,
                    pos.getZ() - 2,
                    pos.getX() + 2,
                    pos.getY() + 2,
                    pos.getZ() + 2
            );
            List<Entity> entities = level.getEntities(this, searchBox, e -> e.getStringUUID().equals(uuid));
            return entities.stream().findFirst().map(e -> (AbstractTrainCar) e);
        } catch (Exception e){
            return Optional.empty();
        }
    }

    private void writeNBT(Entity entity, CompoundTag globalCompound) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", (int)Math.floor(entity.getX()));
        compound.putInt("Y", (int)Math.floor(entity.getY()));
        compound.putInt("Z", (int)Math.floor(entity.getZ()));

        compound.putString("UUID", entity.getUUID().toString());

        globalCompound.put("dominant", compound);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DOMINANT_ID, -1);
    }


    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (level.isClientSide) {
            if (DOMINANT_ID.equals(key)) {
                fetchDominantClient();
            }
        }
    }

    private void fetchDominantClient() {
        Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
        if (potential instanceof AbstractTrainCar t) {
            dominant = Optional.of(t);
        } else {
            dominant = Optional.empty();
        }
    }

    public void tick() {
        tickLoad();
        tickMinecart();
        tickAdjustments();
    }

    protected void tickLoad(){
        if (this.level.isClientSide) {
            fetchDominantClient();
        } else {
            if(dominant.isEmpty() && dominantNBT != null){
                dominant = tryToLoadFromNBT(dominantNBT);
                dominant.ifPresent(d -> d.setDominated(this));
            }
            entityData.set(DOMINANT_ID, dominant.map(Entity::getId).orElse(-1));
        }
    }

    protected void tickAdjustments() {
        this.setYRot(RailUtils.directionFromVelocity(this.getDeltaMovement()).toYRot());

        if (!this.level.isClientSide()) {
            doChainMath();
        }
    }

    @Override
    public Direction getMotionDirection() {
        return this.getDirection();
    }

    protected void tickMinecart() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkOutOfWorld();
        this.handleNetherPortal();
        if (this.level.isClientSide) {
            if (this.lSteps > 0) {
                double d5 = this.getX() + (this.lx - this.getX()) / (double) this.lSteps;
                double d6 = this.getY() + (this.ly - this.getY()) / (double) this.lSteps;
                double d7 = this.getZ() + (this.lz - this.getZ()) / (double) this.lSteps;
                this.setXRot(this.getXRot() + (float) (this.lxr - (double) this.getXRot()) / (float) this.lSteps);
                --this.lSteps;
                this.setPos(d5, d6, d7);
                this.setRot(this.getYRot(), this.getXRot());
            } else {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
            }

        } else {
            if (!this.isNoGravity()) {
                double d0 = this.isInWater() ? -0.005D : -0.04D;
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, d0, 0.0D));
            }

            int k = Mth.floor(this.getX());
            int i = Mth.floor(this.getY());
            int j = Mth.floor(this.getZ());
            if (this.level.getBlockState(new BlockPos(k, i - 1, j)).is(BlockTags.RAILS)) {
                --i;
            }

            BlockPos blockpos = new BlockPos(k, i, j);
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (canUseRail() && BaseRailBlock.isRail(blockstate)) {
                this.moveAlongTrack(blockpos, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock) blockstate.getBlock()).isActivatorRail()) {
                    this.activateMinecart(k, i, j, blockstate.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.comeOffTrack();
            }

            this.checkInsideBlocks();
            this.setXRot(0.0F);

            double d4 = (double) Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (d4 < -170.0D || d4 >= 170.0D) {
                this.flipped = !this.flipped;
            }

            this.setRot(this.getYRot(), this.getXRot());
            AABB box;
            if (getCollisionHandler() != null) box = getCollisionHandler().getMinecartCollisionBox(this);
            else box = this.getBoundingBox().inflate(0.2F, 0.0D, 0.2F);
            if (canBeRidden() && this.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
                List<Entity> list = this.level.getEntities(this, box, EntitySelector.pushableBy(this));
                if (!list.isEmpty()) {
                    for (int l = 0; l < list.size(); ++l) {
                        Entity entity1 = list.get(l);
                        if (!(entity1 instanceof Player) && !(entity1 instanceof IronGolem) && !(entity1 instanceof AbstractMinecart) && !this.isVehicle() && !entity1.isPassenger()) {
                            entity1.startRiding(this);
                        } else {
                            entity1.push(this);
                        }
                    }
                }
            } else {
                for (Entity entity : this.level.getEntities(this, box)) {
                    if (!this.hasPassenger(entity) && entity.isPushable() && entity instanceof AbstractMinecart) {
                        entity.push(this);
                    }
                }
            }

            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements, boolean pTeleport) {
        this.lx = pX;
        this.ly = pY;
        this.lz = pZ;
        this.lxr = (double) pPitch;
        this.lSteps = pPosRotationIncrements + 2;
        super.lerpTo(pX, pY, pZ, pYaw, pPitch, pPosRotationIncrements, pTeleport);
    }

    @Override
    public void remove(RemovalReason r) {
        handleLinkableKill();
        super.remove(r);
    }

    protected void prevent180() {
        var dir = new Vec3(this.getDirection().getStepX(), this.getDirection().getStepY(), this.getDirection().getStepZ());
        var vel = this.getDeltaMovement();
        var mag = vel.multiply(dir);
        var fixer = new Vec3(fixUtil(mag.x), 1, fixUtil(mag.z));
        this.setDeltaMovement(this.getDeltaMovement().multiply(fixer));
    }

    private double fixUtil(double mag) {
        return mag < 0 ? 0 : 1;
    }



    private void doChainMath() {
        dominant.ifPresent(dom -> {
            var railDirDis = RailUtils.getRail(dom.getOnPos().above(), level).flatMap(target ->
                    RailUtils.traverseBi(this.getOnPos().above(), level, (level, p) -> p.equals(target), 20));

            float distance = railDirDis.map(Pair::getSecond).filter(a -> a > 1).map(di -> {
                var euclid = this.distanceTo(dom);
                return euclid < 1 ? di : euclid;
            }).orElse(this.distanceTo(dom));

            if (distance <= 5) {
                Vec3 euclideanDir = dom.position().subtract(position()).normalize();
                var dir = railDirDis
                        .map(Pair::getFirst)
                        .map(Direction::getNormal)
                        .map(Vec3::atLowerCornerOf)
                        .orElse(euclideanDir);


                // TODO: conditional on docking like with vessels
                if (distance > 1.1) {
                    Vec3 parentVelocity = dom.getDeltaMovement();

                    if (parentVelocity.length() == 0) {
                        setDeltaMovement(euclideanDir.scale(0.05));
                    } else {
                        // TODO: sharp corners are    hell
                        setDeltaMovement(dir.scale(parentVelocity.length()));
                        setDeltaMovement(getDeltaMovement().scale(distance));
                    }
                } else if (distance < 0.8){
                    setDeltaMovement(dir.scale(-0.05));
                }
                else
                    setDeltaMovement(Vec3.ZERO);
            } else {
                removeDominant();
                removeDominated();
                handleLinkableKill();
            }
        });
    }
    @Override
    public Type getMinecartType() {
        // Why does this even exist
        return Type.CHEST;
    }

    @Override
    public Optional<AbstractTrainCar> getDominated() {
        return dominated;
    }

    @Override
    public Optional<AbstractTrainCar> getDominant() {
        return dominant;
    }

    @Override
    public void setDominated(AbstractTrainCar entity) {
        dominated = Optional.of(entity);
    }

    @Override
    public void setDominant(AbstractTrainCar entity) {
        dominant = Optional.of(entity);
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
        this.dominant.ifPresent(LinkableEntity::removeDominated);
        removeDominant();
    }

    @Override
    public Train<AbstractTrainCar> getTrain() {
        return train;
    }

    private void invertDoms(){
        var temp = dominant;
        dominant = dominated;
        dominated = temp;
    }

    private static Optional<Pair<AbstractTrainCar, AbstractTrainCar>> tryFindAndPrepareClosePair(Train<AbstractTrainCar> train1, Train<AbstractTrainCar> train2){
        return Optional.empty();
    }

    @Override
    public boolean linkEntities(Player player, Entity target) {
        if (target instanceof AbstractTrainCar t) {
            Train<AbstractTrainCar> train1 = t.getTrain();
            Train<AbstractTrainCar> train2 = this.getTrain();
            if(train2.getTug().isPresent() && train2.getTug().isPresent()){
                // TODO: ERROR two tug
                return false;
            } else if (train2.equals(train1)){
                // TODO: ERROR loop
                return false;
            } else {
                tryFindAndPrepareClosePair(train1, train2)
                        .ifPresentOrElse(pair -> createLinks(pair.getFirst(), pair.getSecond()), () -> {
                            // TODO: ERROR too far
                        });
            }

            createLinks(this, t);
            return true;
        } else {
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.badTypes"), true);
            return false;
        }
    }

    private void createLinks(AbstractTrainCar dominant, AbstractTrainCar dominated) {
        dominated.setDominant(this);
        dominant.setDominated(dominated);
    }

    @Override
    public void setTrain(Train train) {
        this.train = train;
        train.setTail(this);
        dominated.ifPresent(dominated -> {
            // avoid recursion loops
            if (!dominated.getTrain().equals(train)) {
                dominated.setTrain(train);
            }
        });
    }

    @Override
    public boolean hasWaterOnSides() {
        return false;
    }
}
