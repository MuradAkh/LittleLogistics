package dev.murad.shipping.entity.custom.vessel.barge;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class VacuumBargeEntity extends AbstractBargeEntity {

    private static final int ITEM_CHECK_DELAY = 20;
    private static final double PICK_RADIUS = 10;
    private static final double PICK_HEIGHT = 4;


    // There's no point in saving this... probably
    private int itemCheckDelay = 0;

    public VacuumBargeEntity(EntityType<? extends VacuumBargeEntity> type, Level world) {
        super(type, world);
    }
    public VacuumBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.VACUUM_BARGE.get(), worldIn, x, y, z);
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

        if(this.level().isClientSide) {
            return;
        }

        if (this.itemCheckDelay > 0) {
            this.itemCheckDelay--;
            return;
        }

        // perform item check
        AABB searchBox = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ())
                .inflate(PICK_RADIUS, PICK_HEIGHT / 2.0, PICK_RADIUS);

        var items = this.level()
                .getEntitiesOfClass(ItemEntity.class, searchBox, (e) -> e.distanceToSqr(this) < (PICK_RADIUS * PICK_RADIUS));

        if (!items.isEmpty()) {
            var inventoryProviders = getConnectedInventories();
            for (var item : items) {
                var leftOver = item.getItem();
                for (var provider : inventoryProviders) {
                    if (leftOver.isEmpty()) {
                        break;
                    }

                    var itemHandler = provider.getTrainInventoryHandler();
                    if (itemHandler.isPresent()) {
                        leftOver = InventoryUtils.moveItemStackIntoHandler(itemHandler.get(), leftOver);
                    }
                }
                item.setItem(leftOver);
            }
        }
        this.itemCheckDelay = ITEM_CHECK_DELAY;
    }

    @Override
    public Item getDropItem() {
        return ModItems.VACUUM_BARGE.get();
    }
}
