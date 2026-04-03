package de.thomasuebel.lastactiveplayers.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakMilestonesTest {

    private Milestones milestones;

    @BeforeEach
    void setUp() {
        this.milestones = new StreakMilestones();
    }

    @Test
    void noCrossingsWhenStreakDoesNotReachAnyMilestone() {
        assertTrue(milestones.crossedBy(0, 2).isEmpty());
    }

    @Test
    void singleMilestoneCrossedExactly() {
        final List<Integer> crossed = milestones.crossedBy(2, 3);
        assertEquals(List.of(3), crossed);
    }

    @Test
    void multipleMilestonesCrossedInOneJump() {
        final List<Integer> crossed = milestones.crossedBy(0, 7);
        assertEquals(List.of(3, 7), crossed);
    }

    @Test
    void previousMilestoneNotIncluded() {
        final List<Integer> crossed = milestones.crossedBy(3, 7);
        assertEquals(List.of(7), crossed);
    }

    @Test
    void alreadyAtMilestoneNoCrossings() {
        assertTrue(milestones.crossedBy(7, 7).isEmpty());
    }

    @Test
    void allMilestonesReturnedForMaxJump() {
        final List<Integer> crossed = milestones.crossedBy(0, 60);
        assertEquals(List.of(3, 7, 14, 30, 60), crossed);
    }

    @Test
    void beyondHighestMilestoneNoCrossings() {
        assertTrue(milestones.crossedBy(60, 100).isEmpty());
    }
}
