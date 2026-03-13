# LittleLogistics NeoForge 1.21.1 Migration Evaluation

**Evaluator:** evaluator-migration
**Date:** 2026-03-13
**Explorers evaluated:** 12 parallel exploration branches
**Source:** Hive Mind discoveries from all explorer agents

---

## Executive Summary

After synthesizing discoveries from all 12 explorers, the migration from Forge 1.20.1 to NeoForge 1.21.1 is **technically demanding but tractable**. The mod has ~180 Java files, but only ~30 require non-trivial migration work. The adversarial explorer's core argument — that a focused big-bang port is faster than incremental abstraction — is correct and well-supported by the evidence.

The single most valuable pre-migration action is fixing a **pre-existing bug** found by the entity data explorer.

---

## A. What To Do NOW on Forge 1.20.1

These actions are high-value on the current codebase and reduce migration friction without being wasted effort.

### 1. Fix Pre-existing defineId Bug (HIGH PRIORITY)

`FluidTankBargeEntity.java:42-43` passes the wrong class to `SynchedEntityData.defineId()`:

```java
// BUG — should be FluidTankBargeEntity.class, not AbstractTugEntity.class
private static final EntityDataAccessor<Integer> VOLUME =
    SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.INT);
private static final EntityDataAccessor<String> FLUID_TYPE =
    SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.STRING);
```

This causes synced data ID collisions with AbstractTugEntity's own data. NeoForge 1.21.x will make this more visibly broken (explicit warnings/crashes). **Also verify `FluidTankCarEntity.java`** — the explorer suspects the same bug exists there using `AbstractLocomotiveEntity.class` instead of `FluidTankCarEntity.class`.

**Action:** Change `AbstractTugEntity.class` → `FluidTankBargeEntity.class` (and fix FluidTankCarEntity similarly). Safe to do in 1.20.1 and fixes a real bug.

### 2. Remove Dead Event Bus Registration (LOW EFFORT)

`ShippingMod.java:42` calls `MinecraftForge.EVENT_BUS.register(this)`, but `ShippingMod` has **zero `@SubscribeEvent` methods**. This is dead code.

**Action:** Remove `MinecraftForge.EVENT_BUS.register(this)` from `ShippingMod`. No behavior change.

### 3. Replace StallingCapability with instanceof (MEDIUM EFFORT, HIGH VALUE)

`StallingCapability` is an interface implemented by head vehicles and proxied through barges/wagons. The `instanceof` pattern works identically on both Forge 1.20.1 and NeoForge 1.21.x, and removes the dependency on `CapabilityManager.get(CapabilityToken)` — which is completely gone in NeoForge.

**Action:** Make `AbstractTugEntity` and `AbstractLocomotiveEntity` directly implement `StallingCapability`. Replace all `entity.getCapability(StallingCapability.STALLING_CAPABILITY).resolve()` call sites in `LinkingHandler.java` with:

```java
entity instanceof StallingCapability s ? Optional.of(s) : Optional.empty()
```

This works on Forge 1.20.1 today and eliminates the hardest custom-capability migration on NeoForge.

### 4. Write Unit Tests for Pure Logic (MEDIUM EFFORT, SURVIVES MIGRATION)

No tests currently exist (`src/test/` does not exist). Pure-logic code is stable across the migration:

| Target | Coverage value | Survives migration? |
|--------|---------------|---------------------|
| `SpringPhysicsUtil.computeTargetYaw()` | High — non-trivial angle-wrapping logic | Yes (pure math) |
| `TugRoute`/`LocoRoute` NBT round-trips | High — route serialization correctness | Yes (vanilla NBT stable) |
| `RailHelper` static utilities | Medium — complex but Level-coupled | Needs GameTest scaffolding |

**Action:** Add `src/test/java/` with JUnit 5 tests for `SpringPhysicsUtil.computeTargetYaw()` and route serialization round-trips.

### 5. Write a Migration Checklist Document

**Action:** Create `MIGRATION_GUIDE.md` in the repo root documenting each changed API and the files affected (see Section C for the ordered phase plan). Cost: 1-2 hours. Value: the primary pre-migration investment.

---

## B. What NOT To Do Until Migration Day

### Do NOT abstract the networking layer

Any wrapper around `NetworkRegistry.newSimpleChannel()` still depends on Forge-specific types. NeoForge's `CustomPacketPayload` / `RegisterPayloadHandlersEvent` system has **zero API overlap** with `SimpleChannel`. The migration converts all 3 handler classes in a single pass. Pre-emptive wrappers are dead code.

### Do NOT try to bridge LazyOptional

The adversarial explorer's "abstraction paradox" is correct: any `LazyOptional`-hiding wrapper still needs a `ForgeCapabilityImpl(LazyOptional<T>)` on the current version. You'd be writing both codepaths simultaneously — more work, not less. The only capability abstraction worth doing pre-migration is the `StallingCapability` instanceof refactor (Section A.3), because it removes the capability usage entirely.

### Do NOT update mods.toml / build.gradle to NeoForge format

These are Day 1 migration actions. Doing them now breaks the current build.

### Do NOT refactor ResourceLocation usages yet

80 usages of `new ResourceLocation(namespace, path)` across ~25 files. This is a perfect candidate for a single automated find-and-replace on migration day. Doing it piecemeal adds churn with no benefit.

### Do NOT refactor the event bus entrypoint in ShippingMod.java

`FMLJavaModLoadingContext.get().getModEventBus()` → constructor injection is a 10-line change at migration time. Pre-abstracting adds complexity without making Day 1 easier.

### Do NOT write Forge-dependent tests

Tests using `LazyOptional`, capability providers, or entity spawning require the Forge runtime and will need complete rewrites post-migration.

---

## C. Recommended Migration Strategy: Big Bang (with Ordered Phases)

**Verdict: Big Bang Port, Not Incremental.**

The adversarial explorer makes a compelling case confirmed by the other explorers:

1. **No API surface survives unchanged.** Every major subsystem (capabilities, networking, event bus, registries, data gen, GUI) has a complete API replacement with no backward compatibility.
2. **The affected surface is concentrated.** ~30 files drive the bulk of the migration. ~150 files use vanilla MC APIs largely stable across the 1.20→1.21 gap.
3. **"Incremental" requires two codepaths.** Making Forge 1.20.1 code NeoForge-compatible means maintaining both old and new implementations simultaneously — strictly more work.
4. **No test safety net exists.** Without Forge-runnable tests, the "incremental = safer" argument doesn't hold.

### Recommended Big-Bang Execution Order

```
Phase 1 — Build System
  build.gradle: ForgeGradle → NeoGradle 7.x
  settings.gradle: update maven repos
  gradle.properties: forge_version → neo_version, Java 17 → 21
  mods.toml → neoforge.mods.toml (rename + dependency format changes)
  Remove fg.deobf() wrappers, drop Create dependency temporarily

Phase 2 — Mod Entrypoint & Registration
  ShippingMod.java: FMLJavaModLoadingContext → IEventBus constructor injection
  Registration.java: ForgeRegistries → BuiltInRegistries, RegistryObject → DeferredHolder
  MinecraftForge.EVENT_BUS → NeoForge.EVENT_BUS
  Event handler classes: Bus.FORGE → Bus.GAME, update import paths

Phase 3 — Networking
  VehiclePacketHandler, TugRoutePacketHandler, VehicleTrackerPacketHandler
  → single RegisterPayloadHandlersEvent handler
  → 4 packet classes become CustomPacketPayload records with StreamCodec

Phase 4 — Capabilities
  RegisterCapabilitiesEvent: register all entity/BE capability providers centrally
  Remove all getCapability() overrides from entity/BE classes
  LazyOptional → nullable direct returns
  StallingCapability: already done if Section A.3 was completed pre-migration
  ForgeCapabilities → Capabilities (new NeoForge class)

Phase 5 — Entity Data & BlockEntity Serialization
  defineSynchedData(Builder builder) signature update (5+ entity classes)
  LinkingHandler.defineSynchedData() → accept Builder parameter
  BlockEntity load() → loadAdditional(tag, registries)
  BlockEntity saveAdditional(tag) → saveAdditional(tag, registries)

Phase 6 — Data Components (Item NBT)
  Design TugRoute.CODEC + TugRoute.STREAM_CODEC
  Design LocoRoute.CODEC + LocoRoute.STREAM_CODEC
  Create ModDataComponents DeferredRegister
  Replace stack.getTag()/setTag() in TugRouteItem, LocoRouteItem, SpringItem
  CreativeCapacitor: replace initCapabilities() with item capability registration

Phase 7 — Recipe & Data Generation
  RecipeProvider: add Runner inner class, update buildRecipes() signature
  AbstractRouteCopyRecipe: remove ResourceLocation from constructor
  LootTableProvider: add CompletableFuture<HolderLookup.Provider> parameter
  DataGenerators: split GatherDataEvent into client/server variants

Phase 8 — GUI / Screens
  IForgeMenuType → IMenuTypeExtension (import rename)
  MenuScreens.register() → RegisterMenuScreensEvent
  renderBackground(GuiGraphics) → renderBackground(GuiGraphics, mouseX, mouseY)

Phase 9 — Mechanical Cleanup
  ResourceLocation: 80x new RL() → fromNamespaceAndPath() / parse()
  AbstractTrainCarEntity: IForgeAbstractMinecart → NeoForge equivalent
  Run data generators, fix resource path regressions

Phase 10 — Create Compat (follow-up PR)
  Verify NeoForge Create 1.21.x API
  Rewrite CapabilityInjector using NeoForge EntityCapability
  Re-enable CREATE_COMPAT in build.gradle
```

---

## D. Migration Risk Ranking (Hardest First)

| Rank | Area | Risk | Files | Key Challenge |
|------|------|------|-------|---------------|
| 1 | **Capability System** | CRITICAL | 27 | Complete API rewrite; LazyOptional gone; 18+ entity overrides → centralized registration |
| 2 | **Train Car Minecart Inheritance** | HIGH | 3-5 | `IForgeAbstractMinecart` gone; AbstractMinecart behavior hooks redesigned in 1.21 |
| 3 | **Data Components (route items)** | HIGH | 6 | Must design TugRoute/LocoRoute Codecs; preserve world save compatibility |
| 4 | **Build System** | MEDIUM-HIGH | 4 | ForgeGradle → NeoGradle; Java 17 → 21; mods.toml rename; fg.deobf() removal |
| 5 | **Networking** | MEDIUM | 7 | 3 SimpleChannel instances → 1 RegisterPayloadHandlersEvent |
| 6 | **Registration & Event Bus** | MEDIUM | 8 | ForgeRegistries → BuiltInRegistries; RegistryObject → DeferredHolder |
| 7 | **Entity Data & BlockEntity Serialization** | MEDIUM | 12 | defineSynchedData Builder; HolderLookup.Provider on load/save; LinkingHandler refactor |
| 8 | **Recipe & Loot Tables** | LOW-MEDIUM | 5 | RecipeProvider Runner class; GatherDataEvent split |
| 9 | **GUI / Screens** | LOW-MEDIUM | 8 | RegisterMenuScreensEvent; renderBackground signature |
| 10 | **ResourceLocation (80 usages)** | LOW | 25 | Purely mechanical; search-and-replace |
| 11 | **Create Mod Compatibility** | DEFERRED | 2 | Drop during migration; re-enable in follow-up once Create NeoForge 1.21.x API confirmed |

---

## E. Pre-existing Bugs to Fix

### Bug 1: Wrong Class in SynchedEntityData.defineId — FluidTankBargeEntity (CONFIRMED)

**File:** `src/main/java/dev/murad/shipping/entity/custom/vessel/barge/FluidTankBargeEntity.java:42-43`
**Severity:** High — causes synced data ID collision with AbstractTugEntity

```java
// WRONG:
SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.INT);
// CORRECT:
SynchedEntityData.defineId(FluidTankBargeEntity.class, EntityDataSerializers.INT);
```

Fix this on Forge 1.20.1 now. NeoForge 1.21.x explicitly makes this a crash-level issue.

### Bug 2: Likely Same defineId Bug in FluidTankCarEntity (UNVERIFIED — verify and fix)

**File:** `src/main/java/dev/murad/shipping/entity/custom/train/wagon/FluidTankCarEntity.java`

The entity data explorer suspects VOLUME/FLUID_TYPE use `AbstractLocomotiveEntity.class` instead of `FluidTankCarEntity.class`. Verify and fix.

### Bug 3: Dead Event Bus Registration in ShippingMod (CONFIRMED)

**File:** `src/main/java/dev/murad/shipping/ShippingMod.java:42`

`MinecraftForge.EVENT_BUS.register(this)` is called but `ShippingMod` has no `@SubscribeEvent` methods. Remove this line.

---

## Explorer Approach Comparison Table

| Explorer | Primary Contribution | Pre-migration Value |
|----------|---------------------|---------------------|
| explorer-capabilities | Full capability usage inventory; StallingCapability→instanceof strategy | HIGH |
| explorer-networking | Old→new pattern documentation side-by-side | LOW pre-migration; HIGH as guide |
| explorer-registration | Registry mapping; dead bus registration finding | MEDIUM |
| explorer-resourcelocation | 80 usages across 25 files; confirmed mechanical change | LOW pre-migration |
| explorer-datacomponents | Item NBT audit; Codec design guidance | MEDIUM: codec design planning |
| explorer-entitydata | **Found defineId bug**; Builder pattern analysis | HIGH |
| explorer-recipes | Thorough recipe/loot API change documentation | LOW pre-migration; good guide |
| explorer-gui | Screen-by-screen analysis; renderBackground | LOW: mechanical, one-pass |
| explorer-buildsystem | Complete ForgeGradle→NeoGradle mapping | HIGH: Phase 1 reference |
| explorer-create | Scoped Create compat; drop+re-add strategy | HIGH: clear strategy |
| explorer-testing | Unit test ROI analysis; no tests exist | MEDIUM |
| explorer-adversarial | Big-bang case; abstraction paradox | HIGH: correctly scopes what NOT to do |

---

## Final Recommendation

**Big-bang port. ~30 files with breaking changes. 2-3 focused weeks.**

The adversarial explorer's core thesis is correct and the evidence from every other explorer confirms it: no API surface survives the Forge→NeoForge gap intact, and "incremental" means maintaining two codepaths simultaneously.

**Pre-migration work with real ROI (do now):**
1. Fix the `defineId` bug in `FluidTankBargeEntity` and verify `FluidTankCarEntity`
2. Replace `StallingCapability` with `instanceof` pattern
3. Write unit tests for `SpringPhysicsUtil.computeTargetYaw()` and route serialization
4. Remove dead `EVENT_BUS.register(this)` in `ShippingMod`
5. Write `MIGRATION_GUIDE.md` with the Phase 1-10 plan

**Do not pre-abstract networking, LazyOptional, registries, or ResourceLocation.** The abstraction paradox is real.

**Start migration with Phase 1 (build system).** Once the project compiles on NeoForge — even stub-style — iterate through Phases 2-10 in order, validating with `./gradlew build` after each phase.
