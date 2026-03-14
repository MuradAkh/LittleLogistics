package dev.murad.shipping.block.rail;

import net.minecraft.core.Direction;

import java.util.Set;

public class BranchingRailConfiguration {

    private final Direction rootDirection;
    private final Direction unpoweredDirection;
    private final Direction poweredDirection;

    public BranchingRailConfiguration(Direction rootDirection, Direction unpoweredDirection, Direction poweredDirection) {
        this.rootDirection = rootDirection;
        this.unpoweredDirection = unpoweredDirection;
        this.poweredDirection = poweredDirection;
    }

    public Direction getRootDirection() {
        return rootDirection;
    }

    public Direction getUnpoweredDirection() {
        return unpoweredDirection;
    }

    public Direction getPoweredDirection() {
        return poweredDirection;
    }

    static final Set<Direction> NO_POSSIBILITIES = Set.of();
    public Set<Direction> getPossibleDirections(Direction inputSide, boolean automaticSwitching, boolean powered) {
        if (inputSide == getRootDirection()) {
            if (automaticSwitching) {
                return Set.of(getUnpoweredDirection(), getPoweredDirection());
            } else {
                return powered ? Set.of(getPoweredDirection()) : Set.of(getUnpoweredDirection());
            }
        }

        if (inputSide == getUnpoweredDirection()) {
            if (automaticSwitching) {
                return Set.of(getRootDirection());
            } else {
                return powered ? NO_POSSIBILITIES : Set.of(getRootDirection());
            }
        }

        if (inputSide == getPoweredDirection()) {
            if (automaticSwitching) {
                return Set.of(getRootDirection());
            } else {
                return powered ? Set.of(getRootDirection()) : NO_POSSIBILITIES;
            }
        }

        return NO_POSSIBILITIES;
    }
}
