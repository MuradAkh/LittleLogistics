package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.TugRoute;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public final class TugRoutePacketHandler {
    private static final Logger LOGGER = LogManager.getLogger(TugRoutePacketHandler.class);
    public static final ResourceLocation LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "tug_route_channel");
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
        INSTANCE.registerMessage(id++, SetTag.class, SetTag::encode, SetTag::new, TugRoutePacketHandler::handleSetTag);
    }


    public static void handleSetTag(SetTag operation, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.error("Received packet not from player, dropping packet");
                return;
            }

            ItemStack heldStack = player.getItemInHand(operation.isOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
            LOGGER.info("Item in hand is {}", heldStack);
            if (heldStack.getItem() != ModItems.TUG_ROUTE.get()) {
                LOGGER.error("Item held in hand was not tug_route item, perhaps client has de-synced? Dropping packet");
                return;
            }

            CompoundNBT routeTag = operation.tag;
            LOGGER.info(routeTag);
            TugRouteItem.saveRoute(TugRoute.fromNBT(routeTag), heldStack);
        });

        ctx.get().setPacketHandled(true);
    }
}
