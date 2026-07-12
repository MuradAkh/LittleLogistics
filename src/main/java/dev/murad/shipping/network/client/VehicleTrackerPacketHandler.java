package dev.murad.shipping.network.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleTrackerPacketHandler {
    public static List<EntityPosition> toRender = new ArrayList<>();
    public static String toRenderDimension = "";
    public static Map<Integer, TugRouteTrackerData> tugRoutes = new HashMap<>();
    public static String tugRouteDimension = "";

    public static void setTugRoutes(TugRouteTrackerClientPacket packet) {
        Map<Integer, TugRouteTrackerData> routes = new HashMap<>();
        for (TugRouteTrackerData route : packet.routes()) {
            routes.put(route.entityId(), route);
        }
        tugRoutes = routes;
        tugRouteDimension = packet.dimension();
    }

    public static void flush() {
        toRender.clear();
        tugRoutes.clear();
        toRenderDimension = "";
        tugRouteDimension = "";
    }
}
