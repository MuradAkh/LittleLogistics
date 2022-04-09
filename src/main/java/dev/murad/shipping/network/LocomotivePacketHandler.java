package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

public final class LocomotivePacketHandler {
    private static final Logger LOGGER = LogManager.getLogger(LocomotivePacketHandler.class);
    public static final ResourceLocation LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "locomotive_channel");
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            LOCATION,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;
    public static void register() {
        // int index, Class<MSG> messageType, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer
        INSTANCE.registerMessage(id++, SetLocomotiveEnginePacket.class, SetLocomotiveEnginePacket::encode, SetLocomotiveEnginePacket::new, LocomotivePacketHandler::handleSetEngine);
    }


    public static void handleSetEngine(SetLocomotiveEnginePacket operation, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Optional.of(ctx.get()).map(NetworkEvent.Context::getSender).ifPresent(serverPlayer -> {
                var loco = serverPlayer.level.getEntity(operation.locoId);
                if(loco != null && loco.distanceTo(serverPlayer) < 6 && loco instanceof AbstractLocomotiveEntity l){
                    l.setEngineOn(operation.state);
                }
            });

        });

        ctx.get().setPacketHandled(true);
    }
}
