package dev.murad.shipping.entity.custom.vessel.tug;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.Objects;

public class VehicleFrontPart extends PartEntity<Entity> {
    public VehicleFrontPart(Entity parent) {
        super(parent);
        this.refreshDimensions();
    }

    public boolean hurt(DamageSource pSource, float pAmount) {
        return this.isInvulnerableTo(pSource) ? false : getParent().hurt(pSource, pAmount);

    }

    public boolean is(Entity pEntity) {
        return this == pEntity || getParent() == pEntity;
    }

    @Nullable
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    public Packet<?> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    public EntityDimensions getDimensions(Pose pPose) {
        return this.getParent().getDimensions(pPose);
    }

    public boolean shouldBeSaved() {
        return true;
    }

    public void updatePosition(Entity tugEntity){
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        double x = tugEntity.getX() + tugEntity.getDirection().getStepX() * getParent().getBoundingBox().getXsize();
        double z = tugEntity.getZ() + tugEntity.getDirection().getStepZ() * getParent().getBoundingBox().getXsize();
        double y = tugEntity.getY();
        this.setPos(x, y, z);
        this.zOld = oldZ;
        this.zo = oldZ;
        this.xOld = oldX;
        this.xo = oldX;
        this.yOld = oldY;
        this.yo = oldY;
    }

    public boolean isPickable() {
        return !this.isRemoved();
    }

    public BlockPos getPos(){
        return getOnPos();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getParent() instanceof AbstractTugEntity tugEntity){
            if (player.getItemInHand(hand).getItem() instanceof LeadItem || Objects.equals(tugEntity.getLeashHolder(), player)) {
                return tugEntity.interact(player, hand);
            }
        return tugEntity.mobInteract(player, hand);
        } else {
            return getParent().interact(player, hand);
        }
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {

    }
}
