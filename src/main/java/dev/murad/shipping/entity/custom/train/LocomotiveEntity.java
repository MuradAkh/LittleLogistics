package dev.murad.shipping.entity.custom.train;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.RailUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class LocomotiveEntity extends TrainCar {
    private boolean move = false;
    private boolean doflip = false;
    public LocomotiveEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public LocomotiveEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.LOCOMOTIVE.get(), level, aDouble, aDouble1, aDouble2);


    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if(!pHand.equals(InteractionHand.MAIN_HAND)){
            return InteractionResult.PASS;
        }
        if(!this.level.isClientSide){
            if (!pPlayer.isCrouching()) {
                move = !move;
            }
        }
        if(pPlayer.isCrouching()){
            if(this.getDeltaMovement().dot(new Vec3(1,0,1)) < 0.005) {
                this.setDeltaMovement(Vec3.ZERO);
                doflip = true;
            } else if (!this.level.isClientSide)
                pPlayer.sendMessage(new TextComponent("Locomotive must be stationary"), this.getUUID());
        }
        return InteractionResult.PASS;
    }


    @Override
    public void tick(){
        super.tickMinecart();
        tickAdjustments();
        if(!this.level.isClientSide){
            prevent180();

        }
        if(!this.level.isClientSide){
            if(move) {
                doMove();
            }else{
                setDeltaMovement(Vec3.ZERO);
            }
        }

        if(doflip){
            this.setYRot(getDirection().getOpposite().toYRot());
            doflip = false;
        }

    }

    private void prevent180() {
        var dir = Vec3.atLowerCornerOf(this.getDirection().getNormal());
        var vel = this.getDeltaMovement();
        var mag = vel.multiply(dir).normalize().dot(new Vec3(1, 1, 1)) < 0 ? 0 : 1;
        var fixer = new Vec3(fixUtil(dir.x, mag), dir.y, fixUtil(dir.z, mag));
        this.setDeltaMovement(this.getDeltaMovement().multiply(fixer));
    }

    private double fixUtil(double v, int mag) {
        return v == 0 ? 1 : v * mag;
    }

    private double getSpeedModifier(){
        // adjust speed based on slope etc.
        var state = this.level.getBlockState(this.getOnPos().above());
        if (state.is(Blocks.POWERED_RAIL)){
            if(state.getValue(PoweredRailBlock.POWERED)){
                return 0.2;
            } else {
                return 0;
            }
        } else {
            return 0.15;
        }
    }

    private void doMove() {
        this.setDeltaMovement(Vec3.atLowerCornerOf(this.getDirection().getNormal()).scale(getSpeedModifier()));
    }
}
