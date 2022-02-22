package dev.murad.shipping.entity.custom.train;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.RailUtils;
import dev.murad.shipping.util.Train;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TrainCar extends AbstractMinecart implements IForgeAbstractMinecart, LinkableEntity {
    protected Optional<TrainCar> dominant = Optional.empty();
    protected Optional<TrainCar> dominated = Optional.empty();
    public static final EntityDataAccessor<Integer> DOMINANT_ID = SynchedEntityData.defineId(TrainCar.class, EntityDataSerializers.INT);
    protected Train train;
    private boolean flipped = false;
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;


    public TrainCar(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
        train = new Train(this);
    }


    public TrainCar(EntityType<?> p_38087_, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(p_38087_, level, aDouble, aDouble1, aDouble2);
        var pos = new BlockPos(aDouble, aDouble1, aDouble2);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            RailShape railshape = (railBlock).getRailDirection(state, this.level, pos, this);
            var exit = RailUtils.EXITS.get(railshape).getFirst();
            Optional.ofNullable(Direction.fromNormal(exit.getX(), exit.getY(), exit.getZ()))
                    .map(Direction::toYRot)
                    .ifPresent(this::setYRot);
        }

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DOMINANT_ID, -1);
    }


    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (level.isClientSide) {
            if (DOMINANT_ID.equals(key)) {
                fetchDominantClient();
            }
        }
    }

    private void fetchDominantClient() {
        Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
        if (potential instanceof TrainCar t) {
            dominant = Optional.of(t);
        } else {
            dominant = Optional.empty();
        }
    }

    public void tick() {
        tickMinecart();

        tickAdjustments();
    }

    protected void tickAdjustments() {
        if (!this.level.isClientSide()) {
            doChainMath();
        }
        var dir = this.getDeltaMovement().normalize();
        Optional.ofNullable(Direction.fromNormal((int) dir.x, (int) dir.y, (int) dir.z))
                .map(Direction::toYRot).ifPresent(this::setYRot);

        if (this.level.isClientSide) {
            fetchDominantClient();
        } else {
            entityData.set(DOMINANT_ID, dominant.map(Entity::getId).orElse(-1));
        }
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
                double d2 = Mth.wrapDegrees(this.lyr - (double) this.getYRot());
//                this.setYRot(this.getYRot() + (float) d2 / (float) this.lSteps);
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
            double d1 = this.xo - this.getX();
            double d3 = this.zo - this.getZ();
            if (d1 * d1 + d3 * d3 > 0.001D) {
//                this.setYRot((float) (Mth.atan2(d3, d1) * 180.0D / Math.PI));
                if (this.flipped) {
//                    this.setYRot(this.getYRot() + 180.0F);
                }
            }

            double d4 = (double) Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (d4 < -170.0D || d4 >= 170.0D) {
//                this.setYRot(this.getYRot() + 180.0F);
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
        this.lyr = (double) pYaw;
        this.lxr = (double) pPitch;
        this.lSteps = pPosRotationIncrements + 2;
        super.lerpTo(pX, pY, pZ, pYaw, pPitch, pPosRotationIncrements, pTeleport);
    }

    @Override
    public void remove(RemovalReason r) {
        handleLinkableKill();
        super.remove(r);
    }

    private void doChainMath() {
        dominant.ifPresent(trainCar -> {
            var distance = trainCar.distanceTo(this);
            if (distance <= 5) {
                Vec3 direction = trainCar.position().subtract(position()).normalize();

                // TODO: conditional on docking like with vessels
                if (distance > 1.05) {
                    Vec3 parentVelocity = trainCar.getDeltaMovement();

                    if (parentVelocity.length() == 0) {
                        setDeltaMovement(direction.scale(0.05));
                    } else {
                        setDeltaMovement(direction.scale(parentVelocity.length()));
                        setDeltaMovement(getDeltaMovement().scale(distance));
                    }
                } else if (distance < 0.8)
                    setDeltaMovement(direction.scale(-0.05));
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
        if (target instanceof TrainCar t) {
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
