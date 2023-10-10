package dev.murad.shipping.entity.render.barge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.entity.custom.vessel.barge.FishingBargeEntity;
import lombok.Getter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FishingBargeRenderer<T extends FishingBargeEntity> extends MultipartVesselRenderer<T> {

    @Getter
    private final EntityModel<T> transitionInsertModel, deployedInsertModel;

    @Getter
    private final ResourceLocation transitionInsertTextureLocation, deployedInsertTextureLocation;

    protected FishingBargeRenderer(EntityRendererProvider.Context context,
                                   ModelPack<T> baseModelPack,
                                   ModelPack<T> stashedInsertModelPack,
                                   ModelPack<T> transitionInsertModelPack,
                                   ModelPack<T> deployedInsertModelPack,
                                   ModelPack<T> trimModelPack) {
        super(context, baseModelPack, stashedInsertModelPack, trimModelPack);

        this.transitionInsertModel = transitionInsertModelPack.supplier().supply(context.bakeLayer(transitionInsertModelPack.location()));
        this.transitionInsertTextureLocation = transitionInsertModelPack.texture();

        this.deployedInsertModel = deployedInsertModelPack.supplier().supply(context.bakeLayer(deployedInsertModelPack.location()));
        this.deployedInsertTextureLocation = deployedInsertModelPack.texture();
    }

    @Override
    protected void renderInsertModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int overlay) {
        var model = switch(vesselEntity.getStatus()) {
            case STASHED -> getInsertModel();
            case DEPLOYED -> deployedInsertModel;
            case TRANSITION -> transitionInsertModel;
        };

        var texture = switch(vesselEntity.getStatus()) {
            case STASHED -> getInsertTextureLocation();
            case DEPLOYED -> deployedInsertTextureLocation;
            case TRANSITION -> transitionInsertTextureLocation;
        };

        model.renderToBuffer(matrixStack,
                buffer.getBuffer(model.renderType(texture)),
                packedLight, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static class Builder<T extends FishingBargeEntity> extends MultipartVesselRenderer.Builder<T> {
        private ModelPack<T> transitionInsertModelPack;
        private ModelPack<T> deployedInsertModelPack;


        public Builder(EntityRendererProvider.Context context) {
            super(context);
        }

        public Builder<T> transitionInsertModel(ModelSupplier<T> supplier,
                                             ModelLayerLocation location,
                                             ResourceLocation texture) {
            this.transitionInsertModelPack = new ModelPack<>(supplier, location, texture);
            return this;
        }

        public Builder<T> deployedInsertModel(ModelSupplier<T> supplier,
                                             ModelLayerLocation location,
                                             ResourceLocation texture) {
            this.deployedInsertModelPack = new ModelPack<>(supplier, location, texture);
            return this;
        }

        public FishingBargeRenderer<T> build() {
            return new FishingBargeRenderer<>(context, baseModelPack, insertModelPack, transitionInsertModelPack, deployedInsertModelPack, trimModelPack);
        }
    }

}
