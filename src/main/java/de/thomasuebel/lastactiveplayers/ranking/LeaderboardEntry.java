package de.thomasuebel.lastactiveplayers.ranking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A single entry in a player activity leaderboard.
 *
 * @param uuid         the player's UUID; never null
 * @param username     the player's last-known username; never null
 * @param totalSeconds total accumulated play time in the leaderboard window; non-negative
 * @param lastLeave    timestamp of the player's most recent leave, or empty; never null
 */
public record LeaderboardEntry(
    UUID uuid,
    String username,
    long totalSeconds,
    Optional<Instant> lastLeave
) {
}
