package dev.murad.shipping.util;

import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;

public class TugRouteTrack {
    public record Sample(Vec3 position, Vec3 tangent) {
    }

    /** Closest point on the compiled polyline, expressed as route distance. */
    public record Projection(double distanceAlongTrack, double distanceToTrack) {
    }

    private final List<Vec3> points;
    private final List<Double> cumulativeLengths;
    private final double totalLength;

    private TugRouteTrack(List<Vec3> points, List<Double> cumulativeLengths, double totalLength) {
        this.points = points;
        this.cumulativeLengths = cumulativeLengths;
        this.totalLength = totalLength;
    }

    @Nullable
    public static TugRouteTrack from(TugRoute route) {
        List<Vec3> flattened = new ArrayList<>();
        for (TugRouteSegment segment : route.getSegments()) {
            List<TugRoutePoint> points = segment.getPoints();
            for (int i = 0; i < points.size(); i++) {
                if (!flattened.isEmpty() && i == 0) {
                    continue;
                }
                flattened.add(points.get(i).toVec3Center());
            }
        }

        if (flattened.size() < 2) {
            return null;
        }

        List<Double> cumulative = new ArrayList<>();
        cumulative.add(0.0D);
        double length = 0.0D;
        for (int i = 1; i < flattened.size(); i++) {
            length += flattened.get(i - 1).distanceTo(flattened.get(i));
            cumulative.add(length);
        }

        if (length <= 0.0D) {
            return null;
        }

        return new TugRouteTrack(flattened, cumulative, length);
    }

    public boolean isUsable() {
        return totalLength > 0.0D && points.size() >= 2;
    }

    public double getTotalLength() {
        return totalLength;
    }

    public double wrapDistance(double distance) {
        if (totalLength <= 0.0D) {
            return 0.0D;
        }
        double wrapped = distance % totalLength;
        return wrapped < 0.0D ? wrapped + totalLength : wrapped;
    }

    public Sample sample(double distance) {
        return sampleAt(wrapDistance(distance));
    }

    /** Samples an open lead-in path without wrapping its end back to its start. */
    public Sample sampleClamped(double distance) {
        return sampleAt(Mth.clamp(distance, 0.0D, totalLength));
    }

    public List<ChunkPos> upcomingChunks(double startDistance, int maxSteps, boolean wrap) {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        int steps = Math.min(maxSteps, Math.max(1, (int) Math.ceil(totalLength)));
        for (int offset = 0; offset <= steps; offset++) {
            Sample sample = wrap ? sample(startDistance + offset) : sampleClamped(startDistance + offset);
            chunks.add(new ChunkPos(BlockPos.containing(sample.position())));
        }
        return List.copyOf(chunks);
    }

    private Sample sampleAt(double distance) {
        for (int i = 1; i < cumulativeLengths.size(); i++) {
            double segmentEnd = cumulativeLengths.get(i);
            if (distance <= segmentEnd) {
                Vec3 from = points.get(i - 1);
                Vec3 to = points.get(i);
                double segmentStart = cumulativeLengths.get(i - 1);
                double segmentLength = segmentEnd - segmentStart;
                double ratio = segmentLength <= 0.0D ? 0.0D : Mth.clamp((distance - segmentStart) / segmentLength, 0.0D, 1.0D);
                Vec3 tangent = to.subtract(from).normalize();
                return new Sample(MathUtil.lerp(from, to, ratio), tangent);
            }
        }

        Vec3 from = points.get(points.size() - 2);
        Vec3 to = points.get(points.size() - 1);
        return new Sample(to, to.subtract(from).normalize());
    }

    public Optional<Projection> project(Vec3 position) {
        if (!isUsable()) {
            return Optional.empty();
        }

        double closestDistanceSquared = Double.POSITIVE_INFINITY;
        double closestDistanceAlongTrack = 0.0D;
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            Vec3 segment = to.subtract(from);
            double segmentLengthSquared = segment.lengthSqr();
            if (segmentLengthSquared <= 1.0E-8D) {
                continue;
            }

            double progress = Mth.clamp(position.subtract(from).dot(segment) / segmentLengthSquared, 0.0D, 1.0D);
            Vec3 closestPoint = from.add(segment.scale(progress));
            double distanceSquared = position.distanceToSqr(closestPoint);
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                double segmentLength = Math.sqrt(segmentLengthSquared);
                closestDistanceAlongTrack = cumulativeLengths.get(i - 1) + progress * segmentLength;
            }
        }

        return closestDistanceSquared == Double.POSITIVE_INFINITY
                ? Optional.empty()
                : Optional.of(new Projection(closestDistanceAlongTrack, Math.sqrt(closestDistanceSquared)));
    }
}
