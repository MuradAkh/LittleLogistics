package dev.murad.shipping.global;

import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.LinkableEntityHead;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class TrainChunkManager extends SavedData {
    private final static TicketType<Integer> TICKET_TYPE = TicketType.create("littlelogistics:trainticket", Integer::compareTo);
    private final Set<LinkableEntity<? extends Entity>> enrolled = new HashSet<>();
    private final Set<ChunkPos> tickets = new HashSet<>();
    private final ServerLevel level;

    public static TrainChunkManager get(ServerLevel level){
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent((tag) -> new TrainChunkManager(tag, level), () -> new TrainChunkManager(level), "littlelogistics:chunkmanager");
    }


    public static <T extends Entity & LinkableEntity<T>>  void enroll(T entity){
        if(!entity.level.isClientSide) {
            var manager = TrainChunkManager.get((ServerLevel) entity.level);
            manager.enrolled.add(entity);
            manager.onChanged();
        }
    }

    public void tick(){
        enrolled.forEach(entityHead -> entityHead.getTrain()
                .asList()
                .stream()
                .filter(entity -> !((ServerLevel) entity.level).isPositionEntityTicking(entity.getBlockPos()))
                .forEach(Entity::tick));

        if(enrolled.stream()
                .map(e -> (Entity) e)
                .map(e -> e.chunkPosition().toLong() != new ChunkPos(new BlockPos(e.xOld, e.yOld, e.zOld)).toLong())
                .reduce(Boolean.FALSE, Boolean::logicalOr)){
            onChanged();
        }
    }

    private void onChanged() {
        Set<ChunkPos> required = new HashSet<>();
        enrolled.stream().map(this::computeRequiredTickets).forEach(required::addAll);
        removeUnneededTickets(required);
        addNeededTickets(required);
    }

    public Set<ChunkPos> computeRequiredTickets(LinkableEntity<? extends Entity> entity){
        var set = new HashSet<ChunkPos>();
        entity.getTrain().asList().forEach(e -> set.add(e.chunkPosition()));
        set.addAll(ChunkPos.rangeClosed(((Entity) entity).chunkPosition(), 1).collect(Collectors.toList()));
        return set;
    }

    public void removeUnneededTickets(Set<ChunkPos> required){
        Set.copyOf(tickets)
                .stream()
                .filter(pos -> !required.contains(pos))
                .forEach(chunkPos -> {
                    level.getChunkSource().removeRegionTicket(TICKET_TYPE, chunkPos, 0, 0);
                    tickets.remove(chunkPos);
                });
    }

    public void addNeededTickets(Set<ChunkPos> required){
        required
            .stream()
            .filter(pos -> !tickets.contains(pos))
                .collect(Collectors.toSet()) // avoid mutation on the go
                .forEach(chunkPos -> {
                    level.getChunkSource().addRegionTicket(TICKET_TYPE, chunkPos, 0, 0);
                    tickets.add(chunkPos);
                });
    }


    TrainChunkManager(ServerLevel level){
        this.level = level;

    }

    TrainChunkManager(CompoundTag tag, ServerLevel level){
        this.level = level;

    }

    @Override
    public CompoundTag save(CompoundTag p_77763_) {
        return null;
    }
}
