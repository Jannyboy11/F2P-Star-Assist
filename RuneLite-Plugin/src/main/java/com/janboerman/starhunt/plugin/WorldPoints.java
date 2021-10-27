package com.janboerman.starhunt.plugin;

import com.janboerman.starhunt.common.StarLocation;

import net.runelite.api.coords.WorldPoint;

public class WorldPoints {

    static final WorldPoint VARROCK_AUBURY = new WorldPoint(3258, 3408, 0);
    static final WorldPoint DUELLING_ARENA = new WorldPoint(3341, 3267, 0);
    static final WorldPoint MINING_GUILD = new WorldPoint(3030, 3348, 0);

    public static WorldPoint fromLocation(StarLocation location) {
        switch (location) {
            case VARROCK_AUBURY: return VARROCK_AUBURY;
            case DUELLING_ARENA: return DUELLING_ARENA;
            case MINING_GUILD: return MINING_GUILD;
            default: return null;
        }
    }

}

