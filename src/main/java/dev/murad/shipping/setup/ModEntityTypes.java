package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.*;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import dev.murad.shipping.entity.custom.tug.TugDummyHitboxEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class ModEntityTypes {
    public static final RegistryObject<EntityType<ChestBargeEntity>> CHEST_BARGE =
            Registration.ENTITIES.register("barge",
                    () -> EntityType.Builder.<ChestBargeEntity>of(ChestBargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barge").toString()));

    public static final RegistryObject<EntityType<ChunkLoaderBargeEntity>> CHUNK_LOADER_BARGE =
            Registration.ENTITIES.register("chunk_loader_barge",
                    () -> EntityType.Builder.<ChunkLoaderBargeEntity>of(ChunkLoaderBargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "chunk_loader_barge").toString()));

    public static final RegistryObject<EntityType<FishingBargeEntity>> FISHING_BARGE =
            Registration.ENTITIES.register("fishing_barge",
                    () -> EntityType.Builder.<FishingBargeEntity>of(FishingBargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fishing_barge").toString()));

    public static final RegistryObject<EntityType<FluidTankBargeEntity>> FLUID_TANK_BARGE =
            Registration.ENTITIES.register("fluid_barge",
                    () -> EntityType.Builder.<FluidTankBargeEntity>of(FluidTankBargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "fluid_barge").toString()));

    public static final RegistryObject<EntityType<SeaterBargeEntity>> SEATER_BARGE =
            Registration.ENTITIES.register("seater_barge",
                    () -> EntityType.Builder.<SeaterBargeEntity>of(SeaterBargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "seater_barge").toString()));

    public static final RegistryObject<EntityType<SteamTugEntity>> STEAM_TUG =
            Registration.ENTITIES.register("tug",
                    () -> EntityType.Builder.<SteamTugEntity>of(SteamTugEntity::new,
                                    EntityClassification.MISC).sized(0.7f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug").toString()));

    public static final RegistryObject<EntityType<EnergyTugEntity>> ENERGY_TUG =
            Registration.ENTITIES.register("energy_tug",
                    () -> EntityType.Builder.<EnergyTugEntity>of(EnergyTugEntity::new,
                                    EntityClassification.MISC).sized(0.7f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "energy_tug").toString()));

    public static final RegistryObject<EntityType<TugDummyHitboxEntity>> TUG_DUMMY_HITBOX =
            Registration.ENTITIES.register("tug_dummy_hitbox",
                    () -> EntityType.Builder.<TugDummyHitboxEntity>of(TugDummyHitboxEntity::new,
                                    EntityClassification.MISC).sized(0.75f, 0.9f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug_dummy_hitbox").toString()));

    public static final RegistryObject<EntityType<SpringEntity>> SPRING =
            Registration.ENTITIES.register("spring",
                    () -> EntityType.Builder.<SpringEntity>of(SpringEntity::new,
                                    EntityClassification.MISC).sized(0.05f, 0.2f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "spring").toString()));




    public static void register () {

    }
}
