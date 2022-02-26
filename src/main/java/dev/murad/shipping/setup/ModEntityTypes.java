package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCar;
import dev.murad.shipping.entity.custom.train.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.LocomotiveEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.barge.*;
import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final RegistryObject<EntityType<ChestBargeEntity>> CHEST_BARGE =
            Registration.ENTITIES.register("barge",
                    () -> EntityType.Builder.<ChestBargeEntity>of(ChestBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barge").toString()));

    public static final RegistryObject<EntityType<ChunkLoaderBargeEntity>> CHUNK_LOADER_BARGE =
            Registration.ENTITIES.register("chunk_loader_barge",
                    () -> EntityType.Builder.<ChunkLoaderBargeEntity>of(ChunkLoaderBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "chunk_loader_barge").toString()));

    public static final RegistryObject<EntityType<FishingBargeEntity>> FISHING_BARGE =
            Registration.ENTITIES.register("fishing_barge",
                    () -> EntityType.Builder.<FishingBargeEntity>of(FishingBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fishing_barge").toString()));

    public static final RegistryObject<EntityType<FluidTankBargeEntity>> FLUID_TANK_BARGE =
            Registration.ENTITIES.register("fluid_barge",
                    () -> EntityType.Builder.<FluidTankBargeEntity>of(FluidTankBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fluid_barge").toString()));

    public static final RegistryObject<EntityType<SeaterBargeEntity>> SEATER_BARGE =
            Registration.ENTITIES.register("seater_barge",
                    () -> EntityType.Builder.<SeaterBargeEntity>of(SeaterBargeEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "seater_barge").toString()));

    public static final RegistryObject<EntityType<SteamTugEntity>> STEAM_TUG =
            Registration.ENTITIES.register("tug",
                    () -> EntityType.Builder.<SteamTugEntity>of(SteamTugEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug").toString()));

    public static final RegistryObject<EntityType<EnergyTugEntity>> ENERGY_TUG =
            Registration.ENTITIES.register("energy_tug",
                    () -> EntityType.Builder.<EnergyTugEntity>of(EnergyTugEntity::new,
                                    MobCategory.MISC).sized(0.7f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "energy_tug").toString()));

    public static final RegistryObject<EntityType<SpringEntity>> SPRING =
            Registration.ENTITIES.register("spring",
                    () -> EntityType.Builder.<SpringEntity>of(SpringEntity::new,
                                    MobCategory.MISC).sized(0.05f, 0.2f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "spring").toString()));

    public static final RegistryObject<EntityType<AbstractTrainCar>> CHEST_CAR =
            Registration.ENTITIES.register("chest_car",
                    () -> EntityType.Builder.<AbstractTrainCar>of(ChestCarEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "train_car").toString()));

    public static final RegistryObject<EntityType<LocomotiveEntity>> STEAM_LOCOMOTIVE =
            Registration.ENTITIES.register("steam_locomotive",
                    () -> EntityType.Builder.<LocomotiveEntity>of(LocomotiveEntity::new,
                                    MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "locomotive").toString()));


    public static void register () {

    }
}
