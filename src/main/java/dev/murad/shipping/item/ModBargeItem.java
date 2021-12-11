package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.ModBargeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;

public class ModBargeItem extends AbstractEntityAddItem {
    public ModBargeItem(Properties p_i48526_2_) {
        super( p_i48526_2_);
    }

    @Override
    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new ModBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
