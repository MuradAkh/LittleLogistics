package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.entity.ContraptionBargeEntity;
import ca.edtoaster.littlecontraptions.LCMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

public class LCEntityTypes {

    public static final DeferredHolder<EntityType<?>, EntityType<ContraptionBargeEntity>> CONTRAPTION_BARGE =
            Registration.ENTITIES.register("contraption_barge",
                    () -> EntityType.Builder.<ContraptionBargeEntity>of(ContraptionBargeEntity::new, MobCategory.MISC).sized(0.6f, 0.9f)
                            .build(ResourceLocation.fromNamespaceAndPath(LCMod.MOD_ID, "contraption_barge").toString()));

    public static void register () {

    }
}
