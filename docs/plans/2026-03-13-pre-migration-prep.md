# Pre-Migration Prep for NeoForge 1.21.10

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix pre-existing bugs, eliminate custom capability usage, and add unit tests — all safe on Forge 1.20.1, all reducing NeoForge migration friction.

**Architecture:** Four independent changes: (1) fix wrong class in SynchedEntityData.defineId, (2) remove dead event bus registration, (3) replace StallingCapability Forge capability with direct instanceof pattern, (4) add JUnit 5 unit tests for pure logic.

**Tech Stack:** Java 17, Forge 1.20.1, JUnit 5, Gradle

---

### Task 1: Fix defineId Bug in FluidTankBargeEntity

**Files:**
- Modify: `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FluidTankBargeEntity.java:42-43`

**Step 1: Fix the wrong class references**

Change line 42-43 from:
```java
private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);
```

To:
```java
private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(FluidTankBargeEntity.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(FluidTankBargeEntity.class, EntityDataSerializers.STRING);
```

**Step 2: Remove the now-unused import**

Remove line 3:
```java
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
```

**Step 3: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FluidTankBargeEntity.java
git commit -m "fix: use correct class in FluidTankBargeEntity defineId

FluidTankBargeEntity was passing AbstractTugEntity.class to
SynchedEntityData.defineId() instead of FluidTankBargeEntity.class,
causing synced data ID collisions."
```

---

### Task 2: Fix defineId Bug in FluidTankCarEntity

**Files:**
- Modify: `src/main/java/dev/murad/shipping/entity/custom/train/wagon/FluidTankCarEntity.java:43-44`

**Step 1: Fix the wrong class references**

Change line 43-44 from:
```java
private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);
```

To:
```java
private static final EntityDataAccessor<Integer> VOLUME = SynchedEntityData.defineId(FluidTankCarEntity.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> FLUID_TYPE = SynchedEntityData.defineId(FluidTankCarEntity.class, EntityDataSerializers.STRING);
```

**Step 2: Remove the now-unused import**

Remove line 3:
```java
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
```

**Step 3: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/dev/murad/shipping/entity/custom/train/wagon/FluidTankCarEntity.java
git commit -m "fix: use correct class in FluidTankCarEntity defineId

FluidTankCarEntity was passing AbstractTugEntity.class to
SynchedEntityData.defineId() instead of FluidTankCarEntity.class,
causing synced data ID collisions."
```

---

### Task 3: Remove Dead Event Bus Registration

**Files:**
- Modify: `src/main/java/dev/murad/shipping/ShippingMod.java:42`

**Step 1: Remove the dead registration**

Remove line 42:
```java
MinecraftForge.EVENT_BUS.register(this);
```

Also remove the comment on line 41:
```java
// Register ourselves for server and other game events we are interested in
```

**Step 2: Remove the now-unused import**

Remove:
```java
import net.minecraftforge.common.MinecraftForge;
```

**Step 3: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/dev/murad/shipping/ShippingMod.java
git commit -m "cleanup: remove dead EVENT_BUS.register(this) in ShippingMod

ShippingMod has no @SubscribeEvent methods, so this registration
was doing nothing."
```

---

### Task 4: StallingCapability — Make Head Vehicles Implement Directly

This is the largest refactor. The goal is to make `AbstractTugEntity` and `AbstractLocomotiveEntity` directly implement `StallingCapability` instead of wrapping it in an anonymous inner class exposed via the Forge capability system. Barges and wagons delegate to the head vehicle via `instanceof` instead of `getCapability()`.

**Files:**
- Modify: `src/main/java/dev/murad/shipping/capability/StallingCapability.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/AbstractBargeEntity.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/train/wagon/AbstractWagonEntity.java`
- Modify: `src/main/java/dev/murad/shipping/util/LinkingHandler.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/train/AbstractTrainCarEntity.java`
- Modify: `src/main/java/dev/murad/shipping/event/ModEventHandler.java`
- Modify: `src/main/java/dev/murad/shipping/compatibility/create/CapabilityInjector.java`

#### Step 1: Strip Forge capability boilerplate from StallingCapability interface

Keep the interface methods but remove the `Capability` field and `register` method. The interface becomes a pure Java interface.

`StallingCapability.java` becomes:
```java
package dev.murad.shipping.capability;

public interface StallingCapability {
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

#### Step 2: Make AbstractTugEntity directly implement StallingCapability

In `AbstractTugEntity.java`:

1. Add `implements StallingCapability` to the class declaration.
2. Move the method bodies from the anonymous `stalling` inner class (lines 639-686) to be direct `@Override` methods on the class itself.
3. Remove the `stalling` field (line 639), the `stallingOpt` field (line 689), and the `getCapability` override for `STALLING_CAPABILITY` (lines 691-698).
4. Replace self-referencing `getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(cap -> { ... })` calls (lines 221, 403) with direct method calls on `this`:

Line 221 area — `tickCheckDock()`:
```java
// Before:
getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(cap -> {
    ...cap.isDocked()...
// After: just use this directly (this implements StallingCapability)
{
    int x = (int) Math.floor(this.getX());
    int y = (int) Math.floor(this.getY());
    int z = (int) Math.floor(this.getZ());
    boolean docked = this.isDocked();
    // ... rest of method uses this.dock(), this.undock(), etc.
```

Line 403 area — `followGuideRail()`:
```java
// Before:
var dockcap = getCapability(StallingCapability.STALLING_CAPABILITY);
if(dockcap.isPresent() && dockcap.resolve().isPresent()){
    var cap = dockcap.resolve().get();
    if(cap.isDocked() || cap.isFrozen() || cap.isStalled())
        return;
}
// After:
if (this.isDocked() || this.isFrozen() || this.isStalled()) {
    return;
}
```

5. Remove unused Forge capability imports (`LazyOptional`, `Capability`, etc.) if no other capabilities remain in the file.

#### Step 3: Make AbstractLocomotiveEntity directly implement StallingCapability

Same pattern as AbstractTugEntity:

1. Add `implements StallingCapability` to the class declaration.
2. Move method bodies from anonymous `stalling` inner class (lines 507-554) to direct `@Override` methods.
3. Remove `stalling` field (line 507), `stallingOpt` field (line 557), and `getCapability` override for STALLING_CAPABILITY (lines 559-566).
4. Replace `getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(cap -> { ... })` at line 375 (`tickDockCheck()`) with direct `this` calls.

#### Step 4: Refactor AbstractBargeEntity to delegate via instanceof

In `AbstractBargeEntity.java`:

1. Keep `implements StallingCapability` on the class (it already effectively does via the anonymous class).
2. Replace the anonymous inner class (lines 143-195) with direct method implementations that delegate via `instanceof`:

```java
// Replace the delegate() method and anonymous class with:
private Optional<StallingCapability> delegateStalling() {
    if (linkingHandler.train.getHead() instanceof StallingCapability s) {
        return Optional.of(s);
    }
    return Optional.empty();
}

@Override
public boolean isDocked() {
    return delegateStalling().map(StallingCapability::isDocked).orElse(false);
}

@Override
public void dock(double x, double y, double z) {
    delegateStalling().ifPresent(s -> s.dock(x, y, z));
}

@Override
public void undock() {
    delegateStalling().ifPresent(StallingCapability::undock);
}

@Override
public boolean isStalled() {
    return delegateStalling().map(StallingCapability::isStalled).orElse(false);
}

@Override
public void stall() {
    delegateStalling().ifPresent(StallingCapability::stall);
}

@Override
public void unstall() {
    delegateStalling().ifPresent(StallingCapability::unstall);
}

@Override
public boolean isFrozen() {
    return super.isFrozen();
}

@Override
public void freeze() {
    setFrozen(true);
}

@Override
public void unfreeze() {
    setFrozen(false);
}
```

3. Remove the `capability` field, `capabilityOpt` field, and `getCapability` override for STALLING_CAPABILITY.
4. Remove unused Forge capability imports.

#### Step 5: Refactor AbstractWagonEntity to delegate via instanceof

Same pattern as AbstractBargeEntity but delegates to `AbstractLocomotiveEntity`:

```java
private Optional<StallingCapability> delegateStalling() {
    if (linkingHandler.train.getHead() instanceof StallingCapability s) {
        return Optional.of(s);
    }
    return Optional.empty();
}
```

Replace anonymous inner class with direct `@Override` methods (same as Step 4 pattern). Remove `capability`, `capabilityOpt`, and `getCapability` override.

#### Step 6: Update LinkingHandler

In `LinkingHandler.java` line 55, replace:
```java
entity.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
```
With:
```java
if (entity instanceof StallingCapability s) {
    s.stall();
}
```

#### Step 7: Update AbstractTrainCarEntity

In `AbstractTrainCarEntity.java` line 248, replace:
```java
this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
```
With:
```java
if (this instanceof StallingCapability s) {
    s.stall();
}
```

#### Step 8: Update CapabilityInjector (Create compat)

In `CapabilityInjector.java`, replace the `LazyOptional<StallingCapability>` field with a direct reference:

```java
public static class TrainCarController extends MinecartController {
    public static TrainCarController EMPTY;
    private final StallingCapability stallingCapability;

    public TrainCarController(SeaterCarEntity entity) {
        super(entity);
        stallingCapability = entity instanceof StallingCapability s ? s : null;
    }

    public boolean isStalled() {
        return stallingCapability != null && stallingCapability.isFrozen();
    }

    public void setStalledExternally(boolean stall) {
        if (stallingCapability != null) {
            if (stall) {
                stallingCapability.freeze();
            } else {
                stallingCapability.unfreeze();
            }
        }
    }

    public static TrainCarController empty() {
        return EMPTY != null ? EMPTY : (EMPTY = new TrainCarController(null));
    }
}
```

Remove `LazyOptional` import from CapabilityInjector.

#### Step 9: Remove capability registration from ModEventHandler

In `ModEventHandler.java`, remove the `registerCapabilities` method entirely (lines 14-17) and the `StallingCapability` and `RegisterCapabilitiesEvent` imports. If this leaves the class empty (only the creative tab handler remains — check), keep it with only that handler.

**Note:** Check if `ModEventHandler` has other `@SubscribeEvent` methods besides `registerCapabilities`. If it has a `BuildCreativeModeTabContentsEvent` handler, keep the class. Only remove the `registerCapabilities` method and its imports.

#### Step 10: Verify it compiles

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

#### Step 11: Commit

```bash
git add -A
git commit -m "refactor: replace StallingCapability Forge capability with instanceof

Make AbstractTugEntity and AbstractLocomotiveEntity directly implement
the StallingCapability interface. Barges and wagons delegate to the
head vehicle via instanceof checks instead of getCapability().

This eliminates the custom Forge capability registration entirely,
preparing for NeoForge migration where CapabilityManager is removed."
```

---

### Task 5: Add JUnit 5 to Build Configuration

**Files:**
- Modify: `build.gradle`

**Step 1: Add JUnit 5 dependency and test task config**

Add after the `dependencies { ... }` block's closing brace, inside the existing `dependencies` block:

```groovy
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
```

Add after the `dependencies` block:

```groovy
test {
    useJUnitPlatform()
}
```

**Step 2: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add build.gradle
git commit -m "build: add JUnit 5 test dependency"
```

---

### Task 6: Unit Test SpringPhysicsUtil.computeTargetYaw

**Files:**
- Create: `src/test/java/dev/murad/shipping/util/SpringPhysicsUtilTest.java`

**Step 1: Write the tests**

```java
package dev.murad.shipping.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpringPhysicsUtilTest {

    private static final float EPSILON = 0.001f;

    @Test
    void computeTargetYaw_straightNorth() {
        // anchor at origin, other anchor to the north (negative Z)
        float yaw = SpringPhysicsUtil.computeTargetYaw(0f, new Vec3(0, 0, 0), new Vec3(0, 0, -1));
        // atan2(dx=0, -dz=1) = 0 degrees
        assertEquals(0f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightEast() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(90f, new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        // atan2(dx=1, -dz=0) = 90 degrees
        assertEquals(90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightSouth() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(180f, new Vec3(0, 0, 0), new Vec3(0, 0, 1));
        // atan2(dx=0, -dz=-1) = 180 degrees
        assertEquals(180f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightWest() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(-90f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0));
        // atan2(dx=-1, -dz=0) = -90 degrees
        assertEquals(-90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_choosesClosestWrap() {
        // Current yaw is 170, ideal is -170 (equivalent to 190).
        // Without wrapping, distance is 340. With +360 wrap (190), distance is 20.
        float yaw = SpringPhysicsUtil.computeTargetYaw(170f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0.176f));
        // Should pick the value closest to 170, which is the +360 variant
        assertTrue(Math.abs(yaw - 170f) < 180f, "Should choose wrap closest to current yaw 170, got " + yaw);
    }

    @Test
    void computeTargetYaw_negativeCurrentYaw() {
        // Current yaw is -170, ideal is 170.
        // Should wrap to -190 (170 - 360) which is 20 away from -170.
        float yaw = SpringPhysicsUtil.computeTargetYaw(-170f, new Vec3(0, 0, 0), new Vec3(1, 0, 0.176f));
        assertTrue(Math.abs(yaw - (-170f)) < 180f, "Should choose wrap closest to current yaw -170, got " + yaw);
    }
}
```

**Step 2: Run the tests**

Run: `./gradlew test`

**Important:** This may fail if `Vec3` requires Minecraft bootstrap. If it does, the test needs to be restructured to test the math directly without Vec3. In that case, extract the core computation into a testable static method that takes doubles instead of Vec3. However, `Vec3` is a simple record-like class that should work without bootstrap — try first.

Expected: All tests PASS. If Vec3 fails to load, see fallback in Step 3.

**Step 3 (fallback): If Vec3 requires MC bootstrap**

If tests fail with `NoClassDefFoundError` or bootstrap issues, restructure the test to call computeTargetYaw with manually constructed Vec3 replaced by extracting the math. However, `Vec3` only depends on `org.joml` which Forge includes — it should work. If not, skip this test file and note it for post-migration GameTest.

**Step 4: Commit**

```bash
git add src/test/java/dev/murad/shipping/util/SpringPhysicsUtilTest.java
git commit -m "test: add unit tests for SpringPhysicsUtil.computeTargetYaw"
```

---

### Task 7: Unit Test TugRoute NBT Round-Trip

**Files:**
- Create: `src/test/java/dev/murad/shipping/util/TugRouteTest.java`

**Step 1: Write the tests**

```java
package dev.murad.shipping.util;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TugRouteTest {

    @Test
    void roundTrip_emptyRoute() {
        TugRoute route = new TugRoute();
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(0, deserialized.size());
        assertFalse(deserialized.hasCustomName());
    }

    @Test
    void roundTrip_singleNode() {
        TugRoute route = new TugRoute(List.of(new TugRouteNode(10.5, 20.5)));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(1, deserialized.size());
        assertEquals(10.5, deserialized.get(0).getX());
        assertEquals(20.5, deserialized.get(0).getZ());
    }

    @Test
    void roundTrip_multipleNodes() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(1.0, 2.0),
                new TugRouteNode(3.0, 4.0),
                new TugRouteNode(5.0, 6.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(route, deserialized);
    }

    @Test
    void roundTrip_withCustomName() {
        TugRoute route = new TugRoute("My Route", List.of(
                new TugRouteNode("Dock A", 100.0, 200.0),
                new TugRouteNode(300.0, 400.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertTrue(deserialized.hasCustomName());
        assertEquals(route, deserialized);
        assertEquals("Dock A", deserialized.get(0).getName());
        assertNull(deserialized.get(1).getName());
    }

    @Test
    void roundTrip_preservesNodeOrder() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(10.0, 20.0),
                new TugRouteNode(30.0, 40.0),
                new TugRouteNode(50.0, 60.0)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        for (int i = 0; i < route.size(); i++) {
            assertEquals(route.get(i).getX(), deserialized.get(i).getX());
            assertEquals(route.get(i).getZ(), deserialized.get(i).getZ());
        }
    }

    @Test
    void roundTrip_negativeCoordinates() {
        TugRoute route = new TugRoute(List.of(
                new TugRouteNode(-500.5, -1000.25)
        ));
        CompoundTag nbt = route.toNBT();
        TugRoute deserialized = TugRoute.fromNBT(nbt);

        assertEquals(-500.5, deserialized.get(0).getX());
        assertEquals(-1000.25, deserialized.get(0).getZ());
    }
}
```

**Step 2: Run the tests**

Run: `./gradlew test`

**Note:** `CompoundTag` and `ListTag` are vanilla Minecraft classes. They may require MC bootstrap depending on how Forge sets up the test classpath. If they fail to load, this test must be deferred to post-migration GameTests.

Expected: All tests PASS (CompoundTag is a simple NBT class with no static initialization).

**Step 3: Commit**

```bash
git add src/test/java/dev/murad/shipping/util/TugRouteTest.java
git commit -m "test: add unit tests for TugRoute NBT round-trip serialization"
```

---

## Summary

| Task | Type | Files | Risk |
|------|------|-------|------|
| 1. Fix FluidTankBargeEntity defineId | Bug fix | 1 | Low |
| 2. Fix FluidTankCarEntity defineId | Bug fix | 1 | Low |
| 3. Remove dead EVENT_BUS registration | Cleanup | 1 | Low |
| 4. StallingCapability instanceof refactor | Refactor | 9 | Medium |
| 5. Add JUnit 5 to build | Build config | 1 | Low |
| 6. Test SpringPhysicsUtil | Tests | 1 | Low (may need MC bootstrap) |
| 7. Test TugRoute round-trip | Tests | 1 | Low (may need MC bootstrap) |

Total: 7 tasks, ~15 files touched. Tasks 1-3 are trivial. Task 4 is the main refactor. Tasks 5-7 are additive.
