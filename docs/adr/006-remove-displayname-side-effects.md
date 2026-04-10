# ADR-006: Remove displayName Side-Effects

## Status

Accepted

## Context

AwardLifecycle called both `player.setDisplayName()` (legacy) and `player.displayName(Component)`
(Adventure) to prepend the award prefix (crown/fire emoji) to the player's display name. The intent
was for chat messages to show the prefix automatically.

Two problems emerged on Paper 1.21:

1. **Paper's vanilla chat renderer ignores `displayName()`.** It uses `player.name()` (the profile
   username), so the prefix never appeared in chat without a separate chat formatting plugin.

2. **Paper uses `displayName()` for join/leave messages.** The prefix leaked into join and leave
   broadcasts, which is not the plugin's responsibility. The emoji rendered as an invisible character
   in the server console, leaving a confusing leading space before the username.

Setting `displayName()` also conflicts with chat formatting plugins. If a chat formatter reads
`displayName()`, the plugin's prefix competes with whatever format the server operator configured.
The plugin should provide data, not render it.

## Decision

Remove all `setDisplayName()` and `displayName(Component)` calls from AwardLifecycle. Provide the
award prefix exclusively through PlaceholderAPI (`%lastactiveplayers_prefix%`). Chat formatters,
TAB, holograms, and other rendering plugins consume the PAPI placeholder.

## Consequences

- **Join/leave messages** no longer contain the award prefix. This is the intended behavior.
- **Chat prefix** requires a chat formatting plugin (e.g. EternalChatFormatter, LPC-Plus) with
  `%lastactiveplayers_prefix%` in the format string.
- **PlaceholderAPI** becomes a soft requirement for any visible prefix rendering. The plugin still
  functions without it, but the prefix is not displayed anywhere.
- **TAB list, holograms, menus** continue to work unchanged via the same PAPI placeholder.
