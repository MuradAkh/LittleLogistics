package ca.edtoaster.littlecontraptions.item;

import ca.edtoaster.littlecontraptions.setup.LCItems;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;


// TODO: Refactor Item in LL to reduce class bloat
public class ContraptionBargeItem extends AbstractBargeEntity {

    public ContraptionBargeItem(EntityType<? extends AbstractBargeEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public Item getDropItem() {
        return LCItems.CONTRAPTION_BARGE_ITEM.get();
    }

    @Override
    protected void doInteract(Player player) {

    }

}
