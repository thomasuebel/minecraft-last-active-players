# Last Active Players

A Minecraft Paper plugin (1.21.x, Java 21) that tracks join/leave events per player.

## Behaviour on Player Join

- Lists the last {n} active (offline) players with a configurable message template.
- Shows the joining player their rank in the list and a "(N more minutes to next rank)" hint.
- Broadcasts the MVP (most total playtime in a rolling 30-day window) with a configurable crown-style message.
- Broadcasts the Streak Leader (longest consecutive daily login streak) separately.

## MVP and Streak Rewards

- MVP and Streak Leader receive permission nodes (`lastactiveplayers.mvp`, `lastactiveplayers.streak.<milestone>`) so server operators can wire these into their own rewards plugin.
- Their display name gets a configurable prefix (default: crown emoji for MVP, fire emoji for streak leader).
- A server-op subcommand (`/lastactive test`) to preview how the display looks in-game.
- On join, all players are broadcast who the MVP and Streak Leader are.

### Streak Milestones

Streak leader status and permission node granted at: 3, 7, 14, 30, 60 consecutive calendar days (server timezone).
Permission node format: `lastactiveplayers.streak.7` (for a 7-day streak, etc.).
A milestone broadcast fires to all online players when a streak is newly reached.

## Configuration Options (config.yml)

| Key | Default | Description |
|-----|---------|-------------|
| `display.list-size` | `3` | Number of last-active players shown on join |
| `display.sort` | `playtime` | Sort mode: `playtime` (30-day total, desc) or `last_leave` (most recent first) |
| `display.date-format` | `yyyy-MM-dd` | Java DateTimeFormatter pattern for {date} |
| `messages.join-entry` | `"Last Active players: {n}. {player} was here on {date} for {duration}"` | Per-player line |
| `messages.mvp` | `"[Crown] Most active player (last 30 days): {player}"` | MVP broadcast |
| `messages.streak` | `"[Fire] Longest daily login streak: {player} ({streak} days)"` | Streak broadcast |
| `messages.rank-hint` | `"You are rank #{rank}. {minutes} more minutes to reach #{next_rank}."` | Private hint to joining player |
| `session.heartbeat-interval-minutes` | `10` | How often active session time is flushed to DB |
| `data.purge-inactive-days` | `60` | Days of inactivity before a player record is purged |
| `prefix.mvp` | `"[Crown] "` | Display name prefix for current MVP |
| `prefix.streak` | `"[Fire] "` | Display name prefix for streak leader |
| `streak.max-shields` | `3` | Maximum streak shields a player can hold |
| `messages.streak-milestone-title` | `"[Fire] {streak}-Day Streak!"` | Full-screen title when a milestone is crossed; tokens: {player}, {streak} |
| `messages.streak-milestone-subtitle` | `"A new personal best!"` | Subtitle alongside the milestone title; tokens: {player}, {streak} |
| `messages.streak-shield-used` | `"[Shield] Streak protected! ({streak} days) Shields remaining: {shields_remaining}"` | Private message when a shield is consumed; tokens: {streak}, {shields_remaining} |

## /lastactive Command

- Access: any player
- Behaviour: lists the {n} last active players (same as join message) plus current MVP and Streak Leader
- Subcommands (ops only):
  - `/lastactive test` -- preview MVP/streak display names in chat

## Persistence

- Engine: SQLite, stored at `plugins/LastActivePlayers/data.db`
- DB and schema created on first plugin enable if not present
- Heartbeat: active sessions flushed to DB every N minutes (configurable); final flush on leave and server stop
- Crash recovery: on startup, any session with no leave timestamp is closed at its last heartbeat timestamp
- Purge: players with no session activity beyond the configured threshold are purged on startup

## Schema (high level)

```
players  (uuid PK, username, streak_days, streak_last_day, streak_shields)
sessions (id PK, player_uuid FK, join_time, leave_time NULLABLE, last_heartbeat, duration_seconds)
```

## bStats Integration

Plugin ID: 30553

## Development Standards

- Build: Gradle (gradlew), Java 21 (sdkman)
- Commits: atomic, grouped by intent; review-fix commits are also atomic, one commit per finding addressed
- PRs: never squash; merge commit preserves the full commit history of the branch
- TDD: red then green then commit; tests written before behaviour
- Mutation testing: PIT (pitest) on every feature branch
- DI over mocking: dependency injection for testability; avoid Mockito for internal seams
- Branches and MRs: all features on branches, merged via pull request
- Code review: sub-agent acting as principal software engineer reviews every feature; spawn immediately after PR creation, before moving to next task
- ADRs: every architecture decision documented after Nygard in docs/adr/
- Linting: Checkstyle with Google Java Style; no magic numbers; no magic strings
- Coverage: 70-90% line coverage (JaCoCo)
- Documentation: README, CONTRIBUTING, ARCHITECTURE.md, MIT LICENSE
- Remote: git@github.com:thomasuebel/minecraft-last-active-players.git
- Default branch: master
- No trailing whitespace
- No em-dashes in code or comments

## Object Design (Elegant Objects by Yegor Bugayenko)

These rules govern every class in the codebase:

**Naming**
- Classes are named as nouns for what they ARE, never for what they DO.
- No -er/-or/-or suffix names: no Manager, Service, Helper, Formatter, Calculator, Processor, Handler, Controller, Util.
- Correct examples: `ActiveSessions` (not `SessionManager`), `HumanDuration` (not `DurationFormatter`), `FormattedMessage` (not `MessageFormatter`), `Leaderboard` (not `LeaderboardService`), `SqliteDatabase` (not `DatabaseManager`).

**Interfaces**
- Every public type is an interface. Implementations are named by what makes them specific (e.g. `SqliteSessions`, `CachedLeaderboard`).
- No implementation inheritance: `extends` is only used to implement interfaces, never to extend a concrete class.
- Prefer decorator composition over subclassing for extending behaviour.

**Constructors**
- Constructors only assign: `this.x = x`. No logic, no validation, no I/O.
- All fields are `final`. Objects are immutable by default; mutable state requires explicit justification.
- All dependencies injected via constructor only. No setters, no field injection.

**Methods**
- No getters or setters. Objects expose behaviour, not data.
- Methods return objects or primitives that represent results; they do not expose internal state.
- Aim for few public methods per class (ideally 3-5).

**Null**
- Never return null. Never pass null. Use the Null Object pattern.
- Null objects implement the same interface as real objects (e.g. `NoSession`, `UnknownPlayer`).

**Static**
- No static methods. No utility classes. Behaviour belongs to objects.
- Constants (`static final`) are permitted only as named values on interfaces or enums.
