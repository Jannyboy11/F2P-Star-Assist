package com.janboerman.starhunt.common;

public enum StarTier {

    SIZE_1,
    SIZE_2,
    SIZE_3,
    SIZE_4,
    SIZE_5,
    SIZE_6,
    SIZE_7,
    SIZE_8,
    SIZE_9;

    public int getTier() {
        return ordinal() + 1;
    }

    @Override
    public String toString() {
        return "size-" + getTier();
    }

    public int getRequiredMiningLevel() {
        return getTier() * 10;
    }

}
