package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;

/**
 * The consecutive daily login streak for a player.
 */
public interface Streak {

    /**
     * Returns the number of consecutive days in this streak.
     *
     * @return positive day count; at least 1 for any active streak
     */
    int days();

    /**
     * Returns the calendar date on which the streak was last active.
     *
     * @return the last active date; never null
     */
    LocalDate lastDay();
}
