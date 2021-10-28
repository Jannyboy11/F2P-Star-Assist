package com.janboerman.starhunt.plugin;

import com.janboerman.starhunt.common.StarLocation;

import net.runelite.api.coords.WorldPoint;

public class WorldPoints {

    static final WorldPoint WILDY_RUNITE_ROCKS = new WorldPoint(3057, 3887, 0);
    static final WorldPoint WILDY_CENTRE_MINE = new WorldPoint(3093, 3756, 0);
    static final WorldPoint WILDY_SOUTH_WEST_MINE = new WorldPoint(3018, 3593, 0);
    static final WorldPoint WILDY_SOUTH_MINE = new WorldPoint(3108, 3569, 0);

    static final WorldPoint DWARVEN_MINE = new WorldPoint(3018, 3443, 0);
    static final WorldPoint MINING_GUILD = new WorldPoint(3030, 3348, 0);
    static final WorldPoint CRAFTING_GUILD = new WorldPoint(2940, 3280, 0);
    static final WorldPoint RIMMINGTON_MINE = new WorldPoint(2974, 3241, 0);

    static final WorldPoint DRAYNOR_VILLAGE_BANK = new WorldPoint(3094, 3235, 0);
    static final WorldPoint LUMBRIDGE_SWAMP_SOUTH_WEST_MINE = new WorldPoint(3153, 3150, 0);
    static final WorldPoint LUMBRIDGE_SWAMP_SOUTH_EAST_MINE = new WorldPoint(3230, 3155, 0);

    static final WorldPoint VARROCK_SOUTH_WEST_MINE = new WorldPoint(3175, 3362, 0);
    static final WorldPoint VARROCK_SOUTH_EAST_MINE = new WorldPoint(3290, 3353, 0);
    static final WorldPoint VARROCK_AUBURY = new WorldPoint(3258, 3408, 0);

    static final WorldPoint AL_KHARID_MINE = new WorldPoint(3296, 3298, 0);
    static final WorldPoint AL_KHARID_BANK = new WorldPoint(3276, 3164, 0);
    static final WorldPoint DUELLING_ARENA = new WorldPoint(3341, 3267, 0);

    static final WorldPoint CRANDOR_NORTH_MINE = new WorldPoint(2835, 3296, 0);
    static final WorldPoint CRANDOR_SOUTH_MINE = new WorldPoint(2822, 3238, 0);
    static final WorldPoint CORSAIR_COVE_BANK = new WorldPoint(2567, 2858, 0);
    static final WorldPoint CORSAIR_COVE_RESOURCE_AREA = new WorldPoint(2483, 2886, 0);

    public static WorldPoint fromLocation(StarLocation location) {
        switch (location) {
            case WILDY_RUNITE_ROCKS: return WILDY_RUNITE_ROCKS;
            case WILDY_CENTRE_MINE: return WILDY_CENTRE_MINE;
            case WILDY_SOUTH_WEST_MINE: return WILDY_SOUTH_WEST_MINE;
            case WILDY_SOUTH_MINE: return WILDY_SOUTH_MINE;

            case DWARVEN_MINE: return DWARVEN_MINE;
            case MINING_GUILD: return MINING_GUILD;
            case CRAFTING_GUILD: return CRAFTING_GUILD;
            case RIMMINGTON_MINE: return RIMMINGTON_MINE;

            case DRAYNOR_VILLAGE_BANK: return DRAYNOR_VILLAGE_BANK;
            case LUMBRIDGE_SWAMP_SOUTH_WEST_MINE: return LUMBRIDGE_SWAMP_SOUTH_WEST_MINE;
            case LUMBRIDGE_SWAMP_SOUTH_EAST_MINE: return LUMBRIDGE_SWAMP_SOUTH_EAST_MINE;

            case VARROCK_SOUTH_WEST_MINE: return VARROCK_SOUTH_WEST_MINE;
            case VARROCK_SOUTH_EAST_MINE: return VARROCK_SOUTH_EAST_MINE;
            case VARROCK_AUBURY: return VARROCK_AUBURY;

            case AL_KHARID_MINE: return AL_KHARID_MINE;
            case AL_KHARID_BANK: return AL_KHARID_BANK;
            case DUELLING_ARENA: return DUELLING_ARENA;

            case CRANDOR_NORTH_MINE: return CRANDOR_NORTH_MINE;
            case CRANDOR_SOUTH_MINE: return CRANDOR_SOUTH_MINE;
            case CORSAIR_COVE_BANK: return CORSAIR_COVE_BANK;
            case CORSAIR_COVE_RESOURCE_AREA: return CORSAIR_COVE_RESOURCE_AREA;

            default: throw new RuntimeException("A new StarLocation was added which has no registered WorldPoint yet: " + location);
        }
    }

}

