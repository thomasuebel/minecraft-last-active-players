package de.thomasuebel.lastactiveplayers.stats;

import org.junit.jupiter.api.Test;

class NoStatisticsTest {

    @Test
    void registerIsNoOp() {
        final Statistics stats = new NoStatistics();
        stats.register();
    }
}
