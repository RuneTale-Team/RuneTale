# Skills Plugin Implementation Notes

The skills runtime now uses a split-plugin topology while preserving the same OSRS-inspired, data-driven progression behavior.

## Plugin topology

- `:plugins:skills-api` (`SkillsApiPlugin`): shared cross-plugin contracts (`SkillsRuntimeApi`, config/domain models).
- `:plugins:skills` (`SkillsPlugin`): progression core (profile component, XP dispatch/grant pipeline, runtime API provider).
- `:plugins:skills-combat` (`SkillsCombatPlugin`): combat XP routing (melee styles, ranged, block defence) and `/combatstyle` UI/command.
- `:plugins:skills-gathering` (`SkillsGatheringPlugin`): node lookup, gather block-break enforcement, `/skills` overview page command.
- `:plugins:skills-crafting` (`SkillsCraftingPlugin`): smithing/smelting pages, crafting XP dispatch, recipe unlock synchronization.
- `:plugins:skills-actions` (`SkillsActionsPlugin`): generic item-use action routing (`mouse input -> active hand consume -> XP dispatch`) for skills like Prayer/Firemaking.

## Config ownership

- Core-owned defaults: `skills.json`.
- Combat-owned defaults: `combat.json`.
- Gathering-owned defaults: `gathering.json` and `Skills/Nodes/nodes.json`.
- Crafting-owned defaults: `crafting.json`.
- Item-action-owned defaults: `item-actions.json`.

## Runtime flow

1. Plugin setup is split by responsibility: core wires progression services/systems and publishes `SkillsRuntimeApi`; combat wires combat systems/commands; gathering wires node assets and gather systems; crafting wires crafting systems and UI interactions.
2. A profile bootstrap system ensures every player has a persistent skill profile component.
3. `/skill` is a player-only self-inspection command that prints every declared skill with current level and XP.
4. Any runtime source can queue XP grants through `SkillXpDispatchService` (strict skill id parsing, no silent fallback).
5. `SkillXpGrantSystem` applies queued grants via `SkillProgressionService`.
6. On block hit (damage), then block break fallback:
    - Resolve the targeted block to a skill node definition.
    - Enforce skill-level requirements early on hit.
    - Reject unconfigured node-like blocks (candidate-token heuristic) on hit.
    - Apply a per-player notification cooldown to avoid spam while holding hit input.
7. On block break:
    - Resolve the broken block to a skill node definition.
    - Enforce skill-level requirements.
    - Reject unconfigured node-like blocks (candidate-token heuristic).
    - Queue XP grant through the shared progression pipeline.

## Core pieces

- `SkillType`: skill identity enum.
- `SkillsRuntimeApi`: stable cross-plugin progression surface used by gathering/crafting.
- `SkillsRuntimeRegistry`: runtime bridge registration for `SkillsRuntimeApi` provider discovery.
- `PlayerSkillProfileComponent`: persistent per-player map of skill progress.
- `SkillProgress`: per-skill XP + level state.
- `XpService`: XP thresholds and level calculation.
- `SkillXpDispatchService`: API/service entrypoint to enqueue XP grants from any source.
- `SkillProgressionService`: single source of truth for XP+level mutations.
- `SkillXpGrantSystem`: ECS event system that applies queued XP grants and handles feedback.
- `SkillNodeDefinition`: data model for node requirements/rewards.
- `SkillNodeLookupService`: lookup hooks + default node bootstrap.
- `ToolRequirementEvaluator`: keyword + tier validation from held `ItemStack`.
- `EnsurePlayerSkillProfileSystem`: auto-add missing profile components.
- `SkillNodeDamageBlockGateSystem`: early gather gate checks on block-hit interactions.
- `SkillNodeBreakBlockSystem`: break-block fallback gate checks and XP progression.
- `SkillCommand`: prints `SkillType.values()` as `level` + `xp` for the executing player.

## `/skill` command semantics

- Scope: self-only (`AbstractPlayerCommand`), no arguments.
- Help: `/skill help` (also supports `-h`, `--help`, `?`).
- Output: one header line plus one line per `SkillType` entry.
- Per-skill values:
  - Uses `PlayerSkillProfileComponent#getLevel(...)` and `getExperience(...)`.
  - Untrained skills preserve component defaults (`level=1`, `xp=0`).
- Defensive behavior:
  - If the profile component type is unavailable, the command returns a single unavailable message.
  - If the player profile component is unexpectedly missing, the command returns defaults for all skills.

## `/combatstyle` command semantics

- Scope: self-only (`AbstractPlayerCommand`).
- Help: `/combatstyle help` (also supports `-h`, `--help`, `?`).
- Modes: `accurate`, `aggressive`, `defensive`, `controlled` (plus legacy aliases `attack`, `strength`, `defence`).
- UI: `/combatstyle ui` opens a custom page selector.
- Query: `/combatstyle current` prints active mode and XP routing.

## `/skillxp` debug command semantics

- Scope: self-only (`AbstractPlayerCommand`), intended for debug/admin usage.
- Help: `/skillxp help` (also supports `-h`, `--help`, `?`).
- Required args:
  - `skill` (strictly parsed skill id, e.g. `MINING`)
  - `xp` (must be `> 0`)
- Optional args:
  - `source` (telemetry/source tag, defaults to `command:skillxp`)
  - `silent` flag (suppresses player XP/level notifications for the grant)
- Behavior:
  - Validates input.
  - Queues grant through `SkillsPlugin#grantSkillXp(...)`.
  - Confirms queueing in chat.

## Player feedback and logging

Skills gameplay outcomes now surface in player chat (`[Skills] ...`) for normal interactions:

- requirement failures (skill level)
- unconfigured node-like block warnings
- XP gain after successful gather
- level-up notifications

Server logs remain focused on setup/runtime diagnostics and unexpected safety paths (for example, missing profile component).

## Programmatic XP grants

- Use `SkillsPlugin#grantSkillXp(...)` or `SkillXpDispatchService#grantSkillXp(...)` from any system/command that has a `ComponentAccessor<EntityStore>` and player `Ref<EntityStore>`.
- Feature plugins should prefer `SkillsRuntimeRegistry.get()` + `SkillsRuntimeApi#grantSkillXp(...)` instead of direct core-plugin singleton access.
- Preferred inputs:
  - `skillId` (strict parse; unknown values are rejected),
  - `experience` (non-positive values are ignored),
  - `source` tag (for telemetry/logging),
  - `notifyPlayer` (whether player feedback is sent).
- Grant handling stays centralized in `SkillXpGrantSystem` -> `SkillProgressionService` to avoid duplicated XP logic.

## Notes / assumptions

- Node definitions are loaded external-first from `server/mods/runetale/config/skills/Nodes/nodes.json`, then classpath `src/main/resources/Skills/Nodes/nodes.json` as fallback; in-memory defaults remain fail-safe only.
- Unknown blocks remain a no-op path (fail-safe behavior).
- Runtime tuning now loads external-first from `server/mods/runetale/config/skills/Config/*.json`; core owns `skills.json`, combat owns `combat.json` (with legacy `skills.json -> combat` fallback/migration support), classpath resources remain defaults/fallbacks.

## Testing Guide

### 1) Local compile/run verification

```bash
./gradlew :plugins:skills-api:build :plugins:skills:clean :plugins:skills:build :plugins:skills-combat:build :plugins:skills-gathering:build :plugins:skills-crafting:build
```

- Confirms the plugin compiles and resource files under `src/main/resources/Skills/**` are packaged as fallback defaults.
- Expected outcome: build succeeds with no compile/runtime wiring errors.

### 2) Manual in-game flow (quick checklist)

1. Start the game/server with `SkillsPlugin`, `SkillsCombatPlugin`, `SkillsGatheringPlugin`, and `SkillsCraftingPlugin` enabled.
2. Try breaking a configured node block (from `Skills/Nodes/nodes.json`) with low skill level.
   Expected: break is cancelled and a warning is shown.
3. Use any held item and break again after meeting the level requirement.
    Expected: gather succeeds, XP is awarded, level recalculates from cumulative XP.
4. Try breaking an unconfigured but node-like block id (for example ids containing tokens in `Skills/Config/gathering.json` -> `heuristics.nodeCandidateTokens`).
   Expected: break is cancelled and a "not configured yet" warning is shown.
5. Repeat successful gathers until a level-up boundary is crossed.
    Expected: log shows level `before -> after` change.
6. Run `/skill` on a player with no prior gathering progress.
    Expected: every declared `SkillType` is listed with `level=1 xp=0`.
7. Gain XP in at least one skill, then run `/skill` again.
    Expected: that skill reflects updated `level/xp`, while untrained skills remain defaulted.

### 3) Debug logs to watch

- Break handling diagnostics:
  - `Break denied: ... requiredLevel=...`
  - warning for unconfigured node-like block ids.
- XP + level updates:
  - `Applied XP grant source=... skill=... gain=... totalXp=... level=...`
- Resource bootstrap:
  - `Node resource bootstrap completed with N definition(s)`
  - warnings for missing/empty index or node resources.

## Extension Guide

### Add a new node (resource-driven)

1. Create/update `src/main/resources/Skills/Nodes/nodes.json` and add your node under the appropriate skill-group array.
2. Keep keys aligned with current loader schema:
   - `id`, optional `skill` (falls back to group name), `blockIds` (preferred array) or `blockId` (backward-compatible fallback),
   - `requiredSkillLevel`, `requiredToolKeyword`, `requiredToolTier`,
   - `experienceReward`.
   - `blockIds` entries support `*` wildcard matching (for example `Ore_Copper_Surface_*`).
     Exact `blockId` matches always win over wildcard matches.
     If multiple wildcard patterns match, the first declared pattern is used and a warning is logged.
     For namespaced runtime IDs (for example `mymod:Ore_Copper_Surface_A`), lookup also tries the id suffix (`Ore_Copper_Surface_A`).
4. Rebuild and verify log line: `Loaded node resource=... id=... skill=...`.

### Add more nodes for an existing skill

- Repeat the same pattern with additional entries in `nodes.json`.
- Use distinct mapped block ids/patterns per node for deterministic lookup (via `blockIds` or `blockId`).
- Keep progression coherent by increasing `requiredSkillLevel` / `experienceReward` gradually.

### Add a new skill end-to-end

1. Add the identity to `SkillType`.
2. Author one or more grouped node entries in `Skills/Nodes/nodes.json` (use `skill=<NEW_SKILL>` when you need explicit override).
3. Ensure your content (blocks/skills) can satisfy the same runtime checks already used by break handling.
4. Rebuild and test requirement + XP paths exactly as in the Testing Guide.

Runtime wiring expectation: no new progression-core registration is required for basic gather-style skills as long as they fit the existing node schema and `SkillType` enum, and the gathering plugin is enabled.

### Add a timed crafting page (new crafting skill)

Use this pattern for skills like Cooking/Crafting/Fletching that need custom recipe selection, quantity controls, and timed craft progress.

1. Create a new page class under `org.runetale.skills.page` that extends `AbstractTimedCraftingPage<TEventData>`.
2. Reuse `TimedCraftingEventData` for the event payload class and codec (`TimedCraftingEventData.createCodec(...)`).
3. Provide page constants (`UI_PATH`, template selectors, bench id, and fixed craft duration).
4. In `renderPage(...)`, call shared helpers:
   - `CraftingPageSupport.syncQuantityControls(...)`
   - `CraftingPageSupport.configureOutputSlot(...)`
   - `CraftingPageSupport.configureIngredientSlots(...)`
   - `CraftingPageSupport.syncSelectedRecipePreview(...)`
5. Bind recipe selection rows/cards with `EventData.of("RecipeId", recipe.getId())`.
6. Use `CraftingPageSupport.updateCraftButtonState(...)` for consistent craft button behavior.
7. Implement `finishCraft(...)` by calling `CraftingPageSupport.executeCraft(...)`.
8. Register/open the new page from your interaction entrypoint (system/command).
9. Ensure the bench id and bench categories map to your recipe assets.
10. Confirm `CraftingPageProgressSystem` is registered (already done in `SkillsPlugin`).

#### Timed crafting implementation checklist

- [ ] Page extends `AbstractTimedCraftingPage`.
- [ ] Event data extends `TimedCraftingEventData`.
- [ ] UI has required selectors:
  - `#StartCraftingButton`
  - `#CraftProgressBar`, `#CraftProgressLabel`
  - quantity controls: `#Qty1`, `#Qty5`, `#Qty10`, `#QtyAll`, `#QtyCustomInput`, `#QtyCustomApply`
- [ ] Recipe list/grid emits `RecipeId` selection events.
- [ ] `renderPage(...)` drives tier state, quantity state, recipe collection, selected preview, craft button state.
- [ ] `finishCraft(...)` emits success notification/context tags.
- [ ] Build passes and in-game confirms: selection, x1/x5/x10/all/custom, timed queue, XP grant, lock/material states.

#### Template skeleton

```java
public class ExampleCraftingPage extends AbstractTimedCraftingPage<ExampleCraftingPage.EventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String UI_PATH = "SkillsPlugin/ExampleCrafting.ui";
    private static final long CRAFT_DURATION_MILLIS = 3000L;

    public ExampleCraftingPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull BlockPosition blockPosition,
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull CraftingRecipeTagService craftingRecipeTagService,
            @Nonnull CraftingPageTrackerService craftingPageTrackerService,
            @Nonnull CraftingConfig craftingConfig) {
        super(
                playerRef,
                blockPosition,
                runtimeApi,
                craftingRecipeTagService,
                craftingPageTrackerService,
                craftingConfig,
                UI_PATH,
                "example-crafting",
                "Crafting",
                CRAFT_DURATION_MILLIS,
                EventData.CODEC);
    }

    @Override
    protected boolean finishCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String recipeId) {
        return CraftingPageSupport.executeCraft(
                ref,
                store,
                recipeId,
                runtimeApi(),
                craftingRecipeTagService(),
                LOGGER,
                "Crafted",
                "example-craft");
    }

    @Override
    protected void renderPage(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder) {
        // Render tier state, quantity controls, recipe list, selected preview, and craft button state.
    }

    @Override
    protected @Nonnull HytaleLogger getLogger() {
        return LOGGER;
    }

    public static class EventData extends TimedCraftingEventData {
        public static final BuilderCodec<EventData> CODEC = TimedCraftingEventData.createCodec(EventData.class, EventData::new);
    }
}
```

### Tool tier / keyword mapping guidance

- Node definitions still support `requiredToolKeyword` and `requiredToolTier` fields for schema compatibility.
- Current gather break enforcement does not gate on held-tool keyword/tier.
- Tool family/tier logic remains available via `ToolRequirementEvaluator` and `Skills/Config/gathering.json` (`tooling` section) for other call sites.

### Caveats / staged behavior TODOs

- Node resources now parse `skill` strictly. Missing/invalid `skill` values are skipped with a warning instead of silently routing to a default skill.
- If node resources fail to load, in-memory fallback defaults are registered to keep runtime alive (use logs to detect this during testing).

## Acceptance checklist

- [ ] **Persistence through relog**
  - Verify `PlayerSkillProfileComponent` data survives disconnect/reconnect and preserves XP + level per skill.
  - Confirm no profile reset when plugin lifecycle stays within normal hot-reload boundaries.

- [ ] **Strict requirement enforcement**
  - Verify break is cancelled when player skill level is below node `requiredSkillLevel`.
  - Verify unconfigured node-like blocks are cancelled with warning feedback.

- [ ] **XP gain correctness**
  - Verify awarded XP equals node `experienceReward` (rounded by existing XP service behavior).
  - Verify updated level is computed from cumulative XP using OSRS-style thresholds.
  - Verify XP/level mutations are persisted back to the player profile component.

- [ ] **Extensibility for a second skill**
  - Add a second-skill node resource set (e.g., Mining) using the grouped `Skills/Nodes/nodes.json` schema.
  - Confirm loader accepts the second skill identity and systems process it without architectural changes.
