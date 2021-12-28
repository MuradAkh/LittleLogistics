package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

public class TugDummyHitboxEntity extends Entity implements IEntityAdditionalSpawnData {
    private TugEntity tugEntity;

    public TugDummyHitboxEntity(EntityType<TugDummyHitboxEntity> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public TugDummyHitboxEntity(TugEntity tugEntity) {
        this(ModEntityTypes.TUG_DUMMY_HITBOX.get(), tugEntity.level);
        this.tugEntity = tugEntity;
        this.setDeltaMovement(Vector3d.ZERO);
        updatePosition();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
    }

    private void updatePosition(){
        if (tugEntity == null || !tugEntity.isAlive()){
            this.remove();
            return;
        }

        double x = tugEntity.getX() + tugEntity.getDirection().getStepX() * 0.7;
        double z = tugEntity.getZ() + tugEntity.getDirection().getStepZ() * 0.7;
        double y = tugEntity.getY();
        this.setPos(x, y, z);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updatePosition();
    }

    public boolean isPickable() {
        return !this.removed;
    }

    @Override
    public ActionResultType interact(PlayerEntity p_184230_1_, Hand p_184230_2_) {
        System.out.println(1);
        return tugEntity.mobInteract(p_184230_1_, p_184230_2_);
    }

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        System.out.println(1);
        return tugEntity.hurt(p_70097_1_, p_70097_2_);
    }

    @Override
    protected void defineSynchedData() {


    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        tugEntity = nbt.contains("parent") ? (TugEntity) this.level.getEntity(nbt.getInt("parent")) : null;
        if (tugEntity != null && tugEntity.extraHitbox == null) {
            tugEntity.extraHitbox = this;
        } else if (tugEntity != null && !tugEntity.extraHitbox.equals(this)){
            this.remove();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        if (tugEntity != null) {
            nbt.putInt("parent", tugEntity.getId());
        }
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        if (tugEntity != null){
            buffer.writeInt(tugEntity.getId());
        }
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        try {
            tugEntity = (TugEntity) this.level.getEntity(buffer.readInt());
        } catch (IndexOutOfBoundsException e){

        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
