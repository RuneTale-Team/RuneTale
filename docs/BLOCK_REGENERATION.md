# Block Regeneration Plugin

`BlockRegenerationPlugin` is a standalone RuneTale plugin that keeps configured gatherable blocks from disappearing permanently.

## Runtime and config paths

- Plugin JAR runtime root: `server/mods/`
- External config root: `server/mods/runetale/config/block-regeneration/`
- Main config file: `server/mods/runetale/config/block-regeneration/config/blocks.json`

Default config is seeded from `plugins/block-regeneration/src/main/resources/BlockRegen/config/blocks.json` when missing.

## Config model

Top-level fields:

- `version`
- `enabled`
- `respawnTickMillis`
- `notifyCooldownMillis`
- `definitions`

Each definition supports:

- `id`
- `enabled`
- `blockId` (supports `*` wildcard)
- `placeholderBlockId` (exact only)
- `gathering`:
  - `Specific` with `amount`
  - `Random` with `amountMin` and `amountMax`
- `respawn`:
  - `Set` with `millis`
  - `Random` with `millisMin` and `millisMax`

## Behavior

- Successful gather interactions increment per-position counters.
- If depletion threshold is not hit, source block is restored immediately.
- If threshold is hit, block changes to `placeholderBlockId` and waits for respawn.
- While waiting, damage/break attempts are blocked and players get a cooldowned notice.
- Respawn always force-restores source block when due.
- Runtime state resets on restart and is cleared on `/blockregen reload`.

## Commands

- `/blockregen reload`
- `/blockregen stats`
- `/blockregen inspect <x> <y> <z>`

## Manual in-game verification

1. Break configured node below threshold and confirm source restores.
2. Hit threshold and confirm placeholder appears.
3. Attempt to break placeholder and confirm blocked with notice.
4. Wait configured millis and confirm source force-restores.
5. Run `/blockregen stats` and verify counters update.
6. Run `/blockregen reload` and verify active waiting state is cleared.
