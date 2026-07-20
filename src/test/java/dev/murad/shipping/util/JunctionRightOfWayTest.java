package dev.murad.shipping.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JunctionRightOfWayTest {
    private static final UUID LOWER_ID = new UUID(0L, 1L);
    private static final UUID HIGHER_ID = new UUID(0L, 2L);

    @Test
    void clearExitWinsRegardlessOfUuid() {
        var clear = new JunctionRightOfWay.Claim(false, true, HIGHER_ID);
        var blocked = new JunctionRightOfWay.Claim(false, false, LOWER_ID);

        assertTrue(JunctionRightOfWay.wins(clear, blocked));
        assertFalse(JunctionRightOfWay.wins(blocked, clear));
    }

    @Test
    void lowerUuidBreaksEqualExitTie() {
        var lower = new JunctionRightOfWay.Claim(false, true, LOWER_ID);
        var higher = new JunctionRightOfWay.Claim(false, true, HIGHER_ID);

        assertTrue(JunctionRightOfWay.wins(lower, higher));
        assertFalse(JunctionRightOfWay.wins(higher, lower));
    }

    @Test
    void reservationOwnerWinsEvenWithBlockedExit() {
        var owner = new JunctionRightOfWay.Claim(true, false, HIGHER_ID);
        var clearWaiter = new JunctionRightOfWay.Claim(false, true, LOWER_ID);

        assertTrue(JunctionRightOfWay.wins(owner, clearWaiter));
        assertFalse(JunctionRightOfWay.wins(clearWaiter, owner));
    }

    @Test
    void threeContendersHaveExactlyOneWinner() {
        var winner = new JunctionRightOfWay.Claim(false, true, LOWER_ID);
        var sameExit = new JunctionRightOfWay.Claim(false, true, HIGHER_ID);
        var blocked = new JunctionRightOfWay.Claim(false, false, new UUID(0L, 0L));
        List<JunctionRightOfWay.Claim> contenders = List.of(winner, sameExit, blocked);

        long winners = contenders.stream()
                .filter(candidate -> contenders.stream()
                        .filter(other -> other != candidate)
                        .allMatch(other -> JunctionRightOfWay.wins(candidate, other)))
                .count();

        assertTrue(JunctionRightOfWay.wins(winner, sameExit));
        assertTrue(JunctionRightOfWay.wins(winner, blocked));
        assertEquals(1, winners);
    }
}
