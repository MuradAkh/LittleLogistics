package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.network.client.VehicleTrackerClientPacket;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.TugRoute;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static final Logger LOGGER = LogManager.getLogger(NetworkHandler.class);

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ShippingMod.MOD_ID).versioned("1");

        // Client → Server packets
        registrar.playToServer(
                SetEnginePacket.TYPE,
                SetEnginePacket.STREAM_CODEC,
                NetworkHandler::handleSetEngine
        );
        registrar.playToServer(
                EnrollVehiclePacket.TYPE,
                EnrollVehiclePacket.STREAM_CODEC,
                NetworkHandler::handleEnrollVehicle
        );
        registrar.playToServer(
                SetRouteTagPacket.TYPE,
                SetRouteTagPacket.STREAM_CODEC,
                NetworkHandler::handleSetRouteTag
        );

        // Server → Client packets
        registrar.playToClient(
                VehicleTrackerClientPacket.TYPE,
                VehicleTrackerClientPacket.STREAM_CODEC,
                NetworkHandler::handleVehicleTracker
        );
    }

    // --- Server-side handlers (for client→server packets) ---

    private static void handleSetEngine(SetEnginePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            var loco = serverPlayer.level().getEntity(packet.locoId());
            if (loco != null && loco.distanceTo(serverPlayer) < 6 && loco instanceof HeadVehicle l) {
                l.setEngineOn(packet.state());
            }
        });
    }

    private static void handleEnrollVehicle(EnrollVehiclePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            var loco = serverPlayer.level().getEntity(packet.locoId());
            if (loco != null && loco.distanceTo(serverPlayer) < 6 && loco instanceof HeadVehicle l) {
                l.enroll(serverPlayer.getUUID());
            }
        });
    }

    private static void handleSetRouteTag(SetRouteTagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            ItemStack heldStack = player.getItemInHand(
                    packet.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            LOGGER.info("Item in hand is {}", heldStack);
            if (heldStack.getItem() != ModItems.TUG_ROUTE.get()) {
                LOGGER.error("Item held in hand was not tug_route item, perhaps client has de-synced? Dropping packet");
                return;
            }

            CompoundTag routeTag = packet.tag();
            LOGGER.info(routeTag);
            TugRouteItem.saveRoute(TugRoute.fromNBT(routeTag), heldStack);
        });
    }

    // --- Client-side handler (for server→client packets) ---

    private static void handleVehicleTracker(VehicleTrackerClientPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            VehicleTrackerPacketHandler.toRender = packet.parse();
            VehicleTrackerPacketHandler.toRenderDimension = packet.dimension();
        });
    }
}
