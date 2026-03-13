package dev.murad.shipping.network.client;

import dev.murad.shipping.ShippingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

public record VehicleTrackerClientPacket(CompoundTag tag, String dimension)
        implements CustomPacketPayload {

    public static final Type<VehicleTrackerClientPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "vehicle_tracker"));

    public static final StreamCodec<FriendlyByteBuf, VehicleTrackerClientPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, VehicleTrackerClientPacket::tag,
                    ByteBufCodecs.STRING_UTF8, VehicleTrackerClientPacket::dimension,
                    VehicleTrackerClientPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static VehicleTrackerClientPacket of(List<EntityPosition> types, String dimension) {
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
        return new VehicleTrackerClientPacket(tag, dimension);
    }

    public List<EntityPosition> parse() {
        return tag.getAllKeys().stream().map(key -> {
            CompoundTag coords = tag.getCompound(key);
            return new EntityPosition(
                    coords.getString("type"),
                    coords.getInt("eid"),
                    new Vec3(coords.getDouble("x"), coords.getDouble("y"), coords.getDouble("z")),
                    new Vec3(coords.getDouble("xo"), coords.getDouble("yo"), coords.getDouble("zo"))
            );
        }).collect(Collectors.toList());
    }
}
