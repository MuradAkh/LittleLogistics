package dev.murad.shipping.entity.custom.barge;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.container.FishingBargeContainer;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class FishingBargeEntity extends AbstractBargeEntity implements Container, WorldlyContainer {
    protected final ItemStackHandler itemHandler = createHandler();
    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected boolean contentsChanged = false;
    private int ticksDeployable = 0;
    private int fishCooldown = 0;
    private final Set<Pair<Integer, Integer>> overFishedCoords = new HashSet<>();
    private final Queue<Pair<Integer, Integer>> overFishedQueue = new LinkedList<>();

    private static final ResourceLocation fishingLootTable =
            new ResourceLocation(ShippingConfig.Server.FISHING_LOOT_TABLE.get());


    public FishingBargeEntity(EntityType<? extends FishingBargeEntity> type, Level world) {
        super(type, world);
    }
    public FishingBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.FISHING_BARGE.get(), worldIn, x, y, z);
    }


    @Override
    protected void doInteract(Player player) {
        NetworkHooks.openGui((ServerPlayer) player, createContainerProvider(), buffer -> buffer.writeInt(this.getId()));

    }

    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TranslatableComponent("screen.littlelogistics.fishing_barge");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                return new FishingBargeContainer(i, level, getId(), playerInventory, playerEntity);
            }
        };
    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level.isClientSide) {
            Containers.dropContents(this.level, this, this);
        }
        super.remove(r);
    }


    private ItemStackHandler createHandler() {
        return new ItemStackHandler(9);
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
        for (BlockPos pos = this.getOnPos(); this.level.getBlockState(pos).getBlock().equals(Blocks.WATER); pos = pos.below()){
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
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel) this.level))
                    .withParameter(LootContextParams.ORIGIN, this.position())
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                    .withRandom(this.random);

            lootcontext$builder.withParameter(LootContextParams.KILLER_ENTITY, this).withParameter(LootContextParams.THIS_ENTITY, this);
            LootTable loottable = this.level.getServer().getLootTables().get(r < treasure_chance ? BuiltInLootTables.FISHING_TREASURE : fishingLootTable);
            List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));
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
    public void readAdditionalSaveData(CompoundTag compound) {
        itemHandler.deserializeNBT(compound.getCompound("inv"));
        populateOverfish(compound.getString("overfish"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
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
        return 9;
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
    public boolean stillValid(Player p_70300_1_) {
        if (this.dead) {
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
