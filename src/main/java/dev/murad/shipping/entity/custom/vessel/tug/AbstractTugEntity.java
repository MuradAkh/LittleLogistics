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

    private int dockHoldTicks = 0;
    private int postUndockTicks = 0;
    private boolean independentMotion = false;
    private int pathfindCooldown = 0;
    private VehicleFrontPart frontHitbox;
    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);



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
    protected int nextStop;

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
                return stack.getItem() instanceof TugRouteItem;
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
        nextStop = compound.contains("next_stop") ? compound.getInt("next_stop") : 0;
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
        compound.putInt("next_stop", nextStop);
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
            this.setPath(TugRouteItem.getRoute(stack));
            contentsChanged = false;
        }

        // fix for currently borked worlds
        if (nextStop >= this.path.size()) {
            this.nextStop = 0;
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

    private List<Direction> getSideDirections() {
        return this.getDirection() == Direction.NORTH || this.getDirection() == Direction.SOUTH ?
                Arrays.asList(Direction.EAST, Direction.WEST) :
                Arrays.asList(Direction.NORTH, Direction.SOUTH);
    }


    private void tickCheckDock() {
        int x = (int) Math.floor(this.getX());
        int y = (int) Math.floor(this.getY());
        int z = (int) Math.floor(this.getZ());

        boolean wasDocked = this.isDocked();

        if (wasDocked && dockHoldTicks > 0) {
            dockHoldTicks--;
            this.setDeltaMovement(Vec3.ZERO);
            this.moveTo(x + 0.5, getY(), z + 0.5);
            return;
        }

        if (postUndockTicks > 0) {
            postUndockTicks--;
            return;
        }

        DockingStationBlockEntity dock = findAdjacentDock();

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
            dockHoldTicks = dock != null ? 5 : 20;
            this.dock(x + 0.5, getY(), z + 0.5);
        } else {
            if (changedUndock && dock != null) {
                vacateAllDocks();
                postUndockTicks = 10;
            }
            dockHoldTicks = 0;
            this.undock();
        }

        if (changedDock) onDock();
        if (changedUndock) onUndock();
    }

    @Nullable
    private DockingStationBlockEntity findAdjacentDock() {
        BlockPos pos = this.blockPosition();
        for (Direction dir : getSideDirections()) {
            // Check same level and one above — the controller sits on the canal bank
            // which is typically one block higher than the water the tug occupies.
            for (BlockPos candidate : new BlockPos[]{pos.relative(dir), pos.above().relative(dir)}) {
                BlockEntity be = level().getBlockEntity(candidate);
                if (be instanceof DockingStationBlockEntity dockBE) {
                    return dockBE;
                }
            }
        }
        return null;
    }

    private boolean isDockChainHolding() {
        DockingStationBlockEntity headDock = findAdjacentDock();
        if (headDock == null) return false;
        if (headDock.isHolding()) return true;

        for (DockingStationBlockEntity dock : headDock.getFollowerDocks(this.getDirection())) {
            if (dock.isHolding()) return true;
        }
        return false;
    }

    private void occupyFollowerDocks() {
        DockingStationBlockEntity headDock = findAdjacentDock();
        if (headDock == null) return;

        // Get ordered dock chain behind the head dock
        List<DockingStationBlockEntity> followerDocks = headDock.getFollowerDocks(this.getDirection());

        // Walk follower vehicle chain and zip with dock chain by index
        Optional<VesselEntity> follower = this.getFollower();
        int index = 0;
        while (follower.isPresent() && index < followerDocks.size()) {
            DockingStationBlockEntity dock = followerDocks.get(index);
            Entity followerEntity = follower.get();
            Entity current = dock.getDockedVehicle();
            if (current != followerEntity) {
                if (current != null) dock.vacateDock();
                dock.occupyDock(followerEntity);
            }
            follower = follower.get().getFollower();
            index++;
        }
    }

    private void vacateAllDocks() {
        DockingStationBlockEntity headDock = findAdjacentDock();
        if (headDock == null) return;
        headDock.vacateDock();

        for (DockingStationBlockEntity dock : headDock.getFollowerDocks(this.getDirection())) {
            dock.vacateDock();
        }
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
                followGuideRail();

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
        pathfindCooldown--;
        if (!this.path.isEmpty() && !this.docked && engineOn && !shouldFreezeTrain() && tickFuel()) {
            TugRouteNode stop = path.get(nextStop);
            if (navigation.getPath() == null || navigation.getPath().isDone()
            ) {
                if(pathfindCooldown < 0 || navigation.getPath() != null){  //only go on cooldown when the path was not completed
                    navigation.moveTo(stop.getX(), this.getY(), stop.getZ(), 0.3);
                    pathfindCooldown = 20;
                } else {
                    return;
                }
            }
            double distance = Math.abs(Math.hypot(this.getX() - (stop.getX() + 0.5), this.getZ() - (stop.getZ() + 0.5)));
            independentMotion = true;
            entityData.set(INDEPENDENT_MOTION, true);

            if (distance < 0.9) {
                incrementStop();
            }

        } else{
            entityData.set(INDEPENDENT_MOTION, false);
            this.navigation.stop();
            if (remainingStallTime > 0){
                remainingStallTime--;
            }

            if (this.path.isEmpty()){
                this.nextStop = 0;
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
        if (!this.path.isEmpty() && !this.path.equals(path)){
            this.nextStop = 0;
        }
        this.path = path;
    }

    private void incrementStop() {
        if (this.path.size() == 1) {
            nextStop = 0;
        } else if (!this.path.isEmpty()) {
            nextStop = (nextStop + 1) % (this.path.size());
        }
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
