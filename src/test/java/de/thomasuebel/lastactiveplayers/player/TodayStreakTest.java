package de.thomasuebel.lastactiveplayers.player;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TodayStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 1);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 3, 31);
    private static final LocalDate TWO_DAYS_AGO = LocalDate.of(2026, 3, 30);

    private static final int EXISTING_STREAK = 5;

    @Test
    void firstLoginStartsStreakAtOne() {
        final Player player = playerWith(0, Optional.empty());
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(1, streak.days());
    }

    @Test
    void firstLoginSetsLastDayToToday() {
        final Player player = playerWith(0, Optional.empty());
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(TODAY, streak.lastDay());
    }

    @Test
    void consecutiveDayExtendsStreak() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(YESTERDAY));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(EXISTING_STREAK + 1, streak.days());
    }

    @Test
    void consecutiveDaySetsLastDayToToday() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(YESTERDAY));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(TODAY, streak.lastDay());
    }

    @Test
    void sameDayLoginPreservesStreak() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(TODAY));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(EXISTING_STREAK, streak.days());
    }

    @Test
    void sameDayLoginKeepsLastDay() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(TODAY));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(TODAY, streak.lastDay());
    }

    @Test
    void lapsedStreakResetsToOne() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(TWO_DAYS_AGO));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(1, streak.days());
    }

    @Test
    void lapsedStreakSetsLastDayToToday() {
        final Player player = playerWith(EXISTING_STREAK, Optional.of(TWO_DAYS_AGO));
        final Streak streak = new TodayStreak(player, TODAY);
        assertEquals(TODAY, streak.lastDay());
    }

    private static Player playerWith(final int streakDays, final Optional<LocalDate> lastDay) {
        return new Player() {

            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public UUID uuid() {
                return UUID.randomUUID();
            }

            @Override
            public String username() {
                return "TestPlayer";
            }

            @Override
            public int streakDays() {
                return streakDays;
            }

            @Override
            public Optional<LocalDate> streakLastDay() {
                return lastDay;
            }
        };
    }
}
