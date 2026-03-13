package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.TugRoute;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ShippingMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TugRoute>> TUG_ROUTE =
        COMPONENTS.register("tug_route", () ->
            DataComponentType.<TugRoute>builder()
                .persistent(TugRoute.CODEC)
                .networkSynchronized(TugRoute.STREAM_CODEC)
                .build()
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LocoRoute>> LOCO_ROUTE =
        COMPONENTS.register("loco_route", () ->
            DataComponentType.<LocoRoute>builder()
                .persistent(LocoRoute.CODEC)
                .networkSynchronized(LocoRoute.STREAM_CODEC)
                .build()
        );

    // SpringItem "linked" entity ID: transient runtime-only data.
    // NOT persistent (entity IDs don't survive world reload).
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SPRING_LINKED =
        COMPONENTS.register("spring_linked", () ->
            DataComponentType.<Integer>builder()
                .networkSynchronized(ByteBufCodecs.INT)
                .build()
        );
}
