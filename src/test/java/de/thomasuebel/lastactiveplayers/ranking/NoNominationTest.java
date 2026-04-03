package de.thomasuebel.lastactiveplayers.ranking;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NoNominationTest {

    @Test
    void existsReturnsFalse() {
        assertFalse(new NoNomination().exists());
    }

    @Test
    void uuidReturnsZeroUuid() {
        assertEquals(new UUID(0L, 0L), new NoNomination().uuid());
    }
}
