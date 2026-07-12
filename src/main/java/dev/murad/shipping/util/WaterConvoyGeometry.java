package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;

/**
 * Shared geometry for water convoys.  These values describe vehicle centre
 * spacing; docking has its own explicit one-block parked pose.
 *
 */
public final class WaterConvoyGeometry {

    /** Consecutive docking-station vehicle cells are one block apart. */
    public static final double DOCKED_CENTER_SPACING = 1.0D;
    /** Open-water convoy spacing.  This preserves the established spring spacing. */
    public static final double CRUISING_CENTER_SPACING = 1.2D;

    /** Half-length used to orient a body on a curved route. */
    private static final double BARGE_ROUTE_HALF_LENGTH = 0.50D;
    private static final double TUG_ROUTE_HALF_LENGTH = 0.56D;

    private WaterConvoyGeometry() {
    }

    public static double cruisingCenterSpacing(VesselEntity leader, VesselEntity follower) {
        return CRUISING_CENTER_SPACING;
    }

    public static double routeBodyHalfLength(VesselEntity vessel) {
        return vessel instanceof AbstractTugEntity ? TUG_ROUTE_HALF_LENGTH : BARGE_ROUTE_HALF_LENGTH;
    }

}
