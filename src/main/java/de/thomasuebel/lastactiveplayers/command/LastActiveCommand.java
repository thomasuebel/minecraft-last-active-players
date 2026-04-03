package de.thomasuebel.lastactiveplayers.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the {@code /lastactive} command and its {@code test} subcommand.
 *
 * <p>The base command is available to any player and shows the last-active player list
 * plus the current MVP and streak leader. The {@code test} subcommand requires the
 * {@code lastactiveplayers.admin} permission and previews the award display name prefixes.
 */
public final class LastActiveCommand implements CommandExecutor {

    private static final String SUBCOMMAND_TEST = "test";
    private static final String PERM_ADMIN = "lastactiveplayers.admin";

    private final CommandLines list;
    private final CommandLines preview;

    /**
     * Constructs the command handler.
     *
     * @param list    the lines to show for the base command; never null
     * @param preview the lines to show for the {@code test} subcommand; never null
     */
    public LastActiveCommand(final CommandLines list, final CommandLines preview) {
        this.list = list;
        this.preview = preview;
    }

    @Override
    public boolean onCommand(
        final CommandSender sender,
        final Command command,
        final String label,
        final String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        final Set<UUID> online = new HashSet<>();
        for (final Player p : player.getServer().getOnlinePlayers()) {
            online.add(p.getUniqueId());
        }
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
        for (final String line : response.lines(online)) {
            player.sendMessage(line);
        }
        return true;
    }
}
