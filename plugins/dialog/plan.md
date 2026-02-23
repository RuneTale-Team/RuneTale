# Dialog Plugin - Implementation Plan

## Design Philosophy

**Goal:** Enable non-programmers to create NPC dialogs through JSON configuration using a hub-based topic menu system.

**Principles:**
- One dialog file per NPC
- Hub model: NPC greeting → Topic menu → Response → Back to menu
- Topics appear/disappear based on conditions (skills, quests, items)
- Generic effect/trigger system for any system to subscribe
- Custom dialog UI (RuneScape-style popup)

---

## Core Flow (Hub Model)

```
Player presses INTERACT (F) on NPC
                ↓
NPC displays greeting: "Hello adventurer!"
                ↓
Topic Menu appears (filtered by conditions):
┌─────────────────────────────────────────┐
│ > Who are you?                          │  (always shown)
│ > Do you have any quests?               │  (shown: quest not started)
│ > About that quest...                   │  (shown: quest active)
│ > I have your ingredients!              │  (shown: quest active + has items)
│ > Goodbye                               │  (always shown)
└─────────────────────────────────────────┘
                ↓
Player selects a topic
                ↓
NPC responds with text
                ↓
┌─────────────────────────────────────────┐
│ Continue options OR                     │
│ Automatically return to topic menu      │
└─────────────────────────────────────────┘
                ↓
Back to Topic Menu (loop)
```

**Key insight:** The NPC is always in "menu mode". Conversations branch temporarily but always return to the hub.

---

## Folder Structure

```
src/main/resources/Dialogs/
├── index.list                    # Lists all interactable dialog files
└── interactables/
    ├── lumbridge_cook.json
    ├── gypsy_aris.json
    ├── hans.json
    ├── mysterious_crate.json
    └── barmaid.json
```

**One file per interactable.** All topics, conditions, and responses in one place.

---

## Dialog Schema

### Full Dialog Definition

```json
{
  "interactable_id": "lumbridge_cook",
  "greeting": {
    "text": "Hello adventurer! I'm the cook for the Duke of Lumbridge.",
    "condition": null
  },
  "topics": [
    {
      "id": "who_are_you",
      "text": "Who are you?",
      "condition": null,
      "response": {
        "text": "I'm the cook for the Duke of Lumbridge. I prepare all his meals and ensure the kitchen runs smoothly.",
        "continue_options": [
          { "text": "Sounds like hard work!", "next_response": "hard_work" },
          { "text": "Back", "back_to_topics": true }
        ]
      },
      "deep_responses": {
        "hard_work": {
          "text": "Aye, but someone has to do it! The Duke appreciates good food, and I take pride in my work.",
          "back_to_topics": true
        }
      }
    },
    {
      "id": "quest_offer",
      "text": "Do you have any quests for me?",
      "condition": { "quest_not_started": "cooks_assistant" },
      "locked_text": "You've already helped me plenty!",
      "response": {
        "text": "Actually, yes! I need to make a cake for the Duke's birthday, but I've forgotten some ingredients. Could you help?",
        "continue_options": [
          { 
            "text": "Of course! What do you need?", 
            "next_response": "quest_details"
          },
          { "text": "I'm too busy right now.", "back_to_topics": true }
        ]
      },
      "deep_responses": {
        "quest_details": {
          "text": "I need an egg, a bucket of milk, and a pot of flour. You can find these around Lumbridge!",
          "continue_options": [
            { 
              "text": "I'll get them for you!", 
              "effects": [
                { "type": "trigger_event", "event_id": "quest_event", "action": "start", "quest_id": "cooks_assistant" },
                { "type": "set_flag", "flag": "cooks_assistant_accepted" }
              ],
              "back_to_topics": true 
            },
            { "text": "Let me think about it.", "back_to_topics": true }
          ]
        }
      }
    },
    {
      "id": "quest_progress",
      "text": "About that quest...",
      "condition": { "quest_active": "cooks_assistant" },
      "response": {
        "text": "Have you got those ingredients yet? I need an egg, a bucket of milk, and a pot of flour.",
        "continue_options": [
          { 
            "text": "Where can I find them?", 
            "next_response": "ingredient_hints"
          },
          { "text": "Not yet.", "back_to_topics": true }
        ]
      },
      "deep_responses": {
        "ingredient_hints": {
          "text": "Eggs are in the chicken coop to the north. You can get milk from the dairy cow, and flour from the mill near the wheat field.",
          "back_to_topics": true
        }
      }
    },
    {
      "id": "quest_complete",
      "text": "I have your ingredients!",
      "condition": { 
        "all": [
          { "quest_active": "cooks_assistant" },
          { "has_items": [
            { "item_id": "egg", "quantity": 1 },
            { "item_id": "bucket_of_milk", "quantity": 1 },
            { "item_id": "pot_of_flour", "quantity": 1 }
          ]}
        ]
      },
      "response": {
        "text": "Wonderful! You've saved the Duke's birthday celebration!",
        "effects": [
          { "type": "trigger_event", "event_id": "quest_event", "action": "delivery", "quest_id": "cooks_assistant" }
        ],
        "back_to_topics": true
      }
    }
  ]
}
```

---

## Schema Reference

### Dialog (Root)

```json
{
  "interactable_id": "string",  // Unique identifier for NPC or Object
  "portrait": "string",         // Optional portrait ID for UI
  "greeting": { ... },          // Initial greeting when player approaches
  "topics": [ ... ]             // Topic menu options
}
```

### Greeting

```json
{
  "text": "NPC's greeting text",
  "condition": { ... },         // Optional: show different greetings
  "effects": [ ... ]            // Optional: effects when greeting plays
}
```

**Multiple greetings (priority-based):**
```json
{
  "greetings": [
    {
      "text": "Thanks again for your help!",
      "condition": { "quest_complete": "cooks_assistant" }
    },
    {
      "text": "Hello adventurer!",
      "condition": null
    }
  ]
}
```

### Topic

```json
{
  "id": "unique_topic_id",
  "text": "Player's menu text",
  "condition": { ... },         // Optional: when to show this topic
  "locked_text": "Requires...", // Optional: show when condition fails
  "hide_if_locked": false,      // If true, don't show at all when locked
  "response": { ... },          // NPC's response when topic selected
  "deep_responses": { ... }     // Optional: continue conversation
}
```

### Response

```json
{
  "text": "NPC's response text",
  "effects": [ ... ],           // Optional: effects when response plays
  "back_to_topics": true,       // Return to menu after this response
  "continue_options": [ ... ]   // Optional: show follow-up options
}
```

### Continue Option

```json
{
  "text": "Player's option text",
  "next_response": "response_id",  // Go to deep_responses[id]
  "effects": [ ... ],               // Optional
  "back_to_topics": true,           // Shortcut: return to menu
  "condition": { ... }              // Optional: show/hide option
}
```

---

## Condition Types

| Type | Description | Example |
|------|-------------|---------|
| `skill_level` | Minimum skill level | `{ "skill_level": { "skill": "WOODCUTTING", "level": 10 } }` |
| `quest_complete` | Quest finished | `{ "quest_complete": "cooks_assistant" }` |
| `quest_active` | Quest in progress | `{ "quest_active": "demon_slayer" }` |
| `quest_not_started` | Quest not begun | `{ "quest_not_started": "cooks_assistant" }` |
| `quest_step` | At specific step | `{ "quest_step": { "quest": "cooks_assistant", "step": "gather" } }` |
| `has_item` | Item in inventory | `{ "has_item": { "item_id": "silverlight", "quantity": 1 } }` |
| `has_items` | Multiple items | `{ "has_items": [...] }` |
| `equipped` | Item equipped | `{ "equipped": "ring_of_visibility" }` |
| `flag_set` | Dialog flag is true | `{ "flag_set": "talked_to_wizard" }` |
| `flag_not_set` | Dialog flag is false | `{ "flag_not_set": "talked_to_wizard" }` |
| `quest_points` | Minimum quest points | `{ "quest_points": 10 }` |

### Compound Conditions

**AND:**
```json
{
  "all": [
    { "skill_level": { "skill": "MINING", "level": 15 } },
    { "quest_complete": "cooks_assistant" }
  ]
}
```

**OR:**
```json
{
  "any": [
    { "quest_complete": "demon_slayer" },
    { "quest_complete": "vampire_slayer" }
  ]
}
```

**NOT:**
```json
{
  "not": { "quest_complete": "cooks_assistant" }
}
```

---

## Effect Types

Effects are generic events that any plugin can handle.

| Effect | Description | Fields |
|--------|-------------|--------|
| `give_item` | Add item to inventory | `item_id`, `quantity` |
| `remove_items` | Remove from inventory | `items: [{ item_id, quantity }]` |
| `give_xp` | Grant skill XP | `skill`, `amount` |
| `set_flag` | Set dialog flag | `flag`, `value` (optional, default "true") |
| `clear_flag` | Clear dialog flag | `flag` |
| `teleport` | Move player | `x`, `y`, `z` |
| `play_sound` | Play sound effect | `sound_id` |
| `open_shop` | Open shop UI | `shop_id` |
| `trigger_event` | Fire custom event | `event_id`, `data` |

### Effect Example

```json
"effects": [
  { "type": "trigger_event", "event_id": "quest_event", "action": "start", "quest_id": "cooks_assistant" },
  { "type": "set_flag", "flag": "met_the_cook" },
  { "type": "give_item", "item_id": "coins", "quantity": 100 },
  { "type": "play_sound", "sound_id": "quest_start" }
]
```

---

## Effect Handler Registration

```java
// Dialog plugin provides the interface
public interface EffectHandler {
    String effectType();
    void execute(UUID playerId, JsonObject data);
}

// Quest plugin registers handlers
public class QuestEventTriggerHandler implements EffectHandler {
    @Override
    public String effectType() { return "trigger_event"; }
    
    @Override
    public void execute(UUID playerId, JsonObject data) {
        String eventId = data.get("event_id").getAsString();
        // The Dialog plugin simply fires a generic system event:
        EventBus.post(new DialogTriggerEvent(playerId, eventId, data));
        
        // The QuestPlugin listens to DialogTriggerEvent to handle actions
        // like attempting delivery, starting quests, etc.
        // It can then call DialogRuntimeApi.setOverrideResponse(...) 
        // to change the dialog dynamically if a delivery fails (e.g., inventory full).
    }
}

// Registration (in DialogPlugin.setup())
DialogRuntimeApi dialogApi = DialogRuntimeRegistry.get();
dialogApi.registerEffectHandler(new QuestEventTriggerHandler());
```

---

## Condition Handler Registration

```java
// Dialog plugin provides the interface
public interface ConditionHandler {
    String conditionType();
    boolean evaluate(UUID playerId, JsonObject data);
}

// Skills plugin registers
public class SkillLevelHandler implements ConditionHandler {
    @Override
    public String conditionType() { return "skill_level"; }
    
    @Override
    public boolean evaluate(UUID playerId, JsonObject data) {
        String skill = data.get("skill").getAsString();
        int required = data.get("level").getAsInt();
        SkillsRuntimeApi api = SkillsRuntimeRegistry.get();
        return api.getSkillLevel(playerId, SkillType.fromString(skill)) >= required;
    }
}

// Quest plugin registers
public class QuestActiveHandler implements ConditionHandler {
    @Override
    public String conditionType() { return "quest_active"; }
    
    @Override
    public boolean evaluate(UUID playerId, JsonObject data) {
        String questId = data.getAsString();
        return QuestRuntimeRegistry.get().isQuestActive(playerId, questId);
    }
}
```

---

## Dialog Session

```java
public class DialogSession {
    private final UUID playerId;
    private final String interactableId;
    private DialogDefinition dialog;
    private String currentResponseId;  // null = at topic menu
    private final Instant startedAt;
}
```

**States:**
1. `currentResponseId == null` → Showing topic menu
2. `currentResponseId != null` → Showing a response, waiting for continue option

---

## Dialog Flags

Persistent player state (survives restarts):

```java
public class PlayerDialogFlagsComponent implements Component<EntityStore> {
    private Map<String, String> flags = new HashMap<>();
    // "met_the_cook" → "true"
    // "chosen_path" → "warrior"
    // "last_visited_ned" → "2024-01-15"
}
```

---

## Dialog UI (DialogPage)

```
┌─────────────────────────────────────────────┐
│  [Portrait]    Cook                         │
│                                             │
│  "Have you got those ingredients yet?       │
│   I need an egg, a bucket of milk, and      │
│   a pot of flour."                          │
│                                             │
├─────────────────────────────────────────────┤
│  > Where can I find them?                   │
│  > Not yet.                                 │
│                                             │
│  [Back to Topics]              [Close]      │
└─────────────────────────────────────────────┘
```

### Topic Menu View

```
┌─────────────────────────────────────────────┐
│  [Portrait]    Cook                         │
│                                             │
│  "Hello adventurer! What can I do for you?" │
│                                             │
├─────────────────────────────────────────────┤
│  > Who are you?                             │
│  > About that quest...                      │
│  > I have your ingredients!                 │  🔒 Requires items
│  > Goodbye                                  │
│                                             │
│                                [Close]      │
└─────────────────────────────────────────────┘
```

### UI Features
- NPC portrait and name
- Current NPC text
- Topic menu OR continue options
- Locked topics show lock icon + requirement text
- "Back to Topics" button (when in a response)
- "Close" button or ESC to exit

---

## NPC Interaction

```java
public class NpcDialogInteraction extends SimpleInstantInteraction {
    
    public static final String TYPE_NAME = "runetale_npc_dialog";
    
    @Override
    protected void firstRun(InteractionType type, InteractionContext ctx, CooldownHandler cooldown) {
        String interactableId = getInteractableId(ctx);
        UUID playerId = getPlayerId(ctx);
        
        DialogRuntimeApi dialogApi = DialogRuntimeRegistry.get();
        DialogDefinition dialog = dialogApi.getDialog(interactableId);
        
        if (dialog != null) {
            dialogApi.startSession(playerId, interactableId, dialog);
            DialogPage.open(player, dialog);
        }
    }
}
```

---

## Example: Complete NPC

```json
{
  "interactable_id": "gypsy_aris",
  "portrait": "gypsy_aris",
  "greetings": [
    {
      "text": "The stars have told me of your coming, hero.",
      "condition": { "quest_active": "demon_slayer" }
    },
    {
      "text": "Welcome to my tent. Would you like your fortune told?",
      "condition": null
    }
  ],
  "topics": [
    {
      "id": "fortune",
      "text": "Tell me my fortune.",
      "condition": null,
      "response": {
        "text": "Cross my palm with silver... just kidding! What would you like to know?",
        "continue_options": [
          { "text": "Will I be rich?", "next_response": "rich" },
          { "text": "Will I find love?", "next_response": "love" },
          { "text": "Back", "back_to_topics": true }
        ]
      },
      "deep_responses": {
        "rich": {
          "text": "The cards say... maybe! But you'll have to work for it.",
          "back_to_topics": true
        },
        "love": {
          "text": "I see a dark figure in your future... wait, that's just your reflection.",
          "back_to_topics": true
        }
      }
    },
    {
      "id": "demon_slayer_start",
      "text": "Something terrible is coming, isn't it?",
      "condition": { "quest_not_started": "demon_slayer" },
      "response": {
        "text": "Yes... a great demon named Delrith is being summoned. Only Silverlight can stop him.",
        "continue_options": [
          { 
            "text": "How do I get Silverlight?", 
            "next_response": "silverlight",
            "effects": [{ "type": "start_quest", "quest_id": "demon_slayer" }]
          },
          { "text": "I'm not ready for this.", "back_to_topics": true }
        ]
      },
      "deep_responses": {
        "silverlight": {
          "text": "Speak to Sir Prysin in Varrock Palace. He is one of the few who knows where it is kept.",
          "back_to_topics": true
        }
      }
    },
    {
      "id": "demon_slayer_progress",
      "text": "About the demon...",
      "condition": { "quest_active": "demon_slayer" },
      "response": {
        "text": "Delrith grows stronger. You must find Silverlight before it's too late!",
        "back_to_topics": true
      }
    }
  ]
}
```

---

## Implementation Phases

### Phase 1: Core Data Structures
- `DialogDefinition`, `Topic`, `Response`, `ContinueOption` domain classes
- `DialogRegistry` for loading JSON files from `interactables/` folder
- `DialogRuntimeApi` interface + registry singleton
- `DialogSession` for tracking state
- JSON parsing with Gson

### Phase 2: Condition System
- `ConditionHandler` interface + registry
- Compound conditions (AND/OR/NOT)
- Built-in handlers: `flag_set`, `flag_not_set`
- Integration hooks for skills/quest/item plugins

### Phase 3: Effect System
- `EffectHandler` interface + registry
- Built-in handlers: `set_flag`, `clear_flag`, `play_sound`
- Effect execution on response/option selection
- Integration hooks for other plugins

### Phase 4: Interaction
- `InteractableDialogInteraction` interaction type
- Session management (start/end)
- Topic menu filtering (hide locked or show locked)
- Dialog flags component persistence

### Phase 5: Dialog UI
- `DialogPage` custom UI
- Greeting + topic menu view
- Response view with continue options
- Locked topic styling (lock icon + requirement text)
- Back/Close controls

---

## File Structure

```
plugins/dialog/
├── src/main/java/org/runetale/dialog/
│   ├── DialogPlugin.java
│   ├── api/
│   │   ├── DialogRuntimeApi.java
│   │   ├── DialogRuntimeRegistry.java
│   │   ├── EffectHandler.java
│   │   └── ConditionHandler.java
│   ├── domain/
│   │   ├── DialogDefinition.java
│   │   ├── Topic.java
│   │   ├── Response.java
│   │   ├── ContinueOption.java
│   │   ├── DialogCondition.java
│   │   ├── DialogEffect.java
│   │   └── DialogSession.java
│   ├── component/
│   │   └── PlayerDialogFlagsComponent.java
│   ├── service/
│   │   ├── DialogRegistry.java
│   │   ├── ConditionService.java
│   │   ├── EffectService.java
│   │   └── SessionService.java
│   ├── interaction/
│   │   └── NpcDialogInteraction.java
│   ├── page/
│   │   └── DialogPage.java
│   ├── event/
│   │   ├── DialogStartEvent.java
│   │   ├── DialogEndEvent.java
│   │   └── TopicSelectedEvent.java
│   └── config/
│       └── DialogExternalConfigBootstrap.java
├── src/main/resources/Dialogs/
│   ├── index.list
│   └── interactables/
│       ├── lumbridge_cook.json
│       ├── gypsy_aris.json
│       ├── mysterious_crate.json
│       └── hans.json
└── src/test/java/org/runetale/dialog/
```
