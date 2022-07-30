package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class HeadVehicleDataAccessor extends DataAccessor{
    public HeadVehicleDataAccessor(ContainerData data) {
        super(data);
    }

    public boolean isLit() {
        return this.data.get(1) == 1;
    }

    public boolean isOn() {
        return this.data.get(2) == 1;
    }

    public int visitedSize() {
        return this.data.get(3);
    }

    public int routeSize() {
        return this.data.get(4);
    }

    public boolean canMove() {
        return this.data.get(5) == 1;
    }

    public static class Builder {
        SupplierIntArray arr;

        public Builder withId(int id) {
            this.arr.set(0, id);
            return this;
        }

        public Builder withLit(BooleanSupplier lit) {
            this.arr.setSupplier(1, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }


        public Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(2, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public Builder withVisitedSize(IntSupplier s) {
            this.arr.setSupplier(3, s);
            return this;
        }

        public Builder withRouteSize(IntSupplier s) {
            this.arr.setSupplier(4, s);
            return this;
        }

        public Builder withCanMove(BooleanSupplier lit) {
            this.arr.setSupplier(5, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public HeadVehicleDataAccessor build() {
            return new HeadVehicleDataAccessor(this.arr);
        }
    }
}
