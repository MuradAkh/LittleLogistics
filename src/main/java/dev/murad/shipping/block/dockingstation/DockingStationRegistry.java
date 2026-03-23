package dev.murad.shipping.block.dockingstation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Server-side persistent registry of docking station groups ("stations").
 * Multiple adjacent docking station controllers share one StationData entry,
 * identified by a UUID stored in each controller's block entity.
 */
public class DockingStationRegistry extends SavedData {

    public static final String NAME = "littlelogistics_dock_stations";

    // =========================================================================
    // Station data
    // =========================================================================

    public static class StationData {
        public String name;
        public final Set<BlockPos> members = new HashSet<>();
        public long itemsLoaded, itemsUnloaded;
        public long fluidLoaded, fluidUnloaded;   // millibuckets
        public long energyLoaded, energyUnloaded; // FE
        public long vehiclesDocked;

        public StationData(String name) {
            this.name = name;
        }

        void save(CompoundTag tag) {
            tag.putString("Name", name);
            tag.putLongArray("Members", members.stream().mapToLong(BlockPos::asLong).toArray());
            tag.putLong("ItemsLoaded",    itemsLoaded);
            tag.putLong("ItemsUnloaded",  itemsUnloaded);
            tag.putLong("FluidLoaded",    fluidLoaded);
            tag.putLong("FluidUnloaded",  fluidUnloaded);
            tag.putLong("EnergyLoaded",   energyLoaded);
            tag.putLong("EnergyUnloaded", energyUnloaded);
            tag.putLong("VehiclesDocked", vehiclesDocked);
        }

        static StationData load(CompoundTag tag) {
            StationData d = new StationData(tag.getString("Name"));
            for (long l : tag.getLongArray("Members")) d.members.add(BlockPos.of(l));
            d.itemsLoaded    = tag.getLong("ItemsLoaded");
            d.itemsUnloaded  = tag.getLong("ItemsUnloaded");
            d.fluidLoaded    = tag.getLong("FluidLoaded");
            d.fluidUnloaded  = tag.getLong("FluidUnloaded");
            d.energyLoaded   = tag.getLong("EnergyLoaded");
            d.energyUnloaded = tag.getLong("EnergyUnloaded");
            d.vehiclesDocked = tag.getLong("VehiclesDocked");
            return d;
        }
    }

    // =========================================================================
    // Instance
    // =========================================================================

    private final Map<UUID, StationData> stations = new HashMap<>();

    // =========================================================================
    // Factory
    // =========================================================================

    public static DockingStationRegistry getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DockingStationRegistry::new, DockingStationRegistry::load, null),
                NAME);
    }

    // =========================================================================
    // Station management
    // =========================================================================

    public UUID createStation(BlockPos firstMember) {
        UUID id = UUID.randomUUID();
        StationData data = new StationData("Docking Station");
        data.members.add(firstMember);
        stations.put(id, data);
        setDirty();
        return id;
    }

    public void joinStation(UUID id, BlockPos member) {
        StationData data = stations.get(id);
        if (data != null) {
            data.members.add(member);
            setDirty();
        }
    }

    /**
     * Merges the {@code discard} station into {@code keep}, combining stats and
     * re-stamping all members of the discarded station with the new UUID.
     */
    public void mergeInto(UUID keep, UUID discard, ServerLevel level) {
        StationData keepData    = stations.get(keep);
        StationData discardData = stations.remove(discard);
        if (keepData == null || discardData == null) return;

        keepData.itemsLoaded    += discardData.itemsLoaded;
        keepData.itemsUnloaded  += discardData.itemsUnloaded;
        keepData.fluidLoaded    += discardData.fluidLoaded;
        keepData.fluidUnloaded  += discardData.fluidUnloaded;
        keepData.energyLoaded   += discardData.energyLoaded;
        keepData.energyUnloaded += discardData.energyUnloaded;
        keepData.vehiclesDocked += discardData.vehiclesDocked;

        for (BlockPos pos : discardData.members) {
            keepData.members.add(pos);
            if (level.getBlockEntity(pos) instanceof DockingStationBlockEntity be) {
                be.setStationId(keep);
            }
        }
        setDirty();
    }

    public void leaveStation(UUID id, BlockPos member) {
        StationData data = stations.get(id);
        if (data == null) return;
        data.members.remove(member);
        if (data.members.isEmpty()) stations.remove(id);
        setDirty();
    }

    @Nullable
    public StationData getStation(UUID id) {
        return stations.get(id);
    }

    // =========================================================================
    // Stat increments
    // =========================================================================

    public void addItemsLoaded(UUID id, long n)    { mutate(id, d -> d.itemsLoaded    += n); }
    public void addItemsUnloaded(UUID id, long n)  { mutate(id, d -> d.itemsUnloaded  += n); }
    public void addFluidLoaded(UUID id, long mb)   { mutate(id, d -> d.fluidLoaded    += mb); }
    public void addFluidUnloaded(UUID id, long mb) { mutate(id, d -> d.fluidUnloaded  += mb); }
    public void addEnergyLoaded(UUID id, long fe)  { mutate(id, d -> d.energyLoaded   += fe); }
    public void addEnergyUnloaded(UUID id, long fe){ mutate(id, d -> d.energyUnloaded += fe); }
    public void addVehicleDocked(UUID id)          { mutate(id, d -> d.vehiclesDocked++); }

    private void mutate(UUID id, Consumer<StationData> fn) {
        StationData d = stations.get(id);
        if (d != null) { fn.accept(d); setDirty(); }
    }

    // =========================================================================
    // Serialisation
    // =========================================================================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, StationData> entry : stations.entrySet()) {
            CompoundTag stationTag = new CompoundTag();
            stationTag.putUUID("Id", entry.getKey());
            entry.getValue().save(stationTag);
            list.add(stationTag);
        }
        tag.put("Stations", list);
        return tag;
    }

    public static DockingStationRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        DockingStationRegistry registry = new DockingStationRegistry();
        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag stationTag = list.getCompound(i);
            UUID id = stationTag.getUUID("Id");
            registry.stations.put(id, StationData.load(stationTag));
        }
        return registry;
    }
}
