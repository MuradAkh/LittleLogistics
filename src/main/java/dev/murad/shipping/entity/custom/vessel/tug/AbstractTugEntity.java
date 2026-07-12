package dev.murad.shipping.entity.custom.vessel.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.block.guiderail.TugGuideRailBlock;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.*;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.navigation.TugPathNavigator;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModSounds;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class AbstractTugEntity extends VesselEntity implements LinkableEntityHead<VesselEntity>, Container, WorldlyContainer, HeadVehicle, StallingCapability {

    protected final ChunkManagerEnrollmentHandler enrollmentHandler;

    // CONTAINER STUFF
    protected final ItemStackHandler routeItemHandler = createRouteItemHandler();

    public ItemStackHandler getRouteItemHandler() {
        return routeItemHandler;
    }
    protected boolean contentsChanged = false;
    protected boolean docked = false;
    protected int remainingStallTime = 0;

    public int getRemainingStallTime() {
        return remainingStallTime;
    }
    private double swimSpeedMult = 1;

    protected boolean engineOn = true;

    public void setEngineOn(boolean engineOn) {
        this.engineOn = engineOn;
    }

    private DockingState dockingState = DockingState.APPROACHING;
    @Nullable
    private DockingSession dockingSession;
    private boolean independentMotion = false;
    private double routeProgress = 0.0D;
    private VehicleFrontPart frontHitbox;
    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);
    private static final String ROUTE_PROGRESS_TAG = "route_progress";
    private static final double ROUTE_LOOKAHEAD = 0.8D;
    private static final double FOLLOWER_CORRECTION_BLEND = 0.65D;
    private static final double FOLLOWER_HARD_SNAP_DISTANCE = 4.0D;
    private static final double DOCK_SETTLE_STEP = 0.08D;
    private static final double DOCK_SETTLE_EPSILON = 0.02D;
    private static final double DOCK_CLEAR_DISTANCE = 0.75D;
    private static final double ROUTE_REBASE_MAX_DISTANCE = 1.0D;

    /** A tug may only rediscover docks while approaching; a session owns exact dock positions. */
    private enum DockingState {
        APPROACHING,
        SETTLING,
        DOCKED,
        DEPARTING
    }

    private static final class DockingSession {
        private final BlockPos headDockPos;
        private final BlockPos portPos;
        private final Direction heading;
        private final Map<UUID, BlockPos> followerDockPositions = new HashMap<>();

        private DockingSession(BlockPos headDockPos, BlockPos portPos, Direction heading) {
            this.headDockPos = headDockPos.immutable();
            this.portPos = portPos.immutable();
            this.heading = heading;
        }
    }



    public boolean allowDockInterface(){
        return isDocked();
    }

    // =========================================================================
    // Consist (chain) management — head entity owns the authoritative UUID list
    // =========================================================================

    private static final String CONSIST_TAG = "consist";
    /** Ticks without finding an entity before we assume it was permanently removed. */
    private static final int CONSIST_RECONNECT_TIMEOUT = 600;

    /** Ordered UUID list: index 0 = this tug, index N = Nth barge in chain. */
    private List<UUID> consistUUIDs = new ArrayList<>();
    /** Per-UUID reconnect attempt counters; entry removed on successful find. */
    private final Map<UUID, Integer> reconnectAttempts = new HashMap<>();

    protected TugRoute path;
    @Nullable
    private TugRouteTrack routeTrack;

    public AbstractTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
        linkingHandler.train = (new Train<>(this));
        this.path = new TugRoute();
        frontHitbox = new VehicleFrontPart(this);
        enrollmentHandler = new ChunkManagerEnrollmentHandler(this);
        // Seed consist list with just self; extended when barges are linked
        consistUUIDs.add(this.getUUID());
    }

    public AbstractTugEntity(EntityType type, Level worldIn, double x, double y, double z) {
        this(type, worldIn);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public ResourceLocation getRouteIcon() {
        return ModItems.TUG_ROUTE_ICON;
    }


    // CONTAINER STUFF
    @Override
    public void dropLeash(boolean p_110160_1_, boolean p_110160_2_) {
        navigation.recomputePath();
        super.dropLeash(p_110160_1_, p_110160_2_);
    }


    public abstract DataAccessor getDataAccessor();

    private ItemStackHandler createRouteItemHandler() {
        return new ItemStackHandler() {
            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged = true;
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof TugRouteItem && TugRouteItem.getRoute(stack).isComplete();
            }
        };
    }

    @Override
    public String owner() {
        return entityData.get(OWNER);
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    protected abstract MenuProvider createContainerProvider();

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        if(compound.contains("inv")){
            ItemStackHandler old = new ItemStackHandler();
            old.deserializeNBT(this.registryAccess(), compound.getCompound("inv"));
            routeItemHandler.setStackInSlot(0, old.getStackInSlot(0));
        }else{
            routeItemHandler.deserializeNBT(this.registryAccess(), compound.getCompound("routeHandler"));
        }
        routeProgress = compound.contains(ROUTE_PROGRESS_TAG) ? compound.getDouble(ROUTE_PROGRESS_TAG) : 0.0D;
        engineOn = !compound.contains("engineOn") || compound.getBoolean("engineOn");
        contentsChanged = true;
        enrollmentHandler.load(compound);
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
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putDouble(ROUTE_PROGRESS_TAG, routeProgress);
        compound.putBoolean("engineOn", engineOn);
        compound.put("routeHandler", routeItemHandler.serializeNBT(this.registryAccess()));
        enrollmentHandler.save(compound);
        // Persist consist list — rebuild from live chain first so new spring links are captured
        rebuildConsistList();
        ListTag consistTag = new ListTag();
        for (UUID uuid : consistUUIDs) {
            consistTag.add(StringTag.valueOf(uuid.toString()));
        }
        compound.put(CONSIST_TAG, consistTag);
        super.addAdditionalSaveData(compound);
    }

    // =========================================================================
    // Consist reconnection
    // =========================================================================

    private void tickConsist(ServerLevel level) {
        tickReconnect(level);
        rebuildConsistList();
    }

    /**
     * Iterates consistUUIDs and re-establishes any missing links using the level's
     * entity-by-UUID lookup (world-wide, not position-based).
     * Stalls the tug if followers are still loading and we don't have a chunk-loading owner.
     * Times out UUIDs that never load (permanently removed entities).
     */
    private void tickReconnect(ServerLevel level) {
        if (consistUUIDs.size() <= 1) return;

        boolean hadUnloaded = false;
        List<UUID> updated = new ArrayList<>();
        updated.add(this.getUUID());

        VesselEntity prev = this;
        for (int i = 1; i < consistUUIDs.size(); i++) {
            UUID uuid = consistUUIDs.get(i);
            Entity found = level.getEntity(uuid);

            if (found == null || found.isRemoved() || !(found instanceof VesselEntity curr)) {
                // Entity not loaded — check if the NEXT slot is alive with no leader,
                // which indicates UUID[i] was permanently killed (handleLinkableKill fired).
                boolean skippedDeadSlot = false;
                if (i + 1 < consistUUIDs.size()) {
                    Entity nextFound = level.getEntity(consistUUIDs.get(i + 1));
                    if (nextFound instanceof VesselEntity nextVessel && !nextVessel.isRemoved()
                            && nextVessel.getLeader().map(Entity::isRemoved).orElse(
                                    nextVessel.getLeader().isEmpty())) {
                        // UUID[i] is permanently gone; skip it and let the loop connect prev→nextVessel
                        reconnectAttempts.remove(uuid);
                        skippedDeadSlot = true;
                        // Don't add uuid to updated — it's dropped
                    }
                }

                if (!skippedDeadSlot) {
                    int attempts = reconnectAttempts.getOrDefault(uuid, 0) + 1;
                    if (attempts > CONSIST_RECONNECT_TIMEOUT) {
                        // Timed out — treat as permanently gone, stop chain here
                        reconnectAttempts.remove(uuid);
                        break;
                    }
                    reconnectAttempts.put(uuid, attempts);
                    updated.add(uuid);
                    // Preserve the rest of the list as-is (they're behind this pending one)
                    updated.addAll(consistUUIDs.subList(i + 1, consistUUIDs.size()));
                    hadUnloaded = true;
                    break;
                }
                // skippedDeadSlot=true: continue loop — i increments to the next slot (nextVessel)
                continue;
            }

            reconnectAttempts.remove(uuid);
            updated.add(uuid);

            // Reconnect if the link is missing (prev→curr not established)
            if (!prev.getFollower().map(f -> f == curr).orElse(false)) {
                // Only claim curr if it has no live leader (avoid stealing from another train)
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

    /**
     * Rebuilds consistUUIDs by walking the live chain, then appending any
     * pending-reconnect UUIDs from the current list that follow the live tail.
     * This captures new spring links and removes permanently cut wagons.
     */
    private void rebuildConsistList() {
        List<UUID> liveList = new ArrayList<>();
        liveList.add(this.getUUID());
        Optional<VesselEntity> cur = getFollower();
        while (cur.isPresent()) {
            liveList.add(cur.get().getUUID());
            cur = cur.get().getFollower();
        }

        UUID liveTail = liveList.get(liveList.size() - 1);
        int idx = consistUUIDs.indexOf(liveTail);
        if (idx >= 0 && idx + 1 < consistUUIDs.size()) {
            // Append unloaded-but-pending UUIDs that follow the current live tail
            liveList.addAll(consistUUIDs.subList(idx + 1, consistUUIDs.size()));
        }
        consistUUIDs = liveList;
    }

    private void tickRouteCheck() {
        if (contentsChanged) {
            ItemStack stack = routeItemHandler.getStackInSlot(0);
            TugRoute route = TugRouteItem.getRoute(stack);
            this.setPath(route.isComplete() ? route : new TugRoute());
            contentsChanged = false;
        }
    }

    protected abstract boolean tickFuel();

    public static AttributeSupplier.Builder setCustomAttributes() {
        return VesselEntity.setCustomAttributes()
                .add(Attributes.FOLLOW_RANGE, 200);
    }

    protected void onDock() {
        this.playSound(ModSounds.TUG_DOCKING.get(), 0.6f, 1.0f);
    }

    protected void onUndock() {
        this.playSound(ModSounds.TUG_UNDOCKING.get(), 0.6f, 1.5f);
    }

    // MOB STUFF

    private Direction getDockHeading() {
        return Direction.fromYRot(this.getYRot());
    }

    private List<Direction> getSideDirections() {
        Direction heading = getDockHeading();
        return heading == Direction.NORTH || heading == Direction.SOUTH ?
                Arrays.asList(Direction.EAST, Direction.WEST) :
                Arrays.asList(Direction.NORTH, Direction.SOUTH);
    }


    private void tickCheckDock() {
        switch (dockingState) {
            case APPROACHING -> tryBeginDocking();
            case SETTLING -> tickSettlingSession();
            case DOCKED -> tickDockedSession();
            case DEPARTING -> tickDeparture();
        }
    }

    @Nullable
    private DockingStationBlockEntity findAdjacentDock() {
        BlockPos pos = this.blockPosition();
        for (Direction dir : getSideDirections()) {
            // Check same level and one above — the controller sits on the canal bank
            // which is typically one block higher than the water the tug occupies.
            for (BlockPos candidate : new BlockPos[]{pos.relative(dir), pos.above().relative(dir)}) {
                BlockEntity be = level().getBlockEntity(candidate);
                if (be instanceof DockingStationBlockEntity dockBE && isValidDockCandidate(dockBE)) {
                    return dockBE;
                }
            }
        }
        return null;
    }

    /** A nearby controller is not enough: the tug must be in that controller's port cell. */
    private boolean isValidDockCandidate(DockingStationBlockEntity dock) {
        BlockPos portPos = dock.getVehicleBlockPos();
        Direction heading = getDockHeading();
        return portPos.getX() == Mth.floor(this.getX())
                && portPos.getZ() == Mth.floor(this.getZ())
                && heading.getAxis() != dock.getBlockState().getValue(
                        dev.murad.shipping.block.dockingstation.DockingStationBlock.FACING).getAxis();
    }

    private void tryBeginDocking() {
        DockingStationBlockEntity dock = findAdjacentDock();
        Direction heading = getDockHeading();
        if (dock == null || dock.shouldPassThrough(heading) || !dock.tryOccupyDock(this)) {
            return;
        }

        dockingSession = new DockingSession(dock.getBlockPos(), dock.getVehicleBlockPos(), heading);
        docked = true;
        occupyFollowerDocks(dockingSession, dock);
        snapToDock(dock);
        rebaseRouteProgressAtDock();
        dockingState = dockingSession.followerDockPositions.isEmpty() ? DockingState.DOCKED : DockingState.SETTLING;
        onDock();
    }

    private void tickSettlingSession() {
        DockingSession session = dockingSession;
        DockingStationBlockEntity headDock = session == null ? null : getDockAt(session.headDockPos);
        if (session == null || headDock == null || !isDockChainHolding(session, headDock)) {
            beginDeparture();
            return;
        }

        snapToDock(headDock);
        if (areAssignedFollowersSettled(session)) {
            dockingState = DockingState.DOCKED;
        }
    }

    private void tickDockedSession() {
        DockingSession session = dockingSession;
        DockingStationBlockEntity headDock = session == null ? null : getDockAt(session.headDockPos);
        if (session == null || headDock == null || !isDockChainHolding(session, headDock)) {
            beginDeparture();
            return;
        }
        snapToDock(headDock);
    }

    private boolean areAssignedFollowersSettled(DockingSession session) {
        for (Map.Entry<UUID, BlockPos> entry : session.followerDockPositions.entrySet()) {
            Entity entity = findEntity(entry.getKey());
            DockingStationBlockEntity dock = getDockAt(entry.getValue());
            if (entity == null || dock == null) {
                return false;
            }
            if (horizontalDistance(entity.position(), dock.getVehicleCenterPos()) > DOCK_SETTLE_EPSILON) {
                return false;
            }
        }
        return true;
    }

    private void snapToDock(DockingStationBlockEntity dock) {
        Vec3 center = dock.getVehicleCenterPos();
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(center.x, getY(), center.z, dockingSession.heading.toYRot(), getXRot());
    }

    /** Keep route-based follower targets continuous with the snapped dock pose. */
    private void rebaseRouteProgressAtDock() {
        if (!hasCompiledRouteTrack()) {
            return;
        }

        routeTrack.project(this.position())
                .filter(projection -> projection.distanceToTrack() <= ROUTE_REBASE_MAX_DISTANCE)
                .ifPresent(projection -> routeProgress = projection.distanceAlongTrack());
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
        Optional<VesselEntity> follower = this.getFollower();
        int index = 0;
        while (follower.isPresent() && index < followerDocks.size()) {
            DockingStationBlockEntity dock = followerDocks.get(index);
            Entity followerEntity = follower.get();
            if (dock.tryOccupyDock(followerEntity)) {
                session.followerDockPositions.put(followerEntity.getUUID(), dock.getBlockPos().immutable());
            }
            follower = follower.get().getFollower();
            index++;
        }
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
        if (session == null || hasConvoyClearedDock(session)) {
            dockingSession = null;
            dockingState = DockingState.APPROACHING;
            return;
        }
        this.setYRot(session.heading.toYRot());
    }

    private boolean hasConvoyClearedDock(DockingSession session) {
        if (horizontalDistance(this.position(), Vec3.atCenterOf(session.portPos)) <= WaterConvoyGeometry.DOCKED_CENTER_SPACING) {
            return false;
        }

        for (Map.Entry<UUID, BlockPos> entry : session.followerDockPositions.entrySet()) {
            Entity follower = findEntity(entry.getKey());
            DockingStationBlockEntity dock = getDockAt(entry.getValue());
            if (follower == null || dock == null
                    || horizontalDistance(follower.position(), dock.getVehicleCenterPos()) <= DOCK_CLEAR_DISTANCE) {
                return false;
            }
        }
        return true;
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

    protected void makeSmoke() {
        Level world = this.level();
        if (world != null) {
            BlockPos blockpos = this.getOnPos().above().above();
            RandomSource random = world.random;
            if (random.nextFloat() < ShippingConfig.Client.TUG_SMOKE_MODIFIER.get()) {
                for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                    makeParticles(world, blockpos, this);
                }
            }
        }
    }

    public static void makeParticles(Level level, BlockPos pos, Entity entity) {
        RandomSource random = level.getRandom();
        Supplier<Boolean> h = () -> random.nextDouble() < 0.5;

        var dx = (entity.getX() - entity.xOld) / 12.0;
        var dy = (entity.getY() - entity.yOld) / 12.0;
        var dz = (entity.getZ() - entity.zOld) / 12.0;

        double xDrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;
        double zDrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;

        var particleType = random.nextBoolean() ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;

        level.addAlwaysVisibleParticle(particleType,
                true,
                (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1),
                (double)pos.getY() + random.nextDouble() + random.nextDouble(),
                (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1),
                0.007D * xDrift + dx, 0.05D + dy, 0.007D * zDrift + dz);
    }

    @Override
    protected PathNavigation createNavigation(Level p_175447_1_) {
        return new TugPathNavigator(this, p_175447_1_);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            var color = DyeColor.getColor(player.getItemInHand(hand));

            if (color != null) {
                this.getEntityData().set(COLOR_DATA, color.getId());
            } else {
                ((ServerPlayer) player).openMenu(createContainerProvider(), getDataAccessor()::write);
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void enroll(UUID uuid) {
        enrollmentHandler.enroll(uuid);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level().isClientSide) {
            if(INDEPENDENT_MOTION.equals(key)) {
                independentMotion = entityData.get(INDEPENDENT_MOTION);
            }
        }
    }


    protected void registerGoals() {
        this.goalSelector.addGoal(0, new MovementGoal());
    }

    class MovementGoal extends Goal {
        @Override
        public boolean canUse() {
            return AbstractTugEntity.this.path != null;
        }

        public void tick() {
            if(!AbstractTugEntity.this.level().isClientSide) {
                tickRouteCheck();
                tickCheckDock();

                if (AbstractTugEntity.this.level() instanceof ServerLevel serverLevel) {
                    tickConsist(serverLevel);
                }

                followPath();
                if (!hasCompiledRouteTrack()) {
                    followGuideRail();
                }
                // Route/guide motion may update yaw while the tug is clearing the dock.
                // Keep its dock-facing heading until it has crossed the spatial exit boundary.
                if (dockingState == DockingState.DEPARTING && dockingSession != null) {
                    setYRot(dockingSession.heading.toYRot());
                }

            }

        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts()
    {
        return new PartEntity<?>[]{frontHitbox};
    }

    @Override
    public void aiStep(){
        super.aiStep();
        if(!isDeadOrDying() && !this.isNoAi()){
            frontHitbox.updatePosition(this);
        }

    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_149572_) {
        super.recreateFromPacket(p_149572_);
        frontHitbox.setId(p_149572_.getId());
    }

    public void tick() {
        if(this.level().isClientSide && independentMotion){
            makeSmoke();
        }

        if(!this.level().isClientSide) {
            enrollmentHandler.tick();
            enrollmentHandler.getPlayerName().ifPresent(name ->
                    entityData.set(OWNER, name));
        }

        super.tick();
    }

    private void followGuideRail(){
        // do not follow guide rail if stalled
        if (this.isDocked() || this.isFrozen() || this.isStalled()) {
            return;
        }

        List<BlockState> belowList = Arrays.asList(this.level().getBlockState(getOnPos().below()),
                this.level().getBlockState(getOnPos().below().below()));
        BlockState water = this.level().getBlockState(getOnPos());
        for (BlockState below : belowList) {
            if (below.is(ModBlocks.GUIDE_RAIL_TUG.get()) && water.is(Blocks.WATER)) {
                Direction arrows = TugGuideRailBlock.getArrowsDirection(below);
                this.setYRot(arrows.toYRot());
                double modifier = 0.03;
                this.setDeltaMovement(this.getDeltaMovement().add(
                        new Vec3(arrows.getStepX() * modifier, 0, arrows.getStepZ() * modifier)));
            }
        }
    }

    // todo: someone said you could prevent mobs from getting stuck on blocks by override this
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
    }

    private void followPath() {
        if (hasCompiledRouteTrack() && !this.docked && engineOn && !shouldFreezeTrain() && tickFuel()) {
            independentMotion = true;
            entityData.set(INDEPENDENT_MOTION, true);
            navigation.stop();

            double speed = getRouteCruiseSpeed();
            routeProgress = routeTrack.wrapDistance(routeProgress + speed);
            TugRouteTrack.Sample current = routeTrack.sample(routeProgress);
            TugRouteTrack.Sample lookAhead = routeTrack.sample(routeProgress + ROUTE_LOOKAHEAD);
            Vec3 desiredDirection = lookAhead.position().subtract(this.position());
            Vec3 horizontalDirection = new Vec3(desiredDirection.x, 0.0D, desiredDirection.z);

            if (horizontalDirection.lengthSqr() > 1.0E-4D) {
                Vec3 desiredVelocity = horizontalDirection.normalize().scale(speed);
                Vec3 currentVelocity = this.getDeltaMovement();
                this.setDeltaMovement(currentVelocity.scale(0.55D).add(desiredVelocity.scale(0.45D)));
                this.setYRot(computeRouteYaw(desiredVelocity));
            }

            if (this.position().distanceTo(current.position()) > 2.0D) {
                this.moveTo(current.position().x, this.getY(), current.position().z, this.getYRot(), this.getXRot());
            }
        } else {
            entityData.set(INDEPENDENT_MOTION, false);
            this.navigation.stop();
            if (remainingStallTime > 0) {
                remainingStallTime--;
            }
        }
    }

    public boolean shouldFreezeTrain() {
        return !enrollmentHandler.mayMove() || (this.isStalled() && !docked) || linkingHandler.train.asList().stream().anyMatch(VesselEntity::isFrozen);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INDEPENDENT_MOTION, false);
        builder.define(OWNER, "");
    }


    public void setPath(TugRoute path) {
        if (!path.isComplete()) {
            path = new TugRoute();
        }
        if (!this.path.equals(path)) {
            this.routeProgress = 0.0D;
        }
        this.path = path;
        this.routeTrack = TugRouteTrack.from(path);
    }

    @Override
    public void setDominated(VesselEntity entity) {
        linkingHandler.follower = (Optional.of(entity));
    }

    @Override
    public void setDominant(VesselEntity entity) {

    }

    @Override
    public void removeDominated() {
        linkingHandler.follower = (Optional.empty());
        linkingHandler.train.setTail(this);
    }

    @Override
    public boolean hasOwner(){
        return enrollmentHandler.hasOwner();
    }

    @Override
    public void removeDominant() {

    }

    @Override
    public void setTrain(Train<VesselEntity> train) {
        linkingHandler.train = train;
    }

    public boolean hasCompiledRouteTrack() {
        return routeTrack != null && routeTrack.isUsable();
    }

    private double getRouteCruiseSpeed() {
        return ShippingConfig.Server.TUG_BASE_SPEED.get() * 0.04D;
    }

    public int getVisitedRouteNodeCount() {
        if (path == null || path.isEmpty() || !hasCompiledRouteTrack()) {
            return 0;
        }

        double completionRatio = routeTrack.getTotalLength() <= 0.0D ? 0.0D : routeProgress / routeTrack.getTotalLength();
        return Mth.clamp((int) Math.floor(completionRatio * path.size()) + 1, 1, path.size());
    }

    /**
     * Docking owns follower motion while a convoy is settling, parked, or
     * leaving.  Route positioning must not overwrite those explicit poses.
     */
    public boolean updateFollowerForDocking(VesselEntity follower) {
        DockingSession session = dockingSession;
        if (session == null) {
            return false;
        }

        if (dockingState == DockingState.SETTLING || dockingState == DockingState.DOCKED) {
            BlockPos dockPos = session.followerDockPositions.get(follower.getUUID());
            DockingStationBlockEntity dock = dockPos == null ? null : getDockAt(dockPos);
            if (dock == null) {
                return false;
            }

            Vec3 target = dock.getVehicleCenterPos();
            if (dockingState == DockingState.SETTLING) {
                settleFollowerIntoDock(follower, target, session.heading);
            } else {
                parkFollowerAtDock(follower, target, session.heading);
            }
            return true;
        }

        if (dockingState == DockingState.DEPARTING) {
            return follower.getLeader().map(leader -> {
                SpringPhysicsUtil.pullLinkedEntities(
                        leader,
                        follower,
                        WaterConvoyGeometry.cruisingCenterSpacing(leader, follower));
                return true;
            }).orElse(false);
        }

        return false;
    }

    private void settleFollowerIntoDock(VesselEntity follower, Vec3 target, Direction heading) {
        Vec3 current = follower.position();
        Vec3 offset = new Vec3(target.x - current.x, 0.0D, target.z - current.z);
        double distance = offset.length();
        if (distance <= DOCK_SETTLE_EPSILON) {
            parkFollowerAtDock(follower, target, heading);
            return;
        }

        Vec3 step = offset.scale(Math.min(DOCK_SETTLE_STEP, distance) / distance);
        follower.setDeltaMovement(Vec3.ZERO);
        follower.moveTo(current.x + step.x, follower.getY(), current.z + step.z, heading.toYRot(), follower.getXRot());
    }

    private void parkFollowerAtDock(VesselEntity follower, Vec3 target, Direction heading) {
        follower.setDeltaMovement(Vec3.ZERO);
        follower.moveTo(target.x, follower.getY(), target.z, heading.toYRot(), follower.getXRot());
    }

    public boolean updateFollowerOnRoute(VesselEntity follower) {
        if (!hasCompiledRouteTrack()) {
            return false;
        }

        Optional<RouteBodyPose> poseOptional = solveFollowerPoseOnRoute(follower);
        if (poseOptional.isEmpty()) {
            return false;
        }

        RouteBodyPose pose = poseOptional.get();
        Vec3 target = pose.center();
        Vec3 forward = pose.forward();
        double desiredYaw = computeRouteYaw(forward);
        Vec3 currentPosition = follower.position();
        double horizontalError = horizontalDistance(currentPosition, target);

        Vec3 correctedPosition;
        if (horizontalError > FOLLOWER_HARD_SNAP_DISTANCE) {
            correctedPosition = new Vec3(target.x, currentPosition.y, target.z);
        } else {
            correctedPosition = new Vec3(
                    Mth.lerp(FOLLOWER_CORRECTION_BLEND, currentPosition.x, target.x),
                    currentPosition.y,
                    Mth.lerp(FOLLOWER_CORRECTION_BLEND, currentPosition.z, target.z)
            );
        }

        follower.moveTo(correctedPosition.x, follower.getY(), correctedPosition.z, (float) desiredYaw, follower.getXRot());
        follower.setDeltaMovement(forward.scale(getRouteCruiseSpeed()));

        return true;
    }

    private Optional<RouteBodyPose> solveFollowerPoseOnRoute(VesselEntity follower) {
        OptionalDouble centerDistanceOptional = getDistanceToFollowerCenter(follower);
        if (centerDistanceOptional.isEmpty()) {
            return Optional.empty();
        }

        double centerDistance = centerDistanceOptional.getAsDouble();
        double halfLength = WaterConvoyGeometry.routeBodyHalfLength(follower);
        TugRouteTrack.Sample frontSample = routeTrack.sample(routeProgress - centerDistance + halfLength);
        TugRouteTrack.Sample rearSample = routeTrack.sample(routeProgress - centerDistance - halfLength);
        TugRouteTrack.Sample centerSample = routeTrack.sample(routeProgress - centerDistance);

        Vec3 axis = frontSample.position().subtract(rearSample.position());
        Vec3 horizontalAxis = new Vec3(axis.x, 0.0D, axis.z);
        Vec3 forward;
        if (horizontalAxis.lengthSqr() > 1.0E-4D) {
            forward = horizontalAxis.normalize();
        } else {
            Vec3 tangent = frontSample.tangent();
            forward = new Vec3(tangent.x, 0.0D, tangent.z).normalize();
        }

        if (forward.lengthSqr() <= 1.0E-4D) {
            return Optional.empty();
        }

        return Optional.of(new RouteBodyPose(centerSample.position(), forward));
    }

    private OptionalDouble getDistanceToFollowerCenter(VesselEntity target) {
        double distance = 0.0D;
        Optional<VesselEntity> current = this.getFollower();
        VesselEntity previous = this;
        while (current.isPresent()) {
            VesselEntity currentFollower = current.get();
            distance += WaterConvoyGeometry.cruisingCenterSpacing(previous, currentFollower);
            if (currentFollower == target) {
                return OptionalDouble.of(distance);
            }
            previous = currentFollower;
            current = currentFollower.getFollower();
        }
        return OptionalDouble.empty();
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        return Math.hypot(from.x - to.x, from.z - to.z);
    }

    private static float computeRouteYaw(Vec3 velocity) {
        return (float) (Mth.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F;
    }

    private record RouteBodyPose(Vec3 center, Vec3 forward) {
    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level().isClientSide && r != RemovalReason.UNLOADED_TO_CHUNK) {
            var stack = new ItemStack(this.getDropItem());
            if (this.hasCustomName()) {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, this.getCustomName());
            }
            this.spawnAtLocation(stack);
            Containers.dropContents(this.level(), this, this);
            this.spawnAtLocation(routeItemHandler.getStackInSlot(0));
        }
        super.remove(r);
    }

    // Have to implement IInventory to work with hoppers
    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        return null;
    }


    public boolean canPlaceItem(int p_94041_1_, ItemStack p_94041_2_) {
        return true;
    }

    @Override
    public void setChanged() {
        contentsChanged = true;
    }

    @Override
    public boolean isValid(Player p_70300_1_) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(p_70300_1_.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public boolean stillValid(Player p_70300_1_) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(p_70300_1_.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void clearContent() {

    }

    @Override
    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction p_180463_1_) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
        return isDocked();
    }
    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected double swimSpeed() {
        if(this.level().isClientSide){
            return super.swimSpeed();
        }

        if (this.tickCount % 10 == 0){
           swimSpeedMult = computeSpeedMult();
        }

        return swimSpeedMult * super.swimSpeed();
    }

    private double computeSpeedMult(){
        double mult = 1;
        boolean doBreak = false;
        for (int i = 0; i < 10 && !doBreak; i++) {
            for (Direction direction: List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH)) {
                BlockPos pos = this.getOnPos().relative(direction, i);
                if(!this.level().getFluidState(pos).isSource()){
                    doBreak = true;
                    break;
                }
            }
            if(i > 3) {
                mult = 1 + ((i / 10f) * 1.8);
            }
        }
        if(mult < swimSpeedMult) return mult;
        else return (mult + swimSpeedMult * 20) / 21;
    }

    /*
                Stalling Capability
         */
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
}
