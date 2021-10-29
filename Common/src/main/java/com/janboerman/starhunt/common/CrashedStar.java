package com.janboerman.starhunt.common;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

public final class CrashedStar implements Comparable<CrashedStar> {

    private static final Comparator<CrashedStar> COMPARATOR = Comparator
            .comparing(CrashedStar::getTier)
            .thenComparing(CrashedStar::getLocation)
            .thenComparing(CrashedStar::getWorld);

    private final StarLocation location;
    private final int world;
    private StarTier tier;

    private final Instant detectedAt;
    private final String discoveredBy;

    public CrashedStar(StarTier tier, StarLocation location, int world, Instant detectedAt, String discoveredBy) {
        assert tier != null : "tier cannot be null";
        assert location != null : "location cannot be null";
        assert detectedAt != null : "detection timestamp cannot be null";

        this.tier = tier;
        this.location = location;
        this.world = world;
        this.detectedAt = detectedAt;
        this.discoveredBy = discoveredBy;   //differentiate between a Discord user and a RuneScape user?
        //number of miners?
    }

    public CrashedStar(StarKey key, StarTier tier, Instant detectedAt, String discoveredBy) {
        this(tier, key.getLocation(), key.getWorld(), detectedAt, discoveredBy);
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

    public StarKey getKey() {
        return new StarKey(getLocation(), getWorld());
    }

    public void setTier(StarTier lowerTier) {
        assert lowerTier != null : "tier cannot be null";
        tier = lowerTier;
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
    public int compareTo(CrashedStar that) {
        return COMPARATOR.compare(this, that);
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
