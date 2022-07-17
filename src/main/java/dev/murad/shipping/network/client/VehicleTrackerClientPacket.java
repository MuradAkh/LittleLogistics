package dev.murad.shipping.network.client;

import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VehicleTrackerClientPacket {
    public final CompoundTag tag;

    public VehicleTrackerClientPacket(FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    public static VehicleTrackerClientPacket of(List<EntityPosition> types) {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        for (EntityPosition position : types) {
            var coords = new CompoundTag();
            coords.putDouble("x", position.pos().x);
            coords.putDouble("y", position.pos().y);
            coords.putDouble("z", position.pos().z);
            coords.putDouble("xo", position.oldPos().x);
            coords.putDouble("yo", position.oldPos().y);
            coords.putDouble("zo", position.oldPos().z);
            coords.putString("type", position.type());
            coords.putInt("eid", position.id());
            tag.put(String.valueOf(i++), coords);
        }
        return new VehicleTrackerClientPacket(tag);
    }

    public List<EntityPosition> parse() {
        return tag.getAllKeys().stream().map(key -> {
            CompoundTag coords = tag.getCompound(key);
            return new EntityPosition(
                    coords.getString("type"),
                    coords.getInt("eid"),
                    new Vec3(
                            coords.getDouble("x"),
                            coords.getDouble("y"),
                            coords.getDouble("z")),
                    new Vec3(
                            coords.getDouble("xo"),
                            coords.getDouble("yo"),
                            coords.getDouble("zo"))
            );
        }).collect(Collectors.toList());
    }
}
