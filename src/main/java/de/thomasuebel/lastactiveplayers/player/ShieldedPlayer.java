package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link Player} decorator that overrides {@link #streakLastDay()} to a caller-supplied
 * date, while delegating all other behaviour to the wrapped player.
 *
 * <p>Used by streak-shield logic to make {@link TodayStreak} compute the streak as if
 * the player had logged in yesterday rather than two days ago, preserving the streak.
 */
public final class ShieldedPlayer implements Player {

    private final Player delegate;
    private final LocalDate effectiveLastDay;

    /**
     * Constructs a shielded view of the given player.
     *
     * @param delegate         the real player record; never null
     * @param effectiveLastDay the date to substitute for {@link #streakLastDay()}; never null
     */
    public ShieldedPlayer(final Player delegate, final LocalDate effectiveLastDay) {
        this.delegate = delegate;
        this.effectiveLastDay = effectiveLastDay;
    }

    @Override
    public boolean exists() {
        return this.delegate.exists();
    }

    @Override
    public UUID uuid() {
        return this.delegate.uuid();
    }

    @Override
    public String username() {
        return this.delegate.username();
    }

    @Override
    public int streakDays() {
        return this.delegate.streakDays();
    }

    @Override
    public Optional<LocalDate> streakLastDay() {
        return Optional.of(this.effectiveLastDay);
    }
}
