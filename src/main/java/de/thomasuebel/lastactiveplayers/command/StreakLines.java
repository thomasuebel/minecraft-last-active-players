package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link CommandLines} for the {@code /lastactive streak} subcommand.
 *
 * <p>Returns a single line showing the current streak leader, or a tie line when multiple
 * players share the highest streak. Returns an empty list when no streak leader exists.
 */
public final class StreakLines implements CommandLines {

    private static final String TOKEN_PLAYER = "{player}";
    private static final String TOKEN_PLAYERS = "{players}";
    private static final String TOKEN_STREAK = "{streak}";

    private final Players players;
    private final String streakTemplate;
    private final String streakTieTemplate;

    /**
     * Constructs the streak command response.
     *
     * @param players           the player store used to find the streak leader; never null
     * @param streakTemplate    broadcast template for a sole streak leader;
     *                          use {player} and {streak} tokens; never null
     * @param streakTieTemplate broadcast template for tied streak leaders;
     *                          use {players} and {streak} tokens; never null
     */
    public StreakLines(
        final Players players,
        final String streakTemplate,
        final String streakTieTemplate
    ) {
        this.players = players;
        this.streakTemplate = streakTemplate;
        this.streakTieTemplate = streakTieTemplate;
    }

    @Override
    public List<String> lines(final Set<UUID> onlinePlayers) {
        final List<Player> candidates = this.players.withTopStreak();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        final String days = String.valueOf(candidates.get(0).streakDays());
        if (candidates.size() == 1) {
            return Collections.singletonList(
                this.streakTemplate
                    .replace(TOKEN_PLAYER, candidates.get(0).username())
                    .replace(TOKEN_STREAK, days)
            );
        }
        final List<String> names = new ArrayList<>();
        for (final Player player : candidates) {
            names.add(player.username());
        }
        return Collections.singletonList(
            this.streakTieTemplate
                .replace(TOKEN_PLAYERS, String.join(", ", names))
                .replace(TOKEN_STREAK, days)
        );
    }
}
