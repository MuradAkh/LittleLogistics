package dev.murad.shipping.global;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrainChunkManagerManager extends SavedData {
    private final Table<ResourceKey<Level>, UUID, PlayerTrainChunkManager> managers = TreeBasedTable.create();
    MinecraftServer server;

    public static TrainChunkManagerManager get(MinecraftServer server) {
        return server
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        tag -> new TrainChunkManagerManager(tag, server),
                        () -> new TrainChunkManagerManager(server),
                        "littlelogistics:trainchunkmanagermanager");
    }

    private TrainChunkManagerManager(MinecraftServer server) {
        this.server = server;
    }

    private TrainChunkManagerManager(CompoundTag tag, MinecraftServer server) {
        this.server = server;
        for (Tag cell : tag.getList("saved", 10)) {
            if (cell instanceof CompoundTag compoundTag) {
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(compoundTag.getString("level")));
                ServerLevel level = server.getLevel(dimension);
                if (level == null) {
                    return;
                }
                UUID uuid = compoundTag.getUUID("UUID");
                server.execute(() -> {
                    PlayerTrainChunkManager.getSaved(level, uuid).ifPresent(manager -> {
                        managers.put(dimension, uuid, manager);
                    });
                });
            }
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag topList = new ListTag();
        for (var cell : managers.cellSet()) {
            CompoundTag inner = new CompoundTag();
            inner.putString("level", cell.getRowKey().location().toString());
            inner.putUUID("UUID", cell.getColumnKey());
            topList.add(inner);
        }
        tag.put("saved", topList);
        return tag;
    }

    public void enroll(PlayerTrainChunkManager playerTrainChunkManager) {
        managers.put(playerTrainChunkManager.getLevel().dimension(), playerTrainChunkManager.getUuid(), playerTrainChunkManager);
        setDirty();
    }

    public Set<PlayerTrainChunkManager> getManagers(ResourceKey<Level> dimension) {
        return new HashSet<>(managers.row(dimension).values());
    }

    public Set<PlayerTrainChunkManager> getManagers(UUID uuid) {
        return new HashSet<>(managers.column(uuid).values());
    }

    public int countVehicles(UUID uuid) {
        return getManagers(uuid).stream().reduce(0, (i, manager) -> i + manager.getNumVehicles(), Integer::sum);
    }
}
