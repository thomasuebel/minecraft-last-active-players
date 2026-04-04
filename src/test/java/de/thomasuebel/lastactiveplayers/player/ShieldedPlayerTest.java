package de.thomasuebel.lastactiveplayers.player;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShieldedPlayerTest {

    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final LocalDate ORIGINAL_LAST_DAY = LocalDate.of(2026, Month.APRIL, 1);
    private static final LocalDate EFFECTIVE_LAST_DAY = LocalDate.of(2026, Month.APRIL, 3);
    private static final int STREAK_DAYS = 5;

    private static Player basePlayer() {
        return new Player() {
            @Override public boolean exists() { return true; }
            @Override public UUID uuid() { return PLAYER_UUID; }
            @Override public String username() { return "Alice"; }
            @Override public int streakDays() { return STREAK_DAYS; }
            @Override
            public Optional<LocalDate> streakLastDay() {
                return Optional.of(ORIGINAL_LAST_DAY);
            }
        };
    }

    @Test
    void overridesStreakLastDay() {
        final Player shielded = new ShieldedPlayer(basePlayer(), EFFECTIVE_LAST_DAY);
        assertEquals(Optional.of(EFFECTIVE_LAST_DAY), shielded.streakLastDay());
    }

    @Test
    void delegatesStreakDays() {
        final Player shielded = new ShieldedPlayer(basePlayer(), EFFECTIVE_LAST_DAY);
        assertEquals(STREAK_DAYS, shielded.streakDays());
    }

    @Test
    void delegatesUuid() {
        final Player shielded = new ShieldedPlayer(basePlayer(), EFFECTIVE_LAST_DAY);
        assertEquals(PLAYER_UUID, shielded.uuid());
    }

    @Test
    void delegatesUsername() {
        final Player shielded = new ShieldedPlayer(basePlayer(), EFFECTIVE_LAST_DAY);
        assertEquals("Alice", shielded.username());
    }

    @Test
    void delegatesExists() {
        final Player shielded = new ShieldedPlayer(basePlayer(), EFFECTIVE_LAST_DAY);
        assertTrue(shielded.exists());
    }
}
