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
import de.thomasuebel.lastactiveplayers.command.MvpLines;
import de.thomasuebel.lastactiveplayers.command.StreakLines;
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
import de.thomasuebel.lastactiveplayers.stats.Statistics;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    /** Server ticks per second. */
    private static final long TICKS_PER_SECOND = 20L;
    /** Server ticks per minute: 20 ticks/s x 60 s. */
    private static final long TICKS_PER_MINUTE = 1200L;
    private static final int THIRTY_DAYS = 30;
    private static final int DEFAULT_LIST_SIZE = 3;
    private static final String SORT_PLAYTIME = "playtime";
    private static final int BSTATS_PLUGIN_ID = 30553;
    private static final int DEFAULT_PURGE_DAYS = 60;
    private static final int DEFAULT_JOIN_DELAY_SECONDS = 10;
    // Stagger order: t+1*delay = milestone title, t+2*delay = awards, t+3*delay = list.
    private static final long AWARD_BROADCAST_DELAY_MULTIPLIER = 2L;
    private static final long JOIN_BROADCAST_DELAY_MULTIPLIER = 3L;
    private static final String MSG_RELOADED = "Configuration reloaded.";
    private static final String MSG_RELOAD_FAILED = "Reload failed: invalid display.date-format. "
        + "Check the server console for details.";

    private Statistics statistics;
    private Database database;
    private Sessions sessions;
    private Players players;
    private ActiveSessions activeSessions;
    private Leaderboard mvpBoard;
    private StreakMilestones milestones;
    private BukkitTask heartbeatTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.statistics = new BStatsStatistics(this, BSTATS_PLUGIN_ID);
        this.statistics.register();

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

        this.players = new SqlitePlayers(this.database);
        this.sessions = new SqliteSessions(this.database);
        this.sessions.closeOrphans(Instant.now());

        final int purgeInactiveDays =
            getConfig().getInt("data.purge-inactive-days", DEFAULT_PURGE_DAYS);
        try {
            this.players.purgeInactiveBefore(
                Instant.now().minus(purgeInactiveDays, ChronoUnit.DAYS)
            );
            getLogger().info(
                "Startup purge complete: removed players inactive for more than "
                + purgeInactiveDays + " days."
            );
        } catch (final DatabaseException exception) {
            getLogger().warning("Startup purge failed: " + exception.getMessage());
        }

        this.activeSessions = new InMemoryActiveSessions();
        this.mvpBoard = new SqlitePlaytimeLeaderboard(
            this.database, Clock.systemUTC(), THIRTY_DAYS
        );
        this.milestones = new StreakMilestones();

        final String dateFormat = getConfig().getString("display.date-format", "yyyy-MM-dd");
        final DateTimeFormatter dateFormatter;
        try {
            dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        } catch (final IllegalArgumentException exception) {
            getLogger().severe("Invalid display.date-format '" + dateFormat
                + "': " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configure(dateFormatter);
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

    /**
     * Reloads the plugin configuration without restarting the server.
     *
     * <p>Validates the new config before tearing down the existing setup. If validation
     * fails the existing listeners and tasks are left intact and an error is reported to
     * the sender.
     *
     * <p>There is a brief window between {@code HandlerList.unregisterAll} and the
     * {@code registerEvents} calls inside {@code configure()} during which a
     * {@code PlayerQuitEvent} will not be captured. Any in-progress session that ends in
     * this window will be recovered on next startup via the orphan-close logic. Reload is
     * a synchronous, near-instant operation so the window is negligible in practice.
     *
     * @param sender the command sender who triggered the reload; never null
     */
    void reload(final CommandSender sender) {
        reloadConfig();
        final String dateFormat = getConfig().getString("display.date-format", "yyyy-MM-dd");
        final DateTimeFormatter dateFormatter;
        try {
            dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        } catch (final IllegalArgumentException exception) {
            getLogger().severe("Reload aborted: invalid display.date-format '"
                + dateFormat + "': " + exception.getMessage());
            sender.sendMessage(MSG_RELOAD_FAILED);
            return;
        }
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel();
        }
        HandlerList.unregisterAll(this);
        if (configure(dateFormatter)) {
            sender.sendMessage(MSG_RELOADED);
        }
    }

    /**
     * Reads all config values and (re-)registers listeners, the heartbeat task, and the
     * command executor. Called on first enable and on each successful reload.
     *
     * @param dateFormatter pre-validated formatter for the {@code display.date-format} value
     * @return {@code true} if wiring succeeded; {@code false} if the plugin was disabled
     */
    private boolean configure(final DateTimeFormatter dateFormatter) {
        final long heartbeatMinutes =
            getConfig().getLong("session.heartbeat-interval-minutes", 10L);
        final String milestoneTemplate = getConfig().getString(
            "messages.streak-milestone", "\uD83D\uDD25 {player} has reached a {streak}-day streak!"
        );
        final String mvpTemplate = getConfig().getString(
            "messages.mvp", "\uD83D\uDC51 Most active player (last 30 days): {player}"
        );
        final String mvpTieTemplate = getConfig().getString(
            "messages.mvp-tie",
            "\uD83D\uDC51 {players} are tied for MVP (last 30 days)!"
        );
        final String streakTemplate = getConfig().getString(
            "messages.streak", "\uD83D\uDD25 Longest daily login streak: {player} ({streak} days)"
        );
        final String streakTieTemplate = getConfig().getString(
            "messages.streak-tie",
            "\uD83D\uDD25 {players} are tied for longest daily login streak ({streak} days)!"
        );
        final String mvpPrefix = getConfig().getString("prefix.mvp", "\uD83D\uDC51 ");
        final String streakPrefix = getConfig().getString("prefix.streak", "\uD83D\uDD25 ");
        final int listSize = getConfig().getInt("display.list-size", DEFAULT_LIST_SIZE);
        final String sortMode = getConfig().getString("display.sort", SORT_PLAYTIME);
        final String entryTemplate = getConfig().getString(
            "messages.join-entry",
            "Last Active: {n}. {player} was here on {date} for {duration}"
        );
        final String rankHintTemplate = getConfig().getString(
            "messages.rank-hint",
            "You are rank #{rank}. {minutes} more minutes to reach #{next_rank}."
        );
        final int joinDelaySeconds =
            getConfig().getInt("display.join-delay-seconds", DEFAULT_JOIN_DELAY_SECONDS);
        final long joinDelayTicks = (long) joinDelaySeconds * TICKS_PER_SECOND;
        final String milestoneTitleTemplate = getConfig().getString(
            "messages.streak-milestone-title", "\uD83D\uDD25 {streak}-Day Streak!"
        );
        final String milestoneSubtitleTemplate = getConfig().getString(
            "messages.streak-milestone-subtitle", "A new personal best!"
        );

        getServer().getPluginManager().registerEvents(
            new SessionLifecycle(
                this.players, this.sessions, this.activeSessions,
                this.milestones, ZoneId.systemDefault(), milestoneTemplate,
                this, joinDelayTicks,
                milestoneTitleTemplate, milestoneSubtitleTemplate
            ),
            this
        );

        final AwardLifecycle awardLifecycle = new AwardLifecycle(
            this.mvpBoard, this.players, this.milestones, this,
            mvpPrefix, streakPrefix, mvpTemplate, mvpTieTemplate, streakTemplate, streakTieTemplate,
            joinDelayTicks * AWARD_BROADCAST_DELAY_MULTIPLIER
        );
        getServer().getPluginManager().registerEvents(awardLifecycle, this);

        final Heartbeat heartbeat = new SessionHeartbeat(this.activeSessions, this.sessions);
        final long intervalTicks = heartbeatMinutes * TICKS_PER_MINUTE;
        this.heartbeatTask = new BukkitHeartbeat(heartbeat, awardLifecycle::broadcastIfChanged)
            .runTaskTimer(this, intervalTicks, intervalTicks);

        final Leaderboard displayBoard = SORT_PLAYTIME.equals(sortMode)
            ? this.mvpBoard
            : new SqliteLastLeaveLeaderboard(this.database);
        final JoinMessage joinMessage = new LeaderboardJoinMessage(
            displayBoard, listSize, entryTemplate, dateFormatter, ZoneId.systemDefault()
        );
        // Rank hint uses minutes-of-playtime arithmetic; only meaningful for playtime sort.
        final RankHint rankHint = SORT_PLAYTIME.equals(sortMode)
            ? new LeaderboardRankHint(displayBoard, rankHintTemplate)
            : new NoRankHint();
        getServer().getPluginManager().registerEvents(
            new JoinBroadcast(joinMessage, rankHint, this,
                joinDelayTicks * JOIN_BROADCAST_DELAY_MULTIPLIER),
            this
        );

        final CommandLines list = new LastActiveLines(
            joinMessage, this.mvpBoard, this.players, mvpTemplate, streakTemplate
        );
        final CommandLines mvpLines = new MvpLines(this.mvpBoard, mvpTemplate, mvpTieTemplate);
        final CommandLines streakLines = new StreakLines(
            this.players, streakTemplate, streakTieTemplate
        );
        final CommandLines preview = new AwardPreviewLines(
            this.mvpBoard, this.players, mvpPrefix, streakPrefix
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
            lastActive.setExecutor(
                new LastActiveCommand(list, mvpLines, streakLines, preview, this::reload, online)
            );
            return true;
        } else {
            getLogger().severe("/lastactive command not found in plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }
}
