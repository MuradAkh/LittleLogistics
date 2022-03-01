package dev.murad.shipping.entity.custom.train;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.util.LinkableEntityHead;
import dev.murad.shipping.util.RailUtils;
import dev.murad.shipping.util.Train;
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

public class LocomotiveEntity extends AbstractTrainCar implements LinkableEntityHead<AbstractTrainCar> {
    private boolean move = false;
    private boolean doflip = false;
    public LocomotiveEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public LocomotiveEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level, aDouble, aDouble1, aDouble2);
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
                this.setDeltaMovement(Vec3.ZERO);
                doflip = true;
        }
        return InteractionResult.PASS;
    }


    @Override
    public void tick(){
        super.tickLoad();
        super.tickMinecart();

        if(!this.level.isClientSide){
            prevent180();

        }
        tickAdjustments();
        if(!this.level.isClientSide){
            if(move) {
                doMove();
            }else{
                setDeltaMovement(Vec3.ZERO);
            }
        }

        if(this.dominated.isPresent()){
            var r = RailUtils.traverseBi(getOnPos().above(), this.level, (w, p) ->
                    RailUtils.getRail(dominated.get().getOnPos().above(), this.level).map(rp -> rp.equals(p)).orElse(false), 5);
            r.ifPresent(pair -> this.setYRot(pair.getFirst().getOpposite().toYRot()));
        } else if(doflip){
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(getDirection().getOpposite().toYRot());
            doflip = false;
        }



    }



    private double getSpeedModifier(){
        // adjust speed based on slope etc.
        var state = this.level.getBlockState(this.getOnPos().above());
        if (state.is(Blocks.POWERED_RAIL)){
            if(!state.getValue(PoweredRailBlock.POWERED)){
                return 0;
            } else {
                return 0.005;
            }
        }
        return getRailShape().map(shape -> switch (shape) {
            case NORTH_SOUTH, EAST_WEST -> 0.07;
            case SOUTH_WEST, NORTH_WEST, SOUTH_EAST, NORTH_EAST -> 0.03;
            default -> this.getDeltaMovement().y > 0 ? 0.02 : 0.01;
        }).orElse(0d);
    }

    private void doMove() {
        var dir = this.getDirection();
        var dirvel = new Vec3(Math.abs(dir.getStepX()), 0, Math.abs(dir.getStepZ()));
        if(Math.abs(this.getDeltaMovement().dot(dirvel)) < 0.12){
            var mod = this.getSpeedModifier();
            this.push(dir.getStepX() * mod, 0, dir.getStepZ() * mod);
        }
    }

    @Override
    public void setDominated(AbstractTrainCar entity) {
        dominated = Optional.of(entity);
    }

    @Override
    public void setDominant(AbstractTrainCar entity) {
    }

    @Override
    public void removeDominated() {
        dominated = Optional.empty();
        this.train.setTail(this);
    }

    @Override
    public void removeDominant() {

    }


    @Override
    public void setTrain(Train train) {
        this.train = train;
    }
}
