package dev.murad.shipping.item;

import dev.murad.shipping.block.portal.IPortalBlock;
import dev.murad.shipping.block.portal.IPortalTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;


public class PortalLinkerItem extends Item {
    public PortalLinkerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
//        if(pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pContext.getClickedPos());
        if(state.getBlock() instanceof IPortalBlock){
            Function<UseOnContext, InteractionResult> func = getState(pContext.getItemInHand()) == State.READY ?
                this::handleFirstLink : this::handleSecondLink;

            return func.apply(pContext);
        } else {
            return InteractionResult.PASS;
        }
    }

    private InteractionResult handleFirstLink(UseOnContext context){
        Level level = context.getLevel();
        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        IPortalBlock portal = (IPortalBlock) state.getBlock();

        if(!portal.checkValidDimension(level)){
            // TODO msg user

            return InteractionResult.PASS;
        }

        if (state.getValue(IPortalBlock.PORTAL_MODE) == IPortalBlock.PortalMode.UNLINKED){
            setTarget(level, pos, stack, ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
            return InteractionResult.SUCCESS;
        } else return InteractionResult.PASS;

    }

    private InteractionResult handleSecondLink(UseOnContext context){
        Level level = context.getLevel();
        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        IPortalBlock portal = (IPortalBlock) state.getBlock();

        if(!portal.checkValidLinkPair(level, stack, pos, getDimension(stack))){
            // TODO msg user

            return InteractionResult.PASS;
        }

        if (state.getValue(IPortalBlock.PORTAL_MODE) == IPortalBlock.PortalMode.UNLINKED){
            if(level.getBlockEntity(pos) instanceof IPortalTileEntity be){
                be.linkPortals(getSavedPos(stack));
                reset(stack);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private void setTarget(Level level, BlockPos pos, ItemStack stack, String type){
        var nbt = stack.getOrCreateTag();
        nbt.putString("dimension", level.dimension().location().toString());
        nbt.putString("type", type);
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
    }

    public BlockPos getSavedPos(ItemStack stack){
        var nbt = stack.getOrCreateTag();

        return new BlockPos(nbt.getInt("x"),
                nbt.getInt("y"), nbt.getInt("z"));
    }

    private void reset(ItemStack stack){
        stack.removeTagKey("x");
        stack.removeTagKey("y");
        stack.removeTagKey("z");
        stack.removeTagKey("dimension");
    }

    private ResourceKey<Level> getDimension(ItemStack stack){
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(stack.getOrCreateTag().getString("dimension")));
    }

    public static State getState(ItemStack stack) {
        if(stack.getTag() != null && stack.getTag().contains("dimension"))
            return State.WAITING_NEXT;
        return State.READY;
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if (getState(stack).equals(State.WAITING_NEXT)){
            tooltip.add(new TextComponent(getDimension(stack).toString()));
            tooltip.add(new TextComponent(getSavedPos(stack).toString()));
        }
    }

    public enum State {
        WAITING_NEXT,
        READY
    }
}
