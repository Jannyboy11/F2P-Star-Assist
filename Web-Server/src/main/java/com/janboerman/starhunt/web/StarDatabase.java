package com.janboerman.starhunt.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.GroupKey;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StarDatabase {

    private final Cache<GroupKey, StarCache> groupCaches = CacheBuilder.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .build();
    private final WeakHashMap<StarKey, GroupKey> starsFoundByGroup = new WeakHashMap<>();

    public synchronized boolean add(GroupKey groupKey, CrashedStar crashedStar) {
        if (starsFoundByGroup.putIfAbsent(crashedStar.getKey(), groupKey) != null) return false;

        try {
            return groupCaches.get(groupKey, StarCache::new).add(crashedStar);
        } catch (ExecutionException e) {
            throw new RuntimeException("cannot occur", e);
        }
    }

    public synchronized CrashedStar get(GroupKey groupKey, StarKey starKey) {
        try {
            return groupCaches.get(groupKey, StarCache::new).get(starKey);
        } catch (ExecutionException e) {
            throw new RuntimeException("cannot occur", e);
        }
    }

    public synchronized void forceAdd(GroupKey groupKey, CrashedStar crashedStar) {
        try {
            groupCaches.get(groupKey, StarCache::new).forceAdd(crashedStar);
        } catch (ExecutionException e) {
            throw new RuntimeException("cannot occur", e);
        }
    }

    public synchronized boolean remove(GroupKey groupKey, StarKey starKey) {
        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return false;
        boolean removed = starCache.remove(starKey);
        if (removed) starsFoundByGroup.remove(starKey, groupKey);
        return removed;
    }

    public synchronized Set<CrashedStar> getStars(GroupKey groupKey) {
        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return Collections.emptySet();
        return starCache.getStars();
    }

}
