# StarterKit Plugin Design

## Context

The original StarterKit mod (by Serilum) and its Hybrid library dependency are no longer maintained. We need equivalent functionality: grant configurable starter gear to first-time players. This plugin will be built natively against the Hytale Server API with no Hybrid dependency, following RuneTale's existing plugin conventions.

## Requirements

- Grant a configurable set of items to players joining for the first time
- Track kit receipt persistently via ECS component (survives restarts)
- Config-file-only management (no admin commands)
- Single kit definition in JSON
- Items target a named container (hotbar, armour, utility, etc.); engine picks slot within container
- Plugin can be disabled via config without losing tracking state

## Architecture

### ECS-Driven Approach

Follows the `EnsurePlayerSkillProfileSystem` pattern:

1. **`ReceivedStarterKitComponent`** — marker component persisted on the player entity
2. **`GrantStarterKitSystem`** — `HolderSystem<EntityStore>` that queries for players WITHOUT the marker component
3. When a matching player entity is added, the system grants items and attaches the component

This is fully reactive and idempotent — the ECS engine handles timing, and component presence is the single source of truth.

## Module Structure

```
plugins/starterkit/
  src/main/java/org/runetale/starterkit/
    StarterKitPlugin.java                     # JavaPlugin entry point
    config/
      StarterKitPathLayout.java               # Path resolution (mods/runetale/config/starterkit/)
      StarterKitExternalConfigBootstrap.java   # Seeds default config if missing
      StarterKitConfigService.java            # Loads and parses kit.json
    domain/
      StarterKitConfig.java                   # Record: version, enabled, items list
      KitItem.java                            # Record: container, itemId, quantity
    component/
      ReceivedStarterKitComponent.java        # ECS marker with grantedAtEpochMillis
    system/
      GrantStarterKitSystem.java              # HolderSystem that grants kit items
  src/main/resources/
    manifest.json
    StarterKit/
      config/
        kit.json                              # Default kit definition
  src/test/java/org/runetale/starterkit/
    config/
      StarterKitConfigServiceTest.java
    domain/
      KitItemTest.java
```

## Config Format

`kit.json`:
```json
{
  "version": 1,
  "enabled": true,
  "items": [
    { "container": "hotbar", "itemId": "Weapon_Sword_Steel_Rusty", "quantity": 1 },
    { "container": "hotbar", "itemId": "Food_Bread", "quantity": 9 },
    { "container": "armour", "itemId": "Armor_Leather_Light_Legs", "quantity": 1 },
    { "container": "utility", "itemId": "Weapon_Shield_Rusty", "quantity": 1 }
  ]
}
```

Only populated slots are listed. `container` is one of: `hotbar`, `armour`, `utility`, `tools`, `storage`, `backpack`.

## Component: ReceivedStarterKitComponent

Marker component with a single field:

- `grantedAtEpochMillis` (long) — timestamp of when kit was granted

Persisted via `BuilderCodec` following the `OwnedLootComponent` pattern (`plugins/loot-protection/src/main/java/org/runetale/lootprotection/component/OwnedLootComponent.java`).

Implements `Component<EntityStore>` with `clone()`.

## System: GrantStarterKitSystem

Extends `HolderSystem<EntityStore>`, mirrors `EnsurePlayerSkillProfileSystem` (`plugins/skills/src/main/java/org/runetale/skills/system/EnsurePlayerSkillProfileSystem.java`).

**Query:** `Query.and(PlayerRef.getComponentType(), Query.not(receivedKitComponentType))`

**onEntityAdd:**
1. If config `enabled == false`: attach component with timestamp, skip item granting, log, return
2. For each `KitItem` in config:
   a. Look up `Item` via `Item.getAssetMap().getAsset(itemId)` (`plugins/skills-crafting/src/main/java/org/runetale/skills/page/CraftingPageSupport.java:157`)
   b. Resolve target `ItemContainer` from `player.getInventory()`:
      - `hotbar` -> `getHotbar()`
      - `armour` -> `getArmor()`
      - `utility` -> `getUtility()` (or equivalent accessor)
      - `tools` -> `getTools()`
      - `storage` -> `getStorage()`
      - `backpack` -> `getBackpack()`
   c. Place item using `player.giveItem()` for general containers, or container-level transaction API for armor/utility if `giveItem` doesn't target those containers
   d. Log failures (unknown itemId, container full)
3. Attach `ReceivedStarterKitComponent` with current epoch millis

**onEntityRemoved:** No-op (component lifecycle handled by ECS persistence).

## Plugin Lifecycle

### setup()
1. Create `StarterKitPathLayout` from `getDataDirectory()`
2. `StarterKitExternalConfigBootstrap.seedMissingDefaults(pathLayout)` — copies bundled `kit.json` if absent
3. Load config via `StarterKitConfigService`
4. Register `ReceivedStarterKitComponent` with codec via `getEntityStoreRegistry().registerComponent()`
5. Register `GrantStarterKitSystem` via `getEntityStoreRegistry().registerSystem()`

### start()
Log startup complete with config summary (enabled status, item count).

### shutdown()
Clear config references for clean hot-reload.

## Config Infrastructure

Follows the block-regeneration pattern:

- **`StarterKitPathLayout`** — resolves `mods/runetale/config/starterkit/` from data directory (pattern: `plugins/block-regeneration/src/main/java/org/runetale/blockregeneration/config/BlockRegenPathLayout.java`)
- **`StarterKitExternalConfigBootstrap`** — copies classpath `StarterKit/config/kit.json` to external path if missing (pattern: `plugins/block-regeneration/src/main/java/org/runetale/blockregeneration/config/BlockRegenExternalConfigBootstrap.java`)
- **`StarterKitConfigService`** — reads JSON, deserializes to `StarterKitConfig` record, falls back to bundled default on parse failure

## Domain Records

```java
public record StarterKitConfig(int version, boolean enabled, List<KitItem> items) {
    static StarterKitConfig defaults() { ... }
}

public record KitItem(String container, String itemId, int quantity) {}
```

## Build Integration

- Add `:plugins:starterkit` to `settings.gradle.kts`
- No custom `build.gradle.kts` needed (inherits shadow JAR convention from root)
- `manifest.json` with `Main: org.runetale.starterkit.StarterKitPlugin`, no mod dependencies

## Testing

### Unit tests
- Config parsing: valid JSON, missing fields, empty items list, unknown container names
- `KitItem` validation: blank itemId, zero/negative quantity, invalid container name

### Contract tests
- System query construction (verifies correct component type matching)

### Manual verification
- Deploy to local server, join as new player, verify items granted
- Rejoin — verify no duplicate kit (component already present)
- Edit config, restart — verify new config takes effect for new players only
- Set `enabled: false` — verify component attached but no items granted

## Key Reference Files

- `plugins/skills/src/main/java/org/runetale/skills/system/EnsurePlayerSkillProfileSystem.java` — system pattern
- `plugins/loot-protection/src/main/java/org/runetale/lootprotection/component/OwnedLootComponent.java` — component + codec pattern
- `plugins/block-regeneration/src/main/java/org/runetale/blockregeneration/config/` — config bootstrap pattern
- `plugins/loot-protection/src/main/java/org/runetale/lootprotection/service/OwnedLootDeliveryService.java` — `player.giveItem()` usage
- `plugins/skills-equipment/src/main/java/org/runetale/skills/equipment/system/EquipmentRequirementEnforcementSystem.java` — armor container access
- `plugins/skills-crafting/src/main/java/org/runetale/skills/page/CraftingPageSupport.java` — `Item.getAssetMap().getAsset()` lookup
