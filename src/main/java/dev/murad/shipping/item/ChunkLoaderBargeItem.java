package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.vessel.barge.ChunkLoaderBargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

public class ChunkLoaderBargeItem extends AbstractEntityAddItem{
    public ChunkLoaderBargeItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }

    @Override
    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new ChunkLoaderBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
