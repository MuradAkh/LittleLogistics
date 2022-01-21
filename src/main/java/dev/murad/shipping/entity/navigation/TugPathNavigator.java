package dev.murad.shipping.entity.navigation;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.SwimNodeProcessor;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.world.World;

public class TugPathNavigator extends SwimmerPathNavigator {
    public TugPathNavigator(MobEntity p_i45873_1_, World p_i45873_2_) {
        super(p_i45873_1_, p_i45873_2_);
        setMaxVisitedNodesMultiplier(5);
    }

    @Override
    protected PathFinder createPathFinder(int p_179679_1_) {
        this.nodeEvaluator = new TugNodeProcessor();
        return new PathFinder(this.nodeEvaluator, p_179679_1_);
    }
}
