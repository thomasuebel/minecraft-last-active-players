package de.thomasuebel.lastactiveplayers.player;

import java.util.List;

/**
 * A set of streak milestone thresholds.
 *
 * <p>Used to determine which milestones a player newly crossed when their streak
 * advanced from one value to another.
 */
public interface Milestones {

    /**
     * Returns the milestones that fall strictly between {@code previousDays} (exclusive)
     * and {@code newDays} (inclusive), representing newly reached thresholds.
     *
     * @param previousDays the streak day count before the update; non-negative
     * @param newDays      the streak day count after the update; non-negative
     * @return newly reached milestones in ascending order; never null, may be empty
     */
    List<Integer> crossedBy(int previousDays, int newDays);
}
