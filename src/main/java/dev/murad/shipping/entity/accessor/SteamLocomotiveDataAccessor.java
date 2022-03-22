package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;

public class SteamLocomotiveDataAccessor extends SteamTugDataAccessor{
    public SteamLocomotiveDataAccessor(ContainerData data) {
        super(data);
    }

    public boolean isOn() {
        return this.data.get(3) == 1;
    }


    public static class Builder extends SteamTugDataAccessor.Builder {
        public Builder(int uuid) {
            super(uuid);
            this.arr = new SupplierIntArray(4);
            this.arr.set(0, uuid);
        }

        public SteamTugDataAccessor.Builder withOn(BooleanSupplier lit) {
            this.arr.setSupplier(3, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        @Override
        public SteamLocomotiveDataAccessor build() {
            return new SteamLocomotiveDataAccessor(this.arr);
        }
    }
}
