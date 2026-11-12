package me.valkeea.fishyaddons.feature.qol.input;

import java.util.HashSet;
import java.util.Set;

final class KeyPressTracker<T> {
    private final Set<T> held = new HashSet<>();

    boolean update(T key, boolean pressed, boolean allowed) {
        if (!pressed) {
            held.remove(key);
            return false;
        }
        return held.add(key) && allowed;
    }

    void retainKeys(Set<T> keys) {
        held.retainAll(keys);
    }
}
