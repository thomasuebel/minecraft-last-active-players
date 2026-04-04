# ADR-003: Three-Phase Join Message Stagger

## Status

Accepted

## Context

When a player joins the server, three distinct message events are dispatched to online players:

1. Streak milestone broadcast + full-screen title (the joining player just crossed a new milestone).
2. MVP and Streak Leader election broadcast (who currently holds the awards).
3. Last-active player list (the last N offline players with playtime/date).

If all three fire at the same tick they overlap in chat, making them hard to read. The milestone
title in particular competes visually with the award broadcast if both appear simultaneously.

## Decision

Messages are staggered using Bukkit's `runTaskLater` at multiples of a single configurable base
delay (`display.join-delay-seconds`, default 10 s):

| Phase | Delay multiplier | Approximate time (default) |
|-------|-----------------|---------------------------|
| 1. Milestone broadcast + title | 1x | ~10 s after join |
| 2. MVP / Streak Leader election | 2x | ~20 s after join |
| 3. Last-active player list | 3x | ~30 s after join |

The milestone phase fires first because the title screen, if triggered, occupies the player's
full display for several seconds and should settle before the election broadcast arrives. Placing
the last-active list last keeps it closest to when the player is ready to interact.

Setting `display.join-delay-seconds: 0` collapses all three phases to the next server tick,
which is useful for testing or for servers that prefer immediate delivery.

## Consequences

- Chat messages arrive in a predictable, readable order after join noise has settled.
- The title screen and the election broadcast do not overlap.
- Operators can tune the cadence to match their server's join noise by adjusting a single value.
- All three phases share the same base delay unit, so changing `join-delay-seconds` shifts the
  entire sequence proportionally.
- If a player disconnects before a delayed message fires, `broadcastMessage` still delivers to
  all players who are online at dispatch time; the departed player simply no longer receives it,
  which is the desired behaviour.
