package com.janboerman.f2pstarassist.common;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CrashedStar implements Comparable<CrashedStar>, Cloneable {

    private static final Comparator<CrashedStar> COMPARATOR = Comparator
            .comparing(CrashedStar::getTier)
            .thenComparing(CrashedStar::getWorld)
            .thenComparing(CrashedStar::getLocation);

    @Nullable private Long databaseId;

    @Nonnull private final StarLocation location;
    private final int world;
    @Nonnull private StarTier tier;

    @Nonnull private final Instant detectedAt;
    @Nonnull private final User discoveredBy;

    public CrashedStar(StarTier tier, StarLocation location, int world, Instant detectedAt, User discoveredBy) {
        Objects.requireNonNull(tier,"tier cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(detectedAt, "detection timestamp cannot be null");
        Objects.requireNonNull(discoveredBy, "discoveredBy user cannot be null");

        this.tier = tier;
        this.location = location;
        this.world = world;
        this.detectedAt = detectedAt;
        this.discoveredBy = discoveredBy;
    }

    public CrashedStar(StarKey key, StarTier tier, Instant detectedAt, User discoveredBy) {
        this(tier, key.getLocation(), key.getWorld(), detectedAt, discoveredBy);
    }

    public StarTier getTier() {
        return tier;
    }

    //TODO does this still need to be synchronised? can't we just always use the client thread?
    public synchronized void setTier(StarTier lowerTier) {
        assert lowerTier != null : "tier cannot be null";
        tier = lowerTier;
    }

    public Long getId() {
        return databaseId;
    }

    public boolean hasId() {
        return databaseId == null || databaseId.longValue() == 0L;
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

    public User getDiscoveredBy() {
        return discoveredBy;
    }

    public StarKey getKey() {
        return new StarKey(getLocation(), getWorld());
    }

    public void setId(Long databaseId) {
        assert databaseId != null && databaseId.longValue() != 0L;

        this.databaseId = databaseId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CrashedStar)) return false;

        CrashedStar that = (CrashedStar) o;
        return this.getLocation() == that.getLocation()
                && this.getWorld() == that.getWorld()
                && this.getTier() == that.getTier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation(), getWorld(), getTier());
    }

    @Override
    public int compareTo(CrashedStar that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public String toString() {
        return "CrashedStar"
                + "{databaseId=" + getId()
                + ",tier=" + getTier()
                + ",location=" + getLocation()
                + ",world=" + getWorld()
                + ",detected at=" + getDetectedAt()
                + ",discovered by=" + getDiscoveredBy()
                + "}";
    }

}
