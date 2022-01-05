package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class FishingBargeEntity extends AbstractBargeEntity {
    private int ticksDeployable = 0;

    public FishingBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    public FishingBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.FISHING_BARGE.get(), worldIn, x, y, z);
    }

    @Override
    protected void doInteract(PlayerEntity player) {
    }

    @Override
    public void tick(){
        super.tick();
        tickWaterOnSidesCheck();

    }

    private void tickWaterOnSidesCheck(){
        if(hasWaterOnSides()){
            ticksDeployable++;
        }else {
            ticksDeployable = 0;
        }

    }

    @Override
    public Item getDropItem() {
        return ModItems.FISHING_BARGE.get();
    }

    public Status getStatus(){
        return hasWaterOnSides() ? getNonStashedStatus() : Status.STASHED;
    }

    private Status getNonStashedStatus(){
        if (ticksDeployable < 40){
            return Status.TRANSITION;
        } else {
            return this.applyWithDominant(ISpringableEntity::hasWaterOnSides)
                    .reduce(true, Boolean::logicalAnd)
                    ? Status.DEPLOYED : Status.TRANSITION;
        }
    }

    public enum Status {
        STASHED,
        DEPLOYED,
        TRANSITION
    }
}
