package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.NoPlayer;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLifecycleMilestoneTitleTest {

    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final String PLAYER_NAME = "TestPlayer";
    private static final long DELAY_TICKS = 200L;

    /**
     * Players stub returning a player with a 2-day streak last updated yesterday.
     * On join, TodayStreak advances to 3, crossing the first milestone.
     */
    private static de.thomasuebel.lastactiveplayers.player.Players stubPlayers() {
        return new de.thomasuebel.lastactiveplayers.player.Players() {
            @Override
            public void upsert(final UUID uuid, final String username) { }

            @Override
            public void updateStreak(
                final UUID uuid,
                final int streakDays,
                final Optional<LocalDate> streakLastDay
            ) { }

            @Override
            public de.thomasuebel.lastactiveplayers.player.Player withUuid(final UUID uuid) {
                return new de.thomasuebel.lastactiveplayers.player.Player() {
                    @Override
                    public boolean exists() {
                        return true;
                    }
                    @Override
                    public UUID uuid() {
                        return uuid;
                    }
                    @Override
                    public String username() {
                        return PLAYER_NAME;
                    }
                    @Override
                    public int streakDays() {
                        return 2;
                    }
                    @Override
                    public Optional<LocalDate> streakLastDay() {
                        return Optional.of(LocalDate.now().minusDays(1));
                    }
                };
            }

            @Override
            public de.thomasuebel.lastactiveplayers.player.Player withHighestStreak() {
                return new NoPlayer();
            }

            @Override
            public List<de.thomasuebel.lastactiveplayers.player.Player> withTopStreak() {
                return List.of();
            }

            @Override
            public void purgeInactiveBefore(final Instant threshold) { }
        };
    }

    private static Sessions stubSessions() {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("open".equals(method.getName())) {
                return 1L;
            }
            if (method.getReturnType() == List.class) {
                return List.of();
            }
            return null;
        };
        return (Sessions) Proxy.newProxyInstance(
            Sessions.class.getClassLoader(),
            new Class<?>[]{Sessions.class},
            handler
        );
    }

    private static ActiveSessions stubActiveSessions() {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("stop".equals(method.getName())) {
                return Optional.empty();
            }
            if (method.getReturnType() == List.class) {
                return List.of();
            }
            return null;
        };
        return (ActiveSessions) Proxy.newProxyInstance(
            ActiveSessions.class.getClassLoader(),
            new Class<?>[]{ActiveSessions.class},
            handler
        );
    }

    /**
     * Builds a Bukkit Player proxy whose scheduler runs runnables immediately.
     * Title calls are captured as String[]{title, subtitle}, broadcasts as strings.
     *
     * @param online           value returned by {@code isOnline()} when the scheduled task runs
     * @param titlesCapture    receives one entry per {@code sendTitle} call
     * @param broadcastsCapture receives one entry per {@code broadcastMessage} call
     */
    private static org.bukkit.entity.Player stubPlayer(
        final boolean online,
        final List<String[]> titlesCapture,
        final List<String> broadcastsCapture
    ) {
        final InvocationHandler taskHandler = (proxy, method, args) -> null;
        final BukkitTask stubTask = (BukkitTask) Proxy.newProxyInstance(
            BukkitTask.class.getClassLoader(),
            new Class<?>[]{BukkitTask.class},
            taskHandler
        );

        final InvocationHandler schedulerHandler = (proxy, method, args) -> {
            if ("runTaskLater".equals(method.getName())
                && args != null && args.length == 3
                && args[1] instanceof Runnable) {
                ((Runnable) args[1]).run();
                return stubTask;
            }
            return null;
        };
        final BukkitScheduler schedulerProxy = (BukkitScheduler) Proxy.newProxyInstance(
            BukkitScheduler.class.getClassLoader(),
            new Class<?>[]{BukkitScheduler.class},
            schedulerHandler
        );

        final InvocationHandler serverHandler = (proxy, method, args) -> {
            if ("getScheduler".equals(method.getName())) {
                return schedulerProxy;
            }
            if ("broadcastMessage".equals(method.getName())
                && args != null && args.length == 1
                && args[0] instanceof String) {
                broadcastsCapture.add((String) args[0]);
                if (method.getReturnType() == int.class) {
                    return 0;
                }
                return null;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            return null;
        };
        final org.bukkit.Server serverProxy = (org.bukkit.Server) Proxy.newProxyInstance(
            org.bukkit.Server.class.getClassLoader(),
            new Class<?>[]{org.bukkit.Server.class},
            serverHandler
        );

        final InvocationHandler playerHandler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return PLAYER_UUID;
                case "getName": return PLAYER_NAME;
                case "getServer": return serverProxy;
                case "isOnline": return online;
                case "sendTitle":
                    if (args != null && args.length >= 2) {
                        titlesCapture.add(new String[]{(String) args[0], (String) args[1]});
                    }
                    return null;
                default:
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
            }
        };
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
            org.bukkit.entity.Player.class.getClassLoader(),
            new Class<?>[]{org.bukkit.entity.Player.class},
            playerHandler
        );
    }

    private static Plugin stubPlugin(final org.bukkit.Server server) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getServer".equals(method.getName())) {
                return server;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        };
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            handler
        );
    }

    @Test
    void sendsTitleAndSubtitleWithTokensSubstitutedWhenMilestoneCrossed() {
        final List<String[]> titles = new ArrayList<>();
        final List<String> broadcasts = new ArrayList<>();
        final org.bukkit.entity.Player player = stubPlayer(true, titles, broadcasts);
        final Plugin plugin = stubPlugin(player.getServer());

        final Milestones milestones = new StreakMilestones();
        final SessionLifecycle lifecycle = new SessionLifecycle(
            stubPlayers(), stubSessions(), stubActiveSessions(),
            milestones, ZoneId.systemDefault(),
            "Server: {player} hit a {streak}-day streak!",
            plugin, DELAY_TICKS,
            "{streak}-Day Streak!", "{player} did it!"
        );

        lifecycle.onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1, titles.size());
        assertEquals("3-Day Streak!", titles.get(0)[0]);
        assertEquals("TestPlayer did it!", titles.get(0)[1]);
    }

    @Test
    void skipsEmptyTitleTemplates() {
        final List<String[]> titles = new ArrayList<>();
        final List<String> broadcasts = new ArrayList<>();
        final org.bukkit.entity.Player player = stubPlayer(true, titles, broadcasts);
        final Plugin plugin = stubPlugin(player.getServer());

        final Milestones milestones = new StreakMilestones();
        final SessionLifecycle lifecycle = new SessionLifecycle(
            stubPlayers(), stubSessions(), stubActiveSessions(),
            milestones, ZoneId.systemDefault(),
            "Server: {player} hit a {streak}-day streak!",
            plugin, DELAY_TICKS,
            "", ""
        );

        lifecycle.onJoin(new PlayerJoinEvent(player, ""));

        assertTrue(titles.isEmpty());
    }

    @Test
    void broadcastsServerMessageAlongsideTitle() {
        final List<String[]> titles = new ArrayList<>();
        final List<String> broadcasts = new ArrayList<>();
        final org.bukkit.entity.Player player = stubPlayer(true, titles, broadcasts);
        final Plugin plugin = stubPlugin(player.getServer());

        final Milestones milestones = new StreakMilestones();
        final SessionLifecycle lifecycle = new SessionLifecycle(
            stubPlayers(), stubSessions(), stubActiveSessions(),
            milestones, ZoneId.systemDefault(),
            "Server: {player} hit a {streak}-day streak!",
            plugin, DELAY_TICKS,
            "{streak}-Day Streak!", ""
        );

        lifecycle.onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1, broadcasts.size());
        assertTrue(broadcasts.get(0).contains("TestPlayer"));
        assertTrue(broadcasts.get(0).contains("3"));
    }

    @Test
    void suppressesTitleWhenPlayerGoesOfflineBeforeDelay() {
        final List<String[]> titles = new ArrayList<>();
        final List<String> broadcasts = new ArrayList<>();
        final org.bukkit.entity.Player player = stubPlayer(false, titles, broadcasts);
        final Plugin plugin = stubPlugin(player.getServer());

        final Milestones milestones = new StreakMilestones();
        final SessionLifecycle lifecycle = new SessionLifecycle(
            stubPlayers(), stubSessions(), stubActiveSessions(),
            milestones, ZoneId.systemDefault(),
            "Server: {player} hit a {streak}-day streak!",
            plugin, DELAY_TICKS,
            "{streak}-Day Streak!", "You did it!"
        );

        lifecycle.onJoin(new PlayerJoinEvent(player, ""));

        assertTrue(titles.isEmpty());
        assertEquals(1, broadcasts.size());
    }
}
