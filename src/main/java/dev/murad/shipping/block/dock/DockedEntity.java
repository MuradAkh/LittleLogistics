package dev.murad.shipping.block.dock;

import dev.murad.shipping.util.LinkableEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

@AllArgsConstructor
public class DockedEntity<T extends Entity & LinkableEntity<T>> {

    @Nullable
    private T entity;

    @Getter
    @Nullable
    private IItemHandler entityItemHandler;

    @Getter
    @Nullable
    private IEnergyStorage entityEnergyStorage;

    @Getter
    @Nullable
    private IFluidHandler entityFluidHandler;

    public void dockEntity(T entity) {
        this.entity = entity;
        this.entityItemHandler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        this.entityEnergyStorage = entity.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        this.entityFluidHandler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
    }

    public void undockEntity() {
        this.entity = null;
        this.entityItemHandler = null;
        this.entityEnergyStorage = null;
        this.entityFluidHandler = null;
    }

    public boolean hasDockedEntity() {
        return entity != null;
    }

    public static <T extends Entity & LinkableEntity<T>> DockedEntity<T> empty() {
        return new DockedEntity<>(null, null, null, null);
    }

}
