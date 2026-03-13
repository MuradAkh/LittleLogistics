# Phase 9A: Vanilla/NeoForge API Reworks

**Phase:** 9A (split from original Phase 9 "Mechanical Cleanup")
**Prerequisites:** Phases 1–8 complete
**Effort:** MEDIUM — 3 distinct subsystems, ~16 files total
**Risk:** Minecart physics rewrite requires careful testing

---

## Overview

Phase 9A covers API changes that are **not mechanical renames**. The original Phase 9 lumped together trivial `ResourceLocation` find-and-replace (80 usages) with complex API reworks requiring code logic changes. This phase isolates the non-trivial work:

1. **AbstractMinecart.Type removal & push() physics rewrite** (Gap C-3)
2. **PartEntity multipart system relocation** (Gap M-3)
3. **ItemStackHandler import relocation** (Gap L-1)

Phase 9B (mechanical cleanup) handles the remaining `ResourceLocation` renames.

---

## Prerequisites

- **Phase 8 (GUI / Screens) complete** — all compilation errors from Phases 1–8 resolved
- `./gradlew build` passes before starting Phase 9A
- Phase 1.5 (Vanilla API Compat) must have already addressed `collisionExtendsVertically()` removal in `AbstractTrainCarEntity.getOnPos()` (line 323)

---

## Section 1: AbstractMinecart.Type Physics Rewrite (Gap C-3)

### What Changed in MC 1.21

- `AbstractMinecart.Type` enum — **completely removed**
- `getMinecartType()` method — **removed from AbstractMinecart**
- `isPoweredCart()` — **behavior/signature changed**; NeoForge 1.21 uses a different mechanism for powered-vs-unpowered collision resolution

### Current Code Analysis

#### `AbstractTrainCarEntity.getMinecartType()` (line 559)

```java
@Override
public Type getMinecartType() {
    // Why does this even exist
    return Type.CHEST;
}
```

Dead code — the comment acknowledges it. Forge required this override; NeoForge 1.21 does not. **Action: Delete entirely.**

#### `SeaterCarEntity.getMinecartType()` (line 120)

```java
public AbstractMinecart.Type getMinecartType() {
    return AbstractMinecart.Type.RIDEABLE;
}
```

Same situation — Forge-era mandatory override. **Action: Delete entirely.**

#### `AbstractLocomotiveEntity.isPoweredCart()` (line 353)

```java
@Override
public boolean isPoweredCart() {
    return true;
}
```

This marks locomotives as "powered" minecarts. In MC 1.21, `isPoweredCart()` is removed from the vanilla API. However, LittleLogistics uses this in its own `push()` physics (see below). **Action: Keep as a mod-internal method (not an @Override). Remove `@Override` annotation.**

#### `AbstractTrainCarEntity.push()` — The Critical Rewrite (lines 244–310)

This is the core collision physics for train cars. The method is a modified copy of vanilla `AbstractMinecart.push()` with LittleLogistics-specific stalling behavior added.

**Lines 285 and 289 use `isPoweredCart()`:**

```java
// Line 285: other cart is powered, this is not → this yields
if (((AbstractMinecart)pEntity).isPoweredCart() && !this.isPoweredCart()) {
    this.setDeltaMovement(vec32.multiply(0.2D, 1.0D, 0.2D));
    this.push(vec33.x - d0, 0.0D, vec33.z - d1);
    pEntity.setDeltaMovement(vec33.multiply(0.95D, 1.0D, 0.95D));
}
// Line 289: this is powered, other is not → other yields
else if (!((AbstractMinecart)pEntity).isPoweredCart() && this.isPoweredCart()) {
    pEntity.setDeltaMovement(vec33.multiply(0.2D, 1.0D, 0.2D));
    pEntity.push(vec32.x + d0, 0.0D, vec32.z + d1);
    this.setDeltaMovement(vec32.multiply(0.95D, 1.0D, 0.95D));
}
// Line 293: both equal → share momentum
else {
    // ...average velocities...
}
```

**Three-branch physics logic:**
1. Other is powered, this is not → this cart slows (0.2x), absorbs other's momentum; other retains speed (0.95x)
2. This is powered, other is not → mirror of case 1
3. Both equal power → average their momenta

### Migration Steps

1. **Delete `getMinecartType()`** from `AbstractTrainCarEntity` (line 559) and `SeaterCarEntity` (line 120). Remove all `AbstractMinecart.Type` imports.

2. **Convert `isPoweredCart()` to mod-internal method.** In MC 1.21, `isPoweredCart()` no longer exists on `AbstractMinecart`. Options:
   - **Option A (recommended):** Define `isPoweredCart()` as a new method on `AbstractTrainCarEntity` (default `false`), overridden in `AbstractLocomotiveEntity` (returns `true`). Remove `@Override` annotations. The `push()` logic continues to work unchanged because the cast target is now the mod's own type.
   - **Option B:** Replace `isPoweredCart()` checks with `instanceof AbstractLocomotiveEntity`. Simpler but less extensible.

3. **Update `push()` method (lines 273–300).** The `(AbstractMinecart)pEntity` cast at lines 285 and 289 must change:
   - If the other entity is an `AbstractTrainCarEntity`, call the mod-internal `isPoweredCart()`.
   - If it's a vanilla `AbstractMinecart`, treat it as unpowered (vanilla minecarts are never "powered" in 1.21's model).
   - Adjust the cast: `((AbstractMinecart)pEntity).isPoweredCart()` → `(pEntity instanceof AbstractTrainCarEntity atc ? atc.isPoweredCart() : false)`

4. **Verify `AbstractTrainCarEntity.getOnPos()` (line 323).** The call to `blockstate.collisionExtendsVertically(this.level(), blockpos1, this)` is removed in MC 1.21. This should have been handled in Phase 1.5. If not: replace with `blockstate.getCollisionShape(this.level(), blockpos1).max(Direction.Axis.Y) > 1.0` or remove the optimization (just return `blockpos`).

### Files Changed

| File | Lines | Change |
|------|-------|--------|
| `AbstractTrainCarEntity.java` | 559–562 | Delete `getMinecartType()` |
| `AbstractTrainCarEntity.java` | 285, 289 | Rewrite `isPoweredCart()` calls to use mod-internal method |
| `AbstractTrainCarEntity.java` | 273 | Update `instanceof AbstractMinecart` check |
| `AbstractLocomotiveEntity.java` | 352–355 | Remove `@Override`, keep method body |
| `SeaterCarEntity.java` | 120–122 | Delete `getMinecartType()` |

---

## Section 2: PartEntity Multipart System (Gap M-3)

### What Changed

- `net.minecraftforge.entity.PartEntity` → `net.neoforged.neoforge.entity.PartEntity`
- `recreateFromPacket(ClientboundAddEntityPacket)` — verify signature in NeoForge 1.21
- `VehicleFrontPart.defineSynchedData()` → `defineSynchedData(SynchedEntityData.Builder builder)` (Phase 5 signature change applies here too)
- `VehicleFrontPart.getAddEntityPacket()` — PartEntity in NeoForge may handle this differently

### Current Code

**VehicleFrontPart.java** — Extends `PartEntity<Entity>`. A hitbox entity positioned in front of tugs/locomotives so players can interact with the front of multi-block vehicles.

Key methods:
- `defineSynchedData()` (line 89) — empty, but signature must update
- `getAddEntityPacket()` (line 40) — throws `UnsupportedOperationException`; PartEntities shouldn't need packets
- `hurt()`, `interact()`, `updatePosition()` — delegate to parent entity

**AbstractTugEntity.java** — Uses PartEntity:
- Import: `net.minecraftforge.entity.PartEntity` (line 45)
- Field: `VehicleFrontPart frontHitbox` (line 79)
- Constructor: `frontHitbox = new VehicleFrontPart(this)` (line 97)
- `getParts()` (lines 363–365): returns `new PartEntity<?>[]{frontHitbox}`
- `isMultipartEntity()` (line 358): returns `true`
- `recreateFromPacket()` (lines 378–380): calls `super.recreateFromPacket(p)` then `frontHitbox.setId(p.getId())`

**AbstractLocomotiveEntity.java** — Identical pattern:
- Import: line 42
- Field: line 63
- Constructors: lines 92, 98
- `getParts()` (lines 341–343)
- `isMultipartEntity()` (lines 347–350)
- `isPoweredCart()` (lines 353–355)
- `recreateFromPacket()` (lines 358–361)

### Migration Steps

1. **Update VehicleFrontPart.java:**
   - Change import: `net.minecraftforge.entity.PartEntity` → `net.neoforged.neoforge.entity.PartEntity`
   - Update `defineSynchedData()` → `defineSynchedData(SynchedEntityData.Builder builder)` (empty body stays empty)
   - Verify `getAddEntityPacket()` — in NeoForge 1.21, PartEntity may not require this override. If the base class handles it, delete the override. If it's still abstract, keep the throw.
   - Verify `getDimensions(Pose)` — check if return type changed in 1.21

2. **Update AbstractTugEntity.java:**
   - Change import at line 45: `net.minecraftforge.entity.PartEntity` → `net.neoforged.neoforge.entity.PartEntity`
   - Verify `recreateFromPacket()` signature — if `ClientboundAddEntityPacket` changed, update accordingly
   - `getParts()`, `isMultipartEntity()` — verify return types unchanged

3. **Update AbstractLocomotiveEntity.java:**
   - Same changes as AbstractTugEntity

4. **Verify consumer sites:**
   - `SpringItem.java:58–59` — `target instanceof VehicleFrontPart` — no change needed (instanceof works on the class, not the import)
   - `ForgeEventHandler.java:77` — same
   - `PlayerTrainChunkManager.java:118–119` — `entity.getParts()` — no change needed

### Files Changed

| File | Lines | Change |
|------|-------|--------|
| `VehicleFrontPart.java` | 14 | Import: `minecraftforge` → `neoforged.neoforge` |
| `VehicleFrontPart.java` | 40–42 | Verify/update `getAddEntityPacket()` |
| `VehicleFrontPart.java` | 89 | `defineSynchedData()` → `defineSynchedData(Builder)` |
| `AbstractTugEntity.java` | 45 | Import: `minecraftforge` → `neoforged.neoforge` |
| `AbstractTugEntity.java` | 378–380 | Verify `recreateFromPacket()` signature |
| `AbstractLocomotiveEntity.java` | 42 | Import: `minecraftforge` → `neoforged.neoforge` |
| `AbstractLocomotiveEntity.java` | 358–361 | Verify `recreateFromPacket()` signature |

---

## Section 3: ItemStackHandler Import Relocation (Gap L-1)

### What Changed

`net.minecraftforge.items.ItemStackHandler` → `net.neoforged.neoforge.items.ItemStackHandler`

This is a mechanical import rename across 14 files. The class API is unchanged.

### All 14 Files

1. `src/main/java/dev/murad/shipping/util/InventoryUtils.java`
2. `src/main/java/dev/murad/shipping/util/ItemHandlerVanillaContainerWrapper.java`
3. `src/main/java/dev/murad/shipping/util/FuelItemStackHandler.java`
4. `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java`
5. `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/EnergyTugEntity.java`
6. `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/SteamTugEntity.java`
7. `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/ChestBargeEntity.java`
8. `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FishingBargeEntity.java`
9. `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/EnergyLocomotiveEntity.java`
10. `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/SteamLocomotiveEntity.java`
11. `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java`
12. `src/main/java/dev/murad/shipping/entity/custom/train/wagon/ChestCarEntity.java`
13. `src/main/java/dev/murad/shipping/entity/custom/HeadVehicle.java`
14. `src/main/java/dev/murad/shipping/entity/custom/TrainInventoryProvider.java`

### Migration Step

Single find-and-replace across all 14 files:
```
net.minecraftforge.items.ItemStackHandler → net.neoforged.neoforge.items.ItemStackHandler
```

Also check for related imports that may accompany `ItemStackHandler`:
- `net.minecraftforge.items.IItemHandler` → `net.neoforged.neoforge.items.IItemHandler`
- `net.minecraftforge.items.wrapper.*` → `net.neoforged.neoforge.items.wrapper.*`

---

## Verification Steps

### After Section 1 (Minecart Physics)

1. `./gradlew build` — must compile with no `AbstractMinecart.Type` or `getMinecartType()` references
2. `./gradlew runClient` — spawn a locomotive and wagons:
   - Verify collision between locomotive (powered) and wagon (unpowered) — locomotive should push wagon aside
   - Verify collision between two wagons — momentum should be shared equally
   - Verify collision between locomotive and vanilla minecart — locomotive should push it
3. Verify `getOnPos()` still works (train cars stay on rails, don't fall through)

### After Section 2 (PartEntity)

1. `./gradlew build` — must compile with `net.neoforged.neoforge.entity.PartEntity`
2. `./gradlew runClient` — spawn a tug and locomotive:
   - Click on front hitbox of tug → should open tug GUI
   - Hit front hitbox of locomotive → should damage the locomotive
   - Lead rope attached to front hitbox → should attach to tug
   - Verify `VehicleFrontPart` position updates as vehicle moves

### After Section 3 (ItemStackHandler)

1. `./gradlew build` — no import errors
2. No behavioral testing needed — import-only change

### Final Gate

```bash
./gradlew build   # Must pass
./gradlew runData  # Verify data gen still works
```

---

## Complete Files Touched (Phase 9A)

| File | Section | Change Type |
|------|---------|-------------|
| `AbstractTrainCarEntity.java` | 1 | Logic rewrite (push physics, delete getMinecartType) |
| `AbstractLocomotiveEntity.java` | 1, 2, 3 | Remove @Override isPoweredCart, PartEntity import, ItemStackHandler import |
| `SeaterCarEntity.java` | 1 | Delete getMinecartType() |
| `VehicleFrontPart.java` | 2 | PartEntity import, defineSynchedData signature |
| `AbstractTugEntity.java` | 2, 3 | PartEntity import, ItemStackHandler import |
| `EnergyTugEntity.java` | 3 | ItemStackHandler import |
| `SteamTugEntity.java` | 3 | ItemStackHandler import |
| `ChestBargeEntity.java` | 3 | ItemStackHandler import |
| `FishingBargeEntity.java` | 3 | ItemStackHandler import |
| `EnergyLocomotiveEntity.java` | 3 | ItemStackHandler import |
| `SteamLocomotiveEntity.java` | 3 | ItemStackHandler import |
| `ChestCarEntity.java` | 3 | ItemStackHandler import |
| `HeadVehicle.java` | 3 | ItemStackHandler import |
| `TrainInventoryProvider.java` | 3 | ItemStackHandler import |
| `InventoryUtils.java` | 3 | ItemStackHandler import |
| `ItemHandlerVanillaContainerWrapper.java` | 3 | ItemStackHandler import |
| `FuelItemStackHandler.java` | 3 | ItemStackHandler import |

**Total: 17 files** (3 files overlap between sections)
