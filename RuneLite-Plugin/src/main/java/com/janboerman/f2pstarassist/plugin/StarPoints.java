package com.janboerman.f2pstarassist.plugin;

import com.janboerman.f2pstarassist.plugin.model.StarLocation;

import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StarPoints {

    static final WorldPoint WILDERNESS_RUNITE_MINE = new WorldPoint(3057, 3887, 0);
    static final WorldPoint WILDERNESS_CENTRE_MINE = new WorldPoint(3093, 3756, 0);
    static final WorldPoint WILDERNESS_SOUTH_WEST_MINE = new WorldPoint(3018, 3593, 0);
    static final WorldPoint WILDERNESS_SOUTH_MINE = new WorldPoint(3108, 3569, 0);

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
    //static final WorldPoint DUEL_ARENA = new WorldPoint(3341, 3267, 0); //R.I.P. Duel Arena 2022-07-13
    static final WorldPoint PVP_ARENA = new WorldPoint(3351, 3281, 0);

    static final WorldPoint CRANDOR_NORTH_MINE = new WorldPoint(2835, 3296, 0);
    static final WorldPoint CRANDOR_SOUTH_MINE = new WorldPoint(2822, 3238, 0);
    static final WorldPoint CORSAIR_COVE_BANK = new WorldPoint(2567, 2858, 0);
    static final WorldPoint CORSAIR_COVE_RESOURCE_AREA = new WorldPoint(2483, 2886, 0);

    public static WorldPoint fromLocation(@Nonnull StarLocation location) {
        switch (location) {
            case WILDERNESS_RUNITE_MINE: return WILDERNESS_RUNITE_MINE;
            case WILDERNESS_CENTRE_MINE: return WILDERNESS_CENTRE_MINE;
            case WILDERNESS_SOUTH_WEST_MINE: return WILDERNESS_SOUTH_WEST_MINE;
            case WILDERNESS_SOUTH_MINE: return WILDERNESS_SOUTH_MINE;

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
            case PVP_ARENA: return PVP_ARENA;

            case CRANDOR_NORTH_MINE: return CRANDOR_NORTH_MINE;
            case CRANDOR_SOUTH_MINE: return CRANDOR_SOUTH_MINE;
            case CORSAIR_COVE_BANK: return CORSAIR_COVE_BANK;
            case CORSAIR_COVE_RESOURCE_AREA: return CORSAIR_COVE_RESOURCE_AREA;

            default: throw new RuntimeException("A new StarLocation was added which has no registered WorldPoint yet: " + location);
        }
    }

    @Nullable
    public static StarLocation toLocation(WorldPoint starPoint) {
        if (starPoint == WILDERNESS_RUNITE_MINE) return StarLocation.WILDERNESS_RUNITE_MINE;
        if (starPoint == WILDERNESS_CENTRE_MINE) return StarLocation.WILDERNESS_CENTRE_MINE;
        if (starPoint == WILDERNESS_SOUTH_WEST_MINE) return StarLocation.WILDERNESS_SOUTH_WEST_MINE;
        if (starPoint == WILDERNESS_SOUTH_MINE) return StarLocation.WILDERNESS_SOUTH_MINE;

        if (starPoint == DWARVEN_MINE) return StarLocation.DWARVEN_MINE;
        if (starPoint == MINING_GUILD) return StarLocation.MINING_GUILD;
        if (starPoint == CRAFTING_GUILD) return StarLocation.CRAFTING_GUILD;
        if (starPoint == RIMMINGTON_MINE) return StarLocation.RIMMINGTON_MINE;

        if (starPoint == DRAYNOR_VILLAGE_BANK) return StarLocation.DRAYNOR_VILLAGE_BANK;
        if (starPoint == LUMBRIDGE_SWAMP_SOUTH_WEST_MINE) return StarLocation.LUMBRIDGE_SWAMP_SOUTH_WEST_MINE;
        if (starPoint == LUMBRIDGE_SWAMP_SOUTH_EAST_MINE) return StarLocation.LUMBRIDGE_SWAMP_SOUTH_EAST_MINE;

        if (starPoint == VARROCK_SOUTH_WEST_MINE) return StarLocation.VARROCK_SOUTH_WEST_MINE;
        if (starPoint == VARROCK_SOUTH_EAST_MINE) return StarLocation.VARROCK_SOUTH_EAST_MINE;
        if (starPoint == VARROCK_AUBURY) return StarLocation.VARROCK_AUBURY;

        if (starPoint == AL_KHARID_MINE) return StarLocation.AL_KHARID_MINE;
        if (starPoint == AL_KHARID_BANK) return StarLocation.AL_KHARID_BANK;
        if (starPoint == PVP_ARENA) return StarLocation.PVP_ARENA;

        if (starPoint == CRANDOR_NORTH_MINE) return StarLocation.CRANDOR_NORTH_MINE;
        if (starPoint == CRANDOR_SOUTH_MINE) return StarLocation.CRANDOR_SOUTH_MINE;
        if (starPoint == CORSAIR_COVE_BANK) return StarLocation.CORSAIR_COVE_BANK;
        if (starPoint == CORSAIR_COVE_RESOURCE_AREA) return StarLocation.CORSAIR_COVE_RESOURCE_AREA;

        throw new RuntimeException("A new WorldPoint was added which has no registered StarLocation yet: " + starPoint);
    }

}

