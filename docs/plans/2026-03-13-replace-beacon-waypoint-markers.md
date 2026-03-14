# Replace Beacon Beam Waypoint Markers

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the tall beacon beam rendering for tug and loco route waypoints with small floating diamond markers, numbered labels, and connecting lines between consecutive waypoints.

**Architecture:** All changes are in the client-side rendering path. Replace `BeaconRenderer.renderBeaconBeam()` calls in `ForgeClientEventHandler.renderRouteOnStack()` with custom geometry: a small billboard diamond shape at each waypoint, thin vertical stem lines, numbered text labels, and dashed connecting lines between consecutive nodes (tug routes only, since they're ordered). Add distance-based fade so markers aren't visible from arbitrarily far away.

**Tech Stack:** NeoForge 1.21.1 rendering APIs (PoseStack, MultiBufferSource, RenderType), existing `ModRenderType.LINES`.

---

### Task 1: Extract Helper Methods for Marker Geometry

**Files:**
- Create: `src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java`

This new class holds all the low-level rendering helpers so `ForgeClientEventHandler` stays clean.

**Step 1: Create the RouteMarkerRenderer class with a diamond billboard method**

```java
package dev.murad.shipping.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RouteMarkerRenderer {

    private static final float MARKER_SIZE = 0.35f;
    private static final float STEM_HEIGHT = 3.0f;
    private static final double MAX_RENDER_DISTANCE = 128.0;
    private static final double FADE_START_DISTANCE = 96.0;

    /**
     * Compute alpha based on distance, fading from full at FADE_START to 0 at MAX_RENDER_DISTANCE.
     * Returns 0 if beyond max distance (skip rendering).
     */
    public static float computeAlpha(Vec3 markerPos, Vec3 camPos) {
        double dist = markerPos.distanceTo(camPos);
        if (dist > MAX_RENDER_DISTANCE) return 0f;
        if (dist < FADE_START_DISTANCE) return 1f;
        return (float) (1.0 - (dist - FADE_START_DISTANCE) / (MAX_RENDER_DISTANCE - FADE_START_DISTANCE));
    }

    /**
     * Renders a small diamond shape as a camera-facing billboard at the given world position.
     * The diamond hovers STEM_HEIGHT blocks above baseY.
     */
    public static void renderMarker(PoseStack pose, VertexConsumer buffer, Camera camera,
                                     Vec3 camPos, double worldX, double baseY, double worldZ,
                                     float r, float g, float b, float alpha) {
        if (alpha <= 0) return;

        double markerY = baseY + STEM_HEIGHT;

        pose.pushPose();
        pose.translate(worldX + 0.5 - camPos.x, markerY - camPos.y, worldZ + 0.5 - camPos.z);
        pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        Matrix4f mat = pose.last().pose();
        float s = MARKER_SIZE;

        // Diamond shape: 4 triangles forming a rhombus
        // Top triangle
        buffer.addVertex(mat, 0, s, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);
        buffer.addVertex(mat, -s * 0.6f, 0, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);
        buffer.addVertex(mat, s * 0.6f, 0, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);
        // Bottom triangle
        buffer.addVertex(mat, 0, -s, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);
        buffer.addVertex(mat, s * 0.6f, 0, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);
        buffer.addVertex(mat, -s * 0.6f, 0, 0).setColor(r, g, b, alpha).setNormal(0, 0, 1);

        pose.popPose();
    }

    /**
     * Renders a thin vertical stem line from baseY up to the marker.
     */
    public static void renderStem(PoseStack pose, VertexConsumer lineBuffer,
                                   Vec3 camPos, double worldX, double baseY, double worldZ,
                                   float r, float g, float b, float alpha) {
        if (alpha <= 0) return;

        double cx = worldX + 0.5 - camPos.x;
        double cy = baseY - camPos.y;
        double cz = worldZ + 0.5 - camPos.z;

        var mat = pose.last();
        lineBuffer.addVertex(mat, (float) cx, (float) cy, (float) cz)
                .setColor(r, g, b, alpha * 0.5f)
                .setNormal(0, 1, 0);
        lineBuffer.addVertex(mat, (float) cx, (float) (cy + STEM_HEIGHT), (float) cz)
                .setColor(r, g, b, alpha)
                .setNormal(0, 1, 0);
    }

    /**
     * Renders a connecting line between two waypoints (at marker height).
     */
    public static void renderConnection(PoseStack pose, VertexConsumer lineBuffer,
                                         Vec3 camPos,
                                         double x1, double y1, double z1,
                                         double x2, double y2, double z2,
                                         float r, float g, float b, float alpha) {
        if (alpha <= 0) return;

        float mx1 = (float) (x1 + 0.5 - camPos.x);
        float my1 = (float) (y1 + STEM_HEIGHT - camPos.y);
        float mz1 = (float) (z1 + 0.5 - camPos.z);
        float mx2 = (float) (x2 + 0.5 - camPos.x);
        float my2 = (float) (y2 + STEM_HEIGHT - camPos.y);
        float mz2 = (float) (z2 + 0.5 - camPos.z);

        // Direction for normal
        float dx = mx2 - mx1, dy = my2 - my1, dz = mz2 - mz1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;

        var mat = pose.last();
        lineBuffer.addVertex(mat, mx1, my1, mz1).setColor(r, g, b, alpha * 0.6f).setNormal(nx, ny, nz);
        lineBuffer.addVertex(mat, mx2, my2, mz2).setColor(r, g, b, alpha * 0.6f).setNormal(nx, ny, nz);
    }

    /**
     * Renders a numbered text label above the marker position.
     */
    public static void renderLabel(PoseStack pose, MultiBufferSource buffer, Camera camera,
                                    Vec3 camPos, double worldX, double baseY, double worldZ,
                                    String text, float alpha) {
        if (alpha <= 0) return;

        double markerY = baseY + STEM_HEIGHT + MARKER_SIZE + 0.3;

        pose.pushPose();
        pose.translate(worldX + 0.5 - camPos.x, markerY - camPos.y, worldZ + 0.5 - camPos.z);
        pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        pose.scale(-0.025f, -0.025f, -0.025f);

        Font font = Minecraft.getInstance().font;
        float width = -font.width(text) / 2f;
        int color = (((int) (alpha * 255)) << 24) | 0xFFFFFF;
        font.drawInBatch(text, width, 0f, color, true,
                pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 15728880);
        pose.popPose();
    }
}
```

**Step 2: Verify the project compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (the new class is standalone, nothing references it yet)

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/RouteMarkerRenderer.java
git commit -m "feat: add RouteMarkerRenderer helper class for waypoint rendering"
```

---

### Task 2: Add a Translucent Triangle RenderType

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java:53-67` (ModRenderType inner class)

The diamond marker uses triangles, which need a render type that supports translucency and disables depth test (so markers are visible through terrain at short range). We'll use `POSITION_COLOR_NORMAL` format with `TRIANGLES` mode.

**Step 1: Add the MARKER_TRIANGLES render type**

In `ForgeClientEventHandler.ModRenderType`, add after the existing `LINES` field:

```java
public static final RenderType MARKER_TRIANGLES = create(
    "marker_triangles",
    DefaultVertexFormat.POSITION_COLOR_NORMAL,
    VertexFormat.Mode.TRIANGLES,
    256, false, false,
    RenderType.CompositeState.builder()
        .setShaderState(RENDERTYPE_LINES_SHADER)
        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
        .setOutputState(ITEM_ENTITY_TARGET)
        .setWriteMaskState(COLOR_DEPTH_WRITE)
        .setCullState(NO_CULL)
        .createCompositeState(false)
);
```

**Step 2: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: add MARKER_TRIANGLES render type for waypoint diamonds"
```

---

### Task 3: Replace Tug Route Beacon Rendering

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java:136-182` (tug route rendering block)

Replace the `BeaconRenderer.renderBeaconBeam()` call and the existing text label code with calls to `RouteMarkerRenderer`.

**Step 1: Rewrite the tug route rendering block**

Replace the entire `else if (stack.getItem().equals(ModItems.TUG_ROUTE.get()))` block (lines 136-182) with:

```java
} else if (stack.getItem().equals(ModItems.TUG_ROUTE.get())) {
    if (ShippingConfig.Client.DISABLE_TUG_ROUTE_BEACONS.get()) {
        return false;
    }

    var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
    var camPos = camera.getPosition();
    var pose = event.getPoseStack();
    var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    TugRoute route = TugRouteItem.getRoute(stack);
    int routeSize = route.size();

    // Draw connecting lines between consecutive waypoints
    if (routeSize > 1) {
        var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
        for (int i = 0; i < routeSize - 1; i++) {
            TugRouteNode a = route.get(i);
            TugRouteNode b = route.get(i + 1);
            Vec3 midpoint = new Vec3((a.getX() + b.getX()) / 2.0 + 0.5, camPos.y, (a.getZ() + b.getZ()) / 2.0 + 0.5);
            float alpha = RouteMarkerRenderer.computeAlpha(midpoint, camPos);
            RouteMarkerRenderer.renderConnection(pose, lineBuffer, camPos,
                    a.getX(), player.level().getSeaLevel(), a.getZ(),
                    b.getX(), player.level().getSeaLevel(), b.getZ(),
                    1.0f, 0.6f, 0.2f, alpha);
        }
        // Close the loop: last -> first
        TugRouteNode last = route.get(routeSize - 1);
        TugRouteNode first = route.get(0);
        Vec3 midpoint = new Vec3((last.getX() + first.getX()) / 2.0 + 0.5, camPos.y, (last.getZ() + first.getZ()) / 2.0 + 0.5);
        float alpha = RouteMarkerRenderer.computeAlpha(midpoint, camPos);
        RouteMarkerRenderer.renderConnection(pose, lineBuffer, camPos,
                last.getX(), player.level().getSeaLevel(), last.getZ(),
                first.getX(), player.level().getSeaLevel(), first.getZ(),
                1.0f, 0.6f, 0.2f, alpha * 0.4f);
    }

    // Draw markers, stems, and labels
    for (int i = 0; i < routeSize; i++) {
        TugRouteNode node = route.get(i);
        Vec3 nodeWorldPos = new Vec3(node.getX() + 0.5, camPos.y, node.getZ() + 0.5);
        float alpha = RouteMarkerRenderer.computeAlpha(nodeWorldPos, camPos);
        double baseY = player.level().getSeaLevel();

        // Stem line
        RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
                node.getX(), baseY, node.getZ(),
                1.0f, 0.6f, 0.2f, alpha);

        // Diamond marker
        RouteMarkerRenderer.renderMarker(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                node.getX(), baseY, node.getZ(),
                1.0f, 0.6f, 0.2f, alpha);

        // Text label
        RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos,
                node.getX(), baseY, node.getZ(),
                node.getDisplayName(i), alpha);
    }

    buffer.endBatch();
}
```

**Step 2: Remove the unused `BEAM_LOCATION` import and field if no longer needed by loco route**

Don't remove yet — loco route still uses it in this step. We'll remove it in Task 4.

**Step 3: Add the missing import**

Add to the imports at the top of `ForgeClientEventHandler.java`:
```java
import dev.murad.shipping.event.RouteMarkerRenderer;
```
(Note: same package, so this import is optional. But the `Vec3` import and `ByteBufferBuilder` should already be present.)

**Step 4: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: replace tug route beacon beams with floating diamond markers"
```

---

### Task 4: Replace Loco Route Beacon Rendering

**Files:**
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java:79-133` (loco route rendering block)

Keep the rail surface box (it's useful for seeing exactly which rail is a waypoint), but replace the beacon beam with the same diamond marker + stem + label. Loco routes are unordered, so no connecting lines.

**Step 1: Rewrite the loco route beacon section**

Replace the loco route block (the `if (stack.getItem().equals(ModItems.LOCO_ROUTE.get()))` section, lines 79-135) with:

```java
if (stack.getItem().equals(ModItems.LOCO_ROUTE.get())) {
    var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
    var pose = event.getPoseStack();
    var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
    var camPos = camera.getPosition();

    int index = 0;
    for (var node : LocoRouteItem.getRoute(stack)) {
        var block = node.toBlockPos();
        Vec3 nodeWorldPos = new Vec3(block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
        float alpha = RouteMarkerRenderer.computeAlpha(nodeWorldPos, camPos);

        if (alpha > 0) {
            // Diamond marker + stem
            RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
                    block.getX(), block.getY(), block.getZ(),
                    1.0f, 1.0f, 0.3f, alpha);
            RouteMarkerRenderer.renderMarker(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                    block.getX(), block.getY(), block.getZ(),
                    1.0f, 1.0f, 0.3f, alpha);

            // Rail surface box (keep existing behavior)
            pose.pushPose();
            {
                var shape = RailHelper.getRail(block, player.level())
                        .map(pos -> RailHelper.getShape(pos, player.level()))
                        .orElse(RailShape.EAST_WEST);
                double baseY = (shape.isAscending() ? 0.1 : 0);
                double baseX = 0;
                double baseZ = 0;
                var rotation = Axis.ZP.rotationDegrees(0);
                switch (shape) {
                    case ASCENDING_EAST -> {
                        baseX = 0.2;
                        rotation = Axis.ZP.rotationDegrees(45);
                    }
                    case ASCENDING_WEST -> {
                        baseX = 0.1;
                        baseY += 0.7;
                        rotation = Axis.ZP.rotationDegrees(-45);
                    }
                    case ASCENDING_NORTH -> {
                        baseZ = 0.1;
                        baseY += 0.7;
                        rotation = Axis.XP.rotationDegrees(45);
                    }
                    case ASCENDING_SOUTH -> {
                        baseZ = 0.2;
                        rotation = Axis.XP.rotationDegrees(-45);
                    }
                }

                pose.translate(block.getX() + baseX - camPos.x, block.getY() + baseY - camPos.y, block.getZ() + baseZ - camPos.z);
                pose.mulPose(rotation);

                AABB a = new AABB(0, 0, 0, 1, 0.2, 1);
                LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, 1.0f, 1.0f, 0.3f, 0.5f * alpha);
            }
            pose.popPose();

            // Label (show node name or index)
            String label = node.hasCustomName() ? node.getName() : String.valueOf(index + 1);
            RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos,
                    block.getX(), block.getY(), block.getZ(),
                    label, alpha);
        }
        index++;
    }

    buffer.endBatch();
}
```

**Step 2: Remove `BEAM_LOCATION` field and `BeaconRenderer` import**

Since neither tug nor loco routes use beacon rendering anymore:
- Delete `ForgeClientEventHandler.java:51`: `public static final ResourceLocation BEAM_LOCATION = ...`
- Delete the import: `import net.minecraft.client.renderer.blockentity.BeaconRenderer;`
- Delete the import: `import net.minecraft.world.item.DyeColor;`

**Step 3: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java
git commit -m "feat: replace loco route beacon beams with floating diamond markers"
```

---

### Task 5: Rename Config Option and Clean Up

**Files:**
- Modify: `src/main/java/dev/murad/shipping/ShippingConfig.java:30,42-44`
- Modify: `src/main/java/dev/murad/shipping/event/ForgeClientEventHandler.java` (reference to config)

The config option `DISABLE_TUG_ROUTE_BEACONS` / `disableTugRouteBeacons` still references "beacons". Rename it to be marker-agnostic.

**Step 1: Rename the config field and key**

In `ShippingConfig.java`, change:
```java
public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_TUG_ROUTE_BEACONS;
```
to:
```java
public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_ROUTE_MARKERS;
```

And change the definition:
```java
DISABLE_ROUTE_MARKERS =
    BUILDER.comment("Disable in-world route waypoint markers when holding a route item. Default false.")
            .define("disableRouteMarkers", false);
```

**Step 2: Update the reference in ForgeClientEventHandler**

Change `ShippingConfig.Client.DISABLE_TUG_ROUTE_BEACONS.get()` to `ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()`.

Also apply this check to the loco route block too (it was previously missing — beacons were always shown for loco routes with no way to disable).

**Step 3: Delete the old beacon_beam.png texture**

Delete: `src/main/resources/assets/littlelogistics/textures/entity/beacon_beam.png`

**Step 4: Verify compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "chore: rename config to DISABLE_ROUTE_MARKERS, remove beacon_beam.png"
```

---

### Task 6: Visual Testing

**Step 1: Launch the client and test tug route markers**

Run: `./gradlew runClient`

Test checklist:
- [ ] Hold a tug route item with waypoints — see diamond markers at sea level, not beacon beams
- [ ] Markers have orange color with numbered labels
- [ ] Connecting lines visible between consecutive waypoints
- [ ] Loop line (last->first) is dimmer than segment lines
- [ ] Walking far away (>96 blocks) causes markers to fade
- [ ] Walking >128 blocks away causes markers to disappear entirely
- [ ] Config `disableRouteMarkers=true` hides tug markers

**Step 2: Test loco route markers**

- [ ] Hold a loco route item with waypoints on rails — see diamond markers above rails
- [ ] Yellow rail surface boxes still render on the rail blocks
- [ ] Labels show node name or index number
- [ ] Distance fade works
- [ ] Config `disableRouteMarkers=true` hides loco markers

**Step 3: Verify no rendering artifacts**

- [ ] No z-fighting or flickering
- [ ] Markers face camera correctly from all angles
- [ ] No crash when route is empty
- [ ] No crash when holding route in off-hand

**Step 4: Commit any fixes from testing**

```bash
git add -A
git commit -m "fix: visual testing adjustments for route markers"
```

---

## Notes

- **Color choices**: Tug = orange (1.0, 0.6, 0.2), Loco = yellow (1.0, 1.0, 0.3) — consistent with the old beacon colors but as RGB values.
- **MARKER_TRIANGLES shader**: Uses `RENDERTYPE_LINES_SHADER` which may not work for triangles. If rendering is broken, switch to `POSITION_COLOR_SHADER` instead. This is the most likely thing to need adjustment during visual testing.
- **Stem height**: 3 blocks is a good starting point. May need tuning — too short and markers are hidden by terrain, too tall and they look like mini-beacons.
- **Tug route baseY**: Uses `player.level().getSeaLevel()` as the anchor since tug waypoints are 2D (XZ only). This means markers float at sea level, which makes sense for water routes.
- **LocoRoute is a HashSet**: The iteration order is not guaranteed, so we don't draw connecting lines. If ordering is added later, connecting lines can be added trivially.
