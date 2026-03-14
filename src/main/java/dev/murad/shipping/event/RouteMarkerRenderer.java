package dev.murad.shipping.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Low-level rendering helpers for route waypoint markers.
 * Renders diamond-shaped billboards with vertical stems and connecting lines.
 */
public class RouteMarkerRenderer {

    /** Height of the vertical stem above the base Y position. */
    public static final float STEM_HEIGHT = 3.0f;

    /** Half-size of the diamond billboard. */
    public static final float MARKER_SIZE = 0.35f;

    /** Distance beyond which markers are fully invisible. */
    private static final double FADE_FAR = 128.0;

    /** Distance at which markers begin to fade out. */
    private static final double FADE_NEAR = 96.0;

    /**
     * Computes the opacity for a marker based on camera distance.
     * Returns 0 if beyond {@link #FADE_FAR} blocks, fades linearly from 1.0 to 0.0
     * between {@link #FADE_NEAR} and {@link #FADE_FAR} blocks.
     */
    public static float computeAlpha(Vec3 markerPos, Vec3 camPos) {
        double dist = markerPos.distanceTo(camPos);
        if (dist > FADE_FAR) {
            return 0.0f;
        }
        if (dist <= FADE_NEAR) {
            return 1.0f;
        }
        return (float) (1.0 - (dist - FADE_NEAR) / (FADE_FAR - FADE_NEAR));
    }

    /**
     * Renders a small camera-facing diamond billboard (2 triangles forming a rhombus)
     * at {@link #STEM_HEIGHT} blocks above baseY.
     *
     * @param poseStack   the current pose stack
     * @param buffer      vertex consumer for {@link ForgeClientEventHandler.ModRenderType#MARKER_TRIANGLES}
     * @param camera      the render camera
     * @param camPos      camera world position
     * @param worldX      marker world X
     * @param baseY       marker base world Y
     * @param worldZ      marker world Z
     * @param r           red component (0-1)
     * @param g           green component (0-1)
     * @param b           blue component (0-1)
     * @param alpha       opacity (0-1)
     */
    public static void renderMarker(PoseStack poseStack, VertexConsumer buffer, Camera camera,
                                     Vec3 camPos, double worldX, double baseY, double worldZ,
                                     float r, float g, float b, float alpha) {
        if (alpha <= 0.0f) return;

        double markerY = baseY + STEM_HEIGHT;

        poseStack.pushPose();
        {
            // Translate to marker position relative to camera
            poseStack.translate(worldX - camPos.x, markerY - camPos.y, worldZ - camPos.z);

            // Face the camera (billboard)
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

            Matrix4f mat = poseStack.last().pose();
            float s = MARKER_SIZE;

            // Diamond shape: 4 points — top, right, bottom, left
            // Top triangle: left -> top -> right
            buffer.addVertex(mat, -s, 0, 0).setColor(r, g, b, alpha);
            buffer.addVertex(mat, 0, s, 0).setColor(r, g, b, alpha);
            buffer.addVertex(mat, s, 0, 0).setColor(r, g, b, alpha);

            // Bottom triangle: left -> right -> bottom
            buffer.addVertex(mat, -s, 0, 0).setColor(r, g, b, alpha);
            buffer.addVertex(mat, s, 0, 0).setColor(r, g, b, alpha);
            buffer.addVertex(mat, 0, -s, 0).setColor(r, g, b, alpha);
        }
        poseStack.popPose();
    }

    /**
     * Renders a vertical line (stem) from baseY up to baseY + {@link #STEM_HEIGHT}.
     *
     * @param poseStack  the current pose stack
     * @param lineBuffer vertex consumer for {@link ForgeClientEventHandler.ModRenderType#LINES}
     * @param camPos     camera world position
     * @param worldX     marker world X
     * @param baseY      marker base world Y
     * @param worldZ     marker world Z
     * @param r          red component (0-1)
     * @param g          green component (0-1)
     * @param b          blue component (0-1)
     * @param alpha      opacity (0-1)
     */
    public static void renderStem(PoseStack poseStack, VertexConsumer lineBuffer,
                                   Vec3 camPos, double worldX, double baseY, double worldZ,
                                   float r, float g, float b, float alpha) {
        if (alpha <= 0.0f) return;

        poseStack.pushPose();
        {
            poseStack.translate(worldX - camPos.x, baseY - camPos.y, worldZ - camPos.z);

            Matrix4f mat = poseStack.last().pose();

            // Vertical line from (0, 0, 0) to (0, STEM_HEIGHT, 0)
            // LINES mode requires normal for line direction
            lineBuffer.addVertex(mat, 0, 0, 0).setColor(r, g, b, alpha).setNormal(0, 1, 0);
            lineBuffer.addVertex(mat, 0, STEM_HEIGHT, 0).setColor(r, g, b, alpha).setNormal(0, 1, 0);
        }
        poseStack.popPose();
    }

    /**
     * Renders a line connecting two waypoints at marker height ({@link #STEM_HEIGHT} above base).
     *
     * @param poseStack  the current pose stack
     * @param lineBuffer vertex consumer for {@link ForgeClientEventHandler.ModRenderType#LINES}
     * @param camPos     camera world position
     * @param x1         first waypoint world X
     * @param y1         first waypoint base Y
     * @param z1         first waypoint world Z
     * @param x2         second waypoint world X
     * @param y2         second waypoint base Y
     * @param z2         second waypoint world Z
     * @param r          red component (0-1)
     * @param g          green component (0-1)
     * @param b          blue component (0-1)
     * @param alpha      opacity (0-1)
     */
    public static void renderConnection(PoseStack poseStack, VertexConsumer lineBuffer,
                                         Vec3 camPos, double x1, double y1, double z1,
                                         double x2, double y2, double z2,
                                         float r, float g, float b, float alpha) {
        if (alpha <= 0.0f) return;

        double markerY1 = y1 + STEM_HEIGHT;
        double markerY2 = y2 + STEM_HEIGHT;

        poseStack.pushPose();
        {
            // Translate relative to camera
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            Matrix4f mat = poseStack.last().pose();

            // Compute direction for normal
            float dx = (float) (x2 - x1);
            float dy = (float) (markerY2 - markerY1);
            float dz = (float) (z2 - z1);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6f) {
                poseStack.popPose();
                return;
            }
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;

            lineBuffer.addVertex(mat, (float) x1, (float) markerY1, (float) z1)
                    .setColor(r, g, b, alpha).setNormal(nx, ny, nz);
            lineBuffer.addVertex(mat, (float) x2, (float) markerY2, (float) z2)
                    .setColor(r, g, b, alpha).setNormal(nx, ny, nz);
        }
        poseStack.popPose();
    }

    /**
     * Renders camera-facing text above the marker.
     *
     * @param poseStack    the current pose stack
     * @param bufferSource buffer source for text rendering
     * @param camera       the render camera
     * @param camPos       camera world position
     * @param worldX       marker world X
     * @param baseY        marker base world Y
     * @param worldZ       marker world Z
     * @param text         the text string to display
     * @param alpha        opacity (0-1)
     */
    public static void renderLabel(PoseStack poseStack, MultiBufferSource bufferSource,
                                    Camera camera, Vec3 camPos,
                                    double worldX, double baseY, double worldZ,
                                    String text, float alpha) {
        if (alpha <= 0.0f) return;

        // Position text slightly above the diamond marker
        double labelY = baseY + STEM_HEIGHT + MARKER_SIZE + 0.15;

        poseStack.pushPose();
        {
            poseStack.translate(worldX - camPos.x, labelY - camPos.y, worldZ - camPos.z);

            // Face the camera
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

            // Scale down for world-space text
            poseStack.scale(-0.025F, -0.025F, -0.025F);

            Font font = Minecraft.getInstance().font;
            float halfWidth = -font.width(text) / 2.0f;
            int textAlpha = ((int) (alpha * 255.0f)) << 24 | 0x00FFFFFF;
            font.drawInBatch(text, halfWidth, 0.0F, textAlpha, true,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
        }
        poseStack.popPose();
    }
}
