package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class EnergyLocomotiveDataAccessor extends EnergyTugDataAccessor {
    public EnergyLocomotiveDataAccessor(ContainerData data) {
        super(data);
    }

    public boolean isOn() {
        return this.data.get(6) == 1;
    }

    public int visitedSize() {
        return this.data.get(7);
    }

    public int routeSize() {
        return this.data.get(8);
    }


    public static class Builder extends EnergyTugDataAccessor.Builder {
        public Builder(int uuid) {
            super(uuid);
            this.arr = new SupplierIntArray(9);
            this.arr.set(0, uuid);
        }

        public EnergyLocomotiveDataAccessor.Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(6, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public EnergyLocomotiveDataAccessor.Builder withVisitedSize(IntSupplier s) {
            this.arr.setSupplier(7, s);
            return this;
        }
        public EnergyLocomotiveDataAccessor.Builder withRouteSize(IntSupplier s) {
            this.arr.setSupplier(8, s);
            return this;
        }


        @Override
        public EnergyLocomotiveDataAccessor build() {
            return new EnergyLocomotiveDataAccessor(this.arr);
        }
    }
}
