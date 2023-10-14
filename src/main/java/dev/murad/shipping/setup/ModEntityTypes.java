package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.barge.*;
import dev.murad.shipping.entity.custom.train.wagon.ChunkLoaderCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final RegistryObject<EntityType<ChestBargeEntity>> CHEST_BARGE =
            Registration.ENTITIES.register("barge",
                    () -> EntityType.Builder.<ChestBargeEntity>of(ChestBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barge").toString()));

    public static final RegistryObject<EntityType<ChestBargeEntity>> BARREL_BARGE =
            Registration.ENTITIES.register("barrel_barge",
                    () -> EntityType.Builder.<ChestBargeEntity>of(ChestBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barrel_barge").toString()));

    public static final RegistryObject<EntityType<ChunkLoaderBargeEntity>> CHUNK_LOADER_BARGE =
            Registration.ENTITIES.register("chunk_loader_barge",
                    () -> EntityType.Builder.<ChunkLoaderBargeEntity>of(ChunkLoaderBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "chunk_loader_barge").toString()));

    public static final RegistryObject<EntityType<FishingBargeEntity>> FISHING_BARGE =
            Registration.ENTITIES.register("fishing_barge",
                    () -> EntityType.Builder.<FishingBargeEntity>of(FishingBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fishing_barge").toString()));

    public static final RegistryObject<EntityType<FluidTankBargeEntity>> FLUID_TANK_BARGE =
            Registration.ENTITIES.register("fluid_barge",
                    () -> EntityType.Builder.<FluidTankBargeEntity>of(FluidTankBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fluid_barge").toString()));

    public static final RegistryObject<EntityType<SeaterBargeEntity>> SEATER_BARGE =
            Registration.ENTITIES.register("seater_barge",
                    () -> EntityType.Builder.<SeaterBargeEntity>of(SeaterBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "seater_barge").toString()));

    public static final RegistryObject<EntityType<VacuumBargeEntity>> VACUUM_BARGE =
            Registration.ENTITIES.register("vacuum_barge",
                    () -> EntityType.Builder.<VacuumBargeEntity>of(VacuumBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "vacuum_barge").toString()));

    public static final RegistryObject<EntityType<SteamTugEntity>> STEAM_TUG =
            Registration.ENTITIES.register("tug",
                    () -> EntityType.Builder.<SteamTugEntity>of(SteamTugEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug").toString()));

    public static final RegistryObject<EntityType<EnergyTugEntity>> ENERGY_TUG =
            Registration.ENTITIES.register("energy_tug",
                    () -> EntityType.Builder.<EnergyTugEntity>of(EnergyTugEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "energy_tug").toString()));

    public static final RegistryObject<EntityType<ChestCarEntity>> CHEST_CAR =
            Registration.ENTITIES.register("chest_car",
                    () -> EntityType.Builder.<ChestCarEntity>of(ChestCarEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "chest_car").toString()));

    public static final RegistryObject<EntityType<ChestCarEntity>> BARREL_CAR =
            Registration.ENTITIES.register("barrel_car",
                    () -> EntityType.Builder.<ChestCarEntity>of(ChestCarEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barrel_car").toString()));

    public static final RegistryObject<EntityType<SeaterCarEntity>> SEATER_CAR =
            Registration.ENTITIES.register("seater_car",
                    () -> EntityType.Builder.<SeaterCarEntity>of(SeaterCarEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "seater_car").toString()));

    public static final RegistryObject<EntityType<FluidTankCarEntity>> FLUID_CAR =
            Registration.ENTITIES.register("fluid_car",
                    () -> EntityType.Builder.<FluidTankCarEntity>of(FluidTankCarEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fluid_car").toString()));

    public static final RegistryObject<EntityType<ChunkLoaderCarEntity>> CHUNK_LOADER_CAR =
            Registration.ENTITIES.register("chunk_loader_car",
                    () -> EntityType.Builder.<ChunkLoaderCarEntity>of(ChunkLoaderCarEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "chunk_loader_car").toString()));


    public static final RegistryObject<EntityType<AbstractLocomotiveEntity>> STEAM_LOCOMOTIVE =
            Registration.ENTITIES.register("steam_locomotive",
                    () -> EntityType.Builder.<AbstractLocomotiveEntity>of(SteamLocomotiveEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "steam_locomotive").toString()));

    public static final RegistryObject<EntityType<AbstractLocomotiveEntity>> ENERGY_LOCOMOTIVE =
            Registration.ENTITIES.register("energy_locomotive",
                    () -> EntityType.Builder.<AbstractLocomotiveEntity>of(EnergyLocomotiveEntity::new,
                                    MobCategory.MISC)
                            .clientTrackingRange(8)
                            .setShouldReceiveVelocityUpdates(true)
                            .sized(0.7f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "energy_locomotive").toString()));


    public static void register () {

    }
}
