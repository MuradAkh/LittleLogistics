package dev.murad.shipping.global;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.network.client.LocoRouteTrackerClientPacket;
import dev.murad.shipping.network.client.LocoRouteTrackerData;
import dev.murad.shipping.network.client.EntityPosition;
import dev.murad.shipping.network.client.TugRouteTrackerClientPacket;
import dev.murad.shipping.network.client.TugRouteTrackerData;
import dev.murad.shipping.network.client.VehicleTrackerClientPacket;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.LinkableEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;


public class PlayerTrainChunkManager extends SavedData {
    private final static TicketType<UUID> TRAVEL_TICKET = TicketType.create("littlelogistics:travelticket", UUID::compareTo);
    private final static TicketType<UUID> LOAD_TICKET = TicketType.create("littlelogistics:loadticket", UUID::compareTo, 500);
    private static final double WRENCH_TUG_ROUTE_SYNC_DISTANCE = 160.0D;
    private static final double WRENCH_TUG_ROUTE_SYNC_DISTANCE_SQR = WRENCH_TUG_ROUTE_SYNC_DISTANCE * WRENCH_TUG_ROUTE_SYNC_DISTANCE;
    private static final int WRENCH_TUG_ROUTE_SNAPSHOT_INTERVAL = 10;
    private static final int MAX_WRENCH_TUG_ROUTE_VERTICES = 16_384;
    private final Set<Entity> enrolled = new HashSet<>();
    private final Set<ChunkPos> tickets = new HashSet<>();
    private final Set<ChunkPos> toLoad = new HashSet<>();
    private final int loadLevel = ShippingConfig.Server.CHUNK_LOADING_LEVEL.get();
    private boolean changed = false;
    private boolean active = false;
    private boolean wrenchTugRouteSnapshotActive;
    private int wrenchTugRouteSnapshotTimer;
    private Map<UUID, TugRouteTrackerState> lastWrenchTugRouteStates = Map.of();
    private boolean wrenchLocoRouteSnapshotActive;
    private int wrenchLocoRouteSnapshotTimer;
    private Map<UUID, TugRouteTrackerState> lastWrenchLocoRouteStates = Map.of();
    @Getter
    private int numVehicles = 0;
    @Getter
    private final UUID uuid;
    @Getter
    private final ServerLevel level;

    private record TugRouteTrackerState(int entityId, int dyeColorId, long routeRevision, int distanceBucket) {
    }

    public static PlayerTrainChunkManager get(ServerLevel level, UUID uuid){
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(() -> new PlayerTrainChunkManager(level, uuid), (tag, provider) -> new PlayerTrainChunkManager(tag, level, uuid)), "littlelogistics:chunkmanager-" + uuid.toString());
    }

    public static Optional<PlayerTrainChunkManager> getSaved(ServerLevel level, UUID uuid){
        DimensionDataStorage storage = level.getDataStorage();
        return Optional.ofNullable(storage.get(new SavedData.Factory<>(() -> new PlayerTrainChunkManager(level, uuid), (tag, provider) -> new PlayerTrainChunkManager(tag, level, uuid)), "littlelogistics:chunkmanager-" + uuid.toString()));
    }

    public static boolean enroll(Entity entity, UUID uuid){
        if(!entity.level().isClientSide) {
            var manager = PlayerTrainChunkManager.get((ServerLevel) entity.level(), uuid);
            if(!manager.active){
                return false;
            }
            manager.enrolled.add(entity);
            manager.changed = true;
            return true;
        }
        return false;
    }

    public static boolean enrollIfAllowed(Entity entity, UUID uuid){
        if(!entity.level().isClientSide) {
            var manager = PlayerTrainChunkManager.get((ServerLevel) entity.level(), uuid);
            Player player = manager.level.getPlayerByUUID(uuid);
            if(player == null){
                return false;
            }
            int max = ShippingConfig.Server.MAX_REGISTRERED_VEHICLES_PER_PLAYER.get();
            int registered = TrainChunkManagerManager.get(manager.level.getServer()).countVehicles(uuid) + 1;
            if(registered > max){
                player.sendSystemMessage(Component.translatable("global.littlelogistics.locomotive.register_success", max));
                return false;
            } else {
                player.sendSystemMessage(Component.translatable("global.littlelogistics.locomotive.register_fail", registered, max));
                manager.enrolled.add(entity);
                manager.changed = true;
                return true;
            }
        }
        return false;
    }

    public void deactivate(){
        resetTugRouteTracker();
        updateToLoad();
        numVehicles = enrolled.size();
        enrolled.clear();
        tickets.forEach(chunkPos -> level.getChunkSource().removeRegionTicket(TRAVEL_TICKET, chunkPos, loadLevel, uuid));
        tickets.clear();
        active = false;
    }

    public void activate(){
        resetTugRouteTracker();
        active = true;
        level.getServer().execute(() -> {
            toLoad.forEach(chunkPos -> level.getChunkSource().addRegionTicket(LOAD_TICKET, chunkPos, 2, uuid));
        });
    }

    private List<Entity> getAllSubjectEntities(Entity entity){
        List<Entity> subjects = new ArrayList<>();
        subjects.add(entity);
        if(entity instanceof LinkableEntity<?> l){ // need to refactor this somehow to be more generic
           for(var e : l.getTrain().asListOfTugged()){
               if(e instanceof Entity entity1){
                   subjects.add(entity1);
                   subjects.addAll(entity1.getPassengers());
               }
           }
        }

        if(entity.getParts() != null){
            subjects.addAll(List.of(entity.getParts()));
        }
        return subjects;
    }

    private void updateToLoad() {
        toLoad.clear();
        enrolled.forEach(e -> toLoad.addAll(getAllSubjectEntities(e).stream().map(Entity::chunkPosition).collect(Collectors.toSet())));
    }



    public void tick(){
        boolean changed = enrolled.removeIf(e -> !e.isAlive());
        if(!active){
            return;
        }

        enrolled.forEach(entityHead -> getAllSubjectEntities(entityHead)
                .stream()
                .filter(entity -> !((ServerLevel) entity.level()).isPositionEntityTicking(entity.blockPosition()))
                .forEach(Entity::tick));

        Player player = level.getPlayerByUUID(uuid);
        if(player instanceof ServerPlayer serverPlayer && serverPlayer.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(ModItems.CONDUCTORS_WRENCH.get())) {
            PacketDistributor.sendToPlayer(serverPlayer, VehicleTrackerClientPacket.of(getEntityPositions(), level.dimension().toString()));
            tickTugRouteTracker(serverPlayer);
            tickLocoRouteTracker(serverPlayer);
        } else {
            resetTugRouteTracker();
        }

        if(this.changed || changed || enrolled.stream()
                .map(e -> !e.chunkPosition().equals(new ChunkPos(BlockPos.containing(e.xOld, e.yOld, e.zOld))))
                .reduce(Boolean.FALSE, Boolean::logicalOr)){
            this.changed = false;
            level.getServer().execute(this::onChanged);

        }
    }

    public List<EntityPosition> getEntityPositions(){
        return enrolled.stream().map(entity ->
                new EntityPosition(entity.getType().toString(), entity.getId(), entity.position(), new Vec3(entity.xOld, entity.yOld, entity.zOld)))
                .collect(Collectors.toList());
    }

    /**
     * Positions need to be fresh every tick, but complete tug routes are static. Send a full
     * replacement snapshot only when a nearby tug's visible route state changes.
     */
    private void tickTugRouteTracker(ServerPlayer player) {
        boolean firstSnapshot = !wrenchTugRouteSnapshotActive;
        if (!firstSnapshot && ++wrenchTugRouteSnapshotTimer < WRENCH_TUG_ROUTE_SNAPSHOT_INTERVAL) {
            return;
        }
        wrenchTugRouteSnapshotTimer = 0;

        List<AbstractTugEntity> nearbyTugs = enrolled.stream()
            .filter(AbstractTugEntity.class::isInstance)
            .map(AbstractTugEntity.class::cast)
            .filter(tug -> tug.distanceToSqr(player) <= WRENCH_TUG_ROUTE_SYNC_DISTANCE_SQR)
            .sorted(Comparator.comparingDouble(tug -> tug.distanceToSqr(player)))
            .toList();

        Map<UUID, TugRouteTrackerState> visibleStates = new HashMap<>();
        for (AbstractTugEntity tug : nearbyTugs) {
            int dyeColorId = tug.getColor() == null ? DyeColor.RED.getId() : tug.getColor();
            int distanceBucket = (int) (Math.sqrt(tug.distanceToSqr(player)) / 16.0D);
            visibleStates.put(tug.getUUID(), new TugRouteTrackerState(tug.getId(), dyeColorId,
                tug.getRouteOverlayRevision(), distanceBucket));
        }

        if (!firstSnapshot && visibleStates.equals(lastWrenchTugRouteStates)) {
            return;
        }

        int remainingVertices = MAX_WRENCH_TUG_ROUTE_VERTICES;
        List<TugRouteTrackerData> routes = new ArrayList<>();
        for (AbstractTugEntity tug : nearbyTugs) {
            Optional<TugRouteTrackerData> route = TugRouteTrackerData.fromTug(tug);
            if (route.isEmpty() || route.get().pathVertices().size() > remainingVertices) {
                continue;
            }
            routes.add(route.get());
            remainingVertices -= route.get().pathVertices().size();
        }

        PacketDistributor.sendToPlayer(player,
            new TugRouteTrackerClientPacket(level.dimension().toString(), routes));
        lastWrenchTugRouteStates = Map.copyOf(visibleStates);
        wrenchTugRouteSnapshotActive = true;
    }

    private void resetTugRouteTracker() {
        wrenchTugRouteSnapshotActive = false;
        wrenchTugRouteSnapshotTimer = 0;
        lastWrenchTugRouteStates = Map.of();
        wrenchLocoRouteSnapshotActive = false;
        wrenchLocoRouteSnapshotTimer = 0;
        lastWrenchLocoRouteStates = Map.of();
    }

    private void tickLocoRouteTracker(ServerPlayer player) {
        boolean firstSnapshot = !wrenchLocoRouteSnapshotActive;
        if (!firstSnapshot && ++wrenchLocoRouteSnapshotTimer < WRENCH_TUG_ROUTE_SNAPSHOT_INTERVAL) return;
        wrenchLocoRouteSnapshotTimer = 0;

        List<AbstractLocomotiveEntity> nearbyLocos = enrolled.stream()
            .filter(AbstractLocomotiveEntity.class::isInstance)
            .map(AbstractLocomotiveEntity.class::cast)
            .filter(loco -> loco.distanceToSqr(player) <= WRENCH_TUG_ROUTE_SYNC_DISTANCE_SQR)
            .sorted(Comparator.comparingDouble(loco -> loco.distanceToSqr(player)))
            .toList();
        Map<UUID, TugRouteTrackerState> visibleStates = new HashMap<>();
        for (AbstractLocomotiveEntity loco : nearbyLocos) {
            int dyeColorId = loco.getColor() == null ? DyeColor.RED.getId() : loco.getColor();
            int distanceBucket = (int) (Math.sqrt(loco.distanceToSqr(player)) / 16.0D);
            visibleStates.put(loco.getUUID(), new TugRouteTrackerState(loco.getId(), dyeColorId,
                loco.getRouteOverlayRevision(), distanceBucket));
        }
        if (!firstSnapshot && visibleStates.equals(lastWrenchLocoRouteStates)) return;

        int remainingVertices = MAX_WRENCH_TUG_ROUTE_VERTICES;
        List<LocoRouteTrackerData> routes = new ArrayList<>();
        for (AbstractLocomotiveEntity loco : nearbyLocos) {
            Optional<LocoRouteTrackerData> route = LocoRouteTrackerData.fromLocomotive(loco);
            if (route.isEmpty() || route.get().pathVertices().size() > remainingVertices) continue;
            routes.add(route.get());
            remainingVertices -= route.get().pathVertices().size();
        }
        PacketDistributor.sendToPlayer(player,
            new LocoRouteTrackerClientPacket(level.dimension().toString(), routes));
        lastWrenchLocoRouteStates = Map.copyOf(visibleStates);
        wrenchLocoRouteSnapshotActive = true;
    }

    private void onChanged() {
        Set<ChunkPos> required = new HashSet<>();
        numVehicles = enrolled.size();
        if(ShippingConfig.Server.DISABLE_CHUNK_MANAGEMENT.get()){
            removeUnneededTickets(required);
            return;
        }
        enrolled.stream().map(this::computeRequiredTickets).forEach(required::addAll);
        removeUnneededTickets(required);
        addNeededTickets(required);
        updateToLoad();
        setDirty();
    }

    private Set<ChunkPos> computeRequiredTickets(Entity entity) {
        var set = new HashSet<ChunkPos>();
        getAllSubjectEntities(entity).stream()
                .map(Entity::chunkPosition)
                .map(pos -> ChunkPos.rangeClosed(pos, 1))
                .forEach(pos -> set.addAll(pos.collect(Collectors.toList())));

        return set;
    }

    private void removeUnneededTickets(Set<ChunkPos> required){
        Set.copyOf(tickets)
                .stream()
                .filter(pos -> !required.contains(pos))
                .forEach(chunkPos -> {
                    level.getChunkSource().removeRegionTicket(TRAVEL_TICKET, chunkPos, loadLevel, uuid);
                    tickets.remove(chunkPos);
                });
    }

    private void addNeededTickets(Set<ChunkPos> required){
        required
            .stream()
            .filter(pos -> !tickets.contains(pos))
                .collect(Collectors.toSet()) // avoid mutation on the go
                .forEach(chunkPos -> {
                    level.getChunkSource().addRegionTicket(TRAVEL_TICKET, chunkPos, loadLevel, uuid);
                    tickets.add(chunkPos);
                });
    }


    PlayerTrainChunkManager(ServerLevel level, UUID uuid){
        this.level = level;
        this.uuid = uuid;
        TrainChunkManagerManager.get(level.getServer()).enroll(this);
        // active when creating a new one
        active = true;
        setDirty();
    }

    PlayerTrainChunkManager(CompoundTag tag, ServerLevel level, UUID uuid){
        this.level = level;
        this.uuid = uuid;
        numVehicles = tag.getInt("numVehicles");
        Arrays.stream(tag.getLongArray("chunksToLoad")).forEach(chunk -> toLoad.add(new ChunkPos(chunk)));
        if(ShippingConfig.Server.OFFLINE_LOADING.get()){
            activate();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("numVehicles", numVehicles);
        tag.putLongArray("chunksToLoad", toLoad.stream().map(ChunkPos::toLong).collect(Collectors.toList()));
        return tag;
    }

    public boolean isActive() {
        return active;
    }
}
