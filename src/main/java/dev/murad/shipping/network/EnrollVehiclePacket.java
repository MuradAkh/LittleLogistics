package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnrollVehiclePacket(int locoId) implements CustomPacketPayload {

    public static final Type<EnrollVehiclePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "enroll_vehicle"));

    public static final StreamCodec<FriendlyByteBuf, EnrollVehiclePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, EnrollVehiclePacket::locoId,
                    EnrollVehiclePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
