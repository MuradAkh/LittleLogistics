package dev.murad.shipping.compatability.create;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.content.contraptions.components.actors.DrillBlock;
import com.simibubi.create.content.contraptions.components.saw.SawBlock;
import net.minecraft.world.damagesource.DamageSource;

public class CreateDamageSourceAllowlist {
    private static final ImmutableSet<DamageSource> sources =
            ImmutableSet.of(
                    SawBlock.damageSourceSaw,
                    DrillBlock.damageSourceDrill);

    public static boolean isAllowListed(DamageSource damageSource){
        return sources.contains(damageSource);
    }
}
