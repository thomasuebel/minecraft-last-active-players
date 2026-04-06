package de.thomasuebel.lastactiveplayers.ranking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Tracks the last-known leaderboard rank for each online player and emits rank-improvement
 * notifications during heartbeat pulses.
 *
 * <p>A notification is sent only when a player's rank number decreases (improves) compared to
 * the last recorded value, and only when the new rank is not first place (the MVP broadcast
 * covers that case).
 *
 * <p>First appearance on the leaderboard -- when a player's stored rank is unknown -- is
 * treated as a baseline, not an improvement, so no notification fires.
 *
 * <p><strong>Thread safety:</strong> This class is not thread-safe. All methods must be called
 * from the Bukkit main thread. Do not schedule calls on async threads.
 */
public final class OnlineRanks implements TrackedRanks {

    /** Sentinel value for players not yet present on the leaderboard. */
    private static final int UNRANKED = Integer.MAX_VALUE;
    private static final int RANK_ONE = 1;

    private final Map<UUID, Integer> lastRank = new HashMap<>();
    private final String template;

    /**
     * Constructs an {@link OnlineRanks} tracker that formats improvement messages using the
     * given template.
     *
     * @param template message template supporting {rank}, {next_rank}, {minutes} tokens; never null
     */
    public OnlineRanks(final String template) {
        this.template = template;
    }

    /**
     * Records the joining player's current rank derived from the given leaderboard snapshot.
     * If the player does not appear in the snapshot their rank is stored as {@code UNRANKED},
     * preventing spurious notifications on the first heartbeat.
     *
     * @param uuid   the joining player's UUID; never null
     * @param ranked the current leaderboard, best first; never null
     */
    @Override
    public void joined(final UUID uuid, final List<LeaderboardEntry> ranked) {
        this.lastRank.put(uuid, trueRank(uuid, ranked));
    }

    /**
     * Removes the player from rank tracking. Subsequent pulses will not fire for this player.
     *
     * @param uuid the player's UUID; never null
     */
    @Override
    public void quit(final UUID uuid) {
        this.lastRank.remove(uuid);
    }

    /**
     * Clears rank tracking for all players. Used on plugin reload.
     */
    @Override
    public void reset() {
        this.lastRank.clear();
    }

    /**
     * Compares each tracked player's current rank in {@code ranked} against their stored rank.
     * Calls {@code notify} for each player whose rank improved (decreased) since the last
     * recorded value, excluding rank #1 (covered by the MVP broadcast).
     * Updates stored ranks for all players found in the snapshot.
     *
     * @param ranked the current leaderboard, best first; never null
     * @param notify callback that receives the player UUID and a formatted hint message; never null
     */
    @Override
    public void pulse(
        final List<LeaderboardEntry> ranked, final BiConsumer<UUID, String> notify
    ) {
        final RankHint hint = new LeaderboardRankHint(new ListLeaderboard(ranked), this.template);
        final Map<UUID, Integer> updates = new HashMap<>();
        for (final Map.Entry<UUID, Integer> tracked : this.lastRank.entrySet()) {
            final UUID uuid = tracked.getKey();
            final int current = trueRank(uuid, ranked);
            if (current == UNRANKED) {
                continue;
            }
            final int stored = tracked.getValue();
            if (stored != UNRANKED && current < stored && current != RANK_ONE) {
                hint.text(uuid, Set.of()).ifPresent(text -> notify.accept(uuid, text));
            }
            updates.put(uuid, current);
        }
        this.lastRank.putAll(updates);
    }

    private int trueRank(final UUID uuid, final List<LeaderboardEntry> ranked) {
        boolean found = false;
        long playerSeconds = 0L;
        for (final LeaderboardEntry entry : ranked) {
            if (entry.uuid().equals(uuid)) {
                playerSeconds = entry.totalSeconds();
                found = true;
                break;
            }
        }
        if (!found) {
            return UNRANKED;
        }
        int strictlyAbove = 0;
        for (final LeaderboardEntry entry : ranked) {
            if (entry.totalSeconds() > playerSeconds) {
                strictlyAbove++;
            }
        }
        return strictlyAbove + 1;
    }
}
