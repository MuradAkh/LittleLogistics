package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;

public class EnergyLocomotiveDataAccessor extends EnergyTugDataAccessor {
    public EnergyLocomotiveDataAccessor(ContainerData data) {
        super(data);
    }

    public boolean isOn() {
        return this.data.get(6) == 1;
    }


    public static class Builder extends EnergyTugDataAccessor.Builder {
        public Builder(int uuid) {
            super(uuid);
            this.arr = new SupplierIntArray(7);
            this.arr.set(0, uuid);
        }

        public EnergyLocomotiveDataAccessor.Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(6, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        @Override
        public EnergyLocomotiveDataAccessor build() {
            return new EnergyLocomotiveDataAccessor(this.arr);
        }
    }
}
