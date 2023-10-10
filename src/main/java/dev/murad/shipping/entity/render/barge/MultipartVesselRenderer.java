package dev.murad.shipping.entity.render.barge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import lombok.Getter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

public class MultipartVesselRenderer<T extends AbstractBargeEntity> extends AbstractVesselRenderer<T> {

    @Getter
    private final EntityModel<T> baseModel, insertModel, trimModel;

    @Getter
    private final ResourceLocation baseTextureLocation, insertTextureLocation, trimTextureLocation;

    protected MultipartVesselRenderer(EntityRendererProvider.Context context,
                                      ModelPack<T> baseModelPack,
                                      ModelPack<T> insertModelPack,
                                      ModelPack<T> trimModelPack) {
        super(context);
        this.baseModel = baseModelPack.supplier.supply(context.bakeLayer(baseModelPack.location));
        this.baseTextureLocation = baseModelPack.texture;

        this.insertModel = insertModelPack.supplier.supply(context.bakeLayer(insertModelPack.location));
        this.insertTextureLocation = insertModelPack.texture;

        this.trimModel = trimModelPack.supplier.supply(context.bakeLayer(trimModelPack.location));
        this.trimTextureLocation = trimModelPack.texture;
    }

    /**
     * Don't directly use this method, use the multipart methods instead
     */
    @Override
    @Deprecated
    EntityModel<T> getModel(T entity) {
        return baseModel;
    }

    /**
     * Don't directly use this method, use the multipart methods instead
     */
    @Override
    @Deprecated
    public @NotNull ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return baseTextureLocation;
    }

    @Override
    protected void renderModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        int overlay = LivingEntityRenderer.getOverlayCoords(vesselEntity, 0);
        renderBaseModel(vesselEntity, matrixStack, buffer, packedLight, overlay);
        renderInsertModel(vesselEntity, matrixStack, buffer, packedLight, overlay);
        renderTrimModel(vesselEntity, matrixStack, buffer, packedLight, overlay);
    }

    protected void renderBaseModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int overlay) {
        baseModel.renderToBuffer(matrixStack,
                buffer.getBuffer(baseModel.renderType(baseTextureLocation)),
                packedLight, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void renderInsertModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int overlay) {
        insertModel.renderToBuffer(matrixStack,
                buffer.getBuffer(insertModel.renderType(insertTextureLocation)),
                packedLight, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected void renderTrimModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int overlay) {
        var colorId = vesselEntity.getColor();
        var color = (colorId == null ? DyeColor.RED : DyeColor.byId(colorId)).getTextureDiffuseColors();

        trimModel.renderToBuffer(matrixStack,
                buffer.getBuffer(trimModel.renderType(trimTextureLocation)),
                packedLight, overlay,
                color[0], color[1], color[2], 1.0F);
    }

    @FunctionalInterface
    public interface ModelSupplier<T extends Entity> {
        EntityModel<T> supply(ModelPart root);
    }

    public record ModelPack<T extends Entity>(
            ModelSupplier<T> supplier,
            ModelLayerLocation location,
            ResourceLocation texture) {
    }

    public static class Builder<T extends AbstractBargeEntity> {
        protected final EntityRendererProvider.Context context;

        protected ModelPack<T> baseModelPack;
        protected ModelPack<T> insertModelPack;
        protected ModelPack<T> trimModelPack;


        public Builder(EntityRendererProvider.Context context) {
            this.context = context;
        }

        public Builder<T> baseModel(ModelSupplier<T> supplier,
                                    ModelLayerLocation location,
                                    ResourceLocation texture) {
            this.baseModelPack = new ModelPack<>(supplier, location, texture);
            return this;
        }

        public Builder<T> insertModel(ModelSupplier<T> supplier,
                                      ModelLayerLocation location,
                                      ResourceLocation texture) {
            this.insertModelPack = new ModelPack<>(supplier, location, texture);
            return this;
        }

        public Builder<T> trimModel(ModelSupplier<T> supplier,
                                      ModelLayerLocation location,
                                      ResourceLocation texture) {
            this.trimModelPack = new ModelPack<>(supplier, location, texture);
            return this;
        }

        public MultipartVesselRenderer<T> build() {
            return new MultipartVesselRenderer<>(context, baseModelPack, insertModelPack, trimModelPack);
        }
    }
}
