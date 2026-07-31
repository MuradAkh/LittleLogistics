package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.LCMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;


public class Registration {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, LCMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, LCMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, LCMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LCMod.MOD_ID);

    public static void register (IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        TILE_ENTITIES.register(eventBus);
        ENTITIES.register(eventBus);

        LCItems.register();
        LCBlocks.register();
        LCBlockEntityTypes.register();
        LCEntityTypes.register();

        // Ponder registration is client-only; the MOD-bus lifecycle event fires once, so the old
        // Registrate OneTimeEventReceiver self-removing listener is no longer needed.
        eventBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(LCPonder::register));
    }
}
