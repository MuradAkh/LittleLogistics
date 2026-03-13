# Phase 5: Entity Data & BlockEntity Serialization

## Overview

NeoForge 1.21.1 introduces two breaking API changes to data serialization:

1. **`defineSynchedData(SynchedEntityData.Builder builder)`** — Entity synched data registration switches from calling `entity.getEntityData().define(...)` to using a `Builder` parameter: `builder.define(...)`. The method signature gains a `SynchedEntityData.Builder` parameter and no longer calls `super.defineSynchedData()` implicitly through entity data — the super call still happens but passes the builder up.

2. **BlockEntity `HolderLookup.Provider`** — `load()`, `saveAdditional()`, `getUpdateTag()`, and `handleUpdateTag()` all gain a `HolderLookup.Provider` parameter for registry-aware serialization. The method `load(CompoundTag)` becomes `loadAdditional(CompoundTag, HolderLookup.Provider)`. The `onDataPacket` pattern is replaced by `handleUpdateTag`.

## Prerequisites

- **Phase 4 complete** (Capability removal) — Phase 4 removes `LazyOptional`/`Capability` from BlockEntity files, so those files must be stable before Phase 5 modifies their serialization methods.

---

## Part A: defineSynchedData() — Builder Pattern Migration

### Summary of Changes

All 7 entity classes change their `defineSynchedData()` signature and body:
- **Old:** `protected void defineSynchedData()`
- **New:** `protected void defineSynchedData(SynchedEntityData.Builder builder)`
- All `this.getEntityData().define(KEY, val)` and `entityData.define(KEY, val)` calls become `builder.define(KEY, val)`
- `super.defineSynchedData()` becomes `super.defineSynchedData(builder)`

### Step 1: LinkingHandler.defineSynchedData() helper refactor

**File:** `src/main/java/dev/murad/shipping/util/LinkingHandler.java` (line 106)

```java
// CURRENT (line 106-109):
public static void defineSynchedData(Entity entity,
        EntityDataAccessor<Integer> dominantID,
        EntityDataAccessor<Integer> dominatedID) {
    entity.getEntityData().define(dominantID, -1);
    entity.getEntityData().define(dominatedID, -1);
}

// TARGET:
public static void defineSynchedData(SynchedEntityData.Builder builder,
        EntityDataAccessor<Integer> dominantID,
        EntityDataAccessor<Integer> dominatedID) {
    builder.define(dominantID, -1);
    builder.define(dominatedID, -1);
}
```

**New import:** `net.minecraft.network.syncher.SynchedEntityData`
**Removed import:** `net.minecraft.world.entity.Entity` (if no longer used — verify other usages first)

### Step 2: VesselEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/vessel/VesselEntity.java` (line 168)

```java
// CURRENT (lines 168-172):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    this.getEntityData().define(COLOR_DATA, -1);
    LinkingHandler.defineSynchedData(this, DOMINANT_ID, DOMINATED_ID);
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(COLOR_DATA, -1);
    LinkingHandler.defineSynchedData(builder, DOMINANT_ID, DOMINATED_ID);
}
```

### Step 3: AbstractTrainCarEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/train/AbstractTrainCarEntity.java` (line 198)

```java
// CURRENT (lines 198-203):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    getEntityData().define(DOMINANT_ID, -1);
    getEntityData().define(DOMINATED_ID, -1);
    getEntityData().define(COLOR_DATA, -1);
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DOMINANT_ID, -1);
    builder.define(DOMINATED_ID, -1);
    builder.define(COLOR_DATA, -1);
}
```

**Note:** This class defines linking IDs inline (unlike VesselEntity which uses LinkingHandler helper). This inconsistency is pre-existing and not a migration concern — either pattern works.

### Step 4: AbstractTugEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java` (line 462)

```java
// CURRENT (lines 462-466):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(INDEPENDENT_MOTION, false);
    entityData.define(OWNER, "");
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(INDEPENDENT_MOTION, false);
    builder.define(OWNER, "");
}
```

### Step 5: AbstractLocomotiveEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java` (line 252)

```java
// CURRENT (lines 252-256):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(INDEPENDENT_MOTION, false);
    entityData.define(OWNER, "");
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(INDEPENDENT_MOTION, false);
    builder.define(OWNER, "");
}
```

### Step 6: FluidTankBargeEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FluidTankBargeEntity.java` (line 64)

```java
// CURRENT (lines 64-68):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(FLUID_TYPE, "minecraft:empty");
    entityData.define(VOLUME, 0);
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(FLUID_TYPE, "minecraft:empty");
    builder.define(VOLUME, 0);
}
```

### Step 7: FluidTankCarEntity

**File:** `src/main/java/dev/murad/shipping/entity/custom/train/wagon/FluidTankCarEntity.java` (line 63)

```java
// CURRENT (lines 63-67):
@Override
protected void defineSynchedData() {
    super.defineSynchedData();
    entityData.define(FLUID_TYPE, "minecraft:empty");
    entityData.define(VOLUME, 0);
}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(FLUID_TYPE, "minecraft:empty");
    builder.define(VOLUME, 0);
}
```

### Step 8: VehicleFrontPart

**File:** `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/VehicleFrontPart.java` (line 89)

```java
// CURRENT (lines 89-91):
@Override
protected void defineSynchedData() {

}

// TARGET:
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {

}
```

**Note:** Empty body — still needs the signature update for compile compatibility.

---

## Part B: BlockEntity Serialization — HolderLookup.Provider

### Gap M-6: getUpdateTag/handleUpdateTag also gain HolderLookup.Provider

In NeoForge 1.21.1, the full set of BlockEntity serialization method signature changes:

| Old Signature | New Signature |
|---|---|
| `load(CompoundTag tag)` | `loadAdditional(CompoundTag tag, HolderLookup.Provider registries)` |
| `saveAdditional(CompoundTag tag)` | `saveAdditional(CompoundTag tag, HolderLookup.Provider registries)` |
| `getUpdateTag()` | `getUpdateTag(HolderLookup.Provider registries)` |
| `onDataPacket(Connection, ClientboundBlockEntityDataPacket)` | `handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)` |

**Key points:**
- `load()` is renamed to `loadAdditional()` AND gains the provider param
- `saveAdditional()` keeps its name but gains the provider param
- `getUpdateTag()` gains the provider param; internal calls to `saveAdditional()` must pass it through
- `onDataPacket()` is replaced by `handleUpdateTag()` with the provider param
- The `HolderLookup.Provider` is available but not used by this mod's current serialization logic (FluidTank NBT, EnergyStorage NBT) — it's passed through to super calls and available for future registry-dependent serialization

### Step 9: FluidHopperTileEntity

**File:** `src/main/java/dev/murad/shipping/block/fluid/FluidHopperTileEntity.java`

#### 9a. load() -> loadAdditional() (line 71)

```java
// CURRENT (lines 71-74):
@Override
public void load(CompoundTag tag) {
    super.load(tag);
    this.getTank().readFromNBT(tag);
}

// TARGET:
@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    this.getTank().readFromNBT(tag);
}
```

#### 9b. saveAdditional() (line 77)

```java
// CURRENT (lines 77-80):
@Override
public void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    this.getTank().writeToNBT(tag);
}

// TARGET:
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    this.getTank().writeToNBT(tag);
}
```

#### 9c. getUpdateTag() (line 84) [M-6]

```java
// CURRENT (lines 84-88):
@Nonnull
@Override
public CompoundTag getUpdateTag() {
    var tag = new CompoundTag();
    saveAdditional(tag);
    return tag;
}

// TARGET:
@Nonnull
@Override
public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    var tag = new CompoundTag();
    saveAdditional(tag, registries);
    return tag;
}
```

#### 9d. onDataPacket() -> handleUpdateTag() (line 96) [M-6]

```java
// CURRENT (lines 96-98):
@Override
public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
    this.load(packet.getTag());
}

// TARGET:
@Override
public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    loadAdditional(tag, registries);
}
```

#### 9e. getUpdatePacket() (line 91)

```java
// CURRENT (lines 91-93):
@Override
public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
}

// TARGET — no change needed:
// ClientboundBlockEntityDataPacket.create(this) still works in NeoForge 1.21.1
```

**New import:** `net.minecraft.core.HolderLookup`
**Remove imports:** `net.minecraft.network.Connection` (no longer needed after onDataPacket removal)

### Step 10: VesselChargerTileEntity

**File:** `src/main/java/dev/murad/shipping/block/energy/VesselChargerTileEntity.java`

This file has `load()` and `saveAdditional()` but NO `getUpdateTag()`/`onDataPacket()`.

#### 10a. load() -> loadAdditional() (line 71)

```java
// CURRENT (lines 71-74):
@Override
public void load(CompoundTag compound) {
    super.load(compound);
    internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
}

// TARGET:
@Override
protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
    super.loadAdditional(compound, registries);
    internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
}
```

#### 10b. saveAdditional() (line 77)

```java
// CURRENT (lines 77-82):
@Override
public void saveAdditional(CompoundTag compound) {
    CompoundTag energyNBT = new CompoundTag();
    internalBattery.addAdditionalSaveData(energyNBT);
    super.saveAdditional(compound);
    compound.put("energy_storage", energyNBT);
}

// TARGET:
@Override
protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
    CompoundTag energyNBT = new CompoundTag();
    internalBattery.addAdditionalSaveData(energyNBT);
    super.saveAdditional(compound, registries);
    compound.put("energy_storage", energyNBT);
}
```

**New import:** `net.minecraft.core.HolderLookup`

---

## Files Touched (10 files)

| # | File | Changes |
|---|------|---------|
| 1 | `src/main/java/dev/murad/shipping/util/LinkingHandler.java` | `defineSynchedData()` signature: `Entity` param -> `Builder` param |
| 2 | `src/main/java/dev/murad/shipping/entity/custom/vessel/VesselEntity.java` | `defineSynchedData(Builder)`, update body |
| 3 | `src/main/java/dev/murad/shipping/entity/custom/train/AbstractTrainCarEntity.java` | `defineSynchedData(Builder)`, update body |
| 4 | `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java` | `defineSynchedData(Builder)`, update body |
| 5 | `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java` | `defineSynchedData(Builder)`, update body |
| 6 | `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FluidTankBargeEntity.java` | `defineSynchedData(Builder)`, update body |
| 7 | `src/main/java/dev/murad/shipping/entity/custom/train/wagon/FluidTankCarEntity.java` | `defineSynchedData(Builder)`, update body |
| 8 | `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/VehicleFrontPart.java` | `defineSynchedData(Builder)` signature only |
| 9 | `src/main/java/dev/murad/shipping/block/fluid/FluidHopperTileEntity.java` | `loadAdditional`, `saveAdditional`, `getUpdateTag`, replace `onDataPacket` with `handleUpdateTag` |
| 10 | `src/main/java/dev/murad/shipping/block/energy/VesselChargerTileEntity.java` | `loadAdditional`, `saveAdditional` |

## EntityDataAccessor Field Declarations (no changes needed)

The static `EntityDataAccessor` field declarations using `SynchedEntityData.defineId(Class, Serializer)` do **not** change in NeoForge 1.21.1. These 14 fields remain as-is:

- `VesselEntity`: COLOR_DATA, DOMINANT_ID, DOMINATED_ID
- `AbstractTrainCarEntity`: COLOR_DATA, DOMINANT_ID, DOMINATED_ID
- `AbstractTugEntity`: INDEPENDENT_MOTION, OWNER
- `AbstractLocomotiveEntity`: INDEPENDENT_MOTION, OWNER
- `FluidTankBargeEntity`: VOLUME, FLUID_TYPE
- `FluidTankCarEntity`: VOLUME, FLUID_TYPE

## What Does NOT Change

- **Entity `readAdditionalSaveData(CompoundTag)` / `addAdditionalSaveData(CompoundTag)`** — these 18 methods across 14 files keep their existing signature in NeoForge 1.21.1. No changes needed.
- **5 BlockEntities with no serialization** (VesselDetectorTileEntity, BargeDockTileEntity, TugDockTileEntity, LocomotiveDockTileEntity, TrainCarDockTileEntity) — no changes needed.

## Verification Steps

1. **Compile check:** `./gradlew build` — all 10 files must compile without errors.
2. **Search for stale patterns:**
   - `grep -rn "defineSynchedData()" --include="*.java"` should return 0 results (all should have `Builder` param now)
   - `grep -rn "entity.getEntityData().define\|getEntityData().define\|entityData.define" --include="*.java"` inside `defineSynchedData` methods should return 0 results
   - `grep -rn "public void load(CompoundTag" --include="*.java" src/main/java/dev/murad/shipping/block/` should return 0 results
   - `grep -rn "onDataPacket" --include="*.java"` should return 0 results
3. **Runtime smoke test:** `./gradlew runClient` — place a fluid hopper, vessel charger; spawn a tug, locomotive, fluid barge, fluid tank car. Verify:
   - Fluid hopper persists fluid across chunk unload/reload
   - Vessel charger persists energy across save/load
   - Entity color syncs to client correctly
   - Vehicle linking (dominant/dominated IDs) syncs correctly
   - Tug/locomotive owner and independent motion sync correctly

## Execution Order

1. Start with **LinkingHandler** (Step 1) — other files depend on its new signature
2. Do **VesselEntity** (Step 2) and **AbstractTrainCarEntity** (Step 3) next — they are base classes
3. Then subclasses in any order (Steps 4-8)
4. BlockEntity files (Steps 9-10) are independent of entity changes — can be done in parallel

## Risk Assessment

- **Low risk**: All changes are mechanical signature updates. No logic changes.
- **No data migration needed**: NBT format does not change; only method signatures change.
- **Regression risk**: Forgetting to pass `builder` or `registries` to super calls would cause silent data loss. The compile-time check catches missing parameters but not missing super calls — verify manually.
