# Phase 9B: Mechanical Cleanup — ResourceLocation Migration

**Phase:** 9B of 10
**Effort:** LOW
**Estimated files:** 30
**Estimated changes:** ~80 ResourceLocation constructor usages

---

## Overview

Phase 9B is a purely mechanical find-and-replace phase. In NeoForge 1.21.1, the `ResourceLocation` constructor is deprecated/removed. All `new ResourceLocation(...)` calls must be replaced with static factory methods:

- `new ResourceLocation(namespace, path)` → `ResourceLocation.fromNamespaceAndPath(namespace, path)`
- `new ResourceLocation(string)` → `ResourceLocation.parse(string)`

After replacement, run data generators and a full build to verify nothing was missed.

---

## Prerequisites

- **Phase 9A complete** — All vanilla/NeoForge API reworks (AbstractMinecart.Type, PartEntity, ItemStackHandler imports) must be done first.
- All prior phases (1 through 9A) must pass `./gradlew build`.

---

## Replacement Rules

### Rule 1: Two-argument constructor (namespace, path)

```
BEFORE: new ResourceLocation(ShippingMod.MOD_ID, "some/path")
AFTER:  ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "some/path")
```

Regex:
```
new ResourceLocation\(([^,]+),\s*([^)]+)\)
→
ResourceLocation.fromNamespaceAndPath($1, $2)
```

**~75 usages** across 28 files.

### Rule 2: Single-argument constructor (full string)

```
BEFORE: new ResourceLocation("minecraft:textures/block/lava_still.png")
BEFORE: new ResourceLocation(someVariable)
AFTER:  ResourceLocation.parse("minecraft:textures/block/lava_still.png")
AFTER:  ResourceLocation.parse(someVariable)
```

Regex:
```
new ResourceLocation\(([^,)]+)\)
→
ResourceLocation.parse($1)
```

**5 usages** across 5 files.

### Ordering

Apply Rule 1 (two-arg) FIRST, then Rule 2 (single-arg). This avoids Rule 2 accidentally matching the first argument of a two-arg call. Alternatively, apply Rule 1 globally, verify, then apply Rule 2.

---

## Automation Strategy

**Yes, this can be automated with regex find-replace**, with one caveat:

1. **Step 1:** Run two-arg replacement across all `.java` files:
   ```bash
   find src -name '*.java' -exec sed -i '' \
     's/new ResourceLocation(\([^,]*\), /ResourceLocation.fromNamespaceAndPath(\1, /g' {} +
   ```

2. **Step 2:** Run single-arg replacement across all `.java` files:
   ```bash
   find src -name '*.java' -exec sed -i '' \
     's/new ResourceLocation(\([^,)]*\))/ResourceLocation.parse(\1)/g' {} +
   ```

3. **Step 3:** Manual review of any edge cases (e.g., multi-line constructor calls, nested parentheses). Grep to verify zero remaining `new ResourceLocation(` calls:
   ```bash
   grep -rn 'new ResourceLocation(' src/main/java/
   ```
   Expected: only commented-out lines (3 total in FluidRenderUtil.java, SpringItem.java, ModTags.java).

**Caveat:** The `ModEntityTypes.java` usages are inside `.build(new ResourceLocation(...).toString())` chains. The two-arg regex handles this correctly since the inner `new ResourceLocation(ns, path)` is replaced regardless of surrounding context.

---

## Complete File List

### Two-argument usages (`fromNamespaceAndPath`)

| # | File | Line(s) | Count | Context |
|---|------|---------|-------|---------|
| 1 | `ShippingMod.java` | 54 | 1 | Texture helper method |
| 2 | `event/ForgeClientEventHandler.java` | 51 | 1 | Beacon beam texture |
| 3 | `data/client/ModItemModelProvider.java` | 50, 55, 74 | 3 | Item model predicates |
| 4 | `data/client/ModBlockStateProvider.java` | 32 | 1 | Block texture helper |
| 5 | `item/container/TugRouteScreen.java` | 26 | 1 | GUI texture |
| 6 | `item/container/StringInputScreen.java` | 23 | 1 | GUI texture |
| 7 | `entity/container/AbstractHeadVehicleScreen.java` | 15 | 1 | Registration GUI texture |
| 8 | `entity/container/EnergyHeadVehicleScreen.java` | 20 | 1 | Energy locomotive GUI |
| 9 | `entity/container/SteamHeadVehicleScreen.java` | 16 | 1 | Steam locomotive GUI |
| 10 | `entity/render/barge/AbstractVesselRenderer.java` | 29 | 1 | Chain texture |
| 11 | `entity/render/train/TrainCarRenderer.java` | 33, 40 | 2 | Chain texture + base texture |
| 12 | `entity/render/train/MultipartCarRenderer.java` | 55 | 1 | Chain texture |
| 13 | `entity/models/train/EnergyLocomotiveModel.java` | 21 | 1 | ModelLayerLocation |
| 14 | `entity/models/train/SteamLocomotiveModel.java` | 20 | 1 | ModelLayerLocation |
| 15 | `entity/models/train/BaseCarModel.java` | 20 | 1 | ModelLayerLocation |
| 16 | `entity/models/train/ChainModel.java` | 20 | 1 | ModelLayerLocation |
| 17 | `entity/models/train/ChainExtendedModel.java` | 19 | 1 | ModelLayerLocation |
| 18 | `entity/models/train/ChunkLoaderCarModel.java` | 19 | 1 | ModelLayerLocation |
| 19 | `entity/models/train/SeaterCarModel.java` | 17 | 1 | ModelLayerLocation |
| 20 | `entity/models/train/TrimCarModel.java` | 20 | 1 | ModelLayerLocation |
| 21 | `entity/models/insert/CubeInsertCarModel.java` | 17 | 1 | ModelLayerLocation |
| 22 | `entity/models/insert/CubeInsertBargeModel.java` | 17 | 1 | ModelLayerLocation |
| 23 | `entity/models/insert/SeaterInsertBargeModel.java` | 19 | 1 | ModelLayerLocation |
| 24 | `entity/models/insert/FishingInsertBargeModel.java` | 19, 20, 21 | 3 | ModelLayerLocation (3 states) |
| 25 | `entity/models/insert/FluidTankInsertCarModel.java` | 21 | 1 | ModelLayerLocation |
| 26 | `entity/models/insert/FluidTankInsertBargeModel.java` | 19 | 1 | ModelLayerLocation |
| 27 | `entity/models/insert/RingsInsertBargeModel.java` | 19 | 1 | ModelLayerLocation |
| 28 | `entity/models/vessel/SteamTugModel.java` | 20 | 1 | ModelLayerLocation |
| 29 | `entity/models/vessel/EmptyModel.java` | 21 | 1 | ModelLayerLocation |
| 30 | `entity/models/vessel/EnergyTugModel.java` | 18 | 1 | ModelLayerLocation |
| 31 | `entity/models/vessel/base/BaseBargeModel.java` | 18, 19, 20 | 3 | ModelLayerLocation (3 variants) |
| 32 | `entity/models/vessel/base/TrimBargeModel.java` | 21, 22, 23 | 3 | ModelLayerLocation (3 variants) |
| 33 | `setup/ModTags.java` | 25, 29 | 2 | Tag creation (note: line 25 "forge"→"c" is Phase 2/M-4) |
| 34 | `setup/ModSounds.java` | 11, 14, 17 | 3 | SoundEvent creation |
| 35 | `setup/ModItemModelProperties.java` | 14, 18, 22 | 3 | Item property overrides |
| 36 | `setup/ModItems.java` | 40, 41, 42 | 3 | Route/energy icons |
| 37 | `setup/ModEntityTypes.java` | 25, 32, 39, 46, 53, 60, 67, 74, 81, 89, 97, 105, 113, 121, 130, 139 | 16 | Entity type .build() |
| 38 | `network/TugRoutePacketHandler.java` | 22 | 1 | Channel location |
| 39 | `network/client/VehicleTrackerPacketHandler.java` | 22 | 1 | Channel location |
| 40 | `network/VehiclePacketHandler.java` | 19 | 1 | Channel location |

**Subtotal: ~75 usages across 40 source locations in 30 files**

### Single-argument usages (`parse`)

| # | File | Line | Context |
|---|------|------|---------|
| 1 | `global/TrainChunkManagerManager.java` | 44 | Dimension key from NBT string |
| 2 | `entity/custom/train/wagon/FluidTankCarEntity.java` | 114 | Fluid type from entity data |
| 3 | `entity/custom/vessel/barge/FluidTankBargeEntity.java` | 109 | Fluid type from entity data |
| 4 | `entity/custom/vessel/barge/FishingBargeEntity.java` | 58 | Loot table from config |
| 5 | `entity/container/FishingBargeScreen.java` | 12 | Vanilla container texture |

**Subtotal: 5 usages across 5 files**

### Commented-out lines (no action needed)

| File | Line | Status |
|------|------|--------|
| `util/FluidRenderUtil.java` | 183 | Commented out |
| `item/SpringItem.java` | 53 | Commented out |
| `setup/ModTags.java` | 13, 17 | Commented out |

---

## Note on Network Handler Files

The three network handler files (`VehiclePacketHandler.java`, `TugRoutePacketHandler.java`, `VehicleTrackerPacketHandler.java`) are **deleted in Phase 3**. If Phase 3 has already been completed, these files will not exist. If Phase 9B runs before Phase 3 (unlikely given phase ordering), they should still be updated.

---

## Execution Steps

### Step 1: Verify preconditions
```bash
./gradlew build   # Must pass (Phase 9A complete)
```

### Step 2: Apply two-arg replacement
```bash
# In IDE: Find and Replace with regex across src/main/java/
# Find:    new ResourceLocation\(([^,]+),\s*
# Replace: ResourceLocation.fromNamespaceAndPath($1,
```

### Step 3: Apply single-arg replacement
```bash
# Find:    new ResourceLocation\(([^,)]+)\)
# Replace: ResourceLocation.parse($1)
```

### Step 4: Verify no remaining usages
```bash
grep -rn 'new ResourceLocation(' src/main/java/ | grep -v '//'
# Expected output: empty (all active usages replaced)
```

### Step 5: Run data generators
```bash
./gradlew runData
```

### Step 6: Full build
```bash
./gradlew build
```

Both commands must succeed. If `runData` fails, check the data generation provider files (ModBlockStateProvider, ModItemModelProvider) for any ResourceLocation usages that were missed.

---

## Files Touched

All paths relative to `src/main/java/dev/murad/shipping/`:

```
ShippingMod.java
event/ForgeClientEventHandler.java
global/TrainChunkManagerManager.java
data/client/ModItemModelProvider.java
data/client/ModBlockStateProvider.java
item/container/TugRouteScreen.java
item/container/StringInputScreen.java
entity/container/AbstractHeadVehicleScreen.java
entity/container/EnergyHeadVehicleScreen.java
entity/container/SteamHeadVehicleScreen.java
entity/container/FishingBargeScreen.java
entity/render/barge/AbstractVesselRenderer.java
entity/render/train/TrainCarRenderer.java
entity/render/train/MultipartCarRenderer.java
entity/models/train/EnergyLocomotiveModel.java
entity/models/train/SteamLocomotiveModel.java
entity/models/train/BaseCarModel.java
entity/models/train/ChainModel.java
entity/models/train/ChainExtendedModel.java
entity/models/train/ChunkLoaderCarModel.java
entity/models/train/SeaterCarModel.java
entity/models/train/TrimCarModel.java
entity/models/insert/CubeInsertCarModel.java
entity/models/insert/CubeInsertBargeModel.java
entity/models/insert/SeaterInsertBargeModel.java
entity/models/insert/FishingInsertBargeModel.java
entity/models/insert/FluidTankInsertCarModel.java
entity/models/insert/FluidTankInsertBargeModel.java
entity/models/insert/RingsInsertBargeModel.java
entity/models/vessel/SteamTugModel.java
entity/models/vessel/EmptyModel.java
entity/models/vessel/EnergyTugModel.java
entity/models/vessel/base/BaseBargeModel.java
entity/models/vessel/base/TrimBargeModel.java
entity/custom/train/wagon/FluidTankCarEntity.java
entity/custom/vessel/barge/FluidTankBargeEntity.java
entity/custom/vessel/barge/FishingBargeEntity.java
setup/ModTags.java
setup/ModSounds.java
setup/ModItemModelProperties.java
setup/ModItems.java
setup/ModEntityTypes.java
network/TugRoutePacketHandler.java
network/client/VehicleTrackerPacketHandler.java
network/VehiclePacketHandler.java
```

**Total: 44 files** (30 unique files with two-arg, 5 with single-arg, some overlap; 3 network files may be deleted by Phase 3)

---

## Risk Assessment

- **Risk: LOW** — This is a purely mechanical replacement with no behavioral changes.
- **Edge case:** Multi-line `new ResourceLocation(` calls. Grep shows none in this codebase — all calls are single-line.
- **Edge case:** `ModEntityTypes.java` uses `new ResourceLocation(...).toString()` inside `.build()`. The regex handles this correctly since it replaces the constructor call regardless of what follows the closing parenthesis.
- **Edge case:** Commented-out code contains `new ResourceLocation(`. Leave as-is; these are dead code and will not cause compilation errors.
