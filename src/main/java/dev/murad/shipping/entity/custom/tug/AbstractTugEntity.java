package dev.murad.shipping.entity.custom.tug;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.dock.TugDockTileEntity;
import dev.murad.shipping.block.guide_rail.TugGuideRailBlock;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.navigation.TugPathNavigator;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.Train;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class AbstractTugEntity extends VesselEntity implements ISpringableEntity, IInventory, ISidedInventory {

    // CONTAINER STUFF
    protected final ItemStackHandler itemHandler = createHandler();
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected boolean contentsChanged = false;
    protected boolean docked = false;
    private int dockCheckCooldown = 0;
    private boolean independentMotion = false;
    private static final DataParameter<Boolean> INDEPENDENT_MOTION = EntityDataManager.defineId(AbstractTugEntity.class, DataSerializers.BOOLEAN);

    public boolean allowDockInterface(){
        return isDocked();
    }

    private TugDummyHitboxEntity extraHitbox = null;

    private List<Vector2f> path;
    private int nextStop;


    public AbstractTugEntity(EntityType<? extends WaterMobEntity> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
        this.train = new Train(this);
        this.path = new ArrayList<>();
    }

    public AbstractTugEntity(EntityType type, World worldIn, double x, double y, double z) {
        this(type, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
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

    protected abstract INamedContainerProvider createContainerProvider();

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        itemHandler.deserializeNBT(compound.getCompound("inv"));
        nextStop = compound.contains("next_stop") ? compound.getInt("next_stop") : 0;
        contentsChanged = true;
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
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

    public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
        return VesselEntity.createMobAttributes()
                .add(Attributes.FOLLOW_RANGE, 200);

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
            this.setDeltaMovement(Vector3d.ZERO);
            this.moveTo(x + 0.5 ,getY(),z + 0.5);
            return;
        }

        // Check docks
        this.docked = this.getSideDirections()
                .stream()
                .map((curr) ->
                    Optional.ofNullable(level.getBlockEntity(new BlockPos(x + curr.getStepX(), y, z + curr.getStepZ())))
                            .filter(entity -> entity instanceof TugDockTileEntity)
                            .map(entity -> (TugDockTileEntity) entity)
                            .map(dock -> dock.holdVessel(this, curr))
                            .orElse(false))
                .reduce(false, (acc, curr) -> acc || curr);

        if(this.docked) {
            dockCheckCooldown = 20; // todo: magic number
            this.setDeltaMovement(Vector3d.ZERO);
            this.moveTo(x + 0.5 ,getY(),z + 0.5);
        } else {
            dockCheckCooldown = 0;
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    protected void makeSmoke() {
        World world = this.level;
        if (world != null) {
            BlockPos blockpos = this.getOnPos().above().above();
            Random random = world.random;
            if (random.nextFloat() < ShippingConfig.tug_smoke_modifier.get()) {
                for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                    makeParticles(world, blockpos, true, false);
                }
            }
        }
    }

    public static void makeParticles(World p_220098_0_, BlockPos p_220098_1_, boolean p_220098_2_, boolean p_220098_3_) {
        Random random = p_220098_0_.getRandom();
        Supplier<Boolean> h = () -> random.nextDouble() < 0.5;
        BasicParticleType basicparticletype = p_220098_2_ ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        double xdrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;
        double zdrift = (h.get() ? 1 : -1) * random.nextDouble() * 2;
        p_220098_0_.addAlwaysVisibleParticle(basicparticletype, true, (double)p_220098_1_.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)p_220098_1_.getY() + random.nextDouble() + random.nextDouble(), (double)p_220098_1_.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.007D * xdrift, 0.05D, 0.007D * zdrift);
    }



    @Override
    protected PathNavigator createNavigation(World p_175447_1_) {
        return new TugPathNavigator(this, p_175447_1_);
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        if (!player.level.isClientSide()) {
            NetworkHooks.openGui((ServerPlayerEntity) player, createContainerProvider(), getDataAccessor()::write);
        }
        // don't open GUI *and* use item in hand
        return ActionResultType.CONSUME;
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);

        if(level.isClientSide) {
            if(INDEPENDENT_MOTION.equals(key)) {
                independentMotion = entityData.get(INDEPENDENT_MOTION);
            }
        }
    }

    public void tick() {
        if(!level.isClientSide && extraHitbox == null){
            this.extraHitbox = new TugDummyHitboxEntity(this);
            level.addFreshEntity(this.extraHitbox);
        }

        super.tick();

        if(!this.level.isClientSide) {
            tickRouteCheck();
            tickCheckDock();
            followPath();
            followGuideRail();
        }
        if(this.level.isClientSide
                && independentMotion){
            makeSmoke();
        }
    }

    private void followGuideRail(){
        List<BlockState> belowList = Arrays.asList(this.level.getBlockState(getOnPos().below()),
                this.level.getBlockState(getOnPos().below().below()));
        BlockState water = this.level.getBlockState(getOnPos());
        for (BlockState below : belowList) {
            if (below.getBlock().is(ModBlocks.GUIDE_RAIL_TUG.get()) && water.is(Blocks.WATER)) {
                Direction arrows = TugGuideRailBlock.getArrowsDirection(below);
                this.yRot = arrows.toYRot();
                double modifier = 0.03;
                this.setDeltaMovement(this.getDeltaMovement().add(
                        new Vector3d(arrows.getStepX() * modifier, 0, arrows.getStepZ() * modifier)));
            }
        }
    }

    private void followPath() {
        if (!this.path.isEmpty() && !this.docked && tickFuel()) {
            Vector2f stop = path.get(nextStop);
            navigation.moveTo(stop.x, this.getY(), stop.y, 5);
            this.move(MoverType.SELF, this.getDeltaMovement());
            double distance = Math.abs(Math.hypot(this.getX() - stop.x, this.getZ() - stop.y));
            independentMotion = true;
            entityData.set(INDEPENDENT_MOTION, true);

            if (distance < 2.2) {
                incrementStop();
            }

        } else{
            entityData.set(INDEPENDENT_MOTION, false);

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


    public void setPath(List<Vector2f> path) {
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
    public void setDominated(ISpringableEntity entity, SpringEntity spring) {
        this.dominated = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void setDominant(ISpringableEntity entity, SpringEntity spring) {

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
        } else if (!this.level.isClientSide && !this.removed) {
            this.spawnAtLocation(this.getDropItem());
            this.remove();
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide) {
            InventoryHelper.dropContents(this.level, this, this);
        }
        handleSpringableKill();
        super.remove();
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
    public boolean stillValid(PlayerEntity p_70300_1_) {
        if (this.removed) {
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
    public boolean canBeLeashed(PlayerEntity p_184652_1_) {
        return true;
    }


}
