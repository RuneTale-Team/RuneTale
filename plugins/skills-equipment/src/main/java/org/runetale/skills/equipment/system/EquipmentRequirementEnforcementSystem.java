package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EquipmentRequirementEnforcementSystem extends DelayedSystem<EntityStore> {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentConfig equipmentConfig;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;

    public EquipmentRequirementEnforcementSystem(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentConfig equipmentConfig,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        super(equipmentConfig.armorScanTickSeconds());
        this.runtimeApi = runtimeApi;
        this.equipmentConfig = equipmentConfig;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        if (!this.equipmentConfig.enforceArmor()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                continue;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                continue;
            }

            installArmorFilters(store, ref, playerRef, player);

            if (isCreativeExempt(player)) {
                continue;
            }

            if (this.equipmentConfig.enforceArmor()) {
                enforceArmor(store, ref, playerRef, player);
            }
        }
    }

    private void installArmorFilters(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player) {
        ItemContainer armor = player.getInventory().getArmor();
        for (short slot = 0; slot < armor.getCapacity(); slot++) {
            final short armorSlot = slot;
            armor.setSlotFilter(FilterActionType.ADD, slot, (actionType, container, targetSlot, itemStack) -> {
                if (itemStack == null || ItemStack.isEmpty(itemStack)) {
                    return true;
                }

                ItemArmor armorConfig = itemStack.getItem().getArmor();
                if (armorConfig == null) {
                    return false;
                }

                if (armorSlot >= ItemArmorSlot.VALUES.length) {
                    return false;
                }

                ItemArmorSlot expectedSlot = ItemArmorSlot.fromValue(armorSlot);
                if (armorConfig.getArmorSlot() != expectedSlot) {
                    return false;
                }

                if (player.getGameMode() == GameMode.Creative) {
                    return true;
                }

                BlockedRequirement blocked = findFirstUnmetRequirement(store, ref, itemStack);
                if (blocked == null) {
                    return true;
                }

                this.notificationService.sendBlockedEquipNotice(
                        playerRef,
                        blocked.requirement(),
                        blocked.currentLevel(),
                        itemStack.getItem().getId(),
                        EquipmentLocation.fromArmorSlot(expectedSlot));
                return false;
            });
        }
    }

    private void enforceArmor(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player) {
        Inventory inventory = player.getInventory();
        ItemContainer armor = inventory.getArmor();

        for (int slotIndex = 0; slotIndex < armor.getCapacity(); slotIndex++) {
            ItemStack equipped = armor.getItemStack((short) slotIndex);
            if (equipped == null || ItemStack.isEmpty(equipped)) {
                continue;
            }

            ItemArmorSlot armorSlot = ItemArmorSlot.fromValue(slotIndex);
            EquipmentLocation location = EquipmentLocation.fromArmorSlot(armorSlot);
            BlockedRequirement blocked = findFirstUnmetRequirement(store, ref, equipped);
            if (blocked == null) {
                continue;
            }

            MoveTransaction<ItemStackTransaction> transaction = armor.moveItemStackFromSlot(
                    (short) slotIndex,
                    equipped.getQuantity(),
                    inventory.getCombinedBackpackStorageHotbar());

            if (transaction.succeeded()) {
                this.notificationService.sendBlockedEquipNotice(
                        playerRef,
                        blocked.requirement(),
                        blocked.currentLevel(),
                        equipped.getItem().getId(),
                        location);
            }
        }
    }

    @Nullable
    private BlockedRequirement findFirstUnmetRequirement(
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

    private boolean isCreativeExempt(@Nonnull Player player) {
        return player.getGameMode() == GameMode.Creative;
    }
}
