package de.thomasuebel.lastactiveplayers.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The set of chat lines produced by a {@code /lastactive} subcommand.
 */
public interface CommandLines {

    /**
     * Returns the lines to send to the command sender.
     *
     * @param onlinePlayers UUIDs of all currently online players; used as an exclusion set where
     *                      applicable (e.g. the join-message list omits online players); never null
     * @return ordered list of chat lines; never null, may be empty
     */
    List<String> lines(Set<UUID> onlinePlayers);
}
