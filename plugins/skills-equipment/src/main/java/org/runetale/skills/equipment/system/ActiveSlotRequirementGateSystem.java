package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ActiveSlotRequirementGateSystem extends EntityEventSystem<EntityStore, SwitchActiveSlotEvent> {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentConfig equipmentConfig;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;
    private final Query<EntityStore> query;

    public ActiveSlotRequirementGateSystem(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentConfig equipmentConfig,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        super(SwitchActiveSlotEvent.class);
        this.runtimeApi = runtimeApi;
        this.equipmentConfig = equipmentConfig;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull SwitchActiveSlotEvent event) {
        if (event.isCancelled() || event.isServerRequest() || !this.equipmentConfig.enforceActiveHand()) {
            return;
        }

        int sectionId = event.getInventorySectionId();
        if (sectionId != this.equipmentConfig.activeSectionHotbar() && sectionId != this.equipmentConfig.activeSectionTools()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            player = store.getComponent(ref, Player.getComponentType());
        }
        if (player == null) {
            return;
        }

        int selectionCapacity = selectionCapacity(sectionId, player);
        if (selectionCapacity <= 0) {
            return;
        }

        byte newSlot = event.getNewSlot();
        if (newSlot < 0 || newSlot >= selectionCapacity) {
            return;
        }

        ItemContainer section = player.getInventory().getSectionById(sectionId);
        if (section == null) {
            return;
        }

        ItemStack selectedStack = section.getItemStack(newSlot);
        if (selectedStack == null || ItemStack.isEmpty(selectedStack)) {
            return;
        }

        BlockedRequirement blocked = findFirstUnmetRequirement(commandBuffer, ref, selectedStack);
        if (blocked == null) {
            return;
        }

        byte fallbackSlot = findFallbackAllowedSlot(commandBuffer, ref, event, section, selectionCapacity);
        if (fallbackSlot >= 0) {
            event.setNewSlot(fallbackSlot);
        } else {
            event.setCancelled(true);
        }

        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRef == null) {
            return;
        }

        this.notificationService.sendBlockedEquipNotice(
                playerRef,
                blocked.requirement(),
                blocked.currentLevel(),
                selectedStack.getItem().getId(),
                EquipmentLocation.MAINHAND);
    }

    private byte findFallbackAllowedSlot(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull SwitchActiveSlotEvent event,
            @Nonnull ItemContainer section,
            int selectionCapacity) {
        int capacity = Math.min(section.getCapacity(), selectionCapacity);
        if (capacity <= 0) {
            return -1;
        }

        int previousSlot = event.getPreviousSlot();
        int direction = selectionDirection(previousSlot, event.getNewSlot(), capacity);
        int start = wrapSlot(event.getNewSlot(), capacity);

        for (int i = 1; i < capacity; i++) {
            int candidateSlot = wrapSlot(start + (i * direction), capacity);
            ItemStack candidateStack = section.getItemStack((short) candidateSlot);
            if (candidateStack == null || ItemStack.isEmpty(candidateStack)) {
                return (byte) candidateSlot;
            }

            if (findFirstUnmetRequirement(commandBuffer, ref, candidateStack) == null) {
                return (byte) candidateSlot;
            }
        }

        if (previousSlot >= 0 && previousSlot < capacity) {
            return (byte) previousSlot;
        }
        return -1;
    }

    private int selectionDirection(int previousSlot, int newSlot, int capacity) {
        if (capacity <= 1) {
            return 1;
        }

        int prev = wrapSlot(previousSlot, capacity);
        int next = wrapSlot(newSlot, capacity);
        int forwardDistance = (next - prev + capacity) % capacity;
        int backwardDistance = (prev - next + capacity) % capacity;
        if (forwardDistance == 0) {
            return 1;
        }
        return forwardDistance <= backwardDistance ? 1 : -1;
    }

    private int wrapSlot(int slot, int capacity) {
        int wrapped = slot % capacity;
        if (wrapped < 0) {
            wrapped += capacity;
        }
        return wrapped;
    }

    private int selectionCapacity(int sectionId, @Nonnull Player player) {
        int sectionCapacity = sectionCapacity(sectionId, player);
        if (sectionCapacity <= 0) {
            return 0;
        }

        int configured = sectionId == this.equipmentConfig.activeSectionHotbar()
                ? this.equipmentConfig.activeSelectionSlotsHotbar()
                : this.equipmentConfig.activeSelectionSlotsTools();
        return Math.min(sectionCapacity, Math.max(1, configured));
    }

    private int sectionCapacity(int sectionId, @Nonnull Player player) {
        ItemContainer section = player.getInventory().getSectionById(sectionId);
        if (section == null) {
            return 0;
        }
        return section.getCapacity();
    }

    @Nullable
    private BlockedRequirement findFirstUnmetRequirement(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemStack stack) {
        for (SkillRequirement requirement : this.requirementTagService.getRequirements(stack.getItem())) {
            int currentLevel = this.runtimeApi.getSkillLevel(commandBuffer, ref, requirement.skillType());
            if (currentLevel < requirement.requiredLevel()) {
                return new BlockedRequirement(requirement, currentLevel);
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    private record BlockedRequirement(@Nonnull SkillRequirement requirement, int currentLevel) {
    }
}
