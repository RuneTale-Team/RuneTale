# Questing Plugin - Implementation Plan

## Design Philosophy

**Goal:** Enable non-programmers to create rich, RuneScape-style quests through JSON configuration files.

**Principles:**
- Quests are entirely data-driven (JSON files)
- Step types are generic and composable
- Dialog is separate from logic but easily referenced
- Rewards are flexible and extensible
- Prerequisites support skills, items, quests, quest points, and custom handlers
- Journal entries are defined per-task for easy state-aware display

---

## Plan Documents

| Document | Description |
|----------|-------------|
| [plan-domain.md](plan-domain.md) | Core data structures: QuestDefinition, QuestStep, StepType, QuestProgress |
| [plan-api.md](plan-api.md) | Runtime API interfaces: QuestRuntimeApi, QuestRuntimeRegistry, events |
| [plan-handlers.md](plan-handlers.md) | Step type handlers: DialogStepHandler, KillStepHandler, etc. |
| [plan-prerequisites.md](plan-prerequisites.md) | Prerequisite checking: skills, quests, items, custom handlers |
| [plan-journal.md](plan-journal.md) | Quest journal UI: JournalService, QuestJournalPage |
| [plan-integration.md](plan-integration.md) | Cross-plugin integration: skills, dialog, items, zones |

---

## Quest Step Types

| Type | Description | Example |
|------|-------------|---------|
| `DIALOG` | Talk to NPC, progress through dialog tree | "Speak to the Cook in Lumbridge" |
| `DELIVERY` | Give items to NPC (consumes them) | "Bring the Cook an egg, milk, and flour" |
| `COLLECT` | Have items in inventory (any source) | "Obtain 10 iron ore" |
| `KILL` | Kill specific mobs | "Kill 5 goblins" |
| `SKILL_ACTION` | Perform skill action at location | "Mine some copper ore" |
| `REACH` | Enter an area/trigger zone | "Enter the Varrock east gate" |
| `USE_ITEM_ON` | Use item on object/NPC | "Use the key on the chest" |
| `EMOTE` | Perform an emote at location | "Wave at the statue" |
| `EQUIP` | Wear specific equipment | "Wear the ring of visibility" |
| `CHOOSE` | Branching choice | "Side with the goblins or the humans?" |
| `CUSTOM` | Custom handler hook | Puzzle, minigame, etc. |

---

## Quick Schema Reference

### Quest Definition

```json
{
  "id": "cooks_assistant",
  "name": "Cook's Assistant",
  "description": "The Lumbridge cook is in a flap...",
  "difficulty": "NOVICE",
  "length": "SHORT",
  "series": "lumbridge_cook",
  
  "prerequisites": {
    "skills": { "COOKING": 1 },
    "quests": [],
    "items": [],
    "quest_points": 0,
    "custom": []
  },
  
  "steps": [
    { "id": "start_dialog", "type": "DIALOG", "npc_id": "lumbridge_cook", "dialog_id": "cooks_assistant_start", "journal_text": "I spoke to the Cook..." },
    { "id": "gather", "type": "COLLECT", "items": [{ "item_id": "egg", "quantity": 1 }, { "item_id": "milk", "quantity": 1 }, { "item_id": "flour", "quantity": 1 }], "journal_text": "I need to find some eggs, milk, and flour." },
    { "id": "return", "type": "DELIVERY", "target_type": "NPC", "target_id": "lumbridge_cook", "items": [...], "journal_text": "I should return to the Cook." }
  ],
  
  "completion_journal": "I helped the Cook gather ingredients for his cake.",
  
  "rewards": {
    "quest_points": 1,
    "xp": [{ "skill": "COOKING", "amount": 300 }],
    "items": [{ "item_id": "coins", "quantity": 50 }],
    "unlocks": []
  }
}
```

### Prerequisites

```json
{
  "prerequisites": {
    "skills": { "COOKING": 1, "FISHING": 5 },
    "quests": ["cooks_assistant", "rune_mysteries"],
    "items": [{ "item_id": "iron_pickaxe", "quantity": 1 }],
    "quest_points": 5,
    "custom": [
      "faction:varrock_friendly",
      "task:given_cook_eggs:3",
      "minigame:pest_control:wins:10"
    ]
  }
}
```

---

## Folder Structure

```
plugins/questing/
├── src/main/java/org/runetale/questing/
│   ├── QuestingPlugin.java
│   ├── api/
│   │   ├── QuestRuntimeApi.java
│   │   ├── QuestRuntimeRegistry.java
│   │   └── StartQuestResult.java
│   ├── domain/
│   │   ├── QuestDefinition.java
│   │   ├── QuestStep.java (abstract)
│   │   ├── DialogStep.java
│   │   ├── DeliveryStep.java
│   │   ├── CollectStep.java
│   │   ├── KillStep.java
│   │   ├── SkillActionStep.java
│   │   ├── ReachStep.java
│   │   ├── UseItemOnStep.java
│   │   ├── EmoteStep.java
│   │   ├── EquipStep.java
│   │   ├── ChooseStep.java
│   │   ├── CustomStep.java
│   │   ├── StepType.java
│   │   ├── QuestProgress.java
│   │   ├── QuestStatus.java
│   │   ├── QuestReward.java
│   │   ├── QuestPrerequisites.java
│   │   ├── ItemRequirement.java
│   │   ├── ItemReward.java
│   │   ├── XpReward.java
│   │   ├── QuestDifficulty.java
│   │   └── QuestLength.java
│   ├── component/
│   │   └── PlayerQuestProfileComponent.java
│   ├── service/
│   │   ├── QuestRegistry.java
│   │   ├── QuestProgressService.java
│   │   ├── QuestRewardService.java
│   │   ├── PrerequisiteService.java
│   │   └── CustomPrerequisiteService.java
│   ├── handler/
│   │   ├── StepHandler.java
│   │   ├── StepProgressResult.java
│   │   ├── StepTriggerRegistry.java
│   │   ├── StepHandlerRegistry.java
│   │   ├── DialogStepHandler.java
│   │   ├── DeliveryStepHandler.java
│   │   ├── CollectStepHandler.java
│   │   ├── KillStepHandler.java
│   │   ├── SkillActionStepHandler.java
│   │   ├── ReachStepHandler.java
│   │   ├── UseItemOnStepHandler.java
│   │   ├── EmoteStepHandler.java
│   │   ├── EquipStepHandler.java
│   │   ├── ChooseStepHandler.java
│   │   ├── CustomStepHandler.java
│   │   └── prerequisite/
│   │       ├── FactionPrerequisiteHandler.java
│   │       ├── TaskCountPrerequisiteHandler.java
│   │       └── StatPrerequisiteHandler.java
│   ├── journal/
│   │   ├── JournalService.java
│   │   ├── JournalEntry.java
│   │   ├── JournalEntryStatus.java
│   │   └── QuestJournalEntry.java
│   ├── system/
│   │   ├── QuestProgressSystem.java
│   │   └── EnsurePlayerQuestProfileSystem.java
│   ├── command/
│   │   └── QuestCommand.java
│   ├── event/
│   │   ├── QuestCompleteEvent.java
│   │   ├── QuestStepCompleteEvent.java
│   │   └── QuestStartEvent.java
│   ├── page/
│   │   └── QuestJournalPage.java
│   ├── integration/
│   │   ├── SkillsPluginIntegration.java
│   │   ├── QuestDialogEffects.java
│   │   └── QuestDialogConditions.java
│   └── config/
│       └── QuestExternalConfigBootstrap.java
├── src/main/resources/Quests/
│   ├── index.list
│   └── novice/
│       └── cooks_assistant.json
└── src/test/java/org/runetale/questing/
    ├── domain/
    ├── handler/
    ├── service/
    └── integration/
```

---

## Implementation Phases

### Phase 1: Core Data Structures (plan-domain.md)
- [ ] Create `QuestingPlugin` entry point
- [ ] Define `QuestDefinition`, `QuestStep`, `StepType` enums
- [ ] Create `PlayerQuestProfileComponent` for persistence
- [ ] Implement JSON loading via `QuestRegistry`
- [ ] Set up `QuestRuntimeApi` and registry pattern
- [ ] Add `QuestPrerequisites` with custom handler support

### Phase 2: Step Type Handlers (plan-handlers.md)
- [ ] Create `StepHandler` interface
- [ ] Implement handlers for each step type
- [ ] Wire handlers into progress tracking system
- [ ] Handle step completion events
- [ ] Store journal_text per step for display

### Phase 3: Custom Prerequisites (plan-prerequisites.md)
- [ ] Create `PrerequisiteHandler` interface
- [ ] Implement `CustomPrerequisiteService` registry
- [ ] Add built-in handlers (faction, task count, stat)
- [ ] Allow plugins to register custom prerequisite handlers

### Phase 4: Dialog System Integration (plan-integration.md)
- [ ] Register quest dialog effect handlers
- [ ] Register quest dialog condition handlers
- [ ] Support dialog-triggered quest starts
- [ ] Wire up DIALOG step type

### Phase 5: Quest Journal UI (plan-journal.md)
- [ ] In-game quest journal page
- [ ] Display active/completed quests
- [ ] Iterate over steps with journal_text and completion state
- [ ] Show partial progress for collect/kill steps

### Phase 6: Advanced Features
- [ ] Quest series/chains
- [ ] Quest point system
- [ ] Repeatable/daily quests
- [ ] Quest unlocks (areas, items, abilities)
- [ ] Integration with skills plugin for skill requirements

---

## Example Quests

### Simple Quest (Cook's Assistant)

See full example in plan-domain.md - a novice quest with:
- Dialog step to start
- Collect step for ingredients  
- Delivery step to complete

### Complex Quest (Demon Slayer with Branching)

See full example in plan-domain.md - demonstrates:
- Multiple step types
- CHOOSE step with branches
- Conditional requirements (has_item for shortcut)
- Required item for kill step

---

## Integration Points

### Skills Plugin
```java
// Check skill requirement
SkillsRuntimeApi skillsApi = SkillsRuntimeRegistry.get();
int level = skillsApi.getSkillLevel(accessor, playerRef, SkillType.COOKING);

// Grant XP reward
skillsApi.grantSkillXp(accessor, playerRef, SkillType.COOKING, 300, "quest", true);
```

### Dialog Plugin
```java
// Quest effects registered with dialog system
{ "type": "start_quest", "quest_id": "cooks_assistant" }
{ "type": "complete_quest", "quest_id": "cooks_assistant" }

// Quest conditions in dialog topics
{ "quest_active": "cooks_assistant" }
{ "quest_complete": "demon_slayer" }
```

### Custom Prerequisite Registration
```java
CustomPrerequisiteService prereqService = questApi.getCustomPrerequisiteService();

preregService.register("goblin_faction_neutral", (accessor, playerRef) -> {
    return factionService.getStanding(accessor, playerRef, "goblin") == Standing.NEUTRAL;
});

preregService.registerPattern("task:", (id, accessor, playerRef) -> {
    String[] parts = id.split(":");
    return taskService.getCompletionCount(accessor, playerRef, parts[1]) >= Integer.parseInt(parts[2]);
});
```

---

## Related Plans

- [Dialog Plugin Plan](../dialog/plan.md) - NPC dialog system
- [Skills Plugin](../skills/) - Skill progression system
