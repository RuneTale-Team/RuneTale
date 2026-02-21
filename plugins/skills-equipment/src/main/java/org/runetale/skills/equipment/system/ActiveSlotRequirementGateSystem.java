package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
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
        if (event.isCancelled() || !this.equipmentConfig.enforceActiveHand()) {
            return;
        }

        int sectionId = event.getInventorySectionId();

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            player = store.getComponent(ref, Player.getComponentType());
        }
        if (player == null) {
            return;
        }
        if (isCreativeExempt(player)) {
            return;
        }

        byte newSlot = event.getNewSlot();
        if (newSlot < 0) {
            return;
        }

        ItemContainer section = player.getInventory().getSectionById(sectionId);
        if (section == null) {
            return;
        }
        if (newSlot >= section.getCapacity()) {
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

        event.setCancelled(true);

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

    private boolean isCreativeExempt(@Nonnull Player player) {
        return player.getGameMode() == GameMode.Creative;
    }

    private record BlockedRequirement(@Nonnull SkillRequirement requirement, int currentLevel) {
    }
}
