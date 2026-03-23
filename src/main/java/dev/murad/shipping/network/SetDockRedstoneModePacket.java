package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetDockRedstoneModePacket(BlockPos controllerPos, int modeOrdinal) implements CustomPacketPayload {

    public static final Type<SetDockRedstoneModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_dock_redstone_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetDockRedstoneModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetDockRedstoneModePacket::controllerPos,
                    ByteBufCodecs.INT, SetDockRedstoneModePacket::modeOrdinal,
                    SetDockRedstoneModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
