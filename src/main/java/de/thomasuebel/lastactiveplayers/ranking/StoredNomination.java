package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * Package-private immutable implementation of {@link Nomination} wrapping a known
 * title holder's identity.
 */
public final class StoredNomination implements Nomination {

    private final UUID uuid;
    private final String username;

    /**
     * Constructs a nomination for the given player identity.
     *
     * @param uuid     the title holder's UUID; never null
     * @param username the title holder's last-known username; never null
     */
    public StoredNomination(final UUID uuid, final String username) {
        this.uuid = uuid;
        this.username = username;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public String username() {
        return this.username;
    }
}
