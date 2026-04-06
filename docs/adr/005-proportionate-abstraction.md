# ADR-005: Relax Elegant Objects Interface Rule to Proportionate Abstraction

## Status

Accepted

## Context

The codebase has been developed under the Elegant Objects discipline, which includes the rule
"every public type is an interface." Applied strictly, this produces an interface-plus-implementation
(plus null-object) triple for every domain noun, regardless of whether polymorphism is exercised.

A simplicity review of the 67-class production codebase (for a plugin whose core behaviour is
showing who logged in recently) found the following concrete problems caused by this rule:

1. **TrackedRanks** was extracted from `OnlineRanks` in a single commit whose message reads
   "extract TrackedRanks interface from OnlineRanks" — the motivation was rule compliance, not
   a new polymorphism need. One implementation, never substituted, no test uses a fake.

2. **Statistics / NoStatistics / BStatsStatistics** wrap `new Metrics(pluginId, this)` — a
   single constructor call made once in `onEnable()` — behind three files and an interface.
   `NoStatistics` is never constructed in production code; it exists only to satisfy the
   null-object half of the pattern.

3. **Nomination / StoredNomination / NoNomination** and **AwardSnapshot / FrozenAwards /
   NoAwards** are six classes representing "who is the current MVP and streak leader, possibly
   nobody." Two `List<>` fields in `AwardLifecycle` carry the same information.

4. **CommandLines** and its four implementations (`LastActiveLines`, `MvpLines`, `StreakLines`,
   `AwardPreviewLines`) each wrap between two and five lines of string formatting. Each has its
   own test file. The interface delivers no substitutability benefit: the command handler is
   never tested with a fake `CommandLines`.

5. **BukkitHeartbeat** (37 lines) wraps two method calls:
   `delegate.pulse(Instant.now()); afterPulse.run()`. An anonymous `BukkitRunnable` in
   `configure()` is two lines and eliminates `BukkitHeartbeat.java` and `Heartbeat.java`.

6. **LeaderboardEntry / StoredEntry**, **Session / StoredSession**, **Player / StoredPlayer /
   NoPlayer** are data-carrier triads. Every method on the interface returns a field value —
   the interface is a getter-set with a different prefix convention. Java records express the
   same contract in a single declaration; `Optional<T>` replaces null-objects for look-up
   return types.

The Elegant Objects rules were adopted in full and are documented throughout `CLAUDE.md`. They
have produced genuine value in a handful of places:

- `TodayStreak` + `ShieldedPlayer`: decorator pattern decouples shield logic cleanly.
- `DateLabel` / `RelativeDateLabel`: two real implementations exist (fixed-clock in tests).
- `Awards` / `AwardLifecycle`: `AwardPlaceholders` depends on the interface; a second
  implementation or a test double is plausible.
- `Leaderboard`: three implementations exist (`SqlitePlaytimeLeaderboard`,
  `SqliteLastLeaveLeaderboard`, `ListLeaderboard`).
- `Sessions`, `Players`, `ActiveSessions`: the db/session layer benefits from the abstraction
  boundary even with one implementation, because the integration tests need the real SQLite
  implementation and a fake is never needed.

The review finding is therefore not that Elegant Objects is wrong in all cases, but that the
"every public type is an interface" sub-rule has been applied uniformly at a scale where the
cost (navigation overhead, spurious files, false polymorphism expectations) outweighs the
benefit for the majority of types.

## Decision

Partially relax the "every public type is an interface" rule. The revised rule is:

> **Create an interface only when at least one of the following is true:**
> 1. A second implementation exists, or is concretely planned within the current release scope.
> 2. A test uses a substitutable fake or stub (not just the concrete class directly).
> 3. Decorator composition is actually applied (e.g. `ShieldedPlayer` wrapping `Player`).
>
> **Replace data-carrying interface/implementation/null-object triads with Java records.**
> Use `Optional<T>` as the return type of look-up methods instead of null-objects, unless the
> null-object is used as a decorator base (criterion 3 above).
>
> **All other Elegant Objects rules remain in force:** constructor-only injection, immutable
> fields, no static methods, no -er/-or names, no getters/setters, no null parameters or
> returns from non-lookup methods.

The practical consequence is that the following existing abstractions are **kept**:

| Interface | Reason |
|-----------|--------|
| `Leaderboard` | Three implementations |
| `DateLabel` | Two implementations; fixed-clock fake used in tests |
| `Awards` | Used by `AwardPlaceholders`; test-substitutable |
| `RankHint` | Used by `JoinBroadcast`; substitutable in tests |
| `Sessions`, `Players`, `ActiveSessions` | DB boundary abstraction |
| `Migration`, `Migrations` | Two migration impls; extension point for future migrations |
| `TodayStreak` + `ShieldedPlayer` decorator | Active decorator composition |
| `Milestones` | Used as a substitutable strategy |

The following abstractions are **removed** in the implementation plan below:

| Removed | Replaced by |
|---------|------------|
| `Statistics`, `NoStatistics`, `BStatsStatistics` | `new Metrics(this, id)` inline in `onEnable()` |
| `TrackedRanks` | `OnlineRanks` used directly in `HeartbeatRankHints` |
| `Heartbeat`, `BukkitHeartbeat` | Anonymous `BukkitRunnable` in `configure()`; `SessionHeartbeat` called directly |
| `CommandLines`, `LastActiveLines`, `MvpLines`, `StreakLines`, `AwardPreviewLines` | Private methods in `LastActiveCommand` |
| `Nomination`, `StoredNomination`, `NoNomination` | Java record `Nomination`; `Optional<Nomination>` at call sites |
| `AwardSnapshot`, `FrozenAwards`, `NoAwards` | Two `List<>` fields in `AwardLifecycle` |
| `LeaderboardEntry`, `StoredEntry` | Java record `LeaderboardEntry` |
| `Session`, `StoredSession` | Java record `Session` |
| `Player`, `StoredPlayer`, `NoPlayer` | Java record `PlayerRecord`; `Optional<PlayerRecord>` from look-up methods |

## Implementation plan

Changes are applied as atomic commits in TDD order. Each step is a red-green-commit cycle:
existing tests serve as the safety harness; where a class being removed owns tests, those tests
migrate to the absorbing class or are deleted if they only asserted behaviour of the removed
null-object.

### Phase 1 — Dead code and thin wrappers (lowest risk)

**Step 1.1 — Remove `Statistics` abstraction**
Delete `Statistics.java`, `BStatsStatistics.java`, `NoStatistics.java`,
`stats/NoStatisticsTest.java`. Replace the single call site in `onEnable()` with
`new Metrics(this, BSTATS_PLUGIN_ID)`. No test changes beyond deleting the stats test.

**Step 1.2 — Remove `TrackedRanks` interface**
Delete `TrackedRanks.java`. Change `HeartbeatRankHints` to depend on `OnlineRanks` directly.
Existing `OnlineRanksTest` is unchanged.

**Step 1.3 — Remove `Heartbeat` interface and `BukkitHeartbeat`**
Delete `Heartbeat.java` and `BukkitHeartbeat.java`. Replace the `BukkitHeartbeat` construction
in `configure()` with an anonymous `BukkitRunnable`. `SessionHeartbeat` becomes a concrete
class with no interface; its constructor and `pulse()` method are called directly.
`SessionHeartbeatTest` is unchanged.

### Phase 2 — Command package collapse

**Step 2.1 — Collapse `CommandLines` and all four implementations into `LastActiveCommand`**
Delete `CommandLines.java`, `LastActiveLines.java`, `MvpLines.java`, `StreakLines.java`,
`AwardPreviewLines.java`. Move the formatting logic into private methods of `LastActiveCommand`,
which receives the underlying dependencies (leaderboard, players, templates) directly.
Migrate the content of `AwardPreviewLinesTest`, `LastActiveLinesTest`, `MvpLinesTest`,
`StreakLinesTest` into `LastActiveCommandTest`. The five separate test files are deleted.

### Phase 3 — Nomination and AwardSnapshot simplification

**Step 3.1 — Replace `Nomination`/`StoredNomination`/`NoNomination` with a Java record**
Introduce `record Nomination(UUID uuid, String username, int streakDays)` (package-private
or public). Replace `NoNomination` usage with `Optional<Nomination>` at call sites, or with
an empty list — `AwardLifecycle` and `FrozenAwards` already work with `List<Nomination>`.
Delete `Nomination.java` (interface), `StoredNomination.java`, `NoNomination.java`,
`NoNominationTest.java`, `StoredNominationTest.java`. Update `FrozenAwardsTest`.

**Step 3.2 — Collapse `AwardSnapshot`/`FrozenAwards`/`NoAwards` into `AwardLifecycle`**
`AwardLifecycle` replaces `AtomicReference<AwardSnapshot>` with two plain fields:
`List<LeaderboardEntry> currentMvps` and `List<Nomination> currentStreakLeaders`,
both initialised to `List.of()`. The `sameLeaders` comparison (now in `FrozenAwards`)
becomes a private method. Delete `AwardSnapshot.java`, `FrozenAwards.java`, `NoAwards.java`,
`NoAwardsTest.java`, `FrozenAwardsTest.java`. Migrate any non-trivial assertions from
`FrozenAwardsTest` into a new `AwardLifecycleStateTest`.

### Phase 4 — Data carrier simplification

**Step 4.1 — Replace `LeaderboardEntry`/`StoredEntry` with a Java record**
Introduce `record LeaderboardEntry(UUID uuid, String username, long totalSeconds,
Optional<Instant> lastLeave)`. Delete `LeaderboardEntry.java` (interface) and
`StoredEntry.java`. All existing leaderboard tests compile against the record directly.

**Step 4.2 — Replace `Session`/`StoredSession` with a Java record**
Examine whether `Session` methods are pure accessors; if so, introduce a record and delete
the interface and implementation. `SqliteSessionsTest` and `InMemoryActiveSessionsTest`
serve as the safety harness.

**Step 4.3 — Replace `Player`/`StoredPlayer`/`NoPlayer` with a record and `Optional`**
Introduce `record PlayerRecord(UUID uuid, String username, int streakDays,
Optional<LocalDate> streakLastDay, int streakShields)`. Change look-up methods on `Players`
to return `Optional<PlayerRecord>`. Update all call sites to use `Optional.map()`/`orElse()`.
`NoPlayer` in `SessionLifecycleMilestoneTitleTest` and `SessionLifecycleShieldTest` is
replaced with a lambda or inline `Players` stub returning `Optional.empty()`. Delete
`Player.java`, `StoredPlayer.java`, `NoPlayer.java`.

## Consequences

**Gained:**
- Estimated reduction from 67 production files to approximately 42-44, a ~35% decrease.
- Fewer files to navigate; the implementation is directly reachable without interface lookup.
- No loss of functionality, testability, or the remaining Elegant Objects discipline.
- Java records provide equals/hashCode/toString for free; anonymous lambda implementations
  in tests (common workaround for `StoredEntry`, `StoredNomination`) are no longer needed.

**Lost:**
- Strict compliance with the "every public type is an interface" Elegant Objects rule.
  This is the intended trade-off.
- `NoPlayer` and `NoNomination` as named null-objects in tests. Replaced by `Optional.empty()`
  and inline stubs, which are less self-documenting but are idiomatic Java.

**Risks:**
- Phase 3 (AwardSnapshot collapse) touches `AwardLifecycle`, which is the most complex class
  in the codebase and is not directly unit-tested (it is a Bukkit listener). The `FrozenAwardsTest`
  and `NoAwardsTest` assertions must be migrated carefully to avoid losing coverage of the
  same-leaders comparison logic.
- Phase 4.3 (Player → record) requires changing every call site of `Players.withUuid()` from
  `player.exists()` checks to `Optional.isPresent()`. This is a large but mechanical change;
  the integration tests in `SqlitePlayersTest` cover it.

## Consequences for CLAUDE.md

The project instructions in `CLAUDE.md` will be updated to replace the current blanket
"every public type is an interface" rule with the proportionate version stated in this ADR.
The remaining Elegant Objects rules are unchanged.
