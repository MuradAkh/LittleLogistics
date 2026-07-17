package dev.murad.shipping.entity;

import net.minecraft.world.item.DyeColor;

public interface Colorable {
    int DEFAULT_COLOR = DyeColor.RED.getId();

    static int normalizeColor(int color) {
        return color >= 0 && color < DyeColor.values().length ? color : DEFAULT_COLOR;
    }

    static boolean colorsMatch(int first, int second) {
        return normalizeColor(first) == normalizeColor(second);
    }

    int getColor();

    void setColor(int color);
}
