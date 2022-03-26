package dev.murad.shipping.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SetRouteTagPacket {
    public final int routeChecksum;
    public final boolean isOffhand;
    public final CompoundTag tag;

    public SetRouteTagPacket(FriendlyByteBuf buffer) {
        this.routeChecksum = buffer.readInt();
        this.isOffhand = buffer.readBoolean();
        this.tag = buffer.readNbt();
    }

    public SetRouteTagPacket(int checksum, boolean isOffHand, CompoundTag tag) {
        this.routeChecksum = checksum;
        this.isOffhand = isOffHand;
        this.tag = tag;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(routeChecksum);
        buf.writeBoolean(isOffhand);
        buf.writeNbt(tag);
    }
}
