package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SteamLocomotiveEntity extends AbstractLocomotiveEntity {
    public SteamLocomotiveEntity(EntityType<?> type, Level p_38088_) {
        super(type, p_38088_);
    }

    public SteamLocomotiveEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.STEAM_LOCOMOTIVE.get(), level, aDouble, aDouble1, aDouble2);
    }


    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.STEAM_LOCOMOTIVE.get());
    }

    @Override
    protected boolean checkMovementAndTickFuel() {
        return engineOn;
    }
}
