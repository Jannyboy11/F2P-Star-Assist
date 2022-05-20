package com.janboerman.f2pstarassist.web.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

public class BooleanConverter implements ValueConverter<Boolean> {
    @Override
    public Boolean convert(String input) {
        return Boolean.parseBoolean(input);
    }

    @Override
    public String revert(Object value) {
        if (value instanceof Boolean b) {
            return b.toString();
        } else {
            throw new ValueConversionException("Expected Boolean, got " + value);
        }
    }

    @Override
    public Class<? extends Boolean> valueType() {
        return Boolean.class;
    }

    @Override
    public String valuePattern() {
        return "{false, true}";
    }
}
