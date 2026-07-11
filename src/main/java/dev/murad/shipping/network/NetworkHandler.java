package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.network.client.VehicleTrackerClientPacket;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
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
                SetDockConfigPacket.TYPE,
                SetDockConfigPacket.STREAM_CODEC,
                NetworkHandler::handleSetDockConfig
        );
        registrar.playToServer(
                SetStationNamePacket.TYPE,
                SetStationNamePacket.STREAM_CODEC,
                NetworkHandler::handleSetStationName
        );
        registrar.playToServer(
                SetDockRedstoneModePacket.TYPE,
                SetDockRedstoneModePacket.STREAM_CODEC,
                NetworkHandler::handleSetDockRedstoneMode
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

    private static void handleSetDockConfig(SetDockConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            if (serverPlayer.distanceToSqr(
                    packet.pos().getX() + 0.5,
                    packet.pos().getY() + 0.5,
                    packet.pos().getZ() + 0.5) > 64) {
                return;
            }
            BlockEntity be = serverPlayer.level().getBlockEntity(packet.pos());
            if (be instanceof DockingStationBlockEntity dockBE) {
                dockBE.adjustTimeout(packet.scrollDelta());
            }
        });
    }

    private static void handleSetDockRedstoneMode(SetDockRedstoneModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            if (serverPlayer.distanceToSqr(
                    packet.controllerPos().getX() + 0.5,
                    packet.controllerPos().getY() + 0.5,
                    packet.controllerPos().getZ() + 0.5) > 64) {
                return;
            }
            BlockEntity be = serverPlayer.level().getBlockEntity(packet.controllerPos());
            if (be instanceof DockingStationBlockEntity dockBE) {
                dockBE.setRedstoneMode(packet.modeOrdinal());
            }
        });
    }

    private static void handleSetStationName(SetStationNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            if (serverPlayer.distanceToSqr(
                    packet.controllerPos().getX() + 0.5,
                    packet.controllerPos().getY() + 0.5,
                    packet.controllerPos().getZ() + 0.5) > 64) {
                return;
            }
            BlockEntity be = serverPlayer.level().getBlockEntity(packet.controllerPos());
            if (be instanceof DockingStationBlockEntity dockBE) {
                dockBE.setStationName(packet.name());
            }
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
