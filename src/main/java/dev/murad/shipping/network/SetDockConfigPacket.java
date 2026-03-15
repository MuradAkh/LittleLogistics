package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetDockConfigPacket(BlockPos pos, int scrollDelta) implements CustomPacketPayload {

    public static final Type<SetDockConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_dock_config"));

    public static final StreamCodec<FriendlyByteBuf, SetDockConfigPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetDockConfigPacket::pos,
                    ByteBufCodecs.INT, SetDockConfigPacket::scrollDelta,
                    SetDockConfigPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
