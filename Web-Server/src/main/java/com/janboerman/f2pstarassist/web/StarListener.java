package com.janboerman.f2pstarassist.web;

import com.janboerman.f2pstarassist.common.*;

public interface StarListener {

    public void onAdd(GroupKey group, CrashedStar star);

    public void onUpdate(GroupKey group, StarUpdate update);

    public void onRemove(GroupKey group, StarKey star);

}

class DummyStarListener implements StarListener {

    static DummyStarListener INSTANCE = new DummyStarListener();

    private DummyStarListener() {}

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