package dev.murad.shipping.entity.custom.tug;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.dock.TugDockTileEntity;
import dev.murad.shipping.block.guide_rail.TugGuideRailBlock;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.util.*;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.navigation.TugPathNavigator;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class AbstractTugEntity extends VesselEntity implements LinkableEntityHead, SpringableEntity, Container, WorldlyContainer {

    // CONTAINER STUFF
    protected final ItemStackHandler itemHandler = createHandler();
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected boolean contentsChanged = false;
    protected boolean docked = false;
    private int dockCheckCooldown = 0;
    private boolean independentMotion = false;
    private int pathfindCooldown = 0;
    private TugFrontPart frontHitbox;
    private static final EntityDataAccessor<Boolean> INDEPENDENT_MOTION = SynchedEntityData.defineId(AbstractTugEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean allowDockInterface(){
        return isDocked();
    }

    private TugRoute path;
    private int nextStop;


    public AbstractTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
        this.train = new Train(this);
        this.path = new TugRoute();
        frontHitbox = new TugFrontPart(this);
    }

    public AbstractTugEntity(EntityType type, Level worldIn, double x, double y, double z) {
        this(type, worldIn);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    // CONTAINER STUFF
    @Override
    public void dropLeash(boolean p_110160_1_, boolean p_110160_2_) {
        navigation.recomputePath();
        super.dropLeash(p_110160_1_, p_110160_2_);
    }


    public abstract DataAccessor getDataAccessor();

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1 + getNonRouteItemSlots()) {
            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged = true;
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                switch (slot) {
                    case 0: // route
                        return stack.getItem() == ModItems.TUG_ROUTE.get();
                    default: // up to childrenge
                        return isTugSlotItemValid(slot, stack);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                switch (slot) {
                    case 0: // route
                        return 1;
                    default: // up to children
                        return getTugSlotLimit(slot);
                }
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    protected abstract int getNonRouteItemSlots();

    protected boolean isTugSlotItemValid(int slot, @Nonnull ItemStack stack){
        return false;
    }

    protected int getTugSlotLimit(int slot){
        return 0;
    }

    protected abstract MenuProvider createContainerProvider();

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        itemHandler.deserializeNBT(compound.getCompound("inv"));
        nextStop = compound.contains("next_stop") ? compound.getInt("next_stop") : 0;
        contentsChanged = true;
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.put("inv", itemHandler.serializeNBT());
        compound.putInt("next_stop", nextStop);
        super.addAdditionalSaveData(compound);
    }

    private void tickRouteCheck() {
        if (contentsChanged) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            this.setPath(TugRouteItem.getRoute(stack));
        }
    }

    protected abstract boolean tickFuel();

    public static AttributeSupplier.Builder setCustomAttributes() {
        return VesselEntity.setCustomAttributes()
                .add(Attributes.FOLLOW_RANGE, 200);
    }

    protected void onDock() {
        this.playSound(ModSounds.TUG_DOCKING.get(), 0.6f, 1.0f);
    }

    protected void onUndock() {
        this.playSound(ModSounds.TUG_UNDOCKING.get(), 0.6f, 1.5f);
    }

    // MOB STUFF

    private List<Direction> getSideDirections() {
        return this.getDirection() == Direction.NORTH || this.getDirection() == Direction.SOUTH ?
                Arrays.asList(Direction.EAST, Direction.WEST) :
                Arrays.asList(Direction.NORTH, Direction.SOUTH);
    }


    private void tickCheckDock() {
        int x = (int) Math.floor(this.getX());
        int y = (int) Math.floor(this.getY());
        int z = (int) Math.floor(this.getZ());

        if (this.docked && dockCheckCooldown > 0){
            dockCheckCooldown--;
            this.setDeltaMovement(Vec3.ZERO);
            this.moveTo(x + 0.5 ,getY(),z + 0.5);
            return;
        }

        // Check docks
        boolean shouldDock = this.getSideDirections()
                .stream()
                .map((curr) ->
                    Optional.ofNullable(level.getBlockEntity(new BlockPos(x + curr.getStepX(), y, z + curr.getStepZ())))
                            .filter(entity -> entity instanceof TugDockTileEntity)
                            .map(entity -> (TugDockTileEntity) entity)
                            .map(dock -> dock.holdVessel(this, curr))
                            .orElse(false))
                .reduce(false, (acc, curr) -> acc || curr);

         boolean changedDock = !this.docked && shouldDock;
         boolean changedUndock = this.docked && !shouldDock;

        this.docked = shouldDock;

        if(this.docked) {
            dockCheckCooldown = 20; // todo: magic number
            this.setDeltaMovement(Vec3.ZERO);
            this.moveTo(x + 0.5 ,getY(),z + 0.5);
        } else {
            dockCheckCooldown = 0;
        }

        if (changedDock) onDock();
        if (changedUndock) onUndock();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    protected void makeSmoke() {
        Level world = this.level;
        if (world != null) {
            BlockPos blockpos = this.getOnPos().above().above();
            Random random = world.random;
            if (random.nextFloat() < ShippingConfig.Client.TUG_SMOKE_MODIFIER.get()) {
                for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                    makeParticles(world, blockpos, true, false);
                }
            }
        }
    }

    public static void makeParticles(Level p_220098_0_, BlockPos p_220098_1_, boolean p_220098_2_, boolean p_220098_3_) {
        Random random = p_220098_0_.getRandom();
        Supplier<Boolean> h = () -> random.nextDouble() < 0.5;
        SimpleParticleType basicparticletype = p_220098_2_ ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        double xdrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;
        double zdrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;
        p_220098_0_.addAlwaysVisibleParticle(basicparticletype, true, (double)p_220098_1_.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)p_220098_1_.getY() + random.nextDouble() + random.nextDouble(), (double)p_220098_1_.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.007D * xdrift, 0.05D, 0.007D * zdrift);
    }

    @Override
    protected PathNavigation createNavigation(Level p_175447_1_) {
        return new TugPathNavigator(this, p_175447_1_);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.level.isClientSide()) {
            NetworkHooks.openGui((ServerPlayer) player, createContainerProvider(), getDataAccessor()::write);
        }
        // don't open GUI *and* use item in hand
        return InteractionResult.CONSUME;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(INDEPENDENT_MOTION.equals(key)) {
                independentMotion = entityData.get(INDEPENDENT_MOTION);
            }
        }
    }


    protected void registerGoals() {
        this.goalSelector.addGoal(0, new MovementGoal());
    }

    class MovementGoal extends Goal {
        @Override
        public boolean canUse() {
            return AbstractTugEntity.this.path != null;
        }

        public void tick() {
            if(!AbstractTugEntity.this.level.isClientSide) {
                tickRouteCheck();
                tickCheckDock();
                followPath();
                followGuideRail();
            }

        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts()
    {
        return new PartEntity<?>[]{frontHitbox};
    }

    @Override
    public void aiStep(){
        super.aiStep();
        if(!isDeadOrDying() && !this.isNoAi()){
            frontHitbox.updatePosition(this);
        }

    }

    @Override
    public void recreateFromPacket(ClientboundAddMobPacket p_149572_) {
        super.recreateFromPacket(p_149572_);
        frontHitbox.setId(p_149572_.getId());
    }

    public void tick() {


        if(this.level.isClientSide
                && independentMotion){
            makeSmoke();
        }

        super.tick();

    }

    private void followGuideRail(){
        List<BlockState> belowList = Arrays.asList(this.level.getBlockState(getOnPos().below()),
                this.level.getBlockState(getOnPos().below().below()));
        BlockState water = this.level.getBlockState(getOnPos());
        for (BlockState below : belowList) {
            if (below.is(ModBlocks.GUIDE_RAIL_TUG.get()) && water.is(Blocks.WATER)) {
                Direction arrows = TugGuideRailBlock.getArrowsDirection(below);
                this.setYRot(arrows.toYRot());
                double modifier = 0.03;
                this.setDeltaMovement(this.getDeltaMovement().add(
                        new Vec3(arrows.getStepX() * modifier, 0, arrows.getStepZ() * modifier)));
            }
        }
    }

    // todo: someone said you could prevent mobs from getting stuck on blocks by override this
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
    }

    private void followPath() {
        pathfindCooldown--;
        if (!this.path.isEmpty() && !this.docked && tickFuel()) {
            TugRouteNode stop = path.get(nextStop);
            if (navigation.getPath() == null || navigation.getPath().isDone()
            ) {
                if(pathfindCooldown < 0 || navigation.getPath() != null){  //only go on cooldown when the path was not completed
                    navigation.moveTo(stop.getX(), this.getY(), stop.getZ(), 0.3);
                    pathfindCooldown = 20;
                } else {
                    return;
                }
            }
            double distance = Math.abs(Math.hypot(this.getX() - (stop.getX() + 0.5), this.getZ() - (stop.getZ() + 0.5)));
            independentMotion = true;
            entityData.set(INDEPENDENT_MOTION, true);

            if (distance < 0.9) {
                incrementStop();
            }

        } else{
            entityData.set(INDEPENDENT_MOTION, false);
            this.navigation.stop();

            if (this.path.isEmpty()){
                this.nextStop = 0;
            }
        }
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(INDEPENDENT_MOTION, false);
    }


    public void setPath(TugRoute path) {
        this.path = path;
    }

    private void incrementStop() {
        if (this.path.size() == 1) {
            nextStop = 0;
        } else if (!this.path.isEmpty()) {
            nextStop = (nextStop + 1) % (this.path.size());
        }
    }

    @Override
    public void setDominated(LinkableEntity entity) {
        this.dominated = Optional.of((SpringableEntity) entity);
    }

    @Override
    public void setDominatedSpring(SpringEntity spring) {
        this.dominatedS = Optional.of(spring);
    }

    @Override
    public void setDominant(LinkableEntity entity) {

    }

    @Override
    public void setDominantSpring(SpringEntity entity) {

    }

    @Override
    public void removeDominated() {
        this.dominated = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {

    }

    @Override
    public void setTrain(Train train) {
        this.train = train;
    }


    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        if (this.isInvulnerableTo(p_70097_1_)) {
            return false;
        } else if (!this.level.isClientSide && !this.isRemoved()) {
            this.spawnAtLocation(this.getDropItem());
            this.remove(RemovalReason.KILLED);
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level.isClientSide) {
            Containers.dropContents(this.level, this, this);
        }
        handleLinkableKill();
        super.remove(r);
    }


    // Have to implement IInventory to work with hoppers


    @Override
    public ItemStack getItem(int p_70301_1_) {
        return itemHandler.getStackInSlot(p_70301_1_);
    }

    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        return null;
    }


    public boolean canPlaceItem(int p_94041_1_, ItemStack p_94041_2_) {
        return true;
    }

    @Override
    public void setChanged() {
        contentsChanged = true;
    }

    @Override
    public boolean stillValid(Player p_70300_1_) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(p_70300_1_.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void clearContent() {

    }

    @Override
    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction p_180463_1_) {
        return IntStream.range(1, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
        return isDocked();
    }
    @Override
    public int getContainerSize() {
        return 1 + getNonRouteItemSlots();
    }

    public boolean isDocked(){
        return docked;
    }

    @Override
    public boolean canBeLeashed(Player p_184652_1_) {
        return true;
    }


}
