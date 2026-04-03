package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.display.JoinMessage;
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
 * {@link CommandLines} for the base {@code /lastactive} command.
 *
 * <p>Returns the same last-active player list as the join message, followed by one line
 * for the current MVP (if any) and one line for the current streak leader (if any).
 */
public final class LastActiveLines implements CommandLines {

    private static final String TOKEN_PLAYER = "{player}";
    private static final String TOKEN_STREAK = "{streak}";

    private final JoinMessage joinMessage;
    private final Leaderboard mvpBoard;
    private final Players players;
    private final String mvpTemplate;
    private final String streakTemplate;

    /**
     * Constructs the last-active command response.
     *
     * @param joinMessage    the last-active player list; never null
     * @param mvpBoard       the playtime leaderboard used to elect the MVP; never null
     * @param players        the player store used to find the streak leader; never null
     * @param mvpTemplate    broadcast template for the MVP line; use {player} token; never null
     * @param streakTemplate broadcast template for the streak line; use {player} and {streak}
     *                       tokens; never null
     */
    public LastActiveLines(
        final JoinMessage joinMessage,
        final Leaderboard mvpBoard,
        final Players players,
        final String mvpTemplate,
        final String streakTemplate
    ) {
        this.joinMessage = joinMessage;
        this.mvpBoard = mvpBoard;
        this.players = players;
        this.mvpTemplate = mvpTemplate;
        this.streakTemplate = streakTemplate;
    }

    @Override
    public List<String> lines(final Set<UUID> onlinePlayers) {
        final List<String> result = new ArrayList<>(this.joinMessage.lines(onlinePlayers));
        final List<LeaderboardEntry> top = this.mvpBoard.top(1, Set.of());
        if (!top.isEmpty()) {
            result.add(this.mvpTemplate.replace(TOKEN_PLAYER, top.get(0).username()));
        }
        final Player streakLeader = this.players.withHighestStreak();
        if (streakLeader.exists()) {
            result.add(
                this.streakTemplate
                    .replace(TOKEN_PLAYER, streakLeader.username())
                    .replace(TOKEN_STREAK, String.valueOf(streakLeader.streakDays()))
            );
        }
        return Collections.unmodifiableList(result);
    }
}
