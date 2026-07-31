package dev.murad.shipping.block.dockingstation;

import dev.murad.shipping.entity.Colorable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for the CONTROLLER part of a docking station.
 * Detects docked vehicles, proxies their capabilities (items, fluids, energy),
 * and manages hold/timeout/redstone logic.
 */
public class DockingStationBlockEntity extends BlockEntity {

    // =========================================================================
    // Enums
    // =========================================================================

    public enum RedstoneMode {
        IGNORE("ignore"),
        HOLD_WHILE_POWERED("hold"),
        DISABLE_WHILE_POWERED("release");

        private final String translationKey;

        RedstoneMode(String key) {
            this.translationKey = "block.littlelogistics.docking_station.redstone." + key;
        }

        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }
    }

    // =========================================================================
    // Station registry
    // =========================================================================

    @Nullable
    private UUID stationId;
    private boolean needsStationInit = true;
    /** Set true on load when stationId comes from NBT; triggers one-time registry validation. */
    private boolean needsStationValidation = false;

    // =========================================================================
    // Persisted configuration
    // =========================================================================

    private int idleTimeoutTicks = 100;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    // =========================================================================
    // Runtime state (not persisted)
    // =========================================================================

    @Nullable
    private Entity dockedVehicle;
    private boolean occupied;
    private int ticksSinceLastTransfer;

    // =========================================================================
    // Capability wrappers
    // =========================================================================

    private final StationItemHandler itemHandler = new StationItemHandler();
    private final StationFluidHandler fluidHandler = new StationFluidHandler();
    private final StationEnergyStorage energyStorage = new StationEnergyStorage();

    // =========================================================================
    // Constructor
    // =========================================================================

    public DockingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // =========================================================================
    // Station management
    // =========================================================================

    /**
     * Runs once on first tick after loading from disk when stationId was present.
     * Ensures the stationId still exists in the registry and that this BE's position
     * is tracked in the members set. Triggers re-init if the stationId is stale.
     */
    private void validateStation(ServerLevel serverLevel) {
        if (stationId == null) return;
        DockingStationRegistry registry = DockingStationRegistry.getOrCreate(serverLevel);
        if (registry.getStation(stationId) == null) {
            // Registry lost this station (corrupted, mergeInto on unloaded chunk, etc.) — re-init.
            stationId = null;
            needsStationInit = true;
        } else {
            // Ensure our position is in members (handles save/load desync).
            registry.joinStation(stationId, worldPosition);
        }
    }

    /**
     * Scans horizontal neighbours for existing stations, then joins or creates one.
     * Called on the first server tick so all adjacent blocks are fully loaded.
     */
    private void initStation(ServerLevel serverLevel) {
        needsStationInit = false;
        DockingStationRegistry registry = DockingStationRegistry.getOrCreate(serverLevel);

        // Collect distinct station UUIDs from neighbouring CONTROLLER block entities.
        // Only count neighbours whose stationId is actually present in the registry.
        Set<UUID> neighbourIds = new LinkedHashSet<>();
        Direction myFacing = getBlockState().getValue(DockingStationBlock.FACING);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbour instanceof DockingStationBlockEntity otherBe
                    && otherBe.getBlockState().getValue(DockingStationBlock.PART) == DockingStationPart.CONTROLLER
                    && otherBe.getBlockState().getValue(DockingStationBlock.FACING) == myFacing
                    && otherBe.stationId != null
                    && registry.getStation(otherBe.stationId) != null) {
                neighbourIds.add(otherBe.stationId);
            }
        }

        if (neighbourIds.isEmpty()) {
            stationId = registry.createStation(worldPosition);
        } else {
            UUID primaryId = null;
            for (UUID id : neighbourIds) {
                if (primaryId == null) {
                    primaryId = id;
                } else {
                    registry.mergeInto(primaryId, id, serverLevel);
                }
            }
            stationId = primaryId;
            registry.joinStation(stationId, worldPosition);
        }
        setChanged();
    }

    /** Unregisters this controller from its station. Called before block removal. */
    public void leaveStation() {
        if (stationId == null || !(level instanceof ServerLevel serverLevel)) return;
        DockingStationRegistry.getOrCreate(serverLevel).leaveStation(stationId, worldPosition);
        stationId = null;
    }

    /** Used by DockingStationRegistry.mergeInto to re-stamp this BE with a new UUID. */
    public void setStationId(UUID id) {
        this.stationId = id;
        setChanged();
        syncToClient();
    }

    /** Updates the station's display name in the registry. */
    public void setStationName(String name) {
        if (stationId == null || !(level instanceof ServerLevel serverLevel)) return;
        DockingStationRegistry.StationData data =
                DockingStationRegistry.getOrCreate(serverLevel).getStation(stationId);
        if (data != null) {
            data.name = name;
            DockingStationRegistry.getOrCreate(serverLevel).setDirty();
        }
    }

    /** Opens the docking station GUI for the given player. */
    public void openMenu(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        DockingStationRegistry registry = DockingStationRegistry.getOrCreate(serverLevel);
        DockingStationRegistry.StationData data = stationId != null ? registry.getStation(stationId) : null;
        String name            = data != null ? data.name              : "Docking Station";
        long itemsLoaded       = data != null ? data.itemsLoaded       : 0;
        long itemsUnloaded     = data != null ? data.itemsUnloaded     : 0;
        long fluidLoaded       = data != null ? data.fluidLoaded       : 0;
        long fluidUnloaded     = data != null ? data.fluidUnloaded     : 0;
        long energyLoaded      = data != null ? data.energyLoaded      : 0;
        long energyUnloaded    = data != null ? data.energyUnloaded    : 0;
        long vehiclesDocked    = data != null ? data.vehiclesDocked    : 0;
        int dockCount          = data != null ? data.members.size()    : 1;
        int timeout            = idleTimeoutTicks;
        int redstoneModeOrd    = redstoneMode.ordinal();
        BlockPos cPos          = worldPosition;

        player.openMenu(
                new SimpleMenuProvider(
                        (windowId, inv, pl) -> new DockingStationMenu(
                                windowId, inv, cPos,
                                name,
                                itemsLoaded, itemsUnloaded,
                                fluidLoaded, fluidUnloaded,
                                energyLoaded, energyUnloaded,
                                vehiclesDocked,
                                dockCount,
                                redstoneModeOrd, timeout),
                        Component.translatable("container.littlelogistics.docking_station")),
                buf -> {
                    buf.writeBlockPos(cPos);
                    buf.writeUtf(name, 256);
                    buf.writeLong(itemsLoaded);
                    buf.writeLong(itemsUnloaded);
                    buf.writeLong(fluidLoaded);
                    buf.writeLong(fluidUnloaded);
                    buf.writeLong(energyLoaded);
                    buf.writeLong(energyUnloaded);
                    buf.writeLong(vehiclesDocked);
                    buf.writeInt(dockCount);
                    buf.writeInt(redstoneModeOrd);
                    buf.writeInt(timeout);
                });
    }

    // =========================================================================
    // Stat helpers (delegated to registry)
    // =========================================================================

    private void trackItemsLoaded(long n) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addItemsLoaded(stationId, n);
    }

    private void trackItemsUnloaded(long n) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addItemsUnloaded(stationId, n);
    }

    private void trackFluidLoaded(long mb) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addFluidLoaded(stationId, mb);
    }

    private void trackFluidUnloaded(long mb) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addFluidUnloaded(stationId, mb);
    }

    private void trackEnergyLoaded(long fe) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addEnergyLoaded(stationId, fe);
    }

    private void trackEnergyUnloaded(long fe) {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addEnergyUnloaded(stationId, fe);
    }

    private void trackVehicleDocked() {
        if (stationId != null && level instanceof ServerLevel sl)
            DockingStationRegistry.getOrCreate(sl).addVehicleDocked(stationId);
    }

    // =========================================================================
    // Dock lifecycle
    // =========================================================================

    public boolean colorMatches(Entity vehicle) {
        return vehicle instanceof Colorable colorable
                && Colorable.colorsMatch(
                        colorable.getColor(),
                        getBlockState().getValue(DockingStationBlock.COLOR));
    }

    private boolean hasValidOccupant() {
        return dockedVehicle != null
                && dockedVehicle.isAlive()
                && dockedVehicle.level() == level
                && colorMatches(dockedVehicle);
    }

    public boolean canOccupyDock(Entity vehicle) {
        return vehicle.isAlive()
                && vehicle.level() == level
                && colorMatches(vehicle)
                && (dockedVehicle == null || dockedVehicle == vehicle);
    }

    public boolean isOccupiedBy(Entity vehicle) {
        return dockedVehicle == vehicle && hasValidOccupant();
    }

    public boolean isOccupiedBy(UUID vehicleId) {
        return dockedVehicle != null
                && dockedVehicle.getUUID().equals(vehicleId)
                && hasValidOccupant();
    }

    public void revalidateOccupant() {
        if (dockedVehicle != null && !hasValidOccupant()) {
            vacateDock();
        }
    }

    /**
     * Acquires this dock for {@code vehicle}. A dock is never silently stolen:
     * callers must handle a failed acquisition before changing their own state.
     */
    public boolean tryOccupyDock(Entity vehicle) {
        if (!canOccupyDock(vehicle)) return false;
        if (dockedVehicle != null && dockedVehicle != vehicle) return false;
        if (dockedVehicle == vehicle) return true;

        this.dockedVehicle = vehicle;
        this.occupied = true;
        this.ticksSinceLastTransfer = 0;

        IItemHandler vehicleItems = vehicle.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (vehicleItems != null) itemHandler.connect(vehicleItems);

        IFluidHandler vehicleFluids = vehicle.getCapability(Capabilities.FluidHandler.ENTITY, null);
        if (vehicleFluids != null) fluidHandler.connect(vehicleFluids);

        IEnergyStorage vehicleEnergy = vehicle.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        if (vehicleEnergy != null) energyStorage.connect(vehicleEnergy);

        trackVehicleDocked();
        setChanged();
        syncToClient();
        if (level != null) level.invalidateCapabilities(worldPosition);
        return true;
    }

    public void vacateDock() {
        this.dockedVehicle = null;
        this.occupied = false;
        itemHandler.disconnect();
        fluidHandler.disconnect();
        energyStorage.disconnect();

        setChanged();
        syncToClient();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    /** Releases this dock only when it is still owned by the supplied vehicle. */
    public boolean vacateDock(Entity vehicle) {
        if (dockedVehicle != vehicle) return false;
        vacateDock();
        return true;
    }

    /** UUID variant for releasing a persisted docking assignment after an entity reload. */
    public boolean vacateDock(UUID vehicleId) {
        if (dockedVehicle == null || !dockedVehicle.getUUID().equals(vehicleId)) return false;
        vacateDock();
        return true;
    }

    // =========================================================================
    // Hold logic
    // =========================================================================

    public boolean isHolding() {
        if (!hasValidOccupant()) return false;
        if (redstoneMode == RedstoneMode.HOLD_WHILE_POWERED && isPowered()) return true;
        if (redstoneMode == RedstoneMode.DISABLE_WHILE_POWERED && isPowered()) return false;
        return ticksSinceLastTransfer < idleTimeoutTicks;
    }

    private boolean isPowered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    public void notifyTransfer() {
        this.ticksSinceLastTransfer = 0;
    }

    // =========================================================================
    // Pass-through / follower docks (for multi-vehicle trains and convoys)
    // =========================================================================

    /**
     * Returns true if there is another docking station controller adjacent to the
     * vehicle position one step ahead in vehicleHeading, signalling that this vehicle
     * (e.g. a locomotive) should not stop here and instead pass through to let a
     * follower car dock.
     */
    public boolean shouldPassThrough(Entity vehicle, Direction vehicleHeading) {
        if (level == null) return false;
        Direction facing = getBlockState().getValue(DockingStationBlock.FACING);
        Direction inward = facing.getOpposite();
        BlockPos vehiclePos = worldPosition.relative(inward);
        BlockPos current = vehiclePos.relative(vehicleHeading);
        while (true) {
            BlockPos aheadControllerPos = current.relative(facing);
            BlockEntity ahead = level.getBlockEntity(aheadControllerPos);
            if (!(ahead instanceof DockingStationBlockEntity dock) || !isInSameDockLine(dock)) {
                return false;
            }
            if (dock.canOccupyDock(vehicle)) {
                return true;
            }
            current = current.relative(vehicleHeading);
        }
    }

    private boolean isInSameDockLine(DockingStationBlockEntity other) {
        return stationId != null
                && stationId.equals(other.stationId)
                && getBlockState().getValue(DockingStationBlock.FACING)
                == other.getBlockState().getValue(DockingStationBlock.FACING);
    }

    /**
     * Returns an ordered list of docking station controllers positioned for the
     * follower vehicles behind the current (head) vehicle.
     */
    public List<DockingStationBlockEntity> getFollowerDocks(Direction vehicleHeading) {
        List<DockingStationBlockEntity> docks = new ArrayList<>();
        if (level == null) return docks;
        Direction facing = getBlockState().getValue(DockingStationBlock.FACING);
        Direction inward = facing.getOpposite();
        // Start from the vehicle position, walk backward
        BlockPos vehiclePos = worldPosition.relative(inward);
        Direction behind = vehicleHeading.getOpposite();
        BlockPos current = vehiclePos.relative(behind);
        while (true) {
            BlockPos potentialController = current.relative(facing);
            BlockEntity be = level.getBlockEntity(potentialController);
            if (be instanceof DockingStationBlockEntity dbe && isInSameDockLine(dbe)) {
                docks.add(dbe);
                current = current.relative(behind);
            } else {
                break;
            }
        }
        return docks;
    }

    // =========================================================================
    // Tick
    // =========================================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DockingStationBlockEntity be) {
        if (be.needsStationValidation && level instanceof ServerLevel serverLevel) {
            be.needsStationValidation = false;
            be.validateStation(serverLevel);
        }
        if (be.needsStationInit && level instanceof ServerLevel serverLevel) {
            be.initStation(serverLevel);
        }
        be.revalidateOccupant();
        if (be.dockedVehicle != null) {
            be.ticksSinceLastTransfer++;
        }
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /** Adjusts the per-dock idle timeout by stepping through presets. delta=+1 or -1. */
    public void adjustTimeout(int delta) {
        int[] presets = DockingStationMenu.TIMEOUT_PRESETS;
        int currentIndex = 2;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == idleTimeoutTicks) { currentIndex = i; break; }
        }
        idleTimeoutTicks = presets[Math.clamp(currentIndex + delta, 0, presets.length - 1)];
        setChanged();
        syncToClient();
    }

    /** Sets the redstone mode from a GUI-sent ordinal. */
    public void setRedstoneMode(int ordinal) {
        RedstoneMode[] modes = RedstoneMode.values();
        redstoneMode = modes[Math.clamp(ordinal, 0, modes.length - 1)];
        setChanged();
        syncToClient();
    }

    // =========================================================================
    // Client sync
    // =========================================================================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        tag.putBoolean("Occupied", dockedVehicle != null);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IdleTimeoutTicks", idleTimeoutTicks);
        tag.putString("RedstoneMode", redstoneMode.name());
        if (stationId != null) tag.putUUID("StationId", stationId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("IdleTimeoutTicks")) idleTimeoutTicks = tag.getInt("IdleTimeoutTicks");
        if (tag.contains("RedstoneMode")) {
            try {
                redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode"));
            } catch (IllegalArgumentException e) {
                redstoneMode = RedstoneMode.IGNORE;
            }
        }
        if (tag.contains("Occupied")) occupied = tag.getBoolean("Occupied");
        if (tag.hasUUID("StationId")) {
            stationId = tag.getUUID("StationId");
            needsStationInit = false;
            needsStationValidation = true;
        }
    }

    // =========================================================================
    // Capability getters
    // =========================================================================

    @Nonnull public StationItemHandler getItemHandler()     { return itemHandler; }
    @Nonnull public StationFluidHandler getFluidHandler()   { return fluidHandler; }
    @Nonnull public StationEnergyStorage getEnergyStorage() { return energyStorage; }

    // =========================================================================
    // State getters
    // =========================================================================

    @Nullable public Entity getDockedVehicle()     { return dockedVehicle; }
    public boolean isOccupied() {
        return level != null && level.isClientSide ? occupied : hasValidOccupant();
    }
    public RedstoneMode getRedstoneMode()          { return redstoneMode; }
    public int getTicksSinceLastTransfer()         { return ticksSinceLastTransfer; }
    @Nullable public UUID getStationId()           { return stationId; }

    public BlockPos getVehicleBlockPos() {
        Direction inward = getBlockState().getValue(DockingStationBlock.FACING).getOpposite();
        return worldPosition.relative(inward);
    }

    public Vec3 getVehicleCenterPos() {
        return Vec3.atCenterOf(getVehicleBlockPos());
    }
    // =========================================================================
    // Inner capability wrappers
    // =========================================================================

    public class StationItemHandler implements IItemHandler {
        @Nullable private IItemHandler delegate;

        public void connect(IItemHandler h)  { this.delegate = h; }
        public void disconnect()             { this.delegate = null; }

        private boolean canTransfer() { return delegate != null && hasValidOccupant(); }

        @Override public int getSlots() { return canTransfer() ? delegate.getSlots() : 0; }
        @Override public @Nonnull ItemStack getStackInSlot(int slot) {
            return canTransfer() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
        }
        @Override public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!canTransfer()) return stack;
            ItemStack result = delegate.insertItem(slot, stack, simulate);
            if (!simulate && result.getCount() != stack.getCount()) {
                notifyTransfer();
                trackItemsLoaded(stack.getCount() - result.getCount());
            }
            return result;
        }
        @Override public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canTransfer()) return ItemStack.EMPTY;
            ItemStack result = delegate.extractItem(slot, amount, simulate);
            if (!simulate && !result.isEmpty()) {
                notifyTransfer();
                trackItemsUnloaded(result.getCount());
            }
            return result;
        }
        @Override public int getSlotLimit(int slot) { return canTransfer() ? delegate.getSlotLimit(slot) : 0; }
        @Override public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return canTransfer() && delegate.isItemValid(slot, stack);
        }
    }

    public class StationFluidHandler implements IFluidHandler {
        @Nullable private IFluidHandler delegate;

        public void connect(IFluidHandler h) { this.delegate = h; }
        public void disconnect()             { this.delegate = null; }

        private boolean canTransfer() { return delegate != null && hasValidOccupant(); }

        @Override public int getTanks() { return canTransfer() ? delegate.getTanks() : 0; }
        @Override public @Nonnull FluidStack getFluidInTank(int tank) {
            return canTransfer() ? delegate.getFluidInTank(tank) : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) { return canTransfer() ? delegate.getTankCapacity(tank) : 0; }
        @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return canTransfer() && delegate.isFluidValid(tank, stack);
        }
        @Override public int fill(@Nonnull FluidStack resource, FluidAction action) {
            if (!canTransfer()) return 0;
            int filled = delegate.fill(resource, action);
            if (action == FluidAction.EXECUTE && filled > 0) {
                notifyTransfer();
                trackFluidLoaded(filled);
            }
            return filled;
        }
        @Override public @Nonnull FluidStack drain(@Nonnull FluidStack resource, FluidAction action) {
            if (!canTransfer()) return FluidStack.EMPTY;
            FluidStack drained = delegate.drain(resource, action);
            if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
                notifyTransfer();
                trackFluidUnloaded(drained.getAmount());
            }
            return drained;
        }
        @Override public @Nonnull FluidStack drain(int maxDrain, FluidAction action) {
            if (!canTransfer()) return FluidStack.EMPTY;
            FluidStack drained = delegate.drain(maxDrain, action);
            if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
                notifyTransfer();
                trackFluidUnloaded(drained.getAmount());
            }
            return drained;
        }
    }

    public class StationEnergyStorage implements IEnergyStorage {
        @Nullable private IEnergyStorage delegate;

        public void connect(IEnergyStorage s) { this.delegate = s; }
        public void disconnect()              { this.delegate = null; }

        private boolean canTransfer() { return delegate != null && hasValidOccupant(); }

        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canTransfer()) return 0;
            int received = delegate.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                notifyTransfer();
                trackEnergyLoaded(received);
            }
            return received;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canTransfer()) return 0;
            int extracted = delegate.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                notifyTransfer();
                trackEnergyUnloaded(extracted);
            }
            return extracted;
        }
        @Override public int getEnergyStored()    { return canTransfer() ? delegate.getEnergyStored() : 0; }
        @Override public int getMaxEnergyStored() { return canTransfer() ? delegate.getMaxEnergyStored() : 0; }
        @Override public boolean canExtract()     { return canTransfer() && delegate.canExtract(); }
        @Override public boolean canReceive()     { return canTransfer() && delegate.canReceive(); }
    }
}
