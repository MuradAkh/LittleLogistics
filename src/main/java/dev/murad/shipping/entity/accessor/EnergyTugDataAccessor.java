package dev.murad.shipping.entity.accessor;

import net.minecraft.util.IIntArray;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class EnergyTugDataAccessor extends DataAccessor {
    private static final int SHORT_MASK = 0xFFFF;

    public EnergyTugDataAccessor(IIntArray data) {
        super(data);
    }

    /**
     * Lil-endian
     */

    public int getEnergy() {
        int lo = this.data.get(1) & SHORT_MASK;
        int hi = this.data.get(2) & SHORT_MASK;
        return lo | hi << 16;
    }

    public int getCapacity() {
        int lo = this.data.get(3) & SHORT_MASK;
        int hi = this.data.get(4) & SHORT_MASK;
        return lo | hi << 16;
    }

    public boolean isLit() {
        return this.data.get(5) == 1;
    }

    public static class Builder {
        SupplierIntArray arr;

        public Builder(int uuid) {
            this.arr = new SupplierIntArray(6);
            this.arr.set(0, uuid);
        }

        public Builder withEnergy(IntSupplier energy) {
            this.arr.setSupplier(1, () -> energy.getAsInt() & SHORT_MASK);
            this.arr.setSupplier(2, () -> (energy.getAsInt() >> 16) & SHORT_MASK);
            return this;
        }

        public Builder withCapacity(IntSupplier capacity) {
            this.arr.setSupplier(3, () -> capacity.getAsInt() & SHORT_MASK);
            this.arr.setSupplier(4, () -> (capacity.getAsInt() >> 16) & SHORT_MASK);
            return this;
        }

        public Builder withLit(BooleanSupplier lit) {
            this.arr.setSupplier(5, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public EnergyTugDataAccessor build() {
            return new EnergyTugDataAccessor(this.arr);
        }
    }
}