# ADR-004: Streak Shields

## Status

Accepted

## Context

A consecutive daily login streak resets to zero if the player misses a single calendar day. This
is harsh for dedicated players who miss one day due to real-life obligations (travel, illness, etc.)
and creates frustration without a meaningful gameplay reason.

Several mitigation strategies were considered:

- **Grace window** -- extend the allowed gap to two missed days. Simple but unconditional; it
  dilutes the meaning of "daily" for all players.
- **Freeze streak on miss** -- stop the counter from growing but do not reset it. Does not
  reward consistent play any differently from a player who stopped logging in.
- **Consumable shield item** -- give the player a consumable in their inventory. Requires
  inventory management, conflicts with vanilla inventory rules, and is complex to implement.
- **Consumable permission-based shield** -- a lightweight counter stored in the database that
  the player earns and spends without interacting with the inventory system.

## Decision

Players earn streak shields: a small integer counter stored as `streak_shields` in the `players`
table. One shield is awarded each time the player reaches a new streak milestone (3, 7, 14, 30,
or 60 consecutive days). The counter is capped at a configurable maximum (`streak.max-shields`,
default 3).

When a player joins and exactly one calendar day was missed (gap = 2 days in epoch-day arithmetic)
and at least one shield is available, the shield is consumed automatically:

1. `streak_shields` is decremented by one.
2. A `ShieldedPlayer` decorator is used to compute the streak as if yesterday's date were the
   last login date, effectively bridging the gap.
3. A private message is sent to the player confirming the shield was used.

Only gaps of exactly one missed day are bridged. A two-day miss consumes no shield and resets
the streak. This preserves the intended meaning of "daily" while offering a safety net for
single-day absences.

### Shield award timing

Shield awards are evaluated after the streak update within the same `onJoin` handler. This means
a player who simultaneously crosses a milestone and consumes a shield ends up with a net balance
that correctly reflects both operations -- without additional database reads, because the shield
count is tracked in memory throughout the join flow.

### Notification on by default, suppressible via config

The award notification (`messages.streak-shield-earned`) is enabled by default. Operators who
prefer a quieter experience can set it to `""` to suppress it.

## Consequences

- Players with consistent play earn a buffer against single accidental misses.
- The mechanic is transparent: every shield gain and loss generates a private chat message.
- Shield state is durable: it persists across sessions in the database and survives server restarts.
- The cap prevents indefinite accumulation; long-term consistent players are not disadvantaged
  relative to returning players who amassed many shields while inactive.
- Only one DB read is needed per join for the shield counter (instead of one per operation),
  keeping the join-event handler efficient.
