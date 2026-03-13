# Phase 10: Create Mod Compatibility

**Status:** Deferred follow-up PR (after main migration)
**Priority:** Low — non-blocking for core mod functionality
**Estimated effort:** 1-2 days (once Create for NeoForge 1.21.x is available)

---

## 1. Overview

Create mod compatibility is a **deferred phase**, intentionally excluded from the main Forge 1.20.1 → NeoForge 1.21.1 migration. The integration is small (3 files, ~95 lines of mod code) but depends on Create publishing a stable NeoForge 1.21.x release with its minecart controller API. This phase re-enables the integration once that dependency is available.

During the main migration (Phases 1-9B), the Create dependency is **removed from build.gradle** and the compatibility code is left in place but inert (the `CreateCompatibility.enabled()` gate returns `false` when Create is not loaded).

---

## 2. Prerequisites

- **Phases 1-9B complete:** The mod compiles and runs on NeoForge 1.21.1 without Create.
- **Create mod available for NeoForge 1.21.x:** A stable or beta release of Create targeting NeoForge 1.21.1+ must be published to Maven/CurseForge.
- **StallingCapability refactor complete (Phase 4 / pre-migration A.3):** The `StallingCapability` interface is retained but no longer uses Forge's `CapabilityManager`/`CapabilityToken`. Head vehicles directly implement it, and delegation uses `instanceof`.

---

## 3. Current Create Integration Analysis

### 3.1 Files Involved

| File | Lines | Purpose |
|------|-------|---------|
| `compatibility/create/CreateCompatibility.java` | 13 | Gate: checks config + `ModList.isLoaded("create")` |
| `compatibility/create/CapabilityInjector.java` | 48 | Bridges Create's `MinecartController` with LittleLogistics' `StallingCapability` |
| `entity/custom/train/wagon/SeaterCarEntity.java` | 135 | Only entity with Create integration; holds the capability |

### 3.2 What CreateCompatibility Does

Simple boolean gate class:
```java
public static boolean enabled() {
    return ShippingConfig.Common.CREATE_COMPAT.get()
        && ModList.get() != null
        && ModList.get().isLoaded("create");
}
```

- Config key: `ShippingConfig.Common.CREATE_COMPAT` (defaults `true`, under `[compat]` section)
- Uses `net.minecraftforge.fml.ModList` → must become `net.neoforged.fml.ModList`

### 3.3 What CapabilityInjector Does

Contains `TrainCarController extends MinecartController` — a bridge between Create's minecart contraption system and LittleLogistics' stalling system:

- **`isStalled()`** → delegates to `StallingCapability.isFrozen()`
- **`setStalledExternally(stall)`** → calls `freeze()` or `unfreeze()` on the entity
- **`constructMinecartControllerCapability(SeaterCarEntity)`** → returns `LazyOptional<TrainCarController>`
- **`isMinecartControllerCapability(cap)`** → checks `cap == CapabilityMinecartController.MINECART_CONTROLLER_CAPABILITY`

### 3.4 Create APIs Used

| Create Class | Package (Forge 1.20.1) | Usage |
|-------------|----------------------|-------|
| `MinecartController` | `com.simibubi.create.content.contraptions.minecart.capability` | Extended by `TrainCarController` |
| `CapabilityMinecartController` | Same package | Static field `MINECART_CONTROLLER_CAPABILITY` compared in `isMinecartControllerCapability()` |

### 3.5 SeaterCarEntity Integration Points

1. **Field:** `LazyOptional<?> createCompatMinecartControllerCapability` — nullable, initialized in constructor
2. **`initCompat()`** — called from both constructors; creates the capability if Create is loaded
3. **`getCapability(Capability<T> cap)`** — overrides Forge's capability query; returns the Create capability when the cap matches
4. **`remove(RemovalReason)`** — invalidates the `LazyOptional` on entity removal

### 3.6 Build Dependency

In `build.gradle`:
```groovy
// create
runtimeOnly fg.deobf("curse.maven:create-328085:${rootProject.create_version}")
compileOnly fg.deobf("curse.maven:create-328085:${rootProject.create_version}")
```

This was **removed in Phase 1** of the main migration.

---

## 4. Strategy for Re-enabling

### 4.1 Target Create Version

- **Create for NeoForge 1.21.x** — as of the main migration, Create's NeoForge 1.21.x port status should be checked at:
  - https://github.com/Creators-of-Create/Create (release tags)
  - CurseForge / Modrinth project pages
- Target whatever stable release is available. Create's NeoForge port may use a different Maven coordinate than the CurseForge slug.

### 4.2 Expected API Changes in Create for NeoForge

Create's minecart controller system will undergo the same NeoForge capability migration as every other mod:

| Forge 1.20.1 (current) | NeoForge 1.21.x (expected) |
|------------------------|---------------------------|
| `MinecartController` class | Likely preserved or renamed |
| `CapabilityMinecartController.MINECART_CONTROLLER_CAPABILITY` (Forge Capability) | Replaced with NeoForge `EntityCapability<MinecartController, Void>` or equivalent |
| `LazyOptional<MinecartController>` return type | Direct return or `@Nullable MinecartController` |
| Capability registered via `RegisterCapabilitiesEvent` (Forge) | Registered via `RegisterCapabilitiesEvent` (NeoForge) — same name, different package |

**Key risk:** Create may restructure its minecart controller package entirely. The package path `com.simibubi.create.content.contraptions.minecart.capability` may change. This must be verified against the actual Create NeoForge release.

### 4.3 Config System Update

`ShippingConfig.Common` uses `ForgeConfigSpec` on Forge 1.20.1. After the main migration:
- If NeoForge retains `ForgeConfigSpec` (it does, under `net.neoforged.neoforge.common.ModConfigSpec` or similar), the `CREATE_COMPAT` config value survives as-is.
- The config key and default value (`true`) do not change.

---

## 5. Step-by-Step Migration Plan

### Step 1: Verify Create NeoForge Availability

- [ ] Check that Create has a stable NeoForge 1.21.x release
- [ ] Identify the correct Maven coordinates / CurseForge file ID
- [ ] Verify the Create API JAR is accessible at compile time

### Step 2: Re-add Create Dependency to build.gradle

```groovy
// NeoGradle does not use fg.deobf() — direct dependency
compileOnly "curse.maven:create-328085:${rootProject.create_version}"
runtimeOnly "curse.maven:create-328085:${rootProject.create_version}"
```

Update `gradle.properties` with the new `create_version` file ID.

**Note:** NeoGradle resolves deobfuscation automatically; `fg.deobf()` wrappers are gone.

### Step 3: Update CreateCompatibility.java

```java
// BEFORE (Forge):
import net.minecraftforge.fml.ModList;

// AFTER (NeoForge):
import net.neoforged.fml.ModList;
```

The `ModList.get().isLoaded(MOD_ID)` API is identical in NeoForge — only the import changes.

### Step 4: Rewrite CapabilityInjector.java

This is the core migration work. The Forge capability system is completely replaced.

**4a. Determine Create's new capability registration pattern:**
- Look for Create's `MinecartController` class and how it registers its entity capability.
- Expected: Create will register an `EntityCapability<MinecartController, Void>` key.

**4b. Rewrite `TrainCarController`:**
- The `extends MinecartController` relationship should survive if Create retains the class.
- `isStalled()` and `setStalledExternally()` delegate to `StallingCapability` — these are internal and unchanged.
- Remove `LazyOptional` wrapping — return the controller directly.

**4c. Replace capability check method:**

```java
// BEFORE:
public static <T> boolean isMinecartControllerCapability(@NotNull Capability<T> cap) {
    return cap == CapabilityMinecartController.MINECART_CONTROLLER_CAPABILITY;
}

// AFTER (pattern depends on Create's API):
// This method may be removed entirely if capability registration is centralized
```

**4d. Replace capability construction:**

```java
// BEFORE:
public static LazyOptional<?> constructMinecartControllerCapability(SeaterCarEntity entity) {
    return LazyOptional.of(() -> new TrainCarController(entity));
}

// AFTER:
public static TrainCarController constructMinecartController(SeaterCarEntity entity) {
    return new TrainCarController(entity);
}
```

### Step 5: Update SeaterCarEntity.java

**5a. Replace the capability field:**

```java
// BEFORE:
@Nullable
private LazyOptional<?> createCompatMinecartControllerCapability = null;

// AFTER:
@Nullable
private CapabilityInjector.TrainCarController createCompatMinecartController = null;
```

**5b. Update initCompat():**

```java
private void initCompat() {
    if (CreateCompatibility.enabled()) {
        createCompatMinecartController = CapabilityInjector.constructMinecartController(this);
    }
}
```

**5c. Register capability via NeoForge's RegisterCapabilitiesEvent:**

Instead of overriding `getCapability()` on the entity, register a capability provider centrally. Add to the mod's event handler (or a dedicated Create compat event handler):

```java
@SubscribeEvent
public static void registerCapabilities(RegisterCapabilitiesEvent event) {
    if (CreateCompatibility.enabled()) {
        // Register using Create's EntityCapability key
        // event.registerEntity(CreateCapabilityKey.MINECART_CONTROLLER,
        //     ModEntityTypes.SEATER_CAR.get(),
        //     (entity, context) -> entity.getCreateCompatController());
    }
}
```

**The exact API depends on how Create exposes its capability key on NeoForge.** This is the primary unknown.

**5d. Remove getCapability() override:**

The `getCapability(Capability<T> cap)` override in `SeaterCarEntity` is deleted entirely — NeoForge does not use entity-level `getCapability()`.

**5e. Update remove():**

```java
// BEFORE:
if (createCompatMinecartControllerCapability != null && CreateCompatibility.enabled()) {
    createCompatMinecartControllerCapability.invalidate();
}

// AFTER:
// LazyOptional.invalidate() no longer exists.
// If cleanup is needed, null out the reference:
createCompatMinecartController = null;
```

### Step 6: Verify No Other Entities Need Create Compat

- Grep for `CreateCompatibility` and `CapabilityInjector` references across the codebase.
- Currently only `SeaterCarEntity` uses the integration. If other wagon/barge entities need Create contraption support, the same pattern applies.

### Step 7: Test

- [ ] `./gradlew build` succeeds with Create on the compile classpath
- [ ] Launch client with Create installed: `./gradlew runClient`
- [ ] Place a SeaterCar on rails near a Create contraption
- [ ] Verify Create can pick up the SeaterCar as a minecart controller (contraption assembly)
- [ ] Verify stalling works: Create contraption stalls → SeaterCar freezes; contraption releases → SeaterCar unfreezes
- [ ] Verify without Create installed: mod loads normally, `CreateCompatibility.enabled()` returns `false`, no classloading errors
- [ ] Verify config toggle: set `create=false` in `littlelogistics-common.toml` → Create integration disabled even with Create present

---

## 6. Files Touched

| File | Action |
|------|--------|
| `build.gradle` | Re-add Create dependency (NeoGradle format) |
| `gradle.properties` | Update `create_version` to NeoForge-compatible file ID |
| `compatibility/create/CreateCompatibility.java` | Update `ModList` import |
| `compatibility/create/CapabilityInjector.java` | Rewrite: remove `LazyOptional`, update Create API imports, adapt to NeoForge capability pattern |
| `entity/custom/train/wagon/SeaterCarEntity.java` | Remove `getCapability()` override, replace `LazyOptional<?>` field with direct reference, remove `invalidate()` call |
| Event handler (new or existing) | Add `RegisterCapabilitiesEvent` handler for Create's minecart controller capability |

---

## 7. Risks and Unknowns

| Risk | Severity | Mitigation |
|------|----------|------------|
| Create NeoForge 1.21.x not yet released | Blocking | This phase is deferred precisely for this reason. Monitor Create's release schedule. |
| Create restructures minecart controller API | Medium | The integration surface is tiny (1 class extended, 1 capability checked). Even a full rewrite is < 50 lines. |
| Create drops minecart controller capability entirely | Medium | If Create removes this feature, delete the compatibility package entirely. The mod functions without it. |
| `StallingCapability` interface changes during main migration | Low | Phase 4 / pre-migration A.3 stabilizes the interface. `isFrozen()`/`freeze()`/`unfreeze()` are the only methods used by Create compat. |
| Classloading errors when Create is absent | Low | The existing `CreateCompatibility.enabled()` gate prevents classloading of Create types at runtime. This pattern works on both Forge and NeoForge. Verify with testing. |

---

## 8. Decision: Keep or Drop Create Compat?

If Create's NeoForge 1.21.x release is significantly delayed (6+ months after the main migration), consider:

1. **Keep the code but leave it dormant** — minimal maintenance cost, ready when Create ships
2. **Remove the compatibility package entirely** — cleaner codebase, re-add from scratch when Create is ready

The integration is small enough that either approach is viable. The `CreateCompatibility.enabled()` gate means dormant code has zero runtime cost.
