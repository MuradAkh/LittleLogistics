package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public interface RenderWithAttachmentPoints<T extends AbstractTrainCarEntity> {
    Pair<Vec3, Vec3> renderCarAndGetAttachmentPoints(T car, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight);
}
