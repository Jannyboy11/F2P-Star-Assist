package com.janboerman.starhunt.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StarCache {

    private final Cache<StarKey, CrashedStar> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    public boolean add(CrashedStar newStar) {
        StarKey key = newStar.getKey();
        CrashedStar oldStar = cache.getIfPresent(key);
        if (oldStar != null) {
            if (newStar.getTier().getSize() > oldStar.getTier().getSize()) {
                cache.asMap().put(key, newStar);
                return true;
            } else {
                return false;
            }
        } else {
            return cache.asMap().putIfAbsent(newStar.getKey(), newStar) == null;
        }
    }

    public boolean addAll(Collection<CrashedStar> stars) {
        boolean result = false;
        for (CrashedStar star : stars) {
            result |= add(star);
        }
        return result;
    }

    public CrashedStar get(StarKey key) {
        return cache.getIfPresent(key);
    }

    public void forceAdd(CrashedStar star) {
        cache.put(star.getKey(), star);
    }

    public boolean remove(StarKey starKey) {
        boolean contains = cache.getIfPresent(starKey) != null;
        cache.invalidate(starKey);
        return contains;
    }

    public Set<CrashedStar> getStars() {
        return new HashSet<>(cache.asMap().values());
    }
}
