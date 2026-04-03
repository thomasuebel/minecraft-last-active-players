package de.thomasuebel.lastactiveplayers.ranking;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredNominationTest {

    private static final int SEVEN_DAYS = 7;

    @Test
    void existsReturnsTrue() {
        final Nomination nomination =
            new StoredNomination(UUID.randomUUID(), "Alice", SEVEN_DAYS);
        assertTrue(nomination.exists());
    }

    @Test
    void uuidReturnsConstructorValue() {
        final UUID uuid = UUID.randomUUID();
        final Nomination nomination = new StoredNomination(uuid, "Alice", SEVEN_DAYS);
        assertEquals(uuid, nomination.uuid());
    }

    @Test
    void usernameReturnsConstructorValue() {
        final Nomination nomination =
            new StoredNomination(UUID.randomUUID(), "Alice", SEVEN_DAYS);
        assertEquals("Alice", nomination.username());
    }

    @Test
    void streakDaysReturnsConstructorValue() {
        final Nomination nomination =
            new StoredNomination(UUID.randomUUID(), "Alice", SEVEN_DAYS);
        assertEquals(SEVEN_DAYS, nomination.streakDays());
    }
}
