package de.thomasuebel.lastactiveplayers.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoAwardsTest {

    @Test
    void mvpCandidatesIsEmpty() {
        assertTrue(new NoAwards().mvpCandidates().isEmpty());
    }

    @Test
    void streakCandidatesIsEmpty() {
        assertTrue(new NoAwards().streakCandidates().isEmpty());
    }

    @Test
    void sameLeadersAlwaysReturnsFalse() {
        assertFalse(new NoAwards().sameLeaders(new NoAwards()));
    }
}
