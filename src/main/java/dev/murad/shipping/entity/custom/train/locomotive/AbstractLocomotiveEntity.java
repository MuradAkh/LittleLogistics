package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.dock.DockBlockEntity;
import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.vessel.tug.VehicleFrontPart;
import dev.murad.shipping.entity.navigation.LocomotiveNavigator;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import dev.murad.shipping.util.*;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractLocomotiveEntity extends AbstractTrainCarEntity implements LinkableEntityHead<AbstractTrainCarEntity>, ItemHandlerVanillaContainerWrapper, HeadVehicle, StallingCapability {

    @Setter
    protected boolean engineOn = false;

    protected final ChunkManagerEnrollmentHandler enrollmentHandler;

    private boolean independentMotion = false;
    private boolean docked = false;
    private final VehicleFrontPart frontHitbox;
    private int speedRecomputeCooldown = 0;
    private double speedLimit = -1;
    private int collisionCheckCooldown = 0;
    private int remainingStallTime = 0;
    private boolean forceStallCheck = false;

    private BlockPos currentHorizontalBlockPos;
    @Nullable
    private BlockPos oldHorizontalBlockPos;

    @Nullable
    public BlockPos getOldHorizontalBlockPos() {
        return oldHorizontalBlockPos;
    }

    // item handler for loco routes
    private static final String LOCO_ROUTE_INV_TAG = "locoRouteInv";


    protected ItemStackHandler routeItemHandler = createLocoRouteItemHandler();

    public ItemStackHandler getRouteItemHandler() {
        return routeItemHandler;
    }

    private static final String NAVIGATOR_TAG = "navigator";
    protected LocomotiveNavigator navigator = new LocomotiveNavigator(this);

    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(AbstractLocomotiveEntity.class, EntityDataSerializers.STRING);
    private int dockCheckCooldown = 0;


    public AbstractLocomotiveEntity(EntityType<?> type, Level world) {
        super(type, world);
        frontHitbox = new VehicleFrontPart(this);
        enrollmentHandler = new ChunkManagerEnrollmentHandler(this);
    }

    public AbstractLocomotiveEntity(EntityType<?> type, Level level, Double x, Double y, Double z) {
        super(type, level, x, y, z);
        frontHitbox = new VehicleFrontPart(this);
        enrollmentHandler = new ChunkManagerEnrollmentHandler(this);
    }

    @Override
    public void enroll(UUID uuid) {
        enrollmentHandler.enroll(uuid);
    }

    @Override
    public boolean allowDockInterface(){
        return docked;
    }

    @Override
    public ResourceLocation getRouteIcon() {
        return ModItems.LOCO_ROUTE_ICON;
    }

    @Override
    public void remove(RemovalReason r) {
        if(!this.level().isClientSide){
            this.spawnAtLocation(routeItemHandler.getStackInSlot(0));
        }
        super.remove(r);
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {

        InteractionResult ret = super.interact(pPlayer, pHand);
        if (ret.consumesAction()) return ret;

        if(!pHand.equals(InteractionHand.MAIN_HAND)){
            return InteractionResult.PASS;
        }

        if(!this.level().isClientSide){
            ((ServerPlayer) pPlayer).openMenu(createContainerProvider(), getDataAccessor()::write);
        }

        return InteractionResult.CONSUME;
    }

    private ItemStackHandler createLocoRouteItemHandler() {
        return new ItemStackHandler() {
            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                updateNavigatorFromItem();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof LocoRouteItem;
            }
        };
    }

    protected abstract MenuProvider createContainerProvider();

    public abstract DataAccessor getDataAccessor();

    protected abstract boolean tickFuel();

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level().isClientSide) {
            if(INDEPENDENT_MOTION.equals(key)) {
                independentMotion = entityData.get(INDEPENDENT_MOTION);
            }
        }
    }

    @Override
    public String owner() {
        return entityData.get(OWNER);
    }

    @Override
    public boolean hasOwner(){
        return enrollmentHandler.hasOwner();
    }


    @Override
    public void tick(){
        linkingHandler.tickLoad();

        if (!this.level().isClientSide) {
            tickOldBlockPos();
            if(remainingStallTime <= 0){
                navigator.serverTick();
            }
            enrollmentHandler.tick();
            enrollmentHandler.getPlayerName().ifPresent(name ->
                    entityData.set(OWNER, name)
            );
        }

        tickYRot();
        var yrot = this.getYRot();
        tickVanilla();
        this.setYRot(yrot);
        if(linkingHandler.follower.isEmpty() && this.getDeltaMovement().length() > 0.05){
            this.setYRot(RailHelper.directionFromVelocity(getDeltaMovement()).toYRot());
        }
        if(!this.level().isClientSide){
            tickDockCheck();
            tickMovement();
        }

        if(this.level().isClientSide
                && independentMotion){
            doMovementEffect();
        }


        frontHitbox.updatePosition(this);
    }

    private void tickOldBlockPos() {
        if (oldHorizontalBlockPos == null || currentHorizontalBlockPos == null) {
            oldHorizontalBlockPos = getBlockPos();
            currentHorizontalBlockPos = getBlockPos();
        } else {
            if (currentHorizontalBlockPos.getX() != this.getBlockX() ||
                    currentHorizontalBlockPos.getZ() != this.getBlockZ()) {
                oldHorizontalBlockPos = currentHorizontalBlockPos;
                currentHorizontalBlockPos = getBlockPos();
            }
        }
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return (float) (ShippingConfig.Server.TRAIN_MAX_SPEED.get() * 0.9);
    }

    public void flip() {
        this.setYRot(getDirection().getOpposite().toYRot());
    }

    protected void doMovementEffect() {

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INDEPENDENT_MOTION, false);
        builder.define(OWNER, "");
    }

    private void tickMovement() {
        if(remainingStallTime > 0){
            remainingStallTime--;
            if(remainingStallTime == 0)
                forceStallCheck = true;
        } else{
            if (collisionCheckCooldown <= 0 || forceStallCheck){
               var result = railHelper.traverse(getOnPos().above(), this.level(), this.getDirection(), (dir, pos)
                       -> checkCollision(pos) || checkStopSign(pos, dir),
                       4);
               if(result.isPresent()){
                   remainingStallTime = 40;
               }
               collisionCheckCooldown = 4;
               forceStallCheck = false;
            }else{
                collisionCheckCooldown--;
            }
        }
        if(!docked && engineOn && remainingStallTime <= 0 && !forceStallCheck && !shouldFreezeTrain() && tickFuel()) {
            tickSpeedLimit();
            entityData.set(INDEPENDENT_MOTION, true);
            accelerate();
        }else{
            if(RailHelper.getRail(this.getOnPos().above(), this.level())
                    .map(railHelper::getShape)
                    .map(Enum::name)
                    .map(s -> s.contains("ASCENDING"))
                    .orElse(true) && tickFuel()){
                this.setDeltaMovement(Vec3.ZERO);
                entityData.set(INDEPENDENT_MOTION, true);
                this.setPos(xOld, yOld, zOld);
            }else{
                entityData.set(INDEPENDENT_MOTION, false);
            }
        }

        if (shouldFreezeTrain()) {
            linkingHandler.train.asList().forEach(t -> t.setDeltaMovement(0, 0, 0));
        }
    }

    private boolean checkStopSign(BlockPos pos, Direction prevExitTaken){

        return RailHelper.getRail(pos, this.level()).flatMap(block -> {
            if(level().getBlockState(block).getBlock() instanceof MultiShapeRail r){
                if(!this.level().getEntitiesOfClass(Entity.class, new AABB(pos), e -> e.equals(this) || e.equals(frontHitbox)).isEmpty())
                    return Optional.empty();
                return r.getPreferredExits(level().getBlockState(block), prevExitTaken.getOpposite())
                        .stream()
                        .map(p -> railHelper.traverse(pos.relative(p), this.level(), p, (dir, f) -> checkLocoCollision(f), 2))
                        .map(Optional::isPresent)
                        .reduce(Boolean::logicalOr);
            } else return Optional.of(false);
        }).orElse(false);

    }

    private boolean checkCollision(BlockPos pos) {
        AABB aabb = new AABB(pos);
        return !this.level().getEntitiesOfClass(Entity.class, aabb, e -> {
            if(e instanceof AbstractTrainCarEntity t) {
                return t.getTrain().getTug().map(f -> !f.getUUID().equals(this.getUUID())).orElse(true);
            } else if (e instanceof AbstractMinecart ) return true;
            else if(e instanceof VehicleFrontPart p) {
                return !p.is(this);
            } else return false;
        }).isEmpty();
    }

    // to avoid deadlock for stopsign, you only care about incoming "heads"
    private boolean checkLocoCollision(BlockPos pos) {
        AABB aabb = new AABB(pos);
        return !this.level().getEntitiesOfClass(Entity.class, aabb, e -> {
            if(e instanceof AbstractLocomotiveEntity t) {
                return t.getTrain().getTug().map(f -> !f.getUUID().equals(this.getUUID())).orElse(true);
            } else if(e instanceof VehicleFrontPart p) {
                return !p.is(this);
            } else return false;
        }).isEmpty();
    }

    @Override
    public PartEntity<?>[] getParts()
    {
        return new PartEntity<?>[]{frontHitbox};
    }

    @Override
    public boolean isMultipartEntity()
    {
        return true;
    }

    public boolean isPoweredCart() {
        return true;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_149572_) {
        super.recreateFromPacket(p_149572_);
        frontHitbox.setId(p_149572_.getId());
    }


    protected void onDock() {
        this.playSound(ModSounds.TUG_DOCKING.get(), 0.6f, 1.0f);
    }

    protected void onUndock() {
        this.playSound(ModSounds.TUG_UNDOCKING.get(), 0.6f, 1.5f);
    }

    private void tickDockCheck() {
        int x = (int) Math.floor(this.getX());
        int y = (int) Math.floor(this.getY());
        int z = (int) Math.floor(this.getZ());

        boolean wasDocked = this.isDocked();

        if (wasDocked && dockCheckCooldown > 0) {
            dockCheckCooldown--;
            this.setDeltaMovement(Vec3.ZERO);
            this.moveTo(x + 0.5, getY(), z + 0.5);
            return;
        }

        Function<Double, Double> prepCord =
                (Double d) -> Math.abs(d - d.intValue());
        Predicate<Double> aroundCentre =
                (var i) -> prepCord.apply(i) < 0.8 && prepCord.apply(i) > 0.2;

        if (!aroundCentre.test(this.getX()) || !aroundCentre.test(this.getZ())) {
            return;
        }

        DockBlockEntity dock = findDockAtPosition();

        boolean shouldDock;
        if (dock != null) {
            if (!wasDocked) {
                if (dock.shouldPassThrough(this.getDirection())) {
                    shouldDock = false;
                } else {
                    shouldDock = true;
                }
            } else {
                shouldDock = isDockChainHolding();
            }
        } else {
            shouldDock = false;
        }

        boolean changedDock = !wasDocked && shouldDock;
        boolean changedUndock = wasDocked && !shouldDock;

        if (shouldDock) {
            if (changedDock && dock != null) {
                dock.occupyDock(this);
            }
            // Refresh follower dock assignments each check cycle
            // as followers may still be settling into position via spring physics
            occupyFollowerDocks();
            dockCheckCooldown = dock != null ? 5 : 20;
            this.dock(x + 0.5, getY(), z + 0.5);
        } else {
            if (changedUndock && dock != null) {
                vacateAllDocks();
            }
            dockCheckCooldown = 0;
            this.undock();
        }

        if (changedDock) onDock();
        if (changedUndock) onUndock();
    }

    @Nullable
    private DockBlockEntity findDockAtPosition() {
        // Rail docks are under the vehicle (at the rail block position)
        BlockEntity be = level().getBlockEntity(getOnPos().above());
        if (be instanceof DockBlockEntity dockBE) {
            return dockBE;
        }
        // Also check at blockPosition in case of different rail placement
        be = level().getBlockEntity(blockPosition());
        if (be instanceof DockBlockEntity dockBE) {
            return dockBE;
        }
        return null;
    }

    private boolean isDockChainHolding() {
        DockBlockEntity myDock = findDockAtPosition();
        if (myDock != null && myDock.isHolding()) return true;

        // Walk follower chain
        Optional<AbstractTrainCarEntity> follower = this.getFollower();
        while (follower.isPresent()) {
            BlockPos followerPos = follower.get().getOnPos().above();
            BlockEntity be = level().getBlockEntity(followerPos);
            if (be instanceof DockBlockEntity dockBE && dockBE.isHolding()) {
                return true;
            }
            // Also check blockPosition
            be = level().getBlockEntity(follower.get().blockPosition());
            if (be instanceof DockBlockEntity dockBE && dockBE.isHolding()) {
                return true;
            }
            follower = follower.get().getFollower();
        }
        return false;
    }

    private void occupyFollowerDocks() {
        Optional<AbstractTrainCarEntity> follower = this.getFollower();
        while (follower.isPresent()) {
            Entity followerEntity = follower.get();
            // Check dock at follower's rail position
            DockBlockEntity targetDock = null;
            BlockEntity be = level().getBlockEntity(followerEntity.getOnPos().above());
            if (be instanceof DockBlockEntity dockBE) {
                targetDock = dockBE;
            } else {
                be = level().getBlockEntity(followerEntity.blockPosition());
                if (be instanceof DockBlockEntity dockBE) {
                    targetDock = dockBE;
                }
            }
            if (targetDock != null) {
                // Only re-assign if the dock has a different vehicle (or none)
                Entity current = targetDock.getDockedVehicle();
                if (current != followerEntity) {
                    if (current != null) targetDock.vacateDock();
                    targetDock.occupyDock(followerEntity);
                }
            }
            follower = follower.get().getFollower();
        }
    }

    private void vacateAllDocks() {
        // Vacate head dock
        DockBlockEntity myDock = findDockAtPosition();
        if (myDock != null) myDock.vacateDock();

        // Vacate follower docks
        Optional<AbstractTrainCarEntity> follower = this.getFollower();
        while (follower.isPresent()) {
            BlockPos followerPos = follower.get().getOnPos().above();
            BlockEntity be = level().getBlockEntity(followerPos);
            if (be instanceof DockBlockEntity dockBE) {
                dockBE.vacateDock();
            }
            be = level().getBlockEntity(follower.get().blockPosition());
            if (be instanceof DockBlockEntity dockBE) {
                dockBE.vacateDock();
            }
            follower = follower.get().getFollower();
        }
    }

    private double getSpeedModifier(){
        // adjust speed based on slope etc.
        var state = this.level().getBlockState(this.getOnPos().above());
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

    private void tickSpeedLimit(){
        if(speedRecomputeCooldown < 0 || speedLimit < 0 ) {
            var dist = RailHelper.getRail(getOnPos().above(), this.level()).flatMap(pos ->
                            railHelper.traverse(pos,
                                    this.level(),
                                    this.getDirection(),
                                    (direction, p) -> {
                                        var railoc = RailHelper.getRail(p, this.level());
                                        if (railoc.isEmpty()) {
                                            return true;
                                        }
                                        var shape = railHelper.getShape(railoc.get());
                                        var block = this.level().getBlockState(railoc.get());
                                        return !(shape.equals(RailShape.EAST_WEST) || shape.equals(RailShape.NORTH_SOUTH))
                                                || block.is(ModBlocks.DOCK_RAIL.get())
                                                || block.getBlock() instanceof MultiShapeRail;
                                    },
                                    12))
                    .orElse(12);
            double minimum = ShippingConfig.Server.LOCO_BASE_SPEED.get() * 0.2;
            double modifier = dist / 12d;
            speedLimit = minimum + (ShippingConfig.Server.LOCO_BASE_SPEED.get() * 0.8 * modifier);
            speedRecomputeCooldown = 10;
        } else {
            speedRecomputeCooldown--;
        }
    }

    public boolean shouldFreezeTrain() {
        return !enrollmentHandler.mayMove() || (this.isStalled() && !docked) || linkingHandler.train.asList().stream().anyMatch(AbstractTrainCarEntity::isFrozen);
    }

    private void accelerate() {
        var dir = this.getDirection();
        if(Math.abs(this.getDeltaMovement().x) < speedLimit && Math.abs(this.getDeltaMovement().z) < speedLimit){
            var mod = this.getSpeedModifier();
            this.push(dir.getStepX() * mod, 0, dir.getStepZ() * mod);
        }
    }

    @Override
    public void setDominated(AbstractTrainCarEntity entity) {
        linkingHandler.follower = Optional.of(entity);
    }

    @Override
    public void setDominant(AbstractTrainCarEntity entity) {
    }

    @Override
    public void removeDominated() {
        linkingHandler.follower = Optional.empty();
        linkingHandler.train.setTail(this);
    }

    @Override
    public void removeDominant() {

    }

    @Override
    public void setTrain(Train<AbstractTrainCarEntity> train) {
        linkingHandler.train = train;
    }

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
        return remainingStallTime > 0;
    }

    @Override
    public void stall() {
        remainingStallTime = 20;
    }

    @Override
    public void unstall() {
        remainingStallTime = 0;
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

    private void updateNavigatorFromItem() {
        ItemStack stack = routeItemHandler.getStackInSlot(0);
        if (stack.getItem() instanceof LocoRouteItem) {
            navigator.updateWithLocoRouteItem(LocoRouteItem.getRoute(stack));
        } else {
            navigator.updateWithLocoRouteItem(new LocoRoute());
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if(compound.contains("eo")) {
            engineOn = compound.getBoolean("eo");
        }
        routeItemHandler.deserializeNBT(this.registryAccess(), compound.getCompound(LOCO_ROUTE_INV_TAG));
        navigator.loadFromNbt(compound.getCompound(NAVIGATOR_TAG));
        enrollmentHandler.load(compound);
        updateNavigatorFromItem();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("eo", engineOn);
        compound.put(LOCO_ROUTE_INV_TAG, routeItemHandler.serializeNBT(this.registryAccess()));
        compound.put(NAVIGATOR_TAG, navigator.saveToNbt());
        enrollmentHandler.save(compound);
    }

    // duplicate due to linking issues

    @Override
    public boolean isValid(Player pPlayer) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(this.distanceToSqr(pPlayer) > 64D);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(this.distanceToSqr(pPlayer) > 64D);
        }
    }
}
