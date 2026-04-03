package de.thomasuebel.lastactiveplayers.ranking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A single entry in a player activity leaderboard.
 *
 * <p>Carries the player identity, their accumulated play time in the relevant window,
 * and the timestamp of their most recent departure.
 */
public interface LeaderboardEntry {

    /**
     * Returns the player's UUID.
     *
     * @return the player UUID; never null
     */
    UUID uuid();

    /**
     * Returns the player's last-known username.
     *
     * @return username; never null
     */
    String username();

    /**
     * Returns the total accumulated play time for this player in the leaderboard window.
     *
     * @return seconds; non-negative
     */
    long totalSeconds();

    /**
     * Returns the timestamp of the player's most recent leave, or empty if they have
     * never left (e.g. an open session with no close).
     *
     * @return last leave instant, or empty; never null
     */
    Optional<Instant> lastLeave();
}
