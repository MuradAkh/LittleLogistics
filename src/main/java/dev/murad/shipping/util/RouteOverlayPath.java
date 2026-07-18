package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Small path-normalization helper for the conductor's-wrench overlay. */
public final class RouteOverlayPath {
    private RouteOverlayPath() {
    }

    public static List<BlockPos> expandStraightSegments(List<BlockPos> vertices) {
        if (vertices.isEmpty()) return List.of();

        List<BlockPos> expanded = new ArrayList<>();
        expanded.add(vertices.getFirst().immutable());
        for (int index = 1; index < vertices.size(); index++) {
            BlockPos from = vertices.get(index - 1);
            BlockPos to = vertices.get(index);
            int dx = to.getX() - from.getX();
            int dy = to.getY() - from.getY();
            int dz = to.getZ() - from.getZ();
            int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            if (steps == 0) continue;

            if (dx % steps != 0 || dy % steps != 0 || dz % steps != 0) {
                expanded.add(to.immutable());
                continue;
            }

            int stepX = dx / steps;
            int stepY = dy / steps;
            int stepZ = dz / steps;
            for (int step = 1; step <= steps; step++) {
                expanded.add(from.offset(stepX * step, stepY * step, stepZ * step));
            }
        }
        return expanded;
    }
}
