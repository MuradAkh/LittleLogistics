package dev.murad.shipping.block.dock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

@Getter
@RequiredArgsConstructor
public enum DockingMode implements StringRepresentable {
    // Wait until entity is full
    WAIT_UNTIL_FULL("wait_until_full"),

    // Wait until entity is empty
    WAIT_UNTIL_EMPTY("wait_until_empty"),

    // If nothing has been transferred for x amount of time,
    // assume that nothing is transferable.
    WAIT_TIMEOUT("wait_timeout"),
    ;

    private final String serializedName;

    public static final EnumProperty<DockingMode> PROPERTY =
            EnumProperty.create("docking_mode", DockingMode.class);

    public DockingMode nextState() {
        return switch(this) {
            case WAIT_TIMEOUT -> WAIT_UNTIL_FULL;
            case WAIT_UNTIL_FULL -> WAIT_UNTIL_EMPTY;
            case WAIT_UNTIL_EMPTY -> WAIT_TIMEOUT;
        };
    }

    public String getTranslatableTextKey() {
        return String.format("global.littlelogistics.docking_mode.%s", serializedName);
    }
}
