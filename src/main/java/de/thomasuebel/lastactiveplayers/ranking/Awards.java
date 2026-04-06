package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * Provides the current award state for online players.
 *
 * <p>Implementations reflect the most recent election result. Both methods
 * return an empty string for any player who holds no active award.
 */
public interface Awards {

    /**
     * Returns the configured display-name prefix for the award this player currently holds,
     * or an empty string if the player holds no active award.
     *
     * <p>The returned value matches the prefix that was applied to the player's display
     * name: {@code prefix.mvp} for the current MVP, {@code prefix.streak} for the current
     * streak leader, or {@code ""} for everyone else.
     *
     * @param uuid the player's unique ID; never null
     * @return the prefix string, or {@code ""} if no award is held; never null
     */
    String currentPrefix(UUID uuid);

    /**
     * Returns the award type this player currently holds: {@code "mvp"}, {@code "streak"},
     * or {@code ""} if the player holds no active award.
     *
     * @param uuid the player's unique ID; never null
     * @return {@code "mvp"}, {@code "streak"}, or {@code ""}; never null
     */
    String currentAward(UUID uuid);
}
