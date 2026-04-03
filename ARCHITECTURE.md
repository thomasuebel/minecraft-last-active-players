# Architecture

## Overview

LastActivePlayers is a Paper plugin (Minecraft 1.21.x) structured around a small set of
collaborating objects. All design follows Elegant Objects principles: interfaces first, immutable
value objects, no static state, constructor injection throughout.

## Layers

```
+----------------------------+
|  Bukkit event listeners    |  PlayerJoinListener, PlayerQuitListener
|  Command executor          |  LastActiveCommand
+----------------------------+
            |
+----------------------------+
|  Domain objects            |  ActiveSessions, Leaderboard, Streak, ...
|  Value objects             |  HumanDuration, FormattedMessage, RankHint, ...
+----------------------------+
            |
+----------------------------+
|  Persistence               |  SqliteSessions, SqlitePlayers (implement interfaces)
|  Database                  |  SqliteDatabase (connection + migrations)
+----------------------------+
```

## Key interfaces and their role

| Interface | Responsibility |
|-----------|----------------|
| `Sessions` | Open, close, and query player sessions |
| `Players` | Upsert and query player records |
| `Leaderboard` | Ranked list of players by playtime within a window |
| `Streak` | Current consecutive daily login streak for a player |
| `ActiveSessions` | In-memory map of currently open sessions; snapshotted for heartbeat flush |

## Persistence

SQLite is used as the embedded database (see `docs/adr/001-sqlite-persistence.md`).
The schema is versioned; migrations run on plugin enable.

### Schema

```sql
CREATE TABLE players (
    uuid           TEXT PRIMARY KEY,
    username       TEXT NOT NULL,
    streak_days    INTEGER NOT NULL DEFAULT 0,
    streak_last_day TEXT
);

CREATE TABLE sessions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid      TEXT NOT NULL REFERENCES players(uuid),
    join_time        TEXT NOT NULL,
    leave_time       TEXT,
    last_heartbeat   TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0
);
```

## Session lifecycle

See `docs/adr/002-heartbeat-session-model.md` for full detail.

```
Player joins  --> open session row (leave_time NULL)
Every N min   --> heartbeat: update last_heartbeat + duration_seconds (async, batched transaction)
Player leaves --> final flush: set leave_time = NOW
Server stops  --> flush all open sessions (synchronous, onDisable)
Server starts --> orphan recovery: close sessions with NULL leave_time at last_heartbeat
```

## MVP and streak election

On each player join:

1. Query `sessions` for total `duration_seconds` per player in the last 30 days -- elect MVP.
2. Query `players` for `streak_days` -- elect Streak Leader.
3. Grant/revoke permission nodes via `PermissionAttachment`.
4. Apply display name prefix to elected players.
5. Build and send join messages to the joining player and broadcast MVP/streak messages to all.

## Configuration

A single `PluginConfig` object wraps Bukkit's `FileConfiguration`. It is injected into all
objects that need configuration values. Reloading replaces the `PluginConfig` instance.

## Thread model

- All Bukkit event callbacks run on the main thread.
- SQLite writes (heartbeat flush) run on an async `BukkitRunnable`.
- The async task takes a snapshot of the active session map on the main thread before writing.
- `onDisable` flushes synchronously on the main thread.
