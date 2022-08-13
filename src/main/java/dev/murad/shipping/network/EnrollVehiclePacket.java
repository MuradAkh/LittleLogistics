package dev.murad.shipping.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;

@RequiredArgsConstructor
public class EnrollVehiclePacket {
    public final int locoId;

    public EnrollVehiclePacket(FriendlyByteBuf buffer) {
        this.locoId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(locoId);
    }
}
