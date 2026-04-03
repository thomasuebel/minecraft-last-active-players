package de.thomasuebel.lastactiveplayers.display;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoRankHintTest {

    @Test
    void alwaysReturnsEmpty() {
        final RankHint hint = new NoRankHint();
        assertTrue(hint.text(UUID.randomUUID(), Set.of()).isEmpty());
    }
}
