package com.janboerman.f2pstarassist.common.web;

import java.io.IOException;
import java.time.Instant;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.janboerman.f2pstarassist.common.CrashedStar;
import com.janboerman.f2pstarassist.common.RunescapeUser;
import com.janboerman.f2pstarassist.common.StarLocation;
import com.janboerman.f2pstarassist.common.StarTier;
import com.janboerman.f2pstarassist.common.User;

public class CrashedStarAdapater extends TypeAdapter<CrashedStar> {

    // Gson type adapter for StarDto from back-end webserver.

    @Override
    public void write(JsonWriter jsonWriter, CrashedStar crashedStar) throws IOException {
        jsonWriter.beginObject();
        if (crashedStar.hasId()) {
            jsonWriter.name("id").value(crashedStar.getId());
        }
        jsonWriter.name("world").value(crashedStar.getWorld());
        jsonWriter.name("location").value(crashedStar.getLocation().name());
        jsonWriter.name("tier").value(crashedStar.getTier().getSize());
        User discoveredBy = crashedStar.getDiscoveredBy();
        if (!(discoveredBy instanceof User.Unknown)) {
            jsonWriter.name("discoveredBy").value(discoveredBy.toString());
        }
        jsonWriter.name("detectedAt").value(crashedStar.getDetectedAt().toString());
        jsonWriter.endObject();
    }

    @Override
    public CrashedStar read(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();

        Long id = null;
        int world = -1;
        StarLocation location = null;
        StarTier tier = null;
        User discoveredBy = User.unknown();
        Instant detectedAt = null;

        while (jsonReader.hasNext()) {
            String field = jsonReader.nextName();
            switch (field) {
                case "id": id = jsonReader.nextLong();
                case "world": world = jsonReader.nextInt();
                case "location": location = StarLocation.valueOf(jsonReader.nextString());
                case "tier": tier = StarTier.bySize(jsonReader.nextInt());
                case "discoveredBy": discoveredBy = new RunescapeUser(jsonReader.nextString()); // assume only RSNs for now.
                case "detectedAt": detectedAt = Instant.parse(jsonReader.nextString());
            }
        }

        jsonReader.endObject();

        CrashedStar star = new CrashedStar(tier, location, world, detectedAt, discoveredBy);
        if (id != null) {
            star.setId(id.longValue());
        }
        return star;
    }
}
