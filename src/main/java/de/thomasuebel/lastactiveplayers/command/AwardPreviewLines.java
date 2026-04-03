package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link CommandLines} for the {@code /lastactive test} subcommand.
 *
 * <p>Previews how the current MVP and streak leader display names will appear in chat,
 * with their configured prefixes applied.
 */
public final class AwardPreviewLines implements CommandLines {

    private final Leaderboard mvpBoard;
    private final Players players;
    private final String mvpPrefix;
    private final String streakPrefix;

    /**
     * Constructs the award preview response.
     *
     * @param mvpBoard     the playtime leaderboard used to elect the MVP; never null
     * @param players      the player store used to find the streak leader; never null
     * @param mvpPrefix    display name prefix applied to the MVP; never null
     * @param streakPrefix display name prefix applied to the streak leader; never null
     */
    public AwardPreviewLines(
        final Leaderboard mvpBoard,
        final Players players,
        final String mvpPrefix,
        final String streakPrefix
    ) {
        this.mvpBoard = mvpBoard;
        this.players = players;
        this.mvpPrefix = mvpPrefix;
        this.streakPrefix = streakPrefix;
    }

    @Override
    public List<String> lines(final Set<UUID> onlinePlayers) {
        // onlinePlayers is intentionally unused: preview shows actual award state regardless of
        // who is online. The format is also intentionally hardcoded (prefix + name) rather than
        // using the broadcast template, so operators can see the display name as it will appear
        // in chat, not the template text.
        final List<String> result = new ArrayList<>();
        final List<LeaderboardEntry> top = this.mvpBoard.top(1, Set.of());
        if (!top.isEmpty()) {
            result.add(this.mvpPrefix + top.get(0).username());
        }
        final Player streakLeader = this.players.withHighestStreak();
        if (streakLeader.exists()) {
            result.add(
                this.streakPrefix + streakLeader.username()
                + " (" + streakLeader.streakDays() + " days)"
            );
        }
        return Collections.unmodifiableList(result);
    }
}
