package dev.murad.shipping.data.accessor;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;

public class DataAccessor implements IIntArray {
    protected IIntArray data;

    public DataAccessor(IIntArray data) {
        this.data = data;
    }

    public IIntArray getRawData() {
        return this.data;
    }

    public void write(PacketBuffer buffer) {
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
