package dev.murad.shipping.global;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.network.client.EntityPosition;
import dev.murad.shipping.network.client.LocoRouteTrackerClientPacket;
import dev.murad.shipping.network.client.LocoRouteTrackerData;
import dev.murad.shipping.network.client.TugRouteTrackerClientPacket;
import dev.murad.shipping.network.client.TugRouteTrackerData;
import dev.murad.shipping.network.client.VehicleTrackerClientPacket;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authoritative registry and runtime ticket scheduler for owned head vehicles.
 * Registration is independent of whether managed chunk loading is enabled.
 */
public final class VehicleRegistrationData extends SavedData {
    private static final String DATA_NAME = "littlelogistics_vehicle_registrations";
    private static final TicketType<UUID> VEHICLE_TICKET =
        TicketType.create("littlelogistics:vehicle", UUID::compareTo);
    private static final double TRACKING_DISTANCE_SQR = 160.0D * 160.0D;
    private static final int ROUTE_SNAPSHOT_INTERVAL = 10;
    private static final int MAX_TRACKED_VERTICES = 16_384;

    private record RouteState(int entityId, int dyeColor, long revision, int distanceBucket) {}

    private static final class Registration {
        private final UUID vehicle;
        private UUID owner;
        private String dimension;
        private Set<ChunkPos> lastFootprint;

        private Registration(UUID vehicle, UUID owner, String dimension, Set<ChunkPos> lastFootprint) {
            this.vehicle = vehicle;
            this.owner = owner;
            this.dimension = dimension;
            this.lastFootprint = new HashSet<>(lastFootprint);
        }
    }

    private static final class TicketState {
        private final Set<ChunkPos> held = new HashSet<>();
        private final Map<ChunkPos, Long> lastRequired = new HashMap<>();
        private int ticketLevel;
        private boolean waitingForEntity;
        private boolean ready;
    }

    private final MinecraftServer server;
    private final Map<UUID, Registration> registrations = new HashMap<>();
    private final Map<String, Map<UUID, TicketState>> runtimeTickets = new HashMap<>();
    private final Map<UUID, Map<UUID, RouteState>> lastTugRoutes = new HashMap<>();
    private final Map<UUID, Map<UUID, RouteState>> lastLocoRoutes = new HashMap<>();
    private final Map<UUID, Integer> routeSnapshotTimers = new HashMap<>();

    public static VehicleRegistrationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(() -> new VehicleRegistrationData(server),
                (tag, provider) -> new VehicleRegistrationData(tag, server)), DATA_NAME);
    }

    private VehicleRegistrationData(MinecraftServer server) {
        this.server = server;
    }

    private VehicleRegistrationData(CompoundTag tag, MinecraftServer server) {
        this.server = server;
        ListTag vehicles = tag.getList("vehicles", Tag.TAG_COMPOUND);
        for (int index = 0; index < vehicles.size(); index++) {
            CompoundTag vehicleTag = vehicles.getCompound(index);
            if (!vehicleTag.hasUUID("vehicle") || !vehicleTag.hasUUID("owner")) continue;
            Set<ChunkPos> footprint = new HashSet<>();
            for (long packed : vehicleTag.getLongArray("footprint")) {
                footprint.add(new ChunkPos(packed));
            }
            UUID vehicle = vehicleTag.getUUID("vehicle");
            registrations.put(vehicle, new Registration(vehicle, vehicleTag.getUUID("owner"),
                vehicleTag.getString("dimension"), footprint));
        }
    }

    public void register(Entity entity, UUID owner) {
        String dimension = entity.level().dimension().location().toString();
        Registration existing = registrations.get(entity.getUUID());
        if (existing == null) {
            registrations.put(entity.getUUID(), new Registration(entity.getUUID(), owner, dimension,
                Set.of(entity.chunkPosition())));
            setDirty();
            return;
        }

        if (!existing.owner.equals(owner) || !existing.dimension.equals(dimension)) {
            releaseEverywhere(existing.vehicle);
            existing.owner = owner;
            existing.dimension = dimension;
            setDirty();
        }
    }

    public void unregister(UUID vehicle) {
        if (registrations.remove(vehicle) != null) {
            releaseEverywhere(vehicle);
            setDirty();
        }
    }

    public boolean mayRun(UUID owner) {
        return ShippingConfig.Server.OFFLINE_LOADING.get()
            || server.getPlayerList().getPlayer(owner) != null;
    }

    public boolean mayVehicleMove(Entity vehicle, UUID owner) {
        if (!mayRun(owner)) return false;
        if (!ShippingConfig.Server.MANAGED_VEHICLE_LOADING.get()
            || !(vehicle instanceof HeadVehicle head) || !head.isManagedServiceActive()) {
            return true;
        }
        Map<UUID, TicketState> dimensionTickets = runtimeTickets.get(vehicle.level().dimension().location().toString());
        TicketState state = dimensionTickets == null ? null : dimensionTickets.get(vehicle.getUUID());
        return state != null && state.ready;
    }

    public boolean isManaging(Entity vehicle) {
        Map<UUID, TicketState> dimensionTickets = runtimeTickets.get(vehicle.level().dimension().location().toString());
        TicketState state = dimensionTickets == null ? null : dimensionTickets.get(vehicle.getUUID());
        return state != null && !state.held.isEmpty();
    }

    public void tick(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        Set<UUID> presentRecords = new HashSet<>();
        List<Registration> dimensionRecords = registrations.values().stream()
            .filter(registration -> registration.dimension.equals(dimension))
            .toList();

        for (Registration registration : dimensionRecords) {
            presentRecords.add(registration.vehicle);
            tickRegistration(level, registration);
        }

        Map<UUID, TicketState> states = runtimeTickets.get(dimension);
        if (states != null) {
            for (UUID stale : new HashSet<>(states.keySet())) {
                if (!presentRecords.contains(stale)) {
                    release(level, stale);
                }
            }
        }
        tickWrenchTracking(level, dimensionRecords);
    }

    private void tickWrenchTracking(ServerLevel level, List<Registration> dimensionRecords) {
        for (ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.CONDUCTORS_WRENCH.get())) {
                lastTugRoutes.remove(playerId);
                lastLocoRoutes.remove(playerId);
                routeSnapshotTimers.remove(playerId);
                continue;
            }

            List<Entity> vehicles = dimensionRecords.stream()
                .filter(registration -> registration.owner.equals(playerId))
                .map(registration -> level.getEntity(registration.vehicle))
                .filter(Objects::nonNull)
                .toList();
            List<EntityPosition> positions = vehicles.stream()
                .map(entity -> new EntityPosition(entity.getType().toString(), entity.getId(), entity.position(),
                    new Vec3(entity.xOld, entity.yOld, entity.zOld)))
                .toList();
            PacketDistributor.sendToPlayer(player,
                VehicleTrackerClientPacket.of(positions, level.dimension().toString()));

            int timer = routeSnapshotTimers.getOrDefault(playerId, 0) + 1;
            if (timer < ROUTE_SNAPSHOT_INTERVAL && lastTugRoutes.containsKey(playerId)) {
                routeSnapshotTimers.put(playerId, timer);
                continue;
            }
            routeSnapshotTimers.put(playerId, 0);
            sendTugRoutes(player, vehicles);
            sendLocoRoutes(player, vehicles);
        }
    }

    private void sendTugRoutes(ServerPlayer player, List<Entity> vehicles) {
        List<AbstractTugEntity> tugs = vehicles.stream()
            .filter(AbstractTugEntity.class::isInstance)
            .map(AbstractTugEntity.class::cast)
            .filter(tug -> tug.distanceToSqr(player) <= TRACKING_DISTANCE_SQR)
            .sorted(Comparator.comparingDouble(tug -> tug.distanceToSqr(player)))
            .toList();
        Map<UUID, RouteState> states = new HashMap<>();
        for (AbstractTugEntity tug : tugs) {
            int color = tug.getColor() == null ? DyeColor.RED.getId() : tug.getColor();
            states.put(tug.getUUID(), new RouteState(tug.getId(), color, tug.getRouteOverlayRevision(),
                (int) (Math.sqrt(tug.distanceToSqr(player)) / 16.0D)));
        }
        if (states.equals(lastTugRoutes.get(player.getUUID()))) return;

        int remaining = MAX_TRACKED_VERTICES;
        List<TugRouteTrackerData> routes = new ArrayList<>();
        for (AbstractTugEntity tug : tugs) {
            var route = TugRouteTrackerData.fromTug(tug);
            if (route.isPresent() && route.get().pathVertices().size() <= remaining) {
                routes.add(route.get());
                remaining -= route.get().pathVertices().size();
            }
        }
        PacketDistributor.sendToPlayer(player,
            new TugRouteTrackerClientPacket(player.level().dimension().toString(), routes));
        lastTugRoutes.put(player.getUUID(), Map.copyOf(states));
    }

    private void sendLocoRoutes(ServerPlayer player, List<Entity> vehicles) {
        List<AbstractLocomotiveEntity> locomotives = vehicles.stream()
            .filter(AbstractLocomotiveEntity.class::isInstance)
            .map(AbstractLocomotiveEntity.class::cast)
            .filter(locomotive -> locomotive.distanceToSqr(player) <= TRACKING_DISTANCE_SQR)
            .sorted(Comparator.comparingDouble(locomotive -> locomotive.distanceToSqr(player)))
            .toList();
        Map<UUID, RouteState> states = new HashMap<>();
        for (AbstractLocomotiveEntity locomotive : locomotives) {
            int color = locomotive.getColor() == null ? DyeColor.RED.getId() : locomotive.getColor();
            states.put(locomotive.getUUID(), new RouteState(locomotive.getId(), color,
                locomotive.getRouteOverlayRevision(),
                (int) (Math.sqrt(locomotive.distanceToSqr(player)) / 16.0D)));
        }
        if (states.equals(lastLocoRoutes.get(player.getUUID()))) return;

        int remaining = MAX_TRACKED_VERTICES;
        List<LocoRouteTrackerData> routes = new ArrayList<>();
        for (AbstractLocomotiveEntity locomotive : locomotives) {
            var route = LocoRouteTrackerData.fromLocomotive(locomotive);
            if (route.isPresent() && route.get().pathVertices().size() <= remaining) {
                routes.add(route.get());
                remaining -= route.get().pathVertices().size();
            }
        }
        PacketDistributor.sendToPlayer(player,
            new LocoRouteTrackerClientPacket(player.level().dimension().toString(), routes));
        lastLocoRoutes.put(player.getUUID(), Map.copyOf(states));
    }

    private void tickRegistration(ServerLevel level, Registration registration) {
        if (!ShippingConfig.Server.MANAGED_VEHICLE_LOADING.get() || !mayRun(registration.owner)) {
            release(level, registration.vehicle);
            return;
        }

        Entity entity = level.getEntity(registration.vehicle);
        if (entity == null) {
            // Wake the saved location and its immediate guard chunks. Ticket admission is asynchronous.
            Set<ChunkPos> bootstrap = expandFootprint(registration.lastFootprint);
            TicketState state = applyTickets(level, registration.vehicle, bootstrap);
            state.waitingForEntity = true;
            state.ready = false;
            return;
        }
        if (!(entity instanceof HeadVehicle head)) {
            unregister(registration.vehicle);
            return;
        }

        Set<Entity> subjects = collectSubjects(entity);
        Set<ChunkPos> footprint = new HashSet<>();
        for (Entity subject : subjects) {
            if (subject.level() == level && !subject.isRemoved()) {
                footprint.add(subject.chunkPosition());
            }
        }
        updateFootprint(registration, footprint);

        Set<ChunkPos> physicalGuard = expandFootprint(footprint);
        TicketState previousState = getTicketState(level, registration.vehicle);
        if (previousState != null && previousState.waitingForEntity) {
            TicketState bootstrapState = applyTickets(level, registration.vehicle, physicalGuard);
            bootstrapState.ready = areChunksReady(level, physicalGuard);
            if (!bootstrapState.ready) return;

            // Tugs deliberately restore with their engine off until this first server tick rebuilds
            // the compiled track and approach. Do not release their wake ticket before that happens.
            tickSubjectsOutsideEntityRange(level, subjects);
            bootstrapState.waitingForEntity = false;
        }

        if (!head.isManagedServiceActive()) {
            release(level, registration.vehicle);
            return;
        }

        Set<ChunkPos> required = new HashSet<>(physicalGuard);
        int maxSteps = Math.max(32, ShippingConfig.Server.ROUTE_LOOKAHEAD_SECONDS.get() * 20);
        List<ChunkPos> upcoming = head.getUpcomingRouteChunks(maxSteps);
        required.addAll(upcoming);
        TicketState state = applyTickets(level, registration.vehicle, required);

        Set<ChunkPos> movementGuard = new HashSet<>(physicalGuard);
        upcoming.stream().limit(3).forEach(movementGuard::add);
        state.ready = areChunksReady(level, movementGuard);
        if (!state.ready) return;

        // Ticket level zero intentionally leaves ordinary entities unticked. Keep LL consists moving.
        tickSubjectsOutsideEntityRange(level, subjects);
    }

    private Set<ChunkPos> expandFootprint(Set<ChunkPos> footprint) {
        Set<ChunkPos> expanded = new HashSet<>();
        for (ChunkPos occupied : footprint) {
            ChunkPos.rangeClosed(occupied, 1).forEach(expanded::add);
        }
        return expanded;
    }

    private boolean areChunksReady(ServerLevel level, Set<ChunkPos> chunks) {
        return chunks.stream().allMatch(chunk -> level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null);
    }

    private void tickSubjectsOutsideEntityRange(ServerLevel level, Set<Entity> subjects) {
        for (Entity subject : subjects) {
            if (!subject.isRemoved() && subject.level() == level
                && !level.isPositionEntityTicking(subject.blockPosition())) {
                subject.tick();
            }
        }
    }

    private Set<Entity> collectSubjects(Entity head) {
        Set<Entity> subjects = new LinkedHashSet<>();
        subjects.add(head);
        if (head instanceof LinkableEntity<?> linkable) {
            for (Object linked : linkable.getTrain().asListOfTugged()) {
                if (linked instanceof Entity entity) subjects.add(entity);
            }
        }
        for (Entity subject : new ArrayList<>(subjects)) {
            subjects.addAll(subject.getPassengers());
            if (subject.getParts() != null) subjects.addAll(List.of(subject.getParts()));
        }
        return subjects;
    }

    private void updateFootprint(Registration registration, Set<ChunkPos> footprint) {
        if (!footprint.isEmpty() && !registration.lastFootprint.equals(footprint)) {
            registration.lastFootprint = new HashSet<>(footprint);
            setDirty();
        }
    }

    private TicketState applyTickets(ServerLevel level, UUID vehicle, Set<ChunkPos> required) {
        String dimension = level.dimension().location().toString();
        TicketState state = runtimeTickets.computeIfAbsent(dimension, ignored -> new HashMap<>())
            .computeIfAbsent(vehicle, ignored -> new TicketState());
        int ticketLevel = ShippingConfig.Server.CHUNK_LOADING_LEVEL.get();
        long now = level.getGameTime();

        if (!state.held.isEmpty() && state.ticketLevel != ticketLevel) {
            for (ChunkPos chunk : state.held) {
                level.getChunkSource().removeRegionTicket(VEHICLE_TICKET, chunk, state.ticketLevel, vehicle);
            }
            state.held.clear();
            state.lastRequired.clear();
            state.ready = false;
        }
        state.ticketLevel = ticketLevel;

        for (ChunkPos chunk : required) {
            state.lastRequired.put(chunk, now);
            if (state.held.add(chunk)) {
                level.getChunkSource().addRegionTicket(VEHICLE_TICKET, chunk, ticketLevel, vehicle);
            }
        }

        int grace = ShippingConfig.Server.CHUNK_RELEASE_GRACE_TICKS.get();
        for (ChunkPos chunk : new HashSet<>(state.held)) {
            long lastRequired = state.lastRequired.getOrDefault(chunk, now);
            if (!required.contains(chunk) && now - lastRequired >= grace) {
                level.getChunkSource().removeRegionTicket(VEHICLE_TICKET, chunk, ticketLevel, vehicle);
                state.held.remove(chunk);
                state.lastRequired.remove(chunk);
            }
        }
        return state;
    }

    private TicketState getTicketState(ServerLevel level, UUID vehicle) {
        Map<UUID, TicketState> states = runtimeTickets.get(level.dimension().location().toString());
        return states == null ? null : states.get(vehicle);
    }

    private void release(ServerLevel level, UUID vehicle) {
        String dimension = level.dimension().location().toString();
        Map<UUID, TicketState> states = runtimeTickets.get(dimension);
        TicketState state = states == null ? null : states.remove(vehicle);
        if (state == null) return;
        for (ChunkPos chunk : state.held) {
            level.getChunkSource().removeRegionTicket(VEHICLE_TICKET, chunk, state.ticketLevel, vehicle);
        }
        if (states.isEmpty()) runtimeTickets.remove(dimension);
    }

    private void releaseEverywhere(UUID vehicle) {
        for (ServerLevel level : server.getAllLevels()) {
            release(level, vehicle);
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag vehicles = new ListTag();
        for (Registration registration : registrations.values()) {
            CompoundTag vehicle = new CompoundTag();
            vehicle.putUUID("vehicle", registration.vehicle);
            vehicle.putUUID("owner", registration.owner);
            vehicle.putString("dimension", registration.dimension);
            vehicle.putLongArray("footprint", registration.lastFootprint.stream().mapToLong(ChunkPos::toLong).toArray());
            vehicles.add(vehicle);
        }
        tag.put("vehicles", vehicles);
        return tag;
    }
}
