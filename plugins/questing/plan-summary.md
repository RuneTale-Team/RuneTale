# Questing & Dialog Engine Summary

This document summarizes the architecture and integration of the **Questing** and **Dialog** plugins. These systems are heavily data-driven, allowing builders to create rich, branching quests and NPC interactions using JSON files without writing any code.

---

## 1. The Dialog Plugin

The Dialog system drives all interactions with NPCs, objects, and locations (referred to generally as "Interactables"). It uses a **hub-based topic menu** where players greet an interactable, see a list of topics, explore a conversation branch, and ultimately return to the topic menu.

### Dialog Builder Data Structures (JSON)

**Dialog Definition (`interactables/<id>.json`)**
The root structure for an interactable's dialog.
*   **`interactable_id`**: The entity or block ID this dialog attaches to.
*   **`portrait`**: (Optional) UI portrait to display.
*   **`greetings`**: A list of initial greetings. The system picks the first one whose condition is met.
*   **`topics`**: A list of conversation starters available in the hub menu.

**Topic**
An option the player can click in the main menu.
*   **`id`**: Unique identifier for the topic.
*   **`text`**: The text the player clicks.
*   **`condition`**: (Optional) When this topic should be visible (e.g., must have a quest active).
*   **`locked_text`**: (Optional) What to show if the condition is not met (with a padlock icon).
*   **`hide_if_locked`**: Boolean. If true, the topic is completely hidden when locked instead of grayed out.
*   **`response`**: What the interactable says immediately upon clicking the topic.
*   **`deep_responses`**: A map of further conversation branches nested under this topic.

**Response & Continue Options**
*   **`text`**: The text the interactable says.
*   **`effects`**: (Optional) A list of actions to execute when this text is shown (e.g., give items, trigger quest events).
*   **`back_to_topics`**: Boolean. If true, the dialog returns to the main hub menu after this response.
*   **`continue_options`**: A list of player replies to continue the conversation (each containing `text`, `next_response`, `effects`, and `conditions`).

---

## 2. The Questing Plugin

The Questing engine tracks player progress through predefined steps. A quest is a linear sequence of steps, though an individual step can branch or require multiple conditions to complete.

### Quest Builder Data Structures (JSON)

**Quest Definition (`quests/<id>.json`)**
The root structure for a quest.
*   **`id`**: Unique quest identifier.
*   **`name`**: Display name.
*   **`description`**: Journal overview.
*   **`difficulty` / `length`**: Metadata for the journal UI.
*   **`prerequisites`**: Requirements to start (skills, other quests, quest points, items).
*   **`steps`**: The sequence of tasks the player must complete.
*   **`rewards`**: What the player gets upon finishing (XP, items, unlocks).
*   **`completion_journal`**: Text added to the journal when finished.

**Quest Steps**
Every step requires an `id`, a `type`, and `journal_text` (what the journal displays while on this step).
Available `type`s:
*   **`DIALOG`**: Progresses when a specific dialog event is fired.
*   **`COLLECT`**: Progresses automatically when the player obtains required items.
*   **`DELIVERY`**: Giving items to a target (`target_type`, `target_id`, `items`, `consumeItems`).
*   **`KILL`**: Slaying a specific entity (`mob_id`, `quantity`, optionally requiring a specific weapon).
*   **`REACH`**: Arriving at a zone or coordinate.
*   **`SKILL_ACTION`**: Performing a specific skilling action (e.g., chopping Oak trees).
*   **`USE_ITEM_ON`**: Using an item on a target (`item_id`, `target_type`, `target_id`, `consumesItem`, `replacementItemId`).
*   **`EMOTE`**: Performing an emote near a target.
*   **`EQUIP`**: Equipping specific items.
*   **`CHOOSE`**: A non-dialog branching choice with multiple `branches`, tracking the path the player took.

---

## 3. Cross-Plugin Integration

The Dialog and Questing plugins are decoupled but heavily integrated through **Conditions** and **Events**. 

### Dialog Interrogating Quest State (Conditions)
Dialog files use conditions to control what the player sees based on quest progress.
*   `quest_not_started`: Shows a topic offering a quest.
*   `quest_active`: Shows a topic asking for an update on the quest.
*   `quest_step`: Shows a topic only if the player is on a specific step (e.g., "delivery").
*   `quest_complete`: Shows post-quest dialog ("Thanks for the help!").

### Dialog Controlling Quest Progress (Effects)
The Dialog plugin does **not** know how to run quests. Instead, it fires generic `trigger_event` effects, which the Quest plugin listens to.

**Example Dialog Effect:**
```json
{
  "type": "trigger_event",
  "event_id": "quest_event",
  "action": "start",
  "quest_id": "cooks_assistant"
}
```

Actions the Quest plugin listens for:
1.  **`start`**: Begins the quest.
2.  **`advance`**: Completes the current step (or advances to a specific `step_id`).
3.  **`delivery`**: Attempts to consume items for a `DeliveryStep` and advances the step if successful.
4.  **`complete`**: Attempts to grant rewards and mark the quest complete.

### Quest Controlling Dialog Flow (Dynamic Overrides)
When a dialog triggers a `delivery` or `complete` action, the Quest plugin might reject it (e.g., the player doesn't have the required items, or their inventory is too full for the rewards). 

When this happens, the Quest plugin securely overrides the Dialog session, forcing the interactable to navigate to a pre-defined failure response (e.g., bouncing the conversation to a `"missing_items"` or `"inventory_full"` response ID without the Dialog plugin doing any hard work). 

---

## Example Quest Flow (Cook's Assistant)

### 1. The Quest Definition (`quests/cooks_assistant.json`)

```json
{
  "id": "cooks_assistant",
  "name": "Cook's Assistant",
  "description": "The Lumbridge Cook needs help baking a cake for the Duke.",
  "difficulty": "NOVICE",
  "length": "SHORT",
  "prerequisites": null,
  "steps": [
    {
      "id": "gather",
      "type": "COLLECT",
      "items": [
        { "item_id": "egg", "quantity": 1 },
        { "item_id": "bucket_of_milk", "quantity": 1 },
        { "item_id": "pot_of_flour", "quantity": 1 }
      ],
      "journal_text": "I need to gather an egg, a bucket of milk, and a pot of flour for the Cook."
    },
    {
      "id": "return",
      "type": "DELIVERY",
      "target_type": "NPC",
      "target_id": "lumbridge_cook",
      "items": [
        { "item_id": "egg", "quantity": 1 },
        { "item_id": "bucket_of_milk", "quantity": 1 },
        { "item_id": "pot_of_flour", "quantity": 1 }
      ],
      "consumeItems": true,
      "journal_text": "I have the ingredients. I should return them to the Cook."
    }
  ],
  "completion_journal": "I helped the Cook gather ingredients for the Duke's birthday cake.",
  "rewards": {
    "xp": [{ "skill": "COOKING", "amount": 300 }],
    "items": [],
    "quest_points": 1,
    "unlocks": []
  }
}
```

### 2. The Dialog Definition (`interactables/lumbridge_cook.json`)

```json
{
  "interactable_id": "lumbridge_cook",
  "portrait": "lumbridge_cook",
  "greetings": [
    {
      "text": "Thanks again for the help with that cake!",
      "condition": { "quest_complete": "cooks_assistant" }
    },
    {
      "text": "Have you found those ingredients yet?",
      "condition": { "quest_active": "cooks_assistant" }
    },
    {
      "text": "Oh dear, oh dear! The Duke's birthday is today and I don't have the ingredients for his cake!",
      "condition": null
    }
  ],
  "topics": [
    {
      "id": "offer_help",
      "text": "Can I help you?",
      "condition": { "quest_not_started": "cooks_assistant" },
      "response": {
        "text": "Would you really? I need an egg, a bucket of milk, and a pot of flour.",
        "continue_options": [
          {
            "text": "I'll get right on it!",
            "effects": [
              { "type": "trigger_event", "event_id": "quest_event", "action": "start", "quest_id": "cooks_assistant" }
            ],
            "back_to_topics": true
          }
        ]
      }
    },
    {
      "id": "deliver_items",
      "text": "I have your ingredients!",
      "condition": { "quest_step": { "quest": "cooks_assistant", "step": "return" } },
      "response": {
        "text": "Let me see... Ah, perfect! You've saved the Duke's birthday!",
        "effects": [
          { "type": "trigger_event", "event_id": "quest_event", "action": "delivery", "quest_id": "cooks_assistant" }
        ],
        "back_to_topics": true
      }
    }
  ],
  "responses": {
    "missing_items": {
      "text": "Wait a minute, you don't have everything I need! Remember: An egg, a bucket of milk, and a pot of flour.",
      "back_to_topics": true
    },
    "inventory_full": {
      "text": "I was going to give you a reward, but your hands are full! Come back when you have some space.",
      "back_to_topics": true
    }
  }
}
```
