package dev.murad.shipping.global;

import dev.murad.shipping.util.LinkableEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;
import java.util.stream.Collectors;


public class PlayerTrainChunkManager extends SavedData {
    private final static TicketType<UUID> TRAVEL_TICKET = TicketType.create("littlelogistics:travelticket", UUID::compareTo);
    private final static TicketType<UUID> LOAD_TICKET = TicketType.create("littlelogistics:loadticket", UUID::compareTo, 200);
    private final Set<Entity> enrolled = new HashSet<>();
    private final Set<ChunkPos> tickets = new HashSet<>();
    private final Set<ChunkPos> toLoad = new HashSet<>();
    private boolean active = false;
    @Getter
    private final UUID uuid;
    @Getter
    private final ServerLevel level;

    public static PlayerTrainChunkManager get(ServerLevel level, UUID uuid){
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent((tag) -> new PlayerTrainChunkManager(tag, level, uuid), () -> new PlayerTrainChunkManager(level, uuid), "littlelogistics:chunkmanager-" + uuid.toString());
    }

    public static Optional<PlayerTrainChunkManager> getSaved(ServerLevel level, UUID uuid){
        DimensionDataStorage storage = level.getDataStorage();
        return Optional.ofNullable(storage.get((tag) -> new PlayerTrainChunkManager(tag, level, uuid),"littlelogistics:chunkmanager-" + uuid.toString()));
    }



    public static void enroll(Entity entity, UUID uuid){
        if(!entity.level.isClientSide) {
            var manager = PlayerTrainChunkManager.get((ServerLevel) entity.level, uuid);
            manager.enrolled.add(entity);
            manager.onChanged();
        }
    }

    public void deactivate(){
        updateToLoad();
        enrolled.clear();
        tickets.forEach(chunkPos -> level.getChunkSource().removeRegionTicket(TRAVEL_TICKET, chunkPos, 0, uuid));
        tickets.clear();
        active = false;
    }

    private List<Entity> getAllSubjectEntities(Entity entity){
        List<Entity> subjects = new ArrayList<>();
        if(entity instanceof LinkableEntity<?> l){ // need to refactor this somehow to be more generic
           for(var e : l.getTrain().asListOfTugged()){
               if(e instanceof Entity){
                   subjects.add((Entity) e);
               }
           }
        }

        if(entity.getParts() != null){
            subjects.addAll(List.of(entity.getParts()));
        }
        return subjects;
    }

    private void updateToLoad() {
        toLoad.clear();
        enrolled.forEach(e -> toLoad.addAll(getAllSubjectEntities(e).stream().map(Entity::chunkPosition).collect(Collectors.toSet())));
    }

    public void activate(){
        active = true;
        toLoad.forEach(chunkPos -> level.getChunkSource().addRegionTicket(LOAD_TICKET, chunkPos, 2, uuid));
    }

    public void tick(){
        boolean changed = enrolled.removeIf(e -> !((Entity) e).isAlive());
        if(!active){
            return;
        }

        enrolled.forEach(entityHead -> getAllSubjectEntities(entityHead)
                .stream()
                .filter(entity -> !((ServerLevel) entity.level).isPositionEntityTicking(entity.blockPosition()))
                .forEach(Entity::tick));

        if(changed || enrolled.stream()
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
        setDirty();
    }

    public Set<ChunkPos> computeRequiredTickets(Entity entity){
        var set = new HashSet<ChunkPos>();
        getAllSubjectEntities(entity).forEach(e -> set.add(e.chunkPosition()));
        set.addAll(ChunkPos.rangeClosed(((Entity) entity).chunkPosition(), 1).collect(Collectors.toList()));
        return set;
    }

    public void removeUnneededTickets(Set<ChunkPos> required){
        Set.copyOf(tickets)
                .stream()
                .filter(pos -> !required.contains(pos))
                .forEach(chunkPos -> {
                    level.getChunkSource().removeRegionTicket(TRAVEL_TICKET, chunkPos, 0, uuid);
                    tickets.remove(chunkPos);
                });
    }

    public void addNeededTickets(Set<ChunkPos> required){
        required
            .stream()
            .filter(pos -> !tickets.contains(pos))
                .collect(Collectors.toSet()) // avoid mutation on the go
                .forEach(chunkPos -> {
                    level.getChunkSource().addRegionTicket(TRAVEL_TICKET, chunkPos, 0, uuid);
                    tickets.add(chunkPos);
                });
    }


    PlayerTrainChunkManager(ServerLevel level, UUID uuid){
        this.level = level;
        this.uuid = uuid;
        TrainChunkManagerManager.get(level.getServer()).enroll(this);
    }

    PlayerTrainChunkManager(CompoundTag tag, ServerLevel level, UUID uuid){
        this.level = level;
        this.uuid = uuid;
        Arrays.stream(tag.getLongArray("chunksToLoad")).forEach(chunk -> toLoad.add(new ChunkPos(chunk)));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        updateToLoad();
        tag.putLongArray("chunksToLoad", toLoad.stream().map(ChunkPos::toLong).collect(Collectors.toList()));
        return tag;
    }
}
