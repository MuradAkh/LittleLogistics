package dev.murad.shipping.block.dock;

import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public abstract class AbstractDockTileEntity<T extends Entity & LinkableEntity<T>> extends BlockEntity {

    private static final ItemStackHandler EMPTY_ITEM_HANDLER = new ItemStackHandler(0);

    private final MutableObject<Entity> dockedEntity;

    private LazyOptional<IItemHandler> itemCapability;

    public AbstractDockTileEntity(BlockEntityType<?> entityType, BlockPos pos, BlockState s) {
        super(entityType, pos, s);
        dockedEntity = new MutableObject<>(null);
        itemCapability = LazyOptional.of(() -> EMPTY_ITEM_HANDLER);
    }

    public abstract boolean hold(T vessel, Direction direction);

    public Optional<HopperBlockEntity> getHopper(BlockPos p){
        BlockEntity mayBeHopper = this.level.getBlockEntity(p);
        if (mayBeHopper instanceof HopperBlockEntity h) {
            return Optional.of(h);
        }
        else return Optional.empty();
    }

    @Override
    public @NotNull <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }

        // TODO: fluids and energy
        return super.getCapability(cap, side);
    }

    public void setDockedEntity(Entity e) {
        dockedEntity.setValue(e);

        itemCapability.invalidate();
        itemCapability = LazyOptional.of(() -> {
            System.out.println("Item Capability");
            if (dockedEntity.getValue() != null && dockedEntity.getValue().isRemoved()) {
                resetDockedEntity();
            }

            // needs to be below the above block
            var entity = dockedEntity.getValue();

            // Delegate to docked entity
            if (entity != null) {
                var opt = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if (opt.isPresent()) {
                    return opt.get();
                }
            }

            return EMPTY_ITEM_HANDLER;
        });
    }

    public void resetDockedEntity() {
        setDockedEntity(null);
    }
}
