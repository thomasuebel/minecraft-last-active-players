package de.thomasuebel.lastactiveplayers.ranking;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrozenAwardsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final int SEVEN_DAYS = 7;

    // --- mvpCandidates ---

    @Test
    void mvpCandidatesIsEmptyWhenListIsEmpty() {
        final AwardSnapshot snap = new FrozenAwards(List.of(), List.of());
        assertTrue(snap.mvpCandidates().isEmpty());
    }

    @Test
    void mvpCandidatesReturnsSingleNomination() {
        final Nomination alice = nomination(ALICE, "Alice", 0);
        final AwardSnapshot snap = new FrozenAwards(List.of(alice), List.of());
        final List<Nomination> candidates = snap.mvpCandidates();
        assertEquals(1, candidates.size());
        assertEquals(ALICE, candidates.get(0).uuid());
        assertEquals("Alice", candidates.get(0).username());
    }

    @Test
    void mvpCandidatesReturnsTwoNominationsForTie() {
        final AwardSnapshot snap = new FrozenAwards(
            List.of(nomination(ALICE, "Alice", 0), nomination(BOB, "Bob", 0)),
            List.of()
        );
        assertEquals(2, snap.mvpCandidates().size());
    }

    // --- streakCandidates ---

    @Test
    void streakCandidatesIsEmptyWhenListIsEmpty() {
        final AwardSnapshot snap = new FrozenAwards(List.of(), List.of());
        assertTrue(snap.streakCandidates().isEmpty());
    }

    @Test
    void streakCandidatesReturnsSingleNomination() {
        final Nomination alice = nomination(ALICE, "Alice", SEVEN_DAYS);
        final AwardSnapshot snap = new FrozenAwards(List.of(), List.of(alice));
        final List<Nomination> candidates = snap.streakCandidates();
        assertEquals(1, candidates.size());
        assertEquals(ALICE, candidates.get(0).uuid());
        assertEquals(SEVEN_DAYS, candidates.get(0).streakDays());
    }

    @Test
    void streakCandidatesReturnsTwoNominationsForTie() {
        final AwardSnapshot snap = new FrozenAwards(
            List.of(),
            List.of(nomination(ALICE, "Alice", SEVEN_DAYS), nomination(BOB, "Bob", SEVEN_DAYS))
        );
        assertEquals(2, snap.streakCandidates().size());
    }

    // --- sameLeaders ---

    @Test
    void sameLeadersReturnsTrueForIdenticalSnapshots() {
        final Nomination alice = nomination(ALICE, "Alice", 0);
        final AwardSnapshot a = new FrozenAwards(List.of(alice), List.of());
        final AwardSnapshot b = new FrozenAwards(List.of(alice), List.of());
        assertTrue(a.sameLeaders(b));
    }

    @Test
    void sameLeadersReturnsFalseWhenMvpChanges() {
        final AwardSnapshot a = new FrozenAwards(List.of(nomination(ALICE, "Alice", 0)), List.of());
        final AwardSnapshot b = new FrozenAwards(List.of(nomination(BOB, "Bob", 0)), List.of());
        assertFalse(a.sameLeaders(b));
    }

    @Test
    void sameLeadersReturnsFalseWhenStreakChanges() {
        final AwardSnapshot a = new FrozenAwards(
            List.of(), List.of(nomination(ALICE, "Alice", SEVEN_DAYS))
        );
        final AwardSnapshot b = new FrozenAwards(
            List.of(), List.of(nomination(BOB, "Bob", SEVEN_DAYS))
        );
        assertFalse(a.sameLeaders(b));
    }

    @Test
    void sameLeadersReturnsFalseAgainstNoAwards() {
        final AwardSnapshot snap = new FrozenAwards(
            List.of(nomination(ALICE, "Alice", 0)), List.of()
        );
        assertFalse(new NoAwards().sameLeaders(snap));
    }

    @Test
    void sameLeadersReturnsTrueForTwoEmptySnapshots() {
        final AwardSnapshot a = new FrozenAwards(List.of(), List.of());
        final AwardSnapshot b = new FrozenAwards(List.of(), List.of());
        assertTrue(a.sameLeaders(b));
    }

    // --- helper ---

    private static Nomination nomination(final UUID uuid, final String name, final int streak) {
        return new StoredNomination(uuid, name, streak);
    }
}
