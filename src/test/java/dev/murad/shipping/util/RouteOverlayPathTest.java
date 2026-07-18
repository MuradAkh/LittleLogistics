package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteOverlayPathTest {
    @Test
    void expandsCardinalSegments() {
        assertEquals(List.of(
                new BlockPos(0, 4, 0),
                new BlockPos(1, 4, 0),
                new BlockPos(2, 4, 0),
                new BlockPos(3, 4, 0)
            ),
            RouteOverlayPath.expandStraightSegments(List.of(
                new BlockPos(0, 4, 0),
                new BlockPos(3, 4, 0)
            )));
    }

    @Test
    void expandsDiagonalSegments() {
        assertEquals(List.of(
                new BlockPos(3, 0, 1),
                new BlockPos(2, 0, 2),
                new BlockPos(1, 0, 3),
                new BlockPos(0, 0, 4)
            ),
            RouteOverlayPath.expandStraightSegments(List.of(
                new BlockPos(3, 0, 1),
                new BlockPos(0, 0, 4)
            )));
    }

    @Test
    void preservesCornersWithoutDuplicatingVertices() {
        assertEquals(List.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(2, 0, 0),
                new BlockPos(2, 0, 1),
                new BlockPos(2, 0, 2)
            ),
            RouteOverlayPath.expandStraightSegments(List.of(
                new BlockPos(0, 0, 0),
                new BlockPos(2, 0, 0),
                new BlockPos(2, 0, 2)
            )));
    }
}
