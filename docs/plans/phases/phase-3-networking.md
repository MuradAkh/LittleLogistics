# Phase 3: Networking Migration Plan

## Overview

Migrate from Forge's `SimpleChannel` networking to NeoForge's `RegisterPayloadHandlersEvent` + `CustomPacketPayload` records.

**Core changes:**
- 3 `SimpleChannel` handler classes → 1 unified `NetworkHandler.java` using `RegisterPayloadHandlersEvent`
- 4 packet classes (mutable classes with Lombok) → 4 `CustomPacketPayload` records with `StreamCodec`
- `INSTANCE.sendToServer()` → `PacketDistributor.sendToServer(payload)`
- `INSTANCE.send(PacketDistributor.PLAYER.with(...))` → `PacketDistributor.sendToPlayer(player, payload)`
- Handler signature: `(MSG, Supplier<NetworkEvent.Context>)` → `(Payload, IPayloadContext)`
- No more `ctx.get().setPacketHandled(true)` — handled automatically in NeoForge

## Prerequisites

- Phase 2 (Mod Entrypoint & Registration) must be complete
- `Registration.java` must already accept `IEventBus` parameter (Phase 2 change)

---

## Step 1: Rewrite Packet Classes as CustomPacketPayload Records

### 1a. SetEnginePacket

**Current code** (`src/main/java/dev/murad/shipping/network/SetEnginePacket.java`):
```java
@RequiredArgsConstructor
public class SetEnginePacket {
    public final int locoId;
    public final boolean state;

    public SetEnginePacket(FriendlyByteBuf buffer) {
        this.locoId = buffer.readInt();
        this.state = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(locoId);
        buf.writeBoolean(state);
    }
}
```

**Target code:**
```java
package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetEnginePacket(int locoId, boolean state) implements CustomPacketPayload {

    public static final Type<SetEnginePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_engine"));

    public static final StreamCodec<FriendlyByteBuf, SetEnginePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SetEnginePacket::locoId,
                    ByteBufCodecs.BOOL, SetEnginePacket::state,
                    SetEnginePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### 1b. EnrollVehiclePacket

**Current code** (`src/main/java/dev/murad/shipping/network/EnrollVehiclePacket.java`):
```java
@RequiredArgsConstructor
public class EnrollVehiclePacket {
    public final int locoId;

    public EnrollVehiclePacket(FriendlyByteBuf buffer) {
        this.locoId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(locoId);
    }
}
```

**Target code:**
```java
package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnrollVehiclePacket(int locoId) implements CustomPacketPayload {

    public static final Type<EnrollVehiclePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "enroll_vehicle"));

    public static final StreamCodec<FriendlyByteBuf, EnrollVehiclePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, EnrollVehiclePacket::locoId,
                    EnrollVehiclePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### 1c. SetRouteTagPacket

**Current code** (`src/main/java/dev/murad/shipping/network/SetRouteTagPacket.java`):
```java
@RequiredArgsConstructor
public class SetRouteTagPacket {
    public final int routeChecksum;
    public final boolean isOffhand;
    public final CompoundTag tag;

    public SetRouteTagPacket(FriendlyByteBuf buffer) {
        this.routeChecksum = buffer.readInt();
        this.isOffhand = buffer.readBoolean();
        this.tag = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(routeChecksum);
        buf.writeBoolean(isOffhand);
        buf.writeNbt(tag);
    }
}
```

**Target code:**
```java
package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetRouteTagPacket(int routeChecksum, boolean isOffhand, CompoundTag tag)
        implements CustomPacketPayload {

    public static final Type<SetRouteTagPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "set_route_tag"));

    public static final StreamCodec<FriendlyByteBuf, SetRouteTagPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SetRouteTagPacket::routeChecksum,
                    ByteBufCodecs.BOOL, SetRouteTagPacket::isOffhand,
                    ByteBufCodecs.COMPOUND_TAG, SetRouteTagPacket::tag,
                    SetRouteTagPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

### 1d. VehicleTrackerClientPacket

**Current code** (`src/main/java/dev/murad/shipping/network/client/VehicleTrackerClientPacket.java`):
```java
@RequiredArgsConstructor
public class VehicleTrackerClientPacket {
    public final CompoundTag tag;
    public final String dimension;

    public VehicleTrackerClientPacket(FriendlyByteBuf buffer) {
        this.tag = buffer.readNbt();
        this.dimension = new String(buffer.readByteArray());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
        buf.writeByteArray(dimension.getBytes());
    }

    public static VehicleTrackerClientPacket of(List<EntityPosition> types, String dimension) { ... }
    public List<EntityPosition> parse() { ... }
}
```

**Target code:**
```java
package dev.murad.shipping.network.client;

import dev.murad.shipping.ShippingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

public record VehicleTrackerClientPacket(CompoundTag tag, String dimension)
        implements CustomPacketPayload {

    public static final Type<VehicleTrackerClientPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "vehicle_tracker"));

    public static final StreamCodec<FriendlyByteBuf, VehicleTrackerClientPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, VehicleTrackerClientPacket::tag,
                    ByteBufCodecs.STRING_UTF8, VehicleTrackerClientPacket::dimension,
                    VehicleTrackerClientPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Keep existing factory and parse methods as static/instance methods on the record
    public static VehicleTrackerClientPacket of(List<EntityPosition> types, String dimension) {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        for (EntityPosition position : types) {
            var coords = new CompoundTag();
            coords.putDouble("x", position.pos().x);
            coords.putDouble("y", position.pos().y);
            coords.putDouble("z", position.pos().z);
            coords.putDouble("xo", position.oldPos().x);
            coords.putDouble("yo", position.oldPos().y);
            coords.putDouble("zo", position.oldPos().z);
            coords.putString("type", position.type());
            coords.putInt("eid", position.id());
            tag.put(String.valueOf(i++), coords);
        }
        return new VehicleTrackerClientPacket(tag, dimension);
    }

    public List<EntityPosition> parse() {
        return tag.getAllKeys().stream().map(key -> {
            CompoundTag coords = tag.getCompound(key);
            return new EntityPosition(
                    coords.getString("type"),
                    coords.getInt("eid"),
                    new Vec3(coords.getDouble("x"), coords.getDouble("y"), coords.getDouble("z")),
                    new Vec3(coords.getDouble("xo"), coords.getDouble("yo"), coords.getDouble("zo"))
            );
        }).collect(Collectors.toList());
    }
}
```

**Note on `dimension` encoding:** The current code uses `writeByteArray(dimension.getBytes())` / `new String(readByteArray())`. The NeoForge version uses `ByteBufCodecs.STRING_UTF8` which is the standard approach. This is a minor improvement (explicit UTF-8 rather than platform-default charset).

---

## Step 2: Create NetworkHandler.java

**New file:** `src/main/java/dev/murad/shipping/network/NetworkHandler.java`

This replaces all 3 handler classes with a single event-driven registration.

```java
package dev.murad.shipping.network;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.network.client.EntityPosition;
import dev.murad.shipping.network.client.VehicleTrackerClientPacket;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.TugRoute;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static final Logger LOGGER = LogManager.getLogger(NetworkHandler.class);

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ShippingMod.MOD_ID).versioned("1");

        // Client → Server packets
        registrar.playToServer(
                SetEnginePacket.TYPE,
                SetEnginePacket.STREAM_CODEC,
                NetworkHandler::handleSetEngine
        );
        registrar.playToServer(
                EnrollVehiclePacket.TYPE,
                EnrollVehiclePacket.STREAM_CODEC,
                NetworkHandler::handleEnrollVehicle
        );
        registrar.playToServer(
                SetRouteTagPacket.TYPE,
                SetRouteTagPacket.STREAM_CODEC,
                NetworkHandler::handleSetRouteTag
        );

        // Server → Client packets
        registrar.playToClient(
                VehicleTrackerClientPacket.TYPE,
                VehicleTrackerClientPacket.STREAM_CODEC,
                NetworkHandler::handleVehicleTracker
        );
    }

    // --- Server-side handlers (for client→server packets) ---

    private static void handleSetEngine(SetEnginePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            var loco = serverPlayer.level().getEntity(packet.locoId());
            if (loco != null && loco.distanceTo(serverPlayer) < 6 && loco instanceof HeadVehicle l) {
                l.setEngineOn(packet.state());
            }
        });
    }

    private static void handleEnrollVehicle(EnrollVehiclePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.player();
            var loco = serverPlayer.level().getEntity(packet.locoId());
            if (loco != null && loco.distanceTo(serverPlayer) < 6 && loco instanceof HeadVehicle l) {
                l.enroll(serverPlayer.getUUID());
            }
        });
    }

    private static void handleSetRouteTag(SetRouteTagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            ItemStack heldStack = player.getItemInHand(
                    packet.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            LOGGER.info("Item in hand is {}", heldStack);
            if (heldStack.getItem() != ModItems.TUG_ROUTE.get()) {
                LOGGER.error("Item held in hand was not tug_route item, perhaps client has de-synced? Dropping packet");
                return;
            }

            CompoundTag routeTag = packet.tag();
            LOGGER.info(routeTag);
            TugRouteItem.saveRoute(TugRoute.fromNBT(routeTag), heldStack);
        });
    }

    // --- Client-side handler (for server→client packets) ---

    private static void handleVehicleTracker(VehicleTrackerClientPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            VehicleTrackerPacketHandler.toRender = packet.parse();
            VehicleTrackerPacketHandler.toRenderDimension = packet.dimension();
        });
    }
}
```

**Key differences from current code:**
- `IPayloadContext.player()` replaces `ctx.get().getSender()` — returns the sender for server-side handlers
- No `setPacketHandled(true)` — NeoForge handles this automatically
- No `Supplier<NetworkEvent.Context>` wrapper — handler receives `IPayloadContext` directly
- `@EventBusSubscriber` on MOD bus ensures automatic registration

---

## Step 3: Update All Send Sites

### 3a. AbstractHeadVehicleContainer.java (lines 60, 64)

**File:** `src/main/java/dev/murad/shipping/entity/container/AbstractHeadVehicleContainer.java`

**Current:**
```java
import dev.murad.shipping.network.VehiclePacketHandler;
// ...
VehiclePacketHandler.INSTANCE.sendToServer(new SetEnginePacket(entity.getId(), state));
VehiclePacketHandler.INSTANCE.sendToServer(new EnrollVehiclePacket(entity.getId()));
```

**Target:**
```java
import net.neoforged.neoforge.network.PacketDistributor;
// ...
PacketDistributor.sendToServer(new SetEnginePacket(entity.getId(), state));
PacketDistributor.sendToServer(new EnrollVehiclePacket(entity.getId()));
```

Remove the `VehiclePacketHandler` import.

### 3b. TugRouteClientHandler.java (line 187)

**File:** `src/main/java/dev/murad/shipping/item/container/TugRouteClientHandler.java`

**Current:**
```java
import dev.murad.shipping.network.TugRoutePacketHandler;
// ...
TugRoutePacketHandler.INSTANCE.sendToServer(new SetRouteTagPacket(route.hashCode(), isOffHand, route.toNBT()));
```

**Target:**
```java
import net.neoforged.neoforge.network.PacketDistributor;
// ...
PacketDistributor.sendToServer(new SetRouteTagPacket(route.hashCode(), isOffHand, route.toNBT()));
```

Remove the `TugRoutePacketHandler` import.

### 3c. PlayerTrainChunkManager.java (line 144)

**File:** `src/main/java/dev/murad/shipping/global/PlayerTrainChunkManager.java`

**Current:**
```java
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
// ...
VehicleTrackerPacketHandler.INSTANCE.send(
    PacketDistributor.PLAYER.with(() -> serverPlayer),
    VehicleTrackerClientPacket.of(getEntityPositions(), level.dimension().toString())
);
```

**Target:**
```java
import net.neoforged.neoforge.network.PacketDistributor;
// ...
PacketDistributor.sendToPlayer(
    serverPlayer,
    VehicleTrackerClientPacket.of(getEntityPositions(), level.dimension().toString())
);
```

Remove the `VehicleTrackerPacketHandler.INSTANCE` reference. Keep the `VehicleTrackerClientPacket` import.

---

## Step 4: Update Registration.java

**File:** `src/main/java/dev/murad/shipping/setup/Registration.java`

**Remove** these lines (networking is now event-driven via `@EventBusSubscriber`):
```java
import dev.murad.shipping.network.VehiclePacketHandler;
import dev.murad.shipping.network.TugRoutePacketHandler;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
// ...
TugRoutePacketHandler.register();
VehicleTrackerPacketHandler.register();
VehiclePacketHandler.register();
```

No replacement needed — `NetworkHandler` auto-registers via `@SubscribeEvent` on the MOD bus.

---

## Step 5: Retain VehicleTrackerPacketHandler as Client-Side State Container

**File:** `src/main/java/dev/murad/shipping/network/client/VehicleTrackerPacketHandler.java`

This class is **NOT deleted** entirely — it holds client-side state (`toRender`, `toRenderDimension`, `flush()`) used by `ForgeClientEventHandler.java` (lines 71, 205, 210).

**Current → Target:** Strip the `SimpleChannel`, `INSTANCE`, `LOCATION`, `PROTOCOL_VERSION`, `register()`, and `handleData()`. Keep:

```java
package dev.murad.shipping.network.client;

import java.util.ArrayList;
import java.util.List;

public class VehicleTrackerPacketHandler {
    public static List<EntityPosition> toRender = new ArrayList<>();
    public static String toRenderDimension = "";

    public static void flush() {
        toRender.clear();
    }
}
```

**Consumers remain unchanged:**
- `ForgeClientEventHandler.java:71` — calls `VehicleTrackerPacketHandler.flush()`
- `ForgeClientEventHandler.java:205` — reads `VehicleTrackerPacketHandler.toRenderDimension`
- `ForgeClientEventHandler.java:210` — iterates `VehicleTrackerPacketHandler.toRender`

---

## Step 6: EntityPosition.java — No Changes

**File:** `src/main/java/dev/murad/shipping/network/client/EntityPosition.java`

This is already a record with no Forge dependencies. No changes needed.

---

## File Summary

### Files to DELETE (2)

| File | Reason |
|------|--------|
| `src/main/java/dev/murad/shipping/network/VehiclePacketHandler.java` | Replaced by NetworkHandler.java |
| `src/main/java/dev/murad/shipping/network/TugRoutePacketHandler.java` | Replaced by NetworkHandler.java |

### Files to CREATE (1)

| File | Purpose |
|------|---------|
| `src/main/java/dev/murad/shipping/network/NetworkHandler.java` | Unified payload registration + handlers |

### Files to REWRITE (4 packets)

| File | Change |
|------|--------|
| `src/main/java/dev/murad/shipping/network/SetEnginePacket.java` | Class → record implementing CustomPacketPayload |
| `src/main/java/dev/murad/shipping/network/EnrollVehiclePacket.java` | Class → record implementing CustomPacketPayload |
| `src/main/java/dev/murad/shipping/network/SetRouteTagPacket.java` | Class → record implementing CustomPacketPayload |
| `src/main/java/dev/murad/shipping/network/client/VehicleTrackerClientPacket.java` | Class → record implementing CustomPacketPayload |

### Files to MODIFY (4)

| File | Lines | Change |
|------|-------|--------|
| `src/main/java/dev/murad/shipping/setup/Registration.java` | 4-6, 51-53 | Remove handler imports + register() calls |
| `src/main/java/dev/murad/shipping/entity/container/AbstractHeadVehicleContainer.java` | 6, 60, 64 | INSTANCE.sendToServer → PacketDistributor.sendToServer |
| `src/main/java/dev/murad/shipping/item/container/TugRouteClientHandler.java` | 5, 187 | INSTANCE.sendToServer → PacketDistributor.sendToServer |
| `src/main/java/dev/murad/shipping/global/PlayerTrainChunkManager.java` | 6, 144 | INSTANCE.send(PacketDistributor.PLAYER.with(...)) → PacketDistributor.sendToPlayer |
| `src/main/java/dev/murad/shipping/network/client/VehicleTrackerPacketHandler.java` | entire | Strip SimpleChannel; keep client-side state only |

### Files UNCHANGED (1)

| File | Reason |
|------|--------|
| `src/main/java/dev/murad/shipping/network/client/EntityPosition.java` | Already a clean record |

---

## Verification Steps

1. **Compile check:** `./gradlew build` — must pass with no SimpleChannel or NetworkEvent references
2. **Grep for leftover Forge networking imports:**
   ```bash
   grep -r "net.minecraftforge.network" src/main/java/
   # Should return 0 results after Phase 3
   ```
3. **Grep for leftover INSTANCE references:**
   ```bash
   grep -r "INSTANCE\.\(sendToServer\|send\b\)" src/main/java/
   # Should return 0 results after Phase 3
   ```
4. **Manual in-game test:** Open a tug/locomotive GUI, toggle engine on/off, set a route — verify packets arrive correctly
5. **Manual in-game test:** Hold conductor's wrench — verify vehicle tracker positions render on client

---

## Risk Notes

- **`ByteBufCodecs.COMPOUND_TAG`:** Used for `SetRouteTagPacket.tag` and `VehicleTrackerClientPacket.tag`. Verify this codec exists in NeoForge 1.21.1. If not available, use a custom StreamCodec wrapping `FriendlyByteBuf.readNbt()`/`writeNbt()`.
- **`context.player()` cast:** In server-side handlers, `context.player()` returns the sending player (a `ServerPlayer`). The cast is safe for `playToServer` registrations. For `playToClient` the player is the local client player.
- **`VehicleTrackerPacketHandler` rename consideration:** After stripping networking code, this class is just a client-side data holder. A future cleanup could rename it to `VehicleTrackerClientState`, but this is out of scope for Phase 3.
