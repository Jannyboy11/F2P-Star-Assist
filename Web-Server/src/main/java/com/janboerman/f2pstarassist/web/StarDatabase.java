package com.janboerman.f2pstarassist.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.janboerman.f2pstarassist.common.CrashedStar;
import com.janboerman.f2pstarassist.common.GroupKey;
import com.janboerman.f2pstarassist.common.StarCache;
import com.janboerman.f2pstarassist.common.StarKey;
import com.janboerman.f2pstarassist.common.StarList;
import com.janboerman.f2pstarassist.common.StarTier;
import com.janboerman.f2pstarassist.common.StarUpdate;
import com.janboerman.f2pstarassist.common.User;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StarDatabase {

    private final Cache<GroupKey, StarCache> groupCaches = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)   //expire after access (not just writes), because very often it is the StarCache that gets written to, not the groupCaches.
            .build();
    private final Map<StarKey, Set<GroupKey>> owningGroups = new HashMap<>();
    private final StarListener starListener;

    private final Cache<StarKey, Instant> deletedStars = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.HOURS)
            .build();

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

            owningGroups.remove(starKey);
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

        deletedStars.invalidate(starKey);

        if (owningGroups == null) {
            //star didn't have an owner yet.
            this.owningGroups.put(starKey, new LinkedHashSet<>(groupKeys)); //defensive copy
            for (GroupKey groupKey : groupKeys) {
                //add star
                getStarCache(groupKey).add(crashedStar);
                //notify listener
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

        deletedStars.invalidate(starKey);

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
            add(new HashSet<>() {{ add(groupKey); }}, existingStar);
            //if a different group already found the star, then add simply returns null without updating the cache.
            //yet, we still want to return the 'new' star.
            return existingStar;
        }
    }

    public synchronized Map<GroupKey, CrashedStar> update(Set<GroupKey> groups, StarUpdate starUpdate) {
        assert groups != null;

        Map<GroupKey, CrashedStar> result = new HashMap<>();
        for (GroupKey group : groups) {
            result.put(group, update(group, starUpdate));
        }
        return result;
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

    //returns true iff the star was removed from the group's starcache
    public synchronized boolean remove(GroupKey groupKey, StarKey starKey) {
        assert groupKey != null;
        assert starKey != null;

        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return false;
        boolean removed = starCache.remove(starKey) != null;
        if (removed) {
            //remove from owning groups (and also remove the owning group itself if it no longer owns any stars)
            Set<GroupKey> groups = owningGroups.get(starKey);
            if (groups != null) {
                groups.remove(groupKey);
                if (groups.isEmpty()) {
                    owningGroups.remove(starKey);
                }
            }

            //add to deleted stars
            deletedStars.put(starKey, Instant.now());
        }
        return removed;
        //don't notify starListener here because we're already using the RemovalListener.
    }

    public synchronized void remove(Set<GroupKey> groups, StarKey starKey) {
        assert groups != null;

        for (GroupKey groupKey : groups) {
            remove(groupKey, starKey);
        }
    }

    public synchronized Set<CrashedStar> getStars(GroupKey groupKey) {
        assert groupKey != null;

        StarCache starCache = groupCaches.getIfPresent(groupKey);
        if (starCache == null) return Collections.emptySet();
        return starCache.getStars();
    }

    public synchronized Set<CrashedStar> getStars(Set<GroupKey> groups) {
        assert groups != null;

        Set<CrashedStar> result = new HashSet<>();
        for (GroupKey groupKey : groups) {
            result.addAll(getStars(groupKey));
        }
        return result;
    }

    public synchronized StarList calculateDiff(Set<GroupKey> forGroups, Set<CrashedStar> clientKnownStars) {
        Map<Set<CrashedStar>, Set<GroupKey>> freshStars = new HashMap<>();
        Set<StarUpdate> updates = new HashSet<>();
        Set<StarKey> deleted = new HashSet<>();

        Map<StarKey, StarTier> clientStarTiers = clientKnownStars.stream().collect(Collectors.toMap(CrashedStar::getKey, CrashedStar::getTier, BinaryOperator.minBy(Comparator.naturalOrder())));
        Map<GroupKey, Set<CrashedStar>> starsForGroups = forGroups.stream().collect(Collectors.toMap(Function.identity(), this::getStars));

        //get calculate fresh stars per group set
        for (Map.Entry<GroupKey, Set<CrashedStar>> entry : starsForGroups.entrySet()) {
            GroupKey group = entry.getKey();
            Set<CrashedStar> starSet = entry.getValue();

            Map<StarKey, CrashedStar> fresh = starSet.stream().collect(Collectors.toMap(CrashedStar::getKey, Function.identity())); clientKnownStars.forEach(star -> fresh.remove(star.getKey()));
            if (!fresh.isEmpty()) {
                freshStars.computeIfAbsent(new LinkedHashSet<>(fresh.values()), k -> new HashSet<>()).add(group);
            }
        }

        //calculate updates
        Set<CrashedStar> serverStars = getStars(forGroups);
        for (CrashedStar serverStar : serverStars) {
            StarKey key = serverStar.getKey();
            if (clientStarTiers.keySet().contains(key)) {
                if (serverStar.getTier().compareTo(clientStarTiers.get(key)) < 0) {
                    updates.add(new StarUpdate(key, serverStar.getTier()));
                }
            }
        }

        //calculate deletes
        for (CrashedStar clientStar: clientKnownStars) {
            StarKey clientStarKey = clientStar.getKey();
            if (deletedStars.getIfPresent(clientStarKey) != null) {
                deleted.add(clientStarKey);
            }
        }

        return new StarList(freshStars, updates, deleted);
    }

    @Override
    public synchronized String toString() {
        return "StarDatabase{groupCaches = " + new java.util.LinkedHashMap<>(groupCaches.asMap()) + ", owningGroups = " + owningGroups + "}";
    }

}
