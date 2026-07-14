package dev.murad.shipping.network.client;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Full replacement snapshot for wrench locomotive-route overlays. */
public record LocoRouteTrackerClientPacket(String dimension, List<LocoRouteTrackerData> routes)
        implements CustomPacketPayload {
    private static final int MAX_TRACKED_LOCOS = 1_000;
    private static final int MAX_TOTAL_PATH_VERTICES = 16_384;
    public static final Type<LocoRouteTrackerClientPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "loco_route_tracker"));
    public static final StreamCodec<FriendlyByteBuf, LocoRouteTrackerClientPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, LocoRouteTrackerClientPacket::dimension,
        LocoRouteTrackerData.STREAM_CODEC.apply(ByteBufCodecs.list()), LocoRouteTrackerClientPacket::routes,
        LocoRouteTrackerClientPacket::new
    );

    public LocoRouteTrackerClientPacket {
        routes = List.copyOf(routes);
        if (routes.size() > MAX_TRACKED_LOCOS
            || routes.stream().mapToInt(route -> route.pathVertices().size()).sum() > MAX_TOTAL_PATH_VERTICES) {
            throw new IllegalArgumentException("Tracked locomotive route snapshot exceeds the overlay budget");
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
