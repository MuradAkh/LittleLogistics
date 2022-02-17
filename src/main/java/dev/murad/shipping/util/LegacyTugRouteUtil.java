package dev.murad.shipping.util;

import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyTugRouteUtil {
    public static TugRoute convertLegacyRoute(List<Vec2> legacyRoute) {
        return new TugRoute(legacyRoute.stream().map(TugRouteNode::fromVector2f).collect(Collectors.toList()));
    }

    public static List<Vec2> parseLegacyRouteString(String route){
        if(route.equals("")){
            return new ArrayList<>();
        }

        return Arrays.stream(route.split(","))
                .map(string -> string.split(":"))
                .map(arr -> new Vec2(Float.parseFloat(arr[0]), Float.parseFloat(arr[1])))
                .collect(Collectors.toList());

    }
}
