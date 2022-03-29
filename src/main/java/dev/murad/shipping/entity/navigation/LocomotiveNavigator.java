package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.rail.SwitchRail;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LocomotiveNavigator {
    private final Set<BlockPos> route;
    private final Set<BlockPos> visited;
    private final AbstractLocomotiveEntity locomotive;
    private final HashMap<BlockPos, Direction> decisionCache;

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

            locomotive.getRailHelper().getNext(railPos, locomotive.getDirection()).ifPresent(pair -> {
                var nextRail = pair.getFirst();
                var prevExitTake = pair.getSecond();
                var state = locomotive.getLevel().getBlockState(nextRail);
                if (state.getBlock() instanceof SwitchRail s && s.isAutomaticSwitching()){
                   var choices = s.getPossibleOutputDirections(state, prevExitTake.getOpposite()).stream().toList();
                   if (choices.size() == 1){
                      s.setRailState(state, locomotive.level, nextRail, prevExitTake.getOpposite(), choices.get(0));
                   } else if (choices.size() == 2){
                       Set<BlockPos> potential = new HashSet<>(route);
                       potential.removeAll(visited);
                       if(!decisionCache.containsKey(nextRail)){
                           var decision = locomotive.getRailHelper().pickCheaperDir(choices.get(0), choices.get(1), nextRail, RailHelper.samePositionHeuristicSet(potential));
                           decisionCache.put(nextRail, decision);
                       };
                       s.setRailState(state, locomotive.level, nextRail, prevExitTake.getOpposite(), decisionCache.get(nextRail));

                   }
                }

            });
        });
    }

    public void loadFromNbt(CompoundTag tag){

    }

    public CompoundTag saveToNbt(){
        var tag = new CompoundTag();
        return tag;
    }

}
