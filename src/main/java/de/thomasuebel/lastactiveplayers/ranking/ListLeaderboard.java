package de.thomasuebel.lastactiveplayers.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A {@link Leaderboard} backed by a pre-fetched list of entries.
 *
 * <p>Used to wrap an already-queried snapshot so that {@link LeaderboardRankHint} can format
 * a hint without issuing an additional database query.
 *
 * <p>Package-private -- callers outside this package should interact through the
 * {@link Leaderboard} interface.
 */
final class ListLeaderboard implements Leaderboard {

    private final List<LeaderboardEntry> entries;

    ListLeaderboard(final List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    @Override
    public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
        final List<LeaderboardEntry> result = new ArrayList<>();
        for (final LeaderboardEntry entry : this.entries) {
            if (!exclude.contains(entry.uuid()) && result.size() < limit) {
                result.add(entry);
            }
        }
        return result;
    }
}
