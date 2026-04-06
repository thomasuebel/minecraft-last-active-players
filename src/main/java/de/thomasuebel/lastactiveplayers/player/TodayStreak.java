package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;

/**
 * The consecutive daily login streak for a player as of a given calendar date.
 *
 * <p>Computes the updated streak based on the player's last recorded streak day:
 * <ul>
 *   <li>No prior streak or lapsed (last day older than yesterday): resets to 1.</li>
 *   <li>Last day is yesterday: extends the streak by 1.</li>
 *   <li>Last day is today: preserves the existing streak (already counted).</li>
 * </ul>
 */
public final class TodayStreak implements Streak {

    private final PlayerRecord player;
    private final LocalDate today;

    /**
     * Constructs the streak for the given player as of today's date.
     *
     * @param player the player whose streak is being evaluated; never null
     * @param today  the calendar date of the current login; never null
     */
    public TodayStreak(final PlayerRecord player, final LocalDate today) {
        this.player = player;
        this.today = today;
    }

    @Override
    public int days() {
        final Optional<LocalDate> lastDay = this.player.streakLastDay();
        if (lastDay.isEmpty()) {
            return 1;
        }
        final LocalDate last = lastDay.get();
        if (last.equals(this.today)) {
            return this.player.streakDays();
        }
        if (last.equals(this.today.minusDays(1))) {
            return this.player.streakDays() + 1;
        }
        return 1;
    }

    @Override
    public LocalDate lastDay() {
        return this.today;
    }
}
