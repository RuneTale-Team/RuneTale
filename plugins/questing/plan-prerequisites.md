# Questing Plugin - Prerequisites System

## Overview

The prerequisites system determines whether a player can start a quest. It supports:
- Skill level requirements
- Quest completion requirements
- Item requirements
- Quest point requirements
- Custom handler requirements (extensible)

---

## PrerequisiteCheckResult

```java
package org.runetale.questing.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PrerequisiteCheckResult {
    private final boolean satisfied;
    private final List<String> missingRequirements;

    private PrerequisiteCheckResult(boolean satisfied, @Nonnull List<String> missing) {
        this.satisfied = satisfied;
        this.missingRequirements = Collections.unmodifiableList(missing);
    }

    @Nonnull
    public static PrerequisiteCheckResult satisfied() {
        return new PrerequisiteCheckResult(true, List.of());
    }

    @Nonnull
    public static PrerequisiteCheckResult missing(@Nonnull String requirement) {
        return new PrerequisiteCheckResult(false, List.of(requirement));
    }

    @Nonnull
    public static PrerequisiteCheckResult missing(@Nonnull List<String> requirements) {
        return new PrerequisiteCheckResult(false, new ArrayList<>(requirements));
    }

    @Nonnull
    public static PrerequisiteCheckResult combine(@Nonnull List<PrerequisiteCheckResult> results) {
        List<String> allMissing = new ArrayList<>();
        for (PrerequisiteCheckResult r : results) {
            allMissing.addAll(r.missingRequirements);
        }
        return allMissing.isEmpty() ? satisfied() : missing(allMissing);
    }

    public boolean satisfied() { return satisfied; }
    @Nonnull public List<String> missingRequirements() { return missingRequirements; }
}
```

---

## PrerequisiteService

Main service for checking all prerequisite types:

```java
package org.runetale.questing.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.QuestPrerequisites;
import org.runetale.questing.domain.PrerequisiteCheckResult;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PrerequisiteService {

    private final SkillsIntegration skillsIntegration;
    private final ItemIntegration itemIntegration;
    private final CustomPrerequisiteService customPrerequisiteService;

    public PrerequisiteService(
            @Nullable SkillsIntegration skillsIntegration,
            @Nullable ItemIntegration itemIntegration,
            @Nonnull CustomPrerequisiteService customPrerequisiteService) {
        this.skillsIntegration = skillsIntegration;
        this.itemIntegration = itemIntegration;
        this.customPrerequisiteService = customPrerequisiteService;
    }

    @Nonnull
    public PrerequisiteCheckResult check(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestPrerequisites prerequisites,
            @Nonnull CompletedQuestsProvider completedQuests) {

        List<PrerequisiteCheckResult> results = new ArrayList<>();

        results.add(checkSkillRequirements(accessor, playerRef, prerequisites));
        results.add(checkQuestRequirements(prerequisites, completedQuests));
        results.add(checkItemRequirements(accessor, playerRef, prerequisites));
        results.add(checkQuestPointRequirements(prerequisites, completedQuests));
        results.add(checkCustomRequirements(accessor, playerRef, prerequisites));

        return PrerequisiteCheckResult.combine(results);
    }

    @Nonnull
    private PrerequisiteCheckResult checkSkillRequirements(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestPrerequisites prerequisites) {

        if (prerequisites.skills().isEmpty()) {
            return PrerequisiteCheckResult.satisfied();
        }

        if (skillsIntegration == null) {
            return PrerequisiteCheckResult.missing("Skills plugin not available");
        }

        List<String> missing = new ArrayList<>();
        for (var entry : prerequisites.skills().entrySet()) {
            String skillId = entry.getKey();
            int requiredLevel = entry.getValue();
            int actualLevel = skillsIntegration.getSkillLevel(accessor, playerRef, skillId);

            if (actualLevel < requiredLevel) {
                missing.add(String.format(Locale.ROOT, "%s level %d (have %d)", 
                    formatSkillName(skillId), requiredLevel, actualLevel));
            }
        }

        return missing.isEmpty() 
            ? PrerequisiteCheckResult.satisfied() 
            : PrerequisiteCheckResult.missing(missing);
    }

    @Nonnull
    private PrerequisiteCheckResult checkQuestRequirements(
            @Nonnull QuestPrerequisites prerequisites,
            @Nonnull CompletedQuestsProvider completedQuests) {

        if (prerequisites.quests().isEmpty()) {
            return PrerequisiteCheckResult.satisfied();
        }

        List<String> missing = new ArrayList<>();
        for (String questId : prerequisites.quests()) {
            if (!completedQuests.isComplete(questId)) {
                missing.add("Quest: " + formatQuestName(questId));
            }
        }

        return missing.isEmpty() 
            ? PrerequisiteCheckResult.satisfied() 
            : PrerequisiteCheckResult.missing(missing);
    }

    @Nonnull
    private PrerequisiteCheckResult checkItemRequirements(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestPrerequisites prerequisites) {

        if (prerequisites.items().isEmpty()) {
            return PrerequisiteCheckResult.satisfied();
        }

        if (itemIntegration == null) {
            return PrerequisiteCheckResult.missing("Item system not available");
        }

        List<String> missing = new ArrayList<>();
        for (var req : prerequisites.items()) {
            int has = itemIntegration.getItemCount(accessor, playerRef, req.itemId());
            if (has < req.quantity()) {
                missing.add(String.format(Locale.ROOT, "%s x%d", 
                    formatItemName(req.itemId()), req.quantity()));
            }
        }

        return missing.isEmpty() 
            ? PrerequisiteCheckResult.satisfied() 
            : PrerequisiteCheckResult.missing(missing);
    }

    @Nonnull
    private PrerequisiteCheckResult checkQuestPointRequirements(
            @Nonnull QuestPrerequisites prerequisites,
            @Nonnull CompletedQuestsProvider completedQuests) {

        int required = prerequisites.questPoints();
        if (required == 0) {
            return PrerequisiteCheckResult.satisfied();
        }

        int actual = completedQuests.getTotalQuestPoints();
        if (actual < required) {
            return PrerequisiteCheckResult.missing(
                String.format(Locale.ROOT, "%d Quest Points (have %d)", required, actual));
        }

        return PrerequisiteCheckResult.satisfied();
    }

    @Nonnull
    private PrerequisiteCheckResult checkCustomRequirements(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestPrerequisites prerequisites) {

        if (prerequisites.custom().isEmpty()) {
            return PrerequisiteCheckResult.satisfied();
        }

        List<String> missing = new ArrayList<>();
        for (String customId : prerequisites.custom()) {
            if (!customPrerequisiteService.check(accessor, playerRef, customId)) {
                missing.add(formatCustomRequirement(customId));
            }
        }

        return missing.isEmpty() 
            ? PrerequisiteCheckResult.satisfied() 
            : PrerequisiteCheckResult.missing(missing);
    }

    @Nonnull
    private String formatSkillName(@Nonnull String skillId) {
        return skillId.replace("_", " ").toLowerCase(Locale.ROOT);
    }

    @Nonnull
    private String formatQuestName(@Nonnull String questId) {
        return questId.replace("_", " ");
    }

    @Nonnull
    private String formatItemName(@Nonnull String itemId) {
        return itemId.replace("_", " ");
    }

    @Nonnull
    private String formatCustomRequirement(@Nonnull String customId) {
        if (customId.startsWith("faction:")) {
            String[] parts = customId.split(":");
            return parts[1] + " faction standing: " + parts[2];
        }
        if (customId.startsWith("task:")) {
            return "Task completion required";
        }
        return customId;
    }
}
```

---

## Integration Interfaces

### SkillsIntegration

```java
package org.runetale.questing.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public interface SkillsIntegration {
    int getSkillLevel(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String skillId);
}
```

### ItemIntegration

```java
package org.runetale.questing.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public interface ItemIntegration {
    int getItemCount(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId);

    boolean hasItemEquipped(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId);

    int getFreeInventorySlots(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef);
}
```

### CompletedQuestsProvider

```java
package org.runetale.questing.service;

import javax.annotation.Nonnull;
import java.util.Set;

public interface CompletedQuestsProvider {
    boolean isComplete(@Nonnull String questId);
    int getTotalQuestPoints();
    @Nonnull Set<String> getCompletedQuestIds();
}
```

---

## CustomPrerequisiteService Implementation

```java
package org.runetale.questing.service;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.api.CustomPrerequisiteService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomPrerequisiteServiceImpl implements CustomPrerequisiteService {

    private final Map<String, PrerequisiteHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, PatternPrerequisiteHandler> patternHandlers = new ConcurrentHashMap<>();

    @Override
    public void register(@Nonnull String prerequisiteId, @Nonnull PrerequisiteHandler handler) {
        handlers.put(prerequisiteId, handler);
    }

    @Override
    public void registerPattern(@Nonnull String prefix, @Nonnull PatternPrerequisiteHandler handler) {
        patternHandlers.put(prefix, handler);
    }

    @Override
    public boolean unregister(@Nonnull String prerequisiteId) {
        return handlers.remove(prerequisiteId) != null;
    }

    @Override
    public boolean check(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String prerequisiteId) {

        PrerequisiteHandler handler = handlers.get(prerequisiteId);
        if (handler != null) {
            return handler.check(accessor, playerRef);
        }

        for (var entry : patternHandlers.entrySet()) {
            if (prerequisiteId.startsWith(entry.getKey())) {
                return entry.getValue().check(prerequisiteId, accessor, playerRef);
            }
        }

        return false;
    }
}
```

---

## Built-in Custom Prerequisite Handlers

### Faction Prerequisite Handler

```java
package org.runetale.questing.handler.prerequisite;

import org.runetale.questing.service.CustomPrerequisiteService;

public final class FactionPrerequisiteHandler implements CustomPrerequisiteService.PatternPrerequisiteHandler {

    private final FactionService factionService;

    public FactionPrerequisiteHandler(@Nonnull FactionService factionService) {
        this.factionService = factionService;
    }

    @Override
    public boolean check(
            @Nonnull String fullId,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        String[] parts = fullId.split(":");
        if (parts.length != 3) {
            return false;
        }

        String factionId = parts[1];
        String requiredStanding = parts[2].toUpperCase(Locale.ROOT);

        Standing standing = factionService.getStanding(accessor, playerRef, factionId);
        return standing.name().equals(requiredStanding);
    }
}

// Registration:
customPrerequisiteService.registerPattern("faction:", new FactionPrerequisiteHandler(factionService));
```

### Task Count Prerequisite Handler

```java
package org.runetale.questing.handler.prerequisite;

import org.runetale.questing.service.CustomPrerequisiteService;

public final class TaskCountPrerequisiteHandler implements CustomPrerequisiteService.PatternPrerequisiteHandler {

    private final TaskService taskService;

    public TaskCountPrerequisiteHandler(@Nonnull TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public boolean check(
            @Nonnull String fullId,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        String[] parts = fullId.split(":");
        if (parts.length != 3) {
            return false;
        }

        String taskId = parts[1];
        int required = Integer.parseInt(parts[2]);

        int actual = taskService.getCompletionCount(accessor, playerRef, taskId);
        return actual >= required;
    }
}

// Registration:
customPrerequisiteService.registerPattern("task:", new TaskCountPrerequisiteHandler(taskService));
```

### Statistic Prerequisite Handler

```java
package org.runetale.questing.handler.prerequisite;

import org.runetale.questing.service.CustomPrerequisiteService;

public final class StatPrerequisiteHandler implements CustomPrerequisiteService.PatternPrerequisiteHandler {

    private final StatService statService;

    public StatPrerequisiteHandler(@Nonnull StatService statService) {
        this.statService = statService;
    }

    @Override
    public boolean check(
            @Nonnull String fullId,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        String[] parts = fullId.split(":");
        if (parts.length != 3) {
            return false;
        }

        String statKey = parts[1];
        long required = Long.parseLong(parts[2]);

        long actual = statService.getStat(accessor, playerRef, statKey);
        return actual >= required;
    }
}

// Registration:
customPrerequisiteService.registerPattern("stat:", new StatPrerequisiteHandler(statService));
```

### Minigame Prerequisite Handler

```java
package org.runetale.questing.handler.prerequisite;

import org.runetale.questing.service.CustomPrerequisiteService;

public final class MinigamePrerequisiteHandler implements CustomPrerequisiteService.PatternPrerequisiteHandler {

    private final MinigameService minigameService;

    public MinigamePrerequisiteHandler(@Nonnull MinigameService minigameService) {
        this.minigameService = minigameService;
    }

    @Override
    public boolean check(
            @Nonnull String fullId,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        String[] parts = fullId.split(":");
        if (parts.length != 4) {
            return false;
        }

        String minigameId = parts[1];
        String statType = parts[2];
        int required = Integer.parseInt(parts[3]);

        int actual = minigameService.getStat(accessor, playerRef, minigameId, statType);
        return actual >= required;
    }
}

// Registration:
customPrerequisiteService.registerPattern("minigame:", new MinigamePrerequisiteHandler(minigameService));
```

---

## Skills Plugin Integration

```java
package org.runetale.questing.integration;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.service.SkillsIntegration;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.api.SkillsRuntimeRegistry;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SkillsPluginIntegration implements SkillsIntegration {

    @Nullable
    private SkillsRuntimeApi skillsApi;

    public SkillsPluginIntegration() {
        refreshApi();
    }

    public void refreshApi() {
        this.skillsApi = SkillsRuntimeRegistry.get();
    }

    @Override
    public int getSkillLevel(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String skillId) {

        if (skillsApi == null) {
            refreshApi();
            if (skillsApi == null) {
                return 1;
            }
        }

        SkillType skillType = SkillType.tryParseStrict(skillId);
        if (skillType == null) {
            return 1;
        }

        return skillsApi.getSkillLevel(accessor, playerRef, skillType);
    }
}
```

---

## Quest Plugin Setup

```java
private void setupPrerequisiteService() {
    CustomPrerequisiteService customService = new CustomPrerequisiteServiceImpl();
    SkillsIntegration skillsIntegration = new SkillsPluginIntegration();
    ItemIntegration itemIntegration = new ItemPluginIntegration();

    this.prerequisiteService = new PrerequisiteService(
        skillsIntegration,
        itemIntegration,
        customService
    );

    registerBuiltinPrerequisiteHandlers(customService);
}

private void registerBuiltinPrerequisiteHandlers(@Nonnull CustomPrerequisiteService service) {
    // These require external services that may or may not be available
    if (factionService != null) {
        service.registerPattern("faction:", new FactionPrerequisiteHandler(factionService));
    }
    if (taskService != null) {
        service.registerPattern("task:", new TaskCountPrerequisiteHandler(taskService));
    }
    if (statService != null) {
        service.registerPattern("stat:", new StatPrerequisiteHandler(statService));
    }
    if (minigameService != null) {
        service.registerPattern("minigame:", new MinigamePrerequisiteHandler(minigameService));
    }
}
```

---

## Custom Prerequisite Patterns

| Pattern | Format | Example | Description |
|---------|--------|---------|-------------|
| Faction | `faction:<faction>:<standing>` | `faction:goblin:NEUTRAL` | Player faction standing |
| Task | `task:<id>:<count>` | `task:given_cook_eggs:3` | Task completion count |
| Stat | `stat:<key>:<value>` | `stat:chickens_killed:100` | Player statistic |
| Minigame | `minigame:<id>:<stat>:<value>` | `minigame:pest_control:wins:10` | Minigame statistic |
| Handler | `handler:<id>` | `handler:custom_check` | Full custom handler |

---

## Test Cases

```java
class PrerequisiteServiceTest {

    PrerequisiteService service;
    ComponentAccessor<EntityStore> accessor;
    Ref<EntityStore> playerRef;
    CompletedQuestsProvider completedQuests;

    @BeforeEach
    void setup() {
        SkillsIntegration skills = mock(SkillsIntegration.class);
        ItemIntegration items = mock(ItemIntegration.class);
        CustomPrerequisiteService custom = new CustomPrerequisiteServiceImpl();

        service = new PrerequisiteService(skills, items, custom);
    }

    @Test
    void satisfiedWhenNoPrerequisites() {
        QuestPrerequisites empty = new QuestPrerequisites(
            Map.of(), Set.of(), List.of(), 0, List.of()
        );

        PrerequisiteCheckResult result = service.check(accessor, playerRef, empty, completedQuests);

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    void missingSkillRequirement() {
        when(skillsIntegration.getSkillLevel(any(), any(), eq("COOKING"))).thenReturn(3);

        QuestPrerequisites prereqs = new QuestPrerequisites(
            Map.of("COOKING", 10), Set.of(), List.of(), 0, List.of()
        );

        PrerequisiteCheckResult result = service.check(accessor, playerRef, prereqs, completedQuests);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.missingRequirements()).contains("cooking level 10 (have 3)");
    }

    @Test
    void missingQuestRequirement() {
        when(completedQuests.isComplete("cooks_assistant")).thenReturn(false);

        QuestPrerequisites prereqs = new QuestPrerequisites(
            Map.of(), Set.of("cooks_assistant"), List.of(), 0, List.of()
        );

        PrerequisiteCheckResult result = service.check(accessor, playerRef, prereqs, completedQuests);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.missingRequirements()).contains("Quest: cooks assistant");
    }

    @Test
    void multipleMissingRequirements() {
        when(skillsIntegration.getSkillLevel(any(), any(), eq("ATTACK"))).thenReturn(5);
        when(completedQuests.isComplete("demon_slayer")).thenReturn(false);
        when(completedQuests.getTotalQuestPoints()).thenReturn(3);

        QuestPrerequisites prereqs = new QuestPrerequisites(
            Map.of("ATTACK", 20),
            Set.of("demon_slayer"),
            List.of(),
            10,
            List.of()
        );

        PrerequisiteCheckResult result = service.check(accessor, playerRef, prereqs, completedQuests);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.missingRequirements()).hasSize(3);
    }
}

class CustomPrerequisiteServiceTest {

    @Test
    void registeredHandlerIsCalled() {
        CustomPrerequisiteServiceImpl service = new CustomPrerequisiteServiceImpl();
        service.register("test_check", (accessor, ref) -> true);

        boolean result = service.check(accessor, playerRef, "test_check");

        assertThat(result).isTrue();
    }

    @Test
    void patternHandlerMatchesPrefix() {
        CustomPrerequisiteServiceImpl service = new CustomPrerequisiteServiceImpl();
        service.registerPattern("faction:", (id, accessor, ref) -> {
            return id.equals("faction:goblin:FRIENDLY");
        });

        assertThat(service.check(accessor, playerRef, "faction:goblin:FRIENDLY")).isTrue();
        assertThat(service.check(accessor, playerRef, "faction:varrock:HOSTILE")).isFalse();
    }

    @Test
    void unknownPrerequisiteReturnsFalse() {
        CustomPrerequisiteServiceImpl service = new CustomPrerequisiteServiceImpl();

        assertThat(service.check(accessor, playerRef, "unknown")).isFalse();
    }
}
```
