# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [1.0.4] - 2026-04-04

### Added
- Streak shields: players earn a shield on each newly reached streak milestone (3, 7, 14, 30,
  60 days), capped at `streak.max-shields` (default 3). A shield automatically bridges exactly
  one missed calendar day, keeping the streak alive.
- Configurable shield-earned notification (`messages.streak-shield-earned`, token `{shields}`).
  Set to `""` to suppress. Sent to the player whenever a milestone awards a shield.
- Configurable streak milestone full-screen title (`messages.streak-milestone-title`) and
  subtitle (`messages.streak-milestone-subtitle`) shown to the achieving player.
- `/lastactive reload` reloads `config.yml` without restarting the server (requires
  `lastactiveplayers.admin`).
- ADR-003: three-phase join message stagger timing rationale.
- ADR-004: streak shield design and alternatives.

### Fixed
- `SqliteMigrations.currentVersion()` leaked a `Statement` handle; both `Statement` and
  `ResultSet` are now closed in the same try-with-resources block.
- `SqlitePlaytimeLeaderboard.top(limit, exclude)` returned fewer than `limit` results when
  excluded (online) players appeared at the top of the SQL result; the limit is now applied
  after the exclusion check.
- Permission attachments held by `AwardLifecycle` were not removed from online players when
  `/lastactive reload` replaced the listener; `cleanup()` is now called before
  `HandlerList.unregisterAll`.
- Three database reads for shield state in `SessionLifecycle.onJoin` reduced to one; the
  shield count is tracked in memory through consume, award, and notify phases.

### Changed
- Default messages and prefixes use ASCII brackets (`[Crown]`, `[Fire]`, `[Shield]`) instead
  of emoji, for compatibility with all server environments. Emoji can be added by editing
  `config.yml`; comments show examples.
- `messages.streak-milestone-subtitle` default changed from "A new personal best!" to
  "A new milestone reached!" (accurate for re-climbed streaks).
- `streak.max-shields` comment in `config.yml` expanded to explain earning and bridging.
- `messages.rank-hint` comment clarifies that online players are excluded from the ranking.
- ADR-002 corrected: heartbeat flush runs synchronously on the main thread, not async.

## [1.0.3] - 2026-04-04

### Added
- Configurable join message delay (`display.join-delay-seconds`, default 10). The
  last-active list and rank hint are sent after the configured number of seconds so the
  message appears after join noise has settled. If the player disconnects before the
  delay expires no message is sent.

### Changed
- Bukkit framework classes (`LastActivePlayers`, all listeners, `BukkitHeartbeat`) are
  now excluded from JaCoCo coverage reports, mirroring the existing pitest exclusions.

## [1.0.2] - 2026-04-03

### Added
- Broadcast MVP and streak leader after every heartbeat flush and on player quit when
  the set of leaders has changed, so leadership changes during an active session are
  announced without waiting for the next join.
- Tie support: when two or more players share the top MVP playtime or the highest streak,
  all tied candidates receive the award permission, the display-name prefix, and a
  configurable tie broadcast message (`messages.mvp-tie`, `messages.streak-tie`).

### Fixed
- Change detection was permanently suppressed because the previous-snapshot reference
  held a live-query object that re-queried the database on every access; comparing it
  against a freshly built snapshot always returned "same leaders". Replaced with an
  immutable snapshot (`FrozenAwards`) that captures candidate lists at election time.

## [1.0.1] - 2026-04-03

### Fixed
- MVP and streak leader were not broadcast to the second (and subsequent) players who
  joined a session because the broadcast was gated on the leader UUID changing between
  joins. Removed the change-detection guard so both announcements fire unconditionally
  on every join.

## [1.0.0] - 2026-04-03

### Added
- Track player sessions via join/leave events with heartbeat-based duration accumulation
- Leaderboard showing last N active players on join and via `/lastactive`
- MVP election: player with most playtime in the rolling 30-day window
- Streak leader election: player with the longest consecutive daily login streak
- Streak milestone permission nodes at 3, 7, 14, 30, and 60 consecutive days
- Configurable display name prefixes for MVP and streak leader
- `/lastactive test` subcommand for operators to preview display state
- SQLite persistence with schema migrations and orphan session recovery on startup
- Startup purge of player records inactive beyond the configured threshold
- bStats integration (plugin ID 30553)
