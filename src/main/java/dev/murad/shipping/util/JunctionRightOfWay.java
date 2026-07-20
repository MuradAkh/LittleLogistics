package dev.murad.shipping.util;

import java.util.Objects;
import java.util.UUID;

/** Deterministic priority for locomotive heads contending for the same junction. */
public final class JunctionRightOfWay {
    private JunctionRightOfWay() {
    }

    public record Claim(boolean ownsReservation, boolean exitClear, UUID locomotiveId) {
        public Claim {
            Objects.requireNonNull(locomotiveId);
        }
    }

    /**
     * Reservation ownership wins first, followed by a clear exit. UUID ordering is the stable
     * final tie-breaker; unlike runtime entity IDs, it survives entity reloads.
     */
    public static boolean wins(Claim self, Claim other) {
        if (self.ownsReservation() != other.ownsReservation()) {
            return self.ownsReservation();
        }
        if (self.exitClear() != other.exitClear()) {
            return self.exitClear();
        }
        return self.locomotiveId().compareTo(other.locomotiveId()) < 0;
    }
}
