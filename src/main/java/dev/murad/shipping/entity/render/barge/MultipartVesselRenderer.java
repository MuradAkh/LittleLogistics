package dev.murad.shipping.entity.render.barge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import lombok.Getter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class MultipartVesselRenderer<T extends AbstractBargeEntity> extends AbstractVesselRenderer<T> {

    @Getter
    private final EntityModel<T> baseModel, insertModel, trimModel;

    @Getter
    private final ResourceLocation baseTextureLocation, insertTextureLocation, trimTextureLocation;

    private MultipartVesselRenderer(EntityRendererProvider.Context context,
                                   ModelSupplier<T> baseModelSupplier,
                                   ModelLayerLocation baseModelLocation,
                                   ResourceLocation baseTexture,
                                    ModelSupplier<T> insertModelSupplier,
                                    ModelLayerLocation insertModelLocation,
                                    ResourceLocation insertTexture,
                                    ModelSupplier<T> trimModelSupplier,
                                    ModelLayerLocation trimModelLocation,
                                    ResourceLocation trimTexture) {
        super(context);
        this.baseModel = baseModelSupplier.supply(context.bakeLayer(baseModelLocation));
        this.baseTextureLocation = baseTexture;

        this.insertModel = insertModelSupplier.supply(context.bakeLayer(insertModelLocation));
        this.insertTextureLocation = insertTexture;

        this.trimModel = trimModelSupplier.supply(context.bakeLayer(trimModelLocation));
        this.trimTextureLocation = trimTexture;
    }

    /**
     * Don't directly use this method, use the multipart methods instead
     */
    @Override
    @Deprecated
    EntityModel getModel(T entity) {
        return baseModel;
    }

    /**
     * Don't directly use this method, use the multipart methods instead
     */
    @Override
    @Deprecated
    public ResourceLocation getTextureLocation(T pEntity) {
        return baseTextureLocation;
    }

    @Override
    protected void renderModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        int overlay = LivingEntityRenderer.getOverlayCoords(vesselEntity, 0);

        var colorId = vesselEntity.getColor();
        var color = (colorId == null ? DyeColor.RED : DyeColor.byId(colorId)).getTextureDiffuseColors();

        baseModel.renderToBuffer(matrixStack,
                buffer.getBuffer(baseModel.renderType(baseTextureLocation)),
                packedLight, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);

        insertModel.renderToBuffer(matrixStack,
                buffer.getBuffer(insertModel.renderType(insertTextureLocation)),
                packedLight, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);

        trimModel.renderToBuffer(matrixStack,
                buffer.getBuffer(trimModel.renderType(trimTextureLocation)),
                packedLight, overlay,
                color[0], color[1], color[2], 1.0F);
    }

    @FunctionalInterface
    public interface ModelSupplier<T extends VesselEntity> {
        EntityModel<T> supply(ModelPart root);
    }

    public static class Builder<T extends AbstractBargeEntity> {
        private final EntityRendererProvider.Context context;

        private ModelSupplier<T> baseModelSupplier;
        private ModelLayerLocation baseModelLocation;
        private ResourceLocation baseModelTexture;

        private ModelSupplier<T> insertModelSupplier;
        private ModelLayerLocation insertModelLocation;
        private ResourceLocation insertModelTexture;

        private ModelSupplier<T> trimModelSupplier;
        private ModelLayerLocation trimModelLocation;
        private ResourceLocation trimModelTexture;


        public Builder(EntityRendererProvider.Context context) {
            this.context = context;
        }

        public Builder<T> baseModel(ModelSupplier<T> supplier,
                                    ModelLayerLocation location,
                                    ResourceLocation texture) {
            this.baseModelSupplier = supplier;
            this.baseModelLocation = location;
            this.baseModelTexture = texture;
            return this;
        }

        public Builder<T> insertModel(ModelSupplier<T> supplier,
                                      ModelLayerLocation location,
                                      ResourceLocation texture) {
            this.insertModelSupplier = supplier;
            this.insertModelLocation = location;
            this.insertModelTexture = texture;
            return this;
        }

        public Builder<T> trimModel(ModelSupplier<T> supplier,
                                      ModelLayerLocation location,
                                      ResourceLocation texture) {
            this.trimModelSupplier = supplier;
            this.trimModelLocation = location;
            this.trimModelTexture = texture;
            return this;
        }

        public MultipartVesselRenderer<T> build() {
            return new MultipartVesselRenderer<>(context,
                    baseModelSupplier, baseModelLocation, baseModelTexture,
                    insertModelSupplier, insertModelLocation, insertModelTexture,
                    trimModelSupplier, trimModelLocation, trimModelTexture);
        }

    }
}
