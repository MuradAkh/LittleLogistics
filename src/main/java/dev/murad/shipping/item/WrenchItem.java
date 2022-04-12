package dev.murad.shipping.item;

import dev.murad.shipping.util.LocoRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.Map;

public class WrenchItem extends Item {
    private static final Map<RailShape, RailShape> nextShapes = Map.ofEntries(
            Map.entry(RailShape.EAST_WEST, RailShape.NORTH_SOUTH),
            Map.entry(RailShape.NORTH_SOUTH, RailShape.NORTH_EAST),
            Map.entry(RailShape.NORTH_EAST, RailShape.NORTH_WEST),
            Map.entry(RailShape.NORTH_WEST, RailShape.SOUTH_WEST),
            Map.entry(RailShape.SOUTH_WEST, RailShape.SOUTH_EAST),
            Map.entry(RailShape.SOUTH_EAST, RailShape.EAST_WEST)
    );


    public WrenchItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        var state = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if(state.is(Blocks.RAIL)){
            var shape = state.getValue(RailBlock.SHAPE);
            if(shape.isAscending()){
                return InteractionResult.PASS;
            }
            if(!pContext.getLevel().isClientSide()){
                pContext.getLevel().setBlock(pContext.getClickedPos(),
                        state.setValue(RailBlock.SHAPE, nextShapes.getOrDefault(shape, shape)), 2);
            }
            return InteractionResult.SUCCESS;
        } else {
            return super.useOn(pContext);
        }
    }
}
