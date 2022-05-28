package com.janboerman.f2pstarassist.web;

import com.janboerman.f2pstarassist.common.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

public class StarDatabaseUpdateTest {

    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        return new java.util.LinkedHashSet<T>(Arrays.asList(items));
    }

    @Test
    public void testSingleUpdate() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey = new StarKey(StarLocation.RIMMINGTON_MINE, 576);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("Jannyboy11");
        final CrashedStar crashedStar = new CrashedStar(starKey, StarTier.SIZE_3, detectedAt, detectedBy);
        final GroupKey group = new GroupKey("group");

        starDatabase.add(Set.of(group), crashedStar);
        final CrashedStar result = starDatabase.update(group, new StarUpdate(starKey, StarTier.SIZE_2));
        final CrashedStar expected = new CrashedStar(starKey, StarTier.SIZE_2, detectedAt, detectedBy);
        assertEquals(expected, result);

        final Set<CrashedStar> resultStarList = starDatabase.getStars(group);
        final Set<CrashedStar> expectedStarList = Set.of(new CrashedStar(starKey, StarTier.SIZE_2, detectedAt, detectedBy));
        assertEquals(expectedStarList, resultStarList);
    }

    @Test
    public void testMultiUpdate() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey = new StarKey(StarLocation.CRAFTING_GUILD, 575);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("Jannyboy11");
        final CrashedStar crashedStar = new CrashedStar(starKey, StarTier.SIZE_3, detectedAt, detectedBy);
        final Set<GroupKey> groups = new HashSet<>(Arrays.asList(new GroupKey("a"), new GroupKey("b"), new GroupKey("c")));

        starDatabase.add(groups, crashedStar);
        final Map<GroupKey, CrashedStar> resultStars = starDatabase.update(groups, new StarUpdate(starKey, StarTier.SIZE_2));
        assertEquals(groups.size(), resultStars.size());
        final CrashedStar resultStar = resultStars.values().stream().reduce(BinaryOperator.minBy(Comparator.comparing(CrashedStar::getDetectedAt))).get();
        final CrashedStar expectedStar = new CrashedStar(starKey, StarTier.SIZE_2, detectedAt, detectedBy);
        assertEquals(expectedStar, resultStar);

        final Set<CrashedStar> resultStarList = starDatabase.getStars(groups);
        final Set<CrashedStar> expectedStarList = Set.of(new CrashedStar(starKey, StarTier.SIZE_2, detectedAt, detectedBy));
        assertEquals(expectedStarList, resultStarList);
    }

    @Test
    public void testSingleDelete() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey = new StarKey(StarLocation.RIMMINGTON_MINE, 576);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("Jannyboy11");
        final CrashedStar crashedStar = new CrashedStar(starKey, StarTier.SIZE_3, detectedAt, detectedBy);
        final GroupKey group = new GroupKey("group");

        starDatabase.add(setOf(group), crashedStar);
        boolean result = starDatabase.remove(group, starKey);
        assertTrue(result);
        assertTrue(starDatabase.getStars(group).isEmpty());
    }

    @Test
    public void testMultiDelete() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey = new StarKey(StarLocation.RIMMINGTON_MINE, 576);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("Jannyboy11");
        final CrashedStar crashedStar = new CrashedStar(starKey, StarTier.SIZE_3, detectedAt, detectedBy);
        final Set<GroupKey> groups = new HashSet<>(Arrays.asList(new GroupKey("a"), new GroupKey("b")));

        starDatabase.add(groups, crashedStar);
        starDatabase.remove(groups, starKey);
        assertTrue(starDatabase.getStars(groups).isEmpty());
    }

    @Test
    public void testDiff() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey = new StarKey(StarLocation.WILDERNESS_RUNITE_MINE, 301);
        final Instant detectedAt = Instant.now();
        final User detectedBy = new RunescapeUser("Jannyboy11");
        final CrashedStar crashedStar = new CrashedStar(starKey, StarTier.SIZE_9, detectedAt, detectedBy);
        final GroupKey a = new GroupKey("a"), b = new GroupKey("b"), c = new GroupKey("c");

        final Set<GroupKey> groups = setOf(a, b, c);
        final Set<CrashedStar> clientKnownStars = setOf(crashedStar);

        starDatabase.add(groups, crashedStar);
        final StarList result = starDatabase.calculateDiff(groups, clientKnownStars);

        assertTrue(result.getFreshStars().isEmpty());
        assertTrue(result.getStarUpdates().isEmpty());
        assertTrue(result.getDeletedStars().isEmpty());
    }

    //TODO more tests for calculateDiff?

}
