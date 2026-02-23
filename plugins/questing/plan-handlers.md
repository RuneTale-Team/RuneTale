# Questing Plugin - Step Handlers

## Overview

Step handlers are responsible for:
1. Detecting when a step's conditions are met
2. Updating step progress
3. Triggering step completion

---

## StepHandler Interface

```java
package org.runetale.questing.handler;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.questing.domain.QuestStep;
import org.runetale.questing.domain.StepType;

import javax.annotation.Nonnull;

public interface StepHandler<S extends QuestStep> {

    @Nonnull StepType handledType();

    void registerTriggers(@Nonnull StepTriggerRegistry registry);

    @Nonnull StepProgressResult checkProgress(
            @Nonnull S step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress);
}
```

---

## StepProgressResult

```java
package org.runetale.questing.handler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StepProgressResult {
    private final int newProgress;
    private final int requiredProgress;
    private final boolean completed;
    private final String failureReason;

    private StepProgressResult(int newProgress, int required, boolean completed, @Nullable String failure) {
        this.newProgress = newProgress;
        this.requiredProgress = required;
        this.completed = completed;
        this.failureReason = failure;
    }

    @Nonnull
    public static StepProgressResult inProgress(int current, int required) {
        return new StepProgressResult(current, required, false, null);
    }

    @Nonnull
    public static StepProgressResult complete(int finalProgress) {
        return new StepProgressResult(finalProgress, finalProgress, true, null);
    }

    @Nonnull
    public static StepProgressResult failed(@Nonnull String reason) {
        return new StepProgressResult(0, 0, false, reason);
    }

    public int newProgress() { return newProgress; }
    public int requiredProgress() { return requiredProgress; }
    public boolean completed() { return completed; }
    @Nullable public String failureReason() { return failureReason; }
    public boolean isInProgress() { return !completed && failureReason == null; }
}
```

---

## StepTriggerRegistry

Handlers register listeners for game events that might affect their step type:

```java
package org.runetale.questing.handler;

import java.util.function.Consumer;

public interface StepTriggerRegistry {

    <E> void onEvent(@Nonnull Class<E> eventType, @Nonnull Consumer<E> handler);

    void onPlayerKill(@Nonnull KillHandler handler);

    void onPlayerReachZone(@Nonnull ZoneHandler handler);

    void onPlayerDialog(@Nonnull DialogHandler handler);

    void onItemCollected(@Nonnull ItemHandler handler);

    void onTargetInteract(@Nonnull TargetInteractHandler handler);

    void onSkillAction(@Nonnull SkillActionHandler handler);

    void onEmote(@Nonnull EmoteHandler handler);

    void onEquip(@Nonnull EquipHandler handler);

    void onUseItemOn(@Nonnull UseItemOnHandler handler);

    @FunctionalInterface
    interface KillHandler {
        void onKill(@Nonnull UUID playerId, @Nonnull String mobId, @Nonnull String locationId);
    }

    @FunctionalInterface
    interface ZoneHandler {
        void onReachZone(@Nonnull UUID playerId, @Nonnull String zoneId);
    }

    @FunctionalInterface
    interface DialogHandler {
        void onDialogComplete(@Nonnull UUID playerId, @Nonnull String npcId, @Nonnull String dialogId);
    }

    @FunctionalInterface
    interface ItemHandler {
        void onItemEvent(@Nonnull UUID playerId, @Nonnull String itemId, int quantity);
    }

    @FunctionalInterface
    interface TargetInteractHandler {
        void onInteract(@Nonnull UUID playerId, @Nonnull String targetType, @Nonnull String targetId);
    }

    @FunctionalInterface
    interface SkillActionHandler {
        void onSkillAction(@Nonnull UUID playerId, @Nonnull String skillId, @Nonnull String actionId);
    }

    @FunctionalInterface
    interface EmoteHandler {
        void onEmote(@Nonnull UUID playerId, @Nonnull String emoteId, @Nullable String zoneId);
    }

    @FunctionalInterface
    interface EquipHandler {
        void onEquip(@Nonnull UUID playerId, @Nonnull String itemId, @Nonnull String slot);
    }

    @FunctionalInterface
    interface UseItemOnHandler {
        void onUseItemOn(
                @Nonnull UUID playerId,
                @Nonnull String itemId,
                @Nonnull String targetType,
                @Nonnull String targetId);
    }
}
```

---

## Handler Implementations

### DialogStepHandler

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.DialogStep;

public final class DialogStepHandler implements StepHandler<DialogStep> {

    private final QuestProgressService progressService;

    public DialogStepHandler(@Nonnull QuestProgressService progressService) {
        this.progressService = progressService;
    }

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.DIALOG;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onPlayerDialog((playerId, npcId, dialogId) -> {
            progressService.processActiveSteps(playerId, StepType.DIALOG, (step, accessor, playerRef) -> {
                if (step instanceof DialogStep dialogStep) {
                    if (dialogStep.npcId().equals(npcId) && dialogStep.dialogId().equals(dialogId)) {
                        return StepProgressResult.complete(1);
                    }
                }
                return StepProgressResult.inProgress(0, 1);
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull DialogStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        if (currentProgress > 0) {
            return StepProgressResult.complete(1);
        }
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### CollectStepHandler

Monitors inventory for required items:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.CollectStep;
import org.runetale.questing.domain.ItemRequirement;

public final class CollectStepHandler implements StepHandler<CollectStep> {

    private final ItemService itemService;

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.COLLECT;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onItemCollected((playerId, itemId, quantity) -> {
            progressService.checkAutoProgress(playerId, StepType.COLLECT);
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull CollectStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {

        int completed = 0;
        int total = step.items().size();

        for (ItemRequirement req : step.items()) {
            int has = itemService.getItemCount(accessor, playerRef, req.itemId());
            if (has >= req.quantity()) {
                completed++;
            }
        }

        if (completed == total) {
            return StepProgressResult.complete(total);
        }

        return StepProgressResult.inProgress(completed, total);
    }
}
```

---

### KillStepHandler

Tracks mob kills:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.KillStep;

public final class KillStepHandler implements StepHandler<KillStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.KILL;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onPlayerKill((playerId, mobId, locationId) -> {
            progressService.processActiveSteps(playerId, StepType.KILL, (step, accessor, playerRef) -> {
                if (step instanceof KillStep killStep) {
                    if (killStep.mobId().equals(mobId)) {
                        int current = progressService.getStepProgress(accessor, playerRef, step.id());
                        return checkProgress(killStep, accessor, playerRef, current + 1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull KillStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {

        if (step.requiredItem() != null) {
            if (!itemService.hasItemEquipped(accessor, playerRef, step.requiredItem())) {
                return StepProgressResult.failed("Requires equipped item: " + step.requiredItem());
            }
        }

        if (currentProgress >= step.quantity()) {
            return StepProgressResult.complete(step.quantity());
        }

        return StepProgressResult.inProgress(currentProgress, step.quantity());
    }
}
```

---

### DeliveryStepHandler

Combines item check + NPC interaction:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.DeliveryStep;

public final class DeliveryStepHandler implements StepHandler<DeliveryStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.DELIVERY;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        // Handle physical world interactions (crates, objects, non-dialog NPCs)
        registry.onTargetInteract((playerId, targetType, targetId) -> {
            progressService.processActiveSteps(playerId, StepType.DELIVERY, (step, accessor, playerRef) -> {
                if (step instanceof DeliveryStep deliveryStep) {
                    if (deliveryStep.targetType().equalsIgnoreCase(targetType) && deliveryStep.targetId().equals(targetId)) {
                        return attemptDelivery(deliveryStep, accessor, playerRef);
                    }
                }
                return null;
            });
        });

        // Handle dialog-triggered deliveries
        EventBus.subscribe(DialogTriggerEvent.class, event -> {
            if (!event.eventId().equals("quest_event")) return;
            
            var data = event.data();
            if (!data.has("action") || !data.get("action").getAsString().equals("delivery")) return;
            if (!data.has("quest_id")) return;

            String targetQuestId = data.get("quest_id").getAsString();
            String stepId = data.has("step_id") ? data.get("step_id").getAsString() : null;

            progressService.processActiveSteps(event.playerId(), StepType.DELIVERY, (step, accessor, playerRef) -> {
                if (step instanceof DeliveryStep deliveryStep) {
                    // Match the specific quest and step requested by the dialog
                    if (deliveryStep.id().startsWith(targetQuestId) && (stepId == null || deliveryStep.id().endsWith(stepId))) {
                        StepProgressResult result = attemptDelivery(deliveryStep, accessor, playerRef);
                        
                        if (!result.completed()) {
                            // Tell the dialog API to branch to failure text (requires items)
                            DialogRuntimeApi dialogApi = DialogRuntimeRegistry.get();
                            if (dialogApi != null) {
                                dialogApi.setOverrideResponse(event.playerId(), "missing_items");
                            }
                        }
                        return result;
                    }
                }
                return null;
            });
        });
    }

    @Nonnull
    private StepProgressResult attemptDelivery(
            @Nonnull DeliveryStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {

        for (ItemRequirement req : step.items()) {
            int has = itemService.getItemCount(accessor, playerRef, req.itemId());
            if (has < req.quantity()) {
                return StepProgressResult.failed("Missing item: " + req.itemId());
            }
        }

        if (step.consumeItems()) {
            for (ItemRequirement req : step.items()) {
                itemService.removeItem(accessor, playerRef, req.itemId(), req.quantity());
            }
        }

        return StepProgressResult.complete(1);
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull DeliveryStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### ReachStepHandler

Monitors zone entry:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.ReachStep;

public final class ReachStepHandler implements StepHandler<ReachStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.REACH;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onPlayerReachZone((playerId, zoneId) -> {
            progressService.processActiveSteps(playerId, StepType.REACH, (step, accessor, playerRef) -> {
                if (step instanceof ReachStep reachStep) {
                    if (reachStep.zoneId().equals(zoneId)) {
                        return StepProgressResult.complete(1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull ReachStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### SkillActionStepHandler

Watches for skill-based actions:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.SkillActionStep;

public final class SkillActionStepHandler implements StepHandler<SkillActionStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.SKILL_ACTION;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onSkillAction((playerId, skillId, actionId) -> {
            progressService.processActiveSteps(playerId, StepType.SKILL_ACTION, (step, accessor, playerRef) -> {
                if (step instanceof SkillActionStep skillStep) {
                    if (skillStep.skillId().equals(skillId) && skillStep.actionNode().equals(actionId)) {
                        int current = progressService.getStepProgress(accessor, playerRef, step.id());
                        return checkProgress(skillStep, accessor, playerRef, current + 1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull SkillActionStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {

        if (currentProgress >= step.quantity()) {
            return StepProgressResult.complete(step.quantity());
        }

        return StepProgressResult.inProgress(currentProgress, step.quantity());
    }
}
```

---

### UseItemOnStepHandler

Tracks item-object/NPC interactions:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.UseItemOnStep;

public final class UseItemOnStepHandler implements StepHandler<UseItemOnStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.USE_ITEM_ON;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onUseItemOn((playerId, itemId, targetType, targetId) -> {
            progressService.processActiveSteps(playerId, StepType.USE_ITEM_ON, (step, accessor, playerRef) -> {
                if (step instanceof UseItemOnStep useStep) {
                    boolean itemMatch = useStep.itemId().equals(itemId);
                    boolean targetMatch = useStep.targetType().name().equalsIgnoreCase(targetType) 
                                       && useStep.targetId().equals(targetId);
                    
                    if (itemMatch && targetMatch) {
                        if (useStep.consumesItem()) {
                            itemService.removeItem(accessor, playerRef, itemId, 1);
                        }
                        if (useStep.replacementItemId() != null) {
                            itemService.addItem(accessor, playerRef, useStep.replacementItemId(), 1);
                        }
                        return StepProgressResult.complete(1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull UseItemOnStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### EmoteStepHandler

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.EmoteStep;

public final class EmoteStepHandler implements StepHandler<EmoteStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.EMOTE;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onEmote((playerId, emoteId, zoneId) -> {
            progressService.processActiveSteps(playerId, StepType.EMOTE, (step, accessor, playerRef) -> {
                if (step instanceof EmoteStep emoteStep) {
                    boolean emoteMatch = emoteStep.emoteId().equals(emoteId);
                    boolean zoneMatch = emoteStep.zoneId() == null || emoteStep.zoneId().equals(zoneId);
                    
                    if (emoteMatch && zoneMatch) {
                        return StepProgressResult.complete(1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull EmoteStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### EquipStepHandler

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.EquipStep;

public final class EquipStepHandler implements StepHandler<EquipStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.EQUIP;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
        registry.onEquip((playerId, itemId, slot) -> {
            progressService.processActiveSteps(playerId, StepType.EQUIP, (step, accessor, playerRef) -> {
                if (step instanceof EquipStep equipStep) {
                    boolean itemMatch = equipStep.itemId().equals(itemId);
                    boolean slotMatch = equipStep.slot() == null || equipStep.slot().equalsIgnoreCase(slot);
                    
                    if (itemMatch && slotMatch) {
                        return StepProgressResult.complete(1);
                    }
                }
                return null;
            });
        });
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull EquipStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        if (itemService.hasItemEquipped(accessor, playerRef, step.itemId())) {
            if (step.slot() == null || itemService.isInSlot(accessor, playerRef, step.itemId(), step.slot())) {
                return StepProgressResult.complete(1);
            }
        }
        
        return StepProgressResult.inProgress(0, 1);
    }
}
```

---

### ChooseStepHandler

Handles branching paths:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.ChooseStep;

public final class ChooseStepHandler implements StepHandler<ChooseStep> {

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.CHOOSE;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull ChooseStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        String chosenBranch = progressService.getChosenBranch(accessor, playerRef, step.id());
        
        if (chosenBranch != null) {
            return StepProgressResult.complete(1);
        }
        
        return StepProgressResult.inProgress(0, 1);
    }

    @Nonnull
    public List<ChooseStep.Branch> getAvailableBranches(
            @Nonnull ChooseStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {
        
        return step.branches().stream()
            .filter(branch -> isBranchAvailable(branch, accessor, playerRef))
            .collect(Collectors.toList());
    }

    private boolean isBranchAvailable(
            @Nonnull ChooseStep.Branch branch,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef) {
        
        if (branch.condition() == null) {
            return true;
        }
        
        return conditionService.evaluate(branch.condition(), accessor, playerRef);
    }

    public void chooseBranch(
            @Nonnull ChooseStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull String branchId) {
        
        progressService.setChosenBranch(accessor, playerRef, step.id(), branchId);
    }
}
```

---

### CustomStepHandler

Delegates to registered custom handlers:

```java
package org.runetale.questing.handler;

import org.runetale.questing.domain.CustomStep;

public final class CustomStepHandler implements StepHandler<CustomStep> {

    private final Map<String, CustomHandler> customHandlers = new ConcurrentHashMap<>();

    @Override
    @Nonnull
    public StepType handledType() {
        return StepType.CUSTOM;
    }

    @Override
    public void registerTriggers(@Nonnull StepTriggerRegistry registry) {
    }

    public void registerCustomHandler(@Nonnull String handlerId, @Nonnull CustomHandler handler) {
        customHandlers.put(handlerId, handler);
    }

    @Override
    @Nonnull
    public StepProgressResult checkProgress(
            @Nonnull CustomStep step,
            @Nonnull ComponentAccessor<EntityStore> accessor,
            @Nonnull Ref<EntityStore> playerRef,
            int currentProgress) {
        
        CustomHandler handler = customHandlers.get(step.handlerId());
        
        if (handler == null) {
            return StepProgressResult.failed("No handler registered: " + step.handlerId());
        }
        
        return handler.checkProgress(step, accessor, playerRef, currentProgress);
    }

    @FunctionalInterface
    public interface CustomHandler {
        @Nonnull StepProgressResult checkProgress(
                @Nonnull CustomStep step,
                @Nonnull ComponentAccessor<EntityStore> accessor,
                @Nonnull Ref<EntityStore> playerRef,
                int currentProgress);
    }
}
```

---

## Handler Registry

```java
package org.runetale.questing.handler;

public final class StepHandlerRegistry {

    private final Map<StepType, StepHandler<?>> handlers = new EnumMap<>(StepType.class);

    public void register(@Nonnull StepHandler<?> handler) {
        handlers.put(handler.handledType(), handler);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <S extends QuestStep> StepHandler<S> getHandler(@Nonnull StepType type) {
        return (StepHandler<S>) handlers.get(type);
    }

    @Nonnull
    public <S extends QuestStep> StepHandler<S> getHandlerOrThrow(@Nonnull StepType type) {
        StepHandler<S> handler = getHandler(type);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for: " + type);
        }
        return handler;
    }

    public void registerAllTriggers(@Nonnull StepTriggerRegistry registry) {
        for (StepHandler<?> handler : handlers.values()) {
            handler.registerTriggers(registry);
        }
    }
}
```

---

## Test Examples

```java
class KillStepHandlerTest {

    @Test
    void completesWhenKillCountReached() {
        KillStep step = new KillStep("kill_goblins", "Kill goblins", "goblin", 5, null);
        KillStepHandler handler = new KillStepHandler();

        StepProgressResult result = handler.checkProgress(step, accessor, playerRef, 5);
        
        assertThat(result.completed()).isTrue();
        assertThat(result.newProgress()).isEqualTo(5);
    }

    @Test
    void failsWhenRequiredItemNotEquipped() {
        KillStep step = new KillStep("kill_delrith", "Kill Delrith", "delrith", 1, "silverlight");
        KillStepHandler handler = new KillStepHandler(itemService);
        
        when(itemService.hasItemEquipped(any(), any(), eq("silverlight"))).thenReturn(false);
        
        StepProgressResult result = handler.checkProgress(step, accessor, playerRef, 1);
        
        assertThat(result.completed()).isFalse();
        assertThat(result.failureReason()).contains("silverlight");
    }
}

class CollectStepHandlerTest {

    @Test
    void tracksProgressAcrossMultipleItems() {
        CollectStep step = new CollectStep(
            "gather_ingredients",
            "Gather ingredients",
            List.of(
                new ItemRequirement("egg", 1),
                new ItemRequirement("milk", 1),
                new ItemRequirement("flour", 1)
            ),
            true
        );

        when(itemService.getItemCount(any(), any(), eq("egg"))).thenReturn(1);
        when(itemService.getItemCount(any(), any(), eq("milk"))).thenReturn(1);
        when(itemService.getItemCount(any(), any(), eq("flour"))).thenReturn(0);

        CollectStepHandler handler = new CollectStepHandler(itemService);
        StepProgressResult result = handler.checkProgress(step, accessor, playerRef, 0);
        
        assertThat(result.completed()).isFalse();
        assertThat(result.newProgress()).isEqualTo(2);
        assertThat(result.requiredProgress()).isEqualTo(3);
    }
}
```
