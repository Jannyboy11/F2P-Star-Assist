package com.janboerman.starhunt;

public enum StarSize {

    _1(StarId.TIER_1),
    _2(StarId.TIER_2),
    _3(StarId.TIER_3),
    _4(StarId.TIER_4),
    _5(StarId.TIER_5),
    _6(StarId.TIER_6),
    _7(StarId.TIER_7),
    _8(StarId.TIER_8),
    _9(StarId.TIER_9);

    private final int gameObjectId;

    private StarSize(int gameObjectId) {
        this.gameObjectId = gameObjectId;
    }

    public int getObjectId() {
        return gameObjectId;
    }

    public int getRequiredMiningLevel() {
        return (ordinal() + 1) * 10;
    }

    public static StarSize byObjectId(int gameObjectId) {
        switch (gameObjectId) {
            case StarId.TIER_1: return _1;
            case StarId.TIER_2: return _2;
            case StarId.TIER_3: return _3;
            case StarId.TIER_4: return _4;
            case StarId.TIER_5: return _5;
            case StarId.TIER_6: return _6;
            case StarId.TIER_7: return _7;
            case StarId.TIER_8: return _8;
            case StarId.TIER_9: return _9;
            default: return null;
        }
    }

    public String toString() {
        return "size-" + (ordinal() + 1);
    }

}
