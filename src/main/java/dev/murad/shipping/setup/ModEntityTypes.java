package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.ModBargeEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.TugEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class ModEntityTypes {
    public static final RegistryObject<EntityType<ModBargeEntity>> BARGE =
            Registration.ENTITIES.register("barge",
                    () -> EntityType.Builder.<ModBargeEntity>of(ModBargeEntity::new,
                                    EntityClassification.MISC).sized(0.5f, 0.5f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "barge").toString()));

    public static final RegistryObject<EntityType<TugEntity>> TUG =
            Registration.ENTITIES.register("tug",
                    () -> EntityType.Builder.<TugEntity>of(TugEntity::new,
                                    EntityClassification.MISC).sized(0.8f, 0.5f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "tug").toString()));

    public static final RegistryObject<EntityType<SpringEntity>> SPRING =
            Registration.ENTITIES.register("spring",
                    () -> EntityType.Builder.<SpringEntity>of(SpringEntity::new,
                                    EntityClassification.MISC).sized(0.1f, 0.1f)
                            .build(new ResourceLocation(ShippingMod.MOD_ID, "spring").toString()));




    public static void register () {

    }
}
