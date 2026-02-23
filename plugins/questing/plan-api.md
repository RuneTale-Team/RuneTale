# Questing Plugin - Runtime API

## Overview

This document defines the public API interfaces that other plugins use to interact with the questing system.

---

## QuestRuntimeApi

The main interface implemented by QuestingPlugin:

```java
package org.runetale.questing.api;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.QuestDefinition;
import org.runetale.questing.domain.QuestProgress;
import org.runetale.questing.domain.QuestStatus;
import org.runetale.questing.service.CustomPrerequisiteService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

public interface QuestRuntimeApi {

    // ==================== Quest Registry ====================

    @Nonnull Collection<String> getQuestIds();

    @Nonnull Collection<String> getQuestIdsByDifficulty(@Nonnull String difficulty);

    @Nonnull Collection<String> getQuestIdsBySeries(@Nonnull String series);

    @Nullable QuestDefinition getQuestDefinition(@Nonnull String questId);

    boolean questExists(@Nonnull String questId);

    // ==================== Quest State Queries ====================

    @Nonnull QuestStatus getQuestStatus(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    boolean isQuestStarted(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    boolean isQuestActive(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    boolean isQuestComplete(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    @Nonnull Optional<QuestProgress> getQuestProgress(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    @Nonnull Collection<QuestProgress> getActiveQuests(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef);

    @Nonnull Collection<String> getCompletedQuestIds(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef);

    // ==================== Quest Operations ====================

    boolean canStartQuest(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    @Nonnull StartQuestResult startQuest(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    boolean advanceQuest(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    boolean advanceQuestToStep(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId,
            @Nonnull String stepId);

    @Nonnull CompleteQuestResult completeQuest(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    // ==================== Progress Queries ====================

    @Nullable String getCurrentStepId(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId);

    int getStepProgress(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String questId,
            @Nonnull String stepId);

    // ==================== Quest Points ====================

    int getTotalQuestPoints(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef);

    int getMaximumQuestPoints();

    // ==================== Custom Prerequisites ====================

    @Nonnull CustomPrerequisiteService getCustomPrerequisiteService();
}
```

---

## StartQuestResult

```java
package org.runetale.questing.api;

public final class StartQuestResult {
    private final boolean success;
    private final String failureReason;  // null if success
    private final String firstStepId;    // null if failure

    private StartQuestResult(boolean success, @Nullable String failureReason, @Nullable String firstStepId) {
        this.success = success;
        this.failureReason = failureReason;
        this.firstStepId = firstStepId;
    }

    @Nonnull
    public static StartQuestResult success(@Nonnull String firstStepId) {
        return new StartQuestResult(true, null, firstStepId);
    }

    @Nonnull
    public static StartQuestResult failure(@Nonnull String reason) {
        return new StartQuestResult(false, reason, null);
    }

    public boolean success() { return success; }
    @Nullable public String failureReason() { return failureReason; }
    @Nullable public String firstStepId() { return firstStepId; }
}

---

## CompleteQuestResult

```java
package org.runetale.questing.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CompleteQuestResult {
    private final boolean success;
    private final String failureReason;  // null if success

    private CompleteQuestResult(boolean success, @Nullable String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    @Nonnull
    public static CompleteQuestResult success() {
        return new CompleteQuestResult(true, null);
    }

    @Nonnull
    public static CompleteQuestResult failure(@Nonnull String reason) {
        return new CompleteQuestResult(false, reason);
    }

    public boolean success() { return success; }
    @Nullable public String failureReason() { return failureReason; }
}
```
```

---

## QuestRuntimeRegistry

Singleton registry following the skills plugin pattern:

```java
package org.runetale.questing.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestRuntimeRegistry {

    @Nullable
    private static volatile QuestRuntimeApi runtimeApi;

    private QuestRuntimeRegistry() {
    }

    public static void register(@Nonnull QuestRuntimeApi runtime) {
        runtimeApi = runtime;
    }

    public static void clear(@Nonnull QuestRuntimeApi runtime) {
        if (runtimeApi == runtime) {
            runtimeApi = null;
        }
    }

    @Nullable
    public static QuestRuntimeApi get() {
        return runtimeApi;
    }

    @Nonnull
    public static QuestRuntimeApi getOrThrow() {
        QuestRuntimeApi api = runtimeApi;
        if (api == null) {
            throw new IllegalStateException("QuestRuntimeApi not registered. Is the questing plugin loaded?");
        }
        return api;
    }
}
```

---

## CustomPrerequisiteService Interface

```java
package org.runetale.questing.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

public interface CustomPrerequisiteService {

    void register(@Nonnull String prerequisiteId, @Nonnull PrerequisiteHandler handler);

    void registerPattern(@Nonnull String prefix, @Nonnull PatternPrerequisiteHandler handler);

    boolean unregister(@Nonnull String prerequisiteId);

    boolean check(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String prerequisiteId);

    @FunctionalInterface
    interface PrerequisiteHandler {
        boolean check(
                @Nonnull ComponentAccessor<EntityStore> accessor,
                @Nonnull Ref<EntityStore> playerRef);
    }

    @FunctionalInterface
    interface PatternPrerequisiteHandler {
        boolean check(
                @Nonnull String fullId,
                @Nonnull ComponentAccessor<EntityStore> accessor,
                @Nonnull Ref<EntityStore> playerRef);
    }
}
```

---

## Quest Events

Events for cross-plugin communication:

### QuestStartEvent

```java
package org.runetale.questing.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.QuestDefinition;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class QuestStartEvent {
    private final UUID playerId;
    private final Ref<EntityStore> playerRef;
    private final QuestDefinition quest;
    private final String firstStepId;

    public QuestStartEvent(
            @Nonnull UUID playerId,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestDefinition quest,
            @Nonnull String firstStepId) {
        this.playerId = playerId;
        this.playerRef = playerRef;
        this.quest = quest;
        this.firstStepId = firstStepId;
    }

    // Getters...
}
```

### QuestStepCompleteEvent

```java
package org.runetale.questing.event;

import org.runetale.questing.domain.QuestDefinition;
import org.runetale.questing.domain.QuestStep;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class QuestStepCompleteEvent {
    private final UUID playerId;
    private final QuestDefinition quest;
    private final QuestStep completedStep;
    private final String nextStepId;  // null if quest complete

    public QuestStepCompleteEvent(
            @Nonnull UUID playerId,
            @Nonnull QuestDefinition quest,
            @Nonnull QuestStep completedStep,
            @Nullable String nextStepId) {
        this.playerId = playerId;
        this.quest = quest;
        this.completedStep = completedStep;
        this.nextStepId = nextStepId;
    }

    public boolean isQuestComplete() {
        return nextStepId == null;
    }

    // Getters...
}
```

### QuestCompleteEvent

```java
package org.runetale.questing.event;

import org.runetale.questing.domain.QuestDefinition;
import org.runetale.questing.domain.QuestReward;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class QuestCompleteEvent {
    private final UUID playerId;
    private final QuestDefinition quest;
    private final QuestReward rewards;
    private final Instant startedAt;
    private final Instant completedAt;

    public QuestCompleteEvent(
            @Nonnull UUID playerId,
            @Nonnull QuestDefinition quest,
            @Nonnull QuestReward rewards,
            @Nonnull Instant startedAt,
            @Nonnull Instant completedAt) {
        this.playerId = playerId;
        this.quest = quest;
        this.rewards = rewards;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @Nonnull
    public Duration completionTime() {
        return Duration.between(startedAt, completedAt);
    }

    // Getters...
}
```

---

## Integration Examples

### Dialog Plugin Starting a Quest

```java
// In a dialog effect handler
public class StartQuestHandler implements EffectHandler {
    @Override
    public String effectType() { return "start_quest"; }

    @Override
    public void execute(UUID playerId, JsonObject data, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        String questId = data.get("quest_id").getAsString();
        
        QuestRuntimeApi questApi = QuestRuntimeRegistry.getOrThrow();
        StartQuestResult result = questApi.startQuest(accessor, playerRef, questId);
        
        if (!result.success()) {
            LOGGER.atWarning().log("Failed to start quest %s for player %s: %s", 
                questId, playerId, result.failureReason());
        }
    }
}
```

### Skills Plugin Checking Quest Requirements

```java
// Checking if player meets quest skill prerequisites
public boolean meetsSkillPrerequisites(
        ComponentAccessor<EntityStore> accessor,
        Ref<EntityStore> playerRef,
        QuestDefinition quest) {
    
    SkillsRuntimeApi skillsApi = SkillsRuntimeRegistry.getOrThrow();
    
    for (Map.Entry<String, Integer> req : quest.prerequisites().skills().entrySet()) {
        SkillType skill = SkillType.fromString(req.getKey());
        int requiredLevel = req.getValue();
        int actualLevel = skillsApi.getSkillLevel(accessor, playerRef, skill);
        
        if (actualLevel < requiredLevel) {
            return false;
        }
    }
    return true;
}
```

### Custom Prerequisite Registration

```java
// Faction plugin registers custom prerequisite
public class FactionPrerequisiteHandler implements CustomPrerequisiteService.PatternPrerequisiteHandler {
    
    private final FactionService factionService;

    @Override
    public boolean check(String fullId, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        // Parse "faction:goblin:friendly"
        String[] parts = fullId.split(":");
        if (parts.length != 3) return false;
        
        String factionId = parts[1];
        String requiredStanding = parts[2];
        
        Standing standing = factionService.getStanding(accessor, playerRef, factionId);
        return standing.name().toLowerCase(Locale.ROOT).equals(requiredStanding);
    }
}

// Registration
QuestRuntimeApi questApi = QuestRuntimeRegistry.getOrThrow();
questApi.getCustomPrerequisiteService().registerPattern("faction:", new FactionPrerequisiteHandler());
```

---

## API Design Decisions

### Why return Optional<QuestProgress>?

The `getQuestProgress` method returns `Optional` because:
- A quest that hasn't been started has no progress entry
- This is distinct from a quest with `NOT_STARTED` status (which would exist in the progress map)

### Why separate canStartQuest and startQuest?

- `canStartQuest` checks prerequisites without side effects
- `startQuest` validates AND performs the state transition
- Callers can use `canStartQuest` for UI display (grayed out with reason)
- `startQuest` returns detailed failure reasons

### Why use String IDs instead of Enums?

- Quest IDs are defined in JSON data files, not code
- Plugins can add quests without modifying the questing plugin
- Step IDs are unique within a quest, not globally
- Enum-like parsing methods provide type safety where needed

### Why BiPredicate-style handlers for prerequisites?

- Allows inline lambda registration: `service.register("check", (a, p) -> true)`
- Pattern handlers support colon-separated IDs: `faction:goblin:friendly`
- No interface implementation required for simple checks
