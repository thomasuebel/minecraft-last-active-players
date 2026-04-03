package de.thomasuebel.lastactiveplayers.player;

import java.util.ArrayList;
import java.util.List;

/**
 * The defined streak milestone thresholds for LastActivePlayers.
 *
 * <p>Milestones are reached at 3, 7, 14, 30, and 60 consecutive login days.
 * A milestone broadcast and permission node are issued the first time a player
 * crosses each threshold.
 */
public final class StreakMilestones implements Milestones {

    private static final List<Integer> THRESHOLDS = List.of(3, 7, 14, 30, 60);

    @Override
    public List<Integer> crossedBy(final int previousDays, final int newDays) {
        final List<Integer> result = new ArrayList<>();
        for (final int threshold : THRESHOLDS) {
            if (threshold > previousDays && threshold <= newDays) {
                result.add(threshold);
            }
        }
        return result;
    }
}
