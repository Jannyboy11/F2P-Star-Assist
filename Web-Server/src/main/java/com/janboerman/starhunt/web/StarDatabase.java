package com.janboerman.starhunt.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.GroupKey;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;
import com.janboerman.starhunt.common.StarTier;
import com.janboerman.starhunt.common.StarUpdate;
import com.janboerman.starhunt.common.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

public class StarDatabase {

    private final Cache<GroupKey, StarCache> groupCaches = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(2).plusMinutes(30))
            .build();
    private final WeakHashMap<StarKey, GroupKey/*TODO Set<GroupKey>*/> starsFoundByGroup = new WeakHashMap<>();
    private final StarListener starListener;

    public StarDatabase(StarListener listener) {
        this.starListener = Objects.requireNonNull(listener);
    }

    private StarCache newStarCache() {
        return new StarCache(removedEvent -> {
            StarKey starKey = removedEvent.getKey();
            GroupKey groupKey = starsFoundByGroup.get(starKey);
            if (groupKey != null) {
                starListener.onRemove(groupKey, starKey); //notify discord bot
            }
        });
    }

    private StarCache getStarCache(GroupKey groupKey) {
        try {
            return groupCaches.get(groupKey, this::newStarCache);
        } catch (ExecutionException e) {
            throw new RuntimeException("cannot occur", e);
        }
    }

    //returns the existing star, or null if no star existed with that StarKey (like Map.put)
    public synchronized CrashedStar add(GroupKey groupKey/*TODO Set<GroupKey>*/, CrashedStar crashedStar) {
        assert groupKey != null;
        assert crashedStar != null;

        StarKey starKey = crashedStar.getKey();
        GroupKey owningGroup = starsFoundByGroup.computeIfAbsent(starKey, k -> groupKey);

        if (!owningGroup.equals(groupKey)) {
            //another group already found the star first - pretend the star is new
            return null;
        } else {
            //groupKey's group is the owner of this star
            StarCache starCache = getStarCache(groupKey);
            CrashedStar existingStar = starCache.add(crashedStar);

            //notify listener
            if (existingStar == null) {
                starListener.onAdd(groupKey, crashedStar);
            } else {
                starListener.onUpdate(groupKey, new StarUpdate(crashedStar.getKey(), crashedStar.getTier()));
            }

            return existingStar;
        }
    }

    //returns the new complete star
    public synchronized CrashedStar update(GroupKey groupKey, StarUpdate starUpdate) {
        assert groupKey != null;
        assert starUpdate != null;

        StarKey starKey = starUpdate.getKey();
        StarTier newTier = starUpdate.getTier();

        CrashedStar existingStar = get(groupKey, starKey);

        if (existingStar != null) {
            if (existingStar.getTier().compareTo(newTier) > 0) {
                //only update if the currently known tier is higher
                existingStar.setTier(newTier);
                //notify listener
                starListener.onUpdate(groupKey, starUpdate);
            }
            return existingStar;
        } else {
            //update for a non-existing star. just pretend it is new.
            existingStar = new CrashedStar(starKey, newTier, Instant.now(), User.unknown());
            //add it to cache
            add(groupKey, existingStar);
            //if a different group already found the star, then add simply returns null without updating the cache.
            //yet, we still want to return the 'new' star.
            return existingStar;
        }
    }

    public synchronized CrashedStar get(GroupKey groupKey, StarKey starKey) {
        assert groupKey != null;
        assert starKey != null;

        GroupKey existingGroup = starsFoundByGroup.get(starKey);
        if (!groupKey.equals(existingGroup)) {
            return null;
        } else {
            return getStarCache(groupKey).get(starKey);
        }
    }

    public synchronized void forceAdd(GroupKey groupKey, CrashedStar crashedStar) {
        assert groupKey != null;
        assert crashedStar != null;

        StarCache starCache = getStarCache(groupKey);
        StarKey starKey = crashedStar.getKey();

        //perform operation
        starCache.forceAdd(crashedStar);
        //mark as owned if this is the first group that finds this star
        starsFoundByGroup.putIfAbsent(starKey, groupKey);

        //notify listener
        if (starCache.contains(starKey)) {
            starListener.onUpdate(groupKey, new StarUpdate(starKey, crashedStar.getTier()));
        } else {
            starListener.onAdd(groupKey, crashedStar);
        }
    }

    public synchronized boolean remove(GroupKey groupKey, StarKey starKey) {
        assert groupKey != null;
        assert starKey != null;

        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return false;
        boolean removed = starCache.remove(starKey) != null;
        if (removed) starsFoundByGroup.remove(starKey, groupKey);
        return removed;
        //don't notify starListener here because we're already using the RemovalListener.
    }

    public synchronized Set<CrashedStar> getStars(GroupKey groupKey) {
        assert groupKey != null;

        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return Collections.emptySet();
        return starCache.getStars();
    }

}
