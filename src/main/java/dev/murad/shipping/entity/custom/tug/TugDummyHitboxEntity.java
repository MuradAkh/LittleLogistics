package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

public class TugDummyHitboxEntity extends Entity implements IEntityAdditionalSpawnData {
    private AbstractTugEntity tugEntity;
    public static final EntityDataAccessor<Integer> TUG_ID = SynchedEntityData.defineId(TugDummyHitboxEntity.class, DataSerializers.INT);


    public TugDummyHitboxEntity(EntityType<TugDummyHitboxEntity> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public TugDummyHitboxEntity(AbstractTugEntity tugEntity) {
        this(ModEntityTypes.TUG_DUMMY_HITBOX.get(), tugEntity.level);
        this.tugEntity = tugEntity;
        this.setDeltaMovement(Vector3d.ZERO);
        updatePosition();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
    }

    public AbstractTugEntity getTug(){
        return tugEntity;
    }

    @Override
    public void tick(){
        if(this.level.isClientSide && tugEntity == null){
            setTug();
        }
        if(!this.level.isClientSide) {
            if (tugEntity == null || !tugEntity.isAlive()) {
                this.kill();
            } else {
                TugDummyHitboxEntity p = tugEntity.getDummyHitbox();
                if (p != null && !p.equals(this)) {
                    this.kill();
                } else {
                    entityData.set(TUG_ID, tugEntity.getId());
                }
            }
        }
    }

    public void updatePosition(){
        double x = tugEntity.getX() + tugEntity.getDirection().getStepX() * 0.7;
        double z = tugEntity.getZ() + tugEntity.getDirection().getStepZ() * 0.7;
        double y = tugEntity.getY();
        this.moveTo(x, y, z);
    }

    public boolean isPickable() {
        return !this.removed;
    }

    @Override
    public ActionResultType interact(PlayerEntity p_184230_1_, Hand p_184230_2_) {
        return tugEntity.mobInteract(p_184230_1_, p_184230_2_);
    }

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        return tugEntity.hurt(p_70097_1_, p_70097_2_);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TUG_ID, -1);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(TUG_ID.equals(key)) {
                setTug();
            }
        }
    }

    private void setTug() {
        Entity potential = level.getEntity(getEntityData().get(TUG_ID));
        if(potential instanceof AbstractTugEntity){
            tugEntity = (AbstractTugEntity) potential;
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        this.remove();
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
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
            tugEntity = (AbstractTugEntity) this.level.getEntity(buffer.readInt());
        } catch (IndexOutOfBoundsException e){

        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
