# Phase 1.5: Vanilla API Compatibility

**Purpose:** Fix all vanilla MC 1.20.1 -> 1.21.1 API breaks so the project compiles after Phase 1 (NeoGradle build system switch). Without this phase, `./gradlew build` cannot succeed and no subsequent phase can be validated.

**Prerequisites:** Phase 1 complete (NeoGradle building, Java 21, NeoForge dependency resolving).

**Effort:** HIGH -- this is the hardest phase. VesselEntity.travel() alone is a 147-line copy of LivingEntity/Boat physics code with 6+ broken method calls.

**Gaps addressed:** C-1, C-3, H-1, H-2, M-1, M-2 from the evaluation.

---

## Table of Contents

1. [C-1: Boat.Status Rework](#c-1-boatstatus-rework)
2. [H-2: ForgeMod Attribute Relocations](#h-2-forgemod-attribute-relocations)
3. [H-1: AttributeModifier Constructor Change](#h-1-attributemodifier-constructor-change)
4. [M-1: LivingEntity Method Signature Changes](#m-1-livingentity-method-signature-changes)
5. [C-3: AbstractMinecart.Type Removal](#c-3-abstractminecarttype-removal)
6. [M-2: getRailDirection() Parameter Changes](#m-2-getraildirection-parameter-changes)
7. [Files Touched (Complete List)](#files-touched)
8. [Verification Steps](#verification-steps)

---

## C-1: Boat.Status Rework

**What changed in MC 1.21:** The `Boat` class was significantly restructured. `Boat.Status` is an internal enum used by Boat's physics code. In 1.21, Boat physics were refactored -- the Status enum may be renamed, reorganized, or its members changed.

**Impact:** VesselEntity.java copies ~200 lines of Boat physics code. It uses `Boat.Status` as field types and references all 5 enum values throughout its float/status logic.

### Affected Code

**VesselEntity.java:88-89** -- Field declarations:
```java
// CURRENT (1.20.1)
private Boat.Status status;
private Boat.Status oldStatus;
```

**VesselEntity.java:298-336** -- `floatBoat()` method (39 lines):
```java
// Uses Boat.Status.IN_AIR, IN_WATER, UNDER_FLOWING_WATER, UNDER_WATER, ON_LAND
if (this.oldStatus == Boat.Status.IN_AIR && this.status != Boat.Status.IN_AIR && this.status != Boat.Status.ON_LAND) {
    // ... splash landing logic
    this.status = Boat.Status.IN_WATER;
} else {
    if (this.status == Boat.Status.IN_WATER) { /* buoyancy */ }
    else if (this.status == Boat.Status.UNDER_FLOWING_WATER) { /* sink slowly */ }
    else if (this.status == Boat.Status.UNDER_WATER) { /* float up */ }
    else if (this.status == Boat.Status.IN_AIR) { /* air friction */ }
    else if (this.status == Boat.Status.ON_LAND) { /* land friction */ }
}
```

**VesselEntity.java:339-355** -- `getStatus()` method:
```java
private Boat.Status getStatus() {
    Boat.Status Boat$status = this.isUnderwater();
    if (Boat$status != null) { return Boat$status; }
    else if (this.checkInWater()) { return Boat.Status.IN_WATER; }
    else {
        float f = this.getGroundFriction();
        if (f > 0.0F) { return Boat.Status.ON_LAND; }
        else { return Boat.Status.IN_AIR; }
    }
}
```

**VesselEntity.java:466-495** -- `isUnderwater()` method:
```java
// Returns Boat.Status.UNDER_FLOWING_WATER or Boat.Status.UNDER_WATER or null
```

### Target Approach

**Option A (Recommended): Define a local Status enum.**
Since VesselEntity is NOT a Boat subclass (it extends WaterAnimal), it doesn't need to use Boat.Status at all. The code merely borrowed Boat's physics pattern. Replace with a local enum:

```java
// NEW: Local enum in VesselEntity.java
private enum VesselStatus {
    IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR
}

private VesselStatus status;
private VesselStatus oldStatus;
```

Then replace all `Boat.Status.X` references with `VesselStatus.X` throughout `floatBoat()`, `getStatus()`, and `isUnderwater()`. This is a mechanical find-and-replace within VesselEntity.java and completely decouples from Boat API changes.

**Option B: Track 1.21 Boat.Status changes.** If Boat.Status still exists in 1.21 but with renamed members, update references. This is fragile -- Option A is safer and more maintainable.

### Steps
1. Add `private enum VesselStatus { IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR }` inside VesselEntity
2. Change field types at lines 88-89 from `Boat.Status` to `VesselStatus`
3. Find-replace all `Boat.Status.` with `VesselStatus.` in VesselEntity.java (approx 15 occurrences)
4. Remove `import net.minecraft.world.entity.vehicle.Boat;` (line 36) if no other Boat references remain

---

## H-2: ForgeMod Attribute Relocations

**What changed:** `ForgeMod` class renamed to `NeoForgeMod` in NeoForge. Additionally, `ENTITY_GRAVITY` was moved to vanilla `Attributes.GRAVITY` in MC 1.21.

### Affected Code -- VesselEntity.java

| Line | Current Code | Target Code |
|------|-------------|-------------|
| 52 | `import net.minecraftforge.common.ForgeMod;` | `import net.neoforged.neoforge.common.NeoForgeMod;` |
| 142 | `ForgeMod.NAMETAG_DISTANCE.get()` | `NeoForgeMod.NAMETAG_DISTANCE.get()` |
| 143 | `ForgeMod.SWIM_SPEED.get()` | `NeoForgeMod.SWIM_SPEED.get()` |
| 196 | `ForgeMod.SWIM_SPEED.get()` | `NeoForgeMod.SWIM_SPEED.get()` |
| 201 | `ForgeMod.SWIM_SPEED.get()` | `NeoForgeMod.SWIM_SPEED.get()` |
| 206 | `ForgeMod.NAMETAG_DISTANCE.get()` | `NeoForgeMod.NAMETAG_DISTANCE.get()` |
| 549 | `net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get()` | `Attributes.GRAVITY` (vanilla, already imported) |
| 696 | `ForgeMod.SWIM_SPEED.get()` | `NeoForgeMod.SWIM_SPEED.get()` |

### Steps
1. Replace import: `net.minecraftforge.common.ForgeMod` -> `net.neoforged.neoforge.common.NeoForgeMod`
2. Replace 5 occurrences of `ForgeMod.SWIM_SPEED` -> `NeoForgeMod.SWIM_SPEED` (lines 143, 196, 201, 696, and `swimSpeed()` at 696)
3. Replace 2 occurrences of `ForgeMod.NAMETAG_DISTANCE` -> `NeoForgeMod.NAMETAG_DISTANCE` (lines 142, 206)
4. Replace `ForgeMod.ENTITY_GRAVITY.get()` -> `Attributes.GRAVITY` at line 549 (note: in 1.21, gravity is a vanilla attribute, accessed as `Attributes.GRAVITY` not through a Holder, so also change `.getValue()` pattern if needed -- verify the Holder<Attribute> vs Attribute access pattern)

### Note on ENTITY_GRAVITY
In MC 1.21, gravity became a vanilla entity attribute (`Attributes.GRAVITY`). The current code at line 549:
```java
AttributeInstance gravity = this.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
```
Becomes:
```java
AttributeInstance gravity = this.getAttribute(Attributes.GRAVITY);
```
Since `Attributes` is already imported (line 33), this is straightforward. Verify that `Attributes.GRAVITY` exists in NeoForge 1.21.1 (it should -- it was promoted from ForgeMod to vanilla).

---

## H-1: AttributeModifier Constructor Change

**What changed in MC 1.21:** `AttributeModifier(String name, double amount, Operation op)` -> `AttributeModifier(ResourceLocation id, double amount, Operation op)`. The string identifier was replaced with a proper ResourceLocation.

### Affected Code -- VesselEntity.java:199-203

```java
// CURRENT (1.20.1)
this.getAttribute(Attributes.MOVEMENT_SPEED)
    .addTransientModifier(
        new AttributeModifier("movementspeed_mult", newSpeed, AttributeModifier.Operation.ADDITION));
this.getAttribute(ForgeMod.SWIM_SPEED.get())
    .addTransientModifier(
        new AttributeModifier("swimspeed_mult", newSpeed, AttributeModifier.Operation.ADDITION));
```

### Target Code
```java
// TARGET (1.21.1)
this.getAttribute(Attributes.MOVEMENT_SPEED)
    .addTransientModifier(
        new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath("littlelogistics", "movementspeed_mult"),
            newSpeed, AttributeModifier.Operation.ADD_VALUE));
this.getAttribute(NeoForgeMod.SWIM_SPEED.get())
    .addTransientModifier(
        new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath("littlelogistics", "swimspeed_mult"),
            newSpeed, AttributeModifier.Operation.ADD_VALUE));
```

### Notes
- `AttributeModifier.Operation.ADDITION` was renamed to `AttributeModifier.Operation.ADD_VALUE` in 1.21. Verify exact enum name.
- The ResourceLocation should use the mod's namespace (`littlelogistics`) to avoid conflicts.
- Ensure `ResourceLocation` import is present (may already be via other uses, or add `import net.minecraft.resources.ResourceLocation;`).

### Steps
1. Replace string `"movementspeed_mult"` with `ResourceLocation.fromNamespaceAndPath("littlelogistics", "movementspeed_mult")`
2. Replace string `"swimspeed_mult"` with `ResourceLocation.fromNamespaceAndPath("littlelogistics", "swimspeed_mult")`
3. Update `Operation.ADDITION` -> `Operation.ADD_VALUE` if renamed (verify against 1.21 API)
4. Add ResourceLocation import if not already present

---

## M-1: LivingEntity Method Signature Changes

**What changed:** Multiple vanilla method signatures were simplified or removed in MC 1.21.

### M-1a: getFriction() -- VesselEntity.java

**Lines 419, 671:** `BlockState.getFriction(Level, BlockPos, Entity)` simplified.

```java
// CURRENT (1.20.1) -- line 419
f += blockstate.getFriction(this.level(), blockpos$mutableblockpos, this);

// CURRENT (1.20.1) -- line 671
float f3 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement())
    .getFriction(level(), this.getBlockPosBelowThatAffectsMyMovement(), this);
```

**Target approach:** In MC 1.21, `BlockState.getFriction()` was simplified. Check exact new signature:
- If `BlockState.getFriction(Level, BlockPos)` (dropped Entity param): remove `this` argument
- If `BlockState.getFriction()` (no params): use that
- The `Block.getFriction()` base method may now be parameterless

```java
// TARGET (1.21.1) -- verify exact signature
f += blockstate.getFriction(this.level(), blockpos$mutableblockpos);
// or:
f += blockstate.getBlock().getFriction();
```

### M-1b: getBlockPosBelowThatAffectsMyMovement() -- VesselEntity.java

**Lines 670-671:** Method renamed in 1.21.

```java
// CURRENT (1.20.1)
BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
float f3 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFriction(...);
```

**Target:** Renamed to `getBlockPosBelowThatAffectsMyMovement()` -> `getBlockPosBelowThatAffectsMovement()` (verify exact name). The method still returns BlockPos.

### M-1c: collisionExtendsVertically() -- AbstractTrainCarEntity.java

**Line 323:**
```java
// CURRENT (1.20.1)
if (blockstate.collisionExtendsVertically(this.level(), blockpos1, this)) {
    return blockpos1;
}
```

**What changed:** `BlockState.collisionExtendsVertically(BlockGetter, BlockPos, Entity)` was removed in 1.21.

**Target approach:** This method was used in Entity.getOnPos() to check if collision shapes extend vertically (for fences, walls, etc). In 1.21, the Entity.getOnPos() logic was reworked. Since AbstractTrainCarEntity overrides getOnPos() to avoid inheriting mixins, the simplest fix is:

```java
// TARGET (1.21.1) -- use Block.collisionExtendsVertically or equivalent
// If method was removed entirely, replace with:
if (blockstate.is(BlockTags.FENCES) || blockstate.is(BlockTags.WALLS) || blockstate.getBlock() instanceof FenceGateBlock) {
    return blockpos1;
}
// OR: check if vanilla Entity.getOnPos() in 1.21 has a replacement pattern we should copy
```

**Important:** Verify the exact replacement in MC 1.21's Entity.getOnPos() source and mirror that logic.

### M-1d: canStandOnFluid() -- VesselEntity.java

**Lines 554, 607:**
```java
// CURRENT (1.20.1)
if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
// ...
} else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
```

**What changed:** `Entity.canStandOnFluid(FluidState)` signature changed in 1.21. In 1.21, the method either takes no args, takes a different type, or was replaced with a different check.

**Target approach:** Check 1.21 LivingEntity.travel() source for the equivalent check. Likely becomes:
```java
// If removed: replace with direct fluid check or remove condition
// If signature changed: update parameter type
!this.canStandOnFluid(fluidstate.getType())  // possible: takes Fluid instead of FluidState
```

### M-1e: calculateEntityAnimation() -- VesselEntity.java

**Line 692:**
```java
// CURRENT (1.20.1)
this.calculateEntityAnimation(false);
```

**What changed:** In MC 1.21, `LivingEntity.calculateEntityAnimation(boolean)` was changed -- likely the boolean parameter was removed.

**Target:**
```java
// TARGET (1.21.1) -- verify
this.calculateEntityAnimation(this, false);  // or just this.calculateEntityAnimation();
```

### Steps for all M-1 changes
1. Check MC 1.21.1 source (via NeoForge MDK or decompiled sources) for exact new signatures
2. Update `getFriction()` calls at lines 419 and 671
3. Rename `getBlockPosBelowThatAffectsMyMovement()` at lines 670-671
4. Replace `collisionExtendsVertically()` at AbstractTrainCarEntity.java:323
5. Update `canStandOnFluid()` at VesselEntity.java:554, 607
6. Update `calculateEntityAnimation()` at VesselEntity.java:692

---

## C-3: AbstractMinecart.Type Removal

**What changed in MC 1.21:** `AbstractMinecart.Type` enum was completely removed. `getMinecartType()` was removed. `isPoweredCart()` behavior/signature changed.

### Affected Code

**AbstractTrainCarEntity.java:558-562** -- `getMinecartType()`:
```java
// CURRENT (1.20.1)
@Override
public Type getMinecartType() {
    // Why does this even exist
    return Type.CHEST;
}
```

**SeaterCarEntity.java:120-122** -- `getMinecartType()`:
```java
// CURRENT (1.20.1)
public AbstractMinecart.Type getMinecartType() {
    return AbstractMinecart.Type.RIDEABLE;
}
```

**AbstractLocomotiveEntity.java:352-355** -- `isPoweredCart()`:
```java
// CURRENT (1.20.1)
@Override
public boolean isPoweredCart() {
    return true;
}
```

**AbstractTrainCarEntity.java:285, 289** -- `isPoweredCart()` usage in push():
```java
// CURRENT (1.20.1) -- push() physics
if (((AbstractMinecart)pEntity).isPoweredCart() && !this.isPoweredCart()) {
    // entity pushes this cart
} else if (!((AbstractMinecart)pEntity).isPoweredCart() && this.isPoweredCart()) {
    // this cart pushes entity
} else {
    // equal push
}
```

### Target Approach

**getMinecartType() removal:**
- Simply delete the `getMinecartType()` overrides in AbstractTrainCarEntity (line 558-562) and SeaterCarEntity (lines 120-122)
- The method no longer exists in the parent class, so the overrides will cause compilation errors if kept

**isPoweredCart() changes:**
In MC 1.21, `isPoweredCart()` was likely removed or changed since it was tied to `Type.FURNACE`. Check if:
- If `isPoweredCart()` was removed entirely: replace push physics with a custom check. AbstractLocomotiveEntity already overrides it to return `true`, and the default for AbstractMinecart returns `false`. Replace with a local method or instanceof check:

```java
// TARGET (1.21.1) -- in push() method
// Replace isPoweredCart() with instanceof check
if (pEntity instanceof AbstractLocomotiveEntity && !(this instanceof AbstractLocomotiveEntity)) {
    // powered pushes unpowered
} else if (!(pEntity instanceof AbstractLocomotiveEntity) && this instanceof AbstractLocomotiveEntity) {
    // this is powered, pushes unpowered
} else {
    // equal
}
```

Or define a local helper:
```java
private static boolean isPowered(Entity e) {
    return e instanceof AbstractLocomotiveEntity;
}
```

- If `isPoweredCart()` still exists but is not abstract: keep the override in AbstractLocomotiveEntity

### Steps
1. Delete `getMinecartType()` from AbstractTrainCarEntity.java (lines 558-562)
2. Delete `getMinecartType()` from SeaterCarEntity.java (lines 120-122)
3. Check if `isPoweredCart()` exists in 1.21 AbstractMinecart
4. If removed: replace lines 285, 289 in push() with instanceof-based check
5. If removed: delete isPoweredCart() override from AbstractLocomotiveEntity.java (lines 352-355) or convert to local method
6. Remove `import net.minecraftforge.common.extensions.IForgeAbstractMinecart;` from AbstractTrainCarEntity.java (line 48) -- this moves to NeoForge equivalent or is removed

---

## M-2: getRailDirection() Parameter Changes

**What changed:** In NeoForge 1.21, `BaseRailBlock.getRailDirection(BlockState, BlockGetter, BlockPos, @Nullable AbstractMinecart)` parameter types changed. The `BlockGetter` parameter may become `Level`, or the method signature may change to accept a `@Nullable Entity` instead of `@Nullable AbstractMinecart`.

### Affected Files and Lines

| File | Line | Current Call |
|------|------|-------------|
| JunctionRail.java | 69 | `getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart)` (override) |
| SwitchRail.java | 124 | `getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart)` (override) |
| TeeJunctionRail.java | 104 | `getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart)` (override) |
| RailHelper.java | 87 | `((BaseRailBlock) state.getBlock()).getRailDirection(state, minecart.level(), pos, minecart)` |
| RailHelper.java | 93 | `((BaseRailBlock) state.getBlock()).getRailDirection(state, level, pos, null)` |
| RailHelper.java | 102 | `((BaseRailBlock) state.getBlock()).getRailDirection(state, minecart.level(), pos, minecart)` |
| AbstractTrainCarEntity.java | 111 | `(railBlock).getRailDirection(state, this.level(), pos, this)` |
| AbstractTrainCarEntity.java | 412 | `((BaseRailBlock) blockstate.getBlock()).getRailDirection(blockstate, this.level(), new BlockPos(i, j, k), this)` |
| TrainCarItem.java | 40 | `((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, level, blockpos, null)` |
| TrainCarItem.java | 99 | `((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, level, blockpos, null)` |

### Target Approach

Check the exact NeoForge 1.21.1 signature. Likely changes:
1. **Parameter type change:** `@Nullable AbstractMinecart` -> `@Nullable AbstractMinecart` (may stay same) or `@Nullable Entity`
2. **BlockGetter -> Level:** The `BlockGetter` parameter may become `Level` in overrides

For each file:
- **Override declarations** (JunctionRail, SwitchRail, TeeJunctionRail): Update method signature to match parent
- **Call sites** (RailHelper, AbstractTrainCarEntity, TrainCarItem): Update argument types if needed

### Steps
1. Check NeoForge 1.21.1 `BaseRailBlock.getRailDirection()` exact signature
2. Update 3 override declarations (JunctionRail:69, SwitchRail:124, TeeJunctionRail:104)
3. Update 6 call sites (RailHelper:87,93,102; AbstractTrainCarEntity:111,412; TrainCarItem:40,99)
4. If parameter changes from `AbstractMinecart` to `Entity`, cast where needed

---

## Files Touched

Complete list of files modified in this phase, with line numbers of changes:

### VesselEntity.java (~30 changes -- MEGA FILE)
- Lines 36: Remove Boat import (if using local enum)
- Lines 52: `ForgeMod` -> `NeoForgeMod` import
- Lines 88-89: `Boat.Status` -> `VesselStatus` fields
- Lines 123-124: `Boat.Status` -> `VesselStatus` usage in tick()
- Lines 142-143: `ForgeMod.NAMETAG_DISTANCE/SWIM_SPEED` -> `NeoForgeMod.*`
- Lines 196, 201, 206: `ForgeMod.*` -> `NeoForgeMod.*` / `Attributes.GRAVITY`
- Lines 199-203: `AttributeModifier` string -> ResourceLocation constructor
- Lines 298-336: All `Boat.Status.*` refs in floatBoat() -> `VesselStatus.*`
- Lines 339-355: All `Boat.Status.*` refs in getStatus() -> `VesselStatus.*`
- Lines 419: `getFriction()` 3-arg -> new signature
- Lines 466-495: All `Boat.Status.*` refs in isUnderwater() -> `VesselStatus.*`
- Lines 549: `ForgeMod.ENTITY_GRAVITY.get()` -> `Attributes.GRAVITY`
- Lines 554, 607: `canStandOnFluid()` signature update
- Lines 670-671: `getBlockPosBelowThatAffectsMyMovement()` rename
- Lines 671: `getFriction()` 3-arg -> new signature
- Lines 692: `calculateEntityAnimation(false)` -> new signature
- Lines 696: `ForgeMod.SWIM_SPEED` -> `NeoForgeMod.SWIM_SPEED`

### AbstractTrainCarEntity.java (~6 changes)
- Line 48: Remove `IForgeAbstractMinecart` import
- Line 59: Remove `implements IForgeAbstractMinecart` (or update to NeoForge equivalent)
- Line 111: `getRailDirection()` parameter update
- Lines 285, 289: `isPoweredCart()` -> instanceof-based check
- Line 323: `collisionExtendsVertically()` removal
- Line 412: `getRailDirection()` parameter update
- Lines 558-562: Delete `getMinecartType()` override

### AbstractLocomotiveEntity.java (~2 changes)
- Lines 352-355: Delete or update `isPoweredCart()` override
- (Line 136 NetworkHooks is Phase 8, not this phase)

### SeaterCarEntity.java (~1 change)
- Lines 120-122: Delete `getMinecartType()` override

### JunctionRail.java (~1 change)
- Line 69: `getRailDirection()` signature update

### SwitchRail.java (~1 change)
- Line 124: `getRailDirection()` signature update

### TeeJunctionRail.java (~1 change)
- Line 104: `getRailDirection()` signature update

### RailHelper.java (~3 changes)
- Line 87: `getRailDirection()` call update
- Line 93: `getRailDirection()` call update
- Line 102: `getRailDirection()` call update

### TrainCarItem.java (~2 changes)
- Line 40: `getRailDirection()` call update
- Line 99: `getRailDirection()` call update

**Total: 9 files, ~47 individual changes**

---

## Verification Steps

### Step 1: Compile Check
```bash
./gradlew build
```
Must pass. This is the gate for all subsequent phases.

### Step 2: Verify No Remaining Boat.Status References
```bash
grep -rn "Boat.Status" src/main/java/
# Should return 0 results
```

### Step 3: Verify No Remaining ForgeMod References
```bash
grep -rn "ForgeMod\." src/main/java/
# Should return 0 results (all should be NeoForgeMod or removed)
```

### Step 4: Verify No Remaining getMinecartType
```bash
grep -rn "getMinecartType" src/main/java/
# Should return 0 results
```

### Step 5: Verify No Remaining String AttributeModifier Constructors
```bash
grep -rn 'new AttributeModifier("' src/main/java/
# Should return 0 results
```

### Step 6: Functional Smoke Test
```bash
./gradlew runClient
```
- Place a tug on water -- verify it floats correctly
- Place a locomotive on rails -- verify it moves
- Link barges/wagons -- verify spring physics
- Place switch/junction rails -- verify trains traverse them

---

## Implementation Order

Recommended order within this phase (minimizes conflicts, allows incremental testing):

1. **VesselEntity local enum** (C-1) -- biggest single change, fully self-contained
2. **ForgeMod attribute relocations** (H-2) -- prerequisite for H-1
3. **AttributeModifier constructors** (H-1) -- depends on H-2 being done
4. **LivingEntity method signatures** (M-1) -- remaining VesselEntity changes
5. **AbstractMinecart.Type removal** (C-3) -- train entities
6. **getRailDirection() updates** (M-2) -- rail system, 7 files

Steps 1-4 can all be done in VesselEntity.java atomically. Steps 5-6 are independent train/rail changes.

---

## Key Risk: VesselEntity.travel()

The `travel()` method (VesselEntity.java:546-693) is a **147-line copy** of `LivingEntity.travel()` with custom water physics. It contains:
- `ForgeMod.ENTITY_GRAVITY` reference (line 549)
- `canStandOnFluid()` calls (lines 554, 607)
- `getBlockPosBelowThatAffectsMyMovement()` (lines 670-671)
- `getFriction()` 3-arg calls (line 671)
- `calculateEntityAnimation()` (line 692)
- `MobEffects.DOLPHINS_GRACE` reference (line 572) -- verify still exists in 1.21
- `MobEffects.LEVITATION` reference (line 675) -- verify still exists in 1.21

This method will need careful line-by-line comparison with MC 1.21's `LivingEntity.travel()` to ensure the physics are still correct after the API changes. Consider copying the fresh 1.21 `LivingEntity.travel()` and re-applying the mod's customizations rather than patching the old copy.

**Recommendation:** Before modifying travel(), diff the 1.20.1 and 1.21.1 versions of `LivingEntity.travel()` to identify all changes, then apply them to VesselEntity's copy.
