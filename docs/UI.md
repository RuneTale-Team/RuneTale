# Custom UI Findings (Skills Plugin)

## Key Findings

### 1) Resource pathing and packaging
- Skills page is loaded with:
  - Java: `append("SkillsPlugin/SkillsOverview.ui")`
  - Resource location in jar: `Common/UI/Custom/SkillsPlugin/SkillsOverview.ui`
- Confirmed by inspecting deployed jar:
  - `server/mods/skills-0.1.0.jar` contains:
    - `Common/UI/Custom/SkillsPlugin/SkillsOverview.ui`
- Conclusion:
  - Paths are resolved relative to `Common/UI/Custom/`.
  - Current page pathing is correct.

### 2) Decompiled folders are references, not direct runtime source
- `build/decompiled/Interface` and `build/decompiled/UI` are useful for patterns only.
- They confirm syntax, component usage, and visual conventions.
- They do not replace plugin-packaged assets/resources.

### 3) Custom UI element feasibility
- Fully feasible via composition:
  - `Group`, `Label`, `TextButton`, styles, templates (`@Something = Group { ... }`)
  - Runtime dynamic UI updates (`append`, `appendInline`, `clear`, `set`, event bindings)
- This allows building "custom components" without a separate formal component framework.

### 4) Icon support feasibility
- Icon-like UI is commonly done with:
  - `Background: "SomeIcon.png"` or `Background: (TexturePath: "...")`
- Observed in decompiled references:
  - Social/status/icons, server rows, ability icons, etc.
- Conclusion:
  - Texture-backed icons are feasible for Skills UI.
  - Placeholder icons can be implemented immediately.

### 5) Practical placeholder strategy (recommended)
- Phase 1 (no new files required):
  - Use text/shape placeholders (initials/glyphs) in dedicated icon slots.
- Phase 2 (ready for assets):
  - Add texture path hooks/placeholders in UI now.
  - Later drop PNG files under plugin resources and swap paths only.

## Recommended asset location for future icons
- Put icons under plugin resources, e.g.:
  - `Common/UI/Custom/SkillsPlugin/Assets/...`
- Reference from `SkillsOverview.ui` with local relative paths:
  - `Background: (TexturePath: "Assets/IconFishing.png")`

## Stability constraints to preserve
- Keep existing IDs/selectors used by Java event bindings.
- Keep click handling based on current `Index` flow.
- Do not reintroduce hover-driven event flow unless explicitly requested.
- Keep layout two-pane structure unchanged unless a design refactor is requested.