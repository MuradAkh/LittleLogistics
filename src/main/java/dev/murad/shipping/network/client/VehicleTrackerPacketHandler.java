package dev.murad.shipping.network.client;

import dev.murad.shipping.ShippingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.antlr.v4.runtime.misc.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class VehicleTrackerPacketHandler {
    private static final Logger LOGGER = LogManager.getLogger(VehicleTrackerPacketHandler.class);
    public static final ResourceLocation LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "vehicle_tracker_channel");
    private static final String PROTOCOL_VERSION = "1";
    public static List<EntityPosition> toRender = new ArrayList<>();
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            LOCATION,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;
    public static void register() {
        // int index, Class<MSG> messageType, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer
        INSTANCE.registerMessage(id++, VehicleTrackerClientPacket.class, VehicleTrackerClientPacket::encode, VehicleTrackerClientPacket::new, VehicleTrackerPacketHandler::handleData);
    }


    public static void handleData(VehicleTrackerClientPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            VehicleTrackerPacketHandler.toRender = packet.parse();
        });

        ctx.get().setPacketHandled(true);
    }
}
