package dev.murad.shipping.network.client;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Full replacement snapshot for the static tug-route portion of the wrench overlay. */
public record TugRouteTrackerClientPacket(String dimension, List<TugRouteTrackerData> routes)
        implements CustomPacketPayload {
    private static final int MAX_TRACKED_TUGS = 1_000;
    private static final int MAX_TOTAL_PATH_VERTICES = 16_384;
    public static final Type<TugRouteTrackerClientPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "tug_route_tracker"));

    public static final StreamCodec<FriendlyByteBuf, TugRouteTrackerClientPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TugRouteTrackerClientPacket::dimension,
            TugRouteTrackerData.STREAM_CODEC.apply(ByteBufCodecs.list()), TugRouteTrackerClientPacket::routes,
            TugRouteTrackerClientPacket::new
        );

    public TugRouteTrackerClientPacket {
        routes = List.copyOf(routes);
        if (routes.size() > MAX_TRACKED_TUGS
            || routes.stream().mapToInt(route -> route.pathVertices().size()).sum() > MAX_TOTAL_PATH_VERTICES) {
            throw new IllegalArgumentException("Tracked tug route snapshot exceeds the overlay budget");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
