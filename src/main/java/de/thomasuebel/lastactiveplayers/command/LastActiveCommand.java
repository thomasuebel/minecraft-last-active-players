package de.thomasuebel.lastactiveplayers.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Handles the {@code /lastactive} command and its {@code test} subcommand.
 *
 * <p>The base command is available to any {@link CommandSender} (players and console) and
 * shows the last-active player list plus the current MVP and streak leader. The {@code test}
 * subcommand is gated behind the {@code lastactiveplayers.admin} permission (which defaults to
 * op in {@code plugin.yml}); using a named permission node rather than a literal {@link
 * CommandSender#isOp()} check allows server operators to delegate the permission via a
 * permissions plugin if desired.
 */
public final class LastActiveCommand implements CommandExecutor {

    private static final String SUBCOMMAND_TEST = "test";
    private static final String PERM_ADMIN = "lastactiveplayers.admin";

    private final CommandLines list;
    private final CommandLines preview;
    private final Supplier<Set<UUID>> online;

    /**
     * Constructs the command handler.
     *
     * @param list    the lines to show for the base command; never null
     * @param preview the lines to show for the {@code test} subcommand; never null
     * @param online  supplies the current set of online player UUIDs on each invocation; never null
     */
    public LastActiveCommand(
        final CommandLines list, final CommandLines preview, final Supplier<Set<UUID>> online
    ) {
        this.list = list;
        this.preview = preview;
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
        final CommandLines response;
        if (args.length > 0 && SUBCOMMAND_TEST.equals(args[0])) {
            if (!sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage("You do not have permission to use this subcommand.");
                return true;
            }
            response = this.preview;
        } else {
            response = this.list;
        }
        for (final String line : response.lines(this.online.get())) {
            sender.sendMessage(line);
        }
        return true;
    }
}
