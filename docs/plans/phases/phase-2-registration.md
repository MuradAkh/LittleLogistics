# Phase 2: Mod Entrypoint & Registration — Step-by-Step Migration Plan

**Phase:** 2 of 10
**Effort:** MEDIUM (mechanical but broad — 11 files)
**Prerequisite:** Phase 1 (Build System) and Phase 1.5 (Vanilla API Compat) must be complete and `./gradlew build` passing.

---

## Phase Overview and Goal

Convert all Forge registration APIs to NeoForge equivalents:
- `FMLJavaModLoadingContext.get()` → constructor-injected `IEventBus` + `ModContainer`
- `ForgeRegistries.X` → `BuiltInRegistries` / `Registries` keys
- `RegistryObject<T>` → `DeferredHolder<BaseType, T>`
- `@Mod.EventBusSubscriber` → `@EventBusSubscriber`
- `"forge"` tag namespace → `"c"` conventional namespace
- Event import relocations from `net.minecraftforge` → `net.neoforged.neoforge`

After this phase, all mod registration compiles against NeoForge APIs and all deferred registers fire correctly on the mod event bus.

---

## Prerequisites

- [ ] Phase 1 complete: build.gradle uses NeoGradle, dependencies point to `net.neoforged:neoforge`
- [ ] Phase 1.5 complete: vanilla API breaks in VesselEntity, AbstractTrainCarEntity, etc. are resolved
- [ ] `./gradlew build` passes (modulo later-phase compilation errors in networking, capabilities, etc. — registration files themselves must compile)

---

## Files Touched (11 files)

| File | Key Changes |
|------|-------------|
| `ShippingMod.java` | Constructor injection, config registration |
| `Registration.java` | `ForgeRegistries` → `BuiltInRegistries`, `IEventBus` parameter |
| `ModEntityTypes.java` | `RegistryObject` → `DeferredHolder` |
| `ModItems.java` | `RegistryObject` → `DeferredHolder`, event import [L-2] |
| `ModBlocks.java` | `RegistryObject` → `DeferredHolder`, event import [L-2] |
| `ModMenuTypes.java` | `RegistryObject` → `DeferredHolder`, `IForgeMenuType` → `IMenuTypeExtension` |
| `ModTileEntitiesTypes.java` | `RegistryObject` → `DeferredHolder` |
| `ModSounds.java` | `RegistryObject` → `DeferredHolder` |
| `ModRecipeSerializers.java` | `RegistryObject` → `DeferredHolder` |
| `ModEventBusEvents.java` | `@Mod.EventBusSubscriber` → `@EventBusSubscriber`, import [L-3] |
| `ModTags.java` | `"forge"` → `"c"` namespace [M-4] |

---

## Step-by-Step Changes

### Step 1: `ShippingMod.java` — Constructor Injection

The mod entrypoint must change from static `FMLJavaModLoadingContext.get()` to NeoForge's constructor-injected `IEventBus` and `ModContainer`.

**Current code:**
```java
package dev.murad.shipping;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
// ... other imports

@Mod(ShippingMod.MOD_ID)
public class ShippingMod {
    public static final String MOD_ID = "littlelogistics";
    private static final Logger LOGGER = LogManager.getLogger();

    public ShippingMod() {
        Registration.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShippingConfig.Common.SPEC, "littlelogistics-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
        // ... more MenuScreens.register() calls ...
        event.enqueueWork(ModItemModelProperties::register);
    }
}
```

**Target code:**
```java
package dev.murad.shipping;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
// ... other imports (MenuScreens.register moves to Phase 8 — RegisterMenuScreensEvent)

@Mod(ShippingMod.MOD_ID)
public class ShippingMod {
    public static final String MOD_ID = "littlelogistics";
    private static final Logger LOGGER = LogManager.getLogger();

    public ShippingMod(IEventBus modEventBus, ModContainer modContainer) {
        Registration.register(modEventBus);

        modEventBus.addListener(this::doClientStuff);

        modContainer.registerConfig(ModConfig.Type.COMMON, ShippingConfig.Common.SPEC, "littlelogistics-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // NOTE: MenuScreens.register() calls will be moved to RegisterMenuScreensEvent in Phase 8.
        // For now they remain here but will use the event-based approach later.
        MenuScreens.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
        // ... (unchanged for now)
        event.enqueueWork(ModItemModelProperties::register);
    }
}
```

**Key changes:**
1. `FMLJavaModLoadingContext.get().getModEventBus()` → constructor parameter `IEventBus modEventBus`
2. `ModLoadingContext.get().registerConfig(...)` → `modContainer.registerConfig(...)`
3. Import changes: `net.minecraftforge.fml.*` → `net.neoforged.fml.*`, `net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext` → REMOVED
4. New import: `net.neoforged.bus.api.IEventBus`, `net.neoforged.fml.ModContainer`

---

### Step 2: `Registration.java` — DeferredRegister + IEventBus Parameter

The central registration hub must switch from `ForgeRegistries` registry constants to NeoForge's `BuiltInRegistries`/`Registries`, and accept `IEventBus` as a parameter instead of fetching it statically.

**Current code:**
```java
package dev.murad.shipping.setup;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

public class Registration {
    public static final DeferredRegister<Block> BLOCKS = create(ForgeRegistries.BLOCKS);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = create(ForgeRegistries.MENU_TYPES);
    public static final DeferredRegister<EntityType<?>> ENTITIES = create(ForgeRegistries.ENTITY_TYPES);
    public static final DeferredRegister<Item> ITEMS = create(ForgeRegistries.ITEMS);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = create(ForgeRegistries.RECIPE_SERIALIZERS);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = create(ForgeRegistries.BLOCK_ENTITY_TYPES);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = create(ForgeRegistries.SOUND_EVENTS);

    private static <T> DeferredRegister<T> create(IForgeRegistry<T> registry) {
        return DeferredRegister.create(registry, ShippingMod.MOD_ID);
    }

    public static void register() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        CONTAINERS.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        TILE_ENTITIES.register(eventBus);
        ENTITIES.register(eventBus);
        SOUND_EVENTS.register(eventBus);

        ModEntityTypes.register();
        ModItems.register();
        ModBlocks.register();
        ModTileEntitiesTypes.register();
        ModRecipeSerializers.register();
        ModMenuTypes.register();
        TugRoutePacketHandler.register();
        VehicleTrackerPacketHandler.register();
        VehiclePacketHandler.register();
        ModSounds.register();
    }
}
```

**Target code:**
```java
package dev.murad.shipping.setup;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public class Registration {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, ShippingMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(BuiltInRegistries.MENU, ShippingMod.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ShippingMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ShippingMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ShippingMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ShippingMod.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ShippingMod.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        CONTAINERS.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        TILE_ENTITIES.register(eventBus);
        ENTITIES.register(eventBus);
        SOUND_EVENTS.register(eventBus);

        ModEntityTypes.register();
        ModItems.register();
        ModBlocks.register();
        ModTileEntitiesTypes.register();
        ModRecipeSerializers.register();
        ModMenuTypes.register();
        // REMOVE: TugRoutePacketHandler.register() — deleted in Phase 3
        // REMOVE: VehicleTrackerPacketHandler.register() — deleted in Phase 3
        // REMOVE: VehiclePacketHandler.register() — deleted in Phase 3
        ModSounds.register();
    }
}
```

**Key changes:**
1. `ForgeRegistries.BLOCKS` → `BuiltInRegistries.BLOCK` (and similarly for all 7 registries)
2. `DeferredRegister.create(IForgeRegistry<T>, modId)` → `DeferredRegister.create(BuiltInRegistries.X, modId)`
3. `register()` → `register(IEventBus eventBus)` — no more `FMLJavaModLoadingContext.get()`
4. Remove the private `create(IForgeRegistry<T>)` helper — no longer needed since each register is created inline
5. Remove networking handler registration calls (3 lines) — these are deleted in Phase 3
6. Import changes:
   - REMOVE: `net.minecraftforge.registries.ForgeRegistries`
   - REMOVE: `net.minecraftforge.registries.IForgeRegistry`
   - REMOVE: `net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext`
   - REMOVE: all 3 network handler imports
   - ADD: `net.minecraft.core.registries.BuiltInRegistries`
   - CHANGE: `net.minecraftforge.registries.DeferredRegister` → `net.neoforged.neoforge.registries.DeferredRegister`
   - CHANGE: `net.minecraftforge.eventbus.api.IEventBus` → `net.neoforged.bus.api.IEventBus`

**Registry name mapping (complete):**

| Forge (`ForgeRegistries.X`) | NeoForge (`BuiltInRegistries.X`) |
|----|-----|
| `ForgeRegistries.BLOCKS` | `BuiltInRegistries.BLOCK` |
| `ForgeRegistries.ITEMS` | `BuiltInRegistries.ITEM` |
| `ForgeRegistries.ENTITY_TYPES` | `BuiltInRegistries.ENTITY_TYPE` |
| `ForgeRegistries.MENU_TYPES` | `BuiltInRegistries.MENU` |
| `ForgeRegistries.BLOCK_ENTITY_TYPES` | `BuiltInRegistries.BLOCK_ENTITY_TYPE` |
| `ForgeRegistries.RECIPE_SERIALIZERS` | `BuiltInRegistries.RECIPE_SERIALIZER` |
| `ForgeRegistries.SOUND_EVENTS` | `BuiltInRegistries.SOUND_EVENT` |

---

### Step 3: `RegistryObject<T>` → `DeferredHolder<BaseType, T>` (7 files)

Every `RegistryObject<T>` in the setup package must become `DeferredHolder<BaseType, T>`. The `DeferredHolder` is the NeoForge replacement and is fully API-compatible (still has `.get()`, works as `Supplier<T>`).

**Import change (all 7 files):**
```java
// REMOVE:
import net.minecraftforge.registries.RegistryObject;
// ADD:
import net.neoforged.neoforge.registries.DeferredHolder;
```

**Type change pattern:**
```java
// BEFORE:
public static final RegistryObject<EntityType<ChestBargeEntity>> CHEST_BARGE = ...
// AFTER:
public static final DeferredHolder<EntityType<?>, EntityType<ChestBargeEntity>> CHEST_BARGE = ...
```

The base type parameter is the registry type (e.g., `EntityType<?>`, `Block`, `Item`, `MenuType<?>`, `BlockEntityType<?>`, `SoundEvent`, `RecipeSerializer<?>`).

#### 3a: `ModEntityTypes.java` (18 fields)

Each field changes from `RegistryObject<EntityType<X>>` to `DeferredHolder<EntityType<?>, EntityType<X>>`.

**Example (one field — same pattern for all 18):**
```java
// BEFORE:
public static final RegistryObject<EntityType<ChestBargeEntity>> CHEST_BARGE =
        Registration.ENTITIES.register("barge", ...);

// AFTER:
public static final DeferredHolder<EntityType<?>, EntityType<ChestBargeEntity>> CHEST_BARGE =
        Registration.ENTITIES.register("barge", ...);
```

**All 18 fields:** `CHEST_BARGE`, `BARREL_BARGE`, `CHUNK_LOADER_BARGE`, `FISHING_BARGE`, `FLUID_TANK_BARGE`, `SEATER_BARGE`, `VACUUM_BARGE`, `STEAM_TUG`, `ENERGY_TUG`, `CHEST_CAR`, `BARREL_CAR`, `SEATER_CAR`, `FLUID_CAR`, `CHUNK_LOADER_CAR`, `STEAM_LOCOMOTIVE`, `ENERGY_LOCOMOTIVE`.

#### 3b: `ModItems.java` (21 fields + helper method + PRIVATE_TAB_REGISTRY)

Each field changes from `RegistryObject<Item>` (or `RegistryObject<T extends Item>`) to `DeferredHolder<Item, Item>` (or `DeferredHolder<Item, T>`).

**Field changes:**
```java
// BEFORE:
public static final RegistryObject<Item> CONDUCTORS_WRENCH = register(...);

// AFTER:
public static final DeferredHolder<Item, Item> CONDUCTORS_WRENCH = register(...);
```

**Helper method return type:**
```java
// BEFORE:
private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> itemSupplier, List<ResourceKey<CreativeModeTab>> tabs) {
    var res = Registration.ITEMS.register(name, itemSupplier);
    // ...
    return res;
}

// AFTER:
private static <T extends Item> DeferredHolder<Item, T> register(String name, Supplier<T> itemSupplier, List<ResourceKey<CreativeModeTab>> tabs) {
    var res = Registration.ITEMS.register(name, itemSupplier);
    // ...
    return res;
}
```

**PRIVATE_TAB_REGISTRY type change:**
```java
// BEFORE:
private static final MultiMap<ResourceKey<CreativeModeTab>, RegistryObject<? extends Item>> PRIVATE_TAB_REGISTRY = new MultiMap<>();

// AFTER:
private static final MultiMap<ResourceKey<CreativeModeTab>, DeferredHolder<Item, ? extends Item>> PRIVATE_TAB_REGISTRY = new MultiMap<>();
```

**Import change [L-2]:**
```java
// REMOVE:
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
// ADD:
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
```

#### 3c: `ModBlocks.java` (15 fields + helper methods + PRIVATE_TAB_REGISTRY)

**Field changes:**
```java
// BEFORE:
public static final RegistryObject<Block> TUG_DOCK = register(...);

// AFTER:
public static final DeferredHolder<Block, Block> TUG_DOCK = register(...);
```

**Helper method changes:**
```java
// BEFORE:
private static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> block) {
    return Registration.BLOCKS.register(name, block);
}

private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, List<ResourceKey<CreativeModeTab>> tabs) {
    RegistryObject<T> ret = registerNoItem(name, block);
    RegistryObject<BlockItem> item = Registration.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties()));
    // ...
    return ret;
}

// AFTER:
private static <T extends Block> DeferredHolder<Block, T> registerNoItem(String name, Supplier<T> block) {
    return Registration.BLOCKS.register(name, block);
}

private static <T extends Block> DeferredHolder<Block, T> register(String name, Supplier<T> block, List<ResourceKey<CreativeModeTab>> tabs) {
    DeferredHolder<Block, T> ret = registerNoItem(name, block);
    DeferredHolder<Item, BlockItem> item = Registration.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties()));
    // ...
    return ret;
}
```

**PRIVATE_TAB_REGISTRY type change:**
```java
// BEFORE:
private static final MultiMap<ResourceKey<CreativeModeTab>, RegistryObject<? extends ItemLike>> PRIVATE_TAB_REGISTRY = new MultiMap<>();

// AFTER:
private static final MultiMap<ResourceKey<CreativeModeTab>, DeferredHolder<?, ? extends ItemLike>> PRIVATE_TAB_REGISTRY = new MultiMap<>();
```

**Import change [L-2]:**
```java
// REMOVE:
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
// ADD:
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
```

#### 3d: `ModMenuTypes.java` (6 fields)

**Field changes:**
```java
// BEFORE:
public static final RegistryObject<MenuType<SteamHeadVehicleContainer<SteamTugEntity>>> TUG_CONTAINER = ...

// AFTER:
public static final DeferredHolder<MenuType<?>, MenuType<SteamHeadVehicleContainer<SteamTugEntity>>> TUG_CONTAINER = ...
```

**IForgeMenuType → IMenuTypeExtension (Phase 8 concern but import changes here):**
```java
// BEFORE:
import net.minecraftforge.common.extensions.IForgeMenuType;
// ... IForgeMenuType.create(...)

// AFTER:
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
// ... IMenuTypeExtension.create(...)
```

Note: The `IForgeMenuType.create()` → `IMenuTypeExtension.create()` rename is a simple 1:1 API change. The lambda signature stays the same. We do this here because it's in the same file and is a pure rename.

**All 6 fields:** `TUG_CONTAINER`, `ENERGY_TUG_CONTAINER`, `STEAM_LOCOMOTIVE_CONTAINER`, `ENERGY_LOCOMOTIVE_CONTAINER`, `FISHING_BARGE_CONTAINER`, `TUG_ROUTE_CONTAINER`.

#### 3e: `ModTileEntitiesTypes.java` (8 fields + helper method)

**Field changes:**
```java
// BEFORE:
public static final RegistryObject<BlockEntityType<TugDockTileEntity>> TUG_DOCK = register(...);

// AFTER:
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TugDockTileEntity>> TUG_DOCK = register(...);
```

**Helper method:**
```java
// BEFORE:
private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(
        String name, BlockEntityType.BlockEntitySupplier<T> factory, RegistryObject<? extends Block> block) {
    return Registration.TILE_ENTITIES.register(name, () ->
            BlockEntityType.Builder.of(factory, block.get()).build(null));
}

// AFTER:
private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
        String name, BlockEntityType.BlockEntitySupplier<T> factory, DeferredHolder<Block, ? extends Block> block) {
    return Registration.TILE_ENTITIES.register(name, () ->
            BlockEntityType.Builder.of(factory, block.get()).build(null));
}
```

Note: The `block` parameter type also changes from `RegistryObject` to `DeferredHolder` since `ModBlocks` fields are now `DeferredHolder`.

**All 8 fields:** `TUG_DOCK`, `BARGE_DOCK`, `LOCOMOTIVE_DOCK`, `CAR_DOCK`, `VESSEL_DETECTOR`, `FLUID_HOPPER`, `VESSEL_CHARGER`, `RAPID_HOPPER`.

#### 3f: `ModSounds.java` (3 fields)

```java
// BEFORE:
public static final RegistryObject<SoundEvent> STEAM_TUG_WHISTLE = Registration.SOUND_EVENTS.register(...);

// AFTER:
public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_TUG_WHISTLE = Registration.SOUND_EVENTS.register(...);
```

**All 3 fields:** `STEAM_TUG_WHISTLE`, `TUG_DOCKING`, `TUG_UNDOCKING`.

#### 3g: `ModRecipeSerializers.java` (2 fields)

```java
// BEFORE:
public static final RegistryObject<SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> TUG_ROUTE_COPY = ...

// AFTER:
public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AbstractRouteCopyRecipe>> TUG_ROUTE_COPY = ...
```

**All 2 fields:** `TUG_ROUTE_COPY`, `LOCO_ROUTE_COPY`.

Note: The `SimpleCraftingRecipeSerializer` constructor lambda `(loc, cat) -> ...` changes to `(cat) -> ...` in Phase 7 (recipe rework). Do NOT change the lambda signature in Phase 2.

---

### Step 4: `ModEventBusEvents.java` — Annotation + Import Update

**Current code:**
```java
package dev.murad.shipping.setup;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        // ... event.put() calls
    }
}
```

**Target code:**
```java
package dev.murad.shipping.setup;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;  // [L-3]
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        // ... event.put() calls (UNCHANGED)
    }
}
```

**Key changes:**
1. `@Mod.EventBusSubscriber(...)` → `@EventBusSubscriber(...)` — this is now a top-level annotation, not nested in `@Mod`
2. `bus = Mod.EventBusSubscriber.Bus.MOD` → `bus = EventBusSubscriber.Bus.MOD`
3. Import: `net.minecraftforge.fml.common.Mod` → `net.neoforged.fml.common.EventBusSubscriber` [L-3]
4. Import: `net.minecraftforge.event.entity.EntityAttributeCreationEvent` → `net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent` [L-3]
5. Import: `net.minecraftforge.eventbus.api.SubscribeEvent` → `net.neoforged.bus.api.SubscribeEvent`

---

### Step 5: `ModTags.java` — "forge" → "c" Namespace [M-4]

NeoForge 1.21.x uses the `"c"` (Common/Conventional) namespace for shared tags instead of `"forge"`.

**Current code:**
```java
package dev.murad.shipping.setup;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final class Blocks {
        // (commented-out code)
    }

    public static final class Items {
        public static final TagKey<Item> WRENCHES = forge("tools/wrench");

        private static TagKey<Item> forge(String path) {
            return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
        }

        private static TagKey<Item> mod(String path) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(ShippingMod.MOD_ID, path));
        }
    }
}
```

**Target code:**
```java
package dev.murad.shipping.setup;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final class Blocks {
        // (commented-out code — unchanged)
    }

    public static final class Items {
        public static final TagKey<Item> WRENCHES = conventional("tools/wrench");

        private static TagKey<Item> conventional(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }

        private static TagKey<Item> mod(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, path));
        }
    }
}
```

**Key changes:**
1. Rename helper `forge(String)` → `conventional(String)` for clarity
2. `new ResourceLocation("forge", path)` → `ResourceLocation.fromNamespaceAndPath("c", path)` — both the namespace AND the ResourceLocation constructor change
3. Also update `mod()` helper: `new ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath(...)` (Phase 9B mechanical change, but do it here since we're editing the file)

**Tag path note:** The wrench tag path `"tools/wrench"` may need verification against NeoForge 1.21.1 conventional tag names. The NeoForge convention is `c:tools/wrench` — confirm this exists in the NeoForge tag data.

---

## Complete Import Change Summary

| File | Remove Import | Add Import |
|------|--------------|------------|
| **ShippingMod.java** | `net.minecraftforge.fml.ModLoadingContext` | `net.neoforged.fml.ModContainer` |
| | `net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext` | `net.neoforged.bus.api.IEventBus` |
| | `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod` |
| | `net.minecraftforge.fml.config.ModConfig` | `net.neoforged.fml.config.ModConfig` |
| | `net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent` | `net.neoforged.fml.event.lifecycle.FMLClientSetupEvent` |
| **Registration.java** | `net.minecraftforge.registries.ForgeRegistries` | `net.minecraft.core.registries.BuiltInRegistries` |
| | `net.minecraftforge.registries.IForgeRegistry` | *(removed, no replacement)* |
| | `net.minecraftforge.registries.DeferredRegister` | `net.neoforged.neoforge.registries.DeferredRegister` |
| | `net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext` | *(removed, no replacement)* |
| | `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` |
| | 3x network handler imports | *(removed)* |
| **ModEntityTypes.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| **ModItems.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| | `net.minecraftforge.event.BuildCreativeModeTabContentsEvent` | `net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent` |
| **ModBlocks.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| | `net.minecraftforge.event.BuildCreativeModeTabContentsEvent` | `net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent` |
| **ModMenuTypes.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| | `net.minecraftforge.common.extensions.IForgeMenuType` | `net.neoforged.neoforge.common.extensions.IMenuTypeExtension` |
| **ModTileEntitiesTypes.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| **ModSounds.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| **ModRecipeSerializers.java** | `net.minecraftforge.registries.RegistryObject` | `net.neoforged.neoforge.registries.DeferredHolder` |
| **ModEventBusEvents.java** | `net.minecraftforge.event.entity.EntityAttributeCreationEvent` | `net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent` |
| | `net.minecraftforge.eventbus.api.SubscribeEvent` | `net.neoforged.bus.api.SubscribeEvent` |
| | `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.EventBusSubscriber` |

---

## Gap Coverage

| Gap ID | Description | Where Addressed |
|--------|-------------|----------------|
| **M-4** | ModTags "forge" → "c" namespace | Step 5 |
| **L-2** | BuildCreativeModeTabContentsEvent import | Steps 3b, 3c |
| **L-3** | EntityAttributeCreationEvent import | Step 4 |

---

## Verification Steps

After completing all changes:

1. **Compile check:** `./gradlew build` — registration files should compile. Other files (networking, capabilities, etc.) may still fail; that's expected for later phases.

2. **Verify DeferredRegister fires:** Search for any remaining `FMLJavaModLoadingContext` references:
   ```bash
   grep -r "FMLJavaModLoadingContext" src/main/java/
   # Expected: 0 results
   ```

3. **Verify no remaining ForgeRegistries:**
   ```bash
   grep -r "ForgeRegistries" src/main/java/
   # Expected: 0 results (only BuiltInRegistries should remain)
   ```

4. **Verify no remaining RegistryObject in setup/:**
   ```bash
   grep -r "RegistryObject" src/main/java/dev/murad/shipping/setup/
   # Expected: 0 results
   ```
   Note: `RegistryObject` may still exist OUTSIDE setup/ — those are addressed in later phases or may be in files that reference setup fields (they'll need to update their local variable types too).

5. **Verify "forge" namespace removed from tags:**
   ```bash
   grep -r '"forge"' src/main/java/dev/murad/shipping/setup/ModTags.java
   # Expected: 0 results
   ```

6. **Verify annotation update:**
   ```bash
   grep -r "Mod.EventBusSubscriber" src/main/java/dev/murad/shipping/setup/
   # Expected: 0 results (should be @EventBusSubscriber)
   ```

7. **Runtime test (after all phases):** Launch with `./gradlew runClient`, verify:
   - Mod loads without errors in log
   - All entities spawnable
   - All blocks placeable
   - All items in creative tabs
   - Sounds play

---

## Execution Order

The steps above can be done in a single commit since they're all interdependent (Registration.java's signature change requires ShippingMod.java to change simultaneously). Recommended order:

1. `Registration.java` (changes method signature — everything depends on this)
2. `ShippingMod.java` (passes `IEventBus` to `Registration.register()`)
3. All 7 `Mod*.java` files with `RegistryObject` → `DeferredHolder` (independent of each other)
4. `ModEventBusEvents.java` (annotation + imports)
5. `ModTags.java` (namespace change)

All changes should be in a **single atomic commit** to avoid intermediate broken states.

---

## Files NOT Changed in Phase 2

These files use `RegistryObject` or Forge APIs but are handled in later phases:
- `ModItemModelProperties.java` — no `RegistryObject` fields, only uses `.get()` on setup fields (works with `DeferredHolder`)
- `EntityItemMap.java` — only uses `.get()` on setup fields (works with `DeferredHolder`)
- Networking files (`*PacketHandler.java`) — deleted in Phase 3
- Entity files — capabilities in Phase 4, entity data in Phase 5
- Recipe/data gen files — Phase 7
- GUI/screen files — Phase 8
