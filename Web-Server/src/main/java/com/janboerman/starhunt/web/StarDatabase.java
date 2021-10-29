package com.janboerman.starhunt.web;

import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarCache;
import com.janboerman.starhunt.common.StarKey;

import java.util.Set;

public class StarDatabase {

    private final StarCache starCache = new StarCache();

    public synchronized boolean add(CrashedStar crashedStar) {
        return starCache.add(crashedStar);
    }

    public synchronized CrashedStar get(StarKey starKey) {
        return starCache.get(starKey);
    }

    public synchronized void forceAdd(CrashedStar crashedStar) {
        starCache.forceAdd(crashedStar);
    }

    public synchronized boolean remove(StarKey starKey) {
        return starCache.remove(starKey);
    }

    public synchronized Set<CrashedStar> getStars() {
        return starCache.getStars();
    }

}
