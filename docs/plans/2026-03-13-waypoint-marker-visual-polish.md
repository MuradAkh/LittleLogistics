# Waypoint Marker Visual Polish Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add animation, glow, gradient colors, and particles to route waypoint markers for a polished nautical (tug) and industrial (loco) feel.

**Architecture:** All changes are client-side rendering. Add color gradient computation and animation utilities to `RouteMarkerRenderer`, then update `ForgeClientEventHandler` to use them. Particles use Minecraft's built-in `level.addParticle()`. Traveling particles tracked via a lightweight float array reset on route change. Tug baseY changed from sea level to camera-relative.

**Tech Stack:** NeoForge 1.21.1 rendering (PoseStack, MultiBufferSource, RenderType), Minecraft ParticleTypes, existing `ModRenderType.LINES` and `ModRenderType.MARKER_TRIANGLES`.

---

### Task 1: Add Color Gradient and Animation Utilities to RouteMarkerRenderer

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java`

Add static utility methods and constants for the visual polish features. These are pure functions with no state.

**Step 1: Add color constants and gradient method**

Add these constants and method to `RouteMarkerRenderer`:

```java
// Tug route gradient: aquamarine -> teal-green
public static final float[] TUG_COLOR_START = {0.2f, 1.0f, 0.9f};
public static final float[] TUG_COLOR_END = {0.2f, 0.8f, 0.5f};

// Loco route gradient: golden yellow -> deep orange
public static final float[] LOCO_COLOR_START = {1.0f, 0.9f, 0.3f};
public static final float[] LOCO_COLOR_END = {1.0f, 0.45f, 0.1f};

// Glow outer diamond scale multiplier
public static final float GLOW_SCALE = 1.8f;

/**
 * Linearly interpolate a color along a route gradient.
 * @param start RGB array [r, g, b]
 * @param end   RGB array [r, g, b]
 * @param t     progress along route (0.0 = first waypoint, 1.0 = last)
 * @return RGB array [r, g, b]
 */
public static float[] lerpColor(float[] start, float[] end, float t) {
    t = Math.max(0f, Math.min(1f, t));
    return new float[]{
        start[0] + (end[0] - start[0]) * t,
        start[1] + (end[1] - start[1]) * t,
        start[2] + (end[2] - start[2]) * t
    };
}
```

**Step 2: Add bobbing offset method**

```java
/**
 * Compute the vertical bobbing offset for a tug waypoint marker.
 * Each waypoint bobs with a phase offset to avoid synchronized motion.
 * @param gameTime     level game time (ticks)
 * @param partialTick  partial tick for smooth interpolation
 * @param waypointIndex index of this waypoint in the route
 * @return vertical offset in blocks
 */
public static double computeBob(long gameTime, float partialTick, int waypointIndex) {
    double time = (gameTime + partialTick) * 0.08 + waypointIndex * 1.5;
    return Math.sin(time) * 0.3;
}
```

**Step 3: Add glow alpha pulse method**

```java
/**
 * Compute the pulsing alpha for the outer glow diamond.
 * @param gameTime    level game time (ticks)
 * @param partialTick partial tick
 * @return alpha value (0.15 - 0.35)
 */
public static float computeGlowAlpha(long gameTime, float partialTick) {
    double time = (gameTime + partialTick) * 0.05;
    return (float) (0.25 + 0.1 * Math.sin(time));
}

/**
 * Compute the brightness pulse multiplier for loco signal lamp effect.
 * @param gameTime     level game time (ticks)
 * @param partialTick  partial tick
 * @param waypointIndex index for phase offset
 * @return multiplier (0.85 - 1.0)
 */
public static float computeLocoPulse(long gameTime, float partialTick, int waypointIndex) {
    double time = (gameTime + partialTick) * 0.1 + waypointIndex * 1.2;
    return (float) (0.85 + 0.15 * Math.sin(time));
}
```

**Step 4: Add renderGlow method**

This renders the outer glow pass — a larger, translucent diamond behind the core:

```java
/**
 * Renders the outer glow diamond (larger, translucent) behind the core marker.
 * Call this BEFORE renderMarker for correct layering.
 */
public static void renderGlow(PoseStack poseStack, VertexConsumer buffer, Camera camera,
                               Vec3 camPos, double worldX, double markerY, double worldZ,
                               float r, float g, float b, float glowAlpha) {
    if (glowAlpha <= 0.0f) return;

    poseStack.pushPose();
    {
        poseStack.translate(worldX - camPos.x, markerY - camPos.y, worldZ - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        Matrix4f mat = poseStack.last().pose();
        float s = MARKER_SIZE * GLOW_SCALE;

        // Brighter tint for glow
        float gr = Math.min(1.0f, r * 1.3f);
        float gg = Math.min(1.0f, g * 1.3f);
        float gb = Math.min(1.0f, b * 1.3f);

        buffer.addVertex(mat, -s, 0, 0).setColor(gr, gg, gb, glowAlpha);
        buffer.addVertex(mat, 0, s, 0).setColor(gr, gg, gb, glowAlpha);
        buffer.addVertex(mat, s, 0, 0).setColor(gr, gg, gb, glowAlpha);

        buffer.addVertex(mat, -s, 0, 0).setColor(gr, gg, gb, glowAlpha);
        buffer.addVertex(mat, s, 0, 0).setColor(gr, gg, gb, glowAlpha);
        buffer.addVertex(mat, 0, -s, 0).setColor(gr, gg, gb, glowAlpha);
    }
    poseStack.popPose();
}
```

**Step 5: Add animated dash connection method**

Replace the solid connection line with animated dashes:

```java
private static final float DASH_LENGTH = 1.5f;
private static final float DASH_SPEED = 2.0f; // blocks per second

/**
 * Renders an animated dashed line between two waypoints at marker height.
 * Dashes scroll in the direction of travel.
 */
public static void renderDashedConnection(PoseStack poseStack, VertexConsumer lineBuffer,
                                           Vec3 camPos, double x1, double y1, double z1,
                                           double x2, double y2, double z2,
                                           float r1, float g1, float b1,
                                           float r2, float g2, float b2,
                                           float alpha, long gameTime, float partialTick) {
    if (alpha <= 0.0f) return;

    double my1 = y1 + STEM_HEIGHT;
    double my2 = y2 + STEM_HEIGHT;
    double dx = x2 - x1, dy = my2 - my1, dz = z2 - z1;
    double totalLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (totalLen < 0.01) return;

    // Normalize direction
    double nx = dx / totalLen, ny = dy / totalLen, nz = dz / totalLen;
    float fnx = (float) nx, fny = (float) ny, fnz = (float) nz;

    // Scrolling offset
    double timeOffset = (gameTime + partialTick) * DASH_SPEED / 20.0;
    double dashCycle = DASH_LENGTH * 2.0;
    double scrollOffset = (timeOffset % dashCycle);

    poseStack.pushPose();
    {
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = poseStack.last().pose();

        double pos = -scrollOffset;
        while (pos < totalLen) {
            double segStart = Math.max(0, pos);
            double segEnd = Math.min(totalLen, pos + DASH_LENGTH);
            pos += dashCycle;

            if (segEnd <= segStart) continue;

            // Lerp t values for gradient color
            float t1 = (float) (segStart / totalLen);
            float t2 = (float) (segEnd / totalLen);
            float sr = r1 + (r2 - r1) * t1, sg = g1 + (g2 - g1) * t1, sb = b1 + (b2 - b1) * t1;
            float er = r1 + (r2 - r1) * t2, eg = g1 + (g2 - g1) * t2, eb = b1 + (b2 - b1) * t2;

            float sx = (float) (x1 + nx * segStart);
            float sy = (float) (my1 + ny * segStart);
            float sz = (float) (z1 + nz * segStart);
            float ex = (float) (x1 + nx * segEnd);
            float ey = (float) (my1 + ny * segEnd);
            float ez = (float) (z1 + nz * segEnd);

            lineBuffer.addVertex(mat, sx, sy, sz).setColor(sr, sg, sb, alpha).setNormal(fnx, fny, fnz);
            lineBuffer.addVertex(mat, ex, ey, ez).setColor(er, eg, eb, alpha).setNormal(fnx, fny, fnz);
        }
    }
    poseStack.popPose();
}
```

**Step 6: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java
git commit -m "feat: add color gradient, animation, glow, and dash utilities to RouteMarkerRenderer"
```

---

### Task 2: Update Tug Route Rendering with Animation and Gradients

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java:160-208` (tug route block)

Replace the tug route rendering block with animated, gradient-colored markers and dashed connections.

**Step 1: Rewrite the tug route rendering block**

Replace the entire `else if (stack.getItem().equals(ModItems.TUG_ROUTE.get()))` block (lines 160-208) with:

```java
} else if (stack.getItem().equals(ModItems.TUG_ROUTE.get())) {
    if (ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()) {
        return false;
    }

    var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
    var camPos = camera.getPosition();
    var pose = event.getPoseStack();
    var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    TugRoute route = TugRouteItem.getRoute(stack);
    int routeSize = route.size();
    if (routeSize == 0) {
        return true;
    }

    double baseY = camPos.y - 1.5;
    long gameTime = player.level().getGameTime();
    float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

    // Draw animated dashed connections between consecutive waypoints
    var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
    for (int i = 0; i < routeSize; i++) {
        TugRouteNode from = route.get(i);
        TugRouteNode to = route.get((i + 1) % routeSize);
        double fx = from.getX() + 0.5, fz = from.getZ() + 0.5;
        double tx = to.getX() + 0.5, tz = to.getZ() + 0.5;
        Vec3 midpoint = new Vec3((fx + tx) / 2.0, baseY, (fz + tz) / 2.0);
        float connAlpha = RouteMarkerRenderer.computeAlpha(midpoint, camPos);
        if (i == routeSize - 1) connAlpha *= 0.4f;

        float tFrom = routeSize > 1 ? (float) i / (routeSize - 1) : 0f;
        float tTo = routeSize > 1 ? (float) ((i + 1) % routeSize) / (routeSize - 1) : 0f;
        float[] cFrom = RouteMarkerRenderer.lerpColor(RouteMarkerRenderer.TUG_COLOR_START, RouteMarkerRenderer.TUG_COLOR_END, tFrom);
        float[] cTo = RouteMarkerRenderer.lerpColor(RouteMarkerRenderer.TUG_COLOR_START, RouteMarkerRenderer.TUG_COLOR_END, tTo);

        RouteMarkerRenderer.renderDashedConnection(pose, lineBuffer, camPos,
                fx, baseY, fz, tx, baseY, tz,
                cFrom[0], cFrom[1], cFrom[2],
                cTo[0], cTo[1], cTo[2],
                connAlpha, gameTime, partialTick);
    }

    // Draw markers for each waypoint
    for (int i = 0; i < routeSize; i++) {
        TugRouteNode node = route.get(i);
        double wx = node.getX() + 0.5;
        double wz = node.getZ() + 0.5;
        float alpha = RouteMarkerRenderer.computeAlpha(new Vec3(wx, baseY, wz), camPos);
        if (alpha <= 0.0f) continue;

        float t = routeSize > 1 ? (float) i / (routeSize - 1) : 0f;
        float[] color = RouteMarkerRenderer.lerpColor(RouteMarkerRenderer.TUG_COLOR_START, RouteMarkerRenderer.TUG_COLOR_END, t);

        double bob = RouteMarkerRenderer.computeBob(gameTime, partialTick, i);
        double markerY = baseY + RouteMarkerRenderer.STEM_HEIGHT + bob;

        // Stem (from baseY to bobbing marker position)
        RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
                wx, baseY, wz, color[0], color[1], color[2], alpha);

        // Outer glow (rendered first, behind core)
        float glowAlpha = RouteMarkerRenderer.computeGlowAlpha(gameTime, partialTick) * alpha;
        RouteMarkerRenderer.renderGlow(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                wx, markerY, wz, color[0], color[1], color[2], glowAlpha);

        // Inner core diamond (at bobbing position)
        var triBuffer = buffer.getBuffer(ModRenderType.MARKER_TRIANGLES);
        RouteMarkerRenderer.renderMarker(pose, triBuffer, camera, camPos,
                wx, baseY + bob, wz, color[0], color[1], color[2], alpha);

        // Label (above bobbing marker)
        RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos,
                wx, baseY + bob, wz, node.getDisplayName(i), alpha);
    }

    buffer.endBatch();
}
```

**Step 2: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: add animated dashes, bobbing, glow, and gradient to tug route markers"
```

---

### Task 3: Update Loco Route Rendering with Animation and Gradients

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java:88-159` (loco route block)

Update loco markers with gradient coloring, glow, and signal lamp pulse.

**Step 1: Rewrite the loco route rendering block**

Replace the loco route `if` block (lines 88-159) with:

```java
if (stack.getItem().equals(ModItems.LOCO_ROUTE.get())) {
    if (ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()) {
        return false;
    }
    var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
    var camPos = camera.getPosition();
    var pose = event.getPoseStack();
    var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    var routeNodes = LocoRouteItem.getRoute(stack);
    int routeSize = routeNodes.size();
    long gameTime = player.level().getGameTime();
    float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

    int index = 0;
    for (var node : routeNodes) {
        var block = node.toBlockPos();
        double wx = block.getX() + 0.5;
        double wz = block.getZ() + 0.5;
        float alpha = RouteMarkerRenderer.computeAlpha(new Vec3(wx, block.getY(), wz), camPos);
        if (alpha <= 0.0f) { index++; continue; }

        float t = routeSize > 1 ? (float) index / (routeSize - 1) : 0f;
        float[] color = RouteMarkerRenderer.lerpColor(RouteMarkerRenderer.LOCO_COLOR_START, RouteMarkerRenderer.LOCO_COLOR_END, t);

        // Apply signal lamp pulse to color brightness
        float pulse = RouteMarkerRenderer.computeLocoPulse(gameTime, partialTick, index);
        float pr = color[0] * pulse, pg = color[1] * pulse, pb = color[2] * pulse;

        // Stem
        RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
                wx, block.getY(), wz, pr, pg, pb, alpha);

        double markerY = block.getY() + RouteMarkerRenderer.STEM_HEIGHT;

        // Outer glow
        float glowAlpha = RouteMarkerRenderer.computeGlowAlpha(gameTime, partialTick) * alpha;
        RouteMarkerRenderer.renderGlow(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                wx, markerY, wz, color[0], color[1], color[2], glowAlpha);

        // Inner core diamond
        RouteMarkerRenderer.renderMarker(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                wx, block.getY(), wz, pr, pg, pb, alpha);

        // Rail surface box (keep existing rail shape rendering)
        pose.pushPose();
        {
            var shape = RailHelper.getRail(block, player.level())
                    .map(pos -> RailHelper.getShape(pos, player.level()))
                    .orElse(RailShape.EAST_WEST);
            double railBaseY = (shape.isAscending() ? 0.1 : 0);
            double railBaseX = 0;
            double railBaseZ = 0;
            var rotation = Axis.ZP.rotationDegrees(0);
            switch (shape) {
                case ASCENDING_EAST -> {
                    railBaseX = 0.2;
                    rotation = Axis.ZP.rotationDegrees(45);
                }
                case ASCENDING_WEST -> {
                    railBaseX = 0.1;
                    railBaseY += 0.7;
                    rotation = Axis.ZP.rotationDegrees(-45);
                }
                case ASCENDING_NORTH -> {
                    railBaseZ = 0.1;
                    railBaseY += 0.7;
                    rotation = Axis.XP.rotationDegrees(45);
                }
                case ASCENDING_SOUTH -> {
                    railBaseZ = 0.2;
                    rotation = Axis.XP.rotationDegrees(-45);
                }
            }

            pose.translate(block.getX() + railBaseX - camPos.x, block.getY() + railBaseY - camPos.y, block.getZ() + railBaseZ - camPos.z);
            pose.mulPose(rotation);

            AABB a = new AABB(0, 0, 0, 1, 0.2, 1);
            LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, pr, pg, pb, 0.5f * alpha);
        }
        pose.popPose();

        // Label
        String label = node.hasCustomName() ? node.getName() : String.valueOf(index + 1);
        RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos, wx, block.getY(), wz, label, alpha);

        index++;
    }

    buffer.endBatch();
}
```

**Step 2: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: add glow, gradient, and signal lamp pulse to loco route markers"
```

---

### Task 4: Add Ambient and Traveling Particles

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java`

Add particle spawning to both route types. Particles are spawned via `player.level().addParticle()` within the existing render method, gated by distance (<32 blocks) and tick-based cooldown.

**Step 1: Add particle constants and state at the top of `ForgeClientEventHandler`**

Add these fields right after the class declaration (before `ModRenderType`):

```java
private static final double PARTICLE_RANGE = 32.0;
private static final int AMBIENT_PARTICLE_INTERVAL = 12; // ticks between ambient particles

// Traveling particle state for tug routes (progress per segment, 0.0-1.0)
private static float[] travelingParticleProgress = new float[0];
private static int travelingParticleRouteHash = 0;
private static final float TRAVELING_PARTICLE_SPEED = 4.0f / 20.0f; // 4 blocks/sec in ticks
private static final int TRAVELING_PARTICLE_DELAY = 20; // tick delay before respawn
private static int[] travelingParticleDelay = new int[0];
```

**Step 2: Add ambient particle spawning to the tug route rendering block**

After the tug marker rendering loop (after the `buffer.endBatch()` call), add a separate particle loop. Insert this code right before the closing `}` of the tug route block, after `buffer.endBatch();`:

```java
    // Ambient + traveling particles (tick-based, not every frame)
    if (gameTime % 2 == 0) { // every other tick to reduce cost
        // Reset traveling particle state if route changed
        int routeHash = route.hashCode();
        if (routeHash != travelingParticleRouteHash || travelingParticleProgress.length != routeSize) {
            travelingParticleProgress = new float[routeSize];
            travelingParticleDelay = new int[routeSize];
            travelingParticleRouteHash = routeHash;
        }

        for (int i = 0; i < routeSize; i++) {
            TugRouteNode node = route.get(i);
            double wx = node.getX() + 0.5;
            double wz = node.getZ() + 0.5;
            double dist = camPos.distanceTo(new Vec3(wx, baseY, wz));
            if (dist > PARTICLE_RANGE) continue;

            double bob = RouteMarkerRenderer.computeBob(gameTime, partialTick, i);
            double markerY = baseY + RouteMarkerRenderer.STEM_HEIGHT + bob;

            // Ambient dripping water particles
            if (gameTime % AMBIENT_PARTICLE_INTERVAL == (i % AMBIENT_PARTICLE_INTERVAL)) {
                player.level().addParticle(
                        net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER,
                        wx, markerY - 0.3, wz, 0, 0, 0);
            }

            // Traveling particles along connections
            if (i < routeSize) {
                TugRouteNode next = route.get((i + 1) % routeSize);
                double nx = next.getX() + 0.5;
                double nz = next.getZ() + 0.5;

                if (travelingParticleDelay[i] > 0) {
                    travelingParticleDelay[i]--;
                } else {
                    travelingParticleProgress[i] += TRAVELING_PARTICLE_SPEED;
                    if (travelingParticleProgress[i] >= 1.0f) {
                        travelingParticleProgress[i] = 0.0f;
                        travelingParticleDelay[i] = TRAVELING_PARTICLE_DELAY;
                    } else {
                        double px = wx + (nx - wx) * travelingParticleProgress[i];
                        double pz = wz + (nz - wz) * travelingParticleProgress[i];
                        double py = markerY;

                        float tp = routeSize > 1 ? (float) i / (routeSize - 1) : 0f;
                        float[] tColor = RouteMarkerRenderer.lerpColor(
                                RouteMarkerRenderer.TUG_COLOR_START, RouteMarkerRenderer.TUG_COLOR_END, tp);

                        player.level().addParticle(
                                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                px, py, pz,
                                0, -0.1, 0);
                    }
                }
            }
        }
    }
```

**Step 3: Add ambient particle spawning to the loco route rendering block**

After the loco route `buffer.endBatch();`, add a similar particle loop. Insert before the closing `}`:

```java
    // Ambient spark particles (tick-based)
    if (gameTime % 2 == 0) {
        int locoIndex = 0;
        for (var node : routeNodes) {
            var block = node.toBlockPos();
            double wx = block.getX() + 0.5;
            double wz = block.getZ() + 0.5;
            double dist = camPos.distanceTo(new Vec3(wx, block.getY(), wz));
            if (dist > PARTICLE_RANGE) { locoIndex++; continue; }

            double markerY = block.getY() + RouteMarkerRenderer.STEM_HEIGHT;

            if (gameTime % AMBIENT_PARTICLE_INTERVAL == (locoIndex % AMBIENT_PARTICLE_INTERVAL)) {
                player.level().addParticle(
                        net.minecraft.core.particles.ParticleTypes.LAVA,
                        wx, markerY - 0.2, wz, 0, 0, 0);
            }
            locoIndex++;
        }
    }
```

**Step 4: Add the ParticleTypes import**

Add to imports at top of ForgeClientEventHandler:

```java
import net.minecraft.core.particles.ParticleTypes;
```

**Step 5: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: add ambient particles and traveling particles along tug route connections"
```

---

### Task 5: Fix Stem Rendering for Bobbing Tug Markers

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java`

The current `renderStem` always renders from baseY to baseY + STEM_HEIGHT. But with bobbing, the marker position moves, and the stem top should follow. Add an overload that accepts a custom top offset.

**Step 1: Add overloaded renderStem method**

Add this method below the existing `renderStem`:

```java
/**
 * Renders a vertical line (stem) from baseY up to baseY + stemTop.
 * Used when the marker position is offset (e.g., by bobbing animation).
 */
public static void renderStem(PoseStack poseStack, VertexConsumer lineBuffer,
                               Vec3 camPos, double worldX, double baseY, double worldZ,
                               float r, float g, float b, float alpha, float stemTop) {
    if (alpha <= 0.0f) return;

    poseStack.pushPose();
    {
        poseStack.translate(worldX - camPos.x, baseY - camPos.y, worldZ - camPos.z);
        Matrix4f mat = poseStack.last().pose();
        lineBuffer.addVertex(mat, 0, 0, 0).setColor(r, g, b, alpha).setNormal(0, 1, 0);
        lineBuffer.addVertex(mat, 0, stemTop, 0).setColor(r, g, b, alpha).setNormal(0, 1, 0);
    }
    poseStack.popPose();
}
```

**Step 2: Update tug route rendering to use the bobbing-aware stem**

In `ForgeClientEventHandler.java`, in the tug marker loop, replace the stem call:

```java
// Old:
RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
        wx, baseY, wz, color[0], color[1], color[2], alpha);
```

with:

```java
// New: stem stretches to bobbing marker position
RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
        wx, baseY, wz, color[0], color[1], color[2], alpha,
        (float) (RouteMarkerRenderer.STEM_HEIGHT + bob));
```

**Step 3: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: stem line follows bobbing marker position for tug routes"
```

---

### Task 6: Build and Visual Verification

**Step 1: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 2: Launch client and test**

Run: `./gradlew runClient`

Tug route checklist:
- [ ] Markers use aquamarine-to-teal gradient (first waypoint cyan, last green-teal)
- [ ] Markers bob up and down with different phases per waypoint
- [ ] Outer glow diamond visible behind the inner core, pulsing
- [ ] Connection lines are animated dashes scrolling in route direction
- [ ] Dashes follow the color gradient
- [ ] Loop-closing segment is dimmer
- [ ] BaseY tracks player Y (markers visible at any elevation)
- [ ] Water drip particles falling from markers within 32 blocks
- [ ] Glowing particles traveling along connection lines
- [ ] Traveling particles respawn after delay
- [ ] Distance fade still works (96-128 blocks)

Loco route checklist:
- [ ] Markers use golden-yellow-to-deep-orange gradient
- [ ] Signal lamp pulse effect on marker brightness
- [ ] Outer glow diamond pulsing
- [ ] Rail surface boxes still render, colored to match gradient
- [ ] Lava/spark particles falling from markers within 32 blocks
- [ ] Labels show name or 1-based index

General:
- [ ] No rendering artifacts, z-fighting, or flickering
- [ ] Config `disableRouteMarkers=true` disables everything
- [ ] Empty routes don't crash
- [ ] Off-hand route items still render

**Step 3: Commit any fixes from visual testing**

```bash
git add -A
git commit -m "fix: visual testing adjustments for polished route markers"
```
