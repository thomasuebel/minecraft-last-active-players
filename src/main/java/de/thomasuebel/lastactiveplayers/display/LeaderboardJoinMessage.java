package de.thomasuebel.lastactiveplayers.display;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link JoinMessage} backed by a {@link Leaderboard}, formatting each entry with a
 * configurable template string.
 */
public final class LeaderboardJoinMessage implements JoinMessage {

    private final Leaderboard leaderboard;
    private final int size;
    private final String template;
    private final DateLabel dateLabel;

    /**
     * Constructs a join message backed by the given leaderboard.
     *
     * @param leaderboard the leaderboard to query; never null
     * @param size        maximum number of entries to show; positive
     * @param template    entry template with {n} (1-based position in the filtered list),
     *                    {player}, {date}, {duration} tokens; never null
     * @param dateLabel   formats each entry's last-leave instant as a date string; never null
     */
    public LeaderboardJoinMessage(
        final Leaderboard leaderboard,
        final int size,
        final String template,
        final DateLabel dateLabel
    ) {
        this.leaderboard = leaderboard;
        this.size = size;
        this.template = template;
        this.dateLabel = dateLabel;
    }

    @Override
    public List<String> lines(final Set<UUID> exclude) {
        final List<LeaderboardEntry> entries = this.leaderboard.top(this.size, exclude);
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            result.add(new JoinEntryLine(
                entries.get(i), i + 1, this.template, this.dateLabel
            ).text());
        }
        return Collections.unmodifiableList(result);
    }
}
