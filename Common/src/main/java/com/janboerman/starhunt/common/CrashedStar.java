package com.janboerman.starhunt.common;

import java.time.Instant;
import java.util.Objects;

public final class CrashedStar {

    private final StarTier tier;
    private final StarLocation location;
    private final int world;

    private final Instant detectedAt;
    private final String discoveredBy;

    public CrashedStar(StarTier tier, StarLocation location, int world, Instant detectedAt, String discoveredBy) {
        this.tier = tier;
        this.location = location;
        this.world = world;
        this.detectedAt = detectedAt;
        this.discoveredBy = discoveredBy;
    }

    public StarTier getTier() {
        return tier;
    }

    public StarLocation getLocation() {
        return location;
    }

    public int getWorld() {
        return world;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public String getDiscoveredBy() {
        return discoveredBy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CrashedStar)) return false;

        CrashedStar that = (CrashedStar) o;
        return Objects.equals(this.getLocation(), that.getLocation())
                && Objects.equals(this.getWorld(), that.getWorld());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation(), getWorld());
    }

    @Override
    public String toString() {
        return "CrashedStar"
                + "{tier=" + getTier()
                + ",location=" + getLocation()
                + ",world=" + getWorld()
                + ",detected at=" + getDetectedAt()
                + ",detected by=" + getDiscoveredBy()
                + "}";
    }
}
