package de.thomasuebel.lastactiveplayers;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.command.AwardPreviewLines;
import de.thomasuebel.lastactiveplayers.command.CommandLines;
import de.thomasuebel.lastactiveplayers.command.LastActiveCommand;
import de.thomasuebel.lastactiveplayers.command.LastActiveLines;
import de.thomasuebel.lastactiveplayers.display.JoinMessage;
import de.thomasuebel.lastactiveplayers.display.LeaderboardJoinMessage;
import de.thomasuebel.lastactiveplayers.display.LeaderboardRankHint;
import de.thomasuebel.lastactiveplayers.display.NoRankHint;
import de.thomasuebel.lastactiveplayers.display.RankHint;
import de.thomasuebel.lastactiveplayers.listener.AwardLifecycle;
import de.thomasuebel.lastactiveplayers.listener.JoinBroadcast;
import de.thomasuebel.lastactiveplayers.listener.SessionLifecycle;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.SqlitePlayers;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.SqliteLastLeaveLeaderboard;
import de.thomasuebel.lastactiveplayers.ranking.SqlitePlaytimeLeaderboard;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.BukkitHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Heartbeat;
import de.thomasuebel.lastactiveplayers.session.InMemoryActiveSessions;
import de.thomasuebel.lastactiveplayers.session.SessionHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.SqliteSessions;
import de.thomasuebel.lastactiveplayers.session.TrackedSession;
import de.thomasuebel.lastactiveplayers.stats.BStatsStatistics;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Main plugin class for LastActivePlayers.
 *
 * <p>Note: extending JavaPlugin is a framework-imposed constraint and the sole
 * exception to the Elegant Objects rule of no implementation inheritance.
 */
public final class LastActivePlayers extends JavaPlugin {

    /** Server ticks per minute: 20 ticks/s x 60 s. */
    private static final long TICKS_PER_MINUTE = 1200L;
    private static final int THIRTY_DAYS = 30;
    private static final int DEFAULT_LIST_SIZE = 3;
    private static final String SORT_PLAYTIME = "playtime";
    private static final int BSTATS_PLUGIN_ID = 30553;

    private Database database;
    private Sessions sessions;
    private ActiveSessions activeSessions;
    private BukkitTask heartbeatTask;

    @Override
    public void onEnable() {
        new BStatsStatistics(this, BSTATS_PLUGIN_ID);
        saveDefaultConfig();
        final long heartbeatMinutes =
            getConfig().getLong("session.heartbeat-interval-minutes", 10L);
        final String milestoneTemplate = getConfig().getString(
            "messages.streak-milestone", "\uD83D\uDD25 {player} has reached a {streak}-day streak!"
        );
        final String mvpTemplate = getConfig().getString(
            "messages.mvp", "\uD83D\uDC51 Most active player (last 30 days): {player}"
        );
        final String streakTemplate = getConfig().getString(
            "messages.streak", "\uD83D\uDD25 Longest daily login streak: {player} ({streak} days)"
        );
        final String mvpPrefix = getConfig().getString("prefix.mvp", "\uD83D\uDC51 ");
        final String streakPrefix = getConfig().getString("prefix.streak", "\uD83D\uDD25 ");
        final int listSize = getConfig().getInt("display.list-size", DEFAULT_LIST_SIZE);
        final String sortMode = getConfig().getString("display.sort", SORT_PLAYTIME);
        final String dateFormat = getConfig().getString("display.date-format", "yyyy-MM-dd");
        final String entryTemplate = getConfig().getString(
            "messages.join-entry",
            "Last Active: {n}. {player} was here on {date} for {duration}"
        );
        final String rankHintTemplate = getConfig().getString(
            "messages.rank-hint",
            "You are rank #{rank}. {minutes} more minutes to reach #{next_rank}."
        );

        final DateTimeFormatter dateFormatter;
        try {
            dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        } catch (final IllegalArgumentException exception) {
            getLogger().severe("Invalid display.date-format '" + dateFormat
                + "': " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.database = new SqliteDatabase(
                getDataFolder().toPath().resolve("lastactiveplayers.db"),
                new SqliteMigrations(new InitialSchema())
            );
        } catch (final IOException exception) {
            getLogger().severe("Failed to open database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final Players players = new SqlitePlayers(this.database);
        this.sessions = new SqliteSessions(this.database);

        this.sessions.closeOrphans(Instant.now());

        this.activeSessions = new InMemoryActiveSessions();
        final Heartbeat heartbeat = new SessionHeartbeat(this.activeSessions, this.sessions);
        final long intervalTicks = heartbeatMinutes * TICKS_PER_MINUTE;
        this.heartbeatTask = new BukkitHeartbeat(heartbeat)
            .runTaskTimer(this, intervalTicks, intervalTicks);

        final StreakMilestones milestones = new StreakMilestones();
        getServer().getPluginManager().registerEvents(
            new SessionLifecycle(
                players, this.sessions, this.activeSessions,
                milestones, ZoneId.systemDefault(), milestoneTemplate
            ),
            this
        );

        final Leaderboard mvpBoard = new SqlitePlaytimeLeaderboard(
            this.database, Clock.systemUTC(), THIRTY_DAYS
        );
        getServer().getPluginManager().registerEvents(
            new AwardLifecycle(
                mvpBoard, players, milestones, this,
                mvpPrefix, streakPrefix, mvpTemplate, streakTemplate
            ),
            this
        );

        final Leaderboard displayBoard = SORT_PLAYTIME.equals(sortMode)
            ? mvpBoard
            : new SqliteLastLeaveLeaderboard(this.database);
        final JoinMessage joinMessage = new LeaderboardJoinMessage(
            displayBoard, listSize, entryTemplate, dateFormatter, ZoneId.systemDefault()
        );
        // Rank hint uses minutes-of-playtime arithmetic; only meaningful for playtime sort.
        final RankHint rankHint = SORT_PLAYTIME.equals(sortMode)
            ? new LeaderboardRankHint(displayBoard, rankHintTemplate)
            : new NoRankHint();
        getServer().getPluginManager().registerEvents(
            new JoinBroadcast(joinMessage, rankHint),
            this
        );

        final CommandLines list = new LastActiveLines(
            joinMessage, mvpBoard, players, mvpTemplate, streakTemplate
        );
        final CommandLines preview = new AwardPreviewLines(
            mvpBoard, players, mvpPrefix, streakPrefix
        );
        final Supplier<Set<UUID>> online = () -> {
            final Set<UUID> uuids = new HashSet<>();
            for (final Player p : getServer().getOnlinePlayers()) {
                uuids.add(p.getUniqueId());
            }
            return uuids;
        };
        final PluginCommand lastActive = getCommand("lastactive");
        if (lastActive != null) {
            lastActive.setExecutor(new LastActiveCommand(list, preview, online));
        } else {
            getLogger().severe("/lastactive command not found in plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel();
        }
        if (this.activeSessions != null && this.sessions != null) {
            final Instant now = Instant.now();
            for (final TrackedSession tracked : this.activeSessions.all()) {
                try {
                    final long elapsed =
                        Math.max(0L, Duration.between(tracked.lastHeartbeat(), now).getSeconds());
                    this.sessions.heartbeat(tracked.sessionId(), now, elapsed);
                    this.sessions.close(tracked.sessionId(), now);
                } catch (final DatabaseException exception) {
                    getLogger().warning(
                        "Failed to close session " + tracked.sessionId()
                        + " on shutdown: " + exception.getMessage()
                    );
                }
            }
        }
        if (this.database != null) {
            try {
                this.database.close();
            } catch (final IOException exception) {
                getLogger().warning("Error closing database: " + exception.getMessage());
            }
        }
    }
}
