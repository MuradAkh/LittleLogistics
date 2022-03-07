package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ChestCarEntity extends AbstractWagonEntity {
    public ChestCarEntity(EntityType<ChestCarEntity> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public ChestCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.CHEST_CAR.get(), level, aDouble, aDouble1, aDouble2);

    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CHEST_CAR.get());
    }
}
