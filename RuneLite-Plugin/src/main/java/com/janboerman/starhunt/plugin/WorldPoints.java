package com.janboerman.starhunt.plugin;

import com.janboerman.starhunt.common.StarLocation;

import net.runelite.api.coords.WorldPoint;

public class WorldPoints {

    static final WorldPoint VARROCK_AUBURY = new WorldPoint(3258, 3408, 0);
    static final WorldPoint DUELLING_ARENA = new WorldPoint(3341, 3267, 0);
    static final WorldPoint MINING_GUILD = new WorldPoint(3030, 3348, 0);
    static final WorldPoint CORSAIR_COVE_BANK = new WorldPoint(2567, 2858, 0);
    static final WorldPoint VARROCK_SOUTH_WEST_MINE = new WorldPoint(3175, 3362, 0);
    //TODO more

    public static WorldPoint fromLocation(StarLocation location) {
        switch (location) {
            case VARROCK_AUBURY: return VARROCK_AUBURY;
            case DUELLING_ARENA: return DUELLING_ARENA;
            case MINING_GUILD: return MINING_GUILD;
            case CORSAIR_COVE_BANK: return CORSAIR_COVE_BANK;
            case VARROCK_SOUTH_WEST_MINE: return VARROCK_SOUTH_WEST_MINE;
            default: return null;
        }
    }

}

