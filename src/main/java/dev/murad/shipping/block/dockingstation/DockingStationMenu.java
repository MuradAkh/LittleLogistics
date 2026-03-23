package dev.murad.shipping.block.dockingstation;

import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the docking station GUI. No item slots — only station data
 * (name, stats, config) passed from server to client via FriendlyByteBuf.
 */
public class DockingStationMenu extends AbstractContainerMenu {

    /** Shared preset list used by BE and screen alike. */
    public static final int[] TIMEOUT_PRESETS = {20, 40, 100, 200, 400};

    private final BlockPos controllerPos;
    private final String stationName;
    private final long itemsLoaded;
    private final long itemsUnloaded;
    private final long fluidLoaded;
    private final long fluidUnloaded;
    private final long energyLoaded;
    private final long energyUnloaded;
    private final long vehiclesDocked;
    private final int dockCount;
    private final int redstoneModeOrdinal;
    private final int idleTimeoutTicks;

    /** Server-side constructor — called by DockingStationBlockEntity#openMenu. */
    public DockingStationMenu(int windowId, Inventory inv, BlockPos controllerPos,
                               String stationName,
                               long itemsLoaded, long itemsUnloaded,
                               long fluidLoaded, long fluidUnloaded,
                               long energyLoaded, long energyUnloaded,
                               long vehiclesDocked,
                               int dockCount,
                               int redstoneModeOrdinal, int idleTimeoutTicks) {
        super(ModMenuTypes.DOCKING_STATION.get(), windowId);
        this.controllerPos = controllerPos;
        this.stationName = stationName;
        this.itemsLoaded = itemsLoaded;
        this.itemsUnloaded = itemsUnloaded;
        this.fluidLoaded = fluidLoaded;
        this.fluidUnloaded = fluidUnloaded;
        this.energyLoaded = energyLoaded;
        this.energyUnloaded = energyUnloaded;
        this.vehiclesDocked = vehiclesDocked;
        this.dockCount = dockCount;
        this.redstoneModeOrdinal = redstoneModeOrdinal;
        this.idleTimeoutTicks = idleTimeoutTicks;
    }

    /** Client-side constructor — called by IMenuTypeExtension factory. */
    public DockingStationMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
        super(ModMenuTypes.DOCKING_STATION.get(), windowId);
        this.controllerPos = data.readBlockPos();
        this.stationName = data.readUtf(256);
        this.itemsLoaded = data.readLong();
        this.itemsUnloaded = data.readLong();
        this.fluidLoaded = data.readLong();
        this.fluidUnloaded = data.readLong();
        this.energyLoaded = data.readLong();
        this.energyUnloaded = data.readLong();
        this.vehiclesDocked = data.readLong();
        this.dockCount = data.readInt();
        this.redstoneModeOrdinal = data.readInt();
        this.idleTimeoutTicks = data.readInt();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                controllerPos.getX() + 0.5,
                controllerPos.getY() + 0.5,
                controllerPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    // =========================================================================
    // Getters (read-only snapshot from open time)
    // =========================================================================

    public BlockPos getControllerPos()     { return controllerPos; }
    public String   getStationName()       { return stationName; }
    public long     getItemsLoaded()       { return itemsLoaded; }
    public long     getItemsUnloaded()     { return itemsUnloaded; }
    public long     getFluidLoaded()       { return fluidLoaded; }
    public long     getFluidUnloaded()     { return fluidUnloaded; }
    public long     getEnergyLoaded()      { return energyLoaded; }
    public long     getEnergyUnloaded()    { return energyUnloaded; }
    public long     getVehiclesDocked()    { return vehiclesDocked; }
    public int      getDockCount()         { return dockCount; }
    public int      getRedstoneModeOrdinal(){ return redstoneModeOrdinal; }
    public int      getIdleTimeoutTicks()  { return idleTimeoutTicks; }
}
