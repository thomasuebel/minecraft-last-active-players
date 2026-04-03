# ADR-001: SQLite as Persistence Engine

## Status

Accepted

## Context

The plugin must persist player session data (join/leave times, playtime totals, streak counters)
across server restarts. The data must survive crashes with minimal loss, support efficient rolling
30-day queries, and require no external infrastructure beyond the plugin itself.

Candidates considered:

| Option | Pros | Cons |
|--------|------|------|
| YAML/JSON flat file | Zero deps, simple | No transactions, no queries, O(n) scans, corruption risk on crash |
| SQLite | Embedded, ACID, SQL queries, single file | Requires JDBC driver dependency |
| MySQL/MariaDB | Scalable, shared across servers | Requires external server, ops burden, wrong fit for single-server plugin |
| H2 | Embedded SQL, no native lib | Larger JAR, less battle-tested than SQLite |

## Decision

Use SQLite via `org.xerial:sqlite-jdbc`, stored at `plugins/LastActivePlayers/data.db`.

Key configuration applied at connection open:

- `PRAGMA journal_mode=WAL` -- allows concurrent reads during writes; no reader/writer blocking.
- `PRAGMA synchronous=NORMAL` -- safe with WAL; tolerates OS-level crash (not power loss), which
  is acceptable given the heartbeat recovery mechanism (ADR-002).
- `PRAGMA foreign_keys=ON` -- enforces referential integrity between players and sessions tables.

Schema migrations are applied via versioned SQL scripts on each plugin enable. A `schema_version`
table tracks the applied version so migrations are idempotent.

## Consequences

- The plugin ships with `sqlite-jdbc` relocated under `de.thomasuebel.lastactiveplayers.libs.sqlite`
  to avoid classpath conflicts with other plugins.
- The DB file is a single portable artifact; server admins can back it up with a simple file copy
  (safe with WAL mode while the server is running).
- SQLite is single-writer; the heartbeat task and leave/stop flush must coordinate on the same
  connection rather than a pool. A single connection with WAL is sufficient at any realistic
  Minecraft server scale.
- No MySQL/multi-server support. If the operator runs a network (BungeeCord/Velocity), they will
  need per-server instances; cross-server aggregation is out of scope for v1.
