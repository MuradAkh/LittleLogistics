# NeoForge 1.21.1 Migration — Phase Plans

Concrete, step-by-step migration plans for each phase of the Forge 1.20.1 → NeoForge 1.21.1 port.

See [evaluation.md](../2026-03-13-migration-eval.md) for the comprehensive gap analysis that informed these plans.

## Phases

| Phase | Doc | Effort | Key Challenge |
|-------|-----|--------|---------------|
| 1 | [Build System](phase-1-build-system.md) | MEDIUM-HIGH | ForgeGradle → NeoGradle, Java 17 → 21 |
| 1.5 | [Vanilla API Compatibility](phase-1.5-vanilla-api.md) | HIGH | Boat.Status physics rewrite, AttributeModifier, AbstractMinecart.Type removal |
| 2 | [Mod Entrypoint & Registration](phase-2-registration.md) | MEDIUM | RegistryObject → DeferredHolder across 15 files |
| 3 | [Networking](phase-3-networking.md) | MEDIUM | SimpleChannel → CustomPacketPayload records with StreamCodec |
| 4 | [Capabilities](phase-4-capabilities.md) | CRITICAL | 14 getCapability() overrides, 15 LazyOptional fields, centralized RegisterCapabilitiesEvent |
| 5 | [Entity Data & BlockEntity Serialization](phase-5-entity-data.md) | ✅ DONE | defineSynchedData Builder pattern, HolderLookup.Provider on BlockEntity load/save |
| 6 | [Data Components](phase-6-data-components.md) | ✅ DONE | Codec design for TugRoute/LocoRoute, Item NBT → DataComponentType |
| 7 | [Recipe & Data Generation](phase-7-recipe-datagen.md) | ✅ DONE | RecipeOutput signature (NO Runner class in 1.21.1) |
| 8 | [GUI / Screens](phase-8-gui-screens.md) | ✅ DONE | NetworkHooks.openScreen() removal, RegisterMenuScreensEvent |
| 9A | [Vanilla/NeoForge API Reworks](phase-9a-api-reworks.md) | ✅ DONE | AbstractMinecart.Type physics rewrite, PartEntity relocation |
| 9B | [Mechanical Cleanup](phase-9b-mechanical-cleanup.md) | ✅ DONE | 80x ResourceLocation find-and-replace |
| 10 | [Create Mod Compatibility](phase-10-create-compat.md) | DEFERRED | Follow-up PR after Create for NeoForge 1.21.1 is available |

## Execution Order

Phases must be executed in order. Each phase gate requires `./gradlew build` to pass before proceeding to the next.

```
Phase 1 → 1.5 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9A → 9B → 10
```

## Pre-Migration (Complete)

The following pre-migration tasks were completed in run 561c8bb0:
- Fixed defineId bug in FluidTankBargeEntity and FluidTankCarEntity
- Removed dead EVENT_BUS.register(this) in ShippingMod
- Replaced StallingCapability with instanceof pattern
- Added JUnit 5 unit tests for SpringPhysicsUtil and route serialization
