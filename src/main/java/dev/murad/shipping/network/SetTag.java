package dev.murad.shipping.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SetTag {
    public final int routeChecksum;
    public final boolean isOffhand;
    public final CompoundTag tag;

    public SetTag(FriendlyByteBuf buffer) {
        this.routeChecksum = buffer.readInt();
        this.isOffhand = buffer.readBoolean();
        this.tag = buffer.readNbt();
    }

    public SetTag(int checksum, boolean isOffHand, CompoundNBT tag) {
        this.routeChecksum = checksum;
        this.isOffhand = isOffHand;
        this.tag = tag;
    }

    public void encode(PacketBuffer buf) {
        buf.writeInt(routeChecksum);
        buf.writeBoolean(isOffhand);
        buf.writeNbt(tag);
    }
}
