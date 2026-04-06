package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.display.JoinMessage;
import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles the {@code /lastactive} command and its subcommands.
 *
 * <p>The base command and the {@code mvp} and {@code streak} subcommands are available to
 * any {@link CommandSender}. The {@code reload} and {@code test} subcommands are gated
 * behind the {@code lastactiveplayers.admin} permission.
 */
public final class LastActiveCommand implements CommandExecutor {

    private static final String SUBCOMMAND_HELP = "help";
    private static final String SUBCOMMAND_MVP = "mvp";
    private static final String SUBCOMMAND_STREAK = "streak";
    private static final String SUBCOMMAND_RELOAD = "reload";
    private static final String SUBCOMMAND_TEST = "test";
    private static final String PERM_ADMIN = "lastactiveplayers.admin";
    private static final String MSG_NO_PERMISSION =
        "You do not have permission to use this subcommand.";
    private static final String TOKEN_PLAYER = "{player}";
    private static final String TOKEN_PLAYERS = "{players}";
    private static final String TOKEN_STREAK = "{streak}";

    private final JoinMessage joinMessage;
    private final Leaderboard mvpBoard;
    private final Players players;
    private final String mvpTemplate;
    private final String mvpTieTemplate;
    private final String streakTemplate;
    private final String streakTieTemplate;
    private final String mvpPrefix;
    private final String streakPrefix;
    private final Consumer<CommandSender> reloadAction;
    private final Supplier<Set<UUID>> online;

    /**
     * Constructs the command handler.
     *
     * @param joinMessage       the last-active player list shown for the base command; never null
     * @param mvpBoard          the playtime leaderboard used to elect the MVP; never null
     * @param players           the player store used to find the streak leader; never null
     * @param mvpTemplate       broadcast template for a sole MVP; {player} token; never null
     * @param mvpTieTemplate    broadcast template for tied MVPs; {players} token; never null
     * @param streakTemplate    broadcast template for a sole streak leader; {player}, {streak}
     *                          tokens; never null
     * @param streakTieTemplate broadcast template for tied streak leaders; {players}, {streak}
     *                          tokens; never null
     * @param mvpPrefix         display name prefix for the MVP; never null
     * @param streakPrefix      display name prefix for the streak leader; never null
     * @param reloadAction      invoked with the sender when the reload subcommand is executed;
     *                          never null
     * @param online            supplies the current set of online player UUIDs; never null
     */
    public LastActiveCommand(
        final JoinMessage joinMessage,
        final Leaderboard mvpBoard,
        final Players players,
        final String mvpTemplate,
        final String mvpTieTemplate,
        final String streakTemplate,
        final String streakTieTemplate,
        final String mvpPrefix,
        final String streakPrefix,
        final Consumer<CommandSender> reloadAction,
        final Supplier<Set<UUID>> online
    ) {
        this.joinMessage = joinMessage;
        this.mvpBoard = mvpBoard;
        this.players = players;
        this.mvpTemplate = mvpTemplate;
        this.mvpTieTemplate = mvpTieTemplate;
        this.streakTemplate = streakTemplate;
        this.streakTieTemplate = streakTieTemplate;
        this.mvpPrefix = mvpPrefix;
        this.streakPrefix = streakPrefix;
        this.reloadAction = reloadAction;
        this.online = online;
    }

    @Override
    public boolean onCommand(
        final CommandSender sender,
        final Command command,
        final String label,
        final String[] args
    ) {
        if (args.length > 0 && SUBCOMMAND_HELP.equals(args[0])) {
            return false;
        } else if (args.length > 0 && SUBCOMMAND_MVP.equals(args[0])) {
            sendLines(sender, mvpLines());
        } else if (args.length > 0 && SUBCOMMAND_STREAK.equals(args[0])) {
            sendLines(sender, streakLines());
        } else if (args.length > 0 && SUBCOMMAND_RELOAD.equals(args[0])) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(MSG_NO_PERMISSION);
                return true;
            }
            this.reloadAction.accept(sender);
        } else if (args.length > 0 && SUBCOMMAND_TEST.equals(args[0])) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(MSG_NO_PERMISSION);
                return true;
            }
            sendLines(sender, previewLines());
        } else {
            sendLines(sender, listLines(this.online.get()));
        }
        return true;
    }

    private List<String> listLines(final Set<UUID> onlinePlayers) {
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

    private List<String> mvpLines() {
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

    private List<String> streakLines() {
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

    private List<String> previewLines() {
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

    private void sendLines(final CommandSender sender, final List<String> lines) {
        for (final String line : lines) {
            sender.sendMessage(line);
        }
    }
}
