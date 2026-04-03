# ADR-002: Heartbeat Session Model for Crash Durability

## Status

Accepted

## Context

A player session begins on join and ends on leave or server stop. The naive approach -- write the
session only on leave -- loses the entire session if the server crashes. Sessions can last hours;
losing that data degrades the accuracy of playtime rankings and streak calculations.

Two goals must be balanced:

1. Durability: minimize data loss on crash.
2. Performance: do not block the server's main thread with I/O.

## Decision

Active sessions are flushed to the database on a configurable periodic heartbeat (default: every
10 minutes). Each flush updates `last_heartbeat` and accumulates `duration_seconds` for all active
sessions in a single transaction.

On server startup, any session row with a `NULL` leave_time is treated as an orphan (the result of
a crash). It is closed by setting `leave_time = last_heartbeat`. This bounds data loss to at most
one heartbeat interval.

### Performance design

**Async snapshot pattern** -- the heartbeat task (a `BukkitRunnable` scheduled async) must not
read mutable state from the main thread mid-tick. The sequence is:

1. Main thread: snapshot the in-memory active session map (a cheap copy; microseconds).
2. Async thread: write the snapshot to SQLite in a single batched transaction.

This means the main thread is blocked for microseconds per heartbeat, not for the duration of the
DB write.

**Single transaction per flush** -- all session updates for a given heartbeat are wrapped in one
`BEGIN`/`COMMIT`. SQLite performs one fsync per transaction (with `PRAGMA synchronous=NORMAL`),
not one per row. On a busy server with 100 concurrent players this is approximately 5ms of I/O,
off the main thread, every 10 minutes.

**WAL mode** (see ADR-001) -- concurrent reads (join-message queries, `/lastactive`) are not
blocked during the async heartbeat write.

### Clean shutdown

On `onDisable`, all active sessions are flushed synchronously on the main thread (the server is
stopping; no async tasks should be started). `leave_time` is set to `NOW` for all open sessions.

### Configurable interval

The heartbeat interval is configurable via `session.heartbeat-interval-minutes` (default: 10).
Lower values reduce potential data loss; higher values reduce I/O frequency. Operators on hardware
with slow disk may wish to increase this value.

## Consequences

- Maximum data loss on crash: one heartbeat interval (default 10 minutes).
- The heartbeat task adds a small, bounded async I/O load every N minutes.
- Session data is always consistent from the DB's perspective: `duration_seconds` is the ground
  truth, computed incrementally rather than derived from `join_time - leave_time`, which avoids
  clock-skew issues if the server clock is adjusted.
- Orphan recovery on startup means a restarted server produces correct rankings immediately,
  without manual intervention.
