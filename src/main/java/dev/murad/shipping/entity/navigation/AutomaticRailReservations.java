package dev.murad.shipping.entity.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-thread reservation table for automatic branching rails. Reservations are transient:
 * their purpose is to keep one branch selected from the time a locomotive approaches until its
 * final linked car clears the rail.
 */
final class AutomaticRailReservations {
    private record Reservation(UUID owner, long expiresAt) {
    }

    private static final Map<Level, Map<BlockPos, Reservation>> BY_LEVEL = new WeakHashMap<>();

    private AutomaticRailReservations() {
    }

    static boolean acquire(Level level, BlockPos pos, UUID owner, long expiresAt) {
        Map<BlockPos, Reservation> reservations = BY_LEVEL.computeIfAbsent(level, ignored -> new HashMap<>());
        Reservation existing = reservations.get(pos);
        long now = level.getGameTime();
        if (existing != null && existing.expiresAt() > now && !existing.owner().equals(owner)) {
            return false;
        }
        reservations.put(pos.immutable(), new Reservation(owner, expiresAt));
        return true;
    }

    static Optional<UUID> owner(Level level, BlockPos pos) {
        Map<BlockPos, Reservation> reservations = BY_LEVEL.get(level);
        if (reservations == null) return Optional.empty();

        Reservation existing = reservations.get(pos);
        if (existing == null) return Optional.empty();
        if (existing.expiresAt() <= level.getGameTime()) {
            reservations.remove(pos);
            if (reservations.isEmpty()) {
                BY_LEVEL.remove(level);
            }
            return Optional.empty();
        }
        return Optional.of(existing.owner());
    }

    static void release(Level level, BlockPos pos, UUID owner) {
        Map<BlockPos, Reservation> reservations = BY_LEVEL.get(level);
        if (reservations == null) return;
        Reservation existing = reservations.get(pos);
        if (existing != null && existing.owner().equals(owner)) {
            reservations.remove(pos);
        }
        if (reservations.isEmpty()) {
            BY_LEVEL.remove(level);
        }
    }
}
