package dev.murad.shipping.entity;

import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorableTest {

    @Test
    void defaultsToRedAndNormalizesInvalidSavedValuesToRed() {
        assertEquals(DyeColor.RED.getId(), Colorable.DEFAULT_COLOR);
        assertEquals(DyeColor.RED.getId(), Colorable.normalizeColor(-1));
        assertEquals(DyeColor.RED.getId(), Colorable.normalizeColor(16));
    }

    @Test
    void requiresExactNormalizedColourEquality() {
        assertTrue(Colorable.colorsMatch(DyeColor.RED.getId(), DyeColor.RED.getId()));
        assertTrue(Colorable.colorsMatch(-1, DyeColor.RED.getId()));
        assertFalse(Colorable.colorsMatch(DyeColor.RED.getId(), DyeColor.BLUE.getId()));
    }
}
