package de.thomasuebel.lastactiveplayers.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles the {@code /lastactive} command and its subcommands.
 *
 * <p>The base command and the {@code mvp} and {@code streak} subcommands are available to
 * any {@link CommandSender}. The {@code reload} and {@code test} subcommands are gated
 * behind the {@code lastactiveplayers.admin} permission (which defaults to op in
 * {@code plugin.yml}).
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

    private final CommandLines list;
    private final CommandLines mvp;
    private final CommandLines streak;
    private final CommandLines preview;
    private final Consumer<CommandSender> reloadAction;
    private final Supplier<Set<UUID>> online;

    /**
     * Constructs the command handler.
     *
     * @param list         the lines to show for the base command; never null
     * @param mvp          the lines to show for the {@code mvp} subcommand; never null
     * @param streak       the lines to show for the {@code streak} subcommand; never null
     * @param preview      the lines to show for the {@code test} subcommand; never null
     * @param reloadAction invoked with the sender when the {@code reload} subcommand is
     *                     executed; responsible for reloading config and messaging the
     *                     sender with the outcome; never null
     * @param online       supplies the current set of online player UUIDs on each
     *                     invocation; never null
     */
    public LastActiveCommand(
        final CommandLines list,
        final CommandLines mvp,
        final CommandLines streak,
        final CommandLines preview,
        final Consumer<CommandSender> reloadAction,
        final Supplier<Set<UUID>> online
    ) {
        this.list = list;
        this.mvp = mvp;
        this.streak = streak;
        this.preview = preview;
        this.reloadAction = reloadAction;
        this.online = online;
    }

    /**
     * Handles the command invocation.
     *
     * @param sender  the entity that executed the command; never null
     * @param command the command that was triggered; never null
     * @param label   the alias used; never null
     * @param args    the arguments supplied after the command label; never null
     * @return {@code true} always
     */
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
            for (final String line : this.mvp.lines(this.online.get())) {
                sender.sendMessage(line);
            }
        } else if (args.length > 0 && SUBCOMMAND_STREAK.equals(args[0])) {
            for (final String line : this.streak.lines(this.online.get())) {
                sender.sendMessage(line);
            }
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
            for (final String line : this.preview.lines(this.online.get())) {
                sender.sendMessage(line);
            }
        } else {
            for (final String line : this.list.lines(this.online.get())) {
                sender.sendMessage(line);
            }
        }
        return true;
    }
}
