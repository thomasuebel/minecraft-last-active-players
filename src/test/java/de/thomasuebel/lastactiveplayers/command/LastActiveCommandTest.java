package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastActiveCommandTest {

    private static final String PERM_ADMIN = "lastactiveplayers.admin";
    private static final long ONE_HOUR = 3600L;
    private static final int SEVEN_DAYS = 7;
    private static final String MVP_TEMPLATE = "MVP: {player}";
    private static final String MVP_TIE_TEMPLATE = "Tied MVPs: {players}";
    private static final String STREAK_TEMPLATE = "Streak: {player} ({streak} days)";
    private static final String STREAK_TIE_TEMPLATE = "Tied: {players} ({streak} days)";
    private static final String MVP_PREFIX = "[MVP] ";
    private static final String STREAK_PREFIX = "[Streak] ";

    private static CommandSender stubSender(
        final boolean admin, final List<String> captured
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("sendMessage".equals(method.getName())
                && args != null && args.length == 1
                && args[0] instanceof String) {
                captured.add((String) args[0]);
                return null;
            }
            if ("hasPermission".equals(method.getName())
                && args != null && args.length == 1) {
                return admin && PERM_ADMIN.equals(args[0]);
            }
            final Class<?> ret = method.getReturnType();
            if (ret == boolean.class) {
                return false;
            }
            return null;
        };
        return (CommandSender) Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[]{CommandSender.class},
            handler
        );
    }

    private static Command stubCommand() {
        return new Command("lastactive") {
            @Override
            public boolean execute(
                final CommandSender sender,
                final String commandLabel,
                final String[] args
            ) {
                return false;
            }
        };
    }

    private static LeaderboardEntry entry(final String username) {
        return new LeaderboardEntry() {
            @Override
            public UUID uuid() {
                return UUID.randomUUID();
            }
            @Override
            public String username() {
                return username;
            }
            @Override
            public long totalSeconds() {
                return ONE_HOUR;
            }
            @Override
            public Optional<Instant> lastLeave() {
                return Optional.empty();
            }
        };
    }

    private static Player player(final String username, final int streak) {
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
                return username;
            }
            @Override
            public int streakDays() {
                return streak;
            }
            @Override
            public Optional<LocalDate> streakLastDay() {
                return Optional.empty();
            }
        };
    }

    private static Player noPlayer() {
        return new Player() {
            @Override
            public boolean exists() {
                return false;
            }
            @Override
            public UUID uuid() {
                return new UUID(0L, 0L);
            }
            @Override
            public String username() {
                return "";
            }
            @Override
            public int streakDays() {
                return 0;
            }
            @Override
            public Optional<LocalDate> streakLastDay() {
                return Optional.empty();
            }
        };
    }

    private static Players stubPlayers(final Player highest, final List<Player> topStreak) {
        return new Players() {
            @Override
            public void upsert(final UUID uuid, final String username) {
            }
            @Override
            public void updateStreak(
                final UUID uuid, final int days, final Optional<LocalDate> last
            ) {
            }
            @Override
            public Player withUuid(final UUID uuid) {
                return noPlayer();
            }
            @Override
            public Player withHighestStreak() {
                return highest;
            }
            @Override
            public List<Player> withTopStreak() {
                return topStreak;
            }
            @Override
            public void purgeInactiveBefore(final Instant threshold) {
            }
            @Override
            public int shields(final UUID uuid) {
                return 0;
            }
            @Override
            public void storeShields(final UUID uuid, final int count) {
            }
        };
    }

    private static LastActiveCommand command(
        final List<LeaderboardEntry> topOne,
        final List<LeaderboardEntry> topTied,
        final Player highestStreak,
        final List<Player> topStreak
    ) {
        return new LastActiveCommand(
            exclude -> List.of(),
            (limit, exclude) -> limit == 1 ? topOne : topOne,
            stubPlayers(highestStreak, topStreak),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> { },
            Set::of
        );
    }

    private LastActiveCommand commandWithBoard(
        final List<LeaderboardEntry> topOne,
        final List<LeaderboardEntry> topTied
    ) {
        return new LastActiveCommand(
            exclude -> List.of(),
            new de.thomasuebel.lastactiveplayers.ranking.Leaderboard() {
                @Override
                public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
                    return topOne;
                }
                @Override
                public List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
                    return topTied;
                }
            },
            stubPlayers(noPlayer(), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> { },
            Set::of
        );
    }

    // --- Routing tests ---

    @Test
    void returnsHelpUsageForHelpSubcommand() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(List.of(), List.of(), noPlayer(), List.of())
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"help"});
        assertFalse(result);
        assertTrue(captured.isEmpty());
    }

    @Test
    void sendsPermissionDeniedForTestSubcommandWithoutPermission() {
        final List<String> captured = new ArrayList<>();
        command(List.of(), List.of(), noPlayer(), List.of())
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"test"});
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).contains("permission"));
    }

    @Test
    void reloadSubcommandInvokesReloadActionWithPermission() {
        final AtomicBoolean reloadCalled = new AtomicBoolean(false);
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            exclude -> List.of(),
            (limit, exclude) -> List.of(),
            stubPlayers(noPlayer(), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> reloadCalled.set(true),
            Set::of
        );
        cmd.onCommand(stubSender(true, captured), stubCommand(), "lastactive",
            new String[]{"reload"});
        assertTrue(reloadCalled.get());
        assertTrue(captured.isEmpty());
    }

    @Test
    void reloadSubcommandSendsPermissionDeniedWithoutPermission() {
        final AtomicBoolean reloadCalled = new AtomicBoolean(false);
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            exclude -> List.of(),
            (limit, exclude) -> List.of(),
            stubPlayers(noPlayer(), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> reloadCalled.set(true),
            Set::of
        );
        cmd.onCommand(stubSender(false, captured), stubCommand(), "lastactive",
            new String[]{"reload"});
        assertFalse(reloadCalled.get());
        assertTrue(captured.get(0).contains("permission"));
    }

    // --- MVP subcommand formatting tests ---

    @Test
    void mvpSubcommandShowsSingleMvp() {
        final List<String> captured = new ArrayList<>();
        commandWithBoard(List.of(), List.of(entry("Alice")))
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"mvp"});
        assertEquals(List.of("MVP: Alice"), captured);
    }

    @Test
    void mvpSubcommandShowsTiedMvps() {
        final List<String> captured = new ArrayList<>();
        commandWithBoard(List.of(), List.of(entry("Alice"), entry("Bob")))
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"mvp"});
        assertEquals(List.of("Tied MVPs: Alice, Bob"), captured);
    }

    @Test
    void mvpSubcommandEmptyWhenNoMvp() {
        final List<String> captured = new ArrayList<>();
        commandWithBoard(List.of(), List.of())
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"mvp"});
        assertTrue(captured.isEmpty());
    }

    // --- Streak subcommand formatting tests ---

    @Test
    void streakSubcommandShowsSingleLeader() {
        final List<String> captured = new ArrayList<>();
        command(List.of(), List.of(), noPlayer(),
            List.of(player("Alice", SEVEN_DAYS)))
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"streak"});
        assertEquals(List.of("Streak: Alice (7 days)"), captured);
    }

    @Test
    void streakSubcommandShowsTiedLeaders() {
        final List<String> captured = new ArrayList<>();
        command(List.of(), List.of(), noPlayer(),
            List.of(player("Alice", SEVEN_DAYS), player("Bob", SEVEN_DAYS)))
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"streak"});
        assertEquals(List.of("Tied: Alice, Bob (7 days)"), captured);
    }

    @Test
    void streakSubcommandEmptyWhenNoLeader() {
        final List<String> captured = new ArrayList<>();
        command(List.of(), List.of(), noPlayer(), List.of())
            .onCommand(stubSender(false, captured), stubCommand(), "lastactive",
                new String[]{"streak"});
        assertTrue(captured.isEmpty());
    }

    // --- Test (preview) subcommand formatting tests ---

    @Test
    void testSubcommandShowsMvpWithPrefix() {
        final List<String> captured = new ArrayList<>();
        command(List.of(entry("Alice")), List.of(), noPlayer(), List.of())
            .onCommand(stubSender(true, captured), stubCommand(), "lastactive",
                new String[]{"test"});
        assertEquals(List.of("[MVP] Alice"), captured);
    }

    @Test
    void testSubcommandShowsStreakWithPrefix() {
        final List<String> captured = new ArrayList<>();
        command(List.of(), List.of(),
            player("Bob", SEVEN_DAYS), List.of())
            .onCommand(stubSender(true, captured), stubCommand(), "lastactive",
                new String[]{"test"});
        assertEquals(List.of("[Streak] Bob (7 days)"), captured);
    }

    @Test
    void testSubcommandShowsBothWhenBothExist() {
        final List<String> captured = new ArrayList<>();
        command(List.of(entry("Alice")), List.of(),
            player("Bob", SEVEN_DAYS), List.of())
            .onCommand(stubSender(true, captured), stubCommand(), "lastactive",
                new String[]{"test"});
        assertEquals(2, captured.size());
        assertEquals("[MVP] Alice", captured.get(0));
        assertEquals("[Streak] Bob (7 days)", captured.get(1));
    }

    // --- Base command formatting tests ---

    @Test
    void baseCommandIncludesJoinListLines() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            exclude -> List.of("line1", "line2"),
            (limit, exclude) -> List.of(),
            stubPlayers(noPlayer(), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> { },
            Set::of
        );
        cmd.onCommand(stubSender(false, captured), stubCommand(), "lastactive", new String[0]);
        assertTrue(captured.contains("line1"));
        assertTrue(captured.contains("line2"));
    }

    @Test
    void baseCommandIncludesMvpLine() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            exclude -> List.of(),
            (limit, exclude) -> List.of(entry("Alice")),
            stubPlayers(noPlayer(), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> { },
            Set::of
        );
        cmd.onCommand(stubSender(false, captured), stubCommand(), "lastactive", new String[0]);
        assertTrue(captured.contains("MVP: Alice"));
    }

    @Test
    void baseCommandIncludesStreakLine() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            exclude -> List.of(),
            (limit, exclude) -> List.of(),
            stubPlayers(player("Bob", SEVEN_DAYS), List.of()),
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            MVP_PREFIX, STREAK_PREFIX,
            sender -> { },
            Set::of
        );
        cmd.onCommand(stubSender(false, captured), stubCommand(), "lastactive", new String[0]);
        assertTrue(captured.contains("Streak: Bob (7 days)"));
    }
}
