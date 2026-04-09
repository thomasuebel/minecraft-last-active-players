# LastActivePlayers — Integration Examples

Drop-in configurations that wire LastActivePlayers award permissions and
PlaceholderAPI placeholders into common server plugins.

## Contents

| Path | Description |
|------|-------------|
| `essentialsx/kits.yml` | Kit definitions to paste into EssentialsX `config.yml` |
| `deluxemenus/awards_menu.yml` | DeluxeMenus GUI menu — players claim rewards via `/rewards` |

---

## PlaceholderAPI integration

LastActivePlayers registers a PlaceholderAPI expansion automatically when
PlaceholderAPI is present on the server. No configuration is required on the
LastActivePlayers side.

### Requirements

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed
  and running on the same server
- LastActivePlayers 1.0.7+

### Available placeholders

| Placeholder | Returns | Example value |
|-------------|---------|---------------|
| `%lastactiveplayers_prefix%` | The configured award prefix for the player, or empty string | `[Crown] ` |
| `%lastactiveplayers_award%` | `mvp`, `streak`, or empty string | `mvp` |

Both placeholders reflect the **live in-memory award state** at the time of the
request. Award state is updated on every player join, on player quit, and after
each heartbeat flush (every N minutes, configurable). The values are always
current; no caching layer is involved.

For players who hold no active award both placeholders return an empty string,
so they are safe to embed directly in chat formats and menu lore without any
conditional logic.

### Prefix values

`%lastactiveplayers_prefix%` returns exactly the string configured in
`config.yml`:

- `prefix.mvp` (default `[Crown] `) for the current MVP
- `prefix.streak` (default `[Fire] `) for the current streak leader
- `""` for everyone else

If you customise the prefixes with colour codes (e.g. `&6[Crown]&r `) the
placeholder returns those codes verbatim. Add a reset code (`&r`) after the
placeholder in your chat format if you want to prevent colour bleed into the
player name that follows.

### What happens without PlaceholderAPI

If PlaceholderAPI is not installed the plugin starts normally and all core
features work as usual. The placeholders are simply not registered, and no
warning is logged. You can install PlaceholderAPI at any time and restart
(or reload) to activate them.

---

## TAB list prefix

**Requires:** [TAB plugin](https://www.spigotmc.org/resources/tab-list-and-name-tags.57806/)
1.4+, PlaceholderAPI

The award prefix does not appear in the Tab list automatically because Paper's
`displayName()` only affects chat and death messages. To show the prefix in the
Tab list and above players' heads, use the TAB plugin.

Open `plugins/TAB/config.yml` and add `%lastactiveplayers_prefix%` to the
tab name format:

```yaml
tablist-name-formatting:
  enabled: true
  default-value: "%lastactiveplayers_prefix%&f%player%"
```

Run `/tab reload`. The prefix resolves to an empty string for non-award-holders,
so only the MVP and streak leader see the prefix in the Tab list.

**Nameplates** (above-head display) can also use the placeholder if you enable
TAB's nametag feature:

```yaml
nametag:
  enabled: true
  prefix: "%lastactiveplayers_prefix%"
```

---

## Chat prefix

Paper 1.21's vanilla chat renderer uses `player.name()` (the profile username),
not `player.displayName()`. A lightweight chat formatter plugin is needed to
show the award prefix in chat messages.

LastActivePlayers sets both the legacy `player.setDisplayName()` and the
Adventure `player.displayName(Component)` on every award election, so any chat
formatter that supports `{displayname}` picks up the prefix automatically
regardless of which API it reads. Alternatively, the PlaceholderAPI placeholder
`%lastactiveplayers_prefix%` can be embedded directly in the chat format.

### LPC-Plus (recommended if you use LuckPerms)

**Requires:** [LPC-Plus](https://hangar.papermc.io/EmmaTheSigma/LPC-Plus),
[LuckPerms](https://luckperms.net/)

Open `plugins/LPCPlus/config.yml`:

Using displayName (picks up the prefix automatically):

```yaml
chat-format: "<{displayname}&r> {message}"
group-formats:
  default: "<{displayname}&r> {message}"
```

Using the PAPI placeholder (requires PlaceholderAPI):

```yaml
chat-format: "<%lastactiveplayers_prefix%{name}&r> {message}"
group-formats:
  default: "<%lastactiveplayers_prefix%{name}&r> {message}"
```

### EternalChatFormatter (no extra dependencies for the displayName path)

**Requires:** [EternalChatFormatter](https://hangar.papermc.io/EternalCodeTeam/EternalChatFormatter)

Open `plugins/ChatFormatter/config.yml`:

Using displayName:

```yaml
defaultFormat: "<{displayname}&r> {message}"
format:
  default: "<{displayname}&r> {message}"
placeholders:
  "{displayname}": "<displayname>"
  "{message}": "<message>"
```

Using the PAPI placeholder (requires PlaceholderAPI):

```yaml
defaultFormat: "<{mvpPrefix}{name}&r> {message}"
format:
  default: "<{mvpPrefix}{name}&r> {message}"
placeholders:
  "{mvpPrefix}": "%lastactiveplayers_prefix%"
  "{name}": "<name>"
  "{message}": "<message>"
```

### Why not built into LastActivePlayers?

Registering a custom `ChatRenderer` inside the plugin would conflict with any
chat formatting plugin the server operator already uses. `AsyncChatEvent`
carries a single renderer slot -- the last plugin to call `event.renderer()`
wins, so a built-in renderer would either override the operator's chat format
or be overridden and do nothing. The correct integration
pattern is to provide the data (displayName + PAPI placeholder) and let the
chat formatter render it.

---

## Holograms

**Requires:** [DecentHolograms](https://modrinth.com/plugin/decentholograms) or
[FancyHolograms](https://modrinth.com/plugin/fancyholograms), PlaceholderAPI

Any hologram plugin that supports PlaceholderAPI can display the current award
holder's prefix on a hologram line. For example, with DecentHolograms:

```
/dh create awards
/dh line add awards "%lastactiveplayers_prefix%Current MVP"
```

Use `%lastactiveplayers_award%` to conditionally show hologram lines (e.g. only
when a player is the MVP).

---

## EssentialsX kits

**File:** `essentialsx/kits.yml`

**Requires:** EssentialsX 2.20+

Open `plugins/Essentials/config.yml` and paste the contents of
`essentialsx/kits.yml` into the existing `kits:` section. Restart the server
or run `/ess reload`.

Six kits are defined:

| Kit | Cooldown | Tied to |
|-----|----------|---------|
| `mvp-daily` | 24 h | `lastactiveplayers.mvp` |
| `streak-3` | 24 h | `lastactiveplayers.streak.3` |
| `streak-7` | 24 h | `lastactiveplayers.streak.7` |
| `streak-14` | 24 h | `lastactiveplayers.streak.14` |
| `streak-30` | 24 h | `lastactiveplayers.streak.30` |
| `streak-60` | 24 h | `lastactiveplayers.streak.60` |

The kits carry no `essentials.kits.*` permission requirement. Access is
controlled by the DeluxeMenus menu (below), which runs the kit command as
console. EssentialsX still enforces the per-kit cooldown regardless of who
issues the command.

Customise the item lists and cooldown values freely.

---

## DeluxeMenus awards menu

**File:** `deluxemenus/awards_menu.yml`

**Requires:** DeluxeMenus 1.13+, PlaceholderAPI

Copy `deluxemenus/awards_menu.yml` to `plugins/DeluxeMenus/gui_menus/` and
run `/dm reload`. Players open the menu with `/rewards`.

The menu has two reward slots in a compact 27-slot layout:

```
[ ][ ][ ][ ][ ][ ][ ][ ][ ]
[ ][ ][M][ ][ ][S][ ][ ][ ]
[ ][ ][ ][ ][ ][ ][ ][ ][ ]

M = MVP reward slot
S = Streak reward slot
```

**MVP slot** -- shows a gold block when the player holds
`lastactiveplayers.mvp`. Clicking claims `kit mvp-daily`. Shows a locked
placeholder otherwise.

**Streak slot** -- shows the item and lore for the player's highest active
streak milestone (streak.60 down to streak.3, checked in priority order).
Shows a locked placeholder when no milestone is active. Because LastActivePlayers
only grants the highest milestone node, each player sees exactly one claimable
tier at a time.

The menu refreshes every second (`update_interval: 20`) so the display
updates promptly if a player's award status changes while the menu is open.

### Deny messages

If a player's award permission is revoked between opening the menu and
clicking (e.g. they were dethroned mid-session), they receive a private
chat message and the kit command is not issued.

### Adding economy rewards

To also grant in-game currency (EssentialsX Economy), add a console command
to `left_click_commands` for the relevant slot:

```yaml
left_click_commands:
  - '[console] kit mvp-daily %player_name%'
  - '[console] eco give %player_name% 500'
  - '[close]'
```

---

## How the permissions work

LastActivePlayers grants award permissions **in memory on join** and removes
them on leave or when the player is dethroned. They are never written to
LuckPerms or any other permissions store.

Because the permissions are transient, the DeluxeMenus `view_requirement` and
`left_click_requirement` checks are evaluated against the live in-memory state,
which is always current.

Only the highest streak milestone permission is held at any time. A player on a
30-day streak holds `lastactiveplayers.streak.30` only -- not the lower tiers.
