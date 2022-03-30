package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class SteamLocomotiveDataAccessor extends SteamTugDataAccessor{
    public SteamLocomotiveDataAccessor(ContainerData data) {
        super(data);
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

    public static class Builder extends SteamTugDataAccessor.Builder {
        public Builder(int uuid) {
            super(uuid);
            this.arr = new SupplierIntArray(6);
            this.arr.set(0, uuid);
        }

        public SteamLocomotiveDataAccessor.Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(3, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public SteamLocomotiveDataAccessor.Builder withVisitedSize(IntSupplier s) {
            this.arr.setSupplier(4, s);
            return this;
        }
        public SteamLocomotiveDataAccessor.Builder withRouteSize(IntSupplier s) {
            this.arr.setSupplier(5, s);
            return this;
        }


        @Override
        public SteamLocomotiveDataAccessor build() {
            return new SteamLocomotiveDataAccessor(this.arr);
        }
    }
}
