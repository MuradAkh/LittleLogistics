# Comprehensive Gap Analysis: NeoForge 1.21.1 Migration Plan

**Evaluator:** evaluator-gaps
**Date:** 2026-03-13
**Source plan:** `docs/plans/2026-03-13-migration.md` (Section C — 10-phase migration plan)
**Explorers synthesized:** explorer-plan-adapt, explorer-build-reg, explorer-net-cap, explorer-entity-data, explorer-recipe-gui, explorer-adversarial, explorer-cleanup (hive mind)

---

## Executive Summary

The existing 10-phase migration plan covers the Forge→NeoForge API surface well but has **significant blind spots in vanilla MC 1.20→1.21 API changes**. The adversarial explorer's core finding is correct and confirmed: approximately 50% of the total migration work comes from vanilla MC changes not mentioned in the plan. The adversarial estimate of ~45 affected files (vs. the plan's ~30) is more accurate.

Two additional findings change the calculus:

1. **The pre-migration prep work is already done.** The defineId bugs, StallingCapability instanceof refactor, and unit tests were completed in run 561c8bb0. Section A actions can be skipped.

2. **Phase 7 contains two items that do not apply to NeoForge 1.21.1.** The Runner inner class and GatherDataEvent split belong to MC 1.21.2+. These must be removed.

---

## Part 1: Conflict Resolution

### Conflict: Runner Inner Class (Phase 7)

| Explorer | Claim |
|----------|-------|
| explorer-recipe-gui | "Must also add a Runner inner class pattern" |
| explorer-plan-adapt | "Runner inner class pattern does NOT exist in NeoForge 1.21.1" |

**Verdict: explorer-plan-adapt is correct.**

Explorer-plan-adapt specifically verified NeoForge 1.21.1 documentation and confirmed: the Runner inner class was introduced in MC 1.21.2+. The target is NeoForge 1.21.1 (21.1.x). Remove this item from Phase 7.

### Conflict: GatherDataEvent Split (Phase 7)

| Explorer | Claim |
|----------|-------|
| explorer-recipe-gui | "possible client/server split" |
| explorer-plan-adapt | "GatherDataEvent is NOT split in 1.21.1" |

**Verdict: explorer-plan-adapt is correct.** The split happened in MC 1.21.2+. Remove from Phase 7.

### Conflict: Pre-existing defineId Bug Status

| Explorer | Claim |
|----------|-------|
| Prior evaluator | Bug exists in FluidTankBargeEntity and FluidTankCarEntity |
| explorer-entity-data | "The prior defineId class mismatch bug has already been fixed in the current codebase" |

**Verdict: explorer-entity-data is correct.** Git log confirms: run 561c8bb0 fixed both defineId bugs.

---

## Part 2: All Gaps Ranked by Severity

### CRITICAL Gaps

#### C-1: Boat.Status Rework (Vanilla MC 1.21)
Not mentioned anywhere in the original plan.
- VesselEntity.java copies ~200 lines of Boat physics code (lines 298-495)
- Uses Boat.Status.IN_AIR, IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND
- MC 1.21 restructured Boat significantly; Status enum members and physics methods changed
- Impact: VesselEntity.travel() (~150 lines) will not compile; water vessels are completely blocked
- Belongs in: New Phase 1.5

#### C-2: NetworkHooks.openScreen() Removal
Not mentioned in any phase of the original plan.
- NetworkHooks.openScreen(ServerPlayer, MenuProvider, Consumer<FriendlyByteBuf>) is removed in NeoForge
- Replacement: player.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>) (note: RegistryFriendlyByteBuf)
- Impact: 4 files fail to compile
- Files: AbstractTugEntity.java:310, AbstractLocomotiveEntity.java:136, FishingBargeEntity.java, TugRouteItem.java:66
- Belongs in: Phase 8 as an explicit line item

#### C-3: AbstractMinecart.Type Removal (Vanilla MC 1.21)
Only partially mentioned ("IForgeAbstractMinecart → NeoForge equivalent" in Phase 9 — insufficient).
- AbstractMinecart.Type enum completely removed in 1.21
- getMinecartType() removed; isPoweredCart() signature/behavior changed
- Impact: 3 files fail to compile; push physics need rewriting
- Files: AbstractTrainCarEntity.java:559, SeaterCarEntity.java:120, AbstractLocomotiveEntity.java:353, AbstractTrainCarEntity.java:285,289
- Belongs in: New Phase 1.5

#### C-4: VesselEntity is a Compilation Blocker
Not mentioned in the original plan.
- VesselEntity.java is touched by Phase 4, Phase 5, Phase 9, PLUS 5 unlisted vanilla changes
- Until ALL vanilla method signature changes in VesselEntity are fixed, the project does not compile
- The plan's "validate with ./gradlew build after each phase" fails if VesselEntity is split across phases
- Recommendation: Address all VesselEntity changes atomically in Phase 1.5

#### C-5: Phase 7 Contains Wrong Items (1.21.2+ content)
- "RecipeProvider: add Runner inner class" — does NOT exist in 1.21.1
- "DataGenerators: split GatherDataEvent into client/server variants" — does NOT exist in 1.21.1
- If executed as written, these would introduce incorrect patterns

---

### HIGH Gaps

#### H-1: AttributeModifier Constructor Change (Vanilla MC 1.21)
Not mentioned in any phase.
- AttributeModifier(String name, double amount, Operation op) → AttributeModifier(ResourceLocation id, double amount, Operation op)
- Files: VesselEntity.java:200-203 — uses string constructors "movementspeed_mult" and "swimspeed_mult"
- Belongs in: New Phase 1.5

#### H-2: ForgeMod Attribute Relocations
Not mentioned in any phase.
- ForgeMod.SWIM_SPEED → NeoForgeMod.SWIM_SPEED
- ForgeMod.NAMETAG_DISTANCE → NeoForgeMod.NAMETAG_DISTANCE
- ForgeMod.ENTITY_GRAVITY → Attributes.GRAVITY (moved to vanilla in 1.21)
- Files: VesselEntity.java — 7 references at lines 142-143, 196, 201, 206, 549, 696
- Belongs in: New Phase 1.5

#### H-3: Version Target Incorrect in Plan Title
- Plan says "NeoForge 1.21.10" — correct target is NeoForge 1.21.1 (version 21.1.x, e.g. 21.1.77)
- Propagates to gradle.properties specifications

---

### MEDIUM Gaps

#### M-1: LivingEntity Method Signature Changes (Vanilla MC 1.21)
Not mentioned in any phase.

| Method | Change | Files |
|--------|--------|-------|
| getFriction(Level, BlockPos, Entity) | simplified to getFriction() | VesselEntity.java:419, 671 |
| collisionExtendsVertically(Level, BlockPos, Entity) | Removed | AbstractTrainCarEntity.java:323 |
| getBlockPosBelowThatAffectsMyMovement() | Renamed | VesselEntity.java:670-671 |
| canStandOnFluid(FluidState) | Signature changed | VesselEntity.java:554, 607 |
| calculateEntityAnimation(boolean) | Changed | VesselEntity.java:692 |

Belongs in: New Phase 1.5

#### M-2: getRailDirection() API Changes (Vanilla/NeoForge)
Not mentioned in any phase.
- BaseRailBlock.getRailDirection() parameter types changed in NeoForge 1.21
- 12+ call sites across 7 files: JunctionRail, SwitchRail, TeeJunctionRail, RailHelper, TrainCarItem, AbstractTrainCarEntity
- Belongs in: New Phase 1.5

#### M-3: PartEntity Multipart System Relocation
Not mentioned in any phase.
- PartEntity moved: net.minecraftforge.entity → net.neoforged.neoforge.entity
- recreateFromPacket(ClientboundAddEntityPacket) changed
- Files: VehicleFrontPart.java, AbstractTugEntity.java (getParts, recreateFromPacket), AbstractLocomotiveEntity.java

#### M-4: ModTags "forge" Namespace → "c"
Missing from Phase 2.
- NeoForge 1.21.x: conventional tags use "c" namespace instead of "forge"
- TagKey.create(Registries.ITEM, new ResourceLocation("forge", path)) → ResourceLocation.fromNamespaceAndPath("c", path)
- Files: ModTags.java

#### M-5: IVesselLoader Generic Capability<T> Redesign
Missing from Phase 4.
- IVesselLoader currently takes Capability<T> as a generic parameter
- NeoForge uses separate EntityCapability<T, C> / BlockCapability<T, C> instead of generic Capability<T>
- Files: IVesselLoader.java and all callers

#### M-6: BlockEntity getUpdateTag/handleUpdateTag Signature Change
Missing from Phase 5.
- getUpdateTag() → getUpdateTag(HolderLookup.Provider registries)
- handleUpdateTag() gains HolderLookup.Provider parameter
- Files: FluidHopperTileEntity.java:84,91, VesselChargerTileEntity.java

#### M-7: AbstractRouteCopyRecipe assemble() Signature Change
Missing from Phase 7.
- assemble(CraftingContainer, RegistryAccess) → assemble(CraftingContainer, HolderLookup.Provider)
- Files: AbstractRouteCopyRecipe.java

#### M-8: Phase 9 Must Be Split
Phase 9 hides significant complexity behind "Mechanical Cleanup":
- ResourceLocation: 80 usages (truly mechanical — fine)
- IForgeAbstractMinecart/AbstractMinecart.Type: major API rework requiring push() physics rewrite — NOT mechanical
- getRailDirection: 12 call sites across 7 files — medium complexity
- ForgeMod attributes: scattered entity code
- Recommendation: Split into Phase 9A (vanilla/NeoForge API reworks) and Phase 9B (mechanical renames)

---

### LOW Gaps

#### L-1: ItemStackHandler Import Relocation (14 files)
- net.minecraftforge.items.ItemStackHandler → net.neoforged.neoforge.items.ItemStackHandler
- Mechanical import change; affects 14 files
- Belongs in: Phase 9B

#### L-2: BuildCreativeModeTabContentsEvent Import
- net.minecraftforge.event.BuildCreativeModeTabContentsEvent → net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
- Files: ModItems.java, ModBlocks.java

#### L-3: EntityAttributeCreationEvent Import
- net.minecraftforge.event.entity.EntityAttributeCreationEvent → net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
- Files: ModEventBusEvents.java

#### L-4: Data Gen Provider Import Updates
- ModBlockTagsProvider: net.minecraftforge.common.data.BlockTagsProvider → NeoForge equivalent
- ModItemTagsProvider: ExistingFileHelper import change
- ModBlockStateProvider / ModItemModelProvider: net.minecraftforge.client.model.generators.* → NeoForge equivalents

#### L-5: StringInputScreen renderBackground Override
- StringInputScreen extends Screen and OVERRIDES renderBackground()
- Override must update to 4-arg signature: renderBackground(GuiGraphics, int mouseX, int mouseY, float partialTick)

#### L-6: AbstractHeadVehicleScreen.renderBg() Verification
- AbstractHeadVehicleScreen overrides renderBg() — verify if this signature also changes in 1.21.1

#### L-7: Lombok Version Update
- Current: io.freefair.lombok:6.4.1
- Required for Java 21: 8.11
- Without this, annotation processing breaks on Java 21

#### L-8: FishingBargeContainer Dead Code
- Line 23 calls getCapability(ITEM_HANDLER).ifPresent(...) that never executes because VesselEntity blocks ITEM_HANDLER
- Clean up during capability migration

#### L-9: SpringItem DataComponent Decision
- SpringItem stores entity ID ("linked" key) as transient runtime-only data
- Entity IDs don't survive world reload — a transient instance field may be more appropriate than a DataComponent

#### L-10: Patchouli Availability Verification
- patchouli_version=1.20.1-81-FORGE needs NeoForge 1.21.1 equivalent verified
- If unavailable, drop temporarily like Create

---

## Part 3: Corrected and Complete Migration Phase Plan

### Pre-Migration Status (Already Completed in run 561c8bb0)

- [x] Fix defineId bug in FluidTankBargeEntity and FluidTankCarEntity
- [x] Remove dead MinecraftForge.EVENT_BUS.register(this) in ShippingMod
- [x] Replace StallingCapability Forge capability with instanceof pattern
- [x] Add JUnit 5 unit tests for SpringPhysicsUtil.computeTargetYaw() and TugRoute NBT round-trip

---

### Phase 1 — Build System

Files: build.gradle, settings.gradle, gradle.properties, mods.toml

```
build.gradle:
  - net.minecraftforge.gradle → net.neoforged.gradle.userdev
  - io.freefair.lombok 6.4.1 → 8.11  [L-7]
  - REMOVE org.spongepowered.mixin plugin
  - minecraft {} block → subsystems {} + runs {} DSL
  - java.toolchain.languageVersion 17 → 21
  - "net.minecraftforge:forge:..." → "net.neoforged:neoforge:${neo_version}"
  - REMOVE ALL fg.deobf() wrappers
  - REMOVE Create dependency (deferred to Phase 10)
  - Verify Patchouli for NeoForge 1.21.1; drop if unavailable  [L-10]
  - REMOVE jar.finalizedBy('reobfJar')
  - processResources: 'META-INF/mods.toml' → 'META-INF/neoforge.mods.toml'
  - test { useJUnitPlatform() }

settings.gradle:
  - Forge maven → https://maven.neoforged.net/releases
  - REMOVE Sponge maven repo

gradle.properties:
  - mc_version: 1.20.1 → 1.21.1  [H-3]
  - mc_version_range: [1.20,1.21) → [1.21,1.22)
  - REMOVE forge_version, forge_version_range, forge_gradle_version
  - ADD neo_version=21.1.77 (or latest 21.1.x)
  - ADD neo_version_range=[21.1,)
  - mappings_version: update to 1.21.1 Parchment version
  - REMOVE create_version

mods.toml → neoforge.mods.toml (rename + content):
  - loaderVersion → neo_version_range
  - modId="forge" → modId="neoforge"
  - mandatory=true → type="required"
  - mandatory=false → type="optional"
  - All version ranges updated for 1.21.1
```

---

### Phase 1.5 — Vanilla API Compatibility (NEW PHASE)

Purpose: Get the project compiling after the build switch. Fix all vanilla 1.20→1.21 API changes.
Without this phase, ./gradlew build cannot validate any subsequent phase. [C-1, C-3, H-1, H-2, M-1, M-2]

Files: VesselEntity.java, AbstractTrainCarEntity.java, AbstractLocomotiveEntity.java, SeaterCarEntity.java,
       JunctionRail.java, SwitchRail.java, TeeJunctionRail.java, RailHelper.java, TrainCarItem.java (~12 files)

```
VesselEntity.java (all changes atomic — C-4):
  - Boat.Status rework: lines 298-495 (~200 lines of boat physics)  [C-1]
  - AttributeModifier: "movementspeed_mult"/"swimspeed_mult" → ResourceLocation IDs  [H-1]
  - ForgeMod.SWIM_SPEED → NeoForgeMod.SWIM_SPEED (lines 549, 696)  [H-2]
  - ForgeMod.NAMETAG_DISTANCE → NeoForgeMod.NAMETAG_DISTANCE (lines 142-143)  [H-2]
  - ForgeMod.ENTITY_GRAVITY → Attributes.GRAVITY (lines 196, 201, 206)  [H-2]
  - getFriction(Level, BlockPos, Entity) → getFriction() at lines 419, 671  [M-1]
  - getBlockPosBelowThatAffectsMyMovement() rename at lines 670-671  [M-1]
  - canStandOnFluid(FluidState) signature at lines 554, 607  [M-1]
  - calculateEntityAnimation(boolean) change at line 692  [M-1]

AbstractTrainCarEntity.java:
  - getMinecartType() / AbstractMinecart.Type removal  [C-3]
  - isPoweredCart() usage at lines 285, 289  [C-3]
  - collisionExtendsVertically() removal at line 323  [M-1]

AbstractLocomotiveEntity.java:
  - isPoweredCart() override at line 353  [C-3]

SeaterCarEntity.java:
  - AbstractMinecart.Type return at line 120  [C-3]

Rail files (JunctionRail, SwitchRail, TeeJunctionRail, RailHelper, TrainCarItem, AbstractTrainCarEntity):
  - getRailDirection() parameter type updates  [M-2]
```

Gate: ./gradlew build must pass before Phase 2.

---

### Phase 2 — Mod Entrypoint & Registration

Files: ShippingMod.java, Registration.java, ModEntityTypes.java, ModItems.java, ModBlocks.java, ModMenuTypes.java,
       ModTileEntitiesTypes.java, ModSounds.java, ModRecipeSerializers.java, ModEventBusEvents.java, ModTags.java

```
ShippingMod.java:
  - FMLJavaModLoadingContext → IEventBus + ModContainer constructor injection
  - ModLoadingContext → modContainer.registerConfig()

Registration.java:
  - ForgeRegistries.X → Registries.X
  - DeferredRegister.create(ForgeRegistries.X) → DeferredRegister.create(Registries.X, MOD_ID)
  - register() → register(IEventBus eventBus)
  - Remove networking registration calls (Phase 3)

7x setup/*.java (Entities/Items/Blocks/Menus/TileEntities/Sounds/RecipeSerializers):
  - RegistryObject<T> → DeferredHolder<BaseType, T>

ModEventBusEvents.java:
  - @Mod.EventBusSubscriber → @EventBusSubscriber
  - EntityAttributeCreationEvent import update  [L-3]

ModItems.java / ModBlocks.java:
  - BuildCreativeModeTabContentsEvent import update  [L-2]

ModTags.java:
  - "forge" namespace → "c" for conventional tags  [M-4]
```

---

### Phase 3 — Networking

Files: DELETE VehiclePacketHandler.java, TugRoutePacketHandler.java, VehicleTrackerPacketHandler.java;
       REWRITE 4 packet classes; CREATE NetworkHandler.java

```
DELETE: All 3 SimpleChannel handler classes

Rewrite as CustomPacketPayload records (4 files):
  - Add TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(...))
  - Add STREAM_CODEC = StreamCodec.composite(...)
  - Rewrite as record with type() method

NetworkHandler.java (CREATE):
  - @SubscribeEvent onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
  - playToServer() for SetEngine, EnrollVehicle, SetRouteTag
  - playToClient() for VehicleTracker
  - Handler methods: (Payload, IPayloadContext) — no Supplier, no setPacketHandled
  - context.player() instead of ctx.get().getSender()

Send sites:
  - INSTANCE.sendToServer() → PacketDistributor.sendToServer(payload)
  - INSTANCE.send() → PacketDistributor.sendToPlayer(player, payload)
```

---

### Phase 4 — Capabilities

Files: 11 entity files, 2 block entity files, IVesselLoader.java, InventoryUtils.java, CreativeCapacitor.java,
       FishingBargeContainer.java, ModEventHandler.java, CREATE CapabilityRegistration.java

```
CapabilityRegistration.java (CREATE):
  - @SubscribeEvent onRegisterCapabilities(RegisterCapabilitiesEvent event)
  - event.registerEntity() for all 11 entity types
  - event.registerBlockEntity() for VesselCharger and FluidHopper
  - event.registerItem() for CreativeCapacitor

Entity files (11):
  - DELETE all LazyOptional<T> fields (15 total)
  - DELETE all getCapability() overrides (14 total)

IVesselLoader.java:  [M-5]
  - Capability<T> generic → specific EntityCapability types
  - getEntityCapability() → Optional.ofNullable(entity.getCapability(cap, null))

Consumer sites (9+ files):
  - entity.getCapability(ForgeCapabilities.X).map(...) → Optional.ofNullable(entity.getCapability(Capabilities.X.ENTITY, null)).map(...)
  - stack.getCapability(ForgeCapabilities.ENERGY).isPresent() → stack.getCapability(Capabilities.EnergyStorage.ITEM) != null

FishingBargeContainer.java: Remove dead getCapability(ITEM_HANDLER).ifPresent() at line 23  [L-8]
SeaterCarEntity.java (Create compat): DEFER createCompatMinecartControllerCapability to Phase 10
```

---

### Phase 5 — Entity Data & BlockEntity Serialization

Files: VesselEntity.java, AbstractTrainCarEntity.java, AbstractTugEntity.java, AbstractLocomotiveEntity.java,
       FluidTankBargeEntity.java, FluidTankCarEntity.java, VehicleFrontPart.java, LinkingHandler.java,
       FluidHopperTileEntity.java, VesselChargerTileEntity.java

```
defineSynchedData(SynchedEntityData.Builder builder) — 7 entity files + LinkingHandler:
  - All 7 classes update method signature
  - LinkingHandler.defineSynchedData(Entity, ...) → defineSynchedData(Builder, ...)
  - builder.define() instead of entity.getEntityData().define()

BlockEntity serialization (2 files):
  - FluidHopperTileEntity: loadAdditional/saveAdditional/getUpdateTag/handleUpdateTag gain HolderLookup.Provider  [M-6]
  - VesselChargerTileEntity: same

Note: Entity readAdditionalSaveData/addAdditionalSaveData signatures do NOT change in NeoForge 1.21.x.
```

---

### Phase 6 — Data Components (Item NBT)

Files: ModDataComponents.java (CREATE), TugRoute.java, TugRouteNode.java, LocoRoute.java, LocoRouteNode.java,
       TugRouteItem.java, LocoRouteItem.java, SpringItem.java

```
TugRoute.java + TugRouteNode.java: Add CODEC + STREAM_CODEC
LocoRoute.java + LocoRouteNode.java: Add CODEC + STREAM_CODEC

ModDataComponents.java (CREATE):
  - DeferredRegister<DataComponentType<?>>
  - TUG_ROUTE, LOCO_ROUTE DataComponentType registrations
  - SPRING_LINKED: evaluate transient field vs DataComponent  [L-9]

TugRouteItem.java:
  - stack.getTag() → stack.get(ModDataComponents.TUG_ROUTE.get())
  - stack.setTag() → stack.set(...)
  - DROP verifyTagAfterLoad() legacy migration

LocoRouteItem.java:
  - stack.getTag() → stack.get(ModDataComponents.LOCO_ROUTE.get())
  - stack.setTag(null) → stack.remove(...)

SpringItem.java: Update per L-9 decision
```

---

### Phase 7 — Recipe & Data Generation

Files: ModRecipeProvider.java, AbstractRouteCopyRecipe.java, ModRecipeSerializers.java, ModLootTableProvider.java,
       DataGenerators.java, ModBlockTagsProvider.java, ModItemTagsProvider.java, ModBlockStateProvider.java,
       ModItemModelProvider.java

```
ModRecipeProvider.java:
  - Add CompletableFuture<HolderLookup.Provider> constructor parameter
  - buildRecipes(Consumer<FinishedRecipe>) → buildRecipes(RecipeOutput)
  - NO Runner inner class — does not exist in 1.21.1  [C-5 RESOLVED]
  - .save(consumer) → .save(output)

AbstractRouteCopyRecipe.java:
  - Remove ResourceLocation from constructor
  - assemble(CraftingContainer, RegistryAccess) → assemble(CraftingContainer, HolderLookup.Provider)  [M-7]

ModRecipeSerializers.java:
  - Serializer lambda (loc, cat) → (cat)

ModLootTableProvider.java:
  - Add CompletableFuture<HolderLookup.Provider> constructor parameter

DataGenerators.java:
  - GatherDataEvent stays UNIFIED — NO split  [C-5 RESOLVED]
  - Pass lookupProvider to constructors

ModBlockTagsProvider.java / ModItemTagsProvider.java / ModBlockStateProvider.java / ModItemModelProvider.java:
  - Import updates  [L-4]
```

---

### Phase 8 — GUI / Screens

Files: ShippingMod.java, ModMenuTypes.java, AbstractTugEntity.java, AbstractLocomotiveEntity.java,
       FishingBargeEntity.java, TugRouteItem.java, AbstractVehicleScreen.java, FishingBargeScreen.java,
       TugRouteScreen.java, StringInputScreen.java, AbstractHeadVehicleScreen.java

```
ModMenuTypes.java:
  - IForgeMenuType.create() → IMenuTypeExtension.create()

ShippingMod.java — RegisterMenuScreensEvent:
  - MenuScreens.register() → event.register() in @SubscribeEvent handler

NetworkHooks.openScreen() → player.openMenu():  [C-2]
  - AbstractTugEntity.java:310
  - AbstractLocomotiveEntity.java:136
  - FishingBargeEntity.java
  - TugRouteItem.java:66
  - Consumer<FriendlyByteBuf> → Consumer<RegistryFriendlyByteBuf>

renderBackground() (4 files, 5 sites):
  - All call sites: add mouseX, mouseY, partialTick args
  - StringInputScreen: update OVERRIDE signature to 4 params  [L-5]

AbstractHeadVehicleScreen: verify renderBg() signature  [L-6]
```

---

### Phase 9A — Vanilla/NeoForge API Reworks (Split from Phase 9)

Files: AbstractTrainCarEntity.java, AbstractLocomotiveEntity.java, SeaterCarEntity.java,
       VehicleFrontPart.java, AbstractTugEntity.java, ~14x ItemStackHandler files

```
AbstractMinecart.Type physics rewrite:  [C-3]
  - AbstractTrainCarEntity: remove getMinecartType() Type.CHEST return
  - SeaterCarEntity: remove Type return
  - AbstractLocomotiveEntity: refactor isPoweredCart() override
  - AbstractTrainCarEntity: update isPoweredCart() push physics

PartEntity multipart system:  [M-3]
  - VehicleFrontPart: import update
  - AbstractTugEntity / AbstractLocomotiveEntity: getParts(), recreateFromPacket()

ItemStackHandler import relocation (14 files):  [L-1]
  - net.minecraftforge.items → net.neoforged.neoforge.items
```

---

### Phase 9B — Mechanical Cleanup (Renamed from Phase 9)

Files: ~25 files with ResourceLocation usages

```
ResourceLocation (80 usages, ~25 files):
  - new ResourceLocation(namespace, path) → ResourceLocation.fromNamespaceAndPath(namespace, path)
  - new ResourceLocation(string) → ResourceLocation.parse(string)
  - Automated find-and-replace; verify afterward

Run data generators: ./gradlew runData
Final build: ./gradlew build — must succeed
```

---

### Phase 10 — Create Mod Compatibility (Follow-Up PR, DEFERRED)

Files: CapabilityInjector.java, CreateCompatibility.java, build.gradle

```
  - Verify NeoForge Create 1.21.x API
  - Rewrite CapabilityInjector using NeoForge EntityCapability
  - SeaterCarEntity: resolve deferred createCompatMinecartControllerCapability
  - Re-enable CREATE_COMPAT in build.gradle
```

---

## Part 4: Complete File Inventory

| File | Phase(s) | Key Changes |
|------|---------|-------------|
| build.gradle | 1 | Plugin swap, Java 21, deps, runs DSL, Lombok 8.11 |
| settings.gradle | 1 | Maven URLs |
| gradle.properties | 1 | mc_version, neo_version, remove forge props |
| META-INF/mods.toml → neoforge.mods.toml | 1 | Rename + dependency format |
| VesselEntity.java | 1.5, 4, 5, 9B | MEGA-FILE: Boat.Status, AttributeModifier, ForgeMod attrs, LivingEntity methods, getCapability, defineSynchedData |
| AbstractTrainCarEntity.java | 1.5, 5, 9A, 9B | AbstractMinecart.Type, collisionExtendsVertically, defineSynchedData |
| AbstractLocomotiveEntity.java | 1.5, 4, 5, 8, 9A, 9B | isPoweredCart, getCapability, defineSynchedData, NetworkHooks, PartEntity |
| SeaterCarEntity.java | 1.5, 9A | AbstractMinecart.Type, Create compat defer |
| JunctionRail.java | 1.5 | getRailDirection |
| SwitchRail.java | 1.5 | getRailDirection |
| TeeJunctionRail.java | 1.5 | getRailDirection |
| RailHelper.java | 1.5 | getRailDirection |
| TrainCarItem.java | 1.5 | getRailDirection |
| ShippingMod.java | 2, 8 | Constructor injection, RegisterMenuScreensEvent |
| Registration.java | 2 | ForgeRegistries → Registries, IEventBus param |
| ModEntityTypes.java | 2 | RegistryObject → DeferredHolder |
| ModItems.java | 2 | RegistryObject → DeferredHolder, BuildCreativeModeTabContentsEvent |
| ModBlocks.java | 2 | RegistryObject → DeferredHolder, BuildCreativeModeTabContentsEvent |
| ModMenuTypes.java | 2, 8 | RegistryObject → DeferredHolder, IForgeMenuType → IMenuTypeExtension |
| ModTileEntitiesTypes.java | 2 | RegistryObject → DeferredHolder |
| ModSounds.java | 2 | RegistryObject → DeferredHolder |
| ModRecipeSerializers.java | 2, 7 | RegistryObject → DeferredHolder, serializer lambda |
| ModEventBusEvents.java | 2 | Annotation, EntityAttributeCreationEvent import |
| ModTags.java | 2 | "forge" → "c" namespace |
| VehiclePacketHandler.java | 3 | DELETE |
| TugRoutePacketHandler.java | 3 | DELETE |
| VehicleTrackerPacketHandler.java | 3 | DELETE |
| SetEnginePacket.java | 3 | Rewrite as CustomPacketPayload record |
| EnrollVehiclePacket.java | 3 | Rewrite as CustomPacketPayload record |
| SetRouteTagPacket.java | 3 | Rewrite as CustomPacketPayload record |
| VehicleTrackerClientPacket.java | 3 | Rewrite as CustomPacketPayload record |
| NetworkHandler.java | 3 | CREATE: RegisterPayloadHandlersEvent + handlers |
| SteamTugEntity.java | 4 | LazyOptional DELETE, getCapability DELETE |
| EnergyTugEntity.java | 4 | LazyOptional DELETE, getCapability DELETE, stack null-check |
| FluidTankBargeEntity.java | 4, 5 | getCapability DELETE, defineSynchedData |
| FishingBargeEntity.java | 4, 8 | getCapability DELETE, NetworkHooks |
| AbstractBargeEntity.java | 4 | StallingCapability (done in 561c8bb0) |
| SteamLocomotiveEntity.java | 4 | LazyOptional DELETE, getCapability DELETE |
| EnergyLocomotiveEntity.java | 4 | LazyOptional DELETE, getCapability DELETE, stack null-check |
| ChestCarEntity.java | 4 | LazyOptional DELETE, getCapability DELETE |
| FluidTankCarEntity.java | 4, 5 | getCapability DELETE, defineSynchedData |
| AbstractWagonEntity.java | 4 | StallingCapability (done in 561c8bb0) |
| VesselChargerTileEntity.java | 4, 5 | getCapability DELETE, loadAdditional/saveAdditional/getUpdateTag |
| FluidHopperTileEntity.java | 4, 5 | getCapability DELETE, loadAdditional/saveAdditional/getUpdateTag, dead code |
| InventoryUtils.java | 4 | stack.getCapability() → nullable |
| IVesselLoader.java | 4 | Capability<T> → EntityCapability redesign |
| CreativeCapacitor.java | 4 | initCapabilities() DELETE |
| ModEventHandler.java | 4 | StallingCapability registration already removed |
| CapabilityRegistration.java | 4 | CREATE: RegisterCapabilitiesEvent |
| FishingBargeContainer.java | 4 | Dead code cleanup |
| AbstractTugEntity.java | 4, 5, 8, 9A, 9B | StallingCapability done, defineSynchedData, NetworkHooks, PartEntity |
| LinkingHandler.java | 5 | defineSynchedData Builder param |
| VehicleFrontPart.java | 5, 9A | defineSynchedData, PartEntity import |
| ModDataComponents.java | 6 | CREATE: DeferredRegister<DataComponentType<?>> |
| TugRoute.java | 6 | Add CODEC + STREAM_CODEC |
| TugRouteNode.java | 6 | Add CODEC |
| LocoRoute.java | 6 | Add CODEC + STREAM_CODEC |
| LocoRouteNode.java | 6 | Add CODEC |
| TugRouteItem.java | 6, 8 | Data components, NetworkHooks |
| LocoRouteItem.java | 6 | Data components |
| SpringItem.java | 6 | Data components or transient field |
| ModRecipeProvider.java | 7 | HolderLookup.Provider, buildRecipes(RecipeOutput) |
| AbstractRouteCopyRecipe.java | 7 | Remove RL ctor, assemble() HolderLookup |
| ModLootTableProvider.java | 7 | HolderLookup.Provider param |
| DataGenerators.java | 7 | Import updates, lookupProvider threading |
| ModBlockTagsProvider.java | 7 | Import update |
| ModItemTagsProvider.java | 7 | Import update |
| ModBlockStateProvider.java | 7 | Import updates |
| ModItemModelProvider.java | 7 | Import updates |
| AbstractVehicleScreen.java | 8 | renderBackground call |
| FishingBargeScreen.java | 8 | renderBackground call |
| TugRouteScreen.java | 8 | renderBackground call |
| StringInputScreen.java | 8 | renderBackground call + override signature |
| AbstractHeadVehicleScreen.java | 8 | renderBg() verification |
| ~14x ItemStackHandler files | 9A | Import relocation |
| ~25x ResourceLocation files | 9B | fromNamespaceAndPath / parse |
| CapabilityInjector.java | 10 | Create EntityCapability |
| CreateCompatibility.java | 10 | NeoForge Create API |

---

## Part 5: Effort Estimate

### Phase Effort Summary

| Phase | Description | Files | Effort | Hardest Challenge |
|-------|-------------|-------|--------|-------------------|
| 1 | Build System | 4 | MEDIUM-HIGH | DSL rewrite; Lombok version |
| 1.5 | Vanilla API Compat (NEW) | ~12 | HIGH | Boat.Status 200-line physics rewrite |
| 2 | Entrypoint & Registration | 15 | MEDIUM | Mechanical but broad |
| 3 | Networking | 7 | MEDIUM | StreamCodec pattern for all 4 packets |
| 4 | Capabilities | ~22 | CRITICAL | 14 overrides, 15 LazyOptionals, IVesselLoader redesign |
| 5 | Entity Data & BE Serialization | 10 | MEDIUM | LinkingHandler + 7 entity classes |
| 6 | Data Components | 8 | HIGH | Codec design; world save compatibility |
| 7 | Recipe & Data Gen | 9 | LOW-MEDIUM | RecipeOutput signature |
| 8 | GUI / Screens | 7 | LOW-MEDIUM | NetworkHooks.openScreen removal |
| 9A | Vanilla/NeoForge API Reworks | ~16 | MEDIUM | AbstractMinecart.Type physics rewrite |
| 9B | Mechanical Cleanup | ~25 | LOW | Scripted ResourceLocation replacement |
| 10 | Create Compat (deferred) | 2 | TBD | Depends on Create NeoForge API |

### Revised Total

| Metric | Original Plan | Revised |
|--------|--------------|---------|
| Files with breaking changes | ~30 | ~45 |
| Individual changes | ~150 | ~250+ |
| New phases needed | 0 | 1 (Phase 1.5) |
| Phases needing structural correction | 1 (Phase 9) | 2 (Phase 7 + Phase 9) |

### Gap Severity Summary

| Severity | Count | Most Critical |
|----------|-------|--------------|
| CRITICAL | 5 | Boat.Status, NetworkHooks.openScreen, AbstractMinecart.Type, VesselEntity blocker, Phase 7 wrong items |
| HIGH | 3 | AttributeModifier ctor, ForgeMod attrs, version target |
| MEDIUM | 8 | LivingEntity methods, getRailDirection, PartEntity, ModTags namespace, IVesselLoader, getUpdateTag, assemble() sig, Phase 9 split |
| LOW | 10 | ItemStackHandler, event imports, data gen imports, SpringItem, Patchouli, renderBackground override, renderBg verify, Lombok, dead code |

---

## Part 6: Explorer Contribution Summary

| Explorer | Unique Contributions | Accuracy |
|----------|---------------------|---------|
| explorer-plan-adapt | 1.21.1 vs 1.21.2+ distinction; correctly debunked Runner class and GatherDataEvent split; version corrections | HIGH |
| explorer-build-reg | Full Phase 1+2 inventory; ModTags "forge"→"c" gap; Lombok version gap | HIGH |
| explorer-net-cap | Complete capability inventory (14 overrides, 15 fields, 12 consumer sites); IVesselLoader redesign gap; dead code | HIGH |
| explorer-entity-data | Confirmed defineId bug already fixed; VehicleFrontPart edge case; Codec design sketches | HIGH |
| explorer-recipe-gui | Accurate Phase 7+8 inventory; AbstractRouteCopyRecipe assemble() gap; StringInputScreen override gap; WRONG on Runner class and GatherDataEvent split | MEDIUM |
| explorer-adversarial | Found ALL vanilla 1.20→1.21 gaps (Boat.Status, AttributeModifier, AbstractMinecart.Type, LivingEntity methods, getRailDirection, NetworkHooks, PartEntity); VesselEntity compilation blocker; Phase 9 split recommendation | CRITICAL contribution |
| explorer-cleanup | Pre-migration prep plan; StallingCapability refactor detail; confirmed pre-migration work complete in run 561c8bb0 | HIGH |
