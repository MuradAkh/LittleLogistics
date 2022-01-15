package dev.murad.shipping.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEventHandler {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;
        ItemStack istack = player.getItemInHand(Hand.MAIN_HAND);
        if (istack.getItem().equals(ModItems.TUG_ROUTE.get())){
            Vector3d vector3d = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            double d0 = vector3d.x();
            double d1 = vector3d.y();
            double d2 = vector3d.z();
            IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            for (Vector2f v : TugRouteItem.getRoute(istack)) {
                MatrixStack stack = event.getMatrixStack();
                stack.pushPose();
                stack.translate(v.x - d0, 1 - d1, v.y - d2);
                float rotation = (float) Math.floorMod(player.level.getGameTime(), 40L) + event.getPartialTicks();
                stack.mulPose(Vector3f.YP.rotationDegrees(rotation * 2.25F - 45.0F));
                stack.mulPose(Vector3f.XP.rotationDegrees(180));
                WayPointRenderer.renderPart(stack, renderTypeBuffer.getBuffer(WayPointRenderer.createRenderType()), 1, 1, 1, 1, 10, 0.0F, 1, 1, 0.0F, -1, 0.0F, 0.0F, -1);
                stack.mulPose(Vector3f.XP.rotationDegrees(-180));
                WayPointRenderer.renderPart(stack, renderTypeBuffer.getBuffer(WayPointRenderer.createRenderType()), 1, 1,1, 1, 10, 0.0F, 1, 1, 0.0F, -1, 0.0F, 0.0F, -1);
                stack.popPose();

            }
        }
    }

    private static class WayPointRenderer extends RenderState {

        public WayPointRenderer(String p_i225973_1_, Runnable p_i225973_2_, Runnable p_i225973_3_) {
            super(p_i225973_1_, p_i225973_2_, p_i225973_3_);
        }

        public static void renderPart(MatrixStack stack, IVertexBuilder builder, float red, float green, float blue, float alpha, float height, float radius_1, float radius_2, float radius_3, float radius_4, float radius_5, float radius_6, float radius_7, float radius_8) {
            MatrixStack.Entry matrixentry = stack.last();
            Matrix4f matrixpose = matrixentry.pose();
            Matrix3f matrixnormal = matrixentry.normal();
            renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_1, radius_2, radius_3, radius_4);
            renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_7, radius_8, radius_5, radius_6);
            renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_3, radius_4, radius_7, radius_8);
            renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_5, radius_6, radius_1, radius_2);
        }

        private static void renderQuad(Matrix4f pose, Matrix3f normal, IVertexBuilder builder, float red, float green, float blue, float alpha, float y, float z1, float texu1, float z, float texu) {
            addVertex(pose, normal, builder, red, green, blue, alpha, y, z1, texu1, 1f, 0f);
            addVertex(pose, normal, builder, red, green, blue, alpha, 0f, z1, texu1, 1f, 1f);
            addVertex(pose, normal, builder, red, green, blue, alpha, 0f, z, texu, 0f, 1f);
            addVertex(pose, normal, builder, red, green, blue, alpha, y, z, texu, 0f, 0f);
        }

        private static void addVertex(Matrix4f pose, Matrix3f normal, IVertexBuilder builder, float red, float green, float blue, float alpha, float y, float x, float z, float texu, float texv) {
            builder.vertex(pose, x, y, z).color(red, green, blue, alpha).uv(texu, texv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        }

        public static RenderType createRenderType() {
            RenderType.State state = RenderType.State.builder().setTextureState(new RenderState.TextureState(BEAM_LOCATION, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderState.COLOR_WRITE)
                    .setFogState(NO_FOG).createCompositeState(false);
            return RenderType.create("loot_beam", DefaultVertexFormats.BLOCK, 7, 256, false, true, state);
        }
    }


}
