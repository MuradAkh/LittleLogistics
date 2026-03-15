# Dock Rail Dynamic Panels

## Summary

QOL change: when an adjacent block exposes an item/fluid/energy capability toward a dock rail, that side renders a thin panel. This gives visual feedback about which sides are connected for transfers.

## Block State

Add 6 `BooleanProperty` fields to `DockRail`: `NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN`, all default false.

Only perpendicular sides + up + down are ever set true. Along-rail sides stay false (enforced in `neighborChanged`).

## Neighbor Detection

In `neighborChanged` (full `Level` access), for each eligible direction:
1. Get the neighbor block position
2. Check if it exposes `ItemHandler.BLOCK`, `FluidHandler.BLOCK`, or `EnergyStorage.BLOCK` capability toward the dock rail
3. Update the boolean property accordingly

Also update in `onPlace` for initial placement.

## Model (Multipart)

Switch from variants to multipart in `ModBlockStateProvider`.

**Base part:** always present — existing flat rail model (`dock_rail`).

**Panel parts:** conditional on each boolean — thin box elements using `minecraft:block/deepslate` texture (placeholder).

### Panel Geometry (in 16ths)

| Panel  | From         | To           | Notes                          |
|--------|--------------|--------------|--------------------------------|
| North  | 0, 0, -1     | 16, 16, 1    | Full height, straddles boundary |
| South  | 0, 0, 15     | 16, 16, 17   | Full height, straddles boundary |
| East   | 15, 0, 0     | 17, 16, 16   | Full height, straddles boundary |
| West   | -1, 0, 0     | 1, 16, 16    | Full height, straddles boundary |
| Top    | 0, 15, 0     | 16, 17, 16   | Tunnel ceiling, straddles top  |
| Bottom | 0, 0, 0      | 16, 2, 16    | Opaque floor, inside block     |

## Capability Registration

No changes needed — capabilities already ignore direction parameter.

## Files Changed

- `DockRail.java` — add boolean properties, neighbor detection
- `ModBlockStateProvider.java` — switch to multipart, add panel models
- Generated blockstate/model JSON updated via `runData`
