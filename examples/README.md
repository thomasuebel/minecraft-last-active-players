# LastActivePlayers — Integration Examples

Drop-in configurations that wire LastActivePlayers award permissions into
common server plugins.

## Contents

| Path | Description |
|------|-------------|
| `essentialsx/kits.yml` | Kit definitions to paste into EssentialsX `config.yml` |
| `deluxemenus/awards_menu.yml` | DeluxeMenus GUI menu — players claim rewards via `/rewards` |

---

## EssentialsX kits

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

**MVP slot** — shows a gold block when the player holds
`lastactiveplayers.mvp`. Clicking claims `kit mvp-daily`. Shows a locked
placeholder otherwise.

**Streak slot** — shows the item and lore for the player's highest active
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
30-day streak holds `lastactiveplayers.streak.30` only — not the lower tiers.
