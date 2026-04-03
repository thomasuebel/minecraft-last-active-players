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
3. Restart the server. A `config.yml` and `data.db` are created automatically in
   `plugins/LastActivePlayers/`.

## Configuration

See `plugins/LastActivePlayers/config.yml` for all options. Key settings:

| Key | Default | Description |
|-----|---------|-------------|
| `display.list-size` | `3` | Players listed on join |
| `display.sort` | `playtime` | `playtime` or `last_leave` |
| `display.date-format` | `yyyy-MM-dd` | Date format for session entries |
| `session.heartbeat-interval-minutes` | `10` | Session flush interval |
| `data.purge-inactive-days` | `60` | Days before inactive player data is purged |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `lastactiveplayers.use` | everyone | Use `/lastactive` |
| `lastactiveplayers.admin` | op | Use `/lastactive test` |
| `lastactiveplayers.mvp` | false | Granted to current MVP |
| `lastactiveplayers.streak.3` | false | Granted at 3-day streak |
| `lastactiveplayers.streak.7` | false | Granted at 7-day streak |
| `lastactiveplayers.streak.14` | false | Granted at 14-day streak |
| `lastactiveplayers.streak.30` | false | Granted at 30-day streak |
| `lastactiveplayers.streak.60` | false | Granted at 60-day streak |

## License

[MIT](LICENSE)
