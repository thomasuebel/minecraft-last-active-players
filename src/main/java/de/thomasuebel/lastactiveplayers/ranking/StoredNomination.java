package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * Immutable implementation of {@link Nomination} wrapping a known title holder's identity.
 */
public final class StoredNomination implements Nomination {

    private final UUID uuid;
    private final String username;
    private final int streakDays;

    /**
     * Constructs a nomination for the given player identity with an optional streak count.
     *
     * @param uuid       the title holder's UUID; never null
     * @param username   the title holder's last-known username; never null
     * @param streakDays the holder's current streak in days; 0 for MVP nominations
     */
    public StoredNomination(final UUID uuid, final String username, final int streakDays) {
        this.uuid = uuid;
        this.username = username;
        this.streakDays = streakDays;
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

    @Override
    public int streakDays() {
        return this.streakDays;
    }
}
