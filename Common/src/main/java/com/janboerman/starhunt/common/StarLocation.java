package com.janboerman.starhunt.common;

public enum StarLocation {

    WILDERNESS_RUNITE_MINE,
    WILDERNESS_CENTRE_MINE,
    WILDERNESS_SOUTH_WEST_MINE,
    WILDERNESS_SOUTH_MINE,

    DWARVEN_MINE,
    MINING_GUILD,
    CRAFTING_GUILD,
    RIMMINGTON_MINE,

    DRAYNOR_VILLAGE_BANK,
    LUMBRIDGE_SWAMP_SOUTH_WEST_MINE,
    LUMBRIDGE_SWAMP_SOUTH_EAST_MINE,

    VARROCK_SOUTH_WEST_MINE,
    VARROCK_SOUTH_EAST_MINE,
    VARROCK_AUBURY,

    AL_KHARID_MINE,
    AL_KHARID_BANK,
    DUEL_ARENA,

    CRANDOR_NORTH_MINE,
    CRANDOR_SOUTH_MINE,
    CORSAIR_COVE_BANK,
    CORSAIR_COVE_RESOURCE_AREA;

    public boolean isInWilderness() {
        switch (this) {
            case WILDERNESS_RUNITE_MINE:
            case WILDERNESS_CENTRE_MINE:
            case WILDERNESS_SOUTH_WEST_MINE:
            case WILDERNESS_SOUTH_MINE:
                return true;
            default:
                return false;
        }
    }

}
