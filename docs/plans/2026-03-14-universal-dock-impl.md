# Universal Dock System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the multi-block dock/hopper system with universal dock blocks that expose NeoForge capabilities as transparent proxies to docked vehicles.

**Architecture:** Two new blocks (DockBlock for water, DockRail for rail) replace seven existing blocks. Each dock exposes ItemHandler/FluidHandler/EnergyStorage block capabilities that delegate to the docked vehicle's entity capabilities. Vehicles initiate dock checks; docks decide whether to hold via a pass-through chain rule and per-dock idle timeout.

**Tech Stack:** NeoForge 1.21.1, Java 21, DeferredRegister pattern, NeoForge Capabilities API, Lombok

**Design doc:** `docs/plans/2026-03-14-universal-dock-design.md`

---

## Task 1: Register Missing Vehicle Capabilities

Fix the existing gap where ChestBargeEntity and BarrelBargeEntity don't expose `ItemHandler.ENTITY`.

**Files:**
- Modify: `src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java:29-56`

**Step 1: Add ItemHandler.ENTITY for ChestBargeEntity**

In `CapabilityRegistration.java`, in the method that registers `ItemHandler.ENTITY` capabilities, add registrations for `CHEST_BARGE` and `BARREL_BARGE`. Follow the existing pattern used for `CHEST_CAR` and `BARREL_CAR` (around lines 45-56).

Both `ChestBargeEntity` and `BarrelBargeEntity` extend `AbstractBargeEntity` which implements `Container` and `WorldlyContainer`. They should expose an `InvWrapper` (or `SidedInvWrapper`) similar to how `ChestCarEntity` does it. Check how `ChestCarEntity` exposes its handler and replicate the pattern.

Look at `ModEntityTypes.java` for the entity type holders: `CHEST_BARGE` and `BARREL_BARGE`.

**Step 2: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java
git commit -m "fix: register ItemHandler.ENTITY for chest and barrel barges"
```

---

## Task 2: Create DockBlockEntity (Core Logic)

This is the heart of the new system — a block entity that detects docked vehicles, proxies their capabilities, and manages hold state.

**Files:**
- Create: `src/main/java/dev/murad/shipping/block/dock/DockBlockEntity.java`

**Step 1: Create DockBlockEntity class**

```java
package dev.murad.shipping.block.dock;

public class DockBlockEntity extends BlockEntity {
    // --- Configuration (persisted) ---
    private int idleTimeoutTicks = 100; // 5 seconds default
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    public enum RedstoneMode { IGNORE, HOLD_WHILE_POWERED, DISABLE_WHILE_POWERED }

    // --- Runtime state (not persisted) ---
    private Entity dockedVehicle = null;
    private int ticksSinceLastTransfer = 0;
    private boolean holdActive = false;

    // --- Capability wrappers (always valid, delegate or no-op) ---
    private DockItemHandler itemHandler;
    private DockFluidHandler fluidHandler;
    private DockEnergyStorage energyStorage;
}
```

Key methods to implement:

1. **`occupyDock(Entity vehicle)`** — called by the vehicle when it docks here. Sets `dockedVehicle`, resets `ticksSinceLastTransfer`, resolves vehicle capabilities, connects wrappers.

2. **`vacateDock()`** — called when vehicle undocks. Clears `dockedVehicle`, disconnects wrappers (they return no-op).

3. **`isHolding()`** — returns whether this dock wants the train to stay:
   - If `redstoneMode == HOLD_WHILE_POWERED` and powered → true
   - If `redstoneMode == DISABLE_WHILE_POWERED` and powered → false
   - If `dockedVehicle == null` → false
   - If `ticksSinceLastTransfer < idleTimeoutTicks` → true
   - Else → false

4. **`notifyTransfer()`** — called by the capability wrappers whenever an insert/extract operation succeeds. Resets `ticksSinceLastTransfer = 0`.

5. **`tick()`** — increments `ticksSinceLastTransfer` if a vehicle is docked. (This is the only ticking behavior — lightweight.)

6. **`shouldPassThrough(Direction vehicleHeading)`** — checks if there's another dock block/rail in the `vehicleHeading` direction. If yes, return true (let the head vehicle pass). If no, return false (dock here). This is a method on the block entity but could also live on the block — put it here since it needs world access.

7. **`saveAdditional(CompoundTag, HolderLookup.Provider)`** / **`loadAdditional(CompoundTag, HolderLookup.Provider)`** — persist `idleTimeoutTicks` and `redstoneMode`.

8. **`adjustTimeout(int delta)`** — called from block interaction. Cycles through preset values: 20, 40, 100, 200, 400 ticks (1s, 2s, 5s, 10s, 20s).

9. **`cycleRedstoneMode()`** — cycles IGNORE → HOLD_WHILE_POWERED → DISABLE_WHILE_POWERED → IGNORE.

**Step 2: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockBlockEntity.java
git commit -m "feat: add DockBlockEntity with hold logic and config"
```

---

## Task 3: Create Capability Wrapper Classes

Three wrapper classes that always exist on the dock but delegate to the vehicle when one is present.

**Files:**
- Create: `src/main/java/dev/murad/shipping/block/dock/DockItemHandler.java`
- Create: `src/main/java/dev/murad/shipping/block/dock/DockFluidHandler.java`
- Create: `src/main/java/dev/murad/shipping/block/dock/DockEnergyStorage.java`

**Step 1: Create DockItemHandler**

Implements `IItemHandler`. Holds a nullable reference to the vehicle's `IItemHandler`. All methods delegate when connected, return empty/reject when not.

Key: on any successful `insertItem()` or `extractItem()` (where the returned stack differs from input), call `dockBlockEntity.notifyTransfer()`.

```java
package dev.murad.shipping.block.dock;

public class DockItemHandler implements IItemHandler {
    private final DockBlockEntity dock;
    @Nullable private IItemHandler delegate;

    public void connect(IItemHandler handler) { this.delegate = handler; }
    public void disconnect() { this.delegate = null; }

    @Override
    public int getSlots() {
        return delegate != null ? delegate.getSlots() : 0;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (delegate == null) return stack;
        ItemStack result = delegate.insertItem(slot, stack, simulate);
        if (!simulate && result.getCount() != stack.getCount()) dock.notifyTransfer();
        return result;
    }

    // ... same pattern for extractItem, getStackInSlot, getSlotLimit, isItemValid
}
```

**Step 2: Create DockFluidHandler**

Implements `IFluidHandler`. Same pattern — delegate or no-op. Call `notifyTransfer()` on successful `fill()` or `drain()`.

**Step 3: Create DockEnergyStorage**

Implements `IEnergyStorage`. Same pattern — delegate or no-op. Call `notifyTransfer()` on successful `receiveEnergy()` or `extractEnergy()`.

**Step 4: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockItemHandler.java \
        src/main/java/dev/murad/shipping/block/dock/DockFluidHandler.java \
        src/main/java/dev/murad/shipping/block/dock/DockEnergyStorage.java
git commit -m "feat: add capability wrapper classes for dock delegation"
```

---

## Task 4: Create DockBlock (Water)

The universal water dock block replacing TugDockBlock and BargeDockBlock.

**Files:**
- Create: `src/main/java/dev/murad/shipping/block/dock/DockBlock.java`

**Step 1: Create DockBlock**

```java
package dev.murad.shipping.block.dock;

public class DockBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
}
```

Key behaviors:

1. **Block states:** Just `FACING` (horizontal direction the waterway runs).

2. **`getStateForPlacement()`** — face opposite to player look direction (same as current `AbstractDockBlock`).

3. **`newBlockEntity()`** — returns new `DockBlockEntity`.

4. **`useWithoutItem()`** — detect which part of the hitbox was clicked. If the interaction zone: cycle redstone mode. Otherwise: no action. (The scroll-to-adjust-timeout will need a packet, handled in Task 8.)

5. **`getShape()` / `getInteractionShape()`** — standard block shape. The interaction zone is a visual/UX concept handled client-side (Task 9).

6. **`canConnectRedstone()`** — return true.

7. **`neighborChanged()`** — no hopper auto-aiming needed anymore (eliminated). Just handle standard block updates.

**Step 2: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockBlock.java
git commit -m "feat: add universal DockBlock for water vehicles"
```

---

## Task 5: Create DockRail

The universal rail dock block replacing LocomotiveDockingRail and TrainCarDockingRail.

**Files:**
- Create: `src/main/java/dev/murad/shipping/block/dock/DockRail.java`

**Step 1: Create DockRail**

Extends `BaseRailBlock` and implements `EntityBlock`. Reference the existing `AbstractDockingRail` for rail-specific behaviors.

```java
package dev.murad.shipping.block.dock;

public class DockRail extends BaseRailBlock implements EntityBlock {
    public static final EnumProperty<RailShape> RAIL_SHAPE =
        BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty WATERLOGGED =
        BlockStateProperties.WATERLOGGED;
}
```

Key behaviors:

1. **Block states:** `RAIL_SHAPE` (straight only — N/S or E/W), `WATERLOGGED`.

2. **`canMakeSlopes()`** — return false (flat only, same as current).

3. **`getStateForPlacement()`** — infer rail shape from player facing direction. Use `AbstractDockingRail.getRailShapeFromFacing()` logic.

4. **`canSurvive()`** — standard rail survival check. Remove the special case for `FLUID_HOPPER` since that block is being eliminated.

5. **`newBlockEntity()`** — returns new `DockBlockEntity`.

6. **`useWithoutItem()`** — same interaction zone logic as DockBlock for redstone mode cycling.

7. **`canConnectRedstone()`** — return true.

8. **Rail tag:** Must be in `BlockTags.RAILS` (handled in data gen, Task 7).

**Step 2: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockRail.java
git commit -m "feat: add universal DockRail for train vehicles"
```

---

## Task 6: Registration & Capability Wiring

Register the new blocks, block entities, and their capabilities. Remove old registrations.

**Files:**
- Modify: `src/main/java/dev/murad/shipping/setup/ModBlocks.java`
- Modify: `src/main/java/dev/murad/shipping/setup/ModItems.java` (if items are registered separately)
- Modify: `src/main/java/dev/murad/shipping/setup/ModTileEntitiesTypes.java`
- Modify: `src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java`

**Step 1: Register new blocks in ModBlocks.java**

Add:
```java
public static final DeferredHolder<Block, Block> DOCK_BLOCK = register(
    "dock", () -> new DockBlock(BlockBehaviour.Properties.of()...));

public static final DeferredHolder<Block, Block> DOCK_RAIL = register(
    "dock_rail", () -> new DockRail(BlockBehaviour.Properties.of()...));
```

Remove the old registrations:
- `TUG_DOCK`, `BARGE_DOCK`, `FLUID_HOPPER`, `VESSEL_CHARGER`, `RAPID_HOPPER`, `CAR_DOCK_RAIL`, `LOCOMOTIVE_DOCK_RAIL`

Copy appropriate `BlockBehaviour.Properties` from the old blocks (material, strength, etc.).

**Step 2: Register block entity type in ModTileEntitiesTypes.java**

Add:
```java
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DockBlockEntity>> DOCK = register(
    "dock",
    () -> BlockEntityType.Builder.of(DockBlockEntity::new,
        ModBlocks.DOCK_BLOCK.get(), ModBlocks.DOCK_RAIL.get()).build(null));
```

Remove old tile entity registrations: `TUG_DOCK`, `BARGE_DOCK`, `LOCOMOTIVE_DOCK`, `CAR_DOCK`, `FLUID_HOPPER`, `VESSEL_CHARGER`, `RAPID_HOPPER`.

**Step 3: Register block capabilities in CapabilityRegistration.java**

Add capability registrations for the new `DOCK` block entity type:

```java
event.registerBlockEntity(
    Capabilities.ItemHandler.BLOCK,
    ModTileEntitiesTypes.DOCK.get(),
    (blockEntity, direction) -> blockEntity.getItemHandler()
);
event.registerBlockEntity(
    Capabilities.FluidHandler.BLOCK,
    ModTileEntitiesTypes.DOCK.get(),
    (blockEntity, direction) -> blockEntity.getFluidHandler()
);
event.registerBlockEntity(
    Capabilities.EnergyStorage.BLOCK,
    ModTileEntitiesTypes.DOCK.get(),
    (blockEntity, direction) -> blockEntity.getEnergyStorage()
);
```

Remove old capability registrations for `VESSEL_CHARGER` and `FLUID_HOPPER` block entities.

**Step 4: Remove old config values**

In `ShippingConfig.java`, remove `VESSEL_CHARGER_BASE_CAPACITY` and `VESSEL_CHARGER_BASE_MAX_TRANSFER` (lines 75-76, 158-161). The dock doesn't have its own energy buffer — transfer rate is controlled by the connecting mod.

**Step 5: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/dev/murad/shipping/setup/ModBlocks.java \
        src/main/java/dev/murad/shipping/setup/ModTileEntitiesTypes.java \
        src/main/java/dev/murad/shipping/setup/CapabilityRegistration.java \
        src/main/java/dev/murad/shipping/ShippingConfig.java
git commit -m "feat: register universal dock blocks and capabilities, remove old blocks"
```

---

## Task 7: Data Generators

Update recipes, loot tables, block states, block tags, and item models for the new blocks.

**Files:**
- Modify: `src/main/java/dev/murad/shipping/data/ModRecipeProvider.java`
- Modify: `src/main/java/dev/murad/shipping/data/ModLootTableProvider.java`
- Modify: `src/main/java/dev/murad/shipping/data/client/ModBlockStateProvider.java`
- Modify: `src/main/java/dev/murad/shipping/data/ModBlockTagsProvider.java`

**Step 1: Update ModRecipeProvider.java**

Remove old recipes for: `TUG_DOCK`, `BARGE_DOCK`, `FLUID_HOPPER`, `VESSEL_CHARGER`, `RAPID_HOPPER`, `LOCOMOTIVE_DOCK_RAIL`, `CAR_DOCK_RAIL`.

Add new recipes for `DOCK_BLOCK` and `DOCK_RAIL`. Design appropriate crafting recipes — suggest:

- `DOCK_BLOCK`: Similar to old TUG_DOCK recipe but simplified (e.g., iron ingots + chest or similar)
- `DOCK_RAIL`: Similar to old LOCOMOTIVE_DOCK_RAIL recipe but simplified (e.g., rails + iron ingots)

Also update any recipes that used `VESSEL_CHARGER` as an ingredient (lines 188, 296 — used in energy tug and energy locomotive recipes). Replace with an appropriate alternative ingredient (e.g., redstone block, copper block).

**Step 2: Update ModLootTableProvider.java**

Remove `dropSelf()` calls for old blocks. Add:
```java
dropSelf(ModBlocks.DOCK_BLOCK.get());
dropSelf(ModBlocks.DOCK_RAIL.get());
```

**Step 3: Update ModBlockStateProvider.java**

Remove blockstate definitions for old blocks. Add variant builders for `DOCK_BLOCK` (horizontal facing) and `DOCK_RAIL` (rail shape). You'll need block models — for now, reference placeholder models and create proper ones later.

**Step 4: Update ModBlockTagsProvider.java**

Remove old rail blocks from `BlockTags.RAILS`. Add:
```java
tag(BlockTags.RAILS).add(ModBlocks.DOCK_RAIL.get());
```

**Step 5: Run data generators**

Run: `./gradlew runData`
Expected: Data generation completes successfully, generated resources updated.

**Step 6: Verify full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add src/main/java/dev/murad/shipping/data/ src/generated/
git commit -m "feat: update data generators for universal dock blocks"
```

---

## Task 8: Rewire Vehicle Docking Logic

Update `AbstractTugEntity` and `AbstractLocomotiveEntity` to use the new `DockBlockEntity` instead of the old dock tile entities.

**Files:**
- Modify: `src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java`
- Modify: `src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java`

**Step 1: Rewrite AbstractTugEntity.tickCheckDock()**

Current logic (lines 225-259): scans side directions for `TugDockTileEntity`, calls `hold()`, docks/undocks.

New logic:

```java
private void tickCheckDock() {
    BlockPos pos = this.blockPosition();
    boolean docked = this.isDocked();

    if (docked && dockCheckCooldown > 0) {
        dockCheckCooldown--;
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(pos.getX() + 0.5, getY(), pos.getZ() + 0.5);
        return;
    }

    // Check if we're on/adjacent to a DockBlock
    DockBlockEntity dock = findAdjacentDock(pos);

    if (dock == null) {
        if (docked) undock();
        return;
    }

    // Pass-through rule: if there's another dock ahead, don't stop here
    if (!docked && dock.shouldPassThrough(this.getDirection())) {
        return;
    }

    // Check hold across all docks in the chain
    boolean shouldDock = dock.isHolding() || isAnyFollowerDockHolding();

    // First arrival — always dock initially, register with dock
    if (!docked && dock != null) {
        shouldDock = true;
    }

    if (shouldDock) {
        if (!docked) {
            dock.occupyDock(this);
            occupyFollowerDocks();
            onDock();
        }
        dockCheckCooldown = 5; // reduced from 20
        this.dock(pos.getX() + 0.5, getY(), pos.getZ() + 0.5);
    } else {
        vacateAllDocks();
        this.undock();
        onUndock();
    }
}
```

Add helper methods:
- `findAdjacentDock(BlockPos)` — checks side directions for `DockBlock` with a `DockBlockEntity`
- `isAnyFollowerDockHolding()` — walks `LinkableEntity` follower chain, checks if block at each follower's position is a dock, returns true if any `isHolding()`
- `occupyFollowerDocks()` — walks follower chain, calls `occupyDock(follower)` on each dock
- `vacateAllDocks()` — walks follower chain, calls `vacateDock()` on each dock

**Step 2: Rewrite AbstractLocomotiveEntity.tickDockCheck()**

Same pattern as tug, but:
- Check block **at** the locomotive's position (rail dock is under the train, not to the side)
- `findDockRail(BlockPos)` — checks `blockPosition()` for `DockRail` with a `DockBlockEntity`
- Pass-through checks rail direction for another `DockRail` ahead

Reference current implementation at lines 376+ in `AbstractLocomotiveEntity.java`.

**Step 3: Update TugNodeProcessor.java**

In `src/main/java/dev/murad/shipping/entity/navigation/TugNodeProcessor.java`, lines 82-83 reference `ModBlocks.BARGE_DOCK` and `ModBlocks.TUG_DOCK`. Update to `ModBlocks.DOCK_BLOCK`.

**Step 4: Update ModClientEventHandler.java**

In `src/main/java/dev/murad/shipping/event/ModClientEventHandler.java`:
- Remove render layer registrations for old blocks (lines 40-41, 47-48)
- Remove `FluidHopperTileEntityRenderer` registration (line 192)
- Add render layer for new dock blocks if needed

**Step 5: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/dev/murad/shipping/entity/custom/vessel/tug/AbstractTugEntity.java \
        src/main/java/dev/murad/shipping/entity/custom/train/locomotive/AbstractLocomotiveEntity.java \
        src/main/java/dev/murad/shipping/entity/navigation/TugNodeProcessor.java \
        src/main/java/dev/murad/shipping/event/ModClientEventHandler.java
git commit -m "feat: rewire vehicle docking to use universal DockBlockEntity"
```

---

## Task 9: Create-Style Scroll Interaction (Networking)

Add scroll-to-adjust-timeout and the network packet to sync it.

**Files:**
- Create: `src/main/java/dev/murad/shipping/network/SetDockConfigPacket.java`
- Modify: Existing network registration (check how other packets like `SetEnginePacket` are registered)
- Modify: `src/main/java/dev/murad/shipping/block/dock/DockBlock.java`
- Modify: `src/main/java/dev/murad/shipping/block/dock/DockRail.java`

**Step 1: Create SetDockConfigPacket**

Follow the existing `CustomPacketPayload` record pattern used by `SetEnginePacket`, `SetRouteTagPacket`, etc. in `src/main/java/dev/murad/shipping/network/`.

```java
public record SetDockConfigPacket(BlockPos pos, int timeoutTicks)
    implements CustomPacketPayload {
    // StreamCodec, TYPE, handle() method
}
```

The `handle()` method:
1. Get `DockBlockEntity` at `pos`
2. Call `setIdleTimeoutTicks(timeoutTicks)`
3. Mark block entity dirty

**Step 2: Register the packet**

Look at how existing packets are registered (likely in a `ModNetworking` class or in `ModSetup`). Add `SetDockConfigPacket` to the registration.

**Step 3: Client-side scroll handler**

This needs a client-side input event listener. When the player looks at a dock block's interaction zone and scrolls, send `SetDockConfigPacket` to the server.

Register a handler for `InputEvent.MouseScrollingEvent` (or the NeoForge equivalent). Check if the player is looking at a dock block, compute the new timeout, send the packet.

The interaction zone hit detection: use the block's `getShape()` and raytrace subdivision, or simply check if the player is looking at the block and sneaking (simpler UX: sneak+scroll = adjust timeout).

**Step 4: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/dev/murad/shipping/network/SetDockConfigPacket.java \
        src/main/java/dev/murad/shipping/block/dock/DockBlock.java \
        src/main/java/dev/murad/shipping/block/dock/DockRail.java
git commit -m "feat: add scroll-to-configure dock timeout with network sync"
```

---

## Task 10: Delete Old Code

Remove all old dock, hopper, charger, and rapid hopper code.

**Files to delete:**
- `src/main/java/dev/murad/shipping/block/dock/AbstractDockBlock.java`
- `src/main/java/dev/murad/shipping/block/dock/AbstractDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/dock/AbstractHeadDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/dock/AbstractTailDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/dock/BargeDockBlock.java`
- `src/main/java/dev/murad/shipping/block/dock/BargeDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/dock/TugDockBlock.java`
- `src/main/java/dev/murad/shipping/block/dock/TugDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/dock/DockingBlockStates.java`
- `src/main/java/dev/murad/shipping/block/rail/AbstractDockingRail.java`
- `src/main/java/dev/murad/shipping/block/rail/LocomotiveDockingRail.java`
- `src/main/java/dev/murad/shipping/block/rail/TrainCarDockingRail.java`
- `src/main/java/dev/murad/shipping/block/rail/blockentity/LocomotiveDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/rail/blockentity/TrainCarDockTileEntity.java`
- `src/main/java/dev/murad/shipping/block/fluid/FluidHopperBlock.java`
- `src/main/java/dev/murad/shipping/block/fluid/FluidHopperTileEntity.java`
- `src/main/java/dev/murad/shipping/block/fluid/render/FluidHopperTileEntityRenderer.java`
- `src/main/java/dev/murad/shipping/block/energy/VesselChargerBlock.java`
- `src/main/java/dev/murad/shipping/block/energy/VesselChargerTileEntity.java`
- `src/main/java/dev/murad/shipping/block/rapidhopper/RapidHopperBlock.java`
- `src/main/java/dev/murad/shipping/block/rapidhopper/RapidHopperTileEntity.java`

**Files to modify (remove dead references):**
- `src/main/java/dev/murad/shipping/block/IVesselLoader.java` — review if still needed. The `hold()` method is no longer used by external blocks. The `getEntityCapability()` helper may still be useful for the dock block entity to resolve vehicle capabilities. Keep the static helpers, remove the `hold()` method and `Mode` enum if no longer referenced.
- Any `import` statements in files already modified that reference deleted classes.

**Step 1: Delete old files**

Delete all files listed above.

**Step 2: Fix compilation errors**

Run `./gradlew build` and fix any remaining references. Common issues:
- Stale imports in modified files
- References in files not yet updated
- `IVesselLoader` cleanup

**Step 3: Verify clean build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Run data generators**

Run: `./gradlew runData`
Expected: Successful — old generated resources for deleted blocks should be cleaned up.

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove old dock, hopper, charger, and rapid hopper code"
```

---

## Task 11: Visual Feedback & Tooltip Overlay

Add the Create-style tooltip overlay when looking at a dock block, showing timeout and redstone settings.

**Files:**
- Create: `src/main/java/dev/murad/shipping/event/DockOverlayRenderer.java` (or similar client-side class)
- Modify: `src/main/java/dev/murad/shipping/event/ModClientEventHandler.java`

**Step 1: Create overlay renderer**

Register a `RenderGuiLayerEvent.Post` (or `RenderGuiOverlayEvent.Post`) handler. Each frame:
1. Raycast from player's eye to find the block they're looking at
2. If it's a `DockBlock` or `DockRail`, render a small HUD overlay showing:
   - Current idle timeout (e.g., "5s")
   - Current redstone mode (icon or text)
   - Scroll hint (e.g., "Sneak + Scroll to adjust")
3. Style it similar to Create mod's value box — small, unobtrusive, near crosshair

**Step 2: Register the overlay**

In `ModClientEventHandler`, register the overlay renderer on the NeoForge event bus.

**Step 3: Add visual dock indicator**

When a vehicle is docked at a dock block, show particles or a small visual cue. This can be done via `DockBlockEntity` sending a particle spawn on dock/undock, or a simple block state change.

**Step 4: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/DockOverlayRenderer.java \
        src/main/java/dev/murad/shipping/event/ModClientEventHandler.java
git commit -m "feat: add Create-style dock configuration overlay"
```

---

## Task 12: Integration Testing

Manual in-game testing to verify the full system works end-to-end.

**Step 1: Launch the client**

Run: `./gradlew runClient`

**Step 2: Test rail docking**

1. Place 3 DockRails in a line
2. Connect a chest to one rail via a hopper (or pipe mod if available)
3. Place a locomotive + chest car on the track approaching from the back
4. Verify: locomotive passes through to the front dock
5. Verify: chest car settles on the second dock
6. Verify: items transfer from the hopper/pipe into the chest car
7. Verify: after idle timeout, train resumes

**Step 3: Test water docking**

1. Place 3 DockBlocks along a canal
2. Connect storage to the docks
3. Send a tug + barges along a route that passes the docks
4. Verify same behavior as rail

**Step 4: Test configuration**

1. Sneak + scroll on a dock to adjust timeout — verify overlay shows change
2. Right-click interaction zone to cycle redstone mode — verify overlay shows change
3. Apply redstone signal — verify hold/disable behavior

**Step 5: Test edge cases**

1. Short train (fewer vehicles than docks) — unused docks should be inert
2. No pipe connected — vehicle should dock, idle timeout, then leave
3. Multiple capability types on one dock (energy loco with item slot)
4. Breaking a dock block while a vehicle is docked

**Step 6: Commit any fixes**

```bash
git add -A
git commit -m "fix: integration testing fixes for universal dock system"
```

---

## Summary of Tasks

| Task | Description | Dependencies |
|------|-------------|--------------|
| 1 | Register missing barge capabilities | None |
| 2 | DockBlockEntity core logic | None |
| 3 | Capability wrapper classes | Task 2 |
| 4 | DockBlock (water) | Task 2 |
| 5 | DockRail | Task 2 |
| 6 | Registration & capability wiring | Tasks 2-5 |
| 7 | Data generators | Task 6 |
| 8 | Rewire vehicle docking logic | Tasks 4-6 |
| 9 | Scroll interaction networking | Tasks 4-5, 8 |
| 10 | Delete old code | Tasks 6-9 |
| 11 | Visual feedback & overlay | Task 9 |
| 12 | Integration testing | All |

Tasks 1, 2, 3, 4, 5 can be parallelized. Tasks 6-7 depend on 2-5. Task 8 depends on 4-6. Tasks 10-12 are sequential cleanup and verification.
