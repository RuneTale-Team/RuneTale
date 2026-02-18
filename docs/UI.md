# Skills Custom UI Guide

## Purpose
This document explains how the Skills custom UI is wired, where assets live, and which implementation decisions were made so future changes stay safe and predictable.

## Runtime pathing model
- UI files are loaded from plugin resources under `Common/UI/Custom/`.
- Skills page is appended from Java with `append("SkillsPlugin/SkillsOverview.ui")`.
- Effective packaged path inside the plugin jar is `Common/UI/Custom/SkillsPlugin/SkillsOverview.ui`.
- Decompiled `build/decompiled/Interface` and `build/decompiled/UI` are references only, not runtime sources.

## Current UI structure
- Main layout: `plugins/skills/src/main/resources/Common/UI/Custom/SkillsPlugin/SkillsOverview.ui`
  - Left panel contains skill list (`#CommandList`).
  - Right panel contains cards (`#SubcommandCards`).
  - Both containers are scrollable via `LayoutMode: TopScrolling` + `$C.@DefaultScrollbarStyle`.
- Left list row template: `plugins/skills/src/main/resources/Common/UI/Custom/SkillsPlugin/SkillListItem.ui`
- Right card template: `plugins/skills/src/main/resources/Common/UI/Custom/SkillsPlugin/SkillSubmenuCard.ui`

## Java rendering entrypoint
- Page controller: `plugins/skills/src/main/java/org/runetale/skills/page/SkillsOverviewPage.java`
- Event model:
  - `Action=Back`
  - `Action=ToggleTrack`
  - `Index=<skill ordinal>` for list/card activation

## Decisions we made
- Keep two-pane layout (navigation + details).
- Keep interaction click-driven; no hover behavior.
- Selection indicator is visual-only in UI (no `>` prefix in label text).
- Left list always shows small skill icons.
- Right cards show icons on overview cards, but hide icons in detail view cards for readability.
- Missing icon files are acceptable (engine may show red-X placeholders).
- Node card display name uses optional `label` property; fallback is raw `id` verbatim.
- Right card rows include explicit horizontal spacer to prevent cards touching.
  - Row child order is `[left card, spacer, right card]`.

## Icon conventions
- Convention-based icon paths are generated from `SkillType`:
  - `icon_<skill>.png`
  - Example: `WOODCUTTING` -> `icon_woodcutting.png`
- Place icons in plugin resources:
  - `plugins/skills/src/main/resources/Common/UI/Custom/Assets/Icons/`
- Referenced runtime path format:
  - `Assets/Icons/icon_<skill>.png`

## Node definition labels
- Node files live in:
  - External runtime path: `server/mods/runetale/config/skills/Nodes/**/*.properties`
  - Bundled fallback path: `plugins/skills/src/main/resources/Skills/Nodes/**/*.properties`
- Optional display label key:
  - `label=Oak Tree`
- Fallback behavior when label is missing/blank:
  - UI displays `id` as-is.

## Add a new skill (checklist)
1. Add enum value in `plugins/skills/src/main/java/org/runetale/skills/domain/SkillType.java`.
2. Add skill gameplay data (XP profile, node definitions, etc.).
3. Add icon file named `icon_<skill>.png` under `Common/UI/Custom/Assets/Icons/`.
4. Ensure commands/systems include the new skill where needed.
5. Deploy and verify:
   - `./gradlew deployPluginsToRun`
   - open `/skills`
   - verify list icon, overview card icon, selection, and detail view rendering.

## Add or edit node cards (checklist)
1. Create/update node `.properties` file in `server/mods/runetale/config/skills/Nodes/<skill>/`.
2. Ensure `id` is present.
3. Add `label` if you want human-friendly UI text.
4. Ensure node file is included in `server/mods/runetale/config/skills/Nodes/index.list`.
5. Deploy and verify detail roadmap cards.

## Scroll behavior notes
- `#CommandList` and `#SubcommandCards` are scrollable.
- `KeepScrollPosition: true` is enabled on both containers.
- Detail cards are still intentionally capped by `MAX_ROADMAP_CARDS` in
  `plugins/skills/src/main/java/org/runetale/skills/page/SkillsOverviewPage.java`.

## Guardrails for future edits
- Do not rename existing selectors used by Java bindings unless Java is updated in lockstep.
- If changing right-card row composition, update selector indexing logic accordingly.
- Prefer adding new `.ui` templates over overloading one template with too many mode branches.
