package dev.murad.shipping.item;

import dev.murad.shipping.block.portal.IPortalBlock;
import dev.murad.shipping.block.portal.IPortalTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;


public class PortalLinkerItem extends Item {
    public static final String DIMENSION_TAG = "dimension";
    public static final String TYPE_TAG = "type";
    public static final String X_TAG = "x";
    public static final String Y_TAG = "y";
    public static final String Z_TAG = "z";
    public static final String SCALE_TAG = "scale";

    public PortalLinkerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pContext.getClickedPos());
        if(state.getBlock() instanceof IPortalBlock){
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return getState(pContext.getItemInHand()) == State.READY ?
                handleFirstLink(pContext) : handleSecondLink(pContext);
        } else {
            return InteractionResult.PASS;
        }
    }

    private InteractionResult handleFirstLink(UseOnContext context){
        Level level = context.getLevel();
        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var blockState = level.getBlockState(pos);
        IPortalBlock portal = (IPortalBlock) blockState.getBlock();

        if(!portal.checkValidDimension(level)){
            // TODO msg user

            return InteractionResult.FAIL;
        }

        if (blockState.getValue(IPortalBlock.PORTAL_MODE) == IPortalBlock.PortalMode.UNLINKED){
            setTarget(level, pos, stack, ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString());
            return InteractionResult.SUCCESS;
        } else return InteractionResult.FAIL;
    }

    private InteractionResult handleSecondLink(UseOnContext context){
        Level level = context.getLevel();
        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        IPortalBlock portal = (IPortalBlock) state.getBlock();
        ResourceKey<Level> targetDimension = getDimension(stack);

        if(!state.is(getType(stack))){
            // TODO msg user

            return InteractionResult.FAIL;
        }

        if(!portal.checkValidLinkPair(level, getTargetPos(stack), pos, getDimension(stack), stack.getOrCreateTag().getDouble(SCALE_TAG))){
            // TODO msg user

            return InteractionResult.FAIL;
        }

        if (state.getValue(IPortalBlock.PORTAL_MODE) == IPortalBlock.PortalMode.UNLINKED){
            if(level.getBlockEntity(pos) instanceof IPortalTileEntity be){
                be.linkPortals(targetDimension, getTargetPos(stack));
                resetTarget(stack);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    @Nullable
    public static BlockPos getTargetPos(ItemStack stack){
        var nbt = stack.getTag();
        if (nbt == null ||
                !nbt.contains(X_TAG, Tag.TAG_INT) ||
                !nbt.contains(Y_TAG, Tag.TAG_INT) ||
                !nbt.contains(Z_TAG, Tag.TAG_INT)) {
            return null;
        }

        return new BlockPos(nbt.getInt(X_TAG),
                nbt.getInt(Y_TAG), nbt.getInt(Z_TAG));
    }

    public static double getTargetScale(ItemStack stack){
        return stack.getOrCreateTag().getDouble(SCALE_TAG);
    }

    @Nullable
    public static Block getType(ItemStack stack){
        if(!stack.getOrCreateTag().contains(TYPE_TAG)){
            return null;
        } else {
            return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(stack.getOrCreateTag().getString(TYPE_TAG)));
        }
    }

    private void setTarget(Level level, BlockPos pos, ItemStack stack, String type){
        var nbt = stack.getOrCreateTag();
        nbt.putString(DIMENSION_TAG, level.dimension().location().toString());
        nbt.putDouble(SCALE_TAG, level.dimensionType().coordinateScale());
        nbt.putString(TYPE_TAG, type);
        nbt.putInt(X_TAG, pos.getX());
        nbt.putInt(Y_TAG, pos.getY());
        nbt.putInt(Z_TAG, pos.getZ());
    }

    // Should only be called on the server side
    private void resetTarget(ItemStack stack){
        stack.setTag(null);
    }

    public static ResourceKey<Level> getDimension(ItemStack stack){
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(stack.getOrCreateTag().getString(DIMENSION_TAG)));
    }

    public static State getState(ItemStack stack) {
        if(stack.getTag() != null && stack.getTag().contains(DIMENSION_TAG))
            return State.WAITING_NEXT;
        return State.READY;
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if (getState(stack).equals(State.WAITING_NEXT)){
            // todo: localize 
            // STOPSHIP: 6/4/2022
            tooltip.add(new TextComponent(getDimension(stack).location().getPath()));
            tooltip.add(new TextComponent(getTargetPos(stack).toString()));
        }
    }

    public enum State {
        WAITING_NEXT,
        READY
    }
}
