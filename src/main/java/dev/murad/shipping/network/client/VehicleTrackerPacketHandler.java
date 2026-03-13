package dev.murad.shipping.network.client;

import java.util.ArrayList;
import java.util.List;

public class VehicleTrackerPacketHandler {
    public static List<EntityPosition> toRender = new ArrayList<>();
    public static String toRenderDimension = "";

    public static void flush() {
        toRender.clear();
    }
}
