package com.janboerman.f2pstarassist.plugin.web;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.janboerman.f2pstarassist.plugin.model.CrashedStar;

public final class StarTypes {

    public static final Type STAR_LIST = new StarListType();

    private StarTypes() {
    }

    private static final class StarListType implements ParameterizedType {

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { CrashedStar.class };
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return List.class;
        }
    }
}
