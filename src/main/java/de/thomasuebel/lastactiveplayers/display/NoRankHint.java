package de.thomasuebel.lastactiveplayers.display;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Null Object implementation of {@link RankHint} used when no rank hint is applicable,
 * for example when the join list is sorted by last-leave time rather than play time.
 *
 * <p>Always returns empty so callers need not check whether a hint is configured.
 */
public final class NoRankHint implements RankHint {

    /**
     * Constructs a no-rank-hint sentinel.
     */
    public NoRankHint() {
    }

    @Override
    public Optional<String> text(final UUID playerUuid, final Set<UUID> onlinePlayers) {
        return Optional.empty();
    }
}
