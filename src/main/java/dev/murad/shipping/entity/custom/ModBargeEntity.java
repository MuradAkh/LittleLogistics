package dev.murad.shipping.entity.custom;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.Train;
import javafx.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.IntStream;

public class ModBargeEntity extends BoatEntity implements ISpringableEntity, IInventory, INamedContainerProvider, ISidedInventory {
    private Optional<Pair<ISpringableEntity, SpringEntity>> dominated = Optional.empty();
    private Optional<Pair<ISpringableEntity, SpringEntity>> dominant = Optional.empty();
    private Train train;
    private boolean docked;
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);

    public ModBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
        this.train = new Train(this);
    }

    public ModBargeEntity(World worldIn, double x, double y, double z) {
        this(ModEntityTypes.BARGE.get(), worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }


    @Override
    public Item getDropItem() {
        return ModItems.BARGE.get();
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(ShippingMod.MOD_ID, "barge")));
    }

    @Override
    protected void addPassenger(Entity passenger){

    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }


    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (!this.level.isClientSide) {
            doInteract(player);
            return ActionResultType.PASS;
        }
        return ActionResultType.SUCCESS;
    }

    protected void doInteract(PlayerEntity player) {
        player.openMenu(this);
    }

    @Override
    public void remove(boolean keepData) {
        if (!this.level.isClientSide) {
            InventoryHelper.dropContents(this.level, this, this);
        }

        super.remove(keepData);
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
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated() {
        return this.dominated;
    }

    @Override
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominant() {
        return this.dominant;
    }

    @Override
    public void setDominated(ISpringableEntity entity, SpringEntity spring) {
        this.dominated = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void setDominant(ISpringableEntity entity, SpringEntity spring) {
        this.setTrain(entity.getTrain());
        this.dominant = Optional.of(new Pair<>(entity, spring));
    }

    @Override
    public void removeDominated() {
        this.dominated = Optional.empty();
    }

    @Override
    public void removeDominant() {
        this.dominant = Optional.empty();
    }

    @Override
    public Train getTrain() {
        return train;
    }

    @Override
    public void setTrain(Train train) {
        this.train = train;
        train.setTail(this);
        dominated.ifPresent(dominated -> {
            // avoid recursion loops
            if(!dominated.getKey().getTrain().equals(train)){
                dominated.getKey().setTrain(train);
            }
        });
    }

    @Override
    public void remove(){
        handleSpringableKill();
        super.remove();
    }


    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_70301_1_) {
        return this.itemStacks.get(p_70301_1_);
    }

    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
        return ItemStackHelper.removeItem(this.itemStacks, p_70298_1_, p_70298_2_);

    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        ItemStack itemstack = this.itemStacks.get(p_70304_1_);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemStacks.set(p_70304_1_, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        this.itemStacks.set(p_70299_1_, p_70299_2_);
        if (!p_70299_2_.isEmpty() && p_70299_2_.getCount() > this.getMaxStackSize()) {
            p_70299_2_.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void setChanged() {

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
        this.itemStacks.clear();
    }

    @Nullable
    @Override
    public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
        if (p_createMenu_3_.isSpectator()) {
            return null;
        }
        return ChestContainer.threeRows(p_createMenu_1_, p_createMenu_2_, this);

    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT p_213281_1_) {
        ItemStackHelper.saveAllItems(p_213281_1_, this.itemStacks);

    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        ItemStackHelper.loadAllItems(p_70037_1_, this.itemStacks);
    }


    // hack to disable hoppers
    public boolean isDockable() {
        return this.dominant.map(dom -> this.distanceToSqr((Entity) dom.getKey()) < 1.1).orElse(true);
    }


    @Override
    public int[] getSlotsForFace(Direction p_180463_1_) {
        return IntStream.rangeClosed(0, 27).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
        return isDockable();
    }

    @Override
    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
        return isDockable();
    }
}
