package dev.murad.shipping.setup;

import dev.murad.shipping.entity.container.SteamTugContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;

public class ModContainerTypes {

    public static final RegistryObject<ContainerType<SteamTugContainer>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IForgeContainerType.create(
                            (windowId, inv, data) ->
                                    new SteamTugContainer(windowId, inv.player.level, data.readInt(), inv, inv.player)));

    public static void register () {}
}
