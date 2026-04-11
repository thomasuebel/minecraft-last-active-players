# ADR-007: Configurable Extra Award Permissions

## Status

Accepted

## Context

LastActivePlayers grants transient permission nodes (`lastactiveplayers.mvp`,
`lastactiveplayers.streak.<N>`) via Bukkit `PermissionAttachment` when a player
is elected MVP or streak leader. These permissions are used by DeluxeMenus to
gate a reward menu, and by PlaceholderAPI to expose award state.

The example DeluxeMenus configuration originally issued EssentialsX kit commands
as console (`[console] essentials:kit mvp-daily %player_name%`). However,
EssentialsX does not enforce its per-kit cooldown for console-issued commands,
treating them as admin actions. This allowed players to repeatedly claim kits by
reopening the menu.

Switching to `[player]` commands would let EssentialsX enforce cooldowns, but
the player needs the `essentials.kits.<kit>` permission to run the kit command.
That permission must come from somewhere, and granting it statically via
LuckPerms would let any player bypass the menu and claim the kit directly.

## Decision

Add a configurable `awards.*.extra-permissions` section to `config.yml`. When
the plugin attaches an award permission (e.g. `lastactiveplayers.mvp`), it also
attaches all extra permission nodes listed for that award on the same
`PermissionAttachment`. The extra permissions are transient: granted on
election, revoked on dethronement or quit, exactly like the base award
permissions.

The config structure is:

```yaml
awards:
  mvp:
    extra-permissions:
      - essentials.kits.mvp-daily
  streak:
    7:
      extra-permissions:
        - essentials.kits.streak-7
```

The default is empty lists, preserving backwards compatibility.

The extra permissions are represented as an `AwardPermissions` record (per
ADR-005: records for data-carrying types with no substitutable behaviour).

## Consequences

- The plugin remains agnostic to kits and rewards. Operators wire the
  permissions they need; the plugin just grants whatever nodes are listed.
- The DeluxeMenus example now uses `[player]` commands. EssentialsX sees the
  transient `essentials.kits.*` permission and enforces its own cooldown.
- Non-award-holders cannot claim kits even by typing the command directly,
  because the kit permission only exists while the award is held.
- Operators who do not configure extra permissions see no behaviour change.
