package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Null Object implementation of {@link Player} returned when no database record exists.
 *
 * <p>All value methods return safe, empty defaults. Callers that need to distinguish
 * "player found" from "player not found" should check {@link #exists()}.
 */
public final class NoPlayer implements Player {

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public UUID uuid() {
        return new UUID(0L, 0L);
    }

    @Override
    public String username() {
        return "";
    }

    @Override
    public int streakDays() {
        return 0;
    }

    @Override
    public Optional<LocalDate> streakLastDay() {
        return Optional.empty();
    }
}
