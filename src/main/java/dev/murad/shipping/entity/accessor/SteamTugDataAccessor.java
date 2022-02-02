package dev.murad.shipping.entity.accessor;

import net.minecraft.util.IIntArray;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class SteamTugDataAccessor extends DataAccessor {
    public SteamTugDataAccessor(IIntArray data) {
        super(data);
    }

    public int getBurnProgress() {
        return this.data.get(1);
    }

    public boolean isLit() {
        return this.data.get(2) == 1;
    }

    public static class Builder {
        SupplierIntArray arr;

        public Builder(int uuid) {
            this.arr = new SupplierIntArray(3);
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

        public SteamTugDataAccessor build() {
            return new SteamTugDataAccessor(this.arr);
        }
    }
}
