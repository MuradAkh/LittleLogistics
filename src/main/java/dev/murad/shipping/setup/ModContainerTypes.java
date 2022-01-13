package dev.murad.shipping.setup;

import dev.murad.shipping.entity.container.FishingBargeContainer;
import dev.murad.shipping.entity.container.SteamTugContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IntArray;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;

public class ModContainerTypes {

    private static IntArray makeIntArray(PacketBuffer buffer){
        IntArray arr = new IntArray(3);
        arr.set(0, buffer.readInt());
        arr.set(1, buffer.readInt());
        arr.set(2, buffer.readInt());
        return arr;
    }

    public static final RegistryObject<ContainerType<SteamTugContainer>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IForgeContainerType.create(
                            (windowId, inv, data) ->
                                    new SteamTugContainer(windowId, inv.player.level, makeIntArray(data), inv, inv.player)));

    public static final RegistryObject<ContainerType<FishingBargeContainer>> FISHING_BARGE_CONTAINER =
            Registration.CONTAINERS.register("fishing_barge_container",
                    () -> IForgeContainerType.create(
                            (windowId, inv, data) ->
                                    new FishingBargeContainer(windowId, inv.player.level, data.readInt(), inv, inv.player)));




    public static void register () {}
}
