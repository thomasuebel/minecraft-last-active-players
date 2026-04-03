package de.thomasuebel.lastactiveplayers.ranking;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A ranked list of players ordered by an implementation-defined sort criterion.
 *
 * <p>Implementations may sort by total playtime in a rolling window, by most-recent
 * leave time, or by any other metric.
 */
public interface Leaderboard {

    /**
     * Returns up to {@code limit} entries, excluding any players whose UUIDs appear in
     * {@code exclude} (e.g. currently online players).
     *
     * <p>The returned list is in descending rank order (best first).
     *
     * @param limit   maximum number of entries to return; positive
     * @param exclude UUIDs to omit from the result; never null, may be empty
     * @return ranked entries; never null, may be empty
     */
    List<LeaderboardEntry> top(int limit, Set<UUID> exclude);

    /**
     * Returns all entries tied for first place (sharing the highest rank score),
     * excluding any players whose UUIDs appear in {@code exclude}.
     *
     * <p>If no qualifying player exists the returned list is empty.
     * If exactly one player leads, the list contains that single entry.
     * If two or more players share the top score, all are returned.
     *
     * <p>The default implementation returns only the single top entry (no tie detection).
     * Implementations that support tie detection should override this method.
     *
     * @param exclude UUIDs to omit from the result; never null, may be empty
     * @return tied top entries; never null, may be empty
     */
    default List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
        return top(1, exclude);
    }
}
