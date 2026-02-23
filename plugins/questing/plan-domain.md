# Questing Plugin - Domain Model

## Overview

This document defines the core domain classes for the questing system. These are pure data structures with no dependencies on Hytale APIs.

---

## StepType Enum

```java
public enum StepType {
    DIALOG("DIALOG"),
    DELIVERY("DELIVERY"),
    COLLECT("COLLECT"),
    KILL("KILL"),
    SKILL_ACTION("SKILL_ACTION"),
    REACH("REACH"),
    USE_ITEM_ON("USE_ITEM_ON"),
    EMOTE("EMOTE"),
    EQUIP("EQUIP"),
    CHOOSE("CHOOSE"),
    CUSTOM("CUSTOM");

    private final String id;

    StepType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nonnull
    public static StepType fromString(@Nonnull String raw) {
        StepType parsed = tryParseStrict(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown step type: " + raw);
        }
        return parsed;
    }

    @Nullable
    public static StepType tryParseStrict(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toUpperCase(Locale.ROOT).trim();
        for (StepType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
```

---

## QuestStatus Enum

```java
public enum QuestStatus {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED");

    private final String id;

    // ... similar parsing methods
}
```

---

## QuestDifficulty Enum

```java
public enum QuestDifficulty {
    NOVICE("NOVICE"),       // No requirements, short
    INTERMEDIATE("INTERMEDIATE"), // Some skill requirements
    EXPERIENCED("EXPERIENCED"),   // Higher requirements
    MASTER("MASTER"),       // High requirements, long
    GRANDMASTER("GRANDMASTER");   // Very high requirements

    private final String id;
    // ... parsing methods
}
```

---

## QuestLength Enum

```java
public enum QuestLength {
    SHORT("SHORT"),         // 5-10 minutes
    MEDIUM("MEDIUM"),       // 10-30 minutes
    LONG("LONG"),           // 30-60 minutes
    VERY_LONG("VERY_LONG"); // 1+ hours

    private final String id;
    // ... parsing methods
}
```

---

## ItemRequirement (Value Object)

```java
public final class ItemRequirement {
    private final String itemId;
    private final int quantity;

    public ItemRequirement(@Nonnull String itemId, int quantity) {
        this.itemId = Objects.requireNonNull(itemId);
        this.quantity = Math.max(1, quantity);
    }

    @Nonnull
    public String itemId() { return itemId; }
    public int quantity() { return quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemRequirement)) return false;
        ItemRequirement that = (ItemRequirement) o;
        return quantity == that.quantity && itemId.equals(that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, quantity);
    }
}
```

---

## XpReward (Value Object)

```java
public final class XpReward {
    private final String skillId;
    private final long amount;

    public XpReward(@Nonnull String skillId, long amount) {
        this.skillId = Objects.requireNonNull(skillId);
        this.amount = Math.max(0, amount);
    }

    @Nonnull
    public String skillId() { return skillId; }
    public long amount() { return amount; }
}
```

---

## ItemReward (Value Object)

```java
public final class ItemReward {
    private final String itemId;
    private final int quantity;
    private final boolean untradeable;

    public ItemReward(@Nonnull String itemId, int quantity, boolean untradeable) {
        this.itemId = Objects.requireNonNull(itemId);
        this.quantity = Math.max(1, quantity);
        this.untradeable = untradeable;
    }

    // Convenience constructor
    public ItemReward(@Nonnull String itemId, int quantity) {
        this(itemId, quantity, false);
    }

    @Nonnull
    public String itemId() { return itemId; }
    public int quantity() { return quantity; }
    public boolean untradeable() { return untradeable; }
}
```

---

## QuestPrerequisites

```java
public final class QuestPrerequisites {
    private final Map<String, Integer> skills;  // skillId -> minLevel
    private final Set<String> quests;           // required quest ids
    private final List<ItemRequirement> items;  // required items
    private final int questPoints;
    private final List<String> custom;          // custom handler ids

    public QuestPrerequisites(
            @Nonnull Map<String, Integer> skills,
            @Nonnull Set<String> quests,
            @Nonnull List<ItemRequirement> items,
            int questPoints,
            @Nonnull List<String> custom) {
        this.skills = Map.copyOf(skills);
        this.quests = Set.copyOf(quests);
        this.items = List.copyOf(items);
        this.questPoints = Math.max(0, questPoints);
        this.custom = List.copyOf(custom);
    }

    // Immutable getters
    @Nonnull public Map<String, Integer> skills() { return skills; }
    @Nonnull public Set<String> quests() { return quests; }
    @Nonnull public List<ItemRequirement> items() { return items; }
    public int questPoints() { return questPoints; }
    @Nonnull public List<String> custom() { return custom; }

    public boolean isEmpty() {
        return skills.isEmpty() && quests.isEmpty() && items.isEmpty() 
               && questPoints == 0 && custom.isEmpty();
    }
}
```

---

## QuestReward

```java
public final class QuestReward {
    private final int questPoints;
    private final List<XpReward> xp;
    private final List<ItemReward> items;
    private final List<String> unlocks;  // unlock ids (areas, abilities, etc.)

    public QuestReward(
            int questPoints,
            @Nonnull List<XpReward> xp,
            @Nonnull List<ItemReward> items,
            @Nonnull List<String> unlocks) {
        this.questPoints = Math.max(0, questPoints);
        this.xp = List.copyOf(xp);
        this.items = List.copyOf(items);
        this.unlocks = List.copyOf(unlocks);
    }

    @Nonnull public static QuestReward empty() {
        return new QuestReward(0, List.of(), List.of(), List.of());
    }

    // Getters...
}
```

---

## QuestStep

Base class for all step types:

```java
public abstract class QuestStep {
    private final String id;
    private final StepType type;
    private final String journalText;

    protected QuestStep(@Nonnull String id, @Nonnull StepType type, @Nonnull String journalText) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.journalText = Objects.requireNonNull(journalText);
    }

    @Nonnull public String id() { return id; }
    @Nonnull public StepType type() { return type; }
    @Nonnull public String journalText() { return journalText; }
}
```

### DialogStep

```java
public final class DialogStep extends QuestStep {
    private final String npcId;
    private final String dialogId;
    private final boolean completesStep;
    private final List<ItemRequirement> requiredItems;
    private final String requiredSkill;  // nullable

    public DialogStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String npcId,
            @Nonnull String dialogId,
            boolean completesStep,
            @Nonnull List<ItemRequirement> requiredItems,
            @Nullable String requiredSkill) {
        super(id, StepType.DIALOG, journalText);
        this.npcId = Objects.requireNonNull(npcId);
        this.dialogId = Objects.requireNonNull(dialogId);
        this.completesStep = completesStep;
        this.requiredItems = List.copyOf(requiredItems);
        this.requiredSkill = requiredSkill;
    }

    // Getters...
}
```

### DeliveryStep

```java
public final class DeliveryStep extends QuestStep {
    private final String targetType; // e.g. "NPC", "OBJECT"
    private final String targetId;
    private final String dialogId;  // optional
    private final List<ItemRequirement> items;
    private final boolean consumeItems;

    public DeliveryStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String targetType,
            @Nonnull String targetId,
            @Nullable String dialogId,
            @Nonnull List<ItemRequirement> items,
            boolean consumeItems) {
        super(id, StepType.DELIVERY, journalText);
        this.targetType = Objects.requireNonNull(targetType);
        this.targetId = Objects.requireNonNull(targetId);
        this.dialogId = dialogId;
        this.items = List.copyOf(items);
        this.consumeItems = consumeItems;
    }

    // Getters...
}
```

### CollectStep

```java
public final class CollectStep extends QuestStep {
    private final List<ItemRequirement> items;
    private final boolean autoProgress;

    public CollectStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull List<ItemRequirement> items,
            boolean autoProgress) {
        super(id, StepType.COLLECT, journalText);
        this.items = List.copyOf(items);
        this.autoProgress = autoProgress;
    }

    // Getters...
}
```

### KillStep

```java
public final class KillStep extends QuestStep {
    private final String mobId;
    private final int quantity;
    private final String requiredItem;  // nullable (e.g., silverlight for delrith)

    public KillStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String mobId,
            int quantity,
            @Nullable String requiredItem) {
        super(id, StepType.KILL, journalText);
        this.mobId = Objects.requireNonNull(mobId);
        this.quantity = Math.max(1, quantity);
        this.requiredItem = requiredItem;
    }

    // Getters...
}
```

### SkillActionStep

```java
public final class SkillActionStep extends QuestStep {
    private final String skillId;
    private final String actionNode;
    private final int quantity;

    public SkillActionStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String skillId,
            @Nonnull String actionNode,
            int quantity) {
        super(id, StepType.SKILL_ACTION, journalText);
        this.skillId = Objects.requireNonNull(skillId);
        this.actionNode = Objects.requireNonNull(actionNode);
        this.quantity = Math.max(1, quantity);
    }

    // Getters...
}
```

### ReachStep

```java
public final class ReachStep extends QuestStep {
    private final String zoneId;
    private final double triggerRadius;

    public ReachStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String zoneId,
            double triggerRadius) {
        super(id, StepType.REACH, journalText);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.triggerRadius = triggerRadius > 0 ? triggerRadius : 3.0;
    }

    // Getters...
}
```

### UseItemOnStep

```java
public final class UseItemOnStep extends QuestStep {
    public enum TargetType { OBJECT, NPC, LOCATION }

    private final String itemId;
    private final TargetType targetType;
    private final String targetId;
    private final boolean consumesItem;
    private final String replacementItemId; // nullable (e.g., empty bucket)

    public UseItemOnStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String itemId,
            @Nonnull TargetType targetType,
            @Nonnull String targetId,
            boolean consumesItem,
            @Nullable String replacementItemId) {
        super(id, StepType.USE_ITEM_ON, journalText);
        this.itemId = Objects.requireNonNull(itemId);
        this.targetType = Objects.requireNonNull(targetType);
        this.targetId = Objects.requireNonNull(targetId);
        this.consumesItem = consumesItem;
        this.replacementItemId = replacementItemId;
    }

    // Getters...
}
```

### EmoteStep

```java
public final class EmoteStep extends QuestStep {
    private final String emoteId;
    private final String zoneId;  // optional location requirement

    public EmoteStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String emoteId,
            @Nullable String zoneId) {
        super(id, StepType.EMOTE, journalText);
        this.emoteId = Objects.requireNonNull(emoteId);
        this.zoneId = zoneId;
    }

    // Getters...
}
```

### EquipStep

```java
public final class EquipStep extends QuestStep {
    private final String itemId;
    private final String slot;  // optional specific slot

    public EquipStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String itemId,
            @Nullable String slot) {
        super(id, StepType.EQUIP, journalText);
        this.itemId = Objects.requireNonNull(itemId);
        this.slot = slot;
    }

    // Getters...
}
```

### ChooseStep (Branching)

```java
public final class ChooseStep extends QuestStep {
    private final List<Branch> branches;

    public ChooseStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull List<Branch> branches) {
        super(id, StepType.CHOOSE, journalText);
        this.branches = List.copyOf(branches);
    }

    @Nonnull public List<Branch> branches() { return branches; }

    public static final class Branch {
        private final String id;
        private final String label;
        private final BranchCondition condition;  // nullable
        private final List<QuestStep> steps;

        public Branch(
                @Nonnull String id,
                @Nonnull String label,
                @Nullable BranchCondition condition,
                @Nonnull List<QuestStep> steps) {
            this.id = Objects.requireNonNull(id);
            this.label = Objects.requireNonNull(label);
            this.condition = condition;
            this.steps = List.copyOf(steps);
        }

        // Getters...
    }

    public static final class BranchCondition {
        private final String type;  // "has_item", "skill", etc.
        private final Map<String, Object> data;

        public BranchCondition(@Nonnull String type, @Nonnull Map<String, Object> data) {
            this.type = Objects.requireNonNull(type);
            this.data = Map.copyOf(data);
        }

        // Getters...
    }
}
```

### CustomStep

```java
public final class CustomStep extends QuestStep {
    private final String handlerId;
    private final Map<String, Object> data;

    public CustomStep(
            @Nonnull String id,
            @Nonnull String journalText,
            @Nonnull String handlerId,
            @Nonnull Map<String, Object> data) {
        super(id, StepType.CUSTOM, journalText);
        this.handlerId = Objects.requireNonNull(handlerId);
        this.data = Map.copyOf(data);
    }

    // Getters...
}
```

---

## QuestDefinition

The root definition loaded from JSON:

```java
public final class QuestDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final QuestDifficulty difficulty;
    private final QuestLength length;
    private final String series;  // nullable
    private final QuestPrerequisites prerequisites;
    private final List<QuestStep> steps;
    private final String completionJournal;
    private final QuestReward rewards;

    public QuestDefinition(
            @Nonnull String id,
            @Nonnull String name,
            @Nonnull String description,
            @Nonnull QuestDifficulty difficulty,
            @Nonnull QuestLength length,
            @Nullable String series,
            @Nonnull QuestPrerequisites prerequisites,
            @Nonnull List<QuestStep> steps,
            @Nonnull String completionJournal,
            @Nonnull QuestReward rewards) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.difficulty = Objects.requireNonNull(difficulty);
        this.length = Objects.requireNonNull(length);
        this.series = series;
        this.prerequisites = Objects.requireNonNull(prerequisites);
        this.steps = List.copyOf(steps);
        this.completionJournal = Objects.requireNonNull(completionJournal);
        this.rewards = Objects.requireNonNull(rewards);
    }

    // Getters...

    public int totalQuestPoints() {
        return rewards.questPoints();
    }
}
```

---

## QuestProgress (Player State)

```java
public final class QuestProgress {
    private final String questId;
    private final QuestStatus status;
    private final String currentStepId;
    private final Map<String, Integer> stepProgress;  // stepId -> count (for kill/collect)
    private final String chosenBranchId;  // for CHOOSE steps
    private final Instant startedAt;
    private final Instant completedAt;

    public QuestProgress(
            @Nonnull String questId,
            @Nonnull QuestStatus status,
            @Nullable String currentStepId,
            @Nonnull Map<String, Integer> stepProgress,
            @Nullable String chosenBranchId,
            @Nullable Instant startedAt,
            @Nullable Instant completedAt) {
        this.questId = Objects.requireNonNull(questId);
        this.status = Objects.requireNonNull(status);
        this.currentStepId = currentStepId;
        this.stepProgress = Map.copyOf(stepProgress);
        this.chosenBranchId = chosenBranchId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    // Factory methods
    @Nonnull
    public static QuestProgress notStarted(@Nonnull String questId) {
        return new QuestProgress(questId, QuestStatus.NOT_STARTED, null, Map.of(), null, null, null);
    }

    @Nonnull
    public QuestProgress start() {
        if (status != QuestStatus.NOT_STARTED) {
            throw new IllegalStateException("Quest already started: " + questId);
        }
        return new QuestProgress(questId, QuestStatus.IN_PROGRESS, currentStepId, stepProgress, chosenBranchId, Instant.now(), null);
    }

    @Nonnull
    public QuestProgress withCurrentStep(@Nullable String stepId) {
        return new QuestProgress(questId, status, stepId, stepProgress, chosenBranchId, startedAt, completedAt);
    }

    @Nonnull
    public QuestProgress withStepProgress(@Nonnull String stepId, int progress) {
        Map<String, Integer> newProgress = new HashMap<>(stepProgress);
        newProgress.put(stepId, progress);
        return new QuestProgress(questId, status, currentStepId, Map.copyOf(newProgress), chosenBranchId, startedAt, completedAt);
    }

    @Nonnull
    public QuestProgress withChosenBranch(@Nonnull String branchId) {
        return new QuestProgress(questId, status, currentStepId, stepProgress, branchId, startedAt, completedAt);
    }

    @Nonnull
    public QuestProgress complete() {
        if (status != QuestStatus.IN_PROGRESS) {
            throw new IllegalStateException("Quest not in progress: " + questId);
        }
        return new QuestProgress(questId, QuestStatus.COMPLETED, null, stepProgress, chosenBranchId, startedAt, Instant.now());
    }

    // Getters...

    public int getStepProgress(@Nonnull String stepId) {
        return stepProgress.getOrDefault(stepId, 0);
    }
}
```

---

## JSON Codec Design

Use Gson with custom TypeAdapters for polymorphic step deserialization:

```java
public final class QuestStepAdapter extends TypeAdapter<QuestStep> {
    @Override
    public QuestStep read(JsonReader in) throws IOException {
        JsonObject obj = JsonParser.parseReader(in).getAsJsonObject();
        String typeStr = obj.get("type").getAsString();
        StepType type = StepType.fromString(typeStr);

        switch (type) {
            case DIALOG -> { return parseDialogStep(obj); }
            case DELIVERY -> { return parseDeliveryStep(obj); }
            case COLLECT -> { return parseCollectStep(obj); }
            case KILL -> { return parseKillStep(obj); }
            case SKILL_ACTION -> { return parseSkillActionStep(obj); }
            case REACH -> { return parseReachStep(obj); }
            case USE_ITEM_ON -> { return parseUseItemOnStep(obj); }
            case EMOTE -> { return parseEmoteStep(obj); }
            case EQUIP -> { return parseEquipStep(obj); }
            case CHOOSE -> { return parseChooseStep(obj); }
            case CUSTOM -> { return parseCustomStep(obj); }
            default -> throw new IllegalArgumentException("Unknown step type: " + type);
        }
    }

    // ... parse methods for each type
}
```

---

## Test Cases

```java
class QuestProgressTest {

    @Test
    void startTransitionsFromNotStartedToInProgress() {
        QuestProgress progress = QuestProgress.notStarted("cooks_assistant");
        assertThat(progress.status()).isEqualTo(QuestStatus.NOT_STARTED);

        QuestProgress started = progress.start();
        assertThat(started.status()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(started.startedAt()).isNotNull();
    }

    @Test
    void startThrowsWhenAlreadyInProgress() {
        QuestProgress progress = QuestProgress.notStarted("quest").start();
        assertThatThrownBy(progress::start)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stepProgressIsImmutable() {
        QuestProgress progress = QuestProgress.notStarted("quest")
            .withStepProgress("kill_goblins", 3);
        
        assertThat(progress.getStepProgress("kill_goblins")).isEqualTo(3);
        assertThat(progress.getStepProgress("unknown")).isEqualTo(0);
    }
}

class StepTypeTest {

    @Test
    void fromStringParsesValidTypes() {
        assertThat(StepType.fromString("DIALOG")).isEqualTo(StepType.DIALOG);
        assertThat(StepType.fromString("dialog")).isEqualTo(StepType.DIALOG);
    }

    @Test
    void tryParseStrictReturnsNullForInvalid() {
        assertThat(StepType.tryParseStrict("invalid")).isNull();
        assertThat(StepType.tryParseStrict("")).isNull();
        assertThat(StepType.tryParseStrict(null)).isNull();
    }
}
```
