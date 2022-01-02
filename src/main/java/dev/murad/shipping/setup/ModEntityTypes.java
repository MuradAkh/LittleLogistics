package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.BargeEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.tug.TugDummyHitboxEntity;
import dev.murad.shipping.entity.custom.tug.TugEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class ModEntityTypes {
    public static final RegistryObject<EntityType<BargeEntity>> BARGE =
            Registration.ENTITIES.register("barge",
                    () -> EntityType.Builder.<BargeEntity>of(BargeEntity::new,
                                    EntityClassification.MISC).sized(0.6f, 0.6f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barge").toString()));

    public static final RegistryObject<EntityType<TugEntity>> TUG =
            Registration.ENTITIES.register("tug",
                    () -> EntityType.Builder.<TugEntity>of(TugEntity::new,
                                    EntityClassification.MISC).sized(0.7f, 0.6f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug").toString()));

    public static final RegistryObject<EntityType<TugDummyHitboxEntity>> TUG_DUMMY_HITBOX =
            Registration.ENTITIES.register("tug_dummy_hitbox",
                    () -> EntityType.Builder.<TugDummyHitboxEntity>of(TugDummyHitboxEntity::new,
                                    EntityClassification.MISC).sized(0.75f, 0.6f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug_dummy_hitbox").toString()));

    public static final RegistryObject<EntityType<SpringEntity>> SPRING =
            Registration.ENTITIES.register("spring",
                    () -> EntityType.Builder.<SpringEntity>of(SpringEntity::new,
                                    EntityClassification.MISC).sized(0.05f, 0.2f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "spring").toString()));




    public static void register () {

    }
}
