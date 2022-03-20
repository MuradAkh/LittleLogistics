package dev.murad.shipping.entity.custom.barge;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.container.FishingBargeContainer;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class FishingBargeEntity extends AbstractBargeEntity implements IInventory, ISidedInventory {
    protected final ItemStackHandler itemHandler = createHandler();
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected boolean contentsChanged = false;
    private int ticksDeployable = 0;
    private int fishCooldown = 0;
    private final Set<Pair<Integer, Integer>> overFishedCoords = new HashSet<>();
    private final Queue<Pair<Integer, Integer>> overFishedQueue = new LinkedList<>();

    private static final ResourceLocation fishingLootTable =
            new ResourceLocation(ShippingConfig.Server.FISHING_LOOT_TABLE.get());


    public FishingBargeEntity(EntityType<? extends FishingBargeEntity> type, World world) {
        super(type, world);
    }
    public FishingBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.FISHING_BARGE.get(), worldIn, x, y, z);
    }


    @Override
    protected void doInteract(PlayerEntity player) {
        NetworkHooks.openGui((ServerPlayerEntity) player, createContainerProvider(), buffer -> buffer.writeInt(this.getId()));

    }

    protected INamedContainerProvider createContainerProvider() {
        return new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("screen.littlelogistics.fishing_barge");
            }

            @Nullable
            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new FishingBargeContainer(i, level, getId(), playerInventory, playerEntity);
            }
        };
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide) {
            InventoryHelper.dropContents(this.level, this, this);
        }
        super.remove();
    }


    private ItemStackHandler createHandler() {
        return new ItemStackHandler(27);
    }


    @Override
    public void tick(){
        super.tick();
        tickWaterOnSidesCheck();
        if(!this.level.isClientSide && this.getStatus() == Status.DEPLOYED){
            if(fishCooldown < 0) {
                tickFish();
                fishCooldown = 20;
            }  else {
                fishCooldown--;
            }
        }

    }

    private void tickWaterOnSidesCheck(){
        if(hasWaterOnSides()){
            ticksDeployable++;
        }else {
            ticksDeployable = 0;
        }
    }

    private double computeDepthPenalty(){
        int count = 0;
        for (BlockPos pos = this.getOnPos();  this.level.getBlockState(pos).getBlock().equals(Blocks.WATER); pos = pos.below()){
            count ++;
        }
        count = Math.min(count, 20);
        return ((double) count) / 20.0;
    }

    private void tickFish(){
        double overFishPenalty = isOverFished() ? 0.05 : 1;
        double shallowPenalty = computeDepthPenalty();
        double chance = 0.25 * overFishPenalty * shallowPenalty;
        double treasure_chance = shallowPenalty > 0.4 ? chance * (shallowPenalty / 2)
                * ShippingConfig.Server.FISHING_TREASURE_CHANCE_MODIFIER.get() : 0;
        double r = Math.random();
        if(r < chance){
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld)this.level))
                    .withParameter(LootParameters.ORIGIN, this.position())
                    .withParameter(LootParameters.THIS_ENTITY, this)
                    .withParameter(LootParameters.TOOL, new ItemStack(Items.FISHING_ROD))
                    .withRandom(this.random);

            lootcontext$builder.withParameter(LootParameters.KILLER_ENTITY, this).withParameter(LootParameters.THIS_ENTITY, this);
            LootTable loottable = this.level.getServer().getLootTables().get(r < treasure_chance ? LootTables.FISHING_TREASURE : fishingLootTable);
            List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootParameterSets.FISHING));
            for (ItemStack stack : list) {
                int slot = InventoryUtils.findSlotFotItem(this, stack);
                if (slot != -1) {
                    itemHandler.insertItem(slot, stack, false);
                }
                if(!isOverFished()) {
                    addOverFish();
                }
            }
        }
    }

    private String overFishedString(){
        return overFishedQueue.stream().map(t -> t.getFirst() + ":" + t.getSecond()).reduce("", (acc, curr) -> String.join(",", acc, curr));
    }

    private void populateOverfish(String string){
        Arrays.stream(string.split(","))
                .filter(s -> !s.isEmpty())
                .map(s -> s.split(":"))
                .map(arr -> new Pair(Integer.parseInt(arr[0]), Integer.parseInt(arr[1])))
                .forEach(overFishedQueue::add);
        overFishedCoords.addAll(overFishedQueue);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        //backwards compat
        CompoundNBT inv = compound.getCompound("inv");
        inv.remove("Size");

        itemHandler.deserializeNBT(inv);
        populateOverfish(compound.getString("overfish"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        compound.put("inv", itemHandler.serializeNBT());
        compound.putString("overfish", overFishedString());
        super.addAdditionalSaveData(compound);
    }

    private void addOverFish(){
        int x = (int) Math.floor(this.getX());
        int z = (int) Math.floor(this.getZ());
        overFishedCoords.add(new Pair<>(x, z));
        overFishedQueue.add(new Pair<>(x, z));
        if(overFishedQueue.size() > 30){
            overFishedCoords.remove(overFishedQueue.poll());
        }
    }

    private boolean isOverFished(){
        int x = (int) Math.floor(this.getX());
        int z = (int) Math.floor(this.getZ());
        return overFishedCoords.contains(new Pair<>(x, z));
    }

    @Override
    public Item getDropItem() {
        return ModItems.FISHING_BARGE.get();
    }

    @Override
    public int[] getSlotsForFace(Direction p_180463_1_) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
        return isDockable();
    }

    @Override
    public boolean canPlaceItem(int p_94041_1_, ItemStack p_94041_2_) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++){
            if(!itemHandler.getStackInSlot(i).isEmpty() && !itemHandler.getStackInSlot(i).getItem().equals(Items.AIR)){
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int p_70301_1_) {
        return itemHandler.getStackInSlot(p_70301_1_);
    }

    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
       return itemHandler.extractItem(p_70298_1_, p_70298_2_, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        ItemStack itemstack = itemHandler.getStackInSlot(p_70304_1_);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemHandler.setStackInSlot(p_70304_1_, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        itemHandler.setStackInSlot(p_70299_1_, p_70299_2_);
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

    public Status getStatus(){
        return hasWaterOnSides() ? getNonStashedStatus() : Status.STASHED;
    }

    private Status getNonStashedStatus(){
        if (ticksDeployable < 40){
            return Status.TRANSITION;
        } else {
            return this.applyWithDominant(ISpringableEntity::hasWaterOnSides)
                    .reduce(true, Boolean::logicalAnd)
                    ? Status.DEPLOYED : Status.TRANSITION;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void clearContent() {

    }

    public enum Status {
        STASHED,
        DEPLOYED,
        TRANSITION
    }
}
