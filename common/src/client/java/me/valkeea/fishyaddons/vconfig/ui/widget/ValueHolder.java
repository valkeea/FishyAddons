package me.valkeea.fishyaddons.vconfig.ui.widget;

import org.jetbrains.annotations.Nullable;

public interface ValueHolder {
    void set(String v);
    String get();

    default void update(@Nullable String v) {
        if (v != null && !v.equals(get())) set(v);
    }
}
