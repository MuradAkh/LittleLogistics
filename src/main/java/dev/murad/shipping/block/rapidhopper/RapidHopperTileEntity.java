package dev.murad.shipping.block.rapidhopper;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;

public class RapidHopperTileEntity extends HopperBlockEntity {
    private int rapidCooldown = 0;
    private final static int SEARCH_COOLDOWN = 8;
    private final static int RAPID_COOLDOWN = 1;
    public RapidHopperTileEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(pWorldPosition, pBlockState);
    }

    public static void pushItemsTick(Level pLevel, BlockPos pPos, BlockState pState, RapidHopperTileEntity pBlockEntity) {
        pBlockEntity.setCooldown(0);
        pBlockEntity.rapidCooldown--;
        if (pBlockEntity.rapidCooldown <= 0) {
            if(!tryMoveItems(pLevel, pPos, pState, pBlockEntity, () -> suckInItems(pLevel, pBlockEntity))){
                pBlockEntity.rapidCooldown = SEARCH_COOLDOWN;
            }else {
                pBlockEntity.rapidCooldown = RAPID_COOLDOWN;
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModTileEntitiesTypes.RAPID_HOPPER.get();
    }

}
