# Phase 8: GUI / Screens Migration Plan

**Forge 1.20.1 -> NeoForge 1.21.1**
**Estimated effort:** LOW-MEDIUM
**Prerequisites:** Phase 7 (Recipe & Data Gen) complete

---

## Overview

Phase 8 migrates all GUI/screen registration and rendering APIs:

1. **IForgeMenuType -> IMenuTypeExtension** — import rename in ModMenuTypes.java
2. **MenuScreens.register() -> RegisterMenuScreensEvent** — structural change in ShippingMod.java
3. **NetworkHooks.openScreen() -> player.openMenu()** — CRITICAL gap C-2, 3 call sites + 1 dead import
4. **renderBackground() signature change** — 1-arg to 4-arg in 4 files (5 sites), including StringInputScreen override (gap L-5)

---

## Files Touched

| File | Changes |
|------|---------|
| `setup/ModMenuTypes.java` | IForgeMenuType -> IMenuTypeExtension import |
| `ShippingMod.java` | MenuScreens.register() -> RegisterMenuScreensEvent; remove doClientStuff listener |
| `entity/custom/vessel/tug/AbstractTugEntity.java` | NetworkHooks.openScreen() -> player.openMenu() |
| `entity/custom/train/locomotive/AbstractLocomotiveEntity.java` | NetworkHooks.openScreen() -> player.openMenu() |
| `item/TugRouteItem.java` | NetworkHooks.openScreen() -> player.openMenu() |
| `entity/custom/vessel/barge/FishingBargeEntity.java` | Remove dead `import NetworkHooks` (no openScreen call) |
| `entity/container/AbstractVehicleScreen.java` | renderBackground() 1-arg -> 4-arg call |
| `entity/container/FishingBargeScreen.java` | renderBackground() 1-arg -> 4-arg call |
| `item/container/TugRouteScreen.java` | renderBackground() 1-arg -> 4-arg call |
| `item/container/StringInputScreen.java` | renderBackground() call AND override signature -> 4-arg |

**Not changed (verified):**
- `entity/container/AbstractHeadVehicleScreen.java` — overrides `renderBg()` (AbstractContainerScreen method), NOT `renderBackground()`. The `renderBg(GuiGraphics, float, int, int)` signature is unchanged in NeoForge 1.21.1. Gap L-6 is a false alarm.
- `entity/container/SteamHeadVehicleScreen.java` — only overrides `renderBg()` and `render()`, no `renderBackground()` calls
- `entity/container/EnergyHeadVehicleScreen.java` — same as SteamHeadVehicleScreen

---

## Step 1: IForgeMenuType -> IMenuTypeExtension

**File:** `setup/ModMenuTypes.java`
**Effort:** Trivial (import rename only)

The method name `create()` and the lambda signature `(windowId, inv, data) -> ...` are identical.

```java
// BEFORE (line 13)
import net.minecraftforge.common.extensions.IForgeMenuType;

// AFTER
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
```

Then replace all 6 occurrences of `IForgeMenuType.create(` with `IMenuTypeExtension.create(`:
- Line 29: TUG_CONTAINER
- Line 35: ENERGY_TUG_CONTAINER
- Line 41: STEAM_LOCOMOTIVE_CONTAINER
- Line 47: ENERGY_LOCOMOTIVE_CONTAINER
- Line 53: FISHING_BARGE_CONTAINER
- Line 59: TUG_ROUTE_CONTAINER

**Note:** `RegistryObject<MenuType<...>>` will also change to `DeferredHolder<MenuType<?>, MenuType<...>>` as part of Phase 2 (Registration), not Phase 8.

---

## Step 2: MenuScreens.register() -> RegisterMenuScreensEvent

**File:** `ShippingMod.java`
**Effort:** Low (structural move to event-driven registration)

### Current code (lines 41-51):
```java
private void doClientStuff(final FMLClientSetupEvent event) {
    MenuScreens.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
    MenuScreens.register(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), SteamHeadVehicleScreen<SteamLocomotiveEntity>::new);
    MenuScreens.register(ModMenuTypes.ENERGY_TUG_CONTAINER.get(),  EnergyHeadVehicleScreen<EnergyTugEntity>::new);
    MenuScreens.register(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyLocomotiveEntity>::new);
    MenuScreens.register(ModMenuTypes.FISHING_BARGE_CONTAINER.get(), FishingBargeScreen::new);
    MenuScreens.register(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);

    event.enqueueWork(ModItemModelProperties::register);
}
```

### Target code:
```java
// In ShippingMod constructor, REMOVE:
//   FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
// (The @SubscribeEvent on the mod bus picks it up automatically via @Mod.EventBusSubscriber
//  or manual registration — see note below)

// Option A: Inner class with @Mod.EventBusSubscriber (client-only)
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public static class ClientEvents {
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
        event.register(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), SteamHeadVehicleScreen<SteamLocomotiveEntity>::new);
        event.register(ModMenuTypes.ENERGY_TUG_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyTugEntity>::new);
        event.register(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyLocomotiveEntity>::new);
        event.register(ModMenuTypes.FISHING_BARGE_CONTAINER.get(), FishingBargeScreen::new);
        event.register(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);
    }
}
```

### Important details:
- `RegisterMenuScreensEvent` is a **mod bus** event, not game bus
- Import: `net.neoforged.neoforge.client.event.RegisterMenuScreensEvent`
- Remove import: `net.minecraft.client.gui.screens.MenuScreens`
- The `ModItemModelProperties::register` call from `doClientStuff` must be moved elsewhere (likely to a `FMLClientSetupEvent` handler if it still exists, or to a separate client event). This is an item model concern, not a screen concern — track separately.

---

## Step 3: NetworkHooks.openScreen() -> player.openMenu() [CRITICAL C-2]

**Impact:** 3 files fail to compile; 1 file has dead import
**Effort:** Low per-file, but CRITICAL for compilation

### API change:
```java
// BEFORE (Forge 1.20.1)
import net.minecraftforge.network.NetworkHooks;
NetworkHooks.openScreen((ServerPlayer) player, menuProvider, dataWriter);
// dataWriter type: Consumer<FriendlyByteBuf>

// AFTER (NeoForge 1.21.1)
// No import needed — it's a method on ServerPlayer
((ServerPlayer) player).openMenu(menuProvider, dataWriter);
// dataWriter type: Consumer<RegistryFriendlyByteBuf>
```

**Key type change:** `Consumer<FriendlyByteBuf>` -> `Consumer<RegistryFriendlyByteBuf>`. The `getDataAccessor()::write` method references used in all 3 call sites must accept `RegistryFriendlyByteBuf` (which extends `FriendlyByteBuf`, so existing write methods likely still work — but verify the `::write` method signature in each DataAccessor class).

### File 1: `entity/custom/vessel/tug/AbstractTugEntity.java` (line 310)

```java
// BEFORE
NetworkHooks.openScreen((ServerPlayer) player, createContainerProvider(), getDataAccessor()::write);

// AFTER
((ServerPlayer) player).openMenu(createContainerProvider(), getDataAccessor()::write);
```

Remove: `import net.minecraftforge.network.NetworkHooks;`

### File 2: `entity/custom/train/locomotive/AbstractLocomotiveEntity.java` (line 136)

```java
// BEFORE
NetworkHooks.openScreen((ServerPlayer) pPlayer, createContainerProvider(), getDataAccessor()::write);

// AFTER
((ServerPlayer) pPlayer).openMenu(createContainerProvider(), getDataAccessor()::write);
```

Remove: `import net.minecraftforge.network.NetworkHooks;`

### File 3: `item/TugRouteItem.java` (line 66)

```java
// BEFORE
NetworkHooks.openScreen((ServerPlayer) player, createContainerProvider(hand), getDataAccessor(player, hand)::write);

// AFTER
((ServerPlayer) player).openMenu(createContainerProvider(hand), getDataAccessor(player, hand)::write);
```

Remove: `import net.minecraftforge.network.NetworkHooks;`

### File 4: `entity/custom/vessel/barge/FishingBargeEntity.java`

**No openScreen call exists.** The evaluation listed this file, but the import is dead code (line 43).

```java
// REMOVE dead import (line 43)
import net.minecraftforge.network.NetworkHooks;
```

### DataAccessor verification needed:
The `::write` method references resolve to methods like `SteamHeadVehicleDataAccessor::write`, `EnergyHeadVehicleDataAccessor::write`, and `TugRouteScreenDataAccessor::write`. These write `int` values to a `FriendlyByteBuf`. Since `RegistryFriendlyByteBuf extends FriendlyByteBuf`, the `writeInt()` calls are inherited and the method references should work without change. **Verify at compile time.**

---

## Step 4: renderBackground() Signature Change

**Effort:** Low (mechanical)

### API change:
```java
// BEFORE (Forge 1.20.1)
this.renderBackground(graphics);                                    // call
public void renderBackground(GuiGraphics graphics) { ... }         // override

// AFTER (NeoForge 1.21.1)
this.renderBackground(graphics, mouseX, mouseY, partialTick);      // call
public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { ... }  // override
```

### 4a: AbstractVehicleScreen.java (line 21) — call site only

```java
// BEFORE
public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTicks);
    this.renderTooltip(graphics, mouseX, mouseY);
}

// AFTER
public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(graphics, mouseX, mouseY, partialTicks);
    super.render(graphics, mouseX, mouseY, partialTicks);
    this.renderTooltip(graphics, mouseX, mouseY);
}
```

### 4b: FishingBargeScreen.java (line 28) — call site only

```java
// BEFORE
this.renderBackground(graphics);

// AFTER
this.renderBackground(graphics, x, y, partialTicks);
```

### 4c: TugRouteScreen.java (line 102) — call site only

```java
// BEFORE
this.renderBackground(graphics);

// AFTER
this.renderBackground(graphics, mouseX, mouseY, partialTicks);
```

### 4d: StringInputScreen.java — call site (line 68) AND override (line 72) [Gap L-5]

This is the most complex renderBackground change because StringInputScreen overrides `renderBackground()` to draw a custom texture instead of the default dim background.

```java
// BEFORE — render() calls renderBackground with 1 arg
public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTicks);
}

// BEFORE — custom override with 1 arg
public void renderBackground(@NotNull GuiGraphics graphics) {
    int w = 156, h = 65;
    int i = (this.width - w) / 2;
    int j = (this.height - h) / 2;
    graphics.blit(GUI, i, j, 0, 0, w, h);
}

// AFTER — render() calls renderBackground with 4 args
public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(graphics, mouseX, mouseY, partialTicks);
    super.render(graphics, mouseX, mouseY, partialTicks);
}

// AFTER — custom override with 4 args (extra params unused but required)
@Override
public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    int w = 156, h = 65;
    int i = (this.width - w) / 2;
    int j = (this.height - h) / 2;
    graphics.blit(GUI, i, j, 0, 0, w, h);
}
```

**Note:** The override intentionally does NOT call `super.renderBackground()` — it replaces the default dim background with a custom texture. This is correct behavior that must be preserved.

### 4e: AbstractHeadVehicleScreen.java — NO CHANGE NEEDED [Gap L-6 resolved]

`AbstractHeadVehicleScreen` overrides `renderBg()` (line 83), which is `AbstractContainerScreen.renderBg(GuiGraphics, float, int, int)`. This is a **different method** from `Screen.renderBackground()`. The `renderBg()` signature is unchanged in NeoForge 1.21.1. No action required.

---

## Step 5: Additional Cleanup (Phase 9B overlap)

These are NOT Phase 8 tasks but will be encountered while editing Phase 8 files. Note for Phase 9B:

- `new ResourceLocation(namespace, path)` -> `ResourceLocation.fromNamespaceAndPath(namespace, path)` in:
  - `StringInputScreen.java:23`
  - `TugRouteScreen.java:26`
  - `AbstractHeadVehicleScreen.java:15`
  - `SteamHeadVehicleScreen.java:16`
  - `EnergyHeadVehicleScreen.java:20`
  - `FishingBargeScreen.java:12`
  - `ShippingMod.java:54`

Do NOT apply these in Phase 8 — they belong in the Phase 9B mechanical cleanup pass.

---

## Execution Order

1. **Step 1** (ModMenuTypes) — no dependencies
2. **Step 2** (ShippingMod RegisterMenuScreensEvent) — no dependencies
3. **Step 3** (NetworkHooks removal) — no dependencies
4. **Step 4** (renderBackground) — no dependencies

Steps 1-4 are independent and can be done in any order or in parallel.

---

## Verification

After all changes:

1. **Compile check:** `./gradlew build` — all 10 files must compile
2. **Grep for removed APIs:**
   - `grep -r "IForgeMenuType" src/` -> 0 results
   - `grep -r "MenuScreens.register" src/` -> 0 results
   - `grep -r "NetworkHooks" src/` -> 0 results
   - `grep -r "renderBackground(graphics)" src/` -> 0 results (all should be 4-arg now)
3. **Runtime test:** Launch with `./gradlew runClient`
   - Right-click a steam tug -> GUI opens (tests Step 2 + Step 3)
   - Right-click a fishing barge -> no crash (tests dead import removal)
   - Shift-right-click a tug route item -> route GUI opens (tests Step 3 TugRouteItem)
   - Open route screen, click rename button -> StringInputScreen renders correctly with custom background (tests Step 4d / L-5)
4. **No regressions:** Verify all 6 menu types open their correct screens

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| RegistryFriendlyByteBuf incompatibility in DataAccessor::write | Low | Medium | RegistryFriendlyByteBuf extends FriendlyByteBuf; writeInt() inherited |
| StringInputScreen background rendering broken | Low | Low | Custom blit logic is self-contained; just needs signature update |
| ModItemModelProperties::register orphaned | Medium | Low | Must be moved to a surviving client event; track as follow-up |
| Screen constructor changes in 1.21.1 | Low | Medium | Screen/AbstractContainerScreen constructors are stable |
