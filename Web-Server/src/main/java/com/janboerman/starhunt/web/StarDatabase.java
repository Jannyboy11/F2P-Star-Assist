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

    //returns null if the crashedStar is new, or returns an existing star if one already exists.
    public synchronized CrashedStar addIfAbsent(GroupKey groupKey, CrashedStar crashedStar) {
        StarKey starKey = crashedStar.getKey();
        GroupKey owningGroup = starsFoundByGroup.computeIfAbsent(starKey, k -> groupKey);
        if (!owningGroup.equals(groupKey)) {
            //another group already found the star first - pretend we don't know about it
            return null;
        } else {
            //groupKey's group is the owner of this star
            try {
                StarCache starCache = groupCaches.get(groupKey, StarCache::new);
                CrashedStar existingStar = starCache.get(starKey);
                if (existingStar != null && existingStar.getTier().getSize() < crashedStar.getTier().getSize()) {
                    //existing star was already at a lower tier
                    return existingStar;
                } else {
                    //existingStar is null, or has a higher tier
                    starCache.forceAdd(crashedStar);
                    return null;
                }
            } catch (ExecutionException e) {
                throw new RuntimeException("cannot occur", e);
            }
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
