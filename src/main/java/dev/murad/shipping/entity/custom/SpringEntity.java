package dev.murad.shipping.entity.custom;

/*
MIT License

Copyright (c) 2018 Xavier "jglrxavpok" Niochaut

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.EntitySpringAPI;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpringEntity extends Entity implements IEntityAdditionalSpawnData {

    public static final EntityDataAccessor<Integer> DOMINANT_ID = SynchedEntityData.defineId(SpringEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DOMINATED_ID = SynchedEntityData.defineId(SpringEntity.class, EntityDataSerializers.INT);

    private @Nullable CompoundTag dominantNBT;
    private @Nullable CompoundTag dominatedNBT;
    @Nullable
    private VesselEntity dominant;
    @Nullable
    private VesselEntity dominated;

    public VesselEntity getDominant(){
        return dominant;
    }

    public void setDominant(VesselEntity dominant){
        if(dominated != null && dominant != null){
            dominant.setDominated(dominated, this);
            dominated.setDominant(dominant, this);
        }
        this.dominant = dominant;
    }

    public void setDominated(VesselEntity dominated){
        if(dominated != null && dominant != null){
            dominant.setDominated(dominated, this);
            dominated.setDominant(dominant, this);
        }
        this.dominated = dominated;
    }

    public Entity getDominated(){
        return dominant;
    }

    public SpringEntity(EntityType<? extends Entity> type, Level worldIn) {
        super(type, worldIn);
        setNoGravity(true);
        noPhysics = true;
    }

    public SpringEntity(@Nonnull VesselEntity dominant, @Nonnull VesselEntity dominatedEntity) {
        super(ModEntityTypes.SPRING.get(), dominant.getCommandSenderWorld());
        setDominant(dominant);
        setDominated(dominatedEntity);
        setPos((dominant.getX() + dominated.getX())/2, (dominant.getY() + dominated.getY())/2, (dominant.getZ() + dominated.getZ())/2);
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(DOMINANT_ID, -1);
        getEntityData().define(DOMINATED_ID, -1);
    }

    public static Vec3 calculateAnchorPosition(VesselEntity entity, SpringSide side) {
        return EntitySpringAPI.calculateAnchorPosition(entity, side);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(DOMINANT_ID.equals(key)) {
                VesselEntity potential = (VesselEntity) level.getEntity(getEntityData().get(DOMINANT_ID));
                if(potential != null) {
                    setDominant(potential);
                }
            }
            if(DOMINATED_ID.equals(key)) {
                VesselEntity potential = (VesselEntity) level.getEntity(getEntityData().get(DOMINATED_ID));
                if(potential != null) {
                    setDominated(potential);
                }
            }
        }
    }

    @Override
    public Direction getDirection(){
        return this.dominated.getDirection();
    }

    @Override
    public void baseTick() {
        setDeltaMovement(0, 0, 0);
        super.baseTick();
        if (tryLoadAndCalculate()) return;
        syncClient();
    }

    private void syncClient() {
        if(dominant != null && dominated != null) { // send update every tick to ensure client has infos
            if (!this.level.isClientSide) {
                entityData.set(DOMINANT_ID, dominant.getId());
                entityData.set(DOMINATED_ID, dominated.getId());
            }
        } else {
            if (dominant == null){
                onSyncedDataUpdated(DOMINANT_ID);
            }

            if (dominated == null) {
                onSyncedDataUpdated(DOMINATED_ID);
            }
        }

        if(this.level.isClientSide) {
            if (dominant!= null && ! dominant.isAlive()){
                onSyncedDataUpdated(DOMINANT_ID);

            }

            if (dominated!= null && ! dominated.isAlive()){
                onSyncedDataUpdated(DOMINATED_ID);

            }

        }


    }

    private boolean tryLoadAndCalculate() {
        if(!this.level.isClientSide) {
            if (dominant != null && dominated != null) {
                if (dominated.distanceTo(dominant) > 20) {
                    dominated.removeDominant();
                    kill();
                    return true;
                }
                if (!dominant.isAlive() || !dominated.isAlive()) {
                    kill();
                    return true;
                }
                setPos((dominant.getX() + dominated.getX()) / 2, (dominant.getY() + dominated.getY()) / 2, (dominant.getZ() + dominated.getZ()) / 2);


                double distSq = dominant.distanceToSqr(dominated);
                double maxDstSq = (dominant).getTrain().getTug().map(tug -> ((AbstractTugEntity) tug).isDocked() ? 1 : 1.2).orElse(1.2);
                if (distSq > maxDstSq) {
                    Vec3 frontAnchor = calculateAnchorPosition(dominant, SpringSide.DOMINATED);
                    Vec3 backAnchor = calculateAnchorPosition(dominated, SpringSide.DOMINANT);
                    double dist = Math.sqrt(distSq);
                    double dx = (frontAnchor.x - backAnchor.x) / dist;
                    double dy = (frontAnchor.y - backAnchor.y) / dist;
                    double dz = (frontAnchor.z - backAnchor.z) / dist;
                    final double alpha = 0.5;

                    float targetYaw = computeTargetYaw(dominated.getYRot(), frontAnchor, backAnchor);
                    dominated.setYRot((float) ((alpha * dominated.getYRot() + targetYaw * (1f - alpha)) % 360));
                    setYRot(dominated.getYRot());
                    double k = dominant instanceof AbstractTugEntity ? 0.2 : 0.13;
                    double l0 = maxDstSq;
                    dominated.setDeltaMovement(k * (dist - l0) * dx, k * (dist - l0) * dy, k * (dist - l0) * dz);
                    dominated.checkInsideBlocks();
                }
            } else { // front and back entities have not been loaded yet
                if (dominantNBT != null && dominatedNBT != null) {
                    tryToLoadFromNBT(dominantNBT).ifPresent(e -> {
                        setDominant(e);
                        entityData.set(DOMINANT_ID, e.getId());
                    });
                    tryToLoadFromNBT(dominatedNBT).ifPresent(e -> {
                        setDominated(e);
                        entityData.set(DOMINATED_ID, e.getId());
                    });
                }
                updateClient();
            }
        }
        return false;
    }

    private void updateClient(){
        if(this.level.isClientSide) {
            if(this.dominant == null) {
                Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
                if (potential != null) {
                    setDominant((VesselEntity) potential);
                }
            }

            if(this.dominated == null) {
                Entity potential_dominated = level.getEntity(getEntityData().get(DOMINATED_ID));
                if (potential_dominated != null) {
                    setDominated((VesselEntity) potential_dominated);
                }
            }
        }

    }

    private float computeTargetYaw(Float currentYaw, Vec3 anchorPos, Vec3 otherAnchorPos) {
        float idealYaw = (float) (Math.atan2(otherAnchorPos.x - anchorPos.x, -(otherAnchorPos.z - anchorPos.z)) * (180f/Math.PI));
        float closestDistance = Float.POSITIVE_INFINITY;
        float closest = idealYaw;
        for(int sign : Arrays.asList(-1, 0, 1)) {
            float potentialYaw = idealYaw + sign * 360f;
            float distance = Math.abs(potentialYaw - currentYaw);
            if(distance < closestDistance) {
                closestDistance = distance;
                closest = potentialYaw;
            }
        }
        return closest;
    }

    private Optional<VesselEntity> tryToLoadFromNBT(CompoundTag compound) {
        try {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            pos.set(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
            String uuid = compound.getString("UUID");
            AABB searchBox = new AABB(
                    pos.getX() - 2,
                    pos.getY() - 2,
                    pos.getZ() - 2,
                    pos.getX() + 2,
                    pos.getY() + 2,
                    pos.getZ() + 2
            );
            List<Entity> entities = level.getEntities(this, searchBox, e -> e.getStringUUID().equals(uuid));
            return entities.stream().findFirst().map(e -> (VesselEntity) e);
        } catch (Exception e){
            return Optional.empty();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        dominantNBT = compound.getCompound(SpringSide.DOMINANT.name());
        dominatedNBT = compound.getCompound(SpringSide.DOMINATED.name());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if(dominant != null && dominated != null) {
            writeNBT(SpringSide.DOMINANT, dominant, compound);
            writeNBT(SpringSide.DOMINATED, dominated, compound);
        } else {
            if(dominantNBT != null)
                compound.put(SpringSide.DOMINANT.name(), dominantNBT);
            if(dominatedNBT != null)
                compound.put(SpringSide.DOMINATED.name(), dominatedNBT);
        }
    }

    private void writeNBT(SpringSide side, Entity entity, CompoundTag globalCompound) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", (int)Math.floor(entity.getX()));
        compound.putInt("Y", (int)Math.floor(entity.getY()));
        compound.putInt("Z", (int)Math.floor(entity.getZ()));

        compound.putString("UUID", entity.getUUID().toString());

        globalCompound.put(side.name(), compound);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        if(dominated != null && dominant != null) {
            buffer.writeBoolean(true);
            buffer.writeInt(dominant.getId());
            buffer.writeInt(dominated.getId());

            CompoundTag dominatedNBT = new CompoundTag();
            writeNBT(SpringSide.DOMINATED, dominated, dominatedNBT);

            CompoundTag dominantNBT = new CompoundTag();
            writeNBT(SpringSide.DOMINANT, dominant, dominantNBT);

            buffer.writeNbt(dominantNBT);
            buffer.writeNbt(dominatedNBT);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        if(additionalData.readBoolean()) { // has both entities
            int frontID = additionalData.readInt();
            int backID = additionalData.readInt();
            setDominant((VesselEntity) level.getEntity(frontID));
            setDominated((VesselEntity) level.getEntity(backID));

            dominantNBT = additionalData.readNbt();
            dominatedNBT = additionalData.readNbt();
        }
    }
    public static void createSpring(VesselEntity dominantEntity, VesselEntity dominatedEntity) {
        SpringEntity link = new SpringEntity(dominantEntity, dominatedEntity);
        Level world = dominantEntity.getCommandSenderWorld();
        world.addFreshEntity(link);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return BlockEntity.INFINITE_EXTENT_AABB;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return super.interactAt(player, vec, hand);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void remove(RemovalReason r) {
        super.remove(r);
        if(dominant != null){
            dominant.removeDominated();
        }
        if(!level.isClientSide)
            Containers.dropItemStack(level, getX(), getY(), getZ(), new ItemStack(ModItems.SPRING.get()));
    }

    public enum SpringSide {
        DOMINANT, DOMINATED
    }
}
