# LastActivePlayers

[![CI](https://github.com/thomasuebel/minecraft-last-active-players/actions/workflows/ci.yml/badge.svg)](https://github.com/thomasuebel/minecraft-last-active-players/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/thomasuebel/minecraft-last-active-players/graph/badge.svg)](https://codecov.io/gh/thomasuebel/minecraft-last-active-players)

A Paper plugin for Minecraft 1.21.x that tracks player sessions and celebrates the most engaged
members of your community.

## Features

- On join, lists the last N offline players with their last visit date and session duration.
- Shows the joining player their current rank and how many minutes of playtime until the next rank.
- Broadcasts the **MVP** -- the player with the most total playtime in the last 30 days.
- Broadcasts the **Streak Leader** -- the player with the longest consecutive daily login streak.
- Grants permission nodes to the MVP and streak leaders at milestone thresholds (3, 7, 14, 30, 60
  days) so operators can hook into any rewards plugin.
- Applies a configurable display name prefix (crown / fire emoji) to the MVP and streak leader.
- `/lastactive` command available to all players; admin subcommand `/lastactive test` for operators.
- bStats integration (plugin ID 30553).

## Requirements

- Paper 1.21.x
- Java 21

## Installation

1. Download the latest release JAR from the [releases page](https://github.com/thomasuebel/minecraft-last-active-players/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` and `lastactiveplayers.db` are created automatically in
   `plugins/LastActivePlayers/`.

## Configuration

Edit `plugins/LastActivePlayers/config.yml`. The file is created with defaults on first start.
Reload by restarting the server (the plugin does not support `/reload`).

### Display

| Key | Default | Description |
|-----|---------|-------------|
| `display.list-size` | `3` | Number of offline players listed on join and via `/lastactive` |
| `display.sort` | `playtime` | Sort order: `playtime` (30-day total, descending) or `last_leave` (most recent first) |
| `display.date-format` | `yyyy-MM-dd` | Date format for `{date}` token; any [Java DateTimeFormatter](https://docs.oracle.com/en/java/docs/api/java.base/java/time/format/DateTimeFormatter.html) pattern |
| `display.join-delay-seconds` | `10` | Seconds after joining before the first message is sent; `0` means next server tick |

### Messages

| Key | Available tokens | Description |
|-----|-----------------|-------------|
| `messages.join-entry` | `{n}`, `{player}`, `{date}`, `{duration}` | One line per player in the last-active list |
| `messages.mvp` | `{player}` | Broadcast when a single MVP is elected on join |
| `messages.mvp-tie` | `{players}` | Broadcast when two or more players are tied for MVP |
| `messages.streak` | `{player}`, `{streak}` | Broadcast when a single streak leader is elected on join |
| `messages.streak-tie` | `{players}`, `{streak}` | Broadcast when two or more players are tied for streak leader |
| `messages.rank-hint` | `{rank}`, `{next_rank}`, `{minutes}` | Private hint sent only to the joining player (only shown when `display.sort` is `playtime`) |
| `messages.streak-milestone` | `{player}`, `{streak}` | Broadcast to all players when a streak milestone (3/7/14/30/60 days) is newly reached |

### Prefixes

Display name prefixes are applied to the current MVP and streak leader while they are online.

| Key | Default | Description |
|-----|---------|-------------|
| `prefix.mvp` | `"👑 "` | Prepended to the MVP's display name |
| `prefix.streak` | `"🔥 "` | Prepended to the streak leader's display name |

### Session and data

| Key | Default | Description |
|-----|---------|-------------|
| `session.heartbeat-interval-minutes` | `10` | How often active session time is flushed to the database during a session |
| `data.purge-inactive-days` | `60` | Players with no session activity in this many days are removed from the database on startup |

## Commands

| Command | Who | Description |
|---------|-----|-------------|
| `/lastactive` | Everyone | Shows the last-active player list, current MVP, and streak leader |
| `/lastactive mvp` | Everyone | Shows the current MVP (or tied MVPs) |
| `/lastactive streak` | Everyone | Shows the current streak leader(s) |
| `/lastactive test` | Ops (`lastactiveplayers.admin`) | Previews how MVP and streak leader display names look in chat |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `lastactiveplayers.use` | everyone | Use `/lastactive` and its player subcommands |
| `lastactiveplayers.admin` | op | Use `/lastactive test` |
| `lastactiveplayers.mvp` | false | Dynamically granted to the current MVP(s) |
| `lastactiveplayers.streak.3` | false | Dynamically granted at a 3-day consecutive login streak |
| `lastactiveplayers.streak.7` | false | Dynamically granted at a 7-day streak |
| `lastactiveplayers.streak.14` | false | Dynamically granted at a 14-day streak |
| `lastactiveplayers.streak.30` | false | Dynamically granted at a 30-day streak |
| `lastactiveplayers.streak.60` | false | Dynamically granted at a 60-day streak |

Award permissions (`lastactiveplayers.mvp`, `lastactiveplayers.streak.*`) are in-memory only:
they are granted when the player joins and removed when they leave or are dethroned. They are
never written to your permissions plugin's storage.

## Integrating with a rewards plugin

The `lastactiveplayers.mvp` and `lastactiveplayers.streak.*` nodes are designed to be read by
any permission-aware rewards plugin. Because they are granted dynamically by LastActivePlayers
you do not need to assign them in LuckPerms or similar -- just reference them as conditions.

**Example: give the MVP a daily kit using DeluxeMenus / CommandsOnJoin**

```yaml
# In your rewards plugin, trigger a command when the joining player has the node:
permission: lastactiveplayers.mvp
command: give %player% diamond 5
```

**Example: LuckPerms meta (reading the streak tier)**

```
/lp group default permission set lastactiveplayers.streak.7 false
```

Then in your script or plugin check `player.hasPermission("lastactiveplayers.streak.7")`.

## Data and backups

Player and session data is stored in a SQLite database at
`plugins/LastActivePlayers/lastactiveplayers.db`. Include this file in your regular backup
routine. The `config.yml` in the same folder should also be backed up.

On startup the plugin automatically:
- Closes any sessions that were left open by a crash (using the last heartbeat timestamp).
- Purges player records that have been inactive for longer than `data.purge-inactive-days`.

## Upgrading

Drop the new JAR into `plugins/` replacing the old one and restart. Schema migrations run
automatically on startup -- no manual database changes are needed. The CHANGELOG documents
any breaking changes between versions.

## Troubleshooting

**The plugin disables itself on startup**

Check the server log for a `[LastActivePlayers] SEVERE` line. Common causes:

- `display.date-format` contains an invalid Java DateTimeFormatter pattern -- fix the value in
  `config.yml` and restart.
- The database file is locked by another process or the `plugins/LastActivePlayers/` folder is
  not writable -- check file permissions.

**MVP or streak leader is not being announced**

- Confirm at least one player has session data: run `/lastactive` and check whether the list is
  populated.
- If `display.sort` is set to `last_leave`, the rank hint is suppressed (it is only meaningful
  for playtime sorting); the MVP/streak broadcast is unaffected.

**Display name prefixes are not showing**

- The prefix is only applied while the award holder is online. It is reapplied on each join.
- Some chat plugins replace the display name independently. Check for conflicts with your
  chat formatting plugin.

## Anonymous statistics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/LastActivePlayers/30553) to collect
anonymous usage statistics (server version, plugin version, player count ranges). No personal
data is collected. To opt out, set `enabled: false` in `plugins/bStats/config.yml`.

## License

[MIT](LICENSE)
