package de.thomasuebel.lastactiveplayers;

import de.thomasuebel.lastactiveplayers.db.AddShieldsColumn;
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
import de.thomasuebel.lastactiveplayers.display.DateLabel;
import de.thomasuebel.lastactiveplayers.display.JoinMessage;
import de.thomasuebel.lastactiveplayers.display.LeaderboardJoinMessage;
import de.thomasuebel.lastactiveplayers.display.RelativeDateLabel;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardRankHint;
import de.thomasuebel.lastactiveplayers.ranking.RankHint;
import de.thomasuebel.lastactiveplayers.listener.AwardLifecycle;
import de.thomasuebel.lastactiveplayers.listener.HeartbeatRankHints;
import de.thomasuebel.lastactiveplayers.placeholder.AwardPlaceholders;
import de.thomasuebel.lastactiveplayers.listener.JoinBroadcast;
import de.thomasuebel.lastactiveplayers.listener.SessionLifecycle;
import de.thomasuebel.lastactiveplayers.ranking.OnlineRanks;
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
import org.bstats.bukkit.Metrics;
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
    private static final int BSTATS_PLUGIN_ID = 30553;
    private static final int DEFAULT_PURGE_DAYS = 60;
    private static final int DEFAULT_JOIN_DELAY_SECONDS = 10;
    // Stagger order: t+1*delay = milestone title+broadcast, t+2*delay = awards, t+3*delay = list.
    private static final long MILESTONE_BROADCAST_DELAY_MULTIPLIER = 1L;
    private static final long AWARD_BROADCAST_DELAY_MULTIPLIER = 2L;
    private static final long JOIN_BROADCAST_DELAY_MULTIPLIER = 3L;
    private static final int DEFAULT_MAX_SHIELDS = 3;
    private static final String MSG_RELOADED = "Configuration reloaded.";
    private static final String MSG_RELOAD_FAILED = "Reload failed: invalid display.date-format. "
        + "Check the server console for details.";

    private Database database;
    private Sessions sessions;
    private Players players;
    private ActiveSessions activeSessions;
    private Leaderboard mvpBoard;
    private StreakMilestones milestones;
    private BukkitTask heartbeatTask;
    private AwardLifecycle awardLifecycle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new Metrics(this, BSTATS_PLUGIN_ID);

        try {
            this.database = new SqliteDatabase(
                getDataFolder().toPath().resolve("lastactiveplayers.db"),
                new SqliteMigrations(new InitialSchema(), new AddShieldsColumn())
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
        if (this.awardLifecycle != null) {
            this.awardLifecycle.cleanup();
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
            "messages.streak-milestone", "[Fire] {player} has reached a {streak}-day login streak!"
        );
        final String milestoneTitleTemplate = getConfig().getString(
            "messages.streak-milestone-title", "{streak}-Day Streak!"
        );
        final String milestoneSubtitleTemplate = getConfig().getString(
            "messages.streak-milestone-subtitle", "A new milestone reached!"
        );
        final int maxShields = getConfig().getInt("streak.max-shields", DEFAULT_MAX_SHIELDS);
        final String shieldUsedTemplate = getConfig().getString(
            "messages.streak-shield-used",
            "[Shield] Streak protected! ({streak} days) Shields remaining: {shields_remaining}"
        );
        final String shieldEarnedTemplate = getConfig().getString(
            "messages.streak-shield-earned",
            "[Shield] You earned a streak shield! Total shields: {shields}"
        );
        final String mvpTemplate = getConfig().getString(
            "messages.mvp", "[Crown] Most active player (last 30 days): {player}"
        );
        final String mvpTieTemplate = getConfig().getString(
            "messages.mvp-tie",
            "[Crown] {players} are tied for MVP (last 30 days)!"
        );
        final String streakTemplate = getConfig().getString(
            "messages.streak", "[Fire] Longest daily login streak: {player} ({streak} days)"
        );
        final String streakTieTemplate = getConfig().getString(
            "messages.streak-tie",
            "[Fire] {players} are tied for longest daily login streak ({streak} days)!"
        );
        final String mvpPrefix = getConfig().getString("prefix.mvp", "[Crown] ");
        final String streakPrefix = getConfig().getString("prefix.streak", "[Fire] ");
        final int listSize = getConfig().getInt("display.list-size", DEFAULT_LIST_SIZE);
        final String rankHintTemplate = getConfig().getString(
            "messages.rank-hint",
            "You are rank #{rank}. {minutes} more minutes to reach #{next_rank}."
        );
        final String entryTemplate = getConfig().getString(
            "messages.join-entry",
            "Last Active: {n}. {player} was last seen {date} ({duration} last 30 days)"
        );
        final String dateLabelToday = getConfig().getString("messages.date-today", "today");
        final String dateLabelYesterday =
            getConfig().getString("messages.date-yesterday", "yesterday");
        final String dateLabelDaysAgo =
            getConfig().getString("messages.date-days-ago", "{days} days ago");
        final int joinDelaySeconds =
            getConfig().getInt("display.join-delay-seconds", DEFAULT_JOIN_DELAY_SECONDS);
        final long joinDelayTicks = (long) joinDelaySeconds * TICKS_PER_SECOND;

        getServer().getPluginManager().registerEvents(
            new SessionLifecycle(
                this.players, this.sessions, this.activeSessions,
                this.milestones, ZoneId.systemDefault(), milestoneTemplate,
                this, joinDelayTicks * MILESTONE_BROADCAST_DELAY_MULTIPLIER,
                milestoneTitleTemplate, milestoneSubtitleTemplate,
                maxShields, shieldUsedTemplate, shieldEarnedTemplate
            ),
            this
        );

        this.awardLifecycle = new AwardLifecycle(
            this.mvpBoard, this.players, this.milestones, this,
            mvpPrefix, streakPrefix, mvpTemplate, mvpTieTemplate, streakTemplate, streakTieTemplate,
            joinDelayTicks * AWARD_BROADCAST_DELAY_MULTIPLIER
        );
        getServer().getPluginManager().registerEvents(this.awardLifecycle, this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AwardPlaceholders(this.awardLifecycle, getDescription().getVersion()).register();
            getLogger().info("PlaceholderAPI found -- award placeholders registered.");
        }

        final OnlineRanks onlineRanks = new OnlineRanks(rankHintTemplate);
        final HeartbeatRankHints heartbeatRankHints = new HeartbeatRankHints(
            onlineRanks, this.mvpBoard, this
        );
        getServer().getPluginManager().registerEvents(heartbeatRankHints, this);
        // Seed currently online players so they receive rank-up notifications after a reload.
        // lastSnapshot is empty at this point; players are seeded as UNRANKED and will be
        // promoted to their real rank on the first heartbeat without a spurious notification.
        for (final Player online : getServer().getOnlinePlayers()) {
            onlineRanks.joined(online.getUniqueId(), java.util.List.of());
        }

        final Heartbeat heartbeat = new SessionHeartbeat(this.activeSessions, this.sessions);
        final long intervalTicks = heartbeatMinutes * TICKS_PER_MINUTE;
        this.heartbeatTask = new BukkitHeartbeat(heartbeat, () -> {
            this.awardLifecycle.broadcastIfChanged();
            heartbeatRankHints.pulse();
        }).runTaskTimer(this, intervalTicks, intervalTicks);

        final DateLabel dateLabel = new RelativeDateLabel(
            Clock.systemDefaultZone(), ZoneId.systemDefault(),
            dateLabelToday, dateLabelYesterday, dateLabelDaysAgo,
            dateFormatter.withZone(ZoneId.systemDefault())
        );
        final JoinMessage joinMessage = new LeaderboardJoinMessage(
            new SqliteLastLeaveLeaderboard(this.database, Clock.systemUTC(), THIRTY_DAYS),
            listSize, entryTemplate, dateLabel
        );
        final RankHint rankHint = new LeaderboardRankHint(this.mvpBoard, rankHintTemplate);
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
