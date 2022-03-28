package dev.murad.shipping.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;

@RequiredArgsConstructor
public class SetLocomotiveEnginePacket {
    public final int locoId;
    public final boolean state;

    public SetLocomotiveEnginePacket(FriendlyByteBuf buffer) {
        this.locoId = buffer.readInt();
        this.state = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(locoId);
        buf.writeBoolean(state);
    }
}
