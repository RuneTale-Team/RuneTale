package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
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
        if (!this.equipmentConfig.enforceArmor() && !this.equipmentConfig.enforceActiveHandReconcile()) {
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

            if (this.equipmentConfig.enforceArmor()) {
                enforceArmor(store, ref, playerRef, player);
            }
            if (this.equipmentConfig.enforceActiveHandReconcile()) {
                enforceActiveHand(store, ref, playerRef, player);
            }
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

    private void enforceActiveHand(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player) {
        Inventory inventory = player.getInventory();
        enforceActiveSection(store, ref, playerRef, inventory, this.equipmentConfig.activeSectionHotbar());
        enforceActiveSection(store, ref, playerRef, inventory, this.equipmentConfig.activeSectionTools());
    }

    private void enforceActiveSection(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Inventory inventory,
            int sectionId) {
        byte activeSlot;
        try {
            activeSlot = inventory.getActiveSlot(sectionId);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (activeSlot < 0) {
            return;
        }

        ItemContainer section = inventory.getSectionById(sectionId);
        if (section == null) {
            return;
        }
        int capacity = Math.min(section.getCapacity(), selectionCapacity(sectionId));
        if (capacity <= 0) {
            return;
        }

        ItemStack equipped = section.getItemStack(activeSlot);
        if (equipped == null || ItemStack.isEmpty(equipped)) {
            return;
        }

        BlockedRequirement blocked = findFirstUnmetRequirement(store, ref, equipped);
        if (blocked == null) {
            return;
        }

        byte fallbackSlot = findNextAllowedSlot(store, ref, section, activeSlot, capacity);
        try {
            inventory.setActiveSlot(sectionId, fallbackSlot);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        this.notificationService.sendBlockedEquipNotice(
                playerRef,
                blocked.requirement(),
                blocked.currentLevel(),
                equipped.getItem().getId(),
                EquipmentLocation.MAINHAND);
    }

    private byte findNextAllowedSlot(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ItemContainer section,
            byte blockedSlot,
            int capacity) {
        int start = wrap(blockedSlot, capacity);
        for (int offset = 1; offset < capacity; offset++) {
            int slot = wrap(start + offset, capacity);
            ItemStack candidate = section.getItemStack((short) slot);
            if (candidate == null || ItemStack.isEmpty(candidate)) {
                return (byte) slot;
            }

            if (findFirstUnmetRequirement(store, ref, candidate) == null) {
                return (byte) slot;
            }
        }

        return Inventory.INACTIVE_SLOT_INDEX;
    }

    private int selectionCapacity(int sectionId) {
        if (sectionId == this.equipmentConfig.activeSectionHotbar()) {
            return this.equipmentConfig.activeSelectionSlotsHotbar();
        }
        if (sectionId == this.equipmentConfig.activeSectionTools()) {
            return this.equipmentConfig.activeSelectionSlotsTools();
        }
        return Integer.MAX_VALUE;
    }

    private int wrap(int slot, int capacity) {
        int wrapped = slot % capacity;
        if (wrapped < 0) {
            wrapped += capacity;
        }
        return wrapped;
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
}
