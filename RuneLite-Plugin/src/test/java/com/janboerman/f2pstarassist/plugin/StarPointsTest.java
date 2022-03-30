package com.janboerman.f2pstarassist.plugin;

import com.janboerman.f2pstarassist.common.StarLocation;
import static com.janboerman.f2pstarassist.plugin.StarPoints.toLocation;
import static com.janboerman.f2pstarassist.plugin.StarPoints.fromLocation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class StarPointsTest {

    @Test
    public void testWorldPointRoundTrip() {
        for (StarLocation starLocation : StarLocation.values()) {
            assertEquals(starLocation, toLocation(fromLocation(starLocation)));
        }
    }

}
