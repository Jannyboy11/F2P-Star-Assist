package com.janboerman.starhunt.common;

import java.util.Comparator;
import java.util.Objects;

public class StarKey implements Comparable<StarKey> {

    private static final Comparator<StarKey> COMPARATOR = Comparator
            .comparing(StarKey::getLocation)
            .thenComparing(StarKey::getWorld);

    private final StarLocation location;
    private final int world;

    public StarKey(StarLocation location, int world) {
        this.location = location;
        this.world = world;
    }

    public StarLocation getLocation() {
        return location;
    }

    public int getWorld() {
        return world;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, world);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StarKey)) return false;

        StarKey that = (StarKey) obj;
        return this.location == that.location && this.world == that.world;
    }

    @Override
    public int compareTo(StarKey that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public String toString() {
        return "StarKey{location=" + location + ",world=" + world + "}";
    }

}
