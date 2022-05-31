package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class SteamHeadVehicleDataAccessor extends DataAccessor {
    public SteamHeadVehicleDataAccessor(ContainerData data) {
        super(data);
    }

    public int getBurnProgress() {
        return this.data.get(1);
    }

    public boolean isLit() {
        return this.data.get(2) == 1;
    }

    public boolean isOn() {
        return this.data.get(3) == 1;
    }

    public int visitedSize() {
        return this.data.get(4);
    }

    public int routeSize() {
        return this.data.get(5);
    }


    public static class Builder {
        SupplierIntArray arr;

        public Builder(int uuid) {
            this.arr = new SupplierIntArray(6);
            this.arr.set(0, uuid);
        }

        public Builder withBurnProgress(IntSupplier burnProgress) {
            this.arr.setSupplier(1, burnProgress);
            return this;
        }

        public Builder withLit(BooleanSupplier lit) {
            this.arr.setSupplier(2, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }


        public Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(3, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public Builder withVisitedSize(IntSupplier s) {
            this.arr.setSupplier(4, s);
            return this;
        }

        public Builder withRouteSize(IntSupplier s) {
            this.arr.setSupplier(5, s);
            return this;
        }

        public SteamHeadVehicleDataAccessor build() {
            return new SteamHeadVehicleDataAccessor(this.arr);
        }
    }
}
