package dev.murad.shipping.entity.custom;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.util.Train;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

public abstract class VesselEntity extends WaterMobEntity implements ISpringableEntity {
    protected VesselEntity(EntityType<? extends WaterMobEntity> type, World world) {
        super(type, world);
        stuckCounter = 0;
        resetSpeedAttributes();
        setSpeedAttributes(ShippingConfig.Server.TUG_BASE_SPEED.get());
    }

    // MOB STUFF
    private float invFriction;
    private int stuckCounter;
    private double waterLevel;
    private float landFriction;
    private BoatEntity.Status status;
    private BoatEntity.Status oldStatus;
    private double lastYd;
    private Optional<Pair<BlockPos, BlockState>> lastCornerGuideRail = Optional.empty();

    protected Optional<Pair<ISpringableEntity, SpringEntity>> dominated = Optional.empty();
    protected Optional<Pair<ISpringableEntity, SpringEntity>> dominant = Optional.empty();
    protected Train train;

    @Override
    public boolean isPickable(){
        return true;
    }

    public abstract boolean allowDockInterface();

    public boolean hasWaterOnSides(){
        return this.level.getBlockState(this.getOnPos().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.WATER) &&
                this.level.getBlockState(this.getOnPos().relative(this.getDirection().getCounterClockWise())).getBlock().equals(Blocks.WATER) &&
                this.level.getBlockState(this.getOnPos().above().relative(this.getDirection().getClockWise())).getBlock().equals(Blocks.AIR) &&
                this.level.getBlockState(this.getOnPos().above().relative(this.getDirection().getCounterClockWise())).getBlock().equals(Blocks.AIR);
    }

    public BlockPos getBlockPos(){
        return getOnPos();
    }

    @Override
    public void tick() {
        if(this.isAlive()) {
            tickSpringAliveCheck();
        }

        if(!this.level.isClientSide) {
            this.oldStatus = this.status;
            this.status = this.getStatus();

            this.floatBoat();
            this.unDrown();
        }

        super.tick();
    }

    private void unDrown(){
        if(level.getBlockState(getOnPos().above()).getBlock().equals(Blocks.WATER)){
            this.setDeltaMovement(this.getDeltaMovement().add(new Vector3d(0, 0.1, 0)));
        }

    }

    public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.0D);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        // override speed attributes on load from previous versions
        resetSpeedAttributes();
    }

    // reset speed to 1
    private void resetSpeedAttributes() {
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        this.getAttribute(ForgeMod.SWIM_SPEED.get()).setBaseValue(0);
    }

    private void setSpeedAttributes(double speed) {
        this.getAttribute(Attributes.MOVEMENT_SPEED)
                .addTransientModifier(
                        new AttributeModifier("movementspeed_mult", speed, AttributeModifier.Operation.ADDITION));
        this.getAttribute(ForgeMod.SWIM_SPEED.get())
                .addTransientModifier(
                        new AttributeModifier("swimspeed_mult", speed, AttributeModifier.Operation.ADDITION));
    }

    @Override
    protected void handleAirSupply(int p_209207_1_) {
        this.setAirSupply(300);
    }

    public abstract Item getDropItem();


    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(this.getDropItem());
    }

    @Override
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated() {
        return this.dominated;
    }

    @Override
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominant() {
        return this.dominant;
    }

    @Override
    public Train getTrain() {
        return this.train;
    }

    @Override
    public void checkDespawn() {

    }


    private void floatBoat() {
        double d0 = (double) -0.04F;
        double d1 = this.isNoGravity() ? 0.0D : (double) -0.04F;
        double d2 = 0.0D;
        this.invFriction = 0.05F;
        if (this.oldStatus == BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.ON_LAND) {
            this.waterLevel = this.getY(1.0D);
            this.setPos(this.getX(), (double) (this.getWaterLevelAbove() - this.getBbHeight()) + 0.101D, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            this.lastYd = 0.0D;
            this.status = BoatEntity.Status.IN_WATER;
        } else {
            if (this.status == BoatEntity.Status.IN_WATER) {
                d2 = (this.waterLevel - this.getY()) / (double) this.getBbHeight();
                this.invFriction = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                this.invFriction = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_WATER) {
                d2 = (double) 0.01F;
                this.invFriction = 0.45F;
            } else if (this.status == BoatEntity.Status.IN_AIR) {
                this.invFriction = 0.9F;
            } else if (this.status == BoatEntity.Status.ON_LAND) {
                this.invFriction = this.landFriction;
                if (this.getControllingPassenger() instanceof PlayerEntity) {
                    this.landFriction /= 2.0F;
                }
            }

            Vector3d vector3d = this.getDeltaMovement();
            this.setDeltaMovement(vector3d.x * (double) this.invFriction, vector3d.y + d1, vector3d.z * (double) this.invFriction);
            if (d2 > 0.0D) {
                Vector3d vector3d1 = this.getDeltaMovement();
                this.setDeltaMovement(vector3d1.x, (vector3d1.y + d2 * 0.10153846016296973D) * 0.75D, vector3d1.z);
            }
        }

    }


    public float getWaterLevelAbove() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(axisalignedbb.maxY - this.lastYd);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        label39:
        for (int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(l1, k1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if (fluidstate.is(FluidTags.WATER)) {
                        f = Math.max(f, fluidstate.getHeight(this.level, blockpos$mutable));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float) blockpos$mutable.getY() + f;
            }
        }

        return (float) (l + 1);
    }

    private BoatEntity.Status getStatus() {
        BoatEntity.Status boatentity$status = this.isUnderwater();
        if (boatentity$status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return boatentity$status;
        } else if (this.checkInWater()) {
            return BoatEntity.Status.IN_WATER;
        } else {
            float f = this.getGroundFriction();
            if (f > 0.0F) {
                this.landFriction = f;
                return BoatEntity.Status.ON_LAND;
            } else {
                return BoatEntity.Status.IN_AIR;
            }
        }
    }

    public float getGroundFriction() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY - 0.001D, axisalignedbb.minZ, axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ);
        int i = MathHelper.floor(axisalignedbb1.minX) - 1;
        int j = MathHelper.ceil(axisalignedbb1.maxX) + 1;
        int k = MathHelper.floor(axisalignedbb1.minY) - 1;
        int l = MathHelper.ceil(axisalignedbb1.maxY) + 1;
        int i1 = MathHelper.floor(axisalignedbb1.minZ) - 1;
        int j1 = MathHelper.ceil(axisalignedbb1.maxZ) + 1;
        VoxelShape voxelshape = VoxelShapes.create(axisalignedbb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int l1 = i; l1 < j; ++l1) {
            for (int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for (int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            blockpos$mutable.set(l1, k2, i2);
                            BlockState blockstate = this.level.getBlockState(blockpos$mutable);
                            if (!(blockstate.getBlock() instanceof LilyPadBlock) && VoxelShapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level, blockpos$mutable).move((double) l1, (double) k2, (double) i2), voxelshape, IBooleanFunction.AND)) {
                                f += blockstate.getSlipperiness(this.level, blockpos$mutable, this);
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float) k1;
    }

    private boolean checkInWater() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.minY);
        int l = MathHelper.ceil(axisalignedbb.minY + 0.001D);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        this.waterLevel = Double.MIN_VALUE;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if (fluidstate.is(FluidTags.WATER)) {
                        float f = (float) l1 + fluidstate.getHeight(this.level, blockpos$mutable);
                        this.waterLevel = Math.max((double) f, this.waterLevel);
                        flag |= axisalignedbb.minY < (double) f;
                    }
                }
            }
        }

        return flag;
    }

    @Nullable
    private BoatEntity.Status isUnderwater() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        double d0 = axisalignedbb.maxY + 0.001D;
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(d0);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if (fluidstate.is(FluidTags.WATER) && d0 < (double) ((float) blockpos$mutable.getY() + fluidstate.getHeight(this.level, blockpos$mutable))) {
                        if (!fluidstate.isSource()) {
                            return BoatEntity.Status.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? BoatEntity.Status.UNDER_WATER : null;
    }

    @Override
    protected void jumpInLiquid(ITag<Fluid> p_180466_1_) {
        if (this.getNavigation().canFloat()) {
            super.jumpInLiquid(p_180466_1_);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
        }

    }

    public boolean isInvulnerableTo(DamageSource p_180431_1_) {
        return p_180431_1_.equals(DamageSource.IN_WALL) || super.isInvulnerableTo(p_180431_1_);
    }


    // LivingEntity override, to avoid jumping out of water
    @Override
    public void travel(Vector3d p_213352_1_) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            double d0 = 0.08D;
            ModifiableAttributeInstance gravity = this.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
            boolean flag = this.getDeltaMovement().y <= 0.0D;
            d0 = gravity.getValue();

            FluidState fluidstate = this.level.getFluidState(this.blockPosition());
            if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate.getType())) {
                double d8 = this.getY();
                float f5 = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
                float f6 = 0.02F;
                float f7 = 0;
                if (f7 > 3.0F) {
                    f7 = 3.0F;
                }

                if (!this.onGround) {
                    f7 *= 0.5F;
                }

                if (f7 > 0.0F) {
                    f5 += (0.54600006F - f5) * f7 / 3.0F;
                    f6 += (this.getSpeed() - f6) * f7 / 3.0F;
                }

                if (this.hasEffect(Effects.DOLPHINS_GRACE)) {
                    f5 = 0.96F;
                }

                f6 *= (float) this.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()).getValue();
                this.moveRelative(f6, p_213352_1_);
                this.move(MoverType.SELF, this.getDeltaMovement());
                Vector3d vector3d6 = this.getDeltaMovement();
                if (this.horizontalCollision && this.onClimbable()) {
                    vector3d6 = new Vector3d(vector3d6.x, 0.2D, vector3d6.z);
                }

                this.setDeltaMovement(vector3d6.multiply((double) f5, (double) 0.8F, (double) f5));
                Vector3d vector3d2 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                this.setDeltaMovement(vector3d2);
                if (this.horizontalCollision) {
                    if (stuckCounter > 10) {
                        // destroy lilypads
                        Direction direction = getDirection();
                        BlockPos front = getOnPos().relative(direction).above();
                        BlockPos left = front.relative(direction.getClockWise());
                        BlockPos right = front.relative(direction.getCounterClockWise());
                        for (BlockPos pos : Arrays.asList(front, left, right)){
                            BlockState state = this.level.getBlockState(pos);
                            if (state.is(Blocks.LILY_PAD)){
                               this.level.destroyBlock(pos, true);
                            }
                        }
                        stuckCounter = 0;
                    } else {
                        stuckCounter++;
                    }
                } else {
//                    stuckCounter = 0;
                }
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate.getType())) {
                double d7 = this.getY();
                this.moveRelative(0.02F, p_213352_1_);
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, (double) 0.8F, 0.5D));
                    Vector3d vector3d3 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                    this.setDeltaMovement(vector3d3);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d0 / 4.0D, 0.0D));
                }

                Vector3d vector3d4 = this.getDeltaMovement();
                if (this.horizontalCollision && this.isFree(vector3d4.x, vector3d4.y + (double) 0.6F - this.getY() + d7, vector3d4.z)) {
                    this.setDeltaMovement(vector3d4.x, (double) 0.3F, vector3d4.z);
                }
            } else if (this.isFallFlying()) {
                Vector3d vector3d = this.getDeltaMovement();
                if (vector3d.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                Vector3d vector3d1 = this.getLookAngle();
                float f = this.xRot * ((float) Math.PI / 180F);
                double d1 = Math.sqrt(vector3d1.x * vector3d1.x + vector3d1.z * vector3d1.z);
                double d3 = Math.sqrt(getHorizontalDistanceSqr(vector3d));
                double d4 = vector3d1.length();
                float f1 = MathHelper.cos(f);
                f1 = (float) ((double) f1 * (double) f1 * Math.min(1.0D, d4 / 0.4D));
                vector3d = this.getDeltaMovement().add(0.0D, d0 * (-1.0D + (double) f1 * 0.75D), 0.0D);
                if (vector3d.y < 0.0D && d1 > 0.0D) {
                    double d5 = vector3d.y * -0.1D * (double) f1;
                    vector3d = vector3d.add(vector3d1.x * d5 / d1, d5, vector3d1.z * d5 / d1);
                }

                if (f < 0.0F && d1 > 0.0D) {
                    double d9 = d3 * (double) (-MathHelper.sin(f)) * 0.04D;
                    vector3d = vector3d.add(-vector3d1.x * d9 / d1, d9 * 3.2D, -vector3d1.z * d9 / d1);
                }

                if (d1 > 0.0D) {
                    vector3d = vector3d.add((vector3d1.x / d1 * d3 - vector3d.x) * 0.1D, 0.0D, (vector3d1.z / d1 * d3 - vector3d.z) * 0.1D);
                }

                this.setDeltaMovement(vector3d.multiply((double) 0.99F, (double) 0.98F, (double) 0.99F));
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    double d10 = Math.sqrt(getHorizontalDistanceSqr(this.getDeltaMovement()));
                    double d6 = d3 - d10;
                    float f2 = (float) (d6 * 10.0D - 3.0D);
                    if (f2 > 0.0F) {
                        this.playSound(this.getFallDamageSound((int) f2), 1.0F, 1.0F);
                        this.hurt(DamageSource.FLY_INTO_WALL, f2);
                    }
                }

                if (this.onGround && !this.level.isClientSide) {
                    this.setSharedFlag(7, false);
                }
            } else {
                BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
                float f3 = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getSlipperiness(level, this.getBlockPosBelowThatAffectsMyMovement(), this);
                float f4 = this.onGround ? f3 * 0.91F : 0.91F;
                Vector3d vector3d5 = this.handleRelativeFrictionAndCalculateMovement(p_213352_1_, f3);
                double d2 = vector3d5.y;
                if (this.hasEffect(Effects.LEVITATION)) {
                    d2 += (0.05D * (double) (this.getEffect(Effects.LEVITATION).getAmplifier() + 1) - vector3d5.y) * 0.2D;
                    this.fallDistance = 0.0F;
                } else if (this.level.isClientSide && !this.level.hasChunkAt(blockpos)) {
                    if (this.getY() > 0.0D) {
                        d2 = -0.1D;
                    } else {
                        d2 = 0.0D;
                    }
                } else if (!this.isNoGravity()) {
                    d2 -= d0;
                }

                this.setDeltaMovement(vector3d5.x * (double) f4, d2 * (double) 0.98F, vector3d5.z * (double) f4);
            }
        }

        this.calculateEntityAnimation(this, false);
    }

    public Optional<Pair<BlockPos, BlockState>> getLastCornerGuideRail() {
        return lastCornerGuideRail;
    }

    public void setLastCornerGuideRail(Pair<BlockPos, BlockState> lastCornerGuideRail) {
        this.lastCornerGuideRail = Optional.of(lastCornerGuideRail);
    }
}
