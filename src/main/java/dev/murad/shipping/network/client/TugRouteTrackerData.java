package dev.murad.shipping.network.client;

import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRoutePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Render-only snapshot of a tug route. It deliberately omits route names, edit state, and the
 * original point-per-water-cell representation so the wrench overlay stays inexpensive to sync.
 */
public record TugRouteTrackerData(int entityId, int dyeColorId, List<BlockPos> pathVertices,
                                  List<BlockPos> waypointPositions) {
    public static final int MAX_PATH_VERTICES = 2_048;
    public static final int MAX_WAYPOINTS = 256;

    public static final StreamCodec<FriendlyByteBuf, TugRouteTrackerData> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TugRouteTrackerData::entityId,
            ByteBufCodecs.VAR_INT, TugRouteTrackerData::dyeColorId,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), TugRouteTrackerData::pathVertices,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), TugRouteTrackerData::waypointPositions,
            TugRouteTrackerData::new
        );

    public TugRouteTrackerData {
        pathVertices = List.copyOf(pathVertices);
        waypointPositions = List.copyOf(waypointPositions);
        if (pathVertices.size() > MAX_PATH_VERTICES || waypointPositions.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException("Tracked tug route exceeds the overlay budget");
        }
    }

    public static Optional<TugRouteTrackerData> fromTug(AbstractTugEntity tug) {
        TugRoute route = TugRouteItem.getRoute(tug.getRouteItemHandler().getStackInSlot(0));
        if (!route.isComplete() || route.isEmpty()) {
            return Optional.empty();
        }

        List<BlockPos> path = flattenPath(route);
        if (path.size() < 2) {
            return Optional.empty();
        }

        List<BlockPos> simplified = collapseCollinearPoints(path);
        if (simplified.size() > MAX_PATH_VERTICES || route.size() > MAX_WAYPOINTS) {
            return Optional.empty();
        }

        List<BlockPos> waypoints = route.stream().map(node -> node.toBlockPos().immutable()).toList();
        return Optional.of(new TugRouteTrackerData(tug.getId(), tug.getColor(), simplified, waypoints));
    }

    private static List<BlockPos> flattenPath(TugRoute route) {
        List<BlockPos> points = new ArrayList<>();
        if (!route.getSegments().isEmpty()) {
            for (var segment : route.getSegments()) {
                List<TugRoutePoint> segmentPoints = segment.getPoints();
                for (int index = 0; index < segmentPoints.size(); index++) {
                    if (!points.isEmpty() && index == 0) {
                        continue;
                    }
                    points.add(segmentPoints.get(index).toBlockPos().immutable());
                }
            }
            return points;
        }

        for (var node : route) {
            points.add(node.toBlockPos().immutable());
        }
        if (points.size() > 1) {
            points.add(points.getFirst());
        }
        return points;
    }

    private static List<BlockPos> collapseCollinearPoints(List<BlockPos> points) {
        if (points.size() < 3) {
            return points;
        }

        List<BlockPos> simplified = new ArrayList<>();
        simplified.add(points.getFirst());
        for (int index = 1; index < points.size() - 1; index++) {
            BlockPos previous = points.get(index - 1);
            BlockPos current = points.get(index);
            BlockPos next = points.get(index + 1);
            if (!continuesStraight(previous, current, next)) {
                simplified.add(current);
            }
        }
        simplified.add(points.getLast());
        return simplified;
    }

    private static boolean continuesStraight(BlockPos previous, BlockPos current, BlockPos next) {
        int firstX = current.getX() - previous.getX();
        int firstY = current.getY() - previous.getY();
        int firstZ = current.getZ() - previous.getZ();
        int secondX = next.getX() - current.getX();
        int secondY = next.getY() - current.getY();
        int secondZ = next.getZ() - current.getZ();

        int crossX = firstY * secondZ - firstZ * secondY;
        int crossY = firstZ * secondX - firstX * secondZ;
        int crossZ = firstX * secondY - firstY * secondX;
        int dot = firstX * secondX + firstY * secondY + firstZ * secondZ;
        return crossX == 0 && crossY == 0 && crossZ == 0 && dot > 0;
    }
}
