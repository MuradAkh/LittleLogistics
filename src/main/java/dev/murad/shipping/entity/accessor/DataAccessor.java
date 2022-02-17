package dev.murad.shipping.entity.accessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.ContainerData;

public class DataAccessor implements ContainerData {
    protected ContainerData data;

    public DataAccessor(ContainerData data) {
        this.data = data;
    }

    public ContainerData getRawData() {
        return this.data;
    }

    public void write(FriendlyByteBuf buffer) {
        for (int i = 0; i < data.getCount(); i++) {
            buffer.writeInt(data.get(i));
        }
    }

    public int getEntityUUID() {
        return this.data.get(0);
    }

    @Override
    public int get(int i) {
        return data.get(i);
    }

    @Override
    public void set(int i, int j) {
        data.set(i, j);
    }

    @Override
    public int getCount() {
        return data.getCount();
    }
}
