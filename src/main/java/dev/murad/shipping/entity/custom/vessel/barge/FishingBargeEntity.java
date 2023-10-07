package dev.murad.shipping.entity.custom.vessel.barge;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.container.FishingBargeContainer;
import dev.murad.shipping.entity.custom.TrainInventoryProvider;
import dev.murad.shipping.util.InventoryUtils;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class FishingBargeEntity extends AbstractBargeEntity {
    private int ticksDeployable = 0;
    private int fishCooldown = 0;
    private final Set<Pair<Integer, Integer>> overFishedCoords = new HashSet<>();
    private final Queue<Pair<Integer, Integer>> overFishedQueue = new LinkedList<>();

    private static final ResourceLocation FISHING_LOOT_TABLE =
            new ResourceLocation(ShippingConfig.Server.FISHING_LOOT_TABLE.get());

    private static final int FISHING_COOLDOWN =
            ShippingConfig.Server.FISHING_COOLDOWN.get();

    private static final double FISHING_TREASURE_CHANCE =
            ShippingConfig.Server.FISHING_TREASURE_CHANCE_MODIFIER.get();


    public FishingBargeEntity(EntityType<? extends FishingBargeEntity> type, Level world) {
        super(type, world);
    }
    public FishingBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.FISHING_BARGE.get(), worldIn, x, y, z);
    }


    @Override
    // Only called on the server side
    protected void doInteract(Player player) {
        var size = getConnectedInventories().size();

        player.displayClientMessage(
                switch (size) {
                    case 0 -> Component.translatable("global.littlelogistics.no_connected_inventory_barge");
                    default -> Component.translatable("global.littlelogistics.connected_inventory", size);
                }, false);
    }

    @Override
    public void remove(RemovalReason r) {
        super.remove(r);
    }

    @Override
    public void tick(){
        super.tick();
        tickWaterOnSidesCheck();
        if(!this.level().isClientSide && this.getStatus() == Status.DEPLOYED){
            if(fishCooldown < 0) {
                tickFish();
                fishCooldown = FISHING_COOLDOWN;
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
        for (BlockPos pos = this.getOnPos(); this.level().getBlockState(pos).getBlock().equals(Blocks.WATER); pos = pos.below()){
            count ++;
        }
        count = Math.min(count, 20);
        return ((double) count) / 20.0;
    }

    // Only called on server side
    private void tickFish(){
        double overFishPenalty = isOverFished() ? 0.05 : 1;
        double shallowPenalty = computeDepthPenalty();
        double chance = 0.25 * overFishPenalty * shallowPenalty;
        double treasure_chance = shallowPenalty > 0.4 ? chance * (shallowPenalty / 2)
                *  FISHING_TREASURE_CHANCE : 0;
        double r = Math.random();
        if(r < chance){
            LootParams params = new LootParams.Builder((ServerLevel) this.level())
                    .withParameter(LootContextParams.ORIGIN, this.position())
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                    .withParameter(LootContextParams.KILLER_ENTITY, this)
                    .withParameter(LootContextParams.THIS_ENTITY, this)
                    .create(LootContextParamSets.FISHING);

            LootTable loottable = this.level()
                    .getServer()
                    .getLootData()
                    .getLootTable(r < treasure_chance ? BuiltInLootTables.FISHING_TREASURE : FISHING_LOOT_TABLE);

            List<ItemStack> list = loottable.getRandomItems(params);

            var inventoryProviders = getConnectedInventories();

            for (ItemStack stack : list) {
                var leftOver = stack;
                for (var provider : inventoryProviders) {
                    if (leftOver.isEmpty()) {
                        break;
                    }

                    var itemHandler = provider.getTrainInventoryHandler();
                    if (itemHandler.isPresent()) {
                        leftOver = InventoryUtils.moveItemStackIntoHandler(itemHandler.get(), leftOver);
                    }
                }
                // void the stack if we end up not being able to put it in any connected inventory.
            }

            if(!isOverFished()) {
                addOverFish();
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
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putString("overfish", overFishedString());
        super.addAdditionalSaveData(compound);
    }
    private void addOverFish() {
        int x = (int) Math.floor(this.getX());
        int z = (int) Math.floor(this.getZ());
        overFishedCoords.add(new Pair<>(x, z));
        overFishedQueue.add(new Pair<>(x, z));
        if (overFishedQueue.size() > 30) {
            overFishedCoords.remove(overFishedQueue.poll());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        populateOverfish(compound.getString("overfish"));
        super.readAdditionalSaveData(compound);
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

    public Status getStatus(){
        return hasWaterOnSides() ? getNonStashedStatus() : Status.STASHED;
    }

    private Status getNonStashedStatus(){
        if (ticksDeployable < 40){
            return Status.TRANSITION;
        } else {
            return this.applyWithDominant(LinkableEntity::hasWaterOnSides)
                    .reduce(true, Boolean::logicalAnd)
                    ? Status.DEPLOYED : Status.TRANSITION;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    public enum Status {
        STASHED,
        DEPLOYED,
        TRANSITION
    }
}
