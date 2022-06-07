package dev.murad.shipping.entity.custom.vessel;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.SpringableEntity;
import dev.murad.shipping.util.Train;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class VesselEntity extends WaterAnimal implements SpringableEntity {
    @Getter
    @Setter
    private boolean frozen = false;
    private boolean waitForDominated = false;

    protected VesselEntity(EntityType<? extends WaterAnimal> type, Level world) {
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
    private Boat.Status status;
    private Boat.Status oldStatus;
    private double lastYd;

    protected Optional<VesselEntity> dominated = Optional.empty();
    protected Optional<VesselEntity> dominant = Optional.empty();
    protected Optional<SpringEntity> dominantS = Optional.empty();
    protected Optional<SpringEntity> dominatedS = Optional.empty();
    protected Train<VesselEntity> train;

    @Override
    public boolean isPickable(){
        return true;
    }

    public boolean hasWaterOnSides(){
        return this.level.getFluidState(this.getOnPos().relative(this.getDirection().getClockWise())).is(Fluids.WATER) &&
                this.level.getFluidState(this.getOnPos().relative(this.getDirection().getCounterClockWise())).is(Fluids.WATER) &&
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
            if(this.tickCount % 10 == 0){
                this.heal(1f);
            }
        }

        if(!this.level.isClientSide) {
            this.oldStatus = this.status;
            this.status = this.getStatus();

            this.floatBoat();
            this.unDrown();
        }

        if (!this.level.isClientSide && dominated.isPresent()){
            waitForDominated = false;
            if(!((ServerLevel) this.level).isPositionEntityTicking(dominated.get().blockPosition())){
                this.getTrain().getTug().ifPresent(tug -> tug.setDeltaMovement(Vec3.ZERO));
                this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
            } else if (waitForDominated) {
                this.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
            }
        }

        super.tick();
    }

    private void unDrown(){
        if(level.getBlockState(getOnPos().above()).getBlock().equals(Blocks.WATER)){
            this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(0, 0.1, 0)));
        }

    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.0D);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        // override speed attributes on load from previous versions
        resetSpeedAttributes();
        waitForDominated = nbt.getBoolean("hasChild");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("hasChild", getDominated().isPresent());
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

    /**
     * Check if this vessel should pull the vessel behind
     */
    public boolean shouldApplySpringPhysics() {
        if (this.train.getHead() instanceof AbstractTugEntity tug) {
            return !tug.shouldFreezeTrain();
        }
        return true;
    }

    @Override
    public Optional<VesselEntity> getDominated() {
        return this.dominated.map(s -> s); // Java...
    }

    @Override
    public Optional<SpringEntity> getDominatedSpring() {
        return this.dominatedS; // Java...
    }

    @Override
    public Optional<VesselEntity> getDominant() {
        return this.dominant.map(s -> s); // Java...
    }

    @Override
    public Optional<SpringEntity> getDominantSpring() {
        return this.dominantS; // Java...
    }

    @Override
    public Train<VesselEntity> getTrain() {
        return this.train;
    }

    @Override
    public void checkDespawn() {

    }

    public boolean linkEntities(Player player, Entity target) {
        if(!(target instanceof VesselEntity)){
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.badTypes"), true);
            return false;
        }
        Train firstTrain =  this.getTrain();
        Train secondTrain = ((LinkableEntity) target).getTrain();
        if (this.distanceTo(target) > 15){
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.tooFar"), true);
        } else if (firstTrain.getTug().isPresent() && secondTrain.getTug().isPresent()) {
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.noTwoTugs"), true);
        } else if (secondTrain.equals(firstTrain)){
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.spring.noLoops"), true);
        } else if (firstTrain.getTug().isPresent()) {
            SpringEntity.createSpring((VesselEntity) firstTrain.getTail(), (VesselEntity) secondTrain.getHead());
            return true;
        } else {
            SpringEntity.createSpring((VesselEntity) secondTrain.getTail(), (VesselEntity) firstTrain.getHead());
            return true;
        }
        return false;
    }

    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    private void floatBoat() {
        double d0 = (double) -0.04F;
        double d1 = this.isNoGravity() ? 0.0D : (double) -0.04F;
        double d2 = 0.0D;
        this.invFriction = 0.05F;
        if (this.oldStatus == Boat.Status.IN_AIR && this.status != Boat.Status.IN_AIR && this.status != Boat.Status.ON_LAND) {
            this.waterLevel = this.getY(1.0D);
            this.setPos(this.getX(), (double) (this.getWaterLevelAbove() - this.getBbHeight()) + 0.101D, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            this.lastYd = 0.0D;
            this.status = Boat.Status.IN_WATER;
        } else {
            if (this.status == Boat.Status.IN_WATER) {
                d2 = (this.waterLevel - this.getY()) / (double) this.getBbHeight();
                this.invFriction = 0.9F;
            } else if (this.status == Boat.Status.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                this.invFriction = 0.9F;
            } else if (this.status == Boat.Status.UNDER_WATER) {
                d2 = (double) 0.01F;
                this.invFriction = 0.45F;
            } else if (this.status == Boat.Status.IN_AIR) {
                this.invFriction = 0.9F;
            } else if (this.status == Boat.Status.ON_LAND) {
                this.invFriction = this.landFriction;
                if (this.getControllingPassenger() instanceof Player) {
                    this.landFriction /= 2.0F;
                }
            }

            Vec3 vector3d = this.getDeltaMovement();
            this.setDeltaMovement(vector3d.x * (double) this.invFriction, vector3d.y + d1, vector3d.z * (double) this.invFriction);
            if (d2 > 0.0D) {
                Vec3 vector3d1 = this.getDeltaMovement();
                this.setDeltaMovement(vector3d1.x, (vector3d1.y + d2 * 0.10153846016296973D) * 0.75D, vector3d1.z);
            }
        }

    }

    private Boat.Status getStatus() {
        Boat.Status Boat$status = this.isUnderwater();
        if (Boat$status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return Boat$status;
        } else if (this.checkInWater()) {
            return Boat.Status.IN_WATER;
        } else {
            float f = this.getGroundFriction();
            if (f > 0.0F) {
                this.landFriction = f;
                return Boat.Status.ON_LAND;
            } else {
                return Boat.Status.IN_AIR;
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public float getWaterLevelAbove() {
        AABB aabb = this.getBoundingBox();
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.maxY);
        int l = Mth.ceil(aabb.maxY - this.lastYd);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        label39:
        for(int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for(int l1 = i; l1 < j; ++l1) {
                for(int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutableblockpos.set(l1, k1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutableblockpos);
                    if (!fluidstate.isEmpty()) {
                        f = Math.max(f, fluidstate.getHeight(this.level, blockpos$mutableblockpos));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float)blockpos$mutableblockpos.getY() + f;
            }
        }

        return (float)(l + 1);
    }

    /**
     * Decides how much the boat should be gliding on the land (based on any slippery blocks)
     */
    public float getGroundFriction() {
        AABB aabb = this.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX, aabb.minY - 0.001D, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
        int i = Mth.floor(aabb1.minX) - 1;
        int j = Mth.ceil(aabb1.maxX) + 1;
        int k = Mth.floor(aabb1.minY) - 1;
        int l = Mth.ceil(aabb1.maxY) + 1;
        int i1 = Mth.floor(aabb1.minZ) - 1;
        int j1 = Mth.ceil(aabb1.maxZ) + 1;
        VoxelShape voxelshape = Shapes.create(aabb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(int l1 = i; l1 < j; ++l1) {
            for(int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for(int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            blockpos$mutableblockpos.set(l1, k2, i2);
                            BlockState blockstate = this.level.getBlockState(blockpos$mutableblockpos);
                            if (!(blockstate.getBlock() instanceof WaterlilyBlock) && Shapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level, blockpos$mutableblockpos).move((double)l1, (double)k2, (double)i2), voxelshape, BooleanOp.AND)) {
                                f += blockstate.getFriction(this.level, blockpos$mutableblockpos, this);
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float)k1;
    }



    private boolean checkInWater() {
        AABB aabb = this.getBoundingBox();
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.minY);
        int l = Mth.ceil(aabb.minY + 0.001D);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        boolean flag = false;
        this.waterLevel = -Double.MAX_VALUE;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(int k1 = i; k1 < j; ++k1) {
            for(int l1 = k; l1 < l; ++l1) {
                for(int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutableblockpos.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutableblockpos);
                    if (!fluidstate.isEmpty()) {
                        float f = (float)l1 + fluidstate.getHeight(this.level, blockpos$mutableblockpos);
                        this.waterLevel = Math.max((double)f, this.waterLevel);
                        flag |= aabb.minY < (double)f;
                    }
                }
            }
        }

        return flag;
    }

    /**
     * Decides whether the boat is currently underwater.
     */
    @Nullable
    private Boat.Status isUnderwater() {
        AABB aabb = this.getBoundingBox();
        double d0 = aabb.maxY + 0.001D;
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.maxY);
        int l = Mth.ceil(d0);
        int i1 = Mth.floor(aabb.minZ);
        int j1 = Mth.ceil(aabb.maxZ);
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for(int k1 = i; k1 < j; ++k1) {
            for(int l1 = k; l1 < l; ++l1) {
                for(int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutableblockpos.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutableblockpos);
                    if (fluidstate.is(FluidTags.WATER) && d0 < (double)((float)blockpos$mutableblockpos.getY() + fluidstate.getHeight(this.level, blockpos$mutableblockpos))) {
                        if (!fluidstate.isSource()) {
                            return Boat.Status.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? Boat.Status.UNDER_WATER : null;
    }
    @Override
    protected void jumpInLiquid(TagKey<Fluid> pFluidTag) {
        if (this.getNavigation().canFloat()) {
            super.jumpInLiquid(pFluidTag);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
        }

    }

    public boolean isInvulnerableTo(DamageSource pSource) {
        if (ShippingConfig.Server.VESSEL_EXEMPT_DAMAGE_SOURCES.get().contains(pSource.msgId)){
            return true;
        }

        return pSource.equals(DamageSource.IN_WALL)
                || pSource.equals(DamageSource.LAVA)
                || pSource.equals(DamageSource.IN_FIRE)
                || pSource.equals(DamageSource.ON_FIRE)
                || super.isInvulnerableTo(pSource);
    }

    @Override
    public void checkInsideBlocks(){
        super.checkInsideBlocks();
    }

    // Get rid of default armour/hands slots itemhandler from mobs
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.empty();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float p_70097_2_) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else if (!this.level.isClientSide && !this.isRemoved() &&
                damageSource instanceof EntityDamageSource e && e.getEntity() instanceof Player) {
            this.remove(RemovalReason.KILLED);
            return true;
        } else {
            return super.hurt(damageSource, p_70097_2_);
        }
    }

    // LivingEntity override, to avoid jumping out of water
    @Override
    public void travel(Vec3 p_213352_1_) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            double d0 = 0.08D;
            AttributeInstance gravity = this.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
            boolean flag = this.getDeltaMovement().y <= 0.0D;
            d0 = gravity.getValue();

            FluidState fluidstate = this.level.getFluidState(this.blockPosition());
            if ((this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
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

                if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                    f5 = 0.96F;
                }

                f6 *= (float) this.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()).getValue();
                this.moveRelative(f6, p_213352_1_);
                this.move(MoverType.SELF, this.getDeltaMovement());
                Vec3 vector3d6 = this.getDeltaMovement();
                if (this.horizontalCollision && this.onClimbable()) {
                    vector3d6 = new Vec3(vector3d6.x, 0.2D, vector3d6.z);
                }

                this.setDeltaMovement(vector3d6.multiply((double) f5, (double) 0.8F, (double) f5));
                Vec3 vector3d2 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
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
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
                double d7 = this.getY();
                this.moveRelative(0.02F, p_213352_1_);
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, (double) 0.8F, 0.5D));
                    Vec3 vector3d3 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                    this.setDeltaMovement(vector3d3);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d0 / 4.0D, 0.0D));
                }

                Vec3 vector3d4 = this.getDeltaMovement();
                if (this.horizontalCollision && this.isFree(vector3d4.x, vector3d4.y + (double) 0.6F - this.getY() + d7, vector3d4.z)) {
                    this.setDeltaMovement(vector3d4.x, (double) 0.3F, vector3d4.z);
                }
            } else if (this.isFallFlying()) {
                Vec3 vector3d = this.getDeltaMovement();
                if (vector3d.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                Vec3 vector3d1 = this.getLookAngle();
                float f = this.getXRot() * ((float) Math.PI / 180F);
                double d1 = Math.sqrt(vector3d1.x * vector3d1.x + vector3d1.z * vector3d1.z);
                double d3 = this.getDeltaMovement().horizontalDistance();
                double d4 = vector3d1.length();
                float f1 = Mth.cos(f);
                f1 = (float) ((double) f1 * (double) f1 * Math.min(1.0D, d4 / 0.4D));
                vector3d = this.getDeltaMovement().add(0.0D, d0 * (-1.0D + (double) f1 * 0.75D), 0.0D);
                if (vector3d.y < 0.0D && d1 > 0.0D) {
                    double d5 = vector3d.y * -0.1D * (double) f1;
                    vector3d = vector3d.add(vector3d1.x * d5 / d1, d5, vector3d1.z * d5 / d1);
                }

                if (f < 0.0F && d1 > 0.0D) {
                    double d9 = d3 * (double) (-Mth.sin(f)) * 0.04D;
                    vector3d = vector3d.add(-vector3d1.x * d9 / d1, d9 * 3.2D, -vector3d1.z * d9 / d1);
                }

                if (d1 > 0.0D) {
                    vector3d = vector3d.add((vector3d1.x / d1 * d3 - vector3d.x) * 0.1D, 0.0D, (vector3d1.z / d1 * d3 - vector3d.z) * 0.1D);
                }

                this.setDeltaMovement(vector3d.multiply((double) 0.99F, (double) 0.98F, (double) 0.99F));
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    double d10 = this.getDeltaMovement().horizontalDistance();
                    double d6 = d3 - d10;
                    float f2 = (float) (d6 * 10.0D - 3.0D);
                    if (f2 > 0.0F) {
                        this.hurt(DamageSource.FLY_INTO_WALL, f2);
                    }
                }

                if (this.onGround && !this.level.isClientSide) {
                    this.setSharedFlag(7, false);
                }
            } else {
                BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
                float f3 = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFriction(level, this.getBlockPosBelowThatAffectsMyMovement(), this);
                float f4 = this.onGround ? f3 * 0.91F : 0.91F;
                Vec3 vector3d5 = this.handleRelativeFrictionAndCalculateMovement(p_213352_1_, f3);
                double d2 = vector3d5.y;
                if (this.hasEffect(MobEffects.LEVITATION)) {
                    d2 += (0.05D * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vector3d5.y) * 0.2D;
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
}
