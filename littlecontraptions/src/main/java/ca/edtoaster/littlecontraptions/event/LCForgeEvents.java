package ca.edtoaster.littlecontraptions.event;

import ca.edtoaster.littlecontraptions.LCMod;
import ca.edtoaster.littlecontraptions.entity.TrainCarController;
import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import dev.murad.shipping.entity.custom.train.wagon.AbstractWagonEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = LCMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class LCForgeEvents {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractWagonEntity wagon)) return;
        if (wagon.level().isClientSide()) return;

        MinecartController existing = wagon.getData(AllAttachmentTypes.MINECART_CONTROLLER.get());
        if (!(existing instanceof TrainCarController)) {
            wagon.setData(AllAttachmentTypes.MINECART_CONTROLLER.get(), new TrainCarController(wagon));
        }
    }
}
