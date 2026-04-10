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
     * Returns the configured prefix for the award this player currently holds,
     * or an empty string if the player holds no active award.
     *
     * <p>The returned value is {@code prefix.mvp} for the current MVP,
     * {@code prefix.streak} for the current streak leader, or {@code ""} for
     * everyone else. Exposed via PlaceholderAPI as
     * {@code %lastactiveplayers_prefix%}.
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
