package dev.murad.shipping.setup;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class EntityItemMap {
    // leaving this public in case addon mods want to write here
    public static final Map<String, Item> ENTITY_CREATOR_ITEMS = new HashMap<>();

    private static void init() {
        ENTITY_CREATOR_ITEMS.put(ModEntityTypes.ENERGY_LOCOMOTIVE.get().toString(), ModItems.ENERGY_LOCOMOTIVE.get());
        ENTITY_CREATOR_ITEMS.put(ModEntityTypes.STEAM_LOCOMOTIVE.get().toString(), ModItems.STEAM_LOCOMOTIVE.get());
        ENTITY_CREATOR_ITEMS.put(ModEntityTypes.ENERGY_TUG.get().toString(), ModItems.ENERGY_TUG.get());
        ENTITY_CREATOR_ITEMS.put(ModEntityTypes.STEAM_TUG.get().toString(), ModItems.STEAM_TUG.get());
    }

    public static Item get(String entityType){
        if(ENTITY_CREATOR_ITEMS.isEmpty()){
            init();
        }
        return ENTITY_CREATOR_ITEMS.getOrDefault(entityType, Items.MINECART);
    }
}
