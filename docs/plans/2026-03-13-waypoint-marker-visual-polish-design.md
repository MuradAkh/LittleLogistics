# Waypoint Marker Visual Polish Design

## Goal

Add motion, glow, particles, and thematic identity to the route waypoint markers, replacing the current static flat diamonds with animated, visually distinct markers for tug (nautical) and loco (industrial) routes.

## Design Decisions

- **Tug theme:** Nautical buoy — bobbing, dripping water particles, aquamarine/seafoam color
- **Loco theme:** Industrial signal lamp — pulsing glow, falling spark particles, warm amber color
- **Y-level:** Tug markers float relative to the player's camera Y (`camPos.y - 1.5`), not hardcoded to sea level. Loco markers use actual block Y.
- **Route direction:** Color gradient along the route (start color -> end color) so direction is obvious at a glance
- **Particles:** Ambient (drips/sparks) on markers + traveling particles along tug connection lines

## Marker Rendering

### Double-Diamond Glow

Each marker rendered in two passes:
1. **Outer glow:** Same diamond shape, 1.8x scale, low alpha (0.15-0.35, pulsing sinusoidally over ~2s). Brighter tint of the base color.
2. **Inner core:** Current diamond at MARKER_SIZE=0.35, full alpha, base color from route gradient.

Both use the existing `MARKER_TRIANGLES` render type. No new render types needed.

### Color Gradients

- **Tug:** Bright cyan (0.2, 1.0, 0.9) at first waypoint -> warm teal-green (0.2, 0.8, 0.5) at last waypoint. Chosen to be visible against blue water.
- **Loco:** Golden yellow (1.0, 0.9, 0.3) at first waypoint -> deep orange (1.0, 0.45, 0.1) at last waypoint.
- Intermediate waypoints interpolate linearly. Connection line segments blend between their endpoint colors.

### Animation

**Tug bobbing:**
- Vertical offset: `sin(gameTime * 0.08 + waypointIndex * 1.5) * 0.3` blocks
- Phase offset per waypoint prevents synchronized motion
- Applied to marker, stem top, label, and particle spawn point

**Loco pulse:**
- Inner core brightness oscillates: modulate RGB by `0.85 + 0.15 * sin(gameTime * 0.1 + waypointIndex * 1.2)`
- Simulates a signal lamp flicker

**Both:**
- Outer glow alpha pulses: `0.25 + 0.1 * sin(gameTime * 0.05)`

## Connection Lines

### Animated Dashes (Tug Only)

- Each connection line subdivided into ~1.5 block segments
- Every other segment drawn (dash pattern)
- Visible segments scroll forward over time using `gameTime + partialTick`, speed ~2 blocks/second
- Color follows the route gradient between endpoints
- Loop-closing segment (last->first) at 0.4x alpha
- Loco routes: no connections (unordered HashSet, unchanged)

## Particles

### Ambient Particles

**Tug markers:**
- `DRIPPING_WATER` or `FALLING_WATER` particles falling from marker position
- ~1 particle every 10-15 ticks, randomized
- Only within 32 blocks of player

**Loco markers:**
- `LAVA` or `FLAME` particles (small, warm sparks) falling from marker
- Same spawn rate and distance cap

### Traveling Particles (Tug Connections Only)

- Glowing particle spawns at waypoint, moves along connection toward next waypoint
- One particle in flight per segment at a time
- Uses `ENTITY_EFFECT` or `ENCHANT` particle type, tinted to route gradient
- Travel speed: ~4 blocks/second
- Despawns on arrival, respawns at origin after ~20 tick delay
- Track progress per segment as a float array, reset when route changes

## Unchanged Behavior

- Distance fade: 96-128 blocks (existing)
- Text labels above markers (existing)
- Stem lines from base to marker (existing, but stem top now bobs with tug markers)
- Loco rail surface boxes (existing, with fade alpha)
- `DISABLE_ROUTE_MARKERS` config disables everything (existing)

## Files to Modify

- `RouteMarkerRenderer.java` — add glow pass, bobbing offset, dash rendering, gradient color computation
- `ForgeClientEventHandler.java` — update render calls with animation parameters, add particle spawning logic
- No data model changes (TugRouteNode/LocoRouteNode unchanged)
- No new textures or render types needed
