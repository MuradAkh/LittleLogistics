package dev.murad.shipping.entity.accessor;

import dev.murad.shipping.util.EnrollmentHandler;
import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class EnergyHeadVehicleDataAccessor extends HeadVehicleDataAccessor {
    private static final int SHORT_MASK = 0xFFFF;
    
    public EnergyHeadVehicleDataAccessor(ContainerData data) {
        super(data);
    }

    /**
     * Lil-endian
     */

    public int getEnergy() {
        int lo = this.data.get(15) & SHORT_MASK;
        int hi = this.data.get(16) & SHORT_MASK;
        return lo | hi << 16;
    }

    public int getCapacity() {
        int lo = this.data.get(17) & SHORT_MASK;
        int hi = this.data.get(18) & SHORT_MASK;
        return lo | hi << 16;
    }

    public static class Builder extends HeadVehicleDataAccessor.Builder {
        public Builder() {
            this.arr = new SupplierIntArray(20);
        }

        public Builder withEnergy(IntSupplier energy) {
            this.arr.setSupplier(15, () -> energy.getAsInt() & SHORT_MASK);
            this.arr.setSupplier(16, () -> (energy.getAsInt() >> 16) & SHORT_MASK);
            return this;
        }

        public Builder withCapacity(IntSupplier capacity) {
            this.arr.setSupplier(17, () -> capacity.getAsInt() & SHORT_MASK);
            this.arr.setSupplier(18, () -> (capacity.getAsInt() >> 16) & SHORT_MASK);
            return this;
        }

        public EnergyHeadVehicleDataAccessor build() {
            return new EnergyHeadVehicleDataAccessor(this.arr);
        }
    }
}
