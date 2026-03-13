# Phase 4: Capabilities Migration Plan

**Risk Level:** CRITICAL (27 files affected)
**Prerequisites:** Phase 3 (Networking) complete and `./gradlew build` passing

---

## 1. Phase Overview

Phase 4 replaces the Forge Capability system with NeoForge's new capability API:

| Forge 1.20.1 | NeoForge 1.21.1 |
|---------------|-----------------|
| `LazyOptional<T>` wrapper fields | Direct nullable returns or plain fields |
| `getCapability(Capability<T>, Direction)` overrides on each entity/BE | Centralized `RegisterCapabilitiesEvent` handler |
| `ForgeCapabilities.ITEM_HANDLER` / `.ENERGY` / `.FLUID_HANDLER` | `Capabilities.ItemHandler.ENTITY` / `.EnergyStorage.ENTITY` / `.FluidHandler.ENTITY` (and `.BLOCK` variants) |
| `entity.getCapability(cap).map(...)` / `.ifPresent(...)` / `.resolve()` | `entity.getCapability(cap, context)` returns `@Nullable T` directly |
| `ICapabilityProvider.initCapabilities()` for items | `RegisterCapabilitiesEvent.registerItem()` |
| `Capability<T>` generic token (CapabilityManager) | `EntityCapability<T, C>` / `BlockCapability<T, C>` / `ItemCapability<T, C>` |

**Core principle:** Capabilities move from per-class overrides to a single centralized registration file. LazyOptional is completely eliminated.

---

## 2. Complete Inventory of getCapability() Overrides (14 total)

### Entity getCapability() overrides (12)

| # | File | Line | Capability | What it returns |
|---|------|------|-----------|-----------------|
| 1 | `entity/custom/vessel/VesselEntity.java` | 518 | `ITEM_HANDLER` | `LazyOptional.empty()` (blocks mob armour slots) |
| 2 | `entity/custom/vessel/tug/SteamTugEntity.java` | 92 | `ITEM_HANDLER` | `handler` (fuel ItemStackHandler) |
| 3 | `entity/custom/vessel/tug/EnergyTugEntity.java` | 188 | `ENERGY` + `ITEM_HANDLER` | `holder` (battery) + `handler` (energy item) |
| 4 | `entity/custom/vessel/barge/FishingBargeEntity.java` | 231 | (none - delegates to super) | Pass-through to VesselEntity |
| 5 | `entity/custom/vessel/barge/FluidTankBargeEntity.java` | 118 | `FLUID_HANDLER` | `holder` (FluidTank) |
| 6 | `entity/custom/train/locomotive/SteamLocomotiveEntity.java` | 123 | `ITEM_HANDLER` | `handler` (fuel ItemStackHandler) |
| 7 | `entity/custom/train/locomotive/EnergyLocomotiveEntity.java` | 75 | `ITEM_HANDLER` + `ENERGY` | `energyItemHandlerOpt` + `internalBatteryOpt` |
| 8 | `entity/custom/train/wagon/ChestCarEntity.java` | 108 | `ITEM_HANDLER` | `handler` (27-slot chest) |
| 9 | `entity/custom/train/wagon/FluidTankCarEntity.java` | 123 | `FLUID_HANDLER` | `holder` (FluidTank) |
| 10 | `entity/custom/train/wagon/SeaterCarEntity.java` | 126 | Create compat only | `createCompatMinecartControllerCapability` |

### BlockEntity getCapability() overrides (2)

| # | File | Line | Capability | What it returns |
|---|------|------|-----------|-----------------|
| 11 | `block/energy/VesselChargerTileEntity.java` | 40 | `ENERGY` | `holder` (ReadWriteEnergyStorage) |
| 12 | `block/fluid/FluidHopperTileEntity.java` | 60 | `FLUID_HANDLER` | `holder` (FluidTank) |

### Item capability via initCapabilities() (1)

| # | File | Line | Capability | What it returns |
|---|------|------|-----------|-----------------|
| 13 | `item/creative/CreativeCapacitor.java` | 57 | `ENERGY` | `CreativeEnergyStorage` (infinite energy) |

### Create compat capability (1, DEFERRED to Phase 10)

| # | File | Line | Capability | What it returns |
|---|------|------|-----------|-----------------|
| 14 | `compatibility/create/CapabilityInjector.java` | 41 | `MINECART_CONTROLLER` | `TrainCarController` |

**Total: 14 getCapability() sites** (13 migrated in Phase 4, 1 deferred to Phase 10)

---

## 3. Complete Inventory of LazyOptional Fields to Delete (15 total)

| # | File | Line | Field Name | Wraps |
|---|------|------|-----------|-------|
| 1 | `vessel/tug/SteamTugEntity.java` | 35 | `handler` | `LazyOptional<IItemHandler>` |
| 2 | `vessel/tug/EnergyTugEntity.java` | 36 | `handler` | `LazyOptional<IItemHandler>` |
| 3 | `vessel/tug/EnergyTugEntity.java` | 42 | `holder` | `LazyOptional<IEnergyStorage>` |
| 4 | `vessel/barge/FluidTankBargeEntity.java` | 47 | `holder` | `LazyOptional<IFluidHandler>` |
| 5 | `train/locomotive/SteamLocomotiveEntity.java` | 40 | `handler` | `LazyOptional<IItemHandler>` |
| 6 | `train/locomotive/EnergyLocomotiveEntity.java` | 36 | `energyItemHandlerOpt` | `LazyOptional<IItemHandler>` |
| 7 | `train/locomotive/EnergyLocomotiveEntity.java` | 42 | `internalBatteryOpt` | `LazyOptional<IEnergyStorage>` |
| 8 | `train/wagon/ChestCarEntity.java` | 29 | `handler` | `LazyOptional<IItemHandler>` |
| 9 | `train/wagon/FluidTankCarEntity.java` | 46 | `holder` | `LazyOptional<IFluidHandler>` |
| 10 | `train/wagon/SeaterCarEntity.java` | 26 | `createCompatMinecartControllerCapability` | `LazyOptional<?>` (Create compat) |
| 11 | `block/energy/VesselChargerTileEntity.java` | 30 | `holder` | `LazyOptional<IEnergyStorage>` |
| 12 | `block/fluid/FluidHopperTileEntity.java` | 47 | `holder` | `LazyOptional<IFluidHandler>` |
| 13 | `item/creative/CreativeCapacitor.java` | 63 | (inline in initCapabilities) | `LazyOptional.of(CreativeEnergyStorage::new)` |
| 14 | `item/creative/CreativeCapacitor.java` | 65 | (inline in initCapabilities) | `LazyOptional.empty()` |
| 15 | `compatibility/create/CapabilityInjector.java` | 42 | (return value) | `LazyOptional.of(TrainCarController)` |

---

## 4. CapabilityRegistration.java (New File)

**Path:** `src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java`

```java
package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.energy.VesselChargerTileEntity;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.item.creative.CreativeCapacitor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityRegistration {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {

        // === Entity capabilities: ITEM_HANDLER ===

        // SteamTugEntity -> fuel item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_TUG.get(),
            (entity, ctx) -> entity.getFuelItemHandler()
        );

        // EnergyTugEntity -> energy item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity.getItemHandler()
        );

        // SteamLocomotiveEntity -> fuel item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_LOCOMOTIVE.get(),
            (entity, ctx) -> entity.getFuelItemHandler()
        );

        // EnergyLocomotiveEntity -> energy item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> entity.getEnergyItemHandler()
        );

        // ChestCarEntity -> chest item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.CHEST_CAR.get(),
            (entity, ctx) -> entity.getRawHandler()
        );

        // BarrelCarEntity -> chest item handler (same class as ChestCarEntity)
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.BARREL_CAR.get(),
            (entity, ctx) -> entity.getRawHandler()
        );

        // === Entity capabilities: ENERGY ===

        // EnergyTugEntity -> internal battery
        event.registerEntity(
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity.getInternalBattery()
        );

        // EnergyLocomotiveEntity -> internal battery
        event.registerEntity(
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> entity.getInternalBattery()
        );

        // === Entity capabilities: FLUID_HANDLER ===

        // FluidTankBargeEntity -> fluid tank
        event.registerEntity(
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_TANK_BARGE.get(),
            (entity, ctx) -> entity.getTank()
        );

        // FluidTankCarEntity -> fluid tank
        event.registerEntity(
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_CAR.get(),
            (entity, ctx) -> entity.getTank()
        );

        // === BlockEntity capabilities ===

        // VesselChargerTileEntity -> energy storage
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModTileEntitiesTypes.VESSEL_CHARGER.get(),
            (blockEntity, direction) -> blockEntity.getInternalBattery()
        );

        // FluidHopperTileEntity -> fluid handler
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModTileEntitiesTypes.FLUID_HOPPER.get(),
            (blockEntity, direction) -> blockEntity.getTank()
        );

        // === Item capabilities ===

        // CreativeCapacitor -> infinite energy
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new CreativeCapacitor.CreativeEnergyStorage(),
            ModItems.CREATIVE_CAPACITOR.get()
        );
    }
}
```

**Required getter methods to add** (currently the underlying fields are accessed via LazyOptional; they need to be exposed directly):

| Entity/BE | Getter to add | Returns |
|-----------|--------------|---------|
| `SteamTugEntity` | `getFuelItemHandler()` | `fuelItemHandler` (FuelItemStackHandler) |
| `EnergyTugEntity` | `getItemHandler()` | `itemHandler` (ItemStackHandler) |
| `EnergyTugEntity` | `getInternalBattery()` | `internalBattery` (ReadWriteEnergyStorage) |
| `SteamLocomotiveEntity` | `getFuelItemHandler()` | `fuelItemHandler` (FuelItemStackHandler) |
| `EnergyLocomotiveEntity` | `getEnergyItemHandler()` | `energyItemHandler` (ItemStackHandler) |
| `EnergyLocomotiveEntity` | `getInternalBattery()` | `internalBattery` (ReadWriteEnergyStorage) |
| `FluidTankBargeEntity` | `getTank()` | `tank` (FluidTank) — already exists implicitly via `getFluidStack()`, add direct getter |
| `FluidTankCarEntity` | `getTank()` | `tank` (FluidTank) — same |
| `VesselChargerTileEntity` | `getInternalBattery()` | `internalBattery` (ReadWriteEnergyStorage) |
| `FluidHopperTileEntity` | `getTank()` | Already exists at line 66 |

**Note:** With Lombok `@Getter` on these fields, or simple public getter methods. ChestCarEntity already has `getRawHandler()`.

---

## 5. VesselEntity ITEM_HANDLER Block (Special Case)

`VesselEntity.getCapability()` at line 518 returns `LazyOptional.empty()` for `ITEM_HANDLER` to suppress the default mob armour/hands slots. In NeoForge 1.21.1:

- **Do NOT register** `ITEM_HANDLER` for any VesselEntity subtypes that don't have items (barges without items, FishingBargeEntity, etc.)
- Only register `ITEM_HANDLER` for entity types that actually provide one (SteamTugEntity, EnergyTugEntity)
- The VesselEntity `getCapability()` override is **deleted entirely** — NeoForge returns `null` by default if no provider is registered, which achieves the same blocking effect

---

## 6. All Consumer Call Sites (12 total) — Current vs Target Code

### Site 1: EnergyTugEntity.createHandler() (line 80)
**Purpose:** Validate energy items in fuel slot

```java
// CURRENT (Forge):
return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();

// TARGET (NeoForge):
return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
```

### Site 2: EnergyLocomotiveEntity.createHandler() (line 58)
**Purpose:** Validate energy items in fuel slot

```java
// CURRENT (Forge):
return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();

// TARGET (NeoForge):
return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
```

### Site 3: InventoryUtils.mayMoveIntoInventory() (line 49)
**Purpose:** Check if target entity has valid item slots

```java
// CURRENT (Forge):
boolean validSlot = e.getCapability(ForgeCapabilities.ITEM_HANDLER)
    .map(itemHandler -> airList.stream()
        .map(j -> itemHandler.isItemValid(j, stack))
        .reduce(false, Boolean::logicalOr)).orElse(true);

// TARGET (NeoForge):
var itemHandler = e.getCapability(Capabilities.ItemHandler.ENTITY, null);
boolean validSlot = itemHandler != null
    ? airList.stream().map(j -> itemHandler.isItemValid(j, stack)).reduce(false, Boolean::logicalOr)
    : true;
```

### Site 4: InventoryUtils.getEnergyCapabilityInSlot() (line 97)
**Purpose:** Get energy storage from item in slot

```java
// CURRENT (Forge):
LazyOptional<IEnergyStorage> capabilityLazyOpt = stack.getCapability(ForgeCapabilities.ENERGY);
if (capabilityLazyOpt.isPresent()) {
    Optional<IEnergyStorage> capabilityOpt = capabilityLazyOpt.resolve();
    if (capabilityOpt.isPresent()) {
        return capabilityOpt.get();
    }
}
return null;

// TARGET (NeoForge):
return stack.getCapability(Capabilities.EnergyStorage.ITEM);
// Returns @Nullable IEnergyStorage directly — the entire method collapses to one line
```

### Site 5: EnergyHeadVehicleContainer constructor (line 20)
**Purpose:** Add energy item slot to GUI

```java
// CURRENT (Forge):
entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
    .ifPresent(h -> addSlot(new SlotItemHandler(h, 0, 32, 35)
        .setBackground(EMPTY_ATLAS_LOC, ModItems.EMPTY_ENERGY)));

// TARGET (NeoForge):
var h = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
if (h != null) {
    addSlot(new SlotItemHandler(h, 0, 32, 35)
        .setBackground(EMPTY_ATLAS_LOC, ModItems.EMPTY_ENERGY));
}
```

### Site 6: SteamHeadVehicleContainer constructor (line 18)
**Purpose:** Add fuel item slot to GUI

```java
// CURRENT (Forge):
entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
    .ifPresent(h -> addSlot(new SlotItemHandler(h, 0, 42, 40)));

// TARGET (NeoForge):
var h = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
if (h != null) {
    addSlot(new SlotItemHandler(h, 0, 42, 40));
}
```

### Site 7: FishingBargeContainer constructor (line 23) [GAP L-8]
**Purpose:** Add fishing inventory slots — **DEAD CODE**

```java
// CURRENT (Forge):
fishingBargeEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> { ... });

// TARGET (NeoForge):
// DELETE ENTIRELY — this class is marked "// Unused" at line 12.
// VesselEntity blocks ITEM_HANDLER, so the ifPresent() lambda NEVER executes.
// The entire FishingBargeContainer class should be deleted or the slot setup rewritten
// to access the FishingBargeEntity's item storage directly (not via capability).
```

### Site 8: FluidHopperTileEntity.getExternalFluidHandler() (line 112)
**Purpose:** Get fluid handler from adjacent block entity

```java
// CURRENT (Forge):
Optional.ofNullable(this.level.getBlockEntity(pos))
    .map(tile -> tile.getCapability(ForgeCapabilities.FLUID_HANDLER))
    .flatMap(LazyOptional::resolve)
    .map(Optional::of).orElseGet(() -> IVesselLoader.getEntityCapability(...));

// TARGET (NeoForge):
Optional.ofNullable(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null))
    .or(() -> IVesselLoader.getEntityCapability(pos, Capabilities.FluidHandler.ENTITY, this.level));
// NeoForge block capabilities are queried from the Level, not the BlockEntity
```

### Site 9: FluidHopperTileEntity.hold() (line 133)
**Purpose:** Check if vehicle has compatible fluid for docking hold

```java
// CURRENT (Forge):
return vehicle.getCapability(ForgeCapabilities.FLUID_HANDLER).map(iFluidHandler -> {
    switch (mode) { ... }
}).orElse(false);

// TARGET (NeoForge):
var fluidHandler = vehicle.getCapability(Capabilities.FluidHandler.ENTITY, null);
if (fluidHandler == null) return false;
switch (mode) { ... }
```

### Site 10: VesselChargerTileEntity.tryChargeEntity() (line 62-63)
**Purpose:** Find and charge entity via IVesselLoader

```java
// CURRENT (Forge):
IVesselLoader.getEntityCapability(pos, ForgeCapabilities.ENERGY, level).map(...)

// TARGET (NeoForge):
IVesselLoader.getEntityCapability(pos, Capabilities.EnergyStorage.ENTITY, level).map(...)
// After IVesselLoader redesign (see Section 7)
```

### Site 11: VesselChargerTileEntity.hold() (line 86)
**Purpose:** Check if vehicle needs charging for docking hold

```java
// CURRENT (Forge):
return vehicle.getCapability(ForgeCapabilities.ENERGY).map(energyHandler -> { ... }).orElse(false);

// TARGET (NeoForge):
var energyHandler = vehicle.getCapability(Capabilities.EnergyStorage.ENTITY, null);
if (energyHandler == null) return false;
// inline the switch logic
```

### Site 12: IVesselLoader.getEntityCapability() + entityPredicate() (lines 19-41)
**Purpose:** Find entities with a given capability at a block position — see Section 7

---

## 7. Gap M-5: IVesselLoader Generic Capability<T> Redesign

**Current signature:**
```java
static <T> Optional<T> getEntityCapability(BlockPos pos, Capability<T> capability, Level level)
static boolean entityPredicate(Entity entity, BlockPos pos, Capability<?> capability)
```

**Problem:** `Capability<T>` is the Forge generic token. NeoForge replaces this with `EntityCapability<T, C>` which has a different type structure: `entity.getCapability(EntityCapability<T, C> cap, @Nullable C context)`.

**Target signature:**
```java
static <T> Optional<T> getEntityCapability(BlockPos pos, EntityCapability<T, @Nullable Void> capability, Level level)
static <T> boolean entityPredicate(Entity entity, BlockPos pos, EntityCapability<T, @Nullable Void> capability)
```

**Target implementation:**
```java
static <T> Optional<T> getEntityCapability(BlockPos pos, EntityCapability<T, @Nullable Void> capability, Level level) {
    List<Entity> entities = level.getEntities((Entity) null,
        getSearchBox(pos),
        e -> entityPredicate(e, pos, capability)
    );

    if (entities.isEmpty()) {
        return Optional.empty();
    } else {
        Entity entity = entities.get(0);
        return Optional.ofNullable(entity.getCapability(capability, null));
    }
}

static <T> boolean entityPredicate(Entity entity, BlockPos pos, EntityCapability<T, @Nullable Void> capability) {
    T cap = entity.getCapability(capability, null);
    if (cap == null) return false;
    if (entity instanceof LinkableEntity<?> l) {
        return l.allowDockInterface() && (l.getBlockPos().getX() == pos.getX() && l.getBlockPos().getZ() == pos.getZ());
    }
    return true;
}
```

**Callers to update:**
- `FluidHopperTileEntity.getExternalFluidHandler()` — passes `ForgeCapabilities.FLUID_HANDLER` -> `Capabilities.FluidHandler.ENTITY`
- `FluidHopperTileEntity.hold()` — same
- `VesselChargerTileEntity.tryChargeEntity()` — passes `ForgeCapabilities.ENERGY` -> `Capabilities.EnergyStorage.ENTITY`

---

## 8. Gap L-8: FishingBargeContainer Dead Code Cleanup

`FishingBargeContainer.java` is explicitly marked `// Unused` at line 12. The constructor at line 23 calls:
```java
fishingBargeEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> { ... });
```

This **never executes** because `VesselEntity.getCapability()` returns `LazyOptional.empty()` for `ITEM_HANDLER`, and `FishingBargeEntity` delegates to that via `super.getCapability()`.

**Action:** Delete `FishingBargeContainer.java` entirely. If it's referenced in `ModMenuTypes`, remove that registration too. Verify no screen class references it.

---

## 9. CreativeCapacitor initCapabilities() Removal

**Current:** `CreativeCapacitor` overrides `Item.initCapabilities()` (line 57) to return an anonymous `ICapabilityProvider` that provides `ForgeCapabilities.ENERGY`.

**Problem:** `initCapabilities()` is removed in NeoForge 1.21.1. Item capabilities are registered centrally.

**Target:**
1. Delete the entire `initCapabilities()` method (lines 55-68)
2. Make `CreativeEnergyStorage` package-visible (currently static inner class — already accessible)
3. Register in `CapabilityRegistration.java`:
   ```java
   event.registerItem(
       Capabilities.EnergyStorage.ITEM,
       (stack, ctx) -> new CreativeCapacitor.CreativeEnergyStorage(),
       ModItems.CREATIVE_CAPACITOR.get()
   );
   ```
4. Remove imports: `ICapabilityProvider`, `LazyOptional`, `Capability`, `ForgeCapabilities`, `Direction`, `CompoundTag`

---

## 10. SeaterCarEntity / Create Compat (DEFERRED to Phase 10)

`SeaterCarEntity.getCapability()` (line 126) only handles Create's `MINECART_CONTROLLER_CAPABILITY`. The `createCompatMinecartControllerCapability` field (line 26) is a `LazyOptional<?>`.

**Phase 4 action:**
- Delete the `getCapability()` override from SeaterCarEntity
- Delete the `createCompatMinecartControllerCapability` field
- Delete `initCompat()` method and its calls from constructors
- Delete `createCompatMinecartControllerCapability.invalidate()` from `remove()`
- Add a `// TODO: Phase 10 — re-add Create compat via RegisterCapabilitiesEvent` comment

**Phase 10 action (later):**
- Register Create's capability in `CapabilityRegistration.java` using NeoForge Create's API
- Update `CapabilityInjector.java` to return direct value (no LazyOptional)

---

## 11. ModEventHandler.java

Currently empty (line 7-8). The StallingCapability registration was already removed in run 561c8bb0.

**Phase 4 action:** No changes needed. The new `CapabilityRegistration.java` handles all capability registration via `@EventBusSubscriber`.

---

## 12. Step-by-Step Execution Order

### Step 1: Create CapabilityRegistration.java
1. Create `src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java` with the full `RegisterCapabilitiesEvent` handler (Section 4)
2. Add required getter methods to entity/BE classes

### Step 2: Delete LazyOptional fields and getCapability() overrides
For each of the 11 entity files + 2 block entity files:
1. Delete `LazyOptional<T>` field declarations
2. Delete `getCapability()` method override entirely
3. Remove imports: `LazyOptional`, `Capability`, `ForgeCapabilities` (if no longer used)

**Order:** Process in dependency order — VesselEntity first (base class), then subclasses.

| Order | File | Delete fields | Delete getCapability() |
|-------|------|---------------|----------------------|
| 1 | VesselEntity.java | (none) | lines 518-524 |
| 2 | SteamTugEntity.java | `handler` (line 35) | lines 92-98 |
| 3 | EnergyTugEntity.java | `handler` (line 36), `holder` (line 42) | lines 188-198 |
| 4 | FishingBargeEntity.java | (none) | lines 231-233 |
| 5 | FluidTankBargeEntity.java | `holder` (line 47) | lines 118-123 |
| 6 | SteamLocomotiveEntity.java | `handler` (line 40) | lines 123-129 |
| 7 | EnergyLocomotiveEntity.java | `energyItemHandlerOpt` (line 36), `internalBatteryOpt` (line 42) | lines 75-83 |
| 8 | ChestCarEntity.java | `handler` (line 29) | lines 108-114 |
| 9 | FluidTankCarEntity.java | `holder` (line 46) | lines 123-128 |
| 10 | SeaterCarEntity.java | `createCompatMinecartControllerCapability` (line 26) | lines 126-134 + initCompat() + remove() cleanup |
| 11 | VesselChargerTileEntity.java | `holder` (line 30) | lines 40-44 |
| 12 | FluidHopperTileEntity.java | `holder` (line 47) | lines 60-64 |

### Step 3: Migrate CreativeCapacitor
1. Delete `initCapabilities()` method (lines 55-68)
2. Make `CreativeEnergyStorage` package-visible or public
3. Remove unused imports
4. Already registered in CapabilityRegistration.java (Step 1)

### Step 4: Rewrite IVesselLoader
1. Change `Capability<T>` parameter to `EntityCapability<T, @Nullable Void>`
2. Replace `entity.getCapability(capability).resolve()` with `Optional.ofNullable(entity.getCapability(capability, null))`
3. Replace `.resolve().map(...)` with null-check pattern in `entityPredicate()`

### Step 5: Update all consumer call sites
Update the 12 consumer sites (Section 6) in this order:
1. `InventoryUtils.java` — 2 sites (lines 49, 97)
2. `EnergyHeadVehicleContainer.java` — 1 site (line 20)
3. `SteamHeadVehicleContainer.java` — 1 site (line 18)
4. `EnergyTugEntity.createHandler()` — 1 site (line 80)
5. `EnergyLocomotiveEntity.createHandler()` — 1 site (line 58)
6. `FluidHopperTileEntity.java` — 3 sites (lines 112, 133, 114 via IVesselLoader)
7. `VesselChargerTileEntity.java` — 2 sites (lines 62, 86)

### Step 6: Clean up dead code
1. Delete `FishingBargeContainer.java` (dead code — L-8)
2. Remove FishingBargeContainer registration from `ModMenuTypes.java` if present
3. Remove `SeaterCarEntity.initCompat()` and Create-related cleanup

### Step 7: Update imports globally
For all modified files:
- `net.minecraftforge.common.capabilities.Capability` -> remove
- `net.minecraftforge.common.capabilities.ForgeCapabilities` -> `net.neoforged.neoforge.capabilities.Capabilities`
- `net.minecraftforge.common.util.LazyOptional` -> remove
- `net.minecraftforge.common.capabilities.ICapabilityProvider` -> remove

---

## 13. Verification Steps

### Build verification
```bash
./gradlew build
```
Must pass before moving to Phase 5.

### Manual code verification checklist
- [ ] No remaining `LazyOptional` imports in any Phase 4 file
- [ ] No remaining `getCapability()` overrides in entity/BE classes (except Phase 10 deferred)
- [ ] No remaining `ForgeCapabilities` references (replaced with `Capabilities`)
- [ ] `CapabilityRegistration.java` compiles and is discovered by `@EventBusSubscriber`
- [ ] `CreativeCapacitor` no longer has `initCapabilities()`
- [ ] `FishingBargeContainer` deleted or rewritten
- [ ] `IVesselLoader` uses `EntityCapability<T, @Nullable Void>` not `Capability<T>`
- [ ] All consumer sites use null-check pattern (not `.map()` / `.ifPresent()`)

### Grep verification commands
```bash
# Should return 0 results in Phase 4 files:
grep -r "LazyOptional" src/main/java/dev/murad/shipping/ --include="*.java" | grep -v "create/"
grep -r "ForgeCapabilities" src/main/java/dev/murad/shipping/ --include="*.java" | grep -v "create/"
grep -r "getCapability(@Nonnull Capability" src/main/java/dev/murad/shipping/ --include="*.java" | grep -v "create/"
grep -r "initCapabilities" src/main/java/dev/murad/shipping/ --include="*.java"
```

### Runtime verification
- Energy tug/locomotive accept energy items in fuel slot
- Steam tug/locomotive accept fuel items
- Chest car allows hopper import/export
- Fluid tank barge/car allows fluid interaction
- Vessel charger charges energy vehicles
- Fluid hopper transfers fluid to/from barges
- Creative capacitor provides infinite energy when placed in energy vehicle

---

## 14. Complete Files Touched Summary

| Action | Files |
|--------|-------|
| **CREATE** | `setup/CapabilityRegistration.java` |
| **DELETE** | `entity/container/FishingBargeContainer.java` |
| **HEAVY EDIT** (delete override + field) | `VesselEntity.java`, `SteamTugEntity.java`, `EnergyTugEntity.java`, `FishingBargeEntity.java`, `FluidTankBargeEntity.java`, `SteamLocomotiveEntity.java`, `EnergyLocomotiveEntity.java`, `ChestCarEntity.java`, `FluidTankCarEntity.java`, `SeaterCarEntity.java`, `VesselChargerTileEntity.java`, `FluidHopperTileEntity.java` |
| **REWRITE** | `IVesselLoader.java` (generic parameter change), `CreativeCapacitor.java` (delete initCapabilities) |
| **CONSUMER UPDATE** | `InventoryUtils.java`, `EnergyHeadVehicleContainer.java`, `SteamHeadVehicleContainer.java` |
| **POSSIBLE CLEANUP** | `ModMenuTypes.java` (FishingBargeContainer registration) |
| **NO CHANGE** | `ModEventHandler.java` (already empty) |

**Total: ~20 files modified, 1 created, 1 deleted**
