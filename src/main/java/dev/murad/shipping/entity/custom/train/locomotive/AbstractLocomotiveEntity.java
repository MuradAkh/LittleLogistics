package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.block.rail.JunctionRail;
import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.global.VehicleRegistrationData;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.vessel.tug.VehicleFrontPart;
import dev.murad.shipping.entity.navigation.LocomotiveNavigator;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import dev.murad.shipping.util.*;
import lombok.Setter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.ChunkPos;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractLocomotiveEntity extends AbstractTrainCarEntity implements LinkableEntityHead<AbstractTrainCarEntity>, ItemHandlerVanillaContainerWrapper, HeadVehicle, StallingCapability {

    // =========================================================================
    // Consist (chain) management
    // =========================================================================

    private static final String CONSIST_TAG = "consist";
    private static final int CONSIST_RECONNECT_TIMEOUT = 600;
    private static final int LOAD_RECOVERY_TICKS = 5;
    private static final int COLLISION_LOOKAHEAD_STEPS = 5;
    private static final int LEGACY_COLLISION_TRAVERSE_LIMIT = COLLISION_LOOKAHEAD_STEPS - 1;
    private static final int JUNCTION_EXIT_CLEARANCE_STEPS = 3;
    private static final int JUNCTION_ROUTE_SEARCH_STEPS =
            COLLISION_LOOKAHEAD_STEPS + JUNCTION_EXIT_CLEARANCE_STEPS;

    private List<UUID> consistUUIDs = new ArrayList<>();
    private final Map<UUID, Integer> reconnectAttempts = new HashMap<>();

    public List<UUID> getExpectedConsistUUIDs() {
        return List.copyOf(consistUUIDs);
    }

    @Setter
    protected boolean engineOn = false;

    protected final VehicleOwnership ownership;

    private boolean independentMotion = false;
    private boolean docked = false;
    private final VehicleFrontPart frontHitbox;
    private int speedRecomputeCooldown = 0;
    private double speedLimit = -1;
    private int collisionCheckCooldown = 0;
    private int remainingStallTime = 0;
    private boolean forceStallCheck = false;
    private int loadRecoveryTicks;

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
    /** Incremented when the installed route changes, for inexpensive wrench-overlay snapshots. */
    private long routeOverlayRevision;

    public ItemStackHandler getRouteItemHandler() {
        return routeItemHandler;
    }

    public long getRouteOverlayRevision() {
        return routeOverlayRevision;
    }

    private static final String NAVIGATOR_TAG = "navigator";
    protected LocomotiveNavigator navigator = new LocomotiveNavigator(this);

    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractLocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double DOCK_CLEAR_DISTANCE = 0.75D;
    private static final int DOCK_DEPARTURE_TIMEOUT = 100;
    private DockingState dockingState = DockingState.APPROACHING;
    @Nullable
    private DockingSession dockingSession;

    private enum DockingState {
        APPROACHING,
        DOCKED,
        DEPARTING
    }

    private static final class DockingSession {
        private final BlockPos headDockPos;
        private final BlockPos portPos;
        private final Direction heading;
        private final Map<UUID, BlockPos> followerDockPositions = new HashMap<>();
        private int departureTicks;

        private DockingSession(BlockPos headDockPos, BlockPos portPos, Direction heading) {
            this.headDockPos = headDockPos.immutable();
            this.portPos = portPos.immutable();
            this.heading = heading;
        }
    }


    public AbstractLocomotiveEntity(EntityType<?> type, Level world) {
        super(type, world);
        frontHitbox = new VehicleFrontPart(this);
        ownership = new VehicleOwnership(this);
        consistUUIDs.add(this.getUUID());
    }

    public AbstractLocomotiveEntity(EntityType<?> type, Level level, Double x, Double y, Double z) {
        super(type, level, x, y, z);
        frontHitbox = new VehicleFrontPart(this);
        ownership = new VehicleOwnership(this);
        consistUUIDs.add(this.getUUID());
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
        if(!this.level().isClientSide && r != RemovalReason.UNLOADED_TO_CHUNK){
            if (this.level() instanceof ServerLevel level) {
                VehicleRegistrationData.get(level.getServer()).unregister(this.getUUID());
            }
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
            public int getSlotLimit(int slot) {
                // Governs both insertItem (shift-click/hopper) and the GUI's manual
                // placement via SlotItemHandler#getMaxStackSize. Overriding getStackLimit
                // alone only covers the former, letting up to 16 be placed by hand.
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                routeOverlayRevision++;
                updateNavigatorFromItem();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof LocoRouteItem && LocoRouteItem.getRoute(stack).isUsable();
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
    public boolean hasOwner(){
        return ownership.hasOwner();
    }

    @Override
    public Optional<UUID> getOwnerUUID() {
        return ownership.owner();
    }

    @Override
    public void setOwner(UUID uuid) {
        ownership.assign(uuid);
    }

    @Override
    public boolean isManagedServiceActive() {
        return engineOn && navigator.hasUsableRoute();
    }

    @Override
    public List<ChunkPos> getUpcomingRouteChunks(int maxSteps) {
        return navigator.getUpcomingChunks(maxSteps);
    }


    @Override
    public void tick(){
        linkingHandler.tickLoad();
        tickRailTravelDirection();

        if (!this.level().isClientSide) {
            tickOldBlockPos();
            ownership.tick();
            if (this.level() instanceof ServerLevel serverLevel) {
                tickConsist(serverLevel);
            }
            if (loadRecoveryTicks > 0) {
                if (loadRecoveryTicks > 1) {
                    loadRecoveryTicks--;
                } else if (navigator.recoverAfterLoad()) {
                    loadRecoveryTicks = 0;
                }
            } else if(remainingStallTime <= 0){
                navigator.serverTick();
            }
        }

        tickYRot();
        var yrot = this.getYRot();
        tickVanilla();
        tickRailTravelDirection();
        this.setYRot(yrot);
        if(linkingHandler.follower.isEmpty() && this.getDeltaMovement().length() > 0.05){
            this.setYRot(RailHelper.directionFromVelocity(getDeltaMovement()).toYRot());
        }
        if(!this.level().isClientSide){
            if (!isRecoveringAfterLoad()) {
                tickDockCheck();
            }
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
    }

    private void tickMovement() {
        if (isRecoveringAfterLoad()) {
            entityData.set(INDEPENDENT_MOTION, false);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(xOld, yOld, zOld);
            linkingHandler.train.asList().forEach(t -> t.setDeltaMovement(Vec3.ZERO));
            return;
        }

        if(remainingStallTime > 0){
            remainingStallTime--;
            if(remainingStallTime == 0)
                forceStallCheck = true;
        } else{
            if (collisionCheckCooldown <= 0 || forceStallCheck){
               if (hasCollisionAhead()){
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
            if(!docked && RailHelper.getRail(this.getOnPos().above(), this.level())
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

    /**
     * A synchronized compiled route is authoritative at upcoming switches. Walking the switch's
     * current block state can follow a different branch and permanently stall before the navigator
     * gets close enough to configure that switch.
     */
    private boolean hasCollisionAhead() {
        List<LocoRouteStep> upcoming = navigator.getUpcomingSteps(COLLISION_LOOKAHEAD_STEPS);
        if (!upcoming.isEmpty()) {
            return upcoming.stream().anyMatch(step ->
                    checkCollision(step.railPos()) || checkStopSign(step.railPos(), step.incomingDirection()));
        }

        return railHelper.traverse(getOnPos().above(), this.level(), this.getDirection(),
                (dir, pos) -> checkCollision(pos) || checkStopSign(pos, dir),
                LEGACY_COLLISION_TRAVERSE_LIMIT).isPresent();
    }

    private boolean checkStopSign(BlockPos pos, Direction prevExitTaken){

        return RailHelper.getRail(pos, this.level()).flatMap(block -> {
            if(level().getBlockState(block).getBlock() instanceof MultiShapeRail r){
                if(!this.level().getEntitiesOfClass(Entity.class, new AABB(pos), e -> e.equals(this) || e.equals(frontHitbox)).isEmpty())
                    return Optional.empty();
                boolean arbitrateRightOfWay = r instanceof JunctionRail;
                return r.getPreferredExits(level().getBlockState(block), prevExitTaken.getOpposite())
                        .stream()
                        .map(p -> railHelper.traverse(pos.relative(p), this.level(), p,
                                (dir, f) -> shouldYieldToLocomotive(f, block, arbitrateRightOfWay), 2))
                        .map(Optional::isPresent)
                        .reduce(Boolean::logicalOr);
            } else return Optional.of(false);
        }).orElse(false);

    }

    private boolean checkCollision(BlockPos pos) {
        BlockPos scannedRail = RailHelper.getRail(pos, this.level()).orElse(pos);
        AABB aabb = new AABB(scannedRail);
        return !this.level().getEntitiesOfClass(Entity.class, aabb,
                entity -> occupiesScannedRail(entity, scannedRail)).isEmpty();
    }

    /**
     * Bounding boxes may extend into a neighboring route block at curves. Resolve multipart hits
     * to their parent and require the rail vehicle itself to report the rail currently being
     * scanned, so a train on a parallel track is not treated as route occupancy.
     */
    private boolean occupiesScannedRail(Entity entity, BlockPos scannedRail) {
        Entity vehicle = entity instanceof VehicleFrontPart part ? part.getParent() : entity;
        if (!(vehicle instanceof AbstractMinecart minecart)) {
            return false;
        }
        if (vehicle instanceof AbstractTrainCarEntity trainCar) {
            boolean sameConsist = trainCar.getTrain().getTug()
                    .map(head -> head.getUUID().equals(this.getUUID()))
                    .orElse(false);
            if (sameConsist) {
                return false;
            }
        }
        return RailHelper.getRail(minecart.getOnPos().above(), this.level())
                .map(scannedRail::equals)
                .orElse(false);
    }

    /**
     * Stop-sign scans only consider incoming locomotive heads. Four-way junctions arbitrate
     * symmetrically; switch and tee rails retain their existing one-way merge priority.
     */
    private boolean shouldYieldToLocomotive(BlockPos pos, BlockPos junctionRail,
                                            boolean arbitrateRightOfWay) {
        BlockPos scannedRail = RailHelper.getRail(pos, this.level()).orElse(pos);
        AABB aabb = new AABB(scannedRail);
        Map<UUID, AbstractLocomotiveEntity> contenders = new HashMap<>();
        this.level().getEntitiesOfClass(Entity.class, aabb).forEach(entity -> {
            AbstractLocomotiveEntity contender = null;
            if (entity instanceof AbstractLocomotiveEntity locomotive) {
                contender = locomotive;
            } else if (entity instanceof VehicleFrontPart part
                    && part.getParent() instanceof AbstractLocomotiveEntity locomotive) {
                contender = locomotive;
            }
            if (contender == null) return;

            boolean onScannedRail = RailHelper.getRail(
                    contender.getOnPos().above(), this.level()).map(scannedRail::equals).orElse(false);
            if (!onScannedRail) return;

            boolean foreignConsist = contender.getTrain().getTug()
                    .map(head -> !head.getUUID().equals(this.getUUID()))
                    .orElse(true);
            if (foreignConsist) {
                contenders.put(contender.getUUID(), contender);
            }
        });

        return contenders.values().stream().anyMatch(contender ->
                shouldYieldTo(contender, junctionRail, arbitrateRightOfWay));
    }

    private boolean shouldYieldTo(AbstractLocomotiveEntity contender, BlockPos junctionRail,
                                  boolean arbitrateRightOfWay) {
        Optional<UUID> reservationOwner = navigator.getAutomaticRailReservationOwner(junctionRail);
        if (reservationOwner.isPresent()) {
            boolean selfOwnsReservation = reservationOwner.get().equals(this.getUUID());
            boolean contenderOwnsReservation = reservationOwner.get().equals(contender.getUUID());
            if (!selfOwnsReservation && !contenderOwnsReservation) {
                return true;
            }
            JunctionRightOfWay.Claim self = new JunctionRightOfWay.Claim(
                    selfOwnsReservation, false, this.getUUID());
            JunctionRightOfWay.Claim other = new JunctionRightOfWay.Claim(
                    contenderOwnsReservation, false, contender.getUUID());
            return JunctionRightOfWay.wins(other, self);
        }
        if (!arbitrateRightOfWay) {
            return true;
        }

        JunctionRightOfWay.Claim self = new JunctionRightOfWay.Claim(
                false, isJunctionExitClear(junctionRail), this.getUUID());
        JunctionRightOfWay.Claim other = new JunctionRightOfWay.Claim(
                false, contender.isJunctionExitClear(junctionRail), contender.getUUID());
        return JunctionRightOfWay.wins(other, self);
    }

    private boolean isJunctionExitClear(BlockPos junctionRail) {
        List<LocoRouteStep> upcoming = navigator.getUpcomingSteps(JUNCTION_ROUTE_SEARCH_STEPS);
        int junctionIndex = -1;
        for (int index = 0; index < upcoming.size(); index++) {
            if (upcoming.get(index).railPos().equals(junctionRail)) {
                junctionIndex = index;
                break;
            }
        }
        if (junctionIndex < 0) return false;

        int checked = 0;
        for (int index = junctionIndex + 1;
             index < upcoming.size() && checked < JUNCTION_EXIT_CLEARANCE_STEPS;
             index++, checked++) {
            if (checkCollision(upcoming.get(index).railPos())) {
                return false;
            }
        }
        return checked == JUNCTION_EXIT_CLEARANCE_STEPS;
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
        switch (dockingState) {
            case APPROACHING -> tryBeginDocking();
            case DOCKED -> tickDockedSession();
            case DEPARTING -> tickDeparture();
        }
    }

    private boolean isNearBlockCenter() {
        double xFraction = this.getX() - Math.floor(this.getX());
        double zFraction = this.getZ() - Math.floor(this.getZ());
        return xFraction > 0.2D && xFraction < 0.8D
                && zFraction > 0.2D && zFraction < 0.8D;
    }

    private void tryBeginDocking() {
        if (!isNearBlockCenter()) return;

        DockingStationBlockEntity dock = findDockAtPosition();
        Direction heading = this.getDirection();
        if (dock == null || dock.shouldPassThrough(this, heading) || !dock.tryOccupyDock(this)) {
            return;
        }

        dockingSession = new DockingSession(dock.getBlockPos(), dock.getVehicleBlockPos(), heading);
        dockingState = DockingState.DOCKED;
        docked = true;
        occupyFollowerDocks(dockingSession, dock);
        snapToDock(dock);
        onDock();
    }

    private void tickDockedSession() {
        DockingSession session = dockingSession;
        DockingStationBlockEntity headDock = session == null ? null : getDockAt(session.headDockPos);
        if (session != null && headDock != null) {
            occupyFollowerDocks(session, headDock);
            pruneInvalidFollowerAssignments(session);
        }
        if (session == null || headDock == null || !headDock.isOccupiedBy(this)
                || !isDockChainHolding(session, headDock)) {
            beginDeparture();
            return;
        }
        snapToDock(headDock);
    }

    private void snapToDock(DockingStationBlockEntity dock) {
        Vec3 center = dock.getVehicleCenterPos();
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(center.x, getY(), center.z);
    }

    @Nullable
    private DockingStationBlockEntity findDockAtPosition() {
        // Docking station controller is 1 block to the left or right of the rail.
        // getOnPos().above() == the rail block position for a cart.
        BlockPos railPos = getOnPos().above();
        Direction forward = getDirection();
        for (Direction side : new Direction[]{forward.getClockWise(), forward.getCounterClockWise()}) {
            BlockEntity be = level().getBlockEntity(railPos.relative(side));
            if (be instanceof DockingStationBlockEntity dbe
                    && dbe.getVehicleBlockPos().equals(railPos)
                    && forward.getAxis() != dbe.getBlockState().getValue(DockingStationBlock.FACING).getAxis()
                    && dbe.canOccupyDock(this)) {
                return dbe;
            }
        }
        return null;
    }

    private boolean isDockChainHolding(DockingSession session, DockingStationBlockEntity headDock) {
        if (headDock.isHolding()) return true;

        for (BlockPos pos : session.followerDockPositions.values()) {
            DockingStationBlockEntity dock = getDockAt(pos);
            if (dock != null && dock.isHolding()) return true;
        }
        return false;
    }

    private void occupyFollowerDocks(DockingSession session, DockingStationBlockEntity headDock) {
        List<DockingStationBlockEntity> followerDocks = headDock.getFollowerDocks(session.heading);
        Optional<AbstractTrainCarEntity> follower = this.getFollower();
        int index = 0;
        while (follower.isPresent() && index < followerDocks.size()) {
            DockingStationBlockEntity dock = followerDocks.get(index);
            Entity followerEntity = follower.get();
            if (!session.followerDockPositions.containsKey(followerEntity.getUUID())
                    && dock.tryOccupyDock(followerEntity)) {
                session.followerDockPositions.put(
                        followerEntity.getUUID(), dock.getBlockPos().immutable());
            }
            follower = follower.get().getFollower();
            index++;
        }
    }

    private void pruneInvalidFollowerAssignments(DockingSession session) {
        session.followerDockPositions.entrySet().removeIf(entry -> {
            DockingStationBlockEntity dock = getDockAt(entry.getValue());
            return dock == null || !dock.isOccupiedBy(entry.getKey());
        });
    }

    private void beginDeparture() {
        DockingSession session = dockingSession;
        if (session == null) {
            dockingState = DockingState.APPROACHING;
            this.undock();
            return;
        }

        DockingStationBlockEntity headDock = getDockAt(session.headDockPos);
        if (headDock != null) headDock.vacateDock(this);
        for (Map.Entry<UUID, BlockPos> entry : session.followerDockPositions.entrySet()) {
            DockingStationBlockEntity followerDock = getDockAt(entry.getValue());
            if (followerDock != null) {
                followerDock.vacateDock(entry.getKey());
            }
        }

        dockingState = DockingState.DEPARTING;
        this.undock();
        onUndock();
    }

    private void tickDeparture() {
        DockingSession session = dockingSession;
        if (session == null || hasConsistClearedDock(session)
                || ++session.departureTicks >= DOCK_DEPARTURE_TIMEOUT) {
            dockingSession = null;
            dockingState = DockingState.APPROACHING;
            return;
        }
        this.setYRot(session.heading.toYRot());
    }

    private boolean hasConsistClearedDock(DockingSession session) {
        if (horizontalDistance(this.position(), Vec3.atCenterOf(session.portPos)) <= DOCK_CLEAR_DISTANCE) {
            return false;
        }
        for (Map.Entry<UUID, BlockPos> entry : session.followerDockPositions.entrySet()) {
            Entity follower = findEntity(entry.getKey());
            DockingStationBlockEntity dock = getDockAt(entry.getValue());
            if (follower != null && dock != null
                    && horizontalDistance(follower.position(), dock.getVehicleCenterPos()) <= DOCK_CLEAR_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Nullable
    private DockingStationBlockEntity getDockAt(BlockPos pos) {
        BlockEntity be = level().getBlockEntity(pos);
        return be instanceof DockingStationBlockEntity dock ? dock : null;
    }

    @Nullable
    private Entity findEntity(UUID uuid) {
        return level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(uuid) : null;
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
        return isRecoveringAfterLoad() || !ownership.mayMove() || (this.isStalled() && !docked)
                || linkingHandler.train.asList().stream().anyMatch(AbstractTrainCarEntity::isFrozen);
    }

    private boolean isRecoveringAfterLoad() {
        return loadRecoveryTicks > 0;
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

    // =========================================================================
    // Consist reconnection
    // =========================================================================

    private void tickConsist(ServerLevel level) {
        tickReconnect(level);
        rebuildConsistList();
    }

    private void tickReconnect(ServerLevel level) {
        if (consistUUIDs.size() <= 1) return;

        boolean hadUnloaded = false;
        List<UUID> updated = new ArrayList<>();
        updated.add(this.getUUID());

        AbstractTrainCarEntity prev = this;
        for (int i = 1; i < consistUUIDs.size(); i++) {
            UUID uuid = consistUUIDs.get(i);
            Entity found = level.getEntity(uuid);

            if (found == null || found.isRemoved() || !(found instanceof AbstractTrainCarEntity curr)) {
                boolean skippedDeadSlot = false;
                if (i + 1 < consistUUIDs.size()) {
                    Entity nextFound = level.getEntity(consistUUIDs.get(i + 1));
                    if (nextFound instanceof AbstractTrainCarEntity nextCar && !nextCar.isRemoved()
                            && nextCar.getLeader().map(Entity::isRemoved).orElse(
                                    nextCar.getLeader().isEmpty())) {
                        reconnectAttempts.remove(uuid);
                        skippedDeadSlot = true;
                    }
                }

                if (!skippedDeadSlot) {
                    int attempts = reconnectAttempts.getOrDefault(uuid, 0) + 1;
                    if (attempts > CONSIST_RECONNECT_TIMEOUT) {
                        reconnectAttempts.remove(uuid);
                        break;
                    }
                    reconnectAttempts.put(uuid, attempts);
                    updated.add(uuid);
                    updated.addAll(consistUUIDs.subList(i + 1, consistUUIDs.size()));
                    hadUnloaded = true;
                    break;
                }
                continue;
            }

            reconnectAttempts.remove(uuid);
            updated.add(uuid);

            if (!prev.getFollower().map(f -> f == curr).orElse(false)) {
                if (curr.getLeader().map(Entity::isRemoved).orElse(true)) {
                    prev.setDominated(curr);
                    curr.setDominant(prev);
                }
            }

            prev = curr;
        }

        consistUUIDs = updated;

        if (hadUnloaded && !hasOwner()) {
            stall();
        }
    }

    private void rebuildConsistList() {
        List<UUID> liveList = new ArrayList<>();
        liveList.add(this.getUUID());
        Optional<AbstractTrainCarEntity> cur = getFollower();
        while (cur.isPresent()) {
            liveList.add(cur.get().getUUID());
            cur = cur.get().getFollower();
        }

        UUID liveTail = liveList.get(liveList.size() - 1);
        int idx = consistUUIDs.indexOf(liveTail);
        if (idx >= 0 && idx + 1 < consistUUIDs.size()) {
            liveList.addAll(consistUUIDs.subList(idx + 1, consistUUIDs.size()));
        }
        consistUUIDs = liveList;
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
        ownership.load(compound);
        updateNavigatorFromItem();
        navigator.loadFromNbt(compound.getCompound(NAVIGATOR_TAG));
        consistUUIDs.clear();
        reconnectAttempts.clear();
        if (compound.contains(CONSIST_TAG, Tag.TAG_LIST)) {
            ListTag list = compound.getList(CONSIST_TAG, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    consistUUIDs.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (consistUUIDs.isEmpty()) {
            consistUUIDs.add(this.getUUID());
        }
        if (!level().isClientSide) {
            loadRecoveryTicks = LOAD_RECOVERY_TICKS;
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("eo", engineOn);
        compound.put(LOCO_ROUTE_INV_TAG, routeItemHandler.serializeNBT(this.registryAccess()));
        compound.put(NAVIGATOR_TAG, navigator.saveToNbt());
        ownership.save(compound);
        rebuildConsistList();
        ListTag consistTag = new ListTag();
        for (UUID uuid : consistUUIDs) {
            consistTag.add(StringTag.valueOf(uuid.toString()));
        }
        compound.put(CONSIST_TAG, consistTag);
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
