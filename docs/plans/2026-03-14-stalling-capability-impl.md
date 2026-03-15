# StallingCapability NeoForge EntityCapability Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Expose `StallingCapability` as a NeoForge `EntityCapability` so companion mods can query and control stalling/docking/freezing on all vehicles.

**Architecture:** Add an `EntityCapability<StallingCapability, Void>` constant to the existing interface. Register all 16 entity types in `CapabilityRegistration`. Fix the gutted `stallNonTicking()` method. Internal code keeps `instanceof` checks unchanged.

**Tech Stack:** NeoForge 1.21.1 EntityCapability API, Java 21

**Design doc:** `docs/plans/2026-03-14-stalling-capability-design.md`

---

### Task 1: Add EntityCapability constant to StallingCapability interface

**Files:**
- Modify: `src/main/java/dev/murad/shipping/capability/StallingCapability.java`

**Step 1: Add the capability constant**

Replace the entire file with:

```java
package dev.murad.shipping.capability;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;

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

**Step 2: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (no other files reference `ENTITY_CAPABILITY` yet, so this is purely additive)

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/capability/StallingCapability.java
git commit -m "feat: add EntityCapability constant to StallingCapability interface"
```

---

### Task 2: Register all entity types for the StallingCapability

**Files:**
- Modify: `src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java:23-128`

**Step 1: Add stalling capability registrations**

Add the following block inside `onRegisterCapabilities()`, after the existing item capabilities section (after line 127, before the closing brace):

```java
        // === Entity capabilities: STALLING ===

        // Head vehicles — tugs
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_TUG.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.ENERGY_TUG.get(), (entity, ctx) -> entity);

        // Head vehicles — locomotives (typed as EntityType<AbstractLocomotiveEntity>)
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_LOCOMOTIVE.get(), (entity, ctx) -> (StallingCapability) entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.ENERGY_LOCOMOTIVE.get(), (entity, ctx) -> (StallingCapability) entity);

        // Tail vehicles — barges
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHEST_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.BARREL_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHUNK_LOADER_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FISHING_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FLUID_TANK_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.SEATER_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.VACUUM_BARGE.get(), (entity, ctx) -> entity);

        // Tail vehicles — wagons
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHEST_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.BARREL_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.SEATER_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FLUID_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHUNK_LOADER_CAR.get(), (entity, ctx) -> entity);
```

Add necessary imports at the top of the file:

```java
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.vessel.barge.*;
import dev.murad.shipping.entity.custom.train.wagon.*;
```

Note: Some barge/wagon imports may already be covered by existing imports. Only add what's missing. The existing `FluidTankBargeEntity`, `ChestCarEntity`, `FluidTankCarEntity` imports are already present. The wildcard imports above cover the rest.

**Step 2: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java
git commit -m "feat: register StallingCapability for all 16 entity types"
```

---

### Task 3: Fix stallNonTicking() in LinkingHandler

**Files:**
- Modify: `src/main/java/dev/murad/shipping/util/LinkingHandler.java:65-78`

**Step 1: Re-implement stallNonTicking()**

Replace the `stallNonTicking()` method (lines 65-78) with:

```java
    private void stallNonTicking() {
        if (follower.isEmpty()) return;

        boolean skip = entity.getTrain()
                .getTug()
                .filter(tug -> tug instanceof HeadVehicle)
                .map(tug -> ((HeadVehicle) tug).hasOwner())
                .orElse(true);

        if (!skip && !((ServerLevel) entity.level()).isPositionEntityTicking(follower.get().blockPosition())) {
            if (entity instanceof StallingCapability s) {
                s.stall();
            }
        }
    }
```

Add the necessary imports at the top of the file:

```java
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.server.level.ServerLevel;
```

**Step 2: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/util/LinkingHandler.java
git commit -m "fix: re-implement stallNonTicking() for non-ticking follower entities"
```

---

### Task 4: Run data generators and full build to verify

**Step 1: Run data generators**

Run: `./gradlew runData`
Expected: Completes without errors (no datagen changes expected, but verifies nothing broke)

**Step 2: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL with no warnings related to stalling/capability code
