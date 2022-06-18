package dev.murad.shipping.event;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.portal.AdvancedTrainPortalBlock;
import dev.murad.shipping.block.portal.IPortalBlock;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.item.PortalLinkerItem;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Forge-wide event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/beacon_beam.png");

    public static class ModRenderType extends RenderType {
        public static final RenderType LINES = create("lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL).createCompositeState(false));

        public ModRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelLastEvent event) {
        Player player = Minecraft.getInstance().player;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);


        handleLocoRoute(event, player, stack);
        handleTugRoute(event, player, stack);
        handlePortalLinker(event, player, stack);
    }

    private static void handlePortalLinker(RenderLevelLastEvent event, Player player, ItemStack stack) {
        if (stack.getItem().equals(ModItems.TRAIN_PORTAL_LINKER.get()) && PortalLinkerItem.getState(stack) == PortalLinkerItem.State.WAITING_NEXT){
            PoseStack pose = event.getPoseStack();
            Vec3 cameraOff = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            BlockPos savedPos = PortalLinkerItem.getTargetPos(stack);
            var dimension = PortalLinkerItem.getDimension(stack);
            IPortalBlock type = (IPortalBlock) PortalLinkerItem.getType(stack);


            if(player.getLevel().dimension().equals(dimension) && savedPos != null){
                // render selected
                pose.pushPose();

                pose.translate(savedPos.getX() - cameraOff.x, savedPos.getY() - cameraOff.y, savedPos.getZ() - cameraOff.z);
                AABB a = new AABB(0, 0, 0, 1, 1, 1);
                LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, 50, 0, 150, 1);
                pose.popPose();

                if(!ShippingConfig.Server.ALLOW_ADVANCED_PORTAL_WITHIN_DIMENSION.get() || !(type instanceof AdvancedTrainPortalBlock)){
                    buffer.endBatch();
                    return;
                }
            }


            if(ShippingConfig.Server.DISABLE_ADVANCED_PORTAL_RANGE_CHECK.get() && (type instanceof AdvancedTrainPortalBlock)){
                buffer.endBatch();
                return;
            }

            if (type != null && type.validDims().contains(player.level.dimension())){
                // render border
                double thisScale =player.level.dimensionType().coordinateScale();
                var transposed = CrossDimensionalUtil.getPosInDimension(PortalLinkerItem.getTargetScale(stack), thisScale, savedPos);

                int rad = type.linkRadius(player.level.dimensionType());
                for (double x = -1; x <= 1; x+= thisScale / 4) {
                    for (double z = -1; z <= 1; z += thisScale / 4) {
                        if (Math.abs(x) != 1 && Math.abs(z) != 1) {
                            continue;
                        }
                        BlockPos bp = new BlockPos(transposed.getX() + (rad*x), 0, transposed.getZ() + (rad*z));
                        pose.pushPose();
                        pose.translate(bp.getX() - cameraOff.x, 1 - cameraOff.y, bp.getZ() - cameraOff.z);
                        BeaconRenderer.renderBeaconBeam(pose, buffer, BEAM_LOCATION, event.getPartialTick(),
                                1F, player.level.getGameTime(), player.level.getMinBuildHeight() + 1, 1024,
                                DyeColor.PURPLE.getTextureDiffuseColors(), 0.2F, 0.25F);

                        pose.popPose();
                    }
                }
            }

            buffer.endBatch();
        }
    }


    private static void handleTugRoute(RenderLevelLastEvent event, Player player, ItemStack stack) {
        if (stack.getItem().equals(ModItems.TUG_ROUTE.get())){
            if(ShippingConfig.Client.DISABLE_TUG_ROUTE_BEACONS.get()){
                return;
            }
            Vec3 vector3d = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            double d0 = vector3d.x();
            double d1 = vector3d.y();
            double d2 = vector3d.z();
            MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            List<TugRouteNode> route = TugRouteItem.getRoute(stack);
            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                TugRouteNode node = route.get(i);
                PoseStack matrixStack = event.getPoseStack();

                matrixStack.pushPose();
                matrixStack.translate(node.getX() - d0, 1 - d1, node.getZ() - d2);

                BeaconRenderer.renderBeaconBeam(matrixStack, renderTypeBuffer, BEAM_LOCATION, event.getPartialTick(),
                        1F, player.level.getGameTime(), player.level.getMinBuildHeight(), 1024,
                        DyeColor.RED.getTextureDiffuseColors(), 0.2F, 0.25F);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(node.getX() - d0 , player.getY() + 2 - d1, node.getZ() - d2 );
                matrixStack.scale(-0.025F, -0.025F, -0.025F);

                matrixStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

                Matrix4f matrix4f = matrixStack.last().pose();

                Font fontRenderer = Minecraft.getInstance().font;
                String text = node.getDisplayName(i);
                float width = (-fontRenderer.width(text) / (float) 2);
                fontRenderer.drawInBatch(text, width, 0.0F, -1, true, matrix4f, renderTypeBuffer, true, 0, 15728880);
                matrixStack.popPose();

            }
            renderTypeBuffer.endBatch();
        }
    }

    private static void handleLocoRoute(RenderLevelLastEvent event, Player player, ItemStack stack) {
        if (stack.getItem().equals(ModItems.LOCO_ROUTE.get())) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            PoseStack pose = event.getPoseStack();
            Vec3 cameraOff = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            LocoRoutes route = LocoRouteItem.getRoute(stack, player.level);
            var list = route.getNodesForDimension(player.level)
                    .stream().map(LocoRouteNode::toBlockPos).toList();
            for (BlockPos block : list) {
                pose.pushPose();
                pose.translate(block.getX() - cameraOff.x, 1 - cameraOff.y, block.getZ() - cameraOff.z);
                BeaconRenderer.renderBeaconBeam(pose, buffer, BEAM_LOCATION, event.getPartialTick(),
                        1F, player.level.getGameTime(), player.level.getMinBuildHeight() + 1, 1024,
                        DyeColor.RED.getTextureDiffuseColors(), 0F, 0.25F);

                pose.popPose();
            }

            for (BlockPos block : list) {
                pose.pushPose();
                // handling for removed blocks and blocks out of distance
                var shape = RailHelper.getRail(block, player.level)
                        .map(pos -> RailHelper.getShape(pos, player.level))
                        .orElse(RailShape.EAST_WEST);
                double baseY = (shape.getName().contains("ascending") ? 0.1 : 0);
                double baseX = 0;
                double baseZ = 0;
                Runnable mulPose = () -> {};
                switch (shape){
                    case ASCENDING_EAST -> {
                        baseX = 0.2;
                        mulPose = () -> pose.mulPose(Vector3f.ZP.rotationDegrees(45));
                    }
                    case ASCENDING_WEST -> {
                        baseX = 0.1;
                        baseY += 0.7;
                        mulPose = (() -> pose.mulPose(Vector3f.ZP.rotationDegrees(-45)));

                    }
                    case ASCENDING_NORTH -> {
                        baseZ = 0.1;
                        baseY += 0.7;
                        mulPose = () -> pose.mulPose(Vector3f.XP.rotationDegrees(45));
                    }
                    case ASCENDING_SOUTH -> {
                        baseZ = 0.2;
                        mulPose = () -> pose.mulPose(Vector3f.XP.rotationDegrees(-45));
                    }
                }

                pose.translate(block.getX() + baseX - cameraOff.x, block.getY() + baseY - cameraOff.y, block.getZ() + baseZ - cameraOff.z);
                AABB a = new AABB(0, 0, 0, 1, 0.2, 1).deflate(0.2, 0, 0.2);
                mulPose.run();
                LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, 1.0f, 0.3f, 0.3f, 0.5f);
                pose.popPose();

            }



            buffer.endBatch();
        }
    }
}
