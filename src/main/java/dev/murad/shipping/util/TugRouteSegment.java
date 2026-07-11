package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TugRouteSegment {
    private static final String POINTS_TAG = "points";

    public static final Codec<TugRouteSegment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            TugRoutePoint.CODEC.listOf().fieldOf("points")
                .forGetter(segment -> List.copyOf(segment.points))
        ).apply(instance, TugRouteSegment::new)
    );

    public static final StreamCodec<FriendlyByteBuf, TugRouteSegment> STREAM_CODEC =
        StreamCodec.composite(
            TugRoutePoint.STREAM_CODEC.apply(ByteBufCodecs.list()), segment -> List.copyOf(segment.points),
            TugRouteSegment::new
        );

    private final List<TugRoutePoint> points;

    public TugRouteSegment(List<TugRoutePoint> points) {
        this.points = new ArrayList<>(points);
    }

    public List<TugRoutePoint> getPoints() {
        return points;
    }

    public TugRouteSegment copy() {
        return new TugRouteSegment(this.points);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (TugRoutePoint point : points) {
            list.add(point.toNBT());
        }
        tag.put(POINTS_TAG, list);
        return tag;
    }

    public static TugRouteSegment fromNBT(CompoundTag tag) {
        List<TugRoutePoint> points = new ArrayList<>();
        ListTag list = tag.getList(POINTS_TAG, 10);
        for (int i = 0; i < list.size(); i++) {
            points.add(TugRoutePoint.fromNBT(list.getCompound(i)));
        }
        return new TugRouteSegment(points);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TugRouteSegment that = (TugRouteSegment) o;
        return Objects.equals(points, that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points);
    }
}
