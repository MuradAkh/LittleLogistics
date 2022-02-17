package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MathUtil {
    public static List<Pair<Vec3, Vec3>> getEdges(AABB bb) {
        List<Pair<Vec3, Vec3>> edges = new ArrayList<>();
        List<Vec3> corners = getCorners(bb);

        // minY plane
        edges.add(new Pair<>(corners.get(0), corners.get(1)));
        edges.add(new Pair<>(corners.get(0), corners.get(2)));
        edges.add(new Pair<>(corners.get(3), corners.get(1)));
        edges.add(new Pair<>(corners.get(3), corners.get(2)));

        // maxY plane
        edges.add(new Pair<>(corners.get(4), corners.get(5)));
        edges.add(new Pair<>(corners.get(4), corners.get(6)));
        edges.add(new Pair<>(corners.get(7), corners.get(5)));
        edges.add(new Pair<>(corners.get(7), corners.get(6)));

        // vertical edges
        edges.add(new Pair<>(corners.get(0), corners.get(4)));
        edges.add(new Pair<>(corners.get(1), corners.get(5)));
        edges.add(new Pair<>(corners.get(2), corners.get(6)));
        edges.add(new Pair<>(corners.get(3), corners.get(7)));

        return edges;
    }

    // returns a list of corners in a set order, but I can't be bothered to write
    // out the order here.
    public static List<Vec3> getCorners(AABB bb) {
        List<Vec3> corners = new ArrayList<>();
        corners.add(new Vec3(bb.minX, bb.minY, bb.minZ)); // 000
        corners.add(new Vec3(bb.minX, bb.minY, bb.maxZ)); // 001
        corners.add(new Vec3(bb.minX, bb.maxY, bb.minZ)); // 010
        corners.add(new Vec3(bb.minX, bb.maxY, bb.maxZ)); // 011
        corners.add(new Vec3(bb.maxX, bb.minY, bb.minZ)); // 100
        corners.add(new Vec3(bb.maxX, bb.minY, bb.maxZ)); // 101
        corners.add(new Vec3(bb.maxX, bb.maxY, bb.minZ)); // 110
        corners.add(new Vec3(bb.maxX, bb.maxY, bb.maxZ)); // 111
        return corners;
    }

    public static Vec3 lerp(Vec3 from, Vec3 to, double ratio) {
        return new Vec3(Mth.lerp(ratio, from.x, to.x), Mth.lerp(ratio, from.y, to.y), Mth.lerp(ratio, from.z, to.z));
    }
}
