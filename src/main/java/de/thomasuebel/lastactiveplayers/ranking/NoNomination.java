package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * Null Object implementation of {@link Nomination} returned when no title holder exists.
 *
 * <p>Callers that must distinguish "winner found" from "no winner" should check
 * {@link #exists()}.
 */
public final class NoNomination implements Nomination {

    /**
     * Constructs a no-nomination sentinel.
     */
    public NoNomination() {
    }

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
}
