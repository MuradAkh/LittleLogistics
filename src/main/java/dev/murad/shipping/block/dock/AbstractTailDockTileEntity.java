package dev.murad.shipping.block.dock;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.util.InventoryUtils;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractTailDockTileEntity<T extends Entity & LinkableEntity<T>> extends AbstractDockTileEntity<T> {
    public AbstractTailDockTileEntity(BlockEntityType<?> t, BlockPos pos, BlockState state) {
        super(t, pos, state);
    }

    protected Boolean isExtract() {
        return getBlockState().getValue(DockingBlockStates.INVERTED);
    }

    @Override
    public boolean hold(T vessel, Direction direction) {
        return !checkBadDirCondition(direction);
    }

    protected abstract boolean checkBadDirCondition(Direction direction);
}
