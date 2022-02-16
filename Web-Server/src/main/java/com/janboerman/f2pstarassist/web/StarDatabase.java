package com.janboerman.f2pstarassist.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.janboerman.f2pstarassist.common.CrashedStar;
import com.janboerman.f2pstarassist.common.GroupKey;
import com.janboerman.f2pstarassist.common.StarCache;
import com.janboerman.f2pstarassist.common.StarKey;
import com.janboerman.f2pstarassist.common.StarTier;
import com.janboerman.f2pstarassist.common.StarUpdate;
import com.janboerman.f2pstarassist.common.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

public class StarDatabase {

    private final Cache<GroupKey, StarCache> groupCaches = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(2).plusMinutes(30))
            .build();
    private final WeakHashMap<StarKey, Set<GroupKey>> owningGroups = new WeakHashMap<>();
    private final StarListener starListener;

    public StarDatabase(StarListener listener) {
        this.starListener = Objects.requireNonNull(listener);
    }

    private StarCache newStarCache() {
        return new StarCache(removedEvent -> {
            StarKey starKey = removedEvent.getKey();
            Set<GroupKey> groupKeys = owningGroups.get(starKey);
            if (groupKeys != null) {
                for (GroupKey groupKey : groupKeys) {
                    starListener.onRemove(groupKey, starKey); //notify discord bot
                }
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
    public synchronized CrashedStar add(Set<GroupKey> groupKeys, CrashedStar crashedStar) {
        assert groupKeys != null;
        assert crashedStar != null;

        StarKey starKey = crashedStar.getKey();
        Set<GroupKey> owningGroups = this.owningGroups.get(starKey);

        if (owningGroups == null) {
            //star didn't have an owner yet.
            this.owningGroups.put(starKey, owningGroups);
            //notify listener
            for (GroupKey groupKey : groupKeys) {
                starListener.onAdd(groupKey, crashedStar);
            }
            return null;
        } else if (owningGroups.containsAll(groupKeys)) {
            //all groups are the owners of this star
            CrashedStar existingStar = null;

            for (GroupKey groupKey : owningGroups) {
                StarCache starCache = getStarCache(groupKey);
                CrashedStar ex = starCache.add(crashedStar);

                //notify listener and calculate lowest existing star
                if (ex == null) {
                    starListener.onAdd(groupKey, crashedStar);
                } else {
                    if (existingStar == null) {
                        existingStar = ex;
                    } else {
                        if (existingStar.getTier().getSize() > ex.getTier().getSize()) {
                            existingStar = ex;
                        }
                    }
                    starListener.onUpdate(groupKey, new StarUpdate(starKey, crashedStar.getTier()));
                }
            }

            return existingStar;
        } else {
            //another group already found the star first - pretend the star is new
            return null;
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
            add(Set.of(groupKey), existingStar);
            //if a different group already found the star, then add simply returns null without updating the cache.
            //yet, we still want to return the 'new' star.
            return existingStar;
        }
    }

    public synchronized CrashedStar get(GroupKey groupKey, StarKey starKey) {
        assert groupKey != null;
        assert starKey != null;

        Set<GroupKey> existingGroups = owningGroups.get(starKey);
        if (existingGroups == null || !existingGroups.contains(groupKey)) {
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
        owningGroups.computeIfAbsent(starKey, k -> new HashSet<>()).add(groupKey);

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
        if (removed) {
            Set<GroupKey> groups = owningGroups.remove(starKey);
            if (groups != null) groups.remove(groupKey);
        }
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
