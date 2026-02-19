package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.ItemArmorSlot;
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
import java.util.concurrent.ConcurrentHashMap;

public class ArmorEquipRollbackListener {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;
    private final Set<Ref<EntityStore>> guardedRefs = ConcurrentHashMap.newKeySet();

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
        if (event.getItemContainer() != inventory.getArmor()) {
            return;
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || !this.guardedRefs.add(ref)) {
            return;
        }

        try {
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            ItemContainer armor = inventory.getArmor();

            for (short slot = 0; slot < armor.getCapacity(); slot++) {
                ItemStack equipped = armor.getItemStack(slot);
                if (equipped == null || ItemStack.isEmpty(equipped)) {
                    continue;
                }

                BlockedRequirement blocked = findBlockedRequirement(store, ref, equipped);
                if (blocked == null) {
                    continue;
                }

                boolean rolledBack = tryRollbackToSource(armor, slot, equipped, event.getTransaction());
                if (!rolledBack) {
                    MoveTransaction<ItemStackTransaction> fallback = armor.moveItemStackFromSlot(
                            slot,
                            equipped.getQuantity(),
                            inventory.getCombinedBackpackStorageHotbar());
                    rolledBack = fallback.succeeded();
                }

                if (rolledBack && playerRef != null) {
                    this.notificationService.sendBlockedEquipNotice(
                            playerRef,
                            blocked.requirement(),
                            blocked.currentLevel(),
                            equipped.getItem().getId(),
                            EquipmentLocation.fromArmorSlot(ItemArmorSlot.fromValue(slot)));
                }
            }
        } finally {
            this.guardedRefs.remove(ref);
        }
    }

    private boolean tryRollbackToSource(
            @Nonnull ItemContainer armor,
            short armorSlot,
            @Nonnull ItemStack equipped,
            @Nonnull Transaction transaction) {
        if (transaction instanceof MoveTransaction<?> moveTransaction) {
            return rollbackMove(armor, armorSlot, equipped, moveTransaction);
        }
        if (transaction instanceof ListTransaction<?> listTransaction) {
            for (Transaction nested : listTransaction.getList()) {
                if (nested instanceof MoveTransaction<?> moveTransaction
                        && rollbackMove(armor, armorSlot, equipped, moveTransaction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean rollbackMove(
            @Nonnull ItemContainer armor,
            short armorSlot,
            @Nonnull ItemStack equipped,
            @Nonnull MoveTransaction<?> moveTransaction) {
        if (!moveTransaction.succeeded()) {
            return false;
        }

        Transaction addTransaction = moveTransaction.getAddTransaction();
        if (!(addTransaction instanceof SlotTransaction addSlot) || addSlot.getSlot() != armorSlot) {
            return false;
        }

        ItemContainer source = moveTransaction.getOtherContainer();
        if (source == armor) {
            return false;
        }

        short sourceSlot = moveTransaction.getRemoveTransaction().getSlot();
        if (sourceSlot < 0 || sourceSlot >= source.getCapacity()) {
            return false;
        }

        MoveTransaction<SlotTransaction> rollback = armor.moveItemStackFromSlotToSlot(
                armorSlot,
                equipped.getQuantity(),
                source,
                sourceSlot,
                false);
        return rollback.succeeded();
    }

    @Nullable
    private BlockedRequirement findBlockedRequirement(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemStack equipped) {
        for (SkillRequirement requirement : this.requirementTagService.getRequirements(equipped.getItem())) {
            int currentLevel = this.runtimeApi.getSkillLevel(store, ref, requirement.skillType());
            if (currentLevel < requirement.requiredLevel()) {
                return new BlockedRequirement(requirement, currentLevel);
            }
        }
        return null;
    }

    private record BlockedRequirement(@Nonnull SkillRequirement requirement, int currentLevel) {
    }
}
