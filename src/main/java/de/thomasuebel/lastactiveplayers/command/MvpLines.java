package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link CommandLines} for the {@code /lastactive mvp} subcommand.
 *
 * <p>Returns a single line showing the current MVP, or a tie line when multiple players
 * share the top playtime. Returns an empty list when no MVP exists.
 */
public final class MvpLines implements CommandLines {

    private static final String TOKEN_PLAYER = "{player}";
    private static final String TOKEN_PLAYERS = "{players}";

    private final Leaderboard mvpBoard;
    private final String mvpTemplate;
    private final String mvpTieTemplate;

    /**
     * Constructs the MVP command response.
     *
     * @param mvpBoard       the playtime leaderboard used to elect the MVP; never null
     * @param mvpTemplate    broadcast template for a sole MVP; use {player} token; never null
     * @param mvpTieTemplate broadcast template for tied MVPs; use {players} token; never null
     */
    public MvpLines(
        final Leaderboard mvpBoard,
        final String mvpTemplate,
        final String mvpTieTemplate
    ) {
        this.mvpBoard = mvpBoard;
        this.mvpTemplate = mvpTemplate;
        this.mvpTieTemplate = mvpTieTemplate;
    }

    @Override
    public List<String> lines(final Set<UUID> onlinePlayers) {
        final List<LeaderboardEntry> candidates = this.mvpBoard.topTied(Set.of());
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (candidates.size() == 1) {
            return Collections.singletonList(
                this.mvpTemplate.replace(TOKEN_PLAYER, candidates.get(0).username())
            );
        }
        final List<String> names = new ArrayList<>();
        for (final LeaderboardEntry entry : candidates) {
            names.add(entry.username());
        }
        return Collections.singletonList(
            this.mvpTieTemplate.replace(TOKEN_PLAYERS, String.join(", ", names))
        );
    }
}
