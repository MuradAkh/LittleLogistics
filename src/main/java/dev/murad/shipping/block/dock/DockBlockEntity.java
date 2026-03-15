package dev.murad.shipping.block.dock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Universal dock block entity that detects docked vehicles, proxies their
 * capabilities (items, fluids, energy), and manages hold state.
 * <p>
 * Used by both water dock blocks and rail dock blocks.
 */
public class DockBlockEntity extends BlockEntity {

    // --- Persisted configuration ---

    private int idleTimeoutTicks = 100; // default 5 seconds
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    // --- Runtime state (not persisted) ---

    @Nullable
    private Entity dockedVehicle;
    private int ticksSinceLastTransfer;

    // --- Capability wrappers (always non-null; delegate to vehicle when docked) ---

    private final DockItemHandler itemHandler;
    private final DockFluidHandler fluidHandler;
    private final DockEnergyStorage energyStorage;

    // --- Preset timeout values for scroll cycling ---

    private static final int[] TIMEOUT_PRESETS = {20, 40, 100, 200, 400};

    // =========================================================================
    // Enums
    // =========================================================================

    public enum RedstoneMode {
        IGNORE,
        HOLD_WHILE_POWERED,
        DISABLE_WHILE_POWERED;

        private static final RedstoneMode[] VALUES = values();

        public RedstoneMode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    public DockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.itemHandler = new DockItemHandler(this);
        this.fluidHandler = new DockFluidHandler(this);
        this.energyStorage = new DockEnergyStorage(this);
    }

    // =========================================================================
    // Dock lifecycle
    // =========================================================================

    /**
     * Called by a vehicle when it docks at this block.
     * Resolves the vehicle's capabilities so external pipes/hoppers can interact.
     */
    public void occupyDock(Entity vehicle) {
        this.dockedVehicle = vehicle;
        this.ticksSinceLastTransfer = 0;

        // Connect wrappers to the vehicle's capabilities
        IItemHandler vehicleItems = vehicle.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (vehicleItems != null) this.itemHandler.connect(vehicleItems);

        IFluidHandler vehicleFluids = vehicle.getCapability(Capabilities.FluidHandler.ENTITY, null);
        if (vehicleFluids != null) this.fluidHandler.connect(vehicleFluids);

        IEnergyStorage vehicleEnergy = vehicle.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (vehicleEnergy != null) this.energyStorage.connect(vehicleEnergy);

        setChanged();
        // Invalidate block capabilities so adjacent pipes/hoppers re-query
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    /**
     * Called when the vehicle undocks from this block.
     */
    public void vacateDock() {
        this.dockedVehicle = null;
        this.itemHandler.disconnect();
        this.fluidHandler.disconnect();
        this.energyStorage.disconnect();

        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    // =========================================================================
    // Hold logic
    // =========================================================================

    /**
     * Returns true if this dock wants the train/tug to hold position.
     * Called by head vehicles to determine whether to remain stalled.
     */
    public boolean isHolding() {
        if (redstoneMode == RedstoneMode.HOLD_WHILE_POWERED && isPowered()) {
            return true;
        }
        if (redstoneMode == RedstoneMode.DISABLE_WHILE_POWERED && isPowered()) {
            return false;
        }
        if (dockedVehicle == null) {
            return false;
        }
        return ticksSinceLastTransfer < idleTimeoutTicks;
    }

    private boolean isPowered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    // =========================================================================
    // Transfer notification
    // =========================================================================

    /**
     * Called by capability wrappers (Task 3) whenever a successful transfer occurs,
     * resetting the idle timer so the vehicle stays docked.
     */
    public void notifyTransfer() {
        this.ticksSinceLastTransfer = 0;
    }

    // =========================================================================
    // Tick
    // =========================================================================

    /**
     * Static server tick method — lightweight, just increments the idle counter.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity be) {
        if (be.dockedVehicle != null) {
            be.ticksSinceLastTransfer++;
        }
    }

    // =========================================================================
    // Pass-through detection
    // =========================================================================

    /**
     * Checks if there's another dock in the given direction.
     * Used by vehicles to decide whether to skip this dock and continue
     * to the next one in line (e.g., multi-car docking).
     *
     * @param vehicleHeading the direction the vehicle is traveling
     * @return true if another dock block entity exists in the forward direction
     */
    public boolean shouldPassThrough(net.minecraft.core.Direction vehicleHeading) {
        if (level == null) return false;
        BlockPos ahead = worldPosition.relative(vehicleHeading);
        BlockEntity aheadBE = level.getBlockEntity(ahead);
        return aheadBE instanceof DockBlockEntity;
    }

    // =========================================================================
    // Configuration cycling
    // =========================================================================

    /**
     * Adjusts the idle timeout by cycling through preset values.
     *
     * @param delta +1 to increase, -1 to decrease
     */
    public void adjustTimeout(int delta) {
        // Find the current index
        int currentIndex = 2; // default to 100 ticks (index 2)
        for (int i = 0; i < TIMEOUT_PRESETS.length; i++) {
            if (TIMEOUT_PRESETS[i] == idleTimeoutTicks) {
                currentIndex = i;
                break;
            }
        }
        int newIndex = Math.clamp(currentIndex + delta, 0, TIMEOUT_PRESETS.length - 1);
        idleTimeoutTicks = TIMEOUT_PRESETS[newIndex];
        setChanged();
    }

    /**
     * Cycles the redstone mode: IGNORE -> HOLD_WHILE_POWERED -> DISABLE_WHILE_POWERED -> IGNORE.
     */
    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IdleTimeoutTicks", idleTimeoutTicks);
        tag.putString("RedstoneMode", redstoneMode.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("IdleTimeoutTicks")) {
            idleTimeoutTicks = tag.getInt("IdleTimeoutTicks");
        }
        if (tag.contains("RedstoneMode")) {
            try {
                redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode"));
            } catch (IllegalArgumentException e) {
                redstoneMode = RedstoneMode.IGNORE;
            }
        }
    }

    // =========================================================================
    // Capability getters (called by CapabilityRegistration to expose block caps)
    // =========================================================================

    @Nonnull
    public DockItemHandler getItemHandler() {
        return itemHandler;
    }

    @Nonnull
    public DockFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Nonnull
    public DockEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // =========================================================================
    // State getters
    // =========================================================================

    @Nullable
    public Entity getDockedVehicle() {
        return dockedVehicle;
    }

    public int getIdleTimeoutTicks() {
        return idleTimeoutTicks;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public int getTicksSinceLastTransfer() {
        return ticksSinceLastTransfer;
    }
}
