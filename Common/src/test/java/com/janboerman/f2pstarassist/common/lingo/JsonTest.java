package com.janboerman.f2pstarassist.common.lingo;

import com.janboerman.f2pstarassist.common.*;
import com.janboerman.f2pstarassist.common.util.CollectionConvert;
import static com.janboerman.f2pstarassist.common.web.StarJson.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class JsonTest {

    private static final String[] GROUPS = {"ONE", "TWO", "THREE"};
    private static final StarLocation[] LOCATIONS = StarLocation.values();
    private static final StarTier[] TIERS = StarTier.values();
    private static final String[] USER_NAMES = {"Jannyboy11", "John Doe", "Alice", "Bob"};

    private final Random random = new Random();

    private Set<GroupKey> makeGroups() {
        int size = random.nextInt(3);

        Set<GroupKey> result = new HashSet<>();
        for (int i = 0; i < size; i++) {
            result.add(new GroupKey(GROUPS[i]));
        }
        return result;
    }

    private StarLocation makeStarLocation() {
        return LOCATIONS[random.nextInt(LOCATIONS.length)];
    }

    private int makeWorld() {
        int diff = 576 - 301;
        return random.nextInt(diff) + 301;
    }

    private StarTier makeTier() {
        return TIERS[random.nextInt(TIERS.length)];
    }

    private Instant makeDetectedAt() {
        return Instant.now().plus(random.nextInt(7200), ChronoUnit.SECONDS);
    }

    private User makeDetectedBy() {
        switch (random.nextInt(3)) {
            case 0:
                return User.unknown();
            case 1:
                return new RunescapeUser(USER_NAMES[random.nextInt(USER_NAMES.length)]);
            case 2:
                return new DiscordUser(USER_NAMES[random.nextInt(USER_NAMES.length)]);
        }
        throw new RuntimeException("Non-exhaustive switch!");
    }

    private StarKey makeStarKey() {
        return new StarKey(makeStarLocation(), makeWorld());
    }

    private StarUpdate makeStarUpdate() {
        return new StarUpdate(makeStarKey(), makeTier());
    }

    private CrashedStar makeCrashedStar() {
        return new CrashedStar(makeTier(), makeStarLocation(), makeWorld(), makeDetectedAt(), makeDetectedBy());
    }

    private StarPacket makeStarPacket() {
        Payload payload;
        switch (random.nextInt(3)) {
            case 0: payload = makeCrashedStar(); break;
            case 1: payload = makeStarKey(); break;
            case 2: payload = makeStarUpdate(); break;
            default: payload = null;
        }
        return new StarPacket(makeGroups(), payload);
    }

    @Test
    public void testStarPacket() {
        final StarPacket starPacket = makeStarPacket();

        assertEquals(starPacket, starPacket(starPacketJson(starPacket)));
    }

    @Test
    public void testCrashedStar() {
        final CrashedStar crashedStar = makeCrashedStar();

        assertEquals(crashedStar, crashedStar(crashedStarJson(crashedStar)));
    }

    @Test
    public void testCrashedStars() {
        final Set<CrashedStar> stars = CollectionConvert.toSet(Arrays.asList(makeCrashedStar(), makeCrashedStar(), makeCrashedStar()));
        final Set<CrashedStar> noStars = Collections.emptySet();

        assertEquals(stars, crashedStars(crashedStarsJson(stars)));
        assertEquals(noStars, crashedStars(crashedStarsJson(noStars)));
    }

    @Test
    public void testStarUpdate() {
        final StarUpdate starUpdate = makeStarUpdate();

        assertEquals(starUpdate, starUpdate(starUpdateJson(starUpdate)));;
    }

    @Test
    public void testStarKey() {
        final StarKey starKey = makeStarKey();

        assertEquals(starKey, starKey(starKeyJson(starKey)));
    }

    @Test
    public void testGroupKeys() {
        final Set<GroupKey> groups = makeGroups();

        assertEquals(groups, groupKeys(groupKeysJson(groups)));
    }

}
