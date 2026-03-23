package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetStationNamePacket(BlockPos controllerPos, String name) implements CustomPacketPayload {

    public static final Type<SetStationNamePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_station_name"));

    public static final StreamCodec<FriendlyByteBuf, SetStationNamePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetStationNamePacket::controllerPos,
                    ByteBufCodecs.STRING_UTF8, SetStationNamePacket::name,
                    SetStationNamePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
