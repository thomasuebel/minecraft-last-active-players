package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable {@link Player} record loaded from the database.
 */
final class StoredPlayer implements Player {

    private final UUID uuid;
    private final String username;
    private final int streakDays;
    private final Optional<LocalDate> streakLastDay;

    /**
     * Constructs a stored player from its raw field values.
     *
     * @param uuid          the player UUID; never null
     * @param username      the last known username; never null
     * @param streakDays    consecutive daily login streak; non-negative
     * @param streakLastDay the date of the last streak day, or empty
     */
    StoredPlayer(
        final UUID uuid,
        final String username,
        final int streakDays,
        final Optional<LocalDate> streakLastDay
    ) {
        this.uuid = uuid;
        this.username = username;
        this.streakDays = streakDays;
        this.streakLastDay = streakLastDay;
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

    @Override
    public Optional<LocalDate> streakLastDay() {
        return this.streakLastDay;
    }
}
