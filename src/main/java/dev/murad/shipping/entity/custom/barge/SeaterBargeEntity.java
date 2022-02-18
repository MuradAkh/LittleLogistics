package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SeaterBargeEntity extends AbstractBargeEntity{
    public SeaterBargeEntity(EntityType<? extends SeaterBargeEntity> type, Level world) {
        super(type, world);
    }

    public SeaterBargeEntity(Level worldIn, double x, double y, double z) {
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
        p_184454_1_.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(p_184454_1_.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        p_184454_1_.yRotO += f1 - f;
        p_184454_1_.setYRot(p_184454_1_.getYRot() + f1 - f);
        p_184454_1_.setYHeadRot(p_184454_1_.getYRot());
    }

    public void onPassengerTurned(Entity p_184190_1_) {
        this.clampRotation(p_184190_1_);
    }

    public void positionRider(Entity p_184232_1_) {
        if (this.hasPassenger(p_184232_1_)) {
            float f = -0.1F;
            float f1 = (float)((this.dead ? (double)0.01F : this.getPassengersRidingOffset()) + p_184232_1_.getMyRidingOffset());
            Vec3 vector3d = (new Vec3((double)f, 0.0D, 0.0D)).yRot(-this.getYRot() * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
            p_184232_1_.setPos(this.getX() + vector3d.x, this.getY() - 0.5 + (double)f1, this.getZ() + vector3d.z);
            if (p_184232_1_ instanceof Animal && this.getPassengers().size() > 1) {
                int j = p_184232_1_.getId() % 2 == 0 ? 90 : 270;
                p_184232_1_.setYBodyRot(((Animal)p_184232_1_).yBodyRot + (float)j);
                p_184232_1_.setYHeadRot(p_184232_1_.getYHeadRot() + (float)j);
            }

        }
    }


    @Override
    protected void doInteract(Player player) {
        player.startRiding(this);
    }
}
