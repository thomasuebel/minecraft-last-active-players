# LastActivePlayers

[![CI](https://github.com/thomasuebel/minecraft-last-active-players/actions/workflows/ci.yml/badge.svg)](https://github.com/thomasuebel/minecraft-last-active-players/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/thomasuebel/minecraft-last-active-players/graph/badge.svg)](https://codecov.io/gh/thomasuebel/minecraft-last-active-players)

A Paper plugin for Minecraft 1.21.x that tracks player sessions and celebrates the most engaged
members of your community.

## Features

- On join, lists the last N offline players with their last visit date and their playtime in the rolling 30-day window.
- Shows the joining player their current rank and how many minutes of playtime until the next rank.
- Broadcasts the **MVP** -- the player with the most total playtime in the last 30 days.
- Broadcasts the **Streak Leader** -- the player with the longest consecutive daily login streak.
- Grants permission nodes to the MVP and streak leaders at milestone thresholds (3, 7, 14, 30, 60
  days) so operators can hook into any rewards plugin.
- Exposes a configurable prefix (crown / fire emoji by default) for the MVP and streak leader
  via PlaceholderAPI for use in chat formatting, tab list, and hologram plugins.
- **Streak shields** -- players earn a shield on each streak milestone. A shield automatically
  bridges exactly one missed calendar day, keeping the streak alive.
- **Streak milestone broadcasts** -- a server-wide message and a personal full-screen title are
  shown when a player reaches a new streak milestone.
- `/lastactive` command available to all players.
- Reload configuration without restarting: `/lastactive reload` (ops only).
- bStats integration (plugin ID 30553).

## Requirements

- Paper 1.21.x
- Java 21

## Installation

1. Download the latest release JAR from the [releases page](https://github.com/thomasuebel/minecraft-last-active-players/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` and `lastactiveplayers.db` are created automatically in
   `plugins/LastActivePlayers/`.

## Documentation

Full documentation is available on the [wiki](https://github.com/thomasuebel/minecraft-last-active-players/wiki):

- [Configuration](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Configuration) -- all config keys with defaults and tokens
- [Commands and Permissions](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Commands-and-Permissions) -- `/lastactive` subcommands and permission nodes
- [PlaceholderAPI](https://github.com/thomasuebel/minecraft-last-active-players/wiki/PlaceholderAPI) -- available placeholders and use cases
- [Chat Prefix Setup](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Chat-Prefix-Setup) -- LPC-Plus, EternalChatFormatter, spacing tips
- [Tab List and Holograms](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Tab-List-and-Holograms) -- TAB plugin and hologram integration
- [Rewards Integration](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Rewards-Integration) -- EssentialsX kits, DeluxeMenus awards menu
- [Data and Backups](https://github.com/thomasuebel/minecraft-last-active-players/wiki/Data-and-Backups) -- SQLite storage, upgrading, troubleshooting

## Anonymous statistics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/LastActivePlayers/30553) to collect
anonymous usage statistics (server version, plugin version, player count ranges). No personal
data is collected. To opt out, set `enabled: false` in `plugins/bStats/config.yml`.

## License

[MIT](LICENSE)
