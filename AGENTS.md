# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Little Logistics is a Minecraft NeoForge mod (MC 1.21.1, NeoForge) that adds water and rail logistics: tugs with barges and locomotives with train cars. Mod ID: `littlelogistics`. Base package: `dev.murad.shipping`.

## Build Commands

```bash
./gradlew build          # Compile and build the mod JAR
./gradlew runData        # Run data generators (block states, models, tags, recipes, loot tables)
./gradlew runClient      # Launch Minecraft client with the mod loaded
./gradlew runServer      # Launch dedicated server with the mod loaded
```

CI runs: `./gradlew wrapper && ./gradlew runData && ./gradlew build`

Requires Java 21. Uses NeoGradle 7.0.171, Gradle 8.10. Gradle allocates 3GB heap (`-Xmx3G`).

## Architecture

### Registration System (`setup/`)

All game objects use NeoForge's `DeferredRegister` pattern with `DeferredHolder` references. `Registration.java` is the central hub that registers all deferred registers to the mod event bus. Individual registrations live in:
- `ModEntityTypes` - entity type definitions (tugs, barges, locomotives, wagons)
- `ModBlocks` / `ModItems` - blocks and items
- `ModTileEntitiesTypes` - block entities (tile entities)
- `ModMenuTypes` - container/menu types for GUIs (uses `IMenuTypeExtension`)
- `ModDataComponents` - data component types for item data (`TUG_ROUTE`, `LOCO_ROUTE`, `SPRING_LINKED`)
- `ModSounds`, `ModRecipeSerializers`, `ModTags`

### Entity Hierarchy

Two parallel vehicle systems share common abstractions:

**Water vehicles** (`entity/custom/vessel/`):
- `VesselEntity` → base for all water entities
- `AbstractTugEntity` → `SteamTugEntity`, `EnergyTugEntity` (head vehicles that pull barges)
- `AbstractBargeEntity` → `ChestBargeEntity`, `FluidTankBargeEntity`, `FishingBargeEntity`, `SeaterBargeEntity`, `ChunkLoaderBargeEntity`, `VacuumBargeEntity`

**Rail vehicles** (`entity/custom/train/`):
- `AbstractTrainCarEntity` → base for all rail entities
- `AbstractLocomotiveEntity` → `SteamLocomotiveEntity`, `EnergyLocomotiveEntity` (head vehicles)
- `AbstractWagonEntity` → `ChestCarEntity`, `FluidTankCarEntity`, `SeaterCarEntity`, `ChunkLoaderCarEntity`

Both tugs and locomotives implement `HeadVehicle` interface for shared routing/engine behavior. Vehicles are linked together using `LinkableEntity`/`LinkableEntityHead` (spring physics in `SpringPhysicsUtil`).

Steam variants burn fuel; energy variants use NeoForge Energy (`ReadWriteEnergyStorage`). Capabilities are registered via `RegisterCapabilitiesEvent` (no more `LazyOptional`).

### Routing System (`util/`)

- `TugRoute`/`TugRouteNode` - water route waypoints (has `Codec` + `StreamCodec`)
- `LocoRoute`/`LocoRouteNode` - rail route waypoints (has `Codec` + `StreamCodec`)
- Routes are stored as items (`TugRouteItem`, `LocoRouteItem`) using Data Components via `ModDataComponents`
- Route classes retain `toNBT()`/`fromNBT()` for entity serialization (`addAdditionalSaveData`/`readAdditionalSaveData`)

### Block Systems (`block/`)

- `dock/` - Docking blocks for loading/unloading barges (head dock + tail dock pattern)
- `rail/` - Custom rail types: `SwitchRail`, `JunctionRail`, `TeeJunctionRail`, docking rails for locomotives/wagons
- `fluid/` - Fluid hopper for fluid transfer
- `energy/` - Vessel charger for energy vehicles
- `rapidhopper/` - Fast item hopper
- `vesseldetector/` - Redstone output when vessel passes
- `guiderail/` - Guide rails for water navigation

### Client-Side

- Entity models in `entity/models/` with separate insert models for cargo visualization
- Entity renderers in `entity/render/`
- Container screens in `entity/container/` (Abstract*Screen classes)
- Data generators in `data/` produce block states, item models, tags, recipes, loot tables into `src/generated/resources/`

### Networking (`network/`)

Client-server packets implemented as `CustomPacketPayload` records with `StreamCodec`: `SetEnginePacket`, `SetRouteTagPacket`, `EnrollVehiclePacket`, plus `VehicleTrackerPacketHandler` for client-side tracking. Uses `PacketDistributor` for sending.

### Configuration (`ShippingConfig.java`)

Three config types: Common, Client, Server (registered as `littlelogistics-common.toml`, etc.).

### Mod Compatibility (`compatibility/`)

Create mod integration via `CreateCompatibility` and `CapabilityInjector`. (Deferred until Create releases for NeoForge 1.21.1.)

## Key Conventions

- Uses Lombok (via `io.freefair.lombok` plugin) - expect `@Getter`, `@Setter`, etc.
- Uses Parchment mappings for readable Minecraft method/field names
- Mixin support enabled (`org.spongepowered.mixin` plugin)
- Access transformer at `src/main/resources/META-INF/accesstransformer.cfg`
- Chunk loading for trains managed in `global/` package (`PlayerTrainChunkManager`)

## NeoForge 1.21.1 Migration Notes

Migrated from Forge 1.20.1 → NeoForge 1.21.1. Key API changes:
- `DeferredHolder` replaces `RegistryObject`; `BuiltInRegistries` replaces `ForgeRegistries`
- Entity `defineSynchedData(SynchedEntityData.Builder builder)` uses Builder pattern
- BlockEntity `loadAdditional(CompoundTag, HolderLookup.Provider)` replaces `load(CompoundTag)`
- Item data uses Data Components (`DataComponentType` with Codecs) instead of raw NBT (`getTag()`/`setTag()`)
- Capabilities use `RegisterCapabilitiesEvent` — no more `LazyOptional` or `getCapability()` overrides
- Screens registered via `RegisterMenuScreensEvent`; menus opened via `player.openMenu()` (not `NetworkHooks`)
- `ResourceLocation.fromNamespaceAndPath()` / `ResourceLocation.parse()` replace constructors
- `PartEntity` moved to `net.neoforged.neoforge.entity`
- `AbstractMinecart.Type` removed; `isPoweredCart()` is now a mod-internal method
