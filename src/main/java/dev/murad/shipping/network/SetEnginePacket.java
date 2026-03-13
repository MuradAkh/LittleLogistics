package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetEnginePacket(int locoId, boolean state) implements CustomPacketPayload {

    public static final Type<SetEnginePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_engine"));

    public static final StreamCodec<FriendlyByteBuf, SetEnginePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SetEnginePacket::locoId,
                    ByteBufCodecs.BOOL, SetEnginePacket::state,
                    SetEnginePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
