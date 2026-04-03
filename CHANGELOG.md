# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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
