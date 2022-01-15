package dev.murad.shipping.event;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.BeaconTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEventHandler {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.getItemInHand(Hand.MAIN_HAND);
        if (stack.getItem().equals(ModItems.TUG_ROUTE.get())){
            Vector3d vector3d = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            double d0 = vector3d.x();
            double d1 = vector3d.y();
            double d2 = vector3d.z();
            IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            for (Vector2f v : TugRouteItem.getRoute(stack)) {
                BeaconTileEntity beaconTile = new BeaconTileEntity(){
                    @Override
                    public List<BeamSegment> getBeamSections() {
                        return ImmutableList.of(new BeamSegment(DyeColor.CYAN.getTextureDiffuseColors()));
                    }
                };
                beaconTile.setLevelAndPosition(player.level, new BlockPos(v.x, 10, v.y));
                MatrixStack stack1 = event.getMatrixStack();
                stack1.pushPose();
                stack1.translate(v.x - d0 - 1, 1 - d1, v.y - d2);
                setupAndRender(TileEntityRendererDispatcher.instance.getRenderer(beaconTile), beaconTile, event.getPartialTicks(),stack1 , renderTypeBuffer);
                stack1.popPose();
            }
        }
    }

    private static <T extends TileEntity> void setupAndRender(TileEntityRenderer<T> p_228855_0_, T p_228855_1_, float p_228855_2_, MatrixStack p_228855_3_, IRenderTypeBuffer p_228855_4_) {
        World world = p_228855_1_.getLevel();
        int i;
        if (world != null) {
            i = WorldRenderer.getLightColor(world, p_228855_1_.getBlockPos());
        } else {
            i = 15728880;
        }

        p_228855_0_.render(p_228855_1_, p_228855_2_, p_228855_3_, p_228855_4_, i, OverlayTexture.NO_OVERLAY);
    }
}
