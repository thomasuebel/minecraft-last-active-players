# Features

## Last Active Player List

On every player join, the server sends a private message listing the `display.list-size`
(default: 3) most recently offline players, including when they were last seen and their
rolling 30-day playtime. The same list is available on demand via `/lastactive`.

Dates within the last six days are shown as human-readable relative labels ("today",
"yesterday", "2 days ago", etc.) that operators can localise to any language. Older dates
fall back to a configurable date format.

## Playtime Rank Hint

On join, each player receives a private hint telling them their current rank in the
rolling 30-day playtime leaderboard and how many more minutes they need to reach the
next rank up. Players already ranked first see no hint.

## MVP (Most Valuable Player)

The player with the highest total playtime in the last 30 days is the MVP.

- Broadcast to all online players on each join.
- A configurable prefix (default: `[Crown] `) is exposed via PlaceholderAPI.
- Granted the `lastactiveplayers.mvp` permission node while they hold the title.

## Streak Leader

The player with the longest active consecutive daily login streak is the Streak Leader.

- Broadcast to all online players on each join.
- A configurable prefix (default: `[Fire] `) is exposed via PlaceholderAPI.
- Granted the `lastactiveplayers.streak.<days>` permission node for their highest
  milestone reached (3, 7, 14, 30, or 60 days).

## Streak Milestones

When a player's streak crosses a milestone threshold (3, 7, 14, 30, or 60 consecutive
calendar days), the server broadcasts the achievement to all online players and the
player sees a full-screen title. The matching permission node is granted immediately.

## Streak Shields

Players earn a shield each time they reach a new streak milestone, up to a configurable
maximum (default: 3). A shield is consumed automatically if a player misses a calendar
day, preserving their streak rather than resetting it.

## Session Tracking

Join and leave times are persisted to SQLite. Active session playtime is flushed to the
database on a configurable heartbeat interval (default: every 10 minutes), on clean
leave, and on server stop. On startup, any session without a leave timestamp is closed
at its last recorded heartbeat, recovering gracefully from crashes.

## Data Purge

Players with no session activity beyond a configurable threshold (default: 60 days) are
removed from the database on each server startup to keep storage lean.

## /lastactive Command

Available to all players. Displays the last active player list, current MVP, and current
Streak Leader. Subcommands:

| Subcommand | Access | Description |
|---|---|---|
| `help` | all | Show command usage |
| `mvp` | all | Show the current MVP |
| `streak` | all | Show the current Streak Leader |
| `reload` | op | Reload `config.yml` without restarting |
