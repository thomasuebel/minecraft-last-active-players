# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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
