package dev.murad.shipping.entity.custom.vessel.barge;

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
import org.jetbrains.annotations.NotNull;

public class SeaterBargeEntity extends AbstractBargeEntity {
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

    private void clampRotation(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -105.0F, 105.0F);
        passenger.yRotO += f1 - f;
        passenger.setYRot(passenger.getYRot() + f1 - f);
        passenger.setYHeadRot(passenger.getYRot());
    }

    public void onPassengerTurned(@NotNull Entity passenger) {
        this.clampRotation(passenger);
    }

    @Override
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction pCallback) {
        if (this.hasPassenger(passenger)) {
            float f = -0.1F;
            float f1 = (float)((this.dead ? (double)0.01F : this.getPassengersRidingOffset()) + passenger.getMyRidingOffset());
            Vec3 vector3d = (new Vec3((double)f, 0.0D, 0.0D)).yRot(-this.getYRot() * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
            passenger.setPos(this.getX() + vector3d.x, this.getY() - 0.5 + (double)f1, this.getZ() + vector3d.z);
            if (passenger instanceof Animal && this.getPassengers().size() > 1) {
                int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((Animal)passenger).yBodyRot + (float)j);
                passenger.setYHeadRot(passenger.getYHeadRot() + (float)j);
            }
        }
    }


    @Override
    protected void doInteract(Player player) {
        player.startRiding(this);
    }
}
