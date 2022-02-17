package dev.murad.shipping.entity.accessor;

import net.minecraft.world.inventory.ContainerData;

public class TugRouteScreenDataAccessor extends DataAccessor {
    public TugRouteScreenDataAccessor(ContainerData data) {
        super(data);
    }

    public boolean isOffHand() {
        return this.data.get(1) == 1;
    }

    public static class Builder {
        SupplierIntArray arr;

        public Builder(int uuid) {
            this.arr = new SupplierIntArray(2);
            this.arr.set(0, uuid);
        }

        public Builder withOffHand(boolean isOffHand) {
            this.arr.setSupplier(1, () -> isOffHand ? 1 : 0);
            return this;
        }

        public TugRouteScreenDataAccessor build() {
            return new TugRouteScreenDataAccessor(this.arr);
        }
    }
}
