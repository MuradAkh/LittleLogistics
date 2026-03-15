# StallingCapability as NeoForge EntityCapability

## Motivation

Expose `StallingCapability` as a NeoForge `EntityCapability` so companion mods can:
- Query stalling/docking/freezing state on existing vehicles
- Control stalling on head vehicles from new tail vehicle types
- Provide stalling on new head vehicle types
- Register their own entity types for the capability

## Approach

Wrap the existing `StallingCapability` interface as an `EntityCapability`. All entities that implement the interface register themselves to return `this`. Internal code continues using `instanceof` checks; the capability is the cross-mod API.

## Design

### Capability constant on the interface

```java
public interface StallingCapability {
    EntityCapability<StallingCapability, Void> ENTITY_CAPABILITY =
        EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("littlelogistics", "stalling"),
            StallingCapability.class);

    boolean isDocked();
    void dock(double x, double y, double z);
    void undock();
    boolean isStalled();
    void stall();
    void unstall();
    boolean isFrozen();
    void freeze();
    void unfreeze();
}
```

Context type is `Void` — stalling state is not direction-dependent.

### Registration in CapabilityRegistration.java

Register all 16 entity types that implement `StallingCapability`:

**Head vehicles (4):** STEAM_TUG, ENERGY_TUG, STEAM_LOCOMOTIVE, ENERGY_LOCOMOTIVE
**Tail vessels (7):** CHEST_BARGE, BARREL_BARGE, CHUNK_LOADER_BARGE, FISHING_BARGE, FLUID_TANK_BARGE, SEATER_BARGE, VACUUM_BARGE
**Tail rail (5):** CHEST_CAR, BARREL_CAR, SEATER_CAR, FLUID_CAR, CHUNK_LOADER_CAR

Each registration returns `this` since all entities directly implement the interface:
```java
event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_TUG.get(), (entity, ctx) -> entity);
```

For entity types where the generic type is a superclass (e.g., `EntityType<AbstractLocomotiveEntity>`), a cast is needed:
```java
event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_LOCOMOTIVE.get(),
    (entity, ctx) -> (StallingCapability) entity);
```

### Internal code: no changes

All internal `instanceof StallingCapability` checks remain as-is:
- `LinkingHandler.tickLoad()` — uses `instanceof`
- `AbstractTrainCarEntity.push()` — uses `instanceof`
- `AbstractBargeEntity.delegateStalling()` / `AbstractWagonEntity.delegateStalling()` — uses `instanceof`

These operate on entities within the mod's own hierarchy where the type relationship is known.

### Bug fix: stallNonTicking()

Re-implement `LinkingHandler.stallNonTicking()` which was gutted during migration. Uses `instanceof StallingCapability` (internal pattern) and `ServerLevel.isPositionEntityTicking()` to stall head vehicles when followers are outside entity-ticking range.

## Entity types to register

| Entity Type | Generic Type | Implements Via |
|---|---|---|
| STEAM_TUG | `SteamTugEntity` | `AbstractTugEntity` |
| ENERGY_TUG | `EnergyTugEntity` | `AbstractTugEntity` |
| STEAM_LOCOMOTIVE | `AbstractLocomotiveEntity` | `AbstractLocomotiveEntity` |
| ENERGY_LOCOMOTIVE | `AbstractLocomotiveEntity` | `AbstractLocomotiveEntity` |
| CHEST_BARGE | `ChestBargeEntity` | `AbstractBargeEntity` |
| BARREL_BARGE | `ChestBargeEntity` | `AbstractBargeEntity` |
| CHUNK_LOADER_BARGE | `ChunkLoaderBargeEntity` | `AbstractBargeEntity` |
| FISHING_BARGE | `FishingBargeEntity` | `AbstractBargeEntity` |
| FLUID_TANK_BARGE | `FluidTankBargeEntity` | `AbstractBargeEntity` |
| SEATER_BARGE | `SeaterBargeEntity` | `AbstractBargeEntity` |
| VACUUM_BARGE | `VacuumBargeEntity` | `AbstractBargeEntity` |
| CHEST_CAR | `ChestCarEntity` | `AbstractWagonEntity` |
| BARREL_CAR | `ChestCarEntity` | `AbstractWagonEntity` |
| SEATER_CAR | `SeaterCarEntity` | `AbstractWagonEntity` |
| FLUID_CAR | `FluidTankCarEntity` | `AbstractWagonEntity` |
| CHUNK_LOADER_CAR | `ChunkLoaderCarEntity` | `AbstractWagonEntity` |

## Companion mod usage

```java
// Query capability on another entity
StallingCapability cap = entity.getCapability(StallingCapability.ENTITY_CAPABILITY);
if (cap != null) {
    cap.stall();
}

// Register a new entity type in companion mod's RegisterCapabilitiesEvent
event.registerEntity(StallingCapability.ENTITY_CAPABILITY, MY_CUSTOM_BARGE.get(), (entity, ctx) -> entity);
```
