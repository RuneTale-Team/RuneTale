package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ToolUseDamageBlockGateSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;
    private final Query<EntityStore> query;

    public ToolUseDamageBlockGateSystem(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        super(DamageBlockEvent.class);
        this.runtimeApi = runtimeApi;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
        this.query = Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DamageBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            player = store.getComponent(ref, Player.getComponentType());
        }
        if (player == null || player.getGameMode() == GameMode.Creative) {
            return;
        }

        ItemStack heldItem = event.getItemInHand();
        if (heldItem == null || ItemStack.isEmpty(heldItem)) {
            return;
        }

        BlockedRequirement blocked = findBlockedRequirement(commandBuffer, ref, heldItem);
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
                heldItem.getItem().getId(),
                EquipmentLocation.MAINHAND);
    }

    @Nullable
    private BlockedRequirement findBlockedRequirement(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull ItemStack stack) {
        for (SkillRequirement requirement : this.requirementTagService.getRequirements(stack.getItem())) {
            int currentLevel = this.runtimeApi.getSkillLevel(commandBuffer, playerRef, requirement.skillType());
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
