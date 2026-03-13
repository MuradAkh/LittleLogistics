# Phase 6: Data Components (Item NBT to Codecs)

**Phase:** 6 of 10
**Prerequisite:** Phase 5 (Entity Data & BlockEntity Serialization) complete
**Effort:** HIGH
**Files touched:** 8 (4 new/modified data classes, 3 item files, 1 new registration file)

---

## Overview

In NeoForge 1.21.1, item NBT data (`stack.getTag()` / `stack.setTag()`) is replaced by **Data Components** — typed, codec-backed data attached to ItemStacks. This phase migrates all item NBT usage in LittleLogistics to the new system.

Three items use raw NBT:
1. **TugRouteItem** — stores a `TugRoute` (ordered list of water waypoints)
2. **LocoRouteItem** — stores a `LocoRoute` (unordered set of rail waypoints)
3. **SpringItem** — stores a transient entity ID for two-click linking

Each route class needs a `Codec` and `StreamCodec` for serialization. A new `ModDataComponents` registration class binds them to `DataComponentType` entries.

---

## Step 1: Add Codecs to Route Node Classes

### TugRouteNode.java

**Current NBT:** `toNBT()` / `fromNBT()` using nested CompoundTags with `"coordinates"` sub-tag.

**Add:**
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public class TugRouteNode {
    public static final Codec<TugRouteNode> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(n -> Optional.ofNullable(n.getName())),
            Codec.DOUBLE.fieldOf("x")
                .forGetter(TugRouteNode::getX),
            Codec.DOUBLE.fieldOf("z")
                .forGetter(TugRouteNode::getZ)
        ).apply(instance, (name, x, z) -> new TugRouteNode(name.orElse(null), x, z))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRouteNode> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), n -> Optional.ofNullable(n.getName()),
            ByteBufCodecs.DOUBLE, TugRouteNode::getX,
            ByteBufCodecs.DOUBLE, TugRouteNode::getZ,
            (name, x, z) -> new TugRouteNode(name.orElse(null), x, z)
        );

    // ... existing code unchanged ...
}
```

**Keep:** `toNBT()` / `fromNBT()` methods — still used by entity `addAdditionalSaveData` / `readAdditionalSaveData` (entity serialization does NOT use Data Components; those signatures don't change in 1.21.x).

### LocoRouteNode.java

**Current NBT:** `toNBT()` / `fromNBT()` using nested CompoundTags. Note: `fromNBT` reads `getInt()` (was previously `getDouble()` — already had a breaking change).

**Add:**
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public class LocoRouteNode {
    public static final Codec<LocoRouteNode> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(n -> Optional.ofNullable(n.getName())),
            Codec.INT.fieldOf("x")
                .forGetter(LocoRouteNode::getX),
            Codec.INT.fieldOf("y")
                .forGetter(LocoRouteNode::getY),
            Codec.INT.fieldOf("z")
                .forGetter(LocoRouteNode::getZ)
        ).apply(instance, (name, x, y, z) -> new LocoRouteNode(name.orElse(null), x, y, z))
    );

    public static final StreamCodec<FriendlyByteBuf, LocoRouteNode> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), n -> Optional.ofNullable(n.getName()),
            ByteBufCodecs.INT, LocoRouteNode::getX,
            ByteBufCodecs.INT, LocoRouteNode::getY,
            ByteBufCodecs.INT, LocoRouteNode::getZ,
            (name, x, y, z) -> new LocoRouteNode(name.orElse(null), x, y, z)
        );

    // ... existing code unchanged ...
}
```

---

## Step 2: Add Codecs to Route Classes

### TugRoute.java

**Current:** Extends `ArrayList<TugRouteNode>`. Has `name` field, `toNBT()` / `fromNBT()`.

**Add:**
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public class TugRoute extends ArrayList<TugRouteNode> {
    public static final Codec<TugRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(r -> Optional.ofNullable(r.name)),
            TugRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(r -> List.copyOf(r))
        ).apply(instance, (name, nodes) -> new TugRoute(name.orElse(null), nodes))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.name),
            TugRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> List.copyOf(r),
            (name, nodes) -> new TugRoute(name.orElse(null), nodes)
        );

    // ... existing code unchanged ...
}
```

**Note:** `TugRoute.equals()` and `hashCode()` already exist and include both the list contents and `name` — these are required for Data Components to detect changes correctly.

### LocoRoute.java

**Current:** Extends `HashSet<LocoRouteNode>`. Has `name` and `owner` fields.

**Add:**
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public class LocoRoute extends HashSet<LocoRouteNode> {
    public static final Codec<LocoRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(r -> Optional.ofNullable(r.name)),
            Codec.STRING.optionalFieldOf("owner")
                .forGetter(r -> Optional.ofNullable(r.owner)),
            LocoRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(r -> List.copyOf(r))
        ).apply(instance, (name, owner, nodes) ->
            new LocoRoute(name.orElse(null), owner.orElse(null), new HashSet<>(nodes)))
    );

    public static final StreamCodec<FriendlyByteBuf, LocoRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.name),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.owner),
            LocoRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> List.copyOf(r),
            (name, owner, nodes) ->
                new LocoRoute(name.orElse(null), owner.orElse(null), new HashSet<>(nodes))
        );

    // ... existing code unchanged ...
}
```

**Design note:** LocoRoute serializes as a list (via Codec) but deserializes into a HashSet. Duplicate nodes in serialized form will silently collapse. This matches existing NBT behavior.

---

## Step 3: Create ModDataComponents.java

**New file:** `src/main/java/dev/murad/shipping/setup/ModDataComponents.java`

```java
package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.TugRoute;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ShippingMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TugRoute>> TUG_ROUTE =
        COMPONENTS.register("tug_route", () ->
            DataComponentType.<TugRoute>builder()
                .persistent(TugRoute.CODEC)
                .networkSynchronized(TugRoute.STREAM_CODEC)
                .build()
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LocoRoute>> LOCO_ROUTE =
        COMPONENTS.register("loco_route", () ->
            DataComponentType.<LocoRoute>builder()
                .persistent(LocoRoute.CODEC)
                .networkSynchronized(LocoRoute.STREAM_CODEC)
                .build()
        );

    // SpringItem "linked" entity ID: transient runtime-only data.
    // NOT persistent (entity IDs don't survive world reload).
    // Network-synced only so client can render state.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SPRING_LINKED =
        COMPONENTS.register("spring_linked", () ->
            DataComponentType.<Integer>builder()
                .networkSynchronized(ByteBufCodecs.INT)
                .build()
        );
}
```

**Register in Registration.java** (or ShippingMod.java — wherever DeferredRegisters are collected):
```java
ModDataComponents.COMPONENTS.register(eventBus);
```

---

## Step 4: Migrate TugRouteItem.java

**Current code and replacements:**

### 4a. `getTag()` helper (line 153-155)
```java
// DELETE:
@Nullable
private static CompoundTag getTag(ItemStack stack) {
    return stack.getTag();
}
```

### 4b. `getRoute()` (line 106-114)
```java
// OLD:
public static TugRoute getRoute(ItemStack itemStack) {
    CompoundTag nbt = getTag(itemStack);
    if (nbt == null || !nbt.contains(ROUTE_NBT, 10)) {
        return new TugRoute();
    }
    return TugRoute.fromNBT(nbt.getCompound(ROUTE_NBT));
}

// NEW:
public static TugRoute getRoute(ItemStack itemStack) {
    TugRoute route = itemStack.get(ModDataComponents.TUG_ROUTE);
    return route != null ? route : new TugRoute();
}
```

### 4c. `saveRoute()` (line 143-150)
```java
// OLD:
public static void saveRoute(TugRoute route, ItemStack itemStack) {
    CompoundTag nbt = getTag(itemStack);
    if (nbt == null) {
        nbt = new CompoundTag();
        itemStack.setTag(nbt);
    }
    nbt.put(ROUTE_NBT, route.toNBT());
}

// NEW:
public static void saveRoute(TugRoute route, ItemStack itemStack) {
    if (route.isEmpty()) {
        itemStack.remove(ModDataComponents.TUG_ROUTE);
    } else {
        itemStack.set(ModDataComponents.TUG_ROUTE, route);
    }
}
```

### 4d. `verifyTagAfterLoad()` (line 83-95)
```java
// DELETE ENTIRELY.
// Legacy string→compound migration is no longer needed.
// NeoForge 1.21.1 uses Data Components — old NBT format items
// will lose their route data on world upgrade regardless.
// Players upgrading from very old versions must go through
// an intermediate 1.20.1 version first to trigger this migration.
```

### 4e. `ROUTE_NBT` constant (line 36)
```java
// DELETE: private static final String ROUTE_NBT = "route";
// No longer needed — the DataComponentType IS the key.
```

### 4f. Callers (no changes needed)
All external callers use `TugRouteItem.getRoute(stack)` and `TugRouteItem.saveRoute(route, stack)`. These are the public API — their signatures don't change, so all 10+ call sites are unaffected:
- `AbstractTugEntity.java:184` — `TugRouteItem.getRoute(stack)`
- `TugRouteScreen.java:68` — `TugRouteItem.getRoute(stack)`
- `TugRoutePacketHandler.java:55` — `TugRouteItem.saveRoute(...)`
- `ForgeClientEventHandler.java:145` — `TugRouteItem.getRoute(stack)`
- `ModRecipeSerializers.java:18` — `TugRouteItem.getRoute(stack)`
- `ModItemModelProperties.java:19` — `TugRouteItem.getRoute(stack)`

### 4g. `appendHoverText()` signature change (line 98)
```java
// OLD (1.20.1):
public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)

// NEW (1.21.1):
public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn)
```
This is a vanilla signature change in 1.21.1. The method body doesn't reference `worldIn` so the change is trivial.

---

## Step 5: Migrate LocoRouteItem.java

### 5a. `getRoute()` (line 93-100)
```java
// OLD:
public static LocoRoute getRoute(ItemStack stack) {
    if (stack.getTag() != null) {
        return LocoRoute.fromNBT(stack.getTag().getCompound(ROUTE_NBT));
    }
    return new LocoRoute();
}

// NEW:
public static LocoRoute getRoute(ItemStack stack) {
    LocoRoute route = stack.get(ModDataComponents.LOCO_ROUTE);
    return route != null ? route : new LocoRoute();
}
```

### 5b. `saveRoute()` (line 83-91)
```java
// OLD:
private void saveRoute(ItemStack stack, LocoRoute route) {
    if (route.isEmpty()) {
        stack.setTag(null);  // clears ALL tags
    }
    CompoundTag tag = stack.getOrCreateTag();
    tag.put(ROUTE_NBT, route.toNBT());
}

// NEW:
private void saveRoute(ItemStack stack, LocoRoute route) {
    if (route.isEmpty()) {
        stack.remove(ModDataComponents.LOCO_ROUTE);
    } else {
        stack.set(ModDataComponents.LOCO_ROUTE, route);
    }
}
```

**Bug fix:** The old code has a logic issue — when `route.isEmpty()`, it sets tag to null but then immediately calls `getOrCreateTag()` and writes the empty route anyway. The new code properly short-circuits.

### 5c. `ROUTE_NBT` constant (line 28)
```java
// DELETE: private static final String ROUTE_NBT = "route";
```

### 5d. `appendHoverText()` — same signature change as TugRouteItem (see 4g).

### 5e. Callers (no changes needed)
All external callers use `LocoRouteItem.getRoute(stack)`:
- `AbstractLocomotiveEntity.java:552`
- `ForgeClientEventHandler.java:85`
- `ModRecipeSerializers.java:32`
- `ModItemModelProperties.java:23`

---

## Step 6: Migrate SpringItem.java (Gap L-9)

### Decision: Keep as DataComponent (transient, network-only)

**Rationale:** The "linked" entity ID is runtime-only (entity IDs don't survive world reload). However, it must be stored per-ItemStack (a player could have multiple spring items). A DataComponent with `networkSynchronized()` but WITHOUT `persistent()` is the cleanest approach — it syncs to client for rendering the "waiting" state but doesn't pollute saved data.

### 6a. `setDominant()` (line 95-97)
```java
// OLD:
private void setDominant(Level worldIn, ItemStack stack, Entity entity) {
    stack.getOrCreateTag().putInt("linked", entity.getId());
}

// NEW:
private void setDominant(Level worldIn, ItemStack stack, Entity entity) {
    stack.set(ModDataComponents.SPRING_LINKED, entity.getId());
}
```

### 6b. `getDominant()` (line 99-107)
```java
// OLD:
@Nullable
private Entity getDominant(Level worldIn, ItemStack stack) {
    if (stack.getTag() != null && stack.getTag().contains("linked")) {
        int id = stack.getTag().getInt("linked");
        return worldIn.getEntity(id);
    }
    resetLinked(stack);
    return null;
}

// NEW:
@Nullable
private Entity getDominant(Level worldIn, ItemStack stack) {
    Integer id = stack.get(ModDataComponents.SPRING_LINKED);
    if (id != null) {
        return worldIn.getEntity(id);
    }
    return null;
}
```

### 6c. `resetLinked()` (line 109-111)
```java
// OLD:
private void resetLinked(ItemStack itemstack) {
    itemstack.removeTagKey("linked");
}

// NEW:
private void resetLinked(ItemStack itemstack) {
    itemstack.remove(ModDataComponents.SPRING_LINKED);
}
```

### 6d. `getState()` (line 119-123)
```java
// OLD:
public static State getState(ItemStack stack) {
    if (stack.getTag() != null && stack.getTag().contains("linked"))
        return State.WAITING_NEXT;
    return State.READY;
}

// NEW:
public static State getState(ItemStack stack) {
    return stack.has(ModDataComponents.SPRING_LINKED) ? State.WAITING_NEXT : State.READY;
}
```

### 6e. `appendHoverText()` — same signature change as TugRouteItem (see 4g).

---

## Step 7: AbstractRouteCopyRecipe.java — No Direct Changes

`AbstractRouteCopyRecipe.assemble()` copies routes via `filled.copy()` (line 95). In NeoForge 1.21.1, `ItemStack.copy()` copies all Data Components automatically. No changes needed for the copy logic.

**However**, this file has a separate Phase 7 change: `assemble(CraftingContainer, RegistryAccess)` signature becomes `assemble(CraftingContainer, HolderLookup.Provider)`. That is NOT part of Phase 6.

---

## Step 8: Legacy Migration Strategy

### What can be dropped

1. **`TugRouteItem.verifyTagAfterLoad()`** — DELETE entirely. This handled legacy string-format routes from very old versions. In NeoForge 1.21.1, `verifyTagAfterLoad` itself is removed from the Item API.

2. **`TugRoute.fromNBT()` / `TugRoute.toNBT()`** — KEEP. These are still used by entity serialization (`AbstractTugEntity.addAdditionalSaveData` / `readAdditionalSaveData`). Entity save data does NOT use Data Components.

3. **`LocoRoute.fromNBT()` / `LocoRoute.toNBT()`** — KEEP for the same reason (entity serialization).

4. **`TugRouteNode.toNBT()` / `fromNBT()` and `LocoRouteNode.toNBT()` / `fromNBT()`** — KEEP for entity serialization.

### World upgrade path

When a world saved in 1.20.1 is loaded in 1.21.1:
- **Route items in inventories/chests:** Old NBT tags are discarded by vanilla's upgrade process. Routes stored in items will be lost. This is acceptable — routes are easily re-created in-game.
- **Routes stored on entities** (in `addAdditionalSaveData`): These use CompoundTag directly, not Data Components. They survive the upgrade unchanged.
- **Alternative:** If route preservation is critical, a DataFixer could be written to convert old item NBT to the new DataComponent format. This is optional and can be deferred.

### LegacyTugRouteUtil

`LegacyTugRouteUtil` (referenced by `TugRouteItem.verifyTagAfterLoad`) can be deleted entirely since `verifyTagAfterLoad` is removed. If there's concern about preserving the migration path, it can be kept but marked `@Deprecated`.

---

## Step 9: TugRoutePacketHandler Migration

`TugRoutePacketHandler.java:55` currently does:
```java
TugRouteItem.saveRoute(TugRoute.fromNBT(routeTag), heldStack);
```

In Phase 3 (Networking), this handler is rewritten as a `CustomPacketPayload` record. The packet will carry a `TugRoute` object directly (using `TugRoute.STREAM_CODEC`), so the handler becomes:
```java
TugRouteItem.saveRoute(payload.route(), heldStack);
```

This is a Phase 3 concern, but Phase 6 enables it by providing the `STREAM_CODEC`.

---

## Verification Steps

### Unit Tests
1. **Codec round-trip tests** for each type:
   - `TugRouteNode`: encode → decode with and without name, negative coords
   - `TugRoute`: empty route, single node, multiple nodes, with/without name
   - `LocoRouteNode`: encode → decode with and without name, negative coords
   - `LocoRoute`: empty route, single node, multiple nodes, with/without name and owner
   - `LocoRoute`: verify duplicate nodes in serialized list collapse correctly on decode

2. **StreamCodec round-trip tests**: Same cases using `RegistryFriendlyByteBuf` or `FriendlyByteBuf`.

3. **Existing NBT tests still pass**: The existing `TugRouteTest` (if present) should still pass since `toNBT()`/`fromNBT()` are preserved.

### Integration Verification
4. **Build:** `./gradlew build` must pass.
5. **In-game test:**
   - Create a TugRoute item, add waypoints, verify they persist across save/load
   - Create a LocoRoute item, add rail waypoints, verify persistence
   - Use SpringItem to link two vehicles, verify two-click workflow
   - Copy a filled route in crafting grid, verify copy has same route
   - Use route in a tug/locomotive, verify pathfinding works

---

## Complete File Inventory

| File | Action | Changes |
|------|--------|---------|
| `src/main/java/dev/murad/shipping/util/TugRouteNode.java` | MODIFY | Add `CODEC` + `STREAM_CODEC` static fields |
| `src/main/java/dev/murad/shipping/util/TugRoute.java` | MODIFY | Add `CODEC` + `STREAM_CODEC` static fields |
| `src/main/java/dev/murad/shipping/util/LocoRouteNode.java` | MODIFY | Add `CODEC` + `STREAM_CODEC` static fields |
| `src/main/java/dev/murad/shipping/util/LocoRoute.java` | MODIFY | Add `CODEC` + `STREAM_CODEC` static fields |
| `src/main/java/dev/murad/shipping/setup/ModDataComponents.java` | CREATE | `DeferredRegister<DataComponentType<?>>` with 3 entries |
| `src/main/java/dev/murad/shipping/item/TugRouteItem.java` | MODIFY | Replace all `getTag()`/`setTag()` with `get()`/`set()` DataComponent calls; delete `verifyTagAfterLoad()`, `getTag()` helper, `ROUTE_NBT` constant |
| `src/main/java/dev/murad/shipping/item/LocoRouteItem.java` | MODIFY | Replace all `getTag()`/`setTag(null)`/`getOrCreateTag()` with DataComponent calls; delete `ROUTE_NBT` constant |
| `src/main/java/dev/murad/shipping/item/SpringItem.java` | MODIFY | Replace all `getOrCreateTag()`/`getTag()`/`removeTagKey()` with DataComponent calls |
| `src/main/java/dev/murad/shipping/setup/Registration.java` | MODIFY | Add `ModDataComponents.COMPONENTS.register(eventBus)` |
| `src/main/java/dev/murad/shipping/util/LegacyTugRouteUtil.java` | DELETE | No longer referenced after `verifyTagAfterLoad` removal |

**Files NOT changed (callers that use the public API):** AbstractTugEntity, AbstractLocomotiveEntity, EnergyLocomotiveEntity, SteamLocomotiveEntity, TugRouteScreen, TugRoutePacketHandler (Phase 3), ForgeClientEventHandler, ModRecipeSerializers, ModItemModelProperties, AbstractHeadVehicleContainer, AbstractHeadVehicleScreen, HeadVehicle, AbstractRouteCopyRecipe (Phase 7).

---

## Risks and Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| Route data loss on world upgrade | MEDIUM | Document in changelog; routes are easy to recreate. Optional DataFixer can be added later. |
| `LocoRoute` extends `HashSet` — mutable after `set()` | LOW | Data Components should ideally be immutable. Current code mutates routes in-place then calls `saveRoute()`. This pattern still works because `set()` replaces the component entirely. |
| `TugRoute` extends `ArrayList` — same mutability concern | LOW | Same mitigation as LocoRoute. |
| `StreamCodec` for `Optional<String>` may not exist | LOW | Use `ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)` — verify this helper exists in NeoForge 1.21.1. Fallback: write a custom StreamCodec that writes a boolean flag + string. |
| `appendHoverText` signature change | LOW | Vanilla change in 1.21.1 — must update in all 3 item files. Trivial rename of parameter. |
