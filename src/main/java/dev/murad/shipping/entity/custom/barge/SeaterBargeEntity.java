package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SeaterBargeEntity extends AbstractBargeEntity{
    public SeaterBargeEntity(EntityType<? extends SeaterBargeEntity> type, World world) {
        super(type, world);
    }

    public SeaterBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.SEATER_BARGE.get(), worldIn, x, y, z);
    }


    @Override
    public Item getDropItem() {
        return ModItems.SEATER_BARGE.get();
    }

    @Override
    protected boolean canAddPassenger(Entity p_184219_1_) {
        return this.getPassengers().size() < 1;
    }

    private void clampRotation(Entity p_184454_1_) {
        p_184454_1_.setYBodyRot(this.yRot);
        float f = MathHelper.wrapDegrees(p_184454_1_.yRot - this.yRot);
        float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
        p_184454_1_.yRotO += f1 - f;
        p_184454_1_.yRot += f1 - f;
        p_184454_1_.setYHeadRot(p_184454_1_.yRot);
    }

    public void onPassengerTurned(Entity p_184190_1_) {
        this.clampRotation(p_184190_1_);
    }

    public void positionRider(Entity p_184232_1_) {
        if (this.hasPassenger(p_184232_1_)) {
            float f = -0.1F;
            float f1 = (float)((this.removed ? (double)0.01F : this.getPassengersRidingOffset()) + p_184232_1_.getMyRidingOffset());
            Vector3d vector3d = (new Vector3d((double)f, 0.0D, 0.0D)).yRot(-this.yRot * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
            p_184232_1_.setPos(this.getX() + vector3d.x, this.getY() - 0.5 + (double)f1, this.getZ() + vector3d.z);
            if (p_184232_1_ instanceof AnimalEntity && this.getPassengers().size() > 1) {
                int j = p_184232_1_.getId() % 2 == 0 ? 90 : 270;
                p_184232_1_.setYBodyRot(((AnimalEntity)p_184232_1_).yBodyRot + (float)j);
                p_184232_1_.setYHeadRot(p_184232_1_.getYHeadRot() + (float)j);
            }

        }
    }


    @Override
    protected void doInteract(PlayerEntity player) {
        player.startRiding(this);
    }
}
