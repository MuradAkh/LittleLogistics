package dev.murad.shipping.util;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TugRouteTest {
    @Test
    void roundTripEmptyRoute() {
        TugRoute decoded = TugRoute.fromNBT(new TugRoute().toNBT());
        assertEquals(0, decoded.size());
        assertFalse(decoded.hasCustomName());
    }

    @Test
    void roundTripPreservesThreeDimensionalNodeOrder() {
        TugRoute route = new TugRoute(null, List.of(
            new TugRouteNode(null, 10, 64, 20),
            new TugRouteNode(null, 30, 70, 40),
            new TugRouteNode(null, -50, 80, 60)
        ));

        CompoundTag nbt = route.toNBT();
        TugRoute decoded = TugRoute.fromNBT(nbt);

        assertEquals(route, decoded);
        assertEquals(64, decoded.get(0).getY());
        assertEquals(70, decoded.get(1).getY());
        assertEquals(-50, decoded.get(2).getX());
    }

    @Test
    void roundTripPreservesCustomNodeName() {
        TugRoute route = new TugRoute("My Route", List.of(
            new TugRouteNode("Dock A", 100, 63, 200),
            new TugRouteNode(null, 300, 63, 400)
        ));

        TugRoute decoded = TugRoute.fromNBT(route.toNBT());

        assertTrue(decoded.hasCustomName());
        assertEquals("Dock A", decoded.getFirst().getName());
        assertNull(decoded.get(1).getName());
    }
}
