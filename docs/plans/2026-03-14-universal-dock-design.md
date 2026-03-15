# Universal Dock System Design

## Problem

The current loading/unloading system requires players to place specific hopper types (vanilla hopper, RapidHopper, FluidHopper, VesselCharger) adjacent to dock blocks in specific orientations. This is unintuitive, poorly discoverable, and incompatible with how modded Minecraft works -- where pipe/cable/conduit mods interact with blocks via NeoForge capabilities.

Additionally, there are four distinct dock block types (TugDock, BargeDock, LocomotiveDockingRail, TrainCarDockingRail) with confusing INVERTED toggles that control different things on different blocks.

## Design

### New Blocks

| New Block | Replaces |
|---|---|
| `DockRail` | `LocomotiveDockingRail`, `TrainCarDockingRail` |
| `DockBlock` | `TugDockBlock`, `BargeDockBlock` |

### Eliminated Blocks

- `FluidHopperBlock` -- absorbed into dock capability delegation
- `VesselChargerBlock` -- absorbed into dock capability delegation
- `RapidHopperBlock` -- no longer needed; transfer rate determined by connecting mod

### Block States

- `FACING` -- direction the waterway runs (DockBlock only; DockRail infers from rail shape)
- No INVERTED, no POWERED, no head/tail distinction

### Capability Delegation

Dock blocks always expose `ItemHandler.BLOCK`, `FluidHandler.BLOCK`, and `EnergyStorage.BLOCK` on their applicable faces, regardless of whether a vehicle is present:

- **Vehicle present** -- capability wrapper delegates to the vehicle's storage
- **Vehicle absent** -- capability wrapper returns empty-but-valid no-op handlers (empty inventory, empty tank, zero energy; accepts nothing, provides nothing)

This ensures pipe mods can cache capability references, visually connect to docks at all times, and never need re-registration when vehicles arrive/leave.

The dock is a fully transparent proxy. It does not know or control transfer direction -- external blocks (pipes, hoppers, cables) decide whether to push or pull. There is no internal buffer.

#### Face Mapping

- **DockRail** -- all 6 faces (top, bottom, north, south, east, west)
- **DockBlock** (water) -- all faces except the waterway-facing face

### Pass-Through Rule

When a head vehicle (tug/locomotive) arrives at a dock:

1. Head vehicle asks the dock "should I hold here?"
2. Dock checks: is there another dock block in the forward direction (along the vehicle's travel heading)?
3. If yes -- don't hold, let the vehicle pass through
4. If no -- this is the front of the chain, hold here

For rail, "forward" means the next rail block in the travel direction. For water, "forward" means the next block in the `FACING` direction.

Followers (wagons/barges) settle into position on subsequent docks via spring physics when the head vehicle stops.

### Hold Logic

Each dock independently tracks its own hold state. The head vehicle stays docked as long as **any** dock in the chain reports holding. When all docks release, the head vehicle undocks and the convoy resumes.

The head vehicle discovers its dock chain by walking its `LinkableEntity` follower list and checking if the block at each follower's position is a dock. No spatial chain discovery needed.

#### Hold Conditions (per dock)

- **Idle timeout** -- release after no transfers have occurred for N seconds (default: 5s, configurable)
- **Redstone mode** -- Ignore / Hold while powered / Disable while powered

### Configuration (Create-style in-world)

Each dock block has a small interaction zone on part of its hitbox:

- **DockRail** -- small square on top of the rail
- **DockBlock** -- small square on a side face

| Action | Effect |
|---|---|
| Scroll up/down on interaction zone | Adjust idle timeout duration (1s, 2s, 5s, 10s, 20s) |
| Right-click on interaction zone | Cycle redstone mode: Ignore -> Hold while powered -> Disable while powered |

Visual feedback: tooltip overlay showing current settings when looking at the interaction zone, plus a visual indicator on the block when a vehicle is docked.

### Vehicle Capability Fixes

As part of this work, all vehicles with storage must register entity capabilities:

- `ChestBargeEntity` / `BarrelBargeEntity` -- register `ItemHandler.ENTITY` (currently missing)
- All other storage vehicles already have capabilities registered

### Player Workflow

1. Place dock rails/blocks in a line along the track/waterway
2. Connect pipes, cables, or conduits from any mod to the dock faces
3. Optionally scroll/right-click the dock to tune timeout and redstone behavior
4. Vehicle arrives, passes through to the front dock, followers settle behind
5. External mods transfer items/fluids/energy through the dock's capability proxies
6. After idle timeout expires on all docks, convoy resumes

### Breaking Changes

- `TugDockBlock`, `BargeDockBlock`, `LocomotiveDockingRail`, `TrainCarDockingRail` are removed
- `FluidHopperBlock`, `VesselChargerBlock`, `RapidHopperBlock` are removed
- Players must rebuild dock setups with the new universal blocks
- This is acceptable as part of the 1.20 -> 1.21 major version upgrade

### What This Design Does NOT Include

- Item/fluid filtering on docks (future addition)
- Auto-eject to vanilla containers (pipe mods or vanilla hoppers required)
- Vehicle capability exposure outside of docks (vehicles remain opaque to automation unless docked; players can still right-click interact)
