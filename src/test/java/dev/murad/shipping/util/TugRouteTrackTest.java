package dev.murad.shipping.util;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TugRouteTrackTest {
    @Test
    void upcomingChunksFollowCompiledPathOrder() {
        TugRoute route = new TugRoute(null, List.of(), List.of(new TugRouteSegment(List.of(
            new TugRoutePoint(14, 63, 0),
            new TugRoutePoint(15, 63, 0),
            new TugRoutePoint(16, 63, 0),
            new TugRoutePoint(17, 63, 0),
            new TugRoutePoint(32, 63, 0)
        ))));

        TugRouteTrack track = TugRouteTrack.from(route);

        assertEquals(List.of(new ChunkPos(0, 0), new ChunkPos(1, 0), new ChunkPos(2, 0)),
            track.upcomingChunks(0, 32, false));
    }

    @Test
    void openApproachDoesNotWrapToItsStart() {
        TugRoute route = new TugRoute(null, List.of(), List.of(new TugRouteSegment(List.of(
            new TugRoutePoint(0, 63, 0),
            new TugRoutePoint(20, 63, 0)
        ))));

        TugRouteTrack track = TugRouteTrack.from(route);

        assertEquals(List.of(new ChunkPos(1, 0)), track.upcomingChunks(19, 20, false));
    }
}
