package com.janboerman.f2pstarassist.plugin;

import com.google.common.cache.*;
import com.janboerman.f2pstarassist.plugin.model.CrashedStar;
import com.janboerman.f2pstarassist.plugin.model.StarKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StarCache {

    private final Cache<StarKey, CrashedStar> cache;

    private static CacheBuilder<StarKey, CrashedStar> newCacheBuilder() {
        return (CacheBuilder<StarKey, CrashedStar>) (CacheBuilder) CacheBuilder.newBuilder();
    }

    private StarCache(CacheBuilder<StarKey, CrashedStar> cacheBuilder) {
        this.cache = cacheBuilder
                .expireAfterWrite(93, TimeUnit.MINUTES)    // 1 hour, 33 minutes
                .build();
    }

    public StarCache(RemovalListener<StarKey, CrashedStar> removalListener) {
        this(newCacheBuilder().removalListener(removalListener));
    }

    public StarCache() {
        this(newCacheBuilder());
    }

    //returns the old star
    public CrashedStar add(CrashedStar newStar) {
        StarKey key = newStar.getKey();
        CrashedStar oldStar = cache.getIfPresent(key);
        if (oldStar != null) {
            if (newStar.getTier().compareTo(oldStar.getTier()) < 0) {
                oldStar.setTier(newStar.getTier());
            }
        } else {
            cache.put(key, newStar);
        }
        return oldStar;
    }

    //returns true if a star was added, false otherwise
    public boolean addAll(Collection<CrashedStar> stars) {
        boolean result = false;
        for (CrashedStar star : stars) {
            assert star != null;
            CrashedStar oldStar = add(star);
            if (oldStar != null) {
                oldStar.setId(star.getId());
            }
            result |= (oldStar == null);
        }
        return result;
    }

    public CrashedStar get(StarKey key) {
        CrashedStar crashedStar = cache.getIfPresent(key);
        if (crashedStar == null /*does not exist*/
                || crashedStar.getDetectedAt().isBefore(Instant.now().minus(2, ChronoUnit.HOURS))) /*more than two hours ago*/ {
            return null;
        } else {
            return crashedStar;
        }
    }

    public void receiveStars(List<CrashedStar> starList) {
        List<CrashedStar> theNewList = new ArrayList<>(starList);

        // All existing stars which have a database id but are not in the new list are to be removed.
        Set<StarKey> newStars = starList.stream().map(CrashedStar::getKey).collect(Collectors.toSet());
        for (CrashedStar existing : getStars()) {
            StarKey key = existing.getKey();
            if (existing.hasId()) {
                if (!newStars.contains(key)) {
                    remove(key);
                }
            } else {
                // The existing star has no database id.
                // Keep stars which were not synced with server anyway.
                theNewList.add(existing);
            }
        }

        addAll(theNewList);
    }

    //returns the old star
    public CrashedStar remove(StarKey starKey) {
        CrashedStar existing = get(starKey);
        cache.invalidate(starKey);
        return existing;
    }

    public boolean contains(StarKey starKey) {
        return get(starKey) != null;
    }

    public Set<CrashedStar> getStars() {
        return new HashSet<>(cache.asMap().values());
    }

    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", "StarCache{", "}");
        for (Map.Entry<StarKey, CrashedStar> entry : cache.asMap().entrySet()) {
            stringJoiner.add(entry.getKey() + "=" + entry.getValue().getTier());
        }
        return stringJoiner.toString();
    }
}
