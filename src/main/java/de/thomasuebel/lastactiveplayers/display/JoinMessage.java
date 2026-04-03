package de.thomasuebel.lastactiveplayers.display;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The list of last-active player lines shown to a player when they join the server.
 */
public interface JoinMessage {

    /**
     * Returns formatted lines for the last-active player list, excluding online players.
     *
     * @param exclude UUIDs of currently online players to omit from the list; never null
     * @return formatted message lines; never null, may be empty
     */
    List<String> lines(Set<UUID> exclude);
}
