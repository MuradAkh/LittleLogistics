# Dock Rail Dynamic Panels — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add dynamic visual panels to dock rails on sides where adjacent blocks expose item/fluid/energy capabilities.

**Architecture:** Add 6 boolean block state properties to `DockRail`. On neighbor change, check if the adjacent block exposes a capability and update the property. Use multipart block state definition with conditional panel model elements. Placeholder texture: `minecraft:block/deepslate`.

**Tech Stack:** NeoForge 1.21.1, Java 21, NeoForge data generators

---

### Task 1: Add boolean block state properties to DockRail

**Files:**
- Modify: `src/main/java/dev/murad/shipping/block/dock/DockRail.java`

**Step 1: Add the 6 BooleanProperty fields and register them**

In `DockRail.java`, add these imports and properties:

```java
import net.minecraft.world.level.block.state.properties.BooleanProperty;
```

Add static fields:

```java
public static final BooleanProperty NORTH = BooleanProperty.create("north");
public static final BooleanProperty SOUTH = BooleanProperty.create("south");
public static final BooleanProperty EAST = BooleanProperty.create("east");
public static final BooleanProperty WEST = BooleanProperty.create("west");
public static final BooleanProperty UP = BooleanProperty.create("up");
public static final BooleanProperty DOWN = BooleanProperty.create("down");
```

In `createBlockStateDefinition`, add all 6 to the builder:

```java
builder.add(WATERLOGGED, RAIL_SHAPE, NORTH, SOUTH, EAST, WEST, UP, DOWN);
```

In the constructor's `registerDefaultState`, set all 6 to false:

```java
this.registerDefaultState(this.stateDefinition.any()
        .setValue(RAIL_SHAPE, RailShape.NORTH_SOUTH)
        .setValue(WATERLOGGED, false)
        .setValue(NORTH, false)
        .setValue(SOUTH, false)
        .setValue(EAST, false)
        .setValue(WEST, false)
        .setValue(UP, false)
        .setValue(DOWN, false));
```

**Step 2: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (no runtime test yet — just checking compilation)

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockRail.java
git commit -m "feat: add boolean block state properties for dock rail panels"
```

---

### Task 2: Add neighbor capability detection logic

**Files:**
- Modify: `src/main/java/dev/murad/shipping/block/dock/DockRail.java`

**Step 1: Add helper method to check if a direction is perpendicular to the rail**

```java
private static boolean isEligibleSide(Direction direction, RailShape shape) {
    if (direction == Direction.UP || direction == Direction.DOWN) {
        return true;
    }
    if (shape == RailShape.NORTH_SOUTH) {
        return direction == Direction.EAST || direction == Direction.WEST;
    }
    if (shape == RailShape.EAST_WEST) {
        return direction == Direction.NORTH || direction == Direction.SOUTH;
    }
    return false;
}
```

**Step 2: Add helper method to check if a neighbor exposes a capability**

Add imports:

```java
import net.neoforged.neoforge.capabilities.Capabilities;
```

Add method:

```java
private static boolean hasCapability(Level level, BlockPos neighborPos, Direction queryDirection) {
    // queryDirection is the direction FROM the neighbor TOWARD the dock rail
    if (level.getBlockEntity(neighborPos) == null) return false;
    if (level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, queryDirection) != null) return true;
    if (level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, queryDirection) != null) return true;
    if (level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, queryDirection) != null) return true;
    return false;
}
```

**Step 3: Add method to compute the updated block state from neighbors**

```java
private static BooleanProperty propertyForDirection(Direction dir) {
    return switch (dir) {
        case NORTH -> NORTH;
        case SOUTH -> SOUTH;
        case EAST -> EAST;
        case WEST -> WEST;
        case UP -> UP;
        case DOWN -> DOWN;
    };
}

private static BlockState updatePanelState(BlockState state, Level level, BlockPos pos) {
    RailShape shape = state.getValue(RAIL_SHAPE);
    for (Direction dir : Direction.values()) {
        BooleanProperty prop = propertyForDirection(dir);
        if (isEligibleSide(dir, shape)) {
            BlockPos neighborPos = pos.relative(dir);
            boolean connected = hasCapability(level, neighborPos, dir.getOpposite());
            state = state.setValue(prop, connected);
        } else {
            state = state.setValue(prop, false);
        }
    }
    return state;
}
```

**Step 4: Override `neighborChanged` and `onPlace`**

```java
@Override
protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
    super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    if (!level.isClientSide) {
        BlockState updated = updatePanelState(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }
    }
}

@Override
protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    super.onPlace(state, level, pos, oldState, movedByPiston);
    if (!level.isClientSide) {
        BlockState updated = updatePanelState(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }
    }
}
```

**Step 5: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/dev/murad/shipping/block/dock/DockRail.java
git commit -m "feat: add neighbor capability detection for dock rail panels"
```

---

### Task 3: Create panel models and switch to multipart block state

**Files:**
- Modify: `src/main/java/dev/murad/shipping/data/client/ModBlockStateProvider.java`

**Step 1: Replace the dock rail variant builder with a multipart builder**

Remove the existing `getVariantBuilder(ModBlocks.DOCK_RAIL.get())...` block (lines 144-150) and replace with the multipart definition.

The base rail model (always shown) uses two conditions for the two rail shapes, since multipart `apply` can include rotation. Each panel gets its own conditional part.

```java
// --- Dock Rail (multipart) ---
ResourceLocation deepslate = ResourceLocation.withDefaultNamespace("block/deepslate");

// Base rail model (always present, rotated by shape)
ModelFile dockRailModel = models()
        .withExistingParent("dock_rail", mcLoc("rail_flat"))
        .texture("rail", getBlTx("dock_rail"));

getMultipartBuilder(ModBlocks.DOCK_RAIL.get())
        // Base: north_south
        .part().modelFile(dockRailModel).addModel()
            .condition(DockRail.RAIL_SHAPE, RailShape.NORTH_SOUTH).end()
        // Base: east_west (rotated 90)
        .part().modelFile(dockRailModel).rotationY(90).addModel()
            .condition(DockRail.RAIL_SHAPE, RailShape.EAST_WEST).end()
        // North panel
        .part().modelFile(models().getBuilder("dock_rail_panel_north")
            .texture("panel", deepslate)
            .element().from(0, 0, -1).to(16, 16, 1)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.NORTH, true).end()
        // South panel
        .part().modelFile(models().getBuilder("dock_rail_panel_south")
            .texture("panel", deepslate)
            .element().from(0, 0, 15).to(16, 16, 17)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.SOUTH, true).end()
        // East panel
        .part().modelFile(models().getBuilder("dock_rail_panel_east")
            .texture("panel", deepslate)
            .element().from(15, 0, 0).to(17, 16, 16)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.EAST, true).end()
        // West panel
        .part().modelFile(models().getBuilder("dock_rail_panel_west")
            .texture("panel", deepslate)
            .element().from(-1, 0, 0).to(1, 16, 16)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.WEST, true).end()
        // Top panel (tunnel ceiling)
        .part().modelFile(models().getBuilder("dock_rail_panel_top")
            .texture("panel", deepslate)
            .element().from(0, 15, 0).to(16, 17, 16)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.UP, true).end()
        // Bottom panel (opaque floor)
        .part().modelFile(models().getBuilder("dock_rail_panel_bottom")
            .texture("panel", deepslate)
            .element().from(0, 0, 0).to(16, 2, 16)
                .allFaces((dir, f) -> f.texture("#panel")).end())
            .addModel()
            .condition(DockRail.DOWN, true).end();
```

Add import at the top of the file:

```java
import net.minecraft.resources.ResourceLocation;  // already present via getBlTx
```

**Step 2: Run data generators**

Run: `./gradlew runData`
Expected: BUILD SUCCESSFUL, generates updated `blockstates/dock_rail.json` with multipart format and new panel model JSONs in `models/block/`

**Step 3: Verify the generated blockstate JSON has multipart format**

Read `src/generated/resources/assets/littlelogistics/blockstates/dock_rail.json` — should contain `"multipart"` array instead of `"variants"`.

**Step 4: Verify it compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/dev/murad/shipping/data/client/ModBlockStateProvider.java
git add src/generated/resources/
git commit -m "feat: add multipart dock rail model with conditional panel elements"
```

---

### Task 4: Smoke test in-game

**Step 1: Launch the client**

Run: `./gradlew runClient`

**Step 2: Test the dock rail panel behavior**

1. Place a dock rail (north-south orientation)
2. Place a chest or hopper on the east side — east panel should appear
3. Place a chest on the west side — west panel should also appear
4. Place a block on top — top panel (tunnel) should appear
5. Place a hopper below — bottom panel should appear
6. Break the east chest — east panel should disappear
7. Place a dock rail in east-west orientation — verify north/south panels work instead
8. Verify along-rail sides never show panels (e.g. placing a chest to the north of a N-S rail should NOT show a panel)

**Step 3: Commit any fixes if needed**
