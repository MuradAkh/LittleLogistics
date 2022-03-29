package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LocomotiveNavigator {
    private Set<BlockPos> route;
    private Set<BlockPos> visited;
    private AbstractLocomotiveEntity locomotive;
    private HashMap<BlockPos, Direction> decisionCache;

    private void reset(){
        this.visited.clear();
        this.route.clear();
        this.decisionCache.clear();
    }

    public LocomotiveNavigator(AbstractLocomotiveEntity locomotive){
        reset();
        this.locomotive = locomotive;
        this.decisionCache = new HashMap<>();
        this.visited = new HashSet<>();
        this.route = new HashSet<>();
    }

    public void serverTick(){
        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level).ifPresent(railPos ->{
            if(route.contains(railPos)){
                visited.add(railPos);
            }
            if(visited.size() == route.size()){
                visited.clear();
            }
            decisionCache.remove(railPos);
        });
    }

    public void loadFromNbt(CompoundTag tag){

    }

    public CompoundTag saveToNbt(){
        var tag = new CompoundTag();
        return tag;
    }

}
