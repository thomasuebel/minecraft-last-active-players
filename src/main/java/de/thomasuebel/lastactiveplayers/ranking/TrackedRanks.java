package de.thomasuebel.lastactiveplayers.ranking;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Tracks the last-known leaderboard rank for each online player and emits rank-improvement
 * notifications during heartbeat pulses.
 */
public interface TrackedRanks {

    /**
     * Seeds the rank for a joining player from the given leaderboard snapshot.
     *
     * @param uuid   the joining player's UUID; never null
     * @param ranked the current leaderboard, best first; never null
     */
    void joined(UUID uuid, List<LeaderboardEntry> ranked);

    /**
     * Removes the player from rank tracking.
     *
     * @param uuid the player's UUID; never null
     */
    void quit(UUID uuid);

    /**
     * Clears rank tracking for all players.
     */
    void reset();

    /**
     * Compares each tracked player's current rank in {@code ranked} against their stored rank.
     * Calls {@code notify} for each player whose rank improved (decreased) since the last
     * recorded value, excluding rank #1 (covered by the MVP broadcast).
     * Updates stored ranks for all players found in the snapshot.
     *
     * @param ranked the current leaderboard, best first; never null
     * @param notify callback that receives the player UUID and a formatted hint message; never null
     */
    void pulse(List<LeaderboardEntry> ranked, BiConsumer<UUID, String> notify);
}
