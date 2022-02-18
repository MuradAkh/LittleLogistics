package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.ShippingConfig;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class TugPathNavigator extends WaterBoundPathNavigation {
    public TugPathNavigator(Mob p_i45873_1_, Level p_i45873_2_) {
        super(p_i45873_1_, p_i45873_2_);
        setMaxVisitedNodesMultiplier(ShippingConfig.Server.TUG_PATHFINDING_MULTIPLIER.get());
    }

    @Override
    protected PathFinder createPathFinder(int p_179679_1_) {
        this.nodeEvaluator = new TugNodeProcessor();
        return new PathFinder(this.nodeEvaluator, p_179679_1_);
    }

    @Override
    public boolean moveTo(double p_75492_1_, double p_75492_3_, double p_75492_5_, double p_75492_7_) {
        return this.moveTo(this.createPath(p_75492_1_, p_75492_3_, p_75492_5_, 0), p_75492_7_);
    }

    @Override
    protected void doStuckDetection(Vec3 p_179677_1_) {
        if (this.tick - this.lastStuckCheck > 100) {
            if (p_179677_1_.distanceToSqr(this.lastStuckCheckPos) < 2.25D) {
                this.stop();
            }

            this.lastStuckCheck = this.tick;
            this.lastStuckCheckPos = p_179677_1_;
        }

        if (this.path != null && !this.path.isDone()) {
            BlockPos vector3i = this.path.getNextNodePos();
            if (vector3i.equals(this.timeoutCachedNode)) {
                this.timeoutTimer += Util.getMillis() - this.lastTimeoutCheck;
            } else {
                this.timeoutCachedNode = vector3i;
                double d0 = p_179677_1_.distanceTo(Vec3.atCenterOf(this.timeoutCachedNode));
                this.timeoutLimit = this.mob.getSpeed() > 0.0F ? (d0 / (double)this.mob.getSpeed()) * 1000 : 0.0D;
            }

            if (this.timeoutLimit > 0.0D && (double)this.timeoutTimer > this.timeoutLimit * 2.0D) {
                this.timeoutCachedNode = BlockPos.ZERO;
                this.timeoutTimer = 0L;
                this.timeoutLimit = 0.0D;
                this.stop();
            }

            this.lastTimeoutCheck = Util.getMillis();
        }

    }
}
