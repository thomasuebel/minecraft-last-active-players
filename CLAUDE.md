# Last Active Players

A Minecraft Paper plugin (1.21.x, Java 21) that tracks join/leave events per player.

## Behaviour on Player Join

- Lists the last {n} active (offline) players sorted by most recent leave time, with a configurable message template.
- Shows the joining player their playtime rank and a "(N more minutes to next rank)" hint, based on the 30-day playtime leaderboard.
- Broadcasts the MVP (most total playtime in a rolling 30-day window) with a configurable crown-style message.
- Broadcasts the Streak Leader (longest consecutive daily login streak) separately.

## MVP and Streak Rewards

- MVP and Streak Leader receive permission nodes (`lastactiveplayers.mvp`, `lastactiveplayers.streak.<milestone>`) so server operators can wire these into their own rewards plugin.
- A configurable prefix (default: crown emoji for MVP, fire emoji for streak leader) is exposed via PlaceholderAPI.
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
| `display.date-format` | `yyyy-MM-dd` | Java DateTimeFormatter pattern for {date} fallback (dates older than 6 days) |
| `display.join-delay-seconds` | `10` | Stagger delay; milestones at 1x, MVP/streak at 2x, last-active list at 3x |
| `messages.join-entry` | `"Last Active: {n}. {player} was last seen {date} ({duration} last 30 days)"` | Per-player line; `{duration}` is rolling 30-day playtime |
| `messages.date-today` | `"today"` | {date} label when the player left today |
| `messages.date-yesterday` | `"yesterday"` | {date} label when the player left yesterday |
| `messages.date-days-ago` | `"{days} days ago"` | {date} label for 2-6 days ago; token: {days} |
| `messages.mvp` | `"[Crown] Most active player (last 30 days): {player}"` | MVP broadcast; token: {player} |
| `messages.mvp-tie` | `"[Crown] {players} are tied for MVP (last 30 days)!"` | Broadcast when multiple MVPs are tied; token: {players} |
| `messages.streak` | `"[Fire] Longest daily login streak: {player} ({streak} days)"` | Streak broadcast; tokens: {player}, {streak} |
| `messages.streak-tie` | `"[Fire] {players} are tied for longest daily login streak ({streak} days)!"` | Broadcast when multiple streak leaders tie; tokens: {players}, {streak} |
| `messages.rank-hint` | `"You are rank #{rank}. {minutes} more minutes to reach #{next_rank}."` | Private hint to joining player based on 30-day playtime rank |
| `messages.streak-milestone` | `"[Fire] {player} has reached a {streak}-day login streak!"` | Broadcast when a streak milestone is newly reached; tokens: {player}, {streak} |
| `messages.streak-milestone-title` | `"{streak}-Day Streak!"` | Full-screen title when a milestone is crossed; tokens: {player}, {streak} |
| `messages.streak-milestone-subtitle` | `"A new milestone reached!"` | Subtitle alongside the milestone title; tokens: {player}, {streak} |
| `messages.streak-shield-used` | `"[Shield] Streak protected! ({streak} days) Shields remaining: {shields_remaining}"` | Private message when a shield is consumed; tokens: {streak}, {shields_remaining} |
| `messages.streak-shield-earned` | `"[Shield] You earned a streak shield! Total shields: {shields}"` | Private message when shields are awarded at a milestone; token: {shields} |
| `prefix.mvp` | `"[Crown] "` | Display name prefix for current MVP |
| `prefix.streak` | `"[Fire] "` | Display name prefix for streak leader |
| `streak.max-shields` | `3` | Maximum streak shields a player can hold |
| `session.heartbeat-interval-minutes` | `10` | How often active session time is flushed to DB |
| `data.purge-inactive-days` | `60` | Days of inactivity before a player record is purged |

## /lastactive Command

- Access: any player
- Behaviour: lists the {n} last active players (same as join message) plus current MVP and Streak Leader
- Subcommands:
  - `/lastactive help` -- show command usage
  - `/lastactive mvp` -- show the current MVP (or tied MVPs)
  - `/lastactive streak` -- show the current streak leader(s)
- Subcommands (ops only):
  - `/lastactive test` -- preview MVP/streak display names in chat
  - `/lastactive reload` -- reload config.yml without restarting

## Persistence

- Engine: SQLite, stored at `plugins/LastActivePlayers/lastactiveplayers.db`
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
- Create an interface only when at least one of the following is true: (1) a second implementation
  exists or is concretely planned; (2) a test uses a substitutable fake or stub; (3) decorator
  composition is actually applied. See ADR-005.
- Implementations are named by what makes them specific (e.g. `SqliteSessions`, `CachedLeaderboard`).
- No implementation inheritance: `extends` is only used to implement interfaces, never to extend a concrete class.
- Prefer decorator composition over subclassing for extending behaviour.
- Data-carrying types with no substitutable behaviour use Java records instead of interface + implementation + null-object.

**Constructors**
- Constructors only assign: `this.x = x`. No logic, no validation, no I/O.
- All fields are `final`. Objects are immutable by default; mutable state requires explicit justification.
- All dependencies injected via constructor only. No setters, no field injection.

**Methods**
- No getters or setters. Objects expose behaviour, not data.
- Methods return objects or primitives that represent results; they do not expose internal state.
- Aim for few public methods per class (ideally 3-5).

**Null**
- Never return null. Never pass null.
- For look-up methods that may find nothing, return `Optional<T>`.
- Use the Null Object pattern only when the null-object participates in decorator composition or is
  used polymorphically in tests; otherwise prefer `Optional`.

**Static**
- No static methods. No utility classes. Behaviour belongs to objects.
- Constants (`static final`) are permitted only as named values on interfaces, enums, or records.
