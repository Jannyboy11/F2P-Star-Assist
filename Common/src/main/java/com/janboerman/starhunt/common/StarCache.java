package com.janboerman.starhunt.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;

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
        if (oldStar != null && newStar.getTier().getSize() > oldStar.getTier().getSize()) {
            cache.asMap().put(key, newStar);
            return true;
        } else {
            return cache.asMap().putIfAbsent(newStar.getKey(), newStar) != null;
        }
    }

    public void remove(StarKey starKey) {
        cache.invalidate(starKey);
    }

//    private void degrade(StarKey starKey) {
//        CrashedStar crashedStar = cache.getIfPresent(starKey);
//        if (crashedStar != null) {
//            CrashedStar degraded = crashedStar.degraded();
//            if (degraded == null) remove(starKey);
//            else cache.put(starKey, degraded);
//        }
//    }

    public Set<CrashedStar> getStars() {
        return new HashSet<>(cache.asMap().values());
    }
}
