package dev.murad.shipping.entity.custom.train;

import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LocomotiveEntity extends TrainCar {
    private boolean move = false;
    public LocomotiveEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public LocomotiveEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.LOCOMOTIVE.get(), level, aDouble, aDouble1, aDouble2);

    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if(!this.level.isClientSide && pHand.equals(InteractionHand.MAIN_HAND)){
            move = !move;
        }
        return InteractionResult.PASS;
    }


    @Override
    public void tick(){
        if(!this.level.isClientSide && move){
            doMove();
        }
        super.tick();
    }

    private void doMove() {
        this.setDeltaMovement(this.getDeltaMovement().add(Vec3.atCenterOf(this.getDirection().getNormal()).scale(0.01)));
    }
}
