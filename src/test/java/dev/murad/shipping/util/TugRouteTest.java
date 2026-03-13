package dev.murad.shipping.util;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TugRouteTest {

    @Test
    void roundTrip_emptyRoute() {
        TugRoute route = new TugRoute();
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(0, deserialized.size());
        assertFalse(deserialized.hasCustomName());
    }

    @Test
    void roundTrip_singleNode() {
        TugRoute route = new TugRoute(List.of(new TugRouteNode(10.5, 20.5)));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(1, deserialized.size());
        assertEquals(10.5, deserialized.get(0).getX());
        assertEquals(20.5, deserialized.get(0).getZ());
    }

    @Test
    void roundTrip_multipleNodes() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(1.0, 2.0),
                new TugRouteNode(3.0, 4.0),
                new TugRouteNode(5.0, 6.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(route, deserialized);
    }

    @Test
    void roundTrip_withCustomName() {
        TugRoute route = new TugRoute("My Route", List.of(
                new TugRouteNode("Dock A", 100.0, 200.0),
                new TugRouteNode(300.0, 400.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertTrue(deserialized.hasCustomName());
        assertEquals(route, deserialized);
        assertEquals("Dock A", deserialized.get(0).getName());
        assertNull(deserialized.get(1).getName());
    }

    @Test
    void roundTrip_preservesNodeOrder() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(10.0, 20.0),
                new TugRouteNode(30.0, 40.0),
                new TugRouteNode(50.0, 60.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        for (int i = 0; i < route.size(); i++) {
            assertEquals(route.get(i).getX(), deserialized.get(i).getX());
            assertEquals(route.get(i).getZ(), deserialized.get(i).getZ());
        }
    }

    @Test
    void roundTrip_negativeCoordinates() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(-500.5, -1000.25)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(-500.5, deserialized.get(0).getX());
        assertEquals(-1000.25, deserialized.get(0).getZ());
    }
}
