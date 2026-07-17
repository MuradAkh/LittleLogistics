package dev.murad.shipping.network.client;

import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteStep;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Render-only, bounded snapshot of an installed compiled locomotive route. */
public record LocoRouteTrackerData(int entityId, int dyeColorId, List<BlockPos> pathVertices,
                                   List<BlockPos> waypointPositions) {
    public static final int MAX_PATH_VERTICES = 4_096;
    public static final int MAX_WAYPOINTS = 256;
    public static final StreamCodec<FriendlyByteBuf, LocoRouteTrackerData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, LocoRouteTrackerData::entityId,
        ByteBufCodecs.VAR_INT, LocoRouteTrackerData::dyeColorId,
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LocoRouteTrackerData::pathVertices,
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LocoRouteTrackerData::waypointPositions,
        LocoRouteTrackerData::new
    );

    public LocoRouteTrackerData {
        pathVertices = List.copyOf(pathVertices);
        waypointPositions = List.copyOf(waypointPositions);
        if (pathVertices.size() > MAX_PATH_VERTICES || waypointPositions.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException("Tracked locomotive route exceeds the overlay budget");
        }
    }

    public static Optional<LocoRouteTrackerData> fromLocomotive(AbstractLocomotiveEntity locomotive) {
        LocoRoute route = LocoRouteItem.getRoute(locomotive.getRouteItemHandler().getStackInSlot(0));
        if (!route.isUsable()) return Optional.empty();
        List<BlockPos> path = new ArrayList<>();
        for (int segmentIndex = 0; segmentIndex < route.getSegments().size(); segmentIndex++) {
            append(path, route.get(segmentIndex).toBlockPos());
            for (LocoRouteStep step : route.getSegments().get(segmentIndex).getSteps()) append(path, step.railPos());
            append(path, route.get((segmentIndex + 1) % route.size()).toBlockPos());
        }
        if (path.size() < 2 || path.size() > MAX_PATH_VERTICES) return Optional.empty();
        return Optional.of(new LocoRouteTrackerData(locomotive.getId(), locomotive.getColor(), path,
            route.stream().map(node -> node.toBlockPos().immutable()).toList()));
    }

    private static void append(List<BlockPos> path, BlockPos pos) {
        if (path.isEmpty() || !path.getLast().equals(pos)) path.add(pos.immutable());
    }
}
