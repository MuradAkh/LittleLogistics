package dev.murad.shipping.entity.custom;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.EntitySpringAPI;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpringEntity extends Entity implements IEntityAdditionalSpawnData {

    private static AxisAlignedBB nullBB = new AxisAlignedBB(0,0,0,0,0,0);
    public static final DataParameter<Integer> DOMINANT_ID = EntityDataManager.defineId(SpringEntity.class, DataSerializers.INT);
    public static final DataParameter<Integer> DOMINATED_ID = EntityDataManager.defineId(SpringEntity.class, DataSerializers.INT);

    private @Nullable CompoundNBT dominantNBT;
    private @Nullable CompoundNBT dominatedNBT;
    @Nullable
    private Entity dominant;
    @Nullable
    private Entity dominated;

    public Entity getDominant(){
        return dominant;
    }

    public void setDominant(Entity dominant){
        if(dominated instanceof ISpringableEntity && dominant instanceof ISpringableEntity){
            ((ISpringableEntity) dominant).setDominated((ISpringableEntity) dominated, this);
            ((ISpringableEntity) dominated).setDominant((ISpringableEntity) dominant, this);
        }
        this.dominant = dominant;
    }

    public void setDominated(Entity dominated){
        if(dominated instanceof ISpringableEntity && dominant instanceof  ISpringableEntity){
            ((ISpringableEntity) dominant).setDominated((ISpringableEntity) dominated, this);
            ((ISpringableEntity) dominated).setDominant((ISpringableEntity) dominant, this);
        }
        this.dominated = dominated;
    }

    public Entity getDominated(){
        return dominant;
    }

    public SpringEntity(EntityType<? extends Entity> type, World worldIn) {
        super(type, worldIn);
        setNoGravity(true);
        noPhysics = true;
    }

    public SpringEntity(@Nonnull Entity dominant, @Nonnull Entity dominatedEntity) {
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

//    @Override
//    public boolean canBePushed() {
//        return false;
//    }

//    @Nullable
//    @Override
//    public AxisAlignedBB getBoundingBox() {
//        return nullBB;
//    }

//    @Nullable
//    @Override
//    public AxisAlignedBB getCollisionBox(Entity entityIn) {
//        return nullBB;
//    }

    public static Vector3d calculateAnchorPosition(Entity entity, SpringSide side) {
        return EntitySpringAPI.calculateAnchorPosition(entity, side);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(DOMINANT_ID.equals(key)) {
                Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
                if(potential != null) {
                    setDominant(potential);
                }
            }
            if(DOMINATED_ID.equals(key)) {
                Entity potential = level.getEntity(getEntityData().get(DOMINATED_ID));
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

        if(dominant != null && dominated != null) {
            if( ! dominant.isAlive() || ! dominated.isAlive()) {
                kill();
                return;
            }
            setPos((dominant.getX() + dominated.getX())/2, (dominant.getY() + dominated.getY())/2, (dominant.getZ() + dominated.getZ())/2);


            double distSq = dominant.distanceToSqr(dominated);
            double maxDstSq = 0.2;
            if(distSq > maxDstSq) {
                Vector3d frontAnchor = calculateAnchorPosition(dominant, SpringSide.DOMINATED);
                Vector3d backAnchor = calculateAnchorPosition(dominated, SpringSide.DOMINANT);
                double dist = Math.sqrt(distSq);
                double dx = (frontAnchor.x - backAnchor.x) / dist;
                double dy = (frontAnchor.y - backAnchor.y) / dist;
                double dz = (frontAnchor.z - backAnchor.z) / dist;
                final double alpha = 0.1;

                float targetYaw = computeTargetYaw(dominated.yRot, frontAnchor, backAnchor);
                dominated.yRot = (float) (alpha * dominated.yRot + targetYaw * (1f-alpha));
                this.yRot = dominated.yRot;
                double k = 0.1;
                double l0 = 1.1;
                dominated.setDeltaMovement(k*(dist-l0)*dx, k*(dist-l0)*dy, k*(dist-l0)*dz);
            }

            if(!level.isClientSide) { // send update every tick to ensure client has infos
                entityData.set(DOMINANT_ID, dominant.getId());
                entityData.set(DOMINATED_ID, dominated.getId());
            }
        } else { // front and back entities have not been loaded yet
            if(dominantNBT != null && dominatedNBT != null) {
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

    private void updateClient(){
        if(this.level.isClientSide) {
            if(this.dominant == null) {
                Entity potential = level.getEntity(getEntityData().get(DOMINANT_ID));
                if (potential != null) {
                    setDominant(potential);
                }
            }

            if(this.dominated == null) {
                Entity potential_dominated = level.getEntity(getEntityData().get(DOMINATED_ID));
                if (potential_dominated != null) {
                    setDominated(potential_dominated);
                }
            }
        }

    }

    private float computeTargetYaw(Float currentYaw, Vector3d anchorPos, Vector3d otherAnchorPos) {
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

    private Optional<Entity> tryToLoadFromNBT(CompoundNBT compound) {
        try {
            BlockPos.Mutable pos = new BlockPos.Mutable();
            pos.set(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
            String type = compound.getString("Type");
            AxisAlignedBB searchBox = new AxisAlignedBB(pos);
            List<Entity> entities = level.getEntities(this, searchBox, e -> e.getClass().getCanonicalName().equals(type));
            return entities.stream().findFirst();
        } catch (Exception e){
            return Optional.empty();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT compound) {
        dominantNBT = compound.getCompound(SpringSide.DOMINANT.name());
        dominatedNBT = compound.getCompound(SpringSide.DOMINATED.name());
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT compound) {
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

    private void writeNBT(SpringSide side, Entity entity, CompoundNBT globalCompound) {
        CompoundNBT compound = new CompoundNBT();
        compound.putInt("X", (int)Math.floor(entity.getX()));
        compound.putInt("Y", (int)Math.floor(entity.getY()));
        compound.putInt("Z", (int)Math.floor(entity.getZ()));
        compound.putString("Type", entity.getClass().getCanonicalName());

        globalCompound.put(side.name(), compound);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        if(dominated != null && dominant != null) {
            buffer.writeBoolean(true);
            buffer.writeInt(dominant.getId());
            buffer.writeInt(dominated.getId());

            CompoundNBT dominatedNBT = new CompoundNBT();
            writeNBT(SpringSide.DOMINATED, dominated, dominatedNBT);

            CompoundNBT dominantNBT = new CompoundNBT();
            writeNBT(SpringSide.DOMINANT, dominant, dominantNBT);

            buffer.writeNbt(dominantNBT);
            buffer.writeNbt(dominatedNBT);
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        if(additionalData.readBoolean()) { // has both entities
            int frontID = additionalData.readInt();
            int backID = additionalData.readInt();
            setDominant(level.getEntity(frontID));
            setDominated(level.getEntity(backID));

            dominantNBT = additionalData.readNbt();
            dominatedNBT = additionalData.readNbt();
        }
    }

    // Helper methods
    public static boolean hasLinkOnSide(SpringSide side, Entity entity) {
        return streamSpringsAttachedTo(side, entity).count() != 0;
    }

    public static Stream<SpringEntity> streamSpringsAttachedTo(SpringSide side, Entity entity) {
        World world = entity.getCommandSenderWorld();
        return getLoadedEntityList(world)
                .stream()
                .filter(e -> e instanceof SpringEntity)
                .map(e -> (SpringEntity)e)
                .filter(e -> {
                    if(side == SpringSide.DOMINANT)
                        return e.dominated == entity;
                    else
                        return e.dominant == entity;
                });
    }

    private static Collection<Entity> getLoadedEntityList(World world) {
        if(world instanceof ServerWorld) {
            return ((ServerWorld) world).getEntities().collect(Collectors.toList());
        } else {
            Iterable<Entity> entities = ((ClientWorld)world).entitiesForRendering();
            LinkedList<Entity> list = new LinkedList<>();
            entities.forEach(list::add);
            return list;
        }
    }

    public static void createSpring(Entity dominantEntity, Entity dominatedEntity) {
        SpringEntity link = new SpringEntity(dominantEntity, dominatedEntity);
        World world = dominantEntity.getCommandSenderWorld();
        world.addFreshEntity(link);
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        return TileEntity.INFINITE_EXTENT_AABB;
    }

    @Override
    public ActionResultType interactAt(PlayerEntity player, Vector3d vec, Hand hand) {
        return super.interactAt(player, vec, hand);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void kill() {
        super.remove();
        if(dominant instanceof ISpringableEntity){
            ((ISpringableEntity) dominant).removeDominated();
        }
        if(!level.isClientSide)
            InventoryHelper.dropItemStack(level, getX(), getY(), getZ(), new ItemStack(ModItems.SPRING.get()));
    }

    public enum SpringSide {
        DOMINANT, DOMINATED
    }
}
