package com.janboerman.f2pstarassist.web;

import com.janboerman.f2pstarassist.common.*;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Simulates a few scenarios of players calling out star updates to the star database.
 */
public class StarDatabaseTest {

    /**
     * Scenario 0:
     * Player a from group A calls a star
     * Player b from group A requests the lists of known stars
     * expected result: player b gets the star back in the response
     */
    @Test
    public void testScenario0() {
        final StarDatabase starDatabase = new StarDatabase(DummyStarListener.INSTANCE);

        final CrashedStar crashedStar = new CrashedStar(StarTier.SIZE_1, StarLocation.DUEL_ARENA, 565, Instant.now(), new RunescapeUser("a"));
        final GroupKey groupA = new GroupKey("A");

        starDatabase.add(Set.of(groupA), crashedStar);
        final Set<CrashedStar> starsKnownToGroupA = starDatabase.getStars(Set.of(groupA));

        assertTrue(starsKnownToGroupA.contains(crashedStar));
    }

    /**
     * Scenario 1:
     * Player a from group A calls a star
     * Player b from group B requests the list of known stars
     * expected result: b shouldn't get the star found by player a because player b is not in group A.
     */
    @Test
    public void testScenario1() {
        final StarDatabase starDatabase = new StarDatabase(DummyStarListener.INSTANCE);

        final CrashedStar crashedStar = new CrashedStar(StarTier.SIZE_9, StarLocation.VARROCK_AUBURY, 301, Instant.now(), new RunescapeUser("a"));

        final GroupKey groupA = new GroupKey("A");
        final GroupKey groupB = new GroupKey("B");

        starDatabase.add(Set.of(groupA), crashedStar);
        final Set<CrashedStar> starsKnownToGroupB = starDatabase.getStars(Set.of(groupB));

        assertFalse(starsKnownToGroupB.contains(crashedStar));
    }

    /**
     * Scenario 2:
     * Player a from group A calls a star
     * Player b from group B also finds this star
     * Player b2 from group B requests the list of known stars
     * expected result: b2 shouldn't get the star found by player a because player b2 is not in group A.
     */
    @Test
    public void testScenario2() {
        final StarDatabase starDatabase = new StarDatabase(DummyStarListener.INSTANCE);

        final CrashedStar crashedStar = new CrashedStar(StarTier.SIZE_9, StarLocation.VARROCK_AUBURY, 301, Instant.now(), new RunescapeUser("a"));

        final GroupKey groupA = new GroupKey("A");
        final GroupKey groupB = new GroupKey("B");

        starDatabase.add(Set.of(groupA), crashedStar);
        starDatabase.add(Set.of(groupB), crashedStar);
        final Set<CrashedStar> starsKnownToGroupB = starDatabase.getStars(Set.of(groupB));

        assertFalse(starsKnownToGroupB.contains(crashedStar));
    }

    /**
     * Scenario 3:
     * Player x is in both group A and B and calls a star
     * Player a from group A requests stars
     * Player b from group B requests stars
     * expected result: both player a and b should get the found star.
     */
    @Test
    public void testScenario3() {
        final StarDatabase starDatabase = new StarDatabase(DummyStarListener.INSTANCE);

        final CrashedStar crashedStar = new CrashedStar(StarTier.SIZE_5, StarLocation.LUMBRIDGE_SWAMP_SOUTH_EAST_MINE, 382, Instant.now(), new RunescapeUser("x"));

        final GroupKey groupA = new GroupKey("A");
        final GroupKey groupB = new GroupKey("B");

        starDatabase.add(Set.of(groupA, groupB), crashedStar);
        final Set<CrashedStar> starsKnownToGroupA = starDatabase.getStars(Set.of(groupA));
        final Set<CrashedStar> starsKnownToGroupB = starDatabase.getStars(Set.of(groupB));

        assertTrue(starsKnownToGroupA.contains(crashedStar));
        assertTrue(starsKnownToGroupB.contains(crashedStar));
    }

    /**
     * Scenario 4:
     * Player a from group A calls a star
     * Player a witnesses it degrading to a lower tier
     * Player a2 from group A requests the star
     * expected result: a2 gets the updated star back
     */
    @Test
    public void testScenario4() {
        final StarDatabase starDatabase = new StarDatabase(DummyStarListener.INSTANCE);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("a");
        final CrashedStar crashedStar = new CrashedStar(StarTier.SIZE_5, StarLocation.AL_KHARID_BANK, 553, detectedAt, detectedBy);
        final GroupKey groupA = new GroupKey("A");

        starDatabase.add(Set.of(groupA), crashedStar);
        starDatabase.update(groupA, new StarUpdate(StarTier.SIZE_4, StarLocation.AL_KHARID_BANK, 553));

        final CrashedStar updatedStar = new CrashedStar(StarTier.SIZE_4, StarLocation.AL_KHARID_BANK, 553, detectedAt, detectedBy);
        assertTrue(starDatabase.getStars(Set.of(groupA)).contains(updatedStar));
    }

}
