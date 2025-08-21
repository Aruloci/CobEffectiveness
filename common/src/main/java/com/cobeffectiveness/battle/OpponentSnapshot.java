package com.cobeffectiveness.battle;

import com.cobeffectiveness.type.Type;

import java.util.*;

public final class OpponentSnapshot {
    private static Set<Type> types = Collections.emptySet();
    private static long updatedAt = 0L;

    private static final long TTL_MS = 5_000;

    private OpponentSnapshot(){}

    public static void update(Set<Type> newTypes) {
        if (newTypes == null || newTypes.isEmpty()) {
            clear();
            return;
        }
        types = EnumSet.copyOf(newTypes);
        updatedAt = System.currentTimeMillis();
    }

    public static void clear() {
        types = Collections.emptySet();
        updatedAt = 0L;
    }

    public static Optional<Set<Type>> current() {
        Set<Type> snap = types;
        if (snap.isEmpty()) return Optional.empty();
        long now = System.currentTimeMillis();
        if (updatedAt == 0L || now - updatedAt > TTL_MS) return Optional.empty();
        return Optional.of(EnumSet.copyOf(snap));
    }
}
