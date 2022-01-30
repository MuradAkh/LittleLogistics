package dev.murad.shipping.data.accessor;

import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class EnergyTugDataAccessor extends DataAccessor {
    public EnergyTugDataAccessor(IIntArray data) {
        super(data);
    }

    public int getEnergy() {
        return this.data.get(1);
    }

    public int getCapacity() {
        return this.data.get(2);
    }

    public boolean isLit() {
        return this.data.get(3) == 1;
    }

    public static class Builder {
        SupplierIntArray arr;

        public Builder(int uuid) {
            this.arr = new SupplierIntArray(4);
            this.arr.set(0, uuid);
        }

        public Builder withEnergy(IntSupplier energy) {
            this.arr.setSupplier(1, energy);
            return this;
        }

        public Builder withCapacity(IntSupplier capacity) {
            this.arr.setSupplier(2, capacity);
            return this;
        }

        public Builder withLit(BooleanSupplier lit) {
            this.arr.setSupplier(3, () -> lit.getAsBoolean() ? 1 : -1);
            return this;
        }

        public EnergyTugDataAccessor build() {
            return new EnergyTugDataAccessor(this.arr);
        }
    }
}
