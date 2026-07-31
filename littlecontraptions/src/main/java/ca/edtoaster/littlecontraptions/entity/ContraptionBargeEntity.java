package ca.edtoaster.littlecontraptions.entity;

import ca.edtoaster.littlecontraptions.setup.LCEntityTypes;
import ca.edtoaster.littlecontraptions.setup.LCItems;
import com.simibubi.create.AllAttachmentTypes;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ContraptionBargeEntity extends AbstractBargeEntity {

    private final BargeController controller = new BargeController(this);

    public ContraptionBargeEntity(EntityType<? extends ContraptionBargeEntity> type, Level world) {
        super(type, world);
    }

    public ContraptionBargeEntity(Level worldIn, double x, double y, double z) {
        super(LCEntityTypes.CONTRAPTION_BARGE.get(), worldIn, x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // back-calculate xo, yo, and zo on client side to provide smooth
            // rot to contraption entity
            Vec3 clientPos = position();
            float yRotRad = getYRot() * Mth.DEG_TO_RAD;
            double xOff = -Mth.sin(yRotRad);
            double zOff = Mth.cos(yRotRad);
            yo = clientPos.y;
            xo = clientPos.x - xOff;
            zo = clientPos.z - zOff;
        } else if (getData(AllAttachmentTypes.MINECART_CONTROLLER.get()) != controller) {
            // Create 6.0 dropped the minecart-controller capability; its mounted-contraption tick
            // instead reads the controller as a data attachment off the carrier entity. Expose our
            // bridge so Create can push contraption stall state onto the barge's StallingCapability.
            setData(AllAttachmentTypes.MINECART_CONTROLLER.get(), controller);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 1;
    }

    @Override
    public Item getDropItem() {
        return LCItems.CONTRAPTION_BARGE_ITEM.get();
    }

    @Override
    protected void doInteract(Player player) {
        // no op
    }

    public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            var pos = getRiderPosition();
            callback.accept(passenger, pos.x, pos.y, pos.z);
        }
    }

    public Vec3 getRiderPosition() {
        return new Vec3(0, 0.5125, 0).add(this.getX(), this.getY(), this.getZ());
    }
}
