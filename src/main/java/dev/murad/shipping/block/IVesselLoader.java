package dev.murad.shipping.block;

import dev.murad.shipping.entity.custom.VesselEntity;

public interface IVesselLoader {
    enum Mode {
        EXPORT,
        IMPORT
    }

    public boolean holdVessel(VesselEntity vessel, Mode mode);
}
