package com.janboerman.f2pstarassist.web.options;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

public class PortConverter implements ValueConverter<Integer> {

    @Override
    public Integer convert(String input) {
        try {
            int port = Integer.parseInt(input);
            if (0 <= port && port <= 65535) {
                return port;
            } else {
                throw new ValueConversionException("expected a port number ([0-65535]), got: " + input);
            }
        } catch (NumberFormatException e) {
            throw new ValueConversionException("expected a port number ([0-65535]), got: " + input, e);
        }
    }

    @Override
    public String revert(Object value) {
        if (value instanceof Integer port && 0 <= port && port <= 65535) {
            return port.toString();
        } else {
            throw new IllegalArgumentException("expected a port number ([0-65535]), got: " + value);
        }
    }

    @Override
    public Class<? extends Integer> valueType() {
        return Integer.class;
    }

    @Override
    public String valuePattern() {
        return "[0-65535]";
    }

}
