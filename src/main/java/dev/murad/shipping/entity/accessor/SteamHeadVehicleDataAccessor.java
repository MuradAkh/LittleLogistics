package dev.murad.shipping.entity.accessor;

import dev.murad.shipping.util.EnrollmentHandler;
import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class SteamHeadVehicleDataAccessor extends HeadVehicleDataAccessor{
    public SteamHeadVehicleDataAccessor(ContainerData data) {
        super(data);
    }

    public int getBurnProgress() {
        return this.data.get(15);
    }

    public static class Builder extends HeadVehicleDataAccessor.Builder{

        public Builder() {
            this.arr = new SupplierIntArray(20);
        }

        public Builder withBurnProgress(IntSupplier burnProgress) {
            this.arr.setSupplier(15, burnProgress);
            return this;
        }

        public SteamHeadVehicleDataAccessor build() {
            return new SteamHeadVehicleDataAccessor(this.arr);
        }
    }
}
