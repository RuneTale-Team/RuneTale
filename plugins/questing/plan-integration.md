# Questing Plugin - Cross-Plugin Integration

## Overview

This document describes how the questing plugin integrates with other RuneTale plugins and external systems.

---

## Plugin Dependencies

| Plugin | Integration Type | Purpose |
|--------|-----------------|---------|
| skills | Optional | Skill level requirements, XP rewards |
| skills-api | Required | SkillType enum, runtime API pattern |
| dialog | Optional | Quest start via dialog, dialog step handling |
| items | Future | Item requirements, item rewards |
| npc | Future | NPC interactions for dialog/delivery |
| zones | Future | Area-based quest triggers |
| factions | Optional | Custom prerequisites |

---

## Skills Plugin Integration

### Skill Level Prerequisites

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
    private SkillsRuntimeApi cachedApi;

    @Nullable
    private SkillsRuntimeApi getApi() {
        if (cachedApi == null) {
            cachedApi = SkillsRuntimeRegistry.get();
        }
        return cachedApi;
    }

    @Override
    public int getSkillLevel(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String skillId) {

        SkillsRuntimeApi api = getApi();
        if (api == null) {
            return 1;
        }

        SkillType skillType = SkillType.tryParseStrict(skillId);
        if (skillType == null) {
            return 1;
        }

        return api.getSkillLevel(accessor, playerRef, skillType);
    }

    public boolean grantXpReward(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String skillId,
            long amount) {

        SkillsRuntimeApi api = getApi();
        if (api == null) {
            return false;
        }

        SkillType skillType = SkillType.tryParseStrict(skillId);
        if (skillType == null) {
            return false;
        }

        return api.grantSkillXp(accessor, playerRef, skillType, amount, "quest", true);
    }
}
```

### Reward Distribution (XP)

```java
package org.runetale.questing.service;

import org.runetale.questing.domain.QuestReward;
import org.runetale.questing.domain.XpReward;
import org.runetale.questing.integration.SkillsPluginIntegration;

import javax.annotation.Nonnull;
import java.util.List;

public final class QuestRewardService {

    private final SkillsPluginIntegration skillsIntegration;

    public QuestRewardService(@Nullable SkillsPluginIntegration skillsIntegration) {
        this.skillsIntegration = skillsIntegration;
    }

    public void distributeRewards(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull QuestReward reward) {

        distributeXpRewards(accessor, playerRef, reward.xp());
        distributeItemRewards(accessor, playerRef, reward.items());
        applyUnlocks(accessor, playerRef, reward.unlocks());
    }

    private void distributeXpRewards(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull List<XpReward> xpRewards) {

        if (skillsIntegration == null) {
            LOGGER.atWarning().log("Cannot grant XP rewards: Skills plugin not available");
            return;
        }

        for (XpReward xp : xpRewards) {
            boolean success = skillsIntegration.grantXpReward(
                accessor, playerRef, xp.skillId(), xp.amount());

            if (success) {
                LOGGER.atInfo().log("Granted %d %s XP for quest completion", 
                    xp.amount(), xp.skillId());
            } else {
                LOGGER.atWarning().log("Failed to grant %s XP", xp.skillId());
            }
        }
    }

    private void distributeItemRewards(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull List<ItemReward> items) {
        // Future: integrate with items plugin
    }

    private void applyUnlocks(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull List<String> unlocks) {
        // Future: integrate with unlocks plugin
    }
}
```

---

## Dialog Plugin Integration

### Quest Dialog Integration

The dialog plugin broadcasts a generic `DialogTriggerEvent`. The questing plugin listens to this event to perform actions requested by dialogs.

```java
package org.runetale.questing.integration;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.dialog.api.DialogRuntimeApi;
import org.runetale.dialog.api.DialogRuntimeRegistry;
import org.runetale.dialog.event.DialogTriggerEvent;
import org.runetale.questing.api.CompleteQuestResult;
import org.runetale.questing.api.QuestRuntimeApi;
import org.runetale.questing.api.QuestRuntimeRegistry;

import javax.annotation.Nonnull;

public final class DialogEventIntegration {

    public static void registerListeners() {
        EventBus.subscribe(DialogTriggerEvent.class, DialogEventIntegration::onDialogTrigger);
    }

    private static void onDialogTrigger(@Nonnull DialogTriggerEvent event) {
        String eventId = event.eventId();
        if (!eventId.equals("quest_event")) return;

        JsonObject data = event.data();
        if (!data.has("action") || !data.has("quest_id")) return;

        String action = data.get("action").getAsString();
        String questId = data.get("quest_id").getAsString();

        QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
        if (questApi == null) return;

        var accessor = event.accessor();
        var playerRef = event.playerRef();

        switch (action) {
            case "start" -> questApi.startQuest(accessor, playerRef, questId);
            
            case "advance" -> {
                String stepId = data.has("step_id") ? data.get("step_id").getAsString() : null;
                if (stepId != null) {
                    questApi.advanceQuestToStep(accessor, playerRef, questId, stepId);
                } else {
                    questApi.advanceQuest(accessor, playerRef, questId);
                }
            }
            
            case "delivery", "complete" -> {
                CompleteQuestResult result = questApi.completeQuest(accessor, playerRef, questId);
                if (!result.success()) {
                    DialogRuntimeApi dialogApi = DialogRuntimeRegistry.get();
                    if (dialogApi != null) {
                        // Dynamically override the dialog to tell the player they don't have enough space
                        dialogApi.setOverrideResponse(event.playerId(), "inventory_full");
                    }
                }
            }
        }
    }
}
```

### Quest Dialog Conditions

```java
package org.runetale.questing.integration;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.dialog.api.ConditionHandler;
import org.runetale.questing.api.QuestRuntimeApi;
import org.runetale.questing.api.QuestRuntimeRegistry;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class QuestDialogConditions {

    public static void registerAll() {
        DialogRuntimeApi dialogApi = DialogRuntimeRegistry.get();
        if (dialogApi == null) {
            return;
        }

        dialogApi.registerConditionHandler(new QuestActiveCondition());
        dialogApi.registerConditionHandler(new QuestCompleteCondition());
        dialogApi.registerConditionHandler(new QuestNotStartedCondition());
        dialogApi.registerConditionHandler(new QuestStepCondition());
    }
}

final class QuestActiveCondition implements ConditionHandler {

    @Override
    @Nonnull
    public String conditionType() {
        return "quest_active";
    }

    @Override
    public boolean evaluate(
            @Nonnull UUID playerId,
            @Nonnull JsonObject data,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
        if (questApi == null) {
            return false;
        }

        String questId = data.getAsString();
        return questApi.isQuestActive(accessor, playerRef, questId);
    }
}

final class QuestCompleteCondition implements ConditionHandler {

    @Override
    @Nonnull
    public String conditionType() {
        return "quest_complete";
    }

    @Override
    public boolean evaluate(
            @Nonnull UUID playerId,
            @Nonnull JsonObject data,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
        if (questApi == null) {
            return false;
        }

        String questId = data.getAsString();
        return questApi.isQuestComplete(accessor, playerRef, questId);
    }
}

final class QuestNotStartedCondition implements ConditionHandler {

    @Override
    @Nonnull
    public String conditionType() {
        return "quest_not_started";
    }

    @Override
    public boolean evaluate(
            @Nonnull UUID playerId,
            @Nonnull JsonObject data,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
        if (questApi == null) {
            return false;
        }

        String questId = data.getAsString();
        return !questApi.isQuestStarted(accessor, playerRef, questId);
    }
}

final class QuestStepCondition implements ConditionHandler {

    @Override
    @Nonnull
    public String conditionType() {
        return "quest_step";
    }

    @Override
    public boolean evaluate(
            @Nonnull UUID playerId,
            @Nonnull JsonObject data,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
        if (questApi == null) {
            return false;
        }

        String questId = data.get("quest").getAsString();
        String stepId = data.get("step").getAsString();

        String currentStep = questApi.getCurrentStepId(accessor, playerRef, questId);
        return stepId.equals(currentStep);
    }
}
```

---

## QuestingPlugin Setup

```java
package org.runetale.questing;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.runetale.questing.api.QuestRuntimeApi;
import org.runetale.questing.api.QuestRuntimeRegistry;
import org.runetale.questing.command.QuestCommand;
import org.runetale.questing.component.PlayerQuestProfileComponent;
import org.runetale.questing.handler.*;
import org.runetale.questing.integration.*;
import org.runetale.questing.journal.JournalService;
import org.runetale.questing.page.QuestJournalPage;
import org.runetale.questing.service.*;

import javax.annotation.Nonnull;

public final class QuestingPlugin extends JavaPlugin implements QuestRuntimeApi {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComponentType<EntityStore, PlayerQuestProfileComponent> profileComponentType;
    private QuestRegistry questRegistry;
    private QuestProgressService progressService;
    private PrerequisiteService prerequisiteService;
    private CustomPrerequisiteService customPrerequisiteService;
    private QuestRewardService rewardService;
    private StepHandlerRegistry stepHandlerRegistry;
    private JournalService journalService;

    private SkillsPluginIntegration skillsIntegration;

    public QuestingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up questing plugin...");

        registerServices();
        registerIntegrations();
        registerComponents();
        registerHandlers();
        registerCommands();
        registerSystems();
        registerDialogIntegration();

        QuestRuntimeRegistry.register(this);

        LOGGER.atInfo().log("Questing plugin setup complete.");
    }

    private void registerServices() {
        this.questRegistry = new QuestRegistry(getDataDirectory());
        this.questRegistry.loadAll();

        this.customPrerequisiteService = new CustomPrerequisiteServiceImpl();
        this.skillsIntegration = new SkillsPluginIntegration();

        this.prerequisiteService = new PrerequisiteService(
            skillsIntegration,
            null,  // item integration - future
            customPrerequisiteService
        );

        this.progressService = new QuestProgressService(questRegistry);
        this.rewardService = new QuestRewardService(skillsIntegration);
        this.journalService = new JournalService(questRegistry, progressService);
    }

    private void registerIntegrations() {
        // Register built-in custom prerequisite handlers
        // These will be available if the respective services are present
    }

    private void registerComponents() {
        this.profileComponentType = getEntityStoreRegistry()
            .registerComponent(PlayerQuestProfileComponent.class, "PlayerQuestProfile",
                PlayerQuestProfileComponent.CODEC);
    }

    private void registerHandlers() {
        this.stepHandlerRegistry = new StepHandlerRegistry();

        stepHandlerRegistry.register(new DialogStepHandler(progressService));
        stepHandlerRegistry.register(new DeliveryStepHandler(progressService));
        stepHandlerRegistry.register(new CollectStepHandler(progressService));
        stepHandlerRegistry.register(new KillStepHandler(progressService));
        stepHandlerRegistry.register(new SkillActionStepHandler(progressService));
        stepHandlerRegistry.register(new ReachStepHandler(progressService));
        stepHandlerRegistry.register(new UseItemOnStepHandler(progressService));
        stepHandlerRegistry.register(new EmoteStepHandler(progressService));
        stepHandlerRegistry.register(new EquipStepHandler(progressService));
        stepHandlerRegistry.register(new ChooseStepHandler(progressService));
        stepHandlerRegistry.register(new CustomStepHandler());

        stepHandlerRegistry.registerAllTriggers(triggerRegistry);
    }

    private void registerCommands() {
        QuestJournalPage journalPage = new QuestJournalPage(journalService);
        getCommandRegistry().registerCommand(new QuestCommand(this, journalPage, prerequisiteService));
    }

    private void registerSystems() {
        getEntityStoreRegistry().registerSystem(
            new EnsurePlayerQuestProfileSystem(profileComponentType));
        getEntityStoreRegistry().registerSystem(
            new QuestProgressSystem(progressService, stepHandlerRegistry));
    }

    private void registerDialogIntegration() {
        // Register quest-related dialog listeners and conditions
        DialogEventIntegration.registerListeners();
        QuestDialogConditions.registerAll();
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Questing plugin started.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Questing plugin shutting down...");

        profileComponentType = null;
        questRegistry = null;
        progressService = null;
        prerequisiteService = null;
        customPrerequisiteService = null;
        rewardService = null;
        stepHandlerRegistry = null;
        journalService = null;
        skillsIntegration = null;

        QuestRuntimeRegistry.clear(this);
    }

    // QuestRuntimeApi implementation methods...
}
```

---

## Event Listeners

### Cross-Plugin Event Handling

```java
package org.runetale.questing.system;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.event.QuestCompleteEvent;
import org.runetale.questing.event.QuestStartEvent;
import org.runetale.questing.event.QuestStepCompleteEvent;

import javax.annotation.Nonnull;

public final class QuestEventSystem {

    private final QuestRewardService rewardService;
    private final SkillsPluginIntegration skillsIntegration;

    public void onQuestStart(@Nonnull QuestStartEvent event) {
        LOGGER.atInfo().log("Player %s started quest: %s", 
            event.playerId(), event.quest().name());

        // Fire cross-plugin events
        EventBus.post(new PlayerQuestStartedEvent(event.playerId(), event.quest().id()));
    }

    public void onQuestStepComplete(@Nonnull QuestStepCompleteEvent event) {
        LOGGER.atInfo().log("Player %s completed step %s in quest %s",
            event.playerId(), event.completedStep().id(), event.quest().name());

        if (event.isQuestComplete()) {
            // Quest will complete next
        }
    }

    public void onQuestComplete(@Nonnull QuestCompleteEvent event) {
        LOGGER.atInfo().log("Player %s completed quest: %s in %s",
            event.playerId(), event.quest().name(), event.completionTime());

        rewardService.distributeRewards(
            event.accessor(),
            event.playerRef(),
            event.rewards()
        );

        // Fire cross-plugin events
        EventBus.post(new PlayerQuestCompletedEvent(
            event.playerId(),
            event.quest().id(),
            event.rewards().questPoints()
        ));
    }
}
```

---

## Future Integrations

### Items Plugin (Planned)

```java
package org.runetale.questing.integration;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.service.ItemIntegration;

import javax.annotation.Nonnull;

public final class ItemsPluginIntegration implements ItemIntegration {

    private final ItemsRuntimeApi itemsApi;

    @Override
    public int getItemCount(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId) {

        return itemsApi.getItemCount(accessor, playerRef, itemId);
    }

    @Override
    public boolean hasItemEquipped(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId) {

        return itemsApi.isEquipped(accessor, playerRef, itemId);
    }

    @Override
    public int getFreeInventorySlots(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {
        
        return itemsApi.getFreeSlots(accessor, playerRef);
    }

    public boolean addItem(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId,
            int quantity) {

        return itemsApi.addItem(accessor, playerRef, itemId, quantity);
    }

    public boolean removeItem(
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String itemId,
            int quantity) {

        return itemsApi.removeItem(accessor, playerRef, itemId, quantity);
    }
}
```

### Zones Plugin (Planned)

```java
package org.runetale.questing.integration;

import org.runetale.zones.api.ZonesRuntimeApi;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ZonesPluginIntegration {

    private final ZonesRuntimeApi zonesApi;

    public void registerQuestZone(
            @Nonnull String zoneId,
            @Nonnull String questId,
            @Nonnull String stepId) {

        zonesApi.registerZoneTrigger(zoneId, (playerId, zone) -> {
            QuestRuntimeApi questApi = QuestRuntimeRegistry.get();
            if (questApi != null) {
                questApi.advanceQuestToStep(
                    getAccessor(playerId),
                    getPlayerRef(playerId),
                    questId,
                    stepId
                );
            }
        });
    }
}
```

---

## Integration Test Example

```java
class SkillsIntegrationTest {

    @Test
    void getsSkillLevelFromSkillsPlugin() {
        SkillsRuntimeApi mockApi = mock(SkillsRuntimeApi.class);
        when(mockApi.getSkillLevel(any(), any(), eq(SkillType.COOKING))).thenReturn(15);

        SkillsRuntimeRegistry.register(mockApi);

        SkillsPluginIntegration integration = new SkillsPluginIntegration();
        int level = integration.getSkillLevel(accessor, playerRef, "COOKING");

        assertThat(level).isEqualTo(15);
    }

    @Test
    void returnsOneWhenSkillsPluginNotAvailable() {
        SkillsRuntimeRegistry.clear(mockApi);

        SkillsPluginIntegration integration = new SkillsPluginIntegration();
        int level = integration.getSkillLevel(accessor, playerRef, "COOKING");

        assertThat(level).isEqualTo(1);
    }
}

class DialogEventIntegrationTest {

    @Test
    void questEventStartsQuest() {
        QuestRuntimeApi mockQuest = mock(QuestRuntimeApi.class);
        QuestRuntimeRegistry.register(mockQuest);

        JsonObject data = new JsonObject();
        data.addProperty("action", "start");
        data.addProperty("quest_id", "cooks_assistant");

        DialogTriggerEvent event = new DialogTriggerEvent(
            playerId, "quest_event", data, accessor, playerRef
        );
        
        EventBus.post(event);

        verify(mockQuest).startQuest(accessor, playerRef, "cooks_assistant");
    }

    @Test
    void questActiveConditionEvaluatesCorrectly() {
        QuestRuntimeApi mockQuest = mock(QuestRuntimeApi.class);
        when(mockQuest.isQuestActive(any(), any(), eq("cooks_assistant"))).thenReturn(true);
        QuestRuntimeRegistry.register(mockQuest);

        QuestActiveCondition condition = new QuestActiveCondition();
        JsonObject data = new JsonPrimitive("cooks_assistant");

        boolean result = condition.evaluate(playerId, data, accessor, playerRef);

        assertThat(result).isTrue();
    }
}
```
