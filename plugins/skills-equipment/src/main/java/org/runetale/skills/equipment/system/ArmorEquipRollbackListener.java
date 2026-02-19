package org.runetale.skills.equipment.system;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorEquipRollbackListener {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;
    private final Set<UUID> guardedPlayers = ConcurrentHashMap.newKeySet();

    public ArmorEquipRollbackListener(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        this.runtimeApi = runtimeApi;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
    }

    public void handle(@Nonnull LivingEntityInventoryChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.getGameMode() == GameMode.Creative) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemContainer armor = inventory.getArmor();
        if (event.getItemContainer() != armor) {
            return;
        }

        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        if (!this.guardedPlayers.add(playerId)) {
            return;
        }

        try {
            for (short slot = 0; slot < armor.getCapacity(); slot++) {
                ItemStack equipped = armor.getItemStack(slot);
                if (equipped == null || ItemStack.isEmpty(equipped)) {
                    continue;
                }

                BlockedRequirement blocked = findBlockedRequirement(ref.getStore(), ref, equipped);
                if (blocked == null) {
                    continue;
                }

                if (!tryRollbackToSource(armor, slot, equipped, event.getTransaction())) {
                    MoveTransaction<ItemStackTransaction> fallback = armor.moveItemStackFromSlot(
                            slot,
                            equipped.getQuantity(),
                            inventory.getCombinedBackpackStorageHotbar());
                    if (!fallback.succeeded()) {
                        continue;
                    }
                }

                if (playerRef != null) {
                    this.notificationService.sendBlockedEquipNotice(
                            playerRef,
                            blocked.requirement(),
                            blocked.currentLevel(),
                            equipped.getItem().getId(),
                            EquipmentLocation.fromArmorSlot(com.hypixel.hytale.protocol.ItemArmorSlot.fromValue(slot)));
                }
            }
        } finally {
            this.guardedPlayers.remove(playerId);
        }
    }

    private boolean tryRollbackToSource(
            @Nonnull ItemContainer armor,
            short armorSlot,
            @Nonnull ItemStack equipped,
            @Nonnull Transaction transaction) {
        if (transaction instanceof MoveTransaction<?> moveTransaction) {
            return rollbackMoveTransaction(armor, armorSlot, equipped, moveTransaction);
        }
        if (transaction instanceof ListTransaction<?> listTransaction) {
            for (Transaction nested : listTransaction.getList()) {
                if (nested instanceof MoveTransaction<?> moveTransaction
                        && rollbackMoveTransaction(armor, armorSlot, equipped, moveTransaction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean rollbackMoveTransaction(
            @Nonnull ItemContainer armor,
            short armorSlot,
            @Nonnull ItemStack equipped,
            @Nonnull MoveTransaction<?> moveTransaction) {
        if (!moveTransaction.succeeded()) {
            return false;
        }

        Transaction addTransaction = moveTransaction.getAddTransaction();
        if (!(addTransaction instanceof SlotTransaction addSlotTransaction)) {
            return false;
        }
        if (addSlotTransaction.getSlot() != armorSlot) {
            return false;
        }

        ItemContainer sourceContainer = moveTransaction.getOtherContainer();
        if (sourceContainer == armor) {
            return false;
        }

        short sourceSlot = moveTransaction.getRemoveTransaction().getSlot();
        if (sourceSlot < 0 || sourceSlot >= sourceContainer.getCapacity()) {
            return false;
        }

        MoveTransaction<SlotTransaction> rollback = armor.moveItemStackFromSlotToSlot(
                armorSlot,
                equipped.getQuantity(),
                sourceContainer,
                sourceSlot);
        return rollback.succeeded();
    }

    @Nullable
    private BlockedRequirement findBlockedRequirement(
            @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
            @Nonnull com.hypixel.hytale.component.Ref<EntityStore> playerRef,
            @Nonnull ItemStack equipped) {
        for (SkillRequirement requirement : this.requirementTagService.getRequirements(equipped.getItem())) {
            int currentLevel = this.runtimeApi.getSkillLevel(store, playerRef, requirement.skillType());
            if (currentLevel < requirement.requiredLevel()) {
                return new BlockedRequirement(requirement, currentLevel);
            }
        }
        return null;
    }

    private record BlockedRequirement(@Nonnull SkillRequirement requirement, int currentLevel) {
    }
}
