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

    @Test
    public void testDiffNewUpdatedDeleted() {
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);

        final StarKey starKey1 = new StarKey(StarLocation.PVP_ARENA, 551);                      //not known by the server
        final StarKey starKey2 = new StarKey(StarLocation.DRAYNOR_VILLAGE_BANK, 510);           //maintained by the server
        final StarKey starKey3 = new StarKey(StarLocation.CORSAIR_COVE_RESOURCE_AREA, 316);     //updated on the server
        final StarKey starKey4 = new StarKey(StarLocation.DWARVEN_MINE, 576);                   //deleted from the server
        final StarKey starKey5 = new StarKey(StarLocation.MINING_GUILD, 382);                   //not known by the client

        final CrashedStar crashedStar1 = new CrashedStar(starKey1, StarTier.SIZE_3, Instant.now(), User.unknown());
        final CrashedStar crashedStar2 = new CrashedStar(starKey2, StarTier.SIZE_6, Instant.now(), User.unknown());
        final CrashedStar crashedStar3 = new CrashedStar(starKey3, StarTier.SIZE_8, Instant.now(), User.unknown());
        final CrashedStar crashedStar4 = new CrashedStar(starKey4, StarTier.SIZE_1, Instant.now(), User.unknown());
        final CrashedStar crashedStar5 = new CrashedStar(starKey5, StarTier.SIZE_9, Instant.now(), User.unknown());

        final GroupKey groupA = new GroupKey("a");
        final Set<GroupKey> groups = Set.of(groupA);

        starDatabase.add(groups, crashedStar2.clone());
        starDatabase.add(groups, crashedStar3.clone());
        starDatabase.add(groups, crashedStar4.clone());
        starDatabase.add(groups, crashedStar5.clone());

        starDatabase.update(groupA, new StarUpdate(starKey3, StarTier.SIZE_7));
        starDatabase.remove(groupA, starKey4);

        StarList starList = starDatabase.calculateDiff(groups, Set.of(crashedStar1.clone(), crashedStar2.clone(), crashedStar3.clone(), crashedStar4.clone()));

        var freshStars = starList.getFreshStars();
        assertEquals(Map.of(Set.of(crashedStar5), groups), freshStars);

        var starUpdates = starList.getStarUpdates();
        assertEquals(Set.of(new StarUpdate(starKey3, StarTier.SIZE_7)), starUpdates);

        var deletedStars = starList.getDeletedStars();
        assertEquals(Set.of(starKey4), deletedStars);
    }

}
