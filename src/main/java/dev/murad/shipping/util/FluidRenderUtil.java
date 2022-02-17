package dev.murad.shipping.util;

/*
    Adapted from https://github.com/tigres810/TestMod-1.16.4

    Copyright (c) 2020 tigres810

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

public class FluidRenderUtil {

    private static void addQuadVertex(Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer, Vector3f pos, Vec2 texUV, Vector3f normalVector, int color, int lightmapValue) {
        float a = 1.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        renderBuffer.vertex(matrixPos, pos.x(), pos.y(), pos.z()) // position coordinate
                .color(r, g, b, a)        // color
                .uv(texUV.x, texUV.y)                     // texel coordinate
                .overlayCoords(OverlayTexture.NO_OVERLAY)  // only relevant for rendering Entities (Living)
                .uv2(0, 240)             // lightmap with full brightness
                .normal(matrixNormal, normalVector.x(), normalVector.y(), normalVector.z())
                .endVertex();
    }

    private static void addQuad(Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer, Vector3f blpos, Vector3f brpos, Vector3f trpos, Vector3f tlpos, Vec2 blUVpos, Vec2 brUVpos, Vec2 trUVpos, Vec2 tlUVpos, Vector3f normalVector, int color, int lightmapValue) {
        addQuadVertex(matrixPos, matrixNormal, renderBuffer, blpos, blUVpos, normalVector, color, lightmapValue);
        addQuadVertex(matrixPos, matrixNormal, renderBuffer, brpos, brUVpos, normalVector, color, lightmapValue);
        addQuadVertex(matrixPos, matrixNormal, renderBuffer, trpos, trUVpos, normalVector, color, lightmapValue);
        addQuadVertex(matrixPos, matrixNormal, renderBuffer, tlpos, tlUVpos, normalVector, color, lightmapValue);
    }

    private static void addFace(Direction whichFace, Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer, int color, Vec3 centrePos, float width, float height, Vec2 bottomLeftUV, float texUwidth, float texVheight, int lightmapValue) {
        // the Direction class has a bunch of methods which can help you rotate quads
        //  I've written the calculations out long hand, and based them on a centre position, to make it clearer what
        //   is going on.
        // Beware that the Direction class is based on which direction the face is pointing, which is opposite to
        //   the direction that the viewer is facing when looking at the face.
        // Eg when drawing the NORTH face, the face points north, but when we're looking at the face, we are facing south,
        //   so that the bottom left corner is the eastern-most, not the western-most!


        // calculate the bottom left, bottom right, top right, top left vertices from the VIEWER's point of view (not the
        //  face's point of view)

        Vector3f leftToRightDirection, bottomToTopDirection;

        switch (whichFace) {
            case NORTH: { // bottom left is east
                leftToRightDirection = new Vector3f(-1, 0, 0);  // or alternatively Vector3f.XN
                bottomToTopDirection = new Vector3f(0, 1, 0);  // or alternatively Vector3f.YP
                break;
            }
            case SOUTH: {  // bottom left is west
                leftToRightDirection = new Vector3f(1, 0, 0);
                bottomToTopDirection = new Vector3f(0, 1, 0);
                break;
            }
            case EAST: {  // bottom left is south
                leftToRightDirection = new Vector3f(0, 0, -1);
                bottomToTopDirection = new Vector3f(0, 1, 0);
                break;
            }
            case WEST: { // bottom left is north
                leftToRightDirection = new Vector3f(0, 0, 1);
                bottomToTopDirection = new Vector3f(0, 1, 0);
                break;
            }
            case UP: { // bottom left is southwest by minecraft block convention
                leftToRightDirection = new Vector3f(-1, 0, 0);
                bottomToTopDirection = new Vector3f(0, 0, 1);
                break;
            }
            case DOWN: { // bottom left is northwest by minecraft block convention
                leftToRightDirection = new Vector3f(1, 0, 0);
                bottomToTopDirection = new Vector3f(0, 0, 1);
                break;
            }
            default: {  // should never get here, but just in case;
                leftToRightDirection = new Vector3f(0, 0, 1);
                bottomToTopDirection = new Vector3f(0, 1, 0);
                break;
            }
        }
        leftToRightDirection.mul(0.5F * width);  // convert to half width
        bottomToTopDirection.mul(0.5F * height);  // convert to half height

        // calculate the four vertices based on the centre of the face

        Vector3f bottomLeftPos = new Vector3f(centrePos);
        bottomLeftPos.sub(leftToRightDirection);
        bottomLeftPos.sub(bottomToTopDirection);

        Vector3f bottomRightPos = new Vector3f(centrePos);
        bottomRightPos.add(leftToRightDirection);
        bottomRightPos.sub(bottomToTopDirection);

        Vector3f topRightPos = new Vector3f(centrePos);
        topRightPos.add(leftToRightDirection);
        topRightPos.add(bottomToTopDirection);

        Vector3f topLeftPos = new Vector3f(centrePos);
        topLeftPos.sub(leftToRightDirection);
        topLeftPos.add(bottomToTopDirection);

        // texture coordinates are "upside down" relative to the face
        // eg bottom left = [U min, V max]
        Vec2 bottomLeftUVpos = new Vec2(bottomLeftUV.x, bottomLeftUV.y);
        Vec2 bottomRightUVpos = new Vec2(bottomLeftUV.x + texUwidth, bottomLeftUV.y);
        Vec2 topLeftUVpos = new Vec2(bottomLeftUV.x + texUwidth, bottomLeftUV.y + texVheight);
        Vec2 topRightUVpos = new Vec2(bottomLeftUV.x, bottomLeftUV.y + texVheight);

        Vector3f normalVector = whichFace.step();  // gives us the normal to the face

        addQuad(matrixPos, matrixNormal, renderBuffer,
                bottomLeftPos, bottomRightPos, topRightPos, topLeftPos,
                bottomLeftUVpos, bottomRightUVpos, topLeftUVpos, topRightUVpos,
                normalVector, color, lightmapValue);
    }

    public static void renderCubeUsingQuads(int capacity, FluidStack fluid, float partialTicks, PoseStack PoseStack, MultiBufferSource renderBuffers, int combinedLight, int combinedOverlay) {
        // draw the object as a cube, using quad
        // When render method is called, the origin [0,0,0] is at the current [x,y,z] of the block.

        // The cube-drawing method draws the cube in the region from [0,0,0] to [1,1,1] but we want it
        //   to be in the block one above this, i.e. from [0,1,0] to [1,2,1],
        //   so we need to translate up by one block, i.e. by [0,1,0]

        PoseStack.pushPose(); // push the current transformation matrix + normals matrix

        drawCubeQuads(PoseStack, renderBuffers, combinedLight, fluid, capacity);
        PoseStack.popPose(); // restore the original transformation matrix + normals matrix
    }

    /**
     * Draw a cube from [0,0,0] to [1,1,1], same texture on all sides, using a supplied texture
     */
    private static void drawCubeQuads(PoseStack PoseStack, MultiBufferSource renderBuffer, int combinedLight, FluidStack fluid, int capacity) {
        // other typical RenderTypes used by TER are:
        // getEntityCutout, getBeaconBeam (which has translucent),
        FluidAttributes attributes = fluid.getFluid().getAttributes();
        ResourceLocation fluidStill = attributes.getStillTexture(fluid);

        if(fluidStill == null){
            return;
        }
//        VertexConsumer vertexBuilderBlockQuads = renderBuffer.getBuffer(RenderType.entityTranslucent(new ResourceLocation("minecraft:textures/block/lava_still.png")));
        VertexConsumer vertexBuilderBlockQuads = renderBuffer.getBuffer(RenderType.translucent());

        int color = attributes.getColor();
        Matrix4f matrixPos = PoseStack.last().pose();     // retrieves the current transformation matrix
        Matrix3f matrixNormal = PoseStack.last().normal();  // retrieves the current transformation matrix for the normal vector
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(fluidStill);

        // we use the whole texture
        Vec2 bottomLeftUV = new Vec2(sprite.getU0(), sprite.getV0());
        float UVwidth = sprite.getU1() - sprite.getU0();
        float UVheight = sprite.getV1() - sprite.getV0();

        // all faces have the same height and width
        final float WIDTH = 1.0F;
        final float HEIGHT = 1.0F;

        float scale = (1.0f - WIDTH/2 - WIDTH) * fluid.getAmount() / capacity;

        if(scale <= 0) { PoseStack.scale(.5f, Math.abs(scale) + .21f, .5f); }

        final Vec3 EAST_FACE_MIDPOINT = new Vec3(1.0, 0.5, 0.5);
        final Vec3 WEST_FACE_MIDPOINT = new Vec3(0.0, 0.5, 0.5);
        final Vec3 NORTH_FACE_MIDPOINT = new Vec3(0.5, 0.5, 0.0);
        final Vec3 SOUTH_FACE_MIDPOINT = new Vec3(0.5, 0.5, 1.0);
        final Vec3 UP_FACE_MIDPOINT = new Vec3(0.5, 1.0, 0.5);
        final Vec3 DOWN_FACE_MIDPOINT = new Vec3(0.5, 0.0, 0.5);

        addFace(Direction.EAST, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, EAST_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
        addFace(Direction.WEST, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, WEST_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
        addFace(Direction.NORTH, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, NORTH_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
        addFace(Direction.SOUTH, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, SOUTH_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
        addFace(Direction.UP, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, UP_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
        addFace(Direction.DOWN, matrixPos, matrixNormal, vertexBuilderBlockQuads,
                color, DOWN_FACE_MIDPOINT, WIDTH, HEIGHT, bottomLeftUV, UVwidth, UVheight, combinedLight);
    }

}
