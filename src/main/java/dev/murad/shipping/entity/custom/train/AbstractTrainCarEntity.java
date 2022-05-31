package dev.murad.shipping.entity.custom.train;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.vessel.SpringEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.RailHelper;
import dev.murad.shipping.util.Train;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class AbstractTrainCarEntity extends AbstractMinecart implements IForgeAbstractMinecart, LinkableEntity<AbstractTrainCarEntity> {
    protected Optional<AbstractTrainCarEntity> dominant = Optional.empty();
    protected Optional<AbstractTrainCarEntity> dominated = Optional.empty();
    private @Nullable
    CompoundTag dominantNBT;
    public static final EntityDataAccessor<Integer> DOMINANT_ID = SynchedEntityData.defineId(AbstractTrainCarEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DOMINATED_ID = SynchedEntityData.defineId(AbstractTrainCarEntity.class, EntityDataSerializers.INT);
    protected Train<AbstractTrainCarEntity> train;
    protected static double TRAIN_SPEED = ShippingConfig.Server.TRAIN_MAX_SPEED.get();
    @Getter
    protected final RailHelper railHelper;
    private boolean waitForDominated;

    @Getter
    @Setter
    private boolean frozen = false;

    private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), (p_38135_) -> {
        Vec3i west = Direction.WEST.getNormal();
        Vec3i east = Direction.EAST.getNormal();
        Vec3i north = Direction.NORTH.getNormal();
        Vec3i south = Direction.SOUTH.getNormal();
        Vec3i westUnder = west.below();
        Vec3i eastUnder = east.below();
        Vec3i northUnder = north.below();
        Vec3i southUnder = south.below();
        p_38135_.put(RailShape.NORTH_SOUTH, Pair.of(north, south));
        p_38135_.put(RailShape.EAST_WEST, Pair.of(west, east));
        p_38135_.put(RailShape.ASCENDING_EAST, Pair.of(westUnder, east));
        p_38135_.put(RailShape.ASCENDING_WEST, Pair.of(west, eastUnder));
        p_38135_.put(RailShape.ASCENDING_NORTH, Pair.of(north, southUnder));
        p_38135_.put(RailShape.ASCENDING_SOUTH, Pair.of(northUnder, south));
        p_38135_.put(RailShape.SOUTH_EAST, Pair.of(south, east));
        p_38135_.put(RailShape.SOUTH_WEST, Pair.of(south, west));
        p_38135_.put(RailShape.NORTH_WEST, Pair.of(north, west));
        p_38135_.put(RailShape.NORTH_EAST, Pair.of(north, east));
    });

    private static Pair<Vec3i, Vec3i> exits(RailShape pShape) {
        return EXITS.get(pShape);
    }

    public AbstractTrainCarEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
        train = new Train<>(this);
        railHelper = new RailHelper(this);
    }

    public AbstractTrainCarEntity(EntityType<?> p_38087_, Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(p_38087_, level, aDouble, aDouble1, aDouble2);
        var pos = new BlockPos(aDouble, aDouble1, aDouble2);
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            RailShape railshape = (railBlock).getRailDirection(state, this.level, pos, this);
            var exit = RailHelper.EXITS.get(railshape).getFirst();
            this.setYRot(RailHelper.directionFromVelocity(new Vec3(exit.getX(), exit.getY(), exit.getZ())).toYRot());
        }
        train = new Train<>(this);
        railHelper = new RailHelper(this);
    }

    protected Optional<RailShape> getRailShape() {
        for (var pos : Arrays.asList(getOnPos().above(), getOnPos())) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseRailBlock railBlock) {
                return Optional.of(railHelper.getShape(pos));
            }
        }
        return Optional.empty();
    }


    public boolean canBeCollidedWith() {
        // future me: we don't want to change this, because then you can't push the cart
        return super.canBeCollidedWith();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        dominantNBT = compound.getCompound("dominant");
        waitForDominated = compound.getBoolean("hasChild");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (dominant.isPresent()) {
            writeNBT(dominant.get(), compound);
        } else if (dominantNBT != null) {
            compound.put(SpringEntity.SpringSide.DOMINANT.name(), dominantNBT);
        }

        compound.putBoolean("hasChild", dominated.isPresent());

    }

    private Optional<AbstractTrainCarEntity> tryToLoadFromNBT(CompoundTag compound) {
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
            return entities.stream().findFirst().map(e -> (AbstractTrainCarEntity) e);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void writeNBT(Entity entity, CompoundTag globalCompound) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", (int) Math.floor(entity.getX()));
        compound.putInt("Y", (int) Math.floor(entity.getY()));
        compound.putInt("Z", (int) Math.floor(entity.getZ()));

        compound.putString("UUID", entity.getUUID().toString());

        globalCompound.put("dominant", compound);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(DOMINANT_ID, -1);
        getEntityData().define(DOMINATED_ID, -1);
    }


    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (level.isClientSide) {
            if (DOMINANT_ID.equals(key) || DOMINATED_ID.equals(key)) {
                fetchDominantClient();
                fetchDominatedClient();
            }
        }
    }

    private void fetchDominantClient() {
        Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
        if (potential instanceof AbstractTrainCarEntity t) {
            dominant = Optional.of(t);
        } else {
            dominant = Optional.empty();
        }
    }

    private void fetchDominatedClient() {
        Entity potential = level.getEntity(getEntityData().get(DOMINATED_ID));
        if (potential instanceof AbstractTrainCarEntity t) {
            dominated = Optional.of(t);
        } else {
            dominated = Optional.empty();
        }
    }

    public void tick() {
        tickLoad();
        tickYRot();
        var yrot = this.getYRot();
        tickVanilla();
        this.setYRot(yrot);
        if (!level.isClientSide) {
            doChainMath();
            enforceMaxVelocity(TRAIN_SPEED);
        }
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return (float) TRAIN_SPEED;
    }

    protected void enforceMaxVelocity(double maxSpeed) {
        var vel = this.getDeltaMovement();
        var normal = vel.normalize();
        if (Math.abs(vel.x) > maxSpeed) {
            this.setDeltaMovement(normal.x * maxSpeed, vel.y, vel.z);
            vel = this.getDeltaMovement();
        }
        if (Math.abs(vel.z) > maxSpeed) {
            this.setDeltaMovement(vel.x, vel.y, normal.z * maxSpeed);
        }
    }

    @Override
    public void push(Entity pEntity) {
        if (!this.level.isClientSide) {
            // not perfect, doesn't work when a mob stand in the way without moving, but works well enough underwater to keep this
            if (pEntity instanceof LivingEntity l && l.getVehicle() == null){
                this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
            }
            if (!pEntity.noPhysics && !this.noPhysics) {
                // fix carts with passengers falling behind
                if (!this.hasPassenger(pEntity) || this.getDominant().isPresent()) {
                    double d0 = pEntity.getX() - this.getX();
                    double d1 = pEntity.getZ() - this.getZ();
                    double d2 = d0 * d0 + d1 * d1;
                    if (d2 >= (double)1.0E-4F) {
                        d2 = Math.sqrt(d2);
                        d0 /= d2;
                        d1 /= d2;
                        double d3 = 1.0D / d2;
                        if (d3 > 1.0D) {
                            d3 = 1.0D;
                        }

                        d0 *= d3;
                        d1 *= d3;
                        d0 *= (double)0.1F;
                        d1 *= (double)0.1F;
                        d0 *= 0.5D;
                        d1 *= 0.5D;
                        if (pEntity instanceof AbstractMinecart) {
                            double d4 = pEntity.getX() - this.getX();
                            double d5 = pEntity.getZ() - this.getZ();
                            Vec3 vec3 = (new Vec3(d4, 0.0D, d5)).normalize();
                            Vec3 vec31 = (new Vec3((double)Mth.cos(this.getYRot() * ((float)Math.PI / 180F)), 0.0D, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)))).normalize();
                            double d6 = Math.abs(vec3.dot(vec31));
                            if (d6 < (double)0.8F) {
                                return;
                            }

                            Vec3 vec32 = this.getDeltaMovement();
                            Vec3 vec33 = pEntity.getDeltaMovement();
                            if (((AbstractMinecart)pEntity).isPoweredCart() && !this.isPoweredCart()) {
                                this.setDeltaMovement(vec32.multiply(0.2D, 1.0D, 0.2D));
                                this.push(vec33.x - d0, 0.0D, vec33.z - d1);
                                pEntity.setDeltaMovement(vec33.multiply(0.95D, 1.0D, 0.95D));
                            } else if (!((AbstractMinecart)pEntity).isPoweredCart() && this.isPoweredCart()) {
                                pEntity.setDeltaMovement(vec33.multiply(0.2D, 1.0D, 0.2D));
                                pEntity.push(vec32.x + d0, 0.0D, vec32.z + d1);
                                this.setDeltaMovement(vec32.multiply(0.95D, 1.0D, 0.95D));
                            } else {
                                double d7 = (vec33.x + vec32.x) / 2.0D;
                                double d8 = (vec33.z + vec32.z) / 2.0D;
                                this.setDeltaMovement(vec32.multiply(0.2D, 1.0D, 0.2D));
                                this.push(d7 - d0, 0.0D, d8 - d1);
                                pEntity.setDeltaMovement(vec33.multiply(0.2D, 1.0D, 0.2D));
                                pEntity.push(d7 + d0, 0.0D, d8 + d1);
                            }
                        } else {
                            this.push(-d0, 0.0D, -d1);
                            pEntity.push(d0 / 4.0D, 0.0D, d1 / 4.0D);
                        }
                    }

                }
            }
        }
    }

    // avoid inheriting mixins
    @Override
    public BlockPos getOnPos() {
        var position = position();
        int i = Mth.floor(position.x);
        int j = Mth.floor(position.y - (double)0.2F);
        int k = Mth.floor(position.z);
        BlockPos blockpos = new BlockPos(i, j, k);
        if (this.level.isEmptyBlock(blockpos)) {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = this.level.getBlockState(blockpos1);
            if (blockstate.collisionExtendsVertically(this.level, blockpos1, this)) {
                return blockpos1;
            }
        }

        return blockpos;
    }


    protected void tickLoad() {
        if (this.level.isClientSide) {
            fetchDominantClient();
            fetchDominatedClient();
        } else {
            if (dominant.isEmpty() && dominantNBT != null) {
                tryToLoadFromNBT(dominantNBT).ifPresent(this::setDominant);
                dominant.ifPresent(d -> {
                    d.setDominated(this);
                    dominantNBT = null; // done loading
                });
            }
            if (dominated.isPresent()){
                waitForDominated = false;
                if(!((ServerLevel) this.level).isPositionEntityTicking(dominated.get().blockPosition())){
                    this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
                }
            } else if (waitForDominated) {
                this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
            }
            entityData.set(DOMINANT_ID, dominant.map(Entity::getId).orElse(-1));
            entityData.set(DOMINATED_ID, dominated.map(Entity::getId).orElse(-1));
        }
    }

    protected void tickYRot() {
        this.setYRot(computeYaw());
    }

    public float computeYaw() {
        var yrot = this.getYRot();
        // if the car is part of a train, enforce that direction instead
        Optional<RailShape> railShape = getRailShape();
        if (this.dominated.isPresent() && railShape.isPresent()) {
            Optional<Pair<Direction, Integer>> r = railHelper.traverseBi(getOnPos().above(),
                    RailHelper.samePositionPredicate(dominated.get()), 5, this);
            if (r.isPresent()) {
                Direction hordir = yawHelper(r, dominated.get());
                Optional<Vec3i> directionOpt = RailHelper.getDirectionToOtherExit(hordir, railShape.get());
                if (directionOpt.isPresent()) {
                    Vec3i direction = directionOpt.get();
                    return ((float) (Mth.atan2(direction.getZ(), direction.getX()) * 180.0D / Math.PI) + 90);
                }
            }
        } else if (this.dominant.isPresent() && railShape.isPresent()) {
            Optional<Pair<Direction, Integer>> r = railHelper.traverseBi(getOnPos().above(),
                    RailHelper.samePositionPredicate(dominant.get()), 5, this);
            if (r.isPresent()) {
                Direction hordir = yawHelper(r, dominant.get());
                Optional<Vec3i> directionOpt = RailHelper.getDirectionToOtherExit(hordir, railShape.get());
                if (directionOpt.isPresent()) {
                    Vec3i direction = directionOpt.get();
                    return ((float) (Mth.atan2(-direction.getZ(), -direction.getX()) * 180.0D / Math.PI) + 90);
                }
            }
        } else {
            double d1 = this.xo - this.getX();
            double d3 = this.zo - this.getZ();
            if (d1 * d1 + d3 * d3 > 0.001D) {
                return ((float) (Mth.atan2(d3, d1) * 180.0D / Math.PI) + 90);
            }
        }

        return yrot;

    }

    private Direction yawHelper(Optional<Pair<Direction, Integer>> r, Entity e) {
        Direction hordir = null;
        if(r.get().getSecond() == 0) {
            Vec3 dirvec = new Vec3(e.xo - this.xo,0, e.zo - this.zo);
            hordir = Direction.fromNormal((int) dirvec.normalize().x, 0, (int) dirvec.normalize().z); // may fail
        }
        // if still null
        if (hordir == null){
            hordir = r.get().getFirst();
        }
        return hordir;
    }


    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        if (ShippingConfig.Server.TRAIN_EXEMPT_DAMAGE_SOURCES.get().contains(pSource.msgId)){
            return true;
        }
        return super.isInvulnerableTo(pSource);
    }

    /**
     * This method returns the specific position on the track at
     * pOffset blocks from the current position. This overridden
     * method takes into account of the minecart's yRot, which
     * the vanilla code does not (leading to lots of flipping)
     */
    @Nullable
    public Vec3 getPosOffs(double pX, double pY, double pZ, double pOffset) {
        int i = Mth.floor(pX);
        int j = Mth.floor(pY);
        int k = Mth.floor(pZ);
        if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockstate = this.level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = ((BaseRailBlock) blockstate.getBlock()).getRailDirection(blockstate, this.level, new BlockPos(i, j, k), this);
            pY = j;
            if (railshape.isAscending()) {
                pY = j + 1;
            }

            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i exit1 = pair.getFirst();
            Vec3i exit2 = pair.getSecond();

            // check if need to swap end points to make calculation correct
            double yawX = -Math.sin(Math.toRadians(getYRot()));
            double yawZ = Math.cos(Math.toRadians(getYRot()));
            if (new Vec3(yawX, 0, yawZ).dot(new Vec3(exit2.getX() - exit1.getX(), exit2.getY() - exit1.getY(), exit2.getZ() - exit1.getZ())) <= 0) {
                Vec3i temp = exit1;
                exit1 = exit2;
                exit2 = temp;
            }

            // get direction from e1 to e2
            double xDiff = exit2.getX() - exit1.getX();
            double zDiff = exit2.getZ() - exit1.getZ();
            // normalize x and z diff
            double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            xDiff /= dist;
            zDiff /= dist;
            pX += xDiff * pOffset;
            pZ += zDiff * pOffset;
            if (exit1.getY() != 0 && Mth.floor(pX) - i == exit1.getX() && Mth.floor(pZ) - k == exit1.getZ()) {
                pY += exit1.getY();
            } else if (exit2.getY() != 0 && Mth.floor(pX) - i == exit2.getX() && Mth.floor(pZ) - k == exit2.getZ()) {
                pY += exit2.getY();
            }

            return this.getPos(pX, pY, pZ);
        } else {
            return null;
        }
    }

    // force render since we delegate rendering to the head of the train
    @Override
    public boolean shouldRender(double pX, double pY, double pZ) {
        return true;
    }

    @Override
    public Direction getMotionDirection() {
        return Direction.fromYRot((this.getYRot()));
    }

    @Override
    public void setYRot(float pYRot) {
        super.setYRot(pYRot);
    }

    protected void tickVanilla(){
        super.tick();
    }

    @Override
    public void remove(RemovalReason r) {
        handleLinkableKill();
        super.remove(r);
    }

    @Override
    public void destroy(DamageSource pSource) {
        int i = (int) Stream.of(dominant, dominated).filter(Optional::isPresent).count();
        this.remove(Entity.RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(this.getPickResult());
            for (int j = 0; j < i; j++) {
                spawnChain();
            }
        }

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
        dominant.ifPresent(parent -> {
            var railDirDis =
                    railHelper.traverseBi(this.getOnPos().above(), RailHelper.samePositionPredicate(parent), 5, this);

            // this is a fix to mitigate "bouncing" when trains start moving from a stopped position
            // todo: fix based on "docked" instead.
            boolean docked = this.getTrain().getTug().isPresent() && this.getTrain().getTug().get().getDeltaMovement().equals(Vec3.ZERO);
            double maxDist = docked ? 1 : 1.2;
            double minDist = 1.0;

            float distance = railDirDis.map(Pair::getSecond).filter(a -> a > 0).map(di -> {
                var euclid = this.distanceTo(parent);
                return euclid < maxDist ? di : euclid;
            }).orElse(this.distanceTo(parent));

            if (distance <= 6) {
                Vec3 euclideanDir = parent.position().subtract(position()).normalize();
                Vec3 parentDirection = railDirDis
                        .map(Pair::getFirst)
                        .map(Direction::getNormal)
                        .map(Vec3::atLowerCornerOf)
                        .orElse(euclideanDir)
                        .normalize();
                Vec3 parentVelocity = parent.getDeltaMovement();

                if (distance > maxDist) {
                    if (parentVelocity.length() == 0) {
                        setDeltaMovement(parentDirection.scale(0.05));
                    } else {
                        setDeltaMovement(parentDirection.scale(parentVelocity.length()));
                        if (distance > maxDist + 0.2) {
                            setDeltaMovement(getDeltaMovement().scale(distance * 0.8));
                        }
                    }
                } else if (parent.distanceTo(this) < minDist && parent.getDeltaMovement().length() < 0.01) {
                    this.moveTo(Math.floor(getX()) + 0.5, getY(), Math.floor(getZ()) + 0.5);
                    setDeltaMovement(Vec3.ZERO);
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
            } else {
                dominant.ifPresent(LinkableEntity::removeDominated);
                removeDominant();
            }
        });
    }

    @Override
    public Type getMinecartType() {
        // Why does this even exist
        return Type.CHEST;
    }

    @Override
    public Optional<AbstractTrainCarEntity> getDominated() {
        return dominated;
    }

    @Override
    public Optional<AbstractTrainCarEntity> getDominant() {
        return dominant;
    }

    private void spawnChain() {
        var stack = new ItemStack(ModItems.SPRING.get());
        this.spawnAtLocation(stack);
    }

    @Override
    public void handleShearsCut() {
        if (!this.level.isClientSide && dominant.isPresent()) {
            spawnChain();
        }
        this.dominant.ifPresent(LinkableEntity::removeDominated);
        removeDominant();
    }

    @Override
    public BlockPos getBlockPos() {
        return this.getOnPos();
    }

    @Override
    public Train<AbstractTrainCarEntity> getTrain() {
        return train;
    }

    @Override
    public boolean hasWaterOnSides() {
        return false;
    }

    private void invertDoms() {
        var temp = dominant;
        dominant = dominated;
        dominated = temp;
    }

    private Optional<Integer> distHelper(AbstractTrainCarEntity car1, AbstractTrainCarEntity car2) {
        return railHelper.traverseBi(car1.getOnPos().above(),
                (l, p) -> RailHelper.getRail(car2.getOnPos().above(), car2.level)
                        .map(rp -> rp.equals(p)).orElse(false), 5, car1).map(Pair::getSecond);
    }

    private Optional<Pair<AbstractTrainCarEntity, AbstractTrainCarEntity>> findClosestPair(Train<AbstractTrainCarEntity> train1, Train<AbstractTrainCarEntity> train2) {
        int mindistance = Integer.MAX_VALUE;
        Optional<Pair<AbstractTrainCarEntity, AbstractTrainCarEntity>> curr = Optional.empty();
        var pairs = Arrays.asList(
                Pair.of(train1.getHead(), train2.getTail()),
                Pair.of(train1.getTail(), train2.getHead()),
                Pair.of(train1.getTail(), train2.getTail()),
                Pair.of(train1.getHead(), train2.getHead())
        );
        for (var pair : pairs) {

            var d = distHelper(pair.getFirst(), pair.getSecond());
            if (d.isPresent() && d.get() < mindistance) {
                mindistance = d.get();
                curr = Optional.of(pair);
            }

        }

        return curr.filter(pair -> (!(pair.getFirst() instanceof AbstractLocomotiveEntity) || pair.getFirst().getDominated().isEmpty())
                && (!(pair.getSecond() instanceof AbstractLocomotiveEntity) || pair.getSecond().getDominated().isEmpty()));


    }

    private static Pair<AbstractTrainCarEntity, AbstractTrainCarEntity> caseTailHead(Train<AbstractTrainCarEntity> trainTail, Train<AbstractTrainCarEntity> trainHead, Pair<AbstractTrainCarEntity, AbstractTrainCarEntity> targetPair) {

        if (trainHead.getTug().isPresent()) {
            invertTrain(trainHead);
            invertTrain(trainTail);
            return targetPair.swap();
        } else {
            return targetPair;
        }
    }

    private static void invertTrain(Train<AbstractTrainCarEntity> train) {
        var head = train.getHead();
        var tail = train.getTail();
        train.asList().forEach(AbstractTrainCarEntity::invertDoms);
        train.setHead(tail);
        train.setTail(head);
    }

    private Optional<Pair<AbstractTrainCarEntity, AbstractTrainCarEntity>> tryFindAndPrepareClosePair(Train<AbstractTrainCarEntity> train1, Train<AbstractTrainCarEntity> train2) {
        return findClosestPair(train1, train2).flatMap(targetPair -> {
            if (targetPair.getFirst().equals(train1.getHead()) && targetPair.getSecond().equals(train2.getHead())) {
                // if trying to attach to head loco then loco is solo
                if (train1.getTug().isPresent()) {
                    return Optional.of(targetPair);
                } else {
                    invertTrain(train2);
                    return Optional.of(targetPair.swap());
                }
            } else if (targetPair.getFirst().equals(train1.getHead()) && targetPair.getSecond().equals(train2.getTail())) {
                return Optional.of(caseTailHead(train2, train1, targetPair.swap()));
            } else if (targetPair.getFirst().equals(train1.getTail()) && targetPair.getSecond().equals(train2.getHead())) {
                return Optional.of(caseTailHead(train1, train2, targetPair));
            } else if (targetPair.getFirst().equals(train1.getTail()) && targetPair.getSecond().equals(train2.getTail())) {
                if (train2.getTug().isPresent()) {
                    invertTrain(train1);
                    return Optional.of(targetPair.swap());
                } else {
                    invertTrain(train2);
                    return Optional.of(targetPair);
                }
            }

            return Optional.empty();
        });
    }

    @Override
    public boolean linkEntities(Player player, Entity target) {
        if (target instanceof AbstractTrainCarEntity t) {
            Train<AbstractTrainCarEntity> train1 = t.getTrain();
            Train<AbstractTrainCarEntity> train2 = this.getTrain();
            if (train2.getTug().isPresent() && train1.getTug().isPresent()) {
                player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.noTwoLoco"), true);
                return false;
            } else if (train2.equals(train1)) {
                player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.noLoops"), true);
                return false;
            } else {
                tryFindAndPrepareClosePair(train1, train2)
                        .ifPresentOrElse(pair -> createLinks(pair.getFirst(), pair.getSecond()), () -> {
                            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.tooFar"), true);

                        });
            }

            return true;
        } else {
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.badTypes"), true);
            return false;
        }
    }

    private static void createLinks(AbstractTrainCarEntity dominant, AbstractTrainCarEntity dominated) {
        dominated.setDominant(dominant);
        dominant.setDominated(dominated);
    }

    @Override
    public abstract ItemStack getPickResult();


}
