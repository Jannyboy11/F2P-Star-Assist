package com.janboerman.starhunt.plugin;

import com.janboerman.starhunt.common.StarLocation;

import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;

public class StarPoints {

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

    private static final Map<WorldPoint, StarLocation> LOCATIONS = new HashMap<>();
    static {
        LOCATIONS.put(WILDY_RUNITE_ROCKS, StarLocation.WILDY_RUNITE_ROCKS);
        LOCATIONS.put(WILDY_CENTRE_MINE, StarLocation.WILDY_CENTRE_MINE);
        LOCATIONS.put(WILDY_SOUTH_WEST_MINE, StarLocation.WILDY_SOUTH_WEST_MINE);
        LOCATIONS.put(WILDY_SOUTH_MINE, StarLocation.WILDY_SOUTH_MINE);

        LOCATIONS.put(DWARVEN_MINE, StarLocation.DWARVEN_MINE);
        LOCATIONS.put(MINING_GUILD, StarLocation.MINING_GUILD);
        LOCATIONS.put(CRAFTING_GUILD, StarLocation.CRAFTING_GUILD);
        LOCATIONS.put(RIMMINGTON_MINE, StarLocation.RIMMINGTON_MINE);

        LOCATIONS.put(DRAYNOR_VILLAGE_BANK, StarLocation.DRAYNOR_VILLAGE_BANK);
        LOCATIONS.put(LUMBRIDGE_SWAMP_SOUTH_WEST_MINE, StarLocation.LUMBRIDGE_SWAMP_SOUTH_WEST_MINE);
        LOCATIONS.put(LUMBRIDGE_SWAMP_SOUTH_EAST_MINE, StarLocation.LUMBRIDGE_SWAMP_SOUTH_EAST_MINE);

        LOCATIONS.put(VARROCK_SOUTH_WEST_MINE, StarLocation.VARROCK_SOUTH_WEST_MINE);
        LOCATIONS.put(VARROCK_SOUTH_EAST_MINE, StarLocation.VARROCK_SOUTH_EAST_MINE);
        LOCATIONS.put(VARROCK_AUBURY, StarLocation.VARROCK_AUBURY);

        LOCATIONS.put(AL_KHARID_MINE, StarLocation.AL_KHARID_MINE);
        LOCATIONS.put(AL_KHARID_BANK, StarLocation.AL_KHARID_BANK);
        LOCATIONS.put(DUELLING_ARENA, StarLocation.DUELLING_ARENA);

        LOCATIONS.put(CRANDOR_NORTH_MINE, StarLocation.CRANDOR_NORTH_MINE);
        LOCATIONS.put(CRANDOR_SOUTH_MINE, StarLocation.CRANDOR_SOUTH_MINE);
        LOCATIONS.put(CORSAIR_COVE_BANK, StarLocation.CORSAIR_COVE_BANK);
        LOCATIONS.put(CORSAIR_COVE_RESOURCE_AREA, StarLocation.CORSAIR_COVE_RESOURCE_AREA);
    }

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

    public static StarLocation toLocation(WorldPoint starPoint) {
        return LOCATIONS.get(starPoint);
    }

}

