package dev.murad.shipping.entity;

import javax.annotation.Nullable;

public interface Colorable {
    @Nullable
    Integer getColor();

    void setColor(@Nullable Integer color);
}
