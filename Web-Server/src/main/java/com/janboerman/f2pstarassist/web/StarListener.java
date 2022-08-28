package com.janboerman.f2pstarassist.web;

import com.janboerman.f2pstarassist.common.*;

public interface StarListener {

    public void onAdd(GroupKey group, CrashedStar star);

    public void onUpdate(GroupKey group, StarUpdate update);

    public void onRemove(GroupKey group, StarKey star);

    public default StarListener concat(StarListener other) {
        if (this == NoOpStarListener.INSTANCE) return other;
        if (other == NoOpStarListener.INSTANCE) return this;
        return new ConcatStarListener(this, other);
    }

}

class NoOpStarListener implements StarListener {

    static NoOpStarListener INSTANCE = new NoOpStarListener();

    private NoOpStarListener() {}

    @Override
    public void onAdd(GroupKey group, CrashedStar star) {
    }

    @Override
    public void onUpdate(GroupKey group, StarUpdate update) {
    }

    @Override
    public void onRemove(GroupKey group, StarKey star) {
    }

}

class ConcatStarListener implements StarListener {

    private final StarListener one, two;

    ConcatStarListener(StarListener one, StarListener two) {
        assert one != null;
        assert two != null;

        this.one = one;
        this.two = two;
    }

    @Override
    public void onAdd(GroupKey group, CrashedStar star) {
        one.onAdd(group, star);
        two.onAdd(group, star);
    }

    @Override
    public void onUpdate(GroupKey group, StarUpdate update) {
        one.onUpdate(group, update);
        two.onUpdate(group, update);
    }

    @Override
    public void onRemove(GroupKey group, StarKey star) {
        one.onRemove(group, star);
        two.onRemove(group, star);
    }

}