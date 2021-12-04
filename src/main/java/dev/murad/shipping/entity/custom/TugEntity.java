package dev.murad.shipping.entity.custom;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public class TugEntity extends MobEntity {

    public TugEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    public TugEntity(World worldIn, double x, double y, double z) {
        this(ModEntityTypes.TUG.get(), worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

//    @Override
//    public ActionResultType interact(PlayerEntity player, Hand hand) {
//        if (player.isSecondaryUseActive()) {
//            return ActionResultType.PASS;
//        } else {
//            if (!this.level.isClientSide) {
//                doInteract(player);
//            }
//            return ActionResultType.PASS;
//        }
//
//    }


}
