package dev.murad.shipping.util;

import net.minecraft.util.math.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyTugRouteUtil {
    public static TugRoute convertLegacyRoute(List<Vector2f> legacyRoute) {
        return new TugRoute(legacyRoute.stream().map(TugRouteNode::fromVector2f).collect(Collectors.toList()));
    }

    public static List<Vector2f> parseLegacyRouteString(String route){
        if(route.equals("")){
            return new ArrayList<>();
        }

        return Arrays.stream(route.split(","))
                .map(string -> string.split(":"))
                .map(arr -> new Vector2f(Float.parseFloat(arr[0]), Float.parseFloat(arr[1])))
                .collect(Collectors.toList());

    }
}
