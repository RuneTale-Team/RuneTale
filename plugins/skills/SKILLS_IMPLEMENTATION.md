# Skills Plugin Implementation Notes

This plugin implements an OSRS-inspired, data-driven gathering/skills runtime for block-break interactions.

## Runtime flow

1. Plugin setup wires services, codecs, assets, components, then systems in deterministic order.
2. A profile bootstrap system ensures every player has a persistent skill profile component.
3. `/skill` is a player-only self-inspection command that prints every declared skill with current level and XP.
4. On block break:
   - Resolve the broken block to a skill node definition.
   - Enforce skill-level and held-tool requirements.
   - Prevent gathering if the node is currently depleted.
   - Award XP and recompute level using OSRS-style math.
   - Mark node depleted with timed respawn tracking when configured.

## Core pieces

- `SkillType`: skill identity enum.
- `PlayerSkillProfileComponent`: persistent per-player map of skill progress.
- `SkillProgress`: per-skill XP + level state.
- `OsrsXpService`: XP thresholds and level calculation.
- `SkillNodeDefinition`: data model for node requirements/rewards/depletion.
- `SkillNodeLookupService`: lookup hooks + default node bootstrap.
- `ToolRequirementEvaluator`: keyword + tier validation from held `ItemStack`.
- `SkillNodeRuntimeService`: in-memory depletion/respawn state map.
- `EnsurePlayerSkillProfileSystem`: auto-add missing profile components.
- `SkillNodeBreakBlockSystem`: main break-block rules and XP progression.
- `SkillCommand`: prints `SkillType.values()` as `level` + `xp` for the executing player.

## `/skill` command semantics

- Scope: self-only (`AbstractPlayerCommand`), no arguments.
- Output: one header line plus one line per `SkillType` entry.
- Per-skill values:
  - Uses `PlayerSkillProfileComponent#getLevel(...)` and `getExperience(...)`.
  - Untrained skills preserve component defaults (`level=1`, `xp=0`).
- Defensive behavior:
  - If the profile component type is unavailable, the command returns a single unavailable message.
  - If the player profile component is unexpectedly missing, the command returns defaults for all skills.

## Logging

Logs are intentionally included at `INFO`/`FINE`/`FINER` levels at setup and key runtime decision points:

- setup/registration lifecycle
- requirement pass/fail reasons
- lookup misses
- XP mutations and level transitions
- depletion/respawn transitions

## Notes / assumptions

- Node definitions are now loaded from classpath resources under `src/main/resources/Skills/Nodes/` via `index.list`; in-memory defaults remain as a fail-safe fallback only.
- World identity for depletion keys currently uses `world.getName()`.
- Unknown blocks remain a no-op path (fail-safe behavior).

## Testing Guide

### 1) Local compile/run verification

```bash
./gradlew :plugins:skills:clean :plugins:skills:build
```

- Confirms the plugin compiles and resource files under `src/main/resources/Skills/**` are packaged.
- Expected outcome: build succeeds with no compile/runtime wiring errors.

### 2) Manual in-game flow (quick checklist)

1. Start the game/server with this plugin enabled.
2. Try breaking a configured node block (from `Skills/Nodes/*.properties`) with:
   - no item,
   - wrong tool keyword,
   - low tool tier,
   - low skill level.
     Expected: break is cancelled for each failed requirement.
3. Use a valid tool/tier and break again.
   Expected: gather succeeds, XP is awarded, level recalculates from cumulative XP.
4. Repeat successful gathers until a level-up boundary is crossed.
   Expected: log shows level `before -> after` change.
5. Test depletion/respawn:
   - use a node with `depletes=true` and noticeable `depletionChance`,
   - trigger depletion,
   - retry immediately,
   - retry after `respawnSeconds`.
     Expected: denied while depleted, allowed after respawn window.
6. Run `/skill` on a player with no prior gathering progress.
   Expected: every declared `SkillType` is listed with `level=1 xp=0`.
7. Gain XP in at least one skill, then run `/skill` again.
   Expected: that skill reflects updated `level/xp`, while untrained skills remain defaulted.

### 3) Debug logs to watch

- Requirement failures/successes from break handling:
  - `Break denied: ... requiredLevel=...`
  - `Break denied: ... requiredKeyword=... requiredTier=...`
  - `Tool requirement check: item=... detected=... required=... success=...`
- XP + level updates:
  - `Gather success: ... xp=before->after level=before->after ...`
- Depletion lifecycle:
  - `Depletion roll: ... roll=... chance=... result=...`
  - `Node depleted: key=...`
  - `Node still depleted: key=...`
  - `Node respawned: key=...`
- Resource bootstrap:
  - `Node resource bootstrap completed with N definition(s)`
  - warnings for missing/empty index or node resources.

## Extension Guide

### Add a new node (resource-driven)

1. Create `src/main/resources/Skills/Nodes/<your_node>.properties` using existing files as a template.
2. Add that filename as a new line in `src/main/resources/Skills/Nodes/index.list`.
3. Keep keys aligned with current loader schema:
   - `id`, `skill`, `blockIds` (preferred, comma-separated) or `blockId` (backward-compatible fallback),
   - `requiredSkillLevel`, `requiredToolKeyword`, `requiredToolTier`,
   - `experienceReward`, `depletionChance`, `depletes`, `respawnSeconds`.
4. Rebuild and verify log line: `Loaded node resource=... id=... skill=...`.

### Add more nodes for an existing skill

- Repeat the same pattern with additional `*.properties` files.
- Use distinct mapped block ids per node for deterministic lookup (via `blockIds` or `blockId`).
- Keep progression coherent by increasing `requiredSkillLevel` / `requiredToolTier` / `experienceReward` gradually.

### Add a new skill end-to-end

1. Add the identity to `SkillType`.
2. Author one or more node resources with `skill=<NEW_SKILL>` and include them in `index.list`.
3. Ensure your content (blocks/tools) can satisfy the same runtime checks already used by break handling.
4. Rebuild and test requirement + XP + depletion paths exactly as in the Testing Guide.

Runtime wiring expectation: no new system/service registration is required for basic gather-style skills as long as they fit the existing node schema and `SkillType` enum.

### Tool tier / keyword mapping guidance

- Keep node `requiredToolKeyword` aligned with item-id fragments (e.g. `axe`) because tool family checks are substring-based.
- Keep node `requiredToolTier` within declared tiers in `Skills/tool-tier-defaults.properties` for authoring consistency.
- Current tier detection is code-based token matching (`bronze`, `iron`, `steel`, ... `crystal`) against held item id.

### Caveats / staged behavior TODOs

- `Skills/tool-tier-defaults.properties` is currently informational/logged metadata; runtime tool checks still rely on evaluator code paths.
- `SkillType.fromString(...)` falls back to `WOODCUTTING` on unknown values, so malformed `skill=` data can silently route to that skill.
- If node resources fail to load, in-memory fallback defaults are registered to keep runtime alive (use logs to detect this during testing).

## Acceptance checklist

- [ ] **Persistence through relog**
  - Verify `PlayerSkillProfileComponent` data survives disconnect/reconnect and preserves XP + level per skill.
  - Confirm no profile reset when plugin lifecycle stays within normal hot-reload boundaries.

- [ ] **Strict requirement enforcement**
  - Verify break is cancelled when player skill level is below node `requiredSkillLevel`.
  - Verify break is cancelled when held tool does not match `requiredToolKeyword`.
  - Verify break is cancelled when tool tier is below `requiredToolTier`.

- [ ] **XP gain correctness**
  - Verify awarded XP equals node `experienceReward` (rounded by existing XP service behavior).
  - Verify updated level is computed from cumulative XP using OSRS-style thresholds.
  - Verify XP/level mutations are persisted back to the player profile component.

- [ ] **Depletion/respawn behavior**
  - Verify depletion roll uses node `depletionChance` in `[0.0, 1.0]`.
  - Verify node enters depleted state only when roll succeeds and `depletes=true`.
  - Verify node becomes gatherable again after `respawnSeconds`.

- [ ] **Extensibility for a second skill**
  - Add a second-skill node resource set (e.g., Mining) using the same `Skills/Nodes/index.list` + properties schema.
  - Confirm loader accepts the second skill identity and systems process it without architectural changes.
