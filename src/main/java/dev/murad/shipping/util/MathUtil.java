package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class MathUtil {
    public static List<Pair<Vector3d, Vector3d>> getEdges(AxisAlignedBB bb) {
        List<Pair<Vector3d, Vector3d>> edges = new ArrayList<>();
        List<Vector3d> corners = getCorners(bb);

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
    public static List<Vector3d> getCorners(AxisAlignedBB bb) {
        List<Vector3d> corners = new ArrayList<>();
        corners.add(new Vector3d(bb.minX, bb.minY, bb.minZ)); // 000
        corners.add(new Vector3d(bb.minX, bb.minY, bb.maxZ)); // 001
        corners.add(new Vector3d(bb.minX, bb.maxY, bb.minZ)); // 010
        corners.add(new Vector3d(bb.minX, bb.maxY, bb.maxZ)); // 011
        corners.add(new Vector3d(bb.maxX, bb.minY, bb.minZ)); // 100
        corners.add(new Vector3d(bb.maxX, bb.minY, bb.maxZ)); // 101
        corners.add(new Vector3d(bb.maxX, bb.maxY, bb.minZ)); // 110
        corners.add(new Vector3d(bb.maxX, bb.maxY, bb.maxZ)); // 111
        return corners;
    }

    public static Vector3d lerp(Vector3d from, Vector3d to, double ratio) {
        return new Vector3d(MathHelper.lerp(ratio, from.x, to.x), MathHelper.lerp(ratio, from.y, to.y), MathHelper.lerp(ratio, from.z, to.z));
    }
}
