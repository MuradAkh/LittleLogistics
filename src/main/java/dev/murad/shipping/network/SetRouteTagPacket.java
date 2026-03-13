package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetRouteTagPacket(int routeChecksum, boolean isOffhand, CompoundTag tag)
        implements CustomPacketPayload {

    public static final Type<SetRouteTagPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_route_tag"));

    public static final StreamCodec<FriendlyByteBuf, SetRouteTagPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SetRouteTagPacket::routeChecksum,
                    ByteBufCodecs.BOOL, SetRouteTagPacket::isOffhand,
                    ByteBufCodecs.COMPOUND_TAG, SetRouteTagPacket::tag,
                    SetRouteTagPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
