package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ToolUseEntityDamageGateSystem extends DamageEventSystem {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;
    private final Query<EntityStore> query;

    public ToolUseEntityDamageGateSystem(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        this.runtimeApi = runtimeApi;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
        this.query = AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        if (event.isCancelled()) {
            return;
        }

        Damage.Source source = event.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) {
            return;
        }

        Player attacker = commandBuffer.getComponent(attackerRef, Player.getComponentType());
        if (attacker == null) {
            attacker = store.getComponent(attackerRef, Player.getComponentType());
        }
        if (attacker == null || attacker.getGameMode() == GameMode.Creative) {
            return;
        }

        ItemStack heldItem = attacker.getInventory().getItemInHand();
        if (heldItem == null || ItemStack.isEmpty(heldItem)) {
            return;
        }

        BlockedRequirement blocked = findBlockedRequirement(commandBuffer, attackerRef, heldItem);
        if (blocked == null) {
            return;
        }

        event.setCancelled(true);
        event.setAmount(0.0F);

        PlayerRef attackerPlayerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attackerPlayerRef == null) {
            attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        }
        if (attackerPlayerRef == null) {
            return;
        }

        this.notificationService.sendBlockedEquipNotice(
                attackerPlayerRef,
                blocked.requirement(),
                blocked.currentLevel(),
                heldItem.getItem().getId(),
                EquipmentLocation.MAINHAND);
    }

    @Nullable
    private BlockedRequirement findBlockedRequirement(
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull ItemStack heldItem) {
        for (SkillRequirement requirement : this.requirementTagService.getRequirements(heldItem.getItem())) {
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

    @Nonnull
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    private record BlockedRequirement(@Nonnull SkillRequirement requirement, int currentLevel) {
    }
}
