package de.thomasuebel.lastactiveplayers.ranking;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@link RankHint} backed by a {@link Leaderboard}.
 *
 * <p>Queries the leaderboard excluding all online players except the target player, so
 * the player's own accumulated play time is visible in the ranking. Returns empty if the
 * player has no closed sessions or is already ranked first.
 */
public final class LeaderboardRankHint implements RankHint {

    // No artificial cap: every registered player must be reachable so the rank is correct.
    private static final int MAX_SEARCH_RANK = Integer.MAX_VALUE;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final String TOKEN_RANK = "{rank}";
    private static final String TOKEN_NEXT_RANK = "{next_rank}";
    private static final String TOKEN_MINUTES = "{minutes}";

    private final Leaderboard leaderboard;
    private final String template;

    /**
     * Constructs a rank hint backed by the given leaderboard.
     *
     * @param leaderboard the leaderboard used to determine rank positions; never null
     * @param template    message template with {rank}, {next_rank}, {minutes} tokens; never null
     */
    public LeaderboardRankHint(final Leaderboard leaderboard, final String template) {
        this.leaderboard = leaderboard;
        this.template = template;
    }

    @Override
    public Optional<String> text(final UUID playerUuid, final Set<UUID> onlinePlayers) {
        // Copy defensively so we can mutate the set without affecting the caller.
        final Set<UUID> excludeOthers = new HashSet<>(onlinePlayers);
        excludeOthers.remove(playerUuid);
        final List<LeaderboardEntry> ranked =
            this.leaderboard.top(MAX_SEARCH_RANK, excludeOthers);
        boolean found = false;
        long playerSeconds = 0L;
        for (final LeaderboardEntry entry : ranked) {
            if (entry.uuid().equals(playerUuid)) {
                playerSeconds = entry.totalSeconds();
                found = true;
                break;
            }
        }
        if (!found) {
            return Optional.empty();
        }
        // True shared rank: count players with strictly more seconds.
        // The list is sorted DESC, so the last strictly-above entry holds the minimum
        // score strictly above the player -- this is the score of the rank just above.
        int strictlyAbove = 0;
        long nextRankScore = playerSeconds;
        for (final LeaderboardEntry entry : ranked) {
            if (entry.totalSeconds() > playerSeconds) {
                strictlyAbove++;
                nextRankScore = entry.totalSeconds();
            }
        }
        final int rank = strictlyAbove + 1;
        if (rank == 1) {
            return Optional.empty();
        }
        final long gap = nextRankScore - playerSeconds;
        final long minutes = (gap + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE;
        return Optional.of(
            this.template
                .replace(TOKEN_RANK, String.valueOf(rank))
                .replace(TOKEN_NEXT_RANK, String.valueOf(rank - 1))
                .replace(TOKEN_MINUTES, String.valueOf(minutes))
        );
    }
}
