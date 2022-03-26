package dev.murad.shipping.network;

import net.minecraft.network.FriendlyByteBuf;

public class SetLocomotiveEnginePacket {
    public final int locoId;
    public final boolean state;

    public SetLocomotiveEnginePacket(FriendlyByteBuf buffer) {
        this.locoId = buffer.readInt();
        this.state = buffer.readBoolean();
    }

    public SetLocomotiveEnginePacket(int locoId, boolean state) {
        this.locoId = locoId;
        this.state = state;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(locoId);
        buf.writeBoolean(state);
    }
}
