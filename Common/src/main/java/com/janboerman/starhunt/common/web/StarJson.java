package com.janboerman.starhunt.common.web;

import com.google.gson.JsonPrimitive;
import com.janboerman.starhunt.common.CrashedStar;
import com.janboerman.starhunt.common.StarLocation;
import com.janboerman.starhunt.common.StarTier;

import com.google.gson.JsonObject;

import java.time.Instant;

public class StarJson {

    private StarJson() {
    }

    public static CrashedStar crashedStar(JsonObject crashedStar) {
        StarTier tier = StarTier.bySize(crashedStar.get("tier").getAsInt());
        StarLocation location = StarLocation.valueOf(crashedStar.get("location").getAsString());
        int world = crashedStar.get("world").getAsInt();
        Instant detectedAt = Instant.ofEpochMilli(crashedStar.get("detected at").getAsLong());
        String discoveredBy = crashedStar.get("discovered by").getAsString();

        return new CrashedStar(tier, location, world, detectedAt, discoveredBy);
    }

    public static JsonObject crashedStarJson(CrashedStar crashedStar) {
        JsonObject result = new JsonObject();
        result.add("tier", new JsonPrimitive(crashedStar.getTier().getSize()));
        result.add("location", new JsonPrimitive(crashedStar.getLocation().name()));
        result.add("world", new JsonPrimitive(crashedStar.getWorld()));
        result.add("detected at", new JsonPrimitive(crashedStar.getDetectedAt().toEpochMilli()));
        result.add("discovered by", new JsonPrimitive(crashedStar.getDiscoveredBy()));
        return result;
    }
}
