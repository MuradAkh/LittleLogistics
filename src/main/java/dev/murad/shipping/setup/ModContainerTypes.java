package dev.murad.shipping.setup;

import dev.murad.shipping.entity.container.TugContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;

public class ModContainerTypes {

    public static final RegistryObject<ContainerType<TugContainer>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IForgeContainerType.create(
                            (windowId, inv, data) ->
                                    new TugContainer(windowId, inv.player.level, data.readInt(), inv, inv.player)));

    public static void register () {}
}
