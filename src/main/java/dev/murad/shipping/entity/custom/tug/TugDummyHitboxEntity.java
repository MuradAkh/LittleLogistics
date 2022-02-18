package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class TugDummyHitboxEntity extends Entity implements IEntityAdditionalSpawnData {
    private AbstractTugEntity tugEntity;
    public static final EntityDataAccessor<Integer> TUG_ID = SynchedEntityData.defineId(TugDummyHitboxEntity.class, EntityDataSerializers.INT);


    public TugDummyHitboxEntity(EntityType<TugDummyHitboxEntity> p_i48580_1_, Level p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    public TugDummyHitboxEntity(AbstractTugEntity tugEntity) {
        this(ModEntityTypes.TUG_DUMMY_HITBOX.get(), tugEntity.level);
        this.tugEntity = tugEntity;
        this.setDeltaMovement(Vec3.ZERO);
        updatePosition();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    @Nullable
    public ItemStack getPickResult() {
        if(tugEntity == null){
            return null;
        }
        return new ItemStack(tugEntity.getDropItem());
    }


    public AbstractTugEntity getTug(){
        return tugEntity;
    }

    @Override
    public void tick(){
        if(this.level.isClientSide && tugEntity == null){
            setTug();
            if(tugEntity == null){
                this.remove(RemovalReason.DISCARDED);
            }
        }
        if(!this.level.isClientSide) {
            if (tugEntity == null || tugEntity.isRemoved()) {
                this.remove(RemovalReason.DISCARDED);
            } else {
                TugDummyHitboxEntity p = tugEntity.getDummyHitbox();
                if (p != null && !p.equals(this)) {
                    this.remove(RemovalReason.DISCARDED);
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
        return !this.isRemoved();
    }

    @Override
    public InteractionResult interact(Player p_184230_1_, InteractionHand p_184230_2_) {
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
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
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
    public void readAdditionalSaveData(CompoundTag nbt) {
        this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        if (tugEntity != null){
            buffer.writeInt(tugEntity.getId());
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        try {
            tugEntity = (AbstractTugEntity) this.level.getEntity(buffer.readInt());
        } catch (IndexOutOfBoundsException e){

        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
