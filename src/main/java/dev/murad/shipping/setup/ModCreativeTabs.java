package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShippingMod.MOD_ID);

    /**
     * Key for the mod's own creative tab. Items and blocks are added to this tab through the same
     * per-registration tab registry that feeds the vanilla tabs (see {@code ModItems#register},
     * {@code ModBlocks#register} and {@code ModClientEventHandler#buildTabContents}), so the tab
     * stays in sync with registration automatically — there is no hand-maintained item list to keep
     * up to date.
     */
    public static final ResourceKey<CreativeModeTab> LITTLE_LOGISTICS_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "little_logistics"));

    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LITTLE_LOGISTICS = CREATIVE_MODE_TABS.register(
            "little_logistics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.littlelogistics.little_logistics"))
                    .icon(() -> new ItemStack(ModItems.STEAM_TUG.get()))
                    // Contents are populated via BuildCreativeModeTabContentsEvent from the shared
                    // tab registry; see ModClientEventHandler#buildTabContents.
                    .build()
    );

    public static void register() {}
}
