package de.thomasuebel.lastactiveplayers.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakMilestonesTest {

    private static final int MILESTONE_THREE = 3;
    private static final int MILESTONE_SEVEN = 7;
    private static final int MILESTONE_FOURTEEN = 14;
    private static final int MILESTONE_THIRTY = 30;
    private static final int MILESTONE_SIXTY = 60;
    private static final int BEYOND_MAX = 100;

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
        final List<Integer> crossed = milestones.crossedBy(2, MILESTONE_THREE);
        assertEquals(List.of(MILESTONE_THREE), crossed);
    }

    @Test
    void multipleMilestonesCrossedInOneJump() {
        final List<Integer> crossed = milestones.crossedBy(0, MILESTONE_SEVEN);
        assertEquals(List.of(MILESTONE_THREE, MILESTONE_SEVEN), crossed);
    }

    @Test
    void previousMilestoneNotIncluded() {
        final List<Integer> crossed = milestones.crossedBy(MILESTONE_THREE, MILESTONE_SEVEN);
        assertEquals(List.of(MILESTONE_SEVEN), crossed);
    }

    @Test
    void alreadyAtMilestoneNoCrossings() {
        assertTrue(milestones.crossedBy(MILESTONE_SEVEN, MILESTONE_SEVEN).isEmpty());
    }

    @Test
    void allMilestonesReturnedForMaxJump() {
        final List<Integer> crossed = milestones.crossedBy(0, MILESTONE_SIXTY);
        assertEquals(
            List.of(MILESTONE_THREE, MILESTONE_SEVEN, MILESTONE_FOURTEEN,
                MILESTONE_THIRTY, MILESTONE_SIXTY),
            crossed
        );
    }

    @Test
    void beyondHighestMilestoneNoCrossings() {
        assertTrue(milestones.crossedBy(MILESTONE_SIXTY, BEYOND_MAX).isEmpty());
    }
}
