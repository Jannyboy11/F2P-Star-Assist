package com.janboerman.starhunt;

import net.runelite.api.ObjectID;

public final class StarId {

    private StarId() {
    }

    //not sure:
    public static final int TIER_9 = ObjectID.CRASHED_STAR;        //41020
    public static final int TIER_8 = ObjectID.CRASHED_STAR_41021;  //41021
    //pretty sure:
    public static final int TIER_7 = ObjectID.CRASHED_STAR_41223;  //41223
    public static final int TIER_6 = ObjectID.CRASHED_STAR_41224;  //41224
    public static final int TIER_5 = ObjectID.CRASHED_STAR_41225;  //41225
    public static final int TIER_4 = ObjectID.CRASHED_STAR_41226;  //41226
    //confirmed:
    public static final int TIER_3 = ObjectID.CRASHED_STAR_41227;  //41227
    public static final int TIER_2 = ObjectID.CRASHED_STAR_41228;  //41228
    public static final int TIER_1 = ObjectID.CRASHED_STAR_41229;  //41229

    //rubble
    static final int RUBBLE = 29733; //used in north-west, north-east, south-east corners

    public static boolean isCrashedStar(int gameObjectId) {
        switch (gameObjectId) {
            case TIER_9:
            case TIER_8:
            case TIER_7:
            case TIER_6:
            case TIER_5:
            case TIER_4:
            case TIER_3:
            case TIER_2:
            case TIER_1:
                return true;
            default:
                return false;
        }
    }

}
